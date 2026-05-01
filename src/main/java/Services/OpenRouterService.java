package Services;

import Utils.EnvLoader;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

/**
 * OpenRouter AI client — generates contextual ESG recommendations.
 *
 * Reads config from .env:
 *   OPENROUTER_API_KEY, OPENROUTER_MODEL, OPENROUTER_BASE_URL,
 *   OPENROUTER_HTTP_REFERER, OPENROUTER_APP_NAME
 *
 * Falls back gracefully (returns null) if the API is unavailable or the key
 * is missing, so callers can use local heuristics instead.
 */
public class OpenRouterService {

    private static final int TIMEOUT_SECONDS = 20;

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final String referer;
    private final String appName;

    public OpenRouterService() {
        EnvLoader.load();
        this.apiKey  = EnvLoader.get("OPENROUTER_API_KEY",    "");
        this.model   = EnvLoader.get("OPENROUTER_MODEL",      "nvidia/nemotron-3-super-120b-a12b:free");
        this.baseUrl = EnvLoader.get("OPENROUTER_BASE_URL",   "https://openrouter.ai/api/v1");
        this.referer = EnvLoader.get("OPENROUTER_HTTP_REFERER","http://localhost:8000");
        this.appName = EnvLoader.get("OPENROUTER_APP_NAME",   "GreenLedger");
    }

    /** Returns true if an API key is configured. */
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Generate ESG recommendations for a project evaluation.
     *
     * @param projectName   project title
     * @param decision      ML decision (APPROVED / REJECTED / REVISION_REQUIRED)
     * @param esgScore      predicted ESG score (0-10)
     * @param carbonRisk    LOW / MEDIUM / HIGH
     * @param totalTco2     total carbon emissions in tCO2e
     * @param fraudRisk     fraud risk score (0.0-1.0)
     * @param criteriaLines list of "CriterionName: note/10" strings
     * @return pipe-separated recommendations string, or null on failure
     */
    public String generateRecommendations(
            String projectName,
            String decision,
            int    esgScore,
            String carbonRisk,
            double totalTco2,
            double fraudRisk,
            List<String> criteriaLines) {

        if (!isAvailable()) {
            System.out.println("[OpenRouter] No API key configured — skipping AI recommendations");
            return null;
        }

        String prompt = buildPrompt(projectName, decision, esgScore, carbonRisk,
                                    totalTco2, fraudRisk, criteriaLines);
        String requestBody = buildRequestBody(prompt);

        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .header("Authorization",  "Bearer " + apiKey)
                .header("Content-Type",   "application/json")
                .header("HTTP-Referer",   referer)
                .header("X-Title",        appName)
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                System.err.println("[OpenRouter] HTTP " + response.statusCode() + ": " + response.body());
                return null;
            }

            return parseContent(response.body());

        } catch (Exception e) {
            System.err.println("[OpenRouter] Request failed: " + e.getMessage());
            return null;
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String buildPrompt(String projectName, String decision, int esgScore,
                                String carbonRisk, double totalTco2, double fraudRisk,
                                List<String> criteriaLines) {

        StringBuilder sb = new StringBuilder();
        sb.append("You are an ESG expert advisor for GreenLedger, a green finance platform.\n\n");
        sb.append("Project: ").append(projectName != null ? projectName : "Unknown").append("\n");
        sb.append("ML Decision: ").append(decision).append("\n");
        sb.append("ESG Score: ").append(esgScore).append("/10\n");
        sb.append("Carbon Risk: ").append(carbonRisk).append("\n");
        sb.append(String.format(Locale.ROOT, "Total Emissions: %.3f tCO2e\n", totalTco2));
        sb.append(String.format(Locale.ROOT, "Fraud Risk: %.1f%%\n\n", fraudRisk * 100));

        if (criteriaLines != null && !criteriaLines.isEmpty()) {
            sb.append("Evaluation criteria:\n");
            for (String line : criteriaLines) {
                sb.append("- ").append(line).append("\n");
            }
            sb.append("\n");
        }

        sb.append("Generate exactly 6 concise, actionable ESG improvement recommendations for this project.\n");
        sb.append("Rules:\n");
        sb.append("- Each recommendation must be on its own line, starting with a number (1. 2. etc.)\n");
        sb.append("- Be specific and measurable (include targets, timeframes, or metrics where possible)\n");
        sb.append("- Focus on the weakest criteria and highest-impact improvements\n");
        sb.append("- Write in French\n");
        sb.append("- Do NOT include any introduction or conclusion, just the 6 numbered recommendations\n");

        return sb.toString();
    }

    private String buildRequestBody(String prompt) {
        // Manually build JSON to avoid adding a JSON library dependency
        String escapedPrompt = prompt
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");

        return "{"
            + "\"model\":\"" + model + "\","
            + "\"messages\":[{\"role\":\"user\",\"content\":\"" + escapedPrompt + "\"}],"
            + "\"max_tokens\":600,"
            + "\"temperature\":0.7"
            + "}";
    }

    /**
     * Parse the OpenRouter response JSON and extract the assistant message content,
     * then convert numbered lines into a pipe-separated string.
     */
    private String parseContent(String json) {
        // Extract content from: {"choices":[{"message":{"content":"..."}}]}
        int contentIdx = json.indexOf("\"content\":");
        if (contentIdx < 0) {
            System.err.println("[OpenRouter] No 'content' field in response");
            return null;
        }

        int start = json.indexOf("\"", contentIdx + 10);
        if (start < 0) return null;
        start++; // skip opening quote

        // Find the closing quote, respecting escape sequences
        StringBuilder content = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case 'n' -> { content.append('\n'); i++; }
                    case 'r' -> { i++; } // skip \r
                    case 't' -> { content.append('\t'); i++; }
                    case '"' -> { content.append('"'); i++; }
                    case '\\' -> { content.append('\\'); i++; }
                    default  -> content.append(c);
                }
            } else if (c == '"') {
                break; // end of string
            } else {
                content.append(c);
            }
        }

        String raw = content.toString().trim();
        if (raw.isEmpty()) return null;

        // Convert numbered lines to pipe-separated
        String[] lines = raw.split("\n");
        StringBuilder result = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            // Strip leading "1. " "2. " etc.
            trimmed = trimmed.replaceFirst("^\\d+\\.\\s*", "").trim();
            if (trimmed.isEmpty()) continue;
            if (result.length() > 0) result.append(" | ");
            result.append(trimmed);
        }

        String out = result.toString().trim();
        System.out.println("[OpenRouter] Generated " + lines.length + " recommendations");
        return out.isEmpty() ? null : out;
    }
}
