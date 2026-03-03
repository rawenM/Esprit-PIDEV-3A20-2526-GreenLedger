package Services;

import Models.MarketplaceRating;
import DataBase.MyConnection;

import java.sql.*;
import java.util.*;

/**
 * Service for managing user ratings and trust badges in the marketplace
 */
public class MarketplaceRatingService {
    private static final String LOG_TAG = "[MarketplaceRatingService]";
    private static MarketplaceRatingService instance;
    private Connection conn;

    private MarketplaceRatingService() {
        this.conn = MyConnection.getConnection();
    }

    public static MarketplaceRatingService getInstance() {
        if (instance == null) {
            instance = new MarketplaceRatingService();
        }
        return instance;
    }

    /**
     * Submit a rating for a user after transaction
     */
    public int submitRating(int ratedUserId, int raterId, int score, String reviewText, 
                           Integer orderId, Integer tradeId) {
        int ratingId = -1;

        try {
            if (conn == null) return ratingId;

            String sql = "INSERT INTO marketplace_ratings " +
                    "(rated_user_id, rater_id, score_one_to_five, review_text, " +
                    "order_id, trade_id, rating_category, is_verified_transaction) " +
                    "VALUES (?, ?, ?, ?, ?, ?, 'OVERALL', 1)";

            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, ratedUserId);
                stmt.setInt(2, raterId);
                stmt.setInt(3, Math.max(1, Math.min(5, score)));
                stmt.setString(4, reviewText);
                stmt.setObject(5, orderId);
                stmt.setObject(6, tradeId);

                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        ratingId = rs.getInt(1);
                        System.out.println(LOG_TAG + " Rating submitted: ID " + ratingId + 
                            " for user " + ratedUserId);
                        
                        // Update user's average rating
                        updateUserAverageRating(ratedUserId);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR submitting rating: " + e.getMessage());
        }

        return ratingId;
    }

    /**
     * Get average rating for a user
     */
    public double getUserAverageRating(int userId) {
        try {
            if (conn == null) return 5.0;

            String sql = "SELECT AVG(score_one_to_five) as avg_rating FROM marketplace_ratings " +
                    "WHERE rated_user_id = ? AND rating_category = 'OVERALL'";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        double avg = rs.getDouble("avg_rating");
                        return rs.wasNull() ? 5.0 : avg;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR fetching average rating: " + e.getMessage());
        }

        return 5.0;
    }

    /**
     * Get rating count for a user
     */
    public int getUserRatingCount(int userId) {
        try {
            if (conn == null) return 0;

            String sql = "SELECT COUNT(*) as count FROM marketplace_ratings " +
                    "WHERE rated_user_id = ? AND rating_category = 'OVERALL'";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("count");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR fetching rating count: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Get all ratings for a user
     */
    public List<MarketplaceRating> getUserRatings(int userId) {
        List<MarketplaceRating> ratings = new ArrayList<>();

        try {
            if (conn == null) return ratings;

            String sql = "SELECT * FROM marketplace_ratings WHERE rated_user_id = ? " +
                    "ORDER BY created_at DESC";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ratings.add(mapResultToRating(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR fetching user ratings: " + e.getMessage());
        }

        return ratings;
    }

    /**
     * Get ratings by category
     */
    public List<MarketplaceRating> getUserRatingsByCategory(int userId, String category) {
        List<MarketplaceRating> ratings = new ArrayList<>();

        try {
            if (conn == null) return ratings;

            String sql = "SELECT * FROM marketplace_ratings WHERE rated_user_id = ? " +
                    "AND rating_category = ? ORDER BY created_at DESC";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setString(2, category);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ratings.add(mapResultToRating(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR fetching ratings by category: " + e.getMessage());
        }

        return ratings;
    }

    /**
     * Check if user has rated another user for specific transaction
     */
    public boolean hasRatedTransaction(int ratedUserId, int raterId, Integer orderId, Integer tradeId) {
        try {
            if (conn == null) return false;

            String sql = "SELECT COUNT(*) as count FROM marketplace_ratings " +
                    "WHERE rated_user_id = ? AND rater_id = ? AND " +
                    "((order_id = ? AND order_id IS NOT NULL) OR (trade_id = ? AND trade_id IS NOT NULL))";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, ratedUserId);
                stmt.setInt(2, raterId);
                stmt.setObject(3, orderId);
                stmt.setObject(4, tradeId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("count") > 0;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR checking rating: " + e.getMessage());
        }

        return false;
    }

    /**
     * Update user's marketplace KYC with average rating
     */
    private void updateUserAverageRating(int userId) {
        double avgRating = getUserAverageRating(userId);

        try {
            if (conn == null) return;

            String sql = "UPDATE user_marketplace_kyc SET seller_avg_rating = ? WHERE user_id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setDouble(1, avgRating);
                stmt.setInt(2, userId);
                stmt.executeUpdate();

                System.out.println(LOG_TAG + " Updated average rating for user " + userId + 
                    ": " + avgRating);
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR updating average rating: " + e.getMessage());
        }
    }

    /**
     * Map ResultSet to MarketplaceRating
     */
    private MarketplaceRating mapResultToRating(ResultSet rs) throws SQLException {
        MarketplaceRating rating = new MarketplaceRating(
            rs.getInt("rated_user_id"),
            rs.getInt("rater_id"),
            rs.getInt("score_one_to_five"),
            rs.getString("review_text"),
            rs.getString("rating_category")
        );
        rating.setId(rs.getInt("id"));
        rating.setOrderId(rs.getObject("order_id") != null ? rs.getInt("order_id") : null);
        rating.setTradeId(rs.getObject("trade_id") != null ? rs.getInt("trade_id") : null);
        rating.setVerifiedTransaction(rs.getBoolean("is_verified_transaction"));
        rating.setCreatedAt(rs.getTimestamp("created_at"));
        rating.setUpdatedAt(rs.getTimestamp("updated_at"));
        return rating;
    }
}

