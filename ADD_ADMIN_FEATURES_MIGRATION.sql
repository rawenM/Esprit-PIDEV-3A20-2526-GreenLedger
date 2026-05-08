-- ============================================
-- GreenLedger Admin Features Migration
-- Date: 2026-05-08
-- Description: Adds missing fields for admin backoffice features
-- ============================================

-- 1. Add geolocation tracking fields to user table
-- These fields track the user's last login location for security and analytics
ALTER TABLE user 
ADD COLUMN IF NOT EXISTS last_login_country VARCHAR(100) COMMENT 'Country of last login',
ADD COLUMN IF NOT EXISTS last_login_city VARCHAR(100) COMMENT 'City of last login',
ADD COLUMN IF NOT EXISTS last_login_lat DECIMAL(10, 8) COMMENT 'Latitude of last login',
ADD COLUMN IF NOT EXISTS last_login_lng DECIMAL(11, 8) COMMENT 'Longitude of last login';

-- 2. Ensure fraud detection fields exist (may already exist)
ALTER TABLE user 
ADD COLUMN IF NOT EXISTS fraud_score DOUBLE DEFAULT 0.0 COMMENT 'Fraud risk score (0-100)',
ADD COLUMN IF NOT EXISTS fraud_checked BOOLEAN DEFAULT FALSE COMMENT 'Whether fraud check was performed';

-- 3. Ensure project fraud scoring fields exist
ALTER TABLE projet 
ADD COLUMN IF NOT EXISTS fraud_risk_score DECIMAL(5, 2) DEFAULT 0.00 COMMENT 'Fraud risk score (0-1)',
ADD COLUMN IF NOT EXISTS fraud_anomaly_score DECIMAL(5, 2) DEFAULT 0.00 COMMENT 'Anomaly detection score',
ADD COLUMN IF NOT EXISTS fraud_flag TINYINT(1) DEFAULT 0 COMMENT '1 if flagged as suspicious, 0 otherwise',
ADD COLUMN IF NOT EXISTS fraud_reasons TEXT COMMENT 'Reasons for fraud flag',
ADD COLUMN IF NOT EXISTS fraud_model_version VARCHAR(50) COMMENT 'ML model version used',
ADD COLUMN IF NOT EXISTS fraud_scored_at TIMESTAMP NULL COMMENT 'When fraud scoring was performed';

-- 4. Add indexes for performance
CREATE INDEX IF NOT EXISTS idx_user_fraud_score ON user(fraud_score);
CREATE INDEX IF NOT EXISTS idx_user_fraud_checked ON user(fraud_checked);
CREATE INDEX IF NOT EXISTS idx_user_last_login_country ON user(last_login_country);
CREATE INDEX IF NOT EXISTS idx_projet_fraud_flag ON projet(fraud_flag);
CREATE INDEX IF NOT EXISTS idx_projet_fraud_risk_score ON projet(fraud_risk_score);
CREATE INDEX IF NOT EXISTS idx_wallet_available_credits ON wallet(available_credits);

-- 5. Verify audit_log table exists (should already exist)
-- If not, create it
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT COMMENT 'ID of user who performed action',
    user_email VARCHAR(255) COMMENT 'Email of user who performed action',
    user_name VARCHAR(255) COMMENT 'Name of user who performed action',
    action_type VARCHAR(50) NOT NULL COMMENT 'Type of action performed',
    action_description TEXT COMMENT 'Description of action',
    target_user_id BIGINT COMMENT 'ID of target user (if applicable)',
    target_user_email VARCHAR(255) COMMENT 'Email of target user',
    ip_address VARCHAR(45) COMMENT 'IP address of user',
    user_agent TEXT COMMENT 'Browser user agent',
    browser VARCHAR(100) COMMENT 'Browser name',
    operating_system VARCHAR(100) COMMENT 'Operating system',
    status VARCHAR(20) DEFAULT 'SUCCESS' COMMENT 'SUCCESS, FAILED, WARNING',
    old_value TEXT COMMENT 'Old value before change',
    new_value TEXT COMMENT 'New value after change',
    error_message TEXT COMMENT 'Error message if failed',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'When action occurred',
    INDEX idx_audit_user_id (user_id),
    INDEX idx_audit_action_type (action_type),
    INDEX idx_audit_status (status),
    INDEX idx_audit_created_at (created_at),
    INDEX idx_audit_target_user_id (target_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Audit log for all admin actions';

-- 6. Add wallet name column if missing (for better identification)
ALTER TABLE wallet 
ADD COLUMN IF NOT EXISTS name VARCHAR(255) COMMENT 'Friendly name for wallet';

-- 7. Create view for negative wallets (optional, for easier querying)
CREATE OR REPLACE VIEW v_negative_wallets AS
SELECT 
    w.id,
    w.wallet_number,
    w.name,
    w.owner_type,
    w.owner_id,
    w.available_credits,
    w.retired_credits,
    ABS(w.available_credits) as deficit,
    w.created_at,
    CASE 
        WHEN ABS(w.available_credits) >= 1000 THEN 'CRITICAL'
        WHEN ABS(w.available_credits) >= 500 THEN 'HIGH'
        WHEN ABS(w.available_credits) >= 100 THEN 'MEDIUM'
        ELSE 'LOW'
    END as priority
FROM wallet w
WHERE w.available_credits < 0
ORDER BY w.available_credits ASC;

-- 8. Create view for at-risk wallets
CREATE OR REPLACE VIEW v_at_risk_wallets AS
SELECT 
    w.id,
    w.wallet_number,
    w.name,
    w.owner_type,
    w.owner_id,
    w.available_credits,
    w.retired_credits,
    w.created_at,
    CASE 
        WHEN w.available_credits < 10 THEN 'URGENT'
        WHEN w.available_credits < 25 THEN 'HIGH'
        WHEN w.available_credits < 50 THEN 'MEDIUM'
        ELSE 'LOW'
    END as warning_level
FROM wallet w
WHERE w.available_credits >= 0 AND w.available_credits < 50
ORDER BY w.available_credits ASC;

-- 9. Create view for user statistics (for dashboard)
CREATE OR REPLACE VIEW v_user_statistics AS
SELECT 
    COUNT(*) as total_users,
    SUM(CASE WHEN statut = 'ACTIVE' THEN 1 ELSE 0 END) as active_users,
    SUM(CASE WHEN statut = 'EN_ATTENTE' THEN 1 ELSE 0 END) as pending_users,
    SUM(CASE WHEN statut = 'BLOQUE' THEN 1 ELSE 0 END) as blocked_users,
    SUM(CASE WHEN statut = 'SUSPENDU' THEN 1 ELSE 0 END) as suspended_users,
    SUM(CASE WHEN fraud_checked = TRUE AND fraud_score >= 75 THEN 1 ELSE 0 END) as high_fraud_risk,
    SUM(CASE WHEN fraud_checked = TRUE AND fraud_score < 25 THEN 1 ELSE 0 END) as low_fraud_risk,
    SUM(CASE WHEN fraud_checked = TRUE AND fraud_score >= 25 AND fraud_score < 75 THEN 1 ELSE 0 END) as medium_fraud_risk,
    SUM(CASE WHEN type_utilisateur = 'INVESTISSEUR' THEN 1 ELSE 0 END) as investors,
    SUM(CASE WHEN type_utilisateur = 'PORTEUR_PROJET' THEN 1 ELSE 0 END) as project_holders,
    SUM(CASE WHEN type_utilisateur = 'EXPERT_CARBONE' THEN 1 ELSE 0 END) as carbon_experts,
    SUM(CASE WHEN type_utilisateur = 'ADMIN' THEN 1 ELSE 0 END) as admins
FROM user;

-- 10. Create view for project fraud statistics
CREATE OR REPLACE VIEW v_project_fraud_statistics AS
SELECT 
    COUNT(*) as total_projects,
    SUM(CASE WHEN fraud_flag = 1 THEN 1 ELSE 0 END) as suspected_projects,
    SUM(CASE WHEN fraud_flag = 0 THEN 1 ELSE 0 END) as clean_projects,
    AVG(fraud_risk_score) as avg_risk_score,
    MAX(fraud_risk_score) as max_risk_score,
    MIN(fraud_risk_score) as min_risk_score
FROM projet
WHERE fraud_scored_at IS NOT NULL;

-- 11. Sample data for testing (optional - comment out for production)
-- INSERT INTO user (nom, prenom, email, mot_de_passe, type_utilisateur, statut, fraud_score, fraud_checked, last_login_country, last_login_city, last_login_lat, last_login_lng)
-- VALUES 
-- ('Test', 'User1', 'test1@example.com', '$2a$10$...', 'INVESTISSEUR', 'ACTIVE', 15.5, TRUE, 'France', 'Paris', 48.8566, 2.3522),
-- ('Test', 'User2', 'test2@example.com', '$2a$10$...', 'PORTEUR_PROJET', 'EN_ATTENTE', 85.2, TRUE, 'USA', 'New York', 40.7128, -74.0060);

-- ============================================
-- Verification Queries
-- ============================================

-- Check if all columns were added successfully
SELECT 
    COLUMN_NAME, 
    DATA_TYPE, 
    IS_NULLABLE, 
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'user' 
  AND COLUMN_NAME IN ('last_login_country', 'last_login_city', 'last_login_lat', 'last_login_lng', 'fraud_score', 'fraud_checked');

-- Check project fraud columns
SELECT 
    COLUMN_NAME, 
    DATA_TYPE, 
    IS_NULLABLE, 
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'projet' 
  AND COLUMN_NAME IN ('fraud_risk_score', 'fraud_anomaly_score', 'fraud_flag', 'fraud_reasons', 'fraud_model_version', 'fraud_scored_at');

-- Check indexes
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    COLUMN_NAME,
    SEQ_IN_INDEX
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('user', 'projet', 'wallet', 'audit_log')
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;

-- Check views
SELECT 
    TABLE_NAME,
    VIEW_DEFINITION
FROM INFORMATION_SCHEMA.VIEWS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME LIKE 'v_%';

-- ============================================
-- Rollback Script (if needed)
-- ============================================

-- CAUTION: Only run this if you need to undo the migration
-- Uncomment the following lines to rollback:

/*
-- Drop views
DROP VIEW IF EXISTS v_negative_wallets;
DROP VIEW IF EXISTS v_at_risk_wallets;
DROP VIEW IF EXISTS v_user_statistics;
DROP VIEW IF EXISTS v_project_fraud_statistics;

-- Remove indexes
DROP INDEX IF EXISTS idx_user_fraud_score ON user;
DROP INDEX IF EXISTS idx_user_fraud_checked ON user;
DROP INDEX IF EXISTS idx_user_last_login_country ON user;
DROP INDEX IF EXISTS idx_projet_fraud_flag ON projet;
DROP INDEX IF EXISTS idx_projet_fraud_risk_score ON projet;
DROP INDEX IF EXISTS idx_wallet_available_credits ON wallet;

-- Remove columns (CAUTION: This will delete data!)
ALTER TABLE user 
DROP COLUMN IF EXISTS last_login_country,
DROP COLUMN IF EXISTS last_login_city,
DROP COLUMN IF EXISTS last_login_lat,
DROP COLUMN IF EXISTS last_login_lng;

ALTER TABLE projet 
DROP COLUMN IF EXISTS fraud_risk_score,
DROP COLUMN IF EXISTS fraud_anomaly_score,
DROP COLUMN IF EXISTS fraud_flag,
DROP COLUMN IF EXISTS fraud_reasons,
DROP COLUMN IF EXISTS fraud_model_version,
DROP COLUMN IF EXISTS fraud_scored_at;

ALTER TABLE wallet 
DROP COLUMN IF EXISTS name;
*/

-- ============================================
-- Migration Complete
-- ============================================

SELECT 'Migration completed successfully!' as status;
SELECT 'Please verify the changes using the verification queries above.' as next_step;
