package Services;

import Models.AuditLog;
import Models.User;
import dao.AuditLogDAO;
import dao.AuditLogDAOImpl;

/**
 * Service pour le journal d'activité
 * IMPORTANT: L'enregistrement est AUTOMATIQUE
 * L'admin ne peut PAS désactiver cette fonctionnalité
 * C'est essentiel pour la conformité RGPD et la sécurité
 */
public class AuditLogService {
    
    private static AuditLogService instance;
    private final AuditLogDAO auditLogDAO;
    
    private AuditLogService() {
        this.auditLogDAO = new AuditLogDAOImpl();
    }
    
    public static synchronized AuditLogService getInstance() {
        if (instance == null) {
            instance = new AuditLogService();
        }
        return instance;
    }
    
    /**
     * Enregistre une action dans le journal
     * Cette méthode est appelée AUTOMATIQUEMENT par les autres services
     */
    public void log(AuditLog auditLog) {
        try {
            auditLogDAO.log(auditLog);
            System.out.println("[AUDIT] " + auditLog.toString());
        } catch (Exception e) {
            System.err.println("[AUDIT] Erreur lors de l'enregistrement: " + e.getMessage());
        }
    }
    
    // ============================================
    // MÉTHODES PRATIQUES POUR ENREGISTRER LES ACTIONS
    // ============================================
    
    /**
     * Enregistre une connexion réussie
     */
    public void logLogin(User user, String ipAddress) {
        AuditLog log = new AuditLog(
            user.getId(),
            user.getEmail(),
            user.getNomComplet(),
            AuditLog.ActionType.USER_LOGIN,
            "Connexion réussie"
        );
        log.setIpAddress(ipAddress);
        log.setStatus(AuditLog.ActionStatus.SUCCESS);
        log(log);
    }
    
    /**
     * Enregistre une tentative de connexion échouée
     */
    public void logLoginFailed(String email, String reason, String ipAddress) {
        AuditLog log = new AuditLog(
            null,
            email,
            email,
            AuditLog.ActionType.USER_LOGIN_FAILED,
            "Tentative de connexion échouée: " + reason
        );
        log.setIpAddress(ipAddress);
        log.setStatus(AuditLog.ActionStatus.FAILED);
        log.setErrorMessage(reason);
        log(log);
    }
    
    /**
     * Enregistre une déconnexion
     */
    public void logLogout(User user) {
        AuditLog log = new AuditLog(
            user.getId(),
            user.getEmail(),
            user.getNomComplet(),
            AuditLog.ActionType.USER_LOGOUT,
            "Déconnexion"
        );
        log(log);
    }
    
    /**
     * Enregistre une inscription
     */
    public void logRegister(User user, String ipAddress) {
        AuditLog log = new AuditLog(
            user.getId(),
            user.getEmail(),
            user.getNomComplet(),
            AuditLog.ActionType.USER_REGISTER,
            "Nouvelle inscription"
        );
        log.setIpAddress(ipAddress);
        log(log);
    }
    
    /**
     * Enregistre une modification de profil
     */
    public void logProfileUpdate(User user, String fieldChanged, String oldValue, String newValue) {
        AuditLog log = new AuditLog(
            user.getId(),
            user.getEmail(),
            user.getNomComplet(),
            AuditLog.ActionType.USER_PROFILE_UPDATE,
            "Modification du champ: " + fieldChanged
        );
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log(log);
    }
    
    /**
     * Enregistre un changement de mot de passe
     */
    public void logPasswordChange(User user) {
        AuditLog log = new AuditLog(
            user.getId(),
            user.getEmail(),
            user.getNomComplet(),
            AuditLog.ActionType.USER_PASSWORD_CHANGE,
            "Changement de mot de passe"
        );
        log(log);
    }
    
    /**
     * Enregistre une réinitialisation de mot de passe
     */
    public void logPasswordReset(String email) {
        AuditLog log = new AuditLog(
            null,
            email,
            email,
            AuditLog.ActionType.USER_PASSWORD_RESET,
            "Réinitialisation de mot de passe"
        );
        log(log);
    }
    
    /**
     * Enregistre une validation de compte par l'admin
     */
    public void logAdminValidateUser(User admin, User targetUser) {
        AuditLog log = new AuditLog(
            admin.getId(),
            admin.getEmail(),
            admin.getNomComplet(),
            AuditLog.ActionType.ADMIN_USER_VALIDATE,
            "Validation du compte de " + targetUser.getNomComplet()
        );
        log.setTargetUserId(targetUser.getId());
        log.setTargetUserEmail(targetUser.getEmail());
        log(log);
    }
    
    /**
     * Enregistre un blocage d'utilisateur par l'admin
     */
    public void logAdminBlockUser(User admin, User targetUser) {
        AuditLog log = new AuditLog(
            admin.getId(),
            admin.getEmail(),
            admin.getNomComplet(),
            AuditLog.ActionType.ADMIN_USER_BLOCK,
            "Blocage de l'utilisateur " + targetUser.getNomComplet()
        );
        log.setTargetUserId(targetUser.getId());
        log.setTargetUserEmail(targetUser.getEmail());
        log(log);
    }
    
    /**
     * Enregistre un déblocage d'utilisateur par l'admin
     */
    public void logAdminUnblockUser(User admin, User targetUser) {
        AuditLog log = new AuditLog(
            admin.getId(),
            admin.getEmail(),
            admin.getNomComplet(),
            AuditLog.ActionType.ADMIN_USER_UNBLOCK,
            "Déblocage de l'utilisateur " + targetUser.getNomComplet()
        );
        log.setTargetUserId(targetUser.getId());
        log.setTargetUserEmail(targetUser.getEmail());
        log(log);
    }
    
    /**
     * Enregistre une suppression d'utilisateur par l'admin
     */
    public void logAdminDeleteUser(User admin, User targetUser) {
        AuditLog log = new AuditLog(
            admin.getId(),
            admin.getEmail(),
            admin.getNomComplet(),
            AuditLog.ActionType.ADMIN_USER_DELETE,
            "Suppression de l'utilisateur " + targetUser.getNomComplet()
        );
        log.setTargetUserId(targetUser.getId());
        log.setTargetUserEmail(targetUser.getEmail());
        log.setStatus(AuditLog.ActionStatus.WARNING);
        log(log);
    }
    
    /**
     * Enregistre une consultation de détails de fraude
     */
    public void logAdminViewFraud(User admin, User targetUser) {
        AuditLog log = new AuditLog(
            admin.getId(),
            admin.getEmail(),
            admin.getNomComplet(),
            AuditLog.ActionType.ADMIN_VIEW_FRAUD,
            "Consultation des détails de fraude de " + targetUser.getNomComplet()
        );
        log.setTargetUserId(targetUser.getId());
        log.setTargetUserEmail(targetUser.getEmail());
        log(log);
    }
    
    /**
     * Enregistre une détection de fraude
     */
    public void logFraudDetected(User user, int fraudScore, String ipAddress) {
        AuditLog log = new AuditLog(
            user.getId(),
            user.getEmail(),
            user.getNomComplet(),
            AuditLog.ActionType.FRAUD_DETECTED,
            "Fraude détectée - Score: " + fraudScore + "/100"
        );
        log.setIpAddress(ipAddress);
        log.setStatus(AuditLog.ActionStatus.WARNING);
        log(log);
    }
    
    /**
     * Enregistre un échec de CAPTCHA
     */
    public void logCaptchaFailed(String email, String ipAddress) {
        AuditLog log = new AuditLog(
            null,
            email,
            email,
            AuditLog.ActionType.CAPTCHA_FAILED,
            "Échec de la vérification CAPTCHA"
        );
        log.setIpAddress(ipAddress);
        log.setStatus(AuditLog.ActionStatus.FAILED);
        log(log);
    }
    
    /**
     * Enregistre un succès de CAPTCHA
     */
    public void logCaptchaSuccess(String email, String ipAddress) {
        AuditLog log = new AuditLog(
            null,
            email,
            email,
            AuditLog.ActionType.CAPTCHA_SUCCESS,
            "Vérification CAPTCHA réussie"
        );
        log.setIpAddress(ipAddress);
        log(log);
    }
    
    // ============================================
    // MÉTHODES DE CONSULTATION (POUR L'ADMIN)
    // ============================================
    
    public AuditLogDAO getDAO() {
        return auditLogDAO;
    }
}
