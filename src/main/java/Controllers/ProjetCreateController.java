package Controllers;

import Models.Projet;
import Services.ProjetService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.GreenLedger.MainFX;

public class ProjetCreateController {

    private static final int TEST_ENTREPRISE_ID = 1;
    private final ProjetService service = new ProjetService();

    @FXML private TextField tfTitre;
    @FXML private TextField tfBudget;
    @FXML private TextField tfScoreEsg;
    @FXML private TextField tfCompanyAddress;
    @FXML private TextField tfCompanyEmail;
    @FXML private TextField tfCompanyPhone;
    @FXML private TextArea taDescription;

    @FXML
    private void onBack() { goHome(); }

    @FXML
    private void onSaveDraft() { createWithStatus("DRAFT"); }

    @FXML
    private void onAdd() {
        // si tu veux "Ajouter" = DRAFT, remplace SUBMITTED par DRAFT
        createWithStatus("SUBMITTED");
    }

    private void createWithStatus(String statut) {
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

        Projet p = new Projet();
        p.setEntrepriseId(TEST_ENTREPRISE_ID);
        p.setTitre(titre);
        p.setBudget(budget);
        p.setScoreEsg(score);
        p.setDescription(taDescription.getText());
        p.setStatut(statut);

        // ✅ nouveaux champs entreprise
        p.setCompanyAddress(safeNull(tfCompanyAddress.getText()));
        p.setCompanyEmail(safeNull(tfCompanyEmail.getText()));
        p.setCompanyPhone(safeNull(tfCompanyPhone.getText()));

        service.insert(p);
        goHome();
    }

    private void goHome() {
        try {
            MainFX.setRoot("GestionProjet");
        } catch (Exception ex) {
            error("Navigation impossible: " + ex.getMessage());
        }
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }
    private String safeNull(String s) {
        String v = safe(s);
        return v.isEmpty() ? null : v;
    }

    private void error(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
