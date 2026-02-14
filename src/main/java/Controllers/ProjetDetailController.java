package Controllers;

import Models.Projet;
import Services.ProjetService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class ProjetDetailController {

    private final ProjetService service = new ProjetService();

    private Projet projet;
    private Runnable onChanged = null;

    private boolean editMode = false;

    @FXML private Label lblId;
    @FXML private Label lblStatut;

    @FXML private TextField tfTitre;
    @FXML private TextField tfBudget;
    @FXML private TextField tfScoreEsg;
    @FXML private TextArea taDescription;

    @FXML private Button btnSaveChanges;
    @FXML private Button btnCancelEdit;

    public void setProjet(Projet p) {
        this.projet = p;
        render();
    }

    public void setOnChanged(Runnable r) {
        this.onChanged = r;
    }

    @FXML
    private void onBack() {
        closeWindow();
    }

    @FXML
    private void onAnnulerProjet() {
        if (projet == null) return;
        if (!confirm("Annuler le projet (CANCELLED) ?")) return;

        service.cancel(projet.getId());
        if (onChanged != null) onChanged.run();
        closeWindow();
    }

    @FXML
    private void onModifier() {
        if (projet == null) return;

        editMode = true;
        btnSaveChanges.setVisible(true);
        btnCancelEdit.setVisible(true);

        boolean locked = !"DRAFT".equalsIgnoreCase(projet.getStatut());

        // ✅ Après DRAFT : seuls titre/budget/score restent verrouillés; description editable
        tfTitre.setDisable(locked);
        tfBudget.setDisable(locked);
        tfScoreEsg.setDisable(locked);

        taDescription.setDisable(false);
    }

    @FXML
    private void onCancelEdit() {
        editMode = false;
        btnSaveChanges.setVisible(false);
        btnCancelEdit.setVisible(false);
        render();
    }

    @FXML
    private void onSaveChanges() {
        if (projet == null) return;

        // Cas non-DRAFT => description only
        if (!"DRAFT".equalsIgnoreCase(projet.getStatut())) {
            service.updateDescriptionOnly(projet.getId(), taDescription.getText());
            if (onChanged != null) onChanged.run();
            closeWindow();
            return;
        }

        // Cas DRAFT => update complet
        String titre = safe(tfTitre.getText());
        if (titre.length() < 3) { error("Titre: min 3 caractères."); return; }

        double budget;
        try {
            budget = Double.parseDouble(safe(tfBudget.getText()));
            if (budget <= 0) throw new Exception();
        } catch (Exception e) { error("Budget invalide (>0)."); return; }

        int score;
        try {
            score = Integer.parseInt(safe(tfScoreEsg.getText()));
            if (score < 0 || score > 100) throw new Exception();
        } catch (Exception e) { error("Score ESG invalide (0..100)."); return; }

        projet.setTitre(titre);
        projet.setBudget(budget);
        projet.setScoreEsg(score);
        projet.setDescription(taDescription.getText());

        service.update(projet);
        if (onChanged != null) onChanged.run();
        closeWindow();
    }

    private void render() {
        if (projet == null) return;

        lblId.setText(String.valueOf(projet.getId()));
        lblStatut.setText(projet.getStatut());

        tfTitre.setText(projet.getTitre());
        tfBudget.setText(String.valueOf(projet.getBudget()));
        tfScoreEsg.setText(String.valueOf(projet.getScoreEsg()));
        taDescription.setText(projet.getDescription());

        // default view mode
        tfTitre.setDisable(true);
        tfBudget.setDisable(true);
        tfScoreEsg.setDisable(true);
        taDescription.setDisable(true);

        btnSaveChanges.setVisible(false);
        btnCancelEdit.setVisible(false);
    }

    private void closeWindow() {
        Stage stage = (Stage) lblId.getScene().getWindow();
        stage.close();
    }

    private boolean confirm(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.OK, ButtonType.CANCEL);
        a.setHeaderText(null);
        return a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void error(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
