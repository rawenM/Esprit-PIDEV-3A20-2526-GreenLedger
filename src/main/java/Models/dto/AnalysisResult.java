package Models.dto;

/** Full pipeline output from ExpertWorkflowService. */
public class AnalysisResult {

    private CarbonMetricResult   carbonMetric;
    private MlPredictionResult   mlPrediction;
    private FraudAssessmentResult fraudAssessment;
    private GreenCreditResult    greenCreditDispatch;

    public CarbonMetricResult    getCarbonMetric()                          { return carbonMetric; }
    public void                  setCarbonMetric(CarbonMetricResult v)      { this.carbonMetric = v; }

    public MlPredictionResult    getMlPrediction()                          { return mlPrediction; }
    public void                  setMlPrediction(MlPredictionResult v)      { this.mlPrediction = v; }

    public FraudAssessmentResult getFraudAssessment()                       { return fraudAssessment; }
    public void                  setFraudAssessment(FraudAssessmentResult v){ this.fraudAssessment = v; }

    public GreenCreditResult     getGreenCreditDispatch()                   { return greenCreditDispatch; }
    public void                  setGreenCreditDispatch(GreenCreditResult v){ this.greenCreditDispatch = v; }

    // ── Convenience accessors ─────────────────────────────────────────────

    public String getDecision() {
        return mlPrediction != null ? mlPrediction.getDecision() : null;
    }

    public Integer getEsgScore() {
        return mlPrediction != null ? mlPrediction.getPredictedEsgScore() : null;
    }

    public boolean isFraudFlagged() {
        return fraudAssessment != null && fraudAssessment.isFraudFlag();
    }

    public double getFraudRisk() {
        if (fraudAssessment == null || fraudAssessment.getFraudRiskScore() == null) return 0.0;
        return fraudAssessment.getFraudRiskScore();
    }
}
