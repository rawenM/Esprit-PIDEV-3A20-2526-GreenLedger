-- ============================================================
-- CORRECTION COMPLÈTE: Mettre fraud_checked = TRUE pour TOUS
-- ============================================================

USE greenledger;

-- 1. Afficher l'état AVANT
SELECT 
    '=== ÉTAT AVANT CORRECTION ===' AS info,
    id,
    nom,
    prenom,
    email,
    fraud_score,
    fraud_checked,
    statut
FROM `user`
ORDER BY id;

-- 2. Mettre fraud_checked = TRUE pour TOUS les utilisateurs
UPDATE `user`
SET fraud_checked = TRUE
WHERE fraud_checked = FALSE OR fraud_checked = 0 OR fraud_checked IS NULL;

-- 3. S'assurer que fraud_score n'est pas NULL
UPDATE `user`
SET fraud_score = 0.0
WHERE fraud_score IS NULL;

-- 4. Afficher l'état APRÈS
SELECT 
    '=== ÉTAT APRÈS CORRECTION ===' AS info,
    id,
    nom,
    prenom,
    email,
    fraud_score,
    fraud_checked,
    statut,
    CASE 
        WHEN fraud_score < 25 THEN '🟢 Faible'
        WHEN fraud_score < 50 THEN '🟡 Moyen'
        WHEN fraud_score < 75 THEN '🟠 Élevé'
        ELSE '🔴 Critique'
    END as niveau_risque
FROM `user`
ORDER BY fraud_score DESC;

-- 5. Statistiques finales
SELECT 
    '=== STATISTIQUES ===' AS info,
    COUNT(*) as total_users,
    SUM(CASE WHEN fraud_checked = 1 THEN 1 ELSE 0 END) as users_analyzed,
    SUM(CASE WHEN fraud_score >= 75 THEN 1 ELSE 0 END) as critique,
    SUM(CASE WHEN fraud_score >= 50 AND fraud_score < 75 THEN 1 ELSE 0 END) as eleve,
    SUM(CASE WHEN fraud_score >= 25 AND fraud_score < 50 THEN 1 ELSE 0 END) as moyen,
    SUM(CASE WHEN fraud_score < 25 THEN 1 ELSE 0 END) as faible
FROM `user`;

SELECT '✅ Correction terminée! Tous les utilisateurs ont maintenant fraud_checked = TRUE' AS resultat;
SELECT '📝 Recompilez et relancez l\'application: mvn clean compile puis run.bat' AS action_suivante;
