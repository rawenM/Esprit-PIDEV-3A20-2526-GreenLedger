package Controllers;

import Models.Projet;
import Services.ProjetService;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class ProjetController {

    // ✅ si tu n'as pas encore l'auth, on fixe une entreprise de test
    private static final int TEST_ENTREPRISE_ID = 1;

    private final ProjetService service = new ProjetService();

    private final ObservableList<Projet> data = FXCollections.observableArrayList();
    private Projet selected;

    // --- Table ---
    @FXML private TableView<Projet> table;
    @FXML private TableColumn<Projet, Number> colId;
    @FXML private TableColumn<Projet, String> colTitre;
    @FXML private TableColumn<Projet, String> colStatut;
    @FXML private TableColumn<Projet, Number> colBudget;

    // --- Form ---
    @FXML private TextField tfTitre;
    @FXML private TextArea taDescription;
    @FXML private TextField tfBudget;
    @FXML private ComboBox<String> cbStatut;
    @FXML private TextField tfScoreEsg;

    // --- Stats cards ---
    @FXML private Label lblTotal;
    @FXML private Label lblDraft;
    @FXML private Label lblLocked; // Soumis / autres

    @FXML
    public void initialize() {

        // Columns
        colId.setCellValueFactory(v -> new SimpleIntegerProperty(v.getValue().getId()));
        colTitre.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().getTitre()));
        colStatut.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().getStatut()));
        colBudget.setCellValueFactory(v -> new SimpleDoubleProperty(v.getValue().getBudget()));

        // Status list
        cbStatut.setItems(FXCollections.observableArrayList(
                "DRAFT", "SUBMITTED", "IN_PROGRESS", "COMPLETED", "CANCELLED"
        ));
        cbStatut.setValue("DRAFT");

        // Table binding
        table.setItems(data);

        // Selection -> fill form
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, p) -> {
            selected = p;
            if (p != null) {
                fillForm(p);
            } else {
                clearForm();
            }
        });

        // init
        refresh();
        clearForm();
    }

    // -----------------------
    // Actions UI
    // -----------------------

    @FXML
    private void onNew() {
        selected = null;
        table.getSelectionModel().clearSelection();
        clearForm();
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    @FXML
    private void onSave() {
        // ✅ Cas 1: projet existant NON-DRAFT -> on autorise uniquement la description
        if (selected != null && !"DRAFT".equalsIgnoreCase(selected.getStatut())) {
            String newDesc = taDescription.getText() == null ? "" : taDescription.getText().trim();

            if (newDesc.length() > 500) {
                error("Description trop longue (max 500 caractères).");
                return;
            }

            service.updateDescriptionOnly(selected.getId(), newDesc);
            refreshAndReselect(selected.getId());
            return;
        }

        // ✅ Cas 2: Nouveau projet OU projet DRAFT -> CRUD complet
        String titre = tfTitre.getText() == null ? "" : tfTitre.getText().trim();
        if (titre.length() < 3) { error("Titre: minimum 3 caractères."); return; }

        double budget;
        try {
            budget = Double.parseDouble(tfBudget.getText().trim());
            if (budget <= 0) throw new NumberFormatException();
        } catch (Exception e) {
            error("Budget invalide (doit être > 0).");
            return;
        }

        int score;
        try {
            score = Integer.parseInt(tfScoreEsg.getText().trim());
            if (score < 0 || score > 100) throw new NumberFormatException();
        } catch (Exception e) {
            error("Score ESG invalide (0..100).");
            return;
        }

        String statut = cbStatut.getValue() == null ? "DRAFT" : cbStatut.getValue();

        // ✅ règle: si le projet est DRAFT, il peut rester DRAFT (et seulement là)
        if (!"DRAFT".equalsIgnoreCase(statut)) {
            // si tu veux forcer que création = DRAFT uniquement:
            // statut = "DRAFT";
        }

        String desc = taDescription.getText();

        if (selected == null) {
            Projet p = new Projet();
            p.setEntrepriseId(TEST_ENTREPRISE_ID);
            p.setTitre(titre);
            p.setDescription(desc);
            p.setBudget(budget);
            p.setScoreEsg(score);
            p.setStatut(statut); // normalement DRAFT

            service.insert(p);
            refresh();
            onNew();
        } else {
            // selected est DRAFT ici
            selected.setTitre(titre);
            selected.setDescription(desc);
            selected.setBudget(budget);
            selected.setScoreEsg(score);

            // ✅ empêcher de revenir à DRAFT si jamais il avait changé (normalement impossible ici)
            if (!"DRAFT".equalsIgnoreCase(selected.getStatut()) && "DRAFT".equalsIgnoreCase(statut)) {
                error("Impossible de revenir à DRAFT après changement de statut.");
                cbStatut.setValue(selected.getStatut());
                return;
            }

            selected.setStatut(statut);
            service.update(selected);

            refreshAndReselect(selected.getId());
        }
    }

    // Option B: Delete = CANCELLED
    @FXML
    private void onDelete() {
        Projet p = table.getSelectionModel().getSelectedItem();
        if (p == null) { error("Sélectionne un projet."); return; }

        if (!confirm("Annuler le projet #" + p.getId() + " (statut CANCELLED) ?")) return;

        service.cancel(p.getId());
        refresh();
        onNew();
    }

    // -----------------------
    // Helpers
    // -----------------------

    private void refresh() {
        data.setAll(service.afficher()); // ou service.getByEntreprise(TEST_ENTREPRISE_ID);
        updateStats();
    }

    private void refreshAndReselect(int id) {
        refresh();
        // reselection simple
        for (Projet p : data) {
            if (p.getId() == id) {
                table.getSelectionModel().select(p);
                table.scrollTo(p);
                break;
            }
        }
    }

    private void updateStats() {
        int total = data.size();
        long drafts = data.stream()
                .filter(p -> "DRAFT".equalsIgnoreCase(p.getStatut()))
                .count();
        long locked = total - drafts; // soumis + autres

        if (lblTotal != null) lblTotal.setText(String.valueOf(total));
        if (lblDraft != null) lblDraft.setText(String.valueOf(drafts));
        if (lblLocked != null) lblLocked.setText(String.valueOf(locked));
    }

    private void fillForm(Projet p) {
        tfTitre.setText(p.getTitre());
        taDescription.setText(p.getDescription());
        tfBudget.setText(String.valueOf(p.getBudget()));
        tfScoreEsg.setText(String.valueOf(p.getScoreEsg()));
        cbStatut.setValue(p.getStatut());

        boolean locked = !"DRAFT".equalsIgnoreCase(p.getStatut());

        // 🔒 verrouiller les champs sensibles si non-DRAFT
        tfTitre.setDisable(locked);
        tfBudget.setDisable(locked);
        tfScoreEsg.setDisable(locked);
        cbStatut.setDisable(locked);

        // ✅ description toujours modifiable
        taDescription.setDisable(false);
    }

    private void clearForm() {
        tfTitre.clear();
        taDescription.clear();
        tfBudget.setText("0");
        tfScoreEsg.setText("0");
        cbStatut.setValue("DRAFT");

        // ✅ réactiver pour nouveau projet
        tfTitre.setDisable(false);
        tfBudget.setDisable(false);
        tfScoreEsg.setDisable(false);
        cbStatut.setDisable(false);
        taDescription.setDisable(false);
    }

    private void error(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private boolean confirm(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.OK, ButtonType.CANCEL);
        a.setHeaderText(null);
        return a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }
}
