-- Script de diagnostic pour vérifier les données de fraude
USE greenledger;

-- 1. Vérifier que les colonnes existent
SELECT 
    'Colonnes fraud dans la table user' AS info,
    COLUMN_NAME, 
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'greenledger' 
  AND TABLE_NAME = 'user' 
  AND COLUMN_NAME IN ('fraud_score', 'fraud_checked');

-- 2. Afficher tous les utilisateurs avec leurs valeurs de fraude
SELECT 
    'Données actuelles des utilisateurs' AS info,
    id,
    nom,
    prenom,
    email,
    fraud_score,
    fraud_checked,
    statut
FROM `user`
ORDER BY id;

-- 3. Compter les utilisateurs par statut de fraude
SELECT 
    'Statistiques de fraude' AS info,
    COUNT(*) as total,
    SUM(CASE WHEN fraud_checked = 1 THEN 1 ELSE 0 END) as checked,
    SUM(CASE WHEN fraud_checked = 0 OR fraud_checked IS NULL THEN 1 ELSE 0 END) as not_checked,
    AVG(fraud_score) as avg_score,
    MAX(fraud_score) as max_score
FROM `user`;
