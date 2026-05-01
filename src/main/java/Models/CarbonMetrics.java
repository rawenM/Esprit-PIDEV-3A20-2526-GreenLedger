package Models;

import java.time.LocalDateTime;

/**
 * Carbon Metrics Model
 * Stores calculated carbon emissions for a project
 */
public class CarbonMetrics {
    
    private Long id;
    private Integer projectId;
    private Double baselineTco2;
    private Double actualTco2;
    private Double avoidedTco2;
    private Double energyEmissions;
    private Double transportEmissions;
    private Double materialEmissions;
    private Double wasteEmissions;
    private LocalDateTime calculatedAt;
    
    public CarbonMetrics() {
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Integer getProjectId() {
        return projectId;
    }
    
    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }
    
    public Double getBaselineTco2() {
        return baselineTco2;
    }
    
    public void setBaselineTco2(Double baselineTco2) {
        this.baselineTco2 = baselineTco2;
    }
    
    public Double getActualTco2() {
        return actualTco2;
    }
    
    public void setActualTco2(Double actualTco2) {
        this.actualTco2 = actualTco2;
    }
    
    public Double getAvoidedTco2() {
        return avoidedTco2;
    }
    
    public void setAvoidedTco2(Double avoidedTco2) {
        this.avoidedTco2 = avoidedTco2;
    }
    
    public Double getEnergyEmissions() {
        return energyEmissions;
    }
    
    public void setEnergyEmissions(Double energyEmissions) {
        this.energyEmissions = energyEmissions;
    }
    
    public Double getTransportEmissions() {
        return transportEmissions;
    }
    
    public void setTransportEmissions(Double transportEmissions) {
        this.transportEmissions = transportEmissions;
    }
    
    public Double getMaterialEmissions() {
        return materialEmissions;
    }
    
    public void setMaterialEmissions(Double materialEmissions) {
        this.materialEmissions = materialEmissions;
    }
    
    public Double getWasteEmissions() {
        return wasteEmissions;
    }
    
    public void setWasteEmissions(Double wasteEmissions) {
        this.wasteEmissions = wasteEmissions;
    }
    
    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }
    
    public void setCalculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }
    
    @Override
    public String toString() {
        return String.format("CarbonMetrics[projectId=%d, baseline=%.2f, actual=%.2f, avoided=%.2f tCO2e]",
                projectId, baselineTco2, actualTco2, avoidedTco2);
    }
}
