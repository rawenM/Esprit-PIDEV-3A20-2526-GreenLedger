package Services;

import Models.*;
import DataBase.MyConnection;
import Utils.MarketplaceLogger;

import java.sql.*;
import java.util.*;

/**
 * Ensures data consistency across marketplace operations
 * Verifies relationships, orphaned records, and transaction integrity
 */
public class MarketplaceDataConsistencyService {
    private static final String LOG_TAG = "[MarketplaceDataConsistencyService]";
    private static MarketplaceDataConsistencyService instance;
    private Connection conn;

    private MarketplaceDataConsistencyService() {
        this.conn = MyConnection.getConnection();
    }

    public static MarketplaceDataConsistencyService getInstance() {
        if (instance == null) {
            instance = new MarketplaceDataConsistencyService();
        }
        return instance;
    }

    /**
     * Check and log all marketplace data consistency issues
     */
    public ConsistencyReport runFullCheck() {
        ConsistencyReport report = new ConsistencyReport();

        if (conn == null) {
            System.err.println(LOG_TAG + " ERROR: No database connection");
            return report;
        }

        // Run all checks
        report.appendCheck("Orders without listings", checkOrdersWithoutListings());
        report.appendCheck("Listings without sellers", checkListingsWithoutSellers());
        report.appendCheck("Orders without escrow", checkOrdersWithoutEscrow());
        report.appendCheck("Escrow without orders", checkEscrowWithoutOrders());
        report.appendCheck("Fees without orders", checkFeesWithoutOrders());
        report.appendCheck("Disputes without escrow", checkDisputesWithoutEscrow());
        report.appendCheck("Unmatched transactions", checkUnmatchedTransactions());
        report.appendCheck("Duplicate orders", checkDuplicateOrders());
        report.appendCheck("Balance inconsistencies", checkWalletBalanceInconsistencies());

        System.out.println(LOG_TAG + " Data consistency check complete: " + 
            report.getTotalIssues() + " issues found");

        return report;
    }

    /**
     * Find orders that reference non-existent listings
     */
    private int checkOrdersWithoutListings() {
        try {
            String sql = "SELECT COUNT(*) as count FROM marketplace_orders o " +
                "LEFT JOIN marketplace_listings l ON o.listing_id = l.id " +
                "WHERE l.id IS NULL";

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    int count = rs.getInt("count");
                    if (count > 0) {
                        System.err.println(LOG_TAG + " WARNING: Found " + count + 
                            " orders with missing listings");
                        MarketplaceLogger.logError("DataConsistency", 
                            count + " orders reference deleted listings");
                    }
                    return count;
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR checking orders: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Find listings that reference non-existent sellers
     */
    private int checkListingsWithoutSellers() {
        try {
            String sql = "SELECT COUNT(*) as count FROM marketplace_listings l " +
                "LEFT JOIN user u ON l.seller_id = u.id " +
                "WHERE u.id IS NULL";

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    int count = rs.getInt("count");
                    if (count > 0) {
                        System.err.println(LOG_TAG + " WARNING: Found " + count + 
                            " listings with missing sellers");
                    }
                    return count;
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR checking listings: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Find paid orders without escrow records
     */
    private int checkOrdersWithoutEscrow() {
        try {
            String sql = "SELECT COUNT(*) as count FROM marketplace_orders o " +
                "WHERE o.status = 'PAID' AND o.escrow_id IS NULL";

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    int count = rs.getInt("count");
                    if (count > 0) {
                        System.err.println(LOG_TAG + " WARNING: Found " + count + 
                            " paid orders without escrow");
                        MarketplaceLogger.logError("DataConsistency", 
                            count + " paid orders missing escrow holds");
                    }
                    return count;
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR checking escrow: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Find orphaned escrow records (no corresponding order)
     */
    private int checkEscrowWithoutOrders() {
        try {
            String sql = "SELECT COUNT(*) as count FROM marketplace_escrow e " +
                "LEFT JOIN marketplace_orders o ON e.order_id = o.id " +
                "WHERE o.id IS NULL AND e.order_id IS NOT NULL";

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    int count = rs.getInt("count");
                    if (count > 0) {
                        System.err.println(LOG_TAG + " WARNING: Found " + count + 
                            " orphaned escrow records");
                    }
                    return count;
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR checking orphaned escrow: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Find fees without matching orders
     */
    private int checkFeesWithoutOrders() {
        try {
            String sql = "SELECT COUNT(*) as count FROM marketplace_fees f " +
                "LEFT JOIN marketplace_orders o ON f.order_id = o.id " +
                "WHERE o.id IS NULL AND f.order_id IS NOT NULL";

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    int count = rs.getInt("count");
                    if (count > 0) {
                        System.err.println(LOG_TAG + " WARNING: Found " + count + 
                            " fees without orders");
                    }
                    return count;
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR checking fees: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Find disputes without escrow records
     */
    private int checkDisputesWithoutEscrow() {
        try {
            String sql = "SELECT COUNT(*) as count FROM marketplace_disputes d " +
                "LEFT JOIN marketplace_escrow e ON d.escrow_id = e.id " +
                "WHERE e.id IS NULL";

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    int count = rs.getInt("count");
                    if (count > 0) {
                        System.err.println(LOG_TAG + " WARNING: Found " + count + 
                            " disputes without escrow");
                    }
                    return count;
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR checking disputes: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Verify all payments are logged to wallet_transactions
     */
    private int checkUnmatchedTransactions() {
        try {
            String sql = "SELECT COUNT(*) as count FROM marketplace_orders o " +
                "LEFT JOIN wallet_transactions wt ON o.id = wt.order_id " +
                "WHERE o.status = 'COMPLETED' AND wt.id IS NULL";

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    int count = rs.getInt("count");
                    if (count > 0) {
                        System.err.println(LOG_TAG + " WARNING: Found " + count + 
                            " completed orders without wallet transactions");
                    }
                    return count;
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR checking transactions: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Check for duplicate orders (same buyer/listing/timestamp)
     */
    private int checkDuplicateOrders() {
        try {
            String sql = "SELECT COUNT(*) as count FROM (" +
                "SELECT buyer_id, listing_id, COUNT(*) as cnt " +
                "FROM marketplace_orders " +
                "WHERE created_at > DATE_SUB(NOW(), INTERVAL 1 MINUTE) " +
                "GROUP BY buyer_id, listing_id HAVING cnt > 1) as dupes";

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    int count = rs.getInt("count");
                    if (count > 0) {
                        System.err.println(LOG_TAG + " WARNING: Found " + count + 
                            " potential duplicate orders");
                    }
                    return count;
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR checking duplicates: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Check wallet balance consistency with transaction log
     */
    private int checkWalletBalanceInconsistencies() {
        try {
            String sql = "SELECT w.id, w.available_credits, " +
                "COALESCE(SUM(CASE WHEN wt.type = 'ISSUE' THEN wt.amount " +
                "WHEN wt.type = 'RETIRE' THEN -wt.amount ELSE 0 END), 0) as calculated " +
                "FROM wallet w " +
                "LEFT JOIN wallet_transactions wt ON w.id = wt.wallet_id " +
                "GROUP BY w.id HAVING ABS(w.available_credits - calculated) > 0.01";

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                int count = 0;
                while (rs.next()) {
                    count++;
                    int walletId = rs.getInt("id");
                    double actual = rs.getDouble("available_credits");
                    double calculated = rs.getDouble("calculated");
                    System.err.println(LOG_TAG + " Wallet " + walletId + 
                        " balance mismatch: recorded=" + actual + ", calculated=" + calculated);
                    MarketplaceLogger.logError("DataConsistency", 
                        "Wallet " + walletId + " balance inconsistency");
                }
                if (count > 0) {
                    System.err.println(LOG_TAG + " WARNING: Found " + count + 
                        " wallets with balance inconsistencies");
                }
                return count;
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR checking wallet consistency: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Report class for consistency check results
     */
    public static class ConsistencyReport {
        private final List<CheckResult> results = new ArrayList<>();

        private void appendCheck(String checkName, int issuesFound) {
            results.add(new CheckResult(checkName, issuesFound));
        }

        public int getTotalIssues() {
            return results.stream().mapToInt(r -> r.issuesFound).sum();
        }

        public List<CheckResult> getResults() {
            return new ArrayList<>(results);
        }

        public void printReport() {
            System.out.println("\n=== Marketplace Data Consistency Report ===");
            for (CheckResult result : results) {
                String status = result.issuesFound == 0 ? "✓ PASS" : "✗ FAIL";
                System.out.println(status + ": " + result.checkName + " (" + result.issuesFound + " issues)");
            }
            System.out.println("Total issues: " + getTotalIssues());
            System.out.println("==========================================\n");
        }
    }

    public static class CheckResult {
        public final String checkName;
        public final int issuesFound;

        CheckResult(String checkName, int issuesFound) {
            this.checkName = checkName;
            this.issuesFound = issuesFound;
        }
    }
}
