package Models;

import java.sql.Timestamp;

/**
 * Represents a marketplace order - a purchase transaction
 * from a marketplace listing with payment processing
 */
public class MarketplaceOrder {
    private int id;
    private int listingId;
    private int buyerId;
    private int sellerId;
    private double quantity;
    private double unitPriceUsd;  // Price per unit at time of purchase
    private double totalAmountUsd;
    private double platformFeeUsd;  // 2.9% + $0.30
    private double sellerProceedsUsd;  // totalAmount - platformFee
    private String stripePaymentId;
    private String status;  // PENDING, PAYMENT_PROCESSING, ESCROWED, COMPLETED, CANCELLED, REFUNDED, DISPUTED
    private Timestamp createdAt;
    private Timestamp completionDate;
    private Timestamp updatedAt;

    // Constructors
    public MarketplaceOrder() {}

    public MarketplaceOrder(int listingId, int buyerId, int sellerId,
                           double quantity, double unitPriceUsd) {
        this.listingId = listingId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.quantity = quantity;
        this.unitPriceUsd = unitPriceUsd;
        this.totalAmountUsd = quantity * unitPriceUsd;
        calculateFees();
        this.status = "PENDING";
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getListingId() {
        return listingId;
    }

    public void setListingId(int listingId) {
        this.listingId = listingId;
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

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
        recalculateAmounts();
    }

    public double getUnitPriceUsd() {
        return unitPriceUsd;
    }

    public void setUnitPriceUsd(double unitPriceUsd) {
        this.unitPriceUsd = unitPriceUsd;
        recalculateAmounts();
    }

    public double getTotalAmountUsd() {
        return totalAmountUsd;
    }

    public void setTotalAmountUsd(double totalAmountUsd) {
        this.totalAmountUsd = totalAmountUsd;
        calculateFees();
    }

    public double getPlatformFeeUsd() {
        return platformFeeUsd;
    }

    public void setPlatformFeeUsd(double platformFeeUsd) {
        this.platformFeeUsd = platformFeeUsd;
    }

    public double getSellerProceedsUsd() {
        return sellerProceedsUsd;
    }

    public void setSellerProceedsUsd(double sellerProceedsUsd) {
        this.sellerProceedsUsd = sellerProceedsUsd;
    }

    public String getStripePaymentId() {
        return stripePaymentId;
    }

    public void setStripePaymentId(String stripePaymentId) {
        this.stripePaymentId = stripePaymentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getCompletionDate() {
        return completionDate;
    }

    public void setCompletionDate(Timestamp completionDate) {
        this.completionDate = completionDate;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Utility methods
    private void recalculateAmounts() {
        this.totalAmountUsd = quantity * unitPriceUsd;
        calculateFees();
    }

    private void calculateFees() {
        // 2.9% + $0.30 platform fee (standard marketplace fee)
        this.platformFeeUsd = (totalAmountUsd * 0.029) + 0.30;
        this.sellerProceedsUsd = totalAmountUsd - platformFeeUsd;
    }

    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }

    public boolean isPending() {
        return "PENDING".equals(status) || "PAYMENT_PROCESSING".equals(status) || "ESCROWED".equals(status);
    }

    public boolean canBeCancelled() {
        return "PENDING".equals(status) || "PAYMENT_PROCESSING".equals(status);
    }

    public long getHoursSinceCreation() {
        if (createdAt == null) return 0;
        return (System.currentTimeMillis() - createdAt.getTime()) / 3600000;
    }

    @Override
    public String toString() {
        return "MarketplaceOrder{" +
                "id=" + id +
                ", buyerId=" + buyerId +
                ", sellerId=" + sellerId +
                ", quantity=" + quantity +
                ", totalAmountUsd=$" + totalAmountUsd +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
