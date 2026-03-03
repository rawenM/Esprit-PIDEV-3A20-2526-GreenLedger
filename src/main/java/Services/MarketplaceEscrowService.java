package Services;

import Models.MarketplaceEscrow;
import DataBase.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing marketplace escrow operations
 * Handles 24-hour buyer protection holds and fund releases
 */
public class MarketplaceEscrowService {
    private static final String LOG_TAG = "[MarketplaceEscrowService]";
    private static MarketplaceEscrowService instance;
    private Connection conn;
    
    // 24-hour escrow hold period
    private static final int ESCROW_HOLD_HOURS = 24;

    private MarketplaceEscrowService() {
        this.conn = MyConnection.getConnection();
    }

    public static MarketplaceEscrowService getInstance() {
        if (instance == null) {
            instance = new MarketplaceEscrowService();
        }
        return instance;
    }

    /**
     * Create escrow hold for an order
     */
    public int createEscrow(Integer orderId, Integer tradeId, long buyerId, long sellerId, 
                           double amountUsd, String stripeHoldId, String holdReason) {
        if (conn == null) return -1;

        String sql = "INSERT INTO marketplace_escrow " +
            "(order_id, trade_id, buyer_id, seller_id, amount_usd, stripe_hold_id, " +
            "status, hold_reason) VALUES (?, ?, ?, ?, ?, ?, 'HELD', ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setObject(1, orderId);
            stmt.setObject(2, tradeId);
            stmt.setLong(3, buyerId);
            stmt.setLong(4, sellerId);
            stmt.setDouble(5, amountUsd);
            stmt.setString(6, stripeHoldId);
            stmt.setString(7, holdReason);
            
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int escrowId = rs.getInt(1);
                    System.out.println(LOG_TAG + " Escrow created: ID " + escrowId + 
                        " for $" + amountUsd);
                    return escrowId;
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR creating escrow: " + e.getMessage());
        }

        return -1;
    }

    /**
     * Release escrow to seller
     */
    public boolean releaseToSeller(int escrowId) {
        if (conn == null) return false;

        String sql = "UPDATE marketplace_escrow SET status = 'RELEASED_TO_SELLER', " +
            "release_date = NOW() WHERE id = ? AND status = 'HELD'";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, escrowId);
            int updated = stmt.executeUpdate();
            
            if (updated > 0) {
                System.out.println(LOG_TAG + " Escrow " + escrowId + " released to seller");
                return true;
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR releasing escrow: " + e.getMessage());
        }

        return false;
    }

    /**
     * Refund escrow to buyer
     */
    public boolean refundToBuyer(int escrowId) {
        if (conn == null) return false;

        String sql = "UPDATE marketplace_escrow SET status = 'REFUNDED_TO_BUYER', " +
            "release_date = NOW() WHERE id = ? AND status IN ('HELD', 'DISPUTED')";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, escrowId);
            int updated = stmt.executeUpdate();
            
            if (updated > 0) {
                System.out.println(LOG_TAG + " Escrow " + escrowId + " refunded to buyer");
                return true;
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR refunding escrow: " + e.getMessage());
        }

        return false;
    }

    /**
     * Mark escrow as disputed
     */
    public boolean markDisputed(int escrowId) {
        if (conn == null) return false;

        String sql = "UPDATE marketplace_escrow SET status = 'DISPUTED' WHERE id = ? AND status = 'HELD'";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, escrowId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR marking escrow as disputed: " + e.getMessage());
        }

        return false;
    }

    /**
     * Get escrow by ID
     */
    public MarketplaceEscrow getEscrowById(int escrowId) {
        if (conn == null) return null;

        String sql = "SELECT * FROM marketplace_escrow WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, escrowId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultToEscrow(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR fetching escrow: " + e.getMessage());
        }

        return null;
    }

    /**
     * Get escrow by order ID
     */
    public MarketplaceEscrow getEscrowByOrderId(int orderId) {
        if (conn == null) return null;

        String sql = "SELECT * FROM marketplace_escrow WHERE order_id = ? ORDER BY created_at DESC LIMIT 1";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orderId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultToEscrow(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR fetching escrow by order: " + e.getMessage());
        }

        return null;
    }

    /**
     * Get all expired escrows (held > 24 hours)
     */
    public List<MarketplaceEscrow> getExpiredEscrows() {
        List<MarketplaceEscrow> escrows = new ArrayList<>();
        if (conn == null) return escrows;

        String sql = "SELECT * FROM marketplace_escrow " +
            "WHERE status = 'HELD' AND created_at < DATE_SUB(NOW(), INTERVAL ? HOUR)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, ESCROW_HOLD_HOURS);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    escrows.add(mapResultToEscrow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR fetching expired escrows: " + e.getMessage());
        }

        return escrows;
    }

    /**
     * Auto-release expired escrows to sellers
     */
    public int autoReleaseExpiredEscrows() {
        List<MarketplaceEscrow> expired = getExpiredEscrows();
        int released = 0;

        for (MarketplaceEscrow escrow : expired) {
            if (releaseToSeller(escrow.getId())) {
                released++;
            }
        }

        if (released > 0) {
            System.out.println(LOG_TAG + " Auto-released " + released + " expired escrows");
        }

        return released;
    }

    /**
     * Map ResultSet to MarketplaceEscrow object
     */
    private MarketplaceEscrow mapResultToEscrow(ResultSet rs) throws SQLException {
        MarketplaceEscrow escrow = new MarketplaceEscrow();
        escrow.setId(rs.getInt("id"));
        escrow.setOrderId(rs.getObject("order_id", Integer.class));
        escrow.setTradeId(rs.getObject("trade_id", Integer.class));
        escrow.setBuyerId((int) rs.getLong("buyer_id"));
        escrow.setSellerId((int) rs.getLong("seller_id"));
        escrow.setAmountUsd(rs.getDouble("amount_usd"));
        escrow.setHeldByPlatform(rs.getBoolean("held_by_platform"));
        escrow.setStripeHoldId(rs.getString("stripe_hold_id"));
        escrow.setStatus(rs.getString("status"));
        escrow.setHoldReason(rs.getString("hold_reason"));
        
        Timestamp releaseDate = rs.getTimestamp("release_date");
        escrow.setReleaseDate(releaseDate);
        
        Timestamp createdTs = rs.getTimestamp("created_at");
        escrow.setCreatedAt(createdTs);
        
        Timestamp updatedTs = rs.getTimestamp("updated_at");
        escrow.setUpdatedAt(updatedTs);
        
        return escrow;
    }
}
