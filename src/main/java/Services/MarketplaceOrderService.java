package Services;

import Models.MarketplaceOrder;
import DataBase.MyConnection;

import java.sql.*;
import java.util.*;

/**
 * Service for managing marketplace orders
 * Handles purchase transactions with payment processing and escrow
 */
public class MarketplaceOrderService {
    private static final String LOG_TAG = "[MarketplaceOrderService]";
    private static MarketplaceOrderService instance;
    private Connection conn;
    // Payment threshold: orders >= $10,000 go through escrow with verification
    // Orders < $10,000 processed instantly
    private static final double ESCROW_THRESHOLD_USD = 10000.0;


    private final StripePaymentService stripeService;
    private final CarbonPricingService pricingService;
    private final MarketplaceListingService listingService;

    private MarketplaceOrderService() {
        this.conn = MyConnection.getConnection();
        this.stripeService = StripePaymentService.getInstance();
        this.pricingService = CarbonPricingService.getInstance();
        this.listingService = MarketplaceListingService.getInstance();
    }

    public static MarketplaceOrderService getInstance() {
        if (instance == null) {
            instance = new MarketplaceOrderService();
        }
        return instance;
    }

    /**
     * Place an order from a marketplace listing
     * Creates order and initiates payment processing
     */
    public int placeOrder(int listingId, int buyerId, double quantity) {
        int orderId = -1;

        try {
            if (conn == null) return orderId;

            conn.setAutoCommit(false);

            try {
                // Fetch listing
                var listing = listingService.getListingById(listingId);
                if (listing == null || !"ACTIVE".equals(listing.getStatus())) {
                    System.err.println(LOG_TAG + " ERROR: Listing not active");
                    conn.rollback();
                    return orderId;
                }

                // Calculate amounts
                double unitPrice = listing.getPricePerUnit();
                double totalAmount = quantity * unitPrice;
                double platformFee = stripeService.calculatePlatformFee(totalAmount);

                // Determine payment flow based on amount
                boolean requiresEscrow = totalAmount >= ESCROW_THRESHOLD_USD;
                String initialStatus = requiresEscrow ? "PENDING_VERIFICATION" : "PENDING";
                
                System.out.println(LOG_TAG + String.format(" Order amount: $%.2f - %s", 
                    totalAmount, requiresEscrow ? "REQUIRES ESCROW" : "INSTANT PAYMENT"));

                // Create order in database with appropriate status
                String sql = "INSERT INTO marketplace_orders " +
                    "(listing_id, buyer_id, seller_id, quantity, unit_price_usd, total_amount_usd, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

                try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setInt(1, listingId);
                    stmt.setInt(2, buyerId);
                    stmt.setInt(3, listing.getSellerId());
                    stmt.setDouble(4, quantity);
                    stmt.setDouble(5, unitPrice);
                    stmt.setDouble(6, totalAmount);
                    stmt.setString(7, initialStatus);


                    stmt.executeUpdate();

                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            orderId = rs.getInt(1);
                        }
                    }
                }

                // Initiate Stripe payment
                String description = String.format(
                    "Marketplace Order #%d - %.2f tCO2e @ $%.2f/ton%s",
                    orderId, quantity, unitPrice,
                    requiresEscrow ? " [ESCROW REQUIRED]" : ""
                );

                var paymentIntent = stripeService.initiatePayment(
                    orderId, totalAmount, buyerId, listing.getSellerId(), description
                );

                if (paymentIntent != null) {
                    // Update order status based on flow type
                    String newStatus = requiresEscrow ? "PENDING_VERIFICATION" : "PAYMENT_PROCESSING";
                    String updateSql = "UPDATE marketplace_orders SET status = ? WHERE id = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                        stmt.setString(1, newStatus);
                        stmt.setInt(2, orderId);
                        stmt.executeUpdate();
                    }

                    conn.commit();
                    System.out.println(LOG_TAG + String.format(
                        " Order placed: ID %d | Buyer: %d | Amount: $%.2f | Flow: %s",
                        orderId, buyerId, totalAmount, 
                        requiresEscrow ? "ESCROW" : "INSTANT"));
                    return orderId;
                } else {
                    conn.rollback();
                    System.err.println(LOG_TAG + " ERROR: Payment initiation failed");
                    return -1;
                }

            } catch (SQLException e) {
                conn.rollback();
                System.err.println(LOG_TAG + " ERROR placing order: " + e.getMessage());
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR: Database connection failed");
        }

        return orderId;
    }

    /**
     * Complete an order after successful payment
     * Transfers credits and releases escrow
     */
    public boolean completeOrder(int orderId, String stripeChargeId) {
        try {
            if (conn == null) return false;

            conn.setAutoCommit(false);

            try {
                // Get order details
                MarketplaceOrder order = getOrderById(orderId);
                if (order == null) {
                    conn.rollback();
                    return false;
                }

                // Create escrow record
                int escrowId = createEscrow(orderId, null, order.getBuyerId(), 
                    order.getSellerId(), order.getTotalAmountUsd());

                if (escrowId <= 0) {
                    conn.rollback();
                    return false;
                }

                // Transfer credits from listing wallet to buyer wallet
                // (This would need to be implemented based on your wallet transfer logic)

                // Mark listing as sold
                if (order.getQuantity() >= 
                    listingService.getListingById(order.getListingId()).getQuantityOrTokens()) {
                    listingService.markAsSold(order.getListingId());
                }

                // Update order status
                String sql = "UPDATE marketplace_orders SET status = 'COMPLETED', " +
                        "stripe_payment_id = ?, completion_date = NOW(), updated_at = NOW() WHERE id = ?";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, stripeChargeId);
                    stmt.setInt(2, orderId);
                    stmt.executeUpdate();
                }

                // Release escrow to seller
                releaseEscrowToSeller(escrowId);

                // Create transaction fee record
                recordTransactionFee(orderId, null, order.getSellerId(), 
                    stripeService.calculatePlatformFee(order.getTotalAmountUsd()));

                conn.commit();
                System.out.println(LOG_TAG + " Order completed: ID " + orderId);
                return true;

            } catch (Exception e) {
                conn.rollback();
                System.err.println(LOG_TAG + " ERROR completing order: " + e.getMessage());
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR: Database connection failed");
        }

        return false;
    }

    /**
     * Cancel an order and refund the buyer
     */
    public boolean cancelOrder(int orderId, String reason) {
        try {
            if (conn == null) return false;

            var order = getOrderById(orderId);
            if (order == null || !"PENDING".equals(order.getStatus())) {
                System.err.println(LOG_TAG + " ERROR: Cannot cancel - order not pending");
                return false;
            }

            // Process refund through Stripe
            if (order.getStripePaymentId() != null) {
                long amountCents = (long) (order.getTotalAmountUsd() * 100);
                var refund = stripeService.refundPayment(order.getStripePaymentId(), amountCents, reason);

                if (refund != null) {
                    // Update order status
                    String sql = "UPDATE marketplace_orders SET status = 'REFUNDED', updated_at = NOW() WHERE id = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setInt(1, orderId);
                        stmt.executeUpdate();
                    }

                    System.out.println(LOG_TAG + " Order cancelled and refunded: ID " + orderId);
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR cancelling order: " + e.getMessage());
        }

        return false;
    }

    /**
     * Get order by ID
     */
    public MarketplaceOrder getOrderById(int orderId) {
        try {
            if (conn == null) return null;

            String sql = "SELECT * FROM marketplace_orders WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, orderId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultToOrder(rs);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR fetching order: " + e.getMessage());
        }

        return null;
    }

    /**
     * Get order history for a user
     */
    public List<MarketplaceOrder> getOrderHistory(int userId) {
        List<MarketplaceOrder> orders = new ArrayList<>();

        try {
            if (conn == null) return orders;

            String sql = "SELECT * FROM marketplace_orders WHERE buyer_id = ? OR seller_id = ? " +
                    "ORDER BY created_at DESC";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setInt(2, userId);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        orders.add(mapResultToOrder(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR fetching order history: " + e.getMessage());
        }

        return orders;
    }

    /**
     * Create escrow record
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
                        int escrowId = rs.getInt(1);
                        System.out.println(LOG_TAG + " Escrow created: ID " + escrowId);
                        return escrowId;
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
                int updated = stmt.executeUpdate();
                return updated > 0;
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
     * Map ResultSet to MarketplaceOrder
     */
    private MarketplaceOrder mapResultToOrder(ResultSet rs) throws SQLException {
        MarketplaceOrder order = new MarketplaceOrder(
            rs.getInt("listing_id"),
            rs.getInt("buyer_id"),
            rs.getInt("seller_id"),
            rs.getDouble("quantity"),
            rs.getDouble("unit_price_usd")
        );
        order.setId(rs.getInt("id"));
        order.setTotalAmountUsd(rs.getDouble("total_amount_usd"));
        order.setPlatformFeeUsd(rs.getDouble("platform_fee_usd"));
        order.setSellerProceedsUsd(rs.getDouble("seller_proceeds_usd"));
        order.setStripePaymentId(rs.getString("stripe_payment_id"));
        order.setStatus(rs.getString("status"));
        order.setCreatedAt(rs.getTimestamp("created_at"));
        order.setCompletionDate(rs.getTimestamp("completion_date"));
        order.setUpdatedAt(rs.getTimestamp("updated_at"));
        return order;
    }
}

