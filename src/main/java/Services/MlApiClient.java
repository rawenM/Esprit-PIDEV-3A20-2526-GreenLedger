package Services;

import Models.dto.*;
import Utils.EnvLoader;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * ML API Client — calls the Python FastAPI microservice.
 *
 * Endpoints:
 *   POST /predict/esg   → ESG score prediction
 *   POST /predict/fraud → Fraud detection
 *   POST /predict/both  → Both in one call
 *   GET  /health        → Health check
 */
public class MlApiClient {

    private final String baseUrl;
    private final int    timeoutSeconds;
    private final Gson   gson = new Gson();

    public MlApiClient() {
        this.baseUrl        = EnvLoader.get("ML_API_BASE_URL", "http://127.0.0.1:8001");
        this.timeoutSeconds = Integer.parseInt(EnvLoader.get("ML_API_TIMEOUT_SECONDS", "30"));
    }

    // ── Public API ────────────────────────────────────────────────────────

    public MlPredictionResult predictEsg(ProjectData project) {
        JsonObject response = postJson("/predict/esg", buildPayload(project));
        return parseMlPrediction(response);
    }

    public FraudAssessmentResult predictFraud(ProjectData project) {
        JsonObject response = postJson("/predict/fraud", buildPayload(project));
        return parseFraudResult(response);
    }

    public BothPredictionResult predictBoth(ProjectData project) {
        JsonObject response = postJson("/predict/both", buildPayload(project));

        BothPredictionResult result = new BothPredictionResult();

        if (response.has("esg") && response.get("esg").isJsonObject()) {
            result.setEsg(parseMlPrediction(response.getAsJsonObject("esg")));
        } else {
            // /predict/both may return flat structure
            result.setEsg(parseMlPrediction(response));
        }

        if (response.has("fraud") && response.get("fraud").isJsonObject()) {
            result.setFraud(parseFraudResult(response.getAsJsonObject("fraud")));
        } else {
            result.setFraud(parseFraudResult(response));
        }

        return result;
    }

    public boolean isHealthy() {
        try {
            JsonObject response = getJson("/health");
            return "ok".equals(getString(response, "status"));
        } catch (Exception e) {
            return false;
        }
    }

    /** Get recommendations list. */
    public java.util.List<String> getRecommendations(Models.dto.ProjectData project) {
        JsonObject response = postJson("/recommend", buildPayload(project));
        java.util.List<String> labels = new java.util.ArrayList<>();
        if (response.has("recommendations") && response.get("recommendations").isJsonArray()) {
            response.getAsJsonArray("recommendations").forEach(e -> labels.add(e.getAsString()));
        }
        return labels;
    }

    // ── Payload builder ───────────────────────────────────────────────────

    private JsonObject buildPayload(ProjectData project) {
        JsonObject obj = new JsonObject();

        addDoubleOrNull(obj, "consommation_energie", project.getConsommationEnergie());
        addDoubleOrNull(obj, "distance_transport",   project.getDistanceTransport());
        addDoubleOrNull(obj, "quantite_materiau",    project.getQuantiteMateriau());
        addDoubleOrNull(obj, "consommation_eau",     project.getConsommationEau());
        addDoubleOrNull(obj, "dechets_generes",      project.getDechetsGeneres());
        addDoubleOrNull(obj, "emissions_estimees",   project.getEmissionsEstimees());
        addDoubleOrNull(obj, "total_tco2",           project.getTotalTco2());

        addStringOrNull(obj, "secteur",          project.getSecteur());
        addStringOrNull(obj, "type_projet",      project.getTypeProjet());
        addStringOrNull(obj, "localisation",     project.getLocalisation());
        addStringOrNull(obj, "unite_energie",    project.getUniteEnergie());
        addStringOrNull(obj, "type_transport",   project.getTypeTransport());
        addStringOrNull(obj, "type_materiau",    project.getTypeMateriau());
        addStringOrNull(obj, "source_emissions", project.getSourceEmissions());

        return obj;
    }

    // ── HTTP helpers ──────────────────────────────────────────────────────

    private JsonObject postJson(String path, JsonObject payload) {
        RequestConfig config = RequestConfig.custom()
            .setConnectionRequestTimeout(Timeout.of(timeoutSeconds, TimeUnit.SECONDS))
            .setResponseTimeout(Timeout.of(timeoutSeconds, TimeUnit.SECONDS))
            .build();

        try (CloseableHttpClient client = HttpClients.custom()
                .setDefaultRequestConfig(config).build()) {

            HttpPost request = new HttpPost(baseUrl + path);
            request.setHeader("Content-Type", "application/json");
            request.setHeader("Accept", "application/json");
            request.setEntity(new StringEntity(gson.toJson(payload)));

            return client.execute(request, response -> {
                String body = EntityUtils.toString(response.getEntity());
                int code = response.getCode();
                if (code == 503) throw new RuntimeException("ML service unavailable (503): " + body);
                if (code == 422) throw new RuntimeException("ML service rejected input (422): " + body);
                if (code >= 400) throw new RuntimeException("ML service HTTP " + code + ": " + body);
                return gson.fromJson(body, JsonObject.class);
            });

        } catch (Exception e) {
            throw new RuntimeException("ML service unreachable at " + baseUrl + path + ": " + e.getMessage(), e);
        }
    }

    private JsonObject getJson(String path) {
        RequestConfig config = RequestConfig.custom()
            .setConnectionRequestTimeout(Timeout.of(timeoutSeconds, TimeUnit.SECONDS))
            .setResponseTimeout(Timeout.of(timeoutSeconds, TimeUnit.SECONDS))
            .build();

        try (CloseableHttpClient client = HttpClients.custom()
                .setDefaultRequestConfig(config).build()) {

            HttpGet request = new HttpGet(baseUrl + path);
            return client.execute(request, response -> {
                String body = EntityUtils.toString(response.getEntity());
                return gson.fromJson(body, JsonObject.class);
            });

        } catch (Exception e) {
            throw new RuntimeException("ML health check failed: " + e.getMessage(), e);
        }
    }

    // ── Response parsers ──────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private MlPredictionResult parseMlPrediction(JsonObject data) {
        MlPredictionResult r = new MlPredictionResult();
        r.setPredictedEsgScore(getInt(data, "predicted_esg_score"));
        r.setCredibilityScore(getInt(data, "credibility_score"));
        r.setCarbonRisk(getString(data, "carbon_risk"));
        r.setDecision(getString(data, "decision"));
        r.setRecommendations(getString(data, "recommendations"));
        r.setModelVersion(getString(data, "model_source"));
        return r;
    }

    @SuppressWarnings("unchecked")
    private FraudAssessmentResult parseFraudResult(JsonObject data) {
        FraudAssessmentResult r = new FraudAssessmentResult();
        r.setFraudRiskScore(getDouble(data, "risk_score"));
        r.setFraudAnomalyScore(getDouble(data, "anomaly_score"));
        r.setFraudFlag(getBool(data, "fraud_flag"));

        List<String> reasons = new ArrayList<>();
        if (data.has("reasons") && data.get("reasons").isJsonArray()) {
            data.getAsJsonArray("reasons").forEach(e -> reasons.add(e.getAsString()));
        }
        r.setFraudReasons(reasons);
        r.setModelVersion(getString(data, "model_source"));
        return r;
    }

    // ── JSON helpers ──────────────────────────────────────────────────────

    private void addDoubleOrNull(JsonObject obj, String key, Double value) {
        if (value == null) obj.addProperty(key, (Number) null);
        else               obj.addProperty(key, value);
    }

    private void addStringOrNull(JsonObject obj, String key, String value) {
        if (value == null || value.isBlank()) obj.addProperty(key, (String) null);
        else                                  obj.addProperty(key, value);
    }

    private Integer getInt(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return null;
        return obj.get(key).getAsInt();
    }

    private Double getDouble(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return null;
        return obj.get(key).getAsDouble();
    }

    private String getString(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return null;
        return obj.get(key).getAsString();
    }

    private Boolean getBool(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return null;
        return obj.get(key).getAsBoolean();
    }
}
