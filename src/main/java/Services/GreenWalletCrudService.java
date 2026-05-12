package Services;

// This class MUST NOT write to wallet.available_credits or wallet.retired_credits.
// Balance mutations are exclusively handled by EventListenerService.

import DataBase.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wallet CRUD service — creates, lists, finds, updates, and deletes wallet rows
 * without ever touching {@code wallet.available_credits} or
 * {@code wallet.retired_credits}.
 *
 * <p>All database access uses {@link MyConnection#getConnection()} (plain JDBC,
 * no DI framework). Connections, statements, and result-sets are always closed
 * in {@code finally} blocks.
 *
 * <p><strong>Balance-mutation boundary:</strong> This class MUST NOT write to
 * {@code wallet.available_credits} or {@code wallet.retired_credits}. Balance
 * mutations are exclusively handled by {@code EventListenerService}.
 *
 * Requirements: 10.1 – 10.8
 */
public class GreenWalletCrudService {

    // =========================================================================
    // Sub-task 4.1 — Wallet CRUD methods
    // =========================================================================

    /**
     * Insert a new wallet row and return the generated {@code id}.
     *
     * <p>If {@code walletNumber} is {@code null}, a unique number is generated
     * via {@code SELECT COALESCE(MAX(wallet_number),100000)+1 FROM wallet}.
     *
     * Requirements: 10.1
     *
     * @param name         human-readable wallet name (may be null)
     * @param walletNumber explicit wallet number, or {@code null} to auto-generate
     * @param ownerType    owner type string (e.g. "USER", "ENTERPRISE")
     * @param ownerId      owner primary key
     * @return generated {@code wallet.id}
     * @throws RuntimeException if the INSERT fails
     */
    public int createWallet(String name, Integer walletNumber, String ownerType, int ownerId) {
        Connection conn = null;
        PreparedStatement psNum = null;
        ResultSet rsNum = null;
        PreparedStatement psInsert = null;
        ResultSet rsKeys = null;
        try {
            conn = MyConnection.getConnection();

            // Generate wallet number if not provided
            int effectiveWalletNumber;
            if (walletNumber != null) {
                effectiveWalletNumber = walletNumber;
            } else {
                String numSql = "SELECT COALESCE(MAX(wallet_number), 100000) + 1 FROM wallet";
                psNum = conn.prepareStatement(numSql);
                rsNum = psNum.executeQuery();
                if (rsNum.next()) {
                    effectiveWalletNumber = rsNum.getInt(1);
                } else {
                    effectiveWalletNumber = 100001;
                }
            }

            String insertSql =
                "INSERT INTO wallet (wallet_number, name, owner_type, owner_id, available_credits, retired_credits) " +
                "VALUES (?, ?, ?, ?, 0, 0)";
            psInsert = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
            psInsert.setInt(1, effectiveWalletNumber);
            if (name != null) {
                psInsert.setString(2, name);
            } else {
                psInsert.setNull(2, Types.VARCHAR);
            }
            psInsert.setString(3, ownerType);
            psInsert.setInt(4, ownerId);
            psInsert.executeUpdate();

            rsKeys = psInsert.getGeneratedKeys();
            if (rsKeys.next()) {
                return rsKeys.getInt(1);
            }
            throw new RuntimeException("createWallet: INSERT succeeded but no generated key was returned");
        } catch (SQLException ex) {
            throw new RuntimeException("createWallet failed: " + ex.getMessage(), ex);
        } finally {
            closeQuietly(rsKeys);
            closeQuietly(psInsert);
            closeQuietly(rsNum);
            closeQuietly(psNum);
            closeQuietly(conn);
        }
    }

    /**
     * Return a list of wallet rows.
     *
     * <p>COALESCE is applied to {@code name} and {@code blockchain_address} so
     * callers always receive a non-null string for those columns.
     * Admin callers receive up to 100 rows; non-admin callers receive up to 20
     * rows filtered to their own {@code owner_id}.
     *
     * Requirements: 10.2
     *
     * @param isAdmin {@code true} to return all wallets (capped at 100)
     * @param ownerId owner filter applied when {@code isAdmin} is {@code false}
     * @return list of wallet rows as {@code Map<String,Object>}
     */
    public List<Map<String, Object>> listWallets(boolean isAdmin, Integer ownerId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = MyConnection.getConnection();

            String sql;
            if (isAdmin) {
                sql = "SELECT id, wallet_number, COALESCE(name, '') AS name, owner_type, owner_id, " +
                      "COALESCE(blockchain_address, '') AS blockchain_address, " +
                      "available_credits, retired_credits, created_at " +
                      "FROM wallet ORDER BY id DESC LIMIT 100";
                ps = conn.prepareStatement(sql);
            } else {
                sql = "SELECT id, wallet_number, COALESCE(name, '') AS name, owner_type, owner_id, " +
                      "COALESCE(blockchain_address, '') AS blockchain_address, " +
                      "available_credits, retired_credits, created_at " +
                      "FROM wallet WHERE owner_id = ? ORDER BY id DESC LIMIT 20";
                ps = conn.prepareStatement(sql);
                ps.setInt(1, ownerId != null ? ownerId : 0);
            }

            rs = ps.executeQuery();
            List<Map<String, Object>> results = new ArrayList<>();
            while (rs.next()) {
                results.add(resultSetRowToMap(rs));
            }
            return results;
        } catch (SQLException ex) {
            throw new RuntimeException("listWallets failed: " + ex.getMessage(), ex);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
    }

    /**
     * Find a single wallet by its primary key.
     *
     * <p>When {@code isAdmin} is {@code false} an additional
     * {@code owner_id = ownerId} predicate is applied; the method returns
     * {@code null} if no matching row is found.
     *
     * Requirements: 10.3
     *
     * @param walletId primary key of the wallet
     * @param isAdmin  {@code true} to skip the owner check
     * @param ownerId  owner filter applied when {@code isAdmin} is {@code false}
     * @return wallet row as {@code Map<String,Object>}, or {@code null}
     */
    public Map<String, Object> findWalletById(int walletId, boolean isAdmin, Integer ownerId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = MyConnection.getConnection();

            String sql;
            if (isAdmin) {
                sql = "SELECT id, wallet_number, COALESCE(name, '') AS name, owner_type, owner_id, " +
                      "COALESCE(blockchain_address, '') AS blockchain_address, " +
                      "available_credits, retired_credits, created_at " +
                      "FROM wallet WHERE id = ?";
                ps = conn.prepareStatement(sql);
                ps.setInt(1, walletId);
            } else {
                sql = "SELECT id, wallet_number, COALESCE(name, '') AS name, owner_type, owner_id, " +
                      "COALESCE(blockchain_address, '') AS blockchain_address, " +
                      "available_credits, retired_credits, created_at " +
                      "FROM wallet WHERE id = ? AND owner_id = ?";
                ps = conn.prepareStatement(sql);
                ps.setInt(1, walletId);
                ps.setInt(2, ownerId != null ? ownerId : 0);
            }

            rs = ps.executeQuery();
            if (rs.next()) {
                return resultSetRowToMap(rs);
            }
            return null;
        } catch (SQLException ex) {
            throw new RuntimeException("findWalletById failed: " + ex.getMessage(), ex);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
    }

    /**
     * Update the {@code name} and {@code owner_type} of a wallet.
     *
     * <p>When {@code isAdmin} is {@code false} an additional
     * {@code owner_id = ownerId} predicate is applied so that users cannot
     * rename wallets they do not own.
     *
     * Requirements: 10.4
     *
     * @param walletId  primary key of the wallet to update
     * @param name      new name value (may be null to clear the name)
     * @param ownerType new owner type value
     * @param isAdmin   {@code true} to skip the owner check
     * @param ownerId   owner filter applied when {@code isAdmin} is {@code false}
     */
    public void updateWalletName(int walletId, String name, String ownerType,
                                 boolean isAdmin, Integer ownerId) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = MyConnection.getConnection();

            String sql;
            if (isAdmin) {
                sql = "UPDATE wallet SET name = ?, owner_type = ? WHERE id = ?";
                ps = conn.prepareStatement(sql);
                if (name != null) {
                    ps.setString(1, name);
                } else {
                    ps.setNull(1, Types.VARCHAR);
                }
                ps.setString(2, ownerType);
                ps.setInt(3, walletId);
            } else {
                sql = "UPDATE wallet SET name = ?, owner_type = ? WHERE id = ? AND owner_id = ?";
                ps = conn.prepareStatement(sql);
                if (name != null) {
                    ps.setString(1, name);
                } else {
                    ps.setNull(1, Types.VARCHAR);
                }
                ps.setString(2, ownerType);
                ps.setInt(3, walletId);
                ps.setInt(4, ownerId != null ? ownerId : 0);
            }

            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("updateWalletName failed: " + ex.getMessage(), ex);
        } finally {
            closeQuietly(ps);
            closeQuietly(conn);
        }
    }

    /**
     * Delete a wallet row.
     *
     * <p>The wallet's {@code available_credits} is checked first; if it is
     * greater than zero a {@link RuntimeException} is thrown and no deletion
     * occurs.
     *
     * <p>When {@code isAdmin} is {@code false} an additional
     * {@code owner_id = ownerId} predicate is applied to the DELETE statement.
     *
     * Requirements: 10.5, 10.6
     *
     * @param walletId primary key of the wallet to delete
     * @param isAdmin  {@code true} to skip the owner check
     * @param ownerId  owner filter applied when {@code isAdmin} is {@code false}
     * @throws RuntimeException if the wallet holds available credits
     */
    public void deleteWallet(int walletId, boolean isAdmin, Integer ownerId) {
        Connection conn = null;
        PreparedStatement psCheck = null;
        ResultSet rsCheck = null;
        PreparedStatement psDelete = null;
        try {
            conn = MyConnection.getConnection();

            // Check available_credits before deleting
            String checkSql = "SELECT available_credits FROM wallet WHERE id = ?";
            psCheck = conn.prepareStatement(checkSql);
            psCheck.setInt(1, walletId);
            rsCheck = psCheck.executeQuery();
            if (rsCheck.next()) {
                double availableCredits = rsCheck.getDouble(1);
                if (availableCredits > 0) {
                    throw new RuntimeException("Cannot delete wallet with available credits");
                }
            }

            // Perform the DELETE with optional owner check
            String deleteSql;
            if (isAdmin) {
                deleteSql = "DELETE FROM wallet WHERE id = ?";
                psDelete = conn.prepareStatement(deleteSql);
                psDelete.setInt(1, walletId);
            } else {
                deleteSql = "DELETE FROM wallet WHERE id = ? AND owner_id = ?";
                psDelete = conn.prepareStatement(deleteSql);
                psDelete.setInt(1, walletId);
                psDelete.setInt(2, ownerId != null ? ownerId : 0);
            }
            psDelete.executeUpdate();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (SQLException ex) {
            throw new RuntimeException("deleteWallet failed: " + ex.getMessage(), ex);
        } finally {
            closeQuietly(rsCheck);
            closeQuietly(psCheck);
            closeQuietly(psDelete);
            closeQuietly(conn);
        }
    }

    /**
     * Transfer wallet ownership by updating {@code owner_id} and
     * {@code owner_type}.
     *
     * Requirements: 10.7
     *
     * @param walletId     primary key of the wallet
     * @param newOwnerId   new owner primary key
     * @param newOwnerType new owner type string
     */
    public void transferWalletOwnership(int walletId, int newOwnerId, String newOwnerType) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = MyConnection.getConnection();
            String sql = "UPDATE wallet SET owner_id = ?, owner_type = ? WHERE id = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, newOwnerId);
            ps.setString(2, newOwnerType);
            ps.setInt(3, walletId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("transferWalletOwnership failed: " + ex.getMessage(), ex);
        } finally {
            closeQuietly(ps);
            closeQuietly(conn);
        }
    }

    // =========================================================================
    // Sub-task 4.2 — Balance-mutation boundary
    //
    // This class intentionally does NOT contain any method that writes to
    // wallet.available_credits or wallet.retired_credits.
    // Balance mutations are exclusively handled by EventListenerService.
    // =========================================================================

    // =========================================================================
    // Internal helpers
    // =========================================================================

    /**
     * Map the current row of a {@link ResultSet} to a {@code Map<String,Object>}.
     * Column labels are used as keys; SQL NULL becomes Java {@code null}.
     */
    private Map<String, Object> resultSetRowToMap(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();
        Map<String, Object> row = new HashMap<>(cols * 2);
        for (int i = 1; i <= cols; i++) {
            row.put(meta.getColumnLabel(i), rs.getObject(i));
        }
        return row;
    }

    /** Close a {@link Connection} silently. */
    private static void closeQuietly(Connection c) {
        if (c != null) {
            try { c.close(); } catch (SQLException ignored) {}
        }
    }

    /** Close a {@link Statement} (or {@link PreparedStatement}) silently. */
    private static void closeQuietly(Statement s) {
        if (s != null) {
            try { s.close(); } catch (SQLException ignored) {}
        }
    }

    /** Close a {@link ResultSet} silently. */
    private static void closeQuietly(ResultSet rs) {
        if (rs != null) {
            try { rs.close(); } catch (SQLException ignored) {}
        }
    }
}
