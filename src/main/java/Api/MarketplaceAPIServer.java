package Api;

import Models.*;
import Services.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * REST API endpoints for marketplace
 * Provides external access to marketplace features (mobile app, third-party integrations)
 * 
 * Endpoints:
 * GET  /api/marketplace/listings - Browse listings with filters
 * POST /api/marketplace/listings - Create new listing
 * GET  /api/marketplace/listings/{id} - Get listing details
 * POST /api/marketplace/orders - Place order
 * GET  /api/marketplace/orders/{userId} - Get order history
 * GET  /api/marketplace/pricing/current - Current carbon price
 * GET  /api/marketplace/pricing/history/{days} - Price history
 * POST /api/marketplace/trades - Initiate P2P trade
 * GET  /api/marketplace/trades/{userId} - Get user trades
 * GET  /api/user/{id}/marketplace/profile - Get user marketplace profile
 * GET  /api/user/{id}/marketplace/ratings - Get user ratings
 */
public class MarketplaceAPIServer {
    private static final String LOG_TAG = "[MarketplaceAPIServer]";
    private static final int PORT = 8080;
    private static final Gson gson = new Gson();

    private final HttpServer server;
    private final MarketplaceListingService listingService;
    private final MarketplaceOrderService orderService;
    private final PeerTradeService tradeService;
    private final CarbonPricingService pricingService;
    private final MarketplaceRatingService ratingService;
    private final UserMarketplaceKYCService kycService;

    public MarketplaceAPIServer() throws Exception {
        this.server = HttpServer.create(new InetSocketAddress(PORT), 0);
        this.listingService = MarketplaceListingService.getInstance();
        this.orderService = MarketplaceOrderService.getInstance();
        this.tradeService = PeerTradeService.getInstance();
        this.pricingService = CarbonPricingService.getInstance();
        this.ratingService = MarketplaceRatingService.getInstance();
        this.kycService = UserMarketplaceKYCService.getInstance();

        setupRoutes();
    }

    /**
     * Setup all API routes
     */
    private void setupRoutes() {
        // Listings endpoints
        server.createContext("/api/marketplace/listings", new ListingsHandler());

        // Orders endpoints
        server.createContext("/api/marketplace/orders", new OrdersHandler());

        // Pricing endpoints
        server.createContext("/api/marketplace/pricing", new PricingHandler());

        // Trades endpoints
        server.createContext("/api/marketplace/trades", new TradesHandler());

        // User profile endpoints
        server.createContext("/api/user", new UserHandler());

        // Health check
        server.createContext("/api/health", exchange -> {
            sendResponse(exchange, 200, "{\"status\": \"ok\"}");
        });

        System.out.println(LOG_TAG + " Routes configured");
    }

    /**
     * Start the API server
     */
    public void start() {
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(10));
        server.start();
        System.out.println(LOG_TAG + " Server started on port " + PORT);
    }

    /**
     * Stop the API server
     */
    public void stop() {
        server.stop(0);
        System.out.println(LOG_TAG + " Server stopped");
    }

    /**
     * Handler for /api/marketplace/listings endpoints
     */
    private class ListingsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();

            try {
                if ("GET".equals(method)) {
                    handleGetListings(exchange);
                } else if ("POST".equals(method)) {
                    handleCreateListing(exchange);
                } else {
                    sendError(exchange, 405, "Method not allowed");
                }
            } catch (Exception e) {
                System.err.println(LOG_TAG + " ERROR in listings handler: " + e.getMessage());
                sendError(exchange, 500, "Internal server error");
            }
        }

        private void handleGetListings(HttpExchange exchange) throws IOException {
            // Parse query parameters
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = parseQueryString(query);

            String assetType = params.getOrDefault("assetType", null);
            Double minPrice = parseDouble(params.get("minPrice"));
            Double maxPrice = parseDouble(params.get("maxPrice"));
            int limit = Integer.parseInt(params.getOrDefault("limit", "100"));

            List<MarketplaceListing> listings = listingService.searchListings(assetType, minPrice, maxPrice, limit);

            sendJsonResponse(exchange, 200, listings);
        }

        private void handleCreateListing(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            JsonObject json = gson.fromJson(body, JsonObject.class);

            int sellerId = json.get("sellerId").getAsInt();
            String assetType = json.get("assetType").getAsString();
            Integer walletId = json.has("walletId") ? json.get("walletId").getAsInt() : null;
            double quantity = json.get("quantity").getAsDouble();
            double pricePerUnit = json.get("pricePerUnit").getAsDouble();
            double minPriceUsd = json.get("minPriceUsd").getAsDouble();
            Double autoAcceptPriceUsd = json.has("autoAcceptPriceUsd") ? 
                json.get("autoAcceptPriceUsd").getAsDouble() : null;
            String description = json.getAsJsonPrimitive("description").getAsString();

            int listingId = listingService.createListing(sellerId, assetType, walletId, 
                                                         quantity, pricePerUnit, minPriceUsd, autoAcceptPriceUsd, description);

            if (listingId > 0) {
                JsonObject response = new JsonObject();
                response.addProperty("listingId", listingId);
                response.addProperty("status", "created");
                sendJsonResponse(exchange, 201, response);
            } else {
                sendError(exchange, 400, "Failed to create listing");
            }
        }
    }

    /**
     * Handler for /api/marketplace/orders endpoints
     */
    private class OrdersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();

            try {
                if ("POST".equals(method)) {
                    handlePlaceOrder(exchange);
                } else if ("GET".equals(method)) {
                    handleGetOrders(exchange);
                } else {
                    sendError(exchange, 405, "Method not allowed");
                }
            } catch (Exception e) {
                System.err.println(LOG_TAG + " ERROR in orders handler: " + e.getMessage());
                sendError(exchange, 500, "Internal server error");
            }
        }

        private void handlePlaceOrder(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            JsonObject json = gson.fromJson(body, JsonObject.class);

            int listingId = json.get("listingId").getAsInt();
            int buyerId = json.get("buyerId").getAsInt();
            double quantity = json.get("quantity").getAsDouble();

            int orderId = orderService.placeOrder(listingId, buyerId, quantity);

            if (orderId > 0) {
                MarketplaceOrder order = orderService.getOrderById(orderId);
                sendJsonResponse(exchange, 201, order);
            } else {
                sendError(exchange, 400, "Failed to place order");
            }
        }

        private void handleGetOrders(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String userId = path.substring(path.lastIndexOf("/") + 1);

            List<MarketplaceOrder> orders = orderService.getOrderHistory(Integer.parseInt(userId));
            sendJsonResponse(exchange, 200, orders);
        }
    }

    /**
     * Handler for /api/marketplace/pricing endpoints
     */
    private class PricingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();

            try {
                if (path.contains("/current")) {
                    handleGetCurrentPrice(exchange);
                } else if (path.contains("/history")) {
                    handleGetPriceHistory(exchange);
                } else {
                    sendError(exchange, 404, "Not found");
                }
            } catch (Exception e) {
                System.err.println(LOG_TAG + " ERROR in pricing handler: " + e.getMessage());
                sendError(exchange, 500, "Internal server error");
            }
        }

        private void handleGetCurrentPrice(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = parseQueryString(query);
            String creditType = params.getOrDefault("type", "VOLUNTARY_CARBON_MARKET");

            double price = pricingService.getCurrentPrice(creditType);

            JsonObject response = new JsonObject();
            response.addProperty("creditType", creditType);
            response.addProperty("usdPerTon", price);
            response.addProperty("timestamp", System.currentTimeMillis());

            sendJsonResponse(exchange, 200, response);
        }

        private void handleGetPriceHistory(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");
            int days = Integer.parseInt(parts[parts.length - 1]);

            String creditType = "VOLUNTARY_CARBON_MARKET";
            List<CarbonPriceSnapshot> history = pricingService.getPriceHistory(creditType, days);

            sendJsonResponse(exchange, 200, history);
        }
    }

    /**
     * Handler for /api/marketplace/trades endpoints
     */
    private class TradesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();

            try {
                if ("POST".equals(method)) {
                    handleInitiateTrade(exchange);
                } else if ("GET".equals(method)) {
                    handleGetTrades(exchange);
                } else {
                    sendError(exchange, 405, "Method not allowed");
                }
            } catch (Exception e) {
                System.err.println(LOG_TAG + " ERROR in trades handler: " + e.getMessage());
                sendError(exchange, 500, "Internal server error");
            }
        }

        private void handleInitiateTrade(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            JsonObject json = gson.fromJson(body, JsonObject.class);

            int initiatorId = json.get("initiatorId").getAsInt();
            int responderId = json.get("responderId").getAsInt();
            String assetType = json.get("assetType").getAsString();
            double quantity = json.get("quantity").getAsDouble();
            double proposedPrice = json.get("proposedPrice").getAsDouble();

            int tradeId = tradeService.initiateTrade(initiatorId, responderId, assetType, 
                                                     quantity, proposedPrice, null, null);

            if (tradeId > 0) {
                PeerTrade trade = tradeService.getTradeById(tradeId);
                sendJsonResponse(exchange, 201, trade);
            } else {
                sendError(exchange, 400, "Failed to initiate trade");
            }
        }

        private void handleGetTrades(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String userId = path.substring(path.lastIndexOf("/") + 1);

            List<PeerTrade> trades = tradeService.getUserTrades(Integer.parseInt(userId));
            sendJsonResponse(exchange, 200, trades);
        }
    }

    /**
     * Handler for /api/user endpoints
     */
    private class UserHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();

            try {
                if (path.contains("/marketplace/profile")) {
                    handleGetMarketplaceProfile(exchange);
                } else if (path.contains("/marketplace/ratings")) {
                    handleGetUserRatings(exchange);
                } else {
                    sendError(exchange, 404, "Not found");
                }
            } catch (Exception e) {
                System.err.println(LOG_TAG + " ERROR in user handler: " + e.getMessage());
                sendError(exchange, 500, "Internal server error");
            }
        }

        private void handleGetMarketplaceProfile(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");
            int userId = Integer.parseInt(parts[2]);

            UserMarketplaceKYC kyc = kycService.getKYCForUser(userId);
            sendJsonResponse(exchange, 200, kyc);
        }

        private void handleGetUserRatings(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");
            int userId = Integer.parseInt(parts[2]);

            List<MarketplaceRating> ratings = ratingService.getUserRatings(userId);
            sendJsonResponse(exchange, 200, ratings);
        }
    }

    /**
     * Utility: Parse query string to map
     */
    private Map<String, String> parseQueryString(String query) {
        Map<String, String> params = new HashMap<>();
        if (query != null && !query.isEmpty()) {
            for (String param : query.split("&")) {
                String[] parts = param.split("=");
                if (parts.length == 2) {
                    params.put(parts[0], parts[1]);
                }
            }
        }
        return params;
    }

    /**
     * Utility: Parse string to double (null-safe)
     */
    private Double parseDouble(String value) {
        try {
            return value != null ? Double.parseDouble(value) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Utility: Read request body
     */
    private String readRequestBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            body.append(line);
        }
        return body.toString();
    }

    /**
     * Utility: Send JSON response
     */
    private void sendJsonResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = gson.toJson(data);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        sendResponse(exchange, statusCode, json);
    }

    /**
     * Utility: Send text response
     */
    private void sendResponse(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    /**
     * Utility: Send error response
     */
    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        JsonObject error = new JsonObject();
        error.addProperty("error", message);
        error.addProperty("statusCode", statusCode);
        sendJsonResponse(exchange, statusCode, error);
    }

    /**
     * Main method to start API server
     */
    public static void main(String[] args) throws Exception {
        MarketplaceAPIServer server = new MarketplaceAPIServer();
        server.start();

        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        System.out.println(LOG_TAG + " API Server ready. Press Ctrl+C to stop.");
    }
}
