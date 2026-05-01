package Services;

import Models.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced Fraud Risk Assessment Model
 * Calculates fraud risk score (0-1) and anomaly detection
 * 
 * Exact implementation from Symfony ML algorithms
 * 
 * @author GreenLedger Team
 */
public class EnhancedFraudDetectionService {
    
    /**
     * Assess fraud risk for project
     */
    public static FraudAssessmentResult assess(Projet project, CarbonMetrics carbonMetric, 
                                               List<ESGScorePredictionService.DataCheck> checks) {
        
        // Extract project data
        double energy = getPositiveFloat(0.0); // Would get from project
        double transport = getPositiveFloat(0.0);
        double material = getPositiveFloat(0.0);
        double declaredEmissions = getPositiveFloat(0.0);
        double computedTotal = carbonMetric.getActualTco2() != null ? carbonMetric.getActualTco2() : 0.0;
        
        // Count checks
        int blockCount = 0;
        int warnCount = 0;
        int criticalBlockCount = 0;
        
        if (checks != null) {
            blockCount = (int) checks.stream().filter(c -> "BLOCK".equals(c.getOutcome())).count();
            warnCount = (int) checks.stream().filter(c -> "WARN".equals(c.getOutcome())).count();
            criticalBlockCount = (int) checks.stream()
                .filter(c -> "BLOCK".equals(c.getOutcome()) && !"documents".equals(c.getField()))
                .count();
        }
        
        // Count missing critical fields
        int missingNumeric = 0;
        if (energy == 0) missingNumeric++;
        if (declaredEmissions == 0) missingNumeric++;
        
        // Count missing optional fields
        int missingOptional = 0;
        if (transport == 0) missingOptional++;
        if (material == 0) missingOptional++;
        
        // Calculate ratios
        double distancePerMaterial = material > 0 ? transport / material : 0.0;
        double emissionsPerEnergy = energy > 0 ? declaredEmissions / energy : 0.0;
        
        // Carbon gap ratio
        double carbonGapBase = Math.max(Math.max(computedTotal, declaredEmissions), 1.0);
        double carbonGapRatio = Math.abs(declaredEmissions - computedTotal) / carbonGapBase;
        
        // Calculate risk score (exact formula from Symfony)
        double riskScore = 0.0;
        riskScore += Math.min(0.35, blockCount * 0.14);
        riskScore += Math.min(0.12, warnCount * 0.02);
        riskScore += Math.min(0.10, missingNumeric * 0.08);
        riskScore += Math.min(0.06, missingOptional * 0.015);
        riskScore += Math.min(0.10, carbonGapRatio * 0.10);
        
        // Carbon severity increases fraud risk
        if (computedTotal >= 80.0) {
            riskScore += 0.25;
        } else if (computedTotal >= 50.0) {
            riskScore += 0.12;
        }
        
        // Anomalous ratios
        if (distancePerMaterial > 14.0) {
            riskScore += 0.12;
        } else if (distancePerMaterial > 10.0) {
            riskScore += 0.06;
        }
        
        if (emissionsPerEnergy > 0.06) {
            riskScore += 0.10;
        } else if (emissionsPerEnergy > 0.04) {
            riskScore += 0.05;
        }
        
        riskScore = Math.max(0.0, Math.min(1.0, riskScore));
        
        // Anomaly score
        double anomalyScore = Math.max(0.0, Math.min(3.0, (1.05 * riskScore) + (blockCount * 0.08)));
        
        // Hard fraud signals
        boolean hardFraudSignal = criticalBlockCount >= 3 ||
                                  missingNumeric >= 2 ||
                                  distancePerMaterial > 20.0 ||
                                  emissionsPerEnergy > 0.08 ||
                                  computedTotal >= 80.0;
        
        boolean fraudFlag = hardFraudSignal || riskScore >= 0.65;
        
        // Build reasons
        List<String> reasons = new ArrayList<>();
        if (missingNumeric >= 2) {
            reasons.add("Données environnementales critiques manquantes");
        }
        if (carbonGapRatio > 0.35) {
            reasons.add("Écart élevé entre émissions déclarées et calculées");
        }
        if (computedTotal >= 50.0) {
            reasons.add("Risque carbone élevé (total tCO2e)");
        }
        if (distancePerMaterial > 8.0) {
            reasons.add("Distance transport très élevée par rapport à la quantité de matériau");
        }
        if (blockCount > 0) {
            reasons.add("Des contrôles bloquants ont été détectés");
        }
        
        System.out.println(String.format(
            "[Fraud] Assessment: risk=%.4f, anomaly=%.4f, flag=%b, reasons=%d",
            riskScore, anomalyScore, fraudFlag, reasons.size()
        ));
        
        return new FraudAssessmentResult(riskScore, anomalyScore, fraudFlag, reasons);
    }
    
    private static double getPositiveFloat(double value) {
        return Math.max(0.0, value);
    }
    
    /**
     * Fraud Assessment Result
     */
    public static class FraudAssessmentResult {
        private final double riskScore;
        private final double anomalyScore;
        private final boolean fraudFlag;
        private final List<String> reasons;
        
        public FraudAssessmentResult(double riskScore, double anomalyScore, boolean fraudFlag, List<String> reasons) {
            this.riskScore = riskScore;
            this.anomalyScore = anomalyScore;
            this.fraudFlag = fraudFlag;
            this.reasons = reasons;
        }
        
        public double getRiskScore() { return riskScore; }
        public double getAnomalyScore() { return anomalyScore; }
        public boolean isFraudFlag() { return fraudFlag; }
        public List<String> getReasons() { return reasons; }
    }
}
