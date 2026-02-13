package Controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import Models.Projet;
import Services.ProjetService;
import org.GreenLedger.MainFX;

import java.io.IOException;

public class ExpertProjetController extends BaseController {

    @FXML
    private Button btnGestionProjets;

    @FXML
    private Button btnGestionEvaluations;

    @FXML
    private Button btnSettings;

    @FXML
    private TableView<Projet> tableProjets;

    @FXML
    private TableColumn<Projet, Integer> colId;

    @FXML
    private TableColumn<Projet, String> colTitre;

    @FXML
    private TableColumn<Projet, String> colDescription;

    @FXML
    private TableColumn<Projet, Double> colBudget;

    @FXML
    private TableColumn<Projet, String> colStatut;

    @FXML
    private TableColumn<Projet, String> colScore;

    @FXML
    private TableColumn<Projet, Void> colAction;

    @FXML
    private TableColumn<Projet, Void> colIcon;

    @FXML
    private Label lblTotal;

    @FXML
    private Label lblPending;

    @FXML
    private Label lblEvaluated;

    private final ObservableList<Projet> data = FXCollections.observableArrayList();
    private final ProjetService projetService = new ProjetService();

    @FXML
    public void initialize() {
        super.initialize();

        setActiveNav(btnGestionProjets);

        if (btnGestionProjets != null) {
            btnGestionProjets.setOnAction(event -> showGestionProjets());
        }
        if (btnGestionEvaluations != null) {
            btnGestionEvaluations.setOnAction(event -> showGestionEvaluations());
        }
        if (btnSettings != null) {
            btnSettings.setOnAction(event -> showSettings());
        }

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colBudget.setCellValueFactory(new PropertyValueFactory<>("budget"));
        colStatut.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getStatutEvaluation() == null || cellData.getValue().getStatutEvaluation().isEmpty()
                        ? "En attente"
                        : cellData.getValue().getStatutEvaluation()
        ));
        colScore.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getScoreEsg() <= 0 ? "Pending" : String.valueOf(cellData.getValue().getScoreEsg())
        ));
        colAction.setCellFactory(createActionCell());
        colIcon.setCellFactory(column -> new TableCell<>() {
            private final Label icon = new Label(">>");
            {
                icon.getStyleClass().add("row-icon");
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : icon);
            }
        });

        tableProjets.setItems(data);
        refreshTable();
    }

    private void setActiveNav(Button active) {
        if (btnGestionProjets != null) {
            btnGestionProjets.getStyleClass().remove("nav-btn-active");
        }
        if (btnGestionEvaluations != null) {
            btnGestionEvaluations.getStyleClass().remove("nav-btn-active");
        }
        if (active != null) {
            active.getStyleClass().add("nav-btn-active");
        }
    }

    @FXML
    private void showGestionProjets() {
        System.out.println("Affichage de la gestion des projets");
        try {
            MainFX.setRoot("expertProjet");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showGestionEvaluations() {
        System.out.println("Affichage de la gestion des évaluations");
        try {
            MainFX.setRoot("gestionCarbone");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showSettings() {
        try {
            MainFX.setRoot("settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Callback<TableColumn<Projet, Void>, TableCell<Projet, Void>> createActionCell() {
        return column -> new TableCell<>() {
            private final Button button = new Button("Evaluer");
            {
                button.getStyleClass().addAll("btn", "btn-primary");
                button.setOnAction(event -> {
                    Projet projet = getTableView().getItems().get(getIndex());
                    try {
                        CarbonAuditController.setSelectedProjet(projet);
                        MainFX.setRoot("gestionCarbone");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(button);
                }
            }
        };
    }

    private void refreshTable() {
        data.setAll(projetService.afficher());
        updateStats();
    }

    private void updateStats() {
        long total = data.size();
        long pending = data.stream().filter(p -> {
            String s = p.getStatutEvaluation();
            return s == null || s.isEmpty() || s.equalsIgnoreCase("En attente");
        }).count();
        long evaluated = total - pending;

        lblTotal.setText(String.valueOf(total));
        lblPending.setText(String.valueOf(pending));
        lblEvaluated.setText(String.valueOf(evaluated));
    }
}
