package Controllers.greenwallet;

import Services.AirQualityService;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.concurrent.Worker;
import netscape.javascript.JSObject;
import java.net.URL;

/**
 * Map Integration Controller - Leaflet.js Interactive Maps
 * 
 * Responsibilities:
 * - Load Leaflet.js map in WebView
 * - Display pollution heatmap with AQI data
 * - Show carbon project markers with custom icons
 * - Handle JavaScript→JavaFX callbacks for project selection
 * - Animate map flyTo when wallet/project changes
 * 
 * Map Features:
 * - OpenStreetMap base tiles with custom styling
 * - AQI heatmap overlay (green→yellow→orange→red→purple)
 * - Project markers with CO₂ impact emoji badges
 * - Click handlers to select projects and post events
 * 
 * Architecture:
 * - pollution_map.html (JavaScript) loaded via WebEngine
 * - JavaScriptBridge inner class exposes Java methods to JavaScript
 * - JSObject for two-way communication
 * 
 * @author GreenLedger Team
 * @version 2.0 - Production Ready (Implemented)
 */
public class MapIntegrationController {
    
    private AirQualityService airQualityService;
    private WebView mapWebView;
    private WebEngine webEngine;
    private boolean mapLoaded = false;
    
    public MapIntegrationController(AirQualityService airQualityService, WebView mapWebView) {
        this.airQualityService = airQualityService;
        this.mapWebView = mapWebView;
        this.webEngine = mapWebView.getEngine();
        
        initializeMap();
    }
    
    /**
     * Load pollution_map.html and initialize Leaflet.js.
     * Load HTML file, enable JavaScript execution, setup JSObject bridge
     */
    private void initializeMap() {
        System.out.println("[MapIntegration] Initializing Leaflet map...");
        
        // Enable JavaScript
        webEngine.setJavaScriptEnabled(true);
        
        // Listen for load completion
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                mapLoaded = true;
                System.out.println("[MapIntegration] Map loaded successfully");
                
                // Inject bridge object
                try {
                    JSObject window = (JSObject) webEngine.executeScript("window");
                    window.setMember("javaMapBridge", new JavaScriptBridge());
                    System.out.println("[MapIntegration] JavaScript bridge established");
                } catch (Exception e) {
                    System.err.println("[MapIntegration] Error setting up bridge: " + e.getMessage());
                }
            } else if (newState == Worker.State.FAILED) {
                System.err.println("[MapIntegration] Failed to load map");
            }
        });
        
        // Try to load pollution_map.html from resources
        try {
            URL mapUrl = getClass().getResource("/map/pollution_map.html");
            if (mapUrl != null) {
                webEngine.load(mapUrl.toExternalForm());
            } else {
                // Load a simple embedded map if file not found
                loadEmbeddedMap();
            }
        } catch (Exception e) {
            System.err.println("[MapIntegration] Error loading map: " + e.getMessage());
            loadEmbeddedMap();
        }
    }
    
    /**
     * Load a simple embedded OpenStreetMap if external HTML not found
     */
    private void loadEmbeddedMap() {
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Pollution Map</title>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <style>
                    body { margin: 0; padding: 0; }
                    #map { position: absolute; top: 0; bottom: 0; width: 100%; }
                </style>
            </head>
            <body>
                <div id="map"></div>
                <script>
                    var map = L.map('map').setView([36.8065, 10.1815], 6); // Tunisia center
                    
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        attribution: '© OpenStreetMap contributors',
                        maxZoom: 19
                    }).addTo(map);
                    
                    var markers = [];
                    var pollutionPoints = [];
                    
                    function addPollutionPoint(lat, lng, aqi) {
                        var color = aqi < 50 ? '#27ae60' : aqi < 100 ? '#f39c12' : '#e74c3c';
                        var circle = L.circle([lat, lng], {
                            color: color,
                            fillColor: color,
                            fillOpacity: 0.4,
                            radius: 5000
                        }).addTo(map);
                        circle.bindPopup('AQI: ' + aqi);
                        pollutionPoints.push(circle);
                    }
                    
                    function addProjectMarker(name, lat, lng, impact, badge) {
                        var marker = L.marker([lat, lng]).addTo(map);
                        marker.bindPopup('<b>' + name + '</b><br>' + 
                                        'Impact: ' + impact + ' tCO₂e<br>' +
                                        'Standard: ' + badge);
                        markers.push(marker);
                    }
                    
                    function flyToLocation(lat, lng) {
                        map.flyTo([lat, lng], 12, { duration: 0.8 });
                    }
                    
                    function clearOverlays() {
                        markers.forEach(m => map.removeLayer(m));
                        pollutionPoints.forEach(p => map.removeLayer(p));
                        markers = [];
                        pollutionPoints = [];
                    }
                </script>
            </body>
            </html>
            """;
        
        webEngine.loadContent(html);
    }
    
    /**
     * Add pollution data point to map (AQI heatmap).
     * Inject JavaScript to add point to heatmap with AQI color
     */
    public void addPollutionPoint(double latitude, double longitude, int aqiValue) {
        if (!mapLoaded) {
            System.err.println("[MapIntegration] Map not loaded yet");
            return;
        }
        
        System.out.println("[MapIntegration] Adding pollution point: AQI=" + aqiValue);
        
        try {
            String script = String.format("addPollutionPoint(%f, %f, %d);", 
                latitude, longitude, aqiValue);
            webEngine.executeScript(script);
        } catch (Exception e) {
            System.err.println("[MapIntegration] Error adding pollution point: " + e.getMessage());
        }
    }
    
    /**
     * Add carbon project marker to map.
     * Inject JavaScript to add project marker with icon
     */
    public void addProjectMarker(String projectName, double latitude, double longitude, 
                                 double co2Impact, String standardBadge) {
        if (!mapLoaded) {
            System.err.println("[MapIntegration] Map not loaded yet");
            return;
        }
        
        System.out.println("[MapIntegration] Adding project marker: " + projectName);
        
        try {
            // Escape quotes in project name
            String safeName = projectName.replace("'", "\\\\'");
            String safeBadge = standardBadge.replace("'", "\\\\'");
            
            String script = String.format("addProjectMarker('%s', %f, %f, %f, '%s');", 
                safeName, latitude, longitude, co2Impact, safeBadge);
            webEngine.executeScript(script);
        } catch (Exception e) {
            System.err.println("[MapIntegration] Error adding project marker: " + e.getMessage());
        }
    }
    
    /**
     * Animate map to location with smooth flyTo transition.
     * Call JavaScript flyTo(lat, lng, zoom) with 800ms duration
     */
    public void flyToLocation(double latitude, double longitude) {
        if (!mapLoaded) {
            System.err.println("[MapIntegration] Map not loaded yet");
            return;
        }
        
        System.out.println("[MapIntegration] Flying to location: " + latitude + ", " + longitude);
        
        try {
            String script = String.format("flyToLocation(%f, %f);", latitude, longitude);
            webEngine.executeScript(script);
        } catch (Exception e) {
            System.err.println("[MapIntegration] Error flying to location: " + e.getMessage());
        }
    }
    
    /**
     * Load air quality data for region and render heatmap.
     * Call AirQualityService async, then inject data into map
     */
    public void loadAirQualityData(double latitude, double longitude) {
        System.out.println("[MapIntegration] Loading air quality data for: " + latitude + ", " + longitude);
        
        // Async call to air quality service
        new Thread(() -> {
            try {
                if (airQualityService != null) {
                    var airQualityData = airQualityService.getCurrentAirQuality(latitude, longitude);
                    
                    if (airQualityData != null) {
                        // Inject data into map on JavaFX thread
                        javafx.application.Platform.runLater(() -> {
                            // Example: Add pollution points from air quality data
                            if (airQualityData.getList() != null && airQualityData.getCoord() != null) {
                                double lat = airQualityData.getCoord().getLat();
                                double lng = airQualityData.getCoord().getLon();
                                
                                for (var data : airQualityData.getList()) {
                                    // Add pollution data point to map
                                    try {
                                        if (data.getMain() != null && data.getMain().getAqi() != null) {
                                            int aqi = data.getMain().getAqi();
                                            addPollutionPoint(lat, lng, aqi);
                                        }
                                    } catch (Exception e) {
                                        System.err.println("[MapIntegration] Error processing air quality data: " + e.getMessage());
                                    }
                                }
                            }
                        });
                    }
                } else {
                    System.out.println("[MapIntegration] Air quality service not available, using mock data");
                    // Add some mock data points
                    javafx.application.Platform.runLater(() -> {
                        addPollutionPoint(36.8065, 10.1815, 75);  // Tunis
                        addPollutionPoint(35.8256, 10.6346, 45);  // Sousse
                        addPollutionPoint(33.8869, 10.0982, 90);  // Sfax
                    });
                }
            } catch (Exception e) {
                System.err.println("[MapIntegration] Error loading air quality data: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Clear all overlay data (pollution points and project markers).
     * Call JavaScript to clear layers
     */
    public void clearOverlays() {
        if (!mapLoaded) return;
        
        System.out.println("[MapIntegration] Clearing map overlays...");
        
        try {
            webEngine.executeScript("clearOverlays();");
        } catch (Exception e) {
            System.err.println("[MapIntegration] Error clearing overlays: " + e.getMessage());
        }
    }
    
    /**
     * Enable fullscreen mode for map.
     * Expand WebView to fullscreen, hide sidebar/panels
     */
    public void enterFullscreen() {
        System.out.println("[MapIntegration] Entering fullscreen...");
        
        if (mapWebView != null) {
            javafx.stage.Stage stage = (javafx.stage.Stage) mapWebView.getScene().getWindow();
            if (stage != null) {
                stage.setFullScreen(true);
            }
        }
    }
    
    /**
     * JavaScript bridge class for callbacks from map
     */
    public class JavaScriptBridge {
        public void projectClicked(String projectId, String projectName) {
            System.out.println("[MapIntegration] Project clicked: " + projectName + " (ID: " + projectId + ")");
            
            // Post event to EventBus
            try {
                int id = Integer.parseInt(projectId);
                Utils.EventBusManager.post(new Utils.EventBusManager.MapProjectSelectedEvent(id, projectName));
            } catch (NumberFormatException e) {
                System.err.println("[MapIntegration] Invalid project ID: " + projectId);
            }
        }
        
        public void pollutionPointClicked(double lat, double lng, int aqi) {
            System.out.println(String.format("[MapIntegration] Pollution point clicked: (%.4f, %.4f) AQI=%d", 
                lat, lng, aqi));
        }
    }
    
    public void shutdown() {
        clearOverlays();
        mapLoaded = false;
    }
}
