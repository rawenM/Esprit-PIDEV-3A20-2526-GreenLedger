package Services;

import com.google.gson.*;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class OpenAiAssistantService {

    private final StringBuilder conversationMemory = new StringBuilder();
    private final Gson gson = new Gson();
    private final Properties props = new Properties();
    private void trimMemory() {
        if (conversationMemory.length() > 4000) {
            conversationMemory.delete(0, 1500);
        }
    }

    public OpenAiAssistantService() {
        try (InputStream in = OpenAiAssistantService.class.getResourceAsStream("/api-config.properties")) {
            if (in != null) props.load(in);
        } catch (Exception ignored) {}
    }

    private boolean isEnabled() {
        return Boolean.parseBoolean(props.getProperty("api.openai.enabled", "true")) && !getApiKey().isEmpty();
    }

    private String getApiKey() {
        String key = System.getenv("OPENAI_API_KEY");
        if (key == null || key.trim().isEmpty()) key = props.getProperty("openai.api.key", "");
        return key == null ? "" : key.trim();
    }

    private String getBaseUrl() {
        return props.getProperty("openai.api.url", "https://api.openai.com/v1").trim();
    }

    private int getConnectTimeoutMs() {
        return Integer.parseInt(props.getProperty("api.connection.timeout", "5000").trim());
    }

    private int getReadTimeoutMs() {
        return Integer.parseInt(props.getProperty("api.read.timeout", "60000").trim());
    }

    public String ask(String userMessage) {

        if (!isEnabled()) {
            return "Assistant désactivé (OPENAI_API_KEY manquante ou api.openai.enabled=false).";
        }


        conversationMemory.append("Utilisateur: ")
                .append(userMessage)
                .append("\n");

        String input =
                "Tu es l’assistant officiel de GreenLedger.\n" +
                        "Tu aides à préparer et déposer un projet (titre, budget, description, pièces jointes).\n" +
                        "Réponds en français, clair, court.\n\n" +
                        "Conversation actuelle :\n" +
                        conversationMemory.toString();

        JsonObject payload = new JsonObject();
        payload.addProperty("model", "gpt-4.1-mini");
        payload.addProperty("input", input);

        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(getConnectTimeoutMs()))
                .setResponseTimeout(Timeout.ofMilliseconds(getReadTimeoutMs()))
                .build();

        try (CloseableHttpClient http = HttpClients.custom()
                .setDefaultRequestConfig(config)
                .build()) {

            HttpPost post = new HttpPost(getBaseUrl() + "/responses");
            post.setHeader("Authorization", "Bearer " + getApiKey());
            post.setHeader("Content-Type", "application/json");
            post.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));

            return http.execute(post, response -> {
                int status = response.getCode();

                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                }
                String raw = sb.toString();

                if (status < 200 || status >= 300) {
                    return "Erreur OpenAI HTTP " + status + " : " + raw;
                }

                try {
                    JsonObject json = JsonParser.parseString(raw).getAsJsonObject();

                    if (json.has("output_text") && !json.get("output_text").isJsonNull()) {
                        String answer = json.get("output_text").getAsString();

                        //reponse dns cache
                        conversationMemory.append("Assistant: ")
                                .append(answer)
                                .append("\n\n");

                        return answer;
                    }

                    trimMemory();

                    if (json.has("output")) {
                        JsonArray output = json.getAsJsonArray("output");
                        if (output.size() > 0) {
                            JsonObject first = output.get(0).getAsJsonObject();
                            if (first.has("content")) {
                                JsonArray content = first.getAsJsonArray("content");
                                if (content.size() > 0) {
                                    JsonObject c0 = content.get(0).getAsJsonObject();
                                    if (c0.has("text")) return c0.get("text").getAsString();
                                }
                            }
                        }
                    }

                    return "Réponse reçue mais format inattendu: " + raw;
                } catch (Exception e) {
                    return "Réponse OpenAI reçue mais impossible à parser: " + raw;
                }
            });

        } catch (Exception e) {
            return "Erreur appel OpenAI: " + e.getMessage();
        }
    }
}