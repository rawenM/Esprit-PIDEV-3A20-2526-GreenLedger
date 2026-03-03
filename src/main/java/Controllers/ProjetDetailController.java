package Controllers;

<<<<<<< HEAD
import Models.Projet;
import Services.ProjetService;
=======
import Models.Budget;
import Models.ProjectDocument;
import Models.Projet;
import Services.DocumentService;
import Services.ProjetService;
import javafx.collections.FXCollections;
>>>>>>> f3559248f463304c68513eb2c92f99791d2c4657
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

<<<<<<< HEAD
public class ProjetDetailController {

    private final ProjetService service = new ProjetService();
=======
import java.awt.Desktop;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ProjetDetailController {

    private final ProjetService service = new ProjetService();
    private final DocumentService documentService = new DocumentService();
>>>>>>> f3559248f463304c68513eb2c92f99791d2c4657

    private Projet projet;
    private Runnable onChanged = null;

    @FXML private Label lblId;
    @FXML private Label lblStatut;
<<<<<<< HEAD

    @FXML private TextField tfTitre;
    @FXML private TextField tfBudget;
    @FXML private TextField tfScoreEsg;

    @FXML private TextField tfCompanyAddress;
    @FXML private TextField tfCompanyEmail;
    @FXML private TextField tfCompanyPhone;

    @FXML private TextArea taDescription;

    @FXML private Button btnSaveChanges;
    @FXML private Button btnCancelEdit;

=======
    @FXML private TextField tfTitre;
    @FXML private TextField tfBudgetMontant;
    @FXML private ComboBox<String> cbBudgetDevise;
    @FXML private TextArea taBudgetRaison;
    @FXML private TextField tfScoreEsg;
    @FXML private TextField tfCompanyAddress;
    @FXML private TextField tfCompanyEmail;
    @FXML private TextField tfCompanyPhone;
    @FXML private TextArea taDescription;
    @FXML private Button btnSaveChanges;
    @FXML private Button btnCancelEdit;

    // ✅ Documents / images (read-only)
    @FXML private Label lblDocsCount;
    @FXML private ListView<String> lvDocs;

    private List<ProjectDocument> docs = new ArrayList<>();

    @FXML
    public void initialize() {
        if (cbBudgetDevise != null) {
            cbBudgetDevise.getItems().setAll("TND", "EUR", "USD");
        }

        if (lvDocs != null) {
            lvDocs.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2) {
                    onOpenSelectedDoc();
                }
            });
        }
    }

>>>>>>> f3559248f463304c68513eb2c92f99791d2c4657
    public void setProjet(Projet p) {
        this.projet = p;
        render();
    }

    public void setOnChanged(Runnable r) {
        this.onChanged = r;
    }

<<<<<<< HEAD

=======
>>>>>>> f3559248f463304c68513eb2c92f99791d2c4657
    @FXML
    private void onAnnulerProjet() {
        if (projet == null) return;

        boolean isDraft = "DRAFT".equalsIgnoreCase(projet.getStatut());
<<<<<<< HEAD

        String msg = isDraft
                ? "Supprimer définitivement ce DRAFT ?"
                : "Annuler le projet ? (sera marqué CANCELLED, pas de suppression physique)";

        if (!confirm(msg)) return;

        if (isDraft) {                         // supression si DRAFT et CANCELLED pour les autres statuts
            service.delete(projet.getId());    //ken theou taamlou kifha hezou l'partie hedhi w'badlou feha
        } else {
            service.cancel(projet.getId());
        }
=======
        String msg = isDraft
                ? "Supprimer définitivement le DRAFT ?"
                : "Annuler le projet (statut CANCELLED) ?";

        if (!confirm(msg)) return;

        if (isDraft) service.delete(projet.getId());
        else service.cancel(projet.getId());
>>>>>>> f3559248f463304c68513eb2c92f99791d2c4657

        if (onChanged != null) onChanged.run();
        closeWindow();
    }

<<<<<<< HEAD

=======
>>>>>>> f3559248f463304c68513eb2c92f99791d2c4657
    @FXML
    private void onModifier() {
        if (projet == null) return;

        btnSaveChanges.setVisible(true);
        btnCancelEdit.setVisible(true);

<<<<<<< HEAD
        boolean lockedTitreBudgetScore = !"DRAFT".equalsIgnoreCase(projet.getStatut());


        tfTitre.setDisable(lockedTitreBudgetScore);       // tsaker les champs
        tfBudget.setDisable(lockedTitreBudgetScore);
        tfScoreEsg.setDisable(lockedTitreBudgetScore);

        taDescription.setDisable(false);                 // tnajem tbadel les champs
        tfCompanyAddress.setDisable(false);
        tfCompanyEmail.setDisable(false);
        tfCompanyPhone.setDisable(false);
=======
        tfScoreEsg.setDisable(true);

        boolean lockedTitreBudget = !"DRAFT".equalsIgnoreCase(projet.getStatut());
        tfTitre.setDisable(lockedTitreBudget);
        tfBudgetMontant.setDisable(lockedTitreBudget);
        if (cbBudgetDevise != null) cbBudgetDevise.setDisable(lockedTitreBudget);
        if (taBudgetRaison != null) taBudgetRaison.setDisable(lockedTitreBudget);

        taDescription.setDisable(false);
        tfCompanyAddress.setDisable(false);
        tfCompanyEmail.setDisable(false);
        tfCompanyPhone.setDisable(false);

        // ✅ docs restent read-only (on ne les active pas)
        if (lvDocs != null) lvDocs.setDisable(false);
>>>>>>> f3559248f463304c68513eb2c92f99791d2c4657
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

<<<<<<< HEAD
        // Description + company fields toujours modifiables
=======
>>>>>>> f3559248f463304c68513eb2c92f99791d2c4657
        projet.setDescription(taDescription.getText());
        projet.setCompanyAddress(emptyToNull(tfCompanyAddress.getText()));
        projet.setCompanyEmail(emptyToNull(tfCompanyEmail.getText()));
        projet.setCompanyPhone(emptyToNull(tfCompanyPhone.getText()));

        if (!isDraft) {
<<<<<<< HEAD
            // ✅ non-DRAFT: update description + company fields (sans toucher titre/budget/score)
=======
>>>>>>> f3559248f463304c68513eb2c92f99791d2c4657
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

<<<<<<< HEAD

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
=======
        String titre = safe(tfTitre.getText());
        if (titre.length() < 3) { error("Titre: min 3 caractères."); return; }

        double montant;
        try {
            montant = Double.parseDouble(safe(tfBudgetMontant.getText()));
            if (montant <= 0) throw new Exception();
        } catch (Exception e) { error("Budget invalide (>0)."); return; }

        String raison = safe(taBudgetRaison != null ? taBudgetRaison.getText() : null);
        if (raison.length() < 3) { error("Raison budget: min 3 caractères."); return; }

        String devise = (cbBudgetDevise != null && cbBudgetDevise.getValue() != null)
                ? cbBudgetDevise.getValue()
                : "TND";

        Budget b = projet.getBudgetObj();
        if (b == null) b = new Budget();
        b.setMontant(montant);
        b.setRaison(raison);
        b.setDevise(devise);
        b.setIdProjet(projet.getId());

        projet.setTitre(titre);
        projet.setBudget(b);
>>>>>>> f3559248f463304c68513eb2c92f99791d2c4657

        service.update(projet);
        if (onChanged != null) onChanged.run();
        closeWindow();
    }

<<<<<<< HEAD
=======
    // =========================
    // DOCS / IMAGES (READ-ONLY)
    // =========================
    @FXML
    private void onOpenSelectedDoc() {
        if (lvDocs == null || docs == null || docs.isEmpty()) return;

        int idx = lvDocs.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= docs.size()) {
            error("Veuillez sélectionner un fichier.");
            return;
        }

        ProjectDocument d = docs.get(idx);
        String pth = d.getFilePath();
        if (pth == null || pth.trim().isEmpty()) {
            error("Chemin du fichier introuvable.");
            return;
        }

        try {
            File f = resolveFile(pth);
            if (!f.exists()) {
                error("Fichier introuvable sur disque:\n" + f.getAbsolutePath());
                return;
            }

            // ✅ ouvre dans l'app par défaut (Edge/Chrome/Adobe/Photos)
            Desktop.getDesktop().open(f);

        } catch (Exception ex) {
            error("Impossible d'ouvrir le fichier : " + ex.getMessage());
        }
    }

    private void loadDocuments() {
        docs = new ArrayList<>();
        if (projet == null) return;

        try {
            docs = documentService.getByProject(projet.getId());
        } catch (Exception e) {
            System.out.println("loadDocuments error: " + e.getMessage());
            docs = new ArrayList<>();
        }

        if (lblDocsCount != null) {
            lblDocsCount.setText(docs.size() + " fichier(s)");
        }

        if (lvDocs != null) {
            List<String> items = new ArrayList<>();
            for (ProjectDocument d : docs) {
                String tag = d.isImage() ? "🖼" : "📄";
                items.add(tag + " " + safe(d.getFileName()));
            }
            lvDocs.setItems(FXCollections.observableArrayList(items));
        }
    }

    private File resolveFile(String filePathFromDb) {
        File f = new File(filePathFromDb);

        // Si le chemin en DB est relatif ("uploads/.."), on le résout depuis le dossier du projet
        if (!f.isAbsolute()) {
            Path abs = Paths.get(System.getProperty("user.dir")).resolve(filePathFromDb).normalize();
            f = abs.toFile();
        }
        return f;
    }

>>>>>>> f3559248f463304c68513eb2c92f99791d2c4657
    private void render() {
        if (projet == null) return;

        lblId.setText(String.valueOf(projet.getId()));
        lblStatut.setText(projet.getStatut());

        tfTitre.setText(projet.getTitre());
<<<<<<< HEAD
        tfBudget.setText(String.valueOf(projet.getBudget()));
        tfScoreEsg.setText(String.valueOf(projet.getScoreEsg()));
        taDescription.setText(projet.getDescription());

=======

        Budget b = projet.getBudgetObj();
        double montant = (b != null) ? b.getMontant() : 0;
        tfBudgetMontant.setText(String.valueOf(montant));
        if (taBudgetRaison != null) taBudgetRaison.setText(b != null ? safe(b.getRaison()) : "");
        if (cbBudgetDevise != null) cbBudgetDevise.setValue(b != null && b.getDevise() != null ? b.getDevise() : "TND");

        Integer score = projet.getScoreEsg();
        tfScoreEsg.setText(score == null ? "En attente d'évaluation" : String.valueOf(score));

        taDescription.setText(projet.getDescription());
>>>>>>> f3559248f463304c68513eb2c92f99791d2c4657
        tfCompanyAddress.setText(projet.getCompanyAddress());
        tfCompanyEmail.setText(projet.getCompanyEmail());
        tfCompanyPhone.setText(projet.getCompanyPhone());

<<<<<<< HEAD
        // view mode
        tfTitre.setDisable(true);
        tfBudget.setDisable(true);
=======
        tfTitre.setDisable(true);
        tfBudgetMontant.setDisable(true);
        if (cbBudgetDevise != null) cbBudgetDevise.setDisable(true);
        if (taBudgetRaison != null) taBudgetRaison.setDisable(true);
>>>>>>> f3559248f463304c68513eb2c92f99791d2c4657
        tfScoreEsg.setDisable(true);

        taDescription.setDisable(true);
        tfCompanyAddress.setDisable(true);
        tfCompanyEmail.setDisable(true);
        tfCompanyPhone.setDisable(true);

        btnSaveChanges.setVisible(false);
        btnCancelEdit.setVisible(false);
<<<<<<< HEAD
=======

        // ✅ charger docs en mode lecture
        loadDocuments();
        if (lvDocs != null) lvDocs.setDisable(false);
>>>>>>> f3559248f463304c68513eb2c92f99791d2c4657
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
<<<<<<< HEAD
=======

>>>>>>> f3559248f463304c68513eb2c92f99791d2c4657
    private String emptyToNull(String s) {
        String v = safe(s);
        return v.isEmpty() ? null : v;
    }
<<<<<<< HEAD
}
=======
}
>>>>>>> f3559248f463304c68513eb2c92f99791d2c4657
