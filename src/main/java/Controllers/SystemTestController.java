package Controllers;

import Models.Wallet;
import Models.CarbonCreditBatch;
import Services.WalletService;
import Services.TransferService;
import Services.BatchEventService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.event.ActionEvent;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Test Controller for Green Wallet + Batch + Marketplace System Integration Testing.
 * 
 * Allows manual testing of:
 * - Wallet creation
 * - Batch issuance (simulates project evaluation)
 * - Credit transfers between wallets
 * - Batch lineage tracking
 * 
 * This controller is ISOLATED from the project management system.
 * No modifications to existing projet/evaluation code required.
 */
public class SystemTestController {
    
    private WalletService walletService = new WalletService();
    private TransferService transferService = new TransferService();
    private BatchEventService eventService = new BatchEventService();
    
    @FXML private TextArea testLog;
    @FXML private TextField walletIdField;
    @FXML private TextField projectIdField;
    @FXML private TextField amountField;
    @FXML private TextField toWalletIdField;
    @FXML private TextField fromWalletIdField;
    @FXML private TextField transferAmountField;
    @FXML private TextField viewWalletIdField;
    @FXML private Button createWalletBtn;
    @FXML private Button issueBatchBtn;
    @FXML private Button transferBtn;
    @FXML private Button viewBtn;
    @FXML private Button clearLogBtn;
    @FXML private Label statusLabel;
    
    @FXML
    public void initialize() {
        appendLog("=== Green Wallet System Test Console Initialized ===");
        appendLog("Use this panel to test wallet, batch, and marketplace operations");
        appendLog("All changes are atomic and fully traceable");
    }
    
    /**
     * TEST: Create wallet
     */
    @FXML
    private void handleCreateWallet() {
        try {
            Wallet w = new Wallet("ENTERPRISE", 1);
            w.setName("Test Enterprise Wallet");
            
            int walletId = walletService.createWallet(w);
            if (walletId > 0) {
                appendLog("✓ Wallet created successfully");
                appendLog("  Wallet ID: " + walletId);
                walletIdField.setText(String.valueOf(walletId));
                updateStatus("SUCCESS: Wallet created (ID: " + walletId + ")");
            } else {
                appendLog("✗ Failed to create wallet");
                updateStatus("FAILED: Could not create wallet");
            }
        } catch (Exception ex) {
            appendLog("✗ Error: " + ex.getMessage());
            updateStatus("ERROR: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * TEST: Issue batch (simulates project evaluation approval)
     */
    @FXML
    private void handleIssueBatch() {
        try {
            int walletId = getIntField(walletIdField, "Wallet ID");
            int projectId = getIntField(projectIdField, "Project ID");
            double amount = getDoubleField(amountField, "Amount");
            
            if (walletId > 0 && projectId > 0 && amount > 0) {
                boolean success = walletService.issueCredits(
                    walletId, projectId, amount,
                    "TEST: Manual batch issuance",
                    null,
                    "TEST_USER"
                );
                
                if (success) {
                    appendLog("✓ Batch issued successfully");
                    appendLog("  Project: " + projectId);
                    appendLog("  Wallet: " + walletId);
                    appendLog("  Amount: " + amount + " credits");
                    updateStatus("SUCCESS: Batch issued (" + amount + " credits)");
                } else {
                    appendLog("✗ Failed to issue batch");
                    updateStatus("FAILED: Batch issuance failed");
                }
            }
        } catch (Exception ex) {
            appendLog("✗ Error: " + ex.getMessage());
            updateStatus("ERROR: " + ex.getMessage());
        }
    }
    
    /**
     * TEST: Transfer credits between wallets (atomic operation)
     */
    @FXML
    private void handleTransferCredits() {
        try {
            int fromWalletId = getIntField(fromWalletIdField, "From Wallet ID");
            int toWalletId = getIntField(toWalletIdField, "To Wallet ID");
            double amount = getDoubleField(transferAmountField, "Amount");
            
            if (fromWalletId > 0 && toWalletId > 0 && amount > 0) {
                TransferService.TransferResult result = transferService.transferCredits(
                    fromWalletId, toWalletId, amount,
                    "TEST: Manual transfer"
                );
                
                if (result.isSuccess()) {
                    appendLog("✓ Transfer successful");
                    appendLog("  Transfer ID: " + result.getTransferId());
                    appendLog("  From Wallet: " + fromWalletId);
                    appendLog("  To Wallet: " + toWalletId);
                    appendLog("  Amount: " + result.getAmount() + " credits");
                    updateStatus("SUCCESS: " + result.getAmount() + " credits transferred");
                } else {
                    appendLog("✗ Transfer failed: " + result.getMessage());
                    updateStatus("FAILED: " + result.getMessage());
                }
            }
        } catch (Exception ex) {
            appendLog("✗ Error: " + ex.getMessage());
            updateStatus("ERROR: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * TEST: View wallet balance
     */
    @FXML
    private void handleViewWallet() {
        try {
            int walletId = getIntField(viewWalletIdField, "Wallet ID");
            
            if (walletId > 0) {
                Wallet w = walletService.getWalletById(walletId);
                if (w != null) {
                    appendLog("✓ Wallet details:");
                    appendLog("  ID: " + w.getId());
                    appendLog("  Wallet Number: " + w.getWalletNumber());
                    appendLog("  Name: " + w.getName());
                    appendLog("  Owner Type: " + w.getOwnerType());
                    appendLog("  Available Credits: " + String.format("%.2f", w.getAvailableCredits()));
                    appendLog("  Retired Credits: " + String.format("%.2f", w.getRetiredCredits()));
                    appendLog("  Total: " + String.format("%.2f", w.getTotalCredits()));
                    updateStatus("Retrieved wallet " + walletId + ": " + String.format("%.2f", w.getAvailableCredits()) + " available");
                } else {
                    appendLog("✗ Wallet not found");
                    updateStatus("NOT FOUND: Wallet " + walletId);
                }
            }
        } catch (Exception ex) {
            appendLog("✗ Error: " + ex.getMessage());
            updateStatus("ERROR: " + ex.getMessage());
        }
    }
    
    /**
     * TEST: Clear log
     */
    @FXML
    private void handleClearLog() {
        testLog.clear();
        appendLog("=== Log Cleared ===");
        updateStatus("Log cleared");
    }
    
    /**
     * Helper: Append to log
     */
    private synchronized void appendLog(String message) {
        testLog.appendText(message + "\n");
    }
    
    /**
     * Helper: Update status label
     */
    private void updateStatus(String message) {
        statusLabel.setText(message);
    }
    
    /**
     * Helper: Parse integer field
     */
    private int getIntField(TextField field, String fieldName) throws NumberFormatException {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (NumberFormatException ex) {
            throw new NumberFormatException(fieldName + " must be an integer");
        }
    }
    
    /**
     * Helper: Parse double field
     */
    private double getDoubleField(TextField field, String fieldName) throws NumberFormatException {
        try {
            return Double.parseDouble(field.getText().trim());
        } catch (NumberFormatException ex) {
            throw new NumberFormatException(fieldName + " must be a decimal number");
        }
    }
}
