-- ============================================================
-- Script de Vérification de l'Installation
-- Détection de Fraude avec IA
-- ============================================================

USE greenledger;

-- ============================================================
-- 1. Vérifier que la table fraud_detection_results existe
-- ============================================================
SELECT 
    'Table fraud_detection_results' AS verification,
    CASE 
        WHEN COUNT(*) > 0 THEN '✓ EXISTE'
        ELSE '✗ N\'EXISTE PAS'
    END AS statut
FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_SCHEMA = 'greenledger' 
AND TABLE_NAME = 'fraud_detection_results';

-- ============================================================
-- 2. Vérifier la structure de fraud_detection_results
-- ============================================================
SELECT 
    'Structure de fraud_detection_results' AS info;
    
DESCRIBE fraud_detection_results;

-- ============================================================
-- 3. Vérifier les colonnes fraud_score et fraud_checked
-- ============================================================
SELECT 
    'Colonnes de détection de fraude dans user' AS info;

SELECT 
    COLUMN_NAME AS colonne,
    COLUMN_TYPE AS type,
    COLUMN_DEFAULT AS valeur_defaut,
    IS_NULLABLE AS nullable
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'greenledger' 
AND TABLE_NAME = 'user' 
AND COLUMN_NAME IN ('fraud_score', 'fraud_checked');

-- ============================================================
-- 4. Vérifier les index
-- ============================================================
SELECT 
    'Index de détection de fraude' AS info;

SELECT 
    INDEX_NAME AS nom_index,
    COLUMN_NAME AS colonne,
    TABLE_NAME AS table_nom
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = 'greenledger' 
AND (
    (TABLE_NAME = 'user' AND INDEX_NAME IN ('idx_fraud_score', 'idx_fraud_checked'))
    OR (TABLE_NAME = 'fraud_detection_results')
);

-- ============================================================
-- 5. Compter les résultats de détection existants
-- ============================================================
SELECT 
    'Statistiques de détection de fraude' AS info;

SELECT 
    COUNT(*) AS total_analyses,
    SUM(CASE WHEN is_fraudulent = TRUE THEN 1 ELSE 0 END) AS fraudes_detectees,
    AVG(risk_score) AS score_moyen
FROM fraud_detection_results;

-- ============================================================
-- 6. Afficher les dernières détections
-- ============================================================
SELECT 
    'Dernières détections de fraude' AS info;

SELECT 
    f.id,
    u.nom,
    u.prenom,
    u.email,
    f.risk_score AS score,
    f.risk_level AS niveau,
    f.recommendation AS recommandation,
    f.analyzed_at AS date_analyse
FROM fraud_detection_results f
JOIN user u ON f.user_id = u.id
ORDER BY f.analyzed_at DESC
LIMIT 10;

-- ============================================================
-- 7. Résumé de l'installation
-- ============================================================
SELECT 
    '=== RÉSUMÉ DE L\'INSTALLATION ===' AS titre;

SELECT 
    CASE 
        WHEN (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'greenledger' AND TABLE_NAME = 'fraud_detection_results') > 0
        AND (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'greenledger' AND TABLE_NAME = 'user' AND COLUMN_NAME = 'fraud_score') > 0
        AND (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'greenledger' AND TABLE_NAME = 'user' AND COLUMN_NAME = 'fraud_checked') > 0
        THEN '✓ INSTALLATION COMPLÈTE ET FONCTIONNELLE'
        ELSE '✗ INSTALLATION INCOMPLÈTE - Exécutez database_fraud_detection.sql'
    END AS statut_final;
