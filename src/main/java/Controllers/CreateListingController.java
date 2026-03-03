package Controllers;

import Models.*;
import Services.*;
import Utils.SessionManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

/**
 * Controller for creating marketplace listings
 * Supports selling carbon credits (with batch tracking) or entire wallets
 */
public class CreateListingController {
    private static final String LOG_TAG = "[CreateListingController]";
    
    // Services
    private final WalletService walletService = new WalletService();
    private final MarketplaceListingService listingService = MarketplaceListingService.getInstance();
    private final CarbonPricingService pricingService = CarbonPricingService.getInstance();
    
    // FXML Components
    @FXML private RadioButton sellCreditsRadio;
    @FXML private RadioButton sellWalletRadio;
    @FXML private ToggleGroup assetTypeGroup;
    
    @FXML private ComboBox<WalletDisplay> walletComboBox;
    @FXML private Label availableCreditsLabel;
    @FXML private Label marketPriceLabel;
    
    @FXML private TableView<CarbonCreditBatch> batchesTable;
    @FXML private TableColumn<CarbonCreditBatch, String> batchIdColumn;
    @FXML private TableColumn<CarbonCreditBatch, String> batchProjectColumn;
    @FXML private TableColumn<CarbonCreditBatch, String> batchAmountColumn;
    @FXML private TableColumn<CarbonCreditBatch, String> batchStatusColumn;
    
    @FXML private TextField quantityField;
    @FXML private TextField priceField;
    @FXML private TextField minPriceField;
    @FXML private TextField autoAcceptPriceField;
    @FXML private TextArea descriptionArea;
    
    @FXML private Button createButton;
    @FXML private Button cancelButton;
    
    @FXML private Label quantityLabel;
    @FXML private VBox batchesContainer;
    
    // Data
    private ObservableList<WalletDisplay> userWallets = FXCollections.observableArrayList();
    private ObservableList<CarbonCreditBatch> batches = FXCollections.observableArrayList();
    private int currentUserId;
    
    @FXML
    public void initialize() {
        currentUserId = getCurrentUserId();
        
        // Setup radio buttons
        assetTypeGroup = new ToggleGroup();
        sellCreditsRadio.setToggleGroup(assetTypeGroup);
        sellWalletRadio.setToggleGroup(assetTypeGroup);
        sellCreditsRadio.setSelected(true);
        
        // Setup listeners
        assetTypeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> handleAssetTypeChange());
        walletComboBox.setOnAction(e -> handleWalletSelection());
        
        // Setup batches table
        setupBatchesTable();
        
        // Load user's wallets
        loadUserWallets();
        
        // Load current market price
        loadMarketPrice();
        
        // Initial UI state
        handleAssetTypeChange();
    }
    
    /**
     * Setup batches table columns
     */
    private void setupBatchesTable() {
        batchIdColumn.setCellValueFactory(data -> 
            new SimpleStringProperty("Batch #" + data.getValue().getId()));
        
        batchProjectColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getProjectName() != null ? 
                data.getValue().getProjectName() : "N/A"));
        
        batchAmountColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(String.format("%.2f tCO2", data.getValue().getRemainingAmount())));
        
        batchStatusColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getVerificationStatus()));
        
        batchesTable.setItems(batches);
    }
    
    /**
     * Load user's wallets
     */
    private void loadUserWallets() {
        new Thread(() -> {
            try {
                List<Wallet> wallets = walletService.getWalletsByOwnerId(currentUserId);
                
                Platform.runLater(() -> {
                    userWallets.clear();
                    for (Wallet wallet : wallets) {
                        userWallets.add(new WalletDisplay(wallet));
                    }
                    walletComboBox.setItems(userWallets);
                    
                    if (!userWallets.isEmpty()) {
                        walletComboBox.getSelectionModel().selectFirst();
                        handleWalletSelection();
                    }
                });
            } catch (Exception e) {
                System.err.println(LOG_TAG + " Error loading wallets: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Load current market reference price
     */
    private void loadMarketPrice() {
        new Thread(() -> {
            try {
                double marketPrice = pricingService.getCurrentPrice("VOLUNTARY_CARBON_MARKET");
                Platform.runLater(() -> 
                    marketPriceLabel.setText(String.format("Market Reference: $%.2f/tCO2", marketPrice))
                );
            } catch (Exception e) {
                System.err.println(LOG_TAG + " Error loading market price: " + e.getMessage());
                Platform.runLater(() -> 
                    marketPriceLabel.setText("Market Reference: $15.50/tCO2 (default)")
                );
            }
        }).start();
    }
    
    /**
     * Handle asset type change (credits vs wallet)
     */
    private void handleAssetTypeChange() {
        boolean isCredits = sellCreditsRadio.isSelected();
        
        // Show/hide relevant fields
        quantityLabel.setText(isCredits ? "Quantity (tCO2):" : "Total Price (USD):");
        quantityField.setPromptText(isCredits ? "e.g., 100" : "Auto-calculated");
        batchesContainer.setVisible(isCredits);
        batchesContainer.setManaged(isCredits);
        
        if (!isCredits) {
            // Selling entire wallet - calculate total value
            WalletDisplay selected = walletComboBox.getValue();
            if (selected != null) {
                double totalCredits = selected.wallet.getAvailableCredits();
                double marketPrice = extractMarketPrice();
                double suggestedPrice = totalCredits * marketPrice;
                priceField.setText(String.format("%.2f", suggestedPrice));
                if (minPriceField.getText().trim().isEmpty()) {
                    minPriceField.setText(String.format("%.2f", suggestedPrice * 0.90));
                }
            }
        }
        
        handleWalletSelection();
    }
    
    /**
     * Handle wallet selection
     */
    private void handleWalletSelection() {
        WalletDisplay selected = walletComboBox.getValue();
        if (selected == null) return;
        
        Wallet wallet = selected.wallet;
        availableCreditsLabel.setText(String.format("Available: %.2f tCO2", wallet.getAvailableCredits()));
        
        if (sellCreditsRadio.isSelected()) {
            // Load batches for this wallet
            loadWalletBatches(wallet.getId());
        }
    }
    
    /**
     * Load batches for selected wallet
     */
    private void loadWalletBatches(int walletId) {
        new Thread(() -> {
            try {
                List<CarbonCreditBatch> walletBatches = walletService.getAvailableBatchesByWalletId(walletId);
                Platform.runLater(() -> {
                    batches.clear();
                    batches.addAll(walletBatches);
                });
            } catch (Exception e) {
                System.err.println(LOG_TAG + " Error loading batches: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Handle create listing
     */
    @FXML
    private void handleCreate() {
        if (!validateInput()) {
            return;
        }
        
        new Thread(() -> {
            try {
                WalletDisplay selectedWallet = walletComboBox.getValue();
                boolean isCredits = sellCreditsRadio.isSelected();
                
                String assetType = isCredits ? "CARBON_CREDITS" : "WALLET";
                double quantity;
                double price;
                
                double minPrice = Double.parseDouble(minPriceField.getText().trim());
                Double autoAccept = parseOptionalPrice(autoAcceptPriceField.getText().trim());

                if (isCredits) {
                    quantity = Double.parseDouble(quantityField.getText().trim());
                    price = Double.parseDouble(priceField.getText().trim());
                    
                    // Validate against available credits
                    if (quantity > selectedWallet.wallet.getAvailableCredits()) {
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, 
                            "Insufficient Credits", 
                            "You only have " + selectedWallet.wallet.getAvailableCredits() + " tCO2 available."));
                        return;
                    }
                } else {
                    // Selling entire wallet
                    quantity = 1.0;
                    price = Double.parseDouble(priceField.getText().trim());
                }
                
                String description = descriptionArea.getText().trim();
                
                int listingId = listingService.createListing(
                    currentUserId,
                    assetType,
                    selectedWallet.wallet.getId(),
                    quantity,
                    price,
                    minPrice,
                    autoAccept,
                    description
                );
                
                if (listingId > 0) {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.INFORMATION, "Success", 
                            "Listing created successfully! ID: " + listingId);
                        closeWindow();
                    });
                } else {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, 
                        "Error", "Failed to create listing. Please try again."));
                }
                
            } catch (Exception e) {
                System.err.println(LOG_TAG + " Error creating listing: " + e.getMessage());
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, 
                    "Error", "Failed to create listing: " + e.getMessage()));
            }
        }).start();
    }
    
    /**
     * Validate input fields
     */
    private boolean validateInput() {
        if (walletComboBox.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please select a wallet.");
            return false;
        }
        
        if (sellCreditsRadio.isSelected()) {
            String quantityText = quantityField.getText().trim();
            if (quantityText.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation Error", "Please enter quantity.");
                return false;
            }
            
            try {
                double quantity = Double.parseDouble(quantityText);
                if (quantity <= 0) {
                    showAlert(Alert.AlertType.WARNING, "Validation Error", "Quantity must be greater than 0.");
                    return false;
                }
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.WARNING, "Validation Error", "Invalid quantity format.");
                return false;
            }
        }
        
        String priceText = priceField.getText().trim();
        String minPriceText = minPriceField.getText().trim();
        String autoAcceptText = autoAcceptPriceField.getText().trim();

        if (priceText.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please enter price.");
            return false;
        }
        if (minPriceText.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please enter minimum price.");
            return false;
        }
        
        try {
            double price = Double.parseDouble(priceText);
            double minPrice = Double.parseDouble(minPriceText);
            Double autoAccept = parseOptionalPrice(autoAcceptText);

            if (price <= 0 || minPrice <= 0) {
                showAlert(Alert.AlertType.WARNING, "Validation Error", "Price values must be greater than 0.");
                return false;
            }
            if (minPrice > price) {
                showAlert(Alert.AlertType.WARNING, "Validation Error", "Minimum price cannot exceed your asking price.");
                return false;
            }
            if (autoAccept != null && autoAccept < price) {
                showAlert(Alert.AlertType.WARNING, "Validation Error", "Auto-accept must be at or above your asking price.");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Invalid price format.");
            return false;
        }
        
        return true;
    }
    
    /**
     * Handle cancel
     */
    @FXML
    private void handleCancel() {
        closeWindow();
    }
    
    /**
     * Close the window
     */
    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
    
    /**
     * Get current user ID
     */
    private int getCurrentUserId() {
        User user = SessionManager.getInstance().getCurrentUser();
        return user != null ? Math.toIntExact(user.getId()) : -1;
    }
    
    /**
     * Extract market price from label
     */
    private double extractMarketPrice() {
        String text = marketPriceLabel.getText();
        try {
            String priceStr = text.replaceAll("[^0-9.]", "");
            return Double.parseDouble(priceStr);
        } catch (Exception e) {
            return 15.50; // Default fallback
        }
    }

    private Double parseOptionalPrice(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return Double.parseDouble(text.trim());
    }

    /**
     * Show alert dialog
     */
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    /**
     * Wrapper class for displaying wallets in ComboBox
     */
    private static class WalletDisplay {
        private final Wallet wallet;
        
        public WalletDisplay(Wallet wallet) {
            this.wallet = wallet;
        }
        
        @Override
        public String toString() {
            return String.format("Wallet #%s (%.2f tCO2)", 
                wallet.getWalletNumber(), wallet.getAvailableCredits());
        }
    }
}
