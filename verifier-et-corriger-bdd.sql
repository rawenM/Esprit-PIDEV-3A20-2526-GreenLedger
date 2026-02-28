-- ============================================================
-- VÉRIFICATION ET CORRECTION DE LA BASE DE DONNÉES
-- Base: greenledger
-- ============================================================

USE greenledger;

-- ============================================================
-- ÉTAPE 1: VÉRIFIER LES COLONNES EXISTANTES
-- ============================================================

SELECT 'VÉRIFICATION DES COLONNES...' AS etape;

SELECT 
    COLUMN_NAME, 
    COLUMN_TYPE, 
    IS_NULLABLE, 
    COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'greenledger' 
AND TABLE_NAME = 'user' 
AND COLUMN_NAME IN ('fraud_score', 'fraud_checked');

-- ============================================================
-- ÉTAPE 2: AJOUTER LES COLONNES SI ELLES N'EXISTENT PAS
-- ============================================================

SELECT 'AJOUT DES COLONNES SI NÉCESSAIRE...' AS etape;

-- Ajouter fraud_score si elle n'existe pas
SET @exist_fraud_score := (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'greenledger' 
    AND TABLE_NAME = 'user' 
    AND COLUMN_NAME = 'fraud_score'
);

SET @sql_fraud_score = IF(
    @exist_fraud_score = 0,
    'ALTER TABLE `user` ADD COLUMN `fraud_score` DOUBLE DEFAULT 0.0 AFTER `email_verifie`',
    'SELECT "✓ Colonne fraud_score existe déjà" AS message'
);

PREPARE stmt FROM @sql_fraud_score;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ajouter fraud_checked si elle n'existe pas
SET @exist_fraud_checked := (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'greenledger' 
    AND TABLE_NAME = 'user' 
    AND COLUMN_NAME = 'fraud_checked'
);

SET @sql_fraud_checked = IF(
    @exist_fraud_checked = 0,
    'ALTER TABLE `user` ADD COLUMN `fraud_checked` BOOLEAN DEFAULT FALSE AFTER `fraud_score`',
    'SELECT "✓ Colonne fraud_checked existe déjà" AS message'
);

PREPARE stmt FROM @sql_fraud_checked;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- ÉTAPE 3: CRÉER LES INDEX SI NÉCESSAIRE
-- ============================================================

SELECT 'CRÉATION DES INDEX...' AS etape;

-- Index sur fraud_score
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
    'SELECT "✓ Index idx_fraud_score existe déjà" AS message'
);

PREPARE stmt FROM @sql_idx_fraud_score;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Index sur fraud_checked
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
    'SELECT "✓ Index idx_fraud_checked existe déjà" AS message'
);

PREPARE stmt FROM @sql_idx_fraud_checked;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- ÉTAPE 4: VÉRIFIER LA TABLE fraud_detection_results
-- ============================================================

SELECT 'VÉRIFICATION DE LA TABLE fraud_detection_results...' AS etape;

SET @exist_table := (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.TABLES 
    WHERE TABLE_SCHEMA = 'greenledger' 
    AND TABLE_NAME = 'fraud_detection_results'
);

-- Créer la table si elle n'existe pas
SET @sql_create_table = IF(
    @exist_table = 0,
    'CREATE TABLE IF NOT EXISTS `fraud_detection_results` (
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
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci',
    'SELECT "✓ Table fraud_detection_results existe déjà" AS message'
);

PREPARE stmt FROM @sql_create_table;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- ÉTAPE 5: VÉRIFICATION FINALE
-- ============================================================

SELECT '============================================' AS separateur;
SELECT '✅ VÉRIFICATION FINALE' AS titre;
SELECT '============================================' AS separateur;

-- Vérifier les colonnes de user
SELECT 
    'Colonnes de la table user:' AS info,
    COLUMN_NAME, 
    COLUMN_TYPE, 
    COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'greenledger' 
AND TABLE_NAME = 'user' 
AND COLUMN_NAME IN ('fraud_score', 'fraud_checked');

-- Vérifier la table fraud_detection_results
SELECT 
    'Table fraud_detection_results:' AS info,
    COUNT(*) AS existe
FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_SCHEMA = 'greenledger' 
AND TABLE_NAME = 'fraud_detection_results';

-- Afficher les utilisateurs avec leurs scores
SELECT 
    '============================================' AS separateur
UNION ALL
SELECT 
    'UTILISATEURS AVEC SCORES DE FRAUDE:' AS separateur;

SELECT 
    id,
    nom,
    prenom,
    email,
    fraud_score,
    fraud_checked,
    statut
FROM `user`
ORDER BY fraud_score DESC
LIMIT 10;

SELECT '============================================' AS separateur;
SELECT '✅ INSTALLATION TERMINÉE AVEC SUCCÈS!' AS resultat;
SELECT '============================================' AS separateur;
SELECT 'Relancez votre application maintenant!' AS action;
