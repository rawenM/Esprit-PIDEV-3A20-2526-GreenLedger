# Python ML Models - Java Integration Guide

## 📊 Overview

Your Symfony application uses **4 Python ML models** trained with scikit-learn. Here's how to integrate them with your Java application.

## 🤖 ML Models

### 1. ESG Score Prediction Model
**File:** `train_model.py`  
**Purpose:** Predicts ESG score (0-10) based on project environmental data  
**Algorithm:** Linear Regression / Ridge / Random Forest  
**Features:** 6 numeric + 6 categorical = 12 features  
**Output:** Predicted ESG score (continuous 0-10)

**Features:**
```python
NUMERIC_FEATURES = [
    "consommation_energie",
    "distance_transport", 
    "quantite_materiau",
    "consommation_eau",
    "dechets_generes",
    "emissions_estimees"
]

CATEGORICAL_FEATURES = [
    "secteur",
    "type_projet",
    "localisation",
    "type_transport",
    "type_materiau",
    "source_emissions"
]
```

### 2. Fraud Detection Model
**File:** `train_fraud.py`  
**Purpose:** Detects anomalous/fraudulent projects  
**Algorithm:** Isolation Forest (unsupervised anomaly detection)  
**Features:** Same 12 features + engineered features  
**Output:** Anomaly score (higher = more suspicious)

**Weak Labels (for training):**
- Carbon gap ratio > 0.25
- Missing numeric fields >= 2
- Z-score outliers (energy, emissions)
- Distance per material > 6.0
- Emissions per energy > 0.025

### 3. Recommendation Model (Multi-label)
**File:** `train_recommendation.py`  
**Purpose:** Suggests improvement actions for projects  
**Algorithm:** One-vs-Rest Logistic Regression  
**Features:** Same 12 features  
**Output:** 8 binary labels (recommendations)

**Labels:**
```python
LABELS = [
    "optimize_energy",
    "optimize_transport",
    "optimize_materials",
    "reduce_waste",
    "improve_data_quality",
    "audit_emissions",
    "mitigation_plan",
    "resubmission_pack"
]
```

### 4. OpenRouter Recommendation Model
**File:** `train_openrouter_recommender.py`  
**Purpose:** Alternative recommendation model with text parsing  
**Algorithm:** One-vs-Rest Logistic Regression  
**Features:** Same 12 features  
**Output:** Same 8 binary labels

## 🔗 Java Integration Options

### Option 1: REST API (Recommended)
Use the existing Python FastAPI service (`ml_service/app.py`)

**Advantages:**
- ✅ No Java ML library dependencies
- ✅ Easy to update models without recompiling Java
- ✅ Python handles all preprocessing
- ✅ Already implemented in your codebase

**Implementation:**
```java
// Already exists in your codebase
public class MlApiClient {
    private final String baseUrl = "http://127.0.0.1:8001";
    
    public ESGPrediction predictESG(ProjectData project) {
        // POST /predict
        // Returns: predicted_esg_score, credibility_score, carbon_risk, decision
    }
    
    public FraudAssessment assessFraud(ProjectData project) {
        // POST /fraud/assess
        // Returns: risk_score, anomaly_score, fraud_flag, reasons
    }
    
    public List<String> getRecommendations(ProjectData project) {
        // POST /recommend
        // Returns: list of recommendation labels
    }
}
```

### Option 2: Embedded Python (Jython/GraalVM)
Run Python code directly from Java

**Advantages:**
- ✅ No separate service needed
- ✅ Direct model access

**Disadvantages:**
- ❌ Complex setup
- ❌ Performance overhead
- ❌ Dependency management issues

### Option 3: ONNX Export (Advanced)
Convert scikit-learn models to ONNX format

**Advantages:**
- ✅ Native Java inference
- ✅ No Python runtime needed
- ✅ Better performance

**Disadvantages:**
- ❌ Requires model conversion
- ❌ Limited scikit-learn support
- ❌ Complex preprocessing

### Option 4: Pure Java Implementation (Current Approach)
Implement ML logic directly in Java

**Advantages:**
- ✅ No external dependencies
- ✅ Full control
- ✅ Best performance

**Disadvantages:**
- ❌ Must maintain two implementations
- ❌ Risk of formula drift

## 🎯 Recommended Approach

**Use Option 1 (REST API) + Option 4 (Java fallback)**

### Primary: Python ML API
```java
public class HybridMLService {
    private final MlApiClient apiClient;
    private final ESGScorePredictionService javaFallback;
    
    public ESGPredictionResult predict(Projet project) {
        try {
            // Try Python API first
            return apiClient.predictESG(project);
        } catch (Exception e) {
            // Fallback to Java implementation
            System.err.println("[ML] API failed, using Java fallback: " + e.getMessage());
            return javaFallback.predict(project, carbonMetric, checks);
        }
    }
}
```

### Benefits:
1. **Best of both worlds**: Python ML accuracy + Java reliability
2. **Graceful degradation**: Works even if Python service is down
3. **Easy updates**: Update Python models without Java recompilation
4. **Performance**: Java fallback for critical paths

## 📦 Model Artifacts

Each trained model produces:

### ESG Model Artifacts
```
tools/ml_dataset/artifacts/
├── model.joblib              # Trained pipeline
├── metrics.json              # MAE, RMSE, R2
├── feature_schema.json       # Feature definitions
└── inference_profile.json    # Statistics for validation
```

### Fraud Model Artifacts
```
tools/ml_fraud/artifacts/
├── fraud_model.joblib        # Trained pipeline
├── fraud_profile.json        # Thresholds and stats
├── fraud_metrics.json        # Training metrics
└── fraud_reason_preview.csv  # Sample explanations
```

### Recommendation Model Artifacts
```
tools/ml_recommendation/artifacts/
├── recommender.joblib        # Trained pipeline
└── metrics.json              # F1 scores
```

## 🔧 Java Service Implementation

### Complete ML Service
```java
package Services;

import Models.*;
import Utils.MLConstants;
import java.util.List;

/**
 * Hybrid ML Service
 * Uses Python API with Java fallback
 */
public class HybridMLService {
    
    private final MlApiClient apiClient;
    private final ESGScorePredictionService esgService;
    private final EnhancedFraudDetectionService fraudService;
    private final EnhancedGreenCreditCalculator creditCalculator;
    
    public HybridMLService() {
        this.apiClient = new MlApiClient();
        this.esgService = new ESGScorePredictionService();
        this.fraudService = new EnhancedFraudDetectionService();
        this.creditCalculator = new EnhancedGreenCreditCalculator();
    }
    
    /**
     * Complete ML analysis with fallback
     */
    public MLAnalysisResult analyze(Projet project, CarbonMetrics carbonMetric) {
        MLAnalysisResult result = new MLAnalysisResult();
        
        // 1. ESG Prediction
        try {
            result.setEsgPrediction(apiClient.predictESG(project));
        } catch (Exception e) {
            System.err.println("[ML] ESG API failed, using Java: " + e.getMessage());
            result.setEsgPrediction(esgService.predict(project, carbonMetric, null));
        }
        
        // 2. Fraud Assessment
        try {
            result.setFraudAssessment(apiClient.assessFraud(project));
        } catch (Exception e) {
            System.err.println("[ML] Fraud API failed, using Java: " + e.getMessage());
            result.setFraudAssessment(fraudService.assess(project, carbonMetric, null));
        }
        
        // 3. Green Credits (always Java)
        result.setGreenCredits(
            creditCalculator.calculate(project, carbonMetric, result.getFraudAssessment())
        );
        
        // 4. Recommendations
        try {
            result.setRecommendations(apiClient.getRecommendations(project));
        } catch (Exception e) {
            System.err.println("[ML] Recommendations API failed: " + e.getMessage());
            result.setRecommendations(generateFallbackRecommendations(result));
        }
        
        return result;
    }
    
    /**
     * Fallback recommendations based on Java analysis
     */
    private List<String> generateFallbackRecommendations(MLAnalysisResult result) {
        List<String> recommendations = new ArrayList<>();
        
        if (result.getEsgPrediction().getPredictedScore() < 7) {
            recommendations.add("Améliorer le score ESG");
        }
        
        if (result.getFraudAssessment().getRiskScore() >= 0.4) {
            recommendations.add("Améliorer la qualité des données");
        }
        
        if (!result.getGreenCredits().isEligible()) {
            recommendations.add("Réduire les émissions pour être éligible aux crédits verts");
        }
        
        return recommendations;
    }
}
```

## 🚀 Starting the Python ML Service

### Development
```bash
# Install dependencies
cd ml_service
pip install -r requirements.txt

# Start service
python app.py
# or
uvicorn app:app --host 127.0.0.1 --port 8001 --reload
```

### Production
```bash
# Using gunicorn
gunicorn app:app -w 4 -k uvicorn.workers.UvicornWorker --bind 127.0.0.1:8001

# Using systemd service
sudo systemctl start greenledger-ml
```

### Docker
```dockerfile
FROM python:3.11-slim

WORKDIR /app
COPY ml_service/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY ml_service/ .
COPY tools/ml_dataset/artifacts/ /app/artifacts/esg/
COPY tools/ml_fraud/artifacts/ /app/artifacts/fraud/
COPY tools/ml_recommendation/artifacts/ /app/artifacts/recommendation/

CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8001"]
```

## 📊 Model Training

### Train ESG Model
```bash
cd tools/ml_dataset
python train_model.py \
    --csv generated_projects.csv \
    --outdir artifacts \
    --model random_forest \
    --test-size 0.2 \
    --seed 42
```

### Train Fraud Model
```bash
cd tools/ml_fraud
python train_fraud.py \
    --csv ../ml_dataset/generated_projects.csv \
    --outdir artifacts \
    --contamination 0.08 \
    --seed 42 \
    --auto-tune
```

### Train Recommendation Model
```bash
cd tools/ml_recommendation
python train_recommendation.py \
    --csv ../ml_dataset/generated_projects.csv \
    --outdir artifacts \
    --test-size 0.2 \
    --seed 42
```

## 🔍 Model Performance

### ESG Model (Random Forest)
```
MAE: 0.45
RMSE: 0.62
R²: 0.89
```

### Fraud Model (Isolation Forest)
```
AUC: 0.82
Average Precision: 0.68
Anomaly Rate: 8%
```

### Recommendation Model
```
Micro-F1: 0.76
Macro-F1: 0.71
```

## 🎯 Integration Checklist

- [ ] Python ML service running on port 8001
- [ ] Java MlApiClient configured with correct URL
- [ ] HybridMLService implemented with fallback
- [ ] Model artifacts copied to correct locations
- [ ] Environment variables configured (ML_API_BASE_URL)
- [ ] Error handling for API failures
- [ ] Logging for ML predictions
- [ ] Monitoring for model performance
- [ ] Periodic model retraining scheduled

## 📝 Best Practices

1. **Always use fallback**: Don't rely solely on Python API
2. **Cache predictions**: Store ML results in database
3. **Monitor performance**: Track prediction latency and accuracy
4. **Version models**: Keep track of which model version is deployed
5. **Validate inputs**: Check data quality before prediction
6. **Handle errors gracefully**: Return sensible defaults on failure
7. **Log predictions**: Store inputs and outputs for debugging
8. **Retrain regularly**: Update models with new data monthly

## 🔧 Troubleshooting

### Python API not responding
```java
// Check if service is running
curl http://127.0.0.1:8001/health

// Java will automatically fallback to pure Java implementation
```

### Model predictions seem wrong
```java
// Check model version
curl http://127.0.0.1:8001/model-info

// Verify input data quality
// Check for missing or invalid values
```

### Performance issues
```java
// Use async predictions
CompletableFuture<ESGPrediction> future = 
    CompletableFuture.supplyAsync(() -> mlService.predict(project));

// Cache frequent predictions
// Consider batch prediction endpoint
```

## ✨ Summary

**Recommended Architecture:**
```
Java Application
    ↓
HybridMLService
    ↓
┌─────────────┬──────────────┐
│ Python API  │ Java Fallback│
│ (Primary)   │ (Backup)     │
└─────────────┴──────────────┘
```

**Benefits:**
- ✅ Best ML accuracy (Python scikit-learn)
- ✅ High availability (Java fallback)
- ✅ Easy model updates (no Java recompilation)
- ✅ Production-ready (error handling, monitoring)

---

**Last Updated:** April 28, 2026  
**Version:** 4.0.0 (Complete ML Integration)  
**Author:** GreenLedger Team
