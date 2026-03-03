package Models;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Represents a marketplace platform fee from a transaction
 * Tracks fees collected from sellers for platform usage
 */
public class MarketplaceFee {
    private int id;
    private Integer orderId;  // From marketplace_orders
    private Integer tradeId;  // From peer_trades (alternative)
    private long sellerId;
    private BigDecimal transactionAmount;  // Total transaction amount
    private BigDecimal feePercentage;      // Percentage applied (e.g., 2.5)
    private BigDecimal feeAmount;          // Calculated fee amount
    private String feeType;                // TRANSACTION, DISPUTED, REFUND, LISTING
    private LocalDateTime createdAt;

    // Constructors
    public MarketplaceFee() {}

    public MarketplaceFee(Integer orderId, long sellerId, BigDecimal transactionAmount,
                         BigDecimal feePercentage, String feeType) {
        this.orderId = orderId;
        this.sellerId = sellerId;
        this.transactionAmount = transactionAmount;
        this.feePercentage = feePercentage;
        this.feeType = feeType;
        this.createdAt = LocalDateTime.now();
        
        // Calculate fee amount
        this.feeAmount = transactionAmount.multiply(feePercentage).divide(new BigDecimal("100"));
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public Integer getTradeId() {
        return tradeId;
    }

    public void setTradeId(Integer tradeId) {
        this.tradeId = tradeId;
    }

    public long getSellerId() {
        return sellerId;
    }

    public void setSellerId(long sellerId) {
        this.sellerId = sellerId;
    }

    public BigDecimal getTransactionAmount() {
        return transactionAmount;
    }

    public void setTransactionAmount(BigDecimal transactionAmount) {
        this.transactionAmount = transactionAmount;
    }

    public BigDecimal getFeePercentage() {
        return feePercentage;
    }

    public void setFeePercentage(BigDecimal feePercentage) {
        this.feePercentage = feePercentage;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(BigDecimal feeAmount) {
        this.feeAmount = feeAmount;
    }

    public String getFeeType() {
        return feeType;
    }

    public void setFeeType(String feeType) {
        this.feeType = feeType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "MarketplaceFee{" +
                "id=" + id +
                ", orderId=" + orderId +
                ", sellerId=" + sellerId +
                ", transactionAmount=" + transactionAmount +
                ", feeAmount=" + feeAmount +
                ", feeType='" + feeType + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
