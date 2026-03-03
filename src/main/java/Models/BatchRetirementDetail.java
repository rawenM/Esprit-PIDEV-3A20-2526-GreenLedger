package Models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Links retirement transactions to specific carbon credit batches.
 * 
 * When credits are retired (FIFO), this table tracks which specific batches
 * were consumed, enabling complete traceability of retired credits.
 * 
 * Example: If retiring 150 tCO2 and FIFO selects 3 batches:
 * - Batch A: 100 tCO2 retired
 * - Batch B: 30 tCO2 retired
 * - Batch C: 20 tCO2 retired
 * 
 * Three BatchRetirementDetail records are created, all linked to the same transaction_id.
 * 
 * @author GreenLedger Traceability Team
 */
public class BatchRetirementDetail {
    
    private long id;
    private long transactionId;         // Reference to wallet_transactions
    private int batchId;                // Specific batch retired from
    private BigDecimal amountRetired;   // Amount retired from this batch
    private LocalDateTime createdAt;
    
    // Constructors
    public BatchRetirementDetail() {
        this.createdAt = LocalDateTime.now();
    }
    
    public BatchRetirementDetail(long transactionId, int batchId, BigDecimal amountRetired) {
        this();
        this.transactionId = transactionId;
        this.batchId = batchId;
        this.amountRetired = amountRetired;
    }
    
    public BatchRetirementDetail(long transactionId, int batchId, double amountRetired) {
        this(transactionId, batchId, BigDecimal.valueOf(amountRetired));
    }
    
    /**
     * Get human-readable summary
     */
    public String getSummary() {
        return String.format(
            "Retirement Detail [%d]\n" +
            "Transaction: %d | Batch: %d\n" +
            "Amount Retired: %s tCO2\n" +
            "Timestamp: %s",
            id,
            transactionId,
            batchId,
            amountRetired != null ? amountRetired.toPlainString() : "0.00",
            createdAt != null ? createdAt.toString() : "Unknown"
        );
    }
    
    // Getters and Setters
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public long getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(long transactionId) {
        this.transactionId = transactionId;
    }
    
    public int getBatchId() {
        return batchId;
    }
    
    public void setBatchId(int batchId) {
        this.batchId = batchId;
    }
    
    public BigDecimal getAmountRetired() {
        return amountRetired;
    }
    
    public void setAmountRetired(BigDecimal amountRetired) {
        this.amountRetired = amountRetired;
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
        BatchRetirementDetail that = (BatchRetirementDetail) o;
        return id == that.id;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("BatchRetirementDetail{id=%d, transactionId=%d, batchId=%d, amount=%s}",
            id, transactionId, batchId, amountRetired);
    }
}
