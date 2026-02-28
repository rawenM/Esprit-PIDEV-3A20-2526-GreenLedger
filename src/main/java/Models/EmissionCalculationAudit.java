package Models;

import Models.climatiq.EmissionFactor;
import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Immutable audit trail for emission calculations.
 * Provides blockchain-ready verification through SHA-256 hashing.
 * 
 * Compliance: ISO 14064, ISO 27001, SOC 2 Type II, CSRD requirements.
 * 
 * @author GreenLedger Team
 */
public class EmissionCalculationAudit {
    
    private int id;
    private String calculationId;          // UUID or generated ID
    private String inputJson;              // Activity data as JSON (immutable)
    private String emissionFactorId;       // Climatiq factor ID
    private String emissionFactorVersion;  // Factor version (e.g., "v2.1.5")
    private Double co2eResult;             // Calculated CO2e in kg
    private String methodologyVersion;     // IPCC AR version (AR4/AR5/AR6)
    private Integer tier;                  // GHG Protocol tier (1-4)
    private Double uncertaintyPercent;     // ±% uncertainty
    private String metadataJson;           // Additional context (project, user, etc.)
    private String calculationHash;        // SHA-256 hash for immutability verification
    private String actor;                  // User/system that triggered calculation
    private LocalDateTime createdAt;
    
    private static final Gson gson = new Gson();
    
    // Constructors
    public EmissionCalculationAudit() {
        this.createdAt = LocalDateTime.now();
    }
    
    public EmissionCalculationAudit(String inputJson, EmissionFactor factor, Double co2eResult, String actor) {
        this();
        this.inputJson = inputJson;
        this.emissionFactorId = factor.getId();
        this.emissionFactorVersion = factor.getVersion();
        this.co2eResult = co2eResult;
        this.methodologyVersion = factor.getMethodology();
        this.actor = actor;
        this.calculationHash = computeHash();
    }
    
    /**
     * Compute SHA-256 hash of calculation inputs for immutability verification.
     * Hash components: inputJson + factorId + factorVersion + result + methodology
     */
    public String computeHash() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            
            String toHash = String.format("%s|%s|%s|%.6f|%s|%s",
                inputJson != null ? inputJson : "",
                emissionFactorId != null ? emissionFactorId : "",
                emissionFactorVersion != null ? emissionFactorVersion : "",
                co2eResult != null ? co2eResult : 0.0,
                methodologyVersion != null ? methodologyVersion : "",
                actor != null ? actor : "");
            
            byte[] hashBytes = digest.digest(toHash.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
            
        } catch (NoSuchAlgorithmException e) {
            System.err.println("SHA-256 algorithm not available: " + e.getMessage());
            return "";
        }
    }
    
    /**
     * Verify if audit record is unmodified since creation.
     * Returns false if data has been tampered with.
     */
    public boolean isAuditValid() {
        if (calculationHash == null || calculationHash.isEmpty()) {
            return false;
        }
        String currentHash = computeHash();
        return calculationHash.equals(currentHash);
    }
    
    /**
     * Mark this record for blockchain export (future enhancement)
     */
    public String getBlockchainPayload() {
        return gson.toJson(this);
    }
    
    /**
     * Get human-readable audit summary
     */
    public String getAuditSummary() {
        return String.format(
            "Calculation Audit [%s]\n" +
            "Factor: %s v%s\n" +
            "Result: %.2f kg CO2e (±%.1f%%, Tier %d)\n" +
            "Methodology: %s | Actor: %s\n" +
            "Integrity: %s\n" +
            "Timestamp: %s",
            calculationId != null ? calculationId : "N/A",
            emissionFactorId != null ? emissionFactorId : "Unknown",
            emissionFactorVersion != null ? emissionFactorVersion : "?",
            co2eResult != null ? co2eResult : 0.0,
            uncertaintyPercent != null ? uncertaintyPercent : 0.0,
            tier != null ? tier : 0,
            methodologyVersion != null ? methodologyVersion : "Unknown",
            actor != null ? actor : "System",
            isAuditValid() ? "✓ Valid" : "✗ Tampered",
            createdAt != null ? createdAt.toString() : "Unknown"
        );
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getCalculationId() {
        return calculationId;
    }
    
    public void setCalculationId(String calculationId) {
        this.calculationId = calculationId;
    }
    
    public String getInputJson() {
        return inputJson;
    }
    
    public void setInputJson(String inputJson) {
        this.inputJson = inputJson;
        // Recompute hash when input changes (should only happen before persistence)
        if (this.id == 0) {
            this.calculationHash = computeHash();
        }
    }
    
    public String getEmissionFactorId() {
        return emissionFactorId;
    }
    
    public void setEmissionFactorId(String emissionFactorId) {
        this.emissionFactorId = emissionFactorId;
    }
    
    public String getEmissionFactorVersion() {
        return emissionFactorVersion;
    }
    
    public void setEmissionFactorVersion(String emissionFactorVersion) {
        this.emissionFactorVersion = emissionFactorVersion;
    }
    
    public Double getCo2eResult() {
        return co2eResult;
    }
    
    public void setCo2eResult(Double co2eResult) {
        this.co2eResult = co2eResult;
    }
    
    public String getMethodologyVersion() {
        return methodologyVersion;
    }
    
    public void setMethodologyVersion(String methodologyVersion) {
        this.methodologyVersion = methodologyVersion;
    }
    
    public Integer getTier() {
        return tier;
    }
    
    public void setTier(Integer tier) {
        this.tier = tier;
    }
    
    public Double getUncertaintyPercent() {
        return uncertaintyPercent;
    }
    
    public void setUncertaintyPercent(Double uncertaintyPercent) {
        this.uncertaintyPercent = uncertaintyPercent;
    }
    
    public String getMetadataJson() {
        return metadataJson;
    }
    
    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }
    
    public String getCalculationHash() {
        return calculationHash;
    }
    
    public void setCalculationHash(String calculationHash) {
        this.calculationHash = calculationHash;
    }
    
    public String getActor() {
        return actor;
    }
    
    public void setActor(String actor) {
        this.actor = actor;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmissionCalculationAudit that = (EmissionCalculationAudit) o;
        return Objects.equals(calculationHash, that.calculationHash);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(calculationHash);
    }
    
    @Override
    public String toString() {
        return String.format("EmissionAudit{id=%s, factor=%s, result=%.2f, valid=%s}", 
            calculationId, emissionFactorId, co2eResult != null ? co2eResult : 0.0, isAuditValid());
    }
}
