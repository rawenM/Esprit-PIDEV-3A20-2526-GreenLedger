package Services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
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
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Service to extract text from PDF using an external pdfrest API with a local PDFBox fallback.
 * Configure via env vars or a .env file at the project root:
 *  - PDFREST_BASE_URL (e.g. https://api.pdfrest.com/v1/extract) — endpoint that accepts a file upload
 *  - PDFREST_API_KEY (required for remote call)
 *  - PDFREST_TIMEOUT_MS (default 10000)
 *  - PDFREST_USE_FALLBACK (true/false, default true)
 */
public class PdfRestService {

    private final String baseUrl;
    private String apiKey;
    private final int timeoutMs;
    private final boolean useFallback;
    private final HttpClient http;

    // cached dotenv map (lazy-loaded)
    private static Map<String, String> DOTENV = null;

    // default constructor reads API key from environment or .env
    public PdfRestService() {
        this(null);
    }

    // constructor that accepts an API key override (useful for passing key from headers safely at runtime)
    public PdfRestService(String apiKeyOverride) {
        this.baseUrl = getEnvOrDotenv("PDFREST_BASE_URL", "https://api.pdfrest.com/v1/extract");
        this.apiKey = (apiKeyOverride != null && !apiKeyOverride.isBlank()) ? apiKeyOverride : getEnvOrDotenv("PDFREST_API_KEY", null);
        this.timeoutMs = Integer.parseInt(getEnvOrDotenv("PDFREST_TIMEOUT_MS", "10000"));
        this.useFallback = Boolean.parseBoolean(getEnvOrDotenv("PDFREST_USE_FALLBACK", "true"));
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeoutMs)).build();
        // Debug print to confirm which key/source is used
        System.out.println("PdfRestService configured: baseUrl=" + baseUrl + " timeoutMs=" + timeoutMs + " useFallback=" + useFallback);
        System.out.println("PdfRestService apiKey present: " + (this.apiKey != null && !this.apiKey.isBlank()));
    }

    private static synchronized void loadDotEnv() {
        if (DOTENV != null) return;
        DOTENV = new HashMap<>();
        // try several locations: user.dir/.env, working dir ./.env, and classpath resource
        String userDir = System.getProperty("user.dir");
        Path[] candidates = new Path[] { Paths.get(userDir, ".env"), Paths.get(".env") };
        Path found = null;
        for (Path p : candidates) {
            if (Files.exists(p)) { found = p; break; }
        }
        if (found == null) {
            // try classpath resource
            try (InputStream is = PdfRestService.class.getResourceAsStream("/.env")) {
                if (is != null) {
                    try (Stream<String> lines = new java.io.BufferedReader(new java.io.InputStreamReader(is, StandardCharsets.UTF_8)).lines()) {
                        lines.forEach(line -> {
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
                        });
                    }
                    return;
                }
            } catch (IOException ignored) { }
        }
        if (found == null) return;
        try (Stream<String> lines = Files.lines(found, StandardCharsets.UTF_8)) {
            lines.forEach(line -> {
                String l = line.trim();
                if (l.isEmpty() || l.startsWith("#")) return;
                int eq = l.indexOf('=');
                if (eq <= 0) return;
                String k = l.substring(0, eq).trim();
                String v = l.substring(eq + 1).trim();
                // remove surrounding quotes if present
                if (v.length() >= 2 && ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'")))) {
                    v = v.substring(1, v.length() - 1);
                }
                DOTENV.put(k, v);
            });
        } catch (IOException ignore) {
            // ignore reading errors, fallback will use env vars
        }
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

    public String extractTextFromFilePath(String path) throws IOException, InterruptedException {
        File f = new File(path);
        if (!f.exists() || !f.isFile()) throw new IOException("File not found: " + path);
        if (f.length() == 0) return "";
        if (f.length() > 50L * 1024L * 1024L) { // safety: 50MB hard limit
            throw new IOException("File too large (>50MB)");
        }
        try (FileInputStream fis = new FileInputStream(f); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = fis.read(buf)) != -1) bos.write(buf, 0, r);
            return extractText(bos.toByteArray());
        }
    }

    public String extractText(byte[] pdfBytes) throws IOException, InterruptedException {
        // If no API key, go directly to fallback
        if (apiKey == null || apiKey.isEmpty()) {
            if (useFallback) return extractTextLocally(pdfBytes);
            throw new IOException("No PDFREST_API_KEY set and fallback disabled");
        }

        // candidate endpoint variations (include original baseUrl and possible common variants)
        String[] endpointCandidates = new String[] {
                baseUrl,
                baseUrl + "/extract",
                baseUrl + "/text",
                baseUrl + "/extract/text",
                baseUrl + "/files/extract"
        };

        // Try a sequence of upload formats and endpoints until one returns a 2xx response.
        String[] multipartFieldNames = new String[] {"file", "files", "files[]"};
        for (String endpoint : endpointCandidates) {
            for (String field : multipartFieldNames) {
                String boundary = UUID.randomUUID().toString().replace("-", "");
                byte[] multipart = buildMultipart(pdfBytes, boundary, field, "upload.pdf");
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .timeout(Duration.ofMillis(timeoutMs))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .header("Authorization", "Bearer " + apiKey)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(multipart))
                        .build();
                try {
                    HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                    int sc = resp.statusCode();
                    String respBody = resp.body() == null ? "" : resp.body();
                    System.out.println("PdfRestService HTTP " + endpoint + " status=" + sc + " responseLen=" + (respBody == null ? 0 : respBody.length()));
                    if (sc >= 200 && sc < 300) {
                        String text = extractJsonField(respBody, "text");
                        return text != null ? text : respBody;
                    }
                    // If API complains about missing file, try next method
                    if (respBody != null && respBody.toLowerCase().contains("at least one file")) {
                        System.err.println("PdfRestService: endpoint '" + endpoint + "' responded missing file for field '" + field + "' - trying next field name/endpoint");
                        continue;
                    }
                    // If API says invalid endpoint, try next endpoint candidate
                    if (respBody != null && respBody.toLowerCase().contains("invalid endpoint")) {
                        System.err.println("PdfRestService: endpoint '" + endpoint + "' invalid according to server response - trying next candidate");
                        break; // break inner loop to try next endpoint
                    }
                    // other non-2xx: log and try next
                    System.err.println("PdfRestService non-2xx response from '" + endpoint + "': " + respBody);
                } catch (IOException | InterruptedException ex) {
                    System.err.println("PdfRestService exception calling API '" + endpoint + "' with field '" + field + "': " + ex.getMessage());
                }
            }
        }

        // Last resort: try JSON base64 payload if multipart attempts failed
        for (String endpoint : endpointCandidates) {
            try {
                String base64 = Base64.getEncoder().encodeToString(pdfBytes);
                String jsonPayload = String.format(java.util.Locale.ROOT, "{\"files\":[{\"name\":\"%s\",\"content\":\"%s\"}]}", "upload.pdf", base64);
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .timeout(Duration.ofMillis(timeoutMs))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                        .build();
                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                int sc = resp.statusCode();
                String respBody = resp.body() == null ? "" : resp.body();
                System.out.println("PdfRestService (json) HTTP " + endpoint + " status=" + sc + " responseLen=" + (respBody == null ? 0 : respBody.length()));
                if (sc >= 200 && sc < 300) {
                    String text = extractJsonField(respBody, "text");
                    return text != null ? text : respBody;
                }
                System.err.println("PdfRestService json upload non-2xx from '" + endpoint + "': " + respBody);
            } catch (Exception ex) {
                System.err.println("PdfRestService json upload failed for endpoint '" + endpoint + "': " + ex.getMessage());
            }
        }

        if (useFallback) return extractTextLocally(pdfBytes);
        throw new IOException("Failed to extract via pdfrest and fallback disabled");
    }

    private byte[] buildMultipart(byte[] fileBytes, String boundary, String fieldName, String filename) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String nl = "\r\n";
        String partHeader = "--" + boundary + nl
                + "Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + filename + "\"" + nl
                + "Content-Type: application/pdf" + nl + nl;
        out.write(partHeader.getBytes(StandardCharsets.UTF_8));
        out.write(fileBytes);
        out.write(nl.getBytes(StandardCharsets.UTF_8));
        String end = "--" + boundary + "--" + nl;
        out.write(end.getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    /**
     * Naive JSON field extractor for small responses. Returns null if field not found.
     */
    private String extractJsonField(String json, String field) {
        if (json == null || json.isEmpty()) return null;
        String key = '"' + field + '"';
        int idx = json.indexOf(key);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx);
        if (colon < 0) return null;
        int start = json.indexOf('"', colon);
        if (start < 0) return null;
        int end = json.indexOf('"', start + 1);
        if (end < 0) return null;
        String raw = json.substring(start + 1, end);
        // unescape some common escapes
        return raw.replaceAll("\\n", "\n").replaceAll("\\\"", "\"");
    }

    private String extractTextLocally(byte[] pdfBytes) throws IOException {
        try (PDDocument doc = PDDocument.load(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(doc);
            return text == null ? "" : text.trim();
        }
    }
}

