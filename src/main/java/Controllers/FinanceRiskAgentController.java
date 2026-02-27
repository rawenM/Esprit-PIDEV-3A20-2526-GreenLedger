package Controllers;

import Services.RiskAnalysisService;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
}
