package Services;

import DataBase.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Processes marketplace orders by submitting blockchain transfer transactions.
 *
 * <p>Design invariants:
 * <ul>
 *   <li>All order processing uses {@code SELECT … FOR UPDATE} (pessimistic locking) to
 *       prevent concurrent double-spending.</li>
 *   <li>The JDBC transaction is committed BEFORE calling
 *       {@link BlockchainService#transferBatch} — the "commit-before-blockchain" pattern
 *       ensures the pending transaction record survives even if the blockchain call fails.</li>
 *   <li>All amount comparisons use {@link CreditUnitConverter#compareBaseUnits} — never
 *       {@code double}/{@code float} for base-unit amounts.</li>
 *   <li>All database access uses {@link MyConnection#getConnection()} (plain JDBC, no DI).
 *       Connections, statements, and result-sets are always closed in {@code finally} blocks.</li>
 * </ul>
 *
 * Requirements: 9.1 – 9.9
 */
public class TradeExecutionService {

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final BlockchainService    blockchainService;
    private final TransactionService   transactionService;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Construct a {@code TradeExecutionService} with the given collaborators.
     *
     * @param blockchainService  dispatches on-chain transfer operations
     * @param transactionService manages {@code blockchain_transactions} lifecycle
     */
    public TradeExecutionService(BlockchainService blockchainService,
                                 TransactionService transactionService) {
        this.blockchainService  = blockchainService;
        this.transactionService = transactionService;
    }

    // =========================================================================
    // Sub-task 11.1 — submitOrderTransfer
    // =========================================================================

    /**
     * Submit a blockchain transfer for a marketplace order.
     *
     * <p>Full lifecycle:
     * <ol>
     *   <li>Ensure transaction tables exist.</li>
     *   <li>Open JDBC transaction; lock the order row with {@code SELECT … FOR UPDATE}.</li>
     *   <li>Validate {@code payment_status = CONFIRMED} and {@code status IN (PAID, SUBMITTED)}.</li>
     *   <li>Read wallet/batch/amount fields from the locked row.</li>
     *   <li>Verify seller has sufficient {@code wallet_batch_balances}.</li>
     *   <li>Idempotency guard: if {@code transfer_tx_hash} already set, commit and return.</li>
     *   <li>Create a PENDING {@code blockchain_transactions} row; commit the JDBC transaction.</li>
     *   <li>Call {@link BlockchainService#transferBatch}.</li>
     *   <li>On success: mark submitted, update order status; on failure: rollback, mark failed.</li>
     * </ol>
     *
     * @param orderId primary key of the marketplace order
     * @return result map with keys {@code txHash}, {@code orderId}, {@code status}
     * @throws RuntimeException if validation fails, balance is insufficient, or a DB/blockchain
     *                          error occurs
     * Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6
     */
    public Map<String, Object> submitOrderTransfer(int orderId) {
        // ── Ensure tables exist BEFORE opening the JDBC transaction ───────────
        transactionService.ensureTablesExist();

        Connection conn = null;
        PreparedStatement psLock   = null;
        PreparedStatement psBalance = null;
        PreparedStatement psUpdate  = null;
        ResultSet rs = null;

        int    txId           = 0;
        boolean txIdCreated   = false;

        try {
            conn = MyConnection.getConnection();
            conn.setAutoCommit(false);

            // ── Lock the order row ────────────────────────────────────────────
            psLock = conn.prepareStatement(
                "SELECT * FROM marketplace_orders WHERE id = ? FOR UPDATE"
            );
            psLock.setInt(1, orderId);
            rs = psLock.executeQuery();

            if (!rs.next()) {
                rollbackQuietly(conn);
                throw new RuntimeException("Order not found: id=" + orderId);
            }

            Map<String, Object> order = resultSetRowToMap(rs);
            closeQuietly(rs);
            rs = null;

            // ── Validate payment_status and status ────────────────────────────
            String paymentStatus = objectToString(order.get("payment_status"));
            String orderStatus   = objectToString(order.get("status"));

            if (!"CONFIRMED".equalsIgnoreCase(paymentStatus)) {
                rollbackQuietly(conn);
                throw new RuntimeException(
                    "Order " + orderId + " payment_status is '" + paymentStatus +
                    "', expected CONFIRMED");
            }

            boolean statusOk = "PAID".equalsIgnoreCase(orderStatus)
                            || "SUBMITTED".equalsIgnoreCase(orderStatus);
            if (!statusOk) {
                rollbackQuietly(conn);
                throw new RuntimeException(
                    "Order " + orderId + " status is '" + orderStatus +
                    "', expected PAID or SUBMITTED");
            }

            // ── Read order fields ─────────────────────────────────────────────
            int    buyerWalletId  = toInt(order.get("buyer_wallet_id"));
            int    sellerWalletId = toInt(order.get("seller_wallet_id"));
            int    batchId        = toInt(order.get("batch_id"));
            String amountBaseUnits = normalizeAmount(order.get("amount_base_units"));

            // ── Check seller balance ──────────────────────────────────────────
            psBalance = conn.prepareStatement(
                "SELECT COALESCE(SUM(balance_base_units), 0) " +
                "FROM wallet_batch_balances " +
                "WHERE wallet_id = ? AND batch_id = ?"
            );
            psBalance.setInt(1, sellerWalletId);
            psBalance.setInt(2, batchId);
            rs = psBalance.executeQuery();

            String available = "0";
            if (rs.next()) {
                Object val = rs.getObject(1);
                available = val != null ? val.toString() : "0";
            }
            closeQuietly(rs);
            rs = null;

            if (CreditUnitConverter.compareBaseUnits(available, amountBaseUnits) < 0) {
                rollbackQuietly(conn);
                throw new RuntimeException(
                    "Wallet " + sellerWalletId + " does not have enough available balance");
            }

            // ── Idempotency guard ─────────────────────────────────────────────
            String existingTxHash = objectToString(order.get("transfer_tx_hash"));
            if (existingTxHash != null && !existingTxHash.trim().isEmpty()) {
                conn.commit();
                Map<String, Object> idempotentResult = new LinkedHashMap<>();
                idempotentResult.put("txHash",     existingTxHash);
                idempotentResult.put("idempotent", true);
                return idempotentResult;
            }

            // ── Create pending transaction ────────────────────────────────────
            Map<String, Object> payloadMap = new LinkedHashMap<>();
            payloadMap.put("order_id",         orderId);
            payloadMap.put("seller_wallet_id", sellerWalletId);
            payloadMap.put("buyer_wallet_id",  buyerWalletId);
            payloadMap.put("batch_id",         batchId);
            payloadMap.put("amount_base_units", amountBaseUnits);

            // createPendingTransaction uses its own connection internally
            // Commit the JDBC lock transaction first so the order row is visible
            conn.commit();

            Map<String, Object> pendingTx = transactionService.createPendingTransaction(
                "TRANSFER",
                null,
                batchId,
                amountBaseUnits,
                payloadMap,
                sellerWalletId,
                buyerWalletId,
                null
            );

            if (pendingTx == null) {
                throw new RuntimeException(
                    "Failed to create pending transaction for order id=" + orderId);
            }
            txId        = toInt(pendingTx.get("id"));
            txIdCreated = true;

            // ── Call blockchainService.transferBatch ──────────────────────────
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("transaction_id", txId);
            metadata.put("order_id",       orderId);

            Map<String, Object> transferResult = blockchainService.transferBatch(
                sellerWalletId, buyerWalletId, batchId, amountBaseUnits, metadata
            );

            // ── On success: mark submitted, update order ──────────────────────
            String txHash    = objectToString(transferResult.get("txHash"));
            if (txHash == null || txHash.isEmpty()) {
                txHash = objectToString(transferResult.get("tx_hash"));
            }
            int blockNumber  = toInt(transferResult.get("blockNumber"));
            int logIndex     = toInt(transferResult.get("logIndex"));

            transactionService.markSubmitted(txId, txHash, blockNumber, logIndex);

            // UPDATE marketplace_orders
            Connection updateConn = null;
            PreparedStatement psOrderUpdate = null;
            try {
                updateConn = MyConnection.getConnection();
                psOrderUpdate = updateConn.prepareStatement(
                    "UPDATE marketplace_orders " +
                    "SET status = 'SUBMITTED', transfer_tx_hash = ?, updated_at = NOW() " +
                    "WHERE id = ?"
                );
                psOrderUpdate.setString(1, txHash);
                psOrderUpdate.setInt(2, orderId);
                psOrderUpdate.executeUpdate();
            } catch (SQLException e) {
                System.err.println("[TradeExecutionService] Failed to update order status: " + e.getMessage());
            } finally {
                closeQuietly(psOrderUpdate);
                closeQuietly(updateConn);
            }

            // ── Return result ─────────────────────────────────────────────────
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("txHash",  txHash);
            result.put("orderId", orderId);
            result.put("status",  "SUBMITTED");
            return result;

        } catch (RuntimeException e) {
            // Mark transaction as failed if we created one
            if (txIdCreated && txId > 0) {
                String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
                transactionService.markFailed(txId, errorMsg);
            }
            throw e;
        } catch (SQLException e) {
            rollbackQuietly(conn);
            if (txIdCreated && txId > 0) {
                transactionService.markFailed(txId, e.getMessage());
            }
            throw new RuntimeException("DB error in submitOrderTransfer: " + e.getMessage(), e);
        } finally {
            closeQuietly(rs);
            closeQuietly(psUpdate);
            closeQuietly(psBalance);
            closeQuietly(psLock);
            closeQuietly(conn);
        }
    }

    // =========================================================================
    // Sub-task 11.2 — retryOrderTransfer
    // =========================================================================

    /**
     * Retry a blockchain transfer for a marketplace order, with idempotency guard.
     *
     * <p>If {@code transfer_tx_hash} is already set on the order, or the order status
     * is {@code SUBMITTED} or {@code COMPLETED}, returns immediately without re-submitting.
     *
     * @param orderId primary key of the marketplace order
     * @return result map from {@link #submitOrderTransfer(int)}, or idempotent result
     * @throws RuntimeException if {@code payment_status != CONFIRMED} or a DB error occurs
     * Requirements: 9.7
     */
    public Map<String, Object> retryOrderTransfer(int orderId) {
        // ── Fetch order (no lock needed for the idempotency check) ────────────
        Map<String, Object> order = fetchOrder(orderId);
        if (order == null) {
            throw new RuntimeException("Order not found: id=" + orderId);
        }

        // ── Idempotency guard ─────────────────────────────────────────────────
        String existingTxHash = objectToString(order.get("transfer_tx_hash"));
        String orderStatus    = objectToString(order.get("status"));

        boolean alreadyDone = (existingTxHash != null && !existingTxHash.trim().isEmpty())
                           || "SUBMITTED".equalsIgnoreCase(orderStatus)
                           || "COMPLETED".equalsIgnoreCase(orderStatus);

        if (alreadyDone) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("txHash",     existingTxHash);
            result.put("idempotent", true);
            return result;
        }

        // ── Validate payment_status ───────────────────────────────────────────
        String paymentStatus = objectToString(order.get("payment_status"));
        if (!"CONFIRMED".equalsIgnoreCase(paymentStatus)) {
            throw new RuntimeException(
                "Order " + orderId + " payment_status is '" + paymentStatus +
                "', expected CONFIRMED");
        }

        // ── Delegate to submitOrderTransfer ───────────────────────────────────
        return submitOrderTransfer(orderId);
    }

    // =========================================================================
    // Sub-task 11.3 — retryPendingTransfers
    // =========================================================================

    /**
     * Retry all pending (PAID + CONFIRMED) marketplace orders up to {@code limit} rows.
     *
     * <p>Queries {@code marketplace_orders} for rows with {@code payment_status = CONFIRMED},
     * {@code status = PAID}, and no {@code transfer_tx_hash}, ordered by {@code updated_at ASC}.
     * Calls {@link #retryOrderTransfer(int)} for each; logs and continues on exception.
     *
     * @param limit maximum number of orders to process
     * @return summary map with keys {@code processed}, {@code submitted}, {@code failed}
     * Requirements: 9.8
     */
    public Map<String, Object> retryPendingTransfers(int limit) {
        List<Integer> orderIds = fetchPendingOrderIds(limit);

        int processed = 0;
        int submitted = 0;
        int failed    = 0;

        for (int orderId : orderIds) {
            processed++;
            try {
                Map<String, Object> result = retryOrderTransfer(orderId);
                // Count as submitted if not idempotent
                Boolean idempotent = (Boolean) result.get("idempotent");
                if (!Boolean.TRUE.equals(idempotent)) {
                    submitted++;
                }
            } catch (Exception e) {
                failed++;
                System.err.println(
                    "[TradeExecutionService] retryPendingTransfers: order " + orderId +
                    " failed: " + e.getMessage());
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("processed", processed);
        summary.put("submitted", submitted);
        summary.put("failed",    failed);
        return summary;
    }

    // =========================================================================
    // Sub-task 11.4 — Dev-mode inventory recovery (integrated into submitOrderTransfer)
    // =========================================================================

    /**
     * Submit a blockchain transfer for a marketplace order, with dev-mode inventory
     * recovery on insufficient-balance errors.
     *
     * <p>If {@link #submitOrderTransfer(int)} throws with a message containing
     * {@code "does not have enough available balance"} and dev mode is enabled,
     * this method:
     * <ol>
     *   <li>Fetches the batch row from {@code carbon_credit_batches}.</li>
     *   <li>Calls {@link BlockchainService#mintBatch} to reissue inventory for the seller.</li>
     *   <li>Retries {@link #submitOrderTransfer(int)} once.</li>
     *   <li>If the retry also fails, rethrows the original exception.</li>
     * </ol>
     *
     * <p>This method is the public entry point that wraps {@link #submitOrderTransfer(int)}
     * with the dev-mode recovery logic. Call this instead of {@link #submitOrderTransfer(int)}
     * directly when dev-mode recovery is desired.
     *
     * @param orderId primary key of the marketplace order
     * @return result map with keys {@code txHash}, {@code orderId}, {@code status}
     * @throws RuntimeException if the transfer fails and recovery is not applicable or also fails
     * Requirements: 9.9
     */
    public Map<String, Object> submitOrderTransferWithDevRecovery(int orderId) {
        try {
            return submitOrderTransfer(orderId);
        } catch (RuntimeException originalException) {
            String msg = originalException.getMessage();
            boolean isBalanceError = msg != null &&
                msg.contains("does not have enough available balance");

            if (!isBalanceError || !blockchainService.isDevModeEnabled()) {
                throw originalException;
            }

            // ── Dev-mode inventory recovery ───────────────────────────────────
            // Fetch the order to get seller_wallet_id and batch_id
            Map<String, Object> order = fetchOrder(orderId);
            if (order == null) {
                throw originalException;
            }

            int    sellerWalletId  = toInt(order.get("seller_wallet_id"));
            int    batchId         = toInt(order.get("batch_id"));
            String amountBaseUnits = normalizeAmount(order.get("amount_base_units"));

            // Fetch the batch to get total_amount_base_units
            Map<String, Object> batch = fetchBatch(batchId);
            String batchTotalBaseUnits = batch != null
                ? normalizeAmount(batch.get("total_amount_base_units"))
                : amountBaseUnits;  // fallback: use order amount

            // Reissue inventory via mintBatch
            Map<String, Object> mintMetadata = new LinkedHashMap<>();
            mintMetadata.put("order_id",         orderId);
            mintMetadata.put("recovery",         true);
            mintMetadata.put("seller_wallet_id", sellerWalletId);

            try {
                blockchainService.mintBatch(
                    sellerWalletId, batchId, batchTotalBaseUnits, mintMetadata
                );
            } catch (Exception mintException) {
                System.err.println(
                    "[TradeExecutionService] Dev-mode mintBatch recovery failed for order " +
                    orderId + ": " + mintException.getMessage());
                throw originalException;
            }

            // Retry submitOrderTransfer once
            try {
                return submitOrderTransfer(orderId);
            } catch (Exception retryException) {
                // Retry also failed — rethrow the original exception
                throw originalException;
            }
        }
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    /**
     * Fetch a marketplace order row by primary key (no lock).
     *
     * @param orderId order primary key
     * @return order row map, or {@code null} if not found
     */
    private Map<String, Object> fetchOrder(int orderId) {
        String sql = "SELECT * FROM marketplace_orders WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = MyConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, orderId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return resultSetRowToMap(rs);
            }
            return null;
        } catch (SQLException e) {
            System.err.println("[TradeExecutionService] fetchOrder failed: " + e.getMessage());
            return null;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
    }

    /**
     * Fetch a carbon credit batch row by primary key.
     *
     * @param batchId batch primary key
     * @return batch row map, or {@code null} if not found
     */
    private Map<String, Object> fetchBatch(int batchId) {
        String sql = "SELECT * FROM carbon_credit_batches WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = MyConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, batchId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return resultSetRowToMap(rs);
            }
            return null;
        } catch (SQLException e) {
            System.err.println("[TradeExecutionService] fetchBatch failed: " + e.getMessage());
            return null;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
    }

    /**
     * Fetch order IDs eligible for retry: {@code payment_status = CONFIRMED},
     * {@code status = PAID}, and no {@code transfer_tx_hash}, ordered by
     * {@code updated_at ASC}, limited to {@code limit} rows.
     *
     * @param limit maximum number of rows to return
     * @return list of order IDs
     */
    private List<Integer> fetchPendingOrderIds(int limit) {
        String sql =
            "SELECT id FROM marketplace_orders " +
            "WHERE payment_status = 'CONFIRMED' " +
            "  AND status = 'PAID' " +
            "  AND (transfer_tx_hash IS NULL OR transfer_tx_hash = '') " +
            "ORDER BY updated_at ASC " +
            "LIMIT ?";

        List<Integer> ids = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = MyConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, limit);
            rs = ps.executeQuery();
            while (rs.next()) {
                ids.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            System.err.println("[TradeExecutionService] fetchPendingOrderIds failed: " + e.getMessage());
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return ids;
    }

    /**
     * Normalize a base-unit amount from a map value, treating null as {@code "0"}.
     *
     * @param value raw value from the result-set map
     * @return normalized base-unit string
     */
    private String normalizeAmount(Object value) {
        if (value == null) return "0";
        return CreditUnitConverter.normalizeBaseUnits(value.toString());
    }

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
        if (value instanceof String) {
            try {
                return Integer.parseInt(((String) value).trim());
            } catch (NumberFormatException ignored) {}
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
