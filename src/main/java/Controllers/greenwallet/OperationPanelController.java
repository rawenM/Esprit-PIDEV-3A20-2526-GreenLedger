package Controllers.greenwallet;

import Models.Wallet;
import Models.OperationWallet;
import Services.WalletService;
import Controllers.greenwallet.GreenWalletOrchestratorController;
import Utils.EventBusManager;
import Utils.EventBusManager.*;
import com.google.common.eventbus.Subscribe;

import javafx.scene.control.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;

import java.time.Year;
import java.util.List;
import java.math.BigDecimal;

/**
 * Operation Panel Controller - Slide-In Form Handler
 * 
 * Responsibilities:
 * - Manage Issue Credits form with serial number preview
 * - Manage Retire Credits form with irreversibility warning
 * - Manage Transfer Credits form with recipient validation
 * - Form validation and business logic (amounts, recipients)
 * - Execute operations via WalletService
 * - Show inline success/error notifications (no dialogs)
 * 
 * Psychology Features:
 * - Issue: Show "Pending Serial" until confirmed (commitment device)
 * - Retire: Red WARNING box "Action Irréversible" (loss aversion)
 * - Transfer: Recipient tier badge (status signaling)
 * 
 * @author GreenLedger Team
 * @version 2.0 - Production Ready (Implemented)
 */
public class OperationPanelController {
    
    private WalletService walletService;
    private GreenWalletOrchestratorController orchestrator;
    
    // Current wallet being operated on
    private Wallet currentWallet;
    
    // Issue Form Components
    private TextField txtIssueAmount;
    private ComboBox<String> cmbVerificationStandard;
    private TextField txtVintageYear;
    private TextArea txtIssueReference;
    private Label lblIssuePreviewAmount;
    private Label lblIssuePreviewSerial;
    
    // Retire Form Components
    private TextField txtRetireAmount;
    private ComboBox<String> cmbRetireReason;
    private TextArea txtRetireReason;
    private Label lblRetireAvailable;
    private Label lblRetirePreviewAmount;
    
    // Transfer Form Components
    private ComboBox<Wallet> cmbTransferTargetWallet;
    private TextField txtTransferWalletNumber;
    private TextField txtTransferAmount;
    private TextArea txtTransferReference;
    private Label lblTransferAvailable;
    
    // Verification standards for carbon credits
    private static final String[] VERIFICATION_STANDARDS = {
        "VCS (Verra)",
        "Gold Standard",
        "Climate Action Reserve",
        "American Carbon Registry",
        "ISO 14064",
        "Other"
    };
    
    // Common retirement reasons
    private static final String[] RETIREMENT_REASONS = {
        "Corporate Offsetting",
        "Event Neutralization",
        "Product Carbon Neutral",
        "Supply Chain Offsetting",
        "Voluntary Offset",
        "Compliance Obligation",
        "Other"
    };
    
    public OperationPanelController(WalletService walletService, GreenWalletOrchestratorController orchestrator) {
        this.walletService = walletService;
        this.orchestrator = orchestrator;
        EventBusManager.register(this);
    }
    
    // ============================================================================
    // UI COMPONENT SETTERS (Called by parent controller with FXML references)
    // ============================================================================
    
    public void setIssueFormComponents(TextField txtAmount, ComboBox<String> cmbStandard,
                                       TextField txtVintage, TextArea txtReference,
                                       Label lblPreviewAmount, Label lblPreviewSerial) {
        this.txtIssueAmount = txtAmount;
        this.cmbVerificationStandard = cmbStandard;
        this.txtVintageYear = txtVintage;
        this.txtIssueReference = txtReference;
        this.lblIssuePreviewAmount = lblPreviewAmount;
        this.lblIssuePreviewSerial = lblPreviewSerial;
    }
    
    public void setRetireFormComponents(TextField txtAmount, ComboBox<String> cmbReason,
                                       TextArea txtReason, Label lblAvailable,
                                       Label lblPreview) {
        this.txtRetireAmount = txtAmount;
        this.cmbRetireReason = cmbReason;
        this.txtRetireReason = txtReason;
        this.lblRetireAvailable = lblAvailable;
        this.lblRetirePreviewAmount = lblPreview;
    }
    
    public void setTransferFormComponents(ComboBox<Wallet> cmbTarget, TextField txtWalletNum,
                                         TextField txtAmount, TextArea txtReference,
                                         Label lblAvailable) {
        this.cmbTransferTargetWallet = cmbTarget;
        this.txtTransferWalletNumber = txtWalletNum;
        this.txtTransferAmount = txtAmount;
        this.txtTransferReference = txtReference;
        this.lblTransferAvailable = lblAvailable;
    }
    
    // ============================================================================
    // ISSUE CREDITS FORM
    // ============================================================================
    
    /**
     * Prepare issue credits form for wallet.
     * Initialize form fields, bind validators, setup listeners
     */
    public void prepareIssueForm(Wallet wallet) {
        this.currentWallet = wallet;
        System.out.println("[OperationPanel] Preparing Issue form for wallet: " + wallet.getWalletNumber());
        
        if (cmbVerificationStandard != null) {
            cmbVerificationStandard.setItems(FXCollections.observableArrayList(VERIFICATION_STANDARDS));
            cmbVerificationStandard.getSelectionModel().selectFirst();
        }
        
        if (txtVintageYear != null) {
            txtVintageYear.setText(String.valueOf(Year.now().getValue()));
        }
        
        if (txtIssueAmount != null) {
            txtIssueAmount.clear();
            // Add input validation listener
            txtIssueAmount.textProperty().addListener((obs, oldVal, newVal) -> {
                updateIssuePreview();
            });
        }
        
        if (txtIssueReference != null) {
            txtIssueReference.clear();
            txtIssueReference.setPromptText("e.g., Batch #12345 from Solar Project XYZ");
        }
        
        updateIssuePreview();
    }
    
    /**
     * Update issue preview labels
     */
    private void updateIssuePreview() {
        if (lblIssuePreviewAmount != null && txtIssueAmount != null) {
            try {
                double amount = Double.parseDouble(txtIssueAmount.getText());
                lblIssuePreviewAmount.setText(String.format("%.2f tCO₂e", amount));
            } catch (NumberFormatException e) {
                lblIssuePreviewAmount.setText("-- tCO₂e");
            }
        }
        
        if (lblIssuePreviewSerial != null) {
            lblIssuePreviewSerial.setText("CC-" + Year.now().getValue() + "-XXXXXX (Pending)");
        }
    }
    
    /**
     * Execute issue credits operation.
     * Validate form, call WalletService.issueCredits(), post BatchIssuedEvent
     */
    public void executeIssue() {
        System.out.println("[OperationPanel] Executing issue operation...");
        
        if (currentWallet == null) {
            showError("No wallet selected");
            return;
        }
        
        // Validate amount
        double amount;
        try {
            amount = Double.parseDouble(txtIssueAmount.getText());
            if (amount <= 0) {
                showError("Amount must be greater than zero");
                return;
            }
            if (amount > 1000000) { // Reasonable upper limit
                showError("Amount exceeds maximum allowed (1,000,000 tCO₂e)");
                return;
            }
        } catch (NumberFormatException e) {
            showError("Please enter a valid amount");
            return;
        }
        
        // Get description
        String description = (txtIssueReference != null && !txtIssueReference.getText().isEmpty()) 
            ? txtIssueReference.getText() 
            : "Carbon credits issued";
        
        String standard = (cmbVerificationStandard != null && cmbVerificationStandard.getValue() != null)
            ? cmbVerificationStandard.getValue()
            : "VCS (Verra)";
            
        String vintage = (txtVintageYear != null && !txtVintageYear.getText().isEmpty())
            ? txtVintageYear.getText()
            : String.valueOf(Year.now().getValue());
        
        String fullDescription = String.format("%s [%s, Vintage: %s]", description, standard, vintage);
        
        // Execute via service
        boolean success = walletService.quickIssueCredits(currentWallet.getId(), amount, fullDescription);
        
        if (success) {
            showSuccess(String.format("Successfully issued %.2f tCO₂e to wallet #%s", 
                amount, currentWallet.getWalletNumber()));
            
            // Post events
            EventBusManager.post(new WalletUpdatedEvent(currentWallet.getId(), 
                "CREDITS_ISSUED", amount));
            EventBusManager.post(new BatchIssuedEvent(0, currentWallet.getId(), 0, 
                amount, "CC-" + Year.now().getValue() + "-PENDING"));
            
            // Clear form
            clearIssueForm();
        } else {
            showError("Failed to issue credits. Please check the wallet and try again.");
        }
    }
    
    /**
     * Clear issue form
     */
    private void clearIssueForm() {
        if (txtIssueAmount != null) txtIssueAmount.clear();
        if (txtIssueReference != null) txtIssueReference.clear();
        updateIssuePreview();
    }
    
    // ============================================================================
    // RETIRE CREDITS FORM
    // ============================================================================
    
    /**
     * Prepare retire credits form for wallet.
     * Display available balance, setup irreversibility warning styling
     */
    public void prepareRetireForm(Wallet wallet) {
        this.currentWallet = wallet;
        System.out.println("[OperationPanel] Preparing Retire form for wallet: " + wallet.getWalletNumber());
        
        // Load retirement reasons
        if (cmbRetireReason != null) {
            cmbRetireReason.setItems(FXCollections.observableArrayList(RETIREMENT_REASONS));
            cmbRetireReason.getSelectionModel().selectFirst();
        }
        
        // Display available balance
        double available = wallet.getAvailableCredits();
        if (lblRetireAvailable != null) {
            lblRetireAvailable.setText(String.format("Available: %.2f tCO₂e", available));
            lblRetireAvailable.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
        }
        
        if (txtRetireAmount != null) {
            txtRetireAmount.clear();
            // Add input validation listener
            txtRetireAmount.textProperty().addListener((obs, oldVal, newVal) -> {
                updateRetirePreview();
            });
        }
        
        if (txtRetireReason != null) {
            txtRetireReason.clear();
            txtRetireReason.setPromptText("Specify the purpose of retirement (required for reporting)");
        }
        
        updateRetirePreview();
    }
    
    /**
     * Update retire preview labels
     */
    private void updateRetirePreview() {
        if (lblRetirePreviewAmount != null && txtRetireAmount != null && currentWallet != null) {
            try {
                double amount = Double.parseDouble(txtRetireAmount.getText());
                double remaining = currentWallet.getAvailableCredits() - amount;
                lblRetirePreviewAmount.setText(String.format("Will retire: %.2f tCO₂e (Remaining: %.2f)", 
                    amount, Math.max(0, remaining)));
                
                // Warning if exceeds available
                if (amount > currentWallet.getAvailableCredits()) {
                    lblRetirePreviewAmount.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                } else {
                    lblRetirePreviewAmount.setStyle("-fx-text-fill: #34495e;");
                }
            } catch (NumberFormatException e) {
                lblRetirePreviewAmount.setText("-- tCO₂e");
                lblRetirePreviewAmount.setStyle("-fx-text-fill: #95a5a6;");
            }
        }
    }
    
    /**
     * Execute retire credits operation.
     * Show confirmation dialog, call WalletService.retireCredits(), post CreditsRetiredEvent
     */
    public void executeRetire() {
        System.out.println("[OperationPanel] Executing retire operation...");
        
        if (currentWallet == null) {
            showError("No wallet selected");
            return;
        }
        
        // Validate amount
        double amount;
        try {
            amount = Double.parseDouble(txtRetireAmount.getText());
            if (amount <= 0) {
                showError("Amount must be greater than zero");
                return;
            }
            if (amount > currentWallet.getAvailableCredits()) {
                showError(String.format("Insufficient credits. Available: %.2f tCO₂e", 
                    currentWallet.getAvailableCredits()));
                return;
            }
        } catch (NumberFormatException e) {
            showError("Please enter a valid amount");
            return;
        }
        
        // Get retirement reason
        String reason = (txtRetireReason != null && !txtRetireReason.getText().isEmpty())
            ? txtRetireReason.getText()
            : "Carbon offset retirement";
        
        String reasonType = (cmbRetireReason != null && cmbRetireReason.getValue() != null)
            ? cmbRetireReason.getValue()
            : "Voluntary Offset";
        
        String fullReason = String.format("[%s] %s", reasonType, reason);
        
        // Show confirmation dialog (retirement is irreversible)
        boolean confirmed = showConfirmation(
            "Confirm Retirement",
            String.format("⚠️  IRREVERSIBLE ACTION\n\nYou are about to permanently retire %.2f tCO₂e.\n\n" +
                "These credits cannot be recovered or transferred.\n\nProceed?", amount)
        );
        
        if (!confirmed) {
            return;
        }
        
        // Execute via service
        boolean success = walletService.retireCredits(currentWallet.getId(), amount, fullReason);
        
        if (success) {
            showSuccess(String.format("Successfully retired %.2f tCO₂e from wallet #%s", 
                amount, currentWallet.getWalletNumber()));
            
            // Post events
            EventBusManager.post(new WalletUpdatedEvent(currentWallet.getId(), 
                "CREDITS_RETIRED", amount));
            EventBusManager.post(new CreditsRetiredEvent(currentWallet.getId(), 
                amount, fullReason, new int[0]));
            
            // Clear form
            clearRetireForm();
        } else {
            showError("Failed to retire credits. Please check the wallet and try again.");
        }
    }
    
    /**
     * Clear retire form
     */
    private void clearRetireForm() {
        if (txtRetireAmount != null) txtRetireAmount.clear();
        if (txtRetireReason != null) txtRetireReason.clear();
        updateRetirePreview();
    }
    
    // ============================================================================
    // TRANSFER CREDITS FORM
    // ============================================================================
    
    /**
     * Prepare transfer credits form for wallet.
     * Load list of other wallets, setup recipient validation
     */
    public void prepareTransferForm(Wallet wallet) {
        this.currentWallet = wallet;
        System.out.println("[OperationPanel] Preparing Transfer form for wallet: " + wallet.getWalletNumber());
        
        // Display available balance
        double available = wallet.getAvailableCredits();
        if (lblTransferAvailable != null) {
            lblTransferAvailable.setText(String.format("Available: %.2f tCO₂e", available));
            lblTransferAvailable.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
        }
        
        // Load available wallets (exclude current wallet)
        if (cmbTransferTargetWallet != null) {
            List<Wallet> allWallets = walletService.getAllWallets();
            List<Wallet> otherWallets = allWallets.stream()
                .filter(w -> w.getId() != wallet.getId())
                .toList();
            cmbTransferTargetWallet.setItems(FXCollections.observableArrayList(otherWallets));
            
            // Custom cell factory to display wallet info
            cmbTransferTargetWallet.setCellFactory(param -> new javafx.scene.control.ListCell<Wallet>() {
                @Override
                protected void updateItem(Wallet item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(String.format("#%s - %s (%.2f tCO₂e)", 
                            item.getWalletNumber(), 
                            item.getName() != null ? item.getName() : "Unnamed",
                            item.getAvailableCredits()));
                    }
                }
            });
            
            cmbTransferTargetWallet.setButtonCell(new javafx.scene.control.ListCell<Wallet>() {
                @Override
                protected void updateItem(Wallet item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText("Select recipient wallet...");
                    } else {
                        setText(String.format("#%s - %s", item.getWalletNumber(), 
                            item.getName() != null ? item.getName() : "Unnamed"));
                    }
                }
            });
        }
        
        if (txtTransferAmount != null) {
            txtTransferAmount.clear();
        }
        
        if (txtTransferReference != null) {
            txtTransferReference.clear();
            txtTransferReference.setPromptText("e.g., Project collaboration, Asset transfer");
        }
        
        if (txtTransferWalletNumber != null) {
            txtTransferWalletNumber.clear();
            txtTransferWalletNumber.setPromptText("Or enter wallet number manually");
        }
    }
    
    /**
     * Execute transfer operation.
     * Validate recipient exists, call WalletService.transferCredits(), post CreditsTransferredEvent
     */
    public void executeTransfer() {
        System.out.println("[OperationPanel] Executing transfer operation...");
        
        if (currentWallet == null) {
            showError("No wallet selected");
            return;
        }
        
        // Validate amount
        double amount;
        try {
            amount = Double.parseDouble(txtTransferAmount.getText());
            if (amount <= 0) {
                showError("Amount must be greater than zero");
                return;
            }
            if (amount > currentWallet.getAvailableCredits()) {
                showError(String.format("Insufficient credits. Available: %.2f tCO₂e", 
                    currentWallet.getAvailableCredits()));
                return;
            }
        } catch (NumberFormatException e) {
            showError("Please enter a valid amount");
            return;
        }
        
        // Get target wallet
        Wallet targetWallet = null;
        
        // Try ComboBox selection first
        if (cmbTransferTargetWallet != null && cmbTransferTargetWallet.getValue() != null) {
            targetWallet = cmbTransferTargetWallet.getValue();
        } 
        // Try manual wallet number entry
        else if (txtTransferWalletNumber != null && !txtTransferWalletNumber.getText().isEmpty()) {
            try {
                String walletNumber = txtTransferWalletNumber.getText();
                targetWallet = walletService.getWalletByNumber(walletNumber);
                if (targetWallet == null) {
                    showError("Wallet #" + walletNumber + " not found");
                    return;
                }
            } catch (NumberFormatException e) {
                showError("Please enter a valid wallet number");
                return;
            }
        } else {
            showError("Please select or enter a recipient wallet");
            return;
        }
        
        // Prevent self-transfer
        if (targetWallet.getId() == currentWallet.getId()) {
            showError("Cannot transfer to the same wallet");
            return;
        }
        
        // Get reference note
        String reference = (txtTransferReference != null && !txtTransferReference.getText().isEmpty())
            ? txtTransferReference.getText()
            : "Credit transfer";
        
        // Execute via service
        boolean success = walletService.transferCredits(
            currentWallet.getId(), 
            targetWallet.getId(), 
            amount, 
            reference
        );
        
        if (success) {
            showSuccess(String.format("Successfully transferred %.2f tCO₂e to wallet #%s", 
                amount, targetWallet.getWalletNumber()));
            
            // Post events
            EventBusManager.post(new WalletUpdatedEvent(currentWallet.getId(), 
                "CREDITS_TRANSFERRED_OUT", amount));
            EventBusManager.post(new WalletUpdatedEvent(targetWallet.getId(), 
                "CREDITS_TRANSFERRED_IN", amount));
            EventBusManager.post(new CreditsTransferredEvent(
                currentWallet.getId(), 
                targetWallet.getId(), 
                amount, 
                reference
            ));
            
            // Clear form
            clearTransferForm();
        } else {
            showError("Failed to transfer credits. Please check the wallets and try again.");
        }
    }
    
    /**
     * Clear transfer form
     */
    private void clearTransferForm() {
        if (txtTransferAmount != null) txtTransferAmount.clear();
        if (txtTransferReference != null) txtTransferReference.clear();
        if (txtTransferWalletNumber != null) txtTransferWalletNumber.clear();
        if (cmbTransferTargetWallet != null) cmbTransferTargetWallet.getSelectionModel().clearSelection();
    }
    
    // ============================================================================
    // EVENT HANDLERS
    // ============================================================================
    
    @Subscribe
    public void onBatchIssued(BatchIssuedEvent event) {
        Platform.runLater(() -> {
            System.out.println("[OperationPanel] Issue operation succeeded");
            // Refresh current wallet data if needed
            if (currentWallet != null && currentWallet.getId() == event.walletId) {
                Wallet updated = walletService.getWalletById(currentWallet.getId());
                if (updated != null) {
                    currentWallet = updated;
                }
            }
        });
    }
    
    @Subscribe
    public void onCreditsRetired(CreditsRetiredEvent event) {
        Platform.runLater(() -> {
            System.out.println("[OperationPanel] Retire operation succeeded");
            // Refresh current wallet data if needed
            if (currentWallet != null && currentWallet.getId() == event.walletId) {
                Wallet updated = walletService.getWalletById(currentWallet.getId());
                if (updated != null) {
                    currentWallet = updated;
                }
            }
        });
    }
    
    @Subscribe
    public void onCreditsTransferred(CreditsTransferredEvent event) {
        Platform.runLater(() -> {
            System.out.println("[OperationPanel] Transfer operation succeeded");
            // Refresh current wallet data if needed
            if (currentWallet != null && 
                (currentWallet.getId() == event.sourceWalletId || 
                 currentWallet.getId() == event.destinationWalletId)) {
                Wallet updated = walletService.getWalletById(currentWallet.getId());
                if (updated != null) {
                    currentWallet = updated;
                }
            }
        });
    }
    
    // ============================================================================
    // NOTIFICATION HELPERS
    // ============================================================================
    
    /**
     * Show success notification
     */
    private void showSuccess(String message) {
        Platform.runLater(() -> {
            EventBusManager.post(new NotificationEvent(
                NotificationEvent.Type.SUCCESS, 
                message
            ));
        });
    }
    
    /**
     * Show error notification
     */
    private void showError(String message) {
        Platform.runLater(() -> {
            EventBusManager.post(new NotificationEvent(
                NotificationEvent.Type.ERROR, 
                message
            ));
            
            // Also show alert for critical errors
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Operation Error");
            alert.setHeaderText("Operation Failed");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    /**
     * Show confirmation dialog
     */
    private boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        return alert.showAndWait()
            .filter(response -> response == javafx.scene.control.ButtonType.OK)
            .isPresent();
    }
    
    // ============================================================================
    // LIFECYCLE
    // ============================================================================
    
    public void shutdown() {
        EventBusManager.unregister(this);
    }
}
