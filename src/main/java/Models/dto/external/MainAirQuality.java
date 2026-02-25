package Models.dto.external;

/**
 * Main air quality index from OpenWeatherMap.
 * AQI scale: 1 = Good, 2 = Fair, 3 = Moderate, 4 = Poor, 5 = Very Poor
 */
public class MainAirQuality {
    
    private Integer aqi;  // Air Quality Index (1-5)
    
    public MainAirQuality() {}
    
    public Integer getAqi() {
        return aqi;
    }
    
    public void setAqi(Integer aqi) {
        this.aqi = aqi;
    }
    
    @Override
    public String toString() {
        return "MainAirQuality{aqi=" + aqi + '}';
    }
}
