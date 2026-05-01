-- ============================================================
-- Symfony Features Database Schema
-- Adds missing tables for complete feature parity
-- ============================================================

USE greenledger;

-- ============================================================
-- 1. Carbon Metrics Table
-- Stores calculated carbon emissions for projects
-- ============================================================

CREATE TABLE IF NOT EXISTS `carbon_metrics` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `project_id` INT NOT NULL,
    `baseline_tco2` DOUBLE DEFAULT 0.0,
    `actual_tco2` DOUBLE DEFAULT 0.0,
    `avoided_tco2` DOUBLE DEFAULT 0.0,
    `energy_emissions` DOUBLE DEFAULT 0.0,
    `transport_emissions` DOUBLE DEFAULT 0.0,
    `material_emissions` DOUBLE DEFAULT 0.0,
    `waste_emissions` DOUBLE DEFAULT 0.0,
    `calculated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_calculated_at` (`calculated_at`),
    CONSTRAINT `fk_carbon_metrics_project` FOREIGN KEY (`project_id`) REFERENCES `projet`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 2. Carbon Credits Table
-- Stores green credits for approved projects
-- ============================================================

CREATE TABLE IF NOT EXISTS `carbon_credits` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `project_id` INT NOT NULL,
    `credits_amount` DOUBLE NOT NULL,
    `avoided_tco2` DOUBLE NOT NULL,
    `credibility_factor` DOUBLE NOT NULL,
    `esg_multiplier` DOUBLE NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_carbon_credits_project` FOREIGN KEY (`project_id`) REFERENCES `projet`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 3. Notifications Table
-- Stores user notifications
-- ============================================================

CREATE TABLE IF NOT EXISTS `notifications` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `type` VARCHAR(50) NOT NULL,
    `message` TEXT NOT NULL,
    `redirect_url` VARCHAR(255),
    `is_read` BOOLEAN DEFAULT FALSE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_type` (`type`),
    KEY `idx_is_read` (`is_read`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_notifications_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 4. Conversation Threads Table
-- Stores conversation threads between investors and project owners
-- ============================================================

CREATE TABLE IF NOT EXISTS `conversation_threads` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `project_id` INT NOT NULL,
    `investor_id` BIGINT NOT NULL,
    `project_owner_id` BIGINT NOT NULL,
    `financement_id` BIGINT,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_investor_id` (`investor_id`),
    KEY `idx_project_owner_id` (`project_owner_id`),
    KEY `idx_financement_id` (`financement_id`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_thread_project` FOREIGN KEY (`project_id`) REFERENCES `projet`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_thread_investor` FOREIGN KEY (`investor_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_thread_owner` FOREIGN KEY (`project_owner_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 5. Thread Messages Table
-- Stores messages in conversation threads
-- ============================================================

CREATE TABLE IF NOT EXISTS `thread_messages` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `thread_id` BIGINT NOT NULL,
    `sender_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_thread_id` (`thread_id`),
    KEY `idx_sender_id` (`sender_id`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_message_thread` FOREIGN KEY (`thread_id`) REFERENCES `conversation_threads`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_message_sender` FOREIGN KEY (`sender_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 6. ML Decision Snapshots Table (if not exists)
-- Stores snapshots of ML analysis decisions
-- ============================================================

CREATE TABLE IF NOT EXISTS `ml_decision_snapshots` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `project_id` INT NOT NULL,
    `expert_id` INT,
    `project_title` VARCHAR(255),
    `project_description` TEXT,
    `project_status` VARCHAR(50),
    `company_address` VARCHAR(255),
    `company_email` VARCHAR(255),
    `company_phone` VARCHAR(50),
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_expert_id` (`expert_id`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_snapshot_project` FOREIGN KEY (`project_id`) REFERENCES `projet`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 7. Add missing columns to projet table
-- ============================================================

-- Add financing status column
ALTER TABLE `projet` 
ADD COLUMN IF NOT EXISTS `statut_financement` VARCHAR(50) DEFAULT 'NON_APPLICABLE';

-- Add carbon metrics columns
ALTER TABLE `projet` 
ADD COLUMN IF NOT EXISTS `baseline_tco2` DOUBLE DEFAULT 0.0,
ADD COLUMN IF NOT EXISTS `actual_tco2` DOUBLE DEFAULT 0.0,
ADD COLUMN IF NOT EXISTS `avoided_tco2` DOUBLE DEFAULT 0.0;

-- Add environmental data columns
ALTER TABLE `projet` 
ADD COLUMN IF NOT EXISTS `baseline_energy` DOUBLE DEFAULT 0.0,
ADD COLUMN IF NOT EXISTS `baseline_transport` DOUBLE DEFAULT 0.0,
ADD COLUMN IF NOT EXISTS `baseline_material` DOUBLE DEFAULT 0.0,
ADD COLUMN IF NOT EXISTS `baseline_waste` DOUBLE DEFAULT 0.0,
ADD COLUMN IF NOT EXISTS `actual_energy` DOUBLE DEFAULT 0.0,
ADD COLUMN IF NOT EXISTS `actual_transport` DOUBLE DEFAULT 0.0,
ADD COLUMN IF NOT EXISTS `actual_material` DOUBLE DEFAULT 0.0,
ADD COLUMN IF NOT EXISTS `actual_waste` DOUBLE DEFAULT 0.0;

-- Add fraud detection columns (if not already added)
ALTER TABLE `projet` 
ADD COLUMN IF NOT EXISTS `fraud_risk_score` DOUBLE DEFAULT 0.0,
ADD COLUMN IF NOT EXISTS `fraud_flag` BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS `fraud_reasons` TEXT;

-- Add data quality score
ALTER TABLE `projet` 
ADD COLUMN IF NOT EXISTS `data_quality_score` INT DEFAULT 0;

-- ============================================================
-- 8. Create indexes for performance
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_projet_statut_financement ON `projet`(`statut_financement`);
CREATE INDEX IF NOT EXISTS idx_projet_fraud_risk ON `projet`(`fraud_risk_score`);
CREATE INDEX IF NOT EXISTS idx_projet_fraud_flag ON `projet`(`fraud_flag`);
CREATE INDEX IF NOT EXISTS idx_projet_data_quality ON `projet`(`data_quality_score`);

-- ============================================================
-- Verification
-- ============================================================

SELECT 'Symfony features database schema installed successfully!' AS result;

-- Show created tables
SELECT 
    TABLE_NAME,
    TABLE_ROWS,
    CREATE_TIME
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = 'greenledger'
AND TABLE_NAME IN (
    'carbon_metrics',
    'carbon_credits',
    'notifications',
    'conversation_threads',
    'thread_messages',
    'ml_decision_snapshots'
)
ORDER BY TABLE_NAME;

-- Show new columns in projet table
SELECT 
    COLUMN_NAME,
    COLUMN_TYPE,
    COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'greenledger'
AND TABLE_NAME = 'projet'
AND COLUMN_NAME IN (
    'statut_financement',
    'baseline_tco2',
    'actual_tco2',
    'avoided_tco2',
    'fraud_risk_score',
    'fraud_flag',
    'data_quality_score'
)
ORDER BY COLUMN_NAME;
