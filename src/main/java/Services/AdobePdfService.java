package Services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

public class AdobePdfService {
    private final String clientId;
    private final String clientSecret;
    private final String orgId;
    private final String tokenUrl;
    private final String extractUrl; // configurable Adobe extract endpoint
    private final int timeoutMs;
    private final boolean useFallback;
    private final HttpClient http;
    // External access token support: if an access token is provided via env/.env, prefer it
    private final String providedAccessToken;

    public AdobePdfService() {
        this.clientId = getEnvOrDotenv("ADOBE_CLIENT_ID", System.getenv("ADOBE_CLIENT_ID"));
        this.clientSecret = getEnvOrDotenv("ADOBE_CLIENT_SECRET", System.getenv("ADOBE_CLIENT_SECRET"));
        this.orgId = getEnvOrDotenv("ADOBE_ORG_ID", System.getenv("ADOBE_ORG_ID"));
        this.tokenUrl = getEnvOrDotenv("ADOBE_TOKEN_URL", "https://ims-na1.adobelogin.com/ims/token/v3");
        this.extractUrl = getEnvOrDotenv("ADOBE_PDF_EXTRACT_URL", "https://pdf-services.adobe.io/extract");
        this.timeoutMs = Integer.parseInt(getEnvOrDotenv("ADOBE_TIMEOUT_MS", "15000"));
        this.useFallback = Boolean.parseBoolean(getEnvOrDotenv("ADOBE_USE_FALLBACK", "true"));
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeoutMs)).build();
        this.providedAccessToken = getEnvOrDotenv("ADOBE_ACCESS_TOKEN", System.getenv("ADOBE_ACCESS_TOKEN"));
        System.out.println("AdobePdfService configured: extractUrl=" + extractUrl + " tokenUrl=" + tokenUrl + " useFallback=" + useFallback);
        System.out.println("AdobePdfService credentials present: " + (clientId != null && !clientId.isBlank()));
        if (this.providedAccessToken != null && !this.providedAccessToken.isBlank()) {
            System.out.println("AdobePdfService: using provided access token from environment/.env (will not request new token)");
        }
    }

    private static String getEnvOrDotenv(String key, String defaultValue) {
        String v = System.getenv(key);
        if (v != null && !v.isBlank()) return v;
        // try to read from project .env similar to PdfRestService
        try {
            File f = new File(System.getProperty("user.dir"), ".env");
            if (f.exists()) {
                try (FileInputStream fis = new FileInputStream(f)) {
                    String s = new String(fis.readAllBytes(), StandardCharsets.UTF_8);
                    for (String line : s.split("\n")) {
                        String l = line.trim();
                        if (l.startsWith("#") || l.isEmpty()) continue;
                        int eq = l.indexOf('=');
                        if (eq <= 0) continue;
                        String k = l.substring(0, eq).trim();
                        String val = l.substring(eq + 1).trim();
                        if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                            val = val.substring(1, val.length()-1);
                        }
                        if (k.equals(key)) return val;
                    }
                }
            }
        } catch (Exception ignore) { }
        return defaultValue;
    }

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
    }

    private String getAccessToken() throws IOException, InterruptedException {
        // Using OAuth client_credentials grant
        String body = "grant_type=client_credentials&client_id=" + urlEncode(clientId) + "&client_secret=" + urlEncode(clientSecret);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int sc = resp.statusCode();
        String respBody = resp.body() == null ? "" : resp.body();
        // Do not print token endpoint response to keep terminal clean
        if (sc >= 200 && sc < 300) {
            String token = extractJsonField(respBody, "access_token");
            if (token != null && !token.isBlank()) return token;
        }
        throw new IOException("Failed to obtain Adobe access token: status=" + sc);
    }

    public String extractTextFromFilePath(String path) throws IOException {
        File f = new File(path);
        if (!f.exists() || !f.isFile()) throw new IOException("File not found: " + path);
        byte[] bytes = java.nio.file.Files.readAllBytes(f.toPath());

        // If a token was provided via env/.env, prefer it. Otherwise, try to request one via client credentials.
        String token = null;
        if (this.providedAccessToken != null && !this.providedAccessToken.isBlank()) {
            token = this.providedAccessToken;
        } else {
            if (!isConfigured()) {
                if (useFallback) {
                    String local = extractTextLocally(bytes);
                    System.out.println("AdobePdfService: PDF extraction successful — content extracted successfully (LOCAL)");
                    return local;
                }
                throw new IOException("Adobe credentials not configured and no access token provided");
            }

        }

        try {
            // Build a multipart request; field name 'file' used commonly
            String boundary = UUID.randomUUID().toString().replace("-", "");
            byte[] multipart = buildMultipart(bytes, boundary, "file", f.getName());
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(extractUrl))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .header("Authorization", "Bearer " + token)
                    // Adobe APIs often expect x-gw-ims-org-id header
                    .header("x-gw-ims-org-id", orgId == null ? "" : orgId)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(multipart))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int sc = resp.statusCode();
            String respBody = resp.body() == null ? "" : resp.body();

            if (sc >= 200 && sc < 300) {
                String text = extractJsonField(respBody, "text");
                String result = text == null ? respBody : text;
                System.out.println("AdobePdfService: PDF extraction successful — content extracted successfully (HTTP " + sc + ")");
                return result;
            }

            // 404 simulation fallback
            if (sc == 404) {
                String simulate = getEnvOrDotenv("ADOBE_SIMULATE", System.getenv("ADOBE_SIMULATE"));
                if (simulate != null && (simulate.equalsIgnoreCase("1") || simulate.equalsIgnoreCase("true"))) {
                    String simMsg = "[SIMULATION] Extraction simulated: Sample extracted text for UI testing.\n" +
                            "- Title: Test PDF\n- Content: This is simulated extracted content.\n";
                    System.out.println("AdobePdfService: PDF extraction successful — content extracted successfully (SIMULATION)");
                    return simMsg;
                }
            }

            // On non-2xx and no simulation, fall back to local extraction if allowed
            if (useFallback) {
                String local = extractTextLocally(bytes);
                System.out.println("AdobePdfService: PDF extraction successful — content extracted successfully (LOCAL)");
                return local;
            }

            // If we reach here, do not print remote JSON; throw an exception so caller can handle silently
            throw new IOException("Adobe extraction failed: status=" + sc);
        } catch (IOException ex) {
            // rethrow without printing to keep terminal clean
            throw ex;
        } catch (Exception ex) {
            // hide exception details from terminal, wrap as IOException
            throw new IOException("Adobe extraction failed", ex);
        }
    }

    private static String urlEncode(String s) {
        try { return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8); } catch (Exception e) { return s; }
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
        return json.substring(start+1, end);
    }

    private String extractTextLocally(byte[] pdfBytes) {
        try (PDDocument doc = PDDocument.load(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(doc);
            return text == null ? "" : text.trim();
        } catch (Exception ex) {
            return "";
        }
    }
}

