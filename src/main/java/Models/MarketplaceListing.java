package Models;

import java.sql.Timestamp;

/**
 * Represents an active marketplace listing for buying/selling
 * carbon credits or complete wallets
 */
public class MarketplaceListing {
    private int id;
    private int sellerId;
    private String assetType;  // CARBON_CREDITS or WALLET
    private Integer walletId;  // Wallet ID being sold or source wallet
    private double quantityOrTokens;  // Amount for credits, 1 for wallet
    private double pricePerUnit;  // USD per tCO2 or per wallet
    private double minPriceUsd;  // Minimum acceptable offer price per unit
    private Double autoAcceptPriceUsd;  // Auto-accept offers at or above this
    private double totalPriceUsd;  // Calculated: quantity * pricePerUnit
    private String status;  // ACTIVE, PENDING, SOLD, CANCELLED, EXPIRED
    private String description;
    private int minimumBuyerRating;  // 0-5 minimum seller rating requirement
    private Timestamp createdAt;
    private Timestamp expiresAt;
    private Timestamp updatedAt;

    // Constructors
    public MarketplaceListing() {}

    public MarketplaceListing(int sellerId, String assetType, Integer walletId,
                            double quantityOrTokens, double pricePerUnit,
                            double minPriceUsd, Double autoAcceptPriceUsd, String description) {
        this.sellerId = sellerId;
        this.assetType = assetType;
        this.walletId = walletId;
        this.quantityOrTokens = quantityOrTokens;
        this.pricePerUnit = pricePerUnit;
        this.minPriceUsd = minPriceUsd;
        this.autoAcceptPriceUsd = autoAcceptPriceUsd;
        this.description = description;
        this.totalPriceUsd = quantityOrTokens * pricePerUnit;
        this.status = "ACTIVE";
        this.minimumBuyerRating = 0;
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSellerId() {
        return sellerId;
    }

    public void setSellerId(int sellerId) {
        this.sellerId = sellerId;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public Integer getWalletId() {
        return walletId;
    }

    public void setWalletId(Integer walletId) {
        this.walletId = walletId;
    }

    public double getQuantityOrTokens() {
        return quantityOrTokens;
    }

    public void setQuantityOrTokens(double quantityOrTokens) {
        this.quantityOrTokens = quantityOrTokens;
        recalculateTotal();
    }

    public double getPricePerUnit() {
        return pricePerUnit;
    }

    public void setPricePerUnit(double pricePerUnit) {
        this.pricePerUnit = pricePerUnit;
        recalculateTotal();
    }

    public double getMinPriceUsd() {
        return minPriceUsd;
    }

    public void setMinPriceUsd(double minPriceUsd) {
        this.minPriceUsd = minPriceUsd;
    }

    public Double getAutoAcceptPriceUsd() {
        return autoAcceptPriceUsd;
    }

    public void setAutoAcceptPriceUsd(Double autoAcceptPriceUsd) {
        this.autoAcceptPriceUsd = autoAcceptPriceUsd;
    }

    public double getTotalPriceUsd() {
        return totalPriceUsd;
    }

    public void setTotalPriceUsd(double totalPriceUsd) {
        this.totalPriceUsd = totalPriceUsd;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getMinimumBuyerRating() {
        return minimumBuyerRating;
    }

    public void setMinimumBuyerRating(int minimumBuyerRating) {
        this.minimumBuyerRating = minimumBuyerRating;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Timestamp expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Utility methods
    private void recalculateTotal() {
        this.totalPriceUsd = this.quantityOrTokens * this.pricePerUnit;
    }

    public boolean isExpired() {
        if (expiresAt == null) return false;
        return System.currentTimeMillis() > expiresAt.getTime();
    }

    public boolean isActive() {
        return "ACTIVE".equals(status) && !isExpired();
    }

    public String getAssetDescription() {
        return "CARBON_CREDITS".equals(assetType) 
            ? String.format("%.2f tCO2e credits", quantityOrTokens)
            : "Complete Wallet Asset";
    }

    @Override
    public String toString() {
        return "MarketplaceListing{" +
                "id=" + id +
                ", sellerId=" + sellerId +
                ", assetType='" + assetType + '\'' +
                ", walletId=" + walletId +
                ", quantityOrTokens=" + quantityOrTokens +
                ", pricePerUnit=$" + pricePerUnit +
                ", totalPriceUsd=$" + totalPriceUsd +
                ", status='" + status + '\'' +
                '}';
    }
}
