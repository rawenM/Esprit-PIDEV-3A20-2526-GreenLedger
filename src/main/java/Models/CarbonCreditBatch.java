package Models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a batch of carbon credits issued from a verified project.
 * Each batch tracks the total amount issued and the remaining amount available.
 * 
 * Enhanced with elite traceability features:
 * - Unique serial numbers (CC-YYYY-XXXXXX format)
 * - Verification standards (VCS, Gold Standard, CDM, CAR)
 * - Vintage year tracking
 * - Calculation audit linkage for scientific verification
 * - Batch lineage for splits/subdivisions
 */
public class CarbonCreditBatch {
    
    private int id;
    private int projectId;                // Reference to carbon_projects
    private int walletId;                 // Wallet receiving the credits
    private BigDecimal totalAmount;       // Total credits issued in this batch
    private BigDecimal remainingAmount;   // Credits not yet retired
    private String status;                // AVAILABLE, PARTIALLY_RETIRED, FULLY_RETIRED
    private LocalDateTime issuedAt;

    // Elite traceability fields
    private String serialNumber;          // Unique identifier (CC-2026-001234)
    private String verificationStandard;  // VCS, GOLD_STANDARD, CDM, CAR, etc.
    private Integer vintageYear;          // Year credits were issued/verified
    private String projectCertificationUrl; // Link to verification documentation
    private String calculationAuditId;    // Link to EmissionCalculationAudit
    
    // Batch lineage (for splits/subdivisions)
    private Integer parentBatchId;        // If split from larger batch
    private String lineageJson;           // Child batch IDs as JSON array

    // Optional fields for marketplace display
    private String projectName;           // Name of the project (for display)
    private String verificationStatus;    // Verification standard (e.g., VCS, Gold Standard)

    // Constructors
    public CarbonCreditBatch() {
        this.status = "AVAILABLE";
        this.vintageYear = LocalDateTime.now().getYear();
    }

    public CarbonCreditBatch(int projectId, int walletId, BigDecimal totalAmount) {
        this();
        this.projectId = projectId;
        this.walletId = walletId;
        this.totalAmount = totalAmount;
        this.remainingAmount = totalAmount;
    }

    /**
     * Get display-friendly serial number with formatting.
     * Format: CC-YYYY-XXXXXX (e.g., CC-2026-001234)
     */
    public String getDisplaySerial() {
        return serialNumber != null ? serialNumber : "Pending";
    }
    
    /**
     * Get verification badge for UI display.
     */
    public String getVerificationBadge() {
        if (verificationStandard == null) return "";
        
        return switch (verificationStandard.toUpperCase()) {
            case "VCS" -> "🌿 VCS Verified";
            case "GOLD_STANDARD" -> "🥇 Gold Standard";
            case "CDM" -> "🌍 CDM Certified";
            case "CAR" -> "⭐ CAR Verified";
            default -> "✓ " + verificationStandard;
        };
    }
    
    /**
     * Check if batch has been split into sub-batches.
     */
    public boolean hasLineage() {
        return parentBatchId != null || (lineageJson != null && !lineageJson.isEmpty());
    }
    
    /**
     * Get percentage of batch that has been retired.
     */
    public double getRetirementPercentage() {
        if (totalAmount == null || totalAmount.doubleValue() == 0) {
            return 0.0;
        }
        BigDecimal retired = totalAmount.subtract(remainingAmount != null ? remainingAmount : BigDecimal.ZERO);
        return retired.divide(totalAmount, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public int getWalletId() {
        return walletId;
    }

    public void setWalletId(int walletId) {
        this.walletId = walletId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(BigDecimal remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }
    
    public String getSerialNumber() {
        return serialNumber;
    }
    
    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }
    
    public String getVerificationStandard() {
        return verificationStandard;
    }
    
    public void setVerificationStandard(String verificationStandard) {
        this.verificationStandard = verificationStandard;
    }
    
    public Integer getVintageYear() {
        return vintageYear;
    }
    
    public void setVintageYear(Integer vintageYear) {
        this.vintageYear = vintageYear;
    }
    
    public String getProjectCertificationUrl() {
        return projectCertificationUrl;
    }
    
    public void setProjectCertificationUrl(String projectCertificationUrl) {
        this.projectCertificationUrl = projectCertificationUrl;
    }
    
    public String getCalculationAuditId() {
        return calculationAuditId;
    }
    
    public void setCalculationAuditId(String calculationAuditId) {
        this.calculationAuditId = calculationAuditId;
    }
    
    public Integer getParentBatchId() {
        return parentBatchId;
    }
    
    public void setParentBatchId(Integer parentBatchId) {
        this.parentBatchId = parentBatchId;
    }
    
    public String getLineageJson() {
        return lineageJson;
    }
    
    public void setLineageJson(String lineageJson) {
        this.lineageJson = lineageJson;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getVerificationStatus() {
        return verificationStatus != null ? verificationStatus : "VERIFIED";
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    // Utility methods
    public BigDecimal getRetiredAmount() {
        return totalAmount.subtract(remainingAmount);
    }

    public boolean isFullyRetired() {
        return remainingAmount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isPartiallyRetired() {
        return remainingAmount.compareTo(totalAmount) < 0 && 
               remainingAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    @Override
    public String toString() {
        return String.format("Batch[#%d - %s - %.2f/%.2f remaining]", 
            id, getDisplaySerial(), 
            remainingAmount != null ? remainingAmount.doubleValue() : 0.0, 
            totalAmount != null ? totalAmount.doubleValue() : 0.0);
    }
}
