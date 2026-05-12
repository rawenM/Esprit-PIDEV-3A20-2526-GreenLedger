package Services;

import DataBase.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * THE ONLY component in the Java application permitted to write
 * {@code wallet.available_credits} or {@code wallet.retired_credits}.
 *
 * <p>All confirmed blockchain events (Mint, Transfer, Retire) are applied here
 * in an idempotent fashion: each event is checked against
 * {@code blockchain_transactions} before any mutation is performed.
 *
 * <p>Dev-mode simulation methods ({@code simulateMint}, {@code simulateTransfer},
 * {@code simulateRetire}) apply the same mutations but skip the idempotency
 * check, for use by {@code BlockchainService.buildSimulatedResult()} only.
 *
 * <p>All database access uses {@link MyConnection#getConnection()} (plain JDBC,
 * no DI). Connections, statements, and result-sets are always closed in
 * {@code finally} blocks.
 *
 * Requirements: 7.1 – 7.11
 */
public class EventListenerService {

    private final TransactionService transactionService;

    /**
     * Construct an {@code EventListenerService} backed by the given
     * {@link TransactionService}.
     *
     * @param transactionService the transaction-lifecycle service
     */
    public EventListenerService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // =========================================================================
    // Sub-task 5.1 — Idempotency guard
    // =========================================================================

    /**
     * Return {@code true} if a CONFIRMED blockchain transaction with the given
     * hash and type already exists in {@code blockchain_transactions}.
     *
     * <p>SQL: {@code SELECT COUNT(*) FROM blockchain_transactions
     * WHERE tx_hash = ? AND type = ? AND status = 'CONFIRMED'}
     *
     * @param txHash    on-chain transaction hash
     * @param eventType event type string (e.g. "MINT", "TRANSFER", "RETIRE")
     * @return {@code true} if the event has already been applied
     * Requirements: 7.10
     */
    public boolean isEventAlreadyApplied(String txHash, String eventType) {
        String sql =
            "SELECT COUNT(*) FROM blockchain_transactions " +
            "WHERE tx_hash = ? AND type = ? AND status = 'CONFIRMED'";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = MyConnection.getConnection();
            if (conn == null) return false;
            ps = conn.prepareStatement(sql);
            ps.setString(1, txHash);
            ps.setString(2, eventType);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("[EventListenerService] isEventAlreadyApplied failed: " + e.getMessage());
            return false;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
    }

    // =========================================================================
    // Sub-task 5.2 — upsertWalletBatchBalance
    // =========================================================================

    /**
     * Insert or update the {@code wallet_batch_balances} row for the given
     * {@code (walletId, batchId)} pair.
     *
     * <ul>
     *   <li>If a row already exists: {@code UPDATE … SET balance = GREATEST(0, balance + delta),
     *       balance_base_units = GREATEST(0, balance_base_units + deltaBaseUnits)}</li>
     *   <li>If no row exists: {@code INSERT … VALUES (walletId, batchId,
     *       GREATEST(0, delta), GREATEST(0, deltaBaseUnits))}</li>
     * </ul>
     *
     * @param walletId       wallet identifier
     * @param batchId        carbon credit batch identifier
     * @param delta          human-readable credit delta (may be negative for deductions)
     * @param deltaBaseUnits on-chain base-unit delta string (may be negative)
     * Requirements: 7.9
     */
    public void upsertWalletBatchBalance(int walletId, int batchId, double delta, String deltaBaseUnits) {
        // Check whether a row already exists
        String selectSql =
            "SELECT 1 FROM wallet_batch_balances WHERE wallet_id = ? AND batch_id = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = MyConnection.getConnection();
            if (conn == null) return;

            ps = conn.prepareStatement(selectSql);
            ps.setInt(1, walletId);
            ps.setInt(2, batchId);
            rs = ps.executeQuery();
            boolean exists = rs.next();
            closeQuietly(rs);
            rs = null;
            closeQuietly(ps);
            ps = null;

            if (exists) {
                // UPDATE existing row — floor at 0 via GREATEST
                String updateSql =
                    "UPDATE wallet_batch_balances " +
                    "SET balance = GREATEST(0, balance + ?), " +
                    "    balance_base_units = GREATEST(0, balance_base_units + ?) " +
                    "WHERE wallet_id = ? AND batch_id = ?";
                ps = conn.prepareStatement(updateSql);
                ps.setDouble(1, delta);
                ps.setString(2, deltaBaseUnits);
                ps.setInt(3, walletId);
                ps.setInt(4, batchId);
                ps.executeUpdate();
            } else {
                // INSERT new row — floor at 0 via GREATEST
                String insertSql =
                    "INSERT INTO wallet_batch_balances " +
                    "  (wallet_id, batch_id, balance, balance_base_units) " +
                    "VALUES (?, ?, GREATEST(0, ?), GREATEST(0, ?))";
                ps = conn.prepareStatement(insertSql);
                ps.setInt(1, walletId);
                ps.setInt(2, batchId);
                ps.setDouble(3, delta);
                ps.setString(4, deltaBaseUnits);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("[EventListenerService] upsertWalletBatchBalance failed: " + e.getMessage());
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
    }

    // =========================================================================
    // Sub-task 5.3 — applyMintEvent
    // =========================================================================

    /**
     * Apply a confirmed Mint blockchain event to the database.
     *
     * <ol>
     *   <li>Idempotency check — return immediately if already applied.</li>
     *   <li>{@code UPDATE wallet SET available_credits = available_credits + ? WHERE id = ?}</li>
     *   <li>Upsert {@code wallet_batch_balances} (positive delta).</li>
     *   <li>{@code UPDATE carbon_credit_batches SET status = 'active' WHERE id = ?}</li>
     *   <li>Call {@code transactionService.markConfirmedByTxHash(txHash, 0, 0)}.</li>
     *   <li>Insert a {@code MINT} row into {@code wallet_transactions}.</li>
     * </ol>
     *
     * @param txHash          on-chain transaction hash
     * @param batchId         carbon credit batch identifier
     * @param walletId        recipient wallet identifier
     * @param amount          human-readable credit amount
     * @param amountBaseUnits on-chain base-unit amount string
     * Requirements: 7.1, 7.2, 7.5
     */
    public void applyMintEvent(String txHash, int batchId, int walletId,
                               double amount, String amountBaseUnits) {
        if (isEventAlreadyApplied(txHash, "MINT")) {
            return;
        }
        applyMintEventBody(txHash, batchId, walletId, amount, amountBaseUnits);
    }

    /**
     * Internal body of the mint event — shared by {@link #applyMintEvent} and
     * {@link #simulateMint}.
     */
    private void applyMintEventBody(String txHash, int batchId, int walletId,
                                    double amount, String amountBaseUnits) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = MyConnection.getConnection();
            if (conn == null) return;

            // 1. Increment wallet.available_credits
            String updateWallet =
                "UPDATE wallet SET available_credits = available_credits + ? WHERE id = ?";
            ps = conn.prepareStatement(updateWallet);
            ps.setDouble(1, amount);
            ps.setInt(2, walletId);
            ps.executeUpdate();
            closeQuietly(ps);
            ps = null;

            // 2. Upsert wallet_batch_balances
            upsertWalletBatchBalance(walletId, batchId, amount, amountBaseUnits);

            // 3. Set batch status to 'active'
            String updateBatch =
                "UPDATE carbon_credit_batches SET status = 'active' WHERE id = ?";
            ps = conn.prepareStatement(updateBatch);
            ps.setInt(1, batchId);
            ps.executeUpdate();
            closeQuietly(ps);
            ps = null;

            // 4. Mark blockchain transaction as CONFIRMED
            transactionService.markConfirmedByTxHash(txHash, 0, 0);

            // 5. Insert wallet_transactions record
            String insertWalletTx =
                "INSERT INTO wallet_transactions (wallet_id, type, amount, tx_hash, created_at) " +
                "VALUES (?, 'MINT', ?, ?, NOW())";
            ps = conn.prepareStatement(insertWalletTx);
            ps.setInt(1, walletId);
            ps.setDouble(2, amount);
            ps.setString(3, txHash);
            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[EventListenerService] applyMintEventBody failed: " + e.getMessage());
        } finally {
            closeQuietly(ps);
            closeQuietly(conn);
        }
    }

    // =========================================================================
    // Sub-task 5.4 — applyTransferEvent
    // =========================================================================

    /**
     * Apply a confirmed Transfer blockchain event to the database.
     *
     * <ol>
     *   <li>Idempotency check — return immediately if already applied.</li>
     *   <li>Decrement {@code wallet.available_credits} for {@code fromWalletId} (floor at 0).</li>
     *   <li>Increment {@code wallet.available_credits} for {@code toWalletId}.</li>
     *   <li>Upsert {@code wallet_batch_balances} for both wallets.</li>
     *   <li>Call {@code transactionService.markConfirmedByTxHash(txHash, 0, 0)}.</li>
     *   <li>Update {@code marketplace_orders} to COMPLETED where applicable.</li>
     *   <li>Insert {@code TRANSFER_OUT} and {@code TRANSFER_IN} rows into
     *       {@code wallet_transactions}.</li>
     * </ol>
     *
     * @param txHash          on-chain transaction hash
     * @param batchId         carbon credit batch identifier
     * @param fromWalletId    seller wallet identifier
     * @param toWalletId      buyer wallet identifier
     * @param amount          human-readable credit amount
     * @param amountBaseUnits on-chain base-unit amount string
     * Requirements: 7.1, 7.3, 7.6
     */
    public void applyTransferEvent(String txHash, int batchId, int fromWalletId,
                                   int toWalletId, double amount, String amountBaseUnits) {
        if (isEventAlreadyApplied(txHash, "TRANSFER")) {
            return;
        }
        applyTransferEventBody(txHash, batchId, fromWalletId, toWalletId, amount, amountBaseUnits);
    }

    /**
     * Internal body of the transfer event — shared by {@link #applyTransferEvent}
     * and {@link #simulateTransfer}.
     */
    private void applyTransferEventBody(String txHash, int batchId, int fromWalletId,
                                        int toWalletId, double amount, String amountBaseUnits) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = MyConnection.getConnection();
            if (conn == null) return;

            // 1. Decrement seller wallet.available_credits (floor at 0)
            String debitSeller =
                "UPDATE wallet SET available_credits = GREATEST(0, available_credits - ?) WHERE id = ?";
            ps = conn.prepareStatement(debitSeller);
            ps.setDouble(1, amount);
            ps.setInt(2, fromWalletId);
            ps.executeUpdate();
            closeQuietly(ps);
            ps = null;

            // 2. Increment buyer wallet.available_credits
            String creditBuyer =
                "UPDATE wallet SET available_credits = available_credits + ? WHERE id = ?";
            ps = conn.prepareStatement(creditBuyer);
            ps.setDouble(1, amount);
            ps.setInt(2, toWalletId);
            ps.executeUpdate();
            closeQuietly(ps);
            ps = null;

            // 3. Upsert wallet_batch_balances — negative delta for seller
            upsertWalletBatchBalance(fromWalletId, batchId, -amount, "-" + amountBaseUnits);
            // Positive delta for buyer
            upsertWalletBatchBalance(toWalletId, batchId, amount, amountBaseUnits);

            // 4. Mark blockchain transaction as CONFIRMED
            transactionService.markConfirmedByTxHash(txHash, 0, 0);

            // 5. Complete matching marketplace orders
            String updateOrders =
                "UPDATE marketplace_orders SET status = 'COMPLETED' " +
                "WHERE transfer_tx_hash = ? AND status IN ('PAID','SUBMITTED')";
            ps = conn.prepareStatement(updateOrders);
            ps.setString(1, txHash);
            ps.executeUpdate();
            closeQuietly(ps);
            ps = null;

            // 6. Insert TRANSFER_OUT for seller
            String insertOut =
                "INSERT INTO wallet_transactions (wallet_id, type, amount, tx_hash, created_at) " +
                "VALUES (?, 'TRANSFER_OUT', ?, ?, NOW())";
            ps = conn.prepareStatement(insertOut);
            ps.setInt(1, fromWalletId);
            ps.setDouble(2, amount);
            ps.setString(3, txHash);
            ps.executeUpdate();
            closeQuietly(ps);
            ps = null;

            // 7. Insert TRANSFER_IN for buyer
            String insertIn =
                "INSERT INTO wallet_transactions (wallet_id, type, amount, tx_hash, created_at) " +
                "VALUES (?, 'TRANSFER_IN', ?, ?, NOW())";
            ps = conn.prepareStatement(insertIn);
            ps.setInt(1, toWalletId);
            ps.setDouble(2, amount);
            ps.setString(3, txHash);
            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[EventListenerService] applyTransferEventBody failed: " + e.getMessage());
        } finally {
            closeQuietly(ps);
            closeQuietly(conn);
        }
    }

    // =========================================================================
    // Sub-task 5.5 — applyRetireEvent
    // =========================================================================

    /**
     * Apply a confirmed Retire blockchain event to the database.
     *
     * <ol>
     *   <li>Idempotency check — return immediately if already applied.</li>
     *   <li>Decrement {@code wallet.available_credits} for {@code walletId} (floor at 0).</li>
     *   <li>Upsert {@code wallet_batch_balances} (negative delta).</li>
     *   <li>{@code UPDATE carbon_credit_batches SET status = 'retired' WHERE id = ?}</li>
     *   <li>Call {@code transactionService.markConfirmedByTxHash(txHash, 0, 0)}.</li>
     *   <li>Insert a {@code RETIRE} row into {@code wallet_transactions}.</li>
     * </ol>
     *
     * @param txHash          on-chain transaction hash
     * @param batchId         carbon credit batch identifier
     * @param walletId        retiring wallet identifier
     * @param amount          human-readable credit amount
     * @param amountBaseUnits on-chain base-unit amount string
     * Requirements: 7.1, 7.4, 7.7
     */
    public void applyRetireEvent(String txHash, int batchId, int walletId,
                                 double amount, String amountBaseUnits) {
        if (isEventAlreadyApplied(txHash, "RETIRE")) {
            return;
        }
        applyRetireEventBody(txHash, batchId, walletId, amount, amountBaseUnits);
    }

    /**
     * Internal body of the retire event — shared by {@link #applyRetireEvent}
     * and {@link #simulateRetire}.
     */
    private void applyRetireEventBody(String txHash, int batchId, int walletId,
                                      double amount, String amountBaseUnits) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = MyConnection.getConnection();
            if (conn == null) return;

            // 1. Decrement wallet.available_credits (floor at 0)
            String updateWallet =
                "UPDATE wallet SET available_credits = GREATEST(0, available_credits - ?) WHERE id = ?";
            ps = conn.prepareStatement(updateWallet);
            ps.setDouble(1, amount);
            ps.setInt(2, walletId);
            ps.executeUpdate();
            closeQuietly(ps);
            ps = null;

            // 2. Upsert wallet_batch_balances — negative delta
            upsertWalletBatchBalance(walletId, batchId, -amount, "-" + amountBaseUnits);

            // 3. Set batch status to 'retired'
            String updateBatch =
                "UPDATE carbon_credit_batches SET status = 'retired' WHERE id = ?";
            ps = conn.prepareStatement(updateBatch);
            ps.setInt(1, batchId);
            ps.executeUpdate();
            closeQuietly(ps);
            ps = null;

            // 4. Mark blockchain transaction as CONFIRMED
            transactionService.markConfirmedByTxHash(txHash, 0, 0);

            // 5. Insert wallet_transactions record
            String insertWalletTx =
                "INSERT INTO wallet_transactions (wallet_id, type, amount, tx_hash, created_at) " +
                "VALUES (?, 'RETIRE', ?, ?, NOW())";
            ps = conn.prepareStatement(insertWalletTx);
            ps.setInt(1, walletId);
            ps.setDouble(2, amount);
            ps.setString(3, txHash);
            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[EventListenerService] applyRetireEventBody failed: " + e.getMessage());
        } finally {
            closeQuietly(ps);
            closeQuietly(conn);
        }
    }

    // =========================================================================
    // Sub-task 5.6 — Dev-mode simulation methods
    // =========================================================================

    /**
     * Simulate a Mint event in dev mode, skipping the idempotency check.
     *
     * <p>Computes {@code amountBaseUnits} via
     * {@link CreditUnitConverter#toBaseUnits(String)} and delegates to the
     * mint event body directly.
     *
     * @param txHash   dev-mode transaction hash (prefixed with {@code dev_})
     * @param batchId  carbon credit batch identifier
     * @param walletId recipient wallet identifier
     * @param amount   human-readable credit amount
     * Requirements: 7.8
     */
    public void simulateMint(String txHash, int batchId, int walletId, double amount) {
        String amountBaseUnits = CreditUnitConverter.toBaseUnits(String.valueOf(amount));
        applyMintEventBody(txHash, batchId, walletId, amount, amountBaseUnits);
    }

    /**
     * Simulate a Transfer event in dev mode, skipping the idempotency check.
     *
     * @param txHash       dev-mode transaction hash
     * @param batchId      carbon credit batch identifier
     * @param fromWalletId seller wallet identifier
     * @param toWalletId   buyer wallet identifier
     * @param amount       human-readable credit amount
     * Requirements: 7.8
     */
    public void simulateTransfer(String txHash, int batchId, int fromWalletId,
                                 int toWalletId, double amount) {
        String amountBaseUnits = CreditUnitConverter.toBaseUnits(String.valueOf(amount));
        applyTransferEventBody(txHash, batchId, fromWalletId, toWalletId, amount, amountBaseUnits);
    }

    /**
     * Simulate a Retire event in dev mode, skipping the idempotency check.
     *
     * @param txHash   dev-mode transaction hash
     * @param batchId  carbon credit batch identifier
     * @param walletId retiring wallet identifier
     * @param amount   human-readable credit amount
     * Requirements: 7.8
     */
    public void simulateRetire(String txHash, int batchId, int walletId, double amount) {
        String amountBaseUnits = CreditUnitConverter.toBaseUnits(String.valueOf(amount));
        applyRetireEventBody(txHash, batchId, walletId, amount, amountBaseUnits);
    }

    // =========================================================================
    // Sub-task 5.7 — backfillWalletSummaryColumns
    // =========================================================================

    /**
     * Recalculate {@code wallet.available_credits} for every wallet by summing
     * the {@code balance} column from {@code wallet_batch_balances} grouped by
     * {@code wallet_id}, and updating the {@code wallet} table accordingly.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>SELECT all wallet IDs from {@code wallet}.</li>
     *   <li>For each wallet ID: {@code SELECT COALESCE(SUM(balance), 0) FROM
     *       wallet_batch_balances WHERE wallet_id = ?}</li>
     *   <li>{@code UPDATE wallet SET available_credits = ? WHERE id = ?}</li>
     * </ol>
     *
     * Requirements: 7.11
     */
    public void backfillWalletSummaryColumns() {
        // Step 1: Collect all wallet IDs
        List<Integer> walletIds = new ArrayList<>();

        Connection conn = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            conn = MyConnection.getConnection();
            if (conn == null) return;

            st = conn.createStatement();
            rs = st.executeQuery("SELECT id FROM wallet");
            while (rs.next()) {
                walletIds.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            System.err.println("[EventListenerService] backfillWalletSummaryColumns (fetch IDs) failed: "
                + e.getMessage());
            return;
        } finally {
            closeQuietly(rs);
            closeQuietly(st);
            closeQuietly(conn);
        }

        // Step 2 & 3: For each wallet, sum batch balances and update wallet row
        String sumSql =
            "SELECT COALESCE(SUM(balance), 0) FROM wallet_batch_balances WHERE wallet_id = ?";
        String updateSql =
            "UPDATE wallet SET available_credits = ? WHERE id = ?";

        for (int walletId : walletIds) {
            Connection c = null;
            PreparedStatement psSum = null;
            PreparedStatement psUpdate = null;
            ResultSet rsSum = null;
            try {
                c = MyConnection.getConnection();
                if (c == null) continue;

                psSum = c.prepareStatement(sumSql);
                psSum.setInt(1, walletId);
                rsSum = psSum.executeQuery();

                double totalBalance = 0.0;
                if (rsSum.next()) {
                    totalBalance = rsSum.getDouble(1);
                }
                closeQuietly(rsSum);
                rsSum = null;
                closeQuietly(psSum);
                psSum = null;

                psUpdate = c.prepareStatement(updateSql);
                psUpdate.setDouble(1, totalBalance);
                psUpdate.setInt(2, walletId);
                psUpdate.executeUpdate();

            } catch (SQLException e) {
                System.err.println("[EventListenerService] backfillWalletSummaryColumns (wallet "
                    + walletId + ") failed: " + e.getMessage());
            } finally {
                closeQuietly(rsSum);
                closeQuietly(psSum);
                closeQuietly(psUpdate);
                closeQuietly(c);
            }
        }
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

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
