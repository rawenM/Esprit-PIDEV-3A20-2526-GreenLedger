package Services;

import Models.UserMarketplaceKYC;
import DataBase.MyConnection;

import java.security.MessageDigest;
import java.sql.*;
import java.util.*;

/**
 * Service for managing enhanced KYC data for marketplace traders
 * Handles seller verification, trust badges, and transaction limits
 */
public class UserMarketplaceKYCService {
    private static final String LOG_TAG = "[UserMarketplaceKYCService]";
    private static UserMarketplaceKYCService instance;
    private Connection conn;

    private UserMarketplaceKYCService() {
        this.conn = MyConnection.getConnection();
    }

    public static UserMarketplaceKYCService getInstance() {
        if (instance == null) {
            instance = new UserMarketplaceKYCService();
        }
        return instance;
    }

    /**
     * Initialize KYC record for a new user
     */
    public int initializeKYC(int userId) {
        int kycId = -1;

        try {
            if (conn == null) return kycId;

            String sql = "INSERT INTO user_marketplace_kyc (user_id, trust_badge_level) VALUES (?, 'NONE')";

            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, userId);
                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        kycId = rs.getInt(1);
                        System.out.println(LOG_TAG + " KYC initialized for user " + userId);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR initializing KYC: " + e.getMessage());
        }

        return kycId;
    }

    /**
     * Get KYC record for user
     */
    public UserMarketplaceKYC getKYCForUser(int userId) {
        try {
            if (conn == null) return null;

            String sql = "SELECT * FROM user_marketplace_kyc WHERE user_id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultToKYC(rs);
                    } else {
                        // Auto-create if doesn't exist
                        initializeKYC(userId);
                        return getKYCForUser(userId);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR fetching KYC: " + e.getMessage());
        }

        return null;
    }

    /**
     * Update shop profile
     */
    public boolean updateShopProfile(int userId, String shopName, String shopDescription) {
        try {
            if (conn == null) return false;

            String sql = "UPDATE user_marketplace_kyc SET shop_name = ?, shop_description = ? WHERE user_id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, shopName);
                stmt.setString(2, shopDescription);
                stmt.setInt(3, userId);

                int updated = stmt.executeUpdate();
                if (updated > 0) {
                    System.out.println(LOG_TAG + " Shop profile updated for user " + userId);
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR updating shop profile: " + e.getMessage());
        }

        return false;
    }

    /**
     * Verify user identity (ID document)
     */
    public boolean verifyIdentity(int userId, String docType, String docHash) {
        try {
            if (conn == null) return false;

            String sql = "UPDATE user_marketplace_kyc SET id_document_type = ?, " +
                    "id_document_hash = ?, is_verified_trader = 1, verification_date = NOW() " +
                    "WHERE user_id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, docType);
                stmt.setString(2, docHash);
                stmt.setInt(3, userId);

                int updated = stmt.executeUpdate();
                if (updated > 0) {
                    System.out.println(LOG_TAG + " User " + userId + " verified with " + docType);
                    updateTrustBadge(userId);
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR verifying identity: " + e.getMessage());
        }

        return false;
    }

    /**
     * Verify bank account
     */
    public boolean verifyBankAccount(int userId) {
        try {
            if (conn == null) return false;

            String sql = "UPDATE user_marketplace_kyc SET bank_account_verified = 1 WHERE user_id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);

                int updated = stmt.executeUpdate();
                if (updated > 0) {
                    System.out.println(LOG_TAG + " Bank account verified for user " + userId);
                    updateTrustBadge(userId);
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR verifying bank account: " + e.getMessage());
        }

        return false;
    }

    /**
     * Update transaction counts and volumes
     */
    public void recordSellerTransaction(int sellerId, double transactionAmountUsd) {
        try {
            if (conn == null) return;

            String sql = "UPDATE user_marketplace_kyc SET " +
                    "seller_transaction_count = seller_transaction_count + 1, " +
                    "seller_lifetime_volume_usd = seller_lifetime_volume_usd + ? " +
                    "WHERE user_id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setDouble(1, transactionAmountUsd);
                stmt.setInt(2, sellerId);
                stmt.executeUpdate();

                System.out.println(LOG_TAG + " Seller transaction recorded for user " + sellerId);
                updateTrustBadge(sellerId);
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR recording transaction: " + e.getMessage());
        }
    }

    /**
     * Record buyer transaction
     */
    public void recordBuyerTransaction(int buyerId, double transactionAmountUsd) {
        try {
            if (conn == null) return;

            String sql = "UPDATE user_marketplace_kyc SET " +
                    "buyer_transaction_count = buyer_transaction_count + 1, " +
                    "buyer_lifetime_volume_usd = buyer_lifetime_volume_usd + ? " +
                    "WHERE user_id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setDouble(1, transactionAmountUsd);
                stmt.setInt(2, buyerId);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR recording buyer transaction: " + e.getMessage());
        }
    }

    /**
     * Auto-calculate and update trust badge based on transaction history and rating
     */
    public void updateTrustBadge(int userId) {
        try {
            if (conn == null) return;

            UserMarketplaceKYC kyc = getKYCForUser(userId);
            if (kyc == null) return;

            String newBadge = calculateTrustBadge(kyc);

            String sql = "UPDATE user_marketplace_kyc SET trust_badge_level = ?, last_updated = NOW() WHERE user_id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, newBadge);
                stmt.setInt(2, userId);
                stmt.executeUpdate();

                System.out.println(LOG_TAG + " Trust badge updated for user " + userId + ": " + newBadge);
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR updating trust badge: " + e.getMessage());
        }
    }

    /**
     * Calculate trust badge based on transactions and rating
     */
    private String calculateTrustBadge(UserMarketplaceKYC kyc) {
        if (kyc.getSellerTransactionCount() < 20) {
            return "NONE";
        }
        
        if (kyc.getSellerAvgRating() < 3.5) {
            return "NONE";
        }
        
        if (kyc.getSellerTransactionCount() < 50 && kyc.getSellerAvgRating() >= 3.5) {
            return "SELLER";
        }
        
        if (kyc.getSellerAvgRating() >= 4.5 && kyc.getSellerTransactionCount() >= 100) {
            return "POWER_SELLER";
        }
        
        if (kyc.isVerifiedTrader() && kyc.getSellerTransactionCount() >= 200 && 
            kyc.getSellerAvgRating() >= 4.7) {
            return "VERIFIED_PARTNER";
        }
        
        return "SELLER";
    }

    /**
     * Check if user can sell items (seller prerequisites)
     */
    public boolean canUserSell(int userId) {
        UserMarketplaceKYC kyc = getKYCForUser(userId);
        if (kyc == null) return false;
        
        return kyc.isVerifiedTrader();
    }

    /**
     * Check if user can sell wallets (requires higher verification)
     */
    public boolean canUserSellWallets(int userId) {
        UserMarketplaceKYC kyc = getKYCForUser(userId);
        if (kyc == null) return false;
        
        return kyc.canSellWallets();
    }

    /**
     * Get maximum transaction limit for user
     */
    public double getMaxTransactionLimit(int userId) {
        UserMarketplaceKYC kyc = getKYCForUser(userId);
        if (kyc == null) return 100.0;
        
        return kyc.getMaxTransactionLimit();
    }

    /**
     * Check if user has transaction limit protection
     */
    public boolean isLimitedUser(int userId) {
        UserMarketplaceKYC kyc = getKYCForUser(userId);
        if (kyc == null) return true;
        
        return !kyc.isVerifiedTrader();
    }

    /**
     * Hash document for storage (SHA-256)
     */
    public static String hashDocument(byte[] documentData) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(documentData);
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            System.err.println(LOG_TAG + " ERROR hashing document: " + e.getMessage());
            return null;
        }
    }

    /**
     * Map ResultSet to UserMarketplaceKYC
     */
    private UserMarketplaceKYC mapResultToKYC(ResultSet rs) throws SQLException {
        UserMarketplaceKYC kyc = new UserMarketplaceKYC(rs.getInt("user_id"));
        kyc.setId(rs.getInt("id"));
        kyc.setShopName(rs.getString("shop_name"));
        kyc.setShopDescription(rs.getString("shop_description"));
        kyc.setVerifiedTrader(rs.getBoolean("is_verified_trader"));
        kyc.setVerificationDate(rs.getTimestamp("verification_date"));
        kyc.setIdDocumentType(rs.getString("id_document_type"));
        kyc.setIdDocumentHash(rs.getString("id_document_hash"));
        kyc.setBankAccountVerified(rs.getBoolean("bank_account_verified"));
        kyc.setPreferredPayoutMethod(rs.getString("preferred_payout_method"));
        kyc.setSellerAvgRating(rs.getDouble("seller_avg_rating"));
        kyc.setSellerTransactionCount(rs.getInt("seller_transaction_count"));
        kyc.setSellerLifetimeVolumeUsd(rs.getDouble("seller_lifetime_volume_usd"));
        kyc.setBuyerTransactionCount(rs.getInt("buyer_transaction_count"));
        kyc.setBuyerLifetimeVolumeUsd(rs.getDouble("buyer_lifetime_volume_usd"));
        kyc.setTrustBadgeLevel(rs.getString("trust_badge_level"));
        kyc.setLastUpdated(rs.getTimestamp("last_updated"));
        return kyc;
    }
}

