package Services;

import Models.MarketplaceFee;
import DataBase.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

/**
 * Service for tracking marketplace platform fees
 * Records fees from transactions, enables financial reporting
 */
public class MarketplaceFeeService {
    private static final String LOG_TAG = "[MarketplaceFeeService]";
    private static MarketplaceFeeService instance;
    private Connection conn;

    // Platform fee percentage (e.g., 2.5% default)
    private static final BigDecimal DEFAULT_FEE_PERCENTAGE = new BigDecimal("2.5");

    private MarketplaceFeeService() {
        this.conn = MyConnection.getConnection();
    }

    public static MarketplaceFeeService getInstance() {
        if (instance == null) {
            instance = new MarketplaceFeeService();
        }
        return instance;
    }

    /**
     * Record a fee from a marketplace transaction
     */
    public int recordFee(Integer orderId, Integer tradeId, long sellerId, BigDecimal transactionAmount,
                        BigDecimal feePercentage, String feeType) {
        if (conn == null) return -1;

        BigDecimal feeAmount = transactionAmount.multiply(feePercentage).divide(new BigDecimal("100"));

        String sql = "INSERT INTO marketplace_fees " +
            "(order_id, trade_id, seller_id, transaction_amount, fee_percentage, fee_amount, fee_type) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setObject(1, orderId);
            stmt.setObject(2, tradeId);
            stmt.setLong(3, sellerId);
            stmt.setBigDecimal(4, transactionAmount);
            stmt.setBigDecimal(5, feePercentage);
            stmt.setBigDecimal(6, feeAmount);
            stmt.setString(7, feeType);
            
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int feeId = rs.getInt(1);
                    System.out.println(LOG_TAG + " Fee recorded: ID " + feeId + 
                        ", Amount: $" + feeAmount + " from transaction: $" + transactionAmount);
                    return feeId;
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR recording fee: " + e.getMessage());
        }

        return -1;
    }

    /**
     * Record fee using default platform percentage
     */
    public int recordFee(Integer orderId, Integer tradeId, long sellerId, 
                        BigDecimal transactionAmount, String feeType) {
        return recordFee(orderId, tradeId, sellerId, transactionAmount, DEFAULT_FEE_PERCENTAGE, feeType);
    }

    /**
     * Get fees by order
     */
    public List<MarketplaceFee> getFeesByOrder(int orderId) {
        List<MarketplaceFee> fees = new ArrayList<>();
        if (conn == null) return fees;

        String sql = "SELECT * FROM marketplace_fees WHERE order_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orderId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    fees.add(mapResultToFee(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR fetching fees: " + e.getMessage());
        }

        return fees;
    }

    /**
     * Get fees by seller
     */
    public List<MarketplaceFee> getFeesBySeller(long sellerId) {
        List<MarketplaceFee> fees = new ArrayList<>();
        if (conn == null) return fees;

        String sql = "SELECT * FROM marketplace_fees WHERE seller_id = ? ORDER BY created_at DESC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, sellerId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    fees.add(mapResultToFee(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR fetching seller fees: " + e.getMessage());
        }

        return fees;
    }

    /**
     * Get total fees collected (all-time)
     */
    public BigDecimal getTotalFeesCollected() {
        if (conn == null) return BigDecimal.ZERO;

        String sql = "SELECT COALESCE(SUM(fee_amount), 0) as total FROM marketplace_fees";

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getBigDecimal("total");
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR calculating total fees: " + e.getMessage());
        }

        return BigDecimal.ZERO;
    }

    /**
     * Get total fees for a time period
     */
    public BigDecimal getFeesForPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        if (conn == null) return BigDecimal.ZERO;

        String sql = "SELECT COALESCE(SUM(fee_amount), 0) as total FROM marketplace_fees " +
            "WHERE created_at BETWEEN ? AND ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(startDate));
            stmt.setTimestamp(2, Timestamp.valueOf(endDate));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("total");
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR calculating period fees: " + e.getMessage());
        }

        return BigDecimal.ZERO;
    }

    /**
     * Get fees by type
     */
    public List<MarketplaceFee> getFeesByType(String feeType) {
        List<MarketplaceFee> fees = new ArrayList<>();
        if (conn == null) return fees;

        String sql = "SELECT * FROM marketplace_fees WHERE fee_type = ? ORDER BY created_at DESC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, feeType);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    fees.add(mapResultToFee(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR fetching fees by type: " + e.getMessage());
        }

        return fees;
    }

    /**
     * Get monthly fee summary
     */
    public List<MonthlyFeeSummary> getMonthlyFeeSummary(int year) {
        List<MonthlyFeeSummary> summaries = new ArrayList<>();
        if (conn == null) return summaries;

        String sql = "SELECT " +
            "MONTH(created_at) as month, " +
            "COUNT(*) as transaction_count, " +
            "SUM(fee_amount) as total_fees, " +
            "AVG(fee_amount) as avg_fee " +
            "FROM marketplace_fees " +
            "WHERE YEAR(created_at) = ? " +
            "GROUP BY MONTH(created_at) " +
            "ORDER BY month";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, year);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    summaries.add(new MonthlyFeeSummary(
                        rs.getInt("month"),
                        year,
                        rs.getInt("transaction_count"),
                        rs.getBigDecimal("total_fees"),
                        rs.getBigDecimal("avg_fee")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR generating monthly summary: " + e.getMessage());
        }

        return summaries;
    }

    /**
     * Map ResultSet to MarketplaceFee object
     */
    private MarketplaceFee mapResultToFee(ResultSet rs) throws SQLException {
        MarketplaceFee fee = new MarketplaceFee();
        fee.setId(rs.getInt("id"));
        fee.setOrderId(rs.getObject("order_id", Integer.class));
        fee.setTradeId(rs.getObject("trade_id", Integer.class));
        fee.setSellerId(rs.getLong("seller_id"));
        fee.setTransactionAmount(rs.getBigDecimal("transaction_amount"));
        fee.setFeePercentage(rs.getBigDecimal("fee_percentage"));
        fee.setFeeAmount(rs.getBigDecimal("fee_amount"));
        fee.setFeeType(rs.getString("fee_type"));
        fee.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return fee;
    }

    /**
     * Inner class for monthly fee summaries
     */
    public static class MonthlyFeeSummary {
        private int month;
        private int year;
        private int transactionCount;
        private BigDecimal totalFees;
        private BigDecimal avgFee;

        public MonthlyFeeSummary(int month, int year, int transactionCount, 
                               BigDecimal totalFees, BigDecimal avgFee) {
            this.month = month;
            this.year = year;
            this.transactionCount = transactionCount;
            this.totalFees = totalFees;
            this.avgFee = avgFee;
        }

        // Getters
        public int getMonth() { return month; }
        public int getYear() { return year; }
        public int getTransactionCount() { return transactionCount; }
        public BigDecimal getTotalFees() { return totalFees; }
        public BigDecimal getAvgFee() { return avgFee; }
    }
}
