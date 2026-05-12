package Services;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * Utility class for converting between human-readable credit amounts and
 * on-chain base units (credits × 10^18, analogous to wei in Ethereum).
 *
 * All arithmetic uses java.math.BigInteger or java.math.BigDecimal.
 * float/double are NEVER used for base-unit amounts.
 *
 * Requirements: 5.3, 8.4, 8.8
 */
public final class CreditUnitConverter {

    /** 10^18 — the multiplier between credits and base units. */
    private static final BigInteger SCALE = BigInteger.TEN.pow(18);
    private static final BigDecimal SCALE_DECIMAL = new BigDecimal(SCALE);

    // Utility class — no instances.
    private CreditUnitConverter() {}

    /**
     * Convert a human-readable credit amount (decimal string) to base units.
     *
     * Examples:
     *   "1"      → "1000000000000000000"
     *   "1000.5" → "1000500000000000000000"
     *   "0"      → "0"
     *
     * The input may contain a decimal point. Fractional digits beyond 18 are
     * truncated (floor), matching Solidity uint256 semantics.
     *
     * @param credits decimal string representation of the credit amount
     * @return base-unit string (non-negative integer, no decimal point)
     * @throws NumberFormatException if the input is not a valid decimal number
     */
    public static String toBaseUnits(String credits) {
        if (credits == null || credits.trim().isEmpty()) {
            throw new NumberFormatException("credits must not be null or empty");
        }
        // Use BigDecimal to handle the decimal point, then scale up.
        BigDecimal bd = new BigDecimal(credits.trim());
        // Multiply by 10^18 and truncate any remaining fractional part.
        BigDecimal scaled = bd.multiply(SCALE_DECIMAL);
        // setScale(0, FLOOR) truncates toward negative infinity; for credit
        // amounts that are always >= 0 this is equivalent to truncation.
        BigInteger result = scaled.setScale(0, RoundingMode.FLOOR).toBigIntegerExact();
        if (result.signum() < 0) {
            throw new IllegalArgumentException("credits must be non-negative, got: " + credits);
        }
        return result.toString();
    }

    /**
     * Convert base units back to a human-readable double.
     *
     * The result is computed with scale 18 (BigDecimal) and then converted to
     * double for display purposes only. Never use the returned double for
     * further on-chain arithmetic.
     *
     * @param baseUnits string representation of the base-unit amount
     * @return credit amount as double
     */
    public static double fromBaseUnits(String baseUnits) {
        if (baseUnits == null || baseUnits.trim().isEmpty()) {
            return 0.0;
        }
        BigDecimal bd = new BigDecimal(baseUnits.trim());
        BigDecimal result = bd.divide(SCALE_DECIMAL, 18, RoundingMode.HALF_UP);
        return result.doubleValue();
    }

    /**
     * Normalize a raw base-unit string by stripping non-digit characters,
     * removing leading zeros, and returning "0" for an empty result.
     *
     * This is useful for sanitizing values read from external sources (e.g.
     * blockchain event data) before passing them to arithmetic methods.
     *
     * @param raw raw string that may contain non-digit characters
     * @return normalized base-unit string ("0" if the result would be empty)
     */
    public static String normalizeBaseUnits(String raw) {
        if (raw == null) {
            return "0";
        }
        // Strip everything that is not a decimal digit.
        String digitsOnly = raw.replaceAll("[^0-9]", "");
        if (digitsOnly.isEmpty()) {
            return "0";
        }
        // Remove leading zeros by parsing through BigInteger.
        BigInteger value = new BigInteger(digitsOnly);
        return value.toString(); // BigInteger.toString() never produces leading zeros
    }

    /**
     * Add two base-unit amounts.
     *
     * @param a first base-unit string
     * @param b second base-unit string
     * @return sum as a base-unit string
     */
    public static String addBaseUnits(String a, String b) {
        BigInteger bigA = parseBigInteger(a);
        BigInteger bigB = parseBigInteger(b);
        return bigA.add(bigB).toString();
    }

    /**
     * Subtract {@code b} from {@code a}, floored at "0".
     *
     * If the mathematical result would be negative the method returns "0"
     * instead, preventing underflow in wallet balance calculations.
     *
     * @param a minuend base-unit string
     * @param b subtrahend base-unit string
     * @return difference as a base-unit string, minimum "0"
     */
    public static String subtractBaseUnits(String a, String b) {
        BigInteger bigA = parseBigInteger(a);
        BigInteger bigB = parseBigInteger(b);
        BigInteger result = bigA.subtract(bigB);
        if (result.signum() < 0) {
            return "0";
        }
        return result.toString();
    }

    /**
     * Compare two base-unit amounts.
     *
     * @param a first base-unit string
     * @param b second base-unit string
     * @return negative if a &lt; b, zero if a == b, positive if a &gt; b
     */
    public static int compareBaseUnits(String a, String b) {
        BigInteger bigA = parseBigInteger(a);
        BigInteger bigB = parseBigInteger(b);
        return bigA.compareTo(bigB);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Parse a base-unit string into a BigInteger, treating null/empty as zero.
     */
    private static BigInteger parseBigInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BigInteger.ZERO;
        }
        return new BigInteger(value.trim());
    }
}
