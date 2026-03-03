package Api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * REST client for interacting with the Marketplace API
 * Can be used by mobile apps, third-party integrations, or external services
 * 
 * Usage:
 *   MarketplaceAPIClient client = new MarketplaceAPIClient("http://localhost:8080");
 *   List<MarketplaceListing> listings = client.getListings(null, null, null, 100);
 *   int orderId = client.placeOrder(listingId, buyerId, quantity);
 *   double price = client.getCurrentPrice("VOLUNTARY_CARBON_MARKET");
 */
public class MarketplaceAPIClient {
    private static final String LOG_TAG = "[MarketplaceAPIClient]";
    private static final Gson gson = new Gson();
    private static final Type LIST_OF_MAP_TYPE = new TypeToken<List<Map<String, Object>>>() {}.getType();

    private final String baseUrl;
    private String authToken;

    /**
     * Initialize client
     * @param baseUrl Base URL of the API (e.g., "http://localhost:8080")
     */
    public MarketplaceAPIClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Set authentication token for protected endpoints (future enhancement)
     */
    public void setAuthToken(String token) {
        this.authToken = token;
    }

    /**
     * Health check - verify API is reachable
     */
    public boolean healthCheck() {
        try {
            String response = get("/api/health");
            return response != null && response.contains("ok");
        } catch (Exception e) {
            System.err.println(LOG_TAG + " Health check failed: " + e.getMessage());
            return false;
        }
    }

    // ============= LISTINGS ENDPOINTS =============

    /**
     * Get marketplace listings with optional filters
     * @param assetType Filter by asset type (CARBON_CREDITS, WALLET, or null for all)
     * @param minPrice Minimum price filter (or null)
     * @param maxPrice Maximum price filter (or null)
     * @param limit Maximum number of listings to return
     * @return List of MarketplaceListing objects
     */
    public List<Map<String, Object>> getListings(String assetType, Double minPrice, Double maxPrice, int limit) {
        try {
            StringBuilder url = new StringBuilder("/api/marketplace/listings?limit=" + limit);
            if (assetType != null) url.append("&assetType=").append(assetType);
            if (minPrice != null) url.append("&minPrice=").append(minPrice);
            if (maxPrice != null) url.append("&maxPrice=").append(maxPrice);

            String response = get(url.toString());
            return parseList(response);
        } catch (Exception e) {
            System.err.println(LOG_TAG + " Failed to get listings: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Create a new marketplace listing
     * @param sellerId ID of seller
     * @param assetType Type of asset (CARBON_CREDITS or WALLET)
     * @param walletId Wallet ID (required if assetType is WALLET)
     * @param quantity Amount of credits or wallet tokens
     * @param pricePerUnit Price per unit in USD
     * @param description Listing description
     * @return Listing ID if successful, -1 otherwise
     */
    public int createListing(int sellerId, String assetType, Integer walletId, 
                            double quantity, double pricePerUnit, double minPriceUsd,
                            Double autoAcceptPriceUsd, String description) {
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("sellerId", sellerId);
            payload.addProperty("assetType", assetType);
            if (walletId != null) payload.addProperty("walletId", walletId);
            payload.addProperty("quantity", quantity);
            payload.addProperty("pricePerUnit", pricePerUnit);
            payload.addProperty("minPriceUsd", minPriceUsd);
            if (autoAcceptPriceUsd != null) payload.addProperty("autoAcceptPriceUsd", autoAcceptPriceUsd);
            payload.addProperty("description", description);

            String response = post("/api/marketplace/listings", payload.toString());
            JsonObject result = gson.fromJson(response, JsonObject.class);
            return result.get("listingId").getAsInt();
        } catch (Exception e) {
            System.err.println(LOG_TAG + " Failed to create listing: " + e.getMessage());
            return -1;
        }
    }

    // ============= ORDERS ENDPOINTS =============

    /**
     * Place a new order
     * @param listingId ID of the listing to purchase
     * @param buyerId ID of buyer
     * @param quantity Amount to purchase
     * @return Order ID if successful, -1 otherwise
     */
    public int placeOrder(int listingId, int buyerId, double quantity) {
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("listingId", listingId);
            payload.addProperty("buyerId", buyerId);
            payload.addProperty("quantity", quantity);

            String response = post("/api/marketplace/orders", payload.toString());
            JsonObject result = gson.fromJson(response, JsonObject.class);
            return result.get("id").getAsInt();
        } catch (Exception e) {
            System.err.println(LOG_TAG + " Failed to place order: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Get order history for a user
     * @param userId ID of user
     * @return List of orders
     */
    public List<Map<String, Object>> getOrderHistory(int userId) {
        try {
            String response = get("/api/marketplace/orders/" + userId);
            return parseList(response);
        } catch (Exception e) {
            System.err.println(LOG_TAG + " Failed to get order history: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ============= PRICING ENDPOINTS =============

    /**
     * Get current carbon credit price
     * @param creditType Type of carbon credit
     * @return Price in USD per ton
     */
    public double getCurrentPrice(String creditType) {
        try {
            String response = get("/api/marketplace/pricing/current?type=" + creditType);
            JsonObject result = gson.fromJson(response, JsonObject.class);
            return result.get("usdPerTon").getAsDouble();
        } catch (Exception e) {
            System.err.println(LOG_TAG + " Failed to get current price: " + e.getMessage());
            return 0.0;
        }
    }

    /**
     * Get price history for last N days
     * @param days Number of days of history
     * @return List of price snapshots
     */
    public List<Map<String, Object>> getPriceHistory(int days) {
        try {
            String response = get("/api/marketplace/pricing/history/" + days);
            return parseList(response);
        } catch (Exception e) {
            System.err.println(LOG_TAG + " Failed to get price history: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ============= PEER TRADES ENDPOINTS =============

    /**
     * Initiate a peer-to-peer trade
     * @param initiatorId ID of initiating user
     * @param responderId ID of responding user
     * @param assetType Type of asset being traded
     * @param quantity Amount of asset
     * @param proposedPrice Proposed price in USD
     * @return Trade ID if successful, -1 otherwise
     */
    public int initiatePeerTrade(int initiatorId, int responderId, String assetType, 
                                double quantity, double proposedPrice) {
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("initiatorId", initiatorId);
            payload.addProperty("responderId", responderId);
            payload.addProperty("assetType", assetType);
            payload.addProperty("quantity", quantity);
            payload.addProperty("proposedPrice", proposedPrice);

            String response = post("/api/marketplace/trades", payload.toString());
            JsonObject result = gson.fromJson(response, JsonObject.class);
            return result.get("id").getAsInt();
        } catch (Exception e) {
            System.err.println(LOG_TAG + " Failed to initiate trade: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Get peer trades for a user
     * @param userId ID of user
     * @return List of trades
     */
    public List<Map<String, Object>> getUserTrades(int userId) {
        try {
            String response = get("/api/marketplace/trades/" + userId);
            return parseList(response);
        } catch (Exception e) {
            System.err.println(LOG_TAG + " Failed to get user trades: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ============= USER ENDPOINTS =============

    /**
     * Get user's marketplace profile (KYC info, seller stats)
     * @param userId ID of user
     * @return User marketplace profile
     */
    public Map<String, Object> getMarketplaceProfile(int userId) {
        try {
            String response = get("/api/user/" + userId + "/marketplace/profile");
            return gson.fromJson(response, Map.class);
        } catch (Exception e) {
            System.err.println(LOG_TAG + " Failed to get marketplace profile: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Get user's ratings and reviews
     * @param userId ID of user
     * @return List of ratings
     */
    public List<Map<String, Object>> getUserRatings(int userId) {
        try {
            String response = get("/api/user/" + userId + "/marketplace/ratings");
            return parseList(response);
        } catch (Exception e) {
            System.err.println(LOG_TAG + " Failed to get user ratings: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ============= HTTP UTILITIES =============

    /**
     * Perform GET request
     */
    private String get(String endpoint) throws Exception {
        URL url = new URL(baseUrl + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-Type", "application/json");
        if (authToken != null) {
            conn.setRequestProperty("Authorization", "Bearer " + authToken);
        }

        return readResponse(conn);
    }

    /**
     * Perform POST request
     */
    private String post(String endpoint, String payload) throws Exception {
        URL url = new URL(baseUrl + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        if (authToken != null) {
            conn.setRequestProperty("Authorization", "Bearer " + authToken);
        }
        conn.setDoOutput(true);

        byte[] body = payload.getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body);
            os.flush();
        }

        return readResponse(conn);
    }

    /**
     * Read HTTP response
     */
    private String readResponse(HttpURLConnection conn) throws Exception {
        int statusCode = conn.getResponseCode();
        InputStream is = (statusCode >= 400) ? conn.getErrorStream() : conn.getInputStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        if (statusCode >= 400) {
            throw new Exception("HTTP " + statusCode + ": " + response.toString());
        }

        return response.toString();
    }

    private List<Map<String, Object>> parseList(String response) {
        if (response == null || response.isBlank()) {
            return new ArrayList<>();
        }
        return gson.fromJson(response, LIST_OF_MAP_TYPE);
    }

    // ============= EXAMPLE USAGE =============

    /**
     * Example: Using the API client
     */
    public static void main(String[] args) {
        try {
            // Initialize client
            MarketplaceAPIClient client = new MarketplaceAPIClient("http://localhost:8080");

            // Check if API is running
            if (!client.healthCheck()) {
                System.out.println(LOG_TAG + " API is not available");
                return;
            }
            System.out.println(LOG_TAG + " API is healthy");

            // Get current carbon price
            double price = client.getCurrentPrice("VOLUNTARY_CARBON_MARKET");
            System.out.println(LOG_TAG + " Current CO2 price: $" + price + " per ton");

            // Get active listings
            List<Map<String, Object>> listings = client.getListings(null, null, null, 50);
            System.out.println(LOG_TAG + " Found " + listings.size() + " active listings");

            // Get user's marketplace profile
            Map<String, Object> profile = client.getMarketplaceProfile(1);
            System.out.println(LOG_TAG + " User profile: " + profile);

            // Get user's ratings
            List<Map<String, Object>> ratings = client.getUserRatings(1);
            System.out.println(LOG_TAG + " User ratings: " + ratings.size());

            // Get price history (last 7 days)
            List<Map<String, Object>> history = client.getPriceHistory(7);
            System.out.println(LOG_TAG + " Price history: " + history.size() + " snapshots");

        } catch (Exception e) {
            System.err.println(LOG_TAG + " ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
