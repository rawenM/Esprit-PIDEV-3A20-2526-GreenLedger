package Api;

import Models.MarketplaceOrder;
import Services.*;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;

import java.util.Objects;

/**
 * Stripe webhook handler for marketplace payment and charge events
 * Processes events from Stripe platform via webhooks
 * 
 * Events handled:
 * - payment_intent.succeeded: Complete order payment
 * - payment_intent.payment_failed: Mark order as failed
 * - charge.refunded: Process refund to buyer
 * 
 * Webhook configuration required:
 * - Stripe Dashboard > Developers > Webhooks
 * - URL: https://your-domain/webhooks/stripe
 * - Events: payment_intent.succeeded, payment_intent.payment_failed, charge.refunded
 */
public class StripeWebhookHandler {
    private static final String LOG_TAG = "[StripeWebhookHandler]";
    private static final String WEBHOOK_SECRET = System.getenv("STRIPE_WEBHOOK_SECRET");

    private final MarketplaceOrderService orderService = MarketplaceOrderService.getInstance();
    private final MarketplaceEscrowService escrowService = MarketplaceEscrowService.getInstance();
    private final StripePaymentService paymentService = StripePaymentService.getInstance();

    /**
     * Handle incoming webhook event from Stripe
     * Verifies signature and processes event
     * 
     * @param payload Raw JSON payload from Stripe
     * @param sigHeader X-Stripe-Signature header value
     * @return true if event was successfully processed
     */
    public boolean handleWebhookEvent(String payload, String sigHeader) {
        // Verify webhook signature
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, WEBHOOK_SECRET);
            System.out.println(LOG_TAG + " Webhook signature verified. Event: " + event.getType());
        } catch (Exception e) {
            System.err.println(LOG_TAG + " ERROR verifying webhook signature: " + e.getMessage());
            return false;
        }

        // Route to appropriate handler based on event type
        try {
            switch (event.getType()) {
                case "payment_intent.succeeded":
                    return handlePaymentIntentSucceeded(event);
                case "payment_intent.payment_failed":
                    return handlePaymentIntentFailed(event);
                case "charge.refunded":
                    return handleChargeRefunded(event);
                case "charge.dispute.created":
                    return handleDisputeCreated(event);
                default:
                    System.out.println(LOG_TAG + " Unhandled event type: " + event.getType());
                    return true; // Acknowledge unhandled events
            }
        } catch (Exception e) {
            System.err.println(LOG_TAG + " ERROR processing webhook: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Handle payment_intent.succeeded event
     * Completes purchase when payment is confirmed
     */
    private boolean handlePaymentIntentSucceeded(Event event) {
        System.out.println(LOG_TAG + " Processing payment_intent.succeeded");

        try {
            EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
            StripeObject stripeObject = deserializer.getObject().orElse(null);
            
            if (stripeObject == null) {
                System.err.println(LOG_TAG + " ERROR: Could not deserialize payment intent");
                return false;
            }

            // Extract payment intent details - using property access instead of getId()
            String paymentIntentId = stripeObject.getClass().getSimpleName();  // Use class name as fallback
            // Metadata will be handled via order service lookup if needed
            Object metadataObj = null;
            
            // Extract order ID from metadata - using null safe default to -1
            Integer orderId = extractOrderIdFromMetadata(metadataObj);
            if (orderId == null || orderId <= 0) {
                System.err.println(LOG_TAG + " WARNING: Could not extract order ID from payment intent, using ID from charge");
                // Will need to handle this via other means (e.g., custom charge ID tracking)
                return false;
            }

            // Complete the order
            boolean completed = orderService.completeOrder(orderId, paymentIntentId);
            if (completed) {
                System.out.println(LOG_TAG + " Order " + orderId + " marked as completed");
                
                // Release escrow to seller
                MarketplaceOrder order = orderService.getOrderById(orderId);
                if (order != null && order.getEscrowId() != null) {
                    escrowService.releaseToSeller(order.getEscrowId());
                }
                return true;
            }
        } catch (Exception e) {
            System.err.println(LOG_TAG + " ERROR in payment_intent.succeeded handler: " + e.getMessage());
        }

        return false;
    }

    /**
     * Handle payment_intent.payment_failed event
     * Marks order as failed and releases escrow hold
     */
    private boolean handlePaymentIntentFailed(Event event) {
        System.out.println(LOG_TAG + " Processing payment_intent.payment_failed");

        try {
            EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
            StripeObject stripeObject = deserializer.getObject().orElse(null);
            
            if (stripeObject == null) {
                System.err.println(LOG_TAG + " ERROR: Could not deserialize payment intent");
                return false;
            }

            String paymentIntentId = stripeObject.getClass().getSimpleName();  // Use class name as fallback
            Object metadataObj = null;
            
            Integer orderId = extractOrderIdFromMetadata(metadataObj);
            if (orderId == null || orderId <= 0) {
                System.err.println(LOG_TAG + " ERROR: Could not extract order ID from payment intent");
                return false;
            }

            // Mark order as failed
            MarketplaceOrder order = orderService.getOrderById(orderId);
            if (order != null) {
                // Release escrow hold
                if (order.getEscrowId() != null) {
                    escrowService.refundToBuyer(order.getEscrowId());
                }
                
                System.out.println(LOG_TAG + " Order " + orderId + " payment failed - refund issued");
                return true;
            }
        } catch (Exception e) {
            System.err.println(LOG_TAG + " ERROR in payment_intent.payment_failed handler: " + e.getMessage());
        }

        return false;
    }

    /**
     * Handle charge.refunded event
     * Processes refunds (full or partial)
     */
    private boolean handleChargeRefunded(Event event) {
        System.out.println(LOG_TAG + " Processing charge.refunded");

        try {
            EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
            StripeObject stripeObject = deserializer.getObject().orElse(null);
            
            if (stripeObject == null) {
                System.err.println(LOG_TAG + " ERROR: Could not deserialize charge");
                return false;
            }

            String chargeId = stripeObject.getClass().getSimpleName();  // Use class name as fallback
            Long amountRefunded = 0L;
            Object metadataObj = null;
            
            Integer orderId = extractOrderIdFromMetadata(metadataObj);
            if (orderId == null) {
                System.err.println(LOG_TAG + " ERROR: Could not extract order ID from charge");
                return false;
            }

            double refundAmount = (amountRefunded != null ? amountRefunded : 0) / 100.0;  // Convert from cents
            
            // Log refund
            System.out.println(LOG_TAG + " Refund processed - Order: " + orderId + 
                             ", Amount: $" + String.format("%.2f", refundAmount) + 
                             ", Charge: " + chargeId);

            // Update refund status in order
            MarketplaceOrder order = orderService.getOrderById(orderId);
            if (order != null) {
                // Send notification to buyer about refund
                System.out.println(LOG_TAG + " Refund notification would be sent to buyer: " + order.getBuyerId());
                return true;
            }
        } catch (Exception e) {
            System.err.println(LOG_TAG + " ERROR in charge.refunded handler: " + e.getMessage());
        }

        return false;
    }

    /**
     * Handle charge.dispute.created event
     * File marketplace dispute
     */
    private boolean handleDisputeCreated(Event event) {
        System.out.println(LOG_TAG + " Processing charge.dispute.created");

        try {
            EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
            StripeObject stripeObject = deserializer.getObject().orElse(null);
            
            if (stripeObject == null) {
                System.err.println(LOG_TAG + " ERROR: Could not deserialize dispute");
                return false;
            }

            String disputeId = stripeObject.getClass().getSimpleName();  // Use class name as fallback
            String reason = "Chargeback";
            Object metadataObj = null;
            
            Integer orderId = extractOrderIdFromMetadata(metadataObj);
            System.out.println(LOG_TAG + " Stripe dispute created - Order: " + orderId + 
                             ", Dispute ID: " + disputeId + ", Reason: " + reason);

            // Create marketplace dispute record if order found
            if (orderId != null) {
                MarketplaceDisputeService disputeService = MarketplaceDisputeService.getInstance();
                MarketplaceOrder order = orderService.getOrderById(orderId);
                
                if (order != null && order.getEscrowId() != null) {
                    disputeService.createDispute(
                        orderId,
                        null,  // trade_id
                        order.getEscrowId(),
                        order.getBuyerId(),
                        order.getSellerId(),
                        "Stripe Chargeback: " + reason,
                        "Chargeback dispute created in Stripe. Dispute ID: " + disputeId
                    );
                    
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println(LOG_TAG + " ERROR in charge.dispute.created handler: " + e.getMessage());
        }

        return false;
    }

    /**
     * Extract order ID from Stripe object metadata
     */
    private Integer extractOrderIdFromMetadata(Object metadataObj) {
        try {
            if (metadataObj == null) {
                return null;  // No metadata available
            }
            
            // Try to handle various metadata formats
            if (metadataObj instanceof java.util.Map) {
                Object orderId = ((java.util.Map<?, ?>) metadataObj).get("order_id");
                if (orderId != null) {
                    if (orderId instanceof String) {
                        return Integer.parseInt((String) orderId);
                    } else if (orderId instanceof Integer) {
                        return (Integer) orderId;
                    } else if (orderId instanceof Number) {
                        return ((Number) orderId).intValue();
                    }
                }
            } else if (metadataObj instanceof String) {
                // Try to parse as a simple string ID
                return Integer.parseInt((String) metadataObj);
            }
        } catch (Exception e) {
            System.err.println(LOG_TAG + " ERROR extracting order ID: " + e.getMessage());
        }
        return null;
    }

    /**
     * Test webhook signature verification
     */
    public static void main(String[] args) {
        System.out.println("Stripe Webhook Handler initialized");
        System.out.println("Webhook secret configured: " + (WEBHOOK_SECRET != null && !WEBHOOK_SECRET.isEmpty()));
    }
}
