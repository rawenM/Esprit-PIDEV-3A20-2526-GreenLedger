# Symfony to Java Implementation Guide

## Overview

This document maps the Symfony PHP application features to the Java desktop application and identifies what needs to be implemented.

## ✅ Already Implemented

### Core Models
- ✅ User (with fraud detection)
- ✅ Projet (Project)
- ✅ Evaluation
- ✅ Financement (Financing)
- ✅ Budget
- ✅ MlPrediction
- ✅ FraudDetectionResult

### Services
- ✅ FraudDetectionService
- ✅ ClimatiqApiService (Carbon emissions API)
- ✅ MlPredictionService
- ✅ ProjetService
- ✅ FinancementService
- ✅ UserServiceImpl
- ✅ UnifiedEmailService (Gmail API)

### Controllers
- ✅ AdminUsersController (with fraud detection)
- ✅ ExpertProjetController
- ✅ FinancementController
- ✅ ProjetController

## ❌ Missing Features from Symfony

### 1. ExpertWorkflowService - ML Analysis Engine

**Symfony Implementation:**
- `buildSnapshot()` - Gathers all project data
- `runAnalysis()` - Runs complete ML pipeline
- Carbon metric calculation (Climatiq API + fallback)
- ML prediction (ESG score, credibility, carbon risk)
- Fraud assessment
- Green credit dispatch

**Java Implementation Needed:**

```java
package Services;

public class ExpertWorkflowService {
    
    private final ClimatiqApiService climatiqApi;
    private final MlPredictionService mlPrediction;
    private final FraudDetectionService fraudDetection;
    private final CarbonCreditDispatchService creditDispatch;
    
    /**
     * Build complete snapshot of project data for ML analysis
     */
    public MlDecisionSnapshot buildSnapshot(Integer projectId, Integer expertId) {
        // Gather all project data
        // Include environmental metrics
        // Include company information
        // Include historical data
    }
    
    /**
     * Run complete ML analysis pipeline
     */
    public AnalysisResult runAnalysis(Integer projectId, Integer expertId) {
        // 1. Build snapshot
        // 2. Calculate carbon metrics
        // 3. Run ML prediction
        // 4. Assess fraud risk
        // 5. Calculate green credits
        // 6. Generate recommendations
    }
    
    /**
     * Calculate carbon metrics with Climatiq API
     */
    private CarbonMetrics calculateCarbonMetrics(Projet project) {
        // Energy emissions
        // Transport emissions
        // Material emissions
        // Waste emissions
        // Total baseline vs actual
    }
}
```

### 2. ProjectStatusService - State Machine

**Symfony Implementation:**
- `transition()` - Validates and executes status changes
- Triggers notifications on status changes
- Enforces business rules

**Java Implementation Needed:**

```java
package Services;

public class ProjectStatusService {
    
    private final NotificationService notificationService;
    private final ProjetService projetService;
    
    /**
     * Valid status transitions
     */
    private static final Map<String, List<String>> VALID_TRANSITIONS = Map.of(
        "DRAFT", List.of("SUBMITTED"),
        "SUBMITTED", List.of("IN_PROGRESS", "REJECTED"),
        "IN_PROGRESS", List.of("APPROVED", "REJECTED"),
        "APPROVED", List.of("FUNDED"),
        "REJECTED", List.of()
    );
    
    /**
     * Transition project status with validation
     */
    public boolean transition(Integer projectId, String newStatus, Integer userId) {
        String currentStatus = projetService.getStatutById(projectId);
        
        // Validate transition
        if (!isValidTransition(currentStatus, newStatus)) {
            return false;
        }
        
        // Check business rules
        if (!checkBusinessRules(projectId, newStatus)) {
            return false;
        }
        
        // Execute transition
        boolean success = projetService.updateStatut(projectId, newStatus);
        
        // Trigger notifications
        if (success) {
            notificationService.notifyStatusChange(projectId, currentStatus, newStatus);
        }
        
        return success;
    }
    
    private boolean checkBusinessRules(Integer projectId, String newStatus) {
        if ("APPROVED".equals(newStatus)) {
            // Check fraud risk
            // Check ESG score
            // Check carbon risk
        }
        return true;
    }
}
```

### 3. CarbonCreditDispatchService - Green Credits

**Symfony Implementation:**
- `computeAndStoreForProjectId()` - Calculates green credits
- Formula: credits = avoided_tco2 * credibility_factor * esg_multiplier
- Eligibility checks

**Java Implementation Needed:**

```java
package Services;

public class CarbonCreditDispatchService {
    
    /**
     * Calculate and store green credits for approved project
     */
    public CarbonCreditResult computeAndStoreForProjectId(Integer projectId) {
        Projet project = projetService.getById(projectId);
        
        // Eligibility checks
        if (!"APPROVED".equals(project.getStatutEvaluation())) {
            return CarbonCreditResult.notEligible("Project not approved");
        }
        
        // Get carbon metrics
        double avoidedTco2 = project.getAvoidedTco2();
        if (avoidedTco2 <= 0.5) {
            return CarbonCreditResult.notEligible("Insufficient avoided emissions");
        }
        
        // Get ML prediction
        MlPrediction prediction = mlPredictionService.getByProjectId(projectId);
        double credibilityFactor = prediction.getCredibilityScore() / 100.0;
        
        // Get ESG score
        double esgMultiplier = calculateEsgMultiplier(project.getScoreEsg());
        
        // Calculate credits
        double credits = avoidedTco2 * credibilityFactor * esgMultiplier;
        
        // Store in database
        storeCarbonCredits(projectId, credits, avoidedTco2, credibilityFactor, esgMultiplier);
        
        return CarbonCreditResult.success(credits);
    }
    
    private double calculateEsgMultiplier(Integer esgScore) {
        if (esgScore == null) return 1.0;
        if (esgScore >= 9) return 1.2;
        if (esgScore >= 7) return 1.0;
        return 0.8;
    }
}
```

### 4. ConversationService - Messaging System

**Symfony Implementation:**
- `createThreadForFundedProject()` - Auto-creates thread after funding
- `sendMessage()` - Sends message + broadcasts via Pusher
- Real-time updates using Pusher private channels

**Java Implementation Needed:**

```java
package Services;

public class ConversationService {
    
    private final PusherService pusherService;
    
    /**
     * Create conversation thread after successful funding
     */
    public ConversationThread createThreadForFundedProject(Integer financementId) {
        Financement financement = financementService.getById(financementId);
        
        ConversationThread thread = new ConversationThread();
        thread.setProjectId(financement.getProjectId());
        thread.setInvestorId(financement.getInvestorId());
        thread.setProjectOwnerId(financement.getProjectOwnerId());
        thread.setFinancementId(financementId);
        thread.setCreatedAt(LocalDateTime.now());
        
        // Store in database
        conversationDAO.insertThread(thread);
        
        // Send welcome message
        sendWelcomeMessage(thread);
        
        return thread;
    }
    
    /**
     * Send message in thread
     */
    public ThreadMessage sendMessage(Integer threadId, Integer senderId, String content) {
        ThreadMessage message = new ThreadMessage();
        message.setThreadId(threadId);
        message.setSenderId(senderId);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        
        // Store in database
        conversationDAO.insertMessage(message);
        
        // Broadcast via Pusher (or use EventBus for desktop app)
        EventBusManager.getInstance().post(new MessageSentEvent(message));
        
        return message;
    }
}
```

### 5. NotificationService - Notification System

**Symfony Implementation:**
- `notify()` - Creates notification + broadcasts via Pusher
- Role-specific notifications
- All notifications redirect to /front-office/messages

**Java Implementation Needed:**

```java
package Services;

public class NotificationService {
    
    /**
     * Send notification to user
     */
    public void notify(Integer userId, String type, String message, String redirectUrl) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setMessage(message);
        notification.setRedirectUrl(redirectUrl);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        
        // Store in database
        notificationDAO.insert(notification);
        
        // Broadcast via EventBus
        EventBusManager.getInstance().post(new NotificationEvent(notification));
    }
    
    /**
     * Notify project status change
     */
    public void notifyStatusChange(Integer projectId, String oldStatus, String newStatus) {
        Projet project = projetService.getById(projectId);
        
        String message = String.format(
            "Project '%s' status changed from %s to %s",
            project.getTitre(), oldStatus, newStatus
        );
        
        notify(project.getEntrepriseId(), "PROJECT_STATUS", message, "/projects/" + projectId);
    }
    
    /**
     * Notify project funded
     */
    public void notifyProjectFunded(Integer projectId, Integer investorId, double amount) {
        Projet project = projetService.getById(projectId);
        
        // Notify project owner
        String ownerMessage = String.format(
            "Your project '%s' received funding of %.2f",
            project.getTitre(), amount
        );
        notify(project.getEntrepriseId(), "PROJECT_FUNDED", ownerMessage, "/messages");
        
        // Notify investor
        String investorMessage = String.format(
            "Your investment in '%s' was successful",
            project.getTitre()
        );
        notify(investorId, "INVESTMENT_SUCCESS", investorMessage, "/messages");
    }
}
```

### 6. Enhanced Emission Calculations

**Symfony Emission Factors (Realistic Values):**
- Energy: 0.0005 tCO2e/kWh (auto-scales MWh)
- Transport: 0.0001 tCO2e/km
- Material: 2.0 tCO2e/tonne
- Waste: 0.5 tCO2e/tonne

**Java Implementation Enhancement:**

```java
package Services;

public class EnhancedEmissionCalculator {
    
    private static final double ENERGY_FACTOR = 0.0005; // tCO2e/kWh
    private static final double TRANSPORT_FACTOR = 0.0001; // tCO2e/km
    private static final double MATERIAL_FACTOR = 2.0; // tCO2e/tonne
    private static final double WASTE_FACTOR = 0.5; // tCO2e/tonne
    
    /**
     * Calculate total emissions with smart unit detection
     */
    public EmissionResult calculateProjectEmissions(Projet project) {
        double totalEmissions = 0.0;
        
        // Energy (auto-scale if < 10000)
        double energy = project.getEnergyConsumption();
        if (energy > 0 && energy < 10000) {
            energy = energy * 1000; // Convert MWh to kWh
        }
        double energyEmissions = energy * ENERGY_FACTOR;
        
        // Transport
        double transportEmissions = project.getTransportKm() * TRANSPORT_FACTOR;
        
        // Materials
        double materialEmissions = project.getMaterialTonnes() * MATERIAL_FACTOR;
        
        // Waste
        double wasteEmissions = project.getWasteTonnes() * WASTE_FACTOR;
        
        totalEmissions = energyEmissions + transportEmissions + materialEmissions + wasteEmissions;
        
        return new EmissionResult(totalEmissions, energyEmissions, transportEmissions, 
                                  materialEmissions, wasteEmissions);
    }
    
    /**
     * Calculate avoided emissions
     */
    public double calculateAvoidedEmissions(double baselineEmissions, double actualEmissions) {
        return Math.max(0, baselineEmissions - actualEmissions);
    }
}
```

## 📊 Database Schema Updates Needed

### 1. Conversation Tables

```sql
CREATE TABLE IF NOT EXISTS conversation_threads (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id INT NOT NULL,
    investor_id BIGINT NOT NULL,
    project_owner_id BIGINT NOT NULL,
    financement_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projet(id),
    FOREIGN KEY (investor_id) REFERENCES user(id),
    FOREIGN KEY (project_owner_id) REFERENCES user(id),
    FOREIGN KEY (financement_id) REFERENCES financements(id)
);

CREATE TABLE IF NOT EXISTS thread_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    thread_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (thread_id) REFERENCES conversation_threads(id),
    FOREIGN KEY (sender_id) REFERENCES user(id)
);
```

### 2. Notifications Table

```sql
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    redirect_url VARCHAR(255),
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id)
);
```

### 3. Carbon Credits Table

```sql
CREATE TABLE IF NOT EXISTS carbon_credits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id INT NOT NULL,
    credits_amount DOUBLE NOT NULL,
    avoided_tco2 DOUBLE NOT NULL,
    credibility_factor DOUBLE NOT NULL,
    esg_multiplier DOUBLE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projet(id)
);
```

### 4. Carbon Metrics Table

```sql
CREATE TABLE IF NOT EXISTS carbon_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id INT NOT NULL,
    baseline_tco2 DOUBLE,
    actual_tco2 DOUBLE,
    avoided_tco2 DOUBLE,
    energy_emissions DOUBLE,
    transport_emissions DOUBLE,
    material_emissions DOUBLE,
    waste_emissions DOUBLE,
    calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projet(id)
);
```

## 🔧 Implementation Priority

### Phase 1: Core Business Logic (Week 1)
1. ✅ ExpertWorkflowService - ML analysis engine
2. ✅ ProjectStatusService - State machine
3. ✅ EnhancedEmissionCalculator - Realistic emission factors

### Phase 2: Green Credits (Week 2)
1. ✅ CarbonCreditDispatchService
2. ✅ Database schema for carbon_credits
3. ✅ UI for displaying green credits

### Phase 3: Messaging & Notifications (Week 3)
1. ✅ ConversationService
2. ✅ NotificationService
3. ✅ Database schema for conversations and notifications
4. ✅ UI for messages and notifications

### Phase 4: Integration & Testing (Week 4)
1. ✅ Integration testing
2. ✅ End-to-end workflow testing
3. ✅ Performance optimization
4. ✅ Documentation

## 📝 Business Rules to Implement

### Project Approval Flow
1. Project must be SUBMITTED
2. Expert runs ML analysis
3. If ESG score ≥7 AND carbon_risk=LOW → APPROVED
4. If fraud_risk ≥0.65 OR fraud_flag=true → Cannot approve
5. Approved projects → statut_financement = SEEKING_FUNDING

### Emission Calculations
1. Climatiq API primary source
2. Fallback heuristics if API fails
3. Guardrail: if API result >125% of fallback, use fallback
4. Unit assumptions: energy <10000 treated as MWh, materials in tonnes

### Fraud Detection
1. Risk score 0-1 based on: missing data, carbon gap, anomalies
2. Hard fraud signals: critical fields missing, extreme ratios
3. Blocks approval if risk ≥0.65

### Financing Workflow
1. Investor pays via Stripe
2. Webhook confirms payment → creates conversation thread
3. Auto-redirects to messages page
4. Thread sorted by investment amount (highest first)

### Green Credit Eligibility
1. Project must be APPROVED
2. avoided_tco2 > 0
3. avoided_tco2 ≥ 0.5 (minimum threshold)
4. fraud_risk < 0.65
5. data_quality_score ≥ 60

## 🎯 Next Steps

1. **Create missing service classes** (ExpertWorkflowService, ProjectStatusService, etc.)
2. **Update database schema** (run SQL scripts for new tables)
3. **Create DAO implementations** for new tables
4. **Update controllers** to use new services
5. **Create UI components** for messages and notifications
6. **Test complete workflows** end-to-end

## 📚 Reference

- Symfony source: `ExpertWorkflowService.php`
- Emission factors: Updated for realistic values
- Business rules: Documented in Symfony controllers
- API integrations: Climatiq, Stripe, Pusher (replace with EventBus for desktop)
