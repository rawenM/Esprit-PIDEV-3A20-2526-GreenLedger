package Models.dto;

/** Output of green credit dispatch calculation. */
public class GreenCreditResult {

    private Double  baselineTco2;
    private Double  actualTco2;
    private Double  avoidedTco2;
    private Double  dispatchedCredits;
    private Double  credibilityFactor;
    private Double  esgMultiplier;
    private Boolean eligible;
    private String  statusBadge;
    private String  formula;
    private String  explanation;

    public static GreenCreditResult notEligible(String reason) {
        GreenCreditResult r = new GreenCreditResult();
        r.eligible = false;
        r.dispatchedCredits = 0.0;
        r.statusBadge = "Not Eligible";
        r.explanation = reason;
        return r;
    }

    public Double  getBaselineTco2()                    { return baselineTco2; }
    public void    setBaselineTco2(Double v)            { this.baselineTco2 = v; }

    public Double  getActualTco2()                      { return actualTco2; }
    public void    setActualTco2(Double v)              { this.actualTco2 = v; }

    public Double  getAvoidedTco2()                     { return avoidedTco2; }
    public void    setAvoidedTco2(Double v)             { this.avoidedTco2 = v; }

    public Double  getDispatchedCredits()               { return dispatchedCredits; }
    public void    setDispatchedCredits(Double v)       { this.dispatchedCredits = v; }

    public Double  getCredibilityFactor()               { return credibilityFactor; }
    public void    setCredibilityFactor(Double v)       { this.credibilityFactor = v; }

    public Double  getEsgMultiplier()                   { return esgMultiplier; }
    public void    setEsgMultiplier(Double v)           { this.esgMultiplier = v; }

    public Boolean isEligible()                         { return Boolean.TRUE.equals(eligible); }
    public Boolean getEligible()                        { return eligible; }
    public void    setEligible(Boolean v)               { this.eligible = v; }

    public String  getStatusBadge()                     { return statusBadge; }
    public void    setStatusBadge(String v)             { this.statusBadge = v; }

    public String  getFormula()                         { return formula; }
    public void    setFormula(String v)                 { this.formula = v; }

    public String  getExplanation()                     { return explanation; }
    public void    setExplanation(String v)             { this.explanation = v; }
}
