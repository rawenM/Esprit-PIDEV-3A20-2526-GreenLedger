package Services;

import Models.MarketplaceListing;
import DataBase.MyConnection;

import java.sql.*;
import java.util.*;

/**
 * Service for managing marketplace listings
 * Handles CRUD operations and search for buy/sell offers
 */
public class MarketplaceListingService {
    private static final String LOG_TAG = "[MarketplaceListingService]";
    private static MarketplaceListingService instance;
    private Connection conn;

    private MarketplaceListingService() {
        this.conn = MyConnection.getConnection();
    }

    public static MarketplaceListingService getInstance() {
        if (instance == null) {
            instance = new MarketplaceListingService();
        }
        return instance;
    }

    /**
     * Create a new marketplace listing
     */
    public int createListing(int sellerId, String assetType, Integer walletId,
                            double quantityOrTokens, double pricePerUnit,
                            double minPriceUsd, Double autoAcceptPriceUsd, String description) {
        int listingId = -1;

        try {
            if (conn == null) return listingId;

            String sql = "INSERT INTO marketplace_listings " +
                    "(seller_id, asset_type, wallet_id, quantity_or_id, price_per_unit, " +
                    "min_price_usd, auto_accept_price_usd, description, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE')";

            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, sellerId);
                stmt.setString(2, assetType);
                stmt.setObject(3, walletId);
                stmt.setDouble(4, quantityOrTokens);
                stmt.setDouble(5, pricePerUnit);
                stmt.setDouble(6, minPriceUsd);
                if (autoAcceptPriceUsd != null) {
                    stmt.setDouble(7, autoAcceptPriceUsd);
                } else {
                    stmt.setNull(7, Types.DECIMAL);
                }
                stmt.setString(8, description);

                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        listingId = rs.getInt(1);
                        System.out.println(LOG_TAG + " Listing created: ID " + listingId + 
                            " by seller " + sellerId);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR creating listing: " + e.getMessage());
        }

        return listingId;
    }

    /**
     * Get listing by ID
     */
    public MarketplaceListing getListingById(int listingId) {
        try {
            if (conn == null) return null;

            String sql = "SELECT * FROM marketplace_listings WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, listingId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultToListing(rs);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR fetching listing: " + e.getMessage());
        }

        return null;
    }

    /**
     * Get active listings with optional filters
     */
    public List<MarketplaceListing> searchListings(String assetType, Double minPrice, 
                                                    Double maxPrice, int limit) {
        List<MarketplaceListing> listings = new ArrayList<>();

        try {
            if (conn == null) return listings;

            StringBuilder sql = new StringBuilder(
                "SELECT * FROM marketplace_listings WHERE status = 'ACTIVE' AND expires_at IS NULL"
            );

            if (assetType != null && !assetType.isEmpty()) {
                sql.append(" AND asset_type = '").append(assetType).append("'");
            }
            if (minPrice != null) {
                sql.append(" AND price_per_unit >= ").append(minPrice);
            }
            if (maxPrice != null) {
                sql.append(" AND price_per_unit <= ").append(maxPrice);
            }

            sql.append(" ORDER BY created_at DESC LIMIT ").append(limit);

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql.toString())) {

                while (rs.next()) {
                    listings.add(mapResultToListing(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR searching listings: " + e.getMessage());
        }

        return listings;
    }

    /**
     * Get all active listings (marketplace browse)
     */
    public List<MarketplaceListing> getActiveListings() {
        return searchListings(null, null, null, 1000);
    }

    /**
     * Get listings by seller
     */
    public List<MarketplaceListing> getSellerListings(int sellerId) {
        List<MarketplaceListing> listings = new ArrayList<>();

        try {
            if (conn == null) return listings;

            String sql = "SELECT * FROM marketplace_listings WHERE seller_id = ? " +
                    "ORDER BY created_at DESC";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, sellerId);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        listings.add(mapResultToListing(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR fetching seller listings: " + e.getMessage());
        }

        return listings;
    }

    /**
     * Update listing price and quantity
     */
    public boolean updateListing(int listingId, double newQuantity, double newPrice) {
        try {
            if (conn == null) return false;

            String sql = "UPDATE marketplace_listings " +
                    "SET quantity_or_id = ?, price_per_unit = ?, updated_at = NOW() WHERE id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setDouble(1, newQuantity);
                stmt.setDouble(2, newPrice);
                stmt.setInt(3, listingId);

                int updated = stmt.executeUpdate();
                if (updated > 0) {
                    System.out.println(LOG_TAG + " Listing " + listingId + " updated");
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR updating listing: " + e.getMessage());
        }

        return false;
    }

    /**
     * Deactivate listing (remove from marketplace)
     */
    public boolean deactivateListing(int listingId) {
        return updateListingStatus(listingId, "CANCELLED");
    }

    /**
     * Mark listing as sold
     */
    public boolean markAsSold(int listingId) {
        return updateListingStatus(listingId, "SOLD");
    }

    /**
     * Update listing status
     */
    private boolean updateListingStatus(int listingId, String newStatus) {
        try {
            if (conn == null) return false;

            String sql = "UPDATE marketplace_listings SET status = ?, updated_at = NOW() WHERE id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, newStatus);
                stmt.setInt(2, listingId);

                int updated = stmt.executeUpdate();
                if (updated > 0) {
                    System.out.println(LOG_TAG + " Listing " + listingId + " status changed to " + newStatus);
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR updating listing status: " + e.getMessage());
        }

        return false;
    }

    /**
     * Get listings by asset type
     */
    public List<MarketplaceListing> getListingsByAsset(String assetType) {
        return searchListings(assetType, null, null, 1000);
    }

    /**
     * Get listings in price range
     */
    public List<MarketplaceListing> getListingsPriceRange(double minPrice, double maxPrice) {
        return searchListings(null, minPrice, maxPrice, 1000);
    }

    /**
     * Map ResultSet to MarketplaceListing object
     */
    private MarketplaceListing mapResultToListing(ResultSet rs) throws SQLException {
        MarketplaceListing listing = new MarketplaceListing();
        listing.setId(rs.getInt("id"));
        listing.setSellerId(rs.getInt("seller_id"));
        listing.setAssetType(rs.getString("asset_type"));
        listing.setWalletId(rs.getObject("wallet_id") != null ? rs.getInt("wallet_id") : null);
        listing.setQuantityOrTokens(rs.getDouble("quantity_or_id"));
        listing.setPricePerUnit(rs.getDouble("price_per_unit"));
        Object minPrice = rs.getObject("min_price_usd");
        listing.setMinPriceUsd(minPrice != null ? rs.getDouble("min_price_usd") : null);
        Object autoAccept = rs.getObject("auto_accept_price_usd");
        listing.setAutoAcceptPriceUsd(autoAccept != null ? rs.getDouble("auto_accept_price_usd") : null);
        listing.setTotalPriceUsd(rs.getDouble("total_price_usd"));
        listing.setStatus(rs.getString("status"));
        listing.setDescription(rs.getString("description"));
        listing.setMinimumBuyerRating(rs.getInt("minimum_buyer_rating"));
        listing.setCreatedAt(rs.getTimestamp("created_at"));
        listing.setExpiresAt(rs.getTimestamp("expires_at"));
        listing.setUpdatedAt(rs.getTimestamp("updated_at"));
        return listing;
    }
}
