package Utils;

/**
 * Custom exception hierarchy for marketplace operations
 * Provides typed exceptions for better error handling and debugging
 */
public class MarketplaceException extends RuntimeException {
    private final ErrorCode errorCode;
    private final String userMessage;

    public enum ErrorCode {
        // Validation errors
        INVALID_QUANTITY("INVALID_QUANTITY", "Quantity must be greater than 0"),
        INVALID_PRICE("INVALID_PRICE", "Price must be greater than 0"),
        INSUFFICIENT_BALANCE("INSUFFICIENT_BALANCE", "Insufficient balance for this transaction"),
        PRICE_BELOW_MINIMUM("PRICE_BELOW_MINIMUM", "Price is below acceptable minimum"),
        
        // Business logic errors
        LISTING_NOT_FOUND("LISTING_NOT_FOUND", "Listing not found"),
        ORDER_NOT_FOUND("ORDER_NOT_FOUND", "Order not found"),
        OFFER_NOT_FOUND("OFFER_NOT_FOUND", "Offer not found"),
        INVALID_ORDER_STATUS("INVALID_ORDER_STATUS", "Invalid order status for this operation"),
        
        // Payment errors
        PAYMENT_FAILED("PAYMENT_FAILED", "Payment processing failed"),
        ESCROW_ERROR("ESCROW_ERROR", "Escrow processing error"),
        STRIPE_ERROR("STRIPE_ERROR", "Stripe API error"),
        
        // Access control errors
        UNAUTHORIZED("UNAUTHORIZED", "User not authorized for this operation"),
        SELLER_NOT_VERIFIED("SELLER_NOT_VERIFIED", "Seller account not verified"),
        KYC_FAILED("KYC_FAILED", "KYC verification failed"),
        
        // System errors
        DATABASE_ERROR("DATABASE_ERROR", "Database operation failed"),
        INTERNAL_ERROR("INTERNAL_ERROR", "Internal server error");

        private final String code;
        private final String defaultMessage;

        ErrorCode(String code, String defaultMessage) {
            this.code = code;
            this.defaultMessage = defaultMessage;
        }

        public String getCode() {
            return code;
        }

        public String getDefaultMessage() {
            return defaultMessage;
        }
    }

    public MarketplaceException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
        this.userMessage = customMessage;
    }

    public MarketplaceException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.userMessage = errorCode.getDefaultMessage();
    }

    public MarketplaceException(ErrorCode errorCode, String customMessage, Throwable cause) {
        super(customMessage, cause);
        this.errorCode = errorCode;
        this.userMessage = customMessage;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public String getErrorCodeString() {
        return errorCode.getCode();
    }
}
