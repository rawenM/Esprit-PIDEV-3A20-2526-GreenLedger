package Utils;

/**
 * ML Constants - Configuration values from Symfony
 * 
 * All thresholds and factors used in ML algorithms
 * Matches exactly with Symfony .env configuration
 * 
 * @author GreenLedger Team
 */
public class MLConstants {
    
    // ============================================================================
    // FRAUD DETECTION CONFIGURATION
    // ============================================================================
    public static final double FRAUD_RISK_THRESHOLD = 0.65;
    public static final double FRAUD_ANOMALY_THRESHOLD = 0.70;
    public static final int FRAUD_CRITICAL_BLOCKS_THRESHOLD = 3;
    public static final int FRAUD_MISSING_FIELDS_THRESHOLD = 2;
    public static final double FRAUD_CARBON_GAP_THRESHOLD = 0.35;
    public static final double FRAUD_DISTANCE_PER_MATERIAL_THRESHOLD = 14.0;
    public static final double FRAUD_EMISSIONS_PER_ENERGY_THRESHOLD = 0.06;
    
    // ============================================================================
    // GREEN CREDIT ELIGIBILITY
    // ============================================================================
    public static final double GREEN_CREDIT_MIN_AVOIDED_TCO2 = 0.5;
    public static final int GREEN_CREDIT_MIN_DATA_QUALITY = 60;
    public static final double GREEN_CREDIT_MAX_FRAUD_RISK = 0.65;
    public static final int GREEN_CREDIT_CREDIBILITY_QUALITY_THRESHOLD_HIGH = 85;
    public static final int GREEN_CREDIT_CREDIBILITY_QUALITY_THRESHOLD_MEDIUM = 65;
    public static final int GREEN_CREDIT_CREDIBILITY_QUALITY_THRESHOLD_LOW = 45;
    
    // ============================================================================
    // ESG SCORING THRESHOLDS
    // ============================================================================
    public static final double ESG_APPROVAL_THRESHOLD = 7.0;
    public static final double ESG_REVISION_THRESHOLD = 5.0;
    public static final double ESG_MAX_SCORE = 10.0;
    public static final double ESG_MIN_SCORE = 0.0;
    public static final double ESG_MULTIPLIER_EXCELLENT = 1.20; // ESG >= 8.5
    public static final double ESG_MULTIPLIER_GOOD = 1.10;      // ESG >= 7.0
    public static final double ESG_MULTIPLIER_AVERAGE = 1.00;   // ESG >= 5.0
    public static final double ESG_MULTIPLIER_POOR = 0.80;      // ESG < 5.0
    
    // ============================================================================
    // CARBON CALCULATION SETTINGS
    // ============================================================================
    // Emission factors (tCO2e per unit)
    public static final double CARBON_FACTOR_TRANSPORT = 0.0001;  // tCO2e/km
    public static final double CARBON_FACTOR_WASTE = 0.5;         // tCO2e/tonne
    public static final double CARBON_FACTOR_ENERGY = 0.0005;     // tCO2e/kWh
    public static final double CARBON_FACTOR_MATERIAL = 2.0;      // tCO2e/tonne
    public static final double CARBON_FACTOR_WATER = 0.0000003;   // tCO2e/liter
    
    // Baseline emission factors (for avoided emissions calculation)
    public static final double BASELINE_FACTOR_ENERGY = 0.0006;     // tCO2e/kWh
    public static final double BASELINE_FACTOR_TRANSPORT = 0.00012; // tCO2e/km
    public static final double BASELINE_FACTOR_MATERIAL = 0.0015;   // tCO2e/tonne
    public static final double BASELINE_FACTOR_WASTE = 0.0009;      // tCO2e/tonne
    
    // Auto-scale energy units (if value < threshold, treat as MWh not kWh)
    public static final double CARBON_ENERGY_AUTOSCALE_THRESHOLD = 10000.0;
    
    // Carbon risk thresholds (tCO2e)
    public static final double CARBON_RISK_HIGH_THRESHOLD = 50.0;
    public static final double CARBON_RISK_MEDIUM_THRESHOLD = 20.0;
    
    // ============================================================================
    // ML PREDICTION FORMULAS
    // ============================================================================
    // ESG Score Prediction: predictedScore = 9.5 - (totalTco2 / 25.0) - (blockCount * 0.6)
    public static final double ESG_BASE_SCORE = 9.5;
    public static final double ESG_TCO2_DIVISOR = 25.0;
    public static final double ESG_BLOCK_PENALTY = 0.6;
    
    // Credibility Score: credibility = qualityScore - (blockCount * 10)
    public static final int CREDIBILITY_BLOCK_PENALTY = 10;
    
    // ============================================================================
    // FRAUD RISK CALCULATION WEIGHTS
    // ============================================================================
    public static final double FRAUD_WEIGHT_BLOCK = 0.14;
    public static final double FRAUD_WEIGHT_WARN = 0.02;
    public static final double FRAUD_WEIGHT_MISSING_NUMERIC = 0.08;
    public static final double FRAUD_WEIGHT_MISSING_OPTIONAL = 0.015;
    public static final double FRAUD_WEIGHT_CARBON_GAP = 0.10;
    public static final double FRAUD_WEIGHT_HIGH_CARBON = 0.25;      // >= 80 tCO2e
    public static final double FRAUD_WEIGHT_MEDIUM_CARBON = 0.12;    // >= 50 tCO2e
    public static final double FRAUD_WEIGHT_HIGH_DISTANCE = 0.12;    // > 14 km/tonne
    public static final double FRAUD_WEIGHT_MEDIUM_DISTANCE = 0.06;  // > 10 km/tonne
    public static final double FRAUD_WEIGHT_HIGH_EMISSIONS = 0.10;   // > 0.06 tCO2e/kWh
    public static final double FRAUD_WEIGHT_MEDIUM_EMISSIONS = 0.05; // > 0.04 tCO2e/kWh
    
    // Maximum weights
    public static final double FRAUD_MAX_BLOCK_WEIGHT = 0.35;
    public static final double FRAUD_MAX_WARN_WEIGHT = 0.12;
    public static final double FRAUD_MAX_MISSING_NUMERIC_WEIGHT = 0.10;
    public static final double FRAUD_MAX_MISSING_OPTIONAL_WEIGHT = 0.06;
    public static final double FRAUD_MAX_CARBON_GAP_WEIGHT = 0.10;
    
    // Anomaly score formula: anomalyScore = (1.05 * riskScore) + (blockCount * 0.08)
    public static final double ANOMALY_RISK_MULTIPLIER = 1.05;
    public static final double ANOMALY_BLOCK_WEIGHT = 0.08;
    public static final double ANOMALY_MAX_SCORE = 3.0;
    
    // ============================================================================
    // GREEN CREDIT CREDIBILITY ADJUSTMENTS
    // ============================================================================
    // Credibility factors by data quality
    public static final double CREDIBILITY_FACTOR_EXCELLENT = 1.00; // quality >= 85
    public static final double CREDIBILITY_FACTOR_GOOD = 0.85;      // quality >= 65
    public static final double CREDIBILITY_FACTOR_MEDIUM = 0.60;    // quality >= 45
    public static final double CREDIBILITY_FACTOR_LOW = 0.40;       // quality < 45
    
    // Adjustments for low avoided emissions
    public static final double CREDIBILITY_ADJUSTMENT_VERY_LOW = 0.40; // avoided < 1.0
    public static final double CREDIBILITY_ADJUSTMENT_LOW = 0.70;      // avoided < 5.0
    
    // Adjustments for fraud risk
    public static final double CREDIBILITY_ADJUSTMENT_HIGH_FRAUD = 0.70;    // risk >= 0.55
    public static final double CREDIBILITY_ADJUSTMENT_MEDIUM_FRAUD = 0.85;  // risk >= 0.40
    
    // ============================================================================
    // HARD FRAUD SIGNAL THRESHOLDS
    // ============================================================================
    public static final int HARD_FRAUD_CRITICAL_BLOCKS = 3;
    public static final int HARD_FRAUD_MISSING_NUMERIC = 2;
    public static final double HARD_FRAUD_DISTANCE_PER_MATERIAL = 20.0;
    public static final double HARD_FRAUD_EMISSIONS_PER_ENERGY = 0.08;
    public static final double HARD_FRAUD_TOTAL_TCO2 = 80.0;
    
    // ============================================================================
    // API CONFIGURATION
    // ============================================================================
    public static final String ML_API_BASE_URL = "http://127.0.0.1:8001";
    public static final int ML_API_TIMEOUT_SECONDS = 30;
    
    // OpenRouter AI
    public static final String OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1";
    public static final String OPENROUTER_MODEL = "nvidia/nemotron-3-super-120b-a12b:free";
    
    // Gemini AI
    public static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com";
    public static final String GEMINI_MODEL = "gemini-2.5-flash";
    
    // ============================================================================
    // UTILITY METHODS
    // ============================================================================
    
    /**
     * Get ESG multiplier based on score
     */
    public static double getEsgMultiplier(double esgScore) {
        if (esgScore >= 8.5) return ESG_MULTIPLIER_EXCELLENT;
        if (esgScore >= 7.0) return ESG_MULTIPLIER_GOOD;
        if (esgScore >= 5.0) return ESG_MULTIPLIER_AVERAGE;
        return ESG_MULTIPLIER_POOR;
    }
    
    /**
     * Get credibility factor based on data quality
     */
    public static double getCredibilityFactor(double quality) {
        if (quality >= GREEN_CREDIT_CREDIBILITY_QUALITY_THRESHOLD_HIGH) {
            return CREDIBILITY_FACTOR_EXCELLENT;
        } else if (quality >= GREEN_CREDIT_CREDIBILITY_QUALITY_THRESHOLD_MEDIUM) {
            return CREDIBILITY_FACTOR_GOOD;
        } else if (quality >= GREEN_CREDIT_CREDIBILITY_QUALITY_THRESHOLD_LOW) {
            return CREDIBILITY_FACTOR_MEDIUM;
        } else {
            return CREDIBILITY_FACTOR_LOW;
        }
    }
    
    /**
     * Get carbon risk level based on total tCO2e
     */
    public static String getCarbonRisk(double totalTco2) {
        if (totalTco2 >= CARBON_RISK_HIGH_THRESHOLD) {
            return "HIGH";
        } else if (totalTco2 >= CARBON_RISK_MEDIUM_THRESHOLD) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
    
    /**
     * Check if fraud flag should be set
     */
    public static boolean shouldSetFraudFlag(double riskScore, int criticalBlocks, 
                                            int missingNumeric, double distancePerMaterial,
                                            double emissionsPerEnergy, double totalTco2) {
        boolean hardFraudSignal = criticalBlocks >= HARD_FRAUD_CRITICAL_BLOCKS ||
                                  missingNumeric >= HARD_FRAUD_MISSING_NUMERIC ||
                                  distancePerMaterial > HARD_FRAUD_DISTANCE_PER_MATERIAL ||
                                  emissionsPerEnergy > HARD_FRAUD_EMISSIONS_PER_ENERGY ||
                                  totalTco2 >= HARD_FRAUD_TOTAL_TCO2;
        
        return hardFraudSignal || riskScore >= FRAUD_RISK_THRESHOLD;
    }
    
    /**
     * Auto-scale energy value if needed
     */
    public static double autoScaleEnergy(double energy) {
        if (energy > 0 && energy < CARBON_ENERGY_AUTOSCALE_THRESHOLD) {
            return energy * 1000; // Convert MWh to kWh
        }
        return energy;
    }
}
