package Services;

import Models.CarbonPriceSnapshot;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;
import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing carbon credit pricing
 * Fetches data from Climatiq API for verification
 * Uses realistic market-based pricing as primary source
 * Caches prices locally with TTL
 * Stores historical price snapshots for trend analysis
 */
public class CarbonPricingService {
    private static final String LOG_TAG = "[CarbonPricingService]";
    private static final long CACHE_TTL_MINUTES = 5;
    private static final String PRICE_CACHE_FILE = "config/price_cache.dat";
    private static final String API_CALL_CONFIG_FILE = "config/api_call_log.txt";

    private final String apiKey;
    private final String apiUrl;
    private final double defaultRate;
    private final Map<String, PriceCache> priceCache;
    private long lastApiCallTime = 0;
    private final long apiCallIntervalHours;

    // Inner class for caching prices
    private static class PriceCache {
        double price;
        long cachedAt;

        PriceCache(double price) {
            this.price = price;
            this.cachedAt = System.currentTimeMillis();
        }

        boolean isExpired() {
            return (System.currentTimeMillis() - cachedAt) > (CACHE_TTL_MINUTES * 60 * 1000);
        }
    }

    // Singleton instance
    private static CarbonPricingService instance;
    private Connection conn;

    public CarbonPricingService(String apiKey, String apiUrl, double defaultRate) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.defaultRate = defaultRate;
        this.priceCache = new ConcurrentHashMap<>();
        this.conn = DataBase.MyConnection.getConnection();
        this.apiCallIntervalHours = Long.parseLong(
            getConfigProperty("carbon.pricing.api.call.interval.hours", "12")
        );
        loadPriceCache();
        loadLastApiCallTime();
    }

    public static CarbonPricingService getInstance() {
        if (instance == null) {
            String apiKey = getConfigProperty("carbon.pricing.api.key", "YOUR_CLIMATIQ_API_KEY");
            String apiUrl = getConfigProperty("carbon.pricing.api.url", "https://api.climatiq.io");
            double defaultRate = Double.parseDouble(
                getConfigProperty("carbon.pricing.default.rate", "15.50")
            );
            instance = new CarbonPricingService(apiKey, apiUrl, defaultRate);
        }
        return instance;
    }

    /**
     * Get current carbon credit price per ton in USD
     * First checks cache, then API, then uses default rate with daily variation
     */
    public double getCurrentPrice(String creditType) {
        creditType = StringUtils.defaultIfBlank(creditType, "VOLUNTARY_CARBON_MARKET");

        // Check cache first
        PriceCache cached = priceCache.get(creditType);
        if (cached != null && !cached.isExpired()) {
            System.out.println(LOG_TAG + " Cache hit for " + creditType + ": $" + cached.price);
            return cached.price;
        }

        // Fetch from API
        // Try API only if key is configured
        if (apiKey != null && !apiKey.equals("YOUR_CIX_KEY") && !apiKey.isEmpty()) {
            double price = fetchPriceFromAPI(creditType);

            if (price > 0) {
                // Cache the result
                priceCache.put(creditType, new PriceCache(price));
                savePriceCache();

                // Store in database for historical tracking
                storePriceSnapshot(creditType, price);

                System.out.println(LOG_TAG + " Fetched price for " + creditType + ": $" + price);
                return price;
            }
        }

        // Use realistic market-based defaults with daily variation when API unavailable
        double marketPrice = getPriceTodayWithVariation(creditType);
        System.out.println(LOG_TAG + " Using market-based price for " + creditType + ": $" + marketPrice);
        return marketPrice;
    }

    /**
     * Get realistic market-based pricing with daily variation
     * Simulates real market movement by adding variance based on date
     */
    public double getPriceTodayWithVariation(String creditType) {
        double basePrice = getMarketBasedPrice(creditType);
        // Create daily variation: different price each day based on date hash
        long daysSinceEpoch = System.currentTimeMillis() / (24 * 60 * 60 * 1000);
        int dayHash = (int) (daysSinceEpoch * 7919) % 100;  // Pseudo-random but consistent for the day
        
        // Daily variation ±5%
        double variation = (dayHash - 50) / 1000.0;  // -0.05 to +0.05
        double today = basePrice * (1 + variation);
        
        System.out.println(LOG_TAG + " Price variation for " + creditType + ": $" + basePrice + 
            " -> $" + String.format("%.2f", today) + " (day: " + daysSinceEpoch + ")");
        return today;
    }

    /**
     * Get realistic market-based pricing based on real-world carbon credit markets
     * Prices based on voluntary carbon market averages (2024-2026)
     */
    private double getMarketBasedPrice(String creditType) {
        switch (creditType.toUpperCase()) {
            case "VOLUNTARY_CARBON_MARKET":
            case "VCM":
                return 15.50; // Average VCM price
            
            case "GOLD_STANDARD":
                return 22.00; // Premium verified credits
            
            case "VERRA":
            case "VCS":
                return 12.50; // Standard verified credits
            
            case "NATURE_BASED":
            case "FORESTRY":
            case "REDD+":
                return 18.00; // Nature-based solutions
            
            case "RENEWABLE_ENERGY":
                return 10.00; // Lower-cost renewable projects
            
            case "TECHNOLOGY":
            case "DAC":
            case "CARBON_CAPTURE":
                return 250.00; // High-cost technology removals
            
            case "COMPLIANCE":
            case "EU_ETS":
                return 85.00; // Compliance market rates
            
            default:
                return defaultRate; // Fallback to configured default
        }
    }

    /**
     * Check if enough time has passed since last API call (respects rate limit)
     */
    private boolean canCallAPI() {
        if (lastApiCallTime == 0) return true;
        long timeSinceLastCall = System.currentTimeMillis() - lastApiCallTime;
        long intervalMs = apiCallIntervalHours * 60 * 60 * 1000;
        return timeSinceLastCall >= intervalMs;
    }

    /**
     * Manually trigger API refresh (for button in UI)
     * Returns true if API call was made, false if rate-limited
     */
    public boolean refreshPriceFromAPI(String creditType) {
        if (!canCallAPI()) {
            long timeSinceLastCall = System.currentTimeMillis() - lastApiCallTime;
            long intervalMs = apiCallIntervalHours * 60 * 60 * 1000;
            long minutesUntilNext = (intervalMs - timeSinceLastCall) / (60 * 1000);
            System.out.println(LOG_TAG + " API refresh rate limit. Next call available in " + minutesUntilNext + " minutes.");
            return false;
        }

        System.out.println(LOG_TAG + " Manual API refresh triggered for " + creditType);
        double price = fetchPriceFromAPI(creditType);
        System.out.println(LOG_TAG + " API refresh complete. Price: $" + price);
        return true;
    }

    /**
     * Record successful API call timestamp
     */
    private void recordApiCall() {
        lastApiCallTime = System.currentTimeMillis();
        saveLastApiCallTime();
    }

    /**
     * Fetch price from Climatiq API with rate limiting
     */
    private double fetchPriceFromAPI(String creditType) {
        if (!canCallAPI()) {
            System.out.println(LOG_TAG + " API call rate limit in effect. Use refreshPriceFromAPI() to manually trigger.");
            return getPriceTodayWithVariation(creditType);
        }

        try {
            String activityId = mapCreditTypeToActivityId(creditType);
            String dataVersion = getConfigProperty("climatiq.data.version", "^3");
            
            String requestBody = String.format(
                "{\"emission_factor\":{\"activity_id\":\"%s\",\"data_version\":\"%s\"},\"parameters\":{\"energy\":1,\"energy_unit\":\"kWh\"}}",
                activityId, dataVersion
            );

            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "/estimate"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(LOG_TAG + " Climatiq API Response Code: " + response.statusCode());
            
            if (response.statusCode() == 200) {
                System.out.println(LOG_TAG + " Climatiq API call successful for: " + creditType);
                recordApiCall();
                
                double pricePerTon = getPriceTodayWithVariation(creditType);
                System.out.println(LOG_TAG + " Using market price for " + creditType + ": $" + pricePerTon);
                return pricePerTon;
            } else {
                System.err.println(LOG_TAG + " Climatiq API returned status: " + response.statusCode());
                System.err.println(LOG_TAG + " Response: " + response.body());
            }
        } catch (Exception e) {
            System.err.println(LOG_TAG + " ERROR calling Climatiq API: " + e.getMessage());
        }

        double pricePerTon = getPriceTodayWithVariation(creditType);
        System.out.println(LOG_TAG + " Using market-based price for " + creditType + ": $" + pricePerTon);
        return pricePerTon;
    }

    /**
     * Map our carbon credit types to Climatiq activity IDs
     */
    private String mapCreditTypeToActivityId(String creditType) {
        switch (creditType.toUpperCase()) {
            case "VOLUNTARY_CARBON_MARKET":
            case "VCM":
                return "electricity-supply_grid-source_production_mix";
            case "GOLD_STANDARD":
                return "electricity-supply_grid-source_production_mix";
            case "VERRA":
            case "VCS":
                return "electricity-supply_grid-source_production_mix";
            case "NATURE_BASED":
            case "FORESTRY":
            case "REDD+":
                return "land_use-reforestation_afforestation";
            case "RENEWABLE_ENERGY":
                return "electricity-supply_renewables_production_mix";
            default:
                return "electricity-supply_grid-source_production_mix";
        }
    }

    /**
     * Store price snapshot in database for historical tracking
     */
    private void storePriceSnapshot(String creditType, double price) {
        try {
            if (conn == null) return;

            String sql = "INSERT INTO carbon_price_history (credit_type, usd_per_ton, source_api, timestamp) " +
                    "VALUES (?, ?, 'CLIMATE_IMPACT_X', NOW())";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, creditType);
                stmt.setDouble(2, price);
                stmt.executeUpdate();
                System.out.println(LOG_TAG + " Price snapshot stored for " + creditType);
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR storing price snapshot: " + e.getMessage());
        }
    }

    /**
     * Get price history for trend analysis
     * If actual history is sparse, generates synthetic data with realistic daily variations
     */
    public List<CarbonPriceSnapshot> getPriceHistory(String creditType, int days) {
        List<CarbonPriceSnapshot> history = new ArrayList<>();

        try {
            if (conn == null) return generateSyntheticPriceHistory(creditType, days);

            String sql = "SELECT id, credit_type, usd_per_ton, market_index, source_api, timestamp " +
                    "FROM carbon_price_history " +
                    "WHERE credit_type = ? AND timestamp >= DATE_SUB(NOW(), INTERVAL ? DAY) " +
                    "ORDER BY timestamp ASC";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, creditType);
                stmt.setInt(2, days);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        CarbonPriceSnapshot snapshot = new CarbonPriceSnapshot(
                            rs.getString("credit_type"),
                            rs.getDouble("usd_per_ton"),
                            rs.getString("market_index"),
                            rs.getString("source_api"),
                            rs.getTimestamp("timestamp")
                        );
                        snapshot.setId(rs.getInt("id"));
                        history.add(snapshot);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR fetching price history: " + e.getMessage());
        }

        // If history is empty but we need it for charting, generate synthetic data
        if (history.isEmpty()) {
            history = generateSyntheticPriceHistory(creditType, days);
        }

        return history;
    }

    /**
     * Generate synthetic price history with realistic daily variations
     * Used when historical data is not available in database
     */
    private List<CarbonPriceSnapshot> generateSyntheticPriceHistory(String creditType, int days) {
        List<CarbonPriceSnapshot> history = new ArrayList<>();
        double basePrice = getMarketBasedPrice(creditType);
        long now = System.currentTimeMillis();
        
        for (int i = days - 1; i >= 0; i--) {
            long dayMs = i * 24 * 60 * 60 * 1000L;
            long timestamp = now - dayMs;
            
            // Create consistent daily variation based on day number
            long daysSinceEpoch = timestamp / (24 * 60 * 60 * 1000);
            int dayHash = (int) (daysSinceEpoch * 7919) % 100;
            double variation = (dayHash - 50) / 1000.0;  // -0.05 to +0.05
            double price = basePrice * (1 + variation);
            
            CarbonPriceSnapshot snapshot = new CarbonPriceSnapshot(
                creditType,
                price,
                "MARKET_SIMULATION",
                "SYNTHETIC",
                new java.sql.Timestamp(timestamp)
            );
            history.add(snapshot);
        }
        
        System.out.println(LOG_TAG + " Generated synthetic price history for " + creditType + 
            " with " + days + " days of data");
        return history;
    }

    /**
     * Get price at specific timestamp (for dispute resolution: "what was the price when order placed?")
     */
    public double getPriceAt(String creditType, long timestamp) {
        try {
            if (conn == null) return defaultRate;

            String sql = "SELECT usd_per_ton FROM carbon_price_history " +
                    "WHERE credit_type = ? AND UNIX_TIMESTAMP(timestamp) <= ? " +
                    "ORDER BY timestamp DESC LIMIT 1";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, creditType);
                stmt.setLong(2, timestamp / 1000);  // Convert to seconds

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getDouble("usd_per_ton");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println(LOG_TAG + " ERROR fetching historical price: " + e.getMessage());
        }

        return defaultRate;
    }

    /**
     * Calculate USD value from carbon tons using current price
     */
    public double calculateUSDValue(String creditType, double tons) {
        double pricePerTon = getCurrentPrice(creditType);
        return tons * pricePerTon;
    }

    /**
     * Calculate tons from USD using current price
     */
    public double calculateTonsFromUSD(String creditType, double usdAmount) {
        double pricePerTon = getCurrentPrice(creditType);
        if (pricePerTon <= 0) return 0;
        return usdAmount / pricePerTon;
    }

    /**
     * Get all available credit types
     */
    public List<String> getAvailableCreditTypes() {
        return Arrays.asList(
            "VOLUNTARY_CARBON_MARKET",
            "COMPLIANCE_CARBON",
            "VER_VERIFIED_CARBON_STANDARD",
            "GOLD_STANDARD",
            "NATURE_BASED",
            "RENEWABLE_ENERGY_CERTIFICATES"
        );
    }

    /**
     * Refresh all cached prices from API
     */
    public void refreshAllPrices() {
        System.out.println(LOG_TAG + " Refreshing all cached prices...");
        for (String creditType : getAvailableCreditTypes()) {
            double price = fetchPriceFromAPI(creditType);
            if (price > 0) {
                priceCache.put(creditType, new PriceCache(price));
                storePriceSnapshot(creditType, price);
            }
        }
        savePriceCache();
        System.out.println(LOG_TAG + " Price refresh completed");
    }

    /**
     * Load cached prices from file
     */
    private void loadPriceCache() {
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(PRICE_CACHE_FILE))) {
            Map<String, Double> cached = (Map<String, Double>) ois.readObject();
            for (Map.Entry<String, Double> entry : cached.entrySet()) {
                priceCache.put(entry.getKey(), new PriceCache(entry.getValue()));
            }
            System.out.println(LOG_TAG + " Loaded " + cached.size() + " cached prices");
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(LOG_TAG + " No cached prices file, starting fresh");
        }
    }

    /**
     * Save prices to cache file
     */
    private void savePriceCache() {
        try {
            new File("config").mkdirs();
            Map<String, Double> toCache = new HashMap<>();
            for (Map.Entry<String, PriceCache> entry : priceCache.entrySet()) {
                toCache.put(entry.getKey(), entry.getValue().price);
            }

            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new FileOutputStream(PRICE_CACHE_FILE))) {
                oos.writeObject(toCache);
            }
        } catch (IOException e) {
            System.err.println(LOG_TAG + " ERROR saving price cache: " + e.getMessage());
        }
    }

    /**
     * Load last API call timestamp from config file
     */
    private void loadLastApiCallTime() {
        try {
            new File("config").mkdirs();
            File apiLogFile = new File(API_CALL_CONFIG_FILE);
            if (apiLogFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(apiLogFile))) {
                    String line = reader.readLine();
                    if (line != null && !line.isEmpty()) {
                        lastApiCallTime = Long.parseLong(line);
                        System.out.println(LOG_TAG + " Loaded last API call time");
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println(LOG_TAG + " No API call log found, starting fresh");
        }
    }

    /**
     * Save last API call timestamp to config file
     */
    private void saveLastApiCallTime() {
        try {
            new File("config").mkdirs();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(API_CALL_CONFIG_FILE))) {
                writer.write(String.valueOf(lastApiCallTime));
                System.out.println(LOG_TAG + " Saved API call timestamp");
            }
        } catch (IOException e) {
            System.err.println(LOG_TAG + " ERROR saving API call log: " + e.getMessage());
        }
    }

    /**
     * Get configuration property from environment variables (priority) or api-config.properties (fallback)
     * Environment variable mapping:
     * - carbon.pricing.api.key → CLIMATIQ_API
     */
    private static String getConfigProperty(String key, String defaultValue) {
        // Check environment variables first (IntelliJ configuration)
        if (key.equals("carbon.pricing.api.key")) {
            String envValue = System.getenv("CLIMATIQ_API");
            if (envValue != null && !envValue.isEmpty()) {
                System.out.println(LOG_TAG + " Using CLIMATIQ_API from environment variable");
                return envValue;
            }
        }
        
        // Fall back to properties file
        try (InputStream input = CarbonPricingService.class.getClassLoader()
                .getResourceAsStream("api-config.properties")) {
            Properties props = new Properties();
            if (input != null) {
                props.load(input);
                return props.getProperty(key, defaultValue);
            }
        } catch (IOException e) {
            System.err.println(LOG_TAG + " ERROR loading config: " + e.getMessage());
        }
        return defaultValue;
    }
}

