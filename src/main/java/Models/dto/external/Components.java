package Models.dto.external;

/**
 * Air pollution components from OpenWeatherMap.
 * All values are in μg/m³ (micrograms per cubic meter).
 */
public class Components {
    
    private Double co;      // Carbon monoxide
    private Double no;      // Nitrogen monoxide
    private Double no2;     // Nitrogen dioxide
    private Double o3;      // Ozone
    private Double so2;     // Sulphur dioxide
    private Double pm2_5;   // Fine particulate matter
    private Double pm10;    // Coarse particulate matter
    private Double nh3;     // Ammonia
    
    public Components() {}
    
    public Double getCo() {
        return co;
    }
    
    public void setCo(Double co) {
        this.co = co;
    }
    
    public Double getNo() {
        return no;
    }
    
    public void setNo(Double no) {
        this.no = no;
    }
    
    public Double getNo2() {
        return no2;
    }
    
    public void setNo2(Double no2) {
        this.no2 = no2;
    }
    
    public Double getO3() {
        return o3;
    }
    
    public void setO3(Double o3) {
        this.o3 = o3;
    }
    
    public Double getSo2() {
        return so2;
    }
    
    public void setSo2(Double so2) {
        this.so2 = so2;
    }
    
    public Double getPm2_5() {
        return pm2_5;
    }
    
    public void setPm2_5(Double pm2_5) {
        this.pm2_5 = pm2_5;
    }
    
    public Double getPm10() {
        return pm10;
    }
    
    public void setPm10(Double pm10) {
        this.pm10 = pm10;
    }
    
    public Double getNh3() {
        return nh3;
    }
    
    public void setNh3(Double nh3) {
        this.nh3 = nh3;
    }
    
    /**
     * Get formatted string of key pollutants
     */
    public String getKeySummary() {
        StringBuilder sb = new StringBuilder();
        if (co != null) sb.append(String.format("CO: %.2f μg/m³, ", co));
        if (no2 != null) sb.append(String.format("NO2: %.2f μg/m³, ", no2));
        if (pm2_5 != null) sb.append(String.format("PM2.5: %.2f μg/m³, ", pm2_5));
        if (pm10 != null) sb.append(String.format("PM10: %.2f μg/m³", pm10));
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return "Components{" +
                "co=" + co +
                ", no2=" + no2 +
                ", pm2_5=" + pm2_5 +
                ", pm10=" + pm10 +
                '}';
    }
}
