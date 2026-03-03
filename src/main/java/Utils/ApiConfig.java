package Utils;

import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration utility for external API integration.
 * Loads settings from api-config.properties and environment variables.
<<<<<<< HEAD
 * 
 * Environment variables required:
 * - CARBON_API_KEY: API key for Carbon Interface
 * - OPENWEATHERMAP_API_KEY: API key for OpenWeatherMap
 */
public class ApiConfig {
    
    private static final Properties props = new Properties();
    private static boolean initialized = false;
    
    static {
        initialize();
    }
    
    private static void initialize() {
        if (initialized) return;
        
=======
 *
 * Environment variables supported:
 * - CARBON_API_KEY
 * - OPENWEATHERMAP_API_KEY
 * - TWILIO_ACCOUNT_SID
 * - TWILIO_AUTH_TOKEN
 * - TWILIO_FROM_NUMBER
 */
public class ApiConfig {

    private static final Properties props = new Properties();
    private static boolean initialized = false;

    static {
        initialize();
    }

    private static void initialize() {
        if (initialized) return;

>>>>>>> yassine_antar
        try (InputStream in = ApiConfig.class.getResourceAsStream("/api-config.properties")) {
            if (in != null) {
                props.load(in);
                System.out.println("[API CONFIG] Configuration loaded successfully");
            } else {
                System.err.println("[API CONFIG] Warning: api-config.properties not found, using defaults");
            }
        } catch (Exception e) {
            System.err.println("[API CONFIG] Error loading configuration: " + e.getMessage());
        }
<<<<<<< HEAD
        
        initialized = true;
    }
    
    // Carbon Interface API Configuration
    
=======

        initialized = true;
    }

    // -----------------------------
    // Carbon Interface API Config
    // -----------------------------

>>>>>>> yassine_antar
    public static String getCarbonApiKey() {
        String key = System.getenv("CARBON_API_KEY");
        if (key == null || key.trim().isEmpty()) {
            key = props.getProperty("carbon.api.key");
        }
        if (key == null || key.trim().isEmpty()) {
<<<<<<< HEAD
            System.err.println("[API CONFIG] Warning: CARBON_API_KEY environment variable not set");
=======
            System.err.println("[API CONFIG] Warning: CARBON_API_KEY not set");
>>>>>>> yassine_antar
            return "";
        }
        return key.trim();
    }
<<<<<<< HEAD
    
    public static String getCarbonApiUrl() {
        return props.getProperty("carbon.api.url", "https://www.carboninterface.com/api/v1");
    }
    
    public static String getCarbonEstimatesEndpoint() {
        return props.getProperty("carbon.api.estimates.endpoint", "/estimates");
    }
    
    public static String getCarbonEmissionFactorsEndpoint() {
        return props.getProperty("carbon.api.emission_factors.endpoint", "/emission_factors");
    }
    
    public static boolean isCarbonApiEnabled() {
        return Boolean.parseBoolean(props.getProperty("api.carbon.enabled", "true"));
    }
    
    // OpenWeatherMap API Configuration
    
=======

    public static String getCarbonApiUrl() {
        return props.getProperty("carbon.api.url", "https://www.carboninterface.com/api/v1");
    }

    public static String getCarbonEstimatesEndpoint() {
        return props.getProperty("carbon.api.estimates.endpoint", "/estimates");
    }

    public static String getCarbonEmissionFactorsEndpoint() {
        return props.getProperty("carbon.api.emission_factors.endpoint", "/emission_factors");
    }

    public static boolean isCarbonApiEnabled() {
        return Boolean.parseBoolean(props.getProperty("api.carbon.enabled", "true"));
    }

    // -----------------------------
    // OpenWeatherMap API Config
    // -----------------------------

>>>>>>> yassine_antar
    public static String getOpenWeatherMapApiKey() {
        String key = System.getenv("OPENWEATHERMAP_API_KEY");
        if (key == null || key.trim().isEmpty()) {
            key = props.getProperty("openweathermap.api.key");
        }
        if (key == null || key.trim().isEmpty()) {
<<<<<<< HEAD
            System.err.println("[API CONFIG] Warning: OPENWEATHERMAP_API_KEY environment variable not set");
=======
            System.err.println("[API CONFIG] Warning: OPENWEATHERMAP_API_KEY not set");
>>>>>>> yassine_antar
            return "";
        }
        return key.trim();
    }
<<<<<<< HEAD
    
    public static String getOpenWeatherMapApiUrl() {
        return props.getProperty("openweathermap.api.url", 
            "https://api.openweathermap.org/data/2.5/air_pollution");
    }
    
    public static String getOpenWeatherMapHistoryEndpoint() {
        return props.getProperty("openweathermap.api.history.endpoint", "/history");
    }
    
    public static boolean isWeatherApiEnabled() {
        return Boolean.parseBoolean(props.getProperty("api.weather.enabled", "true"));
    }
    
    // General API Configuration
    
    public static int getConnectionTimeout() {
        return Integer.parseInt(props.getProperty("api.connection.timeout", "5000"));
    }
    
    public static int getReadTimeout() {
        return Integer.parseInt(props.getProperty("api.read.timeout", "10000"));
    }
    
    public static int getMaxRetries() {
        return Integer.parseInt(props.getProperty("api.max.retries", "2"));
    }
    
    public static boolean isGracefulDegradationEnabled() {
        return Boolean.parseBoolean(props.getProperty("api.graceful.degradation", "true"));
    }
    
    // Validation
    
    public static boolean areApiKeysConfigured() {
        return !getCarbonApiKey().isEmpty() && !getOpenWeatherMapApiKey().isEmpty();
    }
    
=======

    public static String getOpenWeatherMapApiUrl() {
        return props.getProperty("openweathermap.api.url",
                "https://api.openweathermap.org/data/2.5/air_pollution");
    }

    public static String getOpenWeatherMapHistoryEndpoint() {
        return props.getProperty("openweathermap.api.history.endpoint", "/history");
    }

    public static boolean isWeatherApiEnabled() {
        return Boolean.parseBoolean(props.getProperty("api.weather.enabled", "true"));
    }

    // -----------------------------
    // Twilio SMS Config
    // -----------------------------

    public static boolean isTwilioEnabled() {
        return Boolean.parseBoolean(props.getProperty("api.twilio.enabled", "false"));
    }

    public static String getTwilioAccountSid() {
        String sid = System.getenv("TWILIO_ACCOUNT_SID");
        if (sid == null || sid.trim().isEmpty()) {
            sid = props.getProperty("twilio.account.sid");
        }
        return sid == null ? "" : sid.trim();
    }

    public static String getTwilioAuthToken() {
        String token = System.getenv("TWILIO_AUTH_TOKEN");
        if (token == null || token.trim().isEmpty()) {
            token = props.getProperty("twilio.auth.token");
        }
        return token == null ? "" : token.trim();
    }

    public static String getTwilioFromNumber() {
        String from = System.getenv("TWILIO_FROM_NUMBER");
        if (from == null || from.trim().isEmpty()) {
            from = props.getProperty("twilio.from.number");
        }
        return from == null ? "" : from.trim();
    }

    // -----------------------------
    // General API Configuration
    // -----------------------------

    public static int getConnectionTimeout() {
        return Integer.parseInt(props.getProperty("api.connection.timeout", "5000"));
    }

    public static int getReadTimeout() {
        return Integer.parseInt(props.getProperty("api.read.timeout", "10000"));
    }

    public static int getMaxRetries() {
        return Integer.parseInt(props.getProperty("api.max.retries", "2"));
    }

    public static boolean isGracefulDegradationEnabled() {
        return Boolean.parseBoolean(props.getProperty("api.graceful.degradation", "true"));
    }

    // Validation

    public static boolean areApiKeysConfigured() {
        return !getCarbonApiKey().isEmpty() && !getOpenWeatherMapApiKey().isEmpty();
    }

>>>>>>> yassine_antar
    public static void printConfiguration() {
        System.out.println("[API CONFIG] ========== API Configuration ==========");
        System.out.println("[API CONFIG] Carbon API Enabled: " + isCarbonApiEnabled());
        System.out.println("[API CONFIG] Carbon API Key Present: " + !getCarbonApiKey().isEmpty());
        System.out.println("[API CONFIG] Weather API Enabled: " + isWeatherApiEnabled());
        System.out.println("[API CONFIG] Weather API Key Present: " + !getOpenWeatherMapApiKey().isEmpty());
<<<<<<< HEAD
=======
        System.out.println("[API CONFIG] Twilio Enabled: " + isTwilioEnabled());
        System.out.println("[API CONFIG] Twilio SID Present: " + !getTwilioAccountSid().isEmpty());
        System.out.println("[API CONFIG] Twilio FROM Present: " + !getTwilioFromNumber().isEmpty());
>>>>>>> yassine_antar
        System.out.println("[API CONFIG] Connection Timeout: " + getConnectionTimeout() + "ms");
        System.out.println("[API CONFIG] Graceful Degradation: " + isGracefulDegradationEnabled());
        System.out.println("[API CONFIG] =======================================");
    }
<<<<<<< HEAD
    
    private ApiConfig() {
        // Utility class, no public constructor
    }
}
=======

    private ApiConfig() {
        // Utility class, no public constructor
    }
}
>>>>>>> yassine_antar
