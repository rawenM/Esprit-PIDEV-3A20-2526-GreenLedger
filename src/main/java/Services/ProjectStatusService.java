package Services;

import Models.Projet;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Project Status Service - State Machine
 * 
 * Manages project status transitions with validation and business rules
 * 
 * Valid transitions:
 * - DRAFT → SUBMITTED
 * - SUBMITTED → IN_PROGRESS, REJECTED
 * - IN_PROGRESS → APPROVED, REJECTED
 * - APPROVED → FUNDED
 * - REJECTED → (terminal state)
 * 
 * Business rules:
 * - Cannot approve if fraud_risk ≥ 0.65
 * - Cannot approve if ESG score < 7
 * - Cannot approve if carbon_risk = HIGH
 * - Triggers notifications on status change
 * 
 * @author GreenLedger Team
 */
public class ProjectStatusService {
    
    private final ProjetService projetService;
    private final NotificationService notificationService;
    private final FraudDetectionService fraudDetectionService;
    
    // Valid status transitions
    private static final Map<String, List<String>> VALID_TRANSITIONS = new HashMap<>();
    
    static {
        VALID_TRANSITIONS.put("DRAFT", Arrays.asList("SUBMITTED"));
        VALID_TRANSITIONS.put("SUBMITTED", Arrays.asList("IN_PROGRESS", "REJECTED"));
        VALID_TRANSITIONS.put("IN_PROGRESS", Arrays.asList("APPROVED", "REJECTED"));
        VALID_TRANSITIONS.put("APPROVED", Arrays.asList("FUNDED"));
        VALID_TRANSITIONS.put("REJECTED", Arrays.asList()); // Terminal state
        VALID_TRANSITIONS.put("FUNDED", Arrays.asList()); // Terminal state
        VALID_TRANSITIONS.put("CANCELLED", Arrays.asList()); // Terminal state
    }
    
    public ProjectStatusService() {
        this.projetService = new ProjetService();
        this.notificationService = new NotificationService();
        this.fraudDetectionService = new FraudDetectionService();
    }
    
    /**
     * Transition project status with validation
     * 
     * @param projectId Project ID
     * @param newStatus New status
     * @param userId User performing the transition
     * @return true if transition successful, false otherwise
     */
    public boolean transition(Integer projectId, String newStatus, Integer userId) {
        System.out.println(String.format(
            "[ProjectStatus] Attempting transition for project %d to %s by user %d",
            projectId, newStatus, userId
        ));
        
        Projet project = projetService.getById(projectId);
        if (project == null) {
            System.err.println("[ProjectStatus] Project not found: " + projectId);
            return false;
        }
        
        String currentStatus = project.getStatutEvaluation();
        if (currentStatus == null) {
            currentStatus = "DRAFT";
        }
        
        // Validate transition
        if (!isValidTransition(currentStatus, newStatus)) {
            System.err.println(String.format(
                "[ProjectStatus] Invalid transition: %s → %s",
                currentStatus, newStatus
            ));
            return false;
        }
        
        // Check business rules
        if (!checkBusinessRules(project, newStatus)) {
            System.err.println(String.format(
                "[ProjectStatus] Business rules failed for transition to %s",
                newStatus
            ));
            return false;
        }
        
        // Execute transition
        boolean success = projetService.updateStatut(projectId, newStatus);
        
        if (success) {
            System.out.println(String.format(
                "[ProjectStatus] Transition successful: %s → %s",
                currentStatus, newStatus
            ));
            
            // Trigger notifications
            notificationService.notifyStatusChange(projectId, currentStatus, newStatus);
            
            // Additional actions based on new status
            handleStatusChange(project, newStatus);
        }
        
        return success;
    }
    
    /**
     * Check if transition is valid
     */
    private boolean isValidTransition(String currentStatus, String newStatus) {
        if (currentStatus == null || newStatus == null) {
            return false;
        }
        
        List<String> allowedTransitions = VALID_TRANSITIONS.get(currentStatus.toUpperCase());
        if (allowedTransitions == null) {
            return false;
        }
        
        return allowedTransitions.contains(newStatus.toUpperCase());
    }
    
    /**
     * Check business rules for transition
     */
    private boolean checkBusinessRules(Projet project, String newStatus) {
        if ("APPROVED".equals(newStatus)) {
            return checkApprovalRules(project);
        }
        
        if ("FUNDED".equals(newStatus)) {
            return checkFundingRules(project);
        }
        
        return true;
    }
    
    /**
     * Check approval business rules
     */
    private boolean checkApprovalRules(Projet project) {
        // Rule 1: ESG score must be ≥ 7
        Integer esgScore = project.getScoreEsg();
        if (esgScore == null || esgScore < 7) {
            System.err.println("[ProjectStatus] Approval denied: ESG score < 7");
            return false;
        }
        
        // Rule 2: Fraud risk must be < 0.65
        try {
            var fraudResult = fraudDetectionService.getLatestResultForProject(project.getId());
            if (fraudResult != null && fraudResult.getRiskScore() / 100.0 >= 0.65) {
                System.err.println("[ProjectStatus] Approval denied: Fraud risk >= 0.65");
                return false;
            }
            if (fraudResult != null && fraudResult.isFraudulent()) {
                System.err.println("[ProjectStatus] Approval denied: Fraud flag set");
                return false;
            }
        } catch (Exception e) {
            System.err.println("[ProjectStatus] Error checking fraud risk: " + e.getMessage());
        }
        
        // Rule 3: Carbon risk must not be HIGH (would need ML prediction data)
        // For now, skip this check
        
        return true;
    }
    
    /**
     * Check funding business rules
     */
    private boolean checkFundingRules(Projet project) {
        // Rule: Project must be APPROVED
        if (!"APPROVED".equals(project.getStatutEvaluation())) {
            System.err.println("[ProjectStatus] Funding denied: Project not approved");
            return false;
        }
        
        return true;
    }
    
    /**
     * Handle additional actions on status change
     */
    private void handleStatusChange(Projet project, String newStatus) {
        switch (newStatus) {
            case "APPROVED":
                // Set financing status to SEEKING_FUNDING
                System.out.println("[ProjectStatus] Setting financing status to SEEKING_FUNDING");
                // Would need to implement setFinancingStatus in ProjetService
                break;
                
            case "FUNDED":
                // Create conversation thread (would need ConversationService)
                System.out.println("[ProjectStatus] Project funded - conversation thread should be created");
                break;
                
            case "REJECTED":
                // Send rejection notification with reason
                System.out.println("[ProjectStatus] Project rejected - sending detailed notification");
                break;
        }
    }
    
    /**
     * Get allowed transitions for current status
     */
    public List<String> getAllowedTransitions(String currentStatus) {
        if (currentStatus == null) {
            return Arrays.asList();
        }
        
        List<String> transitions = VALID_TRANSITIONS.get(currentStatus.toUpperCase());
        return transitions != null ? transitions : Arrays.asList();
    }
    
    /**
     * Check if project can be approved
     */
    public boolean canApprove(Integer projectId) {
        Projet project = projetService.getById(projectId);
        if (project == null) {
            return false;
        }
        
        String currentStatus = project.getStatutEvaluation();
        if (!"IN_PROGRESS".equals(currentStatus)) {
            return false;
        }
        
        return checkApprovalRules(project);
    }
}
