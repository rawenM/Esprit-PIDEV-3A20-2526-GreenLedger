package Models;

import java.sql.Timestamp;

/**
 * Represents user feedback and ratings for marketplace sellers/buyers
 * Used to build trust badges and seller reputation
 */
public class MarketplaceRating {
    private int id;
    private int ratedUserId;  // User being rated
    private int raterId;  // User submitting rating
    private Integer orderId;  // Which transaction (marketplace order)
    private Integer tradeId;  // Which transaction (peer trade)
    private int scoreOneToFive;  // 1-5 star rating
    private String reviewText;
    private String ratingCategory;  // COMMUNICATION, HONESTY, TRANSACTION_SPEED, OVERALL
    private boolean verifiedTransaction;  // Did actual purchase occur?
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Constructors
    public MarketplaceRating() {}

    public MarketplaceRating(int ratedUserId, int raterId, int scoreOneToFive,
                            String reviewText, String ratingCategory) {
        this.ratedUserId = ratedUserId;
        this.raterId = raterId;
        this.scoreOneToFive = Math.max(1, Math.min(5, scoreOneToFive));  // Ensure 1-5
        this.reviewText = reviewText;
        this.ratingCategory = ratingCategory != null ? ratingCategory : "OVERALL";
        this.verifiedTransaction = false;
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRatedUserId() {
        return ratedUserId;
    }

    public void setRatedUserId(int ratedUserId) {
        this.ratedUserId = ratedUserId;
    }

    public int getRaterId() {
        return raterId;
    }

    public void setRaterId(int raterId) {
        this.raterId = raterId;
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

    public int getScoreOneToFive() {
        return scoreOneToFive;
    }

    public void setScoreOneToFive(int scoreOneToFive) {
        this.scoreOneToFive = Math.max(1, Math.min(5, scoreOneToFive));
    }

    public String getReviewText() {
        return reviewText;
    }

    public void setReviewText(String reviewText) {
        this.reviewText = reviewText;
    }

    public String getRatingCategory() {
        return ratingCategory;
    }

    public void setRatingCategory(String ratingCategory) {
        this.ratingCategory = ratingCategory;
    }

    public boolean isVerifiedTransaction() {
        return verifiedTransaction;
    }

    public void setVerifiedTransaction(boolean verifiedTransaction) {
        this.verifiedTransaction = verifiedTransaction;
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
    public String getStarDisplay() {
        return "★".repeat(scoreOneToFive) + "☆".repeat(5 - scoreOneToFive);
    }

    public String getScoreLabel() {
        switch (scoreOneToFive) {
            case 5: return "Excellent";
            case 4: return "Good";
            case 3: return "Average";
            case 2: return "Poor";
            case 1: return "Very Poor";
            default: return "Unknown";
        }
    }

    public boolean isPositive() {
        return scoreOneToFive >= 4;
    }

    public boolean isNeutral() {
        return scoreOneToFive == 3;
    }

    public boolean isNegative() {
        return scoreOneToFive <= 2;
    }

    public String getTransactionType() {
        return orderId != null ? "MARKETPLACE_ORDER" : "PEER_TRADE";
    }

    @Override
    public String toString() {
        return "MarketplaceRating{" +
                "id=" + id +
                ", ratedUserId=" + ratedUserId +
                ", score=" + scoreOneToFive + " " + getStarDisplay() +
                ", category='" + ratingCategory + '\'' +
                ", verified=" + verifiedTransaction +
                '}';
    }
}
