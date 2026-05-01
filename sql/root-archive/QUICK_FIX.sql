-- COPY ALL 3 LINES AND RUN IN MYSQL NOW!
-- This fixes transfers and retiring

USE greenledger;

-- Fix 1: Allow NULL batch_id (required for transfers)
ALTER TABLE wallet_transactions MODIFY COLUMN batch_id BIGINT NULL;

-- Fix 2: Add TRANSFER types to enum (required for transfers)
ALTER TABLE wallet_transactions MODIFY COLUMN type ENUM('ISSUE', 'RETIRE', 'TRANSFER_IN', 'TRANSFER_OUT') NOT NULL;

-- Fix 3: Add name column (required for wallet names)
ALTER TABLE wallet ADD COLUMN name VARCHAR(255) AFTER wallet_number;

-- Verify it worked
SELECT 'SUCCESS! Database fixed.' as status;
DESCRIBE wallet_transactions;
