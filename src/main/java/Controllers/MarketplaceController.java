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
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

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

    // Price Display
    @FXML private Label currentCarbonPriceLabel;
    @FXML private Label marketPanelPriceLabel;
    @FXML private Label marketPanelNoteLabel;
    @FXML private LineChart<Number, Number> marketMiniChart;

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
            sentStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
            
            offersSentTable.setItems(offersSentData);
            offersSentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
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

        priceMinSlider.setMin(0);
        priceMinSlider.setMax(100);
        priceMinSlider.setValue(0);

        priceMaxSlider.setMin(0);
        priceMaxSlider.setMax(100);
        priceMaxSlider.setValue(100);

        // Update label when sliders change
        priceMinSlider.valueProperty().addListener((obs, oldVal, newVal) -> updatePriceLabel());
        priceMaxSlider.valueProperty().addListener((obs, oldVal, newVal) -> updatePriceLabel());
        assetTypeFilter.valueProperty().addListener((obs, oldVal, newVal) -> loadListings());
    }

    /**
     * Update price range label
     */
    private void updatePriceLabel() {
        priceRangeLabel.setText(String.format("$%.2f - $%.2f", 
            priceMinSlider.getValue(), priceMaxSlider.getValue()));
    }

    /**
     * Load all active listings from marketplace
     */
    @FXML
    private void loadListings() {
        new Thread(() -> {
            try {
                String assetType = assetTypeFilter.getValue();
                if ("All Types".equals(assetType)) {
                    assetType = null;
                }

                List<MarketplaceListing> listings = listingService.getActiveListings();
                
                // Filter by price range
                double minPrice = priceMinSlider.getValue();
                double maxPrice = priceMaxSlider.getValue();

                listings.removeIf(l -> l.getPricePerUnit() < minPrice || l.getPricePerUnit() > maxPrice);

                javafx.application.Platform.runLater(() -> {
                    listingsData.clear();
                    listingsData.addAll(listings);
                    System.out.println(LOG_TAG + " Loaded " + listings.size() + " listings");
                });
            } catch (Exception e) {
                System.err.println(LOG_TAG + " ERROR loading listings: " + e.getMessage());
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

                javafx.application.Platform.runLater(() -> {
                    offersReceivedData.clear();
                    offersReceivedData.addAll(received);
                    
                    offersSentData.clear();
                    offersSentData.addAll(sent);
                    
                    System.out.println(LOG_TAG + " Loaded " + received.size() + " offers received, " + 
                                      sent.size() + " offers sent");
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
                    marketMiniChart.getData().clear();
                    
                    // Create series for the chart
                    XYChart.Series<Number, Number> series = new XYChart.Series<>();
                    series.setName("Carbon Price (USD/tCO2e)");
                    
                    // Add data points to series
                    for (int i = 0; i < priceHistory.size(); i++) {
                        CarbonPriceSnapshot snapshot = priceHistory.get(i);
                        series.getData().add(new XYChart.Data<>(i, snapshot.getUsdPerTon()));
                    }
                    
                    // Add series to chart
                    marketMiniChart.getData().add(series);
                    
                    // Set chart title and display current price
                    if (!priceHistory.isEmpty()) {
                        CarbonPriceSnapshot latest = priceHistory.get(priceHistory.size() - 1);
                        marketPanelPriceLabel.setText(String.format("$%.2f/tCO2e", latest.getUsdPerTon()));
                        marketPanelNoteLabel.setText("30-day price history with daily updates");
                    }
                });
            } catch (Exception e) {
                System.err.println(LOG_TAG + " ERROR setting up price chart: " + e.getMessage());
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

                double totalAmount = quantity * selected.getPricePerUnit();
                
                // Confirm purchase
                if (!confirmAction(String.format("Purchase %.2f %s for $%.2f?", 
                        quantity, selected.getAssetType(), totalAmount))) {
                    return;
                }

                // Create order first
                int orderId = orderService.placeOrder(selected.getId(), currentUserId, quantity);
                if (orderId <= 0) {
                    showAlert("Error creating order");
                    return;
                }

                // Show payment dialog to user
                PaymentDetails paymentDetails = showPaymentDialog(totalAmount);
                if (paymentDetails == null) {
                    showAlert("Payment cancelled. Order #" + orderId + " created but not paid.");
                    return;
                }

                // Step 1: Create payment intent
                System.out.println(LOG_TAG + " Creating Stripe payment intent for order " + orderId);
                var paymentIntent = stripeService.initiatePayment(
                    orderId, 
                    totalAmount, 
                    currentUserId, 
                    selected.getSellerId(),
                    "Carbon Credit Purchase - Order #" + orderId
                );

                if (paymentIntent == null) {
                    showAlert("Error creating payment intent. Order #" + orderId + " created but not paid.");
                    return;
                }

                System.out.println(LOG_TAG + " Payment intent created: " + paymentIntent.getId());

                // Step 2: Confirm payment with card details
                String[] expiryParts = paymentDetails.expiryDate.split("/");
                String expMonth = expiryParts[0].trim();
                String expYear = expiryParts[1].trim();
                // Convert 2-digit year to 4-digit if needed
                if (expYear.length() == 2) {
                    expYear = "20" + expYear;
                }

                System.out.println(LOG_TAG + " Confirming payment with card...");
                paymentIntent = stripeService.confirmPaymentWithCard(
                    paymentIntent.getId(),
                    paymentDetails.cardNumber,
                    expMonth,
                    expYear,
                    paymentDetails.cvc
                );

                if (paymentIntent != null && "succeeded".equals(paymentIntent.getStatus())) {
                    // Step 3: Complete order with payment ID
                    orderService.completeOrder(orderId, paymentIntent.getId());
                    
                    showAlert("✓ Purchase successful!\n\n" +
                             "Order ID: " + orderId + "\n" +
                             "Payment ID: " + paymentIntent.getId() + "\n" +
                             "Amount: $" + String.format("%.2f", totalAmount) + "\n" +
                             "Card: " + maskCardNumber(paymentDetails.cardNumber) + "\n\n" +
                             "Carbon credits transferred to your wallet!");
                    
                    loadOrderHistory();
                    loadListings();
                    loadMyListings();
                } else if (paymentIntent != null) {
                    // Payment requires additional action or failed
                    String status = paymentIntent.getStatus();
                    if ("requires_payment_method".equals(status) || "requires_confirmation".equals(status)) {
                        showAlert("⚠ Payment could not be processed.\n\n" +
                                 "Status: " + status + "\n" +
                                 "Order ID: " + orderId + "\n\n" +
                                 "Please try again with a different payment method.");
                    } else {
                        showAlert("⚠ Payment status: " + status + "\n\n" +
                                 "Order ID: " + orderId + "\n" +
                                 "Payment ID: " + paymentIntent.getId() + "\n\n" +
                                 "Contact support if funds were charged.");
                    }
                } else {
                    showAlert("✗ Payment processing error.\n\nOrder #" + orderId + " created but payment failed.\nNo funds were charged.");
                }
                
            } catch (NumberFormatException e) {
                showAlert("Invalid quantity");
            } catch (Exception e) {
                System.err.println(LOG_TAG + " ERROR in buy flow: " + e.getMessage());
                showAlert("Error processing purchase: " + e.getMessage());
            }
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

        showAlert("Edit listing feature coming soon");
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
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Marketplace");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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
            int listingId = 1;  // Find first listing or use sample
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
