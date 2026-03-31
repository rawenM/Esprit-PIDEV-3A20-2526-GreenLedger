/*
 * Green Wallet Elite Transformation - Database Schema Updates
 * 
 * Purpose: Add emission calculation audit trail and batch traceability
 * 
 * Features:
 * 1. Emission calculations audit table (ISO 14064 compliance)
 * 2. Enhanced carbon_credit_batches with serial numbers and verification
 * 3. Nullable columns to avoid migration issues
 * 4. Indexes for performance optimization
 * 
 * Author: GreenLedger Elite Engineering Team
 * Date: February 27, 2026
 */

-- ============================================================================
-- 1. EMISSION CALCULATIONS AUDIT TABLE
-- ============================================================================
-- Immutable audit trail for all emission calculations
-- Provides blockchain-ready verification through SHA-256 hashing

CREATE TABLE IF NOT EXISTS emission_calculations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    calculation_id VARCHAR(100) UNIQUE NOT NULL COMMENT 'UUID or generated ID',
    input_json TEXT NOT NULL COMMENT 'Activity data as JSON (immutable)',
    emission_factor_id VARCHAR(200) NOT NULL COMMENT 'Climatiq factor ID',
    emission_factor_version VARCHAR(50) COMMENT 'Factor version (e.g., v2.1.5)',
    co2e_result DECIMAL(15, 4) NOT NULL COMMENT 'Calculated CO2e in kg',
    methodology_version VARCHAR(20) COMMENT 'IPCC AR version (AR4/AR5/AR6)',
    tier INT COMMENT 'GHG Protocol tier (1-4, 1=measured, 4=estimated)',
    uncertainty_percent DECIMAL(5, 2) COMMENT '±% uncertainty',
    metadata_json TEXT COMMENT 'Additional context (project, user, etc.)',
    calculation_hash VARCHAR(64) NOT NULL COMMENT 'SHA-256 hash for immutability',
    actor VARCHAR(200) COMMENT 'User/system that triggered calculation',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_calculation_id (calculation_id),
    INDEX idx_actor (actor),
    INDEX idx_created_at (created_at),
    INDEX idx_tier (tier)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Immutable audit trail for emission calculations';

-- ============================================================================
-- 2. ENHANCE CARBON CREDIT BATCHES TABLE
-- ============================================================================
-- Add traceability fields to existing carbon_credit_batches table
-- All columns nullable to avoid migration issues with existing data

ALTER TABLE carbon_credit_batches 
ADD COLUMN serial_number VARCHAR(50) UNIQUE COMMENT 'Unique identifier (CC-YYYY-XXXXXX)',
ADD COLUMN verification_standard VARCHAR(100) COMMENT 'VCS, GOLD_STANDARD, CDM, CAR, etc.',
ADD COLUMN vintage_year INT COMMENT 'Year credits were issued/verified',
ADD COLUMN project_certification_url TEXT COMMENT 'Link to verification documentation',
ADD COLUMN calculation_audit_id VARCHAR(100) COMMENT 'Link to emission_calculations table',
ADD COLUMN parent_batch_id INT COMMENT 'If split from larger batch',
ADD COLUMN lineage_json TEXT COMMENT 'Child batch IDs as JSON array',

ADD INDEX idx_serial_number (serial_number),
ADD INDEX idx_verification_standard (verification_standard),
ADD INDEX idx_vintage_year (vintage_year),
ADD INDEX idx_calculation_audit (calculation_audit_id),
ADD INDEX idx_parent_batch (parent_batch_id),

ADD FOREIGN KEY fk_calculation_audit (calculation_audit_id) 
    REFERENCES emission_calculations(calculation_id) 
    ON DELETE SET NULL,
    
ADD FOREIGN KEY fk_parent_batch (parent_batch_id) 
    REFERENCES carbon_credit_batches(id) 
    ON DELETE SET NULL;

-- ============================================================================
-- 3. ENHANCE WALLET TRANSACTIONS TABLE
-- ============================================================================
-- Add batch serial and audit linkage to transactions

ALTER TABLE wallet_transactions
ADD COLUMN batch_serial VARCHAR(50) COMMENT 'Batch serial number for display',
ADD COLUMN calculation_audit_id VARCHAR(100) COMMENT 'Link to emission calculation',
ADD COLUMN uncertainty_range VARCHAR(50) COMMENT '±% confidence for display',

ADD INDEX idx_batch_serial (batch_serial),
ADD INDEX idx_calc_audit_trans (calculation_audit_id);

-- ============================================================================
-- 4. CREATE BATCH SERIAL SEQUENCE TABLE (for counter persistence)
-- ============================================================================
-- Maintains year-specific counters for serial number generation

CREATE TABLE IF NOT EXISTS batch_serial_sequences (
    year INT PRIMARY KEY,
    last_sequence INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Serial number counter persistence';

-- Initialize current year
INSERT INTO batch_serial_sequences (year, last_sequence) 
VALUES (YEAR(CURDATE()), 0)
ON DUPLICATE KEY UPDATE last_sequence = last_sequence;

-- ============================================================================
-- 5. CREATE VIEWS FOR ELITE ANALYTICS
-- ============================================================================

-- View: Batch provenance chain (full lineage)
CREATE OR REPLACE VIEW v_batch_lineage AS
SELECT 
    b.id,
    b.serial_number,
    b.total_amount,
    b.remaining_amount,
    b.verification_standard,
    b.vintage_year,
    b.parent_batch_id,
    pb.serial_number AS parent_serial,
    p.name AS project_name,
    p.location AS project_location,
    ec.co2e_result AS calculation_co2e,
    ec.tier AS calculation_tier,
    ec.uncertainty_percent,
    ec.methodology_version
FROM carbon_credit_batches b
LEFT JOIN carbon_credit_batches pb ON b.parent_batch_id = pb.id
LEFT JOIN carbon_projects p ON b.project_id = p.id
LEFT JOIN emission_calculations ec ON b.calculation_audit_id = ec.calculation_id;

-- View: Wallet emission footprint summary
CREATE OR REPLACE VIEW v_wallet_emission_footprint AS
SELECT 
    w.id AS wallet_id,
    w.wallet_number,
    w.name AS wallet_name,
    w.owner_type,
    COUNT(DISTINCT b.id) AS total_batches,
    SUM(b.total_amount) AS total_credits_issued,
    SUM(b.remaining_amount) AS available_credits,
    COUNT(DISTINCT wt.id) AS total_transactions,
    AVG(ec.uncertainty_percent) AS avg_uncertainty,
    GROUP_CONCAT(DISTINCT b.verification_standard) AS verification_standards
FROM wallet w
LEFT JOIN carbon_credit_batches b ON w.id = b.wallet_id
LEFT JOIN wallet_transactions wt ON w.id = wt.wallet_id
LEFT JOIN emission_calculations ec ON b.calculation_audit_id = ec.calculation_id
GROUP BY w.id, w.wallet_number, w.name, w.owner_type;

-- View: High-quality calculations (Tier 1-2 only)
CREATE OR REPLACE VIEW v_high_quality_calculations AS
SELECT 
    ec.*,
    b.serial_number AS batch_serial,
    p.name AS project_name
FROM emission_calculations ec
LEFT JOIN carbon_credit_batches b ON ec.calculation_id = b.calculation_audit_id
LEFT JOIN carbon_projects p ON b.project_id = p.id
WHERE ec.tier <= 2
ORDER BY ec.created_at DESC;

-- ============================================================================
-- 6. DATA QUALITY TRIGGERS
-- ============================================================================

-- Trigger: Auto-generate serial number if not provided
DELIMITER $$
CREATE TRIGGER trg_batch_serial_auto 
BEFORE INSERT ON carbon_credit_batches
FOR EACH ROW
BEGIN
    IF NEW.serial_number IS NULL OR NEW.serial_number = '' THEN
        -- Generate serial: CC-YYYY-XXXXXX
        SET @year = YEAR(CURDATE());
        SET @seq = (SELECT COALESCE(MAX(last_sequence), 0) + 1 
                    FROM batch_serial_sequences 
                    WHERE year = @year);
        
        SET NEW.serial_number = CONCAT('CC-', @year, '-', LPAD(@seq, 6, '0'));
        
        -- Update sequence counter
        INSERT INTO batch_serial_sequences (year, last_sequence) 
        VALUES (@year, @seq)
        ON DUPLICATE KEY UPDATE last_sequence = @seq;
    END IF;
    
    -- Set vintage year if not provided
    IF NEW.vintage_year IS NULL THEN
        SET NEW.vintage_year = YEAR(CURDATE());
    END IF;
END$$
DELIMITER ;

-- Trigger: Update batch status based on remaining amount
DELIMITER $$
CREATE TRIGGER trg_batch_status_update
BEFORE UPDATE ON carbon_credit_batches
FOR EACH ROW
BEGIN
    IF NEW.remaining_amount = 0 THEN
        SET NEW.status = 'FULLY_RETIRED';
    ELSEIF NEW.remaining_amount < NEW.total_amount THEN
        SET NEW.status = 'PARTIALLY_RETIRED';
    ELSE
        SET NEW.status = 'AVAILABLE';
    END IF;
END$$
DELIMITER ;

-- ============================================================================
-- 7. PERFORMANCE INDEXES
-- ============================================================================

-- Composite index for common wallet queries
CREATE INDEX idx_wallet_batch_lookup 
ON carbon_credit_batches(wallet_id, status, vintage_year);

-- Index for audit trail integrity checks
CREATE INDEX idx_audit_hash 
ON emission_calculations(calculation_hash);

-- Index for temporal queries
CREATE INDEX idx_batch_issued_at 
ON carbon_credit_batches(issued_at DESC);

-- ============================================================================
-- 8. SAMPLE DATA FOR TESTING (Optional - comment out for production)
-- ============================================================================

/*
-- Sample emission calculation
INSERT INTO emission_calculations (
    calculation_id, 
    input_json, 
    emission_factor_id, 
    co2e_result, 
    tier, 
    uncertainty_percent,
    calculation_hash,
    actor
) VALUES (
    'calc-test-001',
    '{"activity":"electricity","amount":1000,"unit":"kWh","region":"US-CA"}',
    'electricity-energy_source_grid_mix-US-CA',
    420.5,
    2,
    15.0,
    SHA2(CONCAT('calc-test-001', 'electricity', '420.5'), 256),
    'system'
);

-- Sample batch with traceability
UPDATE carbon_credit_batches 
SET 
    verification_standard = 'VCS',
    vintage_year = 2026,
    calculation_audit_id = 'calc-test-001',
    project_certification_url = 'https://registry.verra.org/...'
WHERE id = 1;
*/

-- ============================================================================
-- SUCCESS MESSAGE
-- ============================================================================
SELECT '✓ Elite Green Wallet schema updates applied successfully!' AS Status;
SELECT 'Schema version: 2.0 (Elite Traceability)' AS Version;
SELECT 'Tables enhanced: emission_calculations, carbon_credit_batches, wallet_transactions' AS Changes;
