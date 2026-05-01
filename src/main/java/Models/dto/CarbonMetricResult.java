package Models.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Output of Climatiq API call (or fallback heuristic).
 * All values in tCO2e.
 */
public class CarbonMetricResult {

    private Double scope1Tco2;
    private Double scope2Tco2;
    private Double scope3Tco2;
    private Double totalTco2;
    private String method;
    private Double dataQualityScore;
    private List<String> providerErrors = new ArrayList<>();

    public static CarbonMetricResult unavailable(String reason) {
        CarbonMetricResult r = new CarbonMetricResult();
        r.totalTco2 = 0.0;
        r.method = "UNAVAILABLE";
        r.providerErrors.add(reason);
        return r;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────

    public Double getScope1Tco2()                       { return scope1Tco2; }
    public void   setScope1Tco2(Double v)               { this.scope1Tco2 = v; }

    public Double getScope2Tco2()                       { return scope2Tco2; }
    public void   setScope2Tco2(Double v)               { this.scope2Tco2 = v; }

    public Double getScope3Tco2()                       { return scope3Tco2; }
    public void   setScope3Tco2(Double v)               { this.scope3Tco2 = v; }

    public Double getTotalTco2()                        { return totalTco2; }
    public void   setTotalTco2(Double v)                { this.totalTco2 = v; }

    public String getMethod()                           { return method; }
    public void   setMethod(String v)                   { this.method = v; }

    public Double getDataQualityScore()                 { return dataQualityScore; }
    public void   setDataQualityScore(Double v)         { this.dataQualityScore = v; }

    public List<String> getProviderErrors()             { return providerErrors; }
    public void         setProviderErrors(List<String> v) { this.providerErrors = v != null ? v : new ArrayList<>(); }
}
