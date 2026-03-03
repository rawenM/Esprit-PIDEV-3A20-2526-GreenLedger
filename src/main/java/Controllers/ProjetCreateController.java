package Controllers;

import Models.Budget;
import Models.ProjectDocument;
import Models.Projet;
import Services.DocumentService;
import Services.ProjetService;
import Utils.ProjetPdfGenerator;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProjetCreateController {

    @FXML private TextField tfTitre;
    @FXML private TextField tfBudgetMontant;
    @FXML private ComboBox<String> cbBudgetDevise;
    @FXML private TextArea taBudgetRaison;

    @FXML private TextField tfCompanyAddress;
    @FXML private TextField tfCompanyEmail;
    @FXML private TextField tfCompanyPhone;

    @FXML private TextArea taDescription;

    @FXML private Label lblFilesCount;
    @FXML private ListView<String> lvFiles;

    private final ProjetService projetService = new ProjetService();
    private final DocumentService documentService = new DocumentService();

    private final List<File> selectedFiles = new ArrayList<>();

    @FXML
    public void initialize() {
        cbBudgetDevise.getItems().addAll("TND", "EUR", "USD");
        cbBudgetDevise.setValue("TND");
        refreshFilesUI();
    }

    // ===============================
    // BACK (NE FERME PAS L'APP)
    // ===============================
    @FXML
    private void onBack() {
        // ✅ Navigation standard (si MainFX existe)
        try {
            org.GreenLedger.MainFX.setRoot("GestionProjet"); // ton accueil
            return;
        } catch (Exception ignored) {}

        // ✅ Fallback: fermer seulement si c'est une fenêtre secondaire
        try {
            Stage stage = (Stage) tfTitre.getScene().getWindow();
            if (stage.getOwner() != null) stage.close();
        } catch (Exception ignored) {}
    }

    // ===============================
    // ADD FILES
    // ===============================
    @FXML
    private void onAddFiles() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Sélectionner des documents / images");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Documents & Images", "*.pdf", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.doc", "*.docx"),
                new FileChooser.ExtensionFilter("PDF", "*.pdf"),
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );

        List<File> files = fc.showOpenMultipleDialog(tfTitre.getScene().getWindow());
        if (files != null && !files.isEmpty()) {
            selectedFiles.addAll(files);
            refreshFilesUI();
        }
    }

    @FXML
    private void onClearFiles() {
        selectedFiles.clear();
        refreshFilesUI();
    }

    private void refreshFilesUI() {
        if (lblFilesCount != null) {
            lblFilesCount.setText(selectedFiles.size() + " fichier(s) sélectionné(s)");
        }
        if (lvFiles != null) {
            List<String> names = new ArrayList<>();
            for (File f : selectedFiles) names.add(f.getName());
            lvFiles.setItems(FXCollections.observableArrayList(names));
        }
    }

    // ===============================
    // PDF PREVIEW + OPEN BROWSER
    // ===============================
    @FXML
    private void onGeneratePdf() {
        try {
            String titre = tfTitre.getText() == null ? "" : tfTitre.getText().trim();
            if (titre.isEmpty()) {
                error("Veuillez saisir au moins un titre avant de générer le PDF.");
                return;
            }

            Projet p = buildProjetFromForm();
            String path = ProjetPdfGenerator.generatePreviewPdf(p);

            info("PDF généré avec succès.");
            openInBrowser(path);

        } catch (Exception e) {
            error("Erreur génération PDF : " + e.getMessage());
        }
    }

    // ===============================
    // SAVE DRAFT
    // ===============================
    @FXML
    private void onSaveDraft() {
        try {
            Projet p = buildProjetFromForm();
            p.setStatutEvaluation("DRAFT");

            int projectId = projetService.insertAndReturnId(p);
            if (projectId <= 0) {
                error("Impossible de créer le projet.");
                return;
            }

            uploadSelectedFiles(projectId);

            info("Projet sauvegardé en DRAFT.\nPièces jointes enregistrées : " + selectedFiles.size());
            resetForm();

        } catch (Exception e) {
            error("Erreur : " + e.getMessage());
        }
    }

    // ===============================
    // SUBMIT + PDF FINAL
    // ===============================
    @FXML
    private void onAdd() {
        try {
            Projet p = buildProjetFromForm();
            p.setStatutEvaluation("SUBMITTED");

            int projectId = projetService.insertAndReturnId(p);
            if (projectId <= 0) {
                error("Impossible de créer le projet.");
                return;
            }

            uploadSelectedFiles(projectId);

            // PDF final (avec docs + images)
            p.setId(projectId);
            List<ProjectDocument> docs = documentService.getByProject(projectId);
            String finalPdfPath = ProjetPdfGenerator.generateSubmittedPdf(p, docs);

            info("Projet soumis avec succès.\nPDF final généré.");
            openInBrowser(finalPdfPath);

            resetForm();

        } catch (Exception e) {
            error("Erreur : " + e.getMessage());
        }
    }

    private void uploadSelectedFiles(int projectId) {
        if (selectedFiles.isEmpty()) return;

        for (File f : selectedFiles) {
            try {
                documentService.saveFile(projectId, f);
            } catch (Exception ex) {
                System.out.println("Upload error: " + ex.getMessage());
            }
        }
    }

    // ===============================
    // BUILD PROJET
    // ===============================
    private Projet buildProjetFromForm() {
        Projet p = new Projet();

        p.setTitre(tfTitre.getText());
        p.setDescription(taDescription.getText());

        p.setCompanyAddress(tfCompanyAddress.getText());
        p.setCompanyEmail(tfCompanyEmail.getText());
        p.setCompanyPhone(tfCompanyPhone.getText());

        Budget b = new Budget();
        try {
            b.setMontant(Double.parseDouble(tfBudgetMontant.getText()));
        } catch (Exception e) {
            b.setMontant(0);
        }
        b.setDevise(cbBudgetDevise.getValue());
        b.setRaison(taBudgetRaison.getText());

        p.setBudget(b);

        return p;
    }

    private void resetForm() {
        tfTitre.clear();
        tfBudgetMontant.clear();
        taBudgetRaison.clear();
        cbBudgetDevise.setValue("TND");

        tfCompanyAddress.clear();
        tfCompanyEmail.clear();
        tfCompanyPhone.clear();
        taDescription.clear();

        selectedFiles.clear();
        refreshFilesUI();
    }

    private void openInBrowser(String path) {
        try {
            Desktop.getDesktop().browse(new File(path).toURI());
        } catch (Exception ex) {
            error("Impossible d'ouvrir automatiquement : " + ex.getMessage());
        }
    }

    private void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private void error(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
