package Services;

import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * ExchangeRateService
 * -------------------
 * Calls the ExchangeRate-API to convert TND amounts to EUR, USD, GBP.
 *
 * API used: https://exchangerate-api.com (free tier, 1500 req/month)
 * Endpoint: GET /v6/{API_KEY}/latest/TND
 *
 * CALLED FROM: FinancementController — triggered on table row selection
 *
 * HOW IT WORKS:
 *   1. Sends one GET request to the API with TND as base currency
 *   2. Parses the JSON response to extract EUR, USD, GBP rates
 *   3. Multiplies the montant by each rate
 *   4. Returns a ConversionResult object with all three values
 */
public class ExchangeRateService {

    // ── Replace with your actual API key from exchangerate-api.com ─
    private static final String API_KEY = "cb7272e95f5bb79441faf4c4";

    private static final String API_URL =
            "https://v6.exchangerate-api.com/v6/" + API_KEY + "/latest/TND";

    /**
     * Converts a TND amount to EUR, USD and GBP.
     *
     * @param montantTND  The amount in TND from financement.montant
     * @return            ConversionResult with all converted values
     * @throws Exception  If API call fails or response is invalid
     */
    public ConversionResult convert(double montantTND) throws Exception {

        // Step 1: Build GET request — no auth header needed, key is in the URL
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .GET()
                .build();

        // Step 2: Send the request
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString()
        );

        // Step 3: Check response status
        if (response.statusCode() != 200) {
            throw new Exception("ExchangeRate API error: HTTP " + response.statusCode());
        }

        // Step 4: Parse the JSON response
        // Response structure:
        // {
        //   "result": "success",
        //   "base_code": "TND",
        //   "conversion_rates": { "EUR": 0.29, "USD": 0.32, "GBP": 0.25, ... }
        // }
        JSONObject json = new JSONObject(response.body());

        String result = json.optString("result", "error");
        if (!result.equals("success")) {
            throw new Exception("ExchangeRate API returned: " + result);
        }

        JSONObject rates = json.getJSONObject("conversion_rates");

        double eurRate = rates.getDouble("EUR");
        double usdRate = rates.getDouble("USD");
        double gbpRate = rates.getDouble("GBP");

        // Step 5: Multiply montant by each rate and return
        return new ConversionResult(
                montantTND,
                montantTND * eurRate,
                montantTND * usdRate,
                montantTND * gbpRate,
                eurRate,
                usdRate,
                gbpRate
        );
    }

    // ─────────────────────────────────────────────────────────────
    // Inner class to hold the conversion results cleanly
    // Accessed in controller as: result.getEur(), result.getUsd() etc.
    // ─────────────────────────────────────────────────────────────
    public static class ConversionResult {

        private final double tnd;
        private final double eur;
        private final double usd;
        private final double gbp;
        private final double eurRate;
        private final double usdRate;
        private final double gbpRate;

        public ConversionResult(double tnd, double eur, double usd, double gbp,
                                double eurRate, double usdRate, double gbpRate) {
            this.tnd = tnd;
            this.eur = eur;
            this.usd = usd;
            this.gbp = gbp;
            this.eurRate = eurRate;
            this.usdRate = usdRate;
            this.gbpRate = gbpRate;
        }

        public double getTnd()     { return tnd; }
        public double getEur()     { return eur; }
        public double getUsd()     { return usd; }
        public double getGbp()     { return gbp; }
        public double getEurRate() { return eurRate; }
        public double getUsdRate() { return usdRate; }
        public double getGbpRate() { return gbpRate; }
    }
}
