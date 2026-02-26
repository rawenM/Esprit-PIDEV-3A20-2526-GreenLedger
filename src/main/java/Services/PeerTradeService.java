package Services;

import Models.PeerTrade;
import DataBase.MyConnection;

import java.sql.*;
import java.util.*;

/**
 * Service for managing peer-to-peer direct trades
 * Handles trade proposals, negotiations, and settlements
 */
public class PeerTradeService {
    private static final String LOG_TAG = "[PeerTradeService]";
    private static PeerTradeService instance;
    private Connection conn;

    private final StripePaymentService stripeService;

    private PeerTradeService() {
        this.conn = MyConnection.getConnection();
        this.stripeService = StripePaymentService.getInstance();
    }

    public static PeerTradeService getInstance() {
        if (instance == null) {
            instance = new PeerTradeService();
        }
        return instance;
    }

    /**
     * Initiate a direct peer-to-peer trade offer
     */
    public int initiateTrade(int initiatorId, int responderId, String assetType,
                            double quantity, double proposedPriceUsd,
                            Integer initiatorWalletId, Integer responderWalletId) {
        int tradeId = -1;

        try {
            if (conn == null) return tradeId;

            String sql = "INSERT INTO peer_trades " +
                    "(initiator_id, responder_id, asset_type, quantity, proposed_price_usd, " +
                    "initiator_wallet_id, responder_wallet_id, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, 'PROPOSED')";

            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, initiatorId);
                stmt.setInt(2, responderId);
                stmt.setString(3, assetType);
                stmt.setDouble(4, quantity);
                stmt.setDouble(5, proposedPriceUsd);
                stmt.setObject(6, initiatorWalletId);
                stmt.setObject(7, responderWalletId);

                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        tradeId = rs.getInt(1);
                        System.out.println(LOG_TAG + " Trade initiated: ID " + tradeId + 
                            " from user " + initiatorId + " to user " + responderId);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR initiating trade: " + e.getMessage());
        }

        return tradeId;
    }

    /**
     * Accept a trade proposal (responder accepts initiator's terms)
     */
    public boolean acceptTrade(int tradeId) {
        return updateTradeStatus(tradeId, "ACCEPTED");
    }

    /**
     * Counter-offer (propose different terms)
     */
    public boolean counterOffer(int tradeId, double newProposedPrice) {
        try {
            if (conn == null) return false;

            String sql = "UPDATE peer_trades SET proposed_price_usd = ?, status = 'NEGOTIATING', " +
                    "updated_at = NOW() WHERE id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setDouble(1, newProposedPrice);
                stmt.setInt(2, tradeId);

                int updated = stmt.executeUpdate();
                if (updated > 0) {
                    System.out.println(LOG_TAG + " Counter-offer made for trade " + tradeId);
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR making counter-offer: " + e.getMessage());
        }

        return false;
    }

    /**
     * Agree on final price (both parties accept)
     */
    public boolean agreeOnPrice(int tradeId, double agreedPrice) {
        try {
            if (conn == null) return false;

            String sql = "UPDATE peer_trades SET agreed_price_usd = ?, status = 'ACCEPTED', " +
                    "updated_at = NOW() WHERE id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setDouble(1, agreedPrice);
                stmt.setInt(2, tradeId);

                int updated = stmt.executeUpdate();
                if (updated > 0) {
                    System.out.println(LOG_TAG + " Price agreed for trade " + tradeId + ": $" + agreedPrice);
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR agreeing on price: " + e.getMessage());
        }

        return false;
    }

    /**
     * Settle a trade (process payment and transfer assets)
     */
    public boolean settleTrade(int tradeId, String stripeChargeId) {
        try {
            if (conn == null) return false;

            conn.setAutoCommit(false);

            try {
                // Get trade details
                PeerTrade trade = getTradeById(tradeId);
                if (trade == null || !trade.isPending()) {
                    conn.rollback();
                    return false;
                }

                double finalPrice = trade.getFinalPrice();
                double totalValue = trade.getTotalValue();

                // Create escrow record
                int escrowId = createEscrow(null, tradeId, trade.getInitiatorId(), 
                    trade.getResponderId(), totalValue);

                if (escrowId <= 0) {
                    conn.rollback();
                    return false;
                }

                // Update trade with Stripe charge ID and agree price if not set
                String updateSql = "UPDATE peer_trades SET stripe_payment_id = ?, " +
                        "agreed_price_usd = ?, escrow_id = ?, status = 'SETTLED', " +
                        "settlement_date = NOW(), updated_at = NOW() WHERE id = ?";

                try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                    stmt.setString(1, stripeChargeId);
                    stmt.setDouble(2, finalPrice);
                    stmt.setInt(3, escrowId);
                    stmt.setInt(4, tradeId);
                    stmt.executeUpdate();
                }

                // Release escrow to seller (responder receives payment)
                releaseEscrowToSeller(escrowId);

                // Record transaction fee
                recordTransactionFee(null, tradeId, trade.getResponderId(), 
                    stripeService.calculatePlatformFee(totalValue));

                conn.commit();
                System.out.println(LOG_TAG + " Trade settled: ID " + tradeId + " Value: $" + totalValue);
                return true;

            } catch (Exception e) {
                conn.rollback();
                System.err.println(LOG_TAG + " ERROR settling trade: " + e.getMessage());
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR: Database connection failed");
        }

        return false;
    }

    /**
     * Deny a trade proposal
     */
    public boolean denyTrade(int tradeId, String reason) {
        try {
            if (conn == null) return false;

            String sql = "UPDATE peer_trades SET status = 'CANCELLED', " +
                    "updated_at = NOW() WHERE id = ? AND status IN ('PROPOSED', 'NEGOTIATING')";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, tradeId);

                int updated = stmt.executeUpdate();
                if (updated > 0) {
                    System.out.println(LOG_TAG + " Trade denied: ID " + tradeId + " Reason: " + reason);
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR denying trade: " + e.getMessage());
        }

        return false;
    }

    /**
     * Get trade by ID
     */
    public PeerTrade getTradeById(int tradeId) {
        try {
            if (conn == null) return null;

            String sql = "SELECT * FROM peer_trades WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, tradeId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultToTrade(rs);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR fetching trade: " + e.getMessage());
        }

        return null;
    }

    /**
     * Get pending trade offers for a user
     */
    public List<PeerTrade> getPendingTradesForUser(int userId) {
        List<PeerTrade> trades = new ArrayList<>();

        try {
            if (conn == null) return trades;

            String sql = "SELECT * FROM peer_trades WHERE responder_id = ? " +
                    "AND status IN ('PROPOSED', 'NEGOTIATING') ORDER BY created_at DESC";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        trades.add(mapResultToTrade(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR fetching pending trades: " + e.getMessage());
        }

        return trades;
    }

    /**
     * Get all trades for a user (both initiated and received)
     */
    public List<PeerTrade> getUserTrades(int userId) {
        List<PeerTrade> trades = new ArrayList<>();

        try {
            if (conn == null) return trades;

            String sql = "SELECT * FROM peer_trades WHERE initiator_id = ? OR responder_id = ? " +
                    "ORDER BY created_at DESC";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setInt(2, userId);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        trades.add(mapResultToTrade(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR fetching user trades: " + e.getMessage());
        }

        return trades;
    }

    /**
     * Create escrow for trade
     */
    private int createEscrow(Integer orderId, Integer tradeId, int buyerId, 
                            int sellerId, double amountUsd) {
        try {
            if (conn == null) return -1;

            String sql = "INSERT INTO marketplace_escrow " +
                    "(order_id, trade_id, buyer_id, seller_id, amount_usd, status) " +
                    "VALUES (?, ?, ?, ?, ?, 'HELD')";

            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setObject(1, orderId);
                stmt.setObject(2, tradeId);
                stmt.setInt(3, buyerId);
                stmt.setInt(4, sellerId);
                stmt.setDouble(5, amountUsd);

                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR creating escrow: " + e.getMessage());
        }

        return -1;
    }

    /**
     * Release escrow to seller
     */
    private boolean releaseEscrowToSeller(int escrowId) {
        try {
            if (conn == null) return false;

            String sql = "UPDATE marketplace_escrow SET status = 'RELEASED_TO_SELLER', " +
                    "release_date = NOW() WHERE id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, escrowId);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR releasing escrow: " + e.getMessage());
        }

        return false;
    }

    /**
     * Record transaction fee
     */
    private void recordTransactionFee(Integer orderId, Integer tradeId, int sellerId, double feeAmount) {
        try {
            if (conn == null) return;

            String sql = "INSERT INTO marketplace_fees " +
                    "(order_id, trade_id, seller_id, fee_amount_usd, fee_type, status) " +
                    "VALUES (?, ?, ?, ?, 'TRANSACTION_FEE', 'PENDING')";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setObject(1, orderId);
                stmt.setObject(2, tradeId);
                stmt.setInt(3, sellerId);
                stmt.setDouble(4, feeAmount);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR recording fee: " + e.getMessage());
        }
    }

    /**
     * Update trade status
     */
    private boolean updateTradeStatus(int tradeId, String newStatus) {
        try {
            if (conn == null) return false;

            String sql = "UPDATE peer_trades SET status = ?, updated_at = NOW() WHERE id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, newStatus);
                stmt.setInt(2, tradeId);

                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR updating trade status: " + e.getMessage());
        }

        return false;
    }

    /**
     * Map ResultSet to PeerTrade
     */
    private PeerTrade mapResultToTrade(ResultSet rs) throws SQLException {
        PeerTrade trade = new PeerTrade(
            rs.getInt("initiator_id"),
            rs.getInt("responder_id"),
            rs.getString("asset_type"),
            rs.getDouble("quantity"),
            rs.getDouble("proposed_price_usd")
        );
        trade.setId(rs.getInt("id"));
        trade.setAgreedPriceUsd(rs.getObject("agreed_price_usd") != null ? 
            rs.getDouble("agreed_price_usd") : null);
        trade.setInitiatorWalletId(rs.getObject("initiator_wallet_id") != null ? 
            rs.getInt("initiator_wallet_id") : null);
        trade.setResponderWalletId(rs.getObject("responder_wallet_id") != null ? 
            rs.getInt("responder_wallet_id") : null);
        trade.setStripePaymentId(rs.getString("stripe_payment_id"));
        trade.setStatus(rs.getString("status"));
        trade.setEscrowId(rs.getObject("escrow_id") != null ? rs.getInt("escrow_id") : null);
        trade.setCreatedAt(rs.getTimestamp("created_at"));
        trade.setSettlementDate(rs.getTimestamp("settlement_date"));
        trade.setUpdatedAt(rs.getTimestamp("updated_at"));
        return trade;
    }
}

