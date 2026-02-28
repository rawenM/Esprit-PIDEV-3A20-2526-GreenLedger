-- ============================================================
-- FIX RAPIDE: Afficher les scores de fraude
-- Temps: 30 secondes
-- ============================================================

USE greenledger;

-- Mettre à jour fraud_checked pour TOUS les utilisateurs
UPDATE `user`
SET fraud_checked = TRUE;

-- Mettre un score par défaut de 0 pour ceux qui n'en ont pas
UPDATE `user`
SET fraud_score = 0.0
WHERE fraud_score IS NULL;

-- Vérifier le résultat
SELECT 
    'Correction terminée!' AS resultat,
    COUNT(*) as total_users,
    SUM(CASE WHEN fraud_checked = 1 THEN 1 ELSE 0 END) as users_with_fraud_checked
FROM `user`;

-- Afficher tous les utilisateurs avec leurs scores
SELECT 
    id,
    nom,
    prenom,
    email,
    fraud_score,
    fraud_checked,
    statut,
    CASE 
        WHEN fraud_score < 25 THEN 'Faible 🟢'
        WHEN fraud_score < 50 THEN 'Moyen 🟡'
        WHEN fraud_score < 75 THEN 'Élevé 🟠'
        ELSE 'Critique 🔴'
    END as niveau_risque
FROM `user`
ORDER BY fraud_score DESC;
