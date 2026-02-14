package Controllers;

import Models.Evaluation;
import Services.EvaluationService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.GreenLedger.MainFX;

import java.io.IOException;
import java.sql.Timestamp;

public class OwnerEvaluationsController {

    private static Integer currentEntrepriseId;

    public static void setCurrentEntrepriseId(Integer entrepriseId) {
        currentEntrepriseId = entrepriseId;
    }

    @FXML private Label lblEntreprise;
    @FXML private Label lblTotal;
    @FXML private TableView<Evaluation> tableEvaluations;

    @FXML private TableColumn<Evaluation, Timestamp> colDate;
    @FXML private TableColumn<Evaluation, String> colDecision;
    @FXML private TableColumn<Evaluation, String> colProjet;
    @FXML private TableColumn<Evaluation, Number> colScore;
    @FXML private TableColumn<Evaluation, String> colObservations;

    private final EvaluationService evaluationService = new EvaluationService();

    @FXML
    public void initialize() {
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateEvaluation"));
        colDecision.setCellValueFactory(new PropertyValueFactory<>("decision"));
        colProjet.setCellValueFactory(new PropertyValueFactory<>("titreProjet"));
        colScore.setCellValueFactory(new PropertyValueFactory<>("scoreGlobal"));
        colObservations.setCellValueFactory(new PropertyValueFactory<>("observations"));

        refresh();
    }

    @FXML
    private void onBack() {
        try {
            MainFX.setRoot("GestionProjet");
        } catch (IOException e) {
            showError("Navigation impossible: " + e.getMessage());
        }
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    private void refresh() {
        if (currentEntrepriseId == null) {
            lblEntreprise.setText("Entreprise: non definie");
            tableEvaluations.setItems(FXCollections.observableArrayList());
            lblTotal.setText("0");
            return;
        }

        lblEntreprise.setText("Entreprise ID: " + currentEntrepriseId);
        ObservableList<Evaluation> items = FXCollections.observableArrayList(
                evaluationService.afficherParEntreprise(currentEntrepriseId)
        );
        tableEvaluations.setItems(items);
        lblTotal.setText(String.valueOf(items.size()));
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Navigation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

