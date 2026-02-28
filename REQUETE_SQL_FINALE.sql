-- ============================================================
-- REQUÊTE SQL FINALE: Corriger fraud_checked pour TOUS
-- Copiez-collez dans phpMyAdmin
-- ============================================================

USE greenledger;

-- Mettre fraud_checked = TRUE pour TOUS les utilisateurs
UPDATE `user`
SET fraud_checked = TRUE
WHERE fraud_checked = FALSE OR fraud_checked = 0 OR fraud_checked IS NULL;

-- S'assurer que fraud_score n'est pas NULL
UPDATE `user`
SET fraud_score = 0.0
WHERE fraud_score IS NULL;

-- Vérifier le résultat
SELECT 
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
