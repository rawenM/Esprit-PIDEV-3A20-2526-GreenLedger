package Services;

import Models.Notification;
import Models.Projet;
import DataBase.MyConnection;
import Utils.EventBusManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * Notification Service
 * 
 * Manages user notifications with real-time broadcasting
 * 
 * Notification types:
 * - PROJECT_STATUS: Project status changed
 * - PROJECT_FUNDED: Project received funding
 * - INVESTMENT_SUCCESS: Investment completed successfully
 * - EVALUATION_COMPLETE: Expert evaluation completed
 * - FRAUD_ALERT: Fraud detected
 * - CREDITS_MINTED: Green credits minted
 * 
 * @author GreenLedger Team
 */
public class NotificationService {
    
    private final ProjetService projetService;
    
    public NotificationService() {
        this.projetService = new ProjetService();
    }
    
    /**
     * Send notification to user
     * 
     * @param userId User ID
     * @param type Notification type
     * @param message Notification message
     * @param redirectUrl URL to redirect when clicked
     */
    public void notify(Integer userId, String type, String message, String redirectUrl) {
        System.out.println(String.format(
            "[Notification] Sending to user %d: [%s] %s",
            userId, type, message
        ));
        
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setMessage(message);
        notification.setRedirectUrl(redirectUrl);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        
        // Store in database
        boolean stored = storeNotification(notification);
        
        if (stored) {
            // Broadcast via EventBus for real-time updates
            EventBusManager.post(new NotificationEvent(notification));
            System.out.println("[Notification] Sent successfully");
        } else {
            System.err.println("[Notification] Failed to store notification");
        }
    }
    
    /**
     * Notify project status change
     */
    public void notifyStatusChange(Integer projectId, String oldStatus, String newStatus) {
        Projet project = projetService.getById(projectId);
        if (project == null) {
            return;
        }
        
        String message = String.format(
            "Project '%s' status changed from %s to %s",
            project.getTitre(), 
            formatStatus(oldStatus), 
            formatStatus(newStatus)
        );
        
        notify(
            project.getEntrepriseId(), 
            "PROJECT_STATUS", 
            message, 
            "/projects/" + projectId
        );
    }
    
    /**
     * Notify project funded
     */
    public void notifyProjectFunded(Integer projectId, Integer investorId, double amount) {
        Projet project = projetService.getById(projectId);
        if (project == null) {
            return;
        }
        
        // Notify project owner
        String ownerMessage = String.format(
            "Your project '%s' received funding of %.2f TND",
            project.getTitre(), amount
        );
        notify(
            project.getEntrepriseId(), 
            "PROJECT_FUNDED", 
            ownerMessage, 
            "/messages"
        );
        
        // Notify investor
        String investorMessage = String.format(
            "Your investment of %.2f TND in '%s' was successful",
            amount, project.getTitre()
        );
        notify(
            investorId, 
            "INVESTMENT_SUCCESS", 
            investorMessage, 
            "/messages"
        );
    }
    
    /**
     * Notify evaluation complete
     */
    public void notifyEvaluationComplete(Integer projectId, Integer expertId, Integer esgScore) {
        Projet project = projetService.getById(projectId);
        if (project == null) {
            return;
        }
        
        String message = String.format(
            "Expert evaluation completed for '%s' - ESG Score: %d/10",
            project.getTitre(), esgScore
        );
        
        notify(
            project.getEntrepriseId(), 
            "EVALUATION_COMPLETE", 
            message, 
            "/projects/" + projectId
        );
    }
    
    /**
     * Notify fraud alert
     */
    public void notifyFraudAlert(Integer projectId, double riskScore) {
        Projet project = projetService.getById(projectId);
        if (project == null) {
            return;
        }
        
        String message = String.format(
            "Fraud alert for project '%s' - Risk Score: %.0f%%",
            project.getTitre(), riskScore * 100
        );
        
        // Notify project owner
        notify(
            project.getEntrepriseId(), 
            "FRAUD_ALERT", 
            message, 
            "/projects/" + projectId
        );
        
        // Notify admins (would need to get admin user IDs)
        // For now, just log
        System.out.println("[Notification] Fraud alert should be sent to admins");
    }
    
    /**
     * Notify green credits minted
     */
    public void notifyCreditsMinted(Integer projectId, double credits) {
        Projet project = projetService.getById(projectId);
        if (project == null) {
            return;
        }
        
        String message = String.format(
            "Green credits minted for '%s': %.2f credits",
            project.getTitre(), credits
        );
        
        notify(
            project.getEntrepriseId(), 
            "CREDITS_MINTED", 
            message, 
            "/projects/" + projectId
        );
    }
    
    /**
     * Store notification in database
     */
    private boolean storeNotification(Notification notification) {
        String sql = "INSERT INTO notifications " +
                    "(user_id, type, message, redirect_url, is_read, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, notification.getUserId());
            ps.setString(2, notification.getType());
            ps.setString(3, notification.getMessage());
            ps.setString(4, notification.getRedirectUrl());
            ps.setBoolean(5, notification.isRead());
            ps.setObject(6, notification.getCreatedAt());
            
            int rows = ps.executeUpdate();
            return rows > 0;
            
        } catch (SQLException e) {
            System.err.println("[Notification] Failed to store: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Format status for display
     */
    private String formatStatus(String status) {
        if (status == null) return "Unknown";
        
        switch (status.toUpperCase()) {
            case "DRAFT": return "Draft";
            case "SUBMITTED": return "Submitted";
            case "IN_PROGRESS": return "In Progress";
            case "APPROVED": return "Approved";
            case "REJECTED": return "Rejected";
            case "FUNDED": return "Funded";
            case "CANCELLED": return "Cancelled";
            default: return status;
        }
    }
    
    /**
     * Notification event for EventBus
     */
    public static class NotificationEvent {
        private final Notification notification;
        
        public NotificationEvent(Notification notification) {
            this.notification = notification;
        }
        
        public Notification getNotification() {
            return notification;
        }
    }
}
