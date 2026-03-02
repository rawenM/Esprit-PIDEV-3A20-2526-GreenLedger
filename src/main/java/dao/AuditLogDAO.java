package dao;

import Models.AuditLog;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Interface pour l'accès aux données du journal d'activité
 */
public interface AuditLogDAO {
    
    /**
     * Enregistre une nouvelle entrée dans le journal
     * IMPORTANT: Cette méthode est appelée AUTOMATIQUEMENT
     * L'admin ne peut PAS désactiver l'enregistrement
     */
    void log(AuditLog auditLog);
    
    /**
     * Récupère tous les logs (avec pagination)
     */
    List<AuditLog> findAll(int limit, int offset);
    
    /**
     * Récupère les logs d'un utilisateur spécifique
     */
    List<AuditLog> findByUserId(Long userId);
    
    /**
     * Récupère les logs par type d'action
     */
    List<AuditLog> findByActionType(AuditLog.ActionType actionType);
    
    /**
     * Récupère les logs par statut
     */
    List<AuditLog> findByStatus(AuditLog.ActionStatus status);
    
    /**
     * Récupère les logs dans une période
     */
    List<AuditLog> findByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Recherche dans les logs
     */
    List<AuditLog> search(String keyword);
    
    /**
     * Compte le nombre total de logs
     */
    long count();
    
    /**
     * Compte les logs par type d'action
     */
    long countByActionType(AuditLog.ActionType actionType);
    
    /**
     * Récupère les derniers logs
     */
    List<AuditLog> findRecent(int limit);
    
    /**
     * Récupère les logs d'aujourd'hui
     */
    List<AuditLog> findToday();
    
    /**
     * Récupère les tentatives de connexion échouées récentes
     */
    List<AuditLog> findRecentFailedLogins(int limit);
    
    /**
     * Récupère tous les logs (sans pagination)
     */
    List<AuditLog> findAll();
    
    /**
     * Compte les logs d'aujourd'hui
     */
    long countToday();
    
    /**
     * Compte les logs par statut
     */
    long countByStatus(AuditLog.ActionStatus status);
    
    /**
     * Supprime les logs plus anciens que X jours
     */
    int deleteOlderThan(int days);
}
