package Services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * GeoIP Service for User Location Tracking
 * Retrieves geographic information from IP addresses
 * 
 * Uses IP-API.com (free tier: 45 requests/minute)
 * For production, consider MaxMind GeoIP2 or similar paid service
 */
public class GeoIpService {

    private static final String IP_API_URL = "http://ip-api.com/json/";
    private static GeoIpService instance;
    
    // Cache to avoid repeated API calls for same IP
    private Map<String, GeoLocation> cache = new HashMap<>();

    private GeoIpService() {}

    public static synchronized GeoIpService getInstance() {
        if (instance == null) {
            instance = new GeoIpService();
        }
        return instance;
    }

    /**
     * Get geographic location from IP address
     * 
     * @param ipAddress IP address to lookup
     * @return GeoLocation object with country, city, lat, lng
     */
    public GeoLocation getLocation(String ipAddress) {
        // Check cache first
        if (cache.containsKey(ipAddress)) {
            return cache.get(ipAddress);
        }

        // Handle localhost/private IPs
        if (isLocalOrPrivateIp(ipAddress)) {
            GeoLocation local = new GeoLocation();
            local.setCountry("Local");
            local.setCity("Localhost");
            local.setLatitude(0.0);
            local.setLongitude(0.0);
            local.setSuccess(true);
            cache.put(ipAddress, local);
            return local;
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(IP_API_URL + ipAddress);
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                JsonObject json = JsonParser.parseString(jsonResponse).getAsJsonObject();
                
                GeoLocation location = new GeoLocation();
                
                if ("success".equals(json.get("status").getAsString())) {
                    location.setCountry(json.has("country") ? json.get("country").getAsString() : null);
                    location.setCountryCode(json.has("countryCode") ? json.get("countryCode").getAsString() : null);
                    location.setCity(json.has("city") ? json.get("city").getAsString() : null);
                    location.setRegion(json.has("regionName") ? json.get("regionName").getAsString() : null);
                    location.setLatitude(json.has("lat") ? json.get("lat").getAsDouble() : null);
                    location.setLongitude(json.has("lon") ? json.get("lon").getAsDouble() : null);
                    location.setTimezone(json.has("timezone") ? json.get("timezone").getAsString() : null);
                    location.setIsp(json.has("isp") ? json.get("isp").getAsString() : null);
                    location.setSuccess(true);
                    
                    // Cache the result
                    cache.put(ipAddress, location);
                    
                    System.out.println("[GeoIP] Location found for " + ipAddress + ": " + 
                        location.getCity() + ", " + location.getCountry());
                } else {
                    location.setSuccess(false);
                    location.setErrorMessage("IP lookup failed");
                }
                
                return location;
            }
            
        } catch (Exception e) {
            System.err.println("[GeoIP] Error looking up IP " + ipAddress + ": " + e.getMessage());
            GeoLocation error = new GeoLocation();
            error.setSuccess(false);
            error.setErrorMessage(e.getMessage());
            return error;
        }
    }

    /**
     * Get location asynchronously (non-blocking)
     */
    public void getLocationAsync(String ipAddress, LocationCallback callback) {
        new Thread(() -> {
            GeoLocation location = getLocation(ipAddress);
            callback.onLocationReceived(location);
        }).start();
    }

    /**
     * Check if IP is localhost or private
     */
    private boolean isLocalOrPrivateIp(String ip) {
        if (ip == null || ip.isEmpty()) return true;
        
        return ip.equals("127.0.0.1") ||
               ip.equals("localhost") ||
               ip.equals("0:0:0:0:0:0:0:1") ||
               ip.equals("::1") ||
               ip.startsWith("192.168.") ||
               ip.startsWith("10.") ||
               ip.startsWith("172.16.") ||
               ip.startsWith("172.17.") ||
               ip.startsWith("172.18.") ||
               ip.startsWith("172.19.") ||
               ip.startsWith("172.20.") ||
               ip.startsWith("172.21.") ||
               ip.startsWith("172.22.") ||
               ip.startsWith("172.23.") ||
               ip.startsWith("172.24.") ||
               ip.startsWith("172.25.") ||
               ip.startsWith("172.26.") ||
               ip.startsWith("172.27.") ||
               ip.startsWith("172.28.") ||
               ip.startsWith("172.29.") ||
               ip.startsWith("172.30.") ||
               ip.startsWith("172.31.");
    }

    /**
     * Clear cache
     */
    public void clearCache() {
        cache.clear();
    }

    /**
     * Get cache size
     */
    public int getCacheSize() {
        return cache.size();
    }

    /**
     * Callback interface for async location lookup
     */
    public interface LocationCallback {
        void onLocationReceived(GeoLocation location);
    }

    /**
     * GeoLocation data class
     */
    public static class GeoLocation {
        private String country;
        private String countryCode;
        private String city;
        private String region;
        private Double latitude;
        private Double longitude;
        private String timezone;
        private String isp;
        private boolean success;
        private String errorMessage;

        // Getters and Setters
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }

        public String getCountryCode() { return countryCode; }
        public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }

        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }

        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }

        public String getTimezone() { return timezone; }
        public void setTimezone(String timezone) { this.timezone = timezone; }

        public String getIsp() { return isp; }
        public void setIsp(String isp) { this.isp = isp; }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        @Override
        public String toString() {
            if (!success) {
                return "GeoLocation{error='" + errorMessage + "'}";
            }
            return "GeoLocation{" +
                    "country='" + country + '\'' +
                    ", city='" + city + '\'' +
                    ", lat=" + latitude +
                    ", lng=" + longitude +
                    '}';
        }

        /**
         * Get formatted location string
         */
        public String getFormattedLocation() {
            if (!success) return "Unknown";
            StringBuilder sb = new StringBuilder();
            if (city != null) sb.append(city);
            if (region != null) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(region);
            }
            if (country != null) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(country);
            }
            return sb.length() > 0 ? sb.toString() : "Unknown";
        }
    }
}
