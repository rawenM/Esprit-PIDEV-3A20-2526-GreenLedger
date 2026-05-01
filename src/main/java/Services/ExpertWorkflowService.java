package Services;

import DataBase.MyConnection;
import Models.*;
import Models.dto.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * Expert Workflow Service — main ML pipeline orchestrator.
 *
 * Pipeline:
 *   1. Extract ProjectData from Projet entity
 *   2. Climatiq API → carbon metrics (scope1/2/3) with fallback heuristics
 *   3. ML API → ESG score + fraud assessment (with heuristic fallback)
 *   4. Persist results to DB (carbon_metric, ml_predictions, ml_fraud_assessment)
 *   5. Green credit dispatch (eligibility + calculation)
 *   6. Update projet table (fraud fields, green credit fields)
 *   7. Return AnalysisResult
 */
public class ExpertWorkflowService {

    // Fallback emission factors (match the original PHP logic and units)
    // transport: tCO2e/km, waste: tCO2e/kg, energy: tCO2e/kWh, material: tCO2e/kg
    private static final double FACTOR_TRANSPORT = 0.00008;
    private static final double FACTOR_WASTE     = 0.00070;
    private static final double FACTOR_ENERGY    = 0.00045;
    private static final double FACTOR_MATERIAL  = 0.00120;

    private final ProjetService           projetService;
    private final ClimatiqApiClient       climatiqClient;
    private final MlApiClient             mlApiClient;
    private final BaselineEmissionService baselineService;
    private final EligibilityCheckService eligibilityService;
    private final AiSuggestionService     aiSuggestionService;

    public ExpertWorkflowService() {
        this.projetService      = new ProjetService();
        this.climatiqClient     = new ClimatiqApiClient();
        this.mlApiClient        = new MlApiClient();
        this.baselineService    = new BaselineEmissionService();
        this.eligibilityService = new EligibilityCheckService();
        this.aiSuggestionService = new AiSuggestionService(
            new ScoringService(), new Services.impl.SimplePolicyEngine());
    }

    // ══════════════════════════════════════════════════════════════════════
    // MAIN ENTRY POINT
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Run the full ML analysis pipeline for a project.
     *
     * @param projectId  ID of the project to analyze
     * @param expertId   ID of the expert running the analysis
     * @return AnalysisResult with all computed values
     */
    public AnalysisResult runAnalysis(int projectId, int expertId) {
        System.out.println("[ExpertWorkflow] Starting analysis for project " + projectId);

        Projet project = projetService.getById(projectId);
        if (project == null) throw new IllegalArgumentException("Project not found: " + projectId);

        // ── Rule 5: SUBMITTED → IN_PROGRESS before analysis ──────────────
        transitionToInProgress(projectId);

        ProjectData data = ProjectData.from(project);

        // ── Rule 9: Get or create evaluation row — evaluationId MUST NOT be null ──
        long evaluationId = getOrCreateEvaluation(projectId);
        System.out.println("[ExpertWorkflow] Using evaluationId=" + evaluationId);

        // ── Step 1: Carbon metrics (Rule 1 + Rule 6) ─────────────────────
        CarbonMetricResult carbonMetric = estimateAndStoreCarbonMetric(projectId, evaluationId, data);
        System.out.printf("[ExpertWorkflow] Carbon: scope1=%.3f scope2=%.3f scope3=%.3f total=%.3f tCO2e%n",
            orZero(carbonMetric.getScope1Tco2()),
            orZero(carbonMetric.getScope2Tco2()),
            orZero(carbonMetric.getScope3Tco2()),
            orZero(carbonMetric.getTotalTco2()));

        // ── Step 2: Set total_tco2 for ML input ───────────────────────────
        data.setTotalTco2(carbonMetric.getTotalTco2());

        // ── Step 3: ML prediction + fraud ─────────────────────────────────
        MlPredictionResult    mlPrediction;
        FraudAssessmentResult fraudAssessment;

        boolean pythonAvailable = mlApiClient.isHealthy();
        if (pythonAvailable) {
            try {
                BothPredictionResult both = mlApiClient.predictBoth(data);
                mlPrediction    = both.getEsg();
                fraudAssessment = both.getFraud();
                System.out.println("[ExpertWorkflow] ML from Python API");
            } catch (Exception e) {
                System.err.println("[ExpertWorkflow] ML API failed, using heuristics: " + e.getMessage());
                mlPrediction    = heuristicMlPrediction(carbonMetric);
                fraudAssessment = heuristicFraudAssessment(data, carbonMetric);
            }
        } else {
            System.out.println("[ExpertWorkflow] ML API unavailable, using heuristics");
            mlPrediction    = heuristicMlPrediction(carbonMetric);
            fraudAssessment = heuristicFraudAssessment(data, carbonMetric);
        }

        System.out.printf("[ExpertWorkflow] ML: ESG=%d credibility=%d risk=%s decision=%s%n",
            mlPrediction.getPredictedEsgScore(),
            mlPrediction.getCredibilityScore(),
            mlPrediction.getCarbonRisk(),
            mlPrediction.getDecision());

        System.out.printf("[ExpertWorkflow] Fraud: risk=%.4f anomaly=%.4f flag=%b%n",
            orZero(fraudAssessment.getFraudRiskScore()),
            orZero(fraudAssessment.getFraudAnomalyScore()),
            fraudAssessment.isFraudFlag());

        // ── Step 4: Persist ML results (Rules 1-4, exact order) ──────────
        // carbon_metric already saved above (step 1)

        // Enrich recommendations via OpenRouter AI (async-safe: runs on this thread before persist)
        enrichRecommendations(mlPrediction, project, fraudAssessment, carbonMetric);

        storeMlPrediction(projectId, evaluationId, mlPrediction, expertId);           // Rule 2 step 2
        storeMlDecisionSnapshot(projectId, evaluationId, mlPrediction, project.getTitre()); // Rule 2 step 3
        storeFraudAssessment(projectId, evaluationId, fraudAssessment);               // Rule 2 step 4
        updateEvaluation(evaluationId, mlPrediction);                                 // Rule 2 step 5 + Rule 3
        updateProjectFraudAndStatus(projectId, mlPrediction, fraudAssessment, project); // Rule 2 step 6 + Rule 4

        // ── Step 5: Green credit dispatch ─────────────────────────────────
        GreenCreditResult greenCredit = computeGreenCredits(
            project, carbonMetric, fraudAssessment, mlPrediction);

        updateProjectGreenCreditFields(projectId, greenCredit, carbonMetric);

        System.out.printf("[ExpertWorkflow] Credits: eligible=%b avoided=%.3f credits=%.3f%n",
            greenCredit.isEligible(),
            orZero(greenCredit.getAvoidedTco2()),
            orZero(greenCredit.getDispatchedCredits()));

        // ── Step 6: Build result ──────────────────────────────────────────
        AnalysisResult result = new AnalysisResult();
        result.setCarbonMetric(carbonMetric);
        result.setMlPrediction(mlPrediction);
        result.setFraudAssessment(fraudAssessment);
        result.setGreenCreditDispatch(greenCredit);

        System.out.println("[ExpertWorkflow] Analysis complete: decision=" + result.getDecision());
        return result;
    }

    // ══════════════════════════════════════════════════════════════════════
    // RULE 9: GET OR CREATE EVALUATION
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Returns the existing evaluation_id for the project, or creates a new
     * evaluation row and returns its generated id.  NEVER returns null.
     */
    public long getOrCreateEvaluation(int projectId) {
        // Try to find an existing evaluation
        String selectSql = "SELECT id_evaluation FROM evaluation " +
                           "WHERE id_projet = ? ORDER BY id_evaluation DESC LIMIT 1";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(selectSql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    if (!rs.wasNull()) {
                        System.out.println("[ExpertWorkflow] Found existing evaluation id=" + id);
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[ExpertWorkflow] getOrCreateEvaluation select failed: " + e.getMessage());
        }

        // No existing evaluation — create one
        String insertSql = "INSERT INTO evaluation (id_projet, date_evaluation) VALUES (?, NOW())";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, projectId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long newId = keys.getLong(1);
                    System.out.println("[ExpertWorkflow] Created new evaluation id=" + newId);
                    return newId;
                }
            }
        } catch (SQLException e) {
            System.err.println("[ExpertWorkflow] getOrCreateEvaluation insert failed: " + e.getMessage());
        }

        throw new IllegalStateException("Could not get or create evaluation for project " + projectId);
    }

    // ══════════════════════════════════════════════════════════════════════
    // RULE 5: SUBMITTED → IN_PROGRESS
    // ══════════════════════════════════════════════════════════════════════

    private void transitionToInProgress(int projectId) {
        String sql = "UPDATE projet SET statut = 'IN_PROGRESS' WHERE id = ? AND statut = 'SUBMITTED'";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            int rows = ps.executeUpdate();
            if (rows > 0) System.out.println("[ExpertWorkflow] Project " + projectId + " → IN_PROGRESS");
        } catch (SQLException e) {
            System.err.println("[ExpertWorkflow] transitionToInProgress failed: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // STEP 1: CARBON METRIC  (Rules 1, 6, 10)
    // ══════════════════════════════════════════════════════════════════════

    private CarbonMetricResult estimateAndStoreCarbonMetric(int projectId, long evaluationId, ProjectData data) {
        // Try Climatiq API (Rule 6: units are normalised inside ClimatiqApiClient)
        CarbonMetricResult result;
        try {
            result = climatiqClient.estimateProjectEmissions(data);
        } catch (Exception e) {
            System.err.println("[ExpertWorkflow] Climatiq failed: " + e.getMessage());
            result = fallbackCarbonMetric(data);
        }

        // Compute data quality score
        double quality = computeDataQualityScore(data, result.getProviderErrors());
        result.setDataQualityScore(quality);

        // Rule 10: check for existing row with this evaluation_id
        boolean exists = carbonMetricExists(projectId, evaluationId);

        if (exists) {
            // UPDATE instead of INSERT
            String sql = "UPDATE carbon_metric SET " +
                         "metric_date=CURDATE(), scope1_tco2=?, scope2_tco2=?, scope3_tco2=?, " +
                         "total_tco2=?, method=?, data_quality_score=? " +
                         "WHERE project_id=? AND evaluation_id=?";
            try (Connection conn = MyConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                setDoubleOrNull(ps, 1, result.getScope1Tco2());
                setDoubleOrNull(ps, 2, result.getScope2Tco2());
                setDoubleOrNull(ps, 3, result.getScope3Tco2());
                setDoubleOrNull(ps, 4, result.getTotalTco2());
                ps.setString(5, result.getMethod());
                setDoubleOrNull(ps, 6, result.getDataQualityScore());
                ps.setInt(7, projectId);
                ps.setLong(8, evaluationId);
                ps.executeUpdate();
                System.out.println("[ExpertWorkflow] Updated existing carbon_metric for evaluationId=" + evaluationId);
            } catch (SQLException e) {
                System.err.println("[ExpertWorkflow] Failed to update carbon_metric: " + e.getMessage());
            }
        } else {
            // INSERT with evaluation_id (Rule 1 — MUST NOT be null)
            String sql = "INSERT INTO carbon_metric " +
                         "(project_id, evaluation_id, metric_date, scope1_tco2, scope2_tco2, scope3_tco2, " +
                         " total_tco2, method, data_quality_score) " +
                         "VALUES (?, ?, CURDATE(), ?, ?, ?, ?, ?, ?)";
            try (Connection conn = MyConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, projectId);
                ps.setLong(2, evaluationId);                   // ← NEVER NULL
                setDoubleOrNull(ps, 3, result.getScope1Tco2());
                setDoubleOrNull(ps, 4, result.getScope2Tco2());
                setDoubleOrNull(ps, 5, result.getScope3Tco2());
                setDoubleOrNull(ps, 6, result.getTotalTco2());
                ps.setString(7, result.getMethod());
                setDoubleOrNull(ps, 8, result.getDataQualityScore());
                ps.executeUpdate();
                System.out.println("[ExpertWorkflow] Inserted carbon_metric with evaluationId=" + evaluationId);
            } catch (SQLException e) {
                System.err.println("[ExpertWorkflow] Failed to store carbon_metric: " + e.getMessage());
            }
        }

        return result;
    }

    /** Rule 10: returns true if a carbon_metric row already exists for this project+evaluation. */
    private boolean carbonMetricExists(int projectId, long evaluationId) {
        String sql = "SELECT COUNT(*) FROM carbon_metric WHERE project_id=? AND evaluation_id=?";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ps.setLong(2, evaluationId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("[ExpertWorkflow] carbonMetricExists check failed: " + e.getMessage());
        }
        return false;
    }

    private Double nullableDouble(ResultSet rs, String col) throws SQLException {
        double v = rs.getDouble(col);
        return rs.wasNull() ? null : v;
    }

    private double toEnergyKwh(double value, String unit) {
        if (value <= 0) return 0.0;
        String normalized = unit != null ? unit.toLowerCase().trim() : "kwh";
        return switch (normalized) {
            case "mwh" -> value * 1_000.0;
            case "gwh" -> value * 1_000_000.0;
            case "wh"  -> value / 1_000.0;
            default     -> value;
        };
    }

    private CarbonMetricResult fallbackCarbonMetric(ProjectData data) {
        double energy    = pos(data.getConsommationEnergie());
        double transport = pos(data.getDistanceTransport());
        double material  = pos(data.getQuantiteMateriau());
        double waste     = pos(data.getDechetsGeneres());

        double energyKwh = toEnergyKwh(energy, data.getUniteEnergie());

        double s1 = r3((transport * FACTOR_TRANSPORT) + (waste * FACTOR_WASTE));
        double s2 = r3(energyKwh * FACTOR_ENERGY);
        double s3 = r3(material * FACTOR_MATERIAL);

        CarbonMetricResult result = new CarbonMetricResult();
        result.setScope1Tco2(s1);
        result.setScope2Tco2(s2);
        result.setScope3Tco2(s3);
        result.setTotalTco2(r3(s1 + s2 + s3));
        result.setMethod("GL_HEURISTIC_CLIMATIQ_COMPAT_V1");
        return result;
    }

    // ══════════════════════════════════════════════════════════════════════
    // STEP 2: HEURISTIC ML FALLBACK
    // Mirrors Python predict_model.py logic exactly
    // ══════════════════════════════════════════════════════════════════════

    private MlPredictionResult heuristicMlPrediction(CarbonMetricResult carbon) {
        double totalTco2 = orZero(carbon.getTotalTco2());
        double quality   = orZero(carbon.getDataQualityScore(), 50.0);

        // ESG score formula (mirrors Python)
        double score = Math.max(0, Math.min(10, 9.5 - (totalTco2 / 25.0)));
        int esgScore = (int) Math.round(score);

        // Credibility
        int credibility = (int) Math.round(Math.max(0, Math.min(100, quality)));

        // Carbon risk
        String carbonRisk;
        if (totalTco2 >= 50) carbonRisk = "HIGH";
        else if (totalTco2 >= 20) carbonRisk = "MEDIUM";
        else carbonRisk = "LOW";

        // Decision
        String decision;
        if ("HIGH".equals(carbonRisk)) {
            decision = "REJECTED";
        } else if (esgScore >= 7 && "LOW".equals(carbonRisk)) {
            decision = "APPROVED";
        } else if (esgScore >= 5) {
            decision = "REVISION_REQUIRED";
        } else {
            decision = "REJECTED";
        }

        MlPredictionResult result = new MlPredictionResult();
        result.setPredictedEsgScore(esgScore);
        result.setCredibilityScore(credibility);
        result.setCarbonRisk(carbonRisk);
        result.setDecision(decision);
        result.setModelVersion("java-heuristic-v1");
        return result;
    }

    private FraudAssessmentResult heuristicFraudAssessment(ProjectData data, CarbonMetricResult carbon) {
        double energy    = pos(data.getConsommationEnergie());
        double transport = pos(data.getDistanceTransport());
        double material  = pos(data.getQuantiteMateriau());
        double declared  = pos(data.getEmissionsEstimees());
        double computed  = orZero(carbon.getTotalTco2());

        int missingNumeric  = (energy == 0 ? 1 : 0) + (declared == 0 ? 1 : 0);
        int missingOptional = (transport == 0 ? 1 : 0) + (material == 0 ? 1 : 0);

        double distPerMat = material > 0 ? transport / material : 0.0;
        double emPerEn    = energy   > 0 ? declared  / energy   : 0.0;
        double gapBase    = Math.max(Math.max(computed, declared), 1.0);
        double carbonGap  = Math.abs(declared - computed) / gapBase;

        double risk = 0.0;
        risk += Math.min(0.10, missingNumeric  * 0.08);
        risk += Math.min(0.06, missingOptional * 0.015);
        risk += Math.min(0.10, carbonGap       * 0.10);

        if (computed >= 80)      risk += 0.25;
        else if (computed >= 50) risk += 0.12;

        if (distPerMat > 14)      risk += 0.12;
        else if (distPerMat > 10) risk += 0.06;

        if (emPerEn > 0.06)      risk += 0.10;
        else if (emPerEn > 0.04) risk += 0.05;

        risk = Math.max(0.0, Math.min(1.0, risk));
        double anomaly = Math.max(0.0, Math.min(3.0, 1.05 * risk));

        boolean hardSignal = missingNumeric >= 2 || distPerMat > 20 || emPerEn > 0.08 || computed >= 80;
        boolean fraudFlag  = hardSignal || risk >= 0.65;

        FraudAssessmentResult result = new FraudAssessmentResult();
        result.setFraudRiskScore(r4(risk));
        result.setFraudAnomalyScore(r4(anomaly));
        result.setFraudFlag(fraudFlag);
        result.setModelVersion("java-heuristic-v1");
        return result;
    }

    // ══════════════════════════════════════════════════════════════════════
    // STEP 3: GREEN CREDIT DISPATCH
    // ══════════════════════════════════════════════════════════════════════

    private GreenCreditResult computeGreenCredits(Projet project,
                                                   CarbonMetricResult carbon,
                                                   FraudAssessmentResult fraud,
                                                   MlPredictionResult ml) {
        double actualTco2   = orZero(carbon.getTotalTco2());
        double quality      = orZero(carbon.getDataQualityScore(), 50.0);
        double avoidedTco2  = baselineService.calculateAvoided(ProjectData.from(project), actualTco2);
        double fraudRisk    = orZero(fraud.getFraudRiskScore());
        int    esgScore     = ml.getPredictedEsgScore() != null ? ml.getPredictedEsgScore() : 0;

        // Eligibility check
        EligibilityCheckService.EligibilityResult eligibility =
            eligibilityService.check(project.getStatutEvaluation(), avoidedTco2, quality, fraud);

        if (!eligibility.eligible()) {
            GreenCreditResult r = GreenCreditResult.notEligible(eligibility.reason());
            r.setBaselineTco2(baselineService.calculateBaseline(ProjectData.from(project)));
            r.setActualTco2(actualTco2);
            r.setAvoidedTco2(avoidedTco2);
            return r;
        }

        // Calculate credits
        double credibilityFactor = eligibilityService.credibilityFactor(quality, fraudRisk, avoidedTco2);
        double esgMultiplier     = eligibilityService.esgMultiplier(esgScore);
        double credits           = r3(avoidedTco2 * credibilityFactor * esgMultiplier);

        String formula = String.format("%.3f tCO2e × %.2f credibility × %.2f ESG = %.3f credits",
            avoidedTco2, credibilityFactor, esgMultiplier, credits);

        String badge = credits > 10 ? "High Impact" : credits > 1 ? "Eligible" : "Low Impact";

        GreenCreditResult result = new GreenCreditResult();
        result.setBaselineTco2(baselineService.calculateBaseline(ProjectData.from(project)));
        result.setActualTco2(actualTco2);
        result.setAvoidedTco2(avoidedTco2);
        result.setDispatchedCredits(credits);
        result.setCredibilityFactor(credibilityFactor);
        result.setEsgMultiplier(esgMultiplier);
        result.setEligible(true);
        result.setStatusBadge(badge);
        result.setFormula(formula);
        result.setExplanation(eligibility.reason());
        return result;
    }

    // ══════════════════════════════════════════════════════════════════════
    // OPENROUTER RECOMMENDATION ENRICHMENT
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Calls OpenRouter to generate contextual recommendations and sets them
     * on the MlPredictionResult before it is persisted.
     * Falls back silently to whatever the ML API already returned.
     */
    private void enrichRecommendations(MlPredictionResult ml, Projet project,
                                        FraudAssessmentResult fraud,
                                        CarbonMetricResult carbon) {
        try {
            String aiRecs = aiSuggestionService.generateRecommendationsForPipeline(
                project.getTitre(),
                ml.getDecision(),
                ml.getPredictedEsgScore() != null ? ml.getPredictedEsgScore() : 0,
                ml.getCarbonRisk(),
                orZero(carbon.getTotalTco2()),
                orZero(fraud.getFraudRiskScore()),
                java.util.Collections.emptyList()
            );
            if (aiRecs != null && !aiRecs.isBlank()) {
                ml.setRecommendations(aiRecs);
                System.out.println("[ExpertWorkflow] Recommendations enriched via OpenRouter");
            }
        } catch (Exception e) {
            System.err.println("[ExpertWorkflow] enrichRecommendations failed: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // DB PERSISTENCE  (Rules 1-4)
    // ══════════════════════════════════════════════════════════════════════
    private void storeMlPrediction(int projectId, long evaluationId,
                                   MlPredictionResult ml, int expertId) {
        String sql = "INSERT INTO ml_predictions " +
                     "(evaluation_id, project_id, predicted_esg_score, credibility_score, carbon_risk, " +
                     " decision, recommendations, model_version, created_by_user_id, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, evaluationId);                       // ← NEVER NULL
            ps.setInt(2, projectId);
            setIntOrNull(ps, 3, ml.getPredictedEsgScore());
            setIntOrNull(ps, 4, ml.getCredibilityScore());
            ps.setString(5, ml.getCarbonRisk());
            ps.setString(6, ml.getDecision());
            ps.setString(7, ml.getRecommendations());
            ps.setString(8, ml.getModelVersion());
            ps.setInt(9, expertId);
            ps.executeUpdate();
            System.out.println("[ExpertWorkflow] Inserted ml_predictions with evaluationId=" + evaluationId);
        } catch (SQLException e) {
            System.err.println("[ExpertWorkflow] Failed to store ml_predictions: " + e.getMessage());
        }
    }

    /** Rule 2 step 3 — ml_decision_snapshots with evaluation_id (NEVER NULL). */
    private void storeMlDecisionSnapshot(int projectId, long evaluationId,
                                         MlPredictionResult ml, String projectName) {
        String sql = "INSERT INTO ml_decision_snapshots " +
                     "(project_id, evaluation_id, project_name, decision, confidence, score, " +
                     " esg_score, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ps.setLong(2, evaluationId);                       // ← NEVER NULL
            ps.setString(3, projectName);
            ps.setString(4, ml.getDecision());
            // confidence = credibility_score / 100.0 (0-1 range)
            Integer cred = ml.getCredibilityScore();
            if (cred == null) ps.setNull(5, Types.DECIMAL);
            else              ps.setDouble(5, cred / 100.0);
            // score = predicted_esg_score
            setIntOrNull(ps, 6, ml.getPredictedEsgScore());
            setIntOrNull(ps, 7, ml.getPredictedEsgScore());
            ps.executeUpdate();
            System.out.println("[ExpertWorkflow] Inserted ml_decision_snapshots with evaluationId=" + evaluationId);
        } catch (SQLException e) {
            System.err.println("[ExpertWorkflow] Failed to store ml_decision_snapshots: " + e.getMessage());
        }
    }

    /** Rule 2 step 4 — ml_fraud_assessment with evaluation_id (NEVER NULL). */
    private void storeFraudAssessment(int projectId, long evaluationId,
                                      FraudAssessmentResult fraud) {
        String sql = "INSERT INTO ml_fraud_assessment " +
                     "(project_id, evaluation_id, fraud_risk_score, anomaly_score, fraud_flag, " +
                     " suspicious_features, model_version, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ps.setLong(2, evaluationId);                       // ← NEVER NULL
            setDoubleOrNull(ps, 3, fraud.getFraudRiskScore());
            setDoubleOrNull(ps, 4, fraud.getFraudAnomalyScore());
            ps.setBoolean(5, fraud.isFraudFlag());
            ps.setString(6, fraud.getFraudReasons() != null
                ? String.join(" | ", fraud.getFraudReasons()) : null);
            ps.setString(7, fraud.getModelVersion());
            ps.executeUpdate();
            System.out.println("[ExpertWorkflow] Inserted ml_fraud_assessment with evaluationId=" + evaluationId);
        } catch (SQLException e) {
            System.err.println("[ExpertWorkflow] Failed to store ml_fraud_assessment: " + e.getMessage());
        }
    }

    /**
     * Rule 2 step 5 + Rule 3 — update evaluation row with final score and validity.
     * score_final = predicted_esg_score
     * est_valide  = 1 if decision starts with "APPROV", else 0
     */
    private void updateEvaluation(long evaluationId, MlPredictionResult ml) {
        String sql = "UPDATE evaluation SET score_final=?, est_valide=? WHERE id_evaluation=?";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setIntOrNull(ps, 1, ml.getPredictedEsgScore());
            String decision = ml.getDecision();
            boolean approved = decision != null && decision.toUpperCase().startsWith("APPROV");
            ps.setInt(2, approved ? 1 : 0);
            ps.setLong(3, evaluationId);
            ps.executeUpdate();
            System.out.printf("[ExpertWorkflow] Updated evaluation id=%d score=%d est_valide=%d%n",
                evaluationId, ml.getPredictedEsgScore(), approved ? 1 : 0);
        } catch (SQLException e) {
            System.err.println("[ExpertWorkflow] Failed to update evaluation: " + e.getMessage());
        }
    }

    /**
     * Rule 2 step 6 + Rules 4 & 5 — update projet fraud fields and statut.
     * Also updates score_esg, fraud_model_version, fraud_scored_at.
     */
    private void updateProjectFraudAndStatus(int projectId, MlPredictionResult ml,
                                              FraudAssessmentResult fraud, Projet project) {
        // Fraud + ESG fields
        String fraudSql = "UPDATE projet SET " +
                          "score_esg=?, fraud_risk_score=?, fraud_anomaly_score=?, fraud_flag=?, " +
                          "fraud_reasons=?, fraud_model_version=?, fraud_scored_at=NOW() WHERE id=?";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(fraudSql)) {
            setIntOrNull(ps, 1, ml.getPredictedEsgScore());
            setDoubleOrNull(ps, 2, fraud.getFraudRiskScore());
            setDoubleOrNull(ps, 3, fraud.getFraudAnomalyScore());
            ps.setBoolean(4, fraud.isFraudFlag());
            ps.setString(5, fraud.getFraudReasons() != null
                ? String.join(" | ", fraud.getFraudReasons()) : null);
            ps.setString(6, fraud.getModelVersion());
            ps.setInt(7, projectId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[ExpertWorkflow] Failed to update projet fraud fields: " + e.getMessage());
        }

        // Rule 4: update statut based on decision
        String decision = ml.getDecision();
        if (decision == null) return;

        if (decision.toUpperCase().startsWith("APPROV")) {
            // APPROVED: set statut + statut_financement
            String statusSql = "UPDATE projet SET statut='APPROVED', " +
                               "statut_financement = CASE WHEN montant_demande > 0 " +
                               "THEN 'SEEKING_FUNDING' ELSE 'NON_APPLICABLE' END WHERE id=?";
            try (Connection conn = MyConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(statusSql)) {
                ps.setInt(1, projectId);
                ps.executeUpdate();
                System.out.println("[ExpertWorkflow] Project " + projectId + " → APPROVED");
            } catch (SQLException e) {
                System.err.println("[ExpertWorkflow] Failed to set project APPROVED: " + e.getMessage());
            }
        } else if (decision.toUpperCase().startsWith("REJECT")) {
            // REJECTED
            String statusSql = "UPDATE projet SET statut='REJECTED', " +
                               "statut_financement='NON_APPLICABLE' WHERE id=?";
            try (Connection conn = MyConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(statusSql)) {
                ps.setInt(1, projectId);
                ps.executeUpdate();
                System.out.println("[ExpertWorkflow] Project " + projectId + " → REJECTED");
            } catch (SQLException e) {
                System.err.println("[ExpertWorkflow] Failed to set project REJECTED: " + e.getMessage());
            }
        }
        // REVISION_REQUIRED: leave statut as IN_PROGRESS (already set in transitionToInProgress)
    }

    private void updateProjectGreenCreditFields(int projectId,
                                                 GreenCreditResult credit,
                                                 CarbonMetricResult carbon) {
        String sql = "UPDATE projet SET " +
                     "baseline_tco2=?, actual_tco2=?, avoided_tco2=?, " +
                     "dispatched_green_credits=?, green_credit_credibility_factor=?, " +
                     "green_credit_esg_multiplier=?, green_credit_dispatch_status=?, " +
                     "green_credit_status_badge=?, green_credit_formula=?, " +
                     "green_credit_last_computed_at=NOW() WHERE id=?";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setDoubleOrNull(ps, 1, credit.getBaselineTco2());
            setDoubleOrNull(ps, 2, credit.getActualTco2());
            setDoubleOrNull(ps, 3, credit.getAvoidedTco2());
            setDoubleOrNull(ps, 4, credit.getDispatchedCredits());
            setDoubleOrNull(ps, 5, credit.getCredibilityFactor());
            setDoubleOrNull(ps, 6, credit.getEsgMultiplier());
            ps.setString(7, credit.isEligible() ? "DISPATCHED" : "NOT_ELIGIBLE");
            ps.setString(8, credit.getStatusBadge());
            ps.setString(9, credit.getFormula());
            ps.setInt(10, projectId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[ExpertWorkflow] Failed to update projet green credit fields: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // DATA QUALITY SCORE
    // ══════════════════════════════════════════════════════════════════════

    private double computeDataQualityScore(ProjectData data, List<String> errors) {
        int score = 100;

        // Deduct for missing fields
        if (pos(data.getConsommationEnergie()) == 0) score -= 15;
        if (pos(data.getDistanceTransport())   == 0) score -= 10;
        if (pos(data.getQuantiteMateriau())    == 0) score -= 10;
        if (pos(data.getDechetsGeneres())      == 0) score -= 5;
        if (pos(data.getEmissionsEstimees())   == 0) score -= 10;
        if (data.getSecteur()    == null || data.getSecteur().isBlank())    score -= 5;
        if (data.getLocalisation() == null || data.getLocalisation().isBlank()) score -= 5;

        // Deduct for API errors
        if (errors != null) score -= Math.min(20, errors.size() * 5);

        return Math.max(0, Math.min(100, score));
    }

    // ══════════════════════════════════════════════════════════════════════
    // UTILITIES
    // ══════════════════════════════════════════════════════════════════════

    private double pos(Double v)                    { return (v != null && v > 0) ? v : 0.0; }
    private double orZero(Double v)                 { return v != null ? v : 0.0; }
    private double orZero(Double v, double def)     { return v != null ? v : def; }
    private double r3(double v)                     { return Math.round(v * 1000.0) / 1000.0; }
    private double r4(double v)                     { return Math.round(v * 10000.0) / 10000.0; }

    private void setDoubleOrNull(PreparedStatement ps, int idx, Double v) throws SQLException {
        if (v == null) ps.setNull(idx, Types.DOUBLE);
        else           ps.setDouble(idx, v);
    }

    private void setIntOrNull(PreparedStatement ps, int idx, Integer v) throws SQLException {
        if (v == null) ps.setNull(idx, Types.INTEGER);
        else           ps.setInt(idx, v);
    }
}
