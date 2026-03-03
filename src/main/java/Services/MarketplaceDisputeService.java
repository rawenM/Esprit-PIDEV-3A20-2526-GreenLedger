package Services;

import Models.MarketplaceDispute;
import DataBase.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing marketplace disputes and resolution
 * Handles dispute filing, admin review, and resolution workflows
 */
public class MarketplaceDisputeService {
    private static final String LOG_TAG = "[MarketplaceDisputeService]";
    private static MarketplaceDisputeService instance;
    private Connection conn;

    private MarketplaceDisputeService() {
        this.conn = MyConnection.getConnection();
    }

    public static MarketplaceDisputeService getInstance() {
        if (instance == null) {
            instance = new MarketplaceDisputeService();
        }
        return instance;
    }

    /**
     * File a new dispute
     */
    public int createDispute(Integer orderId, Integer tradeId, int escrowId, long reporterId, 
                            long reportedUserId, String disputeReason, String description) {
        if (conn == null) return -1;

        String sql = "INSERT INTO marketplace_disputes " +
            "(order_id, trade_id, escrow_id, reporter_id, reported_user_id, " +
            "dispute_reason, description, resolution_type) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDING')";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setObject(1, orderId);
            stmt.setObject(2, tradeId);
            stmt.setInt(3, escrowId);
            stmt.setLong(4, reporterId);
            stmt.setLong(5, reportedUserId);
            stmt.setString(6, disputeReason);
            stmt.setString(7, description);
            
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int disputeId = rs.getInt(1);
                    System.out.println(LOG_TAG + " Dispute created: ID " + disputeId);
                    
                    // Mark associated escrow as disputed
                    MarketplaceEscrowService.getInstance().markDisputed(escrowId);
                    
                    return disputeId;
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR creating dispute: " + e.getMessage());
        }

        return -1;
    }

    /**
     * Resolve dispute with admin decision
     */
    public boolean resolveDispute(int disputeId, String resolutionType, String adminNotes, long resolvedBy) {
        if (conn == null) return false;

        String sql = "UPDATE marketplace_disputes SET " +
            "resolution_type = ?, admin_notes = ?, resolved_by = ?, resolved_at = NOW() " +
            "WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, resolutionType);
            stmt.setString(2, adminNotes);
            stmt.setLong(3, resolvedBy);
            stmt.setInt(4, disputeId);
            
            int updated = stmt.executeUpdate();
            
            if (updated > 0) {
                System.out.println(LOG_TAG + " Dispute " + disputeId + " resolved: " + resolutionType);
                
                // Execute resolution action on escrow
                executeResolution(disputeId, resolutionType);
                
                return true;
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR resolving dispute: " + e.getMessage());
        }

        return false;
    }

    /**
     * Execute resolution action on escrow
     */
    private void executeResolution(int disputeId, String resolutionType) {
        MarketplaceDispute dispute = getDisputeById(disputeId);
        if (dispute == null) return;

        MarketplaceEscrowService escrowService = MarketplaceEscrowService.getInstance();
        
        switch (resolutionType) {
            case "REFUND_TO_BUYER":
                escrowService.refundToBuyer(dispute.getEscrowId());
                break;
            case "RELEASE_TO_SELLER":
                escrowService.releaseToSeller(dispute.getEscrowId());
                break;
            case "SPLIT_FUNDS":
                // TODO: Implement split funds logic (requires escrow update)
                System.out.println(LOG_TAG + " Split funds not yet implemented");
                break;
            default:
                System.out.println(LOG_TAG + " No escrow action for resolution: " + resolutionType);
        }
    }

    /**
     * Get dispute by ID
     */
    public MarketplaceDispute getDisputeById(int disputeId) {
        if (conn == null) return null;

        String sql = "SELECT * FROM marketplace_disputes WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, disputeId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultToDispute(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR fetching dispute: " + e.getMessage());
        }

        return null;
    }

    /**
     * Get disputes by order ID
     */
    public List<MarketplaceDispute> getDisputesByOrderId(int orderId) {
        List<MarketplaceDispute> disputes = new ArrayList<>();
        if (conn == null) return disputes;

        String sql = "SELECT * FROM marketplace_disputes WHERE order_id = ? ORDER BY created_at DESC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orderId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    disputes.add(mapResultToDispute(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR fetching disputes: " + e.getMessage());
        }

        return disputes;
    }

    /**
     * Get all pending disputes
     */
    public List<MarketplaceDispute> getPendingDisputes() {
        List<MarketplaceDispute> disputes = new ArrayList<>();
        if (conn == null) return disputes;

        String sql = "SELECT * FROM marketplace_disputes WHERE resolution_type = 'PENDING' " +
            "ORDER BY created_at DESC";

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                disputes.add(mapResultToDispute(rs));
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR fetching pending disputes: " + e.getMessage());
        }

        return disputes;
    }

    /**
     * Get disputes by reporter
     */
    public List<MarketplaceDispute> getDisputesByReporter(long reporterId) {
        List<MarketplaceDispute> disputes = new ArrayList<>();
        if (conn == null) return disputes;

        String sql = "SELECT * FROM marketplace_disputes WHERE reporter_id = ? ORDER BY created_at DESC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, reporterId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    disputes.add(mapResultToDispute(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR fetching reporter disputes: " + e.getMessage());
        }

        return disputes;
    }

    /**
     * Map ResultSet to MarketplaceDispute object
     */
    private MarketplaceDispute mapResultToDispute(ResultSet rs) throws SQLException {
        MarketplaceDispute dispute = new MarketplaceDispute();
        dispute.setId(rs.getInt("id"));
        dispute.setOrderId(rs.getObject("order_id", Integer.class));
        dispute.setTradeId(rs.getObject("trade_id", Integer.class));
        dispute.setEscrowId(rs.getInt("escrow_id"));
        dispute.setReporterId(rs.getLong("reporter_id"));
        dispute.setReportedUserId(rs.getLong("reported_user_id"));
        dispute.setDisputeReason(rs.getString("dispute_reason"));
        dispute.setDescription(rs.getString("description"));
        dispute.setResolutionType(rs.getString("resolution_type"));
        dispute.setAdminNotes(rs.getString("admin_notes"));
        
        Long resolvedBy = rs.getObject("resolved_by", Long.class);
        dispute.setResolvedBy(resolvedBy);
        
        Timestamp resolvedAt = rs.getTimestamp("resolved_at");
        dispute.setResolvedAt(resolvedAt);
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        dispute.setCreatedAt(createdAt);
        
        return dispute;
    }
}
