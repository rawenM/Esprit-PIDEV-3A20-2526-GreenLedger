package Services;

import DataBase.MyConnection;

import java.sql.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manages the one-batch-per-project carbon credit issuance lifecycle.
 *
 * <p>Lifecycle states: PENDING_TX → SUBMITTED → CONFIRMED (or FAILED on error).
 *
 * <p>Key design invariants:
 * <ul>
 *   <li>At most one {@code carbon_credit_batches} row per project (enforced by UNIQUE on
 *       {@code project_id}).</li>
 *   <li>The DB transaction is committed BEFORE calling
 *       {@link BlockchainService#mintBatch(int, int, String, Map)} — the
 *       "commit-before-blockchain" pattern ensures the batch and pending transaction
 *       records survive even if the blockchain call fails.</li>
 *   <li>Failed batches can be retried: the old FAILED row (and its associated
 *       {@code blockchain_transactions} row) is deleted before a fresh attempt.</li>
 * </ul>
 *
 * <p>All database access uses {@link MyConnection#getConnection()} (plain JDBC, no DI).
 * Connections, statements, and result-sets are always closed in {@code finally} blocks.
 *
 * Requirements: 5.1 – 5.10
 */
public class BlockchainBatchIssuanceService {

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final TransactionService     transactionService;
    private final BlockchainService      blockchainService;
    private final GreenWalletCrudService greenWalletCrudService;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Construct a {@code BlockchainBatchIssuanceService} with the given collaborators.
     *
     * @param transactionService     manages {@code blockchain_transactions} lifecycle
     * @param blockchainService      dispatches on-chain mint operations
     * @param greenWalletCrudService creates wallets when an owner has none
     */
    public BlockchainBatchIssuanceService(TransactionService transactionService,
                                          BlockchainService blockchainService,
                                          GreenWalletCrudService greenWalletCrudService) {
        this.transactionService     = transactionService;
        this.blockchainService      = blockchainService;
        this.greenWalletCrudService = greenWalletCrudService;
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Issue carbon credits for a project.
     *
     * <p>Full lifecycle:
     * <ol>
     *   <li>Validate inputs.</li>
     *   <li>Load project and resolve owner wallet (sub-task 9.1).</li>
     *   <li>Idempotency / retry check (sub-task 9.2).</li>
     *   <li>Insert batch + pending transaction, commit (sub-task 9.3).</li>
     *   <li>Call {@code blockchainService.mintBatch()}, update statuses (sub-task 9.4).</li>
     * </ol>
     *
     * @param projectId     primary key of the project in the {@code projet} table
     * @param creditsAmount human-readable credit amount (must be &gt; 0)
     * @return result map with keys: {@code batchId}, {@code walletId}, {@code projectId},
     *         {@code txHash}, {@code status}, {@code amountBaseUnits}
     * @throws IllegalArgumentException if {@code creditsAmount <= 0} or {@code projectId <= 0}
     * @throws RuntimeException         if the project is not found or a DB/blockchain error occurs
     * Requirements: 5.1 – 5.10
     */
    public Map<String, Object> issueProjectCredits(int projectId, double creditsAmount) {

        // ── Sub-task 9.1: Validate inputs ────────────────────────────────────
        if (projectId <= 0) {
            throw new IllegalArgumentException("projectId must be > 0, got: " + projectId);
        }
        if (creditsAmount <= 0) {
            throw new IllegalArgumentException("creditsAmount must be > 0, got: " + creditsAmount);
        }

        // ── Sub-task 9.1: Load project ────────────────────────────────────────
        Map<String, Object> project = loadProject(projectId);
        if (project == null) {
            throw new RuntimeException("Project not found: id=" + projectId);
        }

        // ── Sub-task 9.1: Resolve owner ───────────────────────────────────────
        // Check entreprise_id first, then created_by, then user_id
        int ownerId = resolveOwnerId(project);
        if (ownerId <= 0) {
            throw new RuntimeException("Cannot resolve owner for project id=" + projectId);
        }

        // ── Sub-task 9.1: Ensure wallet ───────────────────────────────────────
        Map<String, Object> wallet = fetchWalletByOwner(ownerId);
        if (wallet == null) {
            // Create a default wallet for this owner
            greenWalletCrudService.createWallet("Default Wallet", null, "USER", ownerId);
            wallet = fetchWalletByOwner(ownerId);
            if (wallet == null) {
                throw new RuntimeException("Failed to create or fetch wallet for owner id=" + ownerId);
            }
        }
        int walletId = toInt(wallet.get("id"));

        // ── Sub-task 9.1: Convert credits to base units ───────────────────────
        String baseUnits = CreditUnitConverter.toBaseUnits(String.valueOf(creditsAmount));

        // ── Sub-task 9.2: Idempotency / retry check ───────────────────────────
        Map<String, Object> existingBatch = fetchBatchByProject(projectId);
        if (existingBatch != null) {
            String issuanceStatus = objectToString(existingBatch.get("issuance_status"));

            if ("FAILED".equalsIgnoreCase(issuanceStatus)) {
                // Retry path: delete the failed batch (and its blockchain_transactions row)
                deleteFailedBatch(existingBatch);
                // Fall through to create a new batch below
            } else {
                // Any other status (PENDING_TX, SUBMITTED, CONFIRMED, etc.) — return existing
                return buildExistingBatchResult(existingBatch, walletId, projectId, baseUnits);
            }
        }

        // ── Sub-task 9.3: Batch + transaction creation (commit-before-blockchain) ──
        int batchId;
        int txId;

        Connection conn = null;
        try {
            conn = MyConnection.getConnection();
            conn.setAutoCommit(false);

            // INSERT into carbon_credit_batches
            batchId = insertBatch(conn, projectId, walletId, creditsAmount, baseUnits);

            // Build payload map for the pending transaction
            Map<String, Object> payloadMap = new LinkedHashMap<>();
            payloadMap.put("project_id", projectId);
            payloadMap.put("wallet_id", walletId);
            payloadMap.put("amount_base_units", baseUnits);

            // Create pending blockchain_transactions row (uses its own connection internally)
            // We must commit our batch INSERT first so the batch row exists before the
            // transaction service references it.
            conn.commit();

            // Now create the pending transaction (separate connection via TransactionService)
            Map<String, Object> pendingTx = transactionService.createPendingTransaction(
                    "MINT",
                    walletId,
                    batchId,
                    baseUnits,
                    payloadMap,
                    null,
                    null,
                    "Batch issuance for project " + projectId
            );

            if (pendingTx == null) {
                throw new RuntimeException("Failed to create pending transaction for batch id=" + batchId);
            }
            txId = toInt(pendingTx.get("id"));

        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new RuntimeException("DB error during batch/transaction creation: " + e.getMessage(), e);
        } finally {
            closeQuietly(conn);
        }

        // ── Sub-task 9.4: Mint submission and status update ───────────────────
        // Build metadata for the blockchain call
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("transaction_id", txId);
        metadata.put("project_id", projectId);
        metadata.put("wallet_id", walletId);

        try {
            Map<String, Object> mintResult = blockchainService.mintBatch(walletId, batchId, baseUnits, metadata);

            // Extract result fields
            String txHash    = objectToString(mintResult.get("txHash"));
            if (txHash == null || txHash.isEmpty()) {
                txHash = objectToString(mintResult.get("tx_hash"));
            }
            int blockNumber  = toInt(mintResult.get("blockNumber"));
            int logIndex     = toInt(mintResult.get("logIndex"));

            // Mark transaction as submitted
            transactionService.markSubmitted(txId, txHash, blockNumber, logIndex);

            // UPDATE carbon_credit_batches: issuance_status = SUBMITTED
            updateBatchIssuanceStatus(batchId, "SUBMITTED", txHash);

            // Build and return result map
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("batchId",        batchId);
            result.put("walletId",       walletId);
            result.put("projectId",      projectId);
            result.put("txHash",         txHash);
            result.put("status",         "SUBMITTED");
            result.put("amountBaseUnits", baseUnits);
            return result;

        } catch (Exception e) {
            // Mark transaction as failed
            String errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
            transactionService.markFailed(txId, errorMessage);

            // UPDATE carbon_credit_batches: issuance_status = FAILED
            updateBatchIssuanceStatusFailed(batchId);

            // Rethrow
            throw new RuntimeException("mintBatch failed for project " + projectId + ": " + errorMessage, e);
        }
    }

    // =========================================================================
    // Sub-task 9.1 helpers
    // =========================================================================

    /**
     * Load a project row from the {@code projet} table.
     *
     * @param projectId project primary key
     * @return row as {@code Map<String,Object>}, or {@code null} if not found
     * Requirements: 5.1
     */
    private Map<String, Object> loadProject(int projectId) {
        String sql = "SELECT * FROM projet WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = MyConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, projectId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return resultSetRowToMap(rs);
            }
            return null;
        } catch (SQLException e) {
            System.err.println("[BlockchainBatchIssuanceService] loadProject failed: " + e.getMessage());
            return null;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
    }

    /**
     * Resolve the owner ID from a project row.
     *
     * <p>Checks {@code entreprise_id} first, then {@code created_by}, then {@code user_id}.
     * Returns the first non-null, non-zero value found.
     *
     * @param project project row map
     * @return resolved owner ID, or 0 if none found
     * Requirements: 5.1
     */
    private int resolveOwnerId(Map<String, Object> project) {
        // 1. entreprise_id
        int entrepriseId = toInt(project.get("entreprise_id"));
        if (entrepriseId > 0) {
            return entrepriseId;
        }
        // 2. created_by
        int createdBy = toInt(project.get("created_by"));
        if (createdBy > 0) {
            return createdBy;
        }
        // 3. user_id
        int userId = toInt(project.get("user_id"));
        if (userId > 0) {
            return userId;
        }
        return 0;
    }

    /**
     * Fetch the first wallet row for the given owner.
     *
     * @param ownerId owner primary key
     * @return wallet row map, or {@code null} if none found
     * Requirements: 5.2
     */
    private Map<String, Object> fetchWalletByOwner(int ownerId) {
        String sql = "SELECT * FROM wallet WHERE owner_id = ? LIMIT 1";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = MyConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, ownerId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return resultSetRowToMap(rs);
            }
            return null;
        } catch (SQLException e) {
            System.err.println("[BlockchainBatchIssuanceService] fetchWalletByOwner failed: " + e.getMessage());
            return null;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
    }

    // =========================================================================
    // Sub-task 9.2 helpers
    // =========================================================================

    /**
     * Fetch the existing {@code carbon_credit_batches} row for a project (if any).
     *
     * @param projectId project primary key
     * @return batch row map, or {@code null} if none found
     * Requirements: 5.4, 5.5, 5.7
     */
    private Map<String, Object> fetchBatchByProject(int projectId) {
        String sql = "SELECT * FROM carbon_credit_batches WHERE project_id = ? LIMIT 1";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = MyConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, projectId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return resultSetRowToMap(rs);
            }
            return null;
        } catch (SQLException e) {
            System.err.println("[BlockchainBatchIssuanceService] fetchBatchByProject failed: " + e.getMessage());
            return null;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
    }

    /**
     * Delete a FAILED batch row and its associated {@code blockchain_transactions} row.
     *
     * @param batch the FAILED batch row map
     * Requirements: 5.4
     */
    private void deleteFailedBatch(Map<String, Object> batch) {
        int batchId = toInt(batch.get("id"));

        Connection conn = null;
        PreparedStatement psDeleteTx = null;
        PreparedStatement psDeleteBatch = null;
        try {
            conn = MyConnection.getConnection();
            conn.setAutoCommit(false);

            // Delete associated blockchain_transactions row (if any)
            String deleteTxSql =
                "DELETE FROM blockchain_transactions WHERE batch_id = ? AND status IN ('PENDING','FAILED')";
            psDeleteTx = conn.prepareStatement(deleteTxSql);
            psDeleteTx.setInt(1, batchId);
            psDeleteTx.executeUpdate();

            // Delete the batch row
            String deleteBatchSql = "DELETE FROM carbon_credit_batches WHERE id = ?";
            psDeleteBatch = conn.prepareStatement(deleteBatchSql);
            psDeleteBatch.setInt(1, batchId);
            psDeleteBatch.executeUpdate();

            conn.commit();
        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new RuntimeException("Failed to delete FAILED batch id=" + batchId + ": " + e.getMessage(), e);
        } finally {
            closeQuietly(psDeleteBatch);
            closeQuietly(psDeleteTx);
            closeQuietly(conn);
        }
    }

    /**
     * Build a result map from an existing (non-FAILED) batch row.
     *
     * @param batch      existing batch row
     * @param walletId   resolved wallet id
     * @param projectId  project id
     * @param baseUnits  base units string (from the current call)
     * @return result map
     * Requirements: 5.5
     */
    private Map<String, Object> buildExistingBatchResult(Map<String, Object> batch,
                                                          int walletId,
                                                          int projectId,
                                                          String baseUnits) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batchId",         toInt(batch.get("id")));
        result.put("walletId",        walletId);
        result.put("projectId",       projectId);
        result.put("txHash",          objectToString(batch.get("tx_hash_on_batch")));
        result.put("status",          objectToString(batch.get("issuance_status")));
        result.put("amountBaseUnits", objectToString(batch.get("total_amount_base_units")));
        result.put("idempotent",      true);
        return result;
    }

    // =========================================================================
    // Sub-task 9.3 helpers
    // =========================================================================

    /**
     * INSERT a new row into {@code carbon_credit_batches} and return the generated id.
     *
     * <p>Uses the provided (already open, autoCommit=false) connection so the INSERT
     * participates in the caller's transaction.
     *
     * @param conn          open JDBC connection with autoCommit=false
     * @param projectId     project primary key
     * @param walletId      wallet primary key
     * @param creditsAmount human-readable credit amount
     * @param baseUnits     base-unit string
     * @return generated batch id
     * @throws SQLException on DB error
     * Requirements: 5.6, 5.10
     */
    private int insertBatch(Connection conn, int projectId, int walletId,
                             double creditsAmount, String baseUnits) throws SQLException {
        String sql =
            "INSERT INTO carbon_credit_batches " +
            "  (project_id, wallet_id, total_amount, remaining_amount, " +
            "   total_amount_base_units, remaining_amount_base_units, " +
            "   status, batch_type, issuance_status, issued_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, 'AVAILABLE', 'ISSUANCE', 'PENDING_TX', NOW())";

        PreparedStatement ps = null;
        ResultSet generatedKeys = null;
        try {
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, projectId);
            ps.setInt(2, walletId);
            ps.setDouble(3, creditsAmount);   // total_amount
            ps.setDouble(4, creditsAmount);   // remaining_amount
            ps.setString(5, baseUnits);       // total_amount_base_units
            ps.setString(6, baseUnits);       // remaining_amount_base_units

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("INSERT into carbon_credit_batches returned 0 rows affected");
            }

            generatedKeys = ps.getGeneratedKeys();
            if (generatedKeys.next()) {
                return generatedKeys.getInt(1);
            }
            throw new SQLException("INSERT into carbon_credit_batches: no generated key returned");
        } finally {
            closeQuietly(generatedKeys);
            closeQuietly(ps);
        }
    }

    // =========================================================================
    // Sub-task 9.4 helpers
    // =========================================================================

    /**
     * UPDATE {@code carbon_credit_batches} to {@code issuance_status = SUBMITTED}.
     *
     * @param batchId batch primary key
     * @param txHash  on-chain transaction hash
     * Requirements: 5.8
     */
    private void updateBatchIssuanceStatus(int batchId, String status, String txHash) {
        String sql =
            "UPDATE carbon_credit_batches " +
            "SET issuance_status = ?, tx_hash_on_batch = ?, issuance_submitted_at = NOW() " +
            "WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = MyConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, status);
            ps.setString(2, txHash);
            ps.setInt(3, batchId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[BlockchainBatchIssuanceService] updateBatchIssuanceStatus failed: " + e.getMessage());
        } finally {
            closeQuietly(ps);
            closeQuietly(conn);
        }
    }

    /**
     * UPDATE {@code carbon_credit_batches} to {@code issuance_status = FAILED}.
     *
     * @param batchId batch primary key
     * Requirements: 5.9
     */
    private void updateBatchIssuanceStatusFailed(int batchId) {
        String sql = "UPDATE carbon_credit_batches SET issuance_status = 'FAILED' WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = MyConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, batchId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[BlockchainBatchIssuanceService] updateBatchIssuanceStatusFailed failed: " + e.getMessage());
        } finally {
            closeQuietly(ps);
            closeQuietly(conn);
        }
    }

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

    /** Convert an Object to int (0 if null or not a Number). */
    private int toInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    /** Convert an Object to String (null if null). */
    private String objectToString(Object value) {
        if (value == null) return null;
        return value.toString();
    }

    /** Rollback a connection silently. */
    private void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try { conn.rollback(); } catch (SQLException ignored) {}
        }
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
