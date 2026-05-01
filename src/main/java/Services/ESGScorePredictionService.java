package Services;

import Models.*;
import java.util.List;

/**
 * ML-based ESG Score Prediction
 * Predicts ESG score (0-10) based on project environmental data
 * 
 * Formula from Symfony:
 * predictedScore = 9.5 - (totalTco2 / 25.0) - (blockCount * 0.6)
 * 
 * @author GreenLedger Team
 */
public class ESGScorePredictionService {
    
    /**
     * Predict ESG score for project
     */
    public static ESGPredictionResult predict(Projet project, CarbonMetrics carbonMetric, List<DataCheck> checks) {
        double totalTco2 = carbonMetric.getActualTco2() != null ? carbonMetric.getActualTco2() : 0.0;
        double qualityScore = 70.0; // Default quality score
        
        int blockCount = 0;
        if (checks != null) {
            blockCount = (int) checks.stream()
                .filter(c -> "BLOCK".equals(c.getOutcome()))
                .count();
        }
        
        // ML Formula (exact from Symfony)
        double predictedScore = 9.5 - (totalTco2 / 25.0) - (blockCount * 0.6);
        predictedScore = Math.max(0, Math.min(10, predictedScore));
        
        // Credibility Score
        int credibility = (int) Math.round(Math.max(0, Math.min(100, qualityScore - (blockCount * 10))));
        
        // Carbon Risk Assessment
        String carbonRisk = "LOW";
        if (totalTco2 >= 50) {
            carbonRisk = "HIGH";
        } else if (totalTco2 >= 20) {
            carbonRisk = "MEDIUM";
        }
        
        // Decision Logic (exact from Symfony)
        String decision;
        if (carbonRisk.equals("HIGH")) {
            decision = "REJECTED";
        } else if (predictedScore >= 7 && carbonRisk.equals("LOW")) {
            decision = "APPROVED";
        } else if (predictedScore >= 5) {
            decision = "REVISION_REQUIRED";
        } else {
            decision = "REJECTED";
        }
        
        System.out.println(String.format(
            "[ESG] Prediction: score=%.2f, credibility=%d, risk=%s, decision=%s",
            predictedScore, credibility, carbonRisk, decision
        ));
        
        return new ESGPredictionResult(
            (int) Math.round(predictedScore),
            credibility,
            carbonRisk,
            decision
        );
    }
    
    /**
     * ESG Prediction Result
     */
    public static class ESGPredictionResult {
        private final int predictedScore;
        private final int credibility;
        private final String carbonRisk;
        private final String decision;
        
        public ESGPredictionResult(int predictedScore, int credibility, String carbonRisk, String decision) {
            this.predictedScore = predictedScore;
            this.credibility = credibility;
            this.carbonRisk = carbonRisk;
            this.decision = decision;
        }
        
        public int getPredictedScore() { return predictedScore; }
        public int getCredibility() { return credibility; }
        public String getCarbonRisk() { return carbonRisk; }
        public String getDecision() { return decision; }
    }
    
    /**
     * Data Check (for validation)
     */
    public static class DataCheck {
        private String field;
        private String outcome; // BLOCK, WARN, OK
        
        public DataCheck(String field, String outcome) {
            this.field = field;
            this.outcome = outcome;
        }
        
        public String getField() { return field; }
        public String getOutcome() { return outcome; }
    }
}
