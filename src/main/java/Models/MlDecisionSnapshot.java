package Models;

import java.sql.Timestamp;

public class MlDecisionSnapshot {

    private Long id;
    private Integer projectId;
    private Integer evaluationId;
    private String projectName;
    private String decision;
    private Double confidence;
    private Double score;
    private Double compliance;
    private Integer minNote;
    private Integer esgScore;
    private String factors;
    private String explanation;
    private String recommendations;
    private Long createdByUserId;
    private Timestamp createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getProjectId() { return projectId; }
    public void setProjectId(Integer projectId) { this.projectId = projectId; }

    public Integer getEvaluationId() { return evaluationId; }
    public void setEvaluationId(Integer evaluationId) { this.evaluationId = evaluationId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    public Double getCompliance() { return compliance; }
    public void setCompliance(Double compliance) { this.compliance = compliance; }

    public Integer getMinNote() { return minNote; }
    public void setMinNote(Integer minNote) { this.minNote = minNote; }

    public Integer getEsgScore() { return esgScore; }
    public void setEsgScore(Integer esgScore) { this.esgScore = esgScore; }

    public String getFactors() { return factors; }
    public void setFactors(String factors) { this.factors = factors; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public String getRecommendations() { return recommendations; }
    public void setRecommendations(String recommendations) { this.recommendations = recommendations; }

    public Long getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(Long createdByUserId) { this.createdByUserId = createdByUserId; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}

