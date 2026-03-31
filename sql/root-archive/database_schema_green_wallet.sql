-- ==================== GREEN WALLET SYSTEM DATABASE SCHEMA ====================
-- MariaDB/MySQL compatible schema for carbon credit wallet management
-- Created for GreenLedger Carbon Accounting Platform

-- ==================== TABLE: green_wallets ====================
-- Stores carbon credit wallets for enterprises and banks
CREATE TABLE IF NOT EXISTS green_wallets (
    id INT AUTO_INCREMENT PRIMARY KEY,
    wallet_number VARCHAR(50) UNIQUE NOT NULL,
    holder_name VARCHAR(150) NOT NULL,
    owner_type ENUM('ENTERPRISE', 'BANK') NOT NULL,
    owner_id INT NULL,  -- Reference to user/enterprise table
    available_credits DECIMAL(15,2) DEFAULT 0.00,  -- Credits available for use
    retired_credits DECIMAL(15,2) DEFAULT 0.00,    -- Credits permanently retired
    status ENUM('ACTIVE', 'PENDING_REVIEW', 'INACTIVE') DEFAULT 'ACTIVE',
    registry_id VARCHAR(100) NULL,  -- External registry identifier (optional)
    is_external BOOLEAN DEFAULT FALSE,  -- Internal vs external wallet
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_wallet_number (wallet_number),
    INDEX idx_owner_type (owner_type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== TABLE: carbon_projects ====================
-- Extended carbon projects table (may already exist, this adds verification fields)
-- If table exists, run ALTER TABLE statements below instead
CREATE TABLE IF NOT EXISTS carbon_projects (
    id INT AUTO_INCREMENT PRIMARY KEY,
    enterprise_id INT NOT NULL,
    project_name VARCHAR(200) NOT NULL,
    description TEXT,
    estimated_reduction DECIMAL(15,2),  -- Estimated CO2 reduction in tons
    verified_reduction DECIMAL(15,2),   -- Actual verified reduction
    status ENUM('PENDING', 'VERIFIED', 'REJECTED', 'EXPIRED') DEFAULT 'PENDING',
    verification_date TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_enterprise (enterprise_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- If carbon_projects table already exists, add these columns:
-- ALTER TABLE carbon_projects ADD COLUMN verified_reduction DECIMAL(15,2) AFTER estimated_reduction;
-- ALTER TABLE carbon_projects ADD COLUMN verification_date TIMESTAMP NULL AFTER status;

-- ==================== TABLE: carbon_credit_batches ====================
-- Tracks batches of carbon credits issued from verified projects
CREATE TABLE IF NOT EXISTS carbon_credit_batches (
    id INT AUTO_INCREMENT PRIMARY KEY,
    project_id INT NOT NULL,
    wallet_id INT NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,      -- Total credits in batch
    remaining_amount DECIMAL(15,2) NOT NULL,  -- Credits not yet retired
    status ENUM('AVAILABLE', 'PARTIALLY_RETIRED', 'FULLY_RETIRED') DEFAULT 'AVAILABLE',
    issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (project_id) REFERENCES carbon_projects(id) ON DELETE RESTRICT,
    FOREIGN KEY (wallet_id) REFERENCES green_wallets(id) ON DELETE RESTRICT,
    
    INDEX idx_wallet (wallet_id),
    INDEX idx_project (project_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== TABLE: wallet_transactions ====================
-- Immutable audit trail of all credit movements (issues, retirements, transfers)
CREATE TABLE IF NOT EXISTS wallet_transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    wallet_id INT NOT NULL,
    batch_id INT NULL,  -- NULL for retirement transactions
    type ENUM('ISSUE', 'RETIRE', 'TRANSFER') NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    reference_note TEXT,  -- Description/reason for transaction
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (wallet_id) REFERENCES green_wallets(id) ON DELETE RESTRICT,
    FOREIGN KEY (batch_id) REFERENCES carbon_credit_batches(id) ON DELETE RESTRICT,
    
    INDEX idx_wallet (wallet_id),
    INDEX idx_type (type),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== SAMPLE DATA FOR TESTING ====================

-- Sample Wallet 1: Enterprise Internal Wallet
INSERT INTO green_wallets (wallet_number, holder_name, owner_type, owner_id, available_credits, retired_credits, status, is_external)
VALUES ('GW-1000001', 'EcoTech Industries', 'ENTERPRISE', 1, 1250.50, 749.50, 'ACTIVE', FALSE);

-- Sample Wallet 2: Bank Internal Wallet
INSERT INTO green_wallets (wallet_number, holder_name, owner_type, owner_id, available_credits, retired_credits, status, is_external)
VALUES ('GW-1000002', 'Green Finance Bank', 'BANK', 2, 5000.00, 2000.00, 'ACTIVE', FALSE);

-- Sample Wallet 3: External Wallet Pending Review
INSERT INTO green_wallets (wallet_number, holder_name, owner_type, owner_id, available_credits, retired_credits, status, registry_id, is_external)
VALUES ('EXT-REGISTRY-789', 'Carbon Registry Co.', 'ENTERPRISE', NULL, 0.00, 0.00, 'PENDING_REVIEW', 'REG-789-EXTERNAL', TRUE);

-- Sample Carbon Project (ensure this exists or adjust IDs)
INSERT INTO carbon_projects (id, enterprise_id, project_name, description, estimated_reduction, verified_reduction, status, verification_date)
VALUES (1, 1, 'Solar Farm Installation', 'Large-scale solar energy project reducing grid emissions', 5000.00, 4500.00, 'VERIFIED', NOW())
ON DUPLICATE KEY UPDATE verified_reduction = 4500.00, status = 'VERIFIED';

-- Sample Credit Batch
INSERT INTO carbon_credit_batches (project_id, wallet_id, total_amount, remaining_amount, status)
VALUES (1, 1, 2000.00, 1250.50, 'PARTIALLY_RETIRED');

-- Sample Transactions
INSERT INTO wallet_transactions (wallet_id, batch_id, type, amount, reference_note)
VALUES 
    (1, 1, 'ISSUE', 2000.00, 'Initial credit issuance from Solar Farm Project verification'),
    (1, 1, 'RETIRE', 749.50, 'Offset for Q1 2026 corporate carbon emissions');

-- ==================== USEFUL QUERIES ====================

-- View wallet summary with credit details
/*
SELECT 
    w.wallet_number,
    w.holder_name,
    w.owner_type,
    w.available_credits,
    w.retired_credits,
    (w.available_credits + w.retired_credits) AS total_credits,
    w.status,
    COUNT(DISTINCT t.id) AS transaction_count
FROM green_wallets w
LEFT JOIN wallet_transactions t ON w.id = t.wallet_id
GROUP BY w.id
ORDER BY w.created_at DESC;
*/

-- View credit traceability (project → batch → wallet → retirement)
/*
SELECT 
    cp.project_name,
    b.total_amount AS batch_total,
    b.remaining_amount AS batch_remaining,
    w.wallet_number,
    w.holder_name,
    t.type AS transaction_type,
    t.amount AS transaction_amount,
    t.created_at AS transaction_date
FROM carbon_credit_batches b
INNER JOIN carbon_projects cp ON b.project_id = cp.id
INNER JOIN green_wallets w ON b.wallet_id = w.id
LEFT JOIN wallet_transactions t ON t.batch_id = b.id
ORDER BY cp.id, b.issued_at, t.created_at;
*/

-- ==================== MARKETPLACE TABLES ====================

-- ==================== TABLE: carbon_price_history ====================
-- Stores historical carbon credit pricing data from Climate Impact X API
CREATE TABLE IF NOT EXISTS carbon_price_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    credit_type VARCHAR(100) NOT NULL DEFAULT 'VOLUNTARY_CARBON_MARKET',  -- VCM, COMPLIANCE, etc.
    usd_per_ton DECIMAL(10, 4) NOT NULL,  -- Price per metric ton in USD
    market_index VARCHAR(50),  -- CIX index reference
    source_api VARCHAR(50) DEFAULT 'CLIMATE_IMPACT_X',
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_credit_type (credit_type),
    INDEX idx_timestamp (timestamp),
    INDEX idx_lookup (credit_type, timestamp DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== TABLE: marketplace_listings ====================
-- Active buy/sell offers for carbon credits or wallets
CREATE TABLE IF NOT EXISTS marketplace_listings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    seller_id BIGINT NOT NULL,  -- User ID of seller (references user table)
    asset_type ENUM('CARBON_CREDITS', 'WALLET') NOT NULL,
    wallet_id INT NULL,  -- If selling credits OR wallet itself
    quantity_or_id DECIMAL(15, 2) NOT NULL,  -- Amount for credits, 1 for wallet
    price_per_unit DECIMAL(10, 4) NOT NULL,  -- USD per tCO2 or per wallet
    total_price_usd DECIMAL(15, 2) GENERATED ALWAYS AS (quantity_or_id * price_per_unit) STORED,
    status ENUM('ACTIVE', 'PENDING', 'SOLD', 'CANCELLED', 'EXPIRED') DEFAULT 'ACTIVE',
    description TEXT,
    minimum_buyer_rating INT DEFAULT 0,  -- Seller can require minimum 1-5 rating
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL,  -- Auto-expire listings after period
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (seller_id) REFERENCES user(id) ON DELETE RESTRICT,
    FOREIGN KEY (wallet_id) REFERENCES green_wallets(id) ON DELETE SET NULL,
    
    INDEX idx_seller (seller_id),
    INDEX idx_asset_type (asset_type),
    INDEX idx_status (status),
    INDEX idx_created (created_at),
    INDEX idx_active_listings (status, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== TABLE: marketplace_orders ====================
-- Purchase transactions from listings (marketplace orders)
CREATE TABLE IF NOT EXISTS marketplace_orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    listing_id INT NOT NULL,
    buyer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    quantity DECIMAL(15, 2) NOT NULL,
    unit_price_usd DECIMAL(10, 4) NOT NULL,  -- Price at time of purchase
    total_amount_usd DECIMAL(15, 2) NOT NULL,
    platform_fee_usd DECIMAL(15, 2) GENERATED ALWAYS AS (total_amount_usd * 0.029 + 0.30) STORED,
    seller_proceeds_usd DECIMAL(15, 2) GENERATED ALWAYS AS (total_amount_usd - platform_fee_usd) STORED,
    stripe_payment_id VARCHAR(100),  -- Stripe charge ID
    status ENUM('PENDING', 'PAYMENT_PROCESSING', 'ESCROWED', 'COMPLETED', 'CANCELLED', 'REFUNDED', 'DISPUTED') DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completion_date TIMESTAMP NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (listing_id) REFERENCES marketplace_listings(id) ON DELETE RESTRICT,
    FOREIGN KEY (buyer_id) REFERENCES user(id) ON DELETE RESTRICT,
    FOREIGN KEY (seller_id) REFERENCES user(id) ON DELETE RESTRICT,
    
    INDEX idx_buyer (buyer_id),
    INDEX idx_seller (seller_id),
    INDEX idx_status (status),
    INDEX idx_created (created_at),
    INDEX idx_stripe_id (stripe_payment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== TABLE: peer_trades ====================
-- Direct peer-to-peer trade offers and settlements
CREATE TABLE IF NOT EXISTS peer_trades (
    id INT AUTO_INCREMENT PRIMARY KEY,
    initiator_id BIGINT NOT NULL,  -- User proposing the trade
    responder_id BIGINT NOT NULL,  -- User being proposed to
    asset_type ENUM('CARBON_CREDITS', 'WALLET') NOT NULL,
    quantity DECIMAL(15, 2) NOT NULL,
    proposed_price_usd DECIMAL(15, 2) NOT NULL,
    agreed_price_usd DECIMAL(15, 2) NULL,  -- After negotiation
    initiator_wallet_id INT,
    responder_wallet_id INT,
    stripe_payment_id VARCHAR(100) NULL,
    status ENUM('PROPOSED', 'ACCEPTED', 'NEGOTIATING', 'SETTLED', 'CANCELLED', 'DISPUTED') DEFAULT 'PROPOSED',
    escrow_id INT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    settlement_date TIMESTAMP NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (initiator_id) REFERENCES user(id) ON DELETE RESTRICT,
    FOREIGN KEY (responder_id) REFERENCES user(id) ON DELETE RESTRICT,
    FOREIGN KEY (initiator_wallet_id) REFERENCES green_wallets(id) ON DELETE SET NULL,
    FOREIGN KEY (responder_wallet_id) REFERENCES green_wallets(id) ON DELETE SET NULL,
    
    INDEX idx_initiator (initiator_id),
    INDEX idx_responder (responder_id),
    INDEX idx_status (status),
    INDEX idx_pending (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== TABLE: marketplace_escrow ====================
-- Escrow management for secure transactions
CREATE TABLE IF NOT EXISTS marketplace_escrow (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NULL,  -- From marketplace orders
    trade_id INT NULL,  -- From peer trades
    buyer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    amount_usd DECIMAL(15, 2) NOT NULL,
    held_by_platform BOOLEAN DEFAULT TRUE,  -- TRUE = platform holds; FALSE = third-party
    stripe_hold_id VARCHAR(100),  -- Stripe hold reference
    status ENUM('HELD', 'RELEASED_TO_SELLER', 'REFUNDED_TO_BUYER', 'DISPUTED', 'RESOLVED') DEFAULT 'HELD',
    hold_reason TEXT,
    release_date TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (order_id) REFERENCES marketplace_orders(id) ON DELETE SET NULL,
    FOREIGN KEY (trade_id) REFERENCES peer_trades(id) ON DELETE SET NULL,
    FOREIGN KEY (buyer_id) REFERENCES user(id) ON DELETE RESTRICT,
    FOREIGN KEY (seller_id) REFERENCES user(id) ON DELETE RESTRICT,
    
    INDEX idx_status (status),
    INDEX idx_buyer (buyer_id),
    INDEX idx_seller (seller_id),
    INDEX idx_active_holds (status, release_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== TABLE: marketplace_disputes ====================
-- Dispute resolution and mediation
CREATE TABLE IF NOT EXISTS marketplace_disputes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NULL,
    trade_id INT NULL,
    escrow_id INT NOT NULL,
    reporter_id BIGINT NOT NULL,  -- Who filed dispute
    reported_user_id BIGINT NOT NULL,  -- Who is being disputed
    dispute_reason ENUM('PAYMENT_ISSUE', 'ASSET_NOT_RECEIVED', 'ASSET_MISREPRESENTED', 'SELLER_UNRESPONSIVE', 'OTHER') NOT NULL,
    description TEXT NOT NULL,
    resolution_type ENUM('REFUND_TO_BUYER', 'RELEASE_TO_SELLER', 'SPLIT_FUNDS', 'PENDING', 'CANCELLED') DEFAULT 'PENDING',
    admin_notes TEXT,
    resolved_by BIGINT,  -- Admin user ID
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL,
    
    FOREIGN KEY (order_id) REFERENCES marketplace_orders(id) ON DELETE SET NULL,
    FOREIGN KEY (trade_id) REFERENCES peer_trades(id) ON DELETE SET NULL,
    FOREIGN KEY (escrow_id) REFERENCES marketplace_escrow(id) ON DELETE RESTRICT,
    FOREIGN KEY (reporter_id) REFERENCES user(id) ON DELETE RESTRICT,
    FOREIGN KEY (reported_user_id) REFERENCES user(id) ON DELETE RESTRICT,
    FOREIGN KEY (resolved_by) REFERENCES user(id) ON DELETE SET NULL,
    
    INDEX idx_status (resolution_type),
    INDEX idx_reporter (reporter_id),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== TABLE: marketplace_fees ====================
-- Track all platform fees and commissions
CREATE TABLE IF NOT EXISTS marketplace_fees (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NULL,
    trade_id INT NULL,
    seller_id BIGINT NOT NULL,
    fee_amount_usd DECIMAL(15, 2) NOT NULL,
    fee_type ENUM('TRANSACTION_FEE', 'LISTING_FEE', 'VERIFICATION_FEE', 'DISPUTE_PENALTY', 'OTHER') NOT NULL,
    description TEXT,
    status ENUM('PENDING', 'COLLECTED', 'WAIVED', 'REFUNDED') DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    collected_at TIMESTAMP NULL,
    
    FOREIGN KEY (order_id) REFERENCES marketplace_orders(id) ON DELETE SET NULL,
    FOREIGN KEY (trade_id) REFERENCES peer_trades(id) ON DELETE SET NULL,
    FOREIGN KEY (seller_id) REFERENCES user(id) ON DELETE RESTRICT,
    
    INDEX idx_seller (seller_id),
    INDEX idx_status (status),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== TABLE: marketplace_ratings ====================
-- User feedback and trust badges
CREATE TABLE IF NOT EXISTS marketplace_ratings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    rated_user_id BIGINT NOT NULL,  -- User being rated
    rater_id BIGINT NOT NULL,  -- User submitting rating
    order_id INT NULL,  -- Which transaction
    trade_id INT NULL,
    score_one_to_five INT NOT NULL,  -- 1-5 star rating
    review_text TEXT,
    rating_category ENUM('COMMUNICATION', 'HONESTY', 'TRANSACTION_SPEED', 'OVERALL') DEFAULT 'OVERALL',
    is_verified_transaction BOOLEAN DEFAULT FALSE,  -- Did actual purchase occur?
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (rated_user_id) REFERENCES user(id) ON DELETE RESTRICT,
    FOREIGN KEY (rater_id) REFERENCES user(id) ON DELETE RESTRICT,
    FOREIGN KEY (order_id) REFERENCES marketplace_orders(id) ON DELETE SET NULL,
    FOREIGN KEY (trade_id) REFERENCES peer_trades(id) ON DELETE SET NULL,
    
    INDEX idx_rated_user (rated_user_id),
    INDEX idx_order (order_id),
    UNIQUE KEY unique_rating_per_transaction (order_id, trade_id, rater_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== TABLE: user_marketplace_kyc ====================
-- Enhanced KYC data for marketplace traders
CREATE TABLE IF NOT EXISTS user_marketplace_kyc (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    shop_name VARCHAR(150),
    shop_description TEXT,
    is_verified_trader BOOLEAN DEFAULT FALSE,
    verification_date TIMESTAMP NULL,
    id_document_type ENUM('PASSPORT', 'NATIONAL_ID', 'BUSINESS_LICENSE', 'OTHER'),
    id_document_hash VARCHAR(255),  -- SHA256 hash of document
    bank_account_verified BOOLEAN DEFAULT FALSE,
    preferred_payout_method ENUM('BANK_TRANSFER', 'STRIPE_CONNECT', 'WALLET') DEFAULT 'BANK_TRANSFER',
    seller_avg_rating DECIMAL(3, 2) DEFAULT 5.00,  -- Average 1-5 rating
    seller_transaction_count INT DEFAULT 0,
    seller_lifetime_volume_usd DECIMAL(15, 2) DEFAULT 0.00,
    buyer_transaction_count INT DEFAULT 0,
    buyer_lifetime_volume_usd DECIMAL(15, 2) DEFAULT 0.00,
    trust_badge_level ENUM('NONE', 'SELLER', 'POWER_SELLER', 'VERIFIED_PARTNER') DEFAULT 'NONE',
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    
    INDEX idx_verified (is_verified_trader),
    INDEX idx_trust_badge (trust_badge_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== NOTES ====================
-- 1. All credit amounts are in metric tons CO₂ equivalent (tCO₂e)
-- 2. Credits can only be issued from VERIFIED projects
-- 3. Retired credits are permanently removed from circulation
-- 4. External wallets require admin approval before becoming ACTIVE
-- 5. Transaction history is immutable (no DELETE or UPDATE allowed on wallet_transactions)
-- 6. Batch retirement follows FIFO (First In, First Out) principle

-- MARKETPLACE NOTES:
-- 7. Platform fee is 2.9% + $0.30 per transaction (standard marketplace pricing)
-- 8. Carbon price history updated hourly from Climate Impact X API
-- 9. All marketplace orders require escrow holding for 24-hour buyer inspection period
-- 10. Seller ratings averaged across OVERALL scores; trust badges awarded at 50+ trades + 4.5+ rating
-- 11. New users limited to 100 tCO2 per transaction, 500 tCO2/month until verified
-- 12. Wallet sales require full KYC verification and manual admin approval
-- 13. Dispute resolution defaults to platform holding funds until resolved (max 30 days)
-- 14. Stripe Connect used for seller payouts; sellers must complete Stripe onboarding
-- 15. All monetary values use USD (DECIMAL(15,2) for USD amounts, DECIMAL(10,4) for price per unit)
