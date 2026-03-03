package Utils;

/**
 * Input validation utility for marketplace operations
 * Ensures all inputs are valid before processing
 * Follows fail-fast principle to catch errors early
 */
public class MarketplaceValidator {
    private static final String LOG_TAG = "[MarketplaceValidator]";

    /**
     * Validate quantity is positive
     */
    public static void validateQuantity(double quantity) {
        if (quantity <= 0) {
            throw new MarketplaceException(
                MarketplaceException.ErrorCode.INVALID_QUANTITY,
                "Quantity must be greater than 0, received: " + quantity
            );
        }
    }

    /**
     * Validate quantity within bounds
     */
    public static void validateQuantity(double quantity, double maximum) {
        validateQuantity(quantity);
        if (quantity > maximum) {
            throw new MarketplaceException(
                MarketplaceException.ErrorCode.INVALID_QUANTITY,
                "Quantity " + quantity + " exceeds maximum available: " + maximum
            );
        }
    }

    /**
     * Validate price is valid
     */
    public static void validatePrice(double price) {
        if (price <= 0) {
            throw new MarketplaceException(
                MarketplaceException.ErrorCode.INVALID_PRICE,
                "Price must be greater than 0, received: $" + price
            );
        }
    }

    /**
     * Validate price is within valid range
     */
    public static void validatePrice(double price, Double minimumPrice, Double maximumPrice) {
        validatePrice(price);

        if (minimumPrice != null && price < minimumPrice) {
            throw new MarketplaceException(
                MarketplaceException.ErrorCode.PRICE_BELOW_MINIMUM,
                "Price $" + price + " is below minimum: $" + minimumPrice
            );
        }

        if (maximumPrice != null && price > maximumPrice) {
            throw new MarketplaceException(
                MarketplaceException.ErrorCode.INVALID_PRICE,
                "Price $" + price + " exceeds maximum: $" + maximumPrice
            );
        }
    }

    /**
     * Validate balance is sufficient
     */
    public static void validateBalanceSufficient(double balance, double required) {
        if (balance < required) {
            throw new MarketplaceException(
                MarketplaceException.ErrorCode.INSUFFICIENT_BALANCE,
                "Balance $" + String.format("%.2f", balance) + 
                " is insufficient for required amount: $" + String.format("%.2f", required)
            );
        }
    }

    /**
     * Validate user ID exists
     */
    public static void validateUserId(long userId) {
        if (userId <= 0) {
            throw new MarketplaceException(
                MarketplaceException.ErrorCode.INVALID_QUANTITY,
                "Invalid user ID: " + userId
            );
        }
    }

    /**
     * Validate order ID exists
     */
    public static void validateOrderId(int orderId) {
        if (orderId <= 0) {
            throw new MarketplaceException(
                MarketplaceException.ErrorCode.ORDER_NOT_FOUND,
                "Invalid order ID: " + orderId
            );
        }
    }

    /**
     * Validate listing ID exists
     */
    public static void validateListingId(int listingId) {
        if (listingId <= 0) {
            throw new MarketplaceException(
                MarketplaceException.ErrorCode.LISTING_NOT_FOUND,
                "Invalid listing ID: " + listingId
            );
        }
    }

    /**
     * Validate string is not empty
     */
    public static void validateNotEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new MarketplaceException(
                MarketplaceException.ErrorCode.INVALID_QUANTITY,
                fieldName + " cannot be empty"
            );
        }
    }

    /**
     * Validate email format is valid
     */
    public static void validateEmail(String email) {
        validateNotEmpty(email, "Email");
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new MarketplaceException(
                MarketplaceException.ErrorCode.INVALID_QUANTITY,
                "Invalid email format: " + email
            );
        }
    }

    /**
     * Validate KYC requirements for transaction
     */
    public static void validateKycForAmount(boolean kycVerified, double amountUsd) {
        // Unverified users can only trade small amounts
        if (!kycVerified && amountUsd > 5000) {
            throw new MarketplaceException(
                MarketplaceException.ErrorCode.KYC_FAILED,
                "KYC verification required for transactions over $5000"
            );
        }
    }

    /**
     * Validate seller is verified for listing
     */
    public static void validateSellerVerification(boolean sellerVerified) {
        if (!sellerVerified) {
            throw new MarketplaceException(
                MarketplaceException.ErrorCode.SELLER_NOT_VERIFIED,
                "Seller account must be verified to list items"
            );
        }
    }

    /**
     * Validate order status transition is legal
     */
    public static void validateOrderStatusTransition(String currentStatus, String newStatus) {
        // Define legal transitions
        switch (currentStatus) {
            case "PENDING":
                if (!newStatus.equals("PAID") && !newStatus.equals("CANCELLED")) {
                    throw new MarketplaceException(
                        MarketplaceException.ErrorCode.INVALID_ORDER_STATUS,
                        "Cannot transition from PENDING to " + newStatus
                    );
                }
                break;
            case "PAID":
                if (!newStatus.equals("COMPLETED") && !newStatus.equals("DISPUTED") && !newStatus.equals("REFUNDED")) {
                    throw new MarketplaceException(
                        MarketplaceException.ErrorCode.INVALID_ORDER_STATUS,
                        "Cannot transition from PAID to " + newStatus
                    );
                }
                break;
            case "COMPLETED":
            case "REFUNDED":
                throw new MarketplaceException(
                    MarketplaceException.ErrorCode.INVALID_ORDER_STATUS,
                    "Cannot change status from " + currentStatus
                );
            default:
                // Allow other transitions
        }
    }

    /**
     * Validate resource ownership
     */
    public static void validateOwnership(long resourceOwnerId, long userId) {
        if (resourceOwnerId != userId) {
            throw new MarketplaceException(
                MarketplaceException.ErrorCode.UNAUTHORIZED,
                "User does not have permission to access this resource"
            );
        }
    }

    /**
     * Log validation error for debugging
     */
    public static void logValidationError(String fieldName, String reason) {
        System.out.println(LOG_TAG + " Validation failed - " + fieldName + ": " + reason);
    }
}
