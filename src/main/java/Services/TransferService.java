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
    public TransferResult transferCredits(int fromWalletId, int toWalletId, 
                                         double amount, String referenceNote) throws Exception {
        
        if (amount <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than 0");
        }
        
        if (fromWalletId == toWalletId) {
            throw new IllegalArgumentException("Cannot transfer to same wallet");
        }
        
        try {
            // Start atomic transaction
            conn.setAutoCommit(false);
            
            // 1. Lock both wallets in consistent order (prevent deadlock)
            String lockSql = "SELECT id FROM green_wallets WHERE id = ? FOR UPDATE";
            try (PreparedStatement ps = conn.prepareStatement(lockSql)) {
                ps.setInt(1, Math.min(fromWalletId, toWalletId));
                ps.executeQuery();
                ps.setInt(1, Math.max(fromWalletId, toWalletId));
                ps.executeQuery();
            }
            
            // 2. Validate source wallet exists and has credits
            Wallet sourceWallet = getWalletWithLock(fromWalletId);
            if (sourceWallet == null) {
                conn.rollback();
                throw new Exception("Source wallet not found: " + fromWalletId);
            }
            
            if (sourceWallet.getAvailableCredits() < amount) {
                conn.rollback();
                throw new Exception(
                    String.format("Insufficient credits. Available: %.2f, Required: %.2f",
                    sourceWallet.getAvailableCredits(), amount)
                );
            }
            
            // 3. Validate destination wallet exists
            Wallet destWallet = getWalletWithLock(toWalletId);
            if (destWallet == null) {
                conn.rollback();
                throw new Exception("Destination wallet not found: " + toWalletId);
            }
            
            // 4. Deduct from source
            String deductSql = "UPDATE green_wallets SET available_credits = available_credits - ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(deductSql)) {
                ps.setDouble(1, amount);
                ps.setInt(2, fromWalletId);
                int updateCount = ps.executeUpdate();
                if (updateCount == 0) {
                    conn.rollback();
                    throw new Exception("Failed to deduct from source wallet");
                }
            }
            
            // 5. Add to destination
            String addSql = "UPDATE green_wallets SET available_credits = available_credits + ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(addSql)) {
                ps.setDouble(1, amount);
                ps.setInt(2, toWalletId);
                int updateCount = ps.executeUpdate();
                if (updateCount == 0) {
                    conn.rollback();
                    throw new Exception("Failed to add to destination wallet");
                }
            }
            
            // 6. Record transaction
            String transferId = createTransferId();
            recordTransfer(fromWalletId, toWalletId, amount, referenceNote, transferId);
            
            // 7. Record batch events for traceability
            if (eventService != null) {
                JsonObject eventData = new JsonObject();
                eventData.addProperty("from_wallet_id", fromWalletId);
                eventData.addProperty("to_wallet_id", toWalletId);
                eventData.addProperty("amount", amount);
                eventData.addProperty("transfer_id", transferId);
                eventData.addProperty("reference", referenceNote);
            }
            
            // Commit successful transaction
            conn.commit();
            conn.setAutoCommit(true);
            
            return new TransferResult(
                true,
                "Transfer successful",
                transferId,
                amount,
                fromWalletId,
                toWalletId
            );
            
        } catch (Exception ex) {
            try {
                conn.rollback();
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("Error during rollback: " + e.getMessage());
            }
            throw ex;
        }
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
