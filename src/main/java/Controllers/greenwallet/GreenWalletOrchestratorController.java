package Controllers.greenwallet;

import Models.Wallet;
import Models.User;
import Models.OperationWallet;
import Services.WalletService;
import Services.ClimatiqApiService;
import Services.AirQualityService;
import Utils.SessionManager;
import Utils.EventBusManager;
import Utils.EventBusManager.*;
import com.google.common.eventbus.Subscribe;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;
import javafx.application.Platform;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.math.BigDecimal;

/**
 * GREEN WALLET ORCHESTRATOR - Main Controller
 * 
 * Architecture Pattern: Mediator + Observer (Event-Driven)
 * Responsibilities:
 * - Route user actions to specialized controllers
 * - Coordinate inter-controller communication via EventBus
 * - Manage slide-in panel animations (no popup dialogs)
 * - Handle global state (current wallet, user session)
 * - Lifecycle management for child controllers
 * 
 * Delegates to:
 * - DashboardController: Stat cards, real-time metrics, impact bar
 * - OperationPanelController: Issue/Retire/Transfer slide-in forms
 * - ScopeAnalysisController: Waterfall charts, scope breakdown, drill-down
 * - MapIntegrationController: WebView management, Leaflet.js bridge, AQI data
 * - BatchExplorerController: Batch list, timeline view, detail popovers
 * - EmissionsCalculatorController: Reactive calculations, tier indicators
 * 
 * Event Handling:
 * - Subscribes to: WalletUpdatedEvent, BatchIssuedEvent, CreditsRetiredEvent
 * - Posts: RefreshRequestedEvent when wallet changes
 * 
 * Psychology Integration:
 * - Loss aversion: Impact bar shows "+X above baseline" (not total)
 * - Mental accounting: Scope 1/2/3 visually separated in waterfall
 * - Status signaling: Tier badges, verification standards displayed
 * - Social proof: Peer benchmark percentile comparison
 * 
 * Performance:
 * - Lazy initialization of child controllers (only when first needed)
 * - Reactive subscriptions cleaned up in shutdown()
 * - EventBus async handlers prevent UI blocking
 * 
 * @author Elite Green Wallet Team
 * @version 2.0 - Production Ready
 */
public class GreenWalletOrchestratorController {

    // ============================================================================
    // SERVICES & STATE
    // ============================================================================
    
    private WalletService walletService;
    private ClimatiqApiService climatiqService;
    private AirQualityService airQualityService;
    
    private Wallet currentWallet;
    private User currentUser;
    
    // Child Controllers (Lazy Initialized)
    private DashboardController dashboardController;
    private OperationPanelController operationPanelController;
    private ScopeAnalysisController scopeAnalysisController;
    private MapIntegrationController mapIntegrationController;
    private BatchExplorerController batchExplorerController;
    private EmissionsCalculatorController emissionsCalculatorController;
    
    private Map<String, Boolean> controllerInitialized = new HashMap<>();

    // ============================================================================
    // FXML COMPONENTS (From greenwallet.fxml)
    // ============================================================================
    
    // Main Layout
    @FXML private StackPane rootPane;
    @FXML private HBox mainLayer;
    @FXML private BorderPane contentWrapper;
    @FXML private ScrollPane mainScrollPane;
    
    // Sidebar
    @FXML private Button btnGestionProjets;
    @FXML private Button btnWalletOverview;
    @FXML private Button btnMarketplace;
    @FXML private Button btnSettings;
    @FXML private Label lblSidebarAvailable;
    @FXML private Label lblSidebarRetired;
    @FXML private Label lblSidebarGoal;
    @FXML private Label lblLoggedUser;
    
    // Top Impact Bar (Loss Framed)
    @FXML private HBox impactBar;
    @FXML private Label lblImpactAmount;
    @FXML private Label lblImpactGoal;
    @FXML private ProgressBar progressImpact;
    
    // Header
    @FXML private ComboBox<Wallet> cmbWalletSelector;
    @FXML private Button btnCreateWallet;
    
    // Scope Breakdown Section
    @FXML private Label lblScopeDataQuality;
    @FXML private Pane waterfallChartPane;
    @FXML private Label lblScope1Amount;
    @FXML private Label lblScope2Amount;
    @FXML private Label lblScope3Amount;
    
    // Map & Batch Explorer
    @FXML private WebView mapWebView;
    @FXML private Button btnMapFullscreen;
    @FXML private ListView<?> listBatches;
    @FXML private Button btnViewAllBatches;
    @FXML private Button btnIssueBatch;
    
    // Stat Cards
    @FXML private Label lblAvailableCredits;
    @FXML private Label lblRetiredCredits;
    @FXML private Label lblPeerRank;
    
    // Transactions Table
    @FXML private TableView<OperationWallet> tableTransactions;
    @FXML private Button btnRefresh;
    @FXML private Button btnFilterTransactions;
    
    // Action Buttons
    @FXML private Button btnIssueCreditsMain;
    @FXML private Button btnRetireCreditsMain;
    @FXML private Button btnTransferCredits;
    @FXML private Button btnCalculateEmissions;
    @FXML private Button btnExport;
    
    // Slide-In Panels (Overlays)
    @FXML private VBox issueCreditPanel;
    @FXML private VBox retireCreditPanel;
    @FXML private VBox transferCreditPanel;
    @FXML private VBox emissionsCalculatorPanel;

    // ============================================================================
    // INITIALIZATION
    // ============================================================================
    
    @FXML
    public void initialize() {
        System.out.println("[Orchestrator] Initializing Green Wallet Controller...");
        
        // Initialize services
        walletService = new WalletService();
        climatiqService = new ClimatiqApiService();
        airQualityService = new AirQualityService();
        
        // Get current user from session
        currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            lblLoggedUser.setText("👤 " + currentUser.getNom());
        }
        
        // Register this controller with EventBus
        EventBusManager.register(this);
        
        // Setup wallet selector
        setupWalletSelector();
        
        // Initialize child controllers (lazy)
        controllerInitialized.put("dashboard", false);
        controllerInitialized.put("operations", false);
        controllerInitialized.put("scope", false);
        controllerInitialized.put("map", false);
        controllerInitialized.put("batch", false);
        controllerInitialized.put("emissions", false);
        
        // Setup slide-in panels (initially off-screen right)
        initializeSlidePanels();
        
        // Setup navigation listeners
        setupNavigationListeners();
        
        System.out.println("[Orchestrator] Initialization complete.");
    }
    
    /**
     * Initialize slide-in panels off-screen and set managed=false.
     * Panels slide in from right (translateX: 1200 → 0) when triggered.
     */
    private void initializeSlidePanels() {
        issueCreditPanel.setTranslateX(1200);
        issueCreditPanel.setManaged(false);
        retireCreditPanel.setTranslateX(1200);
        retireCreditPanel.setManaged(false);
        transferCreditPanel.setTranslateX(1200);
        transferCreditPanel.setManaged(false);
        emissionsCalculatorPanel.setTranslateX(1200);
        emissionsCalculatorPanel.setManaged(false);
    }
    
    /**
     * Setup wallet ComboBox with user's wallets + change listener.
     */
    private void setupWalletSelector() {
        if (currentUser == null) return;
        
        // Load wallets for current user
        try {
            java.util.List<Wallet> userWallets = walletService.getWalletsByOwnerId(currentUser.getId().intValue());
            if (userWallets != null && !userWallets.isEmpty()) {
                cmbWalletSelector.setItems(javafx.collections.FXCollections.observableArrayList(userWallets));
                
                // Auto-select first wallet
                cmbWalletSelector.getSelectionModel().selectFirst();
                if (cmbWalletSelector.getValue() != null) {
                    loadWallet(cmbWalletSelector.getValue());
                }
            }
        } catch (Exception e) {
            System.err.println("[Orchestrator] Error loading wallets: " + e.getMessage());
        }
        
        cmbWalletSelector.setOnAction(e -> {
            Wallet selectedWallet = cmbWalletSelector.getValue();
            if (selectedWallet != null) {
                loadWallet(selectedWallet);
            }
        });
    }
    
    /**
     * Setup navigation button listeners (sidebar + action buttons).
     */
    private void setupNavigationListeners() {
        btnGestionProjets.setOnAction(e -> navigateToGestionProjets());
        btnMarketplace.setOnAction(e -> navigateToMarketplace());
        btnSettings.setOnAction(e -> navigateToSettings());
        
        btnIssueCreditsMain.setOnAction(e -> showIssueCreditsPanel());
        btnRetireCreditsMain.setOnAction(e -> showRetireCreditsPanel());
        btnTransferCredits.setOnAction(e -> showTransferPanel());
        btnCalculateEmissions.setOnAction(e -> showEmissionsCalculatorPanel());
        btnExport.setOnAction(e -> exportToCsv());
        
        btnCreateWallet.setOnAction(e -> createNewWallet());
        btnRefresh.setOnAction(e -> refreshAll());
    }

    // ============================================================================
    // WALLET MANAGEMENT
    // ============================================================================
    
    /**
     * Load wallet and notify all child controllers via EventBus.
     */
    private void loadWallet(Wallet wallet) {
        this.currentWallet = wallet;
        System.out.println("[Orchestrator] Loading wallet: " + wallet.getWalletNumber());
        
        // Post event to notify all subscribed controllers
        EventBusManager.post(new WalletUpdatedEvent(wallet));
        
        // Initialize dashboard controller if not done yet
        if (!controllerInitialized.get("dashboard")) {
            initializeDashboardController();
        }
        
        // Update sidebar quick stats
        updateSidebarStats();
        
        // Refresh dashboard metrics
        if (dashboardController != null) {
            dashboardController.refreshMetrics(wallet);
        }
    }
    
    /**
     * Update sidebar quick stats (mini-dashboard).
     */
    private void updateSidebarStats() {
        if (currentWallet == null) return;
        
        double available = currentWallet.getAvailableCredits();
        double retired = currentWallet.getRetiredCredits();
        
        lblSidebarAvailable.setText(String.format("%.2f tCO₂", available));
        lblSidebarRetired.setText(String.format("%.2f tCO₂", retired));
        
        // Calculate goal as 2x current retired credits or minimum 100
        double goal = Math.max(100.0, retired * 2);
        lblSidebarGoal.setText(String.format("Goal: %.0f tCO₂", goal));
    }
    
    /**
     * Refresh all data (reload wallet from database).
     */
    private void refreshAll() {
        if (currentWallet != null) {
            // Reload wallet from service
            Wallet refreshedWallet = walletService.getWalletById(currentWallet.getId());
            if (refreshedWallet != null) {
                loadWallet(refreshedWallet);
            }
        }
        
        // Post refresh event
        EventBusManager.post(new RefreshRequestedEvent("ALL"));
    }

    // ============================================================================
    // SLIDE-IN PANEL ANIMATIONS
    // ============================================================================
    
    /**
     * Show Issue Credits panel with slide-in animation.
     */
    @FXML
    public void showIssueCreditsPanel() {
        System.out.println("[Orchestrator] Showing Issue Credits panel");
        
        // Initialize operations controller if not done
        if (!controllerInitialized.get("operations")) {
            initializeOperationPanelController();
        }
        
        // Animate panel into view
        issueCreditPanel.setManaged(true);
        issueCreditPanel.setVisible(true);
        
        TranslateTransition slide = new TranslateTransition(Duration.millis(300), issueCreditPanel);
        slide.setFromX(1200);
        slide.setToX(0);
        slide.play();
        
        // Populate form if operations controller initialized
        if (operationPanelController != null) {
            operationPanelController.prepareIssueForm(currentWallet);
        }
    }
    
    /**
     * Close Issue Credits panel with slide-out animation.
     */
    @FXML
    public void onCloseIssuePanel() {
        TranslateTransition slide = new TranslateTransition(Duration.millis(300), issueCreditPanel);
        slide.setFromX(0);
        slide.setToX(1200);
        slide.setOnFinished(e -> {
            issueCreditPanel.setManaged(false);
            issueCreditPanel.setVisible(false);
        });
        slide.play();
    }
    
    /**
     * Show Retire Credits panel with slide-in animation.
     */
    @FXML
    public void showRetireCreditsPanel() {
        System.out.println("[Orchestrator] Showing Retire Credits panel");
        
        if (!controllerInitialized.get("operations")) {
            initializeOperationPanelController();
        }
        
        retireCreditPanel.setManaged(true);
        retireCreditPanel.setVisible(true);
        
        TranslateTransition slide = new TranslateTransition(Duration.millis(300), retireCreditPanel);
        slide.setFromX(1200);
        slide.setToX(0);
        slide.play();
        
        if (operationPanelController != null) {
            operationPanelController.prepareRetireForm(currentWallet);
        }
    }
    
    /**
     * Close Retire Credits panel.
     */
    @FXML
    public void onCloseRetirePanel() {
        TranslateTransition slide = new TranslateTransition(Duration.millis(300), retireCreditPanel);
        slide.setFromX(0);
        slide.setToX(1200);
        slide.setOnFinished(e -> {
            retireCreditPanel.setManaged(false);
            retireCreditPanel.setVisible(false);
        });
        slide.play();
    }
    
    /**
     * Show Transfer panel.
     */
    @FXML
    public void showTransferPanel() {
        if (!controllerInitialized.get("operations")) {
            initializeOperationPanelController();
        }
        
        transferCreditPanel.setManaged(true);
        transferCreditPanel.setVisible(true);
        
        TranslateTransition slide = new TranslateTransition(Duration.millis(300), transferCreditPanel);
        slide.setFromX(1200);
        slide.setToX(0);
        slide.play();
        
        if (operationPanelController != null) {
            operationPanelController.prepareTransferForm(currentWallet);
        }
    }
    
    /**
     * Close Transfer panel.
     */
    @FXML
    public void onCloseTransferPanel() {
        TranslateTransition slide = new TranslateTransition(Duration.millis(300), transferCreditPanel);
        slide.setFromX(0);
        slide.setToX(1200);
        slide.setOnFinished(e -> {
            transferCreditPanel.setManaged(false);
            transferCreditPanel.setVisible(false);
        });
        slide.play();
    }
    
    /**
     * Show Emissions Calculator panel.
     */
    @FXML
    public void showEmissionsCalculatorPanel() {
        if (!controllerInitialized.get("emissions")) {
            initializeEmissionsCalculatorController();
        }
        
        emissionsCalculatorPanel.setManaged(true);
        emissionsCalculatorPanel.setVisible(true);
        
        TranslateTransition slide = new TranslateTransition(Duration.millis(300), emissionsCalculatorPanel);
        slide.setFromX(1200);
        slide.setToX(0);
        slide.play();
    }
    
    /**
     * Close Emissions Calculator panel.
     */
    @FXML
    public void onCloseEmissionsPanel() {
        TranslateTransition slide = new TranslateTransition(Duration.millis(300), emissionsCalculatorPanel);
        slide.setFromX(0);
        slide.setToX(1200);
        slide.setOnFinished(e -> {
            emissionsCalculatorPanel.setManaged(false);
            emissionsCalculatorPanel.setVisible(false);
        });
        slide.play();
    }

    // ============================================================================
    // CHILD CONTROLLER INITIALIZATION (LAZY)
    // ============================================================================
    
    private void initializeDashboardController() {
        System.out.println("[Orchestrator] Initializing DashboardController...");
        dashboardController = new DashboardController(
            walletService,
            lblImpactAmount,
            lblImpactGoal,
            progressImpact,
            lblAvailableCredits,
            lblRetiredCredits,
            lblPeerRank,
            tableTransactions
        );
        controllerInitialized.put("dashboard", true);
    }
    
    private void initializeOperationPanelController() {
        System.out.println("[Orchestrator] Initializing OperationPanelController...");
        operationPanelController = new OperationPanelController(
            walletService,
            this  // Pass orchestrator reference for callbacks
        );
        controllerInitialized.put("operations", true);
    }
    
    private void initializeScopeAnalysisController() {
        System.out.println("[Orchestrator] Initializing ScopeAnalysisController...");
        scopeAnalysisController = new ScopeAnalysisController(
            climatiqService,
            waterfallChartPane,
            lblScope1Amount,
            lblScope2Amount,
            lblScope3Amount,
            lblScopeDataQuality
        );
        controllerInitialized.put("scope", true);
    }
    
    private void initializeMapIntegrationController() {
        System.out.println("[Orchestrator] Initializing MapIntegrationController...");
        mapIntegrationController = new MapIntegrationController(
            airQualityService,
            mapWebView
        );
        controllerInitialized.put("map", true);
    }
    
    private void initializeBatchExplorerController() {
        System.out.println("[Orchestrator] Initializing BatchExplorerController...");
        batchExplorerController = new BatchExplorerController(
            walletService,
            listBatches
        );
        controllerInitialized.put("batch", true);
    }
    
    private void initializeEmissionsCalculatorController() {
        System.out.println("[Orchestrator] Initializing EmissionsCalculatorController...");
        emissionsCalculatorController = new EmissionsCalculatorController(
            climatiqService,
            emissionsCalculatorPanel
        );
        controllerInitialized.put("emissions", true);
    }

    // ============================================================================
    // EVENT BUS HANDLERS
    // ============================================================================
    
    /**
     * Handle batch issued event (refresh dashboard).
     */
    @Subscribe
    public void onBatchIssued(BatchIssuedEvent event) {
        Platform.runLater(() -> {
            System.out.println("[Orchestrator] Batch issued: " + event.getBatch().getSerialNumber());
            refreshAll();
        });
    }
    
    /**
     * Handle credits retired event (update impact bar).
     */
    @Subscribe
    public void onCreditsRetired(CreditsRetiredEvent event) {
        Platform.runLater(() -> {
            System.out.println("[Orchestrator] Credits retired: " + event.getAmount() + " tCO₂");
            refreshAll();
        });
    }
    
    /**
     * Handle calculation completed event (show result notification).
     */
    @Subscribe
    public void onCalculationCompleted(CalculationCompletedEvent event) {
        Platform.runLater(() -> {
            System.out.println("[Orchestrator] Calculation completed: " + event.getResult().getCo2eAmount() + " tCO₂e");
            // Could show inline notification banner here
        });
    }

    // ============================================================================
    // NAVIGATION
    // ============================================================================
    
    private void navigateToGestionProjets() {
        System.out.println("[Orchestrator] Navigating to Gestion Projets...");
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader();
            loader.setLocation(getClass().getResource("/GestionProjet.fxml"));
            javafx.scene.Parent root = loader.load();
            
            javafx.stage.Stage stage = (javafx.stage.Stage) rootPane.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(root));
            stage.setTitle("Gestion de Projets - Green Wallet");
        } catch (Exception e) {
            System.err.println("[Orchestrator] Error navigating to Gestion Projets: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void navigateToMarketplace() {
        System.out.println("[Orchestrator] Navigating to Marketplace...");
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/marketplace.fxml"));
            javafx.scene.Parent root = loader.load();
            
            javafx.stage.Stage stage = (javafx.stage.Stage) rootPane.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(root));
            stage.setTitle("Marketplace - Green Wallet");
        } catch (Exception e) {
            System.err.println("[Orchestrator] Error navigating to Marketplace: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void navigateToSettings() {
        System.out.println("[Orchestrator] Navigating to Settings...");
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader();
            loader.setLocation(getClass().getResource("/settings.fxml"));
            javafx.scene.Parent root = loader.load();
            
            javafx.stage.Stage stage = (javafx.stage.Stage) rootPane.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(root));
            stage.setTitle("Settings - Green Wallet");
        } catch (Exception e) {
            System.err.println("[Orchestrator] Error navigating to Settings: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ============================================================================
    // ACTION HANDLERS
    // ============================================================================
    
    /**
     * Confirm issue credits operation from slide-in panel.
     */
    @FXML
    public void onConfirmIssue() {
        if (operationPanelController != null) {
            operationPanelController.executeIssue();
            onCloseIssuePanel();
        }
    }
    
    /**
     * Confirm retire credits operation from slide-in panel.
     */
    @FXML
    public void onConfirmRetire() {
        if (operationPanelController != null) {
            operationPanelController.executeRetire();
            onCloseRetirePanel();
        }
    }
    
    /**
     * Confirm transfer operation from slide-in panel.
     */
    @FXML
    public void onConfirmTransfer() {
        if (operationPanelController != null) {
            operationPanelController.executeTransfer();
            onCloseTransferPanel();
        }
    }
    
    /**
     * Execute emissions calculation.
     */
    @FXML
    public void onCalculateEmissions() {
        if (emissionsCalculatorController != null) {
            emissionsCalculatorController.executeCalculation();
        }
    }
    
    /**
     * Export wallet data to CSV.
     */
    private void exportToCsv() {
        System.out.println("[Orchestrator] Exporting to CSV...");
        
        if (currentWallet == null) {
            showAlert("No Wallet", "Please select a wallet to export.", javafx.scene.control.Alert.AlertType.WARNING);
            return;
        }
        
        try {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Export Wallet Data");
            fileChooser.setInitialFileName("wallet_" + currentWallet.getWalletNumber() + "_export.csv");
            fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv")
            );
            
            javafx.stage.Stage stage = (javafx.stage.Stage) rootPane.getScene().getWindow();
            java.io.File file = fileChooser.showSaveDialog(stage);
            
            if (file != null) {
                exportWalletToCsv(file);
                showAlert("Export Success", "Wallet data exported to: " + file.getAbsolutePath(), 
                    javafx.scene.control.Alert.AlertType.INFORMATION);
            }
        } catch (Exception e) {
            System.err.println("[Orchestrator] Export failed: " + e.getMessage());
            showAlert("Export Failed", "Failed to export wallet data: " + e.getMessage(), 
                javafx.scene.control.Alert.AlertType.ERROR);
        }
    }
    
    /**
     * Export wallet to CSV file
     */
    private void exportWalletToCsv(java.io.File file) throws java.io.IOException {
        try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
            // Write header
            writer.write("Wallet Number,Name,Owner Type,Owner ID,Available Credits,Retired Credits,Total Credits\n");
            
            // Write wallet data
            writer.write(String.format("%s,%s,%s,%d,%.2f,%.2f,%.2f\n",
                currentWallet.getWalletNumber(),
                currentWallet.getName() != null ? currentWallet.getName() : "Unnamed",
                currentWallet.getOwnerType(),
                currentWallet.getOwnerId(),
                currentWallet.getAvailableCredits(),
                currentWallet.getRetiredCredits(),
                currentWallet.getTotalCredits()
            ));
            
            writer.write("\n\nTransaction History\n");
            writer.write("ID,Type,Amount,Date,Reference\n");
            
            // Get transactions
            List<OperationWallet> transactions = walletService.getWalletTransactions(currentWallet.getId());
            if (transactions != null) {
                for (OperationWallet transaction : transactions) {
                    writer.write(String.format("%d,%s,%.2f,%s,%s\n",
                        transaction.getId(),
                        transaction.getType(),
                        transaction.getAmount(),
                        transaction.getCreatedAt(),
                        transaction.getReferenceNote() != null ? transaction.getReferenceNote() : ""
                    ));
                }
            }
        }
    }
    
    /**
     * Create new wallet dialog.
     */
    private void createNewWallet() {
        System.out.println("[Orchestrator] Creating new wallet...");
        
        if (currentUser == null) {
            showAlert("Not Logged In", "Please log in to create a wallet.", javafx.scene.control.Alert.AlertType.WARNING);
            return;
        }
        
        // Show text input dialog for wallet name
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
        dialog.setTitle("Create New Wallet");
        dialog.setHeaderText("Create a new carbon credit wallet");
        dialog.setContentText("Wallet Name:");
        
        java.util.Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                try {
                    Wallet newWallet = new Wallet("USER", currentUser.getId().intValue());
                    newWallet.setName(name.trim());
                    
                    int walletId = walletService.createWallet(newWallet);
                    
                    if (walletId > 0) {
                        showAlert("Wallet Created", "New wallet created successfully!", 
                            javafx.scene.control.Alert.AlertType.INFORMATION);
                        
                        // Reload wallet list
                        setupWalletSelector();
                    } else {
                        showAlert("Creation Failed", "Failed to create wallet. Please try again.", 
                            javafx.scene.control.Alert.AlertType.ERROR);
                    }
                } catch (Exception e) {
                    System.err.println("[Orchestrator] Error creating wallet: " + e.getMessage());
                    showAlert("Creation Failed", "Error: " + e.getMessage(), 
                        javafx.scene.control.Alert.AlertType.ERROR);
                }
            }
        });
    }
    
    /**
     * Navigate back to previous view.
     */
    @FXML
    public void onBack() {
        System.out.println("[Orchestrator] Navigating back...");
        try {
            // Navigate back to main menu or dashboard
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader();
            loader.setLocation(getClass().getResource("/main.fxml"));
            javafx.scene.Parent root = loader.load();
            
            javafx.stage.Stage stage = (javafx.stage.Stage) rootPane.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(root));
            stage.setTitle("Green Ledger - Main Menu");
        } catch (Exception e) {
            System.err.println("[Orchestrator] Error navigating back: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Show alert dialog
     */
    private void showAlert(String title, String message, javafx.scene.control.Alert.AlertType type) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ============================================================================
    // LIFECYCLE
    // ============================================================================
    
    /**
     * Cleanup on controller shutdown.
     * Unregister from EventBus, cancel reactive subscriptions.
     */
    public void shutdown() {
        System.out.println("[Orchestrator] Shutting down...");
        EventBusManager.unregister(this);
        
        // Cleanup child controllers
        if (dashboardController != null) {
            dashboardController.shutdown();
        }
        if (mapIntegrationController != null) {
            mapIntegrationController.shutdown();
        }
        // ... cleanup other controllers
    }
}
