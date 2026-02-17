package Controllers;

import Models.Projet;
import Models.Wallet;
import Models.User;
import Services.ProjetService;
import Services.WalletService;
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
import Utils.SessionManager;

import java.util.List;

public class ProjetController {

    private final ProjetService service = new ProjetService();
    private final WalletService walletService = new WalletService();
    private final Services.EvaluationService evaluationService = new Services.EvaluationService();
    private final ObservableList<Projet> data = FXCollections.observableArrayList();
    private java.util.Set<Integer> evaluatedProjectIds = java.util.Collections.emptySet();

    @FXML private TableView<Projet> table;
    @FXML private TableColumn<Projet, Number> colId;
    @FXML private TableColumn<Projet, String> colTitre;
    @FXML private TableColumn<Projet, String> colStatut;
    @FXML private TableColumn<Projet, Number> colBudget;
    @FXML private TableColumn<Projet, Void> colEvaluation;

    @FXML private Label lblTotal;
    @FXML private Label lblDraft;
    @FXML private Label lblLocked;
    @FXML private Label lblWalletAvailable;

    @FXML private Button btnSettings;
    @FXML private Button btnAuditCarbone;
    @FXML private Button btnGestionProjets;
    @FXML private Button btnGreenWallet;

    @FXML private Label lblProfileName;
    @FXML private Label lblProfileType;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(v -> new SimpleIntegerProperty(v.getValue().getId()));
        colTitre.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().getTitre()));
        colStatut.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().getStatut()));
        colBudget.setCellValueFactory(v -> new SimpleDoubleProperty(v.getValue().getBudget()));
        if (colEvaluation != null) {
            colEvaluation.setCellFactory(column -> new TableCell<>() {
                private final Button button = new Button("Voir evaluation");
                {
                    button.getStyleClass().addAll("btn", "btn-secondary");
                    button.setOnAction(event -> {
                        Projet projet = getTableView().getItems().get(getIndex());
                        ProjectEvaluationViewController.setCurrentProjet(projet.getId(), projet.getTitre());
                        try {
                            MainFX.setRoot("projectEvaluationView");
                        } catch (Exception ex) {
                            showError("Navigation impossible: " + ex.getMessage());
                        }
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                        return;
                    }
                    Projet projet = getTableView().getItems().get(getIndex());
                    if (projet == null || !evaluatedProjectIds.contains(projet.getId())) {
                        setGraphic(null);
                    } else {
                        setGraphic(button);
                    }
                }
            });
        }

        table.setItems(data);

        table.setRowFactory(tv -> {
            TableRow<Projet> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    openDetailWindow(row.getItem());
                }
            });
            return row;
        });

        if (btnSettings != null) {
            btnSettings.setOnAction(e -> showSettings());
        }
        if (btnGreenWallet != null) {
            btnGreenWallet.setOnAction(e -> onGreenWallet());
        }

        applyProfile();

        refresh();
    }

    private void applyProfile() {
        Models.User user = Utils.SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        if (lblProfileName != null) {
            lblProfileName.setText(user.getNomComplet());
        }
        if (lblProfileType != null) {
            lblProfileType.setText(user.getTypeUtilisateur().getLibelle());
        }
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

    @FXML
    private void onGestionProjets() {
        refresh();
    }

    @FXML
    private void onAuditCarbone() {
        try {
            MainFX.setRoot("gestionCarbone");
        } catch (Exception ex) {
            showError("Navigation impossible: " + ex.getMessage());
        }
    }

    @FXML
    private void onSettings() {
        showSettings();
    }

    @FXML
    private void onGreenWallet() {
        try {
            MainFX.setRoot("greenwallet");
        } catch (Exception ex) {
            showError("Navigation impossible: " + ex.getMessage());
        }
    }

    @FXML
    private void onBack() {
        try {
            MainFX.setRoot("fxml/dashboard");
        } catch (Exception ex) {
            showError("Navigation impossible: " + ex.getMessage());
        }
    }

    private void refresh() {
        User user = SessionManager.getInstance().getCurrentUser();
        Integer entrepriseId = (user != null && user.getId() != null) ? Math.toIntExact(user.getId()) : null;
        List<Projet> projets = (entrepriseId != null)
                ? service.getByEntreprise(entrepriseId)
                : service.afficher();
        if ((projets == null || projets.isEmpty()) && entrepriseId != null) {
            projets = service.afficher();
        }
        if (projets == null) {
            projets = java.util.Collections.emptyList();
        }
        data.setAll(projets);
        evaluatedProjectIds = getEvaluatedProjectIds(projets);
        updateStats();
        refreshWalletBalance();
    }

    private java.util.Set<Integer> getEvaluatedProjectIds(List<Projet> projets) {
        java.util.Set<Integer> evaluatedIds = evaluationService.getProjetIdsWithEvaluations();
        if (evaluatedIds == null || evaluatedIds.isEmpty()) {
            return java.util.Collections.emptySet();
        }
        if (projets == null || projets.isEmpty()) {
            return evaluatedIds;
        }
        java.util.Set<Integer> visibleIds = new java.util.HashSet<>();
        for (Projet projet : projets) {
            visibleIds.add(projet.getId());
        }
        evaluatedIds.retainAll(visibleIds);
        return evaluatedIds;
    }

    private void updateStats() {
        int total = data.size();
        long drafts = data.stream().filter(p -> "DRAFT".equalsIgnoreCase(p.getStatut())).count();
        long submitted = data.stream().filter(p -> "SUBMITTED".equalsIgnoreCase(p.getStatut())).count();
        long evaluated = data.stream().filter(p -> evaluatedProjectIds.contains(p.getId())).count();

        lblTotal.setText(String.valueOf(total));
        lblDraft.setText(String.valueOf(drafts));
        lblLocked.setText(String.valueOf(submitted));
    }

    private void refreshWalletBalance() {
        if (lblWalletAvailable == null) {
            return;
        }

        User user = Utils.SessionManager.getInstance().getCurrentUser();
        if (user == null || user.getId() == null) {
            lblWalletAvailable.setText("0.00 tCO₂");
            return;
        }

        try {
            int ownerId = user.getId().intValue();
            List<Wallet> wallets = walletService.getAllWallets();
            double totalAvailableCredits = wallets.stream()
                    .filter(wallet -> wallet.getOwnerId() == ownerId)
                    .mapToDouble(Wallet::getAvailableCredits)
                    .sum();
            lblWalletAvailable.setText(String.format("%.2f tCO₂", totalAvailableCredits));
        } catch (Exception ex) {
            lblWalletAvailable.setText("0.00 tCO₂");
        }
    }

    private void openDetailWindow(Projet projet) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ProjetDetail.fxml"));
            Parent root = loader.load();

            ProjetDetailController ctrl = loader.getController();
            ctrl.setProjet(projet);
            ctrl.setOnChanged(this::refresh);

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

    private void showSettings() {
        try {
            MainFX.setRoot("settings");
        } catch (Exception ex) {
            showError("Navigation impossible: " + ex.getMessage());
        }
    }

    @FXML
    private void handleEditProfile() {
        try {
            MainFX.setRoot("editProfile");
        } catch (Exception e) {
            showError("Navigation impossible: " + e.getMessage());
        }
    }
}
