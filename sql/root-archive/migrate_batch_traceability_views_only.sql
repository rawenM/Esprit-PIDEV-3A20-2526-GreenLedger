/*
 * Batch Traceability Migration Script - VIEWS & PROCEDURES ONLY
 * 
 * Purpose: Add missing views and procedures (tables already exist in DB)
 * 
 * Features:
 * 1. Batch event timeline view with hash chain validation
 * 2. Retirement summary view by batch
 * 3. Marketplace batch provenance view
 * 4. Transfer chain tracking view
 * 5. Batch provenance report stored procedure
 * 
 * Author: GreenLedger Traceability Team
 * Date: March 2, 2026
 * Version: 1.0.2-VIEWS-ONLY
 */

-- ============================================================================
-- ENSURE CORRECT DATABASE CONTEXT
-- ============================================================================
USE greenledger;

-- ============================================================================
-- STEP 1: CREATE VIEWS FOR TRACEABILITY QUERIES
-- ============================================================================

-- View: Batch event timeline with hash chain validation
DROP VIEW IF EXISTS v_batch_event_timeline;
CREATE VIEW v_batch_event_timeline AS
SELECT 
    be.id,
    be.batch_id,
    ccb.batch_type,
    be.event_type,
    be.event_data_json,
    be.event_hash,
    be.previous_event_hash,
    be.actor,
    be.created_at,
    -- Validate hash chain
    CASE 
        WHEN be.previous_event_hash IS NULL THEN 'ROOT_EVENT'
        WHEN EXISTS (
            SELECT 1 FROM batch_events prev 
            WHERE prev.batch_id = be.batch_id 
            AND prev.event_hash = be.previous_event_hash
            AND prev.created_at < be.created_at
        ) THEN 'VALID_CHAIN'
        ELSE 'BROKEN_CHAIN'
    END AS chain_status
FROM batch_events be
INNER JOIN carbon_credit_batches ccb ON be.batch_id = ccb.id
ORDER BY be.batch_id, be.created_at;

-- View: Retirement details aggregated by batch
DROP VIEW IF EXISTS v_batch_retirement_summary;
CREATE VIEW v_batch_retirement_summary AS
SELECT 
    ccb.id AS batch_id,
    ccb.batch_type,
    ccb.total_amount,
    ccb.remaining_amount,
    COALESCE(SUM(brd.amount_retired), 0) AS total_retired,
    COUNT(DISTINCT brd.transaction_id) AS retirement_count,
    MIN(wt.created_at) AS first_retirement_date,
    MAX(wt.created_at) AS last_retirement_date
FROM carbon_credit_batches ccb
LEFT JOIN batch_retirement_details brd ON ccb.id = brd.batch_id
LEFT JOIN wallet_transactions wt ON brd.transaction_id = wt.id
GROUP BY ccb.id, ccb.batch_type, ccb.total_amount, ccb.remaining_amount;

-- View: Marketplace order provenance (which batches were sold in each order)
DROP VIEW IF EXISTS v_marketplace_batch_provenance;
CREATE VIEW v_marketplace_batch_provenance AS
SELECT 
    mo.id AS order_id,
    mo.buyer_id,
    mo.seller_id,
    mo.total_amount_usd,
    mo.status AS order_status,
    mo.created_at AS order_date,
    mob.batch_id,
    ccb.batch_type,
    mob.quantity AS batch_quantity,
    ccb.total_amount AS batch_total_amount
FROM marketplace_orders mo
INNER JOIN marketplace_order_batches mob ON mo.id = mob.order_id
INNER JOIN carbon_credit_batches ccb ON mob.batch_id = ccb.id
ORDER BY mo.created_at DESC, mo.id, mob.batch_id;

-- View: Transfer chain tracking (linked TRANSFER_IN/OUT transactions)
DROP VIEW IF EXISTS v_transfer_chain;
CREATE VIEW v_transfer_chain AS
SELECT 
    wt_out.transfer_pair_id,
    wt_out.id AS transfer_out_transaction_id,
    wt_out.wallet_id AS from_wallet_id,
    wt_in.id AS transfer_in_transaction_id,
    wt_in.wallet_id AS to_wallet_id,
    wt_out.amount,
    wt_out.batch_id AS primary_batch_id,
    ccb.batch_type AS primary_batch_type,
    wt_out.created_at AS transfer_date
FROM wallet_transactions wt_out
INNER JOIN wallet_transactions wt_in 
    ON wt_out.transfer_pair_id = wt_in.transfer_pair_id
    AND wt_out.type = 'ISSUE'
    AND wt_in.type = 'ISSUE'
LEFT JOIN carbon_credit_batches ccb ON wt_out.batch_id = ccb.id
WHERE wt_out.transfer_pair_id IS NOT NULL
ORDER BY wt_out.created_at DESC;

-- ============================================================================
-- STEP 2: CREATE STORED PROCEDURE FOR BATCH PROVENANCE REPORT
-- ============================================================================

DELIMITER //

DROP PROCEDURE IF EXISTS sp_get_batch_provenance //

CREATE PROCEDURE sp_get_batch_provenance(IN p_batch_id INT)
BEGIN
    -- Complete provenance report: emission calculation -> issuance -> transfers -> retirement
    
    -- 1. Batch details
    SELECT 
        'BATCH_DETAILS' AS section,
        ccb.id,
        ccb.batch_type,
        ccb.total_amount,
        ccb.remaining_amount,
        ccb.status,
        ccb.project_id,
        ccb.wallet_id,
        ccb.issued_at
    FROM carbon_credit_batches ccb
    WHERE ccb.id = p_batch_id;
    
    -- 2. Event timeline
    SELECT 
        'EVENT_TIMELINE' AS section,
        be.*
    FROM batch_events be
    WHERE be.batch_id = p_batch_id
    ORDER BY be.created_at ASC;
    
    -- 3. Retirement details
    SELECT 
        'RETIREMENT_DETAILS' AS section,
        brd.*,
        wt.type,
        wt.amount AS transaction_amount,
        wt.reference_note,
        wt.created_at AS retirement_date
    FROM batch_retirement_details brd
    INNER JOIN wallet_transactions wt ON brd.transaction_id = wt.id
    WHERE brd.batch_id = p_batch_id
    ORDER BY wt.created_at ASC;
    
    -- 4. Marketplace transactions
    SELECT 
        'MARKETPLACE_TRANSACTIONS' AS section,
        mob.id,
        mob.order_id,
        mob.batch_id,
        mob.quantity,
        mo.buyer_id,
        mo.seller_id,
        mo.total_amount_usd,
        mo.status,
        mo.created_at AS order_date
    FROM marketplace_order_batches mob
    INNER JOIN marketplace_orders mo ON mob.order_id = mo.id
    WHERE mob.batch_id = p_batch_id
    ORDER BY mo.created_at ASC;
    
END//

DELIMITER ;

-- ============================================================================
-- MIGRATION COMPLETE - VIEWS & PROCEDURES ADDED
-- ============================================================================

SELECT 
    '✓ Batch Traceability Views & Procedures Installation Complete!' AS status,
    NOW() AS completed_at,
    '1.0.2-VIEWS-ONLY' AS migration_version,
    'All views and stored procedures created successfully' AS notes;

-- Verify created views
SELECT table_name FROM information_schema.tables 
WHERE table_schema = 'greenledger' 
AND table_type = 'VIEW' 
AND table_name LIKE 'v_%';

-- ============================================================================
-- TEST PROCEDURES (OPTIONAL - Run to verify)
-- ============================================================================

-- Show sample batch details
-- CALL sp_get_batch_provenance(1);

-- Show transfer chain
-- SELECT * FROM v_transfer_chain LIMIT 10;

-- Show retirement summary
-- SELECT * FROM v_batch_retirement_summary;

-- Show marketplace provenance
-- SELECT * FROM v_marketplace_batch_provenance;
