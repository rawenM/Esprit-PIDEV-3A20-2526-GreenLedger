package Services;

import Utils.EnvLoader;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Electricity Maps API client.
 *
 * Fetches real-time carbon intensity (gCO2eq/kWh) for a given zone.
 * Docs: https://static.electricitymaps.com/api/docs/index.html
 *
 * Reads from .env:
 *   ELECTRICITY_MAPS_API_KEY
 *   ELECTRICITY_MAPS_BASE_URL  (default: https://api.electricitymap.org/v3)
 *   ELECTRICITY_MAPS_CACHE_TTL (default: 600 seconds)
 */
public class ElectricityMapsService {

    private static final int TIMEOUT_SECONDS = 10;

    // Zone code mappings for common localisations
    private static final Map<String, String> ZONE_MAP = Map.ofEntries(
        Map.entry("TN",       "TN"),
        Map.entry("TUNISIE",  "TN"),
        Map.entry("TUNISIA",  "TN"),
        Map.entry("MONASTIR", "TN"),
        Map.entry("TUNIS",    "TN"),
        Map.entry("SFAX",     "TN"),
        Map.entry("SOUSSE",   "TN"),
        Map.entry("FR",       "FR"),
        Map.entry("FRANCE",   "FR"),
        Map.entry("DE",       "DE"),
        Map.entry("GERMANY",  "DE"),
        Map.entry("ES",       "ES"),
        Map.entry("SPAIN",    "ES"),
        Map.entry("IT",       "IT"),
        Map.entry("ITALY",    "IT"),
        Map.entry("MA",       "MA"),
        Map.entry("MAROC",    "MA"),
        Map.entry("MOROCCO",  "MA"),
        Map.entry("DZ",       "DZ"),
        Map.entry("ALGERIE",  "DZ"),
        Map.entry("LY",       "LY"),
        Map.entry("LIBYE",    "LY"),
        Map.entry("EG",       "EG"),
        Map.entry("EGYPTE",   "EG"),
        Map.entry("GB",       "GB"),
        Map.entry("UK",       "GB"),
        Map.entry("US",       "US-CAL-CISO"),
        Map.entry("USA",      "US-CAL-CISO")
    );

    // Simple in-memory cache: zone → {timestamp, result}
    private static final Map<String, long[]>   cacheTime   = new ConcurrentHashMap<>();
    private static final Map<String, CarbonIntensityResult> cacheData = new ConcurrentHashMap<>();

    private final String apiKey;
    private final String baseUrl;
    private final long   cacheTtlMs;

    public ElectricityMapsService() {
        EnvLoader.load();
        this.apiKey    = EnvLoader.get("ELECTRICITY_MAPS_API_KEY", "");
        this.baseUrl   = EnvLoader.get("ELECTRICITY_MAPS_BASE_URL", "https://api.electricitymap.org/v3");
        int ttlSec     = parseInt(EnvLoader.get("ELECTRICITY_MAPS_CACHE_TTL", "600"), 600);
        this.cacheTtlMs = ttlSec * 1000L;
    }

    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Fetch carbon intensity for a localisation string (city, country code, zone code, etc.).
     * Returns null if unavailable or API fails.
     */
    public CarbonIntensityResult getCarbonIntensity(String localisation) {
        if (localisation == null || localisation.isBlank()) return null;

        // If it looks like a zone code already (e.g. "US-CAL-CISO", "TN", "FR"), use directly
        // Otherwise resolve from localisation name
        String zone;
        if (localisation.matches("[A-Z]{2}(-[A-Z0-9]+)*")) {
            zone = localisation;
        } else {
            zone = resolveZone(localisation);
            if (zone == null) zone = "TN";
        }

        if (!isAvailable()) return null;

        // Check cache
        long[] ts = cacheTime.get(zone);
        if (ts != null && (System.currentTimeMillis() - ts[0]) < cacheTtlMs) {
            return cacheData.get(zone);
        }

        CarbonIntensityResult result = fetchFromApi(zone);
        if (result != null) {
            cacheTime.put(zone, new long[]{System.currentTimeMillis()});
            cacheData.put(zone, result);
        }
        return result;
    }

    private CarbonIntensityResult fetchFromApi(String zone) {
        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();

            String url = baseUrl + "/carbon-intensity/latest?zone=" + zone;
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("auth-token", apiKey)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .GET()
                .build();

            HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() == 200) {
                return parseResponse(response.body(), zone);
            } else {
                System.err.println("[ElectricityMaps] HTTP " + response.statusCode()
                    + " for zone=" + zone + ": " + response.body());
                return null;
            }
        } catch (Exception e) {
            System.err.println("[ElectricityMaps] Request failed for zone=" + zone + ": " + e.getMessage());
            return null;
        }
    }

    private CarbonIntensityResult parseResponse(String json, String zone) {
        // Parse: {"zone":"TN","carbonIntensity":450,"datetime":"...","updatedAt":"...","emissionFactorType":"lifecycle","isEstimated":true,...}
        try {
            double intensity = extractDouble(json, "carbonIntensity");
            boolean isEstimated = json.contains("\"isEstimated\":true");
            String datetime = extractString(json, "datetime");
            String fossilFuelPct = extractStringOpt(json, "fossilFuelPercentage");

            CarbonIntensityResult r = new CarbonIntensityResult();
            r.zone            = zone;
            r.carbonIntensity = intensity;
            r.isEstimated     = isEstimated;
            r.datetime        = datetime;
            r.fossilFuelPct   = fossilFuelPct;
            return r;
        } catch (Exception e) {
            System.err.println("[ElectricityMaps] Parse failed: " + e.getMessage() + " | json=" + json);
            return null;
        }
    }

    /** Resolve a localisation string to an Electricity Maps zone code. */
    public String resolveZone(String localisation) {
        if (localisation == null) return "TN";
        String upper = localisation.trim().toUpperCase();
        // Direct match
        if (ZONE_MAP.containsKey(upper)) return ZONE_MAP.get(upper);
        // Partial match
        for (Map.Entry<String, String> e : ZONE_MAP.entrySet()) {
            if (upper.contains(e.getKey())) return e.getValue();
        }
        // 2-letter ISO code fallback
        if (upper.length() == 2) return upper;
        return "TN";
    }

    // ── Simple JSON field extractors (no external lib needed) ────────────────

    private double extractDouble(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx < 0) throw new IllegalArgumentException("Key not found: " + key);
        int start = idx + search.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end))
               || json.charAt(end) == '.' || json.charAt(end) == '-')) end++;
        return Double.parseDouble(json.substring(start, end));
    }

    private String extractString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int idx = json.indexOf(search);
        if (idx < 0) return "";
        int start = idx + search.length();
        int end = json.indexOf("\"", start);
        return end > start ? json.substring(start, end) : "";
    }

    private String extractStringOpt(String json, String key) {
        try { return extractString(json, key); } catch (Exception e) { return null; }
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    // ── Result DTO ────────────────────────────────────────────────────────────

    public static class CarbonIntensityResult {
        public String  zone;
        public double  carbonIntensity;   // gCO2eq/kWh
        public boolean isEstimated;
        public String  datetime;
        public String  fossilFuelPct;

        /** Human-readable intensity level. */
        public String getLevel() {
            if (carbonIntensity < 100)  return "Très faible 🟢";
            if (carbonIntensity < 250)  return "Faible 🟡";
            if (carbonIntensity < 450)  return "Moyen 🟠";
            if (carbonIntensity < 650)  return "Élevé 🔴";
            return "Très élevé 🔴";
        }

        /** CSS color for the intensity value. */
        public String getColor() {
            if (carbonIntensity < 100)  return "#059669";
            if (carbonIntensity < 250)  return "#D97706";
            if (carbonIntensity < 450)  return "#EA580C";
            return "#DC2626";
        }
    }
}
