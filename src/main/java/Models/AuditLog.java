package Models;

import java.time.LocalDateTime;

/**
 * Modèle pour le journal d'activité (Audit Log)
 * Enregistre toutes les actions des utilisateurs et administrateurs
 * IMPORTANT: L'enregistrement est AUTOMATIQUE et ne peut pas être désactivé
 */
public class AuditLog {
    
    private Long id;
    
    // Qui a fait l'action?
    private Long userId;
    private String userEmail;
    private String userName;
    
    // Quelle action?
    private ActionType actionType;
    private String actionDescription;
    
    // Sur qui/quoi?
    private Long targetUserId;
    private String targetUserEmail;
    
    // Détails techniques
    private String ipAddress;
    private String userAgent;
    private String browser;
    private String operatingSystem;
    
    // Résultat
    private ActionStatus status;
    private String errorMessage;
    
    // Données avant/après
    private String oldValue;
    private String newValue;
    
    // Timestamp
    private LocalDateTime createdAt;
    
    // Constructeurs
    public AuditLog() {
        this.createdAt = LocalDateTime.now();
        this.status = ActionStatus.SUCCESS;
    }
    
    public AuditLog(Long userId, String userEmail, String userName, ActionType actionType, String actionDescription) {
        this();
        this.userId = userId;
        this.userEmail = userEmail;
        this.userName = userName;
        this.actionType = actionType;
        this.actionDescription = actionDescription;
    }
    
    // Enum pour les types d'actions
    public enum ActionType {
        USER_LOGIN("Connexion"),
        USER_LOGIN_FAILED("Tentative de connexion échouée"),
        USER_LOGOUT("Déconnexion"),
        USER_REGISTER("Inscription"),
        USER_PROFILE_UPDATE("Modification de profil"),
        USER_PASSWORD_CHANGE("Changement de mot de passe"),
        USER_PASSWORD_RESET("Réinitialisation de mot de passe"),
        USER_EMAIL_CHANGE("Changement d'email"),
        ADMIN_USER_VALIDATE("Validation de compte"),
        ADMIN_USER_BLOCK("Blocage d'utilisateur"),
        ADMIN_USER_UNBLOCK("Déblocage d'utilisateur"),
        ADMIN_USER_DELETE("Suppression d'utilisateur"),
        ADMIN_VIEW_FRAUD("Consultation détails fraude"),
        FRAUD_DETECTED("Fraude détectée"),
        CAPTCHA_FAILED("Échec CAPTCHA"),
        CAPTCHA_SUCCESS("Succès CAPTCHA"),
        SESSION_EXPIRED("Session expirée"),
        UNAUTHORIZED_ACCESS("Accès non autorisé");
        
        private final String label;
        
        ActionType(String label) {
            this.label = label;
        }
        
        public String getLabel() {
            return label;
        }
    }
    
    // Enum pour le statut
    public enum ActionStatus {
        SUCCESS("Succès", "#10B981"),
        FAILED("Échec", "#EF4444"),
        WARNING("Avertissement", "#F59E0B");
        
        private final String label;
        private final String color;
        
        ActionStatus(String label, String color) {
            this.label = label;
            this.color = color;
        }
        
        public String getLabel() {
            return label;
        }
        
        public String getColor() {
            return color;
        }
    }
    
    // Getters et Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getUserEmail() {
        return userEmail;
    }
    
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public ActionType getActionType() {
        return actionType;
    }
    
    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }
    
    public String getActionDescription() {
        return actionDescription;
    }
    
    public void setActionDescription(String actionDescription) {
        this.actionDescription = actionDescription;
    }
    
    public Long getTargetUserId() {
        return targetUserId;
    }
    
    public void setTargetUserId(Long targetUserId) {
        this.targetUserId = targetUserId;
    }
    
    public String getTargetUserEmail() {
        return targetUserEmail;
    }
    
    public void setTargetUserEmail(String targetUserEmail) {
        this.targetUserEmail = targetUserEmail;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public String getBrowser() {
        return browser;
    }
    
    public void setBrowser(String browser) {
        this.browser = browser;
    }
    
    public String getOperatingSystem() {
        return operatingSystem;
    }
    
    public void setOperatingSystem(String operatingSystem) {
        this.operatingSystem = operatingSystem;
    }
    
    public ActionStatus getStatus() {
        return status;
    }
    
    public void setStatus(ActionStatus status) {
        this.status = status;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getOldValue() {
        return oldValue;
    }
    
    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }
    
    public String getNewValue() {
        return newValue;
    }
    
    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    // Méthodes utilitaires
    public String getFormattedDate() {
        if (createdAt == null) return "";
        return createdAt.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }
    
    public String getActionIcon() {
        if (actionType == null) return "📝";
        
        switch (actionType) {
            case USER_LOGIN:
            case USER_LOGIN_FAILED:
                return "🔐";
            case USER_LOGOUT:
                return "🚪";
            case USER_REGISTER:
                return "✍️";
            case USER_PROFILE_UPDATE:
            case USER_EMAIL_CHANGE:
                return "✏️";
            case USER_PASSWORD_CHANGE:
            case USER_PASSWORD_RESET:
                return "🔑";
            case ADMIN_USER_VALIDATE:
                return "✅";
            case ADMIN_USER_BLOCK:
                return "⛔";
            case ADMIN_USER_UNBLOCK:
                return "🔓";
            case ADMIN_USER_DELETE:
                return "🗑️";
            case ADMIN_VIEW_FRAUD:
                return "📊";
            case FRAUD_DETECTED:
                return "⚠️";
            case CAPTCHA_FAILED:
            case CAPTCHA_SUCCESS:
                return "🤖";
            case SESSION_EXPIRED:
                return "⏰";
            case UNAUTHORIZED_ACCESS:
                return "🚫";
            default:
                return "📝";
        }
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s - %s: %s", 
            getFormattedDate(), 
            userName != null ? userName : "Système",
            actionType != null ? actionType.getLabel() : "Action",
            actionDescription != null ? actionDescription : ""
        );
    }
}
