-- ============================================
-- JOURNAL D'ACTIVITÉ (AUDIT LOG) - VERSION SIMPLE
-- ============================================
-- Ce script crée UNIQUEMENT la table, sans données de test

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
-- VÉRIFICATION
-- ============================================
SELECT 'Table audit_log créée avec succès!' as message;
SELECT COUNT(*) as nombre_logs FROM audit_log;
