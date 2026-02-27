package Controllers;

import Models.Financement;
import Models.OffreFinancement;
import Services.FinancementService;
import Services.OffreFinancementService;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.io.IOException;

public class FinancementController extends BaseController {

    @FXML private TableView<Financement> tableFinancement;
    @FXML private TableColumn<Financement, Integer> colFinId;
    @FXML private TableColumn<Financement, Integer> colFinProjetId;
    @FXML private TableColumn<Financement, Integer> colFinBanqueId;
    @FXML private TableColumn<Financement, Double> colFinMontant;
    @FXML private TableColumn<Financement, String> colFinDate;

    @FXML private TableView<OffreFinancement> tableOffres;
    @FXML private TableColumn<OffreFinancement, Integer> colOffreId;
    @FXML private TableColumn<OffreFinancement, String> colOffreType;
    @FXML private TableColumn<OffreFinancement, Double> colOffreTaux;
    @FXML private TableColumn<OffreFinancement, Integer> colOffreDuree;
    @FXML private TableColumn<OffreFinancement, Integer> colOffreFinancementId;

    @FXML private TextField txtProjetId;
    @FXML private TextField txtBanqueId;
    @FXML private TextField txtMontant;
    @FXML private TextField txtDateFinancement;

    @FXML private TextField txtTypeOffre;
    @FXML private TextField txtTaux;
    @FXML private TextField txtDuree;
    @FXML private TextField txtOffreFinancementId;

    @FXML private TextField txtDeleteFinancementId;
    @FXML private TextField txtFinancementIdToModify;

    private final FinancementService financementService = new FinancementService();
    private final OffreFinancementService offreService = new OffreFinancementService();
    private final javafx.collections.ObservableList<Financement> financementItems = FXCollections.observableArrayList();
    private final javafx.collections.ObservableList<OffreFinancement> offreItems = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colFinId.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getId()).asObject());
        colFinProjetId.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getProjetId()).asObject());
        colFinBanqueId.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getBanqueId()).asObject());
        colFinMontant.setCellValueFactory(cd -> new SimpleDoubleProperty(cd.getValue().getMontant()).asObject());
        colFinDate.setCellValueFactory(cd -> new SimpleStringProperty(nullSafeString(cd.getValue().getDateFinancement())));

        colOffreId.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getIdOffre()).asObject());
        colOffreType.setCellValueFactory(cd -> new SimpleStringProperty(nullSafeString(cd.getValue().getTypeOffre())));
        colOffreTaux.setCellValueFactory(cd -> new SimpleDoubleProperty(cd.getValue().getTaux()).asObject());
        colOffreDuree.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getDuree()).asObject());
        colOffreFinancementId.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getIdFinancement()).asObject());

        tableFinancement.setItems(financementItems);
        System.out.println(financementItems);
        tableOffres.setItems(offreItems);
        System.out.println(offreItems);

        loadFinancements();
        loadOffres();

        tableFinancement.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) {
                txtProjetId.setText(String.valueOf(n.getProjetId()));
                txtBanqueId.setText(String.valueOf(n.getBanqueId()));
                txtMontant.setText(String.valueOf(n.getMontant()));
                txtDateFinancement.setText(nullSafeString(n.getDateFinancement()));
            }
        });

        tableOffres.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) {
                txtTypeOffre.setText(nullSafeString(n.getTypeOffre()));
                txtTaux.setText(String.valueOf(n.getTaux()));
                txtDuree.setText(String.valueOf(n.getDuree()));
                txtOffreFinancementId.setText(String.valueOf(n.getIdFinancement()));
            }
        });
    }

    @FXML
    private void ajouterFinancement() {
        try {
            Financement f = new Financement();
            f.setProjetId(requireInt(txtProjetId.getText()));
            f.setBanqueId(requireInt(txtBanqueId.getText()));
            f.setMontant(requireDouble(txtMontant.getText()));
            f.setDateFinancement(requireText(txtDateFinancement.getText()));
            financementService.add(f);
            refreshAll();
            clearFinFields();
        } catch (NumberFormatException ex) {
            showError("Valeurs invalides", "Veuillez verifier les champs numeriques.");
        }
    }

    @FXML
    private void modifierFinancement() {
        Financement selected = tableFinancement.getSelectionModel().getSelectedItem();
        Integer targetId = selected != null ? selected.getId() : null;

        if (targetId == null) {
            try {
                targetId = requireInt(txtFinancementIdToModify.getText());
            } catch (NumberFormatException ex) {
                showError("Selection requise", "Selectionnez une ligne ou saisissez un ID a modifier.");
                return;
            }
        }

        Dialog<Financement> dialog = new Dialog<>();
        dialog.setTitle("Modifier financement");
        dialog.setHeaderText("Mettre a jour les informations du financement");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField projetField = new TextField(selected != null ? String.valueOf(selected.getProjetId()) : txtProjetId.getText());
        TextField banqueField = new TextField(selected != null ? String.valueOf(selected.getBanqueId()) : txtBanqueId.getText());
        TextField montantField = new TextField(selected != null ? String.valueOf(selected.getMontant()) : txtMontant.getText());
        TextField dateField = new TextField(selected != null ? nullSafeString(selected.getDateFinancement()) : txtDateFinancement.getText());

        grid.addRow(0, new Label("Projet ID"), projetField);
        grid.addRow(1, new Label("Banque ID"), banqueField);
        grid.addRow(2, new Label("Montant"), montantField);
        grid.addRow(3, new Label("Date (YYYY-MM-DD)"), dateField);

        dialog.getDialogPane().setContent(grid);

        Integer finalTargetId = targetId;
        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                Financement updated = new Financement();
                updated.setId(finalTargetId);
                updated.setProjetId(requireInt(projetField.getText()));
                updated.setBanqueId(requireInt(banqueField.getText()));
                updated.setMontant(requireDouble(montantField.getText()));
                updated.setDateFinancement(requireText(dateField.getText()));
                return updated;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updated -> {
            financementService.update(updated);
            refreshAll();
        });
    }

    @FXML
    private void supprimerFinancement() {
        Financement selected = tableFinancement.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Selection requise", "Choisissez un financement a supprimer.");
            return;
        }
        financementService.delete(selected.getId());
        loadFinancements();
        clearFinFields();
    }

    @FXML
    private void ajouterOffre() {
        try {
            OffreFinancement o = new OffreFinancement();
            o.setTypeOffre(requireText(txtTypeOffre.getText()));
            o.setTaux(requireDouble(txtTaux.getText()));
            o.setDuree(requireInt(txtDuree.getText()));
            o.setIdFinancement(requireInt(txtOffreFinancementId.getText()));
            offreService.add(o);
            refreshAll();
            clearOffreFields();
        } catch (NumberFormatException ex) {
            showError("Valeurs invalides", "Veuillez verifier les champs numeriques.");
        }
    }

    @FXML
    private void modifierOffre() {
        OffreFinancement selected = tableOffres.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Selection requise", "Choisissez une offre a modifier.");
            return;
        }
        try {
            selected.setTypeOffre(requireText(txtTypeOffre.getText()));
            selected.setTaux(requireDouble(txtTaux.getText()));
            selected.setDuree(requireInt(txtDuree.getText()));
            selected.setIdFinancement(requireInt(txtOffreFinancementId.getText()));
            offreService.update(selected);
            loadOffres();
        } catch (NumberFormatException ex) {
            showError("Valeurs invalides", "Veuillez verifier les champs numeriques.");
        }
    }

    @FXML
    private void supprimerOffre() {
        OffreFinancement selected = tableOffres.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Selection requise", "Choisissez une offre a supprimer.");
            return;
        }
        offreService.delete(selected.getIdOffre());
        loadOffres();
        clearOffreFields();
    }

    @FXML
    private void supprimerFinancementParId() {
        try {
            int id = requireInt(txtDeleteFinancementId.getText());
            financementService.delete(id);
            refreshAll();
            txtDeleteFinancementId.clear();
        } catch (NumberFormatException ex) {
            showError("Valeurs invalides", "Veuillez saisir un ID valide.");
        }
    }

    @FXML
    private void refreshAll() {
        loadFinancements();
        loadOffres();
    }

    private void loadFinancements() {
        financementItems.setAll(financementService.getAll());
    }

    private void loadOffres() {
        offreItems.setAll(offreService.getAll());
    }

    private void clearFinFields() {
        txtProjetId.clear();
        txtBanqueId.clear();
        txtMontant.clear();
        txtDateFinancement.clear();
    }

    private void clearOffreFields() {
        txtTypeOffre.clear();
        txtTaux.clear();
        txtDuree.clear();
        txtOffreFinancementId.clear();
    }

    private String nullSafeString(String value) {
        return value == null ? "" : value;
    }

    private int requireInt(String value) {
        if (isBlank(value)) {
            throw new NumberFormatException("empty");
        }
        return Integer.parseInt(value.trim());
    }

    private double requireDouble(String value) {
        if (isBlank(value)) {
            throw new NumberFormatException("empty");
        }
        return Double.parseDouble(value.trim());
    }

    private String requireText(String value) {
        if (isBlank(value)) {
            throw new NumberFormatException("empty");
        }
        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Navigate to dashboard
     */
    @FXML
    private void handleGoDashboard() {
        try {
            org.GreenLedger.MainFX.setRoot("fxml/dashboard");
        } catch (IOException ex) {
            System.err.println("[ERROR] Navigation error: " + ex.getMessage());
            ex.printStackTrace();
            showError("Erreur", "Impossible de naviguer au tableau de bord");
        }
    }

    /**
     * Stay on financement (no-op or refresh)
     */
    @FXML
    private void handleGoFinancement() {
        try {
            refreshAll();
        } catch (Exception ex) {
            System.err.println("[ERROR] Refresh error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Navigate to investor financing
     */
    @FXML
    private void handleGoInvestments() {
        try {
            org.GreenLedger.MainFX.setRoot("fxml/investor_financing");
        } catch (IOException ex) {
            System.err.println("[ERROR] Navigation error: " + ex.getMessage());
            ex.printStackTrace();
            showError("Erreur", "Impossible de naviguer aux investissements");
        }
    }

    /**
     * Navigate to settings
     */
    @FXML
    private void handleGoSettings() {
        try {
            // Navigate to settings - using dashboard as fallback
            org.GreenLedger.MainFX.setRoot("fxml/dashboard");
        } catch (IOException ex) {
            System.err.println("[ERROR] Navigation error: " + ex.getMessage());
            ex.printStackTrace();
            showError("Erreur", "Impossible de naviguer aux paramètres");
        }
    }

    /**
     * Handle sidebar button: New Financing
     */
    @FXML
    private void btnNewFinancement_click() {
        clearFinFields();
        txtProjetId.requestFocus();
    }

    /**
     * Handle sidebar button: New Offer
     */
    @FXML
    private void btnNewOffre_click() {
        clearOffreFields();
        txtTypeOffre.requestFocus();
    }

    /**
     * Navigate to risk agent
     */
    @FXML
    private void handleGoRiskAgent() {
        try {
            org.GreenLedger.MainFX.setRoot("fxml/finance_risk_agent");
        } catch (IOException ex) {
            System.err.println("[ERROR] Navigation error: " + ex.getMessage());
            ex.printStackTrace();
            showError("Erreur", "Impossible d'ouvrir l'agent de risque");
        }
    }
}
