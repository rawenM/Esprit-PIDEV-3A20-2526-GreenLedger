package Controllers;

import DataBase.MyConnection;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Project Fraud Scoring Controller for Admin Dashboard
 * Monitors fraud detection and risk assessment for projects
 * 
 * Features:
 * - View all projects with fraud scores
 * - Filter by fraud flag (suspected/clean)
 * - View detailed fraud analysis
 * - Track fraud model versions
 * - Monitor fraud trends
 */
public class ProjectFraudScoringController {

    @FXML private TableView<ProjectFraudData> fraudTable;
    @FXML private TableColumn<ProjectFraudData, Integer> idColumn;
    @FXML private TableColumn<ProjectFraudData, String> nameColumn;
    @FXML private TableColumn<ProjectFraudData, String> statusColumn;
    @FXML private TableColumn<ProjectFraudData, Double> riskScoreColumn;
    @FXML private TableColumn<ProjectFraudData, Double> anomalyScoreColumn;
    @FXML private TableColumn<ProjectFraudData, Integer> fraudFlagColumn;
    @FXML private TableColumn<ProjectFraudData, String> modelVersionColumn;
    @FXML private TableColumn<ProjectFraudData, String> scoredAtColumn;
    @FXML private TableColumn<ProjectFraudData, Void> actionsColumn;

    @FXML private Label totalProjectsLabel;
    @FXML private Label suspectsLabel;
    @FXML private Label cleanProjectsLabel;
    @FXML private Label avgRiskScoreLabel;

    @FXML private ComboBox<String> filterFlagCombo;
    @FXML private TextField searchField;
    @FXML private VBox detailsPane;

    private Connection conn;
    private ObservableList<ProjectFraudData> projectsList = FXCollections.observableArrayList();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        conn = MyConnection.getConnection();
        setupTableColumns();
        setupFilters();
        loadProjects();
        updateStatistics();
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(cellData -> 
            new SimpleIntegerProperty(cellData.getValue().getId()).asObject());
        
        nameColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getName()));
        
        statusColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getStatus()));
        
        riskScoreColumn.setCellValueFactory(cellData -> 
            new SimpleDoubleProperty(cellData.getValue().getFraudRiskScore()).asObject());
        riskScoreColumn.setCellFactory(column -> new TableCell<ProjectFraudData, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("%.2f", item));
                    String color = item >= 0.7 ? "#EF4444" : item >= 0.4 ? "#F59E0B" : "#10B981";
                    setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
                }
            }
        });
        
        anomalyScoreColumn.setCellValueFactory(cellData -> 
            new SimpleDoubleProperty(cellData.getValue().getFraudAnomalyScore()).asObject());
        anomalyScoreColumn.setCellFactory(column -> new TableCell<ProjectFraudData, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", item));
                }
            }
        });
        
        fraudFlagColumn.setCellValueFactory(cellData -> 
            new SimpleIntegerProperty(cellData.getValue().getFraudFlag()).asObject());
        fraudFlagColumn.setCellFactory(column -> new TableCell<ProjectFraudData, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item == 1 ? "⚠️ SUSPECT" : "✓ CLEAN");
                    String color = item == 1 ? "#EF4444" : "#10B981";
                    setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                            "-fx-font-weight: bold; -fx-padding: 5; -fx-background-radius: 5;");
                }
            }
        });
        
        modelVersionColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getFraudModelVersion()));
        
        scoredAtColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getFormattedScoredAt()));
        
        // Actions column with details button
        actionsColumn.setCellFactory(column -> new TableCell<ProjectFraudData, Void>() {
            private final Button detailsBtn = new Button("📊 Détails");
            {
                detailsBtn.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white;");
                detailsBtn.setOnAction(e -> {
                    ProjectFraudData project = getTableView().getItems().get(getIndex());
                    showFraudDetails(project);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : detailsBtn);
            }
        });
    }

    private void setupFilters() {
        filterFlagCombo.getItems().addAll("Tous", "Suspects uniquement", "Projets sains");
        filterFlagCombo.setValue("Tous");
    }

    private void loadProjects() {
        projectsList.clear();
        
        String sql = "SELECT id, nom, status, fraud_risk_score, fraud_anomaly_score, " +
                     "fraud_flag, fraud_reasons, fraud_model_version, fraud_scored_at " +
                     "FROM projet " +
                     "WHERE fraud_scored_at IS NOT NULL " +
                     "ORDER BY fraud_risk_score DESC";
        
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            
            while (rs.next()) {
                ProjectFraudData project = new ProjectFraudData(
                    rs.getInt("id"),
                    rs.getString("nom"),
                    rs.getString("status"),
                    rs.getDouble("fraud_risk_score"),
                    rs.getDouble("fraud_anomaly_score"),
                    rs.getInt("fraud_flag"),
                    rs.getString("fraud_reasons"),
                    rs.getString("fraud_model_version"),
                    rs.getTimestamp("fraud_scored_at") != null ? 
                        rs.getTimestamp("fraud_scored_at").toLocalDateTime() : null
                );
                projectsList.add(project);
            }
            
            fraudTable.setItems(projectsList);
            
        } catch (SQLException e) {
            System.err.println("[FraudScoring] Error loading projects: " + e.getMessage());
            showError("Erreur", "Impossible de charger les projets: " + e.getMessage());
        }
    }

    private void updateStatistics() {
        try {
            // Total projects analyzed
            int total = projectsList.size();
            totalProjectsLabel.setText(String.valueOf(total));
            
            // Suspects (fraud_flag = 1)
            long suspects = projectsList.stream()
                .filter(p -> p.getFraudFlag() == 1)
                .count();
            suspectsLabel.setText(String.valueOf(suspects));
            
            // Clean projects
            long clean = total - suspects;
            cleanProjectsLabel.setText(String.valueOf(clean));
            
            // Average risk score
            double avgRisk = projectsList.stream()
                .mapToDouble(ProjectFraudData::getFraudRiskScore)
                .average()
                .orElse(0.0);
            avgRiskScoreLabel.setText(String.format("%.2f", avgRisk));
            
        } catch (Exception e) {
            System.err.println("[FraudScoring] Error updating statistics: " + e.getMessage());
        }
    }

    @FXML
    private void handleFilter() {
        String filterValue = filterFlagCombo.getValue();
        String searchTerm = searchField.getText().toLowerCase().trim();
        
        ObservableList<ProjectFraudData> filtered = FXCollections.observableArrayList();
        
        for (ProjectFraudData project : projectsList) {
            // Filter by fraud flag
            boolean matchesFlag = true;
            if ("Suspects uniquement".equals(filterValue)) {
                matchesFlag = project.getFraudFlag() == 1;
            } else if ("Projets sains".equals(filterValue)) {
                matchesFlag = project.getFraudFlag() == 0;
            }
            
            // Filter by search term
            boolean matchesSearch = searchTerm.isEmpty() ||
                project.getName().toLowerCase().contains(searchTerm) ||
                String.valueOf(project.getId()).contains(searchTerm);
            
            if (matchesFlag && matchesSearch) {
                filtered.add(project);
            }
        }
        
        fraudTable.setItems(filtered);
    }

    @FXML
    private void handleRefresh() {
        loadProjects();
        updateStatistics();
        searchField.clear();
        filterFlagCombo.setValue("Tous");
    }

    private void showFraudDetails(ProjectFraudData project) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails Fraude - Projet #" + project.getId());
        alert.setHeaderText(project.getName());
        
        StringBuilder content = new StringBuilder();
        content.append("=== SCORES DE FRAUDE ===\n\n");
        content.append("Score de Risque: ").append(String.format("%.2f", project.getFraudRiskScore())).append("\n");
        content.append("Score d'Anomalie: ").append(String.format("%.2f", project.getFraudAnomalyScore())).append("\n");
        content.append("Statut: ").append(project.getFraudFlag() == 1 ? "⚠️ SUSPECT" : "✓ CLEAN").append("\n\n");
        
        content.append("=== MODÈLE ===\n\n");
        content.append("Version: ").append(project.getFraudModelVersion() != null ? 
            project.getFraudModelVersion() : "N/A").append("\n");
        content.append("Analysé le: ").append(project.getFormattedScoredAt()).append("\n\n");
        
        if (project.getFraudReasons() != null && !project.getFraudReasons().isEmpty()) {
            content.append("=== RAISONS ===\n\n");
            content.append(project.getFraudReasons()).append("\n");
        }
        
        alert.setContentText(content.toString());
        alert.getDialogPane().setPrefWidth(600);
        alert.getDialogPane().setPrefHeight(500);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Inner class to hold project fraud data
     */
    public static class ProjectFraudData {
        private int id;
        private String name;
        private String status;
        private double fraudRiskScore;
        private double fraudAnomalyScore;
        private int fraudFlag;
        private String fraudReasons;
        private String fraudModelVersion;
        private LocalDateTime fraudScoredAt;

        public ProjectFraudData(int id, String name, String status, double fraudRiskScore,
                               double fraudAnomalyScore, int fraudFlag, String fraudReasons,
                               String fraudModelVersion, LocalDateTime fraudScoredAt) {
            this.id = id;
            this.name = name;
            this.status = status;
            this.fraudRiskScore = fraudRiskScore;
            this.fraudAnomalyScore = fraudAnomalyScore;
            this.fraudFlag = fraudFlag;
            this.fraudReasons = fraudReasons;
            this.fraudModelVersion = fraudModelVersion;
            this.fraudScoredAt = fraudScoredAt;
        }

        // Getters
        public int getId() { return id; }
        public String getName() { return name; }
        public String getStatus() { return status; }
        public double getFraudRiskScore() { return fraudRiskScore; }
        public double getFraudAnomalyScore() { return fraudAnomalyScore; }
        public int getFraudFlag() { return fraudFlag; }
        public String getFraudReasons() { return fraudReasons; }
        public String getFraudModelVersion() { return fraudModelVersion; }
        public LocalDateTime getFraudScoredAt() { return fraudScoredAt; }
        
        public String getFormattedScoredAt() {
            return fraudScoredAt != null ? fraudScoredAt.format(DATE_FORMATTER) : "N/A";
        }
    }
}
