package Services;

import DataBase.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the {@code blockchain_transactions}, {@code blockchain_sync_state}, and
 * {@code blockchain_event_log} tables.
 *
 * <p>Every on-chain operation has a tracked lifecycle record in
 * {@code blockchain_transactions}: PENDING → SUBMITTED → CONFIRMED (or FAILED).
 *
 * <p>All database access uses {@link MyConnection#getConnection()} (plain JDBC, no DI).
 * Connections, statements, and result-sets are always closed in {@code finally} blocks.
 *
 * Requirements: 6.1 – 6.10
 */
public class TransactionService {

    // -------------------------------------------------------------------------
    // Table-creation flags — avoid redundant DDL after the first successful run
    // -------------------------------------------------------------------------
    private static volatile boolean transactionsTableEnsured = false;
    private static volatile boolean syncStateTableEnsured    = false;
    private static volatile boolean eventLogTableEnsured     = false;

    // =========================================================================
    // Sub-task 2.1 — Table auto-creation
    // =========================================================================

    /**
     * CREATE TABLE IF NOT EXISTS {@code blockchain_transactions}.
     * Requirements: 6.1
     */
    private void ensureTransactionsTableExists() {
        if (transactionsTableEnsured) return;

        String ddl =
            "CREATE TABLE IF NOT EXISTS blockchain_transactions (" +
            "  id                   INT AUTO_INCREMENT PRIMARY KEY," +
            "  tx_hash              VARCHAR(100)," +
            "  type                 VARCHAR(50)," +
            "  status               VARCHAR(20) DEFAULT 'PENDING'," +
            "  wallet_id            INT," +
            "  batch_id             INT," +
            "  from_wallet_id       INT," +
            "  to_wallet_id         INT," +
            "  amount_base_units    DECIMAL(65,0)," +
            "  reason               TEXT," +
            "  request_payload_json TEXT," +
            "  error_message        TEXT," +
            "  block_number         INT," +
            "  log_index            INT," +
            "  created_at           DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "  updated_at           DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
            "  UNIQUE KEY uq_tx_log (tx_hash, log_index)" +
            ")";

        Connection conn = null;
        Statement st = null;
        try {
            conn = MyConnection.getConnection();
            st = conn.createStatement();
            st.execute(ddl);
            transactionsTableEnsured = true;
        } catch (SQLException e) {
            System.err.println("[TransactionService] Failed to create blockchain_transactions: " + e.getMessage());
        } finally {
            closeQuietly(st);
            closeQuietly(conn);
        }
    }

    /**
     * CREATE TABLE IF NOT EXISTS {@code blockchain_sync_state}.
     * Requirements: 6.2
     */
    private void ensureSyncStateTableExists() {
        if (syncStateTableEnsured) return;

        String ddl =
            "CREATE TABLE IF NOT EXISTS blockchain_sync_state (" +
            "  listener_name            VARCHAR(100) PRIMARY KEY," +
            "  last_processed_block     INT DEFAULT 0," +
            "  last_processed_tx_hash   VARCHAR(100)," +
            "  last_processed_log_index INT," +
            "  updated_at               DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
            ")";

        Connection conn = null;
        Statement st = null;
        try {
            conn = MyConnection.getConnection();
            st = conn.createStatement();
            st.execute(ddl);
            syncStateTableEnsured = true;
        } catch (SQLException e) {
            System.err.println("[TransactionService] Failed to create blockchain_sync_state: " + e.getMessage());
        } finally {
            closeQuietly(st);
            closeQuietly(conn);
        }
    }

    /**
     * CREATE TABLE IF NOT EXISTS {@code blockchain_event_log}.
     * Requirements: 6.3
     */
    private void ensureEventLogTableExists() {
        if (eventLogTableEnsured) return;

        String ddl =
            "CREATE TABLE IF NOT EXISTS blockchain_event_log (" +
            "  id           INT AUTO_INCREMENT PRIMARY KEY," +
            "  tx_hash      VARCHAR(100)," +
            "  log_index    INT," +
            "  event_name   VARCHAR(100)," +
            "  block_number INT," +
            "  processed_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "  UNIQUE KEY uq_event_log (tx_hash, log_index)" +
            ")";

        Connection conn = null;
        Statement st = null;
        try {
            conn = MyConnection.getConnection();
            st = conn.createStatement();
            st.execute(ddl);
            eventLogTableEnsured = true;
        } catch (SQLException e) {
            System.err.println("[TransactionService] Failed to create blockchain_event_log: " + e.getMessage());
        } finally {
            closeQuietly(st);
            closeQuietly(conn);
        }
    }

    /**
     * Ensure all three managed tables exist.
     * Call this once at application startup (or before the first DB operation).
     * Requirements: 6.1, 6.2, 6.3
     */
    public void ensureTablesExist() {
        ensureTransactionsTableExists();
        ensureSyncStateTableExists();
        ensureEventLogTableExists();
    }

    // =========================================================================
    // Sub-task 2.2 — createPendingTransaction
    // =========================================================================

    /**
     * Insert a new row into {@code blockchain_transactions} with {@code status = PENDING}
     * and return the full inserted row as a {@code Map}.
     *
     * @param type             transaction type (e.g. "MINT", "TRANSFER", "RETIRE")
     * @param walletId         nullable wallet identifier
     * @param batchId          nullable batch identifier
     * @param amountBaseUnits  on-chain amount as a decimal string (no floating-point)
     * @param requestPayload   arbitrary metadata; serialised to JSON for storage
     * @param fromWalletId     nullable source wallet (for transfers)
     * @param toWalletId       nullable destination wallet (for transfers)
     * @param reason           human-readable reason string
     * @return the newly inserted row as a {@code Map<String,Object>}, or {@code null} on failure
     * Requirements: 6.4
     */
    public Map<String, Object> createPendingTransaction(
            String type,
            Integer walletId,
            Integer batchId,
            String amountBaseUnits,
            Map<String, Object> requestPayload,
            Integer fromWalletId,
            Integer toWalletId,
            String reason) {

        ensureTablesExist();

        String sql =
            "INSERT INTO blockchain_transactions " +
            "  (type, status, wallet_id, batch_id, amount_base_units, " +
            "   request_payload_json, from_wallet_id, to_wallet_id, reason) " +
            "VALUES (?, 'PENDING', ?, ?, ?, ?, ?, ?, ?)";

        String payloadJson = mapToJson(requestPayload);

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet generatedKeys = null;
        try {
            conn = MyConnection.getConnection();
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, type);
            setNullableInt(ps, 2, walletId);
            setNullableInt(ps, 3, batchId);
            ps.setString(4, amountBaseUnits);
            ps.setString(5, payloadJson);
            setNullableInt(ps, 6, fromWalletId);
            setNullableInt(ps, 7, toWalletId);
            ps.setString(8, reason);

            int affected = ps.executeUpdate();
            if (affected == 0) {
                return null;
            }

            generatedKeys = ps.getGeneratedKeys();
            if (generatedKeys.next()) {
                int newId = generatedKeys.getInt(1);
                return findById(newId);
            }
            return null;

        } catch (SQLException e) {
            System.err.println("[TransactionService] createPendingTransaction failed: " + e.getMessage());
            return null;
        } finally {
            closeQuietly(generatedKeys);
            closeQuietly(ps);
            closeQuietly(conn);
        }
    }

    // =========================================================================
    // Sub-task 2.3 — Status-transition methods
    // =========================================================================

    /**
     * Transition a transaction from PENDING to SUBMITTED.
     *
     * @param transactionId row id
     * @param txHash        on-chain transaction hash
     * @param blockNumber   nullable block number
     * @param logIndex      nullable log index
     * @throws RuntimeException if no PENDING row with the given id exists
     * Requirements: 6.5
     */
    public void markSubmitted(int transactionId, String txHash, Integer blockNumber, Integer logIndex) {
        ensureTablesExist();

        String sql =
            "UPDATE blockchain_transactions " +
            "SET status = 'SUBMITTED', tx_hash = ?, block_number = ?, log_index = ?, " +
            "    updated_at = NOW() " +
            "WHERE id = ? AND status = 'PENDING'";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = MyConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, txHash);
            setNullableInt(ps, 2, blockNumber);
            setNullableInt(ps, 3, logIndex);
            ps.setInt(4, transactionId);

            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new RuntimeException(
                    "[TransactionService] markSubmitted: no PENDING transaction found with id=" + transactionId);
            }
        } catch (SQLException e) {
            throw new RuntimeException("[TransactionService] markSubmitted failed: " + e.getMessage(), e);
        } finally {
            closeQuietly(ps);
            closeQuietly(conn);
        }
    }

    /**
     * Transition a transaction from PENDING to FAILED, or update the error message
     * if the row is already FAILED.
     *
     * @param transactionId row id
     * @param error         error description
     * Requirements: 6.6
     */
    public void markFailed(int transactionId, String error) {
        ensureTablesExist();

        // First attempt: PENDING → FAILED
        String sqlPending =
            "UPDATE blockchain_transactions " +
            "SET status = 'FAILED', error_message = ?, updated_at = NOW() " +
            "WHERE id = ? AND status = 'PENDING'";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = MyConnection.getConnection();
            ps = conn.prepareStatement(sqlPending);
            ps.setString(1, error);
            ps.setInt(2, transactionId);
            int updated = ps.executeUpdate();

            if (updated == 0) {
                // Row may already be FAILED — update error_message only
                closeQuietly(ps);
                String sqlFailed =
                    "UPDATE blockchain_transactions " +
                    "SET error_message = ?, updated_at = NOW() " +
                    "WHERE id = ? AND status = 'FAILED'";
                ps = conn.prepareStatement(sqlFailed);
                ps.setString(1, error);
                ps.setInt(2, transactionId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("[TransactionService] markFailed failed: " + e.getMessage());
        } finally {
            closeQuietly(ps);
            closeQuietly(conn);
        }
    }

    /**
     * Transition a transaction from SUBMITTED to CONFIRMED by tx hash.
     * No-op if the row is already CONFIRMED.
     *
     * @param txHash      on-chain transaction hash
     * @param blockNumber block number at confirmation
     * @param logIndex    log index at confirmation
     * Requirements: 6.7
     */
    public void markConfirmedByTxHash(String txHash, int blockNumber, int logIndex) {
        ensureTablesExist();

        String sql =
            "UPDATE blockchain_transactions " +
            "SET status = 'CONFIRMED', block_number = ?, log_index = ?, updated_at = NOW() " +
            "WHERE tx_hash = ? AND status = 'SUBMITTED'";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = MyConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, blockNumber);
            ps.setInt(2, logIndex);
            ps.setString(3, txHash);
            ps.executeUpdate();
            // 0 rows updated is a no-op (already CONFIRMED or not found) — intentional
        } catch (SQLException e) {
            System.err.println("[TransactionService] markConfirmedByTxHash failed: " + e.getMessage());
        } finally {
            closeQuietly(ps);
            closeQuietly(conn);
        }
    }

    // =========================================================================
    // Sub-task 2.4 — Query and sync-state methods
    // =========================================================================

    /**
     * Find a transaction row by primary key.
     *
     * @param id row id
     * @return row as {@code Map<String,Object>}, or {@code null} if not found
     * Requirements: 6.8 (supports query operations)
     */
    public Map<String, Object> findById(int id) {
        ensureTablesExist();

        String sql = "SELECT * FROM blockchain_transactions WHERE id = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = MyConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            rs = ps.executeQuery();
            if (rs.next()) {
                return resultSetRowToMap(rs);
            }
            return null;
        } catch (SQLException e) {
            System.err.println("[TransactionService] findById failed: " + e.getMessage());
            return null;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
    }

    /**
     * Find a transaction row by on-chain tx hash.
     *
     * @param txHash on-chain transaction hash
     * @return row as {@code Map<String,Object>}, or {@code null} if not found
     * Requirements: 6.8
     */
    public Map<String, Object> findByTxHash(String txHash) {
        ensureTablesExist();

        String sql = "SELECT * FROM blockchain_transactions WHERE tx_hash = ? LIMIT 1";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = MyConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, txHash);
            rs = ps.executeQuery();
            if (rs.next()) {
                return resultSetRowToMap(rs);
            }
            return null;
        } catch (SQLException e) {
            System.err.println("[TransactionService] findByTxHash failed: " + e.getMessage());
            return null;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
    }

    /**
     * Insert a new event into {@code blockchain_event_log} using INSERT IGNORE so that
     * duplicate (txHash, logIndex) pairs are silently discarded.
     *
     * @param txHash      on-chain transaction hash
     * @param logIndex    log index within the transaction
     * @param eventName   name of the blockchain event
     * @param blockNumber block number where the event was emitted
     * @return {@code true} if a new row was inserted; {@code false} if it was a duplicate
     * Requirements: 6.8
     */
    public boolean recordEventIfNew(String txHash, int logIndex, String eventName, int blockNumber) {
        ensureTablesExist();

        String sql =
            "INSERT IGNORE INTO blockchain_event_log (tx_hash, log_index, event_name, block_number) " +
            "VALUES (?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = MyConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, txHash);
            ps.setInt(2, logIndex);
            ps.setString(3, eventName);
            ps.setInt(4, blockNumber);
            int affected = ps.executeUpdate();
            return affected == 1;
        } catch (SQLException e) {
            System.err.println("[TransactionService] recordEventIfNew failed: " + e.getMessage());
            return false;
        } finally {
            closeQuietly(ps);
            closeQuietly(conn);
        }
    }

    /**
     * Fetch the sync-state row for the given listener, inserting a default row if none exists.
     *
     * @param listenerName unique name identifying the listener
     * @return sync-state row as {@code Map<String,Object>}
     * Requirements: 6.9
     */
    public Map<String, Object> getSyncState(String listenerName) {
        ensureTablesExist();

        // Try to fetch existing row first
        String selectSql = "SELECT * FROM blockchain_sync_state WHERE listener_name = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = MyConnection.getConnection();
            ps = conn.prepareStatement(selectSql);
            ps.setString(1, listenerName);
            rs = ps.executeQuery();
            if (rs.next()) {
                return resultSetRowToMap(rs);
            }

            // No row yet — insert default and re-fetch
            closeQuietly(rs);
            closeQuietly(ps);

            String insertSql =
                "INSERT IGNORE INTO blockchain_sync_state (listener_name, last_processed_block) " +
                "VALUES (?, 0)";
            ps = conn.prepareStatement(insertSql);
            ps.setString(1, listenerName);
            ps.executeUpdate();
            closeQuietly(ps);

            ps = conn.prepareStatement(selectSql);
            ps.setString(1, listenerName);
            rs = ps.executeQuery();
            if (rs.next()) {
                return resultSetRowToMap(rs);
            }
            return null;

        } catch (SQLException e) {
            System.err.println("[TransactionService] getSyncState failed: " + e.getMessage());
            return null;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
    }

    /**
     * Upsert the sync-state row for the given listener.
     *
     * @param listenerName   unique name identifying the listener
     * @param lastBlock      last processed block number
     * @param lastTxHash     last processed transaction hash (nullable)
     * @param lastLogIndex   last processed log index (nullable)
     * Requirements: 6.9
     */
    public void saveSyncState(String listenerName, int lastBlock, String lastTxHash, Integer lastLogIndex) {
        ensureTablesExist();

        String sql =
            "INSERT INTO blockchain_sync_state " +
            "  (listener_name, last_processed_block, last_processed_tx_hash, last_processed_log_index) " +
            "VALUES (?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE " +
            "  last_processed_block     = VALUES(last_processed_block), " +
            "  last_processed_tx_hash   = VALUES(last_processed_tx_hash), " +
            "  last_processed_log_index = VALUES(last_processed_log_index)";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = MyConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, listenerName);
            ps.setInt(2, lastBlock);
            ps.setString(3, lastTxHash);
            setNullableInt(ps, 4, lastLogIndex);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[TransactionService] saveSyncState failed: " + e.getMessage());
        } finally {
            closeQuietly(ps);
            closeQuietly(conn);
        }
    }

    /**
     * Return all SUBMITTED transactions whose {@code updated_at} is at least
     * {@code olderThanMinutes} minutes in the past.
     *
     * @param olderThanMinutes age threshold in minutes
     * @return list of matching rows as {@code Map<String,Object>}
     * Requirements: 6.10
     */
    public List<Map<String, Object>> findStaleSubmittedTransactions(int olderThanMinutes) {
        ensureTablesExist();

        String sql =
            "SELECT * FROM blockchain_transactions " +
            "WHERE status = 'SUBMITTED' " +
            "  AND TIMESTAMPDIFF(MINUTE, updated_at, NOW()) >= ?";

        List<Map<String, Object>> results = new ArrayList<>();

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = MyConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, olderThanMinutes);
            rs = ps.executeQuery();
            while (rs.next()) {
                results.add(resultSetRowToMap(rs));
            }
        } catch (SQLException e) {
            System.err.println("[TransactionService] findStaleSubmittedTransactions failed: " + e.getMessage());
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return results;
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    /**
     * Map the current row of a {@link ResultSet} to a {@code Map<String,Object>}.
     * Column names are used as keys; SQL NULL becomes Java {@code null}.
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

    /**
     * Set an {@code INT} parameter that may be {@code null}.
     */
    private void setNullableInt(PreparedStatement ps, int paramIndex, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(paramIndex, Types.INTEGER);
        } else {
            ps.setInt(paramIndex, value);
        }
    }

    /**
     * Serialize a {@code Map<String,Object>} to a minimal JSON string without
     * any external library dependency.
     *
     * <p>Supports {@code String}, {@code Number}, {@code Boolean}, and {@code null}
     * values. Nested maps and lists are serialised recursively.
     */
    @SuppressWarnings("unchecked")
    static String mapToJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(escapeJson(entry.getKey())).append('"').append(':');
            sb.append(valueToJson(entry.getValue()));
        }
        sb.append('}');
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static String valueToJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Number) {
            return value.toString();
        }
        if (value instanceof Map) {
            return mapToJson((Map<String, Object>) value);
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : list) {
                if (!first) sb.append(',');
                first = false;
                sb.append(valueToJson(item));
            }
            sb.append(']');
            return sb.toString();
        }
        // Default: treat as string
        return '"' + escapeJson(value.toString()) + '"';
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
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
