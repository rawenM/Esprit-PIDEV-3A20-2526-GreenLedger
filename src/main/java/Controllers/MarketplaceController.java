package Controllers;

import Models.*;
import Services.*;
import Utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
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

    // Price Display
    @FXML private Label currentCarbonPriceLabel;

    // Services
    private final MarketplaceListingService listingService = MarketplaceListingService.getInstance();
    private final MarketplaceOrderService orderService = MarketplaceOrderService.getInstance();
    private final CarbonPricingService pricingService = CarbonPricingService.getInstance();
    private final UserMarketplaceKYCService kycService = UserMarketplaceKYCService.getInstance();

    private ObservableList<MarketplaceListing> listingsData = FXCollections.observableArrayList();
    private ObservableList<MarketplaceListing> myListingsData = FXCollections.observableArrayList();
    private ObservableList<MarketplaceOrder> ordersData = FXCollections.observableArrayList();

    @Override
    public void initialize() {
        super.initialize();
        setupListingsTable();
        setupMyListingsTable();
        setupOrdersTable();
        setupFilters();
        loadListings();
        loadMyListings();
        loadOrderHistory();
        updateCarbonPrice();
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
     * Update current carbon price display
     */
    private void updateCarbonPrice() {
        new Thread(() -> {
            try {
                double price = pricingService.getCurrentPrice("VOLUNTARY_CARBON_MARKET");
                javafx.application.Platform.runLater(() ->
                    currentCarbonPriceLabel.setText(String.format("Current Carbon Price: $%.2f/tCO2e", price))
                );
            } catch (Exception e) {
                System.err.println(LOG_TAG + " ERROR updating price: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Handle buy button click
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

                // Create order
                int orderId = orderService.placeOrder(selected.getId(), currentUserId, quantity);
                if (orderId > 0) {
                    showAlert("Order placed! Order ID: " + orderId);
                    loadOrderHistory();
                    loadListings();
                } else {
                    showAlert("Error placing order");
                }
            } catch (NumberFormatException e) {
                showAlert("Invalid quantity");
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

    @FXML
    private void handleRefresh() {
        loadListings();
        updateCarbonPrice();
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
