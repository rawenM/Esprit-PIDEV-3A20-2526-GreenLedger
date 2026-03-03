package Models.climatiq;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents the result of an emission calculation with uncertainty quantification.
 * Implements GHG Protocol Tier system (1-4) for data quality transparency.
 */
public class EmissionResult {
    
    private BigDecimal co2eAmount;         // Best estimate in kg CO2e
    private BigDecimal lowerBound;         // Lower confidence bound
    private BigDecimal upperBound;         // Upper confidence bound
    private Integer tier;                  // GHG Protocol tier (1=measured, 4=estimated)
    private Double uncertaintyPercent;     // ±% uncertainty
    private String calculationId;          // Reference to audit trail
    private EmissionFactor emissionFactor; // Factor used for calculation
    private LocalDateTime calculatedAt;
    
    // Activity data
    private Double activityAmount;         // Quantity of activity (1000 kWh, 500 km, etc.)
    private String activityUnit;           // Unit of activity
    private String activityDescription;    // Human-readable description
    private String scope;                  // Scope classification (Scope 1/2/3)
    
    // Constructors
    public EmissionResult() {
        this.calculatedAt = LocalDateTime.now();
    }
    
    public EmissionResult(BigDecimal co2eAmount, Integer tier, Double uncertaintyPercent) {
        this();
        this.co2eAmount = co2eAmount;
        this.tier = tier;
        this.uncertaintyPercent = uncertaintyPercent;
        calculateBounds();
    }
    
    /**
     * Calculate confidence bounds based on uncertainty percentage
     */
    public void calculateBounds() {
        if (co2eAmount != null && uncertaintyPercent != null) {
            BigDecimal uncertainty = co2eAmount.multiply(
                BigDecimal.valueOf(uncertaintyPercent / 100.0)
            );
            this.lowerBound = co2eAmount.subtract(uncertainty);
            this.upperBound = co2eAmount.add(uncertainty);
        }
    }
    
    /**
     * Get tier description according to GHG Protocol
     */
    public String getTierDescription() {
        if (tier == null) return "Unknown";
        return switch (tier) {
            case 1 -> "Measured Data (±5% confidence)";
            case 2 -> "Industry Samples (±15% confidence)";
            case 3 -> "Engineering Calculations (±30% confidence)";
            case 4 -> "Spend-based Estimates (±50% confidence)";
            default -> "Unknown Tier";
        };
    }
    
    /**
     * Get default uncertainty for tier
     */
    public static double getDefaultUncertaintyForTier(int tier) {
        return switch (tier) {
            case 1 -> 5.0;
            case 2 -> 15.0;
            case 3 -> 30.0;
            case 4 -> 50.0;
            default -> 100.0;
        };
    }
    
    /**
     * Create a zero emission result for use as reduce initial value
     */
    public static EmissionResult zero() {
        return new EmissionResult(BigDecimal.ZERO, 4, 100.0);
    }
    
    // Getters and Setters
    public BigDecimal getCo2eAmount() {
        return co2eAmount;
    }
    
    public void setCo2eAmount(BigDecimal co2eAmount) {
        this.co2eAmount = co2eAmount;
        calculateBounds();
    }
    
    public BigDecimal getLowerBound() {
        return lowerBound;
    }
    
    public void setLowerBound(BigDecimal lowerBound) {
        this.lowerBound = lowerBound;
    }
    
    public BigDecimal getUpperBound() {
        return upperBound;
    }
    
    public void setUpperBound(BigDecimal upperBound) {
        this.upperBound = upperBound;
    }
    
    public Integer getTier() {
        return tier;
    }
    
    public void setTier(Integer tier) {
        this.tier = tier;
        // Update uncertainty based on tier if not explicitly set
        if (uncertaintyPercent == null) {
            this.uncertaintyPercent = getDefaultUncertaintyForTier(tier);
            calculateBounds();
        }
    }
    
    public Double getUncertaintyPercent() {
        return uncertaintyPercent;
    }
    
    public void setUncertaintyPercent(Double uncertaintyPercent) {
        this.uncertaintyPercent = uncertaintyPercent;
        calculateBounds();
    }
    
    public String getCalculationId() {
        return calculationId;
    }
    
    public void setCalculationId(String calculationId) {
        this.calculationId = calculationId;
    }
    
    public EmissionFactor getEmissionFactor() {
        return emissionFactor;
    }
    
    public void setEmissionFactor(EmissionFactor emissionFactor) {
        this.emissionFactor = emissionFactor;
    }
    
    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }
    
    public void setCalculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }
    
    public Double getActivityAmount() {
        return activityAmount;
    }
    
    public void setActivityAmount(Double activityAmount) {
        this.activityAmount = activityAmount;
    }
    
    public String getActivityUnit() {
        return activityUnit;
    }
    
    public void setActivityUnit(String activityUnit) {
        this.activityUnit = activityUnit;
    }
    
    public String getActivityDescription() {
        return activityDescription;
    }
    
    public void setActivityDescription(String activityDescription) {
        this.activityDescription = activityDescription;
    }
    
    /**
     * Get methodology - returns tier description or emission factor methodology
     */
    public String getMethodology() {
        if (emissionFactor != null && emissionFactor.getMethodology() != null) {
            return emissionFactor.getMethodology();
        }
        return getTierDescription();
    }
    
    /**
     * Set methodology on the emission factor
     */
    public void setMethodology(String methodology) {
        if (emissionFactor == null) {
            emissionFactor = new EmissionFactor();
        }
        emissionFactor.setMethodology(methodology);
    }
    
    /**
     * Get scope description
     */
    public String getScope() {
        return scope;
    }
    
    /**
     * Set scope classification (Scope 1/2/3)
     */
    public void setScope(String scope) {
        this.scope = scope;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmissionResult that = (EmissionResult) o;
        return Objects.equals(calculationId, that.calculationId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(calculationId);
    }
    
    @Override
    public String toString() {
        return String.format("EmissionResult{co2e=%.2f kg (±%.1f%%), tier=%d, activity=%.2f %s}", 
            co2eAmount != null ? co2eAmount.doubleValue() : 0.0,
            uncertaintyPercent != null ? uncertaintyPercent : 0.0,
            tier != null ? tier : 0,
            activityAmount != null ? activityAmount : 0.0,
            activityUnit != null ? activityUnit : "units");
    }
}
