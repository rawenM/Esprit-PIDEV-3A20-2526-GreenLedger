-- CRITICAL DATABASE UPDATES FOR GREEN WALLET SYSTEM
-- Execute this SQL script to fix all schema issues
-- Run this BEFORE using the application!

USE greenledger;

-- ========================================
-- FIX 1: Add name column to wallet table
-- ========================================
ALTER TABLE wallet 
ADD COLUMN name VARCHAR(255) AFTER wallet_number;

-- Optional: Set default names for existing wallets
-- UPDATE wallet SET name = CONCAT('Wallet #', wallet_number) WHERE name IS NULL;

-- ========================================
-- FIX 2: Make batch_id NULLABLE in wallet_transactions
-- (Required because TRANSFER operations don't have batches)
-- ========================================
ALTER TABLE wallet_transactions 
MODIFY COLUMN batch_id BIGINT NULL;

-- ========================================
-- FIX 3: Add TRANSFER types to transaction enum
-- (Required for transfer functionality)
-- ========================================
ALTER TABLE wallet_transactions 
MODIFY COLUMN type ENUM('ISSUE', 'RETIRE', 'TRANSFER_IN', 'TRANSFER_OUT') NOT NULL;

-- ========================================
-- Verify all changes
-- ========================================
DESCRIBE wallet;
DESCRIBE wallet_transactions;

-- Optional: View current data
SELECT 'WALLET TABLE:' as info;
SELECT id, wallet_number, name, owner_type, owner_id, 
       available_credits, retired_credits, created_at 
FROM wallet;

SELECT 'TRANSACTION TABLE:' as info;
SELECT id, wallet_id, batch_id, type, amount, 
       LEFT(reference_note, 50) as reference, created_at 
FROM wallet_transactions 
ORDER BY created_at DESC LIMIT 10;
