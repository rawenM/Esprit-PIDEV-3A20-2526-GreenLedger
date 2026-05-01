package Services;

import Models.dto.CarbonMetricResult;
import Models.dto.ProjectData;
import Utils.EnvLoader;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Climatiq API Client
 *
 * POST https://api.climatiq.io/data/v1/estimate
 * Returns scope1, scope2, scope3 in kgCO2e → converted to tCO2e
 *
 * Fallback emission factors (when API unavailable):
 *   Transport: 0.0001 tCO2e/km
 *   Waste:     0.5    tCO2e/tonne
 *   Energy:    0.0005 tCO2e/kWh
 *   Material:  2.0    tCO2e/tonne
 */
public class ClimatiqApiClient {

    private static final String BASE_URL     = "https://api.climatiq.io";
    private static final String DATA_VERSION = "^32";  // wildcard — matches latest 32.x
    private static final int    TIMEOUT_SEC  = 15;

    // Fallback factors (match the original PHP logic and units)
    // transport: tCO2e/km, waste: tCO2e/kg, energy: tCO2e/kWh, material: tCO2e/kg
    private static final double FACTOR_TRANSPORT = 0.00008;
    private static final double FACTOR_WASTE     = 0.00070;
    private static final double FACTOR_ENERGY    = 0.00045;
    private static final double FACTOR_MATERIAL  = 0.00120;

    private final String apiKey;
    private final Gson   gson = new Gson();

    public ClimatiqApiClient() {
        this.apiKey = EnvLoader.get("CLIMATIQ_API_KEY", "");
        if (apiKey.isBlank()) {
            System.err.println("[Climatiq] CLIMATIQ_API_KEY not set — will use fallback heuristics");
        } else {
            System.out.println("[Climatiq] API client initialized");
        }
    }

    // ── Main entry point ──────────────────────────────────────────────────

    public CarbonMetricResult estimateProjectEmissions(ProjectData project) {
        List<String> errors = new ArrayList<>();

        if (apiKey.isBlank()) {
            return fallback(project, errors);
        }

        // Normalize once, then reuse the same units everywhere.
        double energyKwh   = toEnergyKwh(pos(project.getConsommationEnergie()), project.getUniteEnergie());
        double distanceKm   = pos(project.getDistanceTransport());
        double materialTon  = toMaterialTonnes(project.getQuantiteMateriau());

        System.out.printf("[Climatiq] Normalized inputs: energy=%.3f kWh, distance=%.3f km, material=%.3f t%n",
            energyKwh, distanceKm, materialTon);

        Double scope1 = null, scope2 = null, scope3 = null;

        // Scope 1: Transport
        if (distanceKm > 0 && materialTon > 0) {
            scope1 = estimateWithCandidates(
                resolveTransportIds(project.getTypeTransport()),
                Map.of("weight", materialTon, "weight_unit", "t",
                       "distance", distanceKm, "distance_unit", "km"),
                errors);
        }

        // Scope 2: Energy
        if (energyKwh > 0) {
            scope2 = estimateWithCandidates(
                List.of(
                    "electricity-supply_grid-source_residual_mix",
                    "electricity-supply_grid-source_supplier_mix",
                    "electricity-supply_grid-source_geothermal_energy"
                ),
                Map.of("energy", energyKwh, "energy_unit", "kWh"),
                errors);
        }

        // Scope 3: Materials
        if (materialTon > 0) {
            scope3 = estimateWithCandidates(
                resolveMaterialIds(project.getTypeMateriau()),
                Map.of("weight", materialTon, "weight_unit", "t"),
                errors);
        }

        // If all scopes failed, use fallback
        if (scope1 == null && scope2 == null && scope3 == null) {
            System.err.println("[Climatiq] All API calls failed — using fallback");
            return fallback(project, errors);
        }

        double s1 = scope1 != null ? scope1 : fallbackScope1(project);
        double s2 = scope2 != null ? scope2 : fallbackScope2(project);
        double s3 = scope3 != null ? scope3 : fallbackScope3(project);
        double total = r3(s1 + s2 + s3);

        // Guardrail: only revert to fallback if Climatiq result is > 10× fallback
        // (catches clearly wrong API responses, not just higher-precision results)
        double fb = r3(fallbackScope1(project) + fallbackScope2(project) + fallbackScope3(project));
        if (fb > 0.1 && total > fb * 10.0) {
            System.err.printf("[Climatiq] Guardrail: API=%.3f > 10× fallback=%.3f — reverting%n", total, fb);
            return fallback(project, errors);
        }

        System.out.printf("[Climatiq] ✓ API result: scope1=%.3f scope2=%.3f scope3=%.3f total=%.3f tCO2e%n",
            s1, s2, s3, total);

        CarbonMetricResult result = new CarbonMetricResult();
        result.setScope1Tco2(r3(s1));
        result.setScope2Tco2(r3(s2));
        result.setScope3Tco2(r3(s3));
        result.setTotalTco2(total);
        result.setMethod("CLIMATIQ_API_V1_ESTIMATE");
        result.setProviderErrors(errors);
        return result;
    }

    // ── Single API call ───────────────────────────────────────────────────

    private Double estimateSingle(String activityId, Map<String, Object> params, List<String> errors) {
        String url = BASE_URL + "/data/v1/estimate";

        JsonObject emissionFactor = new JsonObject();
        emissionFactor.addProperty("activity_id", activityId);
        emissionFactor.addProperty("data_version", DATA_VERSION);

        JsonObject body = new JsonObject();
        body.add("emission_factor", emissionFactor);
        body.add("parameters", gson.toJsonTree(params));

        RequestConfig config = RequestConfig.custom()
            .setConnectionRequestTimeout(Timeout.of(TIMEOUT_SEC, TimeUnit.SECONDS))
            .setResponseTimeout(Timeout.of(TIMEOUT_SEC, TimeUnit.SECONDS))
            .build();

        try (CloseableHttpClient client = HttpClients.custom()
                .setDefaultRequestConfig(config).build()) {

            HttpPost request = new HttpPost(url);
            request.setHeader("Authorization", "Bearer " + apiKey);
            request.setHeader("Content-Type", "application/json");
            request.setHeader("Accept", "application/json");
            request.setEntity(new StringEntity(gson.toJson(body)));

            return client.execute(request, response -> {
                String responseBody = EntityUtils.toString(response.getEntity());
                if (response.getCode() >= 400) {
                    errors.add(String.format("Climatiq %s → HTTP %d", activityId, response.getCode()));
                    return null;
                }
                JsonObject data = gson.fromJson(responseBody, JsonObject.class);
                if (data.has("co2e") && !data.get("co2e").isJsonNull()) {
                    // API returns kgCO2e → convert to tCO2e
                    return data.get("co2e").getAsDouble() / 1000.0;
                }
                errors.add("Climatiq " + activityId + " → no co2e in response");
                return null;
            });

        } catch (Exception e) {
            errors.add("Climatiq " + activityId + " failed: " + e.getMessage());
            return null;
        }
    }

    private Double estimateWithCandidates(List<String> ids, Map<String, Object> params, List<String> errors) {
        for (String id : ids) {
            Double result = estimateSingle(id, params, errors);
            if (result != null) return result;
        }
        return null;
    }

    // ── Activity ID resolvers ─────────────────────────────────────────────

    private List<String> resolveTransportIds(String type) {
        if (type == null) type = "";
        return switch (type.toLowerCase().trim()) {
            case "train"                      -> List.of("freight_train-route_type_na-fuel_source_na");
            case "bateau", "ship", "maritime" -> List.of("sea_freight-route_type_na");
            case "avion", "air"               -> List.of("air_freight-route_type_domestic");
            default -> List.of(
                "freight_vehicle-vehicle_type_commercial_truck-fuel_source_na-vehicle_weight_na-percentage_load_na",
                "freight_vehicle-vehicle_type_straight_truck-fuel_source_na-vehicle_weight_na-percentage_load_na-load_type_na-distance_uplift_na"
            );
        };
    }

    private List<String> resolveMaterialIds(String type) {
        if (type == null) type = "";
        String m = type.toLowerCase();
        if (m.contains("acier") || m.contains("steel"))       return List.of("metals-type_steel_engineering_steel");
        if (m.contains("aluminium") || m.contains("aluminum")) return List.of("metals-type_aluminium_primary_production");
        if (m.contains("bois") || m.contains("wood"))          return List.of("wood_products-type_sawn_timber");
        if (m.contains("plastique") || m.contains("plastic"))  return List.of("plastics-type_average_plastics");
        return List.of("construction_materials-type_cement");
    }

    // ── Fallback heuristics ───────────────────────────────────────────────

    private CarbonMetricResult fallback(ProjectData project, List<String> errors) {
        double s1 = fallbackScope1(project);
        double s2 = fallbackScope2(project);
        double s3 = fallbackScope3(project);

        CarbonMetricResult result = new CarbonMetricResult();
        result.setScope1Tco2(r3(s1));
        result.setScope2Tco2(r3(s2));
        result.setScope3Tco2(r3(s3));
        result.setTotalTco2(r3(s1 + s2 + s3));
        result.setMethod("GL_HEURISTIC_CLIMATIQ_COMPAT_V1");
        result.setProviderErrors(errors);
        return result;
    }

    private double fallbackScope1(ProjectData p) {
        double transport = pos(p.getDistanceTransport());
        double waste     = pos(p.getDechetsGeneres());
        return (transport * FACTOR_TRANSPORT) + (waste * FACTOR_WASTE);
    }

    private double fallbackScope2(ProjectData p) {
        double energyKwh = toEnergyKwh(pos(p.getConsommationEnergie()), p.getUniteEnergie());
        return energyKwh * FACTOR_ENERGY;
    }

    private double fallbackScope3(ProjectData p) {
        return toMaterialTonnes(p.getQuantiteMateriau()) * 1000.0 * FACTOR_MATERIAL;
    }

    // ── Utilities ─────────────────────────────────────────────────────────

    private double toEnergyKwh(double value, String unit) {
        if (value <= 0) return 0.0;
        String normalized = unit != null ? unit.toLowerCase().trim() : "kwh";
        return switch (normalized) {
            case "mwh" -> value * 1_000.0;
            case "gwh" -> value * 1_000_000.0;
            case "wh"  -> value / 1_000.0;
            default    -> value;
        };
    }

    /**
     * The project quantity is stored in kilograms in the current database flow.
     * Climatiq material factors expect tonnes, so convert once here.
     */
    private double toMaterialTonnes(Double value) {
        double kg = pos(value);
        return kg / 1_000.0;
    }

    private double pos(Double v) { return (v != null && v > 0) ? v : 0.0; }
    private double r3(double v)  { return Math.round(v * 1000.0) / 1000.0; }
}
