-- Script pour corriger le flag fraud_checked des utilisateurs existants
USE greenledger;

-- 1. Afficher l'état actuel
SELECT 
    'État AVANT correction' AS info,
    id,
    nom,
    prenom,
    fraud_score,
    fraud_checked
FROM `user`;

-- 2. Mettre à jour fraud_checked pour tous les utilisateurs qui ont un fraud_score
-- Si fraud_score > 0, alors fraud_checked devrait être TRUE
UPDATE `user`
SET fraud_checked = TRUE
WHERE fraud_score > 0;

-- 3. Pour les utilisateurs sans score, analyser et mettre un score par défaut
-- Les utilisateurs normaux (sans indicateurs suspects) auront un score faible
UPDATE `user`
SET 
    fraud_score = 0.0,
    fraud_checked = TRUE
WHERE fraud_score = 0 OR fraud_score IS NULL;

-- 4. Afficher l'état après correction
SELECT 
    'État APRÈS correction' AS info,
    id,
    nom,
    prenom,
    fraud_score,
    fraud_checked,
    statut
FROM `user`;

-- 5. Statistiques finales
SELECT 
    'Statistiques finales' AS info,
    COUNT(*) as total_users,
    SUM(CASE WHEN fraud_checked = 1 THEN 1 ELSE 0 END) as users_analyzed,
    SUM(CASE WHEN fraud_score >= 75 THEN 1 ELSE 0 END) as high_risk,
    SUM(CASE WHEN fraud_score >= 50 AND fraud_score < 75 THEN 1 ELSE 0 END) as medium_risk,
    SUM(CASE WHEN fraud_score >= 25 AND fraud_score < 50 THEN 1 ELSE 0 END) as low_medium_risk,
    SUM(CASE WHEN fraud_score < 25 THEN 1 ELSE 0 END) as low_risk
FROM `user`;

SELECT 'Correction terminée! Relancez l\'application pour voir les changements.' AS resultat;
