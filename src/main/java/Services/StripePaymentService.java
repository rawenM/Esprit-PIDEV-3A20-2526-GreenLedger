package Services;

import DataBase.MyConnection;
import Utils.EnvLoader;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.checkout.SessionCreateParams;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Stripe payment service — handles both marketplace checkout (legacy)
 * and investor project financing (new swipe-card flow).
 */
public class StripePaymentService {

    // ── Singleton (for marketplace compatibility) ─────────────────────────
    private static StripePaymentService instance;
    public static StripePaymentService getInstance() {
        if (instance == null) instance = new StripePaymentService();
        return instance;
    }

    public static class PaymentResult {
        public boolean success;
        public String  clientSecret;
        public String  paymentIntentId;
        public String  errorMessage;
        public double  amount;
        public String  projectName;
        public int     financementId;
    }

    static {
        EnvLoader.load();
        String secretKey = EnvLoader.get("STRIPE_SECRET_KEY");
        if (secretKey != null && !secretKey.isBlank()) {
            Stripe.apiKey = secretKey;
        } else {
            System.err.println("[Stripe] WARNING: STRIPE_SECRET_KEY not configured");
        }
    }

    /** Returns true if using Stripe test keys. */
    public boolean isTestMode() {
        String key = EnvLoader.get("STRIPE_SECRET_KEY", "");
        return key.startsWith("sk_test_");
    }

    /**
     * STEP 1 — Create a PaymentIntent and save a PENDING financement row.
     */
    public PaymentResult initiatePayment(int projectId, long investisseurId,
                                          double amount, String projectName) {
        PaymentResult result = new PaymentResult();
        result.amount      = amount;
        result.projectName = projectName;

        try {
            // Build metadata
            Map<String, String> metadata = new HashMap<>();
            metadata.put("project_id",    String.valueOf(projectId));
            metadata.put("investor_id",   String.valueOf(investisseurId));
            metadata.put("project_name",  projectName);

            // Create Stripe PaymentIntent (amount in cents)
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount((long) (amount * 100))
                .setCurrency("usd")
                .putAllMetadata(metadata)
                .setAutomaticPaymentMethods(
                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true)
                        .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                        .build())
                .build();

            PaymentIntent intent = PaymentIntent.create(params);

            // Save PENDING financement to DB
            int finId = savePendingFinancement(projectId, investisseurId, amount, intent.getId());

            result.success         = true;
            result.clientSecret    = intent.getClientSecret();
            result.paymentIntentId = intent.getId();
            result.financementId   = finId;

        } catch (StripeException e) {
            result.success      = false;
            result.errorMessage = "Stripe error: " + e.getMessage();
            System.err.println("[Stripe] initiatePayment error: " + e.getMessage());
        } catch (Exception e) {
            result.success      = false;
            result.errorMessage = "Error: " + e.getMessage();
            System.err.println("[Stripe] initiatePayment unexpected: " + e.getMessage());
        }

        return result;
    }

    /**
     * STEP 5 — Confirm payment: mark COMPLETED, fund project, create thread + notifications.
     * Idempotent — safe to call multiple times.
     */
    public boolean confirmPayment(String paymentIntentId) {
        // Find financement by intent ID
        String findSql = "SELECT id, project_id, investisseur_id, montant, statut " +
                         "FROM financements WHERE stripe_payment_intent_id=?";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(findSql)) {
            ps.setString(1, paymentIntentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    System.err.println("[Stripe] confirmPayment: no financement for intent " + paymentIntentId);
                    return false;
                }
                int    finId        = rs.getInt("id");
                int    projectId    = rs.getInt("project_id");
                long   investId     = rs.getLong("investisseur_id");
                double amount       = rs.getDouble("montant");
                String statut       = rs.getString("statut");

                // Idempotent check
                if ("COMPLETED".equals(statut)) {
                    System.out.println("[Stripe] confirmPayment: already COMPLETED for " + paymentIntentId);
                    return true;
                }

                // 1. Mark financement COMPLETED
                updateFinancementCompleted(conn, finId);

                // 2. Mark project FUNDED
                int porteurId = fundProject(conn, projectId);

                // 3. Create conversation thread
                createConversationThread(conn, projectId, investId, porteurId);

                // 4. Create notifications
                String projectName = getProjectName(conn, projectId);
                createNotification(conn, investId, "PAYMENT_CONFIRMED",
                    "Votre investissement de " + String.format("%.2f", amount) +
                    " USD pour le projet \"" + projectName + "\" a été confirmé.", projectId);
                if (porteurId > 0) {
                    createNotification(conn, porteurId, "PROJECT_FUNDED",
                        "Votre projet \"" + projectName + "\" a reçu un investissement de " +
                        String.format("%.2f", amount) + " USD.", projectId);
                }

                System.out.println("[Stripe] Payment confirmed for financement #" + finId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[Stripe] confirmPayment SQL error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verify a PaymentIntent status directly with Stripe.
     */
    public String getPaymentStatus(String paymentIntentId) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            return intent.getStatus(); // "succeeded", "requires_payment_method", etc.
        } catch (StripeException e) {
            System.err.println("[Stripe] getPaymentStatus error: " + e.getMessage());
            return "error";
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private int savePendingFinancement(int projectId, long investisseurId,
                                        double amount, String intentId) throws SQLException {
        String sql = "INSERT INTO financements " +
                     "(project_id, investisseur_id, montant, stripe_payment_intent_id, statut, created_at) " +
                     "VALUES (?,?,?,?,'PENDING',NOW())";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, projectId);
            ps.setLong(2, investisseurId);
            ps.setDouble(3, amount);
            ps.setString(4, intentId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    private void updateFinancementCompleted(Connection conn, int finId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE financements SET statut='COMPLETED', completed_at=NOW() WHERE id=?")) {
            ps.setInt(1, finId);
            ps.executeUpdate();
        }
    }

    private int fundProject(Connection conn, int projectId) throws SQLException {
        // Get porteur ID first
        int porteurId = 0;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT entreprise_id FROM projet WHERE id=?")) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) porteurId = rs.getInt("entreprise_id");
            }
        }
        // Mark project funded
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE projet SET statut_financement='FUNDED', funded_at=NOW() WHERE id=?")) {
            ps.setInt(1, projectId);
            ps.executeUpdate();
        }
        return porteurId;
    }

    private void createConversationThread(Connection conn, int projectId,
                                           long investId, long porteurId) throws SQLException {
        // Check if already exists
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM conversation_threads WHERE project_id=? AND investisseur_id=?")) {
            ps.setInt(1, projectId); ps.setLong(2, investId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return; // already exists
            }
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO conversation_threads (project_id, investisseur_id, porteur_id, created_at) " +
                "VALUES (?,?,?,NOW())")) {
            ps.setInt(1, projectId); ps.setLong(2, investId); ps.setLong(3, porteurId);
            ps.executeUpdate();
        }
    }

    private void createNotification(Connection conn, long userId, String type,
                                     String message, int projectId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO notifications (user_id, type, message, is_read, related_project_id, created_at) " +
                "VALUES (?,?,?,0,?,NOW())")) {
            ps.setLong(1, userId);
            ps.setString(2, type);
            ps.setString(3, message);
            ps.setInt(4, projectId);
            ps.executeUpdate();
        }
    }

    private String getProjectName(Connection conn, int projectId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT titre FROM projet WHERE id=?")) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("titre");
            }
        }
        return "Projet #" + projectId;
    }

    /** Record a swipe decision (RIGHT/LEFT/SKIP) to avoid showing same card again. */
    public void recordSwipeDecision(long investisseurId, int projectId, String decision) {
        String sql = "INSERT INTO swipe_decisions (investisseur_id, project_id, decision, decided_at) " +
                     "VALUES (?,?,?,NOW()) ON DUPLICATE KEY UPDATE decision=VALUES(decision), decided_at=NOW()";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, investisseurId);
            ps.setInt(2, projectId);
            ps.setString(3, decision);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[Stripe] recordSwipeDecision: " + e.getMessage());
        }
    }

    /** Run the DB migration to ensure required columns exist. */
    public static void runMigration() {        String[] statements = {
            "ALTER TABLE financements ADD COLUMN IF NOT EXISTS stripe_payment_intent_id VARCHAR(255) DEFAULT NULL",
            "ALTER TABLE financements ADD COLUMN IF NOT EXISTS statut VARCHAR(20) NOT NULL DEFAULT 'PENDING'",
            "ALTER TABLE financements ADD COLUMN IF NOT EXISTS completed_at DATETIME DEFAULT NULL",
            "ALTER TABLE financements ADD COLUMN IF NOT EXISTS created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP",
            "ALTER TABLE financements ADD COLUMN IF NOT EXISTS investisseur_id BIGINT DEFAULT NULL",
            "ALTER TABLE financements ADD COLUMN IF NOT EXISTS project_id INT DEFAULT NULL",
            "ALTER TABLE projet ADD COLUMN IF NOT EXISTS statut_financement VARCHAR(30) DEFAULT 'SEEKING_FUNDING'",
            "ALTER TABLE projet ADD COLUMN IF NOT EXISTS funded_at DATETIME DEFAULT NULL",
            "ALTER TABLE projet ADD COLUMN IF NOT EXISTS roi DOUBLE DEFAULT NULL",
            "CREATE TABLE IF NOT EXISTS swipe_decisions (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "investisseur_id BIGINT NOT NULL," +
                "project_id INT NOT NULL," +
                "decision ENUM('RIGHT','LEFT','SKIP') NOT NULL," +
                "decided_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "UNIQUE KEY uq_swipe (investisseur_id, project_id))"
        };
        try (Connection conn = MyConnection.getConnection()) {
            for (String sql : statements) {
                try (Statement st = conn.createStatement()) {
                    st.execute(sql);
                } catch (SQLException e) {
                    // Ignore "already exists" errors
                    if (!e.getMessage().contains("Duplicate") && !e.getMessage().contains("already exists")) {
                        System.err.println("[Stripe] Migration warning: " + e.getMessage());
                    }
                }
            }
            System.out.println("[Stripe] DB migration completed");
        } catch (SQLException e) {
            System.err.println("[Stripe] Migration error: " + e.getMessage());
        }
    }

    // ── Legacy marketplace methods (kept for backward compatibility) ──────

    /**
     * Legacy: initiatePayment with old signature (orderId, amount, buyerId, sellerId, qty, price).
     * Called by MarketplaceController and ComprehensiveTestController.
     */
    public String createHostedCheckoutUrl(int orderId, double totalAmount,
                                           int buyerId, int sellerId,
                                           double quantity, double pricePerUnit) {
        try {
            String successUrl = "http://localhost:8080/marketplace/success?order=" + orderId;
            String cancelUrl  = "http://localhost:8080/marketplace/cancel?order=" + orderId;

            SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency("usd")
                        .setUnitAmount((long) (totalAmount * 100))
                        .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                            .setName("Carbon Credits — Order #" + orderId)
                            .setDescription(String.format("%.2f units @ $%.2f/unit", quantity, pricePerUnit))
                            .build())
                        .build())
                    .build())
                .putMetadata("order_id",  String.valueOf(orderId))
                .putMetadata("buyer_id",  String.valueOf(buyerId))
                .putMetadata("seller_id", String.valueOf(sellerId))
                .build();

            Session session = Session.create(params);
            return session.getUrl();
        } catch (StripeException e) {
            System.err.println("[Stripe] createHostedCheckoutUrl error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Legacy: initiatePayment with old signature used by ComprehensiveTestController.
     * (int projectId, double amount, int buyerId, int sellerId, String projectName)
     */
    public PaymentResult initiatePayment(int projectId, double amount,
                                          int buyerId, int sellerId, String projectName) {
        return initiatePayment(projectId, (long) buyerId, amount, projectName);
    }

    /**
     * Calculate platform fee (2.9% + $0.30 Stripe fee).
     */
    public double calculatePlatformFee(double amount) {
        return amount * 0.029 + 0.30;
    }

    /**
     * Refund a payment via Stripe.
     */
    public com.stripe.model.Refund refundPayment(String paymentIntentId, long amountCents, String reason) {
        try {
            com.stripe.param.RefundCreateParams params = com.stripe.param.RefundCreateParams.builder()
                .setPaymentIntent(paymentIntentId)
                .setAmount(amountCents)
                .setReason(com.stripe.param.RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
                .build();
            return com.stripe.model.Refund.create(params);
        } catch (StripeException e) {
            System.err.println("[Stripe] refundPayment error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get the PaymentIntent ID from a completed Checkout Session.
     */
    public String getPaidCheckoutPaymentIntent(String sessionId) {
        try {
            Session session = Session.retrieve(sessionId);
            if ("paid".equals(session.getPaymentStatus())) {
                return session.getPaymentIntent();
            }
            return null;
        } catch (StripeException e) {
            System.err.println("[Stripe] getPaidCheckoutPaymentIntent error: " + e.getMessage());
            return null;
        }
    }
}
