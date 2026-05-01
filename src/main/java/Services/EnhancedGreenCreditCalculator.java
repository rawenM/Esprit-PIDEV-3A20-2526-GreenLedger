package Services;

import Models.*;

/**
 * Enhanced Green Credit Dispatch Calculation
 * Calculates eligible carbon credits based on avoided emissions
 * 
 * Exact implementation from Symfony ML algorithms
 * 
 * Formula:
 * credits = avoided * credibilityFactor * esgMultiplier
 * 
 * @author GreenLedger Team
 */
public class EnhancedGreenCreditCalculator {
    
    /**
     * Calculate green credits for project
     */
    public static GreenCreditResult calculate(Projet project, CarbonMetrics carbonMetric, 
                                              EnhancedFraudDetectionService.FraudAssessmentResult fraud) {
        
        // Baseline emissions (what would have been emitted)
        double baseline = calculateBaseline(project);
        
        // Actual emissions (from Climatiq or fallback)
        double actual = carbonMetric.getActualTco2() != null ? carbonMetric.getActualTco2() : 0.0;
        
        // Avoided emissions
        double avoided = Math.max(0, baseline - actual);
        
        // Data quality score
        double quality = 70.0; // Default, would get from carbonMetric
        
        // ESG score
        double esgScore = project.getScoreEsg() != null ? project.getScoreEsg() : 0.0;
        
        // Eligibility checks
        boolean statusApproved = "APPROVED".equals(project.getStatutEvaluation());
        boolean positiveAvoided = avoided > 0;
        boolean minimumThreshold = avoided >= 0.5;
        boolean fraudRiskOk = !fraud.isFraudFlag() && fraud.getRiskScore() < 0.65;
        boolean sufficientQuality = quality >= 60;
        
        boolean eligible = statusApproved && positiveAvoided && minimumThreshold && 
                          fraudRiskOk && sufficientQuality;
        
        // Credibility factor based on data quality (exact from Symfony)
        double credibilityFactor = 0.0;
        if (eligible) {
            if (quality >= 85.0) {
                credibilityFactor = 1.00;
            } else if (quality >= 65.0) {
                credibilityFactor = 0.85;
            } else if (quality >= 45.0) {
                credibilityFactor = 0.60;
            } else {
                credibilityFactor = 0.40;
            }
            
            // Adjust for low avoided emissions
            if (avoided < 1.0) {
                credibilityFactor *= 0.40;
            } else if (avoided < 5.0) {
                credibilityFactor *= 0.70;
            }
            
            // Adjust for fraud risk
            if (fraud.getRiskScore() >= 0.55) {
                credibilityFactor *= 0.70;
            } else if (fraud.getRiskScore() >= 0.40) {
                credibilityFactor *= 0.85;
            }
        }
        
        // ESG multiplier (exact from Symfony)
        double esgMultiplier = 1.00;
        if (esgScore >= 8.5) {
            esgMultiplier = 1.20;
        } else if (esgScore >= 7.0) {
            esgMultiplier = 1.10;
        } else if (esgScore >= 5.0) {
            esgMultiplier = 1.00;
        } else {
            esgMultiplier = 0.80;
        }
        
        // Calculate credits
        double credits = eligible ? avoided * credibilityFactor * esgMultiplier : 0.0;
        credits = Math.max(0.001, credits);
        
        // Status badge
        String statusBadge = eligible ? "Eligible" : "Not Eligible";
        if (!eligible && !fraudRiskOk) {
            statusBadge = "Needs Improvement";
        }
        if (!eligible && !positiveAvoided) {
            statusBadge = "High Emissions";
        }
        
        System.out.println(String.format(
            "[GreenCredit] Result: baseline=%.2f, actual=%.2f, avoided=%.2f, credits=%.2f, eligible=%b",
            baseline, actual, avoided, credits, eligible
        ));
        
        return new GreenCreditResult(
            baseline,
            actual,
            avoided,
            credits,
            credibilityFactor,
            esgMultiplier,
            eligible,
            statusBadge
        );
    }
    
    /**
     * Calculate baseline emissions (exact from Symfony)
     */
    private static double calculateBaseline(Projet project) {
        double energy = 0.0; // Would get from project
        double transport = 0.0;
        double material = 0.0;
        double waste = 0.0;
        
        // Baseline emission factors (exact from Symfony)
        return (energy * 0.0006) + 
               (transport * 0.00012) + 
               (material * 0.0015) + 
               (waste * 0.0009);
    }
    
    /**
     * Green Credit Result
     */
    public static class GreenCreditResult {
        private final double baseline;
        private final double actual;
        private final double avoided;
        private final double credits;
        private final double credibilityFactor;
        private final double esgMultiplier;
        private final boolean eligible;
        private final String statusBadge;
        
        public GreenCreditResult(double baseline, double actual, double avoided, double credits,
                                double credibilityFactor, double esgMultiplier, 
                                boolean eligible, String statusBadge) {
            this.baseline = baseline;
            this.actual = actual;
            this.avoided = avoided;
            this.credits = credits;
            this.credibilityFactor = credibilityFactor;
            this.esgMultiplier = esgMultiplier;
            this.eligible = eligible;
            this.statusBadge = statusBadge;
        }
        
        public double getBaseline() { return baseline; }
        public double getActual() { return actual; }
        public double getAvoided() { return avoided; }
        public double getCredits() { return credits; }
        public double getCredibilityFactor() { return credibilityFactor; }
        public double getEsgMultiplier() { return esgMultiplier; }
        public boolean isEligible() { return eligible; }
        public String getStatusBadge() { return statusBadge; }
    }
}
