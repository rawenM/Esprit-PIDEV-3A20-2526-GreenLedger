package Models;

/**
 * Enum representing types of events in a batch lifecycle.
 * Used for immutable audit trail and blockchain-ready tracking.
 * 
 * @author GreenLedger Traceability Team
 */
public enum BatchEventType {
    /**
     * Batch was issued from emission calculation
     */
    ISSUED,
    
    /**
     * Batch was split into child batches
     */
    SPLIT,
    
    /**
     * Batch was transferred to another wallet
     */
    TRANSFERRED,
    
    /**
     * Credits from batch were retired
     */
    RETIRED,
    
    /**
     * Batch was sold on marketplace
     */
    MARKETPLACE_SOLD,
    
    /**
     * Multiple batches were merged (future enhancement)
     */
    MERGED,
    
    /**
     * Batch verification status updated
     */
    VERIFIED
}
