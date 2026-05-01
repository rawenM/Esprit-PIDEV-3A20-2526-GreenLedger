package Controllers;

import Models.*;
import Services.*;
import Utils.NavigationContext;
import Utils.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluation Form Controller
 * Route: GET+POST /expert/evaluations/{id}/edit
 *
 * Left panel: project snapshot (read-only)
 * Right panel: criteria scoring form + observations
 * Top bar: "Lancer l'analyse ML" + "Soumettre"
 *
 * On ML analysis: calls HybridMLService, populates left panel results
 * On submit: saves evaluation, transitions project status
 */
public class EvaluationFormController extends BaseController {

    // ── Top bar ──────────────────────────────────────────────────────────────
    @FXML private Label  lblUserName;
    @FXML private Label  lblTitle;
    @FXML private Label  lblDecisionBadge;
    @FXML private Label  lblScoreBig;
    @FXML private Label  lblScoreLabel;
    @FXML private Button btnAnalyse;

    // ── KPI mini-cards ────────────────────────────────────────────────────────
    @FXML private Label lblKpiProjet;
    @FXML private Label lblKpiProjetSub;
    @FXML private Label lblKpiEmissions;
    @FXML private Label lblKpiEmissionsSub;
    @FXML private Label lblKpiDecision;
    @FXML private Label lblKpiDecisionSub;
    @FXML private Label lblKpiRecos;
    @FXML private Label lblKpiRecosSub;
    @FXML private Label lblKpiFraud;
    @FXML private Label lblKpiFraudSub;

    // ── Recommendations grid ──────────────────────────────────────────────────
    @FXML private javafx.scene.layout.GridPane gridRecos;
    @FXML private Label lblRecoStatus;

    // ── Electricity Maps ──────────────────────────────────────────────────────
    @FXML private Label lblElectricityCountry;
    @FXML private Label lblMapInfo;
    @FXML private javafx.scene.web.WebView mapWebView;

    // ── Criteria form ─────────────────────────────────────────────────────────
    @FXML private VBox    boxCriteria;
    @FXML private TextArea txtObservations;

    // ── Services ──────────────────────────────────────────────────────────────
    private final ProjetService        projetService     = new ProjetService();
    private final EvaluationService    evaluationService = new EvaluationService();
    private final CritereImpactService critereService    = new CritereImpactService();
    private final ExpertWorkflowService workflowService  = new ExpertWorkflowService();
    private final ProjectStatusService statusService     = new ProjectStatusService();
    private final Services.ElectricityMapsService electricityMapsService = new Services.ElectricityMapsService();

    // ── State ─────────────────────────────────────────────────────────────────
    private Projet     currentProject;
    private Evaluation currentEvaluation;
    private Models.dto.AnalysisResult lastAnalysisResult; // from new pipeline

    // Per-criterion UI rows
    private final List<CriterionRow> criterionRows = new ArrayList<>();

    // ── Init ──────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        super.initialize();

        // Set user name
        var user = Utils.SessionManager.getInstance().getCurrentUser();
        if (user != null && lblUserName != null) lblUserName.setText(user.getNomComplet());

        Integer projectId    = NavigationContext.getInstance().getCurrentProjectId();
        Integer evaluationId = NavigationContext.getInstance().getCurrentEvaluationId();

        if (projectId == null) { showError("Aucun projet sélectionné"); return; }

        currentProject = projetService.getById(projectId);
        if (currentProject == null) { showError("Projet introuvable: " + projectId); return; }

        // Load evaluation: prefer the one from NavigationContext, otherwise fetch the latest from DB
        List<Evaluation> evals = evaluationService.afficherParProjet(projectId);
        if (evaluationId != null) {
            currentEvaluation = evals.stream()
                .filter(e -> e.getIdEvaluation() == evaluationId)
                .findFirst().orElse(null);
        }
        if (currentEvaluation == null && !evals.isEmpty()) {
            // Pick the latest evaluation (afficherParProjet already orders by date DESC)
            currentEvaluation = evals.get(0);
        }
        if (currentEvaluation == null) {
            currentEvaluation = new Evaluation();
            currentEvaluation.setIdProjet(projectId);
        }

        populateSnapshot();
        buildCriteriaForm();
    }

    @FXML private void onDashboard()    { navigate("fxml/evaluation_dashboard"); }
    @FXML private void onFraudInsights(){ navigate("fxml/expert_carbon_dashboard"); }
    @FXML private void onEditProfile()  { navigate("editProfile"); }
    @FXML private void onViewSummary()  { /* show summary dialog */ }
    @FXML private void onLogout() {
        Utils.SessionManager.getInstance().invalidate();
        navigate("fxml/login");
    }

    private void navigate(String fxml) {
        try { org.GreenLedger.MainFX.setRoot(fxml); }
        catch (Exception e) { System.err.println("[EvalForm] Nav: " + e.getMessage()); }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML
    private void onBack() {
        try { org.GreenLedger.MainFX.setRoot("fxml/expert_shell"); }
        catch (Exception e) { navigateBack(); }
    }

    // ── Snapshot population ───────────────────────────────────────────────────

    private void populateSnapshot() {
        String titre = currentProject.getTitre() != null ? currentProject.getTitre() : "—";
        if (lblTitle != null) lblTitle.setText("Évaluation ESG — " + titre);

        int evalId = currentEvaluation.getIdEvaluation();

        // ── Load ML data once from DB ─────────────────────────────────────
        String  mlDec       = null;
        Integer mlEsgScore  = null;
        Integer mlCred      = null;
        String  mlRisk      = null;
        if (evalId > 0) {
            try (java.sql.Connection conn = DataBase.MyConnection.getConnection();
                 java.sql.PreparedStatement ps = conn.prepareStatement(
                     "SELECT decision, predicted_esg_score, credibility_score, carbon_risk " +
                     "FROM ml_predictions WHERE project_id=? AND evaluation_id=? " +
                     "ORDER BY id DESC LIMIT 1")) {
                ps.setInt(1, currentProject.getId());
                ps.setInt(2, evalId);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        mlDec      = rs.getString("decision");
                        int s = rs.getInt("predicted_esg_score");
                        if (!rs.wasNull()) mlEsgScore = s;
                        int c = rs.getInt("credibility_score");
                        if (!rs.wasNull()) mlCred = c;
                        mlRisk = rs.getString("carbon_risk");
                    }
                }
            } catch (java.sql.SQLException ex) {
                System.err.println("[EvalForm] Failed to load ml_predictions: " + ex.getMessage());
            }
        }

        // ── Load carbon metric once from DB ───────────────────────────────
        double totalTco2 = 0.0, s1 = 0.0, s2 = 0.0, s3 = 0.0;
        if (evalId > 0) {
            try (java.sql.Connection conn = DataBase.MyConnection.getConnection();
                 java.sql.PreparedStatement ps = conn.prepareStatement(
                     "SELECT scope1_tco2, scope2_tco2, scope3_tco2, total_tco2 " +
                     "FROM carbon_metric WHERE project_id=? AND evaluation_id=? " +
                     "ORDER BY id DESC LIMIT 1")) {
                ps.setInt(1, currentProject.getId());
                ps.setInt(2, evalId);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        s1 = rs.getDouble("scope1_tco2");
                        s2 = rs.getDouble("scope2_tco2");
                        s3 = rs.getDouble("scope3_tco2");
                        totalTco2 = rs.getDouble("total_tco2");
                    }
                }
            } catch (java.sql.SQLException ex) {
                System.err.println("[EvalForm] Failed to load carbon_metric: " + ex.getMessage());
            }
        }

        // ── Resolve effective decision (ML > evaluation > "—") ────────────
        String dec = mlDec != null ? mlDec : currentEvaluation.getDecision();

        // ── Decision badge ────────────────────────────────────────────────
        if (lblDecisionBadge != null) {
            if (dec == null || dec.isBlank()) {
                lblDecisionBadge.setText("● EN COURS");
                lblDecisionBadge.setStyle("-fx-background-color:#FEF3C7;-fx-text-fill:#92400E;-fx-font-size:10px;-fx-font-weight:700;-fx-background-radius:4;-fx-padding:3 10;");
            } else if (dec.toUpperCase().contains("APPROV")) {
                lblDecisionBadge.setText("● APPROUVÉ");
                lblDecisionBadge.setStyle("-fx-background-color:#D1FAE5;-fx-text-fill:#065F46;-fx-font-size:10px;-fx-font-weight:700;-fx-background-radius:4;-fx-padding:3 10;");
            } else {
                lblDecisionBadge.setText("● REJETÉ");
                lblDecisionBadge.setStyle("-fx-background-color:#FEE2E2;-fx-text-fill:#991B1B;-fx-font-size:10px;-fx-font-weight:700;-fx-background-radius:4;-fx-padding:3 10;");
            }
        }

        // ── Score ESG badge (ML > evaluation.score_final > projet.score_esg) ──
        if (lblScoreBig != null) {
            double score = 0.0;
            if (mlEsgScore != null && mlEsgScore > 0) {
                score = mlEsgScore;
            } else if (currentEvaluation.getScoreGlobal() > 0) {
                score = currentEvaluation.getScoreGlobal();
            } else if (currentProject.getScoreEsg() != null && currentProject.getScoreEsg() > 0) {
                score = currentProject.getScoreEsg();
            }
            lblScoreBig.setText(score > 0 ? String.format("%.2f", score) : "—");
        }

        // ── KPI: PROJET ───────────────────────────────────────────────────
        if (lblKpiProjet != null) {
            lblKpiProjet.setText(titre.length() > 20 ? titre.substring(0, 20) + "…" : titre);
            if (lblKpiProjetSub != null)
                lblKpiProjetSub.setText("ID Évaluation " + (evalId > 0 ? evalId : "—"));
        }

        // ── KPI: ÉMISSIONS ────────────────────────────────────────────────
        if (lblKpiEmissions != null) {
            if (totalTco2 > 0) {
                lblKpiEmissions.setText(String.format("%.3f", totalTco2));
                if (lblKpiEmissionsSub != null)
                    lblKpiEmissionsSub.setText(String.format("Scope(1)=%.3f / Scope(2)=%.3f / Scope(3)=%.3f", s1, s2, s3));
            } else {
                Double declared = currentProject.getEmissionsEstimees();
                lblKpiEmissions.setText(declared != null ? String.format("%.3f", declared) : "—");
                if (lblKpiEmissionsSub != null)
                    lblKpiEmissionsSub.setText(declared != null ? "Scope (1) / Scope (2) / Scope (3)" : "");
            }
        }

        // ── KPI: DÉCISION ML ─────────────────────────────────────────────
        if (lblKpiDecision != null) {
            String displayDec = dec != null ? dec : "—";
            boolean approved = displayDec.toUpperCase().contains("APPROV");
            lblKpiDecision.setText(displayDec);
            lblKpiDecision.setStyle("-fx-font-size:13px;-fx-font-weight:800;-fx-text-fill:"
                + (approved ? "#065F46" : "#991B1B") + ";");
            if (lblKpiDecisionSub != null) {
                lblKpiDecisionSub.setText(String.format("ESG: %s | Crédibilité: %s%% | Risque: %s",
                    mlEsgScore  != null ? mlEsgScore  : "—",
                    mlCred      != null ? mlCred      : "—",
                    mlRisk      != null ? mlRisk      : "—"));
            }
        }

        // ── KPI: RECOMMANDATIONS ─────────────────────────────────────────
        if (lblKpiRecos != null) {
            String recoText = null;
            if (evalId > 0) {
                try (java.sql.Connection conn = DataBase.MyConnection.getConnection();
                     java.sql.PreparedStatement ps = conn.prepareStatement(
                         "SELECT recommendations FROM ml_predictions " +
                         "WHERE project_id=? AND evaluation_id=? ORDER BY id DESC LIMIT 1")) {
                    ps.setInt(1, currentProject.getId());
                    ps.setInt(2, evalId);
                    try (java.sql.ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) recoText = rs.getString(1);
                    }
                } catch (java.sql.SQLException ex) {
                    System.err.println("[EvalForm] Failed to load recommendations: " + ex.getMessage());
                }
            }
            if (recoText != null && !recoText.isBlank()) {
                String[] parts = recoText.split("\\s*\\|\\s*");
                lblKpiRecos.setText(parts.length + " actions");
                if (lblKpiRecosSub != null && parts.length > 0)
                    lblKpiRecosSub.setText(parts[0].trim());
                // Populate recommendations grid if available
                if (gridRecos != null) {
                    gridRecos.getChildren().clear();
                    int col = 0, row = 0;
                    for (String reco : parts) {
                        reco = reco.trim();
                        if (reco.isEmpty()) continue;
                        VBox card = buildRecoCard(reco, "IA", "#EFF6FF", "#2563EB");
                        gridRecos.add(card, col, row);
                        col++;
                        if (col >= 2) { col = 0; row++; }
                    }
                    if (lblRecoStatus != null) {
                        lblRecoStatus.setText(parts.length + " actions");
                        lblRecoStatus.setStyle("-fx-background-color:#FEF3C7;-fx-text-fill:#92400E;" +
                            "-fx-font-size:10px;-fx-font-weight:700;-fx-background-radius:4;-fx-padding:2 8;");
                    }
                }
            } else {
                lblKpiRecos.setText("—");
                if (lblKpiRecosSub != null) lblKpiRecosSub.setText("Action recommandée");
            }
        }

        // ── KPI: FRAUDE ───────────────────────────────────────────────────
        Double fraudRisk = currentProject.getFraudRiskScore();
        if (lblKpiFraud != null) {
            if (fraudRisk != null) {
                lblKpiFraud.setText(String.format("%.4f", fraudRisk));
                String fc = fraudRisk >= 0.65 ? "#991B1B" : fraudRisk >= 0.35 ? "#92400E" : "#065F46";
                lblKpiFraud.setStyle("-fx-font-size:13px;-fx-font-weight:800;-fx-text-fill:" + fc + ";");
                if (lblKpiFraudSub != null)
                    lblKpiFraudSub.setText(fraudRisk >= 0.65 ? "Suspect" : fraudRisk >= 0.35 ? "À vérifier" : "OK");
            } else {
                lblKpiFraud.setText("—");
            }
        }

        // ── Electricity Maps country ──────────────────────────────────────
        String loc = currentProject.getLocalisation();
        if (lblElectricityCountry != null)
            lblElectricityCountry.setText(loc != null ? "Projet : " + loc : "Projet : TN");
        if (lblMapInfo != null)
            lblMapInfo.setText(loc != null ? "Localisation: " + loc : "Localisation du projet");

        // Load real carbon intensity data asynchronously
        loadElectricityData(loc);
    }

    // ── Electricity Maps loader ───────────────────────────────────────────────

    private void loadElectricityData(String localisation) {
        String loc = (localisation != null && !localisation.isBlank()) ? localisation : "TN";
        String zone = electricityMapsService.resolveZone(loc);

        // Country badge
        if (lblElectricityCountry != null)
            lblElectricityCountry.setText("Projet • " + zone);
        if (lblMapInfo != null)
            lblMapInfo.setText("Contexte électrique comparatif pour le projet");

        if (mapWebView == null) return;

        // Load the HTML map page
        javafx.scene.web.WebEngine engine = mapWebView.getEngine();
        engine.setJavaScriptEnabled(true);

        String htmlUrl = getClass().getResource("/html/electricity_map.html").toExternalForm();
        engine.load(htmlUrl);

        // Once the page is ready, fetch data and inject it
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                // Set project zone immediately so the marker is highlighted
                engine.executeScript("setProjectZone('" + zone + "')");

                // Fetch all zone data on a background thread
                new Thread(() -> {
                    String zonesJson = buildZonesJson(zone);
                    javafx.application.Platform.runLater(() -> {
                        try {
                            // Escape for JS string injection
                            String escaped = zonesJson.replace("\\", "\\\\").replace("'", "\\'");
                            engine.executeScript("loadData('" + escaped + "', '" + zone + "')");
                            if (lblElectricityCountry != null)
                                lblElectricityCountry.setText("Projet • " + zone);
                        } catch (Exception e) {
                            System.err.println("[ElectricityMaps] JS inject failed: " + e.getMessage());
                        }
                    });
                }, "electricity-maps-loader").start();
            }
        });
    }

    /**
     * Fetch carbon intensity for all display zones and build a JSON array.
     * Runs on a background thread.
     */
    private String buildZonesJson(String projectZone) {
        // Zones to display in the table (project zone always first)
        String[] zones = {"TN","FR","DE","ES","IT","MA","DZ","EG","GB","PT","NL","BE","PL","SE","NO","CH","AT","GR","TR","SA","AE","IN","CN","JP","AU","US-CAL-CISO","BR","CA-ON"};

        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (String z : zones) {
            Services.ElectricityMapsService.CarbonIntensityResult r =
                electricityMapsService.getCarbonIntensity(z);
            if (!first) sb.append(",");
            first = false;
            sb.append("{\"zone\":\"").append(z).append("\"");
            if (r != null) {
                sb.append(",\"intensity\":").append(String.format(java.util.Locale.ROOT, "%.1f", r.carbonIntensity));
                if (r.fossilFuelPct != null && !r.fossilFuelPct.isBlank())
                    sb.append(",\"load\":\"").append(r.fossilFuelPct).append("\"");
            } else {
                sb.append(",\"intensity\":null");
            }
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    // ── Criteria form ─────────────────────────────────────────────────────────

    private void buildCriteriaForm() {
        boxCriteria.getChildren().clear();
        criterionRows.clear();

        List<CritereReference> criteres = critereService.afficherReferences();
        if (criteres.isEmpty()) {
            Label empty = new Label("Aucun critère défini. Ajoutez des critères dans le module d'audit.");
            empty.setStyle("-fx-font-size:12px;-fx-text-fill:#9CA3AF;-fx-padding:12;");
            boxCriteria.getChildren().add(empty);
            return;
        }

        for (CritereReference critere : criteres) {
            VBox row = buildCriterionRow(critere);
            boxCriteria.getChildren().add(row);
        }
    }

    private VBox buildCriterionRow(CritereReference critere) {
        VBox container = new VBox(6);
        container.setStyle("-fx-background-color:#F9FAFB;-fx-border-color:#E5E7EB;-fx-border-width:1;-fx-border-radius:8;-fx-background-radius:8;-fx-padding:12;");

        // Header: name + weight badge
        HBox header = new HBox(8);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label name = new Label(critere.getNomCritere() != null ? critere.getNomCritere() : "Critère");
        name.setStyle("-fx-font-size:13px;-fx-font-weight:700;-fx-text-fill:#1A2E26;");
        Label weight = new Label("Poids: " + critere.getPoids());
        weight.setStyle("-fx-font-size:9px;-fx-font-weight:700;-fx-text-fill:#2D5F3F;-fx-background-color:#D1FAE5;-fx-background-radius:4;-fx-padding:2 6;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(name, spacer, weight);

        // Description
        if (critere.getDescription() != null && !critere.getDescription().isBlank()) {
            Label desc = new Label(critere.getDescription());
            desc.setStyle("-fx-font-size:11px;-fx-text-fill:#6B7F77;");
            desc.setWrapText(true);
            container.getChildren().addAll(header, desc);
        } else {
            container.getChildren().add(header);
        }

        // Note slider + field
        HBox noteRow = new HBox(10);
        noteRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label noteLabel = new Label("Note (0-10):");
        noteLabel.setStyle("-fx-font-size:11px;-fx-text-fill:#6B7F77;-fx-min-width:80;");
        Slider slider = new Slider(0, 10, 5);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(2);
        slider.setSnapToTicks(true);
        HBox.setHgrow(slider, Priority.ALWAYS);
        TextField noteField = new TextField("5");
        noteField.setPrefWidth(50);
        noteField.setStyle("-fx-font-size:13px;-fx-font-weight:700;-fx-text-fill:#2D5F3F;-fx-border-color:#E0E5E3;-fx-border-radius:6;-fx-background-radius:6;-fx-padding:5 8;");

        // Sync slider ↔ field
        slider.valueProperty().addListener((obs, o, n) -> {
            int v = n.intValue();
            noteField.setText(String.valueOf(v));
            updateNoteColor(noteField, v);
        });
        noteField.textProperty().addListener((obs, o, n) -> {
            try {
                int v = Integer.parseInt(n.trim());
                v = Math.max(0, Math.min(10, v));
                slider.setValue(v);
                updateNoteColor(noteField, v);
            } catch (NumberFormatException ignored) {}
        });
        updateNoteColor(noteField, 5);

        noteRow.getChildren().addAll(noteLabel, slider, noteField);

        // Comment field
        TextField commentField = new TextField();
        commentField.setPromptText("Commentaire sur ce critère (optionnel)…");
        commentField.setStyle("-fx-font-size:12px;-fx-border-color:#E0E5E3;-fx-border-radius:6;-fx-background-radius:6;-fx-padding:7 10;");

        container.getChildren().addAll(noteRow, commentField);

        criterionRows.add(new CriterionRow(critere.getIdCritere(), noteField, commentField));
        return container;
    }

    private void updateNoteColor(TextField field, int note) {
        String color = note >= 7 ? "#065F46" : note >= 5 ? "#92400E" : "#991B1B";
        String bg    = note >= 7 ? "#D1FAE5" : note >= 5 ? "#FEF3C7" : "#FEE2E2";
        field.setStyle("-fx-font-size:13px;-fx-font-weight:700;-fx-text-fill:" + color
            + ";-fx-background-color:" + bg
            + ";-fx-border-color:#E0E5E3;-fx-border-radius:6;-fx-background-radius:6;-fx-padding:5 8;");
    }

    // ── ML Analysis ───────────────────────────────────────────────────────────

    @FXML
    private void onRunMl() {
        if (currentProject == null) return;

        btnAnalyse.setDisable(true);
        btnAnalyse.setText("⏳ Analyse en cours…");

        int projectId = currentProject.getId();
        int expertId  = SessionManager.getInstance().getCurrentUser() != null
            ? SessionManager.getInstance().getCurrentUser().getId().intValue() : 0;

        new Thread(() -> {
            try {
                // Use the new ExpertWorkflowService (Climatiq + ML API + green credits)
                Models.dto.AnalysisResult result = workflowService.runAnalysis(projectId, expertId);
                lastAnalysisResult = result;

                Platform.runLater(() -> {
                    populateMlResults(result);
                    btnAnalyse.setDisable(false);
                    btnAnalyse.setText("⚡ Relancer l'analyse ML");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnAnalyse.setDisable(false);
                    btnAnalyse.setText("⚡ Analyser API + ML");
                    new Alert(Alert.AlertType.ERROR,
                        "Analyse échouée: " + e.getMessage(), ButtonType.OK).showAndWait();
                });
            }
        }).start();
    }

    private void populateMlResults(Models.dto.AnalysisResult result) {
        Models.dto.MlPredictionResult    ml     = result.getMlPrediction();
        Models.dto.FraudAssessmentResult fraud  = result.getFraudAssessment();
        Models.dto.GreenCreditResult     cred   = result.getGreenCreditDispatch();
        Models.dto.CarbonMetricResult    carbon = result.getCarbonMetric();

        // ── Score badge (dark box, top-right) ────────────────────────────
        if (lblScoreBig != null && ml.getPredictedEsgScore() != null) {
            lblScoreBig.setText(String.valueOf(ml.getPredictedEsgScore()));
        }

        // ── Decision badge (top-left, colored) ───────────────────────────
        if (lblDecisionBadge != null) {
            String dec = ml.getDecision() != null ? ml.getDecision() : "—";
            boolean approved = dec.startsWith("APPROV");
            boolean revision = dec.contains("REVISION");
            String bg    = approved ? "#D1FAE5" : revision ? "#FEF3C7" : "#FEE2E2";
            String color = approved ? "#065F46" : revision ? "#92400E" : "#991B1B";
            String text  = approved ? "● APPROUVÉ" : revision ? "● RÉVISION" : "● REJETÉ";
            lblDecisionBadge.setText(text);
            lblDecisionBadge.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + color
                + ";-fx-font-size:10px;-fx-font-weight:700;-fx-background-radius:4;-fx-padding:3 10;");
        }

        // ── KPI: PROJET ───────────────────────────────────────────────────
        if (lblKpiProjet != null) {
            String titre = currentProject.getTitre() != null ? currentProject.getTitre() : "—";
            lblKpiProjet.setText(titre.length() > 22 ? titre.substring(0, 22) + "…" : titre);
            if (lblKpiProjetSub != null) {
                lblKpiProjetSub.setText(String.format("Crédits: %.3f | Évité: %.3f tCO2e",
                    cred != null ? orZero(cred.getDispatchedCredits()) : 0.0,
                    cred != null ? orZero(cred.getAvoidedTco2()) : 0.0));
            }
        }

        // ── KPI: ÉMISSIONS TOTALES ────────────────────────────────────────
        if (lblKpiEmissions != null && carbon != null) {
            double total = orZero(carbon.getTotalTco2());
            lblKpiEmissions.setText(String.format("%.3f", total));
            if (lblKpiEmissionsSub != null) {
                lblKpiEmissionsSub.setText(String.format("Scope(1)=%.3f / Scope(2)=%.3f / Scope(3)=%.3f",
                    orZero(carbon.getScope1Tco2()),
                    orZero(carbon.getScope2Tco2()),
                    orZero(carbon.getScope3Tco2())));
            }
        }

        // ── KPI: DÉCISION ML ─────────────────────────────────────────────
        if (lblKpiDecision != null) {
            String dec = ml.getDecision() != null ? ml.getDecision() : "—";
            boolean approved = dec.startsWith("APPROV");
            boolean revision = dec.contains("REVISION");
            String color = approved ? "#059669" : revision ? "#D97706" : "#DC2626";
            lblKpiDecision.setText(dec);
            lblKpiDecision.setStyle("-fx-font-size:16px;-fx-font-weight:800;-fx-text-fill:" + color + ";");
            if (lblKpiDecisionSub != null) {
                lblKpiDecisionSub.setText(String.format("ESG: %d | Crédibilité: %d%% | Risque: %s",
                    ml.getPredictedEsgScore() != null ? ml.getPredictedEsgScore() : 0,
                    ml.getCredibilityScore()  != null ? ml.getCredibilityScore()  : 0,
                    ml.getCarbonRisk() != null ? ml.getCarbonRisk() : "—"));
            }
        }

        // ── KPI: RECOMMANDATIONS ─────────────────────────────────────────
        if (lblKpiRecos != null && ml.getRecommendations() != null) {
            String[] recoArr = ml.getRecommendations().split("\\s*\\|\\s*");
            int count = (int) java.util.Arrays.stream(recoArr)
                .filter(s -> !s.isBlank()).count();
            lblKpiRecos.setText(count + " actions");
            if (lblKpiRecosSub != null && count > 0) {
                lblKpiRecosSub.setText(recoArr[0].trim());
            }
        }

        // ── KPI: FRAUDE DÉTECT. ───────────────────────────────────────────
        if (lblKpiFraud != null && fraud != null) {
            double risk = orZero(fraud.getFraudRiskScore());
            lblKpiFraud.setText(String.format("%.4f", risk));
            String fc = fraud.isFraudFlag() ? "#DC2626" : risk >= 0.35 ? "#D97706" : "#059669";
            lblKpiFraud.setStyle("-fx-font-size:16px;-fx-font-weight:800;-fx-text-fill:" + fc + ";");
            if (lblKpiFraudSub != null) {
                lblKpiFraudSub.setText(fraud.isFraudFlag() ? "Suspect" : risk >= 0.35 ? "À vérifier" : "OK");
            }
        }

        // ── Recommendations grid (2-column, colored cards) ───────────────
        if (gridRecos != null && ml.getRecommendations() != null) {
            gridRecos.getChildren().clear();
            String[] recos = ml.getRecommendations().split("\\s*\\|\\s*");
            int col = 0, row = 0;
            for (String reco : recos) {
                reco = reco.trim();
                if (reco.isEmpty()) continue;

                // Determine card type
                boolean isCritique = fraud != null && fraud.isFraudFlag() && col == 0 && row == 0;
                String tag, bg, tagBg;
                if (isCritique) {
                    tag = "Critique"; bg = "#FEF2F2"; tagBg = "#DC2626";
                } else {
                    tag = "IA"; bg = "#EFF6FF"; tagBg = "#2563EB";
                }

                VBox card = buildRecoCard(reco, tag, bg, tagBg);
                gridRecos.add(card, col, row);

                col++;
                if (col >= 2) { col = 0; row++; }
            }

            // Update status badge
            int total = (int) java.util.Arrays.stream(recos).filter(s -> !s.isBlank()).count();
            if (lblRecoStatus != null) {
                lblRecoStatus.setText(total + " actions");
                lblRecoStatus.setStyle("-fx-background-color:#FEF3C7;-fx-text-fill:#92400E;" +
                    "-fx-font-size:10px;-fx-font-weight:700;-fx-background-radius:4;-fx-padding:2 8;");
            }
        }

        // ── Electricity Maps country ──────────────────────────────────────
        if (lblElectricityCountry != null) {
            String loc = currentProject.getLocalisation();
            lblElectricityCountry.setText(loc != null && !loc.isBlank()
                ? "Projet • " + loc : "Projet • TN");
        }
        if (lblMapInfo != null) {
            String loc = currentProject.getLocalisation();
            lblMapInfo.setText(loc != null ? "Localisation: " + loc : "Localisation du projet");
        }
    }

    private VBox buildRecoCard(String text, String tag, String bg, String tagBg) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color:" + bg
            + ";-fx-border-color:rgba(0,0,0,0.06);-fx-border-width:1;"
            + "-fx-border-radius:8;-fx-background-radius:8;-fx-padding:14 16;"
            + "-fx-min-width:240;-fx-pref-width:260;");

        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size:12px;-fx-text-fill:#1F2937;-fx-font-weight:500;");
        lbl.setWrapText(true);

        Label tagLbl = new Label(tag);
        tagLbl.setStyle("-fx-background-color:" + tagBg
            + ";-fx-text-fill:white;-fx-font-size:8px;-fx-font-weight:700;"
            + "-fx-background-radius:3;-fx-padding:2 7;");

        HBox tagRow = new HBox();
        tagRow.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        tagRow.getChildren().add(tagLbl);

        card.getChildren().addAll(lbl, tagRow);
        return card;
    }

    @FXML
    private void onSubmit() {
        List<EvaluationResult> resultats = new ArrayList<>();
        for (CriterionRow row : criterionRows) {
            int note = 5;
            try { note = Integer.parseInt(row.noteField.getText().trim()); }
            catch (NumberFormatException ignored) {}
            note = Math.max(0, Math.min(10, note));

            EvaluationResult r = new EvaluationResult();
            r.setIdCritere(row.critereId);
            r.setNote(note);
            r.setCommentaireExpert(row.commentField.getText());
            r.setEstRespecte(note >= 5);
            resultats.add(r);
        }

        double avg = resultats.stream().mapToInt(EvaluationResult::getNote).average().orElse(5.0);
        int    finalScore = (int) Math.round(avg);
        String decision;

        if (lastAnalysisResult != null) {
            Models.dto.MlPredictionResult    ml    = lastAnalysisResult.getMlPrediction();
            Models.dto.FraudAssessmentResult fraud = lastAnalysisResult.getFraudAssessment();

            finalScore = ml.getPredictedEsgScore() != null ? ml.getPredictedEsgScore() : finalScore;
            String mlDec = ml.getDecision() != null ? ml.getDecision() : "REJECTED";

            // Fraud check before approval (exact Symfony rule)
            if ("APPROVED".equals(mlDec)) {
                double fraudRisk = fraud.getFraudRiskScore() != null ? fraud.getFraudRiskScore() : 0.0;
                boolean fraudFlag = fraud.isFraudFlag();
                if ((fraudFlag && fraudRisk >= 0.55) || fraudRisk >= 0.70) {
                    new Alert(Alert.AlertType.WARNING,
                        String.format("Validation bloquée: risque de fraude élevé (%.0f%%).", fraudRisk * 100),
                        ButtonType.OK).showAndWait();
                    return;
                }
            }
            decision = mlDec.startsWith("APPROV") ? "APPROVED" : "REJECTED";
        } else {
            decision = finalScore >= 7 ? "APPROVED" : finalScore >= 5 ? "REVISION_REQUIRED" : "REJECTED";
        }

        currentEvaluation.setScoreGlobal(finalScore);
        currentEvaluation.setDecision(decision);
        currentEvaluation.setObservations(txtObservations.getText());

        try {
            if (currentEvaluation.getIdEvaluation() == 0) {
                evaluationService.ajouterAvecCriteres(currentEvaluation, resultats);
            } else {
                evaluationService.modifier(currentEvaluation);
            }

            // Update project score + status
            currentProject.setScoreEsg(finalScore);
            projetService.updateStatut(currentProject.getId(),
                decision.startsWith("APPROV") ? "APPROVED" : "REJECTED");

            String msg = "APPROVED".equals(decision)
                ? "✅ Projet APPROUVÉ — Score ESG: " + finalScore + "/10"
                : "❌ Projet REJETÉ — Score ESG: " + finalScore + "/10";

            new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK)
                .showAndWait();

            org.GreenLedger.MainFX.setRoot("fxml/expert_shell");

        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR,
                "Erreur lors de la soumission: " + e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    private double orZero(Double v) { return v != null ? v : 0.0; }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String fmt(Double value, String unit) {
        if (value == null || value == 0) return "—";
        return String.format("%.2f %s", value, unit != null ? unit : "");
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    // ── Inner class ───────────────────────────────────────────────────────────

    private static class CriterionRow {
        final int critereId;
        final TextField noteField;
        final TextField commentField;

        CriterionRow(int critereId, TextField noteField, TextField commentField) {
            this.critereId    = critereId;
            this.noteField    = noteField;
            this.commentField = commentField;
        }
    }
}
