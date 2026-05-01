package Models.dto;

import java.util.ArrayList;
import java.util.List;

/** Output of ML fraud detection. */
public class FraudAssessmentResult {

    private Double       fraudRiskScore;    // 0.0-1.0
    private Double       fraudAnomalyScore;
    private Boolean      fraudFlag;
    private List<String> fraudReasons = new ArrayList<>();
    private String       modelVersion;

    public Double       getFraudRiskScore()                 { return fraudRiskScore; }
    public void         setFraudRiskScore(Double v)         { this.fraudRiskScore = v; }

    public Double       getFraudAnomalyScore()              { return fraudAnomalyScore; }
    public void         setFraudAnomalyScore(Double v)      { this.fraudAnomalyScore = v; }

    public Boolean      isFraudFlag()                       { return Boolean.TRUE.equals(fraudFlag); }
    public Boolean      getFraudFlag()                      { return fraudFlag; }
    public void         setFraudFlag(Boolean v)             { this.fraudFlag = v; }

    public List<String> getFraudReasons()                   { return fraudReasons; }
    public void         setFraudReasons(List<String> v)     { this.fraudReasons = v != null ? v : new ArrayList<>(); }

    public String       getModelVersion()                   { return modelVersion; }
    public void         setModelVersion(String v)           { this.modelVersion = v; }
}
