package Services;

import Models.dto.external.AirPollutionResponse;
import Models.dto.external.AirQualityData;
import Utils.ApiConfig;
import com.google.gson.Gson;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.Timeout;

import java.util.concurrent.TimeUnit;

/**
 * Service to interact with OpenWeatherMap Air Pollution API.
 * Provides methods to get current and historical air quality data.
 * 
 * API Documentation: https://openweathermap.org/api/air-pollution
 * 
 * Requires environment variable: OPENWEATHERMAP_API_KEY
 */
public class AirQualityService {
    
    private final Gson gson;
    private final String apiKey;
    private final String baseUrl;
    private final boolean enabled;
    
    public AirQualityService() {
        this.gson = new Gson();
        this.apiKey = ApiConfig.getOpenWeatherMapApiKey();
        this.baseUrl = ApiConfig.getOpenWeatherMapApiUrl();
        this.enabled = ApiConfig.isWeatherApiEnabled() && !apiKey.isEmpty();
        
        if (!enabled) {
            System.err.println("[AIR QUALITY API] Service disabled - API key not configured");
        } else {
            System.out.println("[AIR QUALITY API] Service initialized successfully");
        }
    }
    
    /**
     * Get current air quality for specified coordinates.
     * 
     * @param lat Latitude (decimal degrees)
     * @param lon Longitude (decimal degrees)
     * @return AirPollutionResponse with current air quality data, or null if failed
     */
    public AirPollutionResponse getCurrentAirQuality(double lat, double lon) {
        if (!enabled) {
            System.err.println("[AIR QUALITY API] Service not available");
            return null;
        }
        
        if (!isValidCoordinates(lat, lon)) {
            System.err.println("[AIR QUALITY API] Invalid coordinates: lat=" + lat + ", lon=" + lon);
            return null;
        }
        
        try {
            String url = String.format("%s?lat=%.6f&lon=%.6f&appid=%s", 
                    baseUrl, lat, lon, apiKey);
            
            return executeGetRequest(url);
            
        } catch (Exception e) {
            System.err.println("[AIR QUALITY API] Error getting current air quality: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get historical air quality data for specified coordinates and time range.
     * 
     * @param lat Latitude (decimal degrees)
     * @param lon Longitude (decimal degrees)
     * @param start Unix timestamp (seconds) for start time
     * @param end Unix timestamp (seconds) for end time
     * @return AirPollutionResponse with historical data, or null if failed
     */
    public AirPollutionResponse getHistoricalAirQuality(double lat, double lon, 
                                                        long start, long end) {
        if (!enabled) {
            System.err.println("[AIR QUALITY API] Service not available");
            return null;
        }
        
        if (!isValidCoordinates(lat, lon)) {
            System.err.println("[AIR QUALITY API] Invalid coordinates: lat=" + lat + ", lon=" + lon);
            return null;
        }
        
        try {
            String url = String.format("%s%s?lat=%.6f&lon=%.6f&start=%d&end=%d&appid=%s",
                    baseUrl, 
                    ApiConfig.getOpenWeatherMapHistoryEndpoint(),
                    lat, lon, start, end, apiKey);
            
            return executeGetRequest(url);
            
        } catch (Exception e) {
            System.err.println("[AIR QUALITY API] Error getting historical air quality: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get air quality forecast for specified coordinates.
     * Note: This endpoint may not be available in all API plans.
     * 
     * @param lat Latitude (decimal degrees)
     * @param lon Longitude (decimal degrees)
     * @return AirPollutionResponse with forecast data, or null if failed
     */
    public AirPollutionResponse getForecastAirQuality(double lat, double lon) {
        if (!enabled) {
            System.err.println("[AIR QUALITY API] Service not available");
            return null;
        }
        
        if (!isValidCoordinates(lat, lon)) {
            System.err.println("[AIR QUALITY API] Invalid coordinates: lat=" + lat + ", lon=" + lon);
            return null;
        }
        
        try {
            String forecastEndpoint = baseUrl.replace("/air_pollution", "/air_pollution/forecast");
            String url = String.format("%s?lat=%.6f&lon=%.6f&appid=%s", 
                    forecastEndpoint, lat, lon, apiKey);
            
            return executeGetRequest(url);
            
        } catch (Exception e) {
            System.err.println("[AIR QUALITY API] Error getting forecast: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get a simple mock air quality response for testing/demo purposes.
     * Used as fallback when API keys are not configured.
     * 
     * @param lat Latitude
     * @param lon Longitude
     * @return Mock AirPollutionResponse
     */
    public AirPollutionResponse getMockAirQuality(double lat, double lon) {
        System.out.println("[AIR QUALITY API] Using mock data (API not configured)");
        
        // Create mock response with "Good" air quality
        AirPollutionResponse response = new AirPollutionResponse();
        
        AirPollutionResponse.Coord coord = new AirPollutionResponse.Coord();
        coord.setLat(lat);
        coord.setLon(lon);
        response.setCoord(coord);
        
        AirQualityData data = new AirQualityData();
        data.setDt(System.currentTimeMillis() / 1000);
        
        Models.dto.external.MainAirQuality main = new Models.dto.external.MainAirQuality();
        main.setAqi(2); // Fair air quality
        data.setMain(main);
        
        Models.dto.external.Components components = new Models.dto.external.Components();
        components.setCo(200.0);
        components.setNo2(15.0);
        components.setPm2_5(10.0);
        components.setPm10(20.0);
        data.setComponents(components);
        
        response.setList(java.util.Arrays.asList(data));
        
        return response;
    }
    
    /**
     * Internal method to execute GET request
     */
    private AirPollutionResponse executeGetRequest(String url) {
        RequestConfig config = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.of(ApiConfig.getConnectionTimeout(), TimeUnit.MILLISECONDS))
                .setResponseTimeout(Timeout.of(ApiConfig.getReadTimeout(), TimeUnit.MILLISECONDS))
                .build();
        
        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(config)
                .build()) {
            
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Content-Type", "application/json");
            
            System.out.println("[AIR QUALITY API] GET " + url.replaceAll("appid=[^&]+", "appid=***"));
            
            ClassicHttpResponse response = httpClient.executeOpen(null, httpGet, null);
            try {
                int statusCode = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                
                if (statusCode == 200) {
                    System.out.println("[AIR QUALITY API] Success: " + statusCode);
                    return gson.fromJson(responseBody, AirPollutionResponse.class);
                    
                } else {
                    System.err.println("[AIR QUALITY API] Error " + statusCode + ": " + responseBody);
                    return null;
                }
            } finally {
                if (response != null) {
                    response.close();
                }
            }
            
        } catch (Exception e) {
            System.err.println("[AIR QUALITY API] Exception: " + e.getMessage());
            if (ApiConfig.isGracefulDegradationEnabled()) {
                System.out.println("[AIR QUALITY API] Graceful degradation enabled - continuing without data");
            }
            return null;
        }
    }
    
    /**
     * Validate coordinates are within valid ranges
     */
    private boolean isValidCoordinates(double lat, double lon) {
        return lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180;
    }
    
    /**
     * Get air quality description from AQI value
     */
    public static String getAirQualityDescription(int aqi) {
        switch (aqi) {
            case 1: return "Good - Air quality is satisfactory";
            case 2: return "Fair - Air quality is acceptable";
            case 3: return "Moderate - May cause concerns for sensitive groups";
            case 4: return "Poor - Everyone may experience health effects";
            case 5: return "Very Poor - Health warnings of emergency conditions";
            default: return "Unknown";
        }
    }
    
    /**
     * Test connection to OpenWeatherMap API
     * @return true if API is accessible and configured correctly
     */
    public boolean testConnection() {
        if (!enabled) {
            return false;
        }
        
        try {
            // Try getting air quality for a known location (Paris, France)
            AirPollutionResponse result = getCurrentAirQuality(48.8566, 2.3522);
            return result != null && result.getCurrentReading() != null;
        } catch (Exception e) {
            System.err.println("[AIR QUALITY API] Connection test failed: " + e.getMessage());
            return false;
        }
    }
    
    public boolean isEnabled() {
        return enabled;
    }
}
