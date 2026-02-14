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
                ? "Supprimer définitivement le DRAFT ?"
                : "Annuler le projet (statut CANCELLED) ?";

        if (!confirm(msg)) return;

        if (isDraft) service.delete(projet.getId());
        else service.cancel(projet.getId());

        if (onChanged != null) onChanged.run();
        closeWindow();
    }

    @FXML
    private void onModifier() {
        if (projet == null) return;

        btnSaveChanges.setVisible(true);
        btnCancelEdit.setVisible(true);


        tfScoreEsg.setDisable(true);

        boolean lockedTitreBudget = !"DRAFT".equalsIgnoreCase(projet.getStatut());
        tfTitre.setDisable(lockedTitreBudget);
        tfBudget.setDisable(lockedTitreBudget);

        taDescription.setDisable(false);
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

        projet.setDescription(taDescription.getText());
        projet.setCompanyAddress(emptyToNull(tfCompanyAddress.getText()));
        projet.setCompanyEmail(emptyToNull(tfCompanyEmail.getText()));
        projet.setCompanyPhone(emptyToNull(tfCompanyPhone.getText()));

        if (!isDraft) {
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


        String titre = safe(tfTitre.getText());
        if (titre.length() < 3) { error("Titre: min 3 caractères."); return; }

        double budget;
        try {
            budget = Double.parseDouble(safe(tfBudget.getText()));
            if (budget <= 0) throw new Exception();
        } catch (Exception e) { error("Budget invalide (>0)."); return; }

        projet.setTitre(titre);
        projet.setBudget(budget);

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
        Integer score = projet.getScoreEsg();
        tfScoreEsg.setText(score == null ? "En attente d'évaluation" : String.valueOf(score));
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
