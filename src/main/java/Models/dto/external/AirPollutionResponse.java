package Models.dto.external;

import java.util.List;

/**
 * Response DTO for OpenWeatherMap Air Pollution API.
 * Contains air quality data for specified coordinates.
 */
public class AirPollutionResponse {
    
    private Coord coord;
    private List<AirQualityData> list;
    
    public AirPollutionResponse() {}
    
    public Coord getCoord() {
        return coord;
    }
    
    public void setCoord(Coord coord) {
        this.coord = coord;
    }
    
    public List<AirQualityData> getList() {
        return list;
    }
    
    public void setList(List<AirQualityData> list) {
        this.list = list;
    }
    
    /**
     * Get the most recent air quality reading
     */
    public AirQualityData getCurrentReading() {
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }
    
    @Override
    public String toString() {
        return "AirPollutionResponse{" +
                "coord=" + coord +
                ", readings=" + (list != null ? list.size() : 0) +
                '}';
    }
    
    /**
     * Nested coordinate class
     */
    public static class Coord {
        private Double lon;
        private Double lat;
        
        public Coord() {}
        
        public Double getLon() {
            return lon;
        }
        
        public void setLon(Double lon) {
            this.lon = lon;
        }
        
        public Double getLat() {
            return lat;
        }
        
        public void setLat(Double lat) {
            this.lat = lat;
        }
        
        @Override
        public String toString() {
            return "Coord{lat=" + lat + ", lon=" + lon + '}';
        }
    }
}
