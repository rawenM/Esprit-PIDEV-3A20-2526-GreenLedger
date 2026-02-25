package Models.dto.external;

/**
 * Air quality data point from OpenWeatherMap.
 * Contains timestamp, AQI, and pollutant measurements.
 */
public class AirQualityData {
    
    private Long dt;  // Unix timestamp
    private MainAirQuality main;
    private Components components;
    
    public AirQualityData() {}
    
    public Long getDt() {
        return dt;
    }
    
    public void setDt(Long dt) {
        this.dt = dt;
    }
    
    public MainAirQuality getMain() {
        return main;
    }
    
    public void setMain(MainAirQuality main) {
        this.main = main;
    }
    
    public Components getComponents() {
        return components;
    }
    
    public void setComponents(Components components) {
        this.components = components;
    }
    
    /**
     * Get air quality description based on AQI
     */
    public String getAirQualityDescription() {
        if (main == null) return "Unknown";
        int aqi = main.getAqi();
        switch (aqi) {
            case 1: return "Good";
            case 2: return "Fair";
            case 3: return "Moderate";
            case 4: return "Poor";
            case 5: return "Very Poor";
            default: return "Unknown";
        }
    }
    
    @Override
    public String toString() {
        return "AirQualityData{" +
                "dt=" + dt +
                ", main=" + main +
                ", components=" + components +
                '}';
    }
}
