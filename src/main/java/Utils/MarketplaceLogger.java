package Utils;

import java.util.logging.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Marketplace operation logger for audit trail and debugging
 * Logs all critical marketplace operations for tracking and compliance
 */
public class MarketplaceLogger {
    private static final Logger logger = Logger.getLogger("MarketplaceLogger");
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static {
        // Initialize logger
        try {
            FileHandler fileHandler = new FileHandler("marketplace_operations.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            logger.setLevel(Level.ALL);
        } catch (Exception e) {
            System.err.println("ERROR initializing marketplace logger: " + e.getMessage());
        }
    }

    /**
     * Log successful listing creation
     */
    public static void logListingCreated(int listingId, long sellerId, String assetType, double quantity, double price) {
        String message = String.format(
            "LISTING_CREATED: ID=%d, Seller=%d, Asset=%s, Qty=%.2f, Price=$%.2f",
            listingId, sellerId, assetType, quantity, price
        );
        logger.info(message);
    }

    /**
     * Log successful order placement
     */
    public static void logOrderPlaced(int orderId, long buyerId, long sellerId, int listingId, double amount) {
        String message = String.format(
            "ORDER_PLACED: ID=%d, Buyer=%d, Seller=%d, Listing=%d, Amount=$%.2f",
            orderId, buyerId, sellerId, listingId, amount
        );
        logger.info(message);
    }

    /**
     * Log successful payment
     */
    public static void logPaymentProcessed(int orderId, String paymentId, double amount, String status) {
        String message = String.format(
            "PAYMENT_PROCESSED: OrderID=%d, PaymentID=%s, Amount=$%.2f, Status=%s",
            orderId, paymentId, amount, status
        );
        logger.info(message);
    }

    /**
     * Log refund
     */
    public static void logRefund(int orderId, String refundId, double amount, String reason) {
        String message = String.format(
            "REFUND_ISSUED: OrderID=%d, RefundID=%s, Amount=$%.2f, Reason=%s",
            orderId, refundId, amount, reason
        );
        logger.info(message);
    }

    /**
     * Log dispute creation
     */
    public static void logDisputeCreated(int disputeId, int orderId, long reporterId, String reason) {
        String message = String.format(
            "DISPUTE_CREATED: ID=%d, OrderID=%d, Reporter=%d, Reason=%s",
            disputeId, orderId, reporterId, reason
        );
        logger.info(message);
    }

    /**
     * Log dispute resolution
     */
    public static void logDisputeResolved(int disputeId, String resolution, long resolvedBy) {
        String message = String.format(
            "DISPUTE_RESOLVED: ID=%d, Resolution=%s, ResolvedBy=%d",
            disputeId, resolution, resolvedBy
        );
        logger.info(message);
    }

    /**
     * Log escrow hold
     */
    public static void logEscrowHeld(int escrowId, int orderId, double amount) {
        String message = String.format(
            "ESCROW_HELD: ID=%d, OrderID=%d, Amount=$%.2f",
            escrowId, orderId, amount
        );
        logger.info(message);
    }

    /**
     * Log escrow release
     */
    public static void logEscrowReleased(int escrowId, String releasedTo) {
        String message = String.format(
            "ESCROW_RELEASED: ID=%d, ReleasedTo=%s",
            escrowId, releasedTo
        );
        logger.info(message);
    }

    /**
     * Log fee recording
     */
    public static void logFeeRecorded(int feeId, int orderId, double amount, String feeType) {
        String message = String.format(
            "FEE_RECORDED: ID=%d, OrderID=%d, Amount=$%.2f, Type=%s",
            feeId, orderId, amount, feeType
        );
        logger.info(message);
    }

    /**
     * Log validation failure
     */
    public static void logValidationFailure(String operation, String reason) {
        String message = String.format(
            "VALIDATION_FAILED: Operation=%s, Reason=%s",
            operation, reason
        );
        logger.warning(message);
    }

    /**
     * Log security-related event (KYC, verification, etc)
     */
    public static void logSecurityEvent(String eventType, long userId, String description) {
        String message = String.format(
            "SECURITY_EVENT: Type=%s, UserID=%d, Description=%s, Timestamp=%s",
            eventType, userId, description, LocalDateTime.now().format(formatter)
        );
        logger.info(message);
    }

    /**
     * Log high-value transaction
     */
    public static void logHighValueTransaction(int orderId, double amount) {
        String message = String.format(
            "HIGH_VALUE_TRANSACTION: OrderID=%d, Amount=$%.2f, Timestamp=%s",
            orderId, amount, LocalDateTime.now().format(formatter)
        );
        logger.info(message);
    }

    /**
     * Log error event
     */
    public static void logError(String operation, String errorDetails) {
        String message = String.format(
            "ERROR: Operation=%s, Details=%s, Timestamp=%s",
            operation, errorDetails, LocalDateTime.now().format(formatter)
        );
        logger.severe(message);
    }

    /**
     * Log Stripe webhook event
     */
    public static void logWebhookEvent(String eventType, String eventId, String status) {
        String message = String.format(
            "WEBHOOK_EVENT: Type=%s, EventID=%s, Status=%s",
            eventType, eventId, status
        );
        logger.info(message);
    }

    /**
     * Get logger instance for direct use
     */
    public static Logger getLogger() {
        return logger;
    }
}
