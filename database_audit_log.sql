-- ============================================
-- JOURNAL D'ACTIVITÉ (AUDIT LOG)
-- ============================================
-- Ce script crée la table pour le journal d'activité
-- L'enregistrement est AUTOMATIQUE et ne peut pas être désactivé
-- Seule la CONSULTATION est donnée à l'admin

USE greenledger;

-- Table pour le journal d'activité
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- Qui a fait l'action?
    user_id BIGINT,
    user_email VARCHAR(255),
    user_name VARCHAR(255),
    
    -- Quelle action?
    action_type VARCHAR(50) NOT NULL,
    action_description TEXT,
    
    -- Sur qui/quoi?
    target_user_id BIGINT,
    target_user_email VARCHAR(255),
    
    -- Détails techniques
    ip_address VARCHAR(45),
    user_agent TEXT,
    browser VARCHAR(100),
    operating_system VARCHAR(100),
    
    -- Résultat
    status VARCHAR(20) NOT NULL, -- SUCCESS, FAILED, WARNING
    error_message TEXT,
    
    -- Données avant/après (pour modifications)
    old_value TEXT,
    new_value TEXT,
    
    -- Timestamp
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Index pour recherche rapide
    INDEX idx_user_id (user_id),
    INDEX idx_action_type (action_type),
    INDEX idx_created_at (created_at),
    INDEX idx_status (status),
    INDEX idx_target_user_id (target_user_id),
    
    -- Clé étrangère (optionnelle, peut être NULL pour actions sans utilisateur)
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE SET NULL,
    FOREIGN KEY (target_user_id) REFERENCES user(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- TYPES D'ACTIONS ENREGISTRÉES
-- ============================================
-- USER_LOGIN              - Connexion réussie
-- USER_LOGIN_FAILED       - Tentative de connexion échouée
-- USER_LOGOUT             - Déconnexion
-- USER_REGISTER           - Inscription
-- USER_PROFILE_UPDATE     - Modification de profil
-- USER_PASSWORD_CHANGE    - Changement de mot de passe
-- USER_PASSWORD_RESET     - Réinitialisation de mot de passe
-- USER_EMAIL_CHANGE       - Changement d'email
-- ADMIN_USER_VALIDATE     - Admin valide un compte
-- ADMIN_USER_BLOCK        - Admin bloque un utilisateur
-- ADMIN_USER_UNBLOCK      - Admin débloque un utilisateur
-- ADMIN_USER_DELETE       - Admin supprime un utilisateur
-- ADMIN_VIEW_FRAUD        - Admin consulte détails fraude
-- FRAUD_DETECTED          - Fraude détectée lors inscription
-- CAPTCHA_FAILED          - Échec du CAPTCHA
-- CAPTCHA_SUCCESS         - Succès du CAPTCHA
-- SESSION_EXPIRED         - Session expirée
-- UNAUTHORIZED_ACCESS     - Tentative d'accès non autorisé

-- ============================================
-- DONNÉES DE TEST
-- ============================================
-- Quelques exemples pour tester l'interface
-- IMPORTANT: user_id peut être NULL pour les actions sans utilisateur connecté

INSERT INTO audit_log (user_id, user_email, user_name, action_type, action_description, status, ip_address, created_at) VALUES
(NULL, 'admin@greenledger.com', 'Admin User', 'USER_LOGIN', 'Connexion réussie', 'SUCCESS', '192.168.1.100', NOW() - INTERVAL 5 MINUTE),
(NULL, 'user@example.com', 'John Doe', 'USER_LOGIN_FAILED', 'Mot de passe incorrect', 'FAILED', '192.168.1.101', NOW() - INTERVAL 4 MINUTE),
(NULL, 'user@example.com', 'John Doe', 'USER_LOGIN', 'Connexion réussie', 'SUCCESS', '192.168.1.101', NOW() - INTERVAL 3 MINUTE),
(NULL, 'admin@greenledger.com', 'Admin User', 'ADMIN_USER_VALIDATE', 'Validation du compte de John Doe', 'SUCCESS', '192.168.1.100', NOW() - INTERVAL 2 MINUTE),
(NULL, 'fraud@temp-mail.com', 'Suspect User', 'FRAUD_DETECTED', 'Email jetable détecté - Score: 85/100', 'WARNING', '10.0.0.1', NOW() - INTERVAL 1 MINUTE);

-- ============================================
-- VÉRIFICATION
-- ============================================
SELECT 'Table audit_log créée avec succès!' as message;
SELECT COUNT(*) as nombre_logs FROM audit_log;
