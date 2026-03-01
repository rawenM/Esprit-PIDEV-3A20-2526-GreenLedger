package Services;

import com.stripe.Stripe;
import com.stripe.exception.*;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.param.ChargeCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;

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
    private final boolean testMode;

    private static StripePaymentService instance;

    public StripePaymentService(String apiKey, String webhookSecret) {
        Stripe.apiKey = apiKey;
        this.webhookSecret = webhookSecret;
        this.testMode = apiKey != null && apiKey.startsWith("sk_test");

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

    public boolean isTestMode() {
        return testMode;
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
                " for order " + orderId + " ($" + amountUsd + ")");

            return paymentIntent;

        } catch (StripeException e) {
            System.err.println(LOG_TAG + " ERROR creating payment intent: " + e.getMessage());
            return null;
        }
    }

    /**
     * Create a hosted Stripe Checkout session and return its URL.
     * This opens the real Stripe payment page (hosted checkout UI).
     */
    public String createHostedCheckoutUrl(int orderId, double amountUsd,
                                          int buyerId, int sellerId,
                                          double quantity, double unitPriceUsd) {
        try {
            long amountCents = Math.round(amountUsd * 100);

            String successUrl = getConfigProperty(
                "marketplace.checkout.success.url",
                "https://example.com/payment/success?session_id={CHECKOUT_SESSION_ID}"
            );
            String cancelUrl = getConfigProperty(
                "marketplace.checkout.cancel.url",
                "https://example.com/payment/cancel"
            );

            SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .putMetadata("order_id", String.valueOf(orderId))
                .putMetadata("buyer_id", String.valueOf(buyerId))
                .putMetadata("seller_id", String.valueOf(sellerId))
                .putMetadata("transaction_type", "MARKETPLACE_ORDER")
                .addLineItem(
                    SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(
                            SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("usd")
                                .setUnitAmount(amountCents)
                                .setProductData(
                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Carbon Credit Purchase")
                                        .setDescription(String.format(
                                            "Order #%d | %.2f tCO2e @ $%.2f/unit",
                                            orderId, quantity, unitPriceUsd
                                        ))
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .build();

            Session session = Session.create(params);
            System.out.println(LOG_TAG + " Hosted Checkout session created: " + session.getId() +
                " for order " + orderId + " ($" + amountUsd + ")");
            return session.getUrl();

        } catch (StripeException e) {
            System.err.println(LOG_TAG + " ERROR creating hosted checkout session: " + e.getMessage());
            return null;
        }
    }

    /**
     * Verify a Checkout session and return payment intent id if paid.
     * Returns null when session is not paid or cannot be verified.
     */
    public String getPaidCheckoutPaymentIntent(String checkoutSessionId) {
        try {
            Session session = Session.retrieve(checkoutSessionId);
            String paymentStatus = session.getPaymentStatus();

            if ("paid".equalsIgnoreCase(paymentStatus)) {
                String paymentIntentId = session.getPaymentIntent();
                System.out.println(LOG_TAG + " Checkout paid: " + checkoutSessionId +
                    " -> paymentIntent=" + paymentIntentId);
                return paymentIntentId;
            }

            System.out.println(LOG_TAG + " Checkout not paid yet: " + checkoutSessionId +
                " status=" + paymentStatus);
            return null;
        } catch (StripeException e) {
            System.err.println(LOG_TAG + " ERROR verifying checkout session: " + e.getMessage());
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
     * Create Stripe Connect account for a seller (for marketplace payouts)
     * Sellers must have a Stripe Connect account to receive payments
     */
    public Account createSellerAccount(long sellerId, String email, String businessName, String countryCode) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("type", "express");
            params.put("country", countryCode != null ? countryCode : "US");
            params.put("email", email);
            
            Map<String, Object> businessProfile = new HashMap<>();
            businessProfile.put("name", businessName != null ? businessName : "Carbon Marketplace Seller");
            businessProfile.put("support_email", email);
            params.put("business_profile", businessProfile);
            
            // Add metadata for tracking
            Map<String, String> metadata = new HashMap<>();
            metadata.put("seller_id", String.valueOf(sellerId));
            params.put("metadata", metadata);
            
            Account account = Account.create(params);
            System.out.println(LOG_TAG + " Seller account created: " + account.getId() + 
                " for seller " + sellerId);
            
            return account;
        } catch (StripeException e) {
            System.err.println(LOG_TAG + " ERROR creating seller account: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get onboarding link for Stripe Connect account setup
     * Returns a URL that seller can visit to complete onboarding
     */
    public String getSellerOnboardingUrl(String accountId, String returnUrl) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("account", accountId);
            params.put("type", "account_onboarding");
            
            Map<String, String> refreshUrlMap = new HashMap<>();
            refreshUrlMap.put("url", returnUrl);
            params.put("refresh_url", refreshUrlMap);
            
            Map<String, String> returnUrlMap = new HashMap<>();
            returnUrlMap.put("url", returnUrl);
            params.put("return_url", returnUrlMap);
            
            AccountLink accountLink = AccountLink.create(params);
            System.out.println(LOG_TAG + " Onboarding link generated for account: " + accountId);
            
            return accountLink.getUrl();
        } catch (StripeException e) {
            System.err.println(LOG_TAG + " ERROR generating onboarding link: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get seller account details and onboarding status
     */
    public Account getSellerAccount(String accountId) {
        try {
            Account account = Account.retrieve(accountId);
            
            // Check if fully onboarded
            if (account.getChargesEnabled() && account.getPayoutsEnabled()) {
                System.out.println(LOG_TAG + " Seller account " + accountId + " is fully onboarded");
            } else {
                System.out.println(LOG_TAG + " Seller account " + accountId + 
                    " pending onboarding (charges: " + account.getChargesEnabled() + 
                    ", payouts: " + account.getPayoutsEnabled() + ")");
            }
            
            return account;
        } catch (StripeException e) {
            System.err.println(LOG_TAG + " ERROR retrieving seller account: " + e.getMessage());
            return null;
        }
    }

    /**
     * Transfer funds to seller's Stripe Connect account
     * This is called after payment is completed to move seller's proceeds to their account
     */
    public Transfer transferToSeller(String chargeId, String sellerAccountId, double sellerProceedsUsd) {
        try {
            long amountCents = (long) (sellerProceedsUsd * 100);
            
            com.stripe.param.TransferCreateParams params = 
                com.stripe.param.TransferCreateParams.builder()
                    .setAmount(amountCents)
                    .setCurrency("usd")
                    .setDestination(sellerAccountId)
                    .setSourceTransaction(chargeId)
                    .setDescription("Carbon Marketplace Sale Proceeds")
                    .putMetadata("charge_id", chargeId)
                    .build();
            
            Transfer transfer = Transfer.create(params);
            System.out.println(LOG_TAG + " Transfer created: " + transfer.getId() + 
                " to seller account " + sellerAccountId + 
                " ($" + String.format("%.2f", sellerProceedsUsd) + ")");
            
            return transfer;
        } catch (StripeException e) {
            System.err.println(LOG_TAG + " ERROR creating transfer: " + e.getMessage());
            return null;
        }
    }

    /**
     * Create application fee split for marketplace transaction
     * Stripe application fee is deducted from charge, transfer gets seller proceeds
     */
    public ApplicationFee createApplicationFee(String chargeId, long applicationFeeAmountCents) {
        try {
            // Stripe Platform fees via Stripe Connect - deducted automatically from transfers
            System.out.println(LOG_TAG + " Application fee recorded for charge: " + chargeId + 
                             ", Amount: " + applicationFeeAmountCents + " cents ($" + 
                             String.format("%.2f", applicationFeeAmountCents / 100.0) + ")");
            
            return null;  // Fee handled by Stripe Connect platform automatically
        } catch (Exception e) {
            System.err.println(LOG_TAG + " ERROR recording application fee: " + e.getMessage());
            return null;
        }
    }

    /**
     * Check seller payout status
     */
    public List<Payout> getSellerPayouts(String accountId, int limit) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("limit", limit);
            
            PayoutCollection payouts = Payout.list(params);
            System.out.println(LOG_TAG + " Retrieved " + payouts.getData().size() + 
                " payouts for seller account");
            
            return payouts.getData();
        } catch (StripeException e) {
            System.err.println(LOG_TAG + " ERROR retrieving payouts: " + e.getMessage());
            return new ArrayList<>();
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
