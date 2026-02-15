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

public class FinancementController {

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

    private final FinancementService financementService = new FinancementService();
    private final OffreFinancementService offreService = new OffreFinancementService();
    private final javafx.collections.ObservableList<Financement> financementItems = FXCollections.observableArrayList();
    private final javafx.collections.ObservableList<OffreFinancement> offreItems = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
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
            loadFinancements();
            clearFinFields();
        } catch (NumberFormatException ex) {
            showError("Valeurs invalides", "Veuillez verifier les champs numeriques.");
        }
    }

    @FXML
    private void modifierFinancement() {
        Financement selected = tableFinancement.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Selection requise", "Choisissez un financement a modifier.");
            return;
        }
        try {
            selected.setProjetId(requireInt(txtProjetId.getText()));
            selected.setBanqueId(requireInt(txtBanqueId.getText()));
            selected.setMontant(requireDouble(txtMontant.getText()));
            selected.setDateFinancement(requireText(txtDateFinancement.getText()));
            financementService.update(selected);
            loadFinancements();
        } catch (NumberFormatException ex) {
            showError("Valeurs invalides", "Veuillez verifier les champs numeriques.");
        }
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
            loadOffres();
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
}
