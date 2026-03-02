package Models;

/**
 * Enum representing the type of carbon credit batch.
 * 
 * PRIMARY: Original batch issued directly from emission calculations
 * SECONDARY: Derived batch created through marketplace sales, splits, or transfers
 * 
 * @author GreenLedger Traceability Team
 */
public enum BatchType {
    /**
     * Original batch issued from verified emission calculations
     */
    PRIMARY,
    
    /**
     * Derived batch created from marketplace transactions or batch splits
     */
    SECONDARY
}
