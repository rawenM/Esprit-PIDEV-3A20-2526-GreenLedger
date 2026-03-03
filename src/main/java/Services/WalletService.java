package Services;

import DataBase.MyConnection;
import Models.Wallet;
import Models.CarbonCreditBatch;
import Models.OperationWallet;
import Models.BatchEventType;
import Services.BatchEventService;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonSerializer;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service layer for Green Wallet operations including CRUD and credit management.
 */
public class WalletService {

    private Connection conn;
    private static final Gson gson = createGsonWithLocalDateTime();
    
    // Warning flags - only show schema migration warnings once per session
    private static boolean lineageViewWarningShown = false;
    private static boolean parentBatchIdWarningShown = false;
    
    /**
     * Create Gson instance with LocalDateTime support (Java 17+ module compatibility).
     */
    private static Gson createGsonWithLocalDateTime() {
        return new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) -> 
                context.serialize(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .create();
    }

    public WalletService() {
        try {
            this.conn = MyConnection.getConnection();
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible d'ouvrir la connexion DB", e);
        }
    }

    // ==================== CRUD OPERATIONS ====================

    /**
     * Create a new wallet.
     */
    public int createWallet(Wallet wallet) {
        String sql = "INSERT INTO wallet (wallet_number, name, owner_type, owner_id, available_credits, retired_credits) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            // Generate unique wallet number if not provided
            if (wallet.getWalletNumber() == null) {
                wallet.setWalletNumber(generateUniqueWalletNumber());
            }
            
<<<<<<< HEAD
            ps.setString(1, String.valueOf(wallet.getWalletNumber()));
=======
<<<<<<< HEAD
            ps.setInt(1, wallet.getWalletNumber());
=======
            ps.setString(1, String.valueOf(wallet.getWalletNumber()));
>>>>>>> f3559248f463304c68513eb2c92f99791d2c4657
>>>>>>> yassine_antar
            ps.setString(2, wallet.getName());
            ps.setString(3, wallet.getOwnerType());
            ps.setInt(4, wallet.getOwnerId());
            ps.setDouble(5, wallet.getAvailableCredits());
            ps.setDouble(6, wallet.getRetiredCredits());
            
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
<<<<<<< HEAD
            if (isUnknownColumnError(ex, "name")) {
                return createWalletWithoutName(wallet);
            }
=======
<<<<<<< HEAD
=======
            if (isUnknownColumnError(ex, "name")) {
                return createWalletWithoutName(wallet);
            }
>>>>>>> f3559248f463304c68513eb2c92f99791d2c4657
>>>>>>> yassine_antar
            System.out.println("Error creating wallet: " + ex.getMessage());
        }
        return -1;
    }

<<<<<<< HEAD
=======
<<<<<<< HEAD
=======
>>>>>>> yassine_antar
    private int createWalletWithoutName(Wallet wallet) {
        String sql = "INSERT INTO wallet (wallet_number, owner_type, owner_id, available_credits, retired_credits) " +
                     "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (wallet.getWalletNumber() == null) {
                wallet.setWalletNumber(generateUniqueWalletNumber());
            }

            ps.setString(1, String.valueOf(wallet.getWalletNumber()));
            ps.setString(2, wallet.getOwnerType());
            ps.setInt(3, wallet.getOwnerId());
            ps.setDouble(4, wallet.getAvailableCredits());
            ps.setDouble(5, wallet.getRetiredCredits());

            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException nestedEx) {
            System.out.println("Error creating wallet (legacy schema): " + nestedEx.getMessage());
        }
        return -1;
    }

<<<<<<< HEAD
=======
>>>>>>> f3559248f463304c68513eb2c92f99791d2c4657
>>>>>>> yassine_antar
    /**
     * Read all wallets.
     */
    public List<Wallet> getAllWallets() {
        List<Wallet> wallets = new ArrayList<>();
        String sql = "SELECT * FROM wallet ORDER BY created_at DESC";
        
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                wallets.add(mapResultSetToWallet(rs));
            }
        } catch (SQLException ex) {
            System.out.println("Error fetching wallets: " + ex.getMessage());
        }
        return wallets;
    }

    /**
     * Read a single wallet by ID.
        /**
         * Get all wallets owned by a specific user.
         */
        public List<Wallet> getWalletsByOwnerId(int ownerId) {
            List<Wallet> wallets = new ArrayList<>();
            String sql = "SELECT * FROM wallet WHERE owner_id = ? ORDER BY created_at DESC";
        
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, ownerId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    wallets.add(mapResultSetToWallet(rs));
                }
            } catch (SQLException ex) {
                System.out.println("Error fetching wallets by owner: " + ex.getMessage());
            }
            return wallets;
        }

        /**
         * Read a single wallet by ID.
     */
    public Wallet getWalletById(int id) {
        String sql = "SELECT * FROM wallet WHERE id = ?";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToWallet(rs);
            }
        } catch (SQLException ex) {
            System.out.println("Error fetching wallet: " + ex.getMessage());
        }
        return null;
    }

    /**
     * Read wallet by wallet number.
     */
    public Wallet getWalletByNumber(String walletNumber) {
        String sql = "SELECT * FROM wallet WHERE wallet_number = ?";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, walletNumber);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToWallet(rs);
            }
        } catch (SQLException ex) {
            System.out.println("Error fetching wallet: " + ex.getMessage());
        }
        return null;
    }

    /**
     * Update wallet information.
     */
    public boolean updateWallet(Wallet wallet) {
        String sql = "UPDATE wallet SET name = ?, owner_type = ? WHERE id = ?";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, wallet.getName());
            ps.setString(2, wallet.getOwnerType());
            ps.setInt(3, wallet.getId());
            
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
<<<<<<< HEAD
            if (isUnknownColumnError(ex, "name")) {
                return updateWalletWithoutName(wallet);
            }
=======
<<<<<<< HEAD
=======
            if (isUnknownColumnError(ex, "name")) {
                return updateWalletWithoutName(wallet);
            }
>>>>>>> f3559248f463304c68513eb2c92f99791d2c4657
>>>>>>> yassine_antar
            System.out.println("Error updating wallet: " + ex.getMessage());
        }
        return false;
    }

<<<<<<< HEAD
=======
<<<<<<< HEAD
=======
>>>>>>> yassine_antar
    private boolean updateWalletWithoutName(Wallet wallet) {
        String sql = "UPDATE wallet SET owner_type = ? WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, wallet.getOwnerType());
            ps.setInt(2, wallet.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.out.println("Error updating wallet (legacy schema): " + ex.getMessage());
        }
        return false;
    }

<<<<<<< HEAD
=======
>>>>>>> f3559248f463304c68513eb2c92f99791d2c4657
>>>>>>> yassine_antar
    /**
     * Delete wallet (only if zero credits).
     */
    public boolean deleteWallet(int walletId) {
        Wallet wallet = getWalletById(walletId);
        if (wallet == null) {
            System.out.println("Wallet not found");
            return false;
        }
        
        // Check if wallet has zero credits
        if (wallet.getTotalCredits() > 0) {
            System.out.println("Cannot delete wallet with existing credits");
            return false;
        }
        
        String sql = "DELETE FROM wallet WHERE id = ?";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, walletId);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.out.println("Error deleting wallet: " + ex.getMessage());
        }
        return false;
    }

    // ==================== CREDIT OPERATIONS ====================

    /**
     * Issue carbon credits to a wallet from a verified project.
     * Creates a new credit batch and records the transaction.
     */
    /**
     * Issue credits to a wallet from a project (legacy method without audit linkage).
     */
    public boolean issueCredits(int walletId, int projectId, double amount, String referenceNote) {
        return issueCredits(walletId, projectId, amount, referenceNote, null, "SYSTEM");
    }

    /**
     * Issue credits to a wallet from a project with full traceability.
     * 
     * @param walletId Destination wallet
     * @param projectId Source project
     * @param amount Amount of credits to issue
     * @param referenceNote Description of issuance
     * @param calculationAuditId Optional link to emission calculation audit
     * @param actor User or system performing the issuance
     * @return true if successful, false otherwise
     */
    public boolean issueCredits(int walletId, int projectId, double amount, String referenceNote, 
                               String calculationAuditId, String actor) {
        if (amount <= 0) {
            System.out.println("Amount must be positive");
            return false;
        }

        try {
            conn.setAutoCommit(false);
            BatchEventService eventService = new BatchEventService();
            
            // 1. Create credit batch with traceability
            int batchId = createCreditBatch(projectId, walletId, amount, calculationAuditId, Models.BatchType.PRIMARY);
            if (batchId == -1) {
                conn.rollback();
                return false;
            }
            
            // 2. Update wallet available credits
            String updateWallet = "UPDATE wallet SET available_credits = available_credits + ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateWallet)) {
                ps.setDouble(1, amount);
                ps.setInt(2, walletId);
                ps.executeUpdate();
            }
            
            // 3. Record transaction
            recordTransaction(walletId, batchId, "ISSUE", amount, referenceNote);
            
            // 4. Record ISSUED event for traceability
            JsonObject eventData = new JsonObject();
            eventData.addProperty("total_amount", amount);
            eventData.addProperty("wallet_id", walletId);
            eventData.addProperty("project_id", projectId);
            eventData.addProperty("batch_type", "PRIMARY");
            if (calculationAuditId != null) {
                eventData.addProperty("calculation_audit_id", calculationAuditId);
            }
            eventService.recordEvent(batchId, BatchEventType.ISSUED, eventData, actor);
            
            conn.commit();
            return true;
            
        } catch (SQLException ex) {
            try {
                conn.rollback();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            System.out.println("Error issuing credits: " + ex.getMessage());
            return false;
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Quick issue credits without project (for testing/demo purposes).
     */
    public boolean quickIssueCredits(int walletId, double amount, String description) {
        return quickIssueCredits(walletId, amount, description, null, null, null);
    }
    
    public boolean quickIssueCredits(int walletId, double amount, String description,
                                    String calculationAuditId, String verificationStandard, Integer vintageYear) {
        try {
            conn.setAutoCommit(false);
            
            // Get wallet to determine project ID
            Wallet wallet = getWalletById(walletId);
            if (wallet == null) {
                conn.rollback();
                return false;
            }
            
            // Create batch for traceability (using wallet's owner_id as project substitute)
            int projectId = wallet.getOwnerId() > 0 ? wallet.getOwnerId() : 1;
            String auditId = calculationAuditId != null ? calculationAuditId : "QUICK_ISSUE";
            int batchId = createCreditBatch(projectId, walletId, amount, 
                auditId, Models.BatchType.PRIMARY, verificationStandard, vintageYear);
            
            if (batchId <= 0) {
                conn.rollback();
                return false;
            }
            
            // Update wallet credits
            String sql = "UPDATE wallet SET available_credits = available_credits + ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setDouble(1, amount);
                ps.setInt(2, walletId);
                ps.executeUpdate();
            }
            
            // Record transaction with batch linkage
            recordTransaction(walletId, batchId, "ISSUE", amount, description);
            
            // Record batch event (optional - don't fail transaction if event recording fails)
            try {
                BatchEventService eventService = new BatchEventService();
                com.google.gson.JsonObject eventData = new com.google.gson.JsonObject();
                eventData.addProperty("amount", amount);
                eventData.addProperty("wallet_id", walletId);
                eventData.addProperty("description", description);
                if (verificationStandard != null) {
                    eventData.addProperty("verification_standard", verificationStandard);
                }
                if (vintageYear != null) {
                    eventData.addProperty("vintage_year", vintageYear);
                }
                eventService.recordEvent(batchId, BatchEventType.ISSUED, eventData, "SYSTEM");
            } catch (Exception eventEx) {
                // Event recording failed, but batch creation succeeded - log and continue
                System.err.println("Warning: Batch created but event recording failed: " + eventEx.getMessage());
                System.err.println("Batch ID " + batchId + " issued successfully without event tracking");
            }
            
            conn.commit();
            return true;
        } catch (SQLException ex) {
            try {
                conn.rollback();
            } catch (SQLException e) {
                System.err.println("Error rolling back: " + e.getMessage());
            }
            System.out.println("Error quick issuing credits: " + ex.getMessage());
            return false;
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("Error resetting autocommit: " + e.getMessage());
            }
        }
    }

    /**
     * Retire carbon credits (permanently used for offsetting).
     */
    /**
     * Retire credits from a wallet (legacy method).
     */
    public boolean retireCredits(int walletId, double amount, String referenceNote) {
        return retireCredits(walletId, amount, referenceNote, "SYSTEM");
    }

    /**
     * Retire credits from a wallet with full batch traceability.
     * Tracks which specific batches were consumed during retirement.
     * 
     * @param walletId Source wallet
     * @param amount Amount to retire
     * @param referenceNote Description of retirement
     * @param actor User or system performing retirement
     * @return true if successful, false otherwise
     */
    public boolean retireCredits(int walletId, double amount, String referenceNote, String actor) {
        if (amount <= 0) {
            System.out.println("Amount must be positive");
            return false;
        }

        Wallet wallet = getWalletById(walletId);
        if (wallet == null || wallet.getAvailableCredits() < amount) {
            System.out.println("Insufficient available credits");
            return false;
        }

        try {
            conn.setAutoCommit(false);
            BatchEventService eventService = new BatchEventService();
            
            // Track batches consumed during retirement
            List<BatchRetirementInfo> retirementDetails = new ArrayList<>();
            
            // 1. Update batches (FIFO - retire oldest credits first)
            double remainingToRetire = amount;
            List<CarbonCreditBatch> batches = getAvailableBatches(walletId);
            
            for (CarbonCreditBatch batch : batches) {
                if (remainingToRetire == 0) break;
                
                double retireFromBatch = Math.min(remainingToRetire, batch.getRemainingAmount().doubleValue());
                updateBatchRetirement(batch.getId(), retireFromBatch);
                
                // Track this retirement for later recording
                retirementDetails.add(new BatchRetirementInfo(batch.getId(), retireFromBatch));
                
                remainingToRetire = remainingToRetire - retireFromBatch;
            }
            
            // 2. Update wallet balances
            String updateWallet = "UPDATE wallet SET available_credits = available_credits - ?, " +
                                  "retired_credits = retired_credits + ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateWallet)) {
                ps.setDouble(1, amount);
                ps.setDouble(2, amount);
                ps.setInt(3, walletId);
                ps.executeUpdate();
            }
            
            // 3. Record transaction and get its ID
            long transactionId = recordTransactionWithId(walletId, 
                retirementDetails.isEmpty() ? null : retirementDetails.get(0).batchId, 
                "RETIRE", amount, referenceNote);
            
            if (transactionId == -1) {
                conn.rollback();
                return false;
            }
            
            // 4. Record batch retirement details (link transaction to batches)
            for (BatchRetirementInfo detail : retirementDetails) {
                recordBatchRetirementDetail(transactionId, detail.batchId, detail.amount);
                
                // Record RETIRED event for each batch
                JsonObject eventData = new JsonObject();
                eventData.addProperty("amount_retired", detail.amount);
                eventData.addProperty("transaction_id", transactionId);
                eventData.addProperty("wallet_id", walletId);
                eventData.addProperty("reference_note", referenceNote);
                eventService.recordEvent(detail.batchId, BatchEventType.RETIRED, eventData, actor);
            }
            
            conn.commit();
            return true;
            
        } catch (SQLException ex) {
            try {
                conn.rollback();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            System.out.println("Error retiring credits: " + ex.getMessage());
            ex.printStackTrace();
            return false;
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Helper class to track batch retirement info during FIFO processing.
     */
    private static class BatchRetirementInfo {
        int batchId;
        double amount;
        
        BatchRetirementInfo(int batchId, double amount) {
            this.batchId = batchId;
            this.amount = amount;
        }
    }

    /**
     * Transfer credits between wallets (legacy method - direct transfer).
     */
    public boolean transferCredits(int fromWalletId, int toWalletId, double amount, String referenceNote) {
        return transferCreditsWithMode(fromWalletId, toWalletId, amount, referenceNote, TransferMode.DIRECT, "SYSTEM");
    }

    /**
     * Transfer mode enum for batch handling.
     */
    public enum TransferMode {
        DIRECT,      // Transfer existing batches directly (change wallet_id)
        SPLIT_CHILD  // Create child batches for destination wallet (maintains provenance)
    }

    /**
     * Transfer credits between wallets with traceability and batch handling mode.
     * 
     * @param fromWalletId Source wallet
     * @param toWalletId Destination wallet
     * @param amount Amount to transfer
     * @param referenceNote Description
     * @param mode DIRECT (change ownership) or SPLIT_CHILD (create child batches)
     * @param actor User or system performing transfer
     * @return true if successful
     */
    public boolean transferCreditsWithMode(int fromWalletId, int toWalletId, double amount, 
                                          String referenceNote, TransferMode mode, String actor) {
        if (amount <= 0) {
            System.out.println("Amount must be positive");
            return false;
        }

        Wallet fromWallet = getWalletById(fromWalletId);
        if (fromWallet == null || fromWallet.getAvailableCredits() < amount) {
            System.out.println("Insufficient credits in source wallet");
            return false;
        }

        Wallet toWallet = getWalletById(toWalletId);
        if (toWallet == null) {
            System.out.println("Destination wallet not found");
            return false;
        }

        // Check if we're already in a transaction (autocommit is false)
        boolean wasInTransaction = false;
        try {
            wasInTransaction = !conn.getAutoCommit();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        try {
            // Only manage transaction if we're not already in one
            if (!wasInTransaction) {
                conn.setAutoCommit(false);
            }
            
            BatchEventService eventService = new BatchEventService();
            
            // Generate transfer pair ID to link IN/OUT transactions
            String transferPairId = java.util.UUID.randomUUID().toString();
            
            if (mode == TransferMode.DIRECT) {
                // DIRECT MODE: Transfer existing batches (change wallet_id)
                transferBatchesDirect(fromWalletId, toWalletId, amount, eventService, actor, referenceNote);
            } else {
                // SPLIT_CHILD MODE: Create child batches for destination
                transferBatchesSplitChild(fromWalletId, toWalletId, amount, eventService, actor, referenceNote);
            }
            
            // Deduct from source wallet
            String deductSql = "UPDATE wallet SET available_credits = available_credits - ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(deductSql)) {
                ps.setDouble(1, amount);
                ps.setInt(2, fromWalletId);
                ps.executeUpdate();
            }
            
            // Add to destination wallet
            String addSql = "UPDATE wallet SET available_credits = available_credits + ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(addSql)) {
                ps.setDouble(1, amount);
                ps.setInt(2, toWalletId);
                ps.executeUpdate();
            }
            
<<<<<<< HEAD
            // 3. Record transactions
<<<<<<< HEAD
=======
            // Record linked transactions with transfer_pair_id
>>>>>>> dhiaeddine_bondka_gestion_green_wallet
            String note = String.format("%s (Transfer to Wallet #%s)", referenceNote, safeWalletNumber(toWallet.getWalletNumber()));
            recordTransferTransaction(fromWalletId, null, "TRANSFER_OUT", amount, note, transferPairId);
            
            String noteIn = String.format("%s (Transfer from Wallet #%s)", referenceNote, safeWalletNumber(fromWallet.getWalletNumber()));
<<<<<<< HEAD
=======
<<<<<<< HEAD
            String note = String.format("%s (Transfer to Wallet #%d)", referenceNote, toWallet.getWalletNumber());
            recordTransaction(fromWalletId, null, "TRANSFER_OUT", amount, note);
            
            String noteIn = String.format("%s (Transfer from Wallet #%d)", referenceNote, fromWallet.getWalletNumber());
=======
            String note = String.format("%s (Transfer to Wallet #%s)", referenceNote, safeWalletNumber(toWallet.getWalletNumber()));
            recordTransaction(fromWalletId, null, "TRANSFER_OUT", amount, note);
            
            String noteIn = String.format("%s (Transfer from Wallet #%s)", referenceNote, safeWalletNumber(fromWallet.getWalletNumber()));
>>>>>>> f3559248f463304c68513eb2c92f99791d2c4657
>>>>>>> yassine_antar
            recordTransaction(toWalletId, null, "TRANSFER_IN", amount, noteIn);
=======
            recordTransferTransaction(toWalletId, null, "TRANSFER_IN", amount, noteIn, transferPairId);
>>>>>>> dhiaeddine_bondka_gestion_green_wallet
            
            // Only commit if we started the transaction
            if (!wasInTransaction) {
                conn.commit();
            }
            return true;
            
        } catch (SQLException ex) {
            // Only rollback if we started the transaction
            if (!wasInTransaction) {
                try {
                    conn.rollback();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Error transferring credits: " + ex.getMessage());
            ex.printStackTrace();
            return false;
        } finally {
            // Only restore autocommit if we changed it
            if (!wasInTransaction) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Transfer batches directly (change wallet_id, maintain batch identity).
     */
    private void transferBatchesDirect(int fromWalletId, int toWalletId, double amount,
                                      BatchEventService eventService, String actor, String referenceNote) throws SQLException {
        double remainingToTransfer = amount;
        List<CarbonCreditBatch> batches = getAvailableBatches(fromWalletId);
        
        for (CarbonCreditBatch batch : batches) {
            if (remainingToTransfer <= 0) break;
            
            double transferFromBatch = Math.min(remainingToTransfer, batch.getRemainingAmount().doubleValue());
            
            if (transferFromBatch == batch.getRemainingAmount().doubleValue()) {
                // Transfer entire batch (change wallet_id)
                String sql = "UPDATE carbon_credit_batches SET wallet_id = ? WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, toWalletId);
                    ps.setInt(2, batch.getId());
                    ps.executeUpdate();
                }
                
                // Record TRANSFERRED event
                JsonObject eventData = new JsonObject();
                eventData.addProperty("from_wallet_id", fromWalletId);
                eventData.addProperty("to_wallet_id", toWalletId);
                eventData.addProperty("amount", transferFromBatch);
                eventData.addProperty("transfer_mode", "DIRECT_FULL");
                eventData.addProperty("reference_note", referenceNote);
                eventService.recordEvent(batch.getId(), BatchEventType.TRANSFERRED, eventData, actor);
                
            } else {
                // Partial transfer: split batch
                int childBatchId = splitBatch(batch.getId(), transferFromBatch, toWalletId, actor);
                if (childBatchId == -1) {
                    throw new SQLException("Failed to split batch during transfer");
                }
            }
            
            remainingToTransfer -= transferFromBatch;
        }
    }

    /**
     * Transfer batches by creating child batches (maintains full lineage).
     */
    private void transferBatchesSplitChild(int fromWalletId, int toWalletId, double amount,
                                          BatchEventService eventService, String actor, String referenceNote) throws SQLException {
        double remainingToTransfer = amount;
        List<CarbonCreditBatch> batches = getAvailableBatches(fromWalletId);
        
        for (CarbonCreditBatch batch : batches) {
            if (remainingToTransfer <= 0) break;
            
            double transferFromBatch = Math.min(remainingToTransfer, batch.getRemainingAmount().doubleValue());
            
            // Create child batch for destination wallet
            int childBatchId = splitBatch(batch.getId(), transferFromBatch, toWalletId, actor);
            if (childBatchId == -1) {
                throw new SQLException("Failed to create child batch during transfer");
            }
            
            // Record MARKETPLACE_SOLD event (this is often used for marketplace transactions)
            JsonObject eventData = new JsonObject();
            eventData.addProperty("from_wallet_id", fromWalletId);
            eventData.addProperty("to_wallet_id", toWalletId);
            eventData.addProperty("child_batch_id", childBatchId);
            eventData.addProperty("amount", transferFromBatch);
            eventData.addProperty("transfer_mode", "SPLIT_CHILD");
            eventData.addProperty("reference_note", referenceNote);
            eventService.recordEvent(batch.getId(), BatchEventType.MARKETPLACE_SOLD, eventData, actor);
            
            remainingToTransfer -= transferFromBatch;
        }
    }

    /**
     * Record transaction with transfer_pair_id for linking IN/OUT transactions.
     */
    private void recordTransferTransaction(int walletId, Integer batchId, String type, double amount, 
                                          String note, String transferPairId) throws SQLException {
        String sql = "INSERT INTO wallet_transactions " +
                     "(wallet_id, batch_id, type, amount, reference_note, transfer_pair_id, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        Integer effectiveBatchId = batchId != null ? batchId : 0;
        String effectiveType = normalizeTransactionType(type);
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, walletId);
            ps.setInt(2, effectiveBatchId);
            ps.setString(3, effectiveType);
            ps.setDouble(4, amount);
            ps.setString(5, note);
            ps.setString(6, transferPairId);
            ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            // Fallback if transfer_pair_id column doesn't exist yet
            if (isUnknownColumnError(ex, "transfer_pair_id")) {
                recordTransaction(walletId, effectiveBatchId, effectiveType, amount, note);
                return;
            }
            throw ex;
        }
    }
    

    // ==================== TRANSACTION HISTORY ====================

    /**
     * Get all transactions for a wallet.
     */
    public List<OperationWallet> getWalletTransactions(int walletId) {
        List<OperationWallet> transactions = new ArrayList<>();
        String sql = "SELECT * FROM wallet_transactions WHERE wallet_id = ? ORDER BY created_at DESC";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, walletId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                transactions.add(mapResultSetToTransaction(rs));
            }
        } catch (SQLException ex) {
            System.out.println("Error fetching transactions: " + ex.getMessage());
        }
        return transactions;
    }

    /**
     * Get credit batches for a wallet.
     */
    public List<CarbonCreditBatch> getWalletBatches(int walletId) {
        List<CarbonCreditBatch> batches = new ArrayList<>();
        String sql = "SELECT * FROM carbon_credit_batches WHERE wallet_id = ? ORDER BY issued_at DESC";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, walletId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                batches.add(mapResultSetToBatch(rs));
            }
        } catch (SQLException ex) {
            System.out.println("Error fetching batches: " + ex.getMessage());
        }
        return batches;
    }

    // ==================== BATCH TRACEABILITY METHODS ====================

    /**
     * Split a batch into a child batch and transfer to target wallet.
     * Creates full lineage tracking and records SPLIT event.
     * 
     * @param batchId ID of the batch to split
     * @param amountToSplit Amount to split off
     * @param targetWalletId Destination wallet for the child batch
     * @param actor User or system performing the split
     * @return ID of the newly created child batch, or -1 if failed
     */
    public int splitBatch(int batchId, double amountToSplit, int targetWalletId, String actor) {
        if (amountToSplit <= 0) {
            System.err.println("Split amount must be positive");
            return -1;
        }

        // Get parent batch
        CarbonCreditBatch parentBatch = getBatchById(batchId);
        if (parentBatch == null) {
            System.err.println("Parent batch not found");
            return -1;
        }

        if (parentBatch.getRemainingAmount().doubleValue() < amountToSplit) {
            System.err.println("Insufficient remaining amount in parent batch");
            return -1;
        }

        // Verify target wallet exists
        Wallet targetWallet = getWalletById(targetWalletId);
        if (targetWallet == null) {
            System.err.println("Target wallet not found");
            return -1;
        }

        try {
            conn.setAutoCommit(false);
            BatchEventService eventService = new BatchEventService();

            // 1. Create child batch
            String insertSql = "INSERT INTO carbon_credit_batches " +
                              "(project_id, wallet_id, total_amount, remaining_amount, status, " +
                              "verification_standard, vintage_year, calculation_audit_id, " +
                              "parent_batch_id, batch_type, issued_at) " +
                              "VALUES (?, ?, ?, ?, 'AVAILABLE', ?, ?, ?, ?, 'SECONDARY', NOW())";
            
            int childBatchId = -1;
            try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, parentBatch.getProjectId());
                ps.setInt(2, targetWalletId);
                ps.setDouble(3, amountToSplit);
                ps.setDouble(4, amountToSplit);
                ps.setString(5, parentBatch.getVerificationStandard());
                ps.setObject(6, parentBatch.getVintageYear());
                ps.setString(7, parentBatch.getCalculationAuditId());
                ps.setInt(8, parentBatch.getId());
                
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    childBatchId = rs.getInt(1);
                }
            }

            if (childBatchId == -1) {
                conn.rollback();
                return -1;
            }

            // 2. Update parent batch
            double newRemainingAmount = parentBatch.getRemainingAmount().doubleValue() - amountToSplit;
            String newStatus = newRemainingAmount == 0 ? "FULLY_RETIRED" : 
                             (newRemainingAmount < parentBatch.getTotalAmount().doubleValue() ? "PARTIALLY_RETIRED" : "AVAILABLE");
            
            String updateParent = "UPDATE carbon_credit_batches " +
                                 "SET remaining_amount = ?, status = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateParent)) {
                ps.setDouble(1, newRemainingAmount);
                ps.setString(2, newStatus);
                ps.setInt(3, batchId);
                ps.executeUpdate();
            }

            // 3. Update parent's lineage JSON (add child batch ID)
            updateParentLineage(batchId, childBatchId);

            // 4. Update target wallet credits
            String updateWallet = "UPDATE wallet SET available_credits = available_credits + ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateWallet)) {
                ps.setDouble(1, amountToSplit);
                ps.setInt(2, targetWalletId);
                ps.executeUpdate();
            }

            // 5. Record SPLIT event on parent batch
            JsonObject eventData = new JsonObject();
            eventData.addProperty("child_batch_id", childBatchId);
            eventData.addProperty("amount_split", amountToSplit);
            eventData.addProperty("target_wallet_id", targetWalletId);
            eventData.addProperty("target_wallet_name", targetWallet.getName());
            eventService.recordEvent(batchId, BatchEventType.SPLIT, eventData, actor);

            // 6. Record ISSUED event on child batch (it's a new issuance, though derived)
            JsonObject childEventData = new JsonObject();
            childEventData.addProperty("parent_batch_id", batchId);
            childEventData.addProperty("amount", amountToSplit);
            childEventData.addProperty("batch_type", "SECONDARY");
            childEventData.addProperty("split_from_parent", true);
            eventService.recordEvent(childBatchId, BatchEventType.ISSUED, childEventData, actor);

            conn.commit();
            System.out.println("Batch split successful. Child batch ID: " + childBatchId);
            return childBatchId;

        } catch (SQLException ex) {
            try {
                conn.rollback();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            System.err.println("Error splitting batch: " + ex.getMessage());
            ex.printStackTrace();
            return -1;
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Get full batch lineage (parent-child tree) for a batch.
     * 
     * @param batchId The batch ID
     * @return List of batches in lineage tree
     */
    public List<CarbonCreditBatch> getBatchLineage(int batchId) {
        List<CarbonCreditBatch> lineage = new ArrayList<>();
        
        // Use the view created in the migration
        String sql = "SELECT ccb.* FROM v_batch_full_lineage vbl " +
                    "INNER JOIN carbon_credit_batches ccb ON vbl.id = ccb.id " +
                    "WHERE vbl.id = ? OR vbl.parent_batch_id = ? " +
                    "OR vbl.id IN (SELECT parent_batch_id FROM carbon_credit_batches WHERE id = ?) " +
                    "ORDER BY vbl.lineage_depth ASC, vbl.id ASC";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, batchId);
            ps.setInt(2, batchId);
            ps.setInt(3, batchId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lineage.add(mapResultSetToBatch(rs));
            }
        } catch (SQLException ex) {
            // Fallback to simple query if view doesn't exist
            if (!lineageViewWarningShown) {
                System.err.println("[Schema Info] Lineage view 'v_batch_full_lineage' not found. Using fallback query (basic lineage).");
                System.err.println("[Schema Info] Run ADD_BATCH_METADATA_COLUMNS.sql for full lineage support.");
                lineageViewWarningShown = true;
            }
            return getBatchLineageFallback(batchId);
        }
        return lineage;
    }

    /**
     * Fallback method for getBatchLineage if view is unavailable.
     */
    private List<CarbonCreditBatch> getBatchLineageFallback(int batchId) {
        List<CarbonCreditBatch> lineage = new ArrayList<>();
        
        // Get the batch itself
        CarbonCreditBatch batch = getBatchById(batchId);
        if (batch != null) {
            lineage.add(batch);
            
            // Get parent if exists
            if (batch.getParentBatchId() != null) {
                CarbonCreditBatch parent = getBatchById(batch.getParentBatchId());
                if (parent != null) {
                    lineage.add(0, parent); // Add at beginning
                }
            }
            
            // Get children
            List<CarbonCreditBatch> children = getChildBatches(batchId);
            lineage.addAll(children);
        }
        
        return lineage;
    }

    /**
     * Get child batches (batches that were split from this batch).
     * 
     * @param parentBatchId The parent batch ID
     * @return List of child batches
     */
    public List<CarbonCreditBatch> getChildBatches(int parentBatchId) {
        List<CarbonCreditBatch> children = new ArrayList<>();
        String sql = "SELECT * FROM carbon_credit_batches WHERE parent_batch_id = ? ORDER BY issued_at ASC";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, parentBatchId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                children.add(mapResultSetToBatch(rs));
            }
        } catch (SQLException ex) {
            // Column parent_batch_id doesn't exist - schema not migrated yet
            if (ex.getMessage().contains("parent_batch_id") || ex.getMessage().contains("Unknown column")) {
                if (!parentBatchIdWarningShown) {
                    System.err.println("[Schema Info] Column 'parent_batch_id' not found. Parent-child batch tracking unavailable.");
                    System.err.println("[Schema Info] Run ADD_BATCH_METADATA_COLUMNS.sql to enable lineage tracking.");
                    parentBatchIdWarningShown = true;
                }
                return new ArrayList<>(); // Return empty list
            }
            System.err.println("Error fetching child batches: " + ex.getMessage());
        }
        return children;
    }

    /**
     * Get complete batch provenance (emission calculation -> current state).
     * Returns a comprehensive report including:
     * - Batch details
     * - Emission calculation audit (if linked)
     * - All events in timeline
     * - Lineage (parents and children)
     * 
     * @param batchId The batch ID
     * @return JSON string with complete provenance data
     */
    public String getBatchProvenance(int batchId) {
        JsonObject provenance = new JsonObject();
        
        try {
            // 1. Batch details
            CarbonCreditBatch batch = getBatchById(batchId);
            if (batch == null) {
                provenance.addProperty("error", "Batch not found");
                return gson.toJson(provenance);
            }
            
            // Manually serialize batch to avoid LocalDateTime reflection issues
            JsonObject batchJson = new JsonObject();
            batchJson.addProperty("id", batch.getId());
            batchJson.addProperty("projectId", batch.getProjectId());
            batchJson.addProperty("walletId", batch.getWalletId());
            batchJson.addProperty("totalAmount", batch.getTotalAmount());
            batchJson.addProperty("remainingAmount", batch.getRemainingAmount());
            batchJson.addProperty("status", batch.getStatus());
            if (batch.getIssuedAt() != null) {
                batchJson.addProperty("issuedAt", batch.getIssuedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
            provenance.add("batch", batchJson);
            
            // 2. Emission calculation (if linked)
            if (batch.getCalculationAuditId() != null) {
                String calcSql = "SELECT * FROM emission_calculations WHERE calculation_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(calcSql)) {
                    ps.setString(1, batch.getCalculationAuditId());
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        JsonObject calculation = new JsonObject();
                        calculation.addProperty("calculation_id", rs.getString("calculation_id"));
                        calculation.addProperty("co2e_result", rs.getDouble("co2e_result"));
                        calculation.addProperty("emission_factor_id", rs.getString("emission_factor_id"));
                        calculation.addProperty("methodology_version", rs.getString("methodology_version"));
                        calculation.addProperty("tier", rs.getInt("tier"));
                        calculation.addProperty("actor", rs.getString("actor"));
                        provenance.add("emission_calculation", calculation);
                    }
                }
            }
            
            // 3. Event timeline
            BatchEventService eventService = new BatchEventService();
            List<Models.BatchEvent> events = eventService.getBatchEvents(batchId);
            JsonArray eventsArray = new JsonArray();
            for (Models.BatchEvent event : events) {
                JsonObject eventJson = new JsonObject();
                eventJson.addProperty("id", event.getId());
                eventJson.addProperty("batchId", event.getBatchId());
                if (event.getEventType() != null) {
                    eventJson.addProperty("eventType", event.getEventType().name());
                }
                if (event.getEventDataJson() != null) {
                    eventJson.addProperty("eventData", event.getEventDataJson());
                }
                if (event.getCreatedAt() != null) {
                    eventJson.addProperty("timestamp", event.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                }
                eventsArray.add(eventJson);
            }
            provenance.add("events", eventsArray);
            provenance.addProperty("event_count", events.size());
            provenance.addProperty("chain_valid", eventService.validateEventChain(batchId));
            
            // 4. Lineage
            List<CarbonCreditBatch> lineage = getBatchLineage(batchId);
            JsonArray lineageArray = new JsonArray();
            for (CarbonCreditBatch b : lineage) {
                JsonObject lineageJson = new JsonObject();
                lineageJson.addProperty("id", b.getId());
                lineageJson.addProperty("totalAmount", b.getTotalAmount());
                lineageJson.addProperty("status", b.getStatus());
                if (b.getIssuedAt() != null) {
                    lineageJson.addProperty("issuedAt", b.getIssuedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                }
                lineageArray.add(lineageJson);
            }
            provenance.add("lineage", lineageArray);
            provenance.addProperty("lineage_depth", batch.getLineageDepth());
            
            // 5. Summary stats
            provenance.addProperty("batch_type", batch.getBatchType() != null ? batch.getBatchType().name() : "PRIMARY");
            provenance.addProperty("is_primary", batch.isPrimary());
            provenance.addProperty("retirement_percentage", batch.getRetirementPercentage());
            
        } catch (SQLException ex) {
            System.err.println("Error generating provenance: " + ex.getMessage());
            provenance.addProperty("error", ex.getMessage());
        }
        
        return gson.toJson(provenance);
    }

    /**
     * Get a single batch by ID.
     * 
     * @param batchId The batch ID
     * @return The batch, or null if not found
     */
    public CarbonCreditBatch getBatchById(int batchId) {
        String sql = "SELECT * FROM carbon_credit_batches WHERE id = ?";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, batchId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToBatch(rs);
            }
        } catch (SQLException ex) {
            System.err.println("Error fetching batch: " + ex.getMessage());
        }
        return null;
    }

    /**
     * Update parent batch's lineage JSON to include new child batch ID.
     */
    private void updateParentLineage(int parentBatchId, int childBatchId) throws SQLException {
        // Get current lineage JSON
        String selectSql = "SELECT lineage_json FROM carbon_credit_batches WHERE id = ?";
        String currentLineage = null;
        
        try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
            ps.setInt(1, parentBatchId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                currentLineage = rs.getString("lineage_json");
            }
        }
        
        // Parse and update JSON array
        JsonArray childIds;
        
        if (currentLineage == null || currentLineage.trim().isEmpty()) {
            childIds = new JsonArray();
        } else {
            try {
                childIds = gson.fromJson(currentLineage, JsonArray.class);
            } catch (Exception e) {
                childIds = new JsonArray();
            }
        }
        
        childIds.add(childBatchId);
        
        // Update database
        String updateSql = "UPDATE carbon_credit_batches SET lineage_json = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setString(1, gson.toJson(childIds));
            ps.setInt(2, parentBatchId);
            ps.executeUpdate();
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Generate a unique random wallet number.
     */
    private int generateUniqueWalletNumber() {
        int attempts = 0;
        int maxAttempts = 50;
        
        while (attempts < maxAttempts) {
            // Generate random 6-digit number (100000-999999)
            int walletNumber = 100000 + (int)(Math.random() * 900000);
            
            // Check if it exists
            if (!walletNumberExists(walletNumber)) {
                return walletNumber;
            }
            attempts++;
        }
        
        // Fallback to timestamp-based if random fails
        return (int)(System.currentTimeMillis() % 1000000);
    }
    
    /**
     * Generate unique batch serial number.
     * Format: CC-YYYY-NNNNNN (e.g., CC-2024-000001)
     */
    private String generateBatchSerialNumber() {
        int year = LocalDateTime.now().getYear();
        String sql = "SELECT MAX(CAST(SUBSTRING(serial_number, 9) AS UNSIGNED)) FROM carbon_credit_batches WHERE serial_number LIKE ?";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "CC-" + year + "-%");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int maxNumber = rs.getInt(1);
                return String.format("CC-%d-%06d", year, maxNumber + 1);
            }
        } catch (SQLException ex) {
            // Fallback: use timestamp-based if query fails
            System.err.println("Warning: Could not query for serial number, using fallback: " + ex.getMessage());
        }
        
        // Fallback: CC-YYYY-HHMMSS
        return String.format("CC-%d-%06d", year, (int)(System.currentTimeMillis() % 1000000));
    }

    private boolean walletNumberExists(int walletNumber) {
        String sql = "SELECT COUNT(*) FROM wallet WHERE wallet_number = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, walletNumber);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException ex) {
            System.out.println("Error checking wallet number: " + ex.getMessage());
        }
        return false;
    }

    private String generateWalletNumber() {
        return "GW-" + System.currentTimeMillis();
    }

    /**
     * Create credit batch (legacy method without traceability).
     */
    private int createCreditBatch(int projectId, int walletId, double amount) {
        return createCreditBatch(projectId, walletId, amount, null, Models.BatchType.PRIMARY);
    }

    /**
     * Create credit batch with full traceability support.
     * 
     * @param projectId Source project
     * @param walletId Destination wallet
     * @param amount Amount of credits
     * @param calculationAuditId Optional link to emission calculation
     * @param batchType PRIMARY or SECONDARY
     * @return Batch ID, or -1 if failed
     */
    private int createCreditBatch(int projectId, int walletId, double amount, 
                                 String calculationAuditId, Models.BatchType batchType) {
        return createCreditBatch(projectId, walletId, amount, calculationAuditId, batchType, null, null);
    }
    
    private int createCreditBatch(int projectId, int walletId, double amount, 
                                 String calculationAuditId, Models.BatchType batchType,
                                 String verificationStandard, Integer vintageYear) {
        // Generate unique serial number
        String serialNumber = generateBatchSerialNumber();
        
        // Try with full schema first (includes metadata columns)
        String fullSql = "INSERT INTO carbon_credit_batches (project_id, wallet_id, total_amount, " +
                         "remaining_amount, status, calculation_audit_id, batch_type, " +
                         "verification_standard, vintage_year, serial_number, issued_at) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        // Fallback to basic schema (no metadata columns)
        String basicSql = "INSERT INTO carbon_credit_batches (project_id, wallet_id, total_amount, " +
                          "remaining_amount, status, issued_at) " +
                          "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement ps = conn.prepareStatement(fullSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, projectId);
            ps.setInt(2, walletId);
            ps.setDouble(3, amount);
            ps.setDouble(4, amount);
            ps.setString(5, "AVAILABLE");
            ps.setString(6, calculationAuditId);
            ps.setString(7, batchType != null ? batchType.name() : "PRIMARY");
            ps.setString(8, verificationStandard);
            if (vintageYear != null) {
                ps.setInt(9, vintageYear);
            } else {
                ps.setNull(9, java.sql.Types.INTEGER);
            }
            ps.setString(10, serialNumber);
            ps.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
            
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            // Check if error is due to missing columns - fallback to basic schema
            if (ex.getMessage().contains("Unknown column") || ex.getMessage().contains("calculation_audit_id") 
                || ex.getMessage().contains("verification_standard") || ex.getMessage().contains("vintage_year")
                || ex.getMessage().contains("batch_type")) {
                System.out.println("Metadata columns not found, using basic schema...");
                try (PreparedStatement ps = conn.prepareStatement(basicSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, projectId);
                    ps.setInt(2, walletId);
                    ps.setDouble(3, amount);
                    ps.setDouble(4, amount);
                    ps.setString(5, "AVAILABLE");
                    ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
                    
                    ps.executeUpdate();
                    ResultSet rs = ps.getGeneratedKeys();
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                } catch (SQLException fallbackEx) {
                    System.out.println("Error creating batch (fallback): " + fallbackEx.getMessage());
                    fallbackEx.printStackTrace();
                }
            } else {
                System.out.println("Error creating batch: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
        return -1;
    }

    private void recordTransaction(int walletId, Integer batchId, String type, double amount, String note) throws SQLException {
        String sql = "INSERT INTO wallet_transactions (wallet_id, batch_id, type, amount, reference_note, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
<<<<<<< HEAD
        Integer effectiveBatchId = batchId != null ? batchId : 0;
        String effectiveType = normalizeTransactionType(type);
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, walletId);
            ps.setInt(2, effectiveBatchId);
            ps.setString(3, effectiveType);
=======
<<<<<<< HEAD
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, walletId);
            ps.setObject(2, batchId);
            ps.setString(3, type);
=======
        Integer effectiveBatchId = batchId != null ? batchId : 0;
        String effectiveType = normalizeTransactionType(type);
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, walletId);
            ps.setInt(2, effectiveBatchId);
            ps.setString(3, effectiveType);
>>>>>>> f3559248f463304c68513eb2c92f99791d2c4657
>>>>>>> yassine_antar
            ps.setDouble(4, amount);
            ps.setString(5, note);
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
<<<<<<< HEAD
=======
<<<<<<< HEAD
=======
>>>>>>> yassine_antar
        } catch (SQLException ex) {
            if (isUnknownColumnError(ex, "created_at")) {
                recordTransactionWithoutCreatedAt(walletId, effectiveBatchId, effectiveType, amount, note);
                return;
            }
            throw ex;
        }
    }

    private void recordTransactionWithoutCreatedAt(int walletId, Integer batchId, String type, double amount, String note) throws SQLException {
        String sql = "INSERT INTO wallet_transactions (wallet_id, batch_id, type, amount, reference_note) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, walletId);
            ps.setInt(2, batchId != null ? batchId : 0);
            ps.setString(3, normalizeTransactionType(type));
            ps.setDouble(4, amount);
            ps.setString(5, note);
            ps.executeUpdate();
<<<<<<< HEAD
=======
>>>>>>> f3559248f463304c68513eb2c92f99791d2c4657
>>>>>>> yassine_antar
        }
    }

    /**
     * Record transaction and return its ID for linking retirement details.
     */
    private long recordTransactionWithId(int walletId, Integer batchId, String type, double amount, String note) throws SQLException {
        String sql = "INSERT INTO wallet_transactions (wallet_id, batch_id, type, amount, reference_note, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        Integer effectiveBatchId = batchId != null ? batchId : 0;
        String effectiveType = normalizeTransactionType(type);
        
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, walletId);
            ps.setInt(2, effectiveBatchId);
            ps.setString(3, effectiveType);
            ps.setDouble(4, amount);
            ps.setString(5, note);
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException ex) {
            System.err.println("Error recording transaction with ID: " + ex.getMessage());
            throw ex;
        }
        return -1;
    }

    /**
     * Record batch retirement detail (links transaction to specific batch consumed).
     */
    private void recordBatchRetirementDetail(long transactionId, int batchId, double amountRetired) throws SQLException {
        String sql = "INSERT INTO batch_retirement_details (transaction_id, batch_id, amount_retired) " +
                     "VALUES (?, ?, ?)";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, transactionId);
            ps.setInt(2, batchId);
            ps.setDouble(3, amountRetired);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Error recording batch retirement detail: " + ex.getMessage());
            throw ex;
        }
    }

    /**
     * Get all available credit batches for a wallet (public method for marketplace).
     */
    public List<CarbonCreditBatch> getAvailableBatchesByWalletId(int walletId) {
        return getAvailableBatches(walletId);
    }

    /**
     * Get available credit batches for a wallet (internal method).
     */
    private List<CarbonCreditBatch> getAvailableBatches(int walletId) {
        List<CarbonCreditBatch> batches = new ArrayList<>();
        String sql = "SELECT * FROM carbon_credit_batches WHERE wallet_id = ? AND remaining_amount > 0 " +
                     "ORDER BY issued_at ASC";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, walletId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                batches.add(mapResultSetToBatch(rs));
            }
        } catch (SQLException ex) {
            System.out.println("Error fetching batches: " + ex.getMessage());
        }
        return batches;
    }

    private void updateBatchRetirement(int batchId, double retireAmount) throws SQLException {
        String sql = "UPDATE carbon_credit_batches SET remaining_amount = remaining_amount - ?, " +
                     "status = CASE WHEN remaining_amount - ? = 0 THEN 'FULLY_RETIRED' " +
                     "WHEN remaining_amount - ? < total_amount THEN 'PARTIALLY_RETIRED' " +
                     "ELSE status END WHERE id = ?";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, retireAmount);
            ps.setDouble(2, retireAmount);
            ps.setDouble(3, retireAmount);
            ps.setInt(4, batchId);
            ps.executeUpdate();
        }
    }

    private Wallet mapResultSetToWallet(ResultSet rs) throws SQLException {
        Wallet wallet = new Wallet();
        wallet.setId(rs.getInt("id"));
<<<<<<< HEAD
        wallet.setWalletNumber(readInteger(rs, "wallet_number"));
        wallet.setName(readString(rs, "name"));
=======
<<<<<<< HEAD
        wallet.setWalletNumber((Integer) rs.getObject("wallet_number"));
        wallet.setName(rs.getString("name"));
>>>>>>> yassine_antar
        wallet.setOwnerType(rs.getString("owner_type"));
        Integer ownerId = readInteger(rs, "owner_id");
        wallet.setOwnerId(ownerId != null ? ownerId : 0);
        wallet.setAvailableCredits(rs.getDouble("available_credits"));
        wallet.setRetiredCredits(rs.getDouble("retired_credits"));
<<<<<<< HEAD
        wallet.setCreatedAt(readLocalDateTime(rs, "created_at"));
=======
        wallet.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
=======
        wallet.setWalletNumber(readInteger(rs, "wallet_number"));
        wallet.setName(readString(rs, "name"));
        wallet.setOwnerType(rs.getString("owner_type"));
        Integer ownerId = readInteger(rs, "owner_id");
        wallet.setOwnerId(ownerId != null ? ownerId : 0);
        wallet.setAvailableCredits(rs.getDouble("available_credits"));
        wallet.setRetiredCredits(rs.getDouble("retired_credits"));
        wallet.setCreatedAt(readLocalDateTime(rs, "created_at"));
>>>>>>> f3559248f463304c68513eb2c92f99791d2c4657
>>>>>>> yassine_antar
        return wallet;
    }

    private CarbonCreditBatch mapResultSetToBatch(ResultSet rs) throws SQLException {
        CarbonCreditBatch batch = new CarbonCreditBatch();
        batch.setId(rs.getInt("id"));
        batch.setProjectId(rs.getInt("project_id"));
        batch.setWalletId(rs.getInt("wallet_id"));
        batch.setTotalAmount(rs.getBigDecimal("total_amount"));
        batch.setRemainingAmount(rs.getBigDecimal("remaining_amount"));
        batch.setStatus(rs.getString("status"));
<<<<<<< HEAD
        batch.setIssuedAt(readLocalDateTime(rs, "issued_at"));
=======
<<<<<<< HEAD
        batch.setIssuedAt(rs.getTimestamp("issued_at").toLocalDateTime());
=======
        batch.setIssuedAt(readLocalDateTime(rs, "issued_at"));
>>>>>>> f3559248f463304c68513eb2c92f99791d2c4657
>>>>>>> yassine_antar
        return batch;
    }

    private OperationWallet mapResultSetToTransaction(ResultSet rs) throws SQLException {
        OperationWallet transaction = new OperationWallet();
        transaction.setId(rs.getInt("id"));
        transaction.setWalletId(rs.getInt("wallet_id"));
<<<<<<< HEAD
        transaction.setBatchId(readInteger(rs, "batch_id"));
=======
<<<<<<< HEAD
        transaction.setBatchId((Integer) rs.getObject("batch_id"));
>>>>>>> yassine_antar
        transaction.setType(rs.getString("type"));
        transaction.setAmount(rs.getBigDecimal("amount"));
        transaction.setReferenceNote(rs.getString("reference_note"));
        transaction.setCreatedAt(readLocalDateTime(rs, "created_at"));
        return transaction;
    }
<<<<<<< HEAD
=======
=======
        transaction.setBatchId(readInteger(rs, "batch_id"));
        transaction.setType(rs.getString("type"));
        transaction.setAmount(rs.getBigDecimal("amount"));
        transaction.setReferenceNote(rs.getString("reference_note"));
        transaction.setCreatedAt(readLocalDateTime(rs, "created_at"));
        return transaction;
    }
>>>>>>> yassine_antar

    private Integer readInteger(ResultSet rs, String columnName) throws SQLException {
        Object value = rs.getObject(columnName);
        if (value == null) {
            return null;
        }

        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }

        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private LocalDateTime readLocalDateTime(ResultSet rs, String columnName) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columnName);
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    private String readString(ResultSet rs, String columnName) {
        try {
            return rs.getString(columnName);
        } catch (SQLException ex) {
            return null;
        }
    }

    private String normalizeTransactionType(String type) {
        if (type == null) {
            return "ISSUE";
        }
        return switch (type) {
            case "TRANSFER_IN" -> "ISSUE";
            case "TRANSFER_OUT" -> "RETIRE";
            default -> type;
        };
    }

    private boolean isUnknownColumnError(SQLException ex, String columnName) {
        if (ex == null || ex.getMessage() == null) {
            return false;
        }
        String msg = ex.getMessage().toLowerCase();
        return msg.contains("unknown column") && msg.contains(columnName.toLowerCase());
    }

    private String safeWalletNumber(Integer walletNumber) {
        return walletNumber == null ? "—" : String.valueOf(walletNumber);
    }
<<<<<<< HEAD
=======
>>>>>>> f3559248f463304c68513eb2c92f99791d2c4657
>>>>>>> yassine_antar
}
