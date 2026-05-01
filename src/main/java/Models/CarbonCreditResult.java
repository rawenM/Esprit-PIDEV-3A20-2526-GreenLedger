package Models;

/**
 * Carbon Credit Result
 * Result of green credit calculation
 */
public class CarbonCreditResult {
    
    private boolean eligible;
    private double credits;
    private String reason;
    private double avoidedTco2;
    private double credibilityFactor;
    private double esgMultiplier;
    
    private CarbonCreditResult() {
    }
    
    public static CarbonCreditResult success(double credits) {
        CarbonCreditResult result = new CarbonCreditResult();
        result.eligible = true;
        result.credits = credits;
        result.reason = "Eligible for green credits";
        return result;
    }
    
    public static CarbonCreditResult notEligible(String reason) {
        CarbonCreditResult result = new CarbonCreditResult();
        result.eligible = false;
        result.credits = 0.0;
        result.reason = reason;
        return result;
    }
    
    // Getters and Setters
    public boolean isEligible() {
        return eligible;
    }
    
    public void setEligible(boolean eligible) {
        this.eligible = eligible;
    }
    
    public double getCredits() {
        return credits;
    }
    
    public void setCredits(double credits) {
        this.credits = credits;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public double getAvoidedTco2() {
        return avoidedTco2;
    }
    
    public void setAvoidedTco2(double avoidedTco2) {
        this.avoidedTco2 = avoidedTco2;
    }
    
    public double getCredibilityFactor() {
        return credibilityFactor;
    }
    
    public void setCredibilityFactor(double credibilityFactor) {
        this.credibilityFactor = credibilityFactor;
    }
    
    public double getEsgMultiplier() {
        return esgMultiplier;
    }
    
    public void setEsgMultiplier(double esgMultiplier) {
        this.esgMultiplier = esgMultiplier;
    }
    
    @Override
    public String toString() {
        if (eligible) {
            return String.format("CarbonCreditResult[eligible=true, credits=%.2f]", credits);
        } else {
            return String.format("CarbonCreditResult[eligible=false, reason=%s]", reason);
        }
    }
}
