package Controllers;

import Services.RiskAnalysisService;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import Services.TauxRecommendationService;
import Services.FinancementService;
import Services.ProjetService;
import Models.Financement;
import Models.Projet;
import javafx.collections.FXCollections;
import javafx.scene.layout.VBox;

public class FinanceRiskAgentController extends BaseController {

    // ── Stat cards at the top ──────────────────────────────────────
    @FXML private Label lblModelStatus;
    @FXML private Label lblRiskScore;
    @FXML private Label lblConfidence;
    @FXML private Label lblDecision;
    @FXML private Label lblLastUpdate;

    // ── Form fields ────────────────────────────────────────────────
    // These match the ARFF attributes exactly:
    // budget, duration, type, co2_impact, maturity, funding_ratio
    @FXML private TextField txtMontant;       // → budget
    @FXML private TextField txtDuree;         // → duration
    @FXML private TextField txtTaux;          // → co2_impact (repurposed, see note below)
    @FXML private TextField txtScoreCredit;   // → maturity
    @FXML private TextField txtApport;        // → funding_ratio
    @FXML private ComboBox<String> cmbSecteur; // → type
    @FXML private TextArea txtNotes;          // not used by model, just for display

    private final TauxRecommendationService tauxService    = new TauxRecommendationService();
    private final FinancementService        financementSvc = new FinancementService();
    private final ProjetService             projetSvc      = new ProjetService();

    @FXML private ComboBox<Financement> cmbFinancementTaux;
    @FXML private ComboBox<String>      cmbTypeOffreTaux;
    @FXML private TextField             txtDureeTaux;
    @FXML private TextField             txtAncienneteTaux;
    @FXML private VBox                  tauxResultPanel;
    @FXML private Label                 lblProjetTitre;
    @FXML private Label                 lblProjetStatut;
    @FXML private Label                 lblProjetEsg;
    @FXML private Label                 lblTauxResult;
    @FXML private Label                 lblTauxInterpretation;
    @FXML private Label                 lblBreakdownProjet;
    @FXML private Label                 lblBreakdownStatut;
    @FXML private Label                 lblBreakdownEsg;
    @FXML private Label                 lblBreakdownDuree;
    @FXML private Label                 lblTauxExplication;

    // ── The Weka service ──────────────────────────────────────────
    // This is the class we created in Services/RiskAnalysisService.java
    private RiskAnalysisService riskService;

    // ─────────────────────────────────────────────────────────────
    // initialize() is called automatically by JavaFX when the
    // FXML is loaded. Think of it as the constructor for controllers.
    // ─────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {

        // Populate the ComboBox with the exact values from the ARFF
        // IMPORTANT: these must match type_offre values in your ARFF exactly
        if (cmbSecteur != null) {
            cmbSecteur.getItems().setAll(
                    "solar",
                    "wind",
                    "agriculture",
                    "recycling"
            );
        }

        // Load the Weka model on startup
        // RiskAnalysisService loads ClaudeRisk.model from resources
        riskService = new RiskAnalysisService();

        // Update the status card at the top
        if (lblModelStatus != null) {
            lblModelStatus.setText("✅ Pret");
        }
        initTauxSection();
    }

    // ─────────────────────────────────────────────────────────────
    // This is called when the investor clicks "Analyser le risque"
    // ─────────────────────────────────────────────────────────────
    @FXML
    private void handleRunRiskAnalysis() {

        // Step 1: Make sure the service loaded correctly
        if (riskService == null) {
            setDecision("❌ Service indisponible. Verifiez ClaudeRisk.model dans resources/FinanceAiModels/");
            return;
        }

        // Step 2: Read values from the form fields
        String budgetStr       = safeText(txtMontant);
        String durationStr     = safeText(txtDuree);
        String co2Str          = safeText(txtTaux);        // we're using taux field for co2_impact
        String maturityStr     = safeText(txtScoreCredit); // using scoreCredit field for maturity
        String fundingRatioStr = safeText(txtApport);      // using apport field for funding_ratio
        String type            = cmbSecteur != null && cmbSecteur.getValue() != null
                ? cmbSecteur.getValue() : "";

        // Step 3: Validate — the 3 most important fields must not be empty
        if (budgetStr.isEmpty() || durationStr.isEmpty() || type.isEmpty()) {
            setDecision("⚠️ Veuillez renseigner : Montant, Duree, et le type de projet.");
            return;
        }

        // Step 4: Convert strings to numbers safely
        double budget       = parseDouble(budgetStr);
        double duration     = parseDouble(durationStr);
        double co2Impact    = parseDouble(co2Str);
        double maturity     = parseDouble(maturityStr);
        double fundingRatio = parseDouble(fundingRatioStr);

        // Step 5: Basic sanity checks on values
        if (fundingRatio > 1.0) {
            // If user typed 80 instead of 0.8, auto-correct it
            fundingRatio = fundingRatio / 100.0;
        }

        // Step 6: ✅ THIS IS THE ACTUAL WEKA CALL
        // We pass all 6 values to the service which feeds them to the J48 model
        // and returns "low", "medium", or "high"
        String riskLevel = riskService.predictRisk(
                budget,
                duration,
                type,
                co2Impact,
                maturity,
                fundingRatio
        );

        // Step 7: Display the result in the UI
        updateUI(riskLevel, budget, duration, type, co2Impact, maturity, fundingRatio);
    }

    // ─────────────────────────────────────────────────────────────
    // Takes the risk result and updates all the labels on screen
    // ─────────────────────────────────────────────────────────────
    private void updateUI(String riskLevel, double budget, double duration,
                          String type, double co2Impact, double maturity, double fundingRatio) {

        // Choose emoji and color hint based on result
        String riskDisplay;
        String confidence;
        String recommendation;

        switch (riskLevel) {
            case "low" -> {
                riskDisplay    = "🟢 FAIBLE";
                confidence     = "Fiable — Investissement recommande";
                recommendation = "✅ DECISION: Dossier solide. Financement approuvable.\n\n"
                        + "• Budget: " + (int) budget + " TND\n"
                        + "• Duree: " + (int) duration + " ans\n"
                        + "• Type projet: " + type + "\n"
                        + "• Impact CO2: " + co2Impact + "\n"
                        + "• Maturite entreprise: " + maturity + " ans\n"
                        + "• Ratio financement: " + String.format("%.0f%%", fundingRatio * 100) + "\n\n"
                        + "Le modele J48 classifie ce dossier comme FAIBLE RISQUE.\n"
                        + "Les indicateurs cles (ratio de financement et impact CO2) sont favorables.";
            }
            case "medium" -> {
                riskDisplay    = "🟡 MOYEN";
                confidence     = "Moderee — Analyse approfondie requise";
                recommendation = "⚠️ DECISION: Risque modere detecte. Conditions supplementaires conseillees.\n\n"
                        + "• Budget: " + (int) budget + " TND\n"
                        + "• Duree: " + (int) duration + " ans\n"
                        + "• Type projet: " + type + "\n"
                        + "• Impact CO2: " + co2Impact + "\n"
                        + "• Maturite entreprise: " + maturity + " ans\n"
                        + "• Ratio financement: " + String.format("%.0f%%", fundingRatio * 100) + "\n\n"
                        + "Le modele J48 classifie ce dossier comme RISQUE MOYEN.\n"
                        + "Recommande: demander des garanties supplementaires ou reduire le montant.";
            }
            default -> {
                riskDisplay    = "🔴 ELEVE";
                confidence     = "Alerte — Risque significatif detecte";
                recommendation = "❌ DECISION: Risque eleve. Financement deconseille en l'etat.\n\n"
                        + "• Budget: " + (int) budget + " TND\n"
                        + "• Duree: " + (int) duration + " ans\n"
                        + "• Type projet: " + type + "\n"
                        + "• Impact CO2: " + co2Impact + "\n"
                        + "• Maturite entreprise: " + maturity + " ans\n"
                        + "• Ratio financement: " + String.format("%.0f%%", fundingRatio * 100) + "\n\n"
                        + "Le modele J48 classifie ce dossier comme RISQUE ELEVE.\n"
                        + "Facteurs critiques: ratio de financement faible et/ou impact CO2 insuffisant.\n"
                        + "Recommande: refuser ou demander un apport personnel significatif.";
            }
        }

        // Update the stat cards
        if (lblRiskScore  != null) lblRiskScore.setText(riskDisplay);
        if (lblConfidence != null) lblConfidence.setText(confidence);

        // Update the big result text box
        if (lblDecision   != null) lblDecision.setText(recommendation);

        // Timestamp
        if (lblLastUpdate != null) {
            lblLastUpdate.setText("MAJ: " + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Clears all fields and resets to default state
    // ─────────────────────────────────────────────────────────────
    @FXML
    private void handleReset() {
        if (txtMontant    != null) txtMontant.clear();
        if (txtDuree      != null) txtDuree.clear();
        if (txtTaux       != null) txtTaux.clear();
        if (txtScoreCredit!= null) txtScoreCredit.clear();
        if (txtApport     != null) txtApport.clear();
        if (cmbSecteur    != null) cmbSecteur.getSelectionModel().clearSelection();
        if (txtNotes      != null) txtNotes.clear();
        if (lblDecision   != null) lblDecision.setText("Aucune analyse pour le moment.");
        if (lblRiskScore  != null) lblRiskScore.setText("N/A");
        if (lblConfidence != null) lblConfidence.setText("N/A");
        if (lblLastUpdate != null) lblLastUpdate.setText("");
    }

    // ─────────────────────────────────────────────────────────────
    // Reloads the Weka model (useful if you replace the .model file)
    // ─────────────────────────────────────────────────────────────
    @FXML
    private void handleReloadModel() {
        riskService = new RiskAnalysisService();
        if (lblModelStatus != null) lblModelStatus.setText("✅ Recharge");
    }

    // ── Navigation handlers ───────────────────────────────────────
    @FXML
    private void handleGoFinancement() {
        try { org.GreenLedger.MainFX.setRoot("financement"); }
        catch (IOException ex) { System.err.println("Nav error: " + ex.getMessage()); }
    }

    @FXML
    private void handleGoDashboard() {
        try { org.GreenLedger.MainFX.setRoot("fxml/dashboard"); }
        catch (IOException ex) { System.err.println("Nav error: " + ex.getMessage()); }
    }

    @FXML
    private void handleGoSettings() {
        try { org.GreenLedger.MainFX.setRoot("settings"); }
        catch (IOException ex) { System.err.println("Nav error: " + ex.getMessage()); }
    }

    // ── Utility helpers ───────────────────────────────────────────
    private String safeText(TextField field) {
        return (field == null || field.getText() == null) ? "" : field.getText().trim();
    }

    private double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) return 0;
        try { return Double.parseDouble(value.trim()); }
        catch (NumberFormatException ex) { return 0; }
    }

    private void setDecision(String message) {
        if (lblDecision   != null) lblDecision.setText(message);
        if (lblRiskScore  != null) lblRiskScore.setText("N/A");
        if (lblConfidence != null) lblConfidence.setText("N/A");
    }

    private void initTauxSection() {
        try {
            // Load all financement records into the combo
            List<Financement> financements = financementSvc.getAll();
            if (financements != null) {
                cmbFinancementTaux.setItems(FXCollections.observableArrayList(financements));
            }

            // Custom display: show "ID — Projet ID — Montant TND"
            cmbFinancementTaux.setCellFactory(lv -> new javafx.scene.control.ListCell<Financement>() {
                @Override
                protected void updateItem(Financement item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? ""
                            : "Financement #" + item.getId()
                            + " | Projet " + item.getProjetId()
                            + " | " + String.format("%,.0f TND", item.getMontant()));
                }
            });
            cmbFinancementTaux.setButtonCell(new javafx.scene.control.ListCell<Financement>() {
                @Override
                protected void updateItem(Financement item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? ""
                            : "Financement #" + item.getId()
                            + " | Projet " + item.getProjetId());
                }
            });

            // When user selects a financement → fetch and display the linked projet info
            cmbFinancementTaux.getSelectionModel().selectedItemProperty()
                    .addListener((obs, oldVal, selected) -> {
                        if (selected == null) return;
                        try {
                            // Fetch the projet using the projet_id from financement
                            Projet projet = projetSvc.getById(selected.getProjetId());
                            if (projet != null) {
                                lblProjetTitre.setText(projet.getTitre() != null
                                        ? projet.getTitre() : "Sans titre");
                                lblProjetStatut.setText("Statut: " + projet.getStatut());

                                if (projet.getScoreEsg() != null) {
                                    lblProjetEsg.setText(projet.getScoreEsg() + " / 100");
                                    // Color code ESG
                                    if (projet.getScoreEsg() >= 70)
                                        lblProjetEsg.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#065f46;");
                                    else if (projet.getScoreEsg() >= 40)
                                        lblProjetEsg.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#92400e;");
                                    else
                                        lblProjetEsg.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#991b1b;");
                                } else {
                                    lblProjetEsg.setText("Non évalué");
                                    lblProjetEsg.setStyle("-fx-font-size:13px; -fx-text-fill:#94a3b8;");
                                }
                            } else {
                                lblProjetTitre.setText("Projet introuvable");
                                lblProjetStatut.setText("");
                                lblProjetEsg.setText("—");
                            }
                        } catch (Exception e) {
                            System.err.println("Error fetching projet: " + e.getMessage());
                            lblProjetTitre.setText("Erreur de chargement");
                        }
                    });

        } catch (Exception e) {
            System.err.println("[TauxSection] Init error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void recommanderTaux() {
        // ── Validate inputs ──────────────────────────────────────────
        Financement selectedFin = cmbFinancementTaux.getValue();
        if (selectedFin == null) {
            showAlert("Champ requis", "Veuillez sélectionner un financement.", Alert.AlertType.WARNING);
            return;
        }

        String typeOffre = cmbTypeOffreTaux.getValue();
        if (typeOffre == null) {
            showAlert("Champ requis", "Veuillez sélectionner un type d'offre.", Alert.AlertType.WARNING);
            return;
        }

        String dureeStr = txtDureeTaux.getText().trim();
        String ancienneteStr = txtAncienneteTaux.getText().trim();
        if (dureeStr.isEmpty() || ancienneteStr.isEmpty()) {
            showAlert("Champ requis", "Veuillez renseigner la durée et l'ancienneté.", Alert.AlertType.WARNING);
            return;
        }

        int duree;
        double anciennete;
        try {
            duree = Integer.parseInt(dureeStr);
            anciennete = Double.parseDouble(ancienneteStr);
            if (duree <= 0 || anciennete <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showAlert("Valeur invalide", "Durée et ancienneté doivent être des nombres positifs.", Alert.AlertType.ERROR);
            return;
        }

        // ── Fetch projet from DB using projet_id in financement ──────
        try {
            Projet projet = projetSvc.getById(selectedFin.getProjetId());

            int scoreEsg = 50; // default if null
            String statut = "DRAFT";
            String titrePr = "Inconnu";

            if (projet != null) {
                if (projet.getScoreEsg() != null) scoreEsg = projet.getScoreEsg();
                if (projet.getStatut()   != null) statut   = projet.getStatut();
                if (projet.getTitre()    != null) titrePr  = projet.getTitre();
            }

            // Budget: use financement montant as proxy since budget table
            // may not always have a linked record
            double budgetMontant = selectedFin.getMontant();

            // ── Run ML prediction ─────────────────────────────────────
            double predictedTaux = tauxService.predictTaux(
                    scoreEsg,
                    budgetMontant,
                    statut,
                    duree,
                    typeOffre,
                    anciennete
            );

            if (predictedTaux < 0) {
                showAlert("Erreur modèle", "Le modèle ML n'a pas pu générer une prédiction. Vérifiez que ClaudeTaux.model est bien chargé.", Alert.AlertType.ERROR);
                return;
            }

            // ── Display results ───────────────────────────────────────
            tauxResultPanel.setVisible(true);
            tauxResultPanel.setManaged(true);

            lblTauxResult.setText(String.format("%.2f %%", predictedTaux));
            lblTauxInterpretation.setText(tauxService.interpretTaux(predictedTaux));

            // Breakdown cards
            lblBreakdownProjet.setText(titrePr);
            lblBreakdownStatut.setText(statut);
            lblBreakdownEsg.setText(scoreEsg + " / 100");
            lblBreakdownDuree.setText(duree + " mois");

            // Color the taux based on value
            if (predictedTaux <= 4)
                lblTauxResult.setStyle("-fx-font-size:36px; -fx-font-weight:bold; -fx-text-fill:#10b981;");
            else if (predictedTaux <= 7)
                lblTauxResult.setStyle("-fx-font-size:36px; -fx-font-weight:bold; -fx-text-fill:#f59e0b;");
            else
                lblTauxResult.setStyle("-fx-font-size:36px; -fx-font-weight:bold; -fx-text-fill:#ef4444;");

            // Explanation text
            String explication = buildExplanation(scoreEsg, statut, duree, typeOffre, anciennete, predictedTaux);
            lblTauxExplication.setText(explication);

        } catch (Exception e) {
            System.err.println("[recommanderTaux] Error: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible de générer la recommandation: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Builds a human-readable explanation of why the model
     * recommended this specific taux, based on the input values.
     * This explains the model's reasoning to the investor.
     */
    private String buildExplanation(int scoreEsg, String statut, int duree,
                                    String typeOffre, double anciennete, double taux) {
        StringBuilder sb = new StringBuilder();

        // ESG impact
        if (scoreEsg >= 70) {
            sb.append("✅ Score ESG élevé (").append(scoreEsg).append(") — réduit le taux de ~").append(String.format("%.1f", scoreEsg * 0.035)).append("%. ");
        } else if (scoreEsg >= 40) {
            sb.append("⚠️ Score ESG modéré (").append(scoreEsg).append(") — impact neutre sur le taux. ");
        } else {
            sb.append("❌ Score ESG faible (").append(scoreEsg).append(") — augmente le taux de risque. ");
        }

        // Statut impact
        switch (statut) {
            case "APPROVED":    sb.append("✅ Statut APPROVED — profil validé, taux réduit de ~2%. "); break;
            case "IN_PROGRESS": sb.append("🔵 Statut IN_PROGRESS — projet en cours, impact minime. "); break;
            case "CANCELLED":   sb.append("❌ Statut CANCELLED — historique risqué, taux majoré de ~1.8%. "); break;
            default:            sb.append("⚪ Statut " + statut + " — impact standard. "); break;
        }

        // Duration impact
        if (duree > 180) {
            sb.append("⚠️ Durée longue (").append(duree).append(" mois) — augmente le taux de ~").append(String.format("%.1f", (duree - 12) / 348.0 * 2.5)).append("%. ");
        } else {
            sb.append("✅ Durée raisonnable (").append(duree).append(" mois). ");
        }

        // Anciennete
        if (anciennete >= 10) {
            sb.append("✅ Entreprise établie (").append((int) anciennete).append(" ans) — réduit le taux. ");
        } else {
            sb.append("⚠️ Entreprise jeune (").append((int) anciennete).append(" ans) — risque légèrement plus élevé. ");
        }

        sb.append("→ Taux final recommandé : ").append(String.format("%.2f", taux)).append("%.");
        return sb.toString();
    }

    /**
     * Resets the taux panel to its initial hidden state.
     */
    @FXML
    private void resetTauxPanel() {
        tauxResultPanel.setVisible(false);
        tauxResultPanel.setManaged(false);
        cmbFinancementTaux.getSelectionModel().clearSelection();
        cmbTypeOffreTaux.getSelectionModel().clearSelection();
        txtDureeTaux.clear();
        txtAncienneteTaux.clear();
        lblProjetTitre.setText("—");
        lblProjetStatut.setText("");
        lblProjetEsg.setText("—");
    }

    // ── Helper: show alert ────────────────────────────────────────────
// (only add this if your controller doesn't already have showAlert)
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}
