package Services;

import Models.dto.external.CarbonAttributes;
import Models.dto.external.CarbonEstimateResponse;
import Utils.ApiConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;

import java.util.concurrent.TimeUnit;

/**
 * Service to interact with Carbon Interface API.
 * Provides methods to estimate carbon emissions for various activities.
 * 
 * API Documentation: https://docs.carboninterface.com
 * 
 * Requires environment variable: CARBON_API_KEY
 */
public class ExternalCarbonApiService {
    
    private final Gson gson;
    private final String apiKey;
    private final String baseUrl;
    private final boolean enabled;
    private String lastError;
    
    public ExternalCarbonApiService() {
        this.gson = new Gson();
        this.apiKey = ApiConfig.getCarbonApiKey();
        this.baseUrl = ApiConfig.getCarbonApiUrl();
        this.enabled = ApiConfig.isCarbonApiEnabled() && !apiKey.isEmpty();
        
        if (!enabled) {
            System.err.println("[CARBON API] Service disabled - API key not configured");
        } else {
            System.out.println("[CARBON API] Service initialized successfully");
        }
    }
    
    /**
     * Estimate carbon emissions for electricity consumption.
     * 
     * @param electricityValue Amount of electricity consumed
     * @param electricityUnit Unit of measurement ("kwh", "mwh")
     * @param country Country code (e.g., "us", "fr", "de")
     * @return CarbonEstimateResponse with emission data, or null if failed
     */
    public CarbonEstimateResponse estimateElectricity(double electricityValue, 
                                                      String electricityUnit, 
                                                      String country) {
        if (!enabled) {
            System.err.println("[CARBON API] Service not available");
            return null;
        }

        lastError = null;
        
        try {
            JsonObject request = new JsonObject();
            request.addProperty("type", "electricity");
            request.addProperty("electricity_unit", electricityUnit);
            request.addProperty("electricity_value", electricityValue);
            request.addProperty("country", country);
            
            return postEstimate(request);
            
        } catch (Exception e) {
            System.err.println("[CARBON API] Error estimating electricity: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Estimate carbon emissions for fuel combustion.
     * 
     * @param fuelSourceType Type of fuel ("coal", "natural_gas", "petroleum", "diesel", etc.)
     * @param fuelSourceValue Amount of fuel
     * @param fuelSourceUnit Unit of measurement ("gallon", "liter", "kg", etc.)
     * @return CarbonEstimateResponse with emission data, or null if failed
     */
    public CarbonEstimateResponse estimateFuel(String fuelSourceType,
                                               double fuelSourceValue,
                                               String fuelSourceUnit) {
        if (!enabled) {
            System.err.println("[CARBON API] Service not available");
            return null;
        }

        lastError = null;
        
        try {
            JsonObject request = new JsonObject();
            request.addProperty("type", "fuel_combustion");
            request.addProperty("fuel_source_type", fuelSourceType);
            request.addProperty("fuel_source_unit", fuelSourceUnit);
            request.addProperty("fuel_source_value", fuelSourceValue);
            
            return postEstimate(request);
            
        } catch (Exception e) {
            System.err.println("[CARBON API] Error estimating fuel: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Estimate carbon emissions for shipping/transport.
     * 
     * @param weightValue Weight of goods in kg
     * @param distanceValue Distance in km
     * @param transportMethod Method of transport ("truck", "ship", "train", "plane")
     * @return CarbonEstimateResponse with emission data, or null if failed
     */
    public CarbonEstimateResponse estimateShipping(double weightValue,
                                                   double distanceValue,
                                                   String transportMethod) {
        if (!enabled) {
            System.err.println("[CARBON API] Service not available");
            return null;
        }

        lastError = null;
        
        try {
            JsonObject request = new JsonObject();
            request.addProperty("type", "shipping");
            request.addProperty("weight_value", weightValue);
            request.addProperty("weight_unit", "kg");
            request.addProperty("distance_value", distanceValue);
            request.addProperty("distance_unit", "km");
            request.addProperty("transport_method", transportMethod);
            
            return postEstimate(request);
            
        } catch (Exception e) {
            System.err.println("[CARBON API] Error estimating shipping: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get a simple mock estimate for testing/demo purposes when API is not available.
     * This is used as fallback when API keys are not configured.
     * 
     * @param activityType Type of activity
     * @param value Numeric value
     * @return Mock CarbonEstimateResponse
     */
    public CarbonEstimateResponse getMockEstimate(String activityType, double value) {
        System.out.println("[CARBON API] Using mock data (API not configured)");
        
        CarbonEstimateResponse response = new CarbonEstimateResponse();
        response.setId("mock-" + System.currentTimeMillis());
        response.setType("estimate");
        
        CarbonAttributes attributes = new CarbonAttributes();
        // Simple mock calculation: assume 0.5 kg CO2 per unit
        double mockCarbon = value * 0.5;
        attributes.setCarbonKg(mockCarbon);
        attributes.setCarbonG(mockCarbon * 1000);
        attributes.setCarbonMt(mockCarbon / 1000);
        attributes.setEstimatedAt(java.time.LocalDateTime.now().toString());
        
        response.setAttributes(attributes);
        
        return response;
    }
    
    /**
     * Internal method to POST to /estimates endpoint
     */
    private CarbonEstimateResponse postEstimate(JsonObject requestData) {
        RequestConfig config = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.of(ApiConfig.getConnectionTimeout(), TimeUnit.MILLISECONDS))
                .setResponseTimeout(Timeout.of(ApiConfig.getReadTimeout(), TimeUnit.MILLISECONDS))
                .build();
        
        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(config)
                .build()) {
            
            String url = baseUrl + ApiConfig.getCarbonEstimatesEndpoint();
            HttpPost httpPost = new HttpPost(url);
            
            // Set headers
            httpPost.setHeader("Authorization", "Bearer " + apiKey);
            httpPost.setHeader("Content-Type", "application/json");
            
            // Set request body
            String jsonBody = gson.toJson(requestData);
            httpPost.setEntity(new StringEntity(jsonBody));
            
            System.out.println("[CARBON API] POST " + url);
            
            ClassicHttpResponse response = httpClient.executeOpen(null, httpPost, null);
            try {
                int statusCode = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                
                if (statusCode == 200 || statusCode == 201) {
                    System.out.println("[CARBON API] Success: " + statusCode);
                    lastError = null;
                    
                    // Parse response
                    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                    JsonObject data = jsonResponse.getAsJsonObject("data");
                    
                    return gson.fromJson(data, CarbonEstimateResponse.class);
                    
                } else {
                    lastError = "HTTP " + statusCode + ": " + responseBody;
                    System.err.println("[CARBON API] Error " + statusCode + ": " + responseBody);
                    return null;
                }
            } finally {
                if (response != null) {
                    response.close();
                }
            }
            
        } catch (Exception e) {
            lastError = e.getMessage();
            System.err.println("[CARBON API] Exception: " + e.getMessage());
            if (ApiConfig.isGracefulDegradationEnabled()) {
                System.out.println("[CARBON API] Graceful degradation enabled - continuing without data");
            }
            return null;
        }
    }
    
    /**
     * Test connection to Carbon API
     * @return true if API is accessible and configured correctly
     */
    public boolean testConnection() {
        if (!enabled) {
            return false;
        }
        
        try {
            // Try a simple electricity estimate as a test
            CarbonEstimateResponse result = estimateElectricity(1.0, "kwh", "us");
            return result != null;
        } catch (Exception e) {
            System.err.println("[CARBON API] Connection test failed: " + e.getMessage());
            return false;
        }
    }
    
    public boolean isEnabled() {
        return enabled;
    }

    public String getLastError() {
        return lastError;
    }
}
