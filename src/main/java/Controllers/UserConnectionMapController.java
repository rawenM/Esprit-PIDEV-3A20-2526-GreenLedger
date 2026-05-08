package Controllers;

import Models.User;
import Services.IUserService;
import Services.UserServiceImpl;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class UserConnectionMapController {

    @FXML private WebView mapWebView;
    @FXML private Label totalUsersLabel;
    @FXML private Label locatedUsersLabel;
    @FXML private Label countriesLabel;

    private final IUserService userService = new UserServiceImpl();
    private WebEngine webEngine;

    // ── cached local assets ──────────────────────────────────────────────────
    private static String LEAFLET_JS  = null;
    private static String LEAFLET_CSS = null;
    private static String CLUSTER_JS  = null;
    private static String CLUSTER_CSS = null;
    private static String CLUSTER_DEF_CSS = null;

    @FXML
    public void initialize() {
        webEngine = mapWebView.getEngine();
        loadAssets();
        Platform.runLater(() -> {
            loadMap();
            updateStatistics();
        });
    }

    // ── asset loader ─────────────────────────────────────────────────────────
    private void loadAssets() {
        LEAFLET_JS       = readResource("/js/leaflet.js");
        LEAFLET_CSS      = readResource("/js/leaflet.css");
        CLUSTER_JS       = readResource("/js/leaflet.markercluster.js");
        CLUSTER_CSS      = readResource("/js/MarkerCluster.css");
        CLUSTER_DEF_CSS  = readResource("/js/MarkerCluster.Default.css");
    }

    private static String readResource(String path) {
        try (InputStream is = UserConnectionMapController.class.getResourceAsStream(path)) {
            if (is == null) { System.err.println("[Map] Missing: " + path); return ""; }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("[Map] Error reading " + path + ": " + e.getMessage());
            return "";
        }
    }

    // ── map loading ──────────────────────────────────────────────────────────
    private void loadMap() {
        List<User> users = userService.getAllUsers();

        List<User> locatedUsers = users.stream()
            .filter(u -> u.getLastLoginLat() != null && u.getLastLoginLng() != null)
            .toList();

        // If no GPS data, use address-based fake coords for demo
        JsonArray features = new JsonArray();

        if (locatedUsers.isEmpty()) {
            // Show all users with approximate Algeria coords for demo
            double[][] demoPts = {
                {36.7372, 3.0865},  // Algiers
                {36.3650, 6.6147},  // Constantine
                {35.6969, -0.6331}, // Oran
                {36.4600, 2.8277},  // Blida
                {36.1898, 5.4136},  // Sétif
                {34.8500, 5.7333},  // Batna
            };
            int idx = 0;
            for (User user : users) {
                double[] pt = demoPts[idx % demoPts.length];
                features.add(buildFeature(user, pt[0], pt[1]));
                idx++;
            }
        } else {
            for (User user : locatedUsers) {
                features.add(buildFeature(user,
                    user.getLastLoginLat(), user.getLastLoginLng()));
            }
        }

        JsonObject geojson = new JsonObject();
        geojson.addProperty("type", "FeatureCollection");
        geojson.add("features", features);

        webEngine.loadContent(buildHtml(geojson.toString()));
    }

    private JsonObject buildFeature(User user, double lat, double lng) {
        JsonObject feature = new JsonObject();
        feature.addProperty("type", "Feature");

        JsonObject geometry = new JsonObject();
        geometry.addProperty("type", "Point");
        JsonArray coords = new JsonArray();
        coords.add(lng); coords.add(lat);
        geometry.add("coordinates", coords);
        feature.add("geometry", geometry);

        JsonObject props = new JsonObject();
        props.addProperty("name",    safe(user.getNom()) + " " + safe(user.getPrenom()));
        props.addProperty("email",   safe(user.getEmail()));
        props.addProperty("type",    user.getTypeUtilisateur() != null
                                        ? user.getTypeUtilisateur().name() : "INCONNU");
        props.addProperty("city",    safe(user.getLastLoginCity()));
        props.addProperty("country", safe(user.getLastLoginCountry()));
        props.addProperty("status",  user.getStatut() != null
                                        ? user.getStatut().name() : "—");
        feature.add("properties", props);
        return feature;
    }

    private static String safe(String s) { return s != null ? s : ""; }

    // ── HTML builder (fully offline) ─────────────────────────────────────────
    private String buildHtml(String geojsonData) {
        return "<!DOCTYPE html><html><head><meta charset='utf-8'>" +
            "<style>" + LEAFLET_CSS + "</style>" +
            "<style>" + CLUSTER_CSS + "</style>" +
            "<style>" + CLUSTER_DEF_CSS + "</style>" +
            "<style>" +
            "* { margin:0; padding:0; box-sizing:border-box; }" +
            "html,body { width:100%; height:100%; }" +
            "#map { width:100%; height:100%; }" +
            ".user-popup { font-family: 'Segoe UI', sans-serif; min-width:200px; }" +
            ".user-popup h3 { margin:0 0 8px 0; font-size:14px; color:#0ea5e9; }" +
            ".user-popup p  { margin:3px 0; font-size:12px; color:#374151; }" +
            ".user-popup .lbl { font-weight:600; color:#111827; }" +
            ".user-popup .badge { display:inline-block; padding:2px 8px; border-radius:999px;" +
            "  font-size:11px; font-weight:700; margin-top:4px; }" +
            ".badge-inv  { background:rgba(16,185,129,0.12); color:#059669; }" +
            ".badge-port { background:rgba(245,158,11,0.12);  color:#d97706; }" +
            ".badge-exp  { background:rgba(59,130,246,0.12);  color:#2563eb; }" +
            ".badge-adm  { background:rgba(139,92,246,0.12);  color:#7c3aed; }" +
            "</style>" +
            "</head><body>" +
            "<div id='map'></div>" +
            "<script>" + LEAFLET_JS + "</script>" +
            "<script>" + CLUSTER_JS + "</script>" +
            "<script>" +
            "(function(){" +
            // Map init — center on Algeria
            "var map = L.map('map',{zoomControl:true}).setView([28.0,3.0],5);" +
            // OSM tiles
            "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{" +
            "  attribution:'© <a href=\"https://www.openstreetmap.org/copyright\">OpenStreetMap</a>'," +
            "  maxZoom:18" +
            "}).addTo(map);" +
            // Cluster group
            "var cluster = L.markerClusterGroup({" +
            "  spiderfyOnMaxZoom:true, showCoverageOnHover:false, zoomToBoundsOnClick:true" +
            "});" +
            // Color map
            "var colors = {" +
            "  'INVESTISSEUR':'#10B981'," +
            "  'PORTEUR_PROJET':'#F59E0B'," +
            "  'EXPERT_CARBONE':'#3B82F6'," +
            "  'ADMIN':'#8B5CF6'" +
            "};" +
            "var badgeClass = {" +
            "  'INVESTISSEUR':'badge-inv'," +
            "  'PORTEUR_PROJET':'badge-port'," +
            "  'EXPERT_CARBONE':'badge-exp'," +
            "  'ADMIN':'badge-adm'" +
            "};" +
            // GeoJSON data
            "var data = " + geojsonData + ";" +
            "L.geoJSON(data,{" +
            "  pointToLayer: function(f,ll){" +
            "    var c = colors[f.properties.type] || '#6B7280';" +
            "    var m = L.circleMarker(ll,{" +
            "      radius:9, fillColor:c, color:'#fff'," +
            "      weight:2, opacity:1, fillOpacity:0.85" +
            "    });" +
            "    var bc = badgeClass[f.properties.type] || 'badge-inv';" +
            "    var p = f.properties;" +
            "    m.bindPopup(" +
            "      '<div class=\"user-popup\">' +" +
            "      '<h3>' + p.name + '</h3>' +" +
            "      '<p><span class=\"lbl\">Email:</span> ' + p.email + '</p>' +" +
            "      '<p><span class=\"lbl\">Statut:</span> ' + p.status + '</p>' +" +
            "      '<span class=\"badge ' + bc + '\">' + p.type.replace('_',' ') + '</span>' +" +
            "      '</div>'" +
            "    );" +
            "    return m;" +
            "  }," +
            "  onEachFeature: function(f,l){ cluster.addLayer(l); }" +
            "});" +
            "map.addLayer(cluster);" +
            "if(cluster.getBounds().isValid()){" +
            "  map.fitBounds(cluster.getBounds(),{padding:[40,40]});" +
            "}" +
            "})();" +
            "</script></body></html>";
    }

    // ── statistics ───────────────────────────────────────────────────────────
    private void updateStatistics() {
        List<User> all = userService.getAllUsers();
        if (totalUsersLabel   != null) totalUsersLabel.setText(String.valueOf(all.size()));

        long located = all.stream()
            .filter(u -> u.getLastLoginLat() != null && u.getLastLoginLng() != null)
            .count();
        if (locatedUsersLabel != null) locatedUsersLabel.setText(located + " utilisateur(s) localisé(s)");

        long countries = all.stream()
            .filter(u -> u.getLastLoginCountry() != null)
            .map(User::getLastLoginCountry).distinct().count();
        if (countriesLabel    != null) countriesLabel.setText(String.valueOf(countries));
    }

    // ── FXML handlers ────────────────────────────────────────────────────────
    @FXML private void handleRefresh()   { loadMap(); updateStatistics(); }
    @FXML private void handleZoomIn()    { webEngine.executeScript("map.zoomIn();"); }
    @FXML private void handleZoomOut()   { webEngine.executeScript("map.zoomOut();"); }
    @FXML private void handleResetView() { webEngine.executeScript("map.setView([28.0,3.0],5);"); }

    @FXML
    private void handleBackToList() {
        try { org.GreenLedger.MainFX.setRoot("fxml/admin_shell"); }
        catch (Exception e) { System.err.println("[Map] Back error: " + e.getMessage()); }
    }
}
