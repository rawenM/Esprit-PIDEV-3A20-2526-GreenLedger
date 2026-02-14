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

    @FXML private Label lblId;
    @FXML private Label lblStatut;

    @FXML private TextField tfTitre;
    @FXML private TextField tfBudget;
    @FXML private TextField tfScoreEsg;

    @FXML private TextField tfCompanyAddress;
    @FXML private TextField tfCompanyEmail;
    @FXML private TextField tfCompanyPhone;

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
    private void onAnnulerProjet() {
        if (projet == null) return;

        boolean isDraft = "DRAFT".equalsIgnoreCase(projet.getStatut());

        String msg = isDraft
                ? "Supprimer définitivement ce projet DRAFT ?"
                : "Annuler le projet (statut CANCELLED) ?";

        if (!confirm(msg)) return;

        if (isDraft) {
            service.delete(projet.getId());        // ✅ suppression réelle
        } else {
            service.cancel(projet.getId());        // ✅ CANCELLED pour les autres statuts
        }

        if (onChanged != null) onChanged.run();
        closeWindow();
    }


    @FXML
    private void onModifier() {
        if (projet == null) return;

        btnSaveChanges.setVisible(true);
        btnCancelEdit.setVisible(true);

        boolean lockedTitreBudgetScore = !"DRAFT".equalsIgnoreCase(projet.getStatut());

        // ✅ règles: après DRAFT -> titre/budget/score verrouillés
        tfTitre.setDisable(lockedTitreBudgetScore);
        tfBudget.setDisable(lockedTitreBudgetScore);
        tfScoreEsg.setDisable(lockedTitreBudgetScore);

        // ✅ toi tu veux pouvoir modifier description
        taDescription.setDisable(false);

        // ✅ et aussi infos entreprise (puisqu'elles seront auto plus tard)
        tfCompanyAddress.setDisable(false);
        tfCompanyEmail.setDisable(false);
        tfCompanyPhone.setDisable(false);
    }

    @FXML
    private void onCancelEdit() {
        btnSaveChanges.setVisible(false);
        btnCancelEdit.setVisible(false);
        render();
    }

    @FXML
    private void onSaveChanges() {
        if (projet == null) return;

        boolean isDraft = "DRAFT".equalsIgnoreCase(projet.getStatut());

        // Description + company fields toujours modifiables
        projet.setDescription(taDescription.getText());
        projet.setCompanyAddress(emptyToNull(tfCompanyAddress.getText()));
        projet.setCompanyEmail(emptyToNull(tfCompanyEmail.getText()));
        projet.setCompanyPhone(emptyToNull(tfCompanyPhone.getText()));

        if (!isDraft) {
            // ✅ non-DRAFT: update description + company fields (sans toucher titre/budget/score)
            service.updateDescriptionOnly(
                    projet.getId(),
                    projet.getDescription(),
                    projet.getCompanyAddress(),
                    projet.getCompanyEmail(),
                    projet.getCompanyPhone()
            );
            if (onChanged != null) onChanged.run();
            closeWindow();
            return;
        }

        // ✅ DRAFT: update complet
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

        tfCompanyAddress.setText(projet.getCompanyAddress());
        tfCompanyEmail.setText(projet.getCompanyEmail());
        tfCompanyPhone.setText(projet.getCompanyPhone());

        // view mode
        tfTitre.setDisable(true);
        tfBudget.setDisable(true);
        tfScoreEsg.setDisable(true);

        taDescription.setDisable(true);
        tfCompanyAddress.setDisable(true);
        tfCompanyEmail.setDisable(true);
        tfCompanyPhone.setDisable(true);

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

    private String safe(String s) { return s == null ? "" : s.trim(); }
    private String emptyToNull(String s) {
        String v = safe(s);
        return v.isEmpty() ? null : v;
    }
}
