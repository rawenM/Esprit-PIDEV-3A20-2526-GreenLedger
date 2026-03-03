package Models.dto.external;

/**
 * Carbon attributes from Carbon Interface API.
 * Contains carbon emission values in different units.
 */
public class CarbonAttributes {
    
    private Double carbon_g;    // grams
    private Double carbon_lb;   // pounds
    private Double carbon_kg;   // kilograms
    private Double carbon_mt;   // metric tons
    private String estimated_at;
    
    public CarbonAttributes() {}
    
    public Double getCarbonG() {
        return carbon_g;
    }
    
    public void setCarbonG(Double carbon_g) {
        this.carbon_g = carbon_g;
    }
    
    public Double getCarbonLb() {
        return carbon_lb;
    }
    
    public void setCarbonLb(Double carbon_lb) {
        this.carbon_lb = carbon_lb;
    }
    
    public Double getCarbonKg() {
        return carbon_kg;
    }
    
    public void setCarbonKg(Double carbon_kg) {
        this.carbon_kg = carbon_kg;
    }
    
    public Double getCarbonMt() {
        return carbon_mt;
    }
    
    public void setCarbonMt(Double carbon_mt) {
        this.carbon_mt = carbon_mt;
    }
    
    public String getEstimatedAt() {
        return estimated_at;
    }
    
    public void setEstimatedAt(String estimated_at) {
        this.estimated_at = estimated_at;
    }
    
    /**
     * Get carbon value in kilograms (most common unit for reporting)
     */
    public Double getCarbonKgOrDefault() {
        if (carbon_kg != null) return carbon_kg;
        if (carbon_g != null) return carbon_g / 1000.0;
        if (carbon_lb != null) return carbon_lb * 0.453592;
        if (carbon_mt != null) return carbon_mt * 1000.0;
        return 0.0;
    }
    
    @Override
    public String toString() {
        return "CarbonAttributes{" +
                "carbon_kg=" + carbon_kg +
                ", carbon_g=" + carbon_g +
                ", carbon_lb=" + carbon_lb +
                ", carbon_mt=" + carbon_mt +
                ", estimated_at='" + estimated_at + '\'' +
                '}';
    }
}
