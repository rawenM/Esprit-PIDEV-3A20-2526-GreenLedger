package Services;

import DataBase.MyConnection;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages {@code marketplace_listings} records, including listing creation,
 * retrieval, and atomic amount reservation / release using big-integer arithmetic.
 *
 * <p>Design invariants:
 * <ul>
 *   <li>All amount arithmetic uses {@link CreditUnitConverter} — never {@code double}
 *       or {@code float} for base-unit amounts.</li>
 *   <li>Reservation and release operations use {@code SELECT … FOR UPDATE} inside an
 *       explicit JDBC transaction to prevent concurrent double-spending.</li>
 *   <li>All database access uses {@link MyConnection#getConnection()} (plain JDBC,
 *       no DI framework). Connections, statements, and result-sets are always closed
 *       in {@code finally} blocks.</li>
 * </ul>
 *
 * Requirements: 8.1 – 8.8
 */
public class MarketplaceService {

    // =========================================================================
    // Sub-task 10.1 — createListing
    // =========================================================================

    /**
     * Create a new marketplace listing.
     *
     * <p>Required payload keys (must be present and non-zero):
     * <ul>
     *   <li>{@code seller_id} — seller user ID</li>
     *   <li>{@code seller_wallet_id} — seller wallet ID</li>
     *   <li>{@code batch_id} — carbon credit batch ID</li>
     *   <li>{@code total_amount_base_units} — total amount in base units (string)</li>
     * </ul>
     *
     * <p>Optional payload keys:
     * {@code asset_type}, {@code wallet_id}, {@code quantity_or_id},
     * {@code price_per_unit}, {@code currency_code}.
     *
     * @param payload map of listing fields
     * @return the newly inserted listing row as a {@code Map<String,Object>}
     * @throws IllegalArgumentException if any required field is missing or zero
     * @throws RuntimeException         on database error
     * Requirements: 8.1
     */
    public Map<String, Object> createListing(Map<String, Object> payload) {
        // ── Validation ────────────────────────────────────────────────────────
        validateRequiredField(payload, "seller_id");
        validateRequiredField(payload, "seller_wallet_id");
        validateRequiredField(payload, "batch_id");
        validateRequiredStringField(payload, "total_amount_base_units");

        int    sellerId          = toInt(payload.get("seller_id"));
        int    sellerWalletId    = toInt(payload.get("seller_wallet_id"));
        int    batchId           = toInt(payload.get("batch_id"));
        String totalAmountBase   = objectToString(payload.get("total_amount_base_units"));

        if (sellerId == 0) {
            throw new IllegalArgumentException("seller_id must be non-zero");
        }
        if (sellerWalletId == 0) {
            throw new IllegalArgumentException("seller_wallet_id must be non-zero");
        }
        if (batchId == 0) {
            throw new IllegalArgumentException("batch_id must be non-zero");
        }
        if ("0".equals(CreditUnitConverter.normalizeBaseUnits(totalAmountBase))) {
            throw new IllegalArgumentException("total_amount_base_units must be non-zero");
        }

        // ── Optional fields ───────────────────────────────────────────────────
        String assetType    = objectToString(payload.get("asset_type"));
        Integer walletId    = toIntOrNull(payload.get("wallet_id"));
        Object  qtyOrId     = payload.get("quantity_or_id");
        Object  pricePerUnit = payload.get("price_per_unit");
        String  currencyCode = objectToString(payload.get("currency_code"));

        // ── INSERT ────────────────────────────────────────────────────────────
        String sql =
            "INSERT INTO marketplace_listings " +
            "  (seller_id, seller_wallet_id, batch_id, asset_type, wallet_id, " +
            "   quantity_or_id, price_per_unit, currency_code, status, " +
            "   total_amount_base_units, reserved_amount_base_units, " +
            "   filled_amount_base_units, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, '0', '0', NOW(), NOW())";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet generatedKeys = null;
        try {
            conn = MyConnection.getConnection();
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            ps.setInt(1, sellerId);
            ps.setInt(2, sellerWalletId);
            ps.setInt(3, batchId);
            ps.setString(4, assetType);
            if (walletId != null) {
                ps.setInt(5, walletId);
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            if (qtyOrId != null) {
                ps.setObject(6, qtyOrId);
            } else {
                ps.setNull(6, Types.DECIMAL);
            }
            if (pricePerUnit != null) {
                ps.setObject(7, pricePerUnit);
            } else {
                ps.setNull(7, Types.DECIMAL);
            }
            ps.setString(8, currencyCode);
            ps.setString(9, totalAmountBase);

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new RuntimeException("INSERT into marketplace_listings returned 0 rows affected");
            }

            generatedKeys = ps.getGeneratedKeys();
            if (!generatedKeys.next()) {
                throw new RuntimeException("INSERT into marketplace_listings: no generated key returned");
            }
            int newId = generatedKeys.getInt(1);

            return getListing(newId);

        } catch (SQLException e) {
            throw new RuntimeException("DB error in createListing: " + e.getMessage(), e);
        } finally {
            closeQuietly(generatedKeys);
            closeQuietly(ps);
            closeQuietly(conn);
        }
    }

    // =========================================================================
    // Sub-task 10.2 — getListing
    // =========================================================================

    /**
     * Fetch a marketplace listing by primary key.
     *
     * @param listingId primary key of the listing
     * @return listing row as {@code Map<String,Object>}, or {@code null} if not found
     * @throws RuntimeException on database error
     * Requirements: 8.2
     */
    public Map<String, Object> getListing(int listingId) {
        String sql = "SELECT * FROM marketplace_listings WHERE id = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = MyConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, listingId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return resultSetRowToMap(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("DB error in getListing: " + e.getMessage(), e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
    }

    // =========================================================================
    // Sub-task 10.3 — reserveListingAmount
    // =========================================================================

    /**
     * Atomically reserve an amount from a marketplace listing.
     *
     * <p>Uses a JDBC transaction with {@code SELECT … FOR UPDATE} to prevent
     * concurrent double-spending. The reservation expires after
     * {@code reservationMinutes} minutes.
     *
     * @param listingId          primary key of the listing
     * @param amountBaseUnits    amount to reserve (base-unit string)
     * @param reservationMinutes minutes until the reservation expires
     * @throws RuntimeException if the listing is not found, or if available
     *                          amount is insufficient
     * Requirements: 8.3, 8.4, 8.5, 8.6, 8.8
     */
    public void reserveListingAmount(int listingId, String amountBaseUnits, int reservationMinutes) {
        Connection conn = null;
        PreparedStatement psSelect = null;
        PreparedStatement psUpdate = null;
        ResultSet rs = null;
        try {
            conn = MyConnection.getConnection();
            conn.setAutoCommit(false);

            // ── Lock the row ──────────────────────────────────────────────────
            psSelect = conn.prepareStatement(
                "SELECT * FROM marketplace_listings WHERE id = ? FOR UPDATE"
            );
            psSelect.setInt(1, listingId);
            rs = psSelect.executeQuery();

            if (!rs.next()) {
                rollbackQuietly(conn);
                throw new RuntimeException("Listing not found: id=" + listingId);
            }

            Map<String, Object> listing = resultSetRowToMap(rs);
            closeQuietly(rs);
            rs = null;

            // ── Compute available = total - filled - reserved ─────────────────
            String total    = normalizeAmount(listing.get("total_amount_base_units"));
            String filled   = normalizeAmount(listing.get("filled_amount_base_units"));
            String reserved = normalizeAmount(listing.get("reserved_amount_base_units"));

            // available = total - filled - reserved
            String available = CreditUnitConverter.subtractBaseUnits(
                CreditUnitConverter.subtractBaseUnits(total, filled),
                reserved
            );

            // ── Insufficient check ────────────────────────────────────────────
            if (CreditUnitConverter.compareBaseUnits(available, amountBaseUnits) < 0) {
                rollbackQuietly(conn);
                throw new RuntimeException("Insufficient available amount");
            }

            // ── Compute new reserved amount ───────────────────────────────────
            String reservedAfter = CreditUnitConverter.addBaseUnits(reserved, amountBaseUnits);

            // ── Determine new status ──────────────────────────────────────────
            // RESERVED if reservedAfter >= total (full amount reserved), else PARTIALLY_RESERVED
            String newStatus;
            if (CreditUnitConverter.compareBaseUnits(reservedAfter, total) >= 0) {
                newStatus = "RESERVED";
            } else {
                newStatus = "PARTIALLY_RESERVED";
            }

            // ── UPDATE ────────────────────────────────────────────────────────
            psUpdate = conn.prepareStatement(
                "UPDATE marketplace_listings " +
                "SET reserved_amount_base_units = ?, " +
                "    reservation_expires_at = DATE_ADD(NOW(), INTERVAL ? MINUTE), " +
                "    status = ?, " +
                "    updated_at = NOW() " +
                "WHERE id = ?"
            );
            psUpdate.setString(1, reservedAfter);
            psUpdate.setInt(2, reservationMinutes);
            psUpdate.setString(3, newStatus);
            psUpdate.setInt(4, listingId);
            psUpdate.executeUpdate();

            conn.commit();

        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new RuntimeException("DB error in reserveListingAmount: " + e.getMessage(), e);
        } finally {
            closeQuietly(rs);
            closeQuietly(psUpdate);
            closeQuietly(psSelect);
            closeQuietly(conn);
        }
    }

    // =========================================================================
    // Sub-task 10.4 — releaseReservation
    // =========================================================================

    /**
     * Atomically release a previously reserved amount from a marketplace listing.
     *
     * <p>Uses a JDBC transaction with {@code SELECT … FOR UPDATE}. The
     * {@code reserved_amount_base_units} is decremented by {@code amountBaseUnits}
     * (floored at {@code "0"}). Status is set to {@code FILLED} if
     * {@code filled_amount_base_units >= total_amount_base_units}, otherwise
     * {@code ACTIVE}.
     *
     * @param listingId       primary key of the listing
     * @param amountBaseUnits amount to release (base-unit string)
     * @throws RuntimeException if the listing is not found or a DB error occurs
     * Requirements: 8.7, 8.8
     */
    public void releaseReservation(int listingId, String amountBaseUnits) {
        Connection conn = null;
        PreparedStatement psSelect = null;
        PreparedStatement psUpdate = null;
        ResultSet rs = null;
        try {
            conn = MyConnection.getConnection();
            conn.setAutoCommit(false);

            // ── Lock the row ──────────────────────────────────────────────────
            psSelect = conn.prepareStatement(
                "SELECT * FROM marketplace_listings WHERE id = ? FOR UPDATE"
            );
            psSelect.setInt(1, listingId);
            rs = psSelect.executeQuery();

            if (!rs.next()) {
                rollbackQuietly(conn);
                throw new RuntimeException("Listing not found: id=" + listingId);
            }

            Map<String, Object> listing = resultSetRowToMap(rs);
            closeQuietly(rs);
            rs = null;

            // ── Compute new reserved amount (floor at "0") ────────────────────
            String reserved      = normalizeAmount(listing.get("reserved_amount_base_units"));
            String reservedAfter = CreditUnitConverter.subtractBaseUnits(reserved, amountBaseUnits);

            // ── Determine new status ──────────────────────────────────────────
            String total  = normalizeAmount(listing.get("total_amount_base_units"));
            String filled = normalizeAmount(listing.get("filled_amount_base_units"));

            // FILLED if filled >= total, else ACTIVE
            String newStatus;
            if (CreditUnitConverter.compareBaseUnits(filled, total) >= 0) {
                newStatus = "FILLED";
            } else {
                newStatus = "ACTIVE";
            }

            // ── UPDATE ────────────────────────────────────────────────────────
            psUpdate = conn.prepareStatement(
                "UPDATE marketplace_listings " +
                "SET reserved_amount_base_units = ?, " +
                "    status = ?, " +
                "    updated_at = NOW() " +
                "WHERE id = ?"
            );
            psUpdate.setString(1, reservedAfter);
            psUpdate.setString(2, newStatus);
            psUpdate.setInt(3, listingId);
            psUpdate.executeUpdate();

            conn.commit();

        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new RuntimeException("DB error in releaseReservation: " + e.getMessage(), e);
        } finally {
            closeQuietly(rs);
            closeQuietly(psUpdate);
            closeQuietly(psSelect);
            closeQuietly(conn);
        }
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    /**
     * Validate that a numeric field is present in the payload and non-null.
     *
     * @param payload   the payload map
     * @param fieldName the field name to check
     * @throws IllegalArgumentException if the field is absent or null
     */
    private void validateRequiredField(Map<String, Object> payload, String fieldName) {
        if (!payload.containsKey(fieldName) || payload.get(fieldName) == null) {
            throw new IllegalArgumentException("Required field missing or null: " + fieldName);
        }
    }

    /**
     * Validate that a string field is present, non-null, and non-empty.
     *
     * @param payload   the payload map
     * @param fieldName the field name to check
     * @throws IllegalArgumentException if the field is absent, null, or empty
     */
    private void validateRequiredStringField(Map<String, Object> payload, String fieldName) {
        Object value = payload.get(fieldName);
        if (value == null || value.toString().trim().isEmpty()) {
            throw new IllegalArgumentException("Required field missing or empty: " + fieldName);
        }
    }

    /**
     * Normalize a base-unit amount from a map value, treating null as {@code "0"}.
     *
     * @param value raw value from the result-set map
     * @return normalized base-unit string
     */
    private String normalizeAmount(Object value) {
        if (value == null) return "0";
        return CreditUnitConverter.normalizeBaseUnits(value.toString());
    }

    /**
     * Map the current row of a {@link ResultSet} to a {@code Map<String,Object>}.
     * Column labels are used as keys; SQL NULL becomes Java {@code null}.
     */
    private Map<String, Object> resultSetRowToMap(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();
        Map<String, Object> row = new HashMap<>(cols * 2);
        for (int i = 1; i <= cols; i++) {
            row.put(meta.getColumnLabel(i), rs.getObject(i));
        }
        return row;
    }

    /** Convert an Object to int (0 if null or not a Number). */
    private int toInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt(((String) value).trim());
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    /** Convert an Object to Integer (null if null or not parseable). */
    private Integer toIntOrNull(Object value) {
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt(((String) value).trim());
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    /** Convert an Object to String (null if null). */
    private String objectToString(Object value) {
        if (value == null) return null;
        return value.toString();
    }

    /** Rollback a connection silently. */
    private void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try { conn.rollback(); } catch (SQLException ignored) {}
        }
    }

    /** Close a {@link Connection} silently. */
    private static void closeQuietly(Connection c) {
        if (c != null) {
            try { c.close(); } catch (SQLException ignored) {}
        }
    }

    /** Close a {@link Statement} (or {@link PreparedStatement}) silently. */
    private static void closeQuietly(Statement s) {
        if (s != null) {
            try { s.close(); } catch (SQLException ignored) {}
        }
    }

    /** Close a {@link ResultSet} silently. */
    private static void closeQuietly(ResultSet rs) {
        if (rs != null) {
            try { rs.close(); } catch (SQLException ignored) {}
        }
    }
}
