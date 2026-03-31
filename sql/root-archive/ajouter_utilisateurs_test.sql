-- ═══════════════════════════════════════════════════════════════
-- AJOUTER DES UTILISATEURS DE TEST
-- ═══════════════════════════════════════════════════════════════
-- Base de données : greenledger
-- Table : user
-- ═══════════════════════════════════════════════════════════════

USE greenledger;

-- Supprimer les utilisateurs de test existants (optionnel)
-- DELETE FROM `user` WHERE email IN ('admin@plateforme.com', 'investisseur@test.com', 'porteur@test.com', 'expert@test.com');

-- 1. ADMINISTRATEUR
INSERT INTO `user` (
    nom, prenom, email, mot_de_passe, telephone, adresse, 
    date_naissance, type_utilisateur, statut, email_verifie, 
    token_verification, date_inscription, fraud_score, fraud_checked
) VALUES (
    'Admin',
    'Super',
    'admin@plateforme.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- Mot de passe: admin123
    '+33612345678',
    '123 Rue de l\'Admin, 75001 Paris',
    '1985-01-15',
    'ADMINISTRATEUR',
    'ACTIVE',
    1,
    NULL,
    NOW(),
    0.0,
    1
);

-- 2. INVESTISSEUR (compte actif)
INSERT INTO `user` (
    nom, prenom, email, mot_de_passe, telephone, adresse, 
    date_naissance, type_utilisateur, statut, email_verifie, 
    token_verification, date_inscription, fraud_score, fraud_checked
) VALUES (
    'Dupont',
    'Jean',
    'investisseur@test.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- Mot de passe: admin123
    '+33623456789',
    '45 Avenue des Investisseurs, 69001 Lyon',
    '1990-05-20',
    'INVESTISSEUR',
    'ACTIVE',
    1,
    NULL,
    NOW(),
    15.5,
    1
);

-- 3. PORTEUR DE PROJET (en attente de validation)
INSERT INTO `user` (
    nom, prenom, email, mot_de_passe, telephone, adresse, 
    date_naissance, type_utilisateur, statut, email_verifie, 
    token_verification, date_inscription, fraud_score, fraud_checked
) VALUES (
    'Martin',
    'Sophie',
    'porteur@test.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- Mot de passe: admin123
    '+33634567890',
    '78 Boulevard des Projets, 33000 Bordeaux',
    '1988-08-12',
    'PORTEUR_PROJET',
    'EN_ATTENTE',
    1,
    NULL,
    NOW(),
    8.2,
    1
);

-- 4. EXPERT CARBONE (compte actif)
INSERT INTO `user` (
    nom, prenom, email, mot_de_passe, telephone, adresse, 
    date_naissance, type_utilisateur, statut, email_verifie, 
    token_verification, date_inscription, fraud_score, fraud_checked
) VALUES (
    'Dubois',
    'Pierre',
    'expert@test.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- Mot de passe: admin123
    '+33645678901',
    '12 Rue de l\'Expertise, 31000 Toulouse',
    '1982-03-25',
    'EXPERT_CARBONE',
    'ACTIVE',
    1,
    NULL,
    NOW(),
    5.0,
    1
);

-- 5. INVESTISSEUR BLOQUÉ (pour tester le déblocage)
INSERT INTO `user` (
    nom, prenom, email, mot_de_passe, telephone, adresse, 
    date_naissance, type_utilisateur, statut, email_verifie, 
    token_verification, date_inscription, fraud_score, fraud_checked
) VALUES (
    'Suspect',
    'Paul',
    'bloque@test.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- Mot de passe: admin123
    '+33656789012',
    '99 Rue du Blocage, 13001 Marseille',
    '1995-11-30',
    'INVESTISSEUR',
    'BLOQUE',
    1,
    NULL,
    NOW(),
    85.7,
    1
);

-- 6. INVESTISSEUR AVEC SCORE DE FRAUDE ÉLEVÉ
INSERT INTO `user` (
    nom, prenom, email, mot_de_passe, telephone, adresse, 
    date_naissance, type_utilisateur, statut, email_verifie, 
    token_verification, date_inscription, fraud_score, fraud_checked
) VALUES (
    'Fraudeur',
    'Marc',
    'fraude@test.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- Mot de passe: admin123
    '+33667890123',
    '666 Avenue de la Fraude, 59000 Lille',
    '1992-07-07',
    'INVESTISSEUR',
    'ACTIVE',
    1,
    NULL,
    NOW(),
    92.3,
    1
);

-- Vérifier les utilisateurs ajoutés
SELECT 
    id,
    nom,
    prenom,
    email,
    type_utilisateur,
    statut,
    fraud_score,
    fraud_checked,
    date_inscription
FROM `user`
ORDER BY date_inscription DESC;

-- ═══════════════════════════════════════════════════════════════
-- RÉSUMÉ
-- ═══════════════════════════════════════════════════════════════
-- 6 utilisateurs de test ajoutés :
-- 
-- 1. admin@plateforme.com      - ADMINISTRATEUR - ACTIVE
-- 2. investisseur@test.com     - INVESTISSEUR   - ACTIVE (score fraude: 15.5)
-- 3. porteur@test.com          - PORTEUR_PROJET - EN_ATTENTE (score fraude: 8.2)
-- 4. expert@test.com           - EXPERT_CARBONE - ACTIVE (score fraude: 5.0)
-- 5. bloque@test.com           - INVESTISSEUR   - BLOQUE (score fraude: 85.7)
-- 6. fraude@test.com           - INVESTISSEUR   - ACTIVE (score fraude: 92.3)
--
-- Mot de passe pour tous : admin123
-- ═══════════════════════════════════════════════════════════════
