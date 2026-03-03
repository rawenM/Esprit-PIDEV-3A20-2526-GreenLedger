-- ==================================================================================
-- OPTIONAL DATABASE MIGRATION: Add Metadata Columns to carbon_credit_batches
-- ==================================================================================
-- 
-- This migration adds traceability metadata columns to the carbon_credit_batches table.
-- These columns enable full audit trails with verification standards, vintage years,
-- and calculation audit IDs.
--
-- IMPORTANT: The system works WITHOUT these columns (using fallback logic).
--            Run this migration only if you want full metadata support.
--
-- Status: OPTIONAL
-- Date: March 2026
-- Related: TRACEABLE_BATCH_IMPLEMENTATION.md
-- ==================================================================================

USE pidev;

-- Add metadata columns to carbon_credit_batches table
ALTER TABLE carbon_credit_batches
ADD COLUMN IF NOT EXISTS calculation_audit_id VARCHAR(100) 
    COMMENT 'Link to emission calculation audit' AFTER status,
ADD COLUMN IF NOT EXISTS batch_type ENUM('PRIMARY', 'SECONDARY', 'RETIRED') 
    DEFAULT 'PRIMARY' 
    COMMENT 'PRIMARY=original issuance, SECONDARY=transfer/split, RETIRED=fully retired' AFTER calculation_audit_id,
ADD COLUMN IF NOT EXISTS verification_standard VARCHAR(100) 
    COMMENT 'VCS, GOLD_STANDARD, CDM, CAR, etc.' AFTER batch_type,
ADD COLUMN IF NOT EXISTS vintage_year INT 
    COMMENT 'Year credits were issued/verified' AFTER verification_standard,
ADD COLUMN IF NOT EXISTS parent_batch_id INT NULL
    COMMENT 'For SECONDARY batches, links to source batch for lineage tracking' AFTER vintage_year,
ADD COLUMN IF NOT EXISTS serial_number VARCHAR(50) 
    COMMENT 'Unique batch identifier (CC-YYYY-NNNNNN)' AFTER parent_batch_id;

-- Add foreign key for parent batch lineage
ALTER TABLE carbon_credit_batches
ADD CONSTRAINT fk_parent_batch 
    FOREIGN KEY (parent_batch_id) REFERENCES carbon_credit_batches(id) 
    ON DELETE SET NULL;

-- Add indexes for performance
ALTER TABLE carbon_credit_batches
ADD INDEX IF NOT EXISTS idx_batch_type (batch_type),
ADD INDEX IF NOT EXISTS idx_verification_standard (verification_standard),
ADD INDEX IF NOT EXISTS idx_vintage_year (vintage_year),
ADD INDEX IF NOT EXISTS idx_parent_batch (parent_batch_id),
ADD INDEX IF NOT EXISTS idx_serial_number (serial_number);

-- ==================================================================================
-- Verification Query - Run this to confirm migration success
-- ==================================================================================
-- 
-- SELECT 
--     COLUMN_NAME, 
--     DATA_TYPE, 
--     COLUMN_TYPE,
--     IS_NULLABLE,
--     COLUMN_DEFAULT,
--     COLUMN_COMMENT
-- FROM INFORMATION_SCHEMA.COLUMNS
-- WHERE TABLE_SCHEMA = 'pidev' 
--   AND TABLE_NAME = 'carbon_credit_batches'
-- ORDER BY ORDINAL_POSITION;
--
-- Expected output should include:
--   - calculation_audit_id (VARCHAR(100))
--   - batch_type (ENUM)
--   - verification_standard (VARCHAR(100))
--   - vintage_year (INT)
--   - parent_batch_id (INT)
--   - serial_number (VARCHAR(50))
-- ==================================================================================

-- ==================================================================================
-- ROLLBACK SCRIPT (if needed)
-- ==================================================================================
-- 
-- -- WARNING: This will permanently delete metadata columns and their data!
-- -- ALTER TABLE carbon_credit_batches
-- -- DROP FOREIGN KEY fk_parent_batch,
-- -- DROP COLUMN serial_number,
-- -- DROP COLUMN parent_batch_id,
-- -- DROP COLUMN vintage_year,
-- -- DROP COLUMN verification_standard,
-- -- DROP COLUMN batch_type,
-- -- DROP COLUMN calculation_audit_id;
-- 
-- ==================================================================================

-- Success message
SELECT 'Batch metadata columns migration complete! ✅' AS status;
SELECT 'The system now supports full traceability with verification standards, vintage years, and lineage tracking.' AS info;
SELECT 'Run the verification query above to confirm all columns were added successfully.' AS next_step;
