package Models;

import java.sql.Timestamp;

public class MlPrediction {

    private Long id;
    private Integer evaluationId;
    private Integer projectId;
    private Integer predictedEsgScore;
    private Integer credibilityScore;
    private String carbonRisk;
    private String decision;
    private String recommendations;
    private String modelVersion;
    private Long createdByUserId;
    private Timestamp createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getEvaluationId() { return evaluationId; }
    public void setEvaluationId(Integer evaluationId) { this.evaluationId = evaluationId; }

    public Integer getProjectId() { return projectId; }
    public void setProjectId(Integer projectId) { this.projectId = projectId; }

    public Integer getPredictedEsgScore() { return predictedEsgScore; }
    public void setPredictedEsgScore(Integer predictedEsgScore) { this.predictedEsgScore = predictedEsgScore; }

    public Integer getCredibilityScore() { return credibilityScore; }
    public void setCredibilityScore(Integer credibilityScore) { this.credibilityScore = credibilityScore; }

    public String getCarbonRisk() { return carbonRisk; }
    public void setCarbonRisk(String carbonRisk) { this.carbonRisk = carbonRisk; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getRecommendations() { return recommendations; }
    public void setRecommendations(String recommendations) { this.recommendations = recommendations; }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public Long getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(Long createdByUserId) { this.createdByUserId = createdByUserId; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}

