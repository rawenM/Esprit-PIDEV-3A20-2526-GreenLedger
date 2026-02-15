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

    @FXML private Button btnRefresh;
    @FXML private Button btnAddFinancement;
    @FXML private Button btnEditFinancement;
    @FXML private Button btnDeleteFinancement;

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

    @FXML private void initialize() {
        colFinId.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getId()).asObject());
        colFinProjetId.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getProjetId()).asObject());
        colFinBanqueId.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getBanqueId()).asObject());
        colFinMontant.setCellValueFactory(cd -> new SimpleDoubleProperty(cd.getValue().getMontant()).asObject());
        colFinDate.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().getDateFinancement())));

        colOffreId.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getIdOffre()).asObject());
        colOffreType.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getTypeOffre()));
        colOffreTaux.setCellValueFactory(cd -> new SimpleDoubleProperty(cd.getValue().getTaux()).asObject());
        colOffreDuree.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getDuree()).asObject());
        colOffreFinancementId.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getIdFinancement()).asObject());

        loadFinancements();
        loadOffres();

        tableFinancement.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) {
                txtProjetId.setText(String.valueOf(n.getProjetId()));
                txtBanqueId.setText(String.valueOf(n.getBanqueId()));
                txtMontant.setText(String.valueOf(n.getMontant()));
                txtDateFinancement.setText(String.valueOf(n.getDateFinancement()));
            }
        });

        tableOffres.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) {
                txtTypeOffre.setText(n.getTypeOffre());
                txtTaux.setText(String.valueOf(n.getTaux()));
                txtDuree.setText(String.valueOf(n.getDuree()));
                txtOffreFinancementId.setText(String.valueOf(n.getIdFinancement()));
            }
        });
    }

    @FXML private void ajouterFinancement() {
        Financement f = new Financement();
        f.setProjetId(Integer.parseInt(txtProjetId.getText()));
        f.setBanqueId(Integer.parseInt(txtBanqueId.getText()));
        f.setMontant(Double.parseDouble(txtMontant.getText()));
        f.setDateFinancement(txtDateFinancement.getText());
        financementService.add(f);
        loadFinancements();
        clearFinFields();
    }

    @FXML private void modifierFinancement() {
        Financement selected = tableFinancement.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        selected.setProjetId(Integer.parseInt(txtProjetId.getText()));
        selected.setBanqueId(Integer.parseInt(txtBanqueId.getText()));
        selected.setMontant(Double.parseDouble(txtMontant.getText()));
        selected.setDateFinancement(txtDateFinancement.getText());
        financementService.update(selected);
        loadFinancements();
    }

    @FXML private void supprimerFinancement() {
        Financement selected = tableFinancement.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        financementService.delete(selected.getId());
        loadFinancements();
        clearFinFields();
    }

    @FXML private void refreshAll() {
        loadFinancements();
        loadOffres();
    }

    @FXML private void ajouterOffre() {
        OffreFinancement o = new OffreFinancement();
        o.setTypeOffre(txtTypeOffre.getText());
        o.setTaux(Double.parseDouble(txtTaux.getText()));
        o.setDuree(Integer.parseInt(txtDuree.getText()));
        o.setIdFinancement(Integer.parseInt(txtOffreFinancementId.getText()));
        offreService.add(o);
        loadOffres();
        clearOffreFields();
    }

    @FXML private void modifierOffre() {
        OffreFinancement selected = tableOffres.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        selected.setTypeOffre(txtTypeOffre.getText());
        selected.setTaux(Double.parseDouble(txtTaux.getText()));
        selected.setDuree(Integer.parseInt(txtDuree.getText()));
        selected.setIdFinancement(Integer.parseInt(txtOffreFinancementId.getText()));
        offreService.update(selected);
        loadOffres();
    }

    @FXML private void supprimerOffre() {
        OffreFinancement selected = tableOffres.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        offreService.delete(selected.getIdOffre());
        loadOffres();
        clearOffreFields();
    }

    private void loadFinancements() {
        tableFinancement.setItems(FXCollections.observableArrayList(financementService.getAll()));
    }

    private void loadOffres() {
        tableOffres.setItems(FXCollections.observableArrayList(offreService.getAll()));
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
}
