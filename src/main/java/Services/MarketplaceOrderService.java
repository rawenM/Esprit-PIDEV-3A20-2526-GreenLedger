package Services;

import Models.MarketplaceOrder;
import Services.BatchEventService;
import Models.BatchEventType;
import Models.CarbonCreditBatch;
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

        if (conn == null) {
            System.err.println(LOG_TAG + " ERROR: Database connection is null");
            return orderId;
        }

        boolean originalAutoCommit = true;
        try {
            // Save original autocommit state and disable it
            originalAutoCommit = conn.getAutoCommit();
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
                String initialStatus = requiresEscrow ? "PENDING" : "PENDING";
                
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
                    String newStatus = requiresEscrow ? "PAYMENT_PROCESSING" : "PAYMENT_PROCESSING";
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
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println(LOG_TAG + " ERROR: Rollback failed: " + rollbackEx.getMessage());
                }
                System.err.println(LOG_TAG + " ERROR placing order: " + e.getMessage());
                return orderId;
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR: Database transaction setup failed: " + e.getMessage());
            return orderId;
        } finally {
            // Always restore original autocommit state
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.setAutoCommit(originalAutoCommit);
                }
            } catch (SQLException e) {
                System.err.println(LOG_TAG + " ERROR: Failed to restore autocommit state: " + e.getMessage());
            }
        }
    }

    /**
     * Create an order from an accepted offer with negotiated unit price.
     * This method only creates the order record; payment is handled separately.
     */
    public int createOrderFromOffer(int listingId, int buyerId, int sellerId, double quantity, double unitPrice) {
        int orderId = -1;

        if (conn == null) {
            System.err.println(LOG_TAG + " ERROR: Database connection is null");
            return orderId;
        }

        boolean originalAutoCommit = true;
        try {
            // Save original autocommit state and disable it
            originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try {
                var listing = listingService.getListingById(listingId);
                if (listing == null || !"ACTIVE".equals(listing.getStatus())) {
                    System.err.println(LOG_TAG + " ERROR: Listing not active for offer order");
                    conn.rollback();
                    return orderId;
                }

                double totalAmount = quantity * unitPrice;

                String sql = "INSERT INTO marketplace_orders " +
                    "(listing_id, buyer_id, seller_id, quantity, unit_price_usd, total_amount_usd, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

                try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setInt(1, listingId);
                    stmt.setInt(2, buyerId);
                    stmt.setInt(3, sellerId);
                    stmt.setDouble(4, quantity);
                    stmt.setDouble(5, unitPrice);
                    stmt.setDouble(6, totalAmount);
                    stmt.setString(7, "PENDING");
                    stmt.executeUpdate();

                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            orderId = rs.getInt(1);
                        }
                    }
                }

                conn.commit();
                System.out.println(LOG_TAG + String.format(
                    " Offer order created: ID %d | Buyer: %d | Seller: %d | Qty: %.2f | Unit: $%.2f",
                    orderId, buyerId, sellerId, quantity, unitPrice
                ));
                return orderId;

            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println(LOG_TAG + " ERROR: Rollback failed: " + rollbackEx.getMessage());
                }
                System.err.println(LOG_TAG + " ERROR creating offer order: " + e.getMessage());
                return orderId;
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR: Database transaction setup failed: " + e.getMessage());
            return orderId;
        } finally {
            // Always restore original autocommit state
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.setAutoCommit(originalAutoCommit);
                }
            } catch (SQLException e) {
                System.err.println(LOG_TAG + " ERROR: Failed to restore autocommit state: " + e.getMessage());
            }
        }
    }

    /**
     * Complete an order after successful payment
     * Transfers credits and releases escrow
     */
    // DEPRECATED: Use TradeExecutionService.submitOrderTransfer(orderId) instead
    public boolean completeOrder(int orderId, String stripeChargeId) {
        throw new UnsupportedOperationException("Use TradeExecutionService.submitOrderTransfer() instead");
    }

    /**
     * Record which batches were purchased in this marketplace order.
     * Links marketplace_orders to carbon_credit_batches for provenance tracking.
     */
    private void recordOrderBatches(int orderId, int buyerWalletId, double quantity) {
        try {
            WalletService walletService = new WalletService();
            List<Models.CarbonCreditBatch> buyerBatches = walletService.getWalletBatches(buyerWalletId);
            
            // Find recently created batches (within last minute, up to quantity amount)
            double trackedAmount = 0;
            for (Models.CarbonCreditBatch batch : buyerBatches) {
                if (trackedAmount >= quantity) break;
                
                // Check if batch was recently created (within last minute)
                if (batch.getIssuedAt() != null && 
                    java.time.Duration.between(batch.getIssuedAt(), java.time.LocalDateTime.now()).getSeconds() < 60) {
                    
                    double batchAmount = Math.min(
                        batch.getTotalAmount().doubleValue(), 
                        quantity - trackedAmount
                    );
                    
                    String sql = "INSERT INTO marketplace_order_batches (order_id, batch_id, quantity) " +
                                "VALUES (?, ?, ?)";
                    
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, orderId);
                        ps.setInt(2, batch.getId());
                        ps.setDouble(3, batchAmount);
                        ps.executeUpdate();
                        
                        trackedAmount += batchAmount;
                    } catch (SQLException ex) {
                        // Table might not exist yet, log and continue
                        System.err.println(LOG_TAG + " Warning: Could not record order batch linkage: " + ex.getMessage());
                    }
                }
            }
            
            System.out.println(LOG_TAG + " Tracked " + trackedAmount + " tCO2 in batches for order " + orderId);
            
        } catch (Exception e) {
            // Non-critical failure, log and continue
            System.err.println(LOG_TAG + " Warning: Error recording order batches: " + e.getMessage());
        }
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
        System.out.println(LOG_TAG + " [createEscrow] Called with orderId=" + orderId + ", buyerId=" + buyerId + ", sellerId=" + sellerId + ", amount=" + amountUsd);
        try {
            if (conn == null) {
                System.err.println(LOG_TAG + " [createEscrow] ERROR: Connection is null");
                return -1;
            }

            // Note: Only using order_id here. trade_id is for peer trades (handled separately)
            String sql = "INSERT INTO marketplace_escrow " +
                    "(order_id, buyer_id, seller_id, amount_usd, status) " +
                    "VALUES (?, ?, ?, ?, 'HELD')";

            System.out.println(LOG_TAG + " [createEscrow] Executing INSERT into marketplace_escrow");
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setObject(1, orderId);
                stmt.setInt(2, buyerId);
                stmt.setInt(3, sellerId);
                stmt.setDouble(4, amountUsd);

                int rowsAffected = stmt.executeUpdate();
                System.out.println(LOG_TAG + " [createEscrow] INSERT executed, rows affected: " + rowsAffected);

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int escrowId = rs.getInt(1);
                        System.out.println(LOG_TAG + " [createEscrow] ✓ Escrow created successfully: ID " + escrowId);
                        return escrowId;
                    } else {
                        System.err.println(LOG_TAG + " [createEscrow] ERROR: No generated keys returned!");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " [createEscrow] SQL ERROR: " + e.getMessage());
            System.err.println(LOG_TAG + " [createEscrow] SQL State: " + e.getSQLState());
            System.err.println(LOG_TAG + " [createEscrow] Error Code: " + e.getErrorCode());
            e.printStackTrace();
        }

        System.err.println(LOG_TAG + " [createEscrow] Returning -1 (failure)");
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
     * Get all HELD escrows waiting verification
     */
    public List<Models.MarketplaceEscrow> getHeldEscrows() {
        List<Models.MarketplaceEscrow> escrows = new ArrayList<>();
        try {
            System.out.println("[ESCROW SERVICE DEBUG] getHeldEscrows() called");
            System.out.println("[ESCROW SERVICE DEBUG] conn = " + (conn == null ? "NULL" : "Connected"));
            if (conn == null) {
                System.out.println("[ESCROW SERVICE DEBUG] Connection is null, returning empty list");
                return escrows;
            }

            String sql = "SELECT * FROM marketplace_escrow WHERE status = 'HELD' ORDER BY created_at ASC";
            System.out.println("[ESCROW SERVICE DEBUG] Executing SQL: " + sql);
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    int count = 0;
                    while (rs.next()) {
                        Models.MarketplaceEscrow escrow = mapResultToEscrow(rs);
                        escrows.add(escrow);
                        count++;
                        System.out.println("[ESCROW SERVICE DEBUG] Loaded escrow " + count + ": ID=" + escrow.getId() + ", Status=" + escrow.getStatus());
                    }
                    System.out.println("[ESCROW SERVICE DEBUG] Query returned " + count + " rows");
                }
            }
            System.out.println("[ESCROW SERVICE DEBUG] Returning " + escrows.size() + " held escrows");
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR fetching held escrows: " + e.getMessage());
            e.printStackTrace();
        }
        return escrows;
    }

    /**
     * DEBUG METHOD: Get ALL escrows regardless of status
     */
    public void debugPrintAllEscrows() {
        try {
            if (conn == null) {
                System.out.println("[ESCROW DEBUG] Connection is null");
                return;
            }

            String sql = "SELECT id, order_id, buyer_id, seller_id, amount_usd, status, created_at FROM marketplace_escrow";
            System.out.println("[ESCROW DEBUG] Running debug query: " + sql);
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    int count = 0;
                    while (rs.next()) {
                        count++;
                        System.out.println(String.format("[ESCROW DEBUG] Row %d: ID=%d, OrderID=%d, BuyerID=%d, SellerID=%d, Amount=%.2f USD, Status=%s, Created=%s",
                            count,
                            rs.getInt("id"),
                            rs.getInt("order_id"),
                            rs.getInt("buyer_id"),
                            rs.getInt("seller_id"),
                            rs.getDouble("amount_usd"),
                            rs.getString("status"),
                            rs.getString("created_at")
                        ));
                    }
                    if (count == 0) {
                        System.out.println("[ESCROW DEBUG] No escrows found in database");
                    } else {
                        System.out.println("[ESCROW DEBUG] Total escrows in database: " + count);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[ESCROW DEBUG] Error querying escrows: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * DEBUG METHOD: Create a test escrow
     */
    public void debugCreateTestEscrow() {
        try {
            if (conn == null) {
                System.out.println("[ESCROW DEBUG] Cannot create test escrow: connection is null");
                return;
            }

            String sql = "INSERT INTO marketplace_escrow (order_id, buyer_id, seller_id, amount_usd, status, created_at, hours_held) " +
                         "VALUES (?, ?, ?, ?, ?, NOW(), 0)";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, 1);           // order_id
                stmt.setInt(2, 1);           // buyer_id (admin)
                stmt.setInt(3, 2);           // seller_id (first user)
                stmt.setDouble(4, 15000.0);  // amount_usd ($15,000)
                stmt.setString(5, "HELD");   // status
                
                int rows = stmt.executeUpdate();
                System.out.println("[ESCROW DEBUG] Test escrow created: " + rows + " rows inserted");
            }
        } catch (SQLException e) {
            System.out.println("[ESCROW DEBUG] Could not create test escrow (probably already exists): " + e.getMessage());
        }
    }

    /**
     * Verify and manually release escrow to seller (admin action)
     */
    public boolean verifyAndReleaseEscrow(int escrowId) {
        if (conn == null) return false;

        boolean originalAutoCommit = true;
        try {
            originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try {
                // Get escrow details
                String selectSql = "SELECT * FROM marketplace_escrow WHERE id = ? AND status = 'HELD'";
                Models.MarketplaceEscrow escrow = null;
                try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
                    stmt.setInt(1, escrowId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            escrow = mapResultToEscrow(rs);
                        }
                    }
                }

                if (escrow == null) {
                    System.err.println(LOG_TAG + " ERROR: Escrow not found or not held: " + escrowId);
                    return false;
                }

                // Calculate fees and proceeds
                double platformFee = escrow.getAmountUsd() * 0.03; // 3% fee
                double sellerProceeds = escrow.getAmountUsd() - platformFee;

                if (escrow.getOrderId() != null) {
                    // Update order status to COMPLETED
                    String orderUpdateSql = "UPDATE marketplace_orders SET status = 'COMPLETED', " +
                            "platform_fee_usd = ?, seller_proceeds_usd = ?, updated_at = NOW() WHERE id = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(orderUpdateSql)) {
                        stmt.setDouble(1, platformFee);
                        stmt.setDouble(2, sellerProceeds);
                        stmt.setInt(3, escrow.getOrderId());
                        stmt.executeUpdate();
                    }
                }

                // Release escrow
                String updateSql = "UPDATE marketplace_escrow SET status = 'RELEASED_TO_SELLER', " +
                        "release_date = NOW() WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                    stmt.setInt(1, escrowId);
                    stmt.executeUpdate();
                }

                // Record fee
                recordTransactionFee(escrow.getOrderId(), null, (int)escrow.getSellerId(), platformFee);

                conn.commit();
                System.out.println(LOG_TAG + " Escrow verified & released: ID " + escrowId + 
                    " | Amount: $" + String.format("%.2f", escrow.getAmountUsd()));
                return true;

            } catch (Exception e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println(LOG_TAG + " ERROR: Rollback failed: " + rollbackEx.getMessage());
                }
                System.err.println(LOG_TAG + " ERROR verifying/releasing escrow: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR: Database transaction setup failed: " + e.getMessage());
            return false;
        } finally {
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.setAutoCommit(originalAutoCommit);
                }
            } catch (SQLException e) {
                System.err.println(LOG_TAG + " ERROR: Failed to restore autocommit state: " + e.getMessage());
            }
        }
    }

    /**
     * Auto-release escrow after 24 hours
     * Runs periodically to check and release old held escrows
     */
    public void autoReleaseOldEscrows() {
        try {
            if (conn == null) return;

            // Find escrows held for more than 24 hours
            String sql = "SELECT * FROM marketplace_escrow WHERE status = 'HELD' " +
                    "AND created_at < DATE_SUB(NOW(), INTERVAL 24 HOUR)";
            List<Models.MarketplaceEscrow> oldEscrows = new ArrayList<>();

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        oldEscrows.add(mapResultToEscrow(rs));
                    }
                }
            }

            // Auto-release each old escrow
            for (Models.MarketplaceEscrow escrow : oldEscrows) {
                verifyAndReleaseEscrow(escrow.getId());
            }

            if (!oldEscrows.isEmpty()) {
                System.out.println(LOG_TAG + " Auto-released " + oldEscrows.size() + " escrows after 24h hold");
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR in auto-release process: " + e.getMessage());
        }
    }

    /**
     * Refund escrow to buyer (if buyer disputes or requests cancellation)
     */
    public boolean refundEscrowToBuyer(int escrowId) {
        try {
            if (conn == null) return false;

            String sql = "UPDATE marketplace_escrow SET status = 'REFUNDED_TO_BUYER', " +
                    "release_date = NOW() WHERE id = ? AND status = 'HELD'";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, escrowId);
                int updated = stmt.executeUpdate();
                if (updated > 0) {
                    System.out.println(LOG_TAG + " Escrow refunded to buyer: ID " + escrowId);
                }
                return updated > 0;
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR refunding escrow: " + e.getMessage());
        }
        return false;
    }

    /**
     * Map ResultSet to MarketplaceEscrow
     */
    private Models.MarketplaceEscrow mapResultToEscrow(ResultSet rs) throws SQLException {
        Models.MarketplaceEscrow escrow = new Models.MarketplaceEscrow();
        escrow.setId(rs.getInt("id"));
        escrow.setOrderId(rs.getObject("order_id") != null ? rs.getInt("order_id") : null);
        escrow.setBuyerId((int)rs.getLong("buyer_id"));
        escrow.setSellerId((int)rs.getLong("seller_id"));
        escrow.setAmountUsd(rs.getDouble("amount_usd"));
        escrow.setStatus(rs.getString("status"));
        java.sql.Timestamp createdTs = rs.getTimestamp("created_at");
        if (createdTs != null) {
            escrow.setCreatedAt(createdTs);
        }
        java.sql.Timestamp releaseTs = rs.getTimestamp("release_date");
        if (releaseTs != null) {
            escrow.setReleaseDate(releaseTs);
        }
        return escrow;
    }

    /**
     * Map ResultSet to MarketplaceOrder.
     * Optional / legacy columns (stripe_payment_id, platform_fee_usd,
     * seller_proceeds_usd, completion_date) are read defensively so that
     * queries still work even when those columns are absent from the schema.
     */
    private MarketplaceOrder mapResultToOrder(ResultSet rs) throws SQLException {
        // Core columns that must exist
        int listingId  = 0;
        try { listingId = rs.getInt("listing_id"); } catch (SQLException ignored) {}

        double qty   = 0;
        try { qty   = rs.getDouble("quantity"); }      catch (SQLException ignored) {}

        double price = 0;
        try { price = rs.getDouble("unit_price_usd"); } catch (SQLException ignored) {}

        MarketplaceOrder order = new MarketplaceOrder(
            listingId,
            rs.getInt("buyer_id"),
            rs.getInt("seller_id"),
            qty,
            price
        );
        order.setId(rs.getInt("id"));

        try { order.setTotalAmountUsd(rs.getDouble("total_amount_usd")); }
        catch (SQLException ignored) {}

        // Optional legacy columns — skip if column doesn't exist
        try { order.setPlatformFeeUsd(rs.getDouble("platform_fee_usd")); }
        catch (SQLException ignored) {}

        try { order.setSellerProceedsUsd(rs.getDouble("seller_proceeds_usd")); }
        catch (SQLException ignored) {}

        try { order.setStripePaymentId(rs.getString("stripe_payment_id")); }
        catch (SQLException ignored) {}

        order.setStatus(rs.getString("status"));
        order.setCreatedAt(rs.getTimestamp("created_at"));

        try { order.setCompletionDate(rs.getTimestamp("completion_date")); }
        catch (SQLException ignored) {}

        try { order.setUpdatedAt(rs.getTimestamp("updated_at")); }
        catch (SQLException ignored) {}

        return order;
    }
}

