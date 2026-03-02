package Services;

import Models.AiSuggestion;
import Models.Evaluation;
import Models.EvaluationResult;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * DynamicPDF API client. Generates a PDF from HTML using a configurable request mode.
 */
public class DynamicPdfService {

    private final String apiUrl;
    private final String apiKey;
    private final String authHeader;
    private final String authScheme;
    private final int timeoutMs;
    private final String requestMode;
    private final String requestTemplatePath;
    private final HttpClient http;
    private final boolean enabled;

    private static Map<String, String> DOTENV = null;

    public DynamicPdfService() {
        this.apiUrl = getEnvOrDotenv("DPDF_API_URL", "https://api.dpdf.io/v1.0/pdf");
        this.apiKey = getEnvOrDotenv("DPDF_API_KEY", null);
        this.authHeader = getEnvOrDotenv("DPDF_AUTH_HEADER", "Authorization");
        this.authScheme = getEnvOrDotenv("DPDF_AUTH_SCHEME", "Bearer");
        this.timeoutMs = Integer.parseInt(getEnvOrDotenv("DPDF_TIMEOUT_MS", "15000"));
        this.requestMode = getEnvOrDotenv("DPDF_REQUEST_MODE", "html");
        this.requestTemplatePath = getEnvOrDotenv("DPDF_REQUEST_TEMPLATE_PATH", null);
        this.enabled = Boolean.parseBoolean(getEnvOrDotenv("DPDF_ENABLED", "false"));
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeoutMs)).build();
    }

    public boolean isConfigured() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    public void writePdfFromHtml(String html, File outputFile) throws IOException, InterruptedException {
        if (outputFile == null) {
            throw new IllegalArgumentException("outputFile is null");
        }
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs() && !parent.exists()) {
                throw new IOException("Cannot create directories: " + parent.getAbsolutePath());
            }
        }
        byte[] pdfBytes = createPdfFromHtml(html);
        if (pdfBytes == null || pdfBytes.length < 32) {
            throw new IOException("DynamicPDF returned empty output");
        }
        Files.write(outputFile.toPath(), pdfBytes);
    }

    public byte[] createPdfFromHtml(String html) throws IOException, InterruptedException {
        if (!isConfigured()) {
            throw new IOException("DynamicPDF API key missing (DPDF_API_KEY)");
        }

        String mode = requestMode == null ? "html" : requestMode.trim();
        if ("auto".equalsIgnoreCase(mode)) {
            return tryAutoModes(html);
        }

        RequestPayload payload = buildPayload(mode, html);
        return sendRequest(payload);
    }

    private byte[] tryAutoModes(String html) throws IOException, InterruptedException {
        // Prioritize the inputs-array format which matches dpdf.io documentation structure
        RequestPayload[] attempts = new RequestPayload[] {
                buildPayload("dpdf-inputs", html),
                buildPayload("html", html),
                buildPayload("json", html),
                buildPayload("json-alt", html),
                buildPayload("json-doc", html),
                buildPayload("template", html)
        };
        IOException last = null;
        for (RequestPayload payload : attempts) {
            if (payload == null) continue;
            try {
                return sendRequest(payload);
            } catch (IOException ex) {
                last = ex;
            }
        }
        if (last != null) throw last;
        throw new IOException("DynamicPDF request failed: no valid request payloads built");
    }

    private RequestPayload buildPayload(String mode, String html) throws IOException {
        String safeHtml = html == null ? "" : html;
        if ("template".equalsIgnoreCase(mode)) {
            if (requestTemplatePath == null || requestTemplatePath.isBlank()) return null;
            String template = Files.readString(Paths.get(requestTemplatePath), StandardCharsets.UTF_8);
            String escapedHtml = escapeJson(safeHtml);
            String htmlBase64 = Base64.getEncoder().encodeToString(safeHtml.getBytes(StandardCharsets.UTF_8));
            String body = template.replace("${HTML}", escapedHtml).replace("${HTML_BASE64}", htmlBase64);
            return new RequestPayload("application/json", body, "template");
        }

        String escaped = escapeJson(safeHtml);

        // Mode: dpdf-inputs (use 'content' field with author/title metadata)
        if ("dpdf-inputs".equalsIgnoreCase(mode) || "auto".equalsIgnoreCase(requestMode)) {
             String body = "{" +
                    "\"author\":\"GreenLedger\"," +
                    "\"title\":\"Evaluation\"," +
                    "\"inputs\":[" +
                        "{" +
                            "\"type\":\"html\"," +
                            "\"content\":\"" + escaped + "\"" +
                        "}" +
                    "]" +
                "}";
             return new RequestPayload("application/json", body, "dpdf-inputs");
        }

        if ("json".equalsIgnoreCase(mode)) {
            String body = "{\"html\":\"" + escaped + "\"}";
            return new RequestPayload("application/json", body, "json");
        }
        if ("json-alt".equalsIgnoreCase(mode)) {
            String body = "{\"Html\":\"" + escapeJson(safeHtml) + "\"}";
            return new RequestPayload("application/json", body, "json-alt");
        }
        if ("json-doc".equalsIgnoreCase(mode)) {
            String body = "{\"document\":{\"html\":\"" + escapeJson(safeHtml) + "\"}}";
            return new RequestPayload("application/json", body, "json-doc");
        }
        // default: raw HTML
        return new RequestPayload("text/html; charset=utf-8", safeHtml, "html");
    }

    private byte[] sendRequest(RequestPayload payload) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Accept", "application/pdf")
                .header("Content-Type", payload.contentType)
                .header(authHeader, buildAuthHeaderValue())
                .POST(HttpRequest.BodyPublishers.ofString(payload.body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
        int sc = resp.statusCode();
        if (sc >= 200 && sc < 300 && resp.body() != null && resp.body().length > 0) {
            return resp.body();
        }
        throw new IOException("DynamicPDF request failed: status=" + sc);
    }

    public String buildEvaluationHtml(Evaluation evaluation,
                                      List<EvaluationResult> criteres,
                                      AiSuggestion suggestion,
                                      byte[] signaturePng,
                                      String evaluatorName,
                                      String evaluatorRole) {
        String title = "GreenLedger - Rapport d'Evaluation Carbone";
        String dateStr = (evaluation != null && evaluation.getDateEvaluation() != null)
                ? evaluation.getDateEvaluation().toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                : "N/A";

        StringBuilder html = new StringBuilder(4096);
        html.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">\n");
        html.append("<style>")
                .append("body{font-family:Arial,sans-serif;color:#333;margin:30px;line-height:1.5;}")
                .append(".header{background-color:#DAF7EA;padding:20px;margin-bottom:20px;border-radius:5px;}")
                .append(".title{font-size:22px;font-weight:bold;margin:0;}")
                .append(".section{margin-top:20px;}")
                .append(".section h3{font-size:16px;color:#2c5f2d;margin-bottom:10px;border-bottom:2px solid #ddd;padding-bottom:5px;}")
                .append(".meta{font-size:13px;margin-bottom:15px;}")
                .append(".meta div{margin-bottom:5px;}")
                .append(".small{font-size:12px;}")
                .append("ul{margin:10px 0;padding-left:25px;}")
                .append("li{margin-bottom:8px;}")
                .append("</style></head><body>\n");

        html.append("<div class=\"header\">");
        html.append("<div class=\"title\">").append(htmlEscape(title)).append("</div>");
        html.append("</div>");

        html.append("<div class=\"section meta\">\n");
        html.append("<div><strong>Date d'evaluation:</strong> ").append(htmlEscape(dateStr)).append("</div>");
        if (evaluation != null) {
            String projectLabel = evaluation.getTitreProjet() != null ? evaluation.getTitreProjet() : "";
            html.append("<div><strong>Projet:</strong> ").append(htmlEscape(projectLabel)).append("</div>");
            html.append("<div><strong>Decision:</strong> ").append(htmlEscape(safe(evaluation.getDecision())))
                    .append(" | <strong>Score global:</strong> ")
                    .append(String.format(Locale.ROOT, "%.2f", evaluation.getScoreGlobal()))
                    .append("</div>");
        }
        html.append("</div>\n");

        html.append("<div class=\"section\"><h3>Observations</h3>");
        html.append("<div class=\"small\">")
                .append(htmlEscape(safe(evaluation != null ? evaluation.getObservations() : ""))).append("</div></div>");

        // ESG summary
        ProjectEsgService.EsgBreakdown b = new ProjectEsgService().breakdown(criteres);
        int esg100 = (int) Math.round(b.esg10 * 10.0);
        html.append("<div class=\"section\"><h3>Score ESG</h3>");
        html.append("<div class=\"small\"><strong>ESG:</strong> ").append(esg100).append("/100</div>");
        html.append("<div class=\"small\">Formule: ESG = 50%*E + 30%*S + 20%*G (0-10) puis x10</div>");
        html.append("<div class=\"small\">E=").append(String.format(Locale.ROOT, "%.2f", b.e))
                .append(", S=").append(String.format(Locale.ROOT, "%.2f", b.s))
                .append(", G=").append(String.format(Locale.ROOT, "%.2f", b.g))
                .append(", ESG(0-10)=").append(String.format(Locale.ROOT, "%.2f", b.esg10))
                .append("</div></div>");

        // Penalties
        html.append("<div class=\"section\"><h3>Penalites appliquees</h3><ul>");
        html.append("<li>Critere non respecte: note x 0.6</li>");
        html.append("<li>CO2 &lt; 4: penalite forte sur l'impact</li>");
        html.append("<li>Echec environnemental (&lt;4): impact reduit</li>");
        html.append("</ul></div>");

        // Key factors
        html.append("<div class=\"section\"><h3>Facteurs cles</h3><ul>");
        if (suggestion != null && suggestion.getTopFactors() != null && !suggestion.getTopFactors().isEmpty()) {
            for (String f : suggestion.getTopFactors()) {
                html.append("<li>").append(htmlEscape(f)).append("</li>");
            }
        } else {
            html.append("<li>Facteurs cles non disponibles.</li>");
        }
        html.append("</ul></div>");

        // Recommendations
        html.append("<div class=\"section\"><h3>Recommandations</h3><ul>");
        Services.AdvancedEvaluationFacade facade = new Services.AdvancedEvaluationFacade();
        List<String> recs = facade.criterionRecommendations(criteres);
        List<String> actionable = recs.stream()
                .filter(r -> !r.contains("OK – Maintenir"))
                .collect(java.util.stream.Collectors.toList());
        if (actionable.isEmpty()) {
            html.append("<li>Aucune action prioritaire identifiee. Les bonnes pratiques sont a maintenir.</li>");
        } else {
            for (String r : actionable) {
                html.append("<li>").append(htmlEscape(r)).append("</li>");
            }
        }
        html.append("</ul></div>");

        html.append("</body></html>");
        return html.toString();
    }

    private String buildAuthHeaderValue() {
        if (authScheme == null || authScheme.isBlank()) {
            return apiKey;
        }
        return authScheme + " " + apiKey;
    }

    private static synchronized void loadDotEnv() {
        if (DOTENV != null) return;
        DOTENV = new HashMap<>();
        String userDir = System.getProperty("user.dir");
        Path[] candidates = new Path[] { Paths.get(userDir, ".env"), Paths.get(".env") };
        Path found = null;
        for (Path p : candidates) {
            if (Files.exists(p)) { found = p; break; }
        }
        if (found == null) {
            try (InputStream is = DynamicPdfService.class.getResourceAsStream("/.env")) {
                if (is != null) {
                    try (Stream<String> lines = new java.io.BufferedReader(new java.io.InputStreamReader(is, StandardCharsets.UTF_8)).lines()) {
                        lines.forEach(line -> putDotEnvLine(line));
                    }
                    return;
                }
            } catch (IOException ignored) { }
        }
        if (found == null) return;
        try (Stream<String> lines = Files.lines(found, StandardCharsets.UTF_8)) {
            lines.forEach(line -> putDotEnvLine(line));
        } catch (IOException ignore) {
            // ignore reading errors
        }
    }

    private static void putDotEnvLine(String line) {
        String l = line.trim();
        if (l.isEmpty() || l.startsWith("#")) return;
        int eq = l.indexOf('=');
        if (eq <= 0) return;
        String k = l.substring(0, eq).trim();
        String v = l.substring(eq + 1).trim();
        if (v.length() >= 2 && ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'")))) {
            v = v.substring(1, v.length() - 1);
        }
        DOTENV.put(k, v);
    }

    private static String getEnvOrDotenv(String key, String defaultValue) {
        String v = System.getenv(key);
        if (v != null && !v.isBlank()) return v;
        loadDotEnv();
        if (DOTENV != null) {
            String dv = DOTENV.get(key);
            if (dv != null && !dv.isBlank()) return dv;
        }
        return defaultValue;
    }

    private byte[] loadResourceBytes(String path) {
        try (InputStream is = DynamicPdfService.class.getResourceAsStream(path);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            if (is == null) return null;
            byte[] buf = new byte[4096];
            int r;
            while ((r = is.read(buf)) != -1) bos.write(buf, 0, r);
            return bos.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String htmlEscape(String text) {
        if (text == null) return "";
        String t = text;
        t = t.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
        return t.replace("\n", "<br/>");
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"': sb.append("\\\""); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    private static class RequestPayload {
        final String contentType;
        final String body;
        final String mode;

        RequestPayload(String contentType, String body, String mode) {
            this.contentType = contentType;
            this.body = body;
            this.mode = mode;
        }
    }
}

























