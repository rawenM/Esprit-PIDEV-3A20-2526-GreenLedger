# Complete Entity Mapping - Symfony to Java

## ✅ Already Implemented in Java

### Core Models
- ✅ User (with fraud detection fields)
- ✅ Projet (basic fields)
- ✅ Evaluation
- ✅ Financement
- ✅ Budget
- ✅ MlPrediction
- ✅ FraudDetectionResult
- ✅ CarbonMetrics (newly created)
- ✅ Notification (newly created)

## ❌ Missing Java Entities (Need to Create)

### Critical Missing Entities
1. **EvaluationResultat** - Individual criteria scores
2. **CritereReference** - Evaluation criteria definitions
3. **ConversationThread** - Message threads (partially created)
4. **ThreadMessage** - Individual messages (partially created)
5. **ProjectDocument** - Project documentation
6. **ProjectAttachment** - File attachments
7. **ProjectSnapshot** - Project state snapshots
8. **EvaluationNotification** - Re-evaluation notifications

### ML & Analytics Entities
9. **CarbonMetric** - Enhanced version with scope1/2/3
10. **MlDecisionSnapshot** - Enhanced version
11. **MlFraudAssessment** - Separate fraud assessment table

### Marketplace Entities (Optional)
12. **Wallet** - Carbon credit wallets
13. **GreenWallet** - User green credit balances
14. **CarbonCreditBatch** - Carbon credit batches
15. **MarketplaceListing** - Already exists
16. **MarketplaceOrder** - Already exists

### Support Tables
17. **ModelVersion** - ML model versioning
18. **CarbonPriceHistory** - Historical carbon prices

## 🔧 Fields Missing in Existing Java Models

### User Model - Missing Fields
```java
// Fraud detection (already added)
- fraud_score ✅
- fraud_checked ✅

// Location tracking (MISSING)
- last_login_country
- last_login_city
- last_login_lat
- last_login_lng

// Email verification (MISSING)
- email_verifie
- token_verification
- token_expiry
- token_hash
```

### Projet Model - Missing Fields
```java
// Environmental data (MISSING)
- consommation_energie
- unite_energie
- distance_transport
- type_transport
- type_materiau
- quantite_materiau
- consommation_eau
- dechets_generes
- emissions_estimees
- source_emissions

// Location (MISSING)
- secteur
- type_projet
- localisation
- latitude
- longitude
- geocoded_at
- air_quality_index
- completeness_score

// Fraud detection (MISSING)
- fraud_risk_score
- fraud_anomaly_score
- fraud_flag
- fraud_reasons
- fraud_model_version
- fraud_scored_at

// Carbon metrics (MISSING)
- baseline_tco2
- actual_tco2
- avoided_tco2

// Green credits (MISSING)
- dispatched_green_credits
- green_credit_credibility_factor
- green_credit_esg_multiplier
- green_credit_dispatch_status
- green_credit_status_badge
- green_credit_formula
- green_credit_explanation_json
- green_credit_last_computed_at

// Financing (MISSING)
- montant_demande
- statut_financement
- funded_at
- description_projet
```

## 📊 Priority Implementation Order

### Phase 1: Critical Business Logic (Immediate)
1. ✅ Update Projet model with all missing fields
2. ✅ Create EvaluationResultat entity
3. ✅ Create CritereReference entity
4. ✅ Update CarbonMetric with scope1/2/3
5. ✅ Create MlFraudAssessment entity

### Phase 2: Messaging System (High Priority)
6. ✅ Complete ConversationThread entity
7. ✅ Complete ThreadMessage entity
8. ✅ Create ConversationService
9. ✅ Create DAO implementations

### Phase 3: Document Management (Medium Priority)
10. ✅ Create ProjectDocument entity
11. ✅ Create ProjectAttachment entity
12. ✅ Create DocumentService

### Phase 4: Advanced Features (Low Priority)
13. ✅ Create ProjectSnapshot entity
14. ✅ Create EvaluationNotification entity
15. ✅ Create ModelVersion entity
16. ✅ Create CarbonPriceHistory entity

## 🎯 Implementation Status

### Completed ✅
- ExpertWorkflowService
- CarbonCreditDispatchService
- ProjectStatusService
- NotificationService
- CarbonMetrics model
- CarbonCreditResult model
- Notification model
- Database schema SQL

### In Progress 🔄
- Projet model enhancement
- ConversationThread/ThreadMessage completion
- DAO implementations

### Not Started ❌
- EvaluationResultat entity
- CritereReference entity
- ProjectDocument entity
- ProjectAttachment entity
- ProjectSnapshot entity
- EvaluationNotification entity
- MlFraudAssessment entity
- ModelVersion entity
- CarbonPriceHistory entity
