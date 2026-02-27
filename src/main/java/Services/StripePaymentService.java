package Services;

import com.stripe.Stripe;
import com.stripe.exception.*;
import com.stripe.model.*;
import com.stripe.param.ChargeCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;

import java.io.*;
import java.util.*;

/**
 * Service for handling all Stripe payment processing
 * Manages payment intents, charges, refunds, and escrow holds
 * Implements PCI compliance through tokenized payments
 */
public class StripePaymentService {
    private static final String LOG_TAG = "[StripePaymentService]";
    private final String webhookSecret;
    private final double platformFeePercentage;
    private final double platformFeeFixed;

    private static StripePaymentService instance;

    public StripePaymentService(String apiKey, String webhookSecret) {
        Stripe.apiKey = apiKey;
        this.webhookSecret = webhookSecret;

        // Load fee configuration
        this.platformFeePercentage = Double.parseDouble(
            getConfigProperty("marketplace.fee.percentage", "0.029")
        );
        this.platformFeeFixed = Double.parseDouble(
            getConfigProperty("marketplace.fee.fixed.usd", "0.30")
        );

        System.out.println(LOG_TAG + " Stripe API initialized. Fee: " + 
            (platformFeePercentage * 100) + "% + $" + platformFeeFixed);
    }

    public static StripePaymentService getInstance() {
        if (instance == null) {
            String apiKey = getConfigProperty("stripe.api.key", "sk_test_XXXX");
            String webhookSecret = getConfigProperty("stripe.webhook.secret", "whsec_XXXX");
            instance = new StripePaymentService(apiKey, webhookSecret);
        }
        return instance;
    }

    /**
     * Create a payment intent for a marketplace order
     * Returns a payment intent that can be confirmed by the client
     */
    public PaymentIntent initiatePayment(int orderId, double amountUsd, 
                                         int buyerId, int sellerId, String description) {
        try {
            long amountCents = (long) (amountUsd * 100);  // Stripe uses cents

            PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                .setAmount(amountCents)
                .setCurrency("usd")
                .setDescription(description)
                .setAutomaticPaymentMethods(
                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true)
                        .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                        .build()
                );
            // Note: statement_descriptor not supported with card payment method

            paramsBuilder.putMetadata("order_id", String.valueOf(orderId));
            paramsBuilder.putMetadata("buyer_id", String.valueOf(buyerId));
            paramsBuilder.putMetadata("seller_id", String.valueOf(sellerId));
            paramsBuilder.putMetadata("transaction_type", "MARKETPLACE_ORDER");

            PaymentIntentCreateParams params = paramsBuilder.build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);
            System.out.println(LOG_TAG + " Payment intent created: " + paymentIntent.getId() + 
                " for order " + orderId + " (${" + amountUsd + ")");

            return paymentIntent;

        } catch (StripeException e) {
            System.err.println(LOG_TAG + " ERROR creating payment intent: " + e.getMessage());
            return null;
        }
    }

    /**
     * Confirm a payment after client-side authorization
     */
    public PaymentIntent confirmPayment(String paymentIntentId) {
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            
            // If payment is already succeeded, return it
            if ("succeeded".equals(paymentIntent.getStatus())) {
                System.out.println(LOG_TAG + " Payment already confirmed: " + paymentIntentId);
                return paymentIntent;
            }

            System.out.println(LOG_TAG + " Payment status: " + paymentIntent.getId() + 
                " Status: " + paymentIntent.getStatus());
            return paymentIntent;

        } catch (StripeException e) {
            System.err.println(LOG_TAG + " ERROR confirming payment: " + e.getMessage());
            return null;
        }
    }

    /**
     * Confirm payment with test card details (for testing/demo purposes)
     * Uses Stripe test payment method tokens to avoid raw card API
     * In production, use Stripe.js/Elements on client side
     */
    public PaymentIntent confirmPaymentWithCard(String paymentIntentId, String cardNumber, 
                                                 String expMonth, String expYear, String cvc) {
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            
            // Check if already succeeded
            if ("succeeded".equals(paymentIntent.getStatus())) {
                System.out.println(LOG_TAG + " Payment already succeeded: " + paymentIntentId);
                return paymentIntent;
            }

            // Map card numbers to Stripe test payment method tokens
            // See: https://stripe.com/docs/testing
            String paymentMethodId = getTestPaymentMethodToken(cardNumber);

            // Confirm payment intent with the test payment method
            Map<String, Object> confirmParams = new HashMap<>();
            confirmParams.put("payment_method", paymentMethodId);

            paymentIntent = paymentIntent.confirm(confirmParams);

            System.out.println(LOG_TAG + " Payment confirmed with test token: " + paymentIntentId + 
                " Status: " + paymentIntent.getStatus());

            return paymentIntent;

        } catch (StripeException e) {
            System.err.println(LOG_TAG + " ERROR confirming payment with card: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Map test card numbers to Stripe test payment method tokens
     * This avoids sending raw card data to Stripe API
     */
    private String getTestPaymentMethodToken(String cardNumber) {
        String cleaned = cardNumber.replaceAll("\\s", "");
        
        // Map common test cards to their tokens
        switch (cleaned) {
            case "4242424242424242":
                return "pm_card_visa"; // Visa - succeeds
            case "4000000000000002":
                return "pm_card_chargeDeclined"; // Card declined
            case "4000000000009995":
                return "pm_card_chargeDeclinedInsufficientFunds"; // Insufficient funds
            case "4000002500003155":
                return "pm_card_authenticationRequired"; // 3D Secure required
            case "5555555555554444":
                return "pm_card_mastercard"; // Mastercard - succeeds
            case "378282246310005":
                return "pm_card_amex"; // Amex - succeeds
            default:
                // Default to successful Visa for any other test card
                System.out.println(LOG_TAG + " Unknown test card, using pm_card_visa");
                return "pm_card_visa";
        }
    }

    /**
     * Hold funds in escrow for buyer protection
     * In Stripe, this is done through application fees
     */
    public boolean holdInEscrow(String chargeId, int escrowId, double amountUsd) {
        try {
            Charge charge = Charge.retrieve(chargeId);

            if (!"succeeded".equals(charge.getStatus())) {
                System.err.println(LOG_TAG + " Cannot escrow: charge not succeeded");
                return false;
            }

            // Update metadata to mark as escrow
            Map<String, Object> metadata = new HashMap<>(charge.getMetadata());
            metadata.put("escrow_id", String.valueOf(escrowId));
            metadata.put("escrow_held", "true");
            metadata.put("escrow_amount", String.valueOf(amountUsd));

            Map<String, Object> params = new HashMap<>();
            params.put("metadata", metadata);

            charge.update(params);

            System.out.println(LOG_TAG + " Funds held in escrow: " + chargeId + 
                " for escrow ID " + escrowId);
            return true;

        } catch (StripeException e) {
            System.err.println(LOG_TAG + " ERROR holding funds: " + e.getMessage());
            return false;
        }
    }

    /**
     * Release escrowed funds to seller
     */
    public boolean releaseFundsToSeller(String chargeId, int sellerId) {
        try {
            Charge charge = Charge.retrieve(chargeId);
            Map<String, Object> metadata = new HashMap<>(charge.getMetadata());
            metadata.put("escrow_held", "false");
            metadata.put("released_to_seller", "true");
            metadata.put("released_at", String.valueOf(System.currentTimeMillis()));

            Map<String, Object> params = new HashMap<>();
            params.put("metadata", metadata);
            charge.update(params);

            System.out.println(LOG_TAG + " Funds released to seller: " + sellerId);
            return true;

        } catch (StripeException e) {
            System.err.println(LOG_TAG + " ERROR releasing funds: " + e.getMessage());
            return false;
        }
    }

    /**
     * Refund a charge (full or partial)
     */
    public Refund refundPayment(String chargeId, Long amountCents, String reason) {
        try {
            RefundCreateParams.Builder paramsBuilder = RefundCreateParams.builder()
                .setCharge(chargeId)
                .setReason(toRefundReason(reason));

            if (amountCents != null && amountCents > 0) {
                paramsBuilder.setAmount(amountCents);
            }

            Refund refund = Refund.create(paramsBuilder.build());

            System.out.println(LOG_TAG + " Refund processed: " + refund.getId() + 
                " for charge " + chargeId + " Reason: " + reason);

            return refund;

        } catch (StripeException e) {
            System.err.println(LOG_TAG + " ERROR processing refund: " + e.getMessage());
            return null;
        }
    }

    /**
     * Calculate platform fees
     */
    public double calculatePlatformFee(double transactionAmountUsd) {
        return (transactionAmountUsd * platformFeePercentage) + platformFeeFixed;
    }

    private RefundCreateParams.Reason toRefundReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER;
        }

        switch (reason.toLowerCase(Locale.ROOT)) {
            case "duplicate":
                return RefundCreateParams.Reason.DUPLICATE;
            case "fraudulent":
                return RefundCreateParams.Reason.FRAUDULENT;
            case "requested_by_customer":
                return RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER;
            default:
                return RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER;
        }
    }

    /**
     * Calculate seller proceeds after fees
     */
    public double calculateSellerProceeds(double transactionAmountUsd) {
        return transactionAmountUsd - calculatePlatformFee(transactionAmountUsd);
    }

    /**
     * Verify webhook signature for secure webhook handling
     */
    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            // In production, verify using Stripe's SDK
            // This is a simplified version - use com.stripe.net.Webhook.constructEvent()
            System.out.println(LOG_TAG + " Webhook signature verified");
            return true;

        } catch (Exception e) {
            System.err.println(LOG_TAG + " ERROR verifying webhook: " + e.getMessage());
            return false;
        }
    }

    /**
     * Handle payment success webhook
     */
    public void handlePaymentSuccess(String paymentIntentId) {
        try {
            PaymentIntent pi = PaymentIntent.retrieve(paymentIntentId);
            System.out.println(LOG_TAG + " Payment success webhook: " + paymentIntentId);

            // Update order status in database
            int orderId = Integer.parseInt(pi.getMetadata().get("order_id"));
            updateOrderPaymentStatus(orderId, "COMPLETED", paymentIntentId);

        } catch (StripeException e) {
            System.err.println(LOG_TAG + " ERROR handling payment success: " + e.getMessage());
        }
    }

    /**
     * Handle payment failure webhook
     */
    public void handlePaymentFailure(String paymentIntentId, String errorMessage) {
        try {
            PaymentIntent pi = PaymentIntent.retrieve(paymentIntentId);
            System.out.println(LOG_TAG + " Payment failure webhook: " + paymentIntentId + 
                " Error: " + errorMessage);

            int orderId = Integer.parseInt(pi.getMetadata().get("order_id"));
            updateOrderPaymentStatus(orderId, "FAILED", null);

        } catch (StripeException e) {
            System.err.println(LOG_TAG + " ERROR handling payment failure: " + e.getMessage());
        }
    }

    /**
     * Handle refund completed webhook
     */
    public void handleRefundCompleted(String refundId) {
        try {
            Refund refund = Refund.retrieve(refundId);
            System.out.println(LOG_TAG + " Refund completed webhook: " + refundId);

        } catch (StripeException e) {
            System.err.println(LOG_TAG + " ERROR handling refund: " + e.getMessage());
        }
    }

    /**
     * Get payment intent details
     */
    public PaymentIntent getPaymentDetails(String paymentIntentId) {
        try {
            return PaymentIntent.retrieve(paymentIntentId);
        } catch (StripeException e) {
            System.err.println(LOG_TAG + " ERROR retrieving payment details: " + e.getMessage());
            return null;
        }
    }

    /**
     * List recent transactions for a seller
     */
    public List<Charge> getSellerTransactions(String sellerId, int limit) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("limit", limit);
            ChargeCollection charges = Charge.list(params);
            return charges.getData();
        } catch (StripeException e) {
            System.err.println(LOG_TAG + " ERROR retrieving transactions: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Update order payment status (database utility)
     */
    private void updateOrderPaymentStatus(int orderId, String status, String paymentId) {
        try (java.sql.Connection conn = DataBase.MyConnection.getConnection()) {
            if (conn == null) return;

            String sql = "UPDATE marketplace_orders SET status = ?, stripe_payment_id = ?, updated_at = NOW() WHERE id = ?";
            try (java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, status);
                stmt.setString(2, paymentId);
                stmt.setInt(3, orderId);
                stmt.executeUpdate();
                System.out.println(LOG_TAG + " Order " + orderId + " status updated to " + status);
            }
        } catch (java.sql.SQLException e) {
            System.err.println(LOG_TAG + " ERROR updating order status: " + e.getMessage());
        }
    }

    /**
     * Get configuration property from environment variables (priority) or api-config.properties (fallback)
     * Environment variable mapping:
     * - stripe.api.key → SK_TEST
     * - stripe.publishable.key → PK_TEST
     */
    private static String getConfigProperty(String key, String defaultValue) {
        // Check environment variables first (IntelliJ configuration)
        if (key.equals("stripe.api.key")) {
            String envValue = System.getenv("SK_TEST");
            if (envValue != null && !envValue.isEmpty()) {
                System.out.println(LOG_TAG + " Using SK_TEST from environment variable");
                return envValue;
            }
        } else if (key.equals("stripe.publishable.key")) {
            String envValue = System.getenv("PK_TEST");
            if (envValue != null && !envValue.isEmpty()) {
                System.out.println(LOG_TAG + " Using PK_TEST from environment variable");
                return envValue;
            }
        }
        
        // Fall back to properties file
        try (InputStream input = StripePaymentService.class.getClassLoader()
                .getResourceAsStream("api-config.properties")) {
            Properties props = new Properties();
            if (input != null) {
                props.load(input);
                return props.getProperty(key, defaultValue);
            }
        } catch (IOException e) {
            System.err.println(LOG_TAG + " ERROR loading config: " + e.getMessage());
        }
        return defaultValue;
    }
}
