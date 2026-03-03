package Models;

import java.sql.Timestamp;

/**
 * Represents enhanced KYC (Know Your Customer) data for marketplace traders
 * Includes seller reputation, verification status, and trust badges
 */
public class UserMarketplaceKYC {
    private int id;
    private int userId;  // 1:1 with User
    private String shopName;
    private String shopDescription;
    private boolean isVerifiedTrader;
    private Timestamp verificationDate;
    private String idDocumentType;  // PASSPORT, NATIONAL_ID, BUSINESS_LICENSE, OTHER
    private String idDocumentHash;  // SHA256 hash of document
    private boolean bankAccountVerified;
    private String preferredPayoutMethod;  // BANK_TRANSFER, STRIPE_CONNECT, WALLET
    private double sellerAvgRating;  // 1.00-5.00 average
    private int sellerTransactionCount;
    private double sellerLifetimeVolumeUsd;
    private int buyerTransactionCount;
    private double buyerLifetimeVolumeUsd;
    private String trustBadgeLevel;  // NONE, SELLER, POWER_SELLER, VERIFIED_PARTNER
    private Timestamp lastUpdated;

    // Constructors
    public UserMarketplaceKYC() {}

    public UserMarketplaceKYC(int userId) {
        this.userId = userId;
        this.sellerAvgRating = 5.00;
        this.sellerTransactionCount = 0;
        this.buyerTransactionCount = 0;
        this.trustBadgeLevel = "NONE";
        this.preferredPayoutMethod = "BANK_TRANSFER";
        this.lastUpdated = new Timestamp(System.currentTimeMillis());
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public String getShopDescription() {
        return shopDescription;
    }

    public void setShopDescription(String shopDescription) {
        this.shopDescription = shopDescription;
    }

    public boolean isVerifiedTrader() {
        return isVerifiedTrader;
    }

    public void setVerifiedTrader(boolean verifiedTrader) {
        isVerifiedTrader = verifiedTrader;
    }

    public Timestamp getVerificationDate() {
        return verificationDate;
    }

    public void setVerificationDate(Timestamp verificationDate) {
        this.verificationDate = verificationDate;
    }

    public String getIdDocumentType() {
        return idDocumentType;
    }

    public void setIdDocumentType(String idDocumentType) {
        this.idDocumentType = idDocumentType;
    }

    public String getIdDocumentHash() {
        return idDocumentHash;
    }

    public void setIdDocumentHash(String idDocumentHash) {
        this.idDocumentHash = idDocumentHash;
    }

    public boolean isBankAccountVerified() {
        return bankAccountVerified;
    }

    public void setBankAccountVerified(boolean bankAccountVerified) {
        this.bankAccountVerified = bankAccountVerified;
    }

    public String getPreferredPayoutMethod() {
        return preferredPayoutMethod;
    }

    public void setPreferredPayoutMethod(String preferredPayoutMethod) {
        this.preferredPayoutMethod = preferredPayoutMethod;
    }

    public double getSellerAvgRating() {
        return sellerAvgRating;
    }

    public void setSellerAvgRating(double sellerAvgRating) {
        this.sellerAvgRating = Math.max(1.00, Math.min(5.00, sellerAvgRating));
    }

    public int getSellerTransactionCount() {
        return sellerTransactionCount;
    }

    public void setSellerTransactionCount(int sellerTransactionCount) {
        this.sellerTransactionCount = sellerTransactionCount;
        updateTrustBadge();
    }

    public double getSellerLifetimeVolumeUsd() {
        return sellerLifetimeVolumeUsd;
    }

    public void setSellerLifetimeVolumeUsd(double sellerLifetimeVolumeUsd) {
        this.sellerLifetimeVolumeUsd = sellerLifetimeVolumeUsd;
    }

    public int getBuyerTransactionCount() {
        return buyerTransactionCount;
    }

    public void setBuyerTransactionCount(int buyerTransactionCount) {
        this.buyerTransactionCount = buyerTransactionCount;
    }

    public double getBuyerLifetimeVolumeUsd() {
        return buyerLifetimeVolumeUsd;
    }

    public void setBuyerLifetimeVolumeUsd(double buyerLifetimeVolumeUsd) {
        this.buyerLifetimeVolumeUsd = buyerLifetimeVolumeUsd;
    }

    public String getTrustBadgeLevel() {
        return trustBadgeLevel;
    }

    public void setTrustBadgeLevel(String trustBadgeLevel) {
        this.trustBadgeLevel = trustBadgeLevel;
    }

    public Timestamp getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Timestamp lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    // Utility methods
    private void updateTrustBadge() {
        // Auto-calculate trust badge based on transactions and rating
        if (sellerTransactionCount < 20) {
            trustBadgeLevel = "NONE";
        } else if (sellerAvgRating < 3.5) {
            trustBadgeLevel = "NONE";
        } else if (sellerTransactionCount < 50) {
            trustBadgeLevel = "SELLER";
        } else if (sellerAvgRating >= 4.5 && sellerTransactionCount >= 100) {
            trustBadgeLevel = "POWER_SELLER";
        } else if (isVerifiedTrader && sellerTransactionCount >= 200 && sellerAvgRating >= 4.7) {
            trustBadgeLevel = "VERIFIED_PARTNER";
        } else {
            trustBadgeLevel = "SELLER";
        }
    }

    public boolean canSellWallets() {
        return isVerifiedTrader && bankAccountVerified && sellerTransactionCount >= 5;
    }

    public boolean canSellUnlimitedCredits() {
        return isVerifiedTrader && sellerTransactionCount >= 10;
    }

    public double getMaxTransactionLimit() {
        if (!isVerifiedTrader) {
            return 100.0;  // New users limited to 100 tCO2
        }
        if (sellerTransactionCount < 10) {
            return 500.0;  // Early users limited to 500 tCO2
        }
        return Double.MAX_VALUE;  // Verified traders unlimited
    }

    public String getRatingStars() {
        int stars = (int) Math.round(sellerAvgRating);
        return "★".repeat(stars) + "☆".repeat(5 - stars);
    }

    public String getTrustBadgeDescription() {
        switch (trustBadgeLevel) {
            case "VERIFIED_PARTNER": return "Verified Partner - Highly Trusted";
            case "POWER_SELLER": return "Power Seller - Excellent Service";
            case "SELLER": return "Verified Seller - Good Standing";
            case "NONE": return "Not Yet Verified";
            default: return "Unknown";
        }
    }

    public String getTrustBadgeIcon() {
        switch (trustBadgeLevel) {
            case "VERIFIED_PARTNER": return "🏆";
            case "POWER_SELLER": return "⭐";
            case "SELLER": return "✓";
            case "NONE": return "";
            default: return "";
        }
    }

    public int getTotalTransactions() {
        return sellerTransactionCount + buyerTransactionCount;
    }

    public double getTotalLifetimeVolume() {
        return sellerLifetimeVolumeUsd + buyerLifetimeVolumeUsd;
    }

    @Override
    public String toString() {
        return "UserMarketplaceKYC{" +
                "userId=" + userId +
                ", shopName='" + shopName + '\'' +
                ", isVerified=" + isVerifiedTrader +
                ", sellerAvgRating=" + sellerAvgRating +
                ", trustBadge='" + trustBadgeLevel + '\'' +
                ", transactions=" + getTotalTransactions() +
                '}';
    }
}
