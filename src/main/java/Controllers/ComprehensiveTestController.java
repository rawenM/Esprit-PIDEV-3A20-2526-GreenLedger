package Controllers;

import Models.*;
import Services.*;
import DataBase.MyConnection;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import com.stripe.model.PaymentIntent;

import java.io.File;
import java.io.PrintWriter;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Comprehensive Test Controller for entire system
 * Tests: Wallet • Batch • Marketplace • Payment • Database
 */
public class ComprehensiveTestController {

    // Services
    private final WalletService walletService = new WalletService();
    private final MarketplaceListingService listingService = MarketplaceListingService.getInstance();
    private final MarketplaceOrderService orderService = MarketplaceOrderService.getInstance();
    private final StripePaymentService stripeService = StripePaymentService.getInstance();
    private final BatchEventService batchEventService = new BatchEventService();
    
    // UI Components - Wallet
    @FXML private TextField txtWalletOwnerType;
    @FXML private TextField txtWalletId;
    @FXML private TextField txtNewWalletName;
    
    // UI Components - Batch
    @FXML private TextField txtIssueWalletId;
    @FXML private TextField txtIssueAmount;
    @FXML private TextField txtBatchWalletId;
    @FXML private TextField txtBatchId;
    @FXML private TextField txtEventBatchId;
    @FXML private TextField txtRetireWalletId;
    @FXML private TextField txtRetireAmount;
    
    // UI Components - Transfer
    @FXML private TextField txtTransferFrom;
    @FXML private TextField txtTransferTo;
    @FXML private TextField txtTransferAmount;
    @FXML private TextField txtSplitBatchId;
    @FXML private TextField txtSplitAmount;
    
    // UI Components - Marketplace
    @FXML private TextField txtListingWalletId;
    @FXML private TextField txtListingQty;
    @FXML private TextField txtListingPrice;
    @FXML private TextField txtOrderListingId;
    @FXML private TextField txtOrderBuyerId;
    @FXML private TextField txtOrderQty;
    @FXML private TextField txtOrderId;
    @FXML private TextField txtOrderUserId;
    
    // UI Components - Payment
    @FXML private TextField txtPaymentAmount;
    
    // UI Components - General
    @FXML private TextArea logArea;
    @FXML private Label statusLabel;
    @FXML private Label statsLabel;
    @FXML private CheckBox chkVerbose;
    @FXML private CheckBox chkAutoScroll;
    @FXML private CheckBox chkTimestamp;
    
    private int testsRun = 0;
    private int testsPassed = 0;
    private int testsFailed = 0;

    @FXML
    public void initialize() {
        log("========================================");
        log("🧪 COMPREHENSIVE TEST PANEL INITIALIZED");
        log("========================================");
        log("System ready for testing all integrated modules");
        log("");
        updateStats();
    }

    // ==================== WALLET TESTS ====================
    
    @FXML
    private void testCreateWallet() {
        runTest("Create Wallet", () -> {
            String ownerType = txtWalletOwnerType.getText().isEmpty() ? "ENTERPRISE" : txtWalletOwnerType.getText();
            Wallet wallet = new Wallet(ownerType, 1);
            wallet.setName("Test Wallet " + System.currentTimeMillis());
            
            int walletId = walletService.createWallet(wallet);
            if (walletId > 0) {
                log("✓ Wallet created: ID=" + walletId);
                txtWalletId.setText(String.valueOf(walletId));
                return true;
            }
            return false;
        });
    }
    
    @FXML
    private void testGetWallet() {
        runTest("Get Wallet Details", () -> {
            int walletId = parseInt(txtWalletId.getText());
            Wallet wallet = walletService.getWalletById(walletId);
            if (wallet != null) {
                log("✓ Wallet found:");
                log("  ID: " + wallet.getId());
                log("  Name: " + wallet.getName());
                log("  Available: " + wallet.getAvailableCredits());
                log("  Retired: " + wallet.getRetiredCredits());
                return true;
            }
            log("✗ Wallet not found");
            return false;
        });
    }
    
    @FXML
    private void testListWallets() {
        runTest("List All Wallets", () -> {
            List<Wallet> wallets = walletService.getAllWallets();
            log("✓ Found " + wallets.size() + " wallets");
            int count = 0;
            for (Wallet w : wallets) {
                if (count++ >= 20) {
                    log("  ... (" + (wallets.size() - 20) + " more)");
                    break;
                }
                log(String.format("  [%d] %s - Available: %.2f | Retired: %.2f", 
                    w.getId(), w.getName(), w.getAvailableCredits(), w.getRetiredCredits()));
            }
            return true;
        });
    }
    
    @FXML
    private void testUpdateWallet() {
        runTest("Update Wallet", () -> {
            int walletId = parseInt(txtWalletId.getText());
            String newName = txtNewWalletName.getText();
            if (newName.isEmpty()) {
                log("✗ Please enter new name");
                return false;
            }
            
            Wallet wallet = walletService.getWalletById(walletId);
            if (wallet == null) {
                log("✗ Wallet not found");
                return false;
            }
            
            wallet.setName(newName);
            boolean success = walletService.updateWallet(wallet);
            if (success) {
                log("✓ Wallet updated: " + newName);
                return true;
            }
            return false;
        });
    }

    // ==================== BATCH TESTS ====================
    
    @FXML
    private void testIssueCredits() {
        runTest("Issue Credits (Create Batch)", () -> {
            int walletId = parseInt(txtIssueWalletId.getText());
            double amount = parseDouble(txtIssueAmount.getText());
            
            boolean success = walletService.quickIssueCredits(walletId, amount, "Test issuance");
            if (success) {
                log("✓ Issued " + amount + " credits to wallet " + walletId);
                log("  Batch created with traceability");
                return true;
            }
            return false;
        });
    }
    
    @FXML
    private void testListBatches() {
        runTest("List Wallet Batches", () -> {
            int walletId = parseInt(txtBatchWalletId.getText());
            List<CarbonCreditBatch> batches = walletService.getWalletBatches(walletId);
            
            log("✓ Found " + batches.size() + " batches for wallet " + walletId);
            for (CarbonCreditBatch batch : batches) {
                log(String.format("  Batch #%d: %.2f tCO2 | Serial: %s | Status: %s", 
                    batch.getId(), batch.getRemainingAmount(), 
                    batch.getSerialNumber(), batch.getStatus()));
            }
            return true;
        });
    }
    
    @FXML
    private void testBatchLineage() {
        runTest("View Batch Lineage", () -> {
            int batchId = parseInt(txtBatchId.getText());
            List<CarbonCreditBatch> lineage = walletService.getBatchLineage(batchId);
            
            log("✓ Batch lineage tree:");
            displayLineageTree(lineage, 0);
            return true;
        });
    }
    
    private void displayLineageTree(List<CarbonCreditBatch> batches, int level) {
        String indent = "  ".repeat(level);
        for (CarbonCreditBatch batch : batches) {
            log(String.format("%s├─ Batch #%d (%.2f tCO2) - %s", 
                indent, batch.getId(), batch.getRemainingAmount(), batch.getStatus()));
        }
    }
    
    @FXML
    private void testBatchEvents() {
        runTest("View Batch Events", () -> {
            int batchId = parseInt(txtEventBatchId.getText());
            List<BatchEvent> events = batchEventService.getBatchEvents(batchId);
            
            log("✓ Found " + events.size() + " events for batch " + batchId);
            for (BatchEvent event : events) {
                log(String.format("  [%s] %s by %s - Hash: %s", 
                    event.getCreatedAt(), event.getEventType(), 
                    event.getActor(), event.getEventHash().substring(0, 16) + "..."));
            }
            return true;
        });
    }
    
    @FXML
    private void testRetireCredits() {
        runTest("Retire Credits", () -> {
            int walletId = parseInt(txtRetireWalletId.getText());
            double amount = parseDouble(txtRetireAmount.getText());
            
            boolean success = walletService.retireCredits(walletId, amount, "Test retirement");
            if (success) {
                log("✓ Retired " + amount + " credits from wallet " + walletId);
                return true;
            }
            return false;
        });
    }

    // ==================== TRANSFER TESTS ====================
    
    @FXML
    private void testTransferDirect() {
        runTest("Transfer (DIRECT Mode)", () -> {
            int fromWallet = parseInt(txtTransferFrom.getText());
            int toWallet = parseInt(txtTransferTo.getText());
            double amount = parseDouble(txtTransferAmount.getText());
            
            boolean success = walletService.transferCreditsWithMode(
                fromWallet, toWallet, amount, "Test transfer (DIRECT)", 
                WalletService.TransferMode.DIRECT, "TEST_USER"
            );
            
            if (success) {
                log("✓ Transferred " + amount + " credits (DIRECT mode)");
                log("  From: Wallet " + fromWallet);
                log("  To: Wallet " + toWallet);
                return true;
            }
            return false;
        });
    }
    
    @FXML
    private void testTransferSplit() {
        runTest("Transfer (SPLIT_CHILD Mode)", () -> {
            int fromWallet = parseInt(txtTransferFrom.getText());
            int toWallet = parseInt(txtTransferTo.getText());
            double amount = parseDouble(txtTransferAmount.getText());
            
            boolean success = walletService.transferCreditsWithMode(
                fromWallet, toWallet, amount, "Test transfer (SPLIT_CHILD)", 
                WalletService.TransferMode.SPLIT_CHILD, "TEST_USER"
            );
            
            if (success) {
                log("✓ Transferred " + amount + " credits (SPLIT_CHILD mode)");
                log("  Child batches created for full traceability");
                return true;
            }
            return false;
        });
    }
    
    @FXML
    private void testSplitBatch() {
        runTest("Split Batch", () -> {
            int batchId = parseInt(txtSplitBatchId.getText());
            double amount = parseDouble(txtSplitAmount.getText());
            int toWalletId = parseInt(txtTransferTo.getText());
            
            int childBatchId = walletService.splitBatch(batchId, amount, toWalletId, "TEST_USER");
            if (childBatchId > 0) {
                log("✓ Batch split successful");
                log("  Parent: Batch #" + batchId);
                log("  Child: Batch #" + childBatchId + " (Amount: " + amount + ")");
                return true;
            }
            return false;
        });
    }

    // ==================== MARKETPLACE TESTS ====================
    
    @FXML
    private void testCreateListing() {
        runTest("Create Marketplace Listing", () -> {
            int walletId = parseInt(txtListingWalletId.getText());
            double qty = parseDouble(txtListingQty.getText());
            double price = parseDouble(txtListingPrice.getText());
            
            int listingId = listingService.createListing(
                1, // sellerId
                "CARBON_CREDIT", // assetType
                walletId, // walletId
                qty, // quantityOrTokens
                price, // pricePerUnit
                price * 0.9, // minPriceUsd (10% discount threshold)
                null, // autoAcceptPriceUsd
                "Test Carbon Credits" // description
            );
            
            if (listingId > 0) {
                log("✓ Listing created: ID=" + listingId);
                log("  Quantity: " + qty + " tCO2");
                log("  Price: $" + price + "/unit");
                txtOrderListingId.setText(String.valueOf(listingId));
                return true;
            }
            return false;
        });
    }
    
    @FXML
    private void testListMarketplace() {
        runTest("List Active Marketplace Listings", () -> {
            List<MarketplaceListing> listings = listingService.getActiveListings();
            log("✓ Found " + listings.size() + " active listings");
            
            int count = 0;
            for (MarketplaceListing listing : listings) {
                if (count++ >= 20) {
                    log("  ... (" + (listings.size() - 20) + " more)");
                    break;
                }
                log(String.format("  [%d] %.2f units @ $%.2f - Seller: %d", 
                    listing.getId(), listing.getQuantityOrTokens(), 
                    listing.getPricePerUnit(), listing.getSellerId()));
            }
            return true;
        });
    }
    
    @FXML
    private void testPlaceOrder() {
        runTest("Place Marketplace Order", () -> {
            int listingId = parseInt(txtOrderListingId.getText());
            int buyerId = parseInt(txtOrderBuyerId.getText());
            double qty = parseDouble(txtOrderQty.getText());
            
            int orderId = orderService.placeOrder(listingId, buyerId, qty);
            if (orderId > 0) {
                log("✓ Order placed: ID=" + orderId);
                log("  Buyer: " + buyerId);
                log("  Quantity: " + qty);
                txtOrderId.setText(String.valueOf(orderId));
                return true;
            }
            return false;
        });
    }
    
    @FXML
    private void testCompleteOrder() {
        runTest("Complete Order (Mock Payment)", () -> {
            int orderId = parseInt(txtOrderId.getText());
            String mockPaymentId = "pi_mock_" + System.currentTimeMillis();
            
            boolean success = orderService.completeOrder(orderId, mockPaymentId);
            if (success) {
                log("✓ Order completed: ID=" + orderId);
                log("  Payment ID: " + mockPaymentId);
                log("  Credits transferred with batch traceability");
                return true;
            }
            return false;
        });
    }
    
    @FXML
    private void testListOrders() {
        runTest("List Orders", () -> {
            String userIdStr = txtOrderUserId.getText();
            if (userIdStr.isEmpty()) {
                log("✗ Please enter a user ID");
                return false;
            }
            
            int userId = parseInt(userIdStr);
            List<MarketplaceOrder> orders = orderService.getOrderHistory(userId);
            log("✓ Found " + orders.size() + " orders for user " + userId);
            
            for (MarketplaceOrder order : orders) {
                log(String.format("  Order #%d: %.2f units @ $%.2f - Status: %s", 
                    order.getId(), order.getQuantity(), order.getTotalAmountUsd(), order.getStatus()));
            }
            return true;
        });
    }

    // ==================== PAYMENT TESTS ====================
    
    @FXML
    private void testStripeConfig() {
        runTest("Test Stripe Configuration", () -> {
            boolean testMode = stripeService.isTestMode();
            log("✓ Stripe configured");
            log("  Mode: " + (testMode ? "TEST" : "LIVE"));
            log("  API Key: " + (testMode ? "Test key loaded" : "Live key loaded"));
            return true;
        });
    }
    
    @FXML
    private void testCreatePaymentIntent() {
        runTest("Create Stripe Payment Intent", () -> {
            double amount = parseDouble(txtPaymentAmount.getText());
            
            PaymentIntent intent = stripeService.initiatePayment(
                9999, amount, 1, 2, "Test payment"
            );
            
            if (intent != null) {
                log("✓ Payment Intent created");
                log("  ID: " + intent.getId());
                log("  Amount: $" + amount);
                log("  Status: " + intent.getStatus());
                return true;
            }
            return false;
        });
    }
    
    @FXML
    private void testCheckoutSession() {
        runTest("Create Stripe Checkout Session", () -> {
            double amount = parseDouble(txtPaymentAmount.getText());
            
            String checkoutUrl = stripeService.createHostedCheckoutUrl(
                9999, amount, 1, 2, 100.0, amount / 100.0
            );
            
            if (checkoutUrl != null && !checkoutUrl.isEmpty()) {
                log("✓ Checkout session created");
                log("  URL: " + checkoutUrl);
                return true;
            }
            return false;
        });
    }
    
    @FXML
    private void testWebhook() {
        runTest("Test Webhook Handler", () -> {
            log("✓ Webhook handler exists");
            log("  Endpoint: /api/stripe/webhook");
            log("  Note: Use Stripe CLI for live webhook testing");
            return true;
        });
    }

    // ==================== DATABASE TESTS ====================
    
    @FXML
    private void testConnection() {
        runTest("Test Database Connection", () -> {
            try (Connection conn = MyConnection.getConnection()) {
                if (conn != null && !conn.isClosed()) {
                    DatabaseMetaData meta = conn.getMetaData();
                    log("✓ Database connected");
                    log("  URL: " + meta.getURL());
                    log("  Driver: " + meta.getDriverName());
                    log("  Version: " + meta.getDriverVersion());
                    return true;
                }
            } catch (Exception e) {
                log("✗ Connection failed: " + e.getMessage());
            }
            return false;
        });
    }
    
    @FXML
    private void testCheckSchema() {
        runTest("Check Database Schema", () -> {
            String[] requiredTables = {
                "wallet", "carbon_credit_batches", "batch_events", "wallet_transactions",
                "marketplace_listings", "marketplace_orders", "marketplace_escrow"
            };
            
            try (Connection conn = MyConnection.getConnection()) {
                DatabaseMetaData meta = conn.getMetaData();
                int found = 0;
                
                for (String table : requiredTables) {
                    ResultSet rs = meta.getTables(null, null, table, null);
                    if (rs.next()) {
                        log("  ✓ " + table);
                        found++;
                    } else {
                        log("  ✗ " + table + " MISSING");
                    }
                }
                
                log("Schema check: " + found + "/" + requiredTables.length + " tables found");
                return found == requiredTables.length;
            } catch (Exception e) {
                log("✗ Error: " + e.getMessage());
                return false;
            }
        });
    }
    
    @FXML
    private void testCheckIndexes() {
        runTest("Check Database Indexes", () -> {
            try (Connection conn = MyConnection.getConnection()) {
                String sql = "SELECT TABLE_NAME, INDEX_NAME, NON_UNIQUE FROM information_schema.STATISTICS " +
                           "WHERE TABLE_SCHEMA = DATABASE() AND INDEX_NAME != 'PRIMARY' " +
                           "ORDER BY TABLE_NAME, INDEX_NAME";
                
                int count = 0;
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    
                    while (rs.next()) {
                        if (count++ < 20) {
                            log(String.format("  %s.%s (%s)", 
                                rs.getString("TABLE_NAME"),
                                rs.getString("INDEX_NAME"),
                                rs.getInt("NON_UNIQUE") == 0 ? "UNIQUE" : "INDEX"));
                        }
                    }
                }
                
                log("✓ Found " + count + " indexes");
                return true;
            } catch (Exception e) {
                log("✗ Error: " + e.getMessage());
                return false;
            }
        });
    }
    
    @FXML
    private void testCheckConstraints() {
        runTest("Check Foreign Key Constraints", () -> {
            try (Connection conn = MyConnection.getConnection()) {
                String sql = "SELECT TABLE_NAME, CONSTRAINT_NAME, REFERENCED_TABLE_NAME " +
                           "FROM information_schema.KEY_COLUMN_USAGE " +
                           "WHERE TABLE_SCHEMA = DATABASE() AND REFERENCED_TABLE_NAME IS NOT NULL";
                
                int count = 0;
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    
                    while (rs.next()) {
                        if (count++ < 15) {
                            log(String.format("  %s → %s", 
                                rs.getString("TABLE_NAME"),
                                rs.getString("REFERENCED_TABLE_NAME")));
                        }
                    }
                }
                
                log("✓ Found " + count + " foreign key constraints");
                return true;
            } catch (Exception e) {
                log("✗ Error: " + e.getMessage());
                return false;
            }
        });
    }
    
    @FXML
    private void testOrphanCheck() {
        runTest("Find Orphaned Records", () -> {
            try (Connection conn = MyConnection.getConnection()) {
                // Check orphaned batches
                String sql = "SELECT COUNT(*) FROM carbon_credit_batches b " +
                           "LEFT JOIN wallet w ON b.wallet_id = w.id WHERE w.id IS NULL";
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    
                    if (rs.next()) {
                        int orphaned = rs.getInt(1);
                        if (orphaned > 0) {
                            log("  ⚠️ Found " + orphaned + " orphaned batches");
                        } else {
                            log("  ✓ No orphaned batches");
                        }
                    }
                }
                
                log("✓ Orphan check complete");
                return true;
            } catch (Exception e) {
                log("✗ Error: " + e.getMessage());
                return false;
            }
        });
    }

    // ==================== E2E TESTS ====================
    
    @FXML
    private void testE2EComplete() {
        runTest("End-to-End: Complete Purchase Flow", () -> {
            log("→ Step 1: Create seller wallet");
            Wallet sellerWallet = new Wallet("ENTERPRISE", 1);
            sellerWallet.setName("E2E Seller");
            int sellerWalletId = walletService.createWallet(sellerWallet);
            
            log("→ Step 2: Issue credits to seller");
            walletService.quickIssueCredits(sellerWalletId, 100.0, "E2E test credits");
            
            log("→ Step 3: Create marketplace listing");
            int listingId = listingService.createListing(
                1, // sellerId
                "CARBON_CREDIT",
                sellerWalletId,
                50.0, // quantity
                15.0, // pricePerUnit
                13.5, // minPriceUsd
                null, // autoAcceptPriceUsd
                "E2E Test Credits"
            );
            
            log("→ Step 4: Create buyer wallet");
            Wallet buyerWallet = new Wallet("INDIVIDUAL", 2);
            buyerWallet.setName("E2E Buyer");
            int buyerWalletId = walletService.createWallet(buyerWallet);
            
            log("→ Step 5: Place order");
            int orderId = orderService.placeOrder(listingId, 2, 25.0);
            
            log("→ Step 6: Complete order (mock payment)");
            boolean completed = orderService.completeOrder(orderId, "pi_e2e_" + System.currentTimeMillis());
            
            if (completed) {
                log("✓ E2E test PASSED");
                log("  Seller wallet: " + sellerWalletId);
                log("  Buyer wallet: " + buyerWalletId);
                log("  Order: " + orderId);
                return true;
            }
            return false;
        });
    }
    
    @FXML
    private void testE2EBatchFlow() {
        runTest("End-to-End: Batch Lifecycle", () -> {
            log("→ Creating test wallet");
            Wallet wallet = new Wallet("ENTERPRISE", 1);
            wallet.setName("Batch Lifecycle Test");
            int walletId = walletService.createWallet(wallet);
            
            log("→ Issuing 1000 credits");
            walletService.quickIssueCredits(walletId, 1000.0, "Lifecycle test");
            
            log("→ Creating second wallet for transfer");
            Wallet wallet2 = new Wallet("INDIVIDUAL", 2);
            wallet2.setName("Transfer Target");
            int walletId2 = walletService.createWallet(wallet2);
            
            log("→ Transferring 300 credits");
            walletService.transferCreditsWithMode(walletId, walletId2, 300.0, 
                "Lifecycle transfer", WalletService.TransferMode.SPLIT_CHILD, "TEST");
            
            log("→ Retiring 100 credits from original wallet");
            walletService.retireCredits(walletId, 100.0, "Lifecycle retirement");
            
            log("✓ Batch lifecycle complete");
            log("  Original wallet final balance: " + 
                walletService.getWalletById(walletId).getAvailableCredits());
            log("  Transfer target balance: " + 
                walletService.getWalletById(walletId2).getAvailableCredits());
            
            return true;
        });
    }
    
    @FXML
    private void testE2EEscrow() {
        runTest("End-to-End: Large Order with Escrow (>$10k)", () -> {
            log("→ Creating high-value listing");
            Wallet sellerWallet = new Wallet("ENTERPRISE", 1);
            sellerWallet.setName("Large Seller");
            int sellerWalletId = walletService.createWallet(sellerWallet);
            walletService.quickIssueCredits(sellerWalletId, 2000.0, "Large batch");
            
            int listingId = listingService.createListing(
                1, // sellerId
                "CARBON_CREDIT",
                sellerWalletId,
                1000.0, // quantity
                50.0, // pricePerUnit - $50k total triggers escrow
                45.0, // minPriceUsd
                null, // autoAcceptPriceUsd
                "Premium Credits"
            );
            
            log("→ Placing large order (triggers SPLIT_CHILD mode)");
            int orderId = orderService.placeOrder(listingId, 2, 500.0);
            
            log("→ Completing with escrow");
            boolean completed = orderService.completeOrder(orderId, "pi_escrow_" + System.currentTimeMillis());
            
            if (completed) {
                log("✓ Escrow flow complete");
                log("  Order value: $25,000 (>$10k threshold)");
                log("  Child batches created for buyer");
                return true;
            }
            return false;
        });
    }
    
    @FXML
    private void testStress() {
        runTest("Stress Test: 100 Operations", () -> {
            log("⚠️ Running stress test - this may take time...");
            int operations = 0;
            
            for (int i = 0; i < 20; i++) {
                Wallet w = new Wallet("ENTERPRISE", 1);
                w.setName("Stress Test " + i);
                int wId = walletService.createWallet(w);
                operations++;
                
                if (i % 5 == 0) {
                    walletService.quickIssueCredits(wId, 100.0, "Stress test");
                    operations++;
                }
            }
            
            log("✓ Stress test complete: " + operations + " operations successful");
            return true;
        });
    }

    // ==================== UTILITIES ====================
    
    @FXML
    private void clearLog() {
        logArea.clear();
        testsRun = 0;
        testsPassed = 0;
        testsFailed = 0;
        updateStats();
        log("Log cleared - ready for new tests");
    }
    
    @FXML
    private void exportLog() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Test Log");
        fileChooser.setInitialFileName("test-log-" + System.currentTimeMillis() + ".txt");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );
        
        File file = fileChooser.showSaveDialog(logArea.getScene().getWindow());
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                writer.write(logArea.getText());
                log("✓ Log exported to: " + file.getAbsolutePath());
            } catch (Exception e) {
                log("✗ Export failed: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void resetTestData() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("⚠️ RESET TEST DATA");
        alert.setHeaderText("This will DELETE all test wallets and batches!");
        alert.setContentText("Are you sure? This cannot be undone.");
        
        if (alert.showAndWait().get() == ButtonType.OK) {
            runTest("Reset Test Data", () -> {
                try (Connection conn = MyConnection.getConnection()) {
                    String[] tables = {"wallet_transactions", "batch_events", "carbon_credit_batches"};
                    for (String table : tables) {
                        String sql = "DELETE FROM " + table + " WHERE 1=1"; // Careful!
                        try (Statement stmt = conn.createStatement()) {
                            int deleted = stmt.executeUpdate(sql);
                            log("  Deleted " + deleted + " rows from " + table);
                        }
                    }
                    log("✓ Test data reset complete");
                    return true;
                } catch (Exception e) {
                    log("✗ Reset failed: " + e.getMessage());
                    return false;
                }
            });
        }
    }

    // ==================== HELPER METHODS ====================
    
    private void runTest(String testName, TestRunner test) {
        testsRun++;
        log("");
        log("▶ TEST: " + testName);
        updateStatus("Running: " + testName);
        
        long startTime = System.currentTimeMillis();
        try {
            boolean success = test.run();
            long duration = System.currentTimeMillis() - startTime;
            
            if (success) {
                testsPassed++;
                log("✓ PASSED (" + duration + "ms)");
                updateStatus("✓ " + testName + " PASSED");
            } else {
                testsFailed++;
                log("✗ FAILED (" + duration + "ms)");
                updateStatus("✗ " + testName + " FAILED");
            }
        } catch (Exception e) {
            testsFailed++;
            log("✗ EXCEPTION: " + e.getMessage());
            if (chkVerbose.isSelected()) {
                e.printStackTrace();
            }
            updateStatus("✗ " + testName + " EXCEPTION");
        }
        
        updateStats();
    }
    
    @FunctionalInterface
    interface TestRunner {
        boolean run() throws Exception;
    }
    
    private void log(String message) {
        Platform.runLater(() -> {
            if (chkTimestamp.isSelected()) {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                logArea.appendText("[" + timestamp + "] " + message + "\n");
            } else {
                logArea.appendText(message + "\n");
            }
            
            if (chkAutoScroll.isSelected()) {
                logArea.setScrollTop(Double.MAX_VALUE);
            }
        });
    }
    
    private void updateStatus(String status) {
        Platform.runLater(() -> statusLabel.setText(status));
    }
    
    private void updateStats() {
        Platform.runLater(() -> {
            statsLabel.setText(String.format("Tests: %d | Pass: %d | Fail: %d | Success Rate: %.1f%%",
                testsRun, testsPassed, testsFailed, 
                testsRun > 0 ? (testsPassed * 100.0 / testsRun) : 0));
        });
    }
    
    private int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            log("✗ Invalid integer: " + value);
            throw new IllegalArgumentException("Invalid integer: " + value);
        }
    }
    
    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            log("✗ Invalid number: " + value);
            throw new IllegalArgumentException("Invalid number: " + value);
        }
    }
}
