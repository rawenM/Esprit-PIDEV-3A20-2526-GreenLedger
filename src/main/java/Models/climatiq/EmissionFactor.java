package Models.climatiq;

import java.util.Objects;

/**
 * Represents an emission factor from Climatiq API.
 * Emission factors define the CO2e impact per unit of activity (kWh, kg, km, USD, etc.)
 * 
 * @see <a href="https://www.climatiq.io/docs">Climatiq API Documentation</a>
 */
public class EmissionFactor {
    
    private String id;                     // Climatiq factor ID (e.g., "electricity-energy_source_grid_mix")
    private String activityId;             // Activity identifier
    private String name;                   // Human-readable name
    private String category;               // Category (electricity, transport, etc.)
    private String region;                 // ISO 3166-1 alpha-2 or custom region code
    private String regionName;             // Human-readable region name
    private Integer year;                  // Data year
    private String source;                 // Data source (IEA, EPA, ecoinvent, etc.)
    private String sourceLink;             // URL to source documentation
    private Double co2eTotal;              // kg CO2e per unit
    private Double co2eFactor;             // CO2 component
    private Double ch4Factor;              // Methane component
    private Double n2oFactor;              // Nitrous oxide component
    private String unit;                   // Unit of measurement (kWh, kg, km, USD, etc.)
    private String unitType;               // Money, Energy, Weight, Distance, etc.
    private String lca;                    // Lifecycle analysis scope (well_to_tank, tank_to_wheel, cradle_to_grave)
    private String dataQualityFlags;       // Quality indicators from source
    private String version;                // Climatiq factor version (e.g., "v2.1.5")
    private String methodology;            // IPCC assessment report (AR4, AR5, AR6)
    
    // Constructors
    public EmissionFactor() {
    }
    
    public EmissionFactor(String activityId, String region, Integer year) {
        this.activityId = activityId;
        this.region = region;
        this.year = year;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getActivityId() {
        return activityId;
    }
    
    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getRegion() {
        return region;
    }
    
    public void setRegion(String region) {
        this.region = region;
    }
    
    public String getRegionName() {
        return regionName;
    }
    
    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }
    
    public Integer getYear() {
        return year;
    }
    
    public void setYear(Integer year) {
        this.year = year;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public String getSourceLink() {
        return sourceLink;
    }
    
    public void setSourceLink(String sourceLink) {
        this.sourceLink = sourceLink;
    }
    
    public Double getCo2eTotal() {
        return co2eTotal;
    }
    
    public void setCo2eTotal(Double co2eTotal) {
        this.co2eTotal = co2eTotal;
    }
    
    public Double getCo2eFactor() {
        return co2eFactor;
    }
    
    public void setCo2eFactor(Double co2eFactor) {
        this.co2eFactor = co2eFactor;
    }
    
    public Double getCh4Factor() {
        return ch4Factor;
    }
    
    public void setCh4Factor(Double ch4Factor) {
        this.ch4Factor = ch4Factor;
    }
    
    public Double getN2oFactor() {
        return n2oFactor;
    }
    
    public void setN2oFactor(Double n2oFactor) {
        this.n2oFactor = n2oFactor;
    }
    
    public String getUnit() {
        return unit;
    }
    
    public void setUnit(String unit) {
        this.unit = unit;
    }
    
    public String getUnitType() {
        return unitType;
    }
    
    public void setUnitType(String unitType) {
        this.unitType = unitType;
    }
    
    public String getLca() {
        return lca;
    }
    
    public void setLca(String lca) {
        this.lca = lca;
    }
    
    public String getDataQualityFlags() {
        return dataQualityFlags;
    }
    
    public void setDataQualityFlags(String dataQualityFlags) {
        this.dataQualityFlags = dataQualityFlags;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public String getMethodology() {
        return methodology;
    }
    
    public void setMethodology(String methodology) {
        this.methodology = methodology;
    }
    
    /**
     * Generate cache key for this emission factor
     */
    public String getCacheKey() {
        return String.format("%s:%s:%d:%s", 
            activityId != null ? activityId : "unknown", 
            region != null ? region : "global", 
            year != null ? year : 0,
            methodology != null ? methodology : "AR6");
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmissionFactor that = (EmissionFactor) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("EmissionFactor{id='%s', name='%s', region='%s', co2e=%.4f %s}", 
            id, name, region, co2eTotal != null ? co2eTotal : 0.0, unit);
    }
}
