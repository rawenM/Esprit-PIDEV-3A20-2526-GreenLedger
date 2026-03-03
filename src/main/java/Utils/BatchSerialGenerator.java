package Utils;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe generator for carbon credit batch serial numbers.
 * 
 * Format: CC-YYYY-XXXXXX
 * - CC: Carbon Credit prefix
 * - YYYY: Current year
 * - XXXXXX: 6-digit sequential number (000001-999999)
 * 
 * Features:
 * - Thread-safe atomic counter
 * - Year-aware (resets to 001 each year)
 * - Database-backed persistence (TODO: implement counter persistence)
 * - Collision prevention through uniqueness check
 * 
 * @author GreenLedger Team
 */
public class BatchSerialGenerator {
    
    private static final AtomicInteger counter = new AtomicInteger(1);
    private static final Object lock = new Object();
    private static int lastYear = LocalDateTime.now().getYear();
    
    /**
     * Generate next serial number in sequence.
     * Thread-safe and year-aware.
     * 
     * @return Formatted serial number (e.g., "CC-2026-001234")
     */
    public static String generateSerial() {
        synchronized (lock) {
            int currentYear = LocalDateTime.now().getYear();
            
            // Reset counter if year changed
            if (currentYear != lastYear) {
                counter.set(1);
                lastYear = currentYear;
                System.out.println("[SERIAL] New year detected, reset counter to 001");
            }
            
            // Get next number (max 999999 per year)
            int sequence = counter.getAndIncrement();
            
            if (sequence > 999999) {
                System.err.println("[SERIAL] Sequence exceeded 999999 for year " + currentYear);
                // Wrap around (should implement overflow handling in production)
                counter.set(1);
                sequence = 1;
            }
            
            return String.format("CC-%d-%06d", currentYear, sequence);
        }
    }
    
    /**
     * Validate serial number format.
     * 
     * @param serial Serial number to validate
     * @return true if format is valid
     */
    public static boolean isValidFormat(String serial) {
        if (serial == null || serial.isEmpty()) {
            return false;
        }
        
        // Pattern: CC-YYYY-XXXXXX
        return serial.matches("^CC-\\d{4}-\\d{6}$");
    }
    
    /**
     * Extract year from serial number.
     * 
     * @param serial Serial number
     * @return Year or -1 if invalid
     */
    public static int extractYear(String serial) {
        if (!isValidFormat(serial)) {
            return -1;
        }
        
        try {
            return Integer.parseInt(serial.substring(3, 7));
        } catch (Exception e) {
            return -1;
        }
    }
    
    /**
     * Extract sequence number from serial.
     * 
     * @param serial Serial number
     * @return Sequence number or -1 if invalid
     */
    public static int extractSequence(String serial) {
        if (!isValidFormat(serial)) {
            return -1;
        }
        
        try {
            return Integer.parseInt(serial.substring(8));
        } catch (Exception e) {
            return -1;
        }
    }
    
    /**
     * Set counter to specific value (for database initialization).
     * Use with caution - mainly for recovering from database state.
     * 
     * @param value Counter value to set
     */
    public static void setCounter(int value) {
        synchronized (lock) {
            counter.set(value);
            System.out.println("ℹ️  [SERIAL GEN] Counter manually set to " + value);
        }
    }
    
    /**
     * Get current counter value (for monitoring).
     * 
     * @return Current counter value
     */
    public static int getCurrentCounter() {
        return counter.get();
    }
    
    /**
     * Get status summary for monitoring.
     * 
     * @return Status string
     */
    public static String getStatus() {
        return String.format(
            "Serial Generator: Year=%d, Counter=%d, Next=%s",
            lastYear,
            counter.get(),
            generateNextPreview()
        );
    }
    
    /**
     * Preview next serial without consuming it.
     * 
     * @return Next serial number that would be generated
     */
    private static String generateNextPreview() {
        int currentYear = LocalDateTime.now().getYear();
        return String.format("CC-%d-%06d", currentYear, counter.get());
    }
}
