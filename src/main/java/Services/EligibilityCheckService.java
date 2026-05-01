package Services;

import Models.dto.FraudAssessmentResult;

/**
 * Eligibility Check Service — pure logic, no API.
 *
 * Checks whether a project is eligible for green credit dispatch.
 */
public class EligibilityCheckService {

    private static final double MIN_AVOIDED_TCO2   = 0.5;
    private static final double MIN_DATA_QUALITY    = 60.0;
    private static final double MAX_FRAUD_RISK      = 0.65;

    public record EligibilityResult(boolean eligible, String reason) {}

    /**
     * Check all eligibility criteria.
     */
    public EligibilityResult check(String projectStatus,
                                   double avoidedTco2,
                                   double dataQualityScore,
                                   FraudAssessmentResult fraud) {

        if (!"APPROVED".equals(projectStatus)) {
            return new EligibilityResult(false, "Project not approved (status: " + projectStatus + ")");
        }

        if (avoidedTco2 <= 0) {
            return new EligibilityResult(false, "No avoided emissions (actual >= baseline)");
        }

        if (avoidedTco2 < MIN_AVOIDED_TCO2) {
            return new EligibilityResult(false,
                String.format("Avoided tCO2e (%.3f) below minimum threshold (%.1f)", avoidedTco2, MIN_AVOIDED_TCO2));
        }

        if (dataQualityScore < MIN_DATA_QUALITY) {
            return new EligibilityResult(false,
                String.format("Data quality score (%.0f) below minimum (%.0f)", dataQualityScore, MIN_DATA_QUALITY));
        }

        if (fraud != null) {
            if (Boolean.TRUE.equals(fraud.getFraudFlag())) {
                return new EligibilityResult(false, "Fraud flag set");
            }
            double risk = fraud.getFraudRiskScore() != null ? fraud.getFraudRiskScore() : 0.0;
            if (risk >= MAX_FRAUD_RISK) {
                return new EligibilityResult(false,
                    String.format("Fraud risk (%.4f) exceeds maximum (%.2f)", risk, MAX_FRAUD_RISK));
            }
        }

        return new EligibilityResult(true, "All eligibility criteria met");
    }

    /**
     * Calculate credibility factor based on data quality and fraud risk.
     */
    public double credibilityFactor(double dataQualityScore, double fraudRisk, double avoidedTco2) {
        double factor;

        if (dataQualityScore >= 85.0)      factor = 1.00;
        else if (dataQualityScore >= 65.0) factor = 0.85;
        else if (dataQualityScore >= 45.0) factor = 0.60;
        else                               factor = 0.40;

        // Adjust for low avoided emissions
        if (avoidedTco2 < 1.0)      factor *= 0.40;
        else if (avoidedTco2 < 5.0) factor *= 0.70;

        // Adjust for fraud risk
        if (fraudRisk >= 0.55)      factor *= 0.70;
        else if (fraudRisk >= 0.40) factor *= 0.85;

        return Math.max(0.0, Math.min(1.0, factor));
    }

    /**
     * Calculate ESG multiplier based on score.
     */
    public double esgMultiplier(int esgScore) {
        if (esgScore >= 8.5) return 1.20;  // Excellent
        if (esgScore >= 7.0) return 1.10;  // Good
        if (esgScore >= 5.0) return 1.00;  // Average
        return 0.80;                        // Poor
    }
}
