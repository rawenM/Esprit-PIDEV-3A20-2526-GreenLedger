package Models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a batch of carbon credits issued from a verified project.
 * Each batch tracks the total amount issued and the remaining amount available.
 */
public class CarbonCreditBatch {
    
    private int id;
    private int projectId;                // Reference to carbon_projects
    private int walletId;                 // Wallet receiving the credits
    private BigDecimal totalAmount;       // Total credits issued in this batch
    private BigDecimal remainingAmount;   // Credits not yet retired
    private String status;                // AVAILABLE, PARTIALLY_RETIRED, FULLY_RETIRED
    private LocalDateTime issuedAt;

    // Optional fields for marketplace display
    private String projectName;           // Name of the project (for display)
    private String verificationStatus;    // Verification standard (e.g., VCS, Gold Standard)

    // Constructors
    public CarbonCreditBatch() {
        this.status = "AVAILABLE";
    }

    public CarbonCreditBatch(int projectId, int walletId, BigDecimal totalAmount) {
        this();
        this.projectId = projectId;
        this.walletId = walletId;
        this.totalAmount = totalAmount;
        this.remainingAmount = totalAmount;
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
        return String.format("Batch[#%d - Project: %d - %.2f/%.2f remaining]", 
            id, projectId, remainingAmount, totalAmount);
    }
}
