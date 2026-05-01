package Services;

import Models.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Hybrid ML Service
 *
 * Primary  → Python FastAPI (ml_service/app.py) — trained scikit-learn models
 * Fallback → Pure-Java formulas (exact Symfony algorithm port)
 *
 * Call order:
 *   1. predictESG   → /predict
 *   2. assessFraud  → /fraud/assess
 *   3. greenCredits → Java only (business logic, not ML)
 *   4. recommend    → /recommend
 */
public class HybridMLService {

    private final MlApiClient                    apiClient;
    private final ESGScorePredictionService      esgFallback;
    private final EnhancedFraudDetectionService  fraudFallback;
    private final EnhancedGreenCreditCalculator  creditCalc;
    private final boolean                        pythonAvailable;

    public HybridMLService() {
        this.apiClient      = new MlApiClient();
        this.esgFallback    = new ESGScorePredictionService();
        this.fraudFallback  = new EnhancedFraudDetectionService();
        this.creditCalc     = new EnhancedGreenCreditCalculator();
        this.pythonAvailable = apiClient.isHealthy();

        System.out.println("[HybridML] Python API " +
            (pythonAvailable ? "✓ available — using trained models"
                             : "✗ unavailable — using Java fallback"));
    }

    // -------------------------------------------------------------------------
    // Main entry point
    // -------------------------------------------------------------------------

    public MLAnalysisResult analyze(Projet project, CarbonMetrics carbonMetric) {
        System.out.println("[HybridML] Analyzing project " + project.getId());

        MLAnalysisResult result = new MLAnalysisResult();
        result.setProjectId(project.getId());

        // 1. ESG prediction
        ESGScorePredictionService.ESGPredictionResult esg = predictESG(project, carbonMetric);
        result.setEsgPrediction(esg);

        // 2. Fraud assessment
        EnhancedFraudDetectionService.FraudAssessmentResult fraud = assessFraud(project, carbonMetric);
        result.setFraudAssessment(fraud);

        // 3. Green credits (always Java — pure business logic)
        EnhancedGreenCreditCalculator.GreenCreditResult credits =
            creditCalc.calculate(project, carbonMetric, fraud);
        result.setGreenCredits(credits);

        // 4. Recommendations
        result.setRecommendations(getRecommendations(project, esg, fraud));

        System.out.printf("[HybridML] Done — ESG=%d | fraud=%.3f | credits=%.3f | decision=%s%n",
            esg.getPredictedScore(), fraud.getRiskScore(),
            credits.getCredits(), esg.getDecision());

        return result;
    }

    // -------------------------------------------------------------------------
    // Individual predictions with fallback
    // -------------------------------------------------------------------------

    private ESGScorePredictionService.ESGPredictionResult predictESG(
            Projet project, CarbonMetrics carbonMetric) {

        if (pythonAvailable) {
            try {
                Models.dto.ProjectData data = Models.dto.ProjectData.from(project);
                if (carbonMetric != null && carbonMetric.getActualTco2() != null) {
                    data.setTotalTco2(carbonMetric.getActualTco2());
                }
                Models.dto.MlPredictionResult r = apiClient.predictEsg(data);
                System.out.println("[HybridML] ESG from Python (" + r.getModelVersion() + ")");
                return new ESGScorePredictionService.ESGPredictionResult(
                    r.getPredictedEsgScore() != null ? r.getPredictedEsgScore() : 5,
                    r.getCredibilityScore()  != null ? r.getCredibilityScore()  : 50,
                    r.getCarbonRisk()  != null ? r.getCarbonRisk()  : "MEDIUM",
                    r.getDecision()    != null ? r.getDecision()    : "REVISION_REQUIRED"
                );
            } catch (Exception e) {
                System.err.println("[HybridML] ESG API failed → Java fallback: " + e.getMessage());
            }
        }

        System.out.println("[HybridML] ESG from Java fallback");
        return esgFallback.predict(project, carbonMetric, null);
    }

    private EnhancedFraudDetectionService.FraudAssessmentResult assessFraud(
            Projet project, CarbonMetrics carbonMetric) {

        if (pythonAvailable) {
            try {
                Models.dto.ProjectData data = Models.dto.ProjectData.from(project);
                if (carbonMetric != null && carbonMetric.getActualTco2() != null) {
                    data.setTotalTco2(carbonMetric.getActualTco2());
                }
                Models.dto.FraudAssessmentResult r = apiClient.predictFraud(data);
                System.out.println("[HybridML] Fraud from Python (" + r.getModelVersion() + ")");
                return new EnhancedFraudDetectionService.FraudAssessmentResult(
                    r.getFraudRiskScore()    != null ? r.getFraudRiskScore()    : 0.0,
                    r.getFraudAnomalyScore() != null ? r.getFraudAnomalyScore() : 0.0,
                    Boolean.TRUE.equals(r.getFraudFlag()),
                    r.getFraudReasons()
                );
            } catch (Exception e) {
                System.err.println("[HybridML] Fraud API failed → Java fallback: " + e.getMessage());
            }
        }

        System.out.println("[HybridML] Fraud from Java fallback");
        return fraudFallback.assess(project, carbonMetric, null);
    }

    private List<String> getRecommendations(
            Projet project,
            ESGScorePredictionService.ESGPredictionResult esg,
            EnhancedFraudDetectionService.FraudAssessmentResult fraud) {

        if (pythonAvailable) {
            try {
                Models.dto.ProjectData data = Models.dto.ProjectData.from(project);
                List<String> labels = apiClient.getRecommendations(data);
                System.out.println("[HybridML] Recommendations from Python: " + labels);
                return labels;
            } catch (Exception e) {
                System.err.println("[HybridML] Recommendations API failed → Java fallback: " + e.getMessage());
            }
        }

        return buildFallbackRecommendations(esg, fraud);
    }

    // -------------------------------------------------------------------------
    // Java fallback recommendations (mirrors Python _rec_fallback)
    // -------------------------------------------------------------------------

    private List<String> buildFallbackRecommendations(
            ESGScorePredictionService.ESGPredictionResult esg,
            EnhancedFraudDetectionService.FraudAssessmentResult fraud) {

        List<String> labels = new ArrayList<>();

        if (esg.getPredictedScore() < 7)  labels.add("mitigation_plan");
        if (esg.getPredictedScore() < 5)  labels.add("resubmission_pack");
        if ("HIGH".equals(esg.getCarbonRisk())) {
            labels.add("optimize_energy");
            labels.add("audit_emissions");
        }
        if (fraud.getRiskScore() >= 0.40) labels.add("improve_data_quality");
        if (fraud.getRiskScore() >= 0.40) labels.add("audit_emissions");
        if (fraud.isFraudFlag())          labels.add("resubmission_pack");

        if (labels.isEmpty()) labels.add("mitigation_plan");

        // deduplicate while preserving order
        return labels.stream().distinct().collect(java.util.stream.Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    public boolean isPythonApiAvailable() {
        return pythonAvailable && apiClient.isHealthy();
    }

    // -------------------------------------------------------------------------
    // Result container
    // -------------------------------------------------------------------------

    public static class MLAnalysisResult {
        private Integer projectId;
        private ESGScorePredictionService.ESGPredictionResult esgPrediction;
        private EnhancedFraudDetectionService.FraudAssessmentResult fraudAssessment;
        private EnhancedGreenCreditCalculator.GreenCreditResult greenCredits;
        private List<String> recommendations;

        public Integer  getProjectId()       { return projectId; }
        public void     setProjectId(Integer v)       { projectId = v; }

        public ESGScorePredictionService.ESGPredictionResult getEsgPrediction() { return esgPrediction; }
        public void setEsgPrediction(ESGScorePredictionService.ESGPredictionResult v) { esgPrediction = v; }

        public EnhancedFraudDetectionService.FraudAssessmentResult getFraudAssessment() { return fraudAssessment; }
        public void setFraudAssessment(EnhancedFraudDetectionService.FraudAssessmentResult v) { fraudAssessment = v; }

        public EnhancedGreenCreditCalculator.GreenCreditResult getGreenCredits() { return greenCredits; }
        public void setGreenCredits(EnhancedGreenCreditCalculator.GreenCreditResult v) { greenCredits = v; }

        public List<String> getRecommendations()          { return recommendations; }
        public void         setRecommendations(List<String> v) { recommendations = v; }

        @Override
        public String toString() {
            return String.format(
                "MLAnalysisResult[project=%d, esg=%d, fraud=%.3f, credits=%.3f, recs=%d]",
                projectId,
                esgPrediction   != null ? esgPrediction.getPredictedScore()  : 0,
                fraudAssessment != null ? fraudAssessment.getRiskScore()      : 0.0,
                greenCredits    != null ? greenCredits.getCredits()           : 0.0,
                recommendations != null ? recommendations.size()              : 0
            );
        }
    }
}
