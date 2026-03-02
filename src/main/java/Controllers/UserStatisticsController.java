package Controllers;

import Models.StatutUtilisateur;
import Models.User;
import Services.IUserService;
import Services.UserServiceImpl;
import Utils.ChartDataService;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Contrôleur pour la page de statistiques avec Chart.js
 */
public class UserStatisticsController {

    @FXML private WebView chartsWebView;
    @FXML private Label totalUsersLabel;
    @FXML private Label activeUsersLabel;
    @FXML private Label newUsersLabel;
    @FXML private Label fraudRateLabel;

    private final IUserService userService = new UserServiceImpl();
    private final ChartDataService chartDataService = new ChartDataService();
    private WebEngine webEngine;
    private List<User> allUsers;

    @FXML
    public void initialize() {
        System.out.println("[CHARTS] Initialisation du contrôleur de statistiques...");
        
        // Charger les utilisateurs
        loadUsers();
        
        // Initialiser le WebView avec Chart.js
        initializeCharts();
        
        // Désactiver le zoom du WebView pour permettre le scroll
        if (chartsWebView != null) {
            chartsWebView.setZoom(1.0);
            chartsWebView.setContextMenuEnabled(false);
        }
    }

    /**
     * Charge tous les utilisateurs
     */
    private void loadUsers() {
        try {
            allUsers = userService.getAllUsers();
            System.out.println("[CHARTS] " + allUsers.size() + " utilisateurs chargés");
            updateStatistics();
        } catch (Exception e) {
            System.err.println("[CHARTS] Erreur lors du chargement des utilisateurs: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Met à jour les statistiques rapides
     */
    private void updateStatistics() {
        if (allUsers == null) return;

        // Total
        totalUsersLabel.setText(String.valueOf(allUsers.size()));

        // Actifs
        long activeCount = allUsers.stream()
            .filter(u -> u.getStatut() == StatutUtilisateur.ACTIVE)
            .count();
        activeUsersLabel.setText(String.valueOf(activeCount));

        // Nouveaux (30 derniers jours)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long newCount = allUsers.stream()
            .filter(u -> u.getDateInscription() != null)
            .filter(u -> u.getDateInscription().isAfter(thirtyDaysAgo))
            .count();
        newUsersLabel.setText(String.valueOf(newCount));

        // Taux de fraude (utilisateurs avec score > 70)
        // Pour l'instant, afficher 0% - sera calculé avec les vraies données
        fraudRateLabel.setText("0%");
    }

    /**
     * Initialise les graphiques Chart.js
     */
    private void initializeCharts() {
        if (chartsWebView == null) {
            System.err.println("[CHARTS] WebView est null!");
            return;
        }

        webEngine = chartsWebView.getEngine();
        webEngine.setJavaScriptEnabled(true);

        // Écouter les changements d'état
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            System.out.println("[CHARTS] WebView state: " + newState);
            
            if (newState == Worker.State.SUCCEEDED) {
                System.out.println("[CHARTS] Page chargée avec succès");
                
                // Attendre un peu que Chart.js soit initialisé
                Platform.runLater(() -> {
                    try {
                        Thread.sleep(500);
                        updateChartsData();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            } else if (newState == Worker.State.FAILED) {
                System.err.println("[CHARTS] Échec du chargement de la page");
            }
        });

        // Charger le fichier HTML
        try {
            String htmlPath = getClass().getResource("/charts/user-statistics.html").toExternalForm();
            System.out.println("[CHARTS] Chargement de: " + htmlPath);
            webEngine.load(htmlPath);
        } catch (Exception e) {
            System.err.println("[CHARTS] Erreur lors du chargement du HTML: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Met à jour les données des graphiques
     */
    private void updateChartsData() {
        try {
            if (allUsers == null || allUsers.isEmpty()) {
                System.out.println("[CHARTS] Aucun utilisateur, utilisation de données de test");
                String sampleData = chartDataService.generateSampleData();
                executeJavaScript("updateCharts(" + sampleData + ")");
            } else {
                System.out.println("[CHARTS] Génération des données pour " + allUsers.size() + " utilisateurs");
                String chartData = chartDataService.generateChartData(allUsers);
                System.out.println("[CHARTS] Données générées: " + chartData);
                executeJavaScript("updateCharts(" + chartData + ")");
            }
        } catch (Exception e) {
            System.err.println("[CHARTS] Erreur lors de la mise à jour des graphiques: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Exécute du JavaScript dans le WebView
     */
    private void executeJavaScript(String script) {
        Platform.runLater(() -> {
            try {
                Object result = webEngine.executeScript(script);
                System.out.println("[CHARTS] Script exécuté: " + script.substring(0, Math.min(50, script.length())) + "...");
            } catch (Exception e) {
                System.err.println("[CHARTS] Erreur JavaScript: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Actualiser les données
     */
    @FXML
    private void handleRefresh(ActionEvent event) {
        System.out.println("[CHARTS] Actualisation des données...");
        loadUsers();
        updateChartsData();
    }

    /**
     * Retour au dashboard admin
     */
    @FXML
    private void handleBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/admin_users.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("GreenLedger - Admin Dashboard");
            
            System.out.println("[CHARTS] Retour au dashboard admin");
        } catch (IOException e) {
            System.err.println("[CHARTS] Erreur lors du retour: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
