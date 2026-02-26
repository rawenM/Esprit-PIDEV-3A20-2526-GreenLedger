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
 * Fetches real-time pricing from Climate Impact X API
 * Caches prices locally with TTL
 * Stores historical price snapshots for trend analysis
 */
public class CarbonPricingService {
    private static final String LOG_TAG = "[CarbonPricingService]";
    private static final long CACHE_TTL_MINUTES = 5;
    private static final String PRICE_CACHE_FILE = "config/price_cache.dat";

    private final String apiKey;
    private final String apiUrl;
    private final double defaultRate;
    private final Map<String, PriceCache> priceCache;

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
        loadPriceCache();
    }

    public static CarbonPricingService getInstance() {
        if (instance == null) {
            String apiKey = getConfigProperty("carbon.pricing.api.key", "YOUR_CIX_KEY");
            String apiUrl = getConfigProperty("carbon.pricing.api.url", "https://api.climateimpactx.com/v1");
            double defaultRate = Double.parseDouble(
                getConfigProperty("carbon.pricing.default.rate", "15.50")
            );
            instance = new CarbonPricingService(apiKey, apiUrl, defaultRate);
        }
        return instance;
    }

    /**
     * Get current carbon credit price per ton in USD
     * First checks cache, then API, then uses default rate
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

        // Use realistic market-based defaults when API unavailable
        double marketPrice = getMarketBasedPrice(creditType);
        System.out.println(LOG_TAG + " Using market-based price for " + creditType + ": $" + marketPrice);
        return marketPrice;
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
     * Fetch price from Climate Impact X API
     */
    private double fetchPriceFromAPI(String creditType) {
        try {
            String encodedType = URLEncoder.encode(creditType, StandardCharsets.UTF_8);
            String encodedKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
            String url = apiUrl + "/pricing?type=" + encodedType + "&apikey=" + encodedKey;

            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "GreenWallet-Marketplace/1.0")
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonObject jsonObject = JsonParser.parseString(response.body()).getAsJsonObject();

                // Parse response based on API structure
                if (jsonObject.has("price")) {
                    return jsonObject.get("price").getAsDouble();
                } else if (jsonObject.has("data")) {
                    JsonElement dataElement = jsonObject.get("data");
                    if (dataElement.isJsonObject()) {
                        JsonObject dataObj = dataElement.getAsJsonObject();
                        if (dataObj.has("current_price")) {
                            return dataObj.get("current_price").getAsDouble();
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(LOG_TAG + " ERROR fetching price from API: " + e.getMessage());
        }

        return -1;  // Indicates API fetch failed
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
     */
    public List<CarbonPriceSnapshot> getPriceHistory(String creditType, int days) {
        List<CarbonPriceSnapshot> history = new ArrayList<>();

        try {
            if (conn == null) return history;

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
     * Get configuration property from api-config.properties
     */
    private static String getConfigProperty(String key, String defaultValue) {
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

