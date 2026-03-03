package Utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class CaptchaService {

    private final Properties captchaProps = new Properties();
    private boolean configured = false;

    public CaptchaService() {
        String envSiteKey = System.getenv("RECAPTCHA_SITE_KEY");
        String envSecret = System.getenv("RECAPTCHA_SECRET_KEY");
        if (envSiteKey != null && !envSiteKey.isEmpty() && envSecret != null && !envSecret.isEmpty()) {
            captchaProps.put("captcha.site.key", envSiteKey);
            captchaProps.put("captcha.secret", envSecret);
            captchaProps.put("captcha.verify.url", System.getenv().getOrDefault(
                    "RECAPTCHA_VERIFY_URL",
                    "https://www.google.com/recaptcha/api/siteverify"
            ));
            configured = true;
            return;
        }

        try (InputStream in = getClass().getResourceAsStream("/config.properties")) {
            if (in == null) {
                System.err.println("[CLEAN] config.properties not found in classpath");
                return;
            }
            captchaProps.load(in);
            String siteKey = captchaProps.getProperty("captcha.site.key");
            String secret = captchaProps.getProperty("captcha.secret");
            
            System.out.println("[CLEAN] CaptchaService loaded from config.properties:");
            System.out.println("[CLEAN]   - Site Key: " + (siteKey != null ? siteKey.substring(0, Math.min(10, siteKey.length())) + "... (length: " + siteKey.length() + ")" : "null"));
            System.out.println("[CLEAN]   - Secret: " + (secret != null ? secret.substring(0, Math.min(10, secret.length())) + "... (length: " + secret.length() + ")" : "null"));
            
            if (isNonEmpty(siteKey) && isNonEmpty(secret)) {
                configured = true;
                System.out.println("[CLEAN] CaptchaService configured successfully");
            } else {
                System.err.println("[CLEAN] CaptchaService: Keys are empty or null");
            }
        } catch (Exception e) {
            System.err.println("[CLEAN] Impossible de charger config.properties pour CaptchaService: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean isConfigured() {
        return configured;
    }

    public String getSiteKey() {
        return captchaProps.getProperty("captcha.site.key", "");
    }

    public boolean verifyToken(String token) {
        if (!configured || token == null || token.trim().isEmpty()) {
            return false;
        }
        try {
            String secret = captchaProps.getProperty("captcha.secret", "");
            String verifyUrl = captchaProps.getProperty(
                    "captcha.verify.url",
                    "https://www.google.com/recaptcha/api/siteverify"
            );

            String form = "secret=" + URLEncoder.encode(secret, StandardCharsets.UTF_8) +
                    "&response=" + URLEncoder.encode(token, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(verifyUrl))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("[CLEAN] Captcha verify failed: HTTP " + response.statusCode());
                return false;
            }

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            boolean success = json.has("success") && json.get("success").getAsBoolean();

            if (!success) {
                System.err.println("[CLEAN] Captcha verify rejected: " + json);
                return false;
            }

            // For reCAPTCHA v3, also check the score (0.0 to 1.0)
            // Score >= 0.5 is typically considered acceptable
            if (json.has("score")) {
                double score = json.get("score").getAsDouble();
                System.out.println("[CLEAN] reCAPTCHA v3 score: " + score);
                return score >= 0.5;
            }

            // For v2 or if no score, just return success status
            return true;
        } catch (Exception e) {
            System.err.println("[CLEAN] Captcha verify exception: " + e.getMessage());
            return false;
        }
    }

    private boolean isNonEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
