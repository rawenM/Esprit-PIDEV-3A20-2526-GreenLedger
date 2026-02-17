package Services;

import DataBase.MyConnection;
import Models.Wallet;
import Models.CarbonCreditBatch;
import Models.OperationWallet;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service layer for Green Wallet operations including CRUD and credit management.
 */
public class WalletService {

    private Connection conn;

    public WalletService() {
        this.conn = MyConnection.getConnection();
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
            
            ps.setString(1, String.valueOf(wallet.getWalletNumber()));
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
            if (isUnknownColumnError(ex, "name")) {
                return createWalletWithoutName(wallet);
            }
            System.out.println("Error creating wallet: " + ex.getMessage());
        }
        return -1;
    }

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
            if (isUnknownColumnError(ex, "name")) {
                return updateWalletWithoutName(wallet);
            }
            System.out.println("Error updating wallet: " + ex.getMessage());
        }
        return false;
    }

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
    public boolean issueCredits(int walletId, int projectId, double amount, String referenceNote) {
        if (amount <= 0) {
            System.out.println("Amount must be positive");
            return false;
        }

        try {
            conn.setAutoCommit(false);
            
            // 1. Create credit batch
            int batchId = createCreditBatch(projectId, walletId, amount);
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
        try {
            String sql = "UPDATE wallet SET available_credits = available_credits + ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setDouble(1, amount);
                ps.setInt(2, walletId);
                ps.executeUpdate();
            }
            
            // Record simple transaction without batch
            recordTransaction(walletId, null, "ISSUE", amount, description);
            return true;
        } catch (SQLException ex) {
            System.out.println("Error quick issuing credits: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Retire carbon credits (permanently used for offsetting).
     */
    public boolean retireCredits(int walletId, double amount, String referenceNote) {
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
            
            // 1. Update batches (FIFO - retire oldest credits first)
            double remainingToRetire = amount;
            List<CarbonCreditBatch> batches = getAvailableBatches(walletId);
            
            for (CarbonCreditBatch batch : batches) {
                if (remainingToRetire == 0) break;
                
                double retireFromBatch = Math.min(remainingToRetire, batch.getRemainingAmount().doubleValue());
                updateBatchRetirement(batch.getId(), retireFromBatch);
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
            
            // 3. Record transaction
            recordTransaction(walletId, null, "RETIRE", amount, referenceNote);
            
            conn.commit();
            return true;
            
        } catch (SQLException ex) {
            try {
                conn.rollback();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            System.out.println("Error retiring credits: " + ex.getMessage());
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
     * Transfer credits between wallets.
     */
    public boolean transferCredits(int fromWalletId, int toWalletId, double amount, String referenceNote) {
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

        try {
            conn.setAutoCommit(false);
            
            // 1. Deduct from source wallet
            String deductSql = "UPDATE wallet SET available_credits = available_credits - ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(deductSql)) {
                ps.setDouble(1, amount);
                ps.setInt(2, fromWalletId);
                ps.executeUpdate();
            }
            
            // 2. Add to destination wallet
            String addSql = "UPDATE wallet SET available_credits = available_credits + ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(addSql)) {
                ps.setDouble(1, amount);
                ps.setInt(2, toWalletId);
                ps.executeUpdate();
            }
            
            // 3. Record transactions
            String note = String.format("%s (Transfer to Wallet #%s)", referenceNote, safeWalletNumber(toWallet.getWalletNumber()));
            recordTransaction(fromWalletId, null, "TRANSFER_OUT", amount, note);
            
            String noteIn = String.format("%s (Transfer from Wallet #%s)", referenceNote, safeWalletNumber(fromWallet.getWalletNumber()));
            recordTransaction(toWalletId, null, "TRANSFER_IN", amount, noteIn);
            
            conn.commit();
            return true;
            
        } catch (SQLException ex) {
            try {
                conn.rollback();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            System.out.println("Error transferring credits: " + ex.getMessage());
            return false;
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
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

    private int createCreditBatch(int projectId, int walletId, double amount) {
        String sql = "INSERT INTO carbon_credit_batches (project_id, wallet_id, total_amount, " +
                     "remaining_amount, status, issued_at) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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
        } catch (SQLException ex) {
            System.out.println("Error creating batch: " + ex.getMessage());
        }
        return -1;
    }

    private void recordTransaction(int walletId, Integer batchId, String type, double amount, String note) throws SQLException {
        String sql = "INSERT INTO wallet_transactions (wallet_id, batch_id, type, amount, reference_note, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        Integer effectiveBatchId = batchId != null ? batchId : 0;
        String effectiveType = normalizeTransactionType(type);
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, walletId);
            ps.setInt(2, effectiveBatchId);
            ps.setString(3, effectiveType);
            ps.setDouble(4, amount);
            ps.setString(5, note);
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
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
        }
    }

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
        wallet.setWalletNumber(readInteger(rs, "wallet_number"));
        wallet.setName(readString(rs, "name"));
        wallet.setOwnerType(rs.getString("owner_type"));
        Integer ownerId = readInteger(rs, "owner_id");
        wallet.setOwnerId(ownerId != null ? ownerId : 0);
        wallet.setAvailableCredits(rs.getDouble("available_credits"));
        wallet.setRetiredCredits(rs.getDouble("retired_credits"));
        wallet.setCreatedAt(readLocalDateTime(rs, "created_at"));
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
        batch.setIssuedAt(readLocalDateTime(rs, "issued_at"));
        return batch;
    }

    private OperationWallet mapResultSetToTransaction(ResultSet rs) throws SQLException {
        OperationWallet transaction = new OperationWallet();
        transaction.setId(rs.getInt("id"));
        transaction.setWalletId(rs.getInt("wallet_id"));
        transaction.setBatchId(readInteger(rs, "batch_id"));
        transaction.setType(rs.getString("type"));
        transaction.setAmount(rs.getBigDecimal("amount"));
        transaction.setReferenceNote(rs.getString("reference_note"));
        transaction.setCreatedAt(readLocalDateTime(rs, "created_at"));
        return transaction;
    }

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
}
