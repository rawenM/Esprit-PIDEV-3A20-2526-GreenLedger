package Controllers;

import Models.BatchEvent;
import Models.CarbonCreditBatch;
import Models.Wallet;
import Services.BatchEventService;
import Services.WalletService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.GreenLedger.MainFX;

import java.io.IOException;
import java.util.List;

public class BatchCarbonTestController {

    private final WalletService walletService = new WalletService();
    private final BatchEventService batchEventService = new BatchEventService();

    @FXML private TextField txtOwnerType;
    @FXML private TextField txtWalletId;
    @FXML private Label lblWalletBalance;

    @FXML private TextField txtIssueAmount;
    @FXML private TextField txtBatchId;

    @FXML private TextField txtToWalletId;
    @FXML private TextField txtTransferAmount;
    @FXML private TextField txtRetireAmount;

    @FXML private TextArea logArea;
    @FXML private Label lblStatus;
    @FXML private Label lblStats;

    private int total;
    private int passed;
    private int failed;

    @FXML
    public void initialize() {
        log("=== Batch & Carbon Credit Test Panel Ready ===");
        log("Use this panel to test only carbon credits and batches.");
        updateStats();
    }

    @FXML
    private void onBackToGreenWallet() {
        try {
            MainFX.setRoot("greenwallet");
        } catch (IOException e) {
            log("[ERROR] Back navigation failed: " + e.getMessage());
        }
    }

    @FXML
    private void testCreateWallet() {
        runTest("Create Wallet", () -> {
            String ownerType = getOrDefault(txtOwnerType.getText(), "ENTERPRISE");
            Wallet wallet = new Wallet(ownerType, 1);
            wallet.setName("Batch Test Wallet " + System.currentTimeMillis());
            int id = walletService.createWallet(wallet);
            if (id <= 0) return false;
            txtWalletId.setText(String.valueOf(id));
            log("[OK] Wallet created id=" + id + " type=" + ownerType);
            return true;
        });
    }

    @FXML
    private void testLoadWallet() {
        runTest("Load Wallet", () -> {
            int walletId = parseInt(txtWalletId.getText(), "Wallet ID required");
            Wallet wallet = walletService.getWalletById(walletId);
            if (wallet == null) {
                log("[FAIL] Wallet not found: " + walletId);
                return false;
            }
            lblWalletBalance.setText(String.format("Solde: %.2f", wallet.getAvailableCredits()));
            log(String.format("[OK] Wallet #%d available=%.2f retired=%.2f", wallet.getId(), wallet.getAvailableCredits(), wallet.getRetiredCredits()));
            return true;
        });
    }

    @FXML
    private void testIssueCredits() {
        runTest("Issue Credits", () -> {
            int walletId = parseInt(txtWalletId.getText(), "Wallet ID required");
            double amount = parseDouble(txtIssueAmount.getText(), "Issue amount required");
            boolean ok = walletService.quickIssueCredits(walletId, amount, "BatchCarbonTest issue");
            if (!ok) return false;
            log(String.format("[OK] Issued %.2f credits to wallet %d", amount, walletId));
            testLoadWallet();
            return true;
        });
    }

    @FXML
    private void testListBatches() {
        runTest("List Batches", () -> {
            int walletId = parseInt(txtWalletId.getText(), "Wallet ID required");
            List<CarbonCreditBatch> batches = walletService.getWalletBatches(walletId);
            log("[OK] Found " + batches.size() + " batches");
            for (CarbonCreditBatch batch : batches) {
                log(String.format("  Batch #%d | serial=%s | total=%.2f | remaining=%.2f | status=%s",
                    batch.getId(),
                    batch.getSerialNumber(),
                    batch.getTotalAmount() != null ? batch.getTotalAmount().doubleValue() : 0.0,
                    batch.getRemainingAmount() != null ? batch.getRemainingAmount().doubleValue() : 0.0,
                    batch.getStatus()));
            }
            if (!batches.isEmpty()) {
                txtBatchId.setText(String.valueOf(batches.get(0).getId()));
            }
            return true;
        });
    }

    @FXML
    private void testBatchLineage() {
        runTest("Batch Lineage", () -> {
            int batchId = parseInt(txtBatchId.getText(), "Batch ID required");
            List<CarbonCreditBatch> lineage = walletService.getBatchLineage(batchId);
            log("[OK] Lineage size=" + lineage.size());
            for (CarbonCreditBatch batch : lineage) {
                log(String.format("  Batch #%d parent=%s remaining=%.2f status=%s",
                    batch.getId(),
                    batch.getParentBatchId(),
                    batch.getRemainingAmount() != null ? batch.getRemainingAmount().doubleValue() : 0.0,
                    batch.getStatus()));
            }
            return true;
        });
    }

    @FXML
    private void testBatchEvents() {
        runTest("Batch Events", () -> {
            int batchId = parseInt(txtBatchId.getText(), "Batch ID required");
            List<BatchEvent> events = batchEventService.getBatchEvents(batchId);
            log("[OK] Events=" + events.size());
            for (BatchEvent event : events) {
                String hash = event.getEventHash();
                String shortHash = (hash == null || hash.length() < 10) ? hash : hash.substring(0, 10) + "...";
                log(String.format("  [%s] %s actor=%s hash=%s",
                    event.getCreatedAt(), event.getEventType(), event.getActor(), shortHash));
            }
            return true;
        });
    }

    @FXML
    private void testTransferDirect() {
        runTest("Transfer DIRECT", () -> {
            int fromWallet = parseInt(txtWalletId.getText(), "Source wallet ID required");
            int toWallet = parseInt(txtToWalletId.getText(), "Destination wallet ID required");
            double amount = parseDouble(txtTransferAmount.getText(), "Transfer amount required");
            boolean ok = walletService.transferCreditsWithMode(
                fromWallet,
                toWallet,
                amount,
                "BatchCarbonTest direct",
                WalletService.TransferMode.DIRECT,
                "TEST_USER"
            );
            if (!ok) return false;
            log(String.format("[OK] DIRECT transfer %.2f from %d to %d", amount, fromWallet, toWallet));
            return true;
        });
    }

    @FXML
    private void testTransferSplit() {
        runTest("Transfer SPLIT_CHILD", () -> {
            int fromWallet = parseInt(txtWalletId.getText(), "Source wallet ID required");
            int toWallet = parseInt(txtToWalletId.getText(), "Destination wallet ID required");
            double amount = parseDouble(txtTransferAmount.getText(), "Transfer amount required");
            boolean ok = walletService.transferCreditsWithMode(
                fromWallet,
                toWallet,
                amount,
                "BatchCarbonTest split",
                WalletService.TransferMode.SPLIT_CHILD,
                "TEST_USER"
            );
            if (!ok) return false;
            log(String.format("[OK] SPLIT_CHILD transfer %.2f from %d to %d", amount, fromWallet, toWallet));
            return true;
        });
    }

    @FXML
    private void testRetireCredits() {
        runTest("Retire Credits", () -> {
            int walletId = parseInt(txtWalletId.getText(), "Wallet ID required");
            double amount = parseDouble(txtRetireAmount.getText(), "Retire amount required");
            boolean ok = walletService.retireCredits(walletId, amount, "BatchCarbonTest retire");
            if (!ok) return false;
            log(String.format("[OK] Retired %.2f from wallet %d", amount, walletId));
            testLoadWallet();
            return true;
        });
    }

    @FXML
    private void clearLog() {
        logArea.clear();
        total = 0;
        passed = 0;
        failed = 0;
        updateStats();
        lblStatus.setText("Log cleared");
    }

    private void runTest(String name, ThrowingBooleanSupplier runner) {
        total++;
        lblStatus.setText("Running: " + name);
        try {
            boolean ok = runner.getAsBoolean();
            if (ok) {
                passed++;
                lblStatus.setText("[PASS] " + name);
            } else {
                failed++;
                lblStatus.setText("[FAIL] " + name);
            }
        } catch (Exception e) {
            failed++;
            lblStatus.setText("[ERROR] " + name);
            log("[ERROR] " + name + " -> " + e.getMessage());
        }
        updateStats();
    }

    private void updateStats() {
        lblStats.setText(String.format("Tests: %d | Pass: %d | Fail: %d", total, passed, failed));
    }

    private void log(String text) {
        logArea.appendText(text + "\n");
    }

    private int parseInt(String raw, String error) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalArgumentException(error);
        }
        return Integer.parseInt(raw.trim());
    }

    private double parseDouble(String raw, String error) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalArgumentException(error);
        }
        return Double.parseDouble(raw.trim());
    }

    private String getOrDefault(String raw, String fallback) {
        if (raw == null || raw.trim().isEmpty()) {
            return fallback;
        }
        return raw.trim();
    }

    @FunctionalInterface
    interface ThrowingBooleanSupplier {
        boolean getAsBoolean() throws Exception;
    }
}
