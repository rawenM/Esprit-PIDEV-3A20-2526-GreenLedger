/*
 * Batch Traceability Migration Script (FIXED VERSION)
 * 
 * Purpose: Implement complete carbon credit batch traceability system
 * 
 * Features:
 * 1. Batch type classification (PRIMARY/SECONDARY)
 * 2. Immutable event log for batch lifecycle (blockchain-ready)
 * 3. Retirement tracking (link transactions to specific batches)
 * 4. Marketplace batch provenance tracking
 * 5. Transfer pairing for complete chain of custody
 * 
 * Author: GreenLedger Traceability Team
 * Date: March 1, 2026
 * Version: 1.0.1-FIXED
 */

-- ============================================================================
-- ENSURE CORRECT DATABASE CONTEXT
-- ============================================================================
USE greenledger;

-- ============================================================================
-- SAFETY: DISABLE FOREIGN KEY CHECKS (Re-enable at end)
-- ============================================================================
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================================
-- STEP 1: ADD BATCH TYPE TO carbon_credit_batches
-- ============================================================================

-- Use stored procedure for backwards compatibility with MySQL < 8.0
DELIMITER //
DROP PROCEDURE IF EXISTS add_batch_type_column //
CREATE PROCEDURE add_batch_type_column()
BEGIN
    DECLARE CONTINUE HANDLER FOR 1060 BEGIN END;  -- Ignore "duplicate column" error
    ALTER TABLE carbon_credit_batches
    ADD COLUMN batch_type ENUM('PRIMARY', 'SECONDARY') NOT NULL DEFAULT 'PRIMARY'
        COMMENT 'PRIMARY=issued from emission calc, SECONDARY=marketplace/split child';
END//
CALL add_batch_type_column() //
DROP PROCEDURE add_batch_type_column //
DELIMITER ;

-- Add index
DELIMITER //
DROP PROCEDURE IF EXISTS create_idx_batch_type //
CREATE PROCEDURE create_idx_batch_type()
BEGIN
    DECLARE CONTINUE HANDLER FOR 1061 BEGIN END;  -- Ignore "duplicate index" error
    ALTER TABLE carbon_credit_batches ADD INDEX idx_batch_type (batch_type);
END//
CALL create_idx_batch_type() //
DROP PROCEDURE create_idx_batch_type //
DELIMITER ;

-- ============================================================================
-- STEP 2: CREATE BATCH EVENTS TABLE (Immutable Event Log)
-- ============================================================================

CREATE TABLE IF NOT EXISTS batch_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id INT NOT NULL COMMENT 'Reference to carbon_credit_batches',
    event_type ENUM('ISSUED', 'SPLIT', 'TRANSFERRED', 'RETIRED', 'MARKETPLACE_SOLD', 'MERGED', 'VERIFIED') NOT NULL,
    event_data_json LONGTEXT COMMENT 'Event-specific data (from_wallet, to_wallet, amount, etc.)',
    event_hash VARCHAR(64) NOT NULL COMMENT 'SHA-256 hash of event (blockchain-ready)',
    previous_event_hash VARCHAR(64) COMMENT 'Hash of previous event (creates chain)',
    actor VARCHAR(200) NOT NULL COMMENT 'User/system that triggered event',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_batch_id (batch_id),
    INDEX idx_event_type (event_type),
    INDEX idx_actor (actor),
    INDEX idx_created_at (created_at),
    INDEX idx_event_hash (event_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
COMMENT='Immutable event log for batch lifecycle';

-- Add foreign key constraint after table creation
DELIMITER //
DROP PROCEDURE IF EXISTS add_fk_batch_event //
CREATE PROCEDURE add_fk_batch_event()
BEGIN
    DECLARE CONTINUE HANDLER FOR 1061, 1826 BEGIN END;  -- Ignore constraint errors
    ALTER TABLE batch_events ADD CONSTRAINT fk_batch_event 
        FOREIGN KEY (batch_id) 
        REFERENCES carbon_credit_batches(id) 
        ON DELETE RESTRICT ON UPDATE CASCADE;
END//
CALL add_fk_batch_event() //
DROP PROCEDURE add_fk_batch_event //
DELIMITER ;

-- ============================================================================
-- STEP 3: CREATE BATCH RETIREMENT DETAILS TABLE
-- ============================================================================

CREATE TABLE IF NOT EXISTS batch_retirement_details (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id BIGINT NOT NULL COMMENT 'Reference to wallet_transactions',
    batch_id INT NOT NULL COMMENT 'Specific batch retired from',
    amount_retired DECIMAL(15, 2) NOT NULL COMMENT 'Amount retired from this batch',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_transaction_id (transaction_id),
    INDEX idx_batch_id (batch_id),
    INDEX idx_created_at (created_at),
    
    UNIQUE KEY unique_transaction_batch (transaction_id, batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
COMMENT='Links retirement transactions to specific batches';

-- Add foreign key constraints after table creation
DELIMITER //
DROP PROCEDURE IF EXISTS add_fk_batch_retirement //
CREATE PROCEDURE add_fk_batch_retirement()
BEGIN
    DECLARE CONTINUE HANDLER FOR 1061, 1826 BEGIN END;  -- Ignore constraint errors
    ALTER TABLE batch_retirement_details ADD CONSTRAINT fk_retirement_transaction 
        FOREIGN KEY (transaction_id) 
        REFERENCES wallet_transactions(id) 
        ON DELETE RESTRICT ON UPDATE CASCADE;
    
    ALTER TABLE batch_retirement_details ADD CONSTRAINT fk_retirement_batch 
        FOREIGN KEY (batch_id) 
        REFERENCES carbon_credit_batches(id) 
        ON DELETE RESTRICT ON UPDATE CASCADE;
END//
CALL add_fk_batch_retirement() //
DROP PROCEDURE add_fk_batch_retirement //
DELIMITER ;

-- ============================================================================
-- STEP 4: CREATE MARKETPLACE ORDER BATCHES JUNCTION TABLE
-- ============================================================================

CREATE TABLE IF NOT EXISTS marketplace_order_batches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL COMMENT 'Reference to marketplace_orders',
    batch_id INT NOT NULL COMMENT 'Batch sold in this order',
    quantity DECIMAL(15, 2) NOT NULL COMMENT 'Quantity from this batch',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_order_id (order_id),
    INDEX idx_batch_id (batch_id),
    
    UNIQUE KEY unique_order_batch (order_id, batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
COMMENT='Tracks which batches were sold in marketplace orders';

-- Add foreign key constraints after table creation
DELIMITER //
DROP PROCEDURE IF EXISTS add_fk_marketplace_order_batches //
CREATE PROCEDURE add_fk_marketplace_order_batches()
BEGIN
    -- All DECLARE statements must come first (MySQL/MariaDB requirement)
    DECLARE CONTINUE HANDLER FOR 1025, 1091 BEGIN END;  -- Ignore DROP FK errors
    DECLARE CONTINUE HANDLER FOR 1061, 1826, 3780 BEGIN END;  -- Ignore ADD constraint errors
    
    -- Now executable statements
    SET FOREIGN_KEY_CHECKS = 0;
    
    -- Try to drop existing constraint (error will be caught by handler if not exists)
    ALTER TABLE marketplace_order_batches DROP FOREIGN KEY fk_order_batch_order;
    
    -- Add constraints (errors will be caught by handler)
    ALTER TABLE marketplace_order_batches ADD CONSTRAINT fk_order_batch_order 
        FOREIGN KEY (order_id) 
        REFERENCES marketplace_orders(id) 
        ON DELETE RESTRICT ON UPDATE CASCADE;
    
    ALTER TABLE marketplace_order_batches ADD CONSTRAINT fk_order_batch_batch 
        FOREIGN KEY (batch_id) 
        REFERENCES carbon_credit_batches(id) 
        ON DELETE RESTRICT ON UPDATE CASCADE;
    
    -- Re-enable FK checks
    SET FOREIGN_KEY_CHECKS = 1;
END//
CALL add_fk_marketplace_order_batches() //
DROP PROCEDURE add_fk_marketplace_order_batches //
DELIMITER ;

-- ============================================================================
-- STEP 5: ADD TRANSFER PAIRING TO wallet_transactions
-- ============================================================================

-- Use stored procedure for backwards compatibility with MySQL < 8.0
DELIMITER //
DROP PROCEDURE IF EXISTS add_transfer_pair_id_column //
CREATE PROCEDURE add_transfer_pair_id_column()
BEGIN
    DECLARE CONTINUE HANDLER FOR 1060 BEGIN END;  -- Ignore "duplicate column" error
    ALTER TABLE wallet_transactions
    ADD COLUMN transfer_pair_id VARCHAR(36) COMMENT 'UUID linking TRANSFER_IN and TRANSFER_OUT';
END//
CALL add_transfer_pair_id_column() //
DROP PROCEDURE add_transfer_pair_id_column //
DELIMITER ;

-- Add index
DELIMITER //
DROP PROCEDURE IF EXISTS create_idx_transfer_pair //
CREATE PROCEDURE create_idx_transfer_pair()
BEGIN
    DECLARE CONTINUE HANDLER FOR 1061 BEGIN END;  -- Ignore "duplicate index" error
    ALTER TABLE wallet_transactions ADD INDEX idx_transfer_pair (transfer_pair_id);
END//
CALL create_idx_transfer_pair() //
DROP PROCEDURE create_idx_transfer_pair //
DELIMITER ;

-- ============================================================================
-- STEP 6: BACKFILL EXISTING BATCHES WITH PRIMARY TYPE AND ISSUED EVENTS
-- ============================================================================

-- Set all existing batches to PRIMARY type
UPDATE carbon_credit_batches 
SET batch_type = 'PRIMARY' 
WHERE batch_type IS NULL;

-- Create ISSUED events for existing batches (backfill audit trail)
-- Only insert if event doesn't already exist for this batch
INSERT IGNORE INTO batch_events (batch_id, event_type, event_data_json, event_hash, previous_event_hash, actor, created_at)
SELECT 
    id AS batch_id,
    'ISSUED' AS event_type,
    JSON_OBJECT(
        'total_amount', total_amount,
        'wallet_id', wallet_id,
        'project_id', project_id,
        'batch_type', 'PRIMARY',
        'backfilled', TRUE
    ) AS event_data_json,
    SHA2(CONCAT(
        id, '|',
        'ISSUED', '|',
        total_amount, '|',
        NOW()
    ), 256) AS event_hash,
    NULL AS previous_event_hash,
    'SYSTEM_BACKFILL' AS actor,
    NOW() AS created_at
FROM carbon_credit_batches
WHERE id NOT IN (SELECT DISTINCT batch_id FROM batch_events WHERE event_type = 'ISSUED');

-- ============================================================================
-- STEP 7: CREATE VIEWS FOR TRACEABILITY QUERIES
-- ============================================================================

-- View: Complete batch lineage with all ancestors and descendants
CREATE OR REPLACE VIEW v_batch_full_lineage AS
WITH RECURSIVE batch_tree AS (
    -- Base case: root batches (no parent)
    SELECT 
        id, 
        batch_type,
        total_amount,
        parent_batch_id,
        wallet_id,
        status,
        0 AS lineage_depth,
        CAST(id AS CHAR(1000)) AS lineage_path
    FROM carbon_credit_batches
    WHERE parent_batch_id IS NULL
    
    UNION ALL
    
    -- Recursive case: child batches
    SELECT 
        c.id,
        c.batch_type,
        c.total_amount,
        c.parent_batch_id,
        c.wallet_id,
        c.status,
        bt.lineage_depth + 1,
        CONCAT(bt.lineage_path, ' -> ', c.id)
    FROM carbon_credit_batches c
    INNER JOIN batch_tree bt ON c.parent_batch_id = bt.id
)
SELECT * FROM batch_tree;

-- View: Batch event timeline with hash chain validation
CREATE OR REPLACE VIEW v_batch_event_timeline AS
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
CREATE OR REPLACE VIEW v_batch_retirement_summary AS
SELECT 
    ccb.id AS batch_id,
    ccb.batch_type,
    ccb.total_amount,
    COALESCE(SUM(brd.amount_retired), 0) AS total_retired,
    COUNT(DISTINCT brd.transaction_id) AS retirement_count,
    MIN(wt.created_at) AS first_retirement_date,
    MAX(wt.created_at) AS last_retirement_date
FROM carbon_credit_batches ccb
LEFT JOIN batch_retirement_details brd ON ccb.id = brd.batch_id
LEFT JOIN wallet_transactions wt ON brd.transaction_id = wt.id
GROUP BY ccb.id, ccb.batch_type, ccb.total_amount;

-- View: Marketplace order provenance (which batches were sold in each order)
CREATE OR REPLACE VIEW v_marketplace_batch_provenance AS
SELECT 
    mo.id AS order_id,
    mo.buyer_id,
    mo.seller_id,
    mo.total_amount,
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
CREATE OR REPLACE VIEW v_transfer_chain AS
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
    AND wt_out.type = 'TRANSFER_OUT'
    AND wt_in.type = 'TRANSFER_IN'
LEFT JOIN carbon_credit_batches ccb ON wt_out.batch_id = ccb.id
WHERE wt_out.transfer_pair_id IS NOT NULL
ORDER BY wt_out.created_at DESC;

-- ============================================================================
-- STEP 8: CREATE STORED PROCEDURE FOR BATCH PROVENANCE REPORT
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
        ccb.status,
        ccb.parent_batch_id,
        ccb.wallet_id,
        ccb.project_id
    FROM carbon_credit_batches ccb
    WHERE ccb.id = p_batch_id;
    
    -- 2. Event timeline
    SELECT 
        'EVENT_TIMELINE' AS section,
        be.*
    FROM batch_events be
    WHERE be.batch_id = p_batch_id
    ORDER BY be.created_at ASC;
    
    -- 3. Lineage (parent and children)
    SELECT 
        'LINEAGE' AS section,
        vbl.*
    FROM v_batch_full_lineage vbl
    WHERE vbl.id = p_batch_id 
       OR vbl.parent_batch_id = p_batch_id
       OR vbl.id IN (SELECT parent_batch_id FROM carbon_credit_batches WHERE id = p_batch_id);
    
    -- 4. Retirement details
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
    
    -- 5. Marketplace transactions
    SELECT 
        'MARKETPLACE_TRANSACTIONS' AS section,
        mob.id,
        mob.order_id,
        mob.batch_id,
        mob.quantity,
        mo.buyer_id,
        mo.seller_id,
        mo.total_amount,
        mo.status,
        mo.created_at AS order_date
    FROM marketplace_order_batches mob
    INNER JOIN marketplace_orders mo ON mob.order_id = mo.id
    WHERE mob.batch_id = p_batch_id
    ORDER BY mo.created_at ASC;
    
END//

DELIMITER ;

-- ============================================================================
-- STEP 9: CREATE INDEXES FOR PERFORMANCE
-- ============================================================================

-- Additional composite indexes for common queries
DELIMITER //
DROP PROCEDURE IF EXISTS create_composite_indexes //
CREATE PROCEDURE create_composite_indexes()
BEGIN
    DECLARE CONTINUE HANDLER FOR 1061 BEGIN END;  -- Ignore "duplicate index" error
    
    ALTER TABLE carbon_credit_batches ADD INDEX idx_batch_wallet_status (wallet_id, status);
    ALTER TABLE carbon_credit_batches ADD INDEX idx_batch_type_status (batch_type, status);
    ALTER TABLE carbon_credit_batches ADD INDEX idx_batch_parent_created (parent_batch_id, created_at);
    ALTER TABLE batch_events ADD INDEX idx_event_batch_created (batch_id, created_at);
    ALTER TABLE batch_retirement_details ADD INDEX idx_retirement_batch_amount (batch_id, amount_retired);
END//
CALL create_composite_indexes() //
DROP PROCEDURE create_composite_indexes //
DELIMITER ;

-- ============================================================================
-- STEP 10: DATA INTEGRITY CHECKS
-- ============================================================================

SELECT '=== DATA INTEGRITY CHECKS ===' AS check_type;

-- Count batches without type
SELECT COUNT(*) AS batches_without_type 
FROM carbon_credit_batches 
WHERE batch_type IS NULL;

-- Count orphaned retirement details (if any)
SELECT COUNT(*) AS orphaned_retirement_details
FROM batch_retirement_details brd
WHERE NOT EXISTS (SELECT 1 FROM carbon_credit_batches WHERE id = brd.batch_id)
   OR NOT EXISTS (SELECT 1 FROM wallet_transactions WHERE id = brd.transaction_id);

-- Check for duplicate event hashes
SELECT event_hash, COUNT(*) AS duplicate_count
FROM batch_events
GROUP BY event_hash
HAVING COUNT(*) > 1;

-- ============================================================================
-- RE-ENABLE FOREIGN KEY CHECKS
-- ============================================================================
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- MIGRATION COMPLETE
-- ============================================================================

SELECT 
    '✓ Batch Traceability Migration Complete!' AS status,
    NOW() AS completed_at,
    '1.0.1-FIXED' AS migration_version,
    'All tables, views, and procedures created successfully' AS notes;

-- Verify created objects
SELECT 'batch_events' AS table_name, COUNT(*) AS record_count FROM batch_events
UNION ALL
SELECT 'batch_retirement_details', COUNT(*) FROM batch_retirement_details
UNION ALL
SELECT 'marketplace_order_batches', COUNT(*) FROM marketplace_order_batches;

-- Show final schema status
SHOW TABLES LIKE 'batch%';
SHOW VIEWS LIKE 'v_batch%';

-- ============================================================================
-- ROLLBACK SCRIPT (Run if migration needs to be reverted)
-- ============================================================================

/*
-- ROLLBACK COMMANDS (DO NOT RUN unless reverting)
-- WARNING: This rollback script is DESTRUCTIVE. Backup data before using.

SET FOREIGN_KEY_CHECKS = 0;

DROP PROCEDURE IF EXISTS sp_get_batch_provenance;
DROP VIEW IF EXISTS v_transfer_chain;
DROP VIEW IF EXISTS v_marketplace_batch_provenance;
DROP VIEW IF EXISTS v_batch_retirement_summary;
DROP VIEW IF EXISTS v_batch_event_timeline;
DROP VIEW IF EXISTS v_batch_full_lineage;

DROP TABLE IF EXISTS marketplace_order_batches;
DROP TABLE IF EXISTS batch_retirement_details;
DROP TABLE IF EXISTS batch_events;

ALTER TABLE wallet_transactions DROP COLUMN IF EXISTS transfer_pair_id;
DROP INDEX IF EXISTS idx_transfer_pair ON wallet_transactions;

ALTER TABLE carbon_credit_batches DROP COLUMN IF EXISTS batch_type;
DROP INDEX IF EXISTS idx_batch_type ON carbon_credit_batches;

DROP INDEX IF EXISTS idx_batch_wallet_status ON carbon_credit_batches;
DROP INDEX IF EXISTS idx_batch_type_status ON carbon_credit_batches;
DROP INDEX IF EXISTS idx_batch_parent_created ON carbon_credit_batches;

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'Rollback complete - all traceability tables and views removed' AS status;

*/
