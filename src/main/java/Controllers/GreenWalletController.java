package Controllers;

import Models.Wallet;
import Models.OperationWallet;
import Models.TypeUtilisateur;
import Models.User;
import Services.WalletService;
import Services.ExternalCarbonApiService;
import Services.AirQualityService;
import Models.dto.external.CarbonEstimateResponse;
import Models.dto.external.AirPollutionResponse;
import Models.dto.external.AirQualityData;
import Utils.SessionManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import org.GreenLedger.MainFX;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller for Green Wallet - Carbon Credit Management System.
 */
public class GreenWalletController extends BaseController {
    
    static {
        System.out.println("###############################################");
        System.out.println("# GreenWalletController CLASS LOADED");
        System.out.println("###############################################");
    }

    // Services
    private WalletService walletService;
    private ExternalCarbonApiService carbonApiService;
    private AirQualityService airQualityService;
    private Wallet currentWallet;
    
    // Air quality data cache (disk + memory, 4 hour refresh for performance)
    private java.util.Map<String, CachedAirQuality> airQualityCache = new java.util.HashMap<>();
    private static final long CACHE_DURATION_MS = 14400000; // 4 hours (minimize API calls)
    private static final String CACHE_FILE = "air_quality_cache.json";
    private java.util.concurrent.ExecutorService airQualityExecutor = 
            java.util.concurrent.Executors.newFixedThreadPool(3); // Max 3 concurrent API calls
    
    /**
     * Cache entry for air quality data (serializable)
     */
    private static class CachedAirQuality implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        final int aqi;
        final long timestamp;
        final double lat;
        final double lon;
        
        CachedAirQuality(int aqi, double lat, double lon) {
            this.aqi = aqi;
            this.timestamp = System.currentTimeMillis();
            this.lat = lat;
            this.lon = lon;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_DURATION_MS;
        }
    }

    // Sidebar Buttons
    @FXML private Button btnWalletOverview;
    @FXML private Button btnGestionProjets;
    @FXML private Button btnMarketplace;
    @FXML private Button btnTransactions;
    @FXML private Button btnBatches;
    @FXML private Button btnIssueCredits;
    @FXML private Button btnRetireCredits;
    @FXML private Button btnCreateWallet;
    @FXML private Button btnSettings;
    @FXML private Label lblProfileName;
    @FXML private Label lblProfileType;

    // Sidebar Quick Stats
    @FXML private Label lblSidebarAvailable;
    @FXML private Label lblSidebarRetired;
    @FXML private Label lblSidebarGoal;

    // Impact Alert Section
    @FXML private Label lblImpactAmount;
    @FXML private Label lblImpactGoal;
    @FXML private ProgressBar progressImpact;

    // Wallet Selector
    @FXML private ComboBox<Wallet> cmbWalletSelector;

    // Wallet Info Labels
    @FXML private Label lblWalletNumber;
    @FXML private Label lblHolderName;
    @FXML private Label lblOwnerType;
    @FXML private Label lblStatus;

    // Scope Breakdown Labels
    @FXML private Label lblScopeDataQuality;
    @FXML private Label lblScope1Amount;
    @FXML private Label lblScope2Amount;
    @FXML private Label lblScope3Amount;
    @FXML private Pane waterfallChartPane;

    // Stat Cards
    @FXML private Label lblAvailableCredits;
    @FXML private Label lblRetiredCredits;
    @FXML private Label lblPeerRank;
    @FXML private Label lblTotalCredits;

    // Map & Batches Section
    @FXML private WebView mapWebView;
    @FXML private ListView<String> listBatches;

    // Transactions Table
    @FXML private TableView<OperationWallet> tableTransactions;
    @FXML private TableColumn<OperationWallet, Integer> colTransactionId;
    @FXML private TableColumn<OperationWallet, String> colTransactionType;
    @FXML private TableColumn<OperationWallet, Double> colTransactionAmount;
    @FXML private TableColumn<OperationWallet, String> colTransactionDate;
    @FXML private TableColumn<OperationWallet, String> colTransactionReference;

    // Action Buttons
    @FXML private Button btnIssueCreditsMain;
    @FXML private Button btnRetireCreditsMain;
    @FXML private Button btnTransferCredits;
    @FXML private Button btnEditWallet;
    @FXML private Button btnDeleteWallet;
    @FXML private Button btnExport;
    @FXML private Button btnRefresh;
    @FXML private Button btnFilterTransactions;
    @FXML private Button btnMapFullscreen;
    @FXML private Button btnViewAllBatches;
    @FXML private Button btnIssueBatch;
    @FXML private Button btnCalculateEmissions;
    @FXML private Button btnTestAdd25;
    @FXML private Button btnTestAdd100;
    @FXML private Button btnTestAdd500;
    @FXML private Button btnTestAdd1000;
    
    // API Integration Components
    @FXML private Button btnCalculateElectricity;
    @FXML private Button btnCalculateFuel;
    @FXML private Button btnCalculateShipping;
    @FXML private Button btnCheckAirQuality;
    @FXML private TextArea txtApiResults;

    // Content Pane
    @FXML private VBox contentPane;

    // Scope Toggle Buttons (Emissions Calculator)
    @FXML private ToggleButton btnScope1;
    @FXML private ToggleButton btnScope2;
    @FXML private ToggleButton btnScope3;

    // Slide-in Panels
    @FXML private VBox issueCreditPanel;
    @FXML private VBox retireCreditPanel;
    @FXML private VBox transferCreditPanel;
    @FXML private VBox emissionCalculatorPanel;
    @FXML private VBox createWalletPanel;
    @FXML private VBox editWalletPanel;
    @FXML private VBox deleteWalletPanel;

    // Create Wallet Panel Fields
    @FXML private TextField txtCreateWalletName;
    @FXML private TextField txtCreateWalletNumber;
    @FXML private TextField txtCreateWalletCredits;
    @FXML private Button btnConfirmCreateWallet;

    // Edit Wallet Panel Fields
    @FXML private TextField txtEditWalletName;
    @FXML private TextField txtEditWalletNumber;
    @FXML private Button btnConfirmEditWallet;

    // Delete Wallet Panel Fields
    @FXML private Label lblDeleteWalletName;
    @FXML private CheckBox chkDeleteWalletConfirm;
    @FXML private Button btnConfirmDeleteWallet;

    // Issue Panel Fields
    @FXML private TextField txtIssueAmount;
    @FXML private ComboBox<String> cmbVerificationStandard;
    @FXML private TextField txtVintageYear;
    @FXML private TextField txtCalculationAuditId;
    @FXML private TextArea txtIssueReference;
    @FXML private Label lblIssuePreviewAmount;
    @FXML private Label lblIssuePreviewSerial;
    @FXML private Label lblIssuePreviewStandard;
    @FXML private Button btnConfirmIssue;

    // Retire Panel Fields
    @FXML private TextField txtRetireAmount;
    @FXML private ComboBox<String> cmbRetireReason;
    @FXML private TextArea txtRetireReason;
    @FXML private Label lblRetireAvailable;
    @FXML private Label lblRetirePreviewAmount;
    @FXML private Label lblRetirePreviewBalance;
    @FXML private Button btnConfirmRetire;

    // Transfer Panel Fields
    @FXML private ComboBox<Wallet> cmbTransferTargetWallet;
    @FXML private TextField txtTransferWalletNumber;
    @FXML private TextField txtTransferAmount;
    @FXML private TextArea txtTransferReference;
    @FXML private Label lblTransferAvailable;
    @FXML private Label lblTransferFromWallet;
    @FXML private Label lblTransferToWallet;
    @FXML private Label lblTransferPreviewAmount;
    @FXML private Button btnConfirmTransfer;

    @FXML
    public void initialize() {
        System.out.println("=================================================");
        System.out.println("GREEN WALLET CONTROLLER INITIALIZING...");
        System.out.println("=================================================");
        
        super.initialize();
        walletService = new WalletService();
        carbonApiService = new ExternalCarbonApiService();
        airQualityService = new AirQualityService();
        
        System.out.println("[CACHE] Loading persisted cache from disk...");
        // Load persisted cache from disk
        loadCacheFromDisk();

        applyProfile(lblProfileName, lblProfileType);
        
        setupTableColumns();
        setupWalletSelector();
        setupMapWebView();
        setupListeners();
        setupApiListeners();
        loadWallets();

        if (btnWalletOverview != null) {
            Platform.runLater(() -> btnWalletOverview.requestFocus());
        }
    }

    private void setupTableColumns() {
        colTransactionId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTransactionType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colTransactionAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        
        // Format date column
        colTransactionDate.setCellValueFactory(cellData -> {
            if (cellData.getValue().getCreatedAt() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                return new javafx.beans.property.SimpleStringProperty(
                    cellData.getValue().getCreatedAt().format(formatter)
                );
            }
            return new javafx.beans.property.SimpleStringProperty("—");
        });
        
        colTransactionReference.setCellValueFactory(new PropertyValueFactory<>("referenceNote"));
        
        // Style type column
        colTransactionType.setCellFactory(column -> new TableCell<OperationWallet, String>() {
            @Override
            protected void updateItem(String type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(type);
                    if (type.equals("ISSUE")) {
                        setStyle("-fx-text-fill: #2B6A4A; -fx-font-weight: bold;");
                    } else if (type.equals("RETIRE")) {
                        setStyle("-fx-text-fill: #D97706; -fx-font-weight: bold;");
                    } else if (type.contains("TRANSFER")) {
                        setStyle("-fx-text-fill: #3B82F6; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    private void setupWalletSelector() {
        cmbWalletSelector.setConverter(new javafx.util.StringConverter<Wallet>() {
            @Override
            public String toString(Wallet wallet) {
                if (wallet == null) return null;
                String name = wallet.getName() != null ? wallet.getName() : "Unnamed Wallet";
                return String.format("#%s - %s (%s)", formatWalletNumber(wallet.getWalletNumber()), name, wallet.getOwnerType());
            }

            @Override
            public Wallet fromString(String string) {
                return null;
            }
        });
        
        cmbWalletSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadWallet(newVal.getId());
            }
        });
    }

    private void setupListeners() {
        if (btnWalletOverview != null) {
            btnWalletOverview.setOnAction(e -> showWalletOverview());
        }
        if (btnMarketplace != null) {
            btnMarketplace.setOnAction(e -> showMarketplace());
        }
        if (btnTransactions != null) {
            btnTransactions.setOnAction(e -> showTransactions());
        }
        if (btnBatches != null) {
            btnBatches.setOnAction(e -> showBatches());
        }
        
        if (btnIssueCredits != null) {
            btnIssueCredits.setOnAction(e -> showQuickIssueDialog());
        }
        if (btnRetireCredits != null) {
            btnRetireCredits.setOnAction(e -> showRetireCreditsDialog());
        }
        if (btnCreateWallet != null) {
            btnCreateWallet.setOnAction(e -> showCreateWalletDialog());
        }
        
        if (btnIssueCreditsMain != null) {
            btnIssueCreditsMain.setOnAction(e -> showQuickIssueDialog());
        }
        if (btnRetireCreditsMain != null) {
            btnRetireCreditsMain.setOnAction(e -> showRetireCreditsDialog());
        }
        
        if (btnTransferCredits != null) {
            btnTransferCredits.setOnAction(e -> showTransferDialog());
        }
        if (btnEditWallet != null) {
            btnEditWallet.setOnAction(e -> showEditWalletDialog());
        }
        if (btnDeleteWallet != null) {
            btnDeleteWallet.setOnAction(e -> showDeleteWalletDialog());
        }
        
        if (btnExport != null) {
            btnExport.setOnAction(e -> exportData());
        }
        if (btnRefresh != null) {
            btnRefresh.setOnAction(e -> refreshData());
        }

        if (btnFilterTransactions != null) {
            btnFilterTransactions.setOnAction(e -> showInfo("Bientôt disponible", "Le filtrage des transactions sera implémenté prochainement"));
        }

        if (btnMapFullscreen != null) {
            btnMapFullscreen.setOnAction(e -> showInfo("Bientôt disponible", "La vue plein écran de la carte sera disponible prochainement"));
        }

        if (btnViewAllBatches != null) {
            btnViewAllBatches.setOnAction(e -> showInfo("Bientôt disponible", "La vue complète des batches sera disponible prochainement"));
        }

        if (btnIssueBatch != null) {
            btnIssueBatch.setOnAction(e -> showInfo("Bientôt disponible", "La création de batches sera implémentée prochainement"));
        }

        if (btnTestAdd25 != null) {
            btnTestAdd25.setOnAction(e -> addTestCredits(25.0));
        }
        if (btnTestAdd100 != null) {
            btnTestAdd100.setOnAction(e -> addTestCredits(100.0));
        }
        if (btnTestAdd500 != null) {
            btnTestAdd500.setOnAction(e -> addTestCredits(500.0));
        }
        if (btnTestAdd1000 != null) {
            btnTestAdd1000.setOnAction(e -> addTestCredits(1000.0));
        }
        
        if (btnSettings != null) {
            btnSettings.setOnAction(e -> showSettings());
        }
    }
    
    private void setupApiListeners() {
        if (btnCalculateElectricity != null) {
            btnCalculateElectricity.setOnAction(e -> calculateElectricityEmissions());
        }
        if (btnCalculateFuel != null) {
            btnCalculateFuel.setOnAction(e -> calculateFuelEmissions());
        }
        if (btnCalculateShipping != null) {
            btnCalculateShipping.setOnAction(e -> calculateShippingEmissions());
        }
        if (btnCheckAirQuality != null) {
            btnCheckAirQuality.setOnAction(e -> checkAirQuality());
        }
    }

    /**
     * Load air quality cache from disk (persists between app restarts)
     */
    private void loadCacheFromDisk() {
        try {
            java.io.File cacheFile = new java.io.File(CACHE_FILE);
            if (cacheFile.exists()) {
                try (java.io.ObjectInputStream ois = new java.io.ObjectInputStream(
                        new java.io.FileInputStream(cacheFile))) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, CachedAirQuality> loaded = 
                            (java.util.Map<String, CachedAirQuality>) ois.readObject();
                    
                    // Only load non-expired entries
                    long validCount = 0;
                    for (java.util.Map.Entry<String, CachedAirQuality> entry : loaded.entrySet()) {
                        if (!entry.getValue().isExpired()) {
                            airQualityCache.put(entry.getKey(), entry.getValue());
                            validCount++;
                        }
                    }
                    System.out.println("[AIR QUALITY] Loaded " + validCount + " cached entries from disk");
                }
            }
        } catch (Exception e) {
            System.err.println("[AIR QUALITY] Could not load cache from disk: " + e.getMessage());
        }
    }
    
    /**
     * Save air quality cache to disk (persist between app restarts)
     */
    private void saveCacheToDisk() {
        try {
            try (java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(
                    new java.io.FileOutputStream(CACHE_FILE))) {
                oos.writeObject(airQualityCache);
                System.out.println("[AIR QUALITY] Saved " + airQualityCache.size() + " entries to disk cache");
            }
        } catch (Exception e) {
            System.err.println("[AIR QUALITY] Could not save cache to disk: " + e.getMessage());
        }
    }
    
    /**
     * Get cached air quality data (returns fallback if not cached or expired)
     * NON-BLOCKING - does not make API calls
     */
    private int getCachedAirQuality(double lat, double lon, int fallbackAqi) {
        String cacheKey = String.format("%.2f_%.2f", lat, lon);
        
        CachedAirQuality cached = airQualityCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.aqi;
        }
        
        return fallbackAqi;
    }
    
    /**
     * Fetch real air quality data for a zone's center point ASYNCHRONOUSLY.
     * Does NOT block - returns immediately with cached/fallback data.
     * Real data will be fetched in background and map updated when ready.
     */
    private void fetchRealAirQualityAsync(double lat, double lon, int fallbackAqi, String zoneLabel) {
        String cacheKey = String.format("%.2f_%.2f", lat, lon);
        
        // Check cache first
        CachedAirQuality cached = airQualityCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return; // Already have valid data
        }
        
        // API not enabled - don't waste time trying
        if (!airQualityService.isEnabled()) {
            return;
        }
        
        // Fetch asynchronously in background thread
        airQualityExecutor.submit(() -> {
            try {
                AirPollutionResponse response = airQualityService.getCurrentAirQuality(lat, lon);
                if (response != null && response.getList() != null && !response.getList().isEmpty()) {
                    AirQualityData data = response.getList().get(0);
                    int owmAqi = data.getMain().getAqi();
                    int usEpaAqi = convertOwmAqiToUsEpa(owmAqi, data);
                    
                    // Cache the result
                    airQualityCache.put(cacheKey, new CachedAirQuality(usEpaAqi, lat, lon));
                    System.out.println(String.format("[AIR QUALITY] ✓ %s: %.2f,%.2f → AQI %d", 
                            zoneLabel, lat, lon, usEpaAqi));
                    
                    // Update map zone color in JavaFX thread
                    Platform.runLater(() -> updateMapZone(lat, lon, usEpaAqi));
                    
                    // Periodically save to disk
                    if (airQualityCache.size() % 10 == 0) {
                        saveCacheToDisk();
                    }
                }
            } catch (Exception e) {
                System.err.println("[AIR QUALITY] Error fetching " + zoneLabel + ": " + e.getMessage());
            }
        });
    }
    
    /**
     * Update a specific zone on the map with new AQI data (called from background thread)
     */
    private void updateMapZone(double lat, double lon, int aqi) {
        if (mapWebView == null || mapWebView.getEngine() == null) return;
        
        try {
            String script = String.format(
                "if (typeof updateZoneAqi === 'function') { updateZoneAqi(%.2f, %.2f, %d); }",
                lat, lon, aqi
            );
            mapWebView.getEngine().executeScript(script);
        } catch (Exception e) {
            // Ignore - map might not be fully loaded yet
        }
    }
    
    /**
     * Convert OpenWeatherMap AQI (1-5) to US EPA AQI (0-300+).
     * Uses pollutant concentrations for more accurate conversion.
     */
    private int convertOwmAqiToUsEpa(int owmAqi, AirQualityData data) {
        // Base conversion from OWM scale
        int baseAqi;
        switch (owmAqi) {
            case 1: baseAqi = 25; break;   // Good: 0-50
            case 2: baseAqi = 75; break;   // Fair: 51-100
            case 3: baseAqi = 125; break;  // Moderate: 101-150
            case 4: baseAqi = 175; break;  // Poor: 151-200
            case 5: baseAqi = 250; break;  // Very Poor: 201-300
            default: baseAqi = 100; break;
        }
        
        // Refine using PM2.5 concentration if available
        if (data.getComponents() != null && data.getComponents().getPm2_5() != null) {
            double pm25 = data.getComponents().getPm2_5();
            // US EPA PM2.5 breakpoints
            if (pm25 <= 12.0) baseAqi = Math.min(baseAqi, 50);
            else if (pm25 <= 35.4) baseAqi = (int)(51 + (pm25 - 12.1) * 49 / 23.3);
            else if (pm25 <= 55.4) baseAqi = (int)(101 + (pm25 - 35.5) * 49 / 19.9);
            else if (pm25 <= 150.4) baseAqi = (int)(151 + (pm25 - 55.5) * 49 / 94.9);
            else if (pm25 <= 250.4) baseAqi = (int)(201 + (pm25 - 150.5) * 99 / 99.9);
            else baseAqi = Math.max(baseAqi, 301);
        }
        
        return baseAqi;
    }
    
    /**
     * Start background fetching for all configured zones.
     * Called AFTER map loads - does not block UI.
     */
    private void startBackgroundAirQualityFetch() {
        if (!airQualityService.isEnabled()) {
            System.out.println("[AIR QUALITY] API disabled - using cached/fallback data only");
            return;
        }
        
        new Thread(() -> {
            try {
                System.out.println("[AIR QUALITY] Starting background fetch for expired zones...");
                
                // Give map time to fully load
                Thread.sleep(1000);
                
                // Count how many zones need updating
                long expiredCount = airQualityCache.values().stream()
                        .filter(CachedAirQuality::isExpired).count();
                
                if (expiredCount == 0 && !airQualityCache.isEmpty()) {
                    System.out.println("[AIR QUALITY] All zones have valid cache - no fetch needed");
                    return;
                }
                
                System.out.println("[AIR QUALITY] Fetching zones in background (4h cache)...");
                
                // North America (12 zones)
                fetchRealAirQualityAsync(49, -123, 75, "Vancouver");
                fetchRealAirQualityAsync(47.6, -122.3, 82, "Seattle");
                fetchRealAirQualityAsync(45.5, -122.7, 78, "Portland");
                fetchRealAirQualityAsync(37.8, -122.4, 95, "San Francisco");
                fetchRealAirQualityAsync(34, -118, 118, "Los Angeles");
                fetchRealAirQualityAsync(33.4, -112, 112, "Phoenix");
                fetchRealAirQualityAsync(29.8, -95.4, 105, "Houston");
                fetchRealAirQualityAsync(41.9, -87.6, 98, "Chicago");
                fetchRealAirQualityAsync(40.7, -74, 95, "New York");
                fetchRealAirQualityAsync(42.4, -71.1, 88, "Boston");
                fetchRealAirQualityAsync(43.7, -79.4, 92, "Toronto");
                fetchRealAirQualityAsync(19.4, -99.1, 145, "Mexico City");
                
                // South America (6 zones)
                fetchRealAirQualityAsync(10.5, -66.9, 88, "Caracas");
                fetchRealAirQualityAsync(4.7, -74, 95, "Bogotá");
                fetchRealAirQualityAsync(-12, -77, 92, "Lima");
                fetchRealAirQualityAsync(-23.5, -46.6, 105, "São Paulo");
                fetchRealAirQualityAsync(-22.9, -43.2, 98, "Rio de Janeiro");
                fetchRealAirQualityAsync(-34.6, -58.4, 88, "Buenos Aires");
                
                // Europe West (10 zones)
                fetchRealAirQualityAsync(51.5, -0.1, 78, "London");
                fetchRealAirQualityAsync(53.3, -6.3, 72, "Dublin");
                fetchRealAirQualityAsync(48.9, 2.3, 95, "Paris");
                fetchRealAirQualityAsync(50.8, 4.4, 88, "Brussels");
                fetchRealAirQualityAsync(52.4, 4.9, 82, "Amsterdam");
                fetchRealAirQualityAsync(52.5, 13.4, 102, "Berlin");
                fetchRealAirQualityAsync(50.1, 8.7, 98, "Frankfurt");
                fetchRealAirQualityAsync(48.1, 11.6, 105, "Munich");
                fetchRealAirQualityAsync(47.4, 8.5, 78, "Zurich");
                fetchRealAirQualityAsync(46.2, 6.1, 75, "Geneva");
                
                // Europe South (8 zones)
                fetchRealAirQualityAsync(40.4, -3.7, 112, "Madrid");
                fetchRealAirQualityAsync(41.4, 2.2, 105, "Barcelona");
                fetchRealAirQualityAsync(38.7, -9.1, 88, "Lisbon");
                fetchRealAirQualityAsync(41.9, 12.5, 118, "Rome");
                fetchRealAirQualityAsync(45.5, 9.2, 125, "Milan");
                fetchRealAirQualityAsync(40.9, 14.3, 115, "Naples");
                fetchRealAirQualityAsync(38, 23.7, 122, "Athens");
                fetchRealAirQualityAsync(41, 29, 138, "Istanbul");
                
                // Europe East (6 zones)
                fetchRealAirQualityAsync(52.2, 21, 108, "Warsaw");
                fetchRealAirQualityAsync(50.1, 14.4, 112, "Prague");
                fetchRealAirQualityAsync(48.2, 16.4, 105, "Vienna");
                fetchRealAirQualityAsync(47.5, 19.1, 118, "Budapest");
                fetchRealAirQualityAsync(44.4, 26.1, 125, "Bucharest");
                fetchRealAirQualityAsync(55.8, 37.6, 115, "Moscow");
                
                // North Africa & Middle East (8 zones)
                fetchRealAirQualityAsync(36.8, 10.2, 95, "Tunis");
                fetchRealAirQualityAsync(33.6, -7.6, 105, "Casablanca");
                fetchRealAirQualityAsync(30, 31.2, 185, "Cairo");
                fetchRealAirQualityAsync(33.9, 35.5, 112, "Beirut");
                fetchRealAirQualityAsync(31.8, 35.2, 102, "Jerusalem");
                fetchRealAirQualityAsync(33.3, 44.4, 198, "Baghdad");
                fetchRealAirQualityAsync(29.4, 47.9, 165, "Kuwait City");
                fetchRealAirQualityAsync(25.3, 55.3, 175, "Dubai");
                
                // Sub-Saharan Africa (6 zones)
                fetchRealAirQualityAsync(6.5, 3.4, 115, "Lagos");
                fetchRealAirQualityAsync(5.6, -0.2, 98, "Accra");
                fetchRealAirQualityAsync(-1.3, 36.8, 88, "Nairobi");
                fetchRealAirQualityAsync(-6.2, 35.7, 82, "Dar es Salaam");
                fetchRealAirQualityAsync(-26.2, 28, 68, "Johannesburg");
                fetchRealAirQualityAsync(-33.9, 18.4, 72, "Cape Town");
                
                // South Asia (7 zones)
                fetchRealAirQualityAsync(33.7, 73.1, 188, "Islamabad");
                fetchRealAirQualityAsync(31.6, 74.3, 225, "Lahore");
                fetchRealAirQualityAsync(24.9, 67, 195, "Karachi");
                fetchRealAirQualityAsync(28.6, 77.2, 235, "Delhi");
                fetchRealAirQualityAsync(19.1, 72.9, 198, "Mumbai");
                fetchRealAirQualityAsync(13, 80.2, 175, "Chennai");
                fetchRealAirQualityAsync(22.6, 88.4, 205, "Kolkata");
                
                // Southeast Asia (7 zones)
                fetchRealAirQualityAsync(13.8, 100.5, 165, "Bangkok");
                fetchRealAirQualityAsync(21, 105.8, 155, "Hanoi");
                fetchRealAirQualityAsync(10.8, 106.7, 148, "Ho Chi Minh");
                fetchRealAirQualityAsync(1.3, 103.8, 125, "Singapore");
                fetchRealAirQualityAsync(3.1, 101.7, 142, "Kuala Lumpur");
                fetchRealAirQualityAsync(-6.2, 106.8, 158, "Jakarta");
                fetchRealAirQualityAsync(14.6, 121, 138, "Manila");
                
                // East Asia (9 zones)
                fetchRealAirQualityAsync(39.9, 116.4, 215, "Beijing");
                fetchRealAirQualityAsync(31.2, 121.5, 188, "Shanghai");
                fetchRealAirQualityAsync(23.1, 113.3, 178, "Guangzhou");
                fetchRealAirQualityAsync(30.6, 104.1, 168, "Chengdu");
                fetchRealAirQualityAsync(22.3, 114.2, 145, "Hong Kong");
                fetchRealAirQualityAsync(25, 121.5, 118, "Taipei");
                fetchRealAirQualityAsync(35.7, 139.7, 92, "Tokyo");
                fetchRealAirQualityAsync(34.7, 135.5, 88, "Osaka");
                fetchRealAirQualityAsync(37.6, 127, 118, "Seoul");
                
                // Oceania (4 zones)
                fetchRealAirQualityAsync(-33.9, 151.2, 62, "Sydney");
                fetchRealAirQualityAsync(-37.8, 144.9, 58, "Melbourne");
                fetchRealAirQualityAsync(-27.5, 153, 65, "Brisbane");
                fetchRealAirQualityAsync(-41.3, 174.8, 45, "Wellington");
                
                System.out.println("[AIR QUALITY] All 80+ zone fetches scheduled - using 4h cache to minimize API calls");
                
            } catch (Exception e) {
                System.err.println("[AIR QUALITY] Error in background fetch: " + e.getMessage());
            }
        }).start();
    }
    
    private void setupMapWebView() {
        System.out.println("[MAP SETUP] Starting setupMapWebView()...");
        
        if (mapWebView == null) {
            System.err.println("[MAP SETUP] ERROR: mapWebView is NULL! Check FXML binding.");
            return;
        }

        System.out.println("[MAP SETUP] mapWebView found, initializing...");
        
        // Initialize WebEngine
        mapWebView.getEngine().setJavaScriptEnabled(true);
        
        System.out.println("[GREEN WALLET] Loading map instantly with cached data...");
        System.out.println("[GREEN WALLET] API Status: " + (airQualityService.isEnabled() ? "ENABLED (background fetch)" : "DISABLED"));
        System.out.println("[GREEN WALLET] Cached zones: " + airQualityCache.size() + " (4h TTL)");

        // Create HTML with Leaflet map and global pollution zones
        // OPTIMIZED: Loads instantly with cached data, fetches real data in background
        String htmlContent = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "  <meta charset='utf-8' />\n" +
                "  <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n" +
                "  <link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/leaflet.min.css' />\n" +
                "  <script src='https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/leaflet.min.js'></script>\n" +
                "  <style>\n" +
                "    body { margin: 0; padding: 0; font-family: Arial, sans-serif; }\n" +
                "    #map { position: absolute; top: 0; bottom: 0; width: 100%; }\n" +
                "    .legend { background: white; padding: 10px; border-radius: 5px; box-shadow: 0 0 15px rgba(0,0,0,0.2); }\n" +
                "    .legend-item { display: flex; align-items: center; margin: 5px 0; }\n" +
                "    .legend-color { width: 20px; height: 20px; margin-right: 10px; border-radius: 3px; }\n" +
                "    .legend-label { font-size: 12px; }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div id='map'></div>\n" +
                "  <script>\n" +
                "    var map = L.map('map').setView([30, 0], 2);\n" +
                "    var zoneRectangles = {}; // Store rectangles by lat,lon key\n" +
                "    \n" +
                "    // Base map layer\n" +
                "    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {\n" +
                "      attribution: '© OpenStreetMap',\n" +
                "      maxZoom: 19\n" +
                "    }).addTo(map);\n" +
                "    \n" +
                "    // Color function based on AQI\n" +
                "    function getColor(aqi) {\n" +
                "      if (aqi <= 50) return '#00C9A7';      // Good\n" +
                "      if (aqi <= 100) return '#FFD700';     // Moderate\n" +
                "      if (aqi <= 150) return '#FFA500';     // Unhealthy for Groups\n" +
                "      if (aqi <= 200) return '#FF6B6B';     // Unhealthy\n" +
                "      if (aqi <= 300) return '#CC1133';     // Very Unhealthy\n" +
                "      return '#660000';                      // Hazardous\n" +
                "    }\n" +
                "    \n" +
                "    // Update zone AQI dynamically (called from Java when background fetch completes)\n" +
                "    function updateZoneAqi(lat, lon, newAqi) {\n" +
                "      var key = lat.toFixed(2) + '_' + lon.toFixed(2);\n" +
                "      var rect = zoneRectangles[key];\n" +
                "      if (rect) {\n" +
                "        var newColor = getColor(newAqi);\n" +
                "        rect.setStyle({ color: newColor, fillColor: newColor });\n" +
                "        console.log('Updated zone ' + key + ' to AQI ' + newAqi + ' (' + newColor + ')');\n" +
                "      }\n" +
                "    }\n" +
                "    \n" +
                "    // OPTIMIZED: 80+ global cities with 4h cache\n" +
                "    // Loads INSTANTLY with cached/fallback data\n" +
                "    // Background thread updates with real API data\n" +
                "    var globalZones = [\n" +
                "      // North America (12 zones)\n" +
                "      { bounds: [[47, -125], [51, -121]], lat: 49.3, lon: -123.1, aqi: " + getCachedAirQuality(49.3, -123.1, 75) + ", label: 'Vancouver' },\n" +
                "      { bounds: [[45.5, -124], [49.5, -120]], lat: 47.6, lon: -122.3, aqi: " + getCachedAirQuality(47.6, -122.3, 82) + ", label: 'Seattle' },\n" +
                "      { bounds: [[43.5, -124.5], [47.5, -120.5]], lat: 45.5, lon: -122.7, aqi: " + getCachedAirQuality(45.5, -122.7, 78) + ", label: 'Portland' },\n" +
                "      { bounds: [[35.5, -124.5], [39.5, -120.5]], lat: 37.8, lon: -122.4, aqi: " + getCachedAirQuality(37.8, -122.4, 95) + ", label: 'San Francisco' },\n" +
                "      { bounds: [[32, -120], [36, -116]], lat: 34.1, lon: -118.2, aqi: " + getCachedAirQuality(34.1, -118.2, 118) + ", label: 'Los Angeles' },\n" +
                "      { bounds: [[31.5, -114.5], [35.5, -110.5]], lat: 33.4, lon: -112.1, aqi: " + getCachedAirQuality(33.4, -112.1, 125) + ", label: 'Phoenix' },\n" +
                "      { bounds: [[27.7, -97.5], [31.7, -93.5]], lat: 29.8, lon: -95.4, aqi: " + getCachedAirQuality(29.8, -95.4, 105) + ", label: 'Houston' },\n" +
                "      { bounds: [[39.8, -89.8], [43.8, -85.8]], lat: 41.9, lon: -87.6, aqi: " + getCachedAirQuality(41.9, -87.6, 98) + ", label: 'Chicago' },\n" +
                "      { bounds: [[38.7, -76.1], [42.7, -72.1]], lat: 40.7, lon: -74.0, aqi: " + getCachedAirQuality(40.7, -74.0, 92) + ", label: 'New York' },\n" +
                "      { bounds: [[40.4, -73.2], [44.4, -69.2]], lat: 42.4, lon: -71.1, aqi: " + getCachedAirQuality(42.4, -71.1, 85) + ", label: 'Boston' },\n" +
                "      { bounds: [[41.6, -81.7], [45.6, -77.7]], lat: 43.7, lon: -79.4, aqi: " + getCachedAirQuality(43.7, -79.4, 88) + ", label: 'Toronto' },\n" +
                "      { bounds: [[17.4, -101.1], [21.4, -97.1]], lat: 19.4, lon: -99.1, aqi: " + getCachedAirQuality(19.4, -99.1, 135) + ", label: 'Mexico City' },\n" +
                "      \n" +
                "      // South America (6 zones)\n" +
                "      { bounds: [[8.4, -68.9], [12.4, -64.9]], lat: 10.5, lon: -66.9, aqi: " + getCachedAirQuality(10.5, -66.9, 112) + ", label: 'Caracas' },\n" +
                "      { bounds: [[2.4, -76.2], [6.4, -72.2]], lat: 4.7, lon: -74.1, aqi: " + getCachedAirQuality(4.7, -74.1, 108) + ", label: 'Bogotá' },\n" +
                "      { bounds: [[-14, -79], [-10, -75]], lat: -12.0, lon: -77.0, aqi: " + getCachedAirQuality(-12.0, -77.0, 105) + ", label: 'Lima' },\n" +
                "      { bounds: [[-25.5, -48.6], [-21.5, -44.6]], lat: -23.5, lon: -46.6, aqi: " + getCachedAirQuality(-23.5, -46.6, 95) + ", label: 'São Paulo' },\n" +
                "      { bounds: [[-24.9, -45.5], [-20.9, -41.5]], lat: -22.9, lon: -43.2, aqi: " + getCachedAirQuality(-22.9, -43.2, 88) + ", label: 'Rio de Janeiro' },\n" +
                "      { bounds: [[-36.6, -60.4], [-32.6, -56.4]], lat: -34.6, lon: -58.4, aqi: " + getCachedAirQuality(-34.6, -58.4, 78) + ", label: 'Buenos Aires' },\n" +
                "      \n" +
                "      // Europe West (10 zones)\n" +
                "      { bounds: [[49.5, -2.1], [53.5, 1.9]], lat: 51.5, lon: -0.1, aqi: " + getCachedAirQuality(51.5, -0.1, 72) + ", label: 'London' },\n" +
                "      { bounds: [[51.3, -8.3], [55.3, -4.3]], lat: 53.3, lon: -6.3, aqi: " + getCachedAirQuality(53.3, -6.3, 65) + ", label: 'Dublin' },\n" +
                "      { bounds: [[46.9, 0.4], [50.9, 4.4]], lat: 48.9, lon: 2.4, aqi: " + getCachedAirQuality(48.9, 2.4, 88) + ", label: 'Paris' },\n" +
                "      { bounds: [[48.8, 2.3], [52.8, 6.3]], lat: 50.8, lon: 4.3, aqi: " + getCachedAirQuality(50.8, 4.3, 82) + ", label: 'Brussels' },\n" +
                "      { bounds: [[50.4, 2.9], [54.4, 6.9]], lat: 52.4, lon: 4.9, aqi: " + getCachedAirQuality(52.4, 4.9, 75) + ", label: 'Amsterdam' },\n" +
                "      { bounds: [[50.5, 11.4], [54.5, 15.4]], lat: 52.5, lon: 13.4, aqi: " + getCachedAirQuality(52.5, 13.4, 98) + ", label: 'Berlin' },\n" +
                "      { bounds: [[48.1, 6.7], [52.1, 10.7]], lat: 50.1, lon: 8.7, aqi: " + getCachedAirQuality(50.1, 8.7, 92) + ", label: 'Frankfurt' },\n" +
                "      { bounds: [[46.1, 9.5], [50.1, 13.5]], lat: 48.1, lon: 11.6, aqi: " + getCachedAirQuality(48.1, 11.6, 95) + ", label: 'Munich' },\n" +
                "      { bounds: [[45.4, 6.1], [49.4, 10.1]], lat: 47.4, lon: 8.5, aqi: " + getCachedAirQuality(47.4, 8.5, 68) + ", label: 'Zurich' },\n" +
                "      { bounds: [[44.2, 4.1], [48.2, 8.1]], lat: 46.2, lon: 6.1, aqi: " + getCachedAirQuality(46.2, 6.1, 70) + ", label: 'Geneva' },\n" +
                "      \n" +
                "      // Europe South (8 zones)\n" +
                "      { bounds: [[38.4, -5.7], [42.4, -1.7]], lat: 40.4, lon: -3.7, aqi: " + getCachedAirQuality(40.4, -3.7, 108) + ", label: 'Madrid' },\n" +
                "      { bounds: [[39.1, 0.1], [43.1, 4.1]], lat: 41.4, lon: 2.2, aqi: " + getCachedAirQuality(41.4, 2.2, 102) + ", label: 'Barcelona' },\n" +
                "      { bounds: [[36.7, -11.1], [40.7, -7.1]], lat: 38.7, lon: -9.1, aqi: " + getCachedAirQuality(38.7, -9.1, 85) + ", label: 'Lisbon' },\n" +
                "      { bounds: [[39.9, 10.3], [43.9, 14.3]], lat: 41.9, lon: 12.5, aqi: " + getCachedAirQuality(41.9, 12.5, 118) + ", label: 'Rome' },\n" +
                "      { bounds: [[43.4, 7.3], [47.4, 11.3]], lat: 45.5, lon: 9.2, aqi: " + getCachedAirQuality(45.5, 9.2, 115) + ", label: 'Milan' },\n" +
                "      { bounds: [[38.9, 12.2], [42.9, 16.2]], lat: 40.9, lon: 14.3, aqi: " + getCachedAirQuality(40.9, 14.3, 125) + ", label: 'Naples' },\n" +
                "      { bounds: [[36, 21.7], [40, 25.7]], lat: 38.0, lon: 23.7, aqi: " + getCachedAirQuality(38.0, 23.7, 138) + ", label: 'Athens' },\n" +
                "      { bounds: [[39, 27], [43, 31]], lat: 41.0, lon: 29.0, aqi: " + getCachedAirQuality(41.0, 29.0, 132) + ", label: 'Istanbul' },\n" +
                "      \n" +
                "      // Europe East (6 zones)\n" +
                "      { bounds: [[50.2, 19], [54.2, 23]], lat: 52.2, lon: 21.0, aqi: " + getCachedAirQuality(52.2, 21.0, 125) + ", label: 'Warsaw' },\n" +
                "      { bounds: [[48.1, 12.4], [52.1, 16.4]], lat: 50.1, lon: 14.4, aqi: " + getCachedAirQuality(50.1, 14.4, 115) + ", label: 'Prague' },\n" +
                "      { bounds: [[46.2, 14.4], [50.2, 18.4]], lat: 48.2, lon: 16.4, aqi: " + getCachedAirQuality(48.2, 16.4, 105) + ", label: 'Vienna' },\n" +
                "      { bounds: [[45.5, 17], [49.5, 21]], lat: 47.5, lon: 19.0, aqi: " + getCachedAirQuality(47.5, 19.0, 118) + ", label: 'Budapest' },\n" +
                "      { bounds: [[42.1, 24.1], [46.1, 28.1]], lat: 44.4, lon: 26.1, aqi: " + getCachedAirQuality(44.4, 26.1, 128) + ", label: 'Bucharest' },\n" +
                "      { bounds: [[53.8, 35.6], [57.8, 39.6]], lat: 55.8, lon: 37.6, aqi: " + getCachedAirQuality(55.8, 37.6, 142) + ", label: 'Moscow' },\n" +
                "      \n" +
                "      // North Africa & Middle East (8 zones)\n" +
                "      { bounds: [[34.8, 8.8], [38.8, 12.8]], lat: 36.8, lon: 10.2, aqi: " + getCachedAirQuality(36.8, 10.2, 95) + ", label: 'Tunis' },\n" +
                "      { bounds: [[31.6, -9.6], [35.6, -5.6]], lat: 33.6, lon: -7.6, aqi: " + getCachedAirQuality(33.6, -7.6, 102) + ", label: 'Casablanca' },\n" +
                "      { bounds: [[28, 29.2], [32, 33.2]], lat: 30.0, lon: 31.2, aqi: " + getCachedAirQuality(30.0, 31.2, 165) + ", label: 'Cairo' },\n" +
                "      { bounds: [[31.9, 33.3], [35.9, 37.3]], lat: 33.9, lon: 35.5, aqi: " + getCachedAirQuality(33.9, 35.5, 148) + ", label: 'Beirut' },\n" +
                "      { bounds: [[29.8, 33.1], [33.8, 37.1]], lat: 31.8, lon: 35.2, aqi: " + getCachedAirQuality(31.8, 35.2, 155) + ", label: 'Jerusalem' },\n" +
                "      { bounds: [[31.3, 42.4], [35.3, 46.4]], lat: 33.3, lon: 44.4, aqi: " + getCachedAirQuality(33.3, 44.4, 175) + ", label: 'Baghdad' },\n" +
                "      { bounds: [[27.2, 45.5], [31.2, 49.5]], lat: 29.3, lon: 48.0, aqi: " + getCachedAirQuality(29.3, 48.0, 168) + ", label: 'Kuwait City' },\n" +
                "      { bounds: [[23.3, 53.3], [27.3, 57.3]], lat: 25.3, lon: 55.3, aqi: " + getCachedAirQuality(25.3, 55.3, 158) + ", label: 'Dubai' },\n" +
                "      \n" +
                "      // Sub-Saharan Africa (6 zones)\n" +
                "      { bounds: [[4.5, 1.5], [8.5, 5.5]], lat: 6.5, lon: 3.4, aqi: " + getCachedAirQuality(6.5, 3.4, 142) + ", label: 'Lagos' },\n" +
                "      { bounds: [[3.7, -2.3], [7.7, 1.7]], lat: 5.6, lon: -0.2, aqi: " + getCachedAirQuality(5.6, -0.2, 135) + ", label: 'Accra' },\n" +
                "      { bounds: [[-3.3, 34.7], [0.7, 38.7]], lat: -1.3, lon: 36.8, aqi: " + getCachedAirQuality(-1.3, 36.8, 118) + ", label: 'Nairobi' },\n" +
                "      { bounds: [[-8.9, 37.7], [-4.9, 41.7]], lat: -6.8, lon: 39.3, aqi: " + getCachedAirQuality(-6.8, 39.3, 108) + ", label: 'Dar es Salaam' },\n" +
                "      { bounds: [[-28.2, 26], [-24.2, 30]], lat: -26.2, lon: 28.0, aqi: " + getCachedAirQuality(-26.2, 28.0, 82) + ", label: 'Johannesburg' },\n" +
                "      { bounds: [[-35.9, 16.4], [-31.9, 20.4]], lat: -33.9, lon: 18.4, aqi: " + getCachedAirQuality(-33.9, 18.4, 68) + ", label: 'Cape Town' },\n" +
                "      \n" +
                "      // South Asia (7 zones)\n" +
                "      { bounds: [[31.5, 71], [35.5, 75]], lat: 33.7, lon: 73.1, aqi: " + getCachedAirQuality(33.7, 73.1, 195) + ", label: 'Islamabad' },\n" +
                "      { bounds: [[29.5, 72.3], [33.5, 76.3]], lat: 31.5, lon: 74.3, aqi: " + getCachedAirQuality(31.5, 74.3, 205) + ", label: 'Lahore' },\n" +
                "      { bounds: [[22.9, 65], [26.9, 69]], lat: 24.9, lon: 67.1, aqi: " + getCachedAirQuality(24.9, 67.1, 188) + ", label: 'Karachi' },\n" +
                "      { bounds: [[26.6, 75.2], [30.6, 79.2]], lat: 28.6, lon: 77.2, aqi: " + getCachedAirQuality(28.6, 77.2, 215) + ", label: 'New Delhi' },\n" +
                "      { bounds: [[17.1, 70.9], [21.1, 74.9]], lat: 19.1, lon: 72.9, aqi: " + getCachedAirQuality(19.1, 72.9, 198) + ", label: 'Mumbai' },\n" +
                "      { bounds: [[11, 78.1], [15, 82.1]], lat: 13.1, lon: 80.3, aqi: " + getCachedAirQuality(13.1, 80.3, 175) + ", label: 'Chennai' },\n" +
                "      { bounds: [[20.6, 86.2], [24.6, 90.2]], lat: 22.6, lon: 88.4, aqi: " + getCachedAirQuality(22.6, 88.4, 182) + ", label: 'Kolkata' },\n" +
                "      \n" +
                "      // Southeast Asia (7 zones)\n" +
                "      { bounds: [[11.8, 98.5], [15.8, 102.5]], lat: 13.8, lon: 100.5, aqi: " + getCachedAirQuality(13.8, 100.5, 168) + ", label: 'Bangkok' },\n" +
                "      { bounds: [[19, 103.8], [23, 107.8]], lat: 21.0, lon: 105.8, aqi: " + getCachedAirQuality(21.0, 105.8, 155) + ", label: 'Hanoi' },\n" +
                "      { bounds: [[8.6, 104.6], [12.6, 108.6]], lat: 10.8, lon: 106.7, aqi: " + getCachedAirQuality(10.8, 106.7, 162) + ", label: 'Ho Chi Minh' },\n" +
                "      { bounds: [[-0.3, 101.7], [3.7, 105.7]], lat: 1.4, lon: 103.8, aqi: " + getCachedAirQuality(1.4, 103.8, 125) + ", label: 'Singapore' },\n" +
                "      { bounds: [[1.2, 99.6], [5.2, 103.6]], lat: 3.1, lon: 101.7, aqi: " + getCachedAirQuality(3.1, 101.7, 142) + ", label: 'Kuala Lumpur' },\n" +
                "      { bounds: [[-8.2, 104.8], [-4.2, 108.8]], lat: -6.2, lon: 106.8, aqi: " + getCachedAirQuality(-6.2, 106.8, 178) + ", label: 'Jakarta' },\n" +
                "      { bounds: [[12.6, 119], [16.6, 123]], lat: 14.6, lon: 121.0, aqi: " + getCachedAirQuality(14.6, 121.0, 165) + ", label: 'Manila' },\n" +
                "      \n" +
                "      // East Asia (9 zones)\n" +
                "      { bounds: [[37.9, 114.4], [41.9, 118.4]], lat: 39.9, lon: 116.4, aqi: " + getCachedAirQuality(39.9, 116.4, 195) + ", label: 'Beijing' },\n" +
                "      { bounds: [[29.2, 119.5], [33.2, 123.5]], lat: 31.2, lon: 121.5, aqi: " + getCachedAirQuality(31.2, 121.5, 185) + ", label: 'Shanghai' },\n" +
                "      { bounds: [[21.1, 111.1], [25.1, 115.1]], lat: 23.1, lon: 113.3, aqi: " + getCachedAirQuality(23.1, 113.3, 175) + ", label: 'Guangzhou' },\n" +
                "      { bounds: [[28.7, 102.1], [32.7, 106.1]], lat: 30.7, lon: 104.1, aqi: " + getCachedAirQuality(30.7, 104.1, 182) + ", label: 'Chengdu' },\n" +
                "      { bounds: [[20.3, 112.1], [24.3, 116.1]], lat: 22.3, lon: 114.2, aqi: " + getCachedAirQuality(22.3, 114.2, 165) + ", label: 'Hong Kong' },\n" +
                "      { bounds: [[23, 119.3], [27, 123.3]], lat: 25.0, lon: 121.6, aqi: " + getCachedAirQuality(25.0, 121.6, 128) + ", label: 'Taipei' },\n" +
                "      { bounds: [[33.7, 137.7], [37.7, 141.7]], lat: 35.7, lon: 139.7, aqi: " + getCachedAirQuality(35.7, 139.7, 105) + ", label: 'Tokyo' },\n" +
                "      { bounds: [[32.7, 133.5], [36.7, 137.5]], lat: 34.7, lon: 135.5, aqi: " + getCachedAirQuality(34.7, 135.5, 98) + ", label: 'Osaka' },\n" +
                "      { bounds: [[35.6, 125], [39.6, 129]], lat: 37.6, lon: 127.0, aqi: " + getCachedAirQuality(37.6, 127.0, 115) + ", label: 'Seoul' },\n" +
                "      \n" +
                "      // Oceania (4 zones)\n" +
                "      { bounds: [[-35.9, 149.2], [-31.9, 153.2]], lat: -33.9, lon: 151.2, aqi: " + getCachedAirQuality(-33.9, 151.2, 62) + ", label: 'Sydney' },\n" +
                "      { bounds: [[-39.9, 142.9], [-35.9, 146.9]], lat: -37.8, lon: 145.0, aqi: " + getCachedAirQuality(-37.8, 145.0, 58) + ", label: 'Melbourne' },\n" +
                "      { bounds: [[-29.5, 151.2], [-25.5, 155.2]], lat: -27.5, lon: 153.0, aqi: " + getCachedAirQuality(-27.5, 153.0, 55) + ", label: 'Brisbane' },\n" +
                "      { bounds: [[-43.3, 172.8], [-39.3, 176.8]], lat: -41.3, lon: 174.8, aqi: " + getCachedAirQuality(-41.3, 174.8, 45) + ", label: 'Wellington' }\n" +
                "    ];\n" +
                "    \n" +
                "    // Add rectangles for each zone (store refs for dynamic updates)\n" +
                "    globalZones.forEach(function(zone) {\n" +
                "      var rectangle = L.rectangle(zone.bounds, {\n" +
                "        color: getColor(zone.aqi),\n" +
                "        fillColor: getColor(zone.aqi),\n" +
                "        fillOpacity: 0.45,\n" +
                "        weight: 2,\n" +
                "        opacity: 0.8\n" +
                "      }).addTo(map);\n" +
                "      \n" +
                "      // Store reference for dynamic updates\n" +
                "      if (zone.lat && zone.lon) {\n" +
                "        var key = zone.lat.toFixed(2) + '_' + zone.lon.toFixed(2);\n" +
                "        zoneRectangles[key] = rectangle;\n" +
                "      }\n" +
                "      \n" +
                "      rectangle.bindPopup(\n" +
                "        '<div style=\"font-weight: bold; font-size: 13px;\">' + zone.label + '</div>' +\n" +
                "        '<div style=\"margin-top: 5px;\">AQI: <strong>' + zone.aqi + '</strong></div>' +\n" +
                "        '<div style=\"font-size: 11px; margin-top: 5px; color: ' + getColor(zone.aqi) + ';\">' +\n" +
                "        (zone.aqi <= 50 ? '✓ Bonne Qualité' : \n" +
                "         zone.aqi <= 100 ? '◐ Modéré' :\n" +
                "         zone.aqi <= 150 ? '⚠ Mauvais' :\n" +
                "         zone.aqi <= 200 ? '⛔ Très Mauvais' :\n" +
                "         zone.aqi <= 300 ? '☢ Dangereux' : '☠ Très Dangereux') +\n" +
                "        '</div>' +\n" +
                "        '<div style=\"font-size: 10px; margin-top: 8px; opacity: 0.7;\">📡 Données temps réel (cache 1h)</div>'\n" +
                "      );\n" +
                "    });\n" +
                "    \n" +
                "    // Add legend\n" +
                "    var legend = L.control({position: 'bottomright'});\n" +
                "    legend.onAdd = function(map) {\n" +
                "      var div = L.DomUtil.create('div', 'legend');\n" +
                "      div.innerHTML = '<div style=\"font-weight: bold; margin-bottom: 8px;\">Pollution Mondiale</div>';\n" +
                "      var labels = [\n" +
                "        {color: '#00C9A7', label: 'Bon (0-50)'},\n" +
                "        {color: '#FFD700', label: 'Modéré (51-100)'},\n" +
                "        {color: '#FFA500', label: 'Mauvais (101-150)'},\n" +
                "        {color: '#FF6B6B', label: 'Très Mauvais (151-200)'},\n" +
                "        {color: '#CC1133', label: 'Dangereux (201-300)'},\n" +
                "        {color: '#660000', label: 'Très Dangereux (301+)'}\n" +
                "      ];\n" +
                "      labels.forEach(function(item) {\n" +
                "        div.innerHTML += '<div class=\"legend-item\"><div class=\"legend-color\" style=\"background:' + item.color + '\"></div><div class=\"legend-label\">' + item.label + '</div></div>';\n" +
                "      });\n" +
                "      div.innerHTML += '<div style=\"font-size: 10px; margin-top: 8px; opacity: 0.6;\">📡 Données temps réel (cache 1h)</div>';\n" +
                "      return div;\n" +
                "    };\n" +
                "    legend.addTo(map);\n" +
                "  </script>\n" +
                "</body>\n" +
                "</html>";

        try {
            System.out.println("[MAP SETUP] Loading HTML content into WebView...");
            mapWebView.getEngine().loadContent(htmlContent);
            
            // Listen for page load completion to trigger background fetch
            mapWebView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    System.out.println("[MAP SETUP] ✓ HTML loaded successfully - map should be visible now");
                    System.out.println("[MAP SETUP] Starting background air quality fetch...");
                    startBackgroundAirQualityFetch();
                } else if (newState == javafx.concurrent.Worker.State.FAILED) {
                    System.err.println("[MAP SETUP] ✗ HTML load FAILED!");
                    Throwable exception = mapWebView.getEngine().getLoadWorker().getException();
                    if (exception != null) {
                        System.err.println("[MAP SETUP] Error: " + exception.getMessage());
                        exception.printStackTrace();
                    }
                }
            });
            
            System.out.println("[MAP SETUP] HTML content submitted to WebView engine");
        } catch (Exception e) {
            System.err.println("[MAP SETUP] Error initializing map: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== WALLET LOADING ====================

    private void loadWallets() {
        System.out.println("[LOAD WALLETS] Loading wallets...");
        try {
            List<Wallet> wallets = getScopedWallets();
            System.out.println("[LOAD WALLETS] Found " + wallets.size() + " wallets");
            
            ObservableList<Wallet> walletList = FXCollections.observableArrayList(wallets);
            cmbWalletSelector.setItems(walletList);
            
            // Select first wallet if available
            if (!wallets.isEmpty()) {
                System.out.println("[LOAD WALLETS] Selecting first wallet: " + wallets.get(0).getId());
                cmbWalletSelector.getSelectionModel().select(0);
            } else {
                System.err.println("[LOAD WALLETS] No wallets found!");
                currentWallet = null;
                clearWalletDisplay();
            }
        } catch (Exception e) {
            System.err.println("[LOAD WALLETS] Error: " + e.getMessage());
            e.printStackTrace();
            showError("Erreur lors du chargement des wallets", e.getMessage());
        }
    }

    private void loadWallet(int walletId) {
        System.out.println("[LOAD WALLET] Loading wallet ID: " + walletId);
        try {
            currentWallet = walletService.getWalletById(walletId);
            if (currentWallet != null) {
                System.out.println("[LOAD WALLET] ✓ Wallet loaded successfully");
                updateWalletDisplay();
                loadTransactions();
            } else {
                System.err.println("[LOAD WALLET] ✗ Wallet is NULL after loading!");
            }
        } catch (Exception e) {
            System.err.println("[LOAD WALLET] ✗ Error loading wallet: " + e.getMessage());
            e.printStackTrace();
            showError("Erreur lors du chargement du wallet", e.getMessage());
        }
    }

    private void updateWalletDisplay() {
        System.out.println("[WALLET DISPLAY] Updating wallet display...");
        
        if (currentWallet == null) {
            System.err.println("[WALLET DISPLAY] currentWallet is NULL - clearing display");
            clearWalletDisplay();
            return;
        }
        
        System.out.println("[WALLET DISPLAY] Wallet ID: " + currentWallet.getId());
        System.out.println("[WALLET DISPLAY] Wallet Name: " + currentWallet.getName());
        System.out.println("[WALLET DISPLAY] Available Credits: " + currentWallet.getAvailableCredits());
        System.out.println("[WALLET DISPLAY] Retired Credits: " + currentWallet.getRetiredCredits());
        System.out.println("[WALLET DISPLAY] Total Credits: " + currentWallet.getTotalCredits());
        
        lblWalletNumber.setText(formatWalletNumber(currentWallet.getWalletNumber()));
        lblHolderName.setText(currentWallet.getName() != null ? currentWallet.getName() : "Unnamed Wallet");
        lblOwnerType.setText(currentWallet.getOwnerType());
        lblStatus.setText("Active");
        
        // Update credit stats
        lblAvailableCredits.setText(formatCredits(currentWallet.getAvailableCredits()));
        lblRetiredCredits.setText(formatCredits(currentWallet.getRetiredCredits()));
        if (lblTotalCredits != null) {
            lblTotalCredits.setText(formatCredits(currentWallet.getTotalCredits()));
        }

        // Update sidebar quick stats
        if (lblSidebarAvailable != null) {
            lblSidebarAvailable.setText(formatCredits(currentWallet.getAvailableCredits()));
        }
        if (lblSidebarRetired != null) {
            lblSidebarRetired.setText(formatCredits(currentWallet.getRetiredCredits()));
        }
        if (lblSidebarGoal != null) {
            lblSidebarGoal.setText("Goal: " + formatCredits(currentWallet.getTotalCredits()));
        }

        // Update scope breakdown with sample data
        System.out.println("[WALLET DISPLAY] Calling updateScopeBreakdown()...");
        updateScopeBreakdown();

        // Update peer ranking
        if (lblPeerRank != null) {
            lblPeerRank.setText("Top 23%");
        }
        
        System.out.println("[WALLET DISPLAY] Update complete");
    }

    private void updateScopeBreakdown() {
        System.out.println("[SCOPE BREAKDOWN] Updating scope breakdown...");
        
        if (currentWallet == null) {
            System.err.println("[SCOPE BREAKDOWN] ERROR: currentWallet is NULL!");
            return;
        }
        
        // Calculate scope breakdown (sample calculation)
        double total = currentWallet.getTotalCredits();
        System.out.println("[SCOPE BREAKDOWN] Total credits: " + total);
        
        double scope1 = total * 0.26; // 26%
        double scope2 = total * 0.36; // 36%
        double scope3 = total * 0.38; // 38%

        System.out.println(String.format("[SCOPE BREAKDOWN] Scope 1: %.1f, Scope 2: %.1f, Scope 3: %.1f", 
                scope1, scope2, scope3));

        if (lblScope1Amount != null) {
            lblScope1Amount.setText(String.format("%.1f tCO₂e", scope1));
            System.out.println("[SCOPE BREAKDOWN] ✓ lblScope1Amount updated");
        } else {
            System.err.println("[SCOPE BREAKDOWN] ✗ lblScope1Amount is NULL!");
        }
        
        if (lblScope2Amount != null) {
            lblScope2Amount.setText(String.format("%.1f tCO₂e", scope2));
            System.out.println("[SCOPE BREAKDOWN] ✓ lblScope2Amount updated");
        } else {
            System.err.println("[SCOPE BREAKDOWN] ✗ lblScope2Amount is NULL!");
        }
        
        if (lblScope3Amount != null) {
            lblScope3Amount.setText(String.format("%.1f tCO₂e", scope3));
            System.out.println("[SCOPE BREAKDOWN] ✓ lblScope3Amount updated");
        } else {
            System.err.println("[SCOPE BREAKDOWN] ✗ lblScope3Amount is NULL!");
        }

        // Update impact alert - calculate from available data
        if (lblImpactAmount != null) {
            // Calculate impact as the total emissions (sum of scopes)
            double impact = scope1 + scope2 + scope3;
            // Show emissions above baseline (simulated as 20% of total for now)
            double aboveBaseline = impact * 0.2;

            if (aboveBaseline > 0.05) {
                lblImpactAmount.setText(String.format("+%.1f tCO₂e", aboveBaseline));
                lblImpactAmount.setStyle("-fx-font-size: 22px; -fx-font-weight: 700; -fx-text-fill: #dc2626;");
            } else if (aboveBaseline < -0.05) {
                lblImpactAmount.setText(String.format("%.1f tCO₂e", aboveBaseline));
                lblImpactAmount.setStyle("-fx-font-size: 22px; -fx-font-weight: 700; -fx-text-fill: #16a34a;");
            } else {
                lblImpactAmount.setText("0.0 tCO₂e");
                lblImpactAmount.setStyle("-fx-font-size: 22px; -fx-font-weight: 700; -fx-text-fill: #6b7280;");
            }

            System.out.println("[SCOPE BREAKDOWN] Impact amount updated: " + lblImpactAmount.getText());
        }
        if (lblImpactGoal != null) {
            // Calculate progress based on reduction goals
            double progressPercent = total > 0 ? Math.min(100, (total / 500.0) * 100.0) : 0;
            lblImpactGoal.setText(String.format("%.0f%% objectif", progressPercent));
            System.out.println("[SCOPE BREAKDOWN] Impact goal updated: " + lblImpactGoal.getText());
        }
        if (progressImpact != null) {
            // Update progress bar based on total credits vs 500 target
            double progress = total > 0 ? Math.min(1.0, total / 500.0) : 0.0;
            progressImpact.setProgress(progress);
            System.out.println("[SCOPE BREAKDOWN] Progress updated: " + (progress * 100) + "%");
        }
        
        System.out.println("[SCOPE BREAKDOWN] Update complete");
    }

    private void clearWalletDisplay() {
        lblWalletNumber.setText("—");
        lblHolderName.setText("—");
        lblOwnerType.setText("—");
        lblStatus.setText("—");
        lblAvailableCredits.setText("0.00 tCO₂");
        lblRetiredCredits.setText("0.00 tCO₂");
        if (lblTotalCredits != null) {
            lblTotalCredits.setText("0.00 tCO₂");
        }
        if (lblSidebarAvailable != null) {
            lblSidebarAvailable.setText("0.00 tCO₂");
        }
        if (lblSidebarRetired != null) {
            lblSidebarRetired.setText("0.00 tCO₂");
        }
        if (lblScope1Amount != null) {
            lblScope1Amount.setText("—");
        }
        if (lblScope2Amount != null) {
            lblScope2Amount.setText("—");
        }
        if (lblScope3Amount != null) {
            lblScope3Amount.setText("—");
        }
        tableTransactions.setItems(FXCollections.observableArrayList());
    }

    private void loadTransactions() {
        if (currentWallet == null) return;
        
        try {
            List<OperationWallet> transactions = walletService.getWalletTransactions(currentWallet.getId());
            ObservableList<OperationWallet> transactionList = FXCollections.observableArrayList(transactions);
            tableTransactions.setItems(transactionList);
        } catch (Exception e) {
            showError("Erreur lors du chargement des transactions", e.getMessage());
        }
    }

    // ==================== ACTIONS ====================

    private void showCreateWalletDialog() {
        User user = SessionManager.getInstance().getCurrentUser();
        Integer userId = getCurrentUserIdAsInt(user);
        if (user == null || userId == null) {
            showWarning("Session invalide", "Impossible de créer un wallet sans utilisateur connecté.");
            return;
        }

        // Clear form fields
        txtCreateWalletName.clear();
        txtCreateWalletNumber.clear();
        txtCreateWalletCredits.setText("0");

        // Show slide-in panel
        showSlidePanel(createWalletPanel);
    }

    @FXML
    private void onCloseCreateWalletPanel() {
        hideSlidePanel(createWalletPanel);
    }

    @FXML
    private void onConfirmCreateWallet() {
        User user = SessionManager.getInstance().getCurrentUser();
        Integer userId = getCurrentUserIdAsInt(user);

        String name = txtCreateWalletName.getText() == null ? "" : txtCreateWalletName.getText().trim();
        if (name.isEmpty()) {
            showWarning("Nom requis", "Veuillez saisir un nom de wallet.");
            return;
        }

        String walletNumberText = txtCreateWalletNumber.getText() == null ? "" : txtCreateWalletNumber.getText().trim();
        Integer walletNumber = null;
        if (!walletNumberText.isEmpty()) {
            try {
                walletNumber = Integer.parseInt(walletNumberText);
                if (walletNumber <= 0) {
                    showWarning("Numéro invalide", "Le numéro du wallet doit être un entier positif.");
                    return;
                }
            } catch (NumberFormatException ex) {
                showWarning("Numéro invalide", "Le numéro du wallet doit être numérique.");
                return;
            }
        }

        String creditsText = txtCreateWalletCredits.getText() == null ? "0" : txtCreateWalletCredits.getText().trim();
        double credits = 0;
        try {
            credits = Double.parseDouble((creditsText.isEmpty() ? "0" : creditsText).replace(',', '.'));
            if (credits < 0) {
                showWarning("Crédits invalides", "Les crédits initiaux ne peuvent pas être négatifs.");
                return;
            }
        } catch (NumberFormatException ex) {
            showWarning("Crédits invalides", "Veuillez saisir une valeur numérique valide.");
            return;
        }

        // Create and save wallet
        Wallet wallet = new Wallet();
        wallet.setName(name);
        if (walletNumber != null) {
            wallet.setWalletNumber(walletNumber);
        }
        wallet.setOwnerType(resolveOwnerTypeForUser(user));
        wallet.setOwnerId(userId);
        wallet.setAvailableCredits(credits);
        wallet.setRetiredCredits(0.0);

        try {
            int id = walletService.createWallet(wallet);
            if (id > 0) {
                showInfo("Succès", "Wallet créé avec succès!");
                onCloseCreateWalletPanel();
                loadWallets();
            } else {
                showError("Erreur", "Impossible de créer le wallet");
            }
        } catch (Exception e) {
            showError("Erreur lors de la création", e.getMessage());
        }
    }

    private void showQuickIssueDialog() {
        if (currentWallet == null) {
            showWarning("Aucun wallet sélectionné", "Veuillez sélectionner un wallet");
            return;
        }

        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("🌱 Émettre des Crédits Carbone");
        dialog.setHeaderText(String.format("Wallet: %s\nDisponible: %.2f tCO₂", 
            currentWallet.getName(), currentWallet.getAvailableCredits()));

        ButtonType issueButtonType = new ButtonType("✓ Émettre", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(issueButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        TextField amount = new TextField();
        amount.setPromptText("Montant (tCO₂)");
        
        ComboBox<String> presetAmounts = new ComboBox<>();
        presetAmounts.getItems().addAll("100.00", "500.00", "1000.00", "5000.00", "Personnalisé");
        presetAmounts.setValue("Personnalisé");
        presetAmounts.setOnAction(e -> {
            String val = presetAmounts.getValue();
            if (!val.equals("Personnalisé")) {
                amount.setText(val);
            }
        });
        
        ComboBox<String> sourcePresets = new ComboBox<>();
        sourcePresets.getItems().addAll(
            "🌞 Installation Solaire - Phase 1",
            "🌲 Reforestation Amazonie",
            "💨 Capture CO₂ Industrielle",
            "⚡ Parc Éolien Offshore",
            "🛰️ Vérification Projet Tiers",
            "Autre source..."
        );
        sourcePresets.setValue("Autre source...");
        
        TextArea reference = new TextArea();
        reference.setPromptText("Description de l'émission...");
        reference.setPrefRowCount(3);
        
        sourcePresets.setOnAction(e -> {
            String selected = sourcePresets.getValue();
            if (!selected.equals("Autre source...")) {
                reference.setText("Crédits émis depuis: " + selected);
            }
        });

        grid.add(new Label("📊 Montant Rapide:"), 0, 0);
        grid.add(presetAmounts, 1, 0);
        grid.add(new Label("💰 Montant Exact (tCO₂):"), 0, 1);
        grid.add(amount, 1, 1);
        grid.add(new Label("🏭 Source:"), 0, 2);
        grid.add(sourcePresets, 1, 2);
        grid.add(new Label("📝 Référence:"), 0, 3);
        grid.add(reference, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == issueButtonType) {
                return new String[]{amount.getText(), reference.getText()};
            }
            return null;
        });

        Optional<String[]> result = dialog.showAndWait();
        result.ifPresent(data -> {
            try {
                double amt = Double.parseDouble(data[0]);
                String ref = data[1].isEmpty() ? "Émission de crédits carbone" : data[1];
                
                boolean success = walletService.quickIssueCredits(currentWallet.getId(), amt, ref);
                if (success) {
                    showInfo("✔ Succès", String.format("%.2f tCO₂ émis avec succès!", amt));
                    refreshData();
                } else {
                    showError("Erreur", "Impossible d'émettre les crédits");
                }
            } catch (Exception e) {
                showError("Erreur lors de l'émission", e.getMessage());
            }
        });
    }

    private void showEditWalletDialog() {
        if (currentWallet == null) {
            showWarning("Aucun wallet sélectionné", "Veuillez sélectionner un wallet");
            return;
        }

        // Pre-populate form fields
        txtEditWalletName.setText(currentWallet.getName());
        txtEditWalletNumber.setText(formatWalletNumber(currentWallet.getWalletNumber()));

        // Show slide-in panel
        showSlidePanel(editWalletPanel);
    }

    @FXML
    private void onCloseEditWalletPanel() {
        hideSlidePanel(editWalletPanel);
    }

    @FXML
    private void onConfirmEditWallet() {
        if (currentWallet == null) {
            return;
        }

        String name = txtEditWalletName.getText() == null ? "" : txtEditWalletName.getText().trim();
        if (name.isEmpty()) {
            showWarning("Nom requis", "Veuillez saisir un nom de wallet.");
            return;
        }

        // Update wallet
        currentWallet.setName(name);

        try {
            boolean success = walletService.updateWallet(currentWallet);
            if (success) {
                showInfo("✔ Succès", "Wallet modifié avec succès!");
                onCloseEditWalletPanel();
                refreshData();
            } else {
                showError("Erreur", "Impossible de modifier le wallet");
            }
        } catch (Exception e) {
            showError("Erreur lors de la modification", e.getMessage());
        }
    }

    private void showTransferDialog() {
        if (currentWallet == null) {
            showWarning("Aucun wallet sélectionné", "Veuillez sélectionner un wallet source");
            return;
        }

        if (currentWallet.getAvailableCredits() <= 0) {
            showWarning("Crédits insuffisants", "Ce wallet n'a pas de crédits disponibles pour le transfert");
            return;
        }

        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("🔄 Transférer des Crédits");
        dialog.setHeaderText(String.format("Source: %s\nDisponible: %.2f tCO₂", 
            currentWallet.getName(), currentWallet.getAvailableCredits()));

        ButtonType transferButtonType = new ButtonType("➡️ Transférer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(transferButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        ComboBox<Wallet> destinationWallet = new ComboBox<>();
        List<Wallet> allWallets = getScopedWallets();
        allWallets.removeIf(w -> w.getId() == currentWallet.getId());
        destinationWallet.setItems(FXCollections.observableArrayList(allWallets));
        destinationWallet.setConverter(new javafx.util.StringConverter<Wallet>() {
            @Override
            public String toString(Wallet w) {
                if (w == null) return null;
                return String.format("#%s - %s", formatWalletNumber(w.getWalletNumber()), w.getName());
            }
            @Override
            public Wallet fromString(String s) { return null; }
        });
        
        TextField amount = new TextField();
        amount.setPromptText("Montant à transférer");
        
        ComboBox<String> quickAmounts = new ComboBox<>();
        quickAmounts.getItems().addAll("25%", "50%", "75%", "100%", "Personnalisé");
        quickAmounts.setValue("Personnalisé");
        quickAmounts.setOnAction(e -> {
            String val = quickAmounts.getValue();
            if (!val.equals("Personnalisé")) {
                double percentage = Double.parseDouble(val.replace("%", "")) / 100.0;
                double amt = currentWallet.getAvailableCredits() * percentage;
                amount.setText(String.format("%.2f", amt));
            }
        });
        
        TextArea reference = new TextArea();
        reference.setPromptText("Raison du transfert (obligatoire)");
        reference.setPrefRowCount(3);

        grid.add(new Label("🎯 Destination:"), 0, 0);
        grid.add(destinationWallet, 1, 0);
        grid.add(new Label("📊 Montant Rapide:"), 0, 1);
        grid.add(quickAmounts, 1, 1);
        grid.add(new Label("💰 Montant (tCO₂):"), 0, 2);
        grid.add(amount, 1, 2);
        grid.add(new Label("📝 Raison:"), 0, 3);
        grid.add(reference, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == transferButtonType) {
                Wallet dest = destinationWallet.getValue();
                if (dest == null) {
                    showWarning("Destination requise", "Veuillez sélectionner un wallet de destination");
                    return null;
                }
                return new String[]{String.valueOf(dest.getId()), amount.getText(), reference.getText()};
            }
            return null;
        });

        Optional<String[]> result = dialog.showAndWait();
        result.ifPresent(data -> {
            try {
                int destId = Integer.parseInt(data[0]);
                double amt = Double.parseDouble(data[1]);
                String ref = data[2];
                
                if (ref.trim().isEmpty()) {
                    showWarning("Référence requise", "Veuillez indiquer la raison du transfert");
                    return;
                }
                
                boolean success = walletService.transferCredits(currentWallet.getId(), destId, amt, ref);
                if (success) {
                    showInfo("✔ Transfert Réussi", String.format("%.2f tCO₂ transférés avec succès!", amt));
                    refreshData();
                } else {
                    showError("Erreur", "Impossible de transférer les crédits");
                }
            } catch (Exception e) {
                showError("Erreur lors du transfert", e.getMessage());
            }
        });
    }

    private void showIssueCreditsDialog() {
        if (currentWallet == null) {
            showWarning("Aucun wallet sélectionné", "Veuillez sélectionner un wallet");
            return;
        }

        Dialog<double[]> dialog = new Dialog<>();
        dialog.setTitle("Émettre des Crédits Carbone");
        dialog.setHeaderText("Émission de crédits pour: Wallet #" + formatWalletNumber(currentWallet.getWalletNumber()));

        ButtonType issueButtonType = new ButtonType("Émettre", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(issueButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField projectId = new TextField();
        projectId.setPromptText("ID du Projet");
        
        TextField amount = new TextField();
        amount.setPromptText("Montant (tCO₂)");
        
        TextArea reference = new TextArea();
        reference.setPromptText("Note de référence");
        reference.setPrefRowCount(3);

        grid.add(new Label("ID Projet:"), 0, 0);
        grid.add(projectId, 1, 0);
        grid.add(new Label("Montant:"), 0, 1);
        grid.add(amount, 1, 1);
        grid.add(new Label("Référence:"), 0, 2);
        grid.add(reference, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == issueButtonType) {
                try {
                    int projId = Integer.parseInt(projectId.getText());
                    double amt = Double.parseDouble(amount.getText());
                    return new double[]{projId, amt};
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });

        Optional<double[]> result = dialog.showAndWait();
        result.ifPresent(data -> {
            try {
                int projId = (int) data[0];
                double amt = data[1];
                String ref = reference.getText();
                
                boolean success = walletService.issueCredits(currentWallet.getId(), projId, amt, ref);
                if (success) {
                    showInfo("Succès", amt + " tCO₂ émis avec succès!");
                    refreshData();
                } else {
                    showError("Erreur", "Impossible d'émettre les crédits");
                }
            } catch (Exception e) {
                showError("Erreur lors de l'émission", e.getMessage());
            }
        });
    }

    private void showRetireCreditsDialog() {
        if (currentWallet == null) {
            showWarning("Aucun wallet sélectionné", "Veuillez sélectionner un wallet");
            return;
        }

        if (currentWallet.getAvailableCredits() <= 0) {
            showWarning("Aucun crédit disponible", "Ce wallet n'a pas de crédits disponibles à retirer");
            return;
        }

        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("♻️ Retirer des Crédits Carbone");
        dialog.setHeaderText(String.format("Wallet: %s\nDisponible: %.2f tCO₂", 
            currentWallet.getName(), currentWallet.getAvailableCredits()));

        ButtonType retireButtonType = new ButtonType("🔒 Retirer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(retireButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        ComboBox<String> quickAmounts = new ComboBox<>();
        quickAmounts.getItems().addAll("25%", "50%", "75%", "100%", "Personnalisé");
        quickAmounts.setValue("Personnalisé");
        
        TextField amount = new TextField();
        amount.setPromptText("Montant (tCO₂)");
        
        quickAmounts.setOnAction(e -> {
            String val = quickAmounts.getValue();
            if (!val.equals("Personnalisé")) {
                double percentage = Double.parseDouble(val.replace("%", "")) / 100.0;
                double amt = currentWallet.getAvailableCredits() * percentage;
                amount.setText(String.format("%.2f", amt));
            }
        });
        
        ComboBox<String> reasonPresets = new ComboBox<>();
        reasonPresets.getItems().addAll(
            "🌍 Compensation empreinte carbone entreprise",
            "✈️ Neutralité carbone - Voyage aérien",
            "🏭 Compensation production industrielle",
            "🚗 Neutralisation émissions transport",
            "🏢 Bilan carbone annuel - Neutralité",
            "🎯 Objectif Net-Zero atteint",
            "Autre raison..."
        );
        reasonPresets.setValue("Autre raison...");
        
        TextArea reference = new TextArea();
        reference.setPromptText("Ex: Compensation carbone pour conférence internationale à Paris, 200 participants...");
        reference.setPrefRowCount(4);
        
        reasonPresets.setOnAction(e -> {
            String selected = reasonPresets.getValue();
            if (!selected.equals("Autre raison...")) {
                reference.setText(selected);
            }
        });

        grid.add(new Label("📊 Montant Rapide:"), 0, 0);
        grid.add(quickAmounts, 1, 0);
        grid.add(new Label("💰 Montant Exact (tCO₂):"), 0, 1);
        grid.add(amount, 1, 1);
        grid.add(new Label("🏷️ Raison Type:"), 0, 2);
        grid.add(reasonPresets, 1, 2);
        grid.add(new Label("📝 Raison détaillée:"), 0, 3);
        grid.add(reference, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == retireButtonType) {
                return new String[]{amount.getText(), reference.getText()};
            }
            return null;
        });

        Optional<String[]> result = dialog.showAndWait();
        result.ifPresent(data -> {
            try {
                double amt = Double.parseDouble(data[0]);
                String ref = data[1];
                
                if (ref.trim().isEmpty()) {
                    showWarning("Référence requise", "Veuillez indiquer la raison du retirement");
                    return;
                }
                
                boolean success = walletService.retireCredits(currentWallet.getId(), amt, ref);
                if (success) {
                    showInfo("Retrait effectué", String.format("%.2f tCO₂ retirés avec succès!\n\nCes crédits sont maintenant définitivement retirés du marché.", amt));
                    refreshData();
                } else {
                    showError("Erreur", "Impossible de retirer les crédits");
                }
            } catch (Exception e) {
                showError("Erreur lors du retirement", e.getMessage());
            }
        });
    }

    private void showDeleteWalletDialog() {
        if (currentWallet == null) {
            showWarning("Aucun wallet sélectionné", "Veuillez sélectionner un wallet");
            return;
        }

        // Safety check: Can only delete wallets with zero balance
        if (currentWallet.getTotalCredits() > 0) {
            showWarning(
                "Suppression impossible", 
                String.format(
                    "Ce wallet contient encore des crédits:\n\n" +
                    "💰 Disponibles: %.2f tCO₂\n" +
                    "♻️ Retirés: %.2f tCO₂\n" +
                    "📊 Total: %.2f tCO₂\n\n" +
                    "Vous devez d'abord transférer ou retirer tous les crédits disponibles.",
                    currentWallet.getAvailableCredits(),
                    currentWallet.getRetiredCredits(),
                    currentWallet.getTotalCredits()
                )
            );
            return;
        }

        // Pre-populate form fields
        lblDeleteWalletName.setText(currentWallet.getName() + " (#" + formatWalletNumber(currentWallet.getWalletNumber()) + ")");
        chkDeleteWalletConfirm.setSelected(false);
        btnConfirmDeleteWallet.setDisable(true);

        // Enable delete button only when checkbox is checked
        chkDeleteWalletConfirm.selectedProperty().addListener((obs, oldVal, newVal) -> {
            btnConfirmDeleteWallet.setDisable(!newVal);
        });

        // Show slide-in panel
        showSlidePanel(deleteWalletPanel);
    }

    @FXML
    private void onCloseDeleteWalletPanel() {
        hideSlidePanel(deleteWalletPanel);
    }

    @FXML
    private void onConfirmDeleteWallet() {
        if (currentWallet == null || !chkDeleteWalletConfirm.isSelected()) {
            return;
        }

        try {
            boolean success = walletService.deleteWallet(currentWallet.getId());
            if (success) {
                showInfo("Wallet supprimé", "Le wallet a été supprimé avec succès!");
                onCloseDeleteWalletPanel();
                currentWallet = null;
                loadWallets();
                clearWalletDisplay();
            } else {
                showError("Erreur", "Impossible de supprimer le wallet");
            }
        } catch (Exception e) {
            showError("Erreur lors de la suppression", e.getMessage());
        }
    }

    private void showWalletOverview() {
        // Current view is already overview
        refreshData();
    }

    private void showTransactions() {
        // Already showing transactions in main view
        refreshData();
    }

    private void showBatches() {
        showInfo("Bientôt disponible", "La vue des batches sera implémentée prochainement");
    }

    private void exportData() {
        showInfo("Bientôt disponible", "La fonction d'export sera implémentée prochainement");
    }

    private void refreshData() {
        if (currentWallet != null) {
            loadWallet(currentWallet.getId());
        }
    }

    private void addTestCredits(double amount) {
        if (currentWallet == null) {
            showWarning("Aucun wallet sélectionné", "Veuillez sélectionner un wallet");
            return;
        }

        String note = String.format("[TEST] Quick top-up %.2f tCO₂", amount);
        boolean success = walletService.quickIssueCredits(currentWallet.getId(), amount, note);
        if (success) {
            showInfo("🧪 Crédit Test Ajouté", String.format("%.2f tCO₂ ajoutés au wallet %s", amount, currentWallet.getName()));
            refreshData();
        } else {
            showError("Erreur", "Impossible d'ajouter les crédits de test");
        }
    }

    private void showSettings() {
        try {
            MainFX.setRoot("settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showMarketplace() {
        try {
            MainFX.setRoot("fxml/marketplace");
        } catch (IOException e) {
            try {
                MainFX.setRoot("marketplace");
                return;
            } catch (IOException ignored) {
                // fall through to error handling
            }
            showError("Erreur", "Impossible d'ouvrir le marketplace: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onMarketplace() {
        showMarketplace();
    }

    @FXML
    private void onGestionProjets() {
        try {
            MainFX.setRoot("GestionProjet");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleEditProfile() {
        try {
            MainFX.setRoot("editProfile");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onBack() {
        try {
            MainFX.setRoot("fxml/dashboard");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onShowIssuePanel() {
        if (currentWallet == null) {
            showWarning("Aucun wallet sélectionné", "Veuillez sélectionner un wallet");
            return;
        }
        showSlidePanel(issueCreditPanel);
    }

    @FXML
    private void onShowRetirePanel() {
        if (currentWallet == null) {
            showWarning("Aucun wallet sélectionné", "Veuillez sélectionner un wallet");
            return;
        }
        if (currentWallet.getAvailableCredits() <= 0) {
            showWarning("Crédits insuffisants", "Ce wallet n'a pas de crédits disponibles pour le retrait");
            return;
        }
        showSlidePanel(retireCreditPanel);
    }

    @FXML
    private void onShowTransferPanel() {
        if (currentWallet == null) {
            showWarning("Aucun wallet sélectionné", "Veuillez sélectionner un wallet source");
            return;
        }
        if (currentWallet.getAvailableCredits() <= 0) {
            showWarning("Crédits insuffisants", "Ce wallet n'a pas de crédits disponibles pour le transfert");
            return;
        }
        // Load destination wallets
        if (cmbTransferTargetWallet != null) {
            List<Wallet> allWallets = getScopedWallets();
            allWallets.removeIf(w -> w.getId() == currentWallet.getId());
            cmbTransferTargetWallet.setItems(FXCollections.observableArrayList(allWallets));
        }
        showSlidePanel(transferCreditPanel);
    }

    @FXML
    private void onShowEmissionsPanel() {
        if (currentWallet == null) {
            showWarning("Aucun wallet sélectionné", "Veuillez sélectionner un wallet");
            return;
        }
        showSlidePanel(emissionCalculatorPanel);
    }

    @FXML
    private void onCloseIssuePanel() {
        hideSlidePanel(issueCreditPanel);
    }

    @FXML
    private void onCloseRetirePanel() {
        hideSlidePanel(retireCreditPanel);
    }

    @FXML
    private void onCloseTransferPanel() {
        hideSlidePanel(transferCreditPanel);
    }

    @FXML
    private void onCloseEmissionsPanel() {
        hideSlidePanel(emissionCalculatorPanel);
    }

    @FXML
    private void onConfirmIssue() {
        if (currentWallet == null) return;

        try {
            double amount = Double.parseDouble(txtIssueAmount.getText());
            String standard = cmbVerificationStandard.getValue();
            String reference = txtIssueReference.getText();

            if (amount <= 0 || amount > 10000) {
                showWarning("Montant invalide", "Le montant doit être entre 1 et 10,000 tCO₂");
                return;
            }

            if (standard == null || standard.trim().isEmpty()) {
                showWarning("Standard requis", "Veuillez sélectionner un standard de vérification");
                return;
            }

            String fullReference = String.format("[%s] %s", standard, 
                reference.isEmpty() ? "Émission de crédits carbone" : reference);

            boolean success = walletService.quickIssueCredits(currentWallet.getId(), amount, fullReference);
            if (success) {
                showInfo("✔ Succès", String.format("%.2f tCO₂ émis avec succès!", amount));
                clearIssueForm();
                hideSlidePanel(issueCreditPanel);
                refreshData();
            } else {
                showError("Erreur", "Impossible d'émettre les crédits");
            }
        } catch (NumberFormatException e) {
            showWarning("Format invalide", "Veuillez entrer un montant valide");
        } catch (Exception e) {
            showError("Erreur lors de l'émission", e.getMessage());
        }
    }

    @FXML
    private void onConfirmRetire() {
        if (currentWallet == null) return;

        try {
            double amount = Double.parseDouble(txtRetireAmount.getText());
            String reason = cmbRetireReason.getValue();
            String notes = txtRetireReason.getText();

            if (amount <= 0 || amount > currentWallet.getAvailableCredits()) {
                showWarning("Montant invalide", 
                    String.format("Le montant doit être entre 1 et %.2f tCO₂", currentWallet.getAvailableCredits()));
                return;
            }

            if (reason == null || reason.trim().isEmpty()) {
                showWarning("Raison requise", "Veuillez sélectionner une raison de retrait");
                return;
            }

            String fullReference = String.format("[RETIRE: %s] %s", reason, 
                notes.isEmpty() ? "Compensation carbone permanente" : notes);

            boolean success = walletService.retireCredits(currentWallet.getId(), amount, fullReference);
            if (success) {
                showInfo("✔ Succès", String.format("%.2f tCO₂ retirés avec succès!", amount));
                clearRetireForm();
                hideSlidePanel(retireCreditPanel);
                refreshData();
            } else {
                showError("Erreur", "Impossible de retirer les crédits");
            }
        } catch (NumberFormatException e) {
            showWarning("Format invalide", "Veuillez entrer un montant valide");
        } catch (Exception e) {
            showError("Erreur lors du retrait", e.getMessage());
        }
    }

    @FXML
    private void onConfirmTransfer() {
        if (currentWallet == null) return;

        try {
            Wallet destination = cmbTransferTargetWallet.getValue();
            double amount = Double.parseDouble(txtTransferAmount.getText());
            String reference = txtTransferReference.getText();

            if (destination == null) {
                showWarning("Destination requise", "Veuillez sélectionner un wallet de destination");
                return;
            }

            if (amount <= 0 || amount > currentWallet.getAvailableCredits()) {
                showWarning("Montant invalide", 
                    String.format("Le montant doit être entre 1 et %.2f tCO₂", currentWallet.getAvailableCredits()));
                return;
            }

            if (reference.trim().isEmpty()) {
                showWarning("Référence requise", "Veuillez fournir une raison pour le transfert");
                return;
            }

            boolean success = walletService.transferCredits(currentWallet.getId(), destination.getId(), amount, reference);
            if (success) {
                showInfo("✔ Succès", String.format("%.2f tCO₂ transférés avec succès!", amount));
                clearTransferForm();
                hideSlidePanel(transferCreditPanel);
                refreshData();
            } else {
                showError("Erreur", "Impossible de transférer les crédits");
            }
        } catch (NumberFormatException e) {
            showWarning("Format invalide", "Veuillez entrer un montant valide");
        } catch (Exception e) {
            showError("Erreur lors du transfert", e.getMessage());
        }
    }

    @FXML
    private void onCalculateEmissions() {
        // This is called from the emissions panel
        // For now, show info that user should use the specific calculators
        showInfo("Calculateur d'Émissions", 
            "Veuillez utiliser les boutons de calcul spécifiques (Électricité, Carburant, Transport) disponibles dans la section API Integration.");
        hideSlidePanel(emissionCalculatorPanel);
    }

    @FXML
    private void onExportCsv() {
        exportData();
    }

    // ==================== SLIDE PANEL ANIMATION ====================

    private void showSlidePanel(VBox panel) {
        if (panel == null) return;
        
        panel.setManaged(true);
        panel.setVisible(true);
        
        javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(
            javafx.util.Duration.millis(300), panel);
        tt.setFromX(panel.getTranslateX());
        tt.setToX(0);
        tt.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
        tt.play();
    }

    private void hideSlidePanel(VBox panel) {
        if (panel == null) return;
        
        javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(
            javafx.util.Duration.millis(250), panel);
        tt.setFromX(panel.getTranslateX());
        tt.setToX(1200);
        tt.setInterpolator(javafx.animation.Interpolator.EASE_IN);
        tt.setOnFinished(e -> {
            panel.setVisible(false);
            panel.setManaged(false);
        });
        tt.play();
    }

    private void clearIssueForm() {
        if (txtIssueAmount != null) txtIssueAmount.clear();
        if (cmbVerificationStandard != null) cmbVerificationStandard.setValue(null);
        if (txtVintageYear != null) txtVintageYear.clear();
        if (txtCalculationAuditId != null) txtCalculationAuditId.clear();
        if (txtIssueReference != null) txtIssueReference.clear();
    }

    private void clearRetireForm() {
        if (txtRetireAmount != null) txtRetireAmount.clear();
        if (cmbRetireReason != null) cmbRetireReason.setValue(null);
        if (txtRetireReason != null) txtRetireReason.clear();
    }

    private void clearTransferForm() {
        if (txtTransferAmount != null) txtTransferAmount.clear();
        if (cmbTransferTargetWallet != null) cmbTransferTargetWallet.setValue(null);
        if (txtTransferWalletNumber != null) txtTransferWalletNumber.clear();
        if (txtTransferReference != null) txtTransferReference.clear();
    }

    // ==================== UTILITY METHODS ====================

    private String formatCredits(double credits) {
        return String.format("%.2f tCO₂", credits);
    }

    private String formatWalletNumber(Integer walletNumber) {
        return walletNumber == null ? "—" : String.valueOf(walletNumber);
    }

    private Integer parseIntegerOrNull(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private List<Wallet> getScopedWallets() {
        List<Wallet> allWallets = walletService.getAllWallets();
        User user = SessionManager.getInstance().getCurrentUser();
        Integer currentUserId = getCurrentUserIdAsInt(user);

        if (user == null || currentUserId == null || user.getTypeUtilisateur() == TypeUtilisateur.ADMIN) {
            return allWallets;
        }

        return allWallets.stream()
                .filter(wallet -> wallet.getOwnerId() == currentUserId)
                .collect(Collectors.toList());
    }

    private Integer getCurrentUserIdAsInt(User user) {
        if (user == null || user.getId() == null) {
            return null;
        }
        try {
            return Math.toIntExact(user.getId());
        } catch (ArithmeticException ex) {
            return null;
        }
    }

    private String resolveOwnerTypeForUser(User user) {
        if (user == null || user.getTypeUtilisateur() == null) {
            return "ENTERPRISE";
        }

        if (user.getTypeUtilisateur() == TypeUtilisateur.INVESTISSEUR) {
            return "BANK";
        }
        return "ENTERPRISE";
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showWarning(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    // ==================== CARBON API INTEGRATION ====================
    
    private void calculateElectricityEmissions() {
        if (!ensureCarbonApiAvailable()) {
            return;
        }
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("⚡ Calcul Émissions Électricité");
        dialog.setHeaderText("Estimer les émissions CO₂ de la consommation électrique");
        
        ButtonType calculateButtonType = new ButtonType("Calculer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(calculateButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        
        TextField electricityValue = new TextField("1000");
        electricityValue.setPromptText("Valeur");
        
        ComboBox<String> electricityUnit = new ComboBox<>();
        electricityUnit.getItems().addAll("kwh", "mwh");
        electricityUnit.setValue("kwh");
        
        TextField country = new TextField("us");
        country.setPromptText("Code pays (ex: us, fr)");
        
        TextField state = new TextField();
        state.setPromptText("État (optionnel, ex: fl)");
        
        grid.add(new Label("⚡ Consommation:"), 0, 0);
        grid.add(electricityValue, 1, 0);
        grid.add(new Label("📊 Unité:"), 0, 1);
        grid.add(electricityUnit, 1, 1);
        grid.add(new Label("🌍 Pays:"), 0, 2);
        grid.add(country, 1, 2);
        grid.add(new Label("📍 État:"), 0, 3);
        grid.add(state, 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == calculateButtonType) {
                try {
                    return Double.parseDouble(electricityValue.getText());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });
        
        Optional<Double> result = dialog.showAndWait();
        result.ifPresent(value -> {
            appendToApiResults("⚡ Calcul d'émissions d'électricité en cours...\n");
            
            new Thread(() -> {
                try {
                    String countryCode = country.getText().trim();
                    
                    CarbonEstimateResponse response = carbonApiService.estimateElectricity(
                        value,
                        electricityUnit.getValue(),
                        countryCode
                    );
                    
                    Platform.runLater(() -> {
                        if (response != null && response.getAttributes() != null) {
                            appendToApiResults(formatCarbonEstimateResponse(response));
                        } else {
                            appendToApiResults(buildCarbonApiErrorMessage());
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> appendToApiResults("❌ Erreur: " + e.getMessage() + "\n"));
                }
            }).start();
        });
    }
    
    private void calculateFuelEmissions() {
        if (!ensureCarbonApiAvailable()) {
            return;
        }
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("⛽ Calcul Émissions Carburant");
        dialog.setHeaderText("Estimer les émissions CO₂ de la combustion de carburant");
        
        ButtonType calculateButtonType = new ButtonType("Calculer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(calculateButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        
        ComboBox<String> fuelSourceType = new ComboBox<>();
        fuelSourceType.getItems().addAll("dfo", "rfo", "lng", "lpg", "cng", "coal", "petcoke");
        fuelSourceType.setValue("dfo");
        
        TextField fuelSourceValue = new TextField("100");
        fuelSourceValue.setPromptText("Valeur");
        
        ComboBox<String> fuelSourceUnit = new ComboBox<>();
        fuelSourceUnit.getItems().addAll("litre", "gallon", "tonne");
        fuelSourceUnit.setValue("litre");
        
        grid.add(new Label("⛽ Type Carburant:"), 0, 0);
        grid.add(fuelSourceType, 1, 0);
        grid.add(new Label("📊 Quantité:"), 0, 1);
        grid.add(fuelSourceValue, 1, 1);
        grid.add(new Label("📏 Unité:"), 0, 2);
        grid.add(fuelSourceUnit, 1, 2);
        
        Label infoLabel = new Label("dfo=Diesel, rfo=Fuel Heavy, lng=Gaz Naturel Liquéfié");
        infoLabel.setStyle("-fx-font-size: 10; -fx-opacity: 0.7;");
        grid.add(infoLabel, 0, 3, 2, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == calculateButtonType) {
                try {
                    return Double.parseDouble(fuelSourceValue.getText());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });
        
        Optional<Double> result = dialog.showAndWait();
        result.ifPresent(value -> {
            appendToApiResults("⛽ Calcul d'émissions de carburant en cours...\n");
            
            new Thread(() -> {
                try {
                    CarbonEstimateResponse response = carbonApiService.estimateFuel(
                        fuelSourceType.getValue(),
                        value,
                        fuelSourceUnit.getValue()
                    );
                    
                    Platform.runLater(() -> {
                        if (response != null && response.getAttributes() != null) {
                            appendToApiResults(formatCarbonEstimateResponse(response));
                        } else {
                            appendToApiResults(buildCarbonApiErrorMessage());
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> appendToApiResults("❌ Erreur: " + e.getMessage() + "\n"));
                }
            }).start();
        });
    }
    
    private void calculateShippingEmissions() {
        if (!ensureCarbonApiAvailable()) {
            return;
        }
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("🚢 Calcul Émissions Transport");
        dialog.setHeaderText("Estimer les émissions CO₂ du transport maritime");
        
        ButtonType calculateButtonType = new ButtonType("Calculer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(calculateButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        
        TextField weightValue = new TextField("1000");
        weightValue.setPromptText("Poids");
        
        ComboBox<String> weightUnit = new ComboBox<>();
        weightUnit.getItems().addAll("kg", "lb", "mt", "g");
        weightUnit.setValue("kg");
        
        TextField distanceValue = new TextField("1000");
        distanceValue.setPromptText("Distance");
        
        ComboBox<String> distanceUnit = new ComboBox<>();
        distanceUnit.getItems().addAll("km", "mi");
        distanceUnit.setValue("km");
        
        ComboBox<String> transportMethod = new ComboBox<>();
        transportMethod.getItems().addAll("ship", "train", "truck", "plane");
        transportMethod.setValue("ship");
        
        grid.add(new Label("📦 Poids:"), 0, 0);
        grid.add(weightValue, 1, 0);
        grid.add(new Label("⚖️ Unité Poids:"), 0, 1);
        grid.add(weightUnit, 1, 1);
        grid.add(new Label("📏 Distance:"), 0, 2);
        grid.add(distanceValue, 1, 2);
        grid.add(new Label("📐 Unité Distance:"), 0, 3);
        grid.add(distanceUnit, 1, 3);
        grid.add(new Label("🚚 Mode Transport:"), 0, 4);
        grid.add(transportMethod, 1, 4);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == calculateButtonType) {
                try {
                    return Double.parseDouble(weightValue.getText());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });
        
        Optional<Double> result = dialog.showAndWait();
        result.ifPresent(weight -> {
            appendToApiResults("🚢 Calcul d'émissions de transport en cours...\n");
            
            new Thread(() -> {
                try {
                    double distance = Double.parseDouble(distanceValue.getText());
                    double weightKg = convertWeightToKg(weight, weightUnit.getValue());
                    double distanceKm = convertDistanceToKm(distance, distanceUnit.getValue());
                    
                    CarbonEstimateResponse response = carbonApiService.estimateShipping(
                        weightKg,
                        distanceKm,
                        transportMethod.getValue()
                    );
                    
                    Platform.runLater(() -> {
                        if (response != null && response.getAttributes() != null) {
                            appendToApiResults(formatCarbonEstimateResponse(response));
                        } else {
                            appendToApiResults(buildCarbonApiErrorMessage());
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> appendToApiResults("❌ Erreur: " + e.getMessage() + "\n"));
                }
            }).start();
        });
    }
    
    private void checkAirQuality() {
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("🌫️ Vérification Qualité de l'Air");
        dialog.setHeaderText("Obtenir les données de qualité de l'air pour une localisation");
        
        ButtonType checkButtonType = new ButtonType("Vérifier", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(checkButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        
        TextField latitude = new TextField("40.7128");
        latitude.setPromptText("Latitude");
        
        TextField longitude = new TextField("-74.0060");
        longitude.setPromptText("Longitude");
        
        Label exampleLabel = new Label("Exemple: New York = 40.7128, -74.0060");
        exampleLabel.setStyle("-fx-font-size: 10; -fx-opacity: 0.7;");
        
        HBox presetBox = new HBox(8);
        Button btnParis = new Button("🇫🇷 Paris");
        Button btnNewYork = new Button("🇺🇸 New York");
        Button btnTokyo = new Button("🇯🇵 Tokyo");
        
        btnParis.setOnAction(e -> {
            latitude.setText("48.8566");
            longitude.setText("2.3522");
        });
        btnNewYork.setOnAction(e -> {
            latitude.setText("40.7128");
            longitude.setText("-74.0060");
        });
        btnTokyo.setOnAction(e -> {
            latitude.setText("35.6762");
            longitude.setText("139.6503");
        });
        
        presetBox.getChildren().addAll(btnParis, btnNewYork, btnTokyo);
        
        grid.add(new Label("📍 Latitude:"), 0, 0);
        grid.add(latitude, 1, 0);
        grid.add(new Label("📍 Longitude:"), 0, 1);
        grid.add(longitude, 1, 1);
        grid.add(exampleLabel, 0, 2, 2, 1);
        grid.add(new Label("🗺️ Préréglages:"), 0, 3);
        grid.add(presetBox, 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == checkButtonType) {
                return new String[]{latitude.getText(), longitude.getText()};
            }
            return null;
        });
        
        Optional<String[]> result = dialog.showAndWait();
        result.ifPresent(coords -> {
            appendToApiResults("🌫️ Vérification de la qualité de l'air en cours...\n");
            
            new Thread(() -> {
                try {
                    double lat = Double.parseDouble(coords[0]);
                    double lon = Double.parseDouble(coords[1]);
                    
                    AirPollutionResponse response = airQualityService.getCurrentAirQuality(lat, lon);
                    
                    Platform.runLater(() -> {
                        if (response != null && response.getList() != null && !response.getList().isEmpty()) {
                            appendToApiResults(formatAirQualityResponse(response, lat, lon));
                        } else {
                            appendToApiResults("❌ Aucune donnée de qualité de l'air retournée.\n");
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> appendToApiResults("❌ Erreur: " + e.getMessage() + "\n"));
                }
            }).start();
        });
    }
    
    private String formatCarbonEstimateResponse(CarbonEstimateResponse response) {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════════════\n");
        sb.append("✅ RÉSULTAT DU CALCUL D'ÉMISSIONS CO₂\n");
        sb.append("═══════════════════════════════════════════════════\n\n");
        
        if (response.getAttributes() != null) {
            var attrs = response.getAttributes();
            
            sb.append(String.format("🌍 Émissions CO₂: %.3f kg\n", attrs.getCarbonKg()));
            sb.append(String.format("📊 Équivalent: %.6f tonnes\n", attrs.getCarbonMt()));
            
            if (attrs.getEstimatedAt() != null) {
                sb.append(String.format("⏰ Calculé le: %s\n", attrs.getEstimatedAt()));
            }
        }
        
        sb.append("\n═══════════════════════════════════════════════════\n\n");
        return sb.toString();
    }
    
    private String formatAirQualityResponse(AirPollutionResponse response, double lat, double lon) {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════════════\n");
        sb.append("✅ QUALITÉ DE L'AIR\n");
        sb.append("═══════════════════════════════════════════════════\n\n");
        
        sb.append(String.format("📍 Localisation: %.4f, %.4f\n\n", lat, lon));
        
        if (!response.getList().isEmpty()) {
            var data = response.getList().get(0);
            
            if (data.getMain() != null) {
                int aqi = data.getMain().getAqi();
                sb.append(String.format("🌫️ Indice Qualité Air (AQI): %d - %s\n\n", 
                    aqi, getAqiDescription(aqi)));
            }
            
            if (data.getComponents() != null) {
                var comp = data.getComponents();
                sb.append("📊 COMPOSANTS (μg/m³):\n");
                sb.append(String.format("  • CO (Monoxyde de carbone): %.2f\n", comp.getCo()));
                sb.append(String.format("  • NO₂ (Dioxyde d'azote): %.2f\n", comp.getNo2()));
                sb.append(String.format("  • O₃ (Ozone): %.2f\n", comp.getO3()));
                sb.append(String.format("  • PM2.5 (Particules fines): %.2f\n", comp.getPm2_5()));
                sb.append(String.format("  • PM10 (Particules): %.2f\n", comp.getPm10()));
                sb.append(String.format("  • SO₂ (Dioxyde de soufre): %.2f\n", comp.getSo2()));
            }
        }
        
        sb.append("\n═══════════════════════════════════════════════════\n\n");
        return sb.toString();
    }
    
    private String getAqiDescription(int aqi) {
        switch (aqi) {
            case 1: return "Bon ✅";
            case 2: return "Moyen 🟡";
            case 3: return "Modéré 🟠";
            case 4: return "Mauvais 🔴";
            case 5: return "Très mauvais ⛔";
            default: return "Inconnu";
        }
    }
    
    private void appendToApiResults(String text) {
        if (txtApiResults != null) {
            txtApiResults.appendText(text);
        }
    }

    private String buildCarbonApiErrorMessage() {
        String lastError = carbonApiService != null ? carbonApiService.getLastError() : null;
        if (lastError == null || lastError.trim().isEmpty()) {
            return "❌ Aucune donnée retournée par l'API.\n";
        }
        return "❌ API Carbon Error: " + lastError + "\n";
    }

    private boolean ensureCarbonApiAvailable() {
        if (carbonApiService == null || !carbonApiService.isEnabled()) {
            appendToApiResults("❌ Carbon API non configurée. Ajoutez CARBON_API_KEY ou carbon.api.key.\n");
            showWarning("API non configurée", "La clé Carbon API est manquante. Ajoutez CARBON_API_KEY ou carbon.api.key.");
            return false;
        }
        return true;
    }

    private double convertWeightToKg(double value, String unit) {
        if (unit == null) return value;
        switch (unit) {
            case "lb":
                return value * 0.453592;
            case "g":
                return value / 1000.0;
            case "mt":
                return value * 1000.0;
            case "kg":
            default:
                return value;
        }
    }

    private double convertDistanceToKm(double value, String unit) {
        if (unit == null) return value;
        switch (unit) {
            case "mi":
                return value * 1.60934;
            case "km":
            default:
                return value;
        }
    }
}
