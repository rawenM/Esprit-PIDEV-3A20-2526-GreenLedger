package Services;

import DataBase.MyConnection;
import Models.MarketplaceListing;
import Models.MarketplaceOffer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for marketplace negotiation offers.
 */
public class MarketplaceOfferService {
    private static final String LOG_TAG = "[MarketplaceOfferService]";
    private static MarketplaceOfferService instance;
    private Connection conn;

    private final MarketplaceListingService listingService = MarketplaceListingService.getInstance();

    private MarketplaceOfferService() {
        this.conn = MyConnection.getConnection();
    }

    public static MarketplaceOfferService getInstance() {
        if (instance == null) {
            instance = new MarketplaceOfferService();
        }
        return instance;
    }

    public int createOffer(int listingId, long buyerId, double quantity, double offerPriceUsd) {
        if (conn == null) return -1;

        MarketplaceListing listing = listingService.getListingById(listingId);
        if (listing == null || !"ACTIVE".equals(listing.getStatus())) {
            System.err.println(LOG_TAG + " Listing not active");
            return -1;
        }

        if (offerPriceUsd < listing.getMinPriceUsd()) {
            System.err.println(LOG_TAG + " Offer below minimum price");
            return -1;
        }

        String status = "PENDING";
        if (listing.getAutoAcceptPriceUsd() != null && offerPriceUsd >= listing.getAutoAcceptPriceUsd()) {
            status = "ACCEPTED";
        }

        String sql = "INSERT INTO marketplace_offers " +
            "(listing_id, buyer_id, seller_id, quantity, offer_price_usd, status) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, listingId);
            stmt.setLong(2, buyerId);
            stmt.setLong(3, listing.getSellerId());
            stmt.setDouble(4, quantity);
            stmt.setDouble(5, offerPriceUsd);
            stmt.setString(6, status);
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR creating offer: " + e.getMessage());
        }

        return -1;
    }

    public List<MarketplaceOffer> getOffersReceived(long sellerId) {
        return getOffersByRole("seller_id", sellerId);
    }

    public List<MarketplaceOffer> getOffersSent(long buyerId) {
        return getOffersByRole("buyer_id", buyerId);
    }

    public MarketplaceOffer getAcceptedOfferForBuyer(int listingId, long buyerId) {
        String sql = "SELECT * FROM marketplace_offers " +
                "WHERE listing_id = ? AND buyer_id = ? AND status = 'ACCEPTED' " +
                "ORDER BY updated_at DESC LIMIT 1";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, listingId);
            stmt.setLong(2, buyerId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultToOffer(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR fetching accepted offer: " + e.getMessage());
        }
        return null;
    }

    public boolean acceptOffer(int offerId) {
        return updateOfferStatus(offerId, "ACCEPTED", null);
    }

    public boolean rejectOffer(int offerId) {
        return updateOfferStatus(offerId, "REJECTED", null);
    }

    public boolean counterOffer(int offerId, double counterPrice) {
        return updateOfferStatus(offerId, "COUNTERED", counterPrice);
    }

    public boolean cancelOffer(int offerId, long buyerId) {
        String sql = "UPDATE marketplace_offers SET status = 'CANCELLED' " +
                "WHERE id = ? AND buyer_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, offerId);
            stmt.setLong(2, buyerId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR cancelling offer: " + e.getMessage());
        }
        return false;
    }

    private boolean updateOfferStatus(int offerId, String status, Double counterPrice) {
        String sql = "UPDATE marketplace_offers SET status = ?, counter_price_usd = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            if (counterPrice != null) {
                stmt.setDouble(2, counterPrice);
            } else {
                stmt.setNull(2, Types.DECIMAL);
            }
            stmt.setInt(3, offerId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR updating offer: " + e.getMessage());
        }
        return false;
    }

    private List<MarketplaceOffer> getOffersByRole(String column, long userId) {
        List<MarketplaceOffer> offers = new ArrayList<>();
        String sql = "SELECT * FROM marketplace_offers WHERE " + column + " = ? ORDER BY created_at DESC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    offers.add(mapResultToOffer(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR fetching offers: " + e.getMessage());
        }
        return offers;
    }

    private MarketplaceOffer mapResultToOffer(ResultSet rs) throws SQLException {
        MarketplaceOffer offer = new MarketplaceOffer();
        offer.setId(rs.getInt("id"));
        offer.setListingId(rs.getInt("listing_id"));
        offer.setBuyerId(rs.getLong("buyer_id"));
        offer.setSellerId(rs.getLong("seller_id"));
        offer.setQuantity(rs.getDouble("quantity"));
        offer.setOfferPriceUsd(rs.getDouble("offer_price_usd"));
        offer.setStatus(rs.getString("status"));
        Object counter = rs.getObject("counter_price_usd");
        offer.setCounterPriceUsd(counter != null ? rs.getDouble("counter_price_usd") : null);
        offer.setExpiresAt(rs.getTimestamp("expires_at"));
        offer.setCreatedAt(rs.getTimestamp("created_at"));
        offer.setUpdatedAt(rs.getTimestamp("updated_at"));
        return offer;
    }
}
