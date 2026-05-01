package Models.dto;

/** Output of ML ESG prediction. */
public class MlPredictionResult {

    private Integer predictedEsgScore;    // 0-10
    private Integer credibilityScore;     // 0-100
    private String  carbonRisk;           // LOW, MEDIUM, HIGH
    private String  decision;             // APPROVED, REVISION_REQUIRED, REJECTED
    private String  recommendations;
    private String  modelVersion;

    public Integer getPredictedEsgScore()               { return predictedEsgScore; }
    public void    setPredictedEsgScore(Integer v)      { this.predictedEsgScore = v; }

    public Integer getCredibilityScore()                { return credibilityScore; }
    public void    setCredibilityScore(Integer v)       { this.credibilityScore = v; }

    public String  getCarbonRisk()                      { return carbonRisk; }
    public void    setCarbonRisk(String v)              { this.carbonRisk = v; }

    public String  getDecision()                        { return decision; }
    public void    setDecision(String v)                { this.decision = v; }

    public String  getRecommendations()                 { return recommendations; }
    public void    setRecommendations(String v)         { this.recommendations = v; }

    public String  getModelVersion()                    { return modelVersion; }
    public void    setModelVersion(String v)            { this.modelVersion = v; }
}
