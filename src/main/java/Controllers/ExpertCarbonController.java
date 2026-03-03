package Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import Models.CarbonReport;
import Models.User;
import Services.CarbonReportService;
import Utils.SessionManager;
import org.GreenLedger.MainFX;

import java.io.IOException;
import java.util.List;

/**
 * Contrôleur du dashboard de l'Expert Carbone
 * Gère l'évaluation, validation et rejet des rapports d'émissions carbone
 */
public class ExpertCarbonController {

    @FXML private Label userLabel;
    @FXML private Label pendingCountLabel;
    @FXML private Label validatedCountLabel;
    @FXML private Label rejectedCountLabel;

    // Onglet: Rapports en attente
    @FXML private TableView<CarbonReport> pendingReportsTable;
    @FXML private TableColumn<CarbonReport, String> projectNameColumn;
    @FXML private TableColumn<CarbonReport, String> porteurNameColumn;
    @FXML private TableColumn<CarbonReport, String> dateColumn;
    @FXML private TableColumn<CarbonReport, String> detailsColumn;
    @FXML private TableColumn<CarbonReport, Void> actionsColumn;

    // Onglet: Rapports validés
    @FXML private TableView<CarbonReport> validatedReportsTable;
    @FXML private TableColumn<CarbonReport, String> projectNameValidColumn;
    @FXML private TableColumn<CarbonReport, String> porteurNameValidColumn;
    @FXML private TableColumn<CarbonReport, Double> emissionsValidColumn;
    @FXML private TableColumn<CarbonReport, String> dateValidColumn;
    @FXML private TableColumn<CarbonReport, String> commentsValidColumn;

    // Onglet: Rapports rejetés
    @FXML private TableView<CarbonReport> rejectedReportsTable;
    @FXML private TableColumn<CarbonReport, String> projectNameRejColumn;
    @FXML private TableColumn<CarbonReport, String> porteurNameRejColumn;
    @FXML private TableColumn<CarbonReport, String> dateRejColumn;
    @FXML private TableColumn<CarbonReport, String> motifRejColumn;
    @FXML private TableColumn<CarbonReport, Void> actionsRejColumn;

    // Statistiques
    @FXML private Label totalStatsLabel;
    @FXML private Label pendingStatsLabel;
    @FXML private Label validatedStatsLabel;
    @FXML private Label rejectedStatsLabel;
    @FXML private Label averageEmissionsLabel;

    private User currentUser;
    private final CarbonReportService reportService = new CarbonReportService();

    @FXML
    public void initialize() {
        setupPendingTable();
        setupValidatedTable();
        setupRejectedTable();
        refreshAllData();
    }

    private void setupPendingTable() {
        projectNameColumn.setCellValueFactory(new PropertyValueFactory<>("projectName"));
        porteurNameColumn.setCellValueFactory(new PropertyValueFactory<>("porteurProjetName"));
        dateColumn.setCellValueFactory(cellData -> {
            String date = cellData.getValue().getDateCreation().toString().substring(0, 10);
            return new javafx.beans.property.SimpleStringProperty(date);
        });
        detailsColumn.setCellValueFactory(new PropertyValueFactory<>("evaluationDetails"));

        addActionsColumnPending();
    }

    private void setupValidatedTable() {
        projectNameValidColumn.setCellValueFactory(new PropertyValueFactory<>("projectName"));
        porteurNameValidColumn.setCellValueFactory(new PropertyValueFactory<>("porteurProjetName"));
        emissionsValidColumn.setCellValueFactory(new PropertyValueFactory<>("emissionsEstimate"));
        dateValidColumn.setCellValueFactory(cellData -> {
            String date = cellData.getValue().getDateValidation().toString().substring(0, 10);
            return new javafx.beans.property.SimpleStringProperty(date);
        });
        commentsValidColumn.setCellValueFactory(new PropertyValueFactory<>("commentairesExpert"));
    }

    private void setupRejectedTable() {
        projectNameRejColumn.setCellValueFactory(new PropertyValueFactory<>("projectName"));
        porteurNameRejColumn.setCellValueFactory(new PropertyValueFactory<>("porteurProjetName"));
        dateRejColumn.setCellValueFactory(cellData -> {
            String date = cellData.getValue().getDateValidation().toString().substring(0, 10);
            return new javafx.beans.property.SimpleStringProperty(date);
        });
        motifRejColumn.setCellValueFactory(new PropertyValueFactory<>("commentairesExpert"));

        addActionsColumnRejected();
    }

    private void addActionsColumnPending() {
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button validateBtn = new Button("[CLEAN] Valider");
            private final Button rejectBtn = new Button("[CLEAN] Rejeter");

            {
                validateBtn.setStyle("-fx-padding: 5 10; -fx-font-size: 10; -fx-background-color: #10B981; -fx-text-fill: white;");
                rejectBtn.setStyle("-fx-padding: 5 10; -fx-font-size: 10; -fx-background-color: #EF4444; -fx-text-fill: white;");

                validateBtn.setOnAction(e -> handleValidateReport(getTableView().getItems().get(getIndex())));
                rejectBtn.setOnAction(e -> handleRejectReport(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(5);
                    hbox.setAlignment(Pos.CENTER);
                    hbox.getChildren().addAll(validateBtn, rejectBtn);
                    setGraphic(hbox);
                }
            }
        });
    }

    private void addActionsColumnRejected() {
        actionsRejColumn.setCellFactory(col -> new TableCell<>() {
            private final Button resubmitBtn = new Button("🔄 Réouvrir");

            {
                resubmitBtn.setStyle("-fx-padding: 5 10; -fx-font-size: 10; -fx-background-color: #3B82F6; -fx-text-fill: white;");
                resubmitBtn.setOnAction(e -> {
                    CarbonReport report = getTableView().getItems().get(getIndex());
                    report.setStatut(CarbonReport.StatutRapport.EN_ATTENTE);
                    refreshAllData();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(resubmitBtn);
                }
            }
        });
    }

    private void handleValidateReport(CarbonReport report) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Valider le rapport");
        dialog.setHeaderText("Évaluation : " + report.getProjectName());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        TextField emissionsField = new TextField();
        emissionsField.setPromptText("Ex: 1500.50");
        TextArea detailsArea = new TextArea();
        detailsArea.setPromptText("Détails de l'évaluation...");
        detailsArea.setPrefRowCount(4);
        TextArea commentairesArea = new TextArea();
        commentairesArea.setPromptText("Commentaires (optionnel)...");
        commentairesArea.setPrefRowCount(3);

        grid.add(new Label("Émissions (tCO2e):"), 0, 0);
        grid.add(emissionsField, 1, 0);
        grid.add(new Label("Détails:"), 0, 1);
        grid.add(detailsArea, 1, 1);
        grid.add(new Label("Commentaires:"), 0, 2);
        grid.add(commentairesArea, 1, 2);

        ScrollPane scrollPane = new ScrollPane(grid);
        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                Double emissions = Double.parseDouble(emissionsField.getText());
                String details = detailsArea.getText();
                String commentaires = commentairesArea.getText();

                reportService.validateReport(report.getId(), currentUser.getId(), emissions, details, commentaires);
                refreshAllData();
                showAlert("Succès", "Rapport validé avec succès", Alert.AlertType.INFORMATION);
            } catch (NumberFormatException ex) {
                showAlert("Erreur", "Format d'émissions invalide", Alert.AlertType.ERROR);
            }
        }
    }

    private void handleRejectReport(CarbonReport report) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Rejeter le rapport");
        dialog.setHeaderText("Motif du rejet : " + report.getProjectName());
        dialog.setContentText("Veuillez expliquer pourquoi ce rapport est rejeté:");

        if (dialog.showAndWait().isPresent()) {
            String motif = dialog.getResult();
            reportService.rejectReport(report.getId(), currentUser.getId(), motif);
            refreshAllData();
            showAlert("Succès", "Rapport rejeté", Alert.AlertType.INFORMATION);
        }
    }

    private void refreshAllData() {
        // Chargement des rapports
        List<CarbonReport> pending = reportService.getPendingReports();
        List<CarbonReport> validated = reportService.getValidatedReports();
        List<CarbonReport> rejected = reportService.getRejectedReports();

        pendingReportsTable.getItems().setAll(pending);
        validatedReportsTable.getItems().setAll(validated);
        rejectedReportsTable.getItems().setAll(rejected);

        // Mise à jour des compteurs
        pendingCountLabel.setText(String.valueOf(pending.size()));
        validatedCountLabel.setText(String.valueOf(validated.size()));
        rejectedCountLabel.setText(String.valueOf(rejected.size()));

        // Mise à jour des statistiques
        totalStatsLabel.setText(String.valueOf(reportService.getTotalCount()));
        pendingStatsLabel.setText(String.valueOf(reportService.getPendingCount()));
        validatedStatsLabel.setText(String.valueOf(reportService.getValidatedCount()));
        rejectedStatsLabel.setText(String.valueOf(reportService.getRejectedCount()));

        Double avgEmissions = reportService.getAverageEmissions();
        averageEmissionsLabel.setText(String.format("%.2f tCO2e", avgEmissions));
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        userLabel.setText(user.getNomComplet() + " (Expert Carbone)");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            SessionManager.getInstance().invalidate();
            MainFX.setRoot("fxml/login");
            System.out.println("[CLEAN] Déconnexion réussie");
        } catch (IOException e) {
            System.err.println("Erreur déconnexion: " + e.getMessage());
        }
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
