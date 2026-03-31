-- ============================================================
-- Script de Détection de Fraude avec IA
-- Base de données: greenledger
-- ============================================================

USE greenledger;

-- ============================================================
-- Table pour stocker les résultats de détection de fraude
-- ============================================================
DROP TABLE IF EXISTS `fraud_detection_results`;
CREATE TABLE IF NOT EXISTS `fraud_detection_results` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `risk_score` DOUBLE NOT NULL,
    `risk_level` VARCHAR(20) NOT NULL,
    `is_fraudulent` BOOLEAN NOT NULL DEFAULT FALSE,
    `recommendation` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    `analysis_details` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    `analyzed_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (`id`),
    KEY `fk_fraud_user` (`user_id`),
    KEY `idx_risk_level` (`risk_level`),
    KEY `idx_is_fraudulent` (`is_fraudulent`),
    KEY `idx_analyzed_at` (`analyzed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Ajouter les colonnes de détection de fraude à la table user
-- ============================================================

-- Vérifier si les colonnes existent déjà avant de les ajouter
SET @exist_fraud_score := (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'greenledger' 
    AND TABLE_NAME = 'user' 
    AND COLUMN_NAME = 'fraud_score'
);

SET @exist_fraud_checked := (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'greenledger' 
    AND TABLE_NAME = 'user' 
    AND COLUMN_NAME = 'fraud_checked'
);

-- Ajouter fraud_score si elle n'existe pas
SET @sql_fraud_score = IF(
    @exist_fraud_score = 0,
    'ALTER TABLE `user` ADD COLUMN `fraud_score` DOUBLE DEFAULT 0.0 AFTER `email_verifie`',
    'SELECT "Column fraud_score already exists" AS message'
);
PREPARE stmt FROM @sql_fraud_score;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ajouter fraud_checked si elle n'existe pas
SET @sql_fraud_checked = IF(
    @exist_fraud_checked = 0,
    'ALTER TABLE `user` ADD COLUMN `fraud_checked` BOOLEAN DEFAULT FALSE AFTER `fraud_score`',
    'SELECT "Column fraud_checked already exists" AS message'
);
PREPARE stmt FROM @sql_fraud_checked;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- Créer les index pour les recherches rapides
-- ============================================================

-- Index sur fraud_score (si pas déjà existant)
SET @exist_idx_fraud_score := (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = 'greenledger' 
    AND TABLE_NAME = 'user' 
    AND INDEX_NAME = 'idx_fraud_score'
);

SET @sql_idx_fraud_score = IF(
    @exist_idx_fraud_score = 0,
    'CREATE INDEX idx_fraud_score ON `user`(fraud_score)',
    'SELECT "Index idx_fraud_score already exists" AS message'
);
PREPARE stmt FROM @sql_idx_fraud_score;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Index sur fraud_checked (si pas déjà existant)
SET @exist_idx_fraud_checked := (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = 'greenledger' 
    AND TABLE_NAME = 'user' 
    AND INDEX_NAME = 'idx_fraud_checked'
);

SET @sql_idx_fraud_checked = IF(
    @exist_idx_fraud_checked = 0,
    'CREATE INDEX idx_fraud_checked ON `user`(fraud_checked)',
    'SELECT "Index idx_fraud_checked already exists" AS message'
);
PREPARE stmt FROM @sql_idx_fraud_checked;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- Vérification de l'installation
-- ============================================================

SELECT 'Installation terminée avec succès!' AS status;

-- Afficher la structure de la nouvelle table
DESCRIBE fraud_detection_results;

-- Afficher les nouvelles colonnes de la table user
SELECT 
    COLUMN_NAME, 
    COLUMN_TYPE, 
    COLUMN_DEFAULT, 
    IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'greenledger' 
AND TABLE_NAME = 'user' 
AND COLUMN_NAME IN ('fraud_score', 'fraud_checked');

-- ============================================================
-- Fin du script
-- ============================================================
