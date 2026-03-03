package Models;

import java.sql.Timestamp;

/**
 * Represents an escrow holding for secure marketplace transactions
 * Funds are held by platform until buyer confirms or dispute is resolved
 */
public class MarketplaceEscrow {
    private int id;
    private Integer orderId;  // From marketplace_orders
    private Integer tradeId;  // From peer_trades
    private int buyerId;
    private int sellerId;
    private double amountUsd;
    private boolean heldByPlatform;  // TRUE = platform holds; FALSE = third-party
    private String stripeHoldId;
    private String status;  // HELD, RELEASED_TO_SELLER, REFUNDED_TO_BUYER, DISPUTED, RESOLVED
    private String holdReason;
    private Timestamp releaseDate;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Constructors
    public MarketplaceEscrow() {}

    public MarketplaceEscrow(Integer orderId, Integer tradeId,
                            int buyerId, int sellerId, double amountUsd) {
        this.orderId = orderId;
        this.tradeId = tradeId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.amountUsd = amountUsd;
        this.heldByPlatform = true;
        this.status = "HELD";
        this.createdAt = new Timestamp(System.currentTimeMillis());
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

    public int getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(int buyerId) {
        this.buyerId = buyerId;
    }

    public int getSellerId() {
        return sellerId;
    }

    public void setSellerId(int sellerId) {
        this.sellerId = sellerId;
    }

    public double getAmountUsd() {
        return amountUsd;
    }

    public void setAmountUsd(double amountUsd) {
        this.amountUsd = amountUsd;
    }

    public boolean isHeldByPlatform() {
        return heldByPlatform;
    }

    public void setHeldByPlatform(boolean heldByPlatform) {
        this.heldByPlatform = heldByPlatform;
    }

    public String getStripeHoldId() {
        return stripeHoldId;
    }

    public void setStripeHoldId(String stripeHoldId) {
        this.stripeHoldId = stripeHoldId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getHoldReason() {
        return holdReason;
    }

    public void setHoldReason(String holdReason) {
        this.holdReason = holdReason;
    }

    public Timestamp getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(Timestamp releaseDate) {
        this.releaseDate = releaseDate;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Utility methods
    public boolean isActive() {
        return "HELD".equals(status);
    }

    public boolean isReleased() {
        return "RELEASED_TO_SELLER".equals(status) || "REFUNDED_TO_BUYER".equals(status);
    }

    public boolean canBeDisputed() {
        return "HELD".equals(status) || "DISPUTED".equals(status);
    }

    public long getHoursHeld() {
        if (createdAt == null) return 0;
        long endTime = releaseDate != null ? releaseDate.getTime() : System.currentTimeMillis();
        return (endTime - createdAt.getTime()) / 3600000;
    }

    public boolean isBeyond24Hours() {
        return getHoursHeld() > 24;
    }

    public String getTransactionType() {
        return orderId != null ? "MARKETPLACE_ORDER" : "PEER_TRADE";
    }

    @Override
    public String toString() {
        return "MarketplaceEscrow{" +
                "id=" + id +
                ", amountUsd=$" + amountUsd +
                ", status='" + status + '\'' +
                ", buyerId=" + buyerId +
                ", sellerId=" + sellerId +
                ", hoursHeld=" + getHoursHeld() +
                '}';
    }
}
