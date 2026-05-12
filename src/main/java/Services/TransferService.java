package Services;

import DataBase.MyConnection;
import Models.Wallet;
import Models.CarbonCreditBatch;
import Models.BatchEventType;
import com.google.gson.JsonObject;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for transferring carbon credits between wallets.
 * Ensures ACID properties: all-or-nothing credit movements.
 * 
 * Atomic Operation Flow:
 * 1. Validate both wallets exist
 * 2. Check source wallet has sufficient credits
 * 3. Lock transaction (prevent double-spend)
 * 4. Deduct from source
 * 5. Add to destination
 * 6. Record transaction
 * 7. Record batch events
 * 8. Commit or rollback on error
 */
public class TransferService {
    
    private Connection conn;
    private BatchEventService eventService;
    
    public TransferService() {
        this.conn = MyConnection.getConnection();
        this.eventService = new BatchEventService();
    }
    
    public TransferService(Connection conn, BatchEventService eventService) {
        this.conn = conn;
        this.eventService = eventService;
    }
    
    /**
     * Transfer credits atomically from one wallet to another.
     * 
     * @param fromWalletId Source wallet ID
     * @param toWalletId Destination wallet ID
     * @param amount Amount to transfer (must be > 0)
     * @param referenceNote Description of transfer
     * @return TransferResult with success status and details
     * @throws Exception if transfer fails for any reason
     */
    // DEPRECATED: Use BlockchainService.transferBatch() + EventListenerService.applyTransferEvent() instead
    public TransferResult transferCredits(int fromWalletId, int toWalletId, 
                                         double amount, String referenceNote) throws Exception {
        throw new UnsupportedOperationException("Use BlockchainService.transferBatch() + EventListenerService.applyTransferEvent() instead");
    }
    
    /**
     * Get wallet with FOR UPDATE lock (for atomic operations).
     */
    private Wallet getWalletWithLock(int walletId) throws SQLException {
        String sql = "SELECT id, wallet_number, holder_name, owner_type, owner_id, available_credits, retired_credits " +
                     "FROM green_wallets WHERE id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, walletId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Wallet w = new Wallet();
                    w.setId(rs.getInt("id"));
                    w.setWalletNumber(Integer.parseInt(rs.getString("wallet_number")));
                    w.setName(rs.getString("holder_name"));
                    w.setOwnerType(rs.getString("owner_type"));
                    w.setOwnerId(rs.getInt("owner_id"));
                    w.setAvailableCredits(rs.getDouble("available_credits"));
                    w.setRetiredCredits(rs.getDouble("retired_credits"));
                    return w;
                }
            }
        }
        return null;
    }
    
    /**
     * Record transfer transaction in audit trail.
     */
    private void recordTransfer(int fromWalletId, int toWalletId, double amount,
                               String referenceNote, String transferId) throws SQLException {
        String sql = "INSERT INTO wallet_transactions " +
                     "(wallet_id, batch_id, type, amount, reference_note, created_at) " +
                     "VALUES (?, NULL, ?, ?, ?, CURRENT_TIMESTAMP)";
        
        // Record TRANSFER_OUT for source
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, fromWalletId);
            ps.setString(2, "TRANSFER_OUT");
            ps.setDouble(3, amount);
            ps.setString(4, referenceNote + " [TXN:" + transferId + "]");
            ps.executeUpdate();
        }
        
        // Record TRANSFER_IN for destination
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, toWalletId);
            ps.setString(2, "TRANSFER_IN");
            ps.setDouble(3, amount);
            ps.setString(4, referenceNote + " [TXN:" + transferId + "]");
            ps.executeUpdate();
        }
    }
    
    /**
     * Generate unique transfer identifier.
     */
    private String createTransferId() {
        return "TXN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    // ==================== RESULT CLASS ====================
    
    /**
     * Result object for transfer operations.
     */
    public static class TransferResult {
        private boolean success;
        private String message;
        private String transferId;
        private double amount;
        private int fromWalletId;
        private int toWalletId;
        
        public TransferResult(boolean success, String message, String transferId,
                             double amount, int fromWalletId, int toWalletId) {
            this.success = success;
            this.message = message;
            this.transferId = transferId;
            this.amount = amount;
            this.fromWalletId = fromWalletId;
            this.toWalletId = toWalletId;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getTransferId() { return transferId; }
        public double getAmount() { return amount; }
        public int getFromWalletId() { return fromWalletId; }
        public int getToWalletId() { return toWalletId; }
    }
}
