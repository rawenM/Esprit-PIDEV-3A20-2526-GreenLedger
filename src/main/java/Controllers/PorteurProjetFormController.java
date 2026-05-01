package Controllers;

import Models.Projet;
import Services.ProjetService;
import Utils.EnvLoader;
import Utils.NavigationContext;
import Utils.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Pages 2 & 3 — Create / Edit project form for Porteur de Projet.
 * Handles completeness score, validation, save, submit, and AI assistant.
 */
public class PorteurProjetFormController extends BaseController {

    // ── Top bar ───────────────────────────────────────────────────────────
    @FXML private Label  lblFormTitle;
    @FXML private Label  lblCompletenessScore;
    @FXML private Button btnSubmit;
    @FXML private Button btnSubmitBottom;

    // ── Completeness ──────────────────────────────────────────────────────
    @FXML private ProgressBar progressCompleteness;
    @FXML private Label       lblCompletePct;
    @FXML private Label       lblMissingFields;

    // ── Right panel: live preview ─────────────────────────────────────────
    @FXML private Label       lblEsgScore;
    @FXML private Label       lblEsgDecision;
    @FXML private ProgressBar progressEsg;
    @FXML private Label       lblEsgDetail;
    @FXML private Label       lblTotalTco2;
    @FXML private Label       lblScope1;
    @FXML private Label       lblScope2;
    @FXML private Label       lblScope3;
    @FXML private Label       lblGreenCredits;
    @FXML private Label       lblCreditsDetail;
    @FXML private Label       lblCarbonRisk;

    // ── Section 1 ─────────────────────────────────────────────────────────
    @FXML private TextField txtTitre;
    @FXML private TextArea  txtDescription;

    // ── Section 2 ─────────────────────────────────────────────────────────
    @FXML private TextField txtAddress;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private TextField txtLocalisation;

    // ── Section 3 ─────────────────────────────────────────────────────────
    @FXML private ComboBox<String> cmbSecteur;
    @FXML private ComboBox<String> cmbTypeProjet;

    // ── Section 4 ─────────────────────────────────────────────────────────
    @FXML private TextField        txtEnergie;
    @FXML private ComboBox<String> cmbUniteEnergie;
    @FXML private TextField        txtTransport;
    @FXML private ComboBox<String> cmbTypeTransport;
    @FXML private TextField        txtMateriau;
    @FXML private ComboBox<String> cmbTypeMateriau;
    @FXML private TextField        txtEau;
    @FXML private TextField        txtDechets;
    @FXML private TextField        txtEmissions;
    @FXML private ComboBox<String> cmbSourceEmissions;

    // ── Section 5 ─────────────────────────────────────────────────────────
    @FXML private TextField txtMontant;
    @FXML private TextArea  txtPitch;

    // ── AI ────────────────────────────────────────────────────────────────
    @FXML private TextField txtAiQuestion;
    @FXML private TextArea  txtAiResponse;

    // ── Error ─────────────────────────────────────────────────────────────
    @FXML private Label lblError;

    private final ProjetService projetService = new ProjetService();
    private Projet editingProjet = null;  // null = create mode

    @FXML
    public void initialize() {
        super.initialize();
        populateDropdowns();
        attachCompletenessListeners();

        Integer projectId = NavigationContext.getInstance().getCurrentProjectId();
        if (projectId != null && projectId > 0) {
            editingProjet = projetService.getById(projectId);
            if (editingProjet != null) {
                // Validate ownership
                var user = SessionManager.getInstance().getCurrentUser();
                if (user == null || editingProjet.getEntrepriseId() != user.getId().intValue()) {
                    showError("Acces refuse.");
                    return;
                }
                // Block edit if APPROVED or REJECTED
                String statut = editingProjet.getStatutEvaluation();
                if ("APPROVED".equals(statut) || "REJECTED".equals(statut)) {
                    showError("Ce projet ne peut plus etre modifie (statut: " + statut + ").");
                    disableForm();
                    return;
                }
                lblFormTitle.setText("Modifier le projet");
                populateForm(editingProjet);
                // Show submit button only for DRAFT
                if ("DRAFT".equals(statut)) {
                    btnSubmit.setVisible(true);
                    btnSubmit.setManaged(true);
                    if (btnSubmitBottom != null) { btnSubmitBottom.setVisible(true); btnSubmitBottom.setManaged(true); }
                }
            }
        } else {
            NavigationContext.getInstance().setCurrentProjectId(null);
            lblFormTitle.setText("Nouveau Projet");
        }
        updateCompleteness();
        updateLivePreview();
        applyProfile(lblProfileName, lblProfileType);
    }

    // ── Sidebar ───────────────────────────────────────────────────────────
    @FXML private Label lblProfileName;
    @FXML private Label lblProfileType;

    // ── Sidebar navigation ────────────────────────────────────────────────
    @FXML private void onDashboard()   { navigate("fxml/porteur_shell"); }
    @FXML private void onProjets()     { navigate("fxml/porteur_projets"); }
    @FXML private void onEvaluations() { navigate("ownerEvaluations"); }
    @FXML private void onFinancing()   { navigate("financement"); }
    @FXML private void onMessages()    { navigate("fxml/porteur_messages"); }
    @FXML private void onAssistant()   { navigate("fxml/porteur_assistant"); }
    @FXML private void onEditProfile() { navigate("editProfile"); }
    @FXML private void onLogout() {
        Utils.SessionManager.getInstance().invalidate();
        navigate("fxml/login");
    }

    @FXML private void onBack() { navigate("fxml/porteur_projets"); }

    @FXML
    private void onSave() {
        lblError.setText("");
        String error = validate();
        if (error != null) { showError(error); return; }

        var user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        Projet p = buildProjet();
        p.setEntrepriseId(user.getId().intValue());

        if (editingProjet != null) {
            p.setId(editingProjet.getId());
            p.setStatutEvaluation(editingProjet.getStatutEvaluation());
            projetService.update(p);
            new Alert(Alert.AlertType.INFORMATION, "Projet mis a jour !", ButtonType.OK).showAndWait();
        } else {
            p.setStatutEvaluation("DRAFT");
            int newId = projetService.insertAndReturnId(p);
            if (newId > 0) {
                NavigationContext.getInstance().setCurrentProjectId(newId);
                editingProjet = projetService.getById(newId);
                btnSubmit.setVisible(true);
                btnSubmit.setManaged(true);
                if (btnSubmitBottom != null) { btnSubmitBottom.setVisible(true); btnSubmitBottom.setManaged(true); }
                new Alert(Alert.AlertType.INFORMATION, "Projet cree !", ButtonType.OK).showAndWait();
            } else {
                showError("Erreur lors de la creation du projet.");
                return;
            }
        }
        navigate("fxml/porteur_projets");
    }

    @FXML
    private void onSubmit() {
        if (editingProjet == null) { onSave(); return; }

        // Check completeness >= 80%
        int score = computeCompletenessScore();
        if (score < 80) {
            showError("Completude insuffisante (" + score + "%). Minimum 80% requis pour soumettre.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Soumettre ce projet pour evaluation ?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                projetService.updateStatut(editingProjet.getId(), "SUBMITTED");
                new Alert(Alert.AlertType.INFORMATION,
                    "Projet soumis pour evaluation !", ButtonType.OK).showAndWait();
                navigate("fxml/porteur_projets");
            }
        });
    }

    @FXML
    private void onAskAi() {
        String question = txtAiQuestion.getText().trim();
        if (question.isBlank()) return;

        txtAiResponse.setText("Chargement...");
        String projectJson = buildProjectJson();

        new Thread(() -> {
            String response = callOpenRouter(question, projectJson);
            Platform.runLater(() -> txtAiResponse.setText(
                response != null ? response : fallbackAiResponse()));
        }).start();
    }

    // ── Completeness ──────────────────────────────────────────────────────

    private void attachCompletenessListeners() {
        List<javafx.scene.control.TextInputControl> fields = List.of(
            txtTitre, txtDescription, txtAddress, txtEmail, txtPhone, txtLocalisation,
            txtEnergie, txtTransport, txtMateriau, txtEau, txtDechets, txtEmissions);
        for (var f : fields) {
            f.textProperty().addListener((o, a, b) -> { updateCompleteness(); updateLivePreview(); });
        }
        List<ComboBox<String>> combos = List.of(
            cmbSecteur, cmbTypeProjet, cmbUniteEnergie, cmbTypeTransport,
            cmbTypeMateriau, cmbSourceEmissions);
        for (var c : combos) {
            c.valueProperty().addListener((o, a, b) -> { updateCompleteness(); updateLivePreview(); });
        }
    }

    // ── Live preview (right panel) ────────────────────────────────────────

    private void updateLivePreview() {
        // Carbon heuristic (mirrors fallback formula)
        double energy    = parseDoubleOr(txtEnergie, 0);
        double transport = parseDoubleOr(txtTransport, 0);
        double material  = parseDoubleOr(txtMateriau, 0);
        double waste     = parseDoubleOr(txtDechets, 0);

        // Unit conversion for energy
        String unit = cmbUniteEnergie.getValue();
        double energyKwh = energy;
        if ("MWh".equals(unit)) energyKwh = energy * 1000;
        else if ("GWh".equals(unit)) energyKwh = energy * 1_000_000;
        else if ("Wh".equals(unit))  energyKwh = energy / 1000;

        double s1 = round3((transport * 0.00008) + (waste * 0.00070));
        double s2 = round3(energyKwh * 0.00045);
        double s3 = round3(material  * 0.00120);
        double total = round3(s1 + s2 + s3);

        // ESG score prediction (mirrors heuristic)
        double esgRaw = Math.max(0, Math.min(10, 9.5 - (total / 25.0)));
        int esgScore = (int) Math.round(esgRaw);

        // Carbon risk
        String risk = total >= 50 ? "HIGH" : total >= 20 ? "MEDIUM" : "LOW";

        // Decision
        String decision;
        if ("HIGH".equals(risk))                          decision = "REJECTED";
        else if (esgScore >= 7 && "LOW".equals(risk))     decision = "APPROVED";
        else if (esgScore >= 5)                           decision = "REVISION";
        else                                              decision = "REJECTED";

        // Green credits estimate (simplified)
        double baseline = (energyKwh * 0.0006) + (transport * 0.00012) + (material * 0.0015) + (waste * 0.0009);
        double avoided  = Math.max(0, baseline - total);
        double credits  = round3(avoided * 0.85); // credibility factor

        // Update right panel
        if (lblEsgScore != null) {
            lblEsgScore.setText(total > 0 ? String.valueOf(esgScore) : "—");
            String scoreColor = esgScore >= 7 ? "#6EE7B7" : esgScore >= 5 ? "#FCD34D" : "#F87171";            lblEsgScore.setStyle("-fx-font-size:42px;-fx-font-weight:800;-fx-text-fill:" + scoreColor + ";");
        }
        if (progressEsg != null) progressEsg.setProgress(total > 0 ? esgScore / 10.0 : 0);

        if (lblEsgDecision != null && total > 0) {
            String decColor = decision.startsWith("APPROV") ? "#D1FAE5;-fx-text-fill:#065F46"
                            : decision.equals("REVISION")   ? "#FEF3C7;-fx-text-fill:#92400E"
                            : "#FEE2E2;-fx-text-fill:#991B1B";
            lblEsgDecision.setText(decision);
            lblEsgDecision.setStyle("-fx-font-size:10px;-fx-font-weight:700;-fx-background-color:" + decColor
                + ";-fx-background-radius:4;-fx-padding:2 7;");
        }

        if (lblEsgDetail != null) {
            lblEsgDetail.setText(total > 0
                ? String.format(java.util.Locale.ROOT, "Total: %.3f tCO2e | Risque: %s", total, risk)
                : "Remplissez les donnees environnementales");
        }

        if (lblTotalTco2 != null) lblTotalTco2.setText(total > 0 ? String.format(java.util.Locale.ROOT, "%.3f", total) : "—");
        if (lblScope1 != null)    lblScope1.setText(total > 0 ? String.format(java.util.Locale.ROOT, "%.3f", s1) : "—");
        if (lblScope2 != null)    lblScope2.setText(total > 0 ? String.format(java.util.Locale.ROOT, "%.3f", s2) : "—");
        if (lblScope3 != null)    lblScope3.setText(total > 0 ? String.format(java.util.Locale.ROOT, "%.3f", s3) : "—");

        if (lblGreenCredits != null) {
            lblGreenCredits.setText(credits > 0 ? String.format(java.util.Locale.ROOT, "%.2f", credits) : "—");
        }
        if (lblCreditsDetail != null && credits > 0) {
            lblCreditsDetail.setText(String.format(java.util.Locale.ROOT,
                "Evite: %.3f tCO2e | Baseline: %.3f tCO2e", avoided, baseline));
        }

        if (lblCarbonRisk != null && total > 0) {
            String riskColor = "HIGH".equals(risk) ? "#F87171" : "MEDIUM".equals(risk) ? "#FCD34D" : "#6EE7B7";
            lblCarbonRisk.setText(risk);
            lblCarbonRisk.setStyle("-fx-font-size:16px;-fx-font-weight:800;-fx-text-fill:" + riskColor + ";");
        }
    }

    private double parseDoubleOr(TextField f, double def) {
        if (f == null || blank(f)) return def;
        try { return Double.parseDouble(f.getText().trim()); } catch (Exception e) { return def; }
    }

    private double round3(double v) { return Math.round(v * 1000.0) / 1000.0; }

    private void updateCompleteness() {
        int score = computeCompletenessScore();
        progressCompleteness.setProgress(score / 100.0);
        lblCompletePct.setText(score + "%");
        lblCompletenessScore.setText("Completude: " + score + "%");

        List<String> missing = getMissingFields();
        if (missing.isEmpty()) {
            lblMissingFields.setText("Dossier complet !");
        } else {
            lblMissingFields.setText("Manquant: " + String.join(", ", missing.subList(0, Math.min(5, missing.size()))));
        }

        String color = score >= 80 ? "#2D5F3F" : score >= 50 ? "#D97706" : "#DC2626";
        lblCompletePct.setStyle("-fx-font-size:14px;-fx-font-weight:800;-fx-text-fill:" + color + ";");
        lblCompletenessScore.setText("Completude: " + score + "%");
        lblCompletenessScore.setStyle("-fx-font-size:12px;-fx-font-weight:700;-fx-text-fill:" + color + ";");
    }

    private int computeCompletenessScore() {
        int filled = 0, total = 19;
        if (!blank(txtTitre))       filled++;
        if (!blank(txtDescription)) filled++;
        if (!blank(txtAddress))     filled++;
        if (!blank(txtEmail))       filled++;
        if (!blank(txtPhone))       filled++;
        if (!blank(txtLocalisation))filled++;
        // lat+lng counted as 1 (not in form, skip)
        if (cmbSecteur.getValue()    != null) filled++;
        if (cmbTypeProjet.getValue() != null) filled++;
        if (!blank(txtEnergie))     filled++;
        if (cmbUniteEnergie.getValue() != null) filled++;
        if (!blank(txtTransport))   filled++;
        if (cmbTypeTransport.getValue() != null) filled++;
        if (cmbTypeMateriau.getValue()  != null) filled++;
        if (!blank(txtMateriau))    filled++;
        if (!blank(txtEau))         filled++;
        if (!blank(txtDechets))     filled++;
        if (!blank(txtEmissions))   filled++;
        if (cmbSourceEmissions.getValue() != null) filled++;
        return (int) Math.round((double) filled / total * 100);
    }

    private List<String> getMissingFields() {
        List<String> m = new ArrayList<>();
        if (blank(txtTitre))       m.add("Titre");
        if (blank(txtDescription)) m.add("Description");
        if (blank(txtAddress))     m.add("Adresse");
        if (blank(txtEmail))       m.add("Email");
        if (blank(txtPhone))       m.add("Telephone");
        if (blank(txtLocalisation))m.add("Localisation");
        if (cmbSecteur.getValue()    == null) m.add("Secteur");
        if (cmbTypeProjet.getValue() == null) m.add("Type projet");
        if (blank(txtEnergie))     m.add("Energie");
        if (blank(txtTransport))   m.add("Transport");
        if (blank(txtMateriau))    m.add("Materiau");
        if (blank(txtEau))         m.add("Eau");
        if (blank(txtDechets))     m.add("Dechets");
        if (blank(txtEmissions))   m.add("Emissions");
        return m;
    }

    // ── Validation ────────────────────────────────────────────────────────

    private String validate() {
        if (blank(txtTitre) || txtTitre.getText().trim().length() < 3)
            return "Le titre doit contenir au moins 3 caracteres.";
        if (txtTitre.getText().trim().length() > 150)
            return "Le titre ne peut pas depasser 150 caracteres.";
        if (blank(txtDescription) || txtDescription.getText().trim().length() < 10)
            return "La description doit contenir au moins 10 caracteres.";
        if (blank(txtAddress))
            return "L'adresse est obligatoire.";
        if (blank(txtEmail) || !txtEmail.getText().contains("@"))
            return "Email invalide.";
        if (blank(txtPhone) || txtPhone.getText().trim().length() < 8)
            return "Le telephone doit contenir au moins 8 caracteres.";
        // Validate positive decimals
        for (TextField f : List.of(txtEnergie, txtTransport, txtMateriau, txtEau, txtDechets, txtEmissions, txtMontant)) {
            if (!blank(f)) {
                try {
                    double v = Double.parseDouble(f.getText().trim());
                    if (v < 0) return "Les valeurs numeriques doivent etre positives.";
                } catch (NumberFormatException e) {
                    return "Valeur numerique invalide: " + f.getText();
                }
            }
        }
        return null;
    }

    // ── Form helpers ──────────────────────────────────────────────────────

    private void populateDropdowns() {
        cmbSecteur.setItems(FXCollections.observableArrayList(
            "Energie", "Agriculture", "Industrie", "Transport", "Batiment", "Eau", "Dechets", "Foret", "Autre"));
        cmbTypeProjet.setItems(FXCollections.observableArrayList(
            "Solaire", "Eolien", "Hydraulique", "Biomasse", "Geothermie",
            "Efficacite energetique", "Agroforesterie", "Recyclage", "Transport propre", "Autre"));
        cmbUniteEnergie.setItems(FXCollections.observableArrayList("kWh", "MWh", "GWh", "Wh"));
        cmbTypeTransport.setItems(FXCollections.observableArrayList("camion", "train", "bateau", "avion"));
        cmbTypeMateriau.setItems(FXCollections.observableArrayList("acier", "aluminium", "bois", "plastique", "beton", "autre"));
        cmbSourceEmissions.setItems(FXCollections.observableArrayList("scope_1", "scope_2", "scope_3", "declaration", "mesure"));
    }

    private void populateForm(Projet p) {
        txtTitre.setText(safe(p.getTitre()));
        txtDescription.setText(safe(p.getDescription()));
        txtAddress.setText(safe(p.getCompanyAddress()));
        txtEmail.setText(safe(p.getCompanyEmail()));
        txtPhone.setText(safe(p.getCompanyPhone()));
        txtLocalisation.setText(safe(p.getLocalisation()));
        cmbSecteur.setValue(p.getSecteur());
        cmbTypeProjet.setValue(p.getTypeProjet());
        setDouble(txtEnergie, p.getConsommationEnergie());
        cmbUniteEnergie.setValue(p.getUniteEnergie());
        setDouble(txtTransport, p.getDistanceTransport());
        cmbTypeTransport.setValue(p.getTypeTransport());
        setDouble(txtMateriau, p.getQuantiteMateriau());
        cmbTypeMateriau.setValue(p.getTypeMateriau());
        setDouble(txtEau, p.getConsommationEau());
        setDouble(txtDechets, p.getDechetsGeneres());
        setDouble(txtEmissions, p.getEmissionsEstimees());
        cmbSourceEmissions.setValue(p.getSourceEmissions());
        setDouble(txtMontant, p.getMontantDemande());
        txtPitch.setText(safe(p.getDescriptionProjet()));
    }

    private Projet buildProjet() {
        Projet p = new Projet();
        p.setTitre(txtTitre.getText().trim());
        p.setDescription(txtDescription.getText().trim());
        p.setCompanyAddress(txtAddress.getText().trim());
        p.setCompanyEmail(txtEmail.getText().trim());
        p.setCompanyPhone(txtPhone.getText().trim());
        p.setLocalisation(txtLocalisation.getText().trim());
        p.setSecteur(cmbSecteur.getValue());
        p.setTypeProjet(cmbTypeProjet.getValue());
        p.setConsommationEnergie(parseDouble(txtEnergie));
        p.setUniteEnergie(cmbUniteEnergie.getValue());
        p.setDistanceTransport(parseDouble(txtTransport));
        p.setTypeTransport(cmbTypeTransport.getValue());
        p.setQuantiteMateriau(parseDouble(txtMateriau));
        p.setTypeMateriau(cmbTypeMateriau.getValue());
        p.setConsommationEau(parseDouble(txtEau));
        p.setDechetsGeneres(parseDouble(txtDechets));
        p.setEmissionsEstimees(parseDouble(txtEmissions));
        p.setSourceEmissions(cmbSourceEmissions.getValue());
        p.setMontantDemande(parseDouble(txtMontant));
        p.setDescriptionProjet(txtPitch.getText().trim());
        return p;
    }

    private void disableForm() {
        for (var node : List.of(txtTitre, txtDescription, txtAddress, txtEmail, txtPhone,
                txtLocalisation, txtEnergie, txtTransport, txtMateriau, txtEau, txtDechets,
                txtEmissions, txtMontant, txtPitch)) {
            node.setEditable(false);
        }
    }

    // ── AI Assistant ──────────────────────────────────────────────────────

    private String callOpenRouter(String question, String projectJson) {
        EnvLoader.load();
        String apiKey = EnvLoader.get("OPENROUTER_API_KEY", "");
        if (apiKey.isBlank()) return fallbackAiResponse();

        String systemMsg = "Tu es un assistant IA de creation de projet pour GreenLedger. "
            + "Tu aides un utilisateur a remplir un formulaire projet. "
            + "Reponds toujours en francais. Sois concret, court, utile et oriente action.";
        String userMsg = "Question: " + question + "\\n\\nChamps actuellement saisis:\\n" + projectJson;

        String body = "{\"model\":\"nvidia/nemotron-3-super-120b-a12b:free\","
            + "\"temperature\":0.4,\"max_tokens\":450,"
            + "\"messages\":["
            + "{\"role\":\"system\",\"content\":\"" + escapeJson(systemMsg) + "\"},"
            + "{\"role\":\"user\",\"content\":\"" + escapeJson(userMsg) + "\"}"
            + "]}";

        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15)).build();
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://openrouter.ai/api/v1/chat/completions"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("HTTP-Referer", "http://localhost:8000")
                .header("X-Title", "GreenLedger")
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                return extractContent(resp.body());
            }
        } catch (Exception e) {
            System.err.println("[PorteurForm] AI error: " + e.getMessage());
        }
        return fallbackAiResponse();
    }

    private String fallbackAiResponse() {
        List<String> missing = getMissingFields();
        if (missing.isEmpty()) return "Votre dossier est complet. Vous pouvez soumettre votre projet.";
        return "Champs manquants: " + String.join(", ", missing)
            + ".\n\nConseils:\n- Titre: soyez precis et descriptif (ex: 'Centrale solaire 500kW Sfax')\n"
            + "- Description: expliquez l'impact environnemental et les objectifs.";
    }

    private String buildProjectJson() {
        return "{\"titre\":\"" + escapeJson(txtTitre.getText()) + "\","
            + "\"secteur\":\"" + escapeJson(safe(cmbSecteur.getValue())) + "\","
            + "\"type_projet\":\"" + escapeJson(safe(cmbTypeProjet.getValue())) + "\","
            + "\"localisation\":\"" + escapeJson(txtLocalisation.getText()) + "\","
            + "\"completude\":" + computeCompletenessScore() + "}";
    }

    private String extractContent(String json) {
        int idx = json.indexOf("\"content\":\"");
        if (idx < 0) return null;
        int start = idx + 11;
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char n = json.charAt(i + 1);
                if (n == 'n') { sb.append('\n'); i++; }
                else if (n == '"') { sb.append('"'); i++; }
                else if (n == '\\') { sb.append('\\'); i++; }
                else sb.append(c);
            } else if (c == '"') break;
            else sb.append(c);
        }
        return sb.toString().trim();
    }

    // ── Utilities ─────────────────────────────────────────────────────────

    private boolean blank(TextInputControl f) {
        return f.getText() == null || f.getText().isBlank();
    }

    private Double parseDouble(TextField f) {
        if (blank(f)) return null;
        try { return Double.parseDouble(f.getText().trim()); } catch (Exception e) { return null; }
    }

    private void setDouble(TextField f, Double v) {
        f.setText(v != null ? String.format(Locale.ROOT, "%.4f", v) : "");
    }

    private String safe(String s) { return s != null ? s : ""; }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "").replace("\t", "\\t");
    }

    private void showError(String msg) {
        lblError.setText(msg);
    }

    private void navigate(String fxml) {
        try { org.GreenLedger.MainFX.setRoot(fxml); }
        catch (Exception e) { System.err.println("[PorteurForm] Nav: " + e.getMessage()); }
    }
}
