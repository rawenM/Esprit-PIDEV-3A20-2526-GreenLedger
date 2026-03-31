-- ============================================================
-- FIX RAPIDE: Ajouter les colonnes de fraude
-- Base: greenledger
-- ============================================================

USE greenledger;

-- Ajouter les colonnes si elles n'existent pas
ALTER TABLE `user` 
ADD COLUMN IF NOT EXISTS `fraud_score` DOUBLE DEFAULT 0.0,
ADD COLUMN IF NOT EXISTS `fraud_checked` BOOLEAN DEFAULT FALSE;

-- Créer les index
CREATE INDEX IF NOT EXISTS idx_fraud_score ON `user`(fraud_score);
CREATE INDEX IF NOT EXISTS idx_fraud_checked ON `user`(fraud_checked);

-- Créer la table fraud_detection_results
CREATE TABLE IF NOT EXISTS `fraud_detection_results` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `risk_score` DOUBLE NOT NULL,
    `risk_level` VARCHAR(20) NOT NULL,
    `is_fraudulent` BOOLEAN NOT NULL DEFAULT FALSE,
    `recommendation` VARCHAR(255),
    `analysis_details` TEXT,
    `analyzed_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `fk_fraud_user` (`user_id`),
    KEY `idx_risk_level` (`risk_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Vérification
SELECT 'Installation terminée!' AS resultat;
SELECT COLUMN_NAME, COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'greenledger' AND TABLE_NAME = 'user' 
AND COLUMN_NAME IN ('fraud_score', 'fraud_checked');
