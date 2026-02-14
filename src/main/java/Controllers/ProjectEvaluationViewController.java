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

public class ProjectEvaluationViewController {

    private static Integer currentProjetId;
    private static String currentProjetTitre;

    public static void setCurrentProjet(Integer projetId, String projetTitre) {
        currentProjetId = projetId;
        currentProjetTitre = projetTitre;
    }

    @FXML private Label lblProjet;
    @FXML private Label lblTotal;
    @FXML private TableView<Evaluation> tableEvaluations;
    @FXML private TableView<Models.CritereImpact> tableCriteres;

    @FXML private TableColumn<Evaluation, Timestamp> colDate;
    @FXML private TableColumn<Evaluation, String> colDecision;
    @FXML private TableColumn<Evaluation, Number> colScore;
    @FXML private TableColumn<Evaluation, String> colObservations;

    @FXML private TableColumn<Models.CritereImpact, String> colCritereNom;
    @FXML private TableColumn<Models.CritereImpact, Number> colCritereNote;
    @FXML private TableColumn<Models.CritereImpact, String> colCritereCommentaire;

    private final EvaluationService evaluationService = new EvaluationService();
    private final Services.CritereImpactService critereImpactService = new Services.CritereImpactService();

    @FXML
    public void initialize() {
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateEvaluation"));
        colDecision.setCellValueFactory(new PropertyValueFactory<>("decision"));
        colScore.setCellValueFactory(new PropertyValueFactory<>("scoreGlobal"));
        colObservations.setCellValueFactory(new PropertyValueFactory<>("observations"));

        colCritereNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colCritereNote.setCellValueFactory(new PropertyValueFactory<>("note"));
        colCritereCommentaire.setCellValueFactory(new PropertyValueFactory<>("commentaireTechnique"));

        if (tableEvaluations != null) {
            tableEvaluations.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
                loadCriteria(selected);
            });
        }

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
        if (currentProjetId == null) {
            lblProjet.setText("Projet: non defini");
            tableEvaluations.setItems(FXCollections.observableArrayList());
            lblTotal.setText("0");
            if (tableCriteres != null) {
                tableCriteres.setItems(FXCollections.observableArrayList());
            }
            return;
        }

        String title = currentProjetTitre == null ? "" : " - " + currentProjetTitre;
        lblProjet.setText("Projet ID: " + currentProjetId + title);
        ObservableList<Evaluation> items = FXCollections.observableArrayList(
                evaluationService.afficherParProjet(currentProjetId)
        );
        tableEvaluations.setItems(items);
        lblTotal.setText(String.valueOf(items.size()));
        if (!items.isEmpty()) {
            tableEvaluations.getSelectionModel().selectFirst();
        } else {
            loadCriteria(null);
        }
    }

    private void loadCriteria(Evaluation evaluation) {
        if (tableCriteres == null) {
            return;
        }
        if (evaluation == null) {
            tableCriteres.setItems(FXCollections.observableArrayList());
            return;
        }
        ObservableList<Models.CritereImpact> items = FXCollections.observableArrayList(
                critereImpactService.afficherParEvaluation(evaluation.getIdEvaluation())
        );
        tableCriteres.setItems(items);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Navigation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
