package Models.dto;

/** Combined ESG + Fraud result from /predict/both endpoint. */
public class BothPredictionResult {
    private MlPredictionResult   esg;
    private FraudAssessmentResult fraud;

    public MlPredictionResult    getEsg()                       { return esg; }
    public void                  setEsg(MlPredictionResult v)   { this.esg = v; }

    public FraudAssessmentResult getFraud()                     { return fraud; }
    public void                  setFraud(FraudAssessmentResult v) { this.fraud = v; }
}
