-- ============================================================
-- SCRIPT DE VÉRIFICATION DE LA BASE DE DONNÉES
-- ============================================================

USE greenledger;

-- 1. Vérifier si les colonnes fraud existent
SELECT 
    'Vérification des colonnes fraud' AS etape,
    COLUMN_NAME, 
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'greenledger' 
  AND TABLE_NAME = 'user' 
  AND COLUMN_NAME IN ('fraud_score', 'fraud_checked');

-- 2. Vérifier si la table fraud_detection_results existe
SELECT 
    'Vérification de la table fraud_detection_results' AS etape,
    TABLE_NAME,
    ENGINE,
    TABLE_ROWS
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = 'greenledger'
  AND TABLE_NAME = 'fraud_detection_results';

-- 3. Afficher quelques utilisateurs avec leurs scores de fraude
SELECT 
    'Exemple d\'utilisateurs avec scores' AS etape,
    id,
    nom,
    prenom,
    email,
    fraud_score,
    fraud_checked,
    statut
FROM `user`
ORDER BY fraud_score DESC
LIMIT 5;

-- 4. Statistiques de fraude
SELECT 
    'Statistiques de fraude' AS etape,
    COUNT(*) as total_users,
    SUM(CASE WHEN fraud_checked = 1 THEN 1 ELSE 0 END) as users_checked,
    SUM(CASE WHEN fraud_score >= 75 THEN 1 ELSE 0 END) as high_risk,
    SUM(CASE WHEN fraud_score >= 50 AND fraud_score < 75 THEN 1 ELSE 0 END) as medium_risk,
    SUM(CASE WHEN fraud_score < 50 THEN 1 ELSE 0 END) as low_risk
FROM `user`;
