package Controllers;

import Models.Projet;
import Services.ProjetService;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.GreenLedger.MainFX;

public class ProjetController {

    private final ProjetService service = new ProjetService();
    private final ObservableList<Projet> data = FXCollections.observableArrayList();

    @FXML private TableView<Projet> table;
    @FXML private TableColumn<Projet, Number> colId;
    @FXML private TableColumn<Projet, String> colTitre;
    @FXML private TableColumn<Projet, String> colStatut;
    @FXML private TableColumn<Projet, Number> colBudget;

    @FXML private Label lblTotal;
    @FXML private Label lblDraft;
    @FXML private Label lblLocked;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(v -> new SimpleIntegerProperty(v.getValue().getId()));
        colTitre.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().getTitre()));
        colStatut.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().getStatut()));
        colBudget.setCellValueFactory(v -> new SimpleDoubleProperty(v.getValue().getBudget()));

        table.setItems(data);

        // ✅ Double clic -> ouvrir fenêtre détail
        table.setRowFactory(tv -> {
            TableRow<Projet> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    openDetailWindow(row.getItem());
                }
            });
            return row;
        });

        refresh();
    }

    @FXML
    private void onNew() {
        try {
            MainFX.setRoot("ProjetCreate");
        } catch (Exception ex) {
            showError("Navigation impossible: " + ex.getMessage());
        }
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    private void refresh() {
        data.setAll(service.afficher());
        updateStats();
    }

    private void updateStats() {
        int total = data.size();
        long drafts = data.stream().filter(p -> "DRAFT".equalsIgnoreCase(p.getStatut())).count();
        long locked = total - drafts;

        lblTotal.setText(String.valueOf(total));
        lblDraft.setText(String.valueOf(drafts));
        lblLocked.setText(String.valueOf(locked));
    }

    private void openDetailWindow(Projet projet) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ProjetDetail.fxml"));
            Parent root = loader.load();

            ProjetDetailController ctrl = loader.getController();
            ctrl.setProjet(projet);
            ctrl.setOnChanged(this::refresh); // callback refresh après modif/annulation

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Détails Projet #" + projet.getId());
            stage.setScene(new javafx.scene.Scene(root, 520, 520));
            stage.showAndWait();

        } catch (Exception ex) {
            showError("Impossible d'ouvrir détail: " + ex.getMessage());
        }
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
