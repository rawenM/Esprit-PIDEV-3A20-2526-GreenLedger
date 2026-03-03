package Controllers;

import Models.Projet;
import Services.AirQualityService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
     * MapViewController - Manages interactive Leaflet.js pollution map.
     * 
     * Features:
     * - Real-time air quality visualization (OpenWeatherMap API)
     * - Carbon project markers with location data
     * - Interactive pollution heatmap overlays
     * - JavaFX ↔ JavaScript bridge for seamless integration
     * - Smooth flyTo animations for location focus
     * - Click handlers trigger JavaFX panel actions
     * 
     * @author GreenLedger Team
     */
public class MapViewController {
    
    private WebView webView;
    private WebEngine webEngine;
    private final AirQualityService airQualityService;
    private final Gson gson;
    private boolean mapInitialized = false;
    
    // Callback for when user clicks project on map
    private ProjectSelectionCallback onProjectSelectedCallback;
    
    public interface ProjectSelectionCallback {
        void onProjectSelected(int projectId, String projectName);
    }
    
    public MapViewController() {
        this.airQualityService = new AirQualityService();
        this.gson = new Gson();
    }
    
    /**
     * Initialize the map view with WebView.
     * Call this from your controller's initialize() method.
     * 
     * @param webView JavaFX WebView component
     */
    public void initialize(WebView webView) {
        this.webView = webView;
        this.webEngine = webView.getEngine();
        
        // Enable JavaScript
        webEngine.setJavaScriptEnabled(true);
        
        // Monitor loading state
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                onMapLoaded();
            } else if (newState == Worker.State.FAILED) {
                System.err.println("[MAP] Failed to load pollution map");
            }
        });
        
        // Load the map HTML
        String mapPath = getClass().getResource("/map/pollution_map.html").toExternalForm();
        webEngine.load(mapPath);
        
        System.out.println("[MAP] MapViewController initialized");
    }
    
    /**
     * Called when map HTML is loaded and ready.
     */
    private void onMapLoaded() {
        try {
            // Set up JavaFX ↔ JavaScript bridge
            JSObject window = (JSObject) webEngine.executeScript("window");
            window.setMember("javaFxBridge", new JavaScriptBridge());
            
            // Initialize the Leaflet map (California default)
            executeScript("pollutionMap.init(36.7783, -119.4179, 6)");
            
            mapInitialized = true;
            System.out.println("[MAP] Leaflet map initialized with JavaScript bridge");
            
        } catch (Exception e) {
            System.err.println("[MAP] Failed to set up JavaScript bridge: " + e.getMessage());
        }
    }
    
    /**
     * Add pollution data point to map.
     * 
     * @param latitude Latitude
     * @param longitude Longitude
     * @param aqi Air Quality Index (0-500)
     * @param pollutantData Optional detailed pollutant breakdown (JSON)
     */
    public void addPollutionPoint(double latitude, double longitude, double aqi, String pollutantData) {
        if (!mapInitialized) {
            System.err.println("[MAP] Cannot add pollution - map not initialized");
            return;
        }
        
        String pollutantJson = pollutantData != null ? 
            "'" + pollutantData.replace("'", "\\'") + "'" : "null";
        
        String script = String.format("pollutionMap.addPollutionPoint(%f, %f, %f, %s)",
            latitude, longitude, aqi, pollutantJson);
        
        executeScript(script);
    }
    
    /**
     * Add carbon project marker to map.
     * 
     * @param project Carbon project with location data
     */
    public void addProjectMarker(Projet project) {
        if (!mapInitialized || project.getLatitude() == null || project.getLongitude() == null) {
            return;
        }
        
        // Use titre for project name (Projet class doesn't have carbon reduction data yet)
        String verification = "Verified";
        double co2eOffset = 0.0;
        
        String script = String.format(
            "pollutionMap.addProjectMarker(%d, %f, %f, '%s', %f, '%s')",
            project.getId(),
            project.getLatitude(),
            project.getLongitude(),
            escapeSingleQuotes(project.getTitre()),
            co2eOffset,
            verification
        );
        
        executeScript(script);
    }
    
    /**
     * Load air quality data and visualize on map.
     * Fetches pollution data from OpenWeatherMap API.
     * 
     * @param locations List of coordinates to check
     */
    public void loadAirQualityData(List<Projet> locations) {
        CompletableFuture.runAsync(() -> {
            for (Projet project : locations) {
                if (project.getLatitude() != null && project.getLongitude() != null) {
                    try {
                        var airQualityResponse = airQualityService.getCurrentAirQuality(
                            project.getLatitude(),
                            project.getLongitude()
                        );
                        
                        if (airQualityResponse != null) {
                            var currentReading = airQualityResponse.getCurrentReading();
                            if (currentReading != null && currentReading.getMain() != null) {
                                double aqi = currentReading.getMain().getAqi();
                                String pollutantJson = gson.toJson(currentReading.getComponents());
                                
                                Platform.runLater(() -> 
                                    addPollutionPoint(
                                        project.getLatitude(),
                                        project.getLongitude(),
                                        aqi,
                                        pollutantJson
                                    )
                                );
                            }
                        }
                        
                        // Rate limiting - wait between API calls
                        Thread.sleep(200);
                        
                    } catch (Exception e) {
                        System.err.println("[MAP] Failed to fetch air quality for project " + 
                            project.getId() + ": " + e.getMessage());
                    }
                }
            }
        });
    }
    
    /**
     * Fly to specific location with smooth animation.
     * 
     * @param latitude Target latitude
     * @param longitude Target longitude
     * @param zoom Zoom level (default 12)
     */
    public void flyToLocation(double latitude, double longitude, int zoom) {
        if (!mapInitialized) return;
        
        String script = String.format("pollutionMap.flyTo(%f, %f, %d)",
            latitude, longitude, zoom);
        executeScript(script);
    }
    
    /**
     * Highlight specific project and fly to its location.
     * 
     * @param projectId Project ID to highlight
     */
    public void highlightProject(int projectId) {
        if (!mapInitialized) return;
        
        String script = String.format("pollutionMap.highlightProject(%d)", projectId);
        executeScript(script);
    }
    
    /**
     * Clear all pollution circles from map.
     */
    public void clearPollutionData() {
        if (!mapInitialized) return;
        executeScript("pollutionMap.clearPollution()");
    }
    
    /**
     * Clear all project markers from map.
     */
    public void clearProjectMarkers() {
        if (!mapInitialized) return;
        executeScript("pollutionMap.clearProjects()");
    }
    
    /**
     * Set callback for when user clicks project on map.
     * This triggers the "Issue Credits" panel to open.
     * 
     * @param callback Callback function
     */
    public void setOnProjectSelected(ProjectSelectionCallback callback) {
        this.onProjectSelectedCallback = callback;
    }
    
    /**
     * Execute JavaScript in WebEngine safely.
     */
    private void executeScript(String script) {
        Platform.runLater(() -> {
            try {
                webEngine.executeScript(script);
            } catch (Exception e) {
                System.err.println("[MAP] Script execution failed: " + script);
                System.err.println("    Error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Escape single quotes for JavaScript injection safety.
     */
    private String escapeSingleQuotes(String text) {
        return text != null ? text.replace("'", "\\'") : "";
    }
    
    /**
     * JavaScript bridge class for callbacks from Leaflet to JavaFX.
     * This is exposed to JavaScript via JSObject.
     */
    public class JavaScriptBridge {
        
        /**
         * Called from JavaScript when user clicks project marker.
         * Triggers JavaFX panel to open with project pre-selected.
         * 
         * @param projectId Project ID
         * @param projectName Project name
         */
        public void onProjectSelected(int projectId, String projectName) {
            Platform.runLater(() -> {
                if (onProjectSelectedCallback != null) {
                    onProjectSelectedCallback.onProjectSelected(projectId, projectName);
                    System.out.println("[MAP] Project selected: " + projectName + " (ID: " + projectId + ")");
                } else {
                    System.out.println("[MAP] Project clicked but no callback set: " + projectName);
                }
            });
        }
        
        /**
         * Log messages from JavaScript console.
         * 
         * @param message Log message
         */
        public void log(String message) {
            System.out.println("[MAP JS] " + message);
        }
        
        /**
         * Error logging from JavaScript.
         * 
         * @param error Error message
         */
        public void error(String error) {
            System.err.println("[MAP JS ERROR] " + error);
        }
    }
    
    /**
     * Get WebView component.
     * 
     * @return WebView instance
     */
    public WebView getWebView() {
        return webView;
    }
    
    /**
     * Check if map is ready for interaction.
     * 
     * @return true if initialized
     */
    public boolean isMapReady() {
        return mapInitialized;
    }
}
