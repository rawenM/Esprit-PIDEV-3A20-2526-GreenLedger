-- ==================== MARKETPLACE SCHEMA UPDATE ====================
-- Run this to add missing columns and tables to your existing greenledger database
-- Date: 2026-02-27
-- Database: greenledger

USE greenledger;

-- ==================== UPDATE EXISTING marketplace_listings TABLE ====================
-- Add negotiation pricing columns (MySQL compatible - no IF NOT EXISTS for ALTER TABLE)
-- If columns already exist, you'll see "Duplicate column name" errors - these can be safely ignored

ALTER TABLE marketplace_listings
    ADD COLUMN min_price_usd DECIMAL(10, 4) NULL COMMENT 'Minimum acceptable offer price per unit';

ALTER TABLE marketplace_listings
    ADD COLUMN auto_accept_price_usd DECIMAL(10, 4) NULL COMMENT 'Auto-accept offers at or above this price';

-- Set default values for existing rows (optional)
UPDATE marketplace_listings 
SET min_price_usd = price_per_unit * 0.90,  -- Min = 90% of asking price
    auto_accept_price_usd = price_per_unit * 1.10  -- Auto-accept = 110% of asking price
WHERE min_price_usd IS NULL;

-- ==================== CREATE marketplace_offers TABLE ====================
-- Tracks buyer offers and seller counter-offers (negotiation workflow)
CREATE TABLE IF NOT EXISTS marketplace_offers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    listing_id INT NOT NULL,
    buyer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    quantity DECIMAL(15, 2) NOT NULL,
    offer_price_usd DECIMAL(10, 4) NOT NULL COMMENT 'Buyer initial offer per unit',
    counter_price_usd DECIMAL(10, 4) NULL COMMENT 'Seller counter-offer per unit',
    status ENUM('PENDING', 'COUNTERED', 'ACCEPTED', 'REJECTED', 'EXPIRED', 'CANCELLED') DEFAULT 'PENDING',
    expires_at TIMESTAMP NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL 48 HOUR),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (listing_id) REFERENCES marketplace_listings(id) ON DELETE CASCADE,
    FOREIGN KEY (buyer_id) REFERENCES user(id) ON DELETE RESTRICT,
    FOREIGN KEY (seller_id) REFERENCES user(id) ON DELETE RESTRICT,
    
    INDEX idx_listing (listing_id),
    INDEX idx_buyer (buyer_id),
    INDEX idx_seller (seller_id),
    INDEX idx_status (status),
    INDEX idx_active_offers (status, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== OPTIONAL: Add currency support (future-proofing) ====================
-- Add currency column to allow multi-currency support (default USD for now)
-- If columns already exist, you'll see "Duplicate column name" errors - safe to ignore

ALTER TABLE marketplace_listings
    ADD COLUMN currency_code VARCHAR(3) DEFAULT 'USD' AFTER price_per_unit;

ALTER TABLE marketplace_orders
    ADD COLUMN currency_code VARCHAR(3) DEFAULT 'USD' AFTER unit_price_usd;

ALTER TABLE marketplace_offers
    ADD COLUMN currency_code VARCHAR(3) DEFAULT 'USD' AFTER offer_price_usd;

-- ==================== VERIFICATION QUERIES ====================
-- Run these to confirm changes were applied successfully

-- Check marketplace_listings columns
SELECT COLUMN_NAME, DATA_TYPE, COLUMN_DEFAULT, IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'greenledger' 
  AND TABLE_NAME = 'marketplace_listings'
  AND COLUMN_NAME IN ('min_price_usd', 'auto_accept_price_usd', 'currency_code');

-- Check marketplace_offers table exists
SELECT COUNT(*) as offers_table_exists 
FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_SCHEMA = 'greenledger' 
  AND TABLE_NAME = 'marketplace_offers';

-- ==================== NOTES ====================
-- 1. The currency_code columns are optional but recommended for future flexibility
-- 2. All price fields remain as written (no forced USD naming convention)
-- 3. Existing marketplace_listings rows will get default min/auto-accept prices
-- 4. Offers table includes 48-hour default expiration for pending offers
-- 5. Status transitions: PENDING → COUNTERED/ACCEPTED/REJECTED/EXPIRED
