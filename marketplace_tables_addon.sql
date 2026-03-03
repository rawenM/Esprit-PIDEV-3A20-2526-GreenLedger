-- ==================== MARKETPLACE ADDON FOR GREENLEDGER DATABASE ====================
-- Add these tables to your existing greenledger database
-- Compatible with your existing tables: user (BIGINT id), wallet (INT id)
-- Run this file after verifying your database structure
-- Command: SOURCE d:/PiDev/Pi_Dev/marketplace_tables_addon.sql;

USE greenledger;

-- ==================== TABLE: marketplace_listings ====================
-- Active buy/sell offers for carbon credits or wallets
CREATE TABLE IF NOT EXISTS marketplace_listings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    seller_id BIGINT NOT NULL,  -- References user(id) BIGINT
    asset_type ENUM('CARBON_CREDITS', 'WALLET') NOT NULL,
    wallet_id INT NULL,  -- References wallet(id) INT
    quantity_or_id DECIMAL(15, 2) NOT NULL,  -- Amount for credits, 1 for wallet
    price_per_unit DECIMAL(10, 4) NOT NULL,  -- USD per tCO2 or per wallet
    min_price_usd DECIMAL(10, 4) NOT NULL,  -- Minimum acceptable offer price per unit
    auto_accept_price_usd DECIMAL(10, 4) NULL,  -- Auto-accept offers at or above this
    total_price_usd DECIMAL(15, 2) GENERATED ALWAYS AS (quantity_or_id * price_per_unit) STORED,
    status ENUM('ACTIVE', 'PENDING', 'SOLD', 'CANCELLED', 'EXPIRED') DEFAULT 'ACTIVE',
    description TEXT,
    minimum_buyer_rating INT DEFAULT 0,  -- Seller can require minimum 1-5 rating
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL,  -- Auto-expire listings after period
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (seller_id) REFERENCES user(id) ON DELETE RESTRICT,
    FOREIGN KEY (wallet_id) REFERENCES wallet(id) ON DELETE SET NULL,
    
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
    buyer_id BIGINT NOT NULL,  -- References user(id)
    seller_id BIGINT NOT NULL,  -- References user(id)
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

-- ==================== TABLE: marketplace_offers ====================
-- Negotiation offers before purchase
CREATE TABLE IF NOT EXISTS marketplace_offers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    listing_id INT NOT NULL,
    buyer_id BIGINT NOT NULL,  -- References user(id)
    seller_id BIGINT NOT NULL,  -- References user(id)
    quantity DECIMAL(15, 2) NOT NULL,
    offer_price_usd DECIMAL(10, 4) NOT NULL,  -- Offer price per unit
    status ENUM('PENDING', 'COUNTERED', 'ACCEPTED', 'REJECTED', 'EXPIRED', 'CANCELLED') DEFAULT 'PENDING',
    counter_price_usd DECIMAL(10, 4) NULL,
    expires_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (listing_id) REFERENCES marketplace_listings(id) ON DELETE RESTRICT,
    FOREIGN KEY (buyer_id) REFERENCES user(id) ON DELETE RESTRICT,
    FOREIGN KEY (seller_id) REFERENCES user(id) ON DELETE RESTRICT,
    
    INDEX idx_listing (listing_id),
    INDEX idx_buyer (buyer_id),
    INDEX idx_seller (seller_id),
    INDEX idx_status (status),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== TABLE: peer_trades ====================
-- Direct peer-to-peer trade offers and settlements
CREATE TABLE IF NOT EXISTS peer_trades (
    id INT AUTO_INCREMENT PRIMARY KEY,
    initiator_id BIGINT NOT NULL,  -- References user(id) BIGINT
    responder_id BIGINT NOT NULL,  -- References user(id) BIGINT
    asset_type ENUM('CARBON_CREDITS', 'WALLET') NOT NULL,
    quantity DECIMAL(15, 2) NOT NULL,
    proposed_price_usd DECIMAL(15, 2) NOT NULL,
    agreed_price_usd DECIMAL(15, 2) NULL,  -- After negotiation
    initiator_wallet_id INT NULL,  -- References wallet(id) INT
    responder_wallet_id INT NULL,  -- References wallet(id) INT
    stripe_payment_id VARCHAR(100) NULL,
    status ENUM('PROPOSED', 'ACCEPTED', 'NEGOTIATING', 'SETTLED', 'CANCELLED', 'DISPUTED') DEFAULT 'PROPOSED',
    escrow_id INT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    settlement_date TIMESTAMP NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (initiator_id) REFERENCES user(id) ON DELETE RESTRICT,
    FOREIGN KEY (responder_id) REFERENCES user(id) ON DELETE RESTRICT,
    FOREIGN KEY (initiator_wallet_id) REFERENCES wallet(id) ON DELETE SET NULL,
    FOREIGN KEY (responder_wallet_id) REFERENCES wallet(id) ON DELETE SET NULL,
    
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
    buyer_id BIGINT NOT NULL,  -- References user(id)
    seller_id BIGINT NOT NULL,  -- References user(id)
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
    reporter_id BIGINT NOT NULL,  -- References user(id) - Who filed dispute
    reported_user_id BIGINT NOT NULL,  -- References user(id) - Who is being disputed
    dispute_reason ENUM('PAYMENT_ISSUE', 'ASSET_NOT_RECEIVED', 'ASSET_MISREPRESENTED', 'SELLER_UNRESPONSIVE', 'OTHER') NOT NULL,
    description TEXT NOT NULL,
    resolution_type ENUM('REFUND_TO_BUYER', 'RELEASE_TO_SELLER', 'SPLIT_FUNDS', 'PENDING', 'CANCELLED') DEFAULT 'PENDING',
    admin_notes TEXT,
    resolved_by BIGINT NULL,  -- Admin user ID from user table
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
    seller_id BIGINT NOT NULL,  -- References user(id)
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
    rated_user_id BIGINT NOT NULL,  -- References user(id) - User being rated
    rater_id BIGINT NOT NULL,  -- References user(id) - User submitting rating
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
    user_id BIGINT NOT NULL UNIQUE,  -- References user(id)
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

-- ==================== VERIFICATION QUERIES ====================
-- Run these after executing the script to verify successful creation:

-- Check all marketplace tables created:
-- SHOW TABLES LIKE 'marketplace%';

-- Verify foreign key relationships:
-- SELECT 
--     TABLE_NAME, 
--     CONSTRAINT_NAME, 
--     REFERENCED_TABLE_NAME 
-- FROM information_schema.KEY_COLUMN_USAGE 
-- WHERE TABLE_SCHEMA = 'greenledger' 
--   AND TABLE_NAME LIKE 'marketplace%' 
--   AND REFERENCED_TABLE_NAME IS NOT NULL;

-- ==================== IMPORTANT NOTES ====================
-- 1. Matches YOUR database structure exactly:
--    - user table: id is BIGINT (your existing structure)
--    - wallet table: id is INT, owner_id is INT (your existing structure)
-- 2. Foreign keys reference user(id) as BIGINT
-- 3. Foreign keys reference wallet(id) as INT
-- 4. Platform fee: 2.9% + $0.30 per transaction (auto-calculated in generated columns)
-- 5. All monetary values in USD (DECIMAL types for precision)
-- 6. Status ENUMs provide transaction state tracking
-- 7. Indexes optimized for common queries (seller lookups, status filters, date ranges)

-- ==================== NEXT STEPS ====================
-- 1. Execute this file: mysql -u root -p greenledger < marketplace_tables_addon.sql
--    OR in MySQL shell: USE greenledger; SOURCE d:/PiDev/Pi_Dev/marketplace_tables_addon.sql;
-- 2. Verify tables: SHOW TABLES LIKE 'marketplace%';
-- 3. Check structure: DESCRIBE marketplace_listings;
-- 4. Verify foreign keys: 
--    SELECT TABLE_NAME, CONSTRAINT_NAME, REFERENCED_TABLE_NAME 
--    FROM information_schema.KEY_COLUMN_USAGE 
--    WHERE TABLE_SCHEMA = 'greenledger' AND REFERENCED_TABLE_NAME IN ('user', 'wallet');
-- 5. Update application configuration files with Stripe keys
-- 6. Test marketplace UI navigation (already implemented)
-- 7. Begin end-to-end testing with test Stripe account
