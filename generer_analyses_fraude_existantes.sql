-- ═══════════════════════════════════════════════════════════
-- GÉNÉRER DES ANALYSES DE FRAUDE POUR LES UTILISATEURS EXISTANTS
-- ═══════════════════════════════════════════════════════════

USE greenledger;

-- Vérifier les utilisateurs sans analyse de fraude
SELECT 
    u.id,
    u.nom_complet,
    u.email,
    u.fraud_score,
    u.fraud_checked,
    fd.id AS fraud_detection_id
FROM user u
LEFT JOIN fraud_detection fd ON u.id = fd.user_id
WHERE fd.id IS NULL;

-- Insérer des analyses de fraude pour les utilisateurs sans analyse
-- (Score faible par défaut pour les utilisateurs existants)

INSERT INTO fraud_detection (
    user_id,
    risk_score,
    risk_level,
    is_fraudulent,
    recommendation,
    analysis_details,
    indicators,
    analyzed_at
)
SELECT 
    u.id,
    CASE 
        WHEN u.statut = 'BLOQUE' THEN 85.0
        WHEN u.statut = 'SUSPENDU' THEN 65.0
        ELSE 15.0
    END AS risk_score,
    CASE 
        WHEN u.statut = 'BLOQUE' THEN 'HIGH'
        WHEN u.statut = 'SUSPENDU' THEN 'MEDIUM'
        ELSE 'LOW'
    END AS risk_level,
    CASE 
        WHEN u.statut = 'BLOQUE' THEN TRUE
        ELSE FALSE
    END AS is_fraudulent,
    CASE 
        WHEN u.statut = 'BLOQUE' THEN 'Compte bloqué - Vérification manuelle requise'
        WHEN u.statut = 'SUSPENDU' THEN 'Compte suspendu - Surveillance recommandée'
        ELSE 'Utilisateur légitime - Aucune action requise'
    END AS recommendation,
    CONCAT(
        'Analyse générée automatiquement pour utilisateur existant.\n',
        'Nom: ', u.nom_complet, '\n',
        'Email: ', u.email, '\n',
        'Type: ', u.type_utilisateur, '\n',
        'Statut: ', u.statut, '\n',
        'Date création: ', u.date_creation
    ) AS analysis_details,
    CASE 
        WHEN u.statut = 'BLOQUE' THEN '[{"type":"ACCOUNT_STATUS","description":"Compte bloqué","detected":true,"severity":"HIGH"}]'
        WHEN u.statut = 'SUSPENDU' THEN '[{"type":"ACCOUNT_STATUS","description":"Compte suspendu","detected":true,"severity":"MEDIUM"}]'
        ELSE '[]'
    END AS indicators,
    NOW() AS analyzed_at
FROM user u
LEFT JOIN fraud_detection fd ON u.id = fd.user_id
WHERE fd.id IS NULL;

-- Mettre à jour les champs fraud_score et fraud_checked dans la table user
UPDATE user u
LEFT JOIN fraud_detection fd ON u.id = fd.user_id
SET 
    u.fraud_score = fd.risk_score,
    u.fraud_checked = TRUE
WHERE fd.id IS NOT NULL AND u.fraud_checked = FALSE;

-- Vérifier le résultat
SELECT 
    u.id,
    u.nom_complet,
    u.email,
    u.fraud_score,
    u.fraud_checked,
    fd.risk_score,
    fd.risk_level,
    fd.is_fraudulent
FROM user u
LEFT JOIN fraud_detection fd ON u.id = fd.user_id
ORDER BY u.id;

-- ═══════════════════════════════════════════════════════════
-- RÉSUMÉ
-- ═══════════════════════════════════════════════════════════
-- Ce script:
-- 1. Identifie les utilisateurs sans analyse de fraude
-- 2. Génère des analyses avec des scores appropriés selon le statut
-- 3. Met à jour les champs fraud_score et fraud_checked
-- 4. Affiche le résultat final
-- ═══════════════════════════════════════════════════════════
