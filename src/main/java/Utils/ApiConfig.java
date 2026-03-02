package Utils;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;
import java.util.Properties;

/**
 * Configuration utility for external API integration.
 * Loads settings from api-config.properties and environment variables.
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

        // Also allow an external api-config.properties in the working directory to override classpath values
        try {
            File external = new File("api-config.properties");
            if (external.exists() && external.isFile()) {
                try (FileInputStream fis = new FileInputStream(external)) {
                    props.load(fis);
                    System.out.println("[API CONFIG] Loaded external api-config.properties from working directory");
                }
            }
        } catch (Exception e) {
            System.err.println("[API CONFIG] Error loading external configuration: " + e.getMessage());
        }

        initialized = true;
    }

    // -----------------------------
    // Carbon Interface API Config
    // -----------------------------

    public static String getCarbonApiKey() {
        String key = System.getenv("CARBON_API_KEY");
        if (key == null || key.trim().isEmpty()) key = System.getProperty("carbon.api.key");
        if (key == null || key.trim().isEmpty()) {
            key = props.getProperty("carbon.api.key");
        }
        if (key == null || key.trim().isEmpty()) {
            System.err.println("[API CONFIG] Warning: CARBON_API_KEY not set");
            return "";
        }
        return key.trim();
    }

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

    public static String getOpenWeatherMapApiKey() {
        String key = System.getenv("OPENWEATHERMAP_API_KEY");
        if (key == null || key.trim().isEmpty()) key = System.getProperty("openweathermap.api.key");
        if (key == null || key.trim().isEmpty()) {
            key = props.getProperty("openweathermap.api.key");
        }
        if (key == null || key.trim().isEmpty()) {
            System.err.println("[API CONFIG] Warning: OPENWEATHERMAP_API_KEY not set");
            return "";
        }
        return key.trim();
    }

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
        if (sid == null || sid.trim().isEmpty()) sid = System.getProperty("twilio.account.sid");
        if (sid == null || sid.trim().isEmpty()) {
            sid = props.getProperty("twilio.account.sid");
        }
        return sid == null ? "" : sid.trim();
    }

    public static String getTwilioAuthToken() {
        String token = System.getenv("TWILIO_AUTH_TOKEN");
        if (token == null || token.trim().isEmpty()) token = System.getProperty("twilio.auth.token");
        if (token == null || token.trim().isEmpty()) {
            token = props.getProperty("twilio.auth.token");
        }
        return token == null ? "" : token.trim();
    }

    public static String getTwilioFromNumber() {
        String from = System.getenv("TWILIO_FROM_NUMBER");
        if (from == null || from.trim().isEmpty()) from = System.getProperty("twilio.from.number");
        if (from == null || from.trim().isEmpty()) {
            from = props.getProperty("twilio.from.number");
        }
        return from == null ? "" : from.trim();
    }

    // -----------------------------
    // OpenAI Config
    // -----------------------------

    public static boolean isOpenAiEnabled() {
        return Boolean.parseBoolean(props.getProperty("api.openai.enabled", "true"));
    }

    public static String getOpenAiApiKey() {
        String key = System.getenv("OPENAI_API_KEY");
        if (key == null || key.trim().isEmpty()) key = System.getProperty("openai.api.key");
        if (key == null || key.trim().isEmpty()) key = props.getProperty("openai.api.key");
        if (key == null || key.trim().isEmpty()) {
            System.err.println("[API CONFIG] Warning: OPENAI_API_KEY not set (environment variable or property)");
            return "";
        }
        return key.trim();
    }

    public static String getOpenAiBaseUrl() {
        String url = System.getenv("OPENAI_API_URL");
        if (url == null || url.trim().isEmpty()) url = System.getProperty("openai.api.url");
        if (url == null || url.trim().isEmpty()) url = props.getProperty("openai.api.url", "https://api.openai.com/v1");
        return url == null ? "https://api.openai.com/v1" : url.trim();
    }

    public static int getOpenAiConnectTimeout() {
        String v = props.getProperty("api.connection.timeout", "5000");
        try { return Integer.parseInt(v.trim()); } catch (Exception e) { return 5000; }
    }

    public static int getOpenAiReadTimeout() {
        String v = props.getProperty("api.read.timeout", "60000");
        try { return Integer.parseInt(v.trim()); } catch (Exception e) { return 60000; }
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

    public static void printConfiguration() {
        System.out.println("[API CONFIG] ========== API Configuration ==========");
        System.out.println("[API CONFIG] Carbon API Enabled: " + isCarbonApiEnabled());
        System.out.println("[API CONFIG] Carbon API Key Present: " + !getCarbonApiKey().isEmpty());
        System.out.println("[API CONFIG] Weather API Enabled: " + isWeatherApiEnabled());
        System.out.println("[API CONFIG] Weather API Key Present: " + !getOpenWeatherMapApiKey().isEmpty());
        System.out.println("[API CONFIG] Twilio Enabled: " + isTwilioEnabled());
        System.out.println("[API CONFIG] Twilio SID Present: " + !getTwilioAccountSid().isEmpty());
        System.out.println("[API CONFIG] Twilio TOKEN Present: " + !getTwilioAuthToken().isEmpty());
        System.out.println("[API CONFIG] Twilio FROM Present: " + !getTwilioFromNumber().isEmpty());
        System.out.println("[API CONFIG] OpenAI Enabled: " + isOpenAiEnabled());
        System.out.println("[API CONFIG] OpenAI API Key Present: " + !getOpenAiApiKey().isEmpty());
        System.out.println("[API CONFIG] OpenAI Base URL: " + getOpenAiBaseUrl());
        System.out.println("[API CONFIG] Connection Timeout: " + getConnectionTimeout() + "ms");
        System.out.println("[API CONFIG] Graceful Degradation: " + isGracefulDegradationEnabled());
        System.out.println("[API CONFIG] =======================================");
    }

    private ApiConfig() {
        // Utility class, no public constructor
    }
}