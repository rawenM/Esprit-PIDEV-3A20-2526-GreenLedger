package Controllers;

import Models.*;
import Services.*;
import Utils.SessionManager;
import com.stripe.model.PaymentIntent;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.application.Platform;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller for the marketplace browse and purchase interface
 */
public class MarketplaceController extends BaseController {
    private static final String LOG_TAG = "[MarketplaceController]";

    @FXML private TabPane marketplaceTabPane;
    @FXML private Tab browseListingsTab;
    @FXML private Tab myListingsTab;
    @FXML private Tab orderHistoryTab;

    // Browse Listings Tab
    @FXML private TableView<MarketplaceListing> listingsTable;
    @FXML private TableColumn<MarketplaceListing, Integer> listingIdColumn;
    @FXML private TableColumn<MarketplaceListing, String> assetTypeColumn;
    @FXML private TableColumn<MarketplaceListing, Double> quantityColumn;
    @FXML private TableColumn<MarketplaceListing, Double> pricePerUnitColumn;
    @FXML private TableColumn<MarketplaceListing, Double> totalPriceColumn;

    @FXML private ComboBox<String> assetTypeFilter;
    @FXML private Slider priceMinSlider;
    @FXML private Slider priceMaxSlider;
    @FXML private Label priceRangeLabel;
    @FXML private Button backButton;
    @FXML private Button refreshButton;
    @FXML private Button refreshPriceButton;
    @FXML private Button testPaymentButton;
    @FXML private Button buyButton;

    // My Listings Tab
    @FXML private TableView<MarketplaceListing> myListingsTable;
    @FXML private TableColumn<MarketplaceListing, String> myListingAssetColumn;
    @FXML private TableColumn<MarketplaceListing, Double> myListingQuantityColumn;
    @FXML private TableColumn<MarketplaceListing, Double> myListingPriceColumn;
    @FXML private TableColumn<MarketplaceListing, String> myListingStatusColumn;

    @FXML private Button createListingButton;
    @FXML private Button editListingButton;
    @FXML private Button deleteListingButton;

    // Order History Tab
    @FXML private TableView<MarketplaceOrder> ordersTable;
    @FXML private TableColumn<MarketplaceOrder, Integer> orderIdColumn;
    @FXML private TableColumn<MarketplaceOrder, Double> orderAmountColumn;
    @FXML private TableColumn<MarketplaceOrder, String> orderStatusColumn;
    @FXML private TableColumn<MarketplaceOrder, Long> orderDateColumn;

    @FXML private Label totalSpendingLabel;
    @FXML private Label completedOrdersLabel;

    // Offers Tab
    @FXML private TableView<MarketplaceOffer> offersReceivedTable;
    @FXML private TableColumn<MarketplaceOffer, Integer> offerListingColumn;
    @FXML private TableColumn<MarketplaceOffer, Long> offerBuyerColumn;
    @FXML private TableColumn<MarketplaceOffer, Double> offerQuantityColumn;
    @FXML private TableColumn<MarketplaceOffer, Double> offerPriceColumn;
    @FXML private TableColumn<MarketplaceOffer, String> offerStatusColumn;

    @FXML private TableView<MarketplaceOffer> offersSentTable;
    @FXML private TableColumn<MarketplaceOffer, Integer> sentListingColumn;
    @FXML private TableColumn<MarketplaceOffer, Long> sentSellerColumn;
    @FXML private TableColumn<MarketplaceOffer, Double> sentQuantityColumn;
    @FXML private TableColumn<MarketplaceOffer, Double> sentPriceColumn;
    @FXML private TableColumn<MarketplaceOffer, String> sentStatusColumn;

    @FXML private Button acceptOfferButton;
    @FXML private Button counterOfferButton;
    @FXML private Button rejectOfferButton;
    @FXML private Button cancelOfferButton;
    @FXML private Button payNowButton;

    // Price Display
    @FXML private Label currentCarbonPriceLabel;
    @FXML private Label marketPanelPriceLabel;
    @FXML private Label marketPanelNoteLabel;
    @FXML private LineChart<Number, Number> marketMiniChart;
    @FXML private LineChart<Number, Number> marketChart;

    // Services
    private final MarketplaceListingService listingService = MarketplaceListingService.getInstance();
    private final MarketplaceOrderService orderService = MarketplaceOrderService.getInstance();
    private final CarbonPricingService pricingService = CarbonPricingService.getInstance();
    private final UserMarketplaceKYCService kycService = UserMarketplaceKYCService.getInstance();
    private final MarketplaceOfferService offerService = MarketplaceOfferService.getInstance();
    private final StripePaymentService stripeService = StripePaymentService.getInstance();

    private ObservableList<MarketplaceListing> listingsData = FXCollections.observableArrayList();
    private ObservableList<MarketplaceListing> myListingsData = FXCollections.observableArrayList();
    private ObservableList<MarketplaceOrder> ordersData = FXCollections.observableArrayList();
    private ObservableList<MarketplaceOffer> offersReceivedData = FXCollections.observableArrayList();
    private ObservableList<MarketplaceOffer> offersSentData = FXCollections.observableArrayList();

    @Override
    public void initialize() {
        super.initialize();
        setupListingsTable();
        setupMyListingsTable();
        setupOrdersTable();
        setupOffersReceivedTable();
        setupOffersSentTable();
        setupFilters();
        loadListings();
        loadMyListings();
        loadOrderHistory();
        loadOffers();
        updateCarbonPrice();
        setupPriceChart();
        updatePriceLabel(); // Initialize price range label
    }

    /**
     * Setup listings browse table columns
     */
    private void setupListingsTable() {
        listingIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        assetTypeColumn.setCellValueFactory(new PropertyValueFactory<>("assetType"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantityOrTokens"));
        pricePerUnitColumn.setCellValueFactory(new PropertyValueFactory<>("pricePerUnit"));
        totalPriceColumn.setCellValueFactory(new PropertyValueFactory<>("totalPriceUsd"));

        // Add custom cell factory for listing details with batch info
        assetTypeColumn.setCellFactory(column -> new TableCell<MarketplaceListing, String>() {
            @Override
            protected void updateItem(String assetType, boolean empty) {
                super.updateItem(assetType, empty);
                if (empty || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    MarketplaceListing listing = getTableRow().getItem();
                    try {
                        // Get seller's batches for this listing
                        WalletService walletService = new WalletService();
                        List<CarbonCreditBatch> sellerBatches = walletService.getWalletBatches(
                            listing.getWalletId()
                        );
                        
                        String batchSerials = sellerBatches.isEmpty() ? "No batches" :
                            sellerBatches.stream()
                                .filter(b -> b.getStatus() != null && !b.getStatus().contains("RETIRED"))
                                .limit(3)
                                .map(b -> b.getSerialNumber() != null ? b.getSerialNumber() : "B" + b.getId())
                                .collect(Collectors.joining(", "));
                        
                        String displayText = assetType + "\n📦 Batches: " + batchSerials;
                        setText(displayText);
                        setStyle("-fx-text-alignment: left;");
                    } catch (Exception e) {
                        setText(assetType);
                    }
                }
            }
        });

        // Add transfer mode indicator to price column
        pricePerUnitColumn.setCellFactory(column -> new TableCell<MarketplaceListing, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    MarketplaceListing listing = getTableRow().getItem();
                    double totalPrice = listing.getTotalPriceUsd();
                    String transferMode = totalPrice >= 10000 ? "🔒 Escrow" : "⚡ Instant";
                    String displayText = String.format("$%.2f\n%s", price, transferMode);
                    setText(displayText);
                    setStyle("-fx-text-alignment: center;");
                }
            }
        });

        listingsTable.setItems(listingsData);
        listingsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    /**
     * Setup my listings table
     */
    private void setupMyListingsTable() {
        myListingAssetColumn.setCellValueFactory(new PropertyValueFactory<>("assetType"));
        myListingQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantityOrTokens"));
        myListingPriceColumn.setCellValueFactory(new PropertyValueFactory<>("pricePerUnit"));
        myListingStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        myListingsTable.setItems(myListingsData);
        myListingsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    /**
     * Setup order history table
     */
    private void setupOrdersTable() {
        orderIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        orderAmountColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmountUsd"));
        orderStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        orderDateColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleLongProperty(
                cellData.getValue().getCreatedAt().getTime()
            ).asObject()
        );

        ordersTable.setItems(ordersData);
        ordersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    /**
     * Setup offers received table (seller view)
     */
    private void setupOffersReceivedTable() {
        if (offersReceivedTable != null) {
            offerListingColumn.setCellValueFactory(new PropertyValueFactory<>("listingId"));
            offerBuyerColumn.setCellValueFactory(new PropertyValueFactory<>("buyerId"));
            offerQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
            offerPriceColumn.setCellValueFactory(new PropertyValueFactory<>("offerPriceUsd"));
            offerStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
            
            offersReceivedTable.setItems(offersReceivedData);
            offersReceivedTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        }
    }

    /**
     * Setup offers sent table (buyer view)
     */
    private void setupOffersSentTable() {
        if (offersSentTable != null) {
            sentListingColumn.setCellValueFactory(new PropertyValueFactory<>("listingId"));
            sentSellerColumn.setCellValueFactory(new PropertyValueFactory<>("sellerId"));
            sentQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
            sentPriceColumn.setCellValueFactory(new PropertyValueFactory<>("offerPriceUsd"));
            sentStatusColumn.setCellValueFactory(cellData -> {
                String status = cellData.getValue().getStatus();
                String displayStatus;
                if ("PAID_PENDING".equals(status)) {
                    displayStatus = "DONE (processing)";
                } else if ("COMPLETED".equals(status)) {
                    displayStatus = "DONE";
                } else {
                    displayStatus = status;
                }
                return new javafx.beans.property.SimpleStringProperty(displayStatus);
            });
            
            offersSentTable.setItems(offersSentData);
            offersSentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            
            // Initialize button as disabled
            if (payNowButton != null) {
                payNowButton.setDisable(true);
            }
            
            // Add selection listener to enable/disable Pay Now button
            offersSentTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                updatePayNowButtonState();
            });
        }
    }
    
    /**
     * Update Pay Now button state based on selected offer
     */
    private void updatePayNowButtonState() {
        MarketplaceOffer selected = offersSentTable.getSelectionModel().getSelectedItem();
        if (selected != null && "ACCEPTED".equals(selected.getStatus())) {
            payNowButton.setDisable(false);
        } else {
            payNowButton.setDisable(true);
        }
    }

    /**
     * Setup filter controls
     */
    private void setupFilters() {
        assetTypeFilter.setItems(FXCollections.observableArrayList(
            "All Types", "CARBON_CREDITS", "WALLET"
        ));
        assetTypeFilter.setValue("All Types");

        // Set default values for price sliders
        priceMinSlider.setMin(0);
        priceMinSlider.setMax(1000);
        priceMinSlider.setValue(0);
        
        priceMaxSlider.setMin(0);
        priceMaxSlider.setMax(1000);
        priceMaxSlider.setValue(1000);

        // Add listeners to price sliders for validation and filtering
        priceMinSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            // Ensure min doesn't exceed max
            if (newVal.doubleValue() > priceMaxSlider.getValue()) {
                priceMinSlider.setValue(priceMaxSlider.getValue());
                return;
            }
            updatePriceLabel();
            loadListings();
        });
        
        priceMaxSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            // Ensure max doesn't go below min
            if (newVal.doubleValue() < priceMinSlider.getValue()) {
                priceMaxSlider.setValue(priceMinSlider.getValue());
                return;
            }
            updatePriceLabel();
            loadListings();
        });
        
        assetTypeFilter.valueProperty().addListener((obs, oldVal, newVal) -> loadListings());
    }

    /**
     * Update price range label
     */
    private void updatePriceLabel() {
        double min = priceMinSlider.getValue();
        double max = priceMaxSlider.getValue();
        priceRangeLabel.setText(String.format("$%.2f - $%.2f", min, max));
    }

    /**
     * Load all active listings from marketplace
     */
    @FXML
    private void loadListings() {
        new Thread(() -> {
            try {
                // Get all active listings
                List<MarketplaceListing> listings = listingService.getActiveListings();
                System.out.println(LOG_TAG + " ===== DEBUG: loadListings() START =====");
                System.out.println(LOG_TAG + " Total active listings from DB: " + listings.size());
                
                // Get filter values on JavaFX thread
                final String assetTypeValue = assetTypeFilter.getValue();
                final double minPrice = priceMinSlider.getValue();
                final double maxPrice = priceMaxSlider.getValue();
                
                System.out.println(LOG_TAG + " Filter - Asset Type: " + assetTypeValue);
                System.out.println(LOG_TAG + " Filter - Price Range: $" + minPrice + " - $" + maxPrice);
                
                // Debug: Print all listings before filtering
                for (MarketplaceListing listing : listings) {
                    System.out.println(LOG_TAG + "   Listing #" + listing.getId() + 
                        ": " + listing.getAssetType() + 
                        ", Price: $" + listing.getPricePerUnit() + 
                        ", Seller: " + listing.getSellerId());
                }
                
                // Apply asset type filter
                if (assetTypeValue != null && !"All Types".equals(assetTypeValue)) {
                    int beforeCount = listings.size();
                    listings.removeIf(l -> !assetTypeValue.equals(l.getAssetType()));
                    System.out.println(LOG_TAG + " Asset type filter: " + beforeCount + " -> " + listings.size());
                }
                
                // Apply price range filter
                int beforePriceFilter = listings.size();
                listings.removeIf(l -> {
                    double price = l.getPricePerUnit();
                    boolean filtered = price < minPrice || price > maxPrice;
                    if (filtered) {
                        System.out.println(LOG_TAG + " FILTERED OUT Listing #" + l.getId() + 
                            " - Price $" + price + " outside range");
                    }
                    return filtered;
                });
                System.out.println(LOG_TAG + " Price filter: " + beforePriceFilter + " -> " + listings.size());
                
                final int finalCount = listings.size();
                javafx.application.Platform.runLater(() -> {
                    listingsData.clear();
                    listingsData.addAll(listings);
                    System.out.println(LOG_TAG + " ===== Loaded " + finalCount + " listings to table =====");
                });
            } catch (Exception e) {
                System.err.println(LOG_TAG + " ERROR loading listings: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Load user's own listings
     */
    @FXML
    private void loadMyListings() {
        new Thread(() -> {
            try {
                int userId = getCurrentUserId();
                List<MarketplaceListing> myListings = listingService.getSellerListings(userId);

                javafx.application.Platform.runLater(() -> {
                    myListingsData.clear();
                    myListingsData.addAll(myListings);
                });
            } catch (Exception e) {
                System.err.println(LOG_TAG + " ERROR loading my listings: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Load order history
     */
    @FXML
    private void loadOrderHistory() {
        new Thread(() -> {
            try {
                int userId = getCurrentUserId();
                List<MarketplaceOrder> orders = orderService.getOrderHistory(userId);

                double totalSpending = orders.stream()
                    .filter(o -> "COMPLETED".equals(o.getStatus()))
                    .mapToDouble(MarketplaceOrder::getTotalAmountUsd)
                    .sum();

                long completedCount = orders.stream()
                    .filter(o -> "COMPLETED".equals(o.getStatus()))
                    .count();

                javafx.application.Platform.runLater(() -> {
                    ordersData.clear();
                    ordersData.addAll(orders);
                    totalSpendingLabel.setText(String.format("Total Spending: $%.2f", totalSpending));
                    completedOrdersLabel.setText("Completed Orders: " + completedCount);
                });
            } catch (Exception e) {
                System.err.println(LOG_TAG + " ERROR loading order history: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Load offers (both received and sent)
     */
    @FXML
    private void loadOffers() {
        new Thread(() -> {
            try {
                long userId = getCurrentUserId();
                
                // Load offers received (as seller)
                List<MarketplaceOffer> received = offerService.getOffersReceived(userId);
                
                // Load offers sent (as buyer)
                List<MarketplaceOffer> sent = offerService.getOffersSent(userId);

                // Keep only actionable offers in Received tab
                List<MarketplaceOffer> actionableReceived = received.stream()
                    .filter(o -> "PENDING".equals(o.getStatus()) || "COUNTERED".equals(o.getStatus()))
                    .collect(Collectors.toList());

                // Hide fully completed/cancelled offers from Sent actionable list
                List<MarketplaceOffer> actionableSent = sent.stream()
                    .filter(o -> !"COMPLETED".equals(o.getStatus()) && !"CANCELLED".equals(o.getStatus()))
                    .collect(Collectors.toList());

                javafx.application.Platform.runLater(() -> {
                    offersReceivedData.clear();
                    offersReceivedData.addAll(actionableReceived);
                    
                    offersSentData.clear();
                    offersSentData.addAll(actionableSent);
                    
                    updatePayNowButtonState();  // Update button state after loading offers
                    
                    System.out.println(LOG_TAG + " Loaded offers -> received actionable: " + actionableReceived.size() +
                                      ", sent actionable: " + actionableSent.size());
                });
            } catch (Exception e) {
                System.err.println(LOG_TAG + " ERROR loading offers: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Update current carbon price display
     */
    private void updateCarbonPrice() {
        new Thread(() -> {
            try {
                double price = pricingService.getCurrentPrice("VOLUNTARY_CARBON_MARKET");
                javafx.application.Platform.runLater(() -> {
                    currentCarbonPriceLabel.setText(String.format("Current Carbon Price: $%.2f/tCO2e", price));
                    setupPriceChart();  // Refresh chart when price updates
                });
            } catch (Exception e) {
                System.err.println(LOG_TAG + " ERROR updating price: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Setup price history chart with 30-day data
     */
    private void setupPriceChart() {
        new Thread(() -> {
            try {
                // Get 30 days of price history
                List<CarbonPriceSnapshot> priceHistory = pricingService.getPriceHistory("VOLUNTARY_CARBON_MARKET", 30);
                
                javafx.application.Platform.runLater(() -> {
                    // Clear existing data
                    if (marketMiniChart != null) {
                        marketMiniChart.getData().clear();
                    }
                    if (marketChart != null) {
                        marketChart.getData().clear();
                    }
                    
                    // Create series for the charts
                    XYChart.Series<Number, Number> miniSeries = new XYChart.Series<>();
                    miniSeries.setName("Carbon Price (USD/tCO2e)");
                    
                    XYChart.Series<Number, Number> mainSeries = new XYChart.Series<>();
                    mainSeries.setName("Carbon Price (USD/tCO2e)");
                    
                    // Add data points to series
                    for (int i = 0; i < priceHistory.size(); i++) {
                        CarbonPriceSnapshot snapshot = priceHistory.get(i);
                        miniSeries.getData().add(new XYChart.Data<>(i, snapshot.getUsdPerTon()));
                        mainSeries.getData().add(new XYChart.Data<>(i, snapshot.getUsdPerTon()));
                    }
                    
                    // Add series to charts
                    if (marketMiniChart != null) {
                        marketMiniChart.getData().add(miniSeries);
                    }
                    if (marketChart != null) {
                        marketChart.getData().add(mainSeries);
                    }
                    
                    // Set chart title and display current price
                    if (!priceHistory.isEmpty()) {
                        CarbonPriceSnapshot latest = priceHistory.get(priceHistory.size() - 1);
                        if (marketPanelPriceLabel != null) {
                            marketPanelPriceLabel.setText(String.format("$%.2f/tCO2e", latest.getUsdPerTon()));
                        }
                        if (marketPanelNoteLabel != null) {
                            marketPanelNoteLabel.setText("30-day price history with daily updates");
                        }
                    }
                });
            } catch (Exception e) {
                System.err.println(LOG_TAG + " ERROR setting up price chart: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Handle buy button click with Stripe payment integration
     */
    @FXML
    private void handleBuyClick() {
        MarketplaceListing selected = listingsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Please select a listing to purchase");
            return;
        }

        // Show quantity dialog
        TextInputDialog dialog = new TextInputDialog("1");
        dialog.setTitle("Purchase Quantity");
        dialog.setHeaderText("Enter quantity of " + selected.getAssetDescription());
        dialog.setContentText("Quantity:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                double quantity = Double.parseDouble(result.get());
                if (quantity <= 0 || quantity > selected.getQuantityOrTokens()) {
                    showAlert("Invalid quantity");
                    return;
                }

                // Check user limits
                int currentUserId = getCurrentUserId();
                double maxLimit = kycService.getMaxTransactionLimit(currentUserId);
                if (quantity > maxLimit) {
                    showAlert("Quantity exceeds your limit of " + maxLimit);
                    return;
                }

                double pricePerUnit = selected.getPricePerUnit();
                double subtotal = quantity * pricePerUnit;
                double platformFee = subtotal * 0.029 + 0.30;
                double totalAmount = subtotal + platformFee;
                
                // Confirm purchase with breakdown
                String confirmMessage = String.format(
                    "Payment Confirmation\n\n" +
                    "Quantity: %.2f %s\n" +
                    "Price per unit: $%.2f\n" +
                    "Subtotal: $%.2f\n" +
                    "Platform fee (2.9%% + $0.30): $%.2f\n" +
                    "Total Amount: $%.2f\n\n" +
                    "Proceed with Stripe Checkout?",
                    quantity,
                    selected.getAssetType(),
                    pricePerUnit,
                    subtotal,
                    platformFee,
                    totalAmount
                );

                if (!confirmAction(confirmMessage)) {
                    return;
                }

                // Disable buy button during processing
                if (buyButton != null) {
                    buyButton.setDisable(true);
                }

                // Process payment asynchronously
                new Thread(() -> {
                    try {
                        // Create order first
                        int orderId = orderService.placeOrder(selected.getId(), currentUserId, quantity);
                        if (orderId <= 0) {
                            Platform.runLater(() -> {
                                showAlert("Error creating order");
                                if (buyButton != null) buyButton.setDisable(false);
                            });
                            return;
                        }

                        // Create Stripe Checkout session
                        String checkoutUrl = stripeService.createHostedCheckoutUrl(
                            orderId,
                            totalAmount,
                            currentUserId,
                            selected.getSellerId(),
                            quantity,
                            pricePerUnit
                        );

                        if (checkoutUrl == null || checkoutUrl.isEmpty()) {
                            Platform.runLater(() -> {
                                showAlert("Error creating Stripe checkout session.\nOrder #" + orderId + " created but not paid.");
                                if (buyButton != null) buyButton.setDisable(false);
                            });
                            return;
                        }

                        Platform.runLater(() -> {
                            // Open Stripe Checkout in popup
                            openStripeCheckoutPopup(checkoutUrl, orderId);
                            
                            String testModeHint = stripeService.isTestMode()
                                ? "\n\nTEST MODE: Use card 4242 4242 4242 4242, any future date, any CVC."
                                : "";

                            showAlert("✅ Stripe Checkout opened!\n\n" +
                                "Order ID: #" + orderId + "\n" +
                                "Total Amount: $" + String.format("%.2f", totalAmount) + "\n\n" +
                                "Complete payment on the Stripe hosted page." + testModeHint);
                        });

                    } catch (Exception e) {
                        System.err.println(LOG_TAG + " ERROR in buy flow: " + e.getMessage());
                        Platform.runLater(() -> {
                            showAlert("Error processing purchase: " + e.getMessage());
                            if (buyButton != null) buyButton.setDisable(false);
                        });
                        e.printStackTrace();
                    }
                }).start();
                
            } catch (NumberFormatException e) {
                showAlert("Invalid quantity");
            } catch (Exception e) {
                System.err.println(LOG_TAG + " ERROR in buy flow: " + e.getMessage());
                showAlert("Error processing purchase: " + e.getMessage());
            }
        }
    }

    /**
     * Open Stripe Checkout in a popup WebView window
     */
    private void openStripeCheckoutPopup(String checkoutUrl, int orderId) {
        try {
            WebView webView = new WebView();
            webView.getEngine().locationProperty().addListener((obs, oldUrl, newUrl) -> {
                if (newUrl == null) {
                    return;
                }

                // Check for successful payment
                if (newUrl.contains("/payment/success") && newUrl.contains("session_id=")) {
                    String sessionId = extractSessionId(newUrl);
                    if (sessionId == null || sessionId.isEmpty()) {
                        return;
                    }

                    new Thread(() -> {
                        try {
                            String paymentIntentId = stripeService.getPaidCheckoutPaymentIntent(sessionId);
                            if (paymentIntentId != null && !paymentIntentId.isEmpty()) {
                                boolean completed = orderService.completeOrder(orderId, paymentIntentId);
                                if (completed) {
                                    Platform.runLater(() -> {
                                        Stage stage = (Stage) webView.getScene().getWindow();
                                        stage.close();
                                        showAlert("✅ Payment successful!\n\n" +
                                                 "Order #" + orderId + " completed.\n" +
                                                 "Payment ID: " + paymentIntentId + "\n\n" +
                                                 "Carbon credits transferred to your wallet!");
                                        loadOrderHistory();
                                        loadListings();
                                        loadMyListings();
                                        if (buyButton != null) buyButton.setDisable(false);
                                    });
                                } else {
                                    Platform.runLater(() -> {
                                        showAlert("Payment confirmed, but order finalization failed.\nPlease contact support with order #" + orderId);
                                        if (buyButton != null) buyButton.setDisable(false);
                                    });
                                }
                            } else {
                                Platform.runLater(() -> {
                                    showAlert("Payment not confirmed yet. Please complete checkout first.");
                                    if (buyButton != null) buyButton.setDisable(false);
                                });
                            }
                        } catch (Exception e) {
                            Platform.runLater(() -> {
                                showAlert("Error confirming checkout: " + e.getMessage());
                                if (buyButton != null) buyButton.setDisable(false);
                            });
                        }
                    }).start();
                } else if (newUrl.contains("/payment/cancel")) {
                    // Payment cancelled
                    Stage stage = (Stage) webView.getScene().getWindow();
                    stage.close();
                    showAlert("Payment cancelled.\nOrder #" + orderId + " created but not paid.");
                    if (buyButton != null) buyButton.setDisable(false);
                }
            });

            webView.getEngine().load(checkoutUrl);

            BorderPane root = new BorderPane(webView);
            Scene scene = new Scene(root, 980, 760);

            Stage popup = new Stage();
            popup.setTitle("🔒 Secure Stripe Checkout");
            popup.setScene(scene);
            popup.initModality(Modality.APPLICATION_MODAL);

            if (buyButton != null && buyButton.getScene() != null) {
                popup.initOwner(buyButton.getScene().getWindow());
            }

            popup.setOnCloseRequest(e -> {
                if (buyButton != null) buyButton.setDisable(false);
            });

            popup.show();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Stripe Checkout URL");
            alert.setHeaderText("Unable to open in-app checkout popup. Copy this URL:");
            TextArea textArea = new TextArea(checkoutUrl);
            textArea.setWrapText(true);
            textArea.setPrefHeight(100);
            textArea.setEditable(false);
            alert.getDialogPane().setContent(textArea);
            alert.showAndWait();
            if (buyButton != null) buyButton.setDisable(false);
        }
    }

    /**
     * View batch provenance for a selected listing.
     * Shows the complete audit trail and batch lineage for carbon credits in the listing.
     */
    private void viewListingProvenance() {
        MarketplaceListing selected = listingsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Please select a listing to view provenance");
            return;
        }

        try {
            // Get seller's batches
            WalletService walletService = new WalletService();
            List<CarbonCreditBatch> sellerBatches = walletService.getWalletBatches(
                selected.getWalletId()
            );

            if (sellerBatches.isEmpty()) {
                showAlert("No batches found for this listing");
                return;
            }

            // Show provenance for the first batch
            CarbonCreditBatch firstBatch = sellerBatches.get(0);
            openBatchProvenanceViewer(firstBatch.getId(), selected);

        } catch (Exception e) {
            showAlert("Error viewing provenance: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Open batch lineage and provenance viewer in a new window.
     */
    private void openBatchProvenanceViewer(int batchId, MarketplaceListing listing) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/batchLineage.fxml"));
            VBox batchLineageView = loader.load();

            Stage window = new Stage();
            window.setTitle("🔍 Batch Provenance - Listing #" + listing.getId());
            window.setWidth(1400);
            window.setHeight(900);
            window.setScene(new Scene(batchLineageView));
            window.show();

            // Pre-load batch ID in the controller
            BatchLineageController controller = loader.getController();
            javafx.application.Platform.runLater(() -> {
                controller.setBatchId(batchId);
                controller.loadBatchDetails();
            });

        } catch (IOException e) {
            showAlert("Error opening provenance viewer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle create listing
     */
    @FXML
    private void handleCreateListing() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/create_listing.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Create Marketplace Listing");
            stage.setScene(new Scene(root));
            stage.setOnHidden(e -> loadMyListings());
            stage.show();
        } catch (IOException e) {
            System.err.println(LOG_TAG + " ERROR opening create listing dialog: " + e.getMessage());
        }
    }

    /**
     * Handle edit listing
     */
    @FXML
    private void handleEditListing() {
        MarketplaceListing selected = myListingsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Please select a listing to edit");
            return;
        }

        // Show edit dialog
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Edit Listing");
        dialog.setHeaderText("Modify listing details");
        dialog.setResizable(true);

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));

        // Price field
        Label priceLabel = new Label("Price Per Unit ($):");
        TextField priceField = new TextField(String.valueOf(selected.getPricePerUnit()));
        priceField.setPrefWidth(200);

        // Min price
        Label minPriceLabel = new Label("Minimum Price ($):");
        TextField minPriceField = new TextField();
        if (selected.getMinPriceUsd() != null) {
            minPriceField.setText(String.valueOf(selected.getMinPriceUsd()));
        }
        minPriceField.setPrefWidth(200);

        // Auto-accept price
        Label autoAcceptLabel = new Label("Auto-Accept Price ($):");
        TextField autoAcceptField = new TextField();
        if (selected.getAutoAcceptPriceUsd() != null) {
            autoAcceptField.setText(String.valueOf(selected.getAutoAcceptPriceUsd()));
        }
        autoAcceptField.setPrefWidth(200);

        // Description
        Label descLabel = new Label("Description:");
        TextArea descArea = new TextArea(selected.getDescription() != null ? selected.getDescription() : "");
        descArea.setWrapText(true);
        descArea.setPrefHeight(100);
        descArea.setPrefWidth(300);

        content.getChildren().addAll(
            priceLabel, priceField,
            minPriceLabel, minPriceField,
            autoAcceptLabel, autoAcceptField,
            descLabel, descArea
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(
            new ButtonType("Save", ButtonBar.ButtonData.OK_DONE),
            ButtonType.CANCEL
        );

        dialog.setResultConverter(buttonType -> {
            if (buttonType.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                try {
                    double newPrice = Double.parseDouble(priceField.getText());
                    if (newPrice <= 0) {
                        showAlert("Price must be greater than 0");
                        return null;
                    }

                    Double minPrice = minPriceField.getText().isEmpty() ? 
                        null : Double.parseDouble(minPriceField.getText());
                    
                    Double autoAcceptPrice = autoAcceptField.getText().isEmpty() ? 
                        null : Double.parseDouble(autoAcceptField.getText());

                    // Validate price constraints
                    if (minPrice != null && minPrice > newPrice) {
                        showAlert("Minimum price cannot exceed asking price");
                        return null;
                    }

                    if (autoAcceptPrice != null && autoAcceptPrice < (minPrice != null ? minPrice : 0)) {
                        showAlert("Auto-accept price must be at least the minimum price");
                        return null;
                    }

                    // Update listing
                    if (listingService.updateListingPrice(selected.getId(), newPrice, minPrice, autoAcceptPrice)) {
                        if (!descArea.getText().isEmpty() && !descArea.getText().equals(selected.getDescription())) {
                            listingService.updateListingDescription(selected.getId(), descArea.getText());
                        }
                        showAlert("✓ Listing updated successfully!");
                        loadMyListings();
                    } else {
                        showAlert("Error updating listing");
                    }
                } catch (NumberFormatException e) {
                    showAlert("Invalid price format");
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    /**
     * Handle delete listing
     */
    @FXML
    private void handleDeleteListing() {
        MarketplaceListing selected = myListingsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Please select a listing to delete");
            return;
        }

        if (confirmAction("Are you sure you want to delete this listing?")) {
            if (listingService.deactivateListing(selected.getId())) {
                showAlert("Listing deleted");
                loadMyListings();
            }
        }
    }

    /**
     * Get currently logged-in user ID
     */
    private int getCurrentUserId() {
        User user = SessionManager.getInstance().getCurrentUser();
        return user != null ? Math.toIntExact(user.getId()) : -1;
    }

    /**
     * Show alert dialog
     */
    private void showAlert(String message) {
        Runnable show = () -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Marketplace");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        };

        if (Platform.isFxApplicationThread()) {
            show.run();
        } else {
            Platform.runLater(show);
        }
    }

    /**
     * Show confirmation dialog
     */
    private boolean confirmAction(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Action");
        alert.setHeaderText(null);
        alert.setContentText(message);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /**
     * Handle make offer button click (negotiation)
     */
    @FXML
    private void handleMakeOffer() {
        MarketplaceListing selected = listingsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Please select a listing to make an offer");
            return;
        }

        // Show quantity dialog first
        TextInputDialog qtyDialog = new TextInputDialog(String.valueOf(selected.getQuantityOrTokens()));
        qtyDialog.setTitle("Make Offer - Quantity");
        qtyDialog.setHeaderText("How much do you want to buy?");
        qtyDialog.setContentText("Quantity:");

        Optional<String> qtyResult = qtyDialog.showAndWait();
        if (!qtyResult.isPresent()) return;

        double quantity;
        try {
            quantity = Double.parseDouble(qtyResult.get());
            if (quantity <= 0 || quantity > selected.getQuantityOrTokens()) {
                showAlert("Invalid quantity. Must be between 1 and " + selected.getQuantityOrTokens());
                return;
            }
        } catch (NumberFormatException e) {
            showAlert("Invalid quantity format");
            return;
        }

        // Show offer price dialog
        String minPriceInfo = "";
        if (selected.getMinPriceUsd() != null) {
            minPriceInfo = "\nMinimum price: $" + selected.getMinPriceUsd();
        }
        if (selected.getAutoAcceptPriceUsd() != null) {
            minPriceInfo += "\nAuto-accept price: $" + selected.getAutoAcceptPriceUsd();
        }

        TextInputDialog dialog = new TextInputDialog(String.valueOf(selected.getPricePerUnit()));
        dialog.setTitle("Make Offer - Price");
        dialog.setHeaderText("Asking price: $" + selected.getPricePerUnit() + minPriceInfo);
        dialog.setContentText("Your offer (price per unit):");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                double offerPrice = Double.parseDouble(result.get());
                
                // Validate offer is above minimum (if minimum price is set)
                if (selected.getMinPriceUsd() != null && offerPrice < selected.getMinPriceUsd()) {
                    showAlert("Offer must be at least $" + selected.getMinPriceUsd());
                    return;
                }

                long userId = getCurrentUserId();
                int offerId = offerService.createOffer(selected.getId(), userId, quantity, offerPrice);
                
                if (offerId > 0) {
                    MarketplaceOffer createdOffer = offerService.getOffersSent(userId).stream()
                        .filter(o -> o.getId() == offerId)
                        .findFirst()
                        .orElse(null);
                    
                    if (createdOffer != null && "ACCEPTED".equals(createdOffer.getStatus())) {
                        showAlert("✓ Offer auto-accepted!\nYour offer of $" + offerPrice + 
                                 " met the auto-accept threshold.\nOffer ID: " + offerId);
                    } else {
                        showAlert("✓ Offer submitted!\nOffer ID: " + offerId + 
                                 "\nQuantity: " + quantity + 
                                 "\nPrice: $" + offerPrice + " per unit\n\nThe seller will be notified.");
                    }
                    
                    loadOffers();
                } else {
                    showAlert("Error creating offer. Please try again.");
                }
                
            } catch (NumberFormatException e) {
                showAlert("Invalid price format");
            } catch (Exception e) {
                System.err.println(LOG_TAG + " ERROR creating offer: " + e.getMessage());
                showAlert("Error: " + e.getMessage());
            }
        }
    }

    /**
     * Handle opening the Market tab
     */
    @FXML
    private void handleOpenMarketTab() {
        // Switch to Market tab if it exists
        for (Tab tab : marketplaceTabPane.getTabs()) {
            if ("Market".equals(tab.getText())) {
                marketplaceTabPane.getSelectionModel().select(tab);
                return;
            }
        }
    }

    /**
     * Handle accept offer button
     */
    @FXML
    private void handleAcceptOffer() {
        MarketplaceOffer selected = offersReceivedTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Please select an offer to accept");
            return;
        }

        if (!"PENDING".equals(selected.getStatus()) && !"COUNTERED".equals(selected.getStatus())) {
            showAlert("Can only accept pending or countered offers");
            return;
        }

        double finalPrice = selected.getCounterPriceUsd() != null ? 
                           selected.getCounterPriceUsd() : selected.getOfferPriceUsd();
        
        if (confirmAction(String.format("Accept offer of $%.2f per unit for %.2f quantity?", 
                finalPrice, selected.getQuantity()))) {
            
            if (offerService.acceptOffer(selected.getId())) {
                showAlert("✓ Offer accepted!\nThe buyer will be notified to complete payment.");
                loadOffers();
                loadMyListings();
            } else {
                showAlert("Error accepting offer");
            }
        }
    }

    /**
     * Handle counter offer button
     */
    @FXML
    private void handleCounterOffer() {
        MarketplaceOffer selected = offersReceivedTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Please select an offer to counter");
            return;
        }

        if (!"PENDING".equals(selected.getStatus())) {
            showAlert("Can only counter pending offers");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(String.valueOf(selected.getOfferPriceUsd()));
        dialog.setTitle("Counter Offer");
        dialog.setHeaderText("Current offer: $" + selected.getOfferPriceUsd() + " per unit");
        dialog.setContentText("Your counter-offer:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                double counterPrice = Double.parseDouble(result.get());
                
                if (counterPrice <= 0) {
                    showAlert("Invalid price");
                    return;
                }

                if (offerService.counterOffer(selected.getId(), counterPrice)) {
                    showAlert("✓ Counter-offer sent!\nBuyer will be notified of your price: $" + counterPrice);
                    loadOffers();
                } else {
                    showAlert("Error sending counter-offer");
                }
                
            } catch (NumberFormatException e) {
                showAlert("Invalid price format");
            }
        }
    }

    /**
     * Handle reject offer button
     */
    @FXML
    private void handleRejectOffer() {
        MarketplaceOffer selected = offersReceivedTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Please select an offer to reject");
            return;
        }

        if (!"PENDING".equals(selected.getStatus()) && !"COUNTERED".equals(selected.getStatus())) {
            showAlert("Can only reject pending or countered offers");
            return;
        }

        if (confirmAction("Reject this offer?")) {
            if (offerService.rejectOffer(selected.getId())) {
                showAlert("Offer rejected");
                loadOffers();
            } else {
                showAlert("Error rejecting offer");
            }
        }
    }

    /**
     * Handle pay now button - process payment for accepted offer
     */
    @FXML
    public void handlePayNow(ActionEvent event) {
        MarketplaceOffer selected = offersSentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Please select an offer to pay for");
            return;
        }

        if (!"ACCEPTED".equals(selected.getStatus())) {
            showAlert("Can only pay for accepted offers");
            return;
        }

        int buyerId = getCurrentUserId();
        double finalPrice = selected.getCounterPriceUsd() != null
            ? selected.getCounterPriceUsd()
            : selected.getOfferPriceUsd();
        double totalAmount = finalPrice * selected.getQuantity();
        double platformFee = totalAmount * 0.029 + 0.30;
        double finalTotal = totalAmount + platformFee;

        String confirmMessage = String.format(
            "Payment Confirmation\n\n" +
            "Quantity: %.2f units\n" +
            "Price per unit: $%.2f\n" +
            "Subtotal: $%.2f\n" +
            "Platform fee (2.9%% + $0.30): $%.2f\n" +
            "Total Amount: $%.2f\n\n" +
            "Proceed with Stripe Checkout?",
            selected.getQuantity(),
            finalPrice,
            totalAmount,
            platformFee,
            finalTotal
        );

        if (!confirmAction(confirmMessage)) {
            return;
        }

        payNowButton.setDisable(true);

        new Thread(() -> {
            try {
                int orderId = orderService.createOrderFromOffer(
                    (int) selected.getListingId(),
                    buyerId,
                    (int) selected.getSellerId(),
                    selected.getQuantity(),
                    finalPrice
                );

                if (orderId <= 0) {
                    showAlert("Error creating order");
                    Platform.runLater(() -> payNowButton.setDisable(false));
                    return;
                }

                String checkoutUrl = stripeService.createHostedCheckoutUrl(
                    orderId,
                    finalTotal,
                    buyerId,
                    (int) selected.getSellerId(),
                    selected.getQuantity(),
                    finalPrice
                );

                if (checkoutUrl == null || checkoutUrl.isEmpty()) {
                    showAlert("Error creating Stripe checkout session");
                    Platform.runLater(() -> payNowButton.setDisable(false));
                    return;
                }

                Platform.runLater(() -> {
                    openPaymentLink(checkoutUrl, orderId, selected.getId());
                    String testModeHint = stripeService.isTestMode()
                        ? "\n\nTEST MODE: Use card 4242 4242 4242 4242, any future date, any CVC."
                        : "";

                    showAlert("✅ Stripe Checkout opened!\n\n" +
                        "Order ID: #" + orderId + "\n" +
                        "Total Amount: $" + String.format("%.2f", finalTotal) + "\n\n" +
                        "Complete payment on the Stripe hosted page." + testModeHint);
                });

            } catch (Exception e) {
                showAlert("Error processing payment: " + e.getMessage());
                Platform.runLater(() -> payNowButton.setDisable(false));
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Open payment link in an in-app popup window
     */
    private void openPaymentLink(String paymentLink, int orderId, int offerId) {
        try {
            WebView webView = new WebView();
            webView.getEngine().locationProperty().addListener((obs, oldUrl, newUrl) -> {
                if (newUrl == null) {
                    return;
                }

                if (newUrl.contains("/payment/success") && newUrl.contains("session_id=")) {
                    String sessionId = extractSessionId(newUrl);
                    if (sessionId == null || sessionId.isEmpty()) {
                        return;
                    }

                    new Thread(() -> {
                        try {
                            String paymentIntentId = stripeService.getPaidCheckoutPaymentIntent(sessionId);
                            if (paymentIntentId != null && !paymentIntentId.isEmpty()) {
                                boolean completed = orderService.completeOrder(orderId, paymentIntentId);
                                if (completed) {
                                    offerService.updateOfferStatus(offerId, "COMPLETED");
                                    Platform.runLater(() -> {
                                        Stage stage = (Stage) webView.getScene().getWindow();
                                        stage.close();
                                        showAlert("✅ Payment successful!\n\nOrder #" + orderId + " completed.");
                                        loadOffers();
                                        loadOrderHistory();
                                        loadListings();
                                        loadMyListings();
                                        payNowButton.setDisable(false);
                                    });
                                } else {
                                    Platform.runLater(() -> {
                                        showAlert("Payment confirmed, but order finalization failed. Please contact support with order #" + orderId);
                                        payNowButton.setDisable(false);
                                    });
                                }
                            } else {
                                Platform.runLater(() -> {
                                    showAlert("Payment not confirmed yet. Please complete checkout first.");
                                    payNowButton.setDisable(false);
                                });
                            }
                        } catch (Exception e) {
                            Platform.runLater(() -> {
                                showAlert("Error confirming checkout: " + e.getMessage());
                                payNowButton.setDisable(false);
                            });
                        }
                    }).start();
                } else if (newUrl.contains("/payment/cancel")) {
                    Stage stage = (Stage) webView.getScene().getWindow();
                    stage.close();
                    showAlert("Payment cancelled. Offer remains accepted and unpaid.");
                    payNowButton.setDisable(false);
                }
            });

            webView.getEngine().load(paymentLink);

            BorderPane root = new BorderPane(webView);
            Scene scene = new Scene(root, 980, 760);

            Stage popup = new Stage();
            popup.setTitle("Stripe Checkout");
            popup.setScene(scene);
            popup.initModality(Modality.APPLICATION_MODAL);

            if (payNowButton != null && payNowButton.getScene() != null) {
                popup.initOwner(payNowButton.getScene().getWindow());
            }

            popup.show();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Stripe Checkout URL");
            alert.setHeaderText("Unable to open in-app checkout popup. Copy this URL:");
            TextArea textArea = new TextArea(paymentLink);
            textArea.setWrapText(true);
            textArea.setPrefHeight(100);
            textArea.setEditable(false);
            alert.getDialogPane().setContent(textArea);
            alert.showAndWait();
            payNowButton.setDisable(false);
        }
    }

    private String extractSessionId(String url) {
        String key = "session_id=";
        int index = url.indexOf(key);
        if (index < 0) {
            return null;
        }

        String value = url.substring(index + key.length());
        int amp = value.indexOf('&');
        if (amp >= 0) {
            value = value.substring(0, amp);
        }
        return value;
    }

    /**
     * Handle cancel offer button (for sent offers)
     */
    @FXML
    private void handleCancelOffer() {
        MarketplaceOffer selected = offersSentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Please select an offer to cancel");
            return;
        }

        if (!"PENDING".equals(selected.getStatus())) {
            showAlert("Can only cancel pending offers");
            return;
        }

        if (confirmAction("Cancel this offer?")) {
            long userId = getCurrentUserId();
            if (offerService.cancelOffer(selected.getId(), userId)) {
                showAlert("Offer cancelled");
                loadOffers();
            } else {
                showAlert("Error cancelling offer");
            }
        }
    }

    @FXML
    private void handleRefresh() {
        loadListings();
        updateCarbonPrice();
    }

    /**
     * Inner class to hold payment card details
     */
    private static class PaymentDetails {
        String cardNumber;
        String expiryDate;
        String cvc;

        PaymentDetails(String cardNumber, String expiryDate, String cvc) {
            this.cardNumber = cardNumber;
            this.expiryDate = expiryDate;
            this.cvc = cvc;
        }
    }

    /**
     * Show payment dialog for user to enter card details
     * Test card: 4242 4242 4242 4242, Expiry: Any future, CVC: Any 3 digits
     */
    private PaymentDetails showPaymentDialog(double totalAmount) {
        Dialog<PaymentDetails> dialog = new Dialog<>();
        dialog.setTitle("Payment Information");
        dialog.setHeaderText("Enter Payment Details");
        dialog.setResizable(true);

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        content.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11;");

        Label amountLabel = new Label("Amount to Pay: $" + String.format("%.2f", totalAmount));
        amountLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

        Label cardLabel = new Label("Card Number");
        TextField cardField = new TextField();
        cardField.setPromptText("4242 4242 4242 4242");
        cardField.setStyle("-fx-font-family: monospace; -fx-font-size: 11;");

        Label expiryLabel = new Label("Expiry Date (MM/YY)");
        TextField expiryField = new TextField();
        expiryField.setPromptText("12/25");
        expiryField.setMaxWidth(100);

        Label cvcLabel = new Label("CVC");
        TextField cvcField = new PasswordField();
        cvcField.setPromptText("123");
        cvcField.setMaxWidth(80);

        HBox expiryBox = new HBox(12);
        expiryBox.getChildren().addAll(
            new VBox(4, expiryLabel, expiryField),
            new VBox(4, cvcLabel, cvcField)
        );

        Label infoLabel = new Label(
            "Test Cards (Expiry: any future date, CVC: any 3 digits):\n\n" +
            "✓ Success: 4242 4242 4242 4242\n" +
            "✗ Declined: 4000 0000 0000 0002\n" +
            "✗ Insufficient Funds: 4000 0000 0000 9995\n" +
            "✓ Mastercard: 5555 5555 5555 4444"
        );
        infoLabel.setStyle("-fx-text-fill: #0066cc; -fx-font-size: 9; -fx-font-family: monospace;");
        infoLabel.setWrapText(true);

        content.getChildren().addAll(
            amountLabel,
            new Separator(),
            cardLabel,
            cardField,
            expiryBox,
            infoLabel
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(
            new ButtonType("Pay Now", ButtonBar.ButtonData.OK_DONE),
            ButtonType.CANCEL
        );

        dialog.setResultConverter(buttonType -> {
            if (buttonType.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                String cardNum = cardField.getText().trim();
                String expiry = expiryField.getText().trim();
                String cvc = cvcField.getText().trim();

                if (cardNum.isEmpty() || expiry.isEmpty() || cvc.isEmpty()) {
                    showAlert("Please fill in all payment fields");
                    return null;
                }

                if (!expiry.contains("/")) {
                    showAlert("Expiry format should be MM/YY");
                    return null;
                }

                if (cvc.length() < 3 || cvc.length() > 4) {
                    showAlert("CVC must be 3-4 digits");
                    return null;
                }

                return new PaymentDetails(cardNum, expiry, cvc);
            }
            return null;
        });

        Optional<PaymentDetails> result = dialog.showAndWait();
        return result.orElse(null);
    }

    /**
     * Mask card number for display (show only last 4 digits)
     */
    private String maskCardNumber(String cardNumber) {
        String cleaned = cardNumber.replaceAll("\\s", "");
        if (cleaned.length() >= 4) {
            return "**** **** **** " + cleaned.substring(cleaned.length() - 4);
        }
        return "****";
    }

    @FXML
    private void handleRefreshPrice() {
        String creditType = "VOLUNTARY_CARBON_MARKET";
        boolean success = pricingService.refreshPriceFromAPI(creditType);
        
        if (success) {
            showAlert("✓ API price refreshed successfully!");
            updateCarbonPrice();
        } else {
            long hours = 24;  // Approximate message
            showAlert("API refresh rate limit in effect.\nNext update available in approximately " + hours + " hours.\nUse manual refresh again later.");
        }
    }

    @FXML
    private void handleTestPayment() {
        // Create dialog for test payment information
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Stripe Test Payment");
        dialog.setHeaderText("Test Card Payment Information");
        
        VBox content = new VBox(12);
        content.setPadding(new Insets(12));
        content.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11;");
        
        Label infoLabel = new Label(
            "Test Stripe Payments\n\n" +
            "Available Test Cards (Expiry: any future date, CVC: any 3 digits):\n\n" +
            "✓ Success:\n" +
            "  • 4242 4242 4242 4242 (Visa)\n" +
            "  • 5555 5555 5555 4444 (Mastercard)\n" +
            "  • 3782 822463 10005 (Amex)\n\n" +
            "✗ Failures:\n" +
            "  • 4000 0000 0000 0002 (Declined)\n" +
            "  • 4000 0000 0000 9995 (Insufficient Funds)\n\n" +
            "Amount: $25.00 (simulated carbon credit purchase)"
        );
        infoLabel.setWrapText(true);
        infoLabel.setStyle("-fx-font-size: 10; -fx-font-family: 'Segoe UI';");
        
        content.getChildren().add(infoLabel);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(
            new ButtonType("Start Test Payment", ButtonBar.ButtonData.OK_DONE),
            ButtonType.CANCEL
        );
        
        dialog.setResultConverter(buttonType -> {
            if (buttonType.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                return "proceed";
            }
            return null;
        });
        
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && "proceed".equals(result.get())) {
            startTestPayment();
        }
    }

    /**
     * Start a test payment with simulated data
     */
    private void startTestPayment() {
        try {
            // Create test order
            int buyerId = getCurrentUserId();
            int sellerId = 1;  // Simulated seller
            // int listingId = 1;  // Find first listing or use sample
            double amountUsd = 25.00;  // Test amount
            
            System.out.println("[MarketplaceController] Starting test payment: $" + amountUsd);
            
            // Initiate Stripe payment
            PaymentIntent paymentIntent = stripeService.initiatePayment(
                0,  // orderId (0 for test)
                amountUsd,
                buyerId,
                sellerId,
                "GreenWallet Test Carbon Credit Purchase"
            );
            
            if (paymentIntent != null) {
                String status = paymentIntent.getStatus();
                String id = paymentIntent.getId();
                
                showAlert("✓ Test Payment Intent Created!\n\n" +
                         "Payment ID: " + id + "\n" +
                         "Amount: $" + amountUsd + "\n" +
                         "Status: " + status + "\n\n" +
                         "Use the test card information above to complete payment.\n" +
                         "Card: 4242 4242 4242 4242\n" +
                         "Expiry: Any future date\n" +
                         "CVC: Any 3 digits");
            } else {
                showAlert("✗ Failed to create test payment intent");
            }
        } catch (Exception e) {
            showAlert("✗ Test Payment Error: " + e.getMessage());
            System.err.println("[MarketplaceController] Test payment error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack() {
        try {
            org.GreenLedger.MainFX.setRoot("greenwallet");
        } catch (IOException e) {
            showAlert("Error navigating back: " + e.getMessage());
        }
    }
}
