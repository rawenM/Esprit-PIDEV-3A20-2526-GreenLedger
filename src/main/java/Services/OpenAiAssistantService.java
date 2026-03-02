package Services;

import Utils.ApiConfig;

import com.google.gson.*;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class OpenAiAssistantService {

    private final StringBuilder conversationMemory = new StringBuilder();
    private final Gson gson = new Gson();

    public OpenAiAssistantService() {
        // Configuration is loaded from ApiConfig
    }

    private void trimMemory() {
        if (conversationMemory.length() > 4000) {
            conversationMemory.delete(0, 1500);
        }
    }

    private boolean isEnabled() {
        return ApiConfig.isOpenAiEnabled() && !ApiConfig.getOpenAiApiKey().isEmpty();
    }

    private String getApiKey() {
        return ApiConfig.getOpenAiApiKey();
    }

    private String getBaseUrl() {
        return ApiConfig.getOpenAiBaseUrl();
    }

    private int getConnectTimeoutMs() {
        return ApiConfig.getOpenAiConnectTimeout();
    }

    private int getReadTimeoutMs() {
        return ApiConfig.getOpenAiReadTimeout();
    }

    public String ask(String userMessage) {

        if (!isEnabled()) {
            return "Assistant désactivé (OPENAI_API_KEY manquante ou api.openai.enabled=false).";
        }


        conversationMemory.append("Utilisateur: ")
                .append(userMessage)
                .append("\n");

        String input =
                "Tu es l'assistant officiel de GreenLedger.\n" +
                        "Tu aides à préparer et déposer un projet (titre, budget, description, pièces jointes).\n" +
                        "Réponds en français, clair, court.\n\n" +
                        "Conversation actuelle :\n" +
                        conversationMemory.toString();

        JsonObject payload = new JsonObject();
        payload.addProperty("model", "gpt-3.5-turbo");

        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", input);
        messages.add(message);

        payload.add("messages", messages);
        payload.addProperty("max_tokens", 500);
        payload.addProperty("temperature", 0.7);

        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(getConnectTimeoutMs()))
                .setResponseTimeout(Timeout.ofMilliseconds(getReadTimeoutMs()))
                .build();

        try (CloseableHttpClient http = HttpClients.custom()
                .setDefaultRequestConfig(config)
                .build()) {

            HttpPost post = new HttpPost(getBaseUrl() + "/chat/completions");
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

                    // OpenAI chat completion response structure
                    if (json.has("choices")) {
                        JsonElement choicesElement = json.get("choices");
                        if (choicesElement.isJsonArray()) {
                            JsonArray choices = choicesElement.getAsJsonArray();
                            if (choices.size() > 0) {
                                JsonObject choice = choices.get(0).getAsJsonObject();
                                if (choice.has("message")) {
                                    JsonObject messageObj = choice.getAsJsonObject("message");
                                    if (messageObj.has("content")) {
                                        String answer = messageObj.get("content").getAsString();
                                        conversationMemory.append("Assistant: ")
                                                .append(answer)
                                                .append("\n\n");
                                        trimMemory();
                                        return answer;
                                    }
                                }
                            }
                        }
                    }

                    // Fallback for legacy response format
                    if (json.has("output_text") && !json.get("output_text").isJsonNull()) {
                        String answer = json.get("output_text").getAsString();
                        conversationMemory.append("Assistant: ")
                                .append(answer)
                                .append("\n\n");
                        trimMemory();
                        return answer;
                    }

                    if (json.has("output")) {
                        JsonElement outputElement = json.get("output");
                        if (outputElement.isJsonArray()) {
                            JsonArray output = outputElement.getAsJsonArray();
                            if (output.size() > 0) {
                                JsonObject first = output.get(0).getAsJsonObject();
                                if (first.has("content")) {
                                    JsonElement contentElement = first.get("content");
                                    if (contentElement.isJsonArray()) {
                                        JsonArray content = contentElement.getAsJsonArray();
                                        if (content.size() > 0) {
                                            JsonObject c0 = content.get(0).getAsJsonObject();
                                            if (c0.has("text")) {
                                                return c0.get("text").getAsString();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    return "Réponse reçue mais format inattendu: " + raw;
                } catch (Exception e) {
                    System.err.println("[OpenAI] Erreur parsing réponse: " + e.getMessage());
                    return "Réponse OpenAI reçue mais impossible à parser: " + raw;
                }
            });

        } catch (Exception e) {
            return "Erreur appel OpenAI: " + e.getMessage();
        }
    }
}