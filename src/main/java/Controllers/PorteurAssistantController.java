package Controllers;

import DataBase.MyConnection;
import Utils.EnvLoader;
import Utils.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.Duration;

/**
 * Page 10 — AI Assistant (Gemini) for Porteur de Projet.
 */
public class PorteurAssistantController extends BaseController {

    @FXML private VBox     boxChat;
    @FXML private TextArea txtInput;
    @FXML private Button   btnSend;
    @FXML private javafx.scene.control.ScrollPane scrollChat;

    private long currentUserId;

    @FXML
    public void initialize() {
        super.initialize();
        var user = SessionManager.getInstance().getCurrentUser();
        if (user != null) currentUserId = user.getId();

        // Enter key sends message
        txtInput.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER && !e.isShiftDown()) {
                e.consume();
                onSend();
            }
        });
    }

    @FXML private void onBack() { navigate("fxml/porteur_shell"); }

    @FXML
    private void onClear() {
        boxChat.getChildren().clear();
        addAiMessage("Bonjour ! Comment puis-je vous aider avec votre strategie GreenLedger ?");
    }

    @FXML
    private void onSend() {
        String question = txtInput.getText().trim();
        if (question.isBlank()) return;
        txtInput.clear();

        addUserMessage(question);
        btnSend.setDisable(true);

        String context = buildContext();
        new Thread(() -> {
            String response = callGemini(question, context);
            Platform.runLater(() -> {
                addAiMessage(response != null ? response : "Desolee, je n'ai pas pu repondre. Reessayez.");
                btnSend.setDisable(false);
                scrollToBottom();
            });
        }).start();
    }

    // ── Chat UI ───────────────────────────────────────────────────────────

    private void addUserMessage(String text) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_RIGHT);
        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(500);
        bubble.setStyle("-fx-background-color:#2D5F3F;-fx-text-fill:white;-fx-font-size:13px;"
            + "-fx-border-radius:10;-fx-background-radius:10;-fx-padding:10 14;");
        row.getChildren().add(bubble);
        boxChat.getChildren().add(row);
        scrollToBottom();
    }

    private void addAiMessage(String text) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.TOP_LEFT);
        Label avatar = new Label("AI");
        avatar.setStyle("-fx-background-color:#2D5F3F;-fx-text-fill:white;-fx-font-size:10px;"
            + "-fx-font-weight:700;-fx-background-radius:14;-fx-min-width:28;-fx-min-height:28;-fx-alignment:CENTER;");
        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(560);
        bubble.setStyle("-fx-background-color:white;-fx-text-fill:#1A2E26;-fx-font-size:13px;"
            + "-fx-border-color:#E5E7EB;-fx-border-width:1;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:10 14;");
        row.getChildren().addAll(avatar, bubble);
        boxChat.getChildren().add(row);
    }

    private void scrollToBottom() {
        Platform.runLater(() -> scrollChat.setVvalue(1.0));
    }

    // ── Gemini API ────────────────────────────────────────────────────────

    private String callGemini(String question, String context) {
        EnvLoader.load();
        String apiKey = EnvLoader.get("GEMINI_API_KEY", "");
        if (apiKey.isBlank()) return fallbackResponse(question);

        String systemInstruction = "Tu es l'assistant strategique GreenLedger. "
            + "Tu expliques les KPI, la strategie de financement, les risques, et les prochaines actions. "
            + "Reponds en francais, de facon claire, concise, utile et actionnable. "
            + "Utilise uniquement le contexte fourni, et si une information manque, dis-le explicitement.";

        String userContent = "Contexte GreenLedger: " + context + "\\n\\nQuestion: " + escapeJson(question);

        String body = "{"
            + "\"systemInstruction\":{\"parts\":[{\"text\":\"" + escapeJson(systemInstruction) + "\"}]},"
            + "\"contents\":[{\"role\":\"user\",\"parts\":[{\"text\":\"" + userContent + "\"}]}],"
            + "\"generationConfig\":{\"temperature\":0.4,\"topP\":0.95,\"maxOutputTokens\":768}"
            + "}";

        String model = EnvLoader.get("GEMINI_MODEL", "gemini-2.5-flash");
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
            + model + ":generateContent?key=" + apiKey;

        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15)).build();
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(25))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                return extractGeminiContent(resp.body());
            }
            System.err.println("[PorteurAssistant] Gemini HTTP " + resp.statusCode());
        } catch (Exception e) {
            System.err.println("[PorteurAssistant] Gemini error: " + e.getMessage());
        }
        return fallbackResponse(question);
    }

    private String extractGeminiContent(String json) {
        int idx = json.indexOf("\"text\":\"");
        if (idx < 0) return null;
        int start = idx + 8;
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char n = json.charAt(i + 1);
                if (n == 'n') { sb.append('\n'); i++; }
                else if (n == '"') { sb.append('"'); i++; }
                else if (n == '\\') { sb.append('\\'); i++; }
                else sb.append(c);
            } else if (c == '"') break;
            else sb.append(c);
        }
        return sb.toString().trim();
    }

    private String fallbackResponse(String question) {
        return "Je n'ai pas pu contacter l'IA (cle API manquante ou erreur reseau).\n\n"
            + "Votre question: \"" + question + "\"\n\n"
            + "Conseil: Verifiez votre cle GEMINI_API_KEY dans le fichier .env.";
    }

    // ── Context builder ───────────────────────────────────────────────────

    private String buildContext() {
        int total = 0, approved = 0, submitted = 0, inProgress = 0;
        int evaluations = 0, pending = 0;
        double availableCredits = 0, retiredCredits = 0;
        int financements = 0;

        try (Connection conn = MyConnection.getConnection()) {
            // Projects
            String sql = "SELECT statut, COUNT(*) AS cnt FROM projet WHERE entreprise_id=? GROUP BY statut";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, currentUserId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int cnt = rs.getInt("cnt");
                        total += cnt;
                        switch (rs.getString("statut")) {
                            case "APPROVED"    -> approved   += cnt;
                            case "SUBMITTED"   -> submitted  += cnt;
                            case "IN_PROGRESS" -> inProgress += cnt;
                        }
                    }
                }
            }
            // Evaluations
            String evalSql = "SELECT COUNT(*) FROM evaluation e JOIN projet p ON p.id=e.id_projet WHERE p.entreprise_id=?";
            try (PreparedStatement ps = conn.prepareStatement(evalSql)) {
                ps.setLong(1, currentUserId);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) evaluations = rs.getInt(1); }
            }
            // Pending evaluations
            String pendSql = "SELECT COUNT(*) FROM evaluation e JOIN projet p ON p.id=e.id_projet WHERE p.entreprise_id=? AND e.est_valide IS NULL";
            try (PreparedStatement ps = conn.prepareStatement(pendSql)) {
                ps.setLong(1, currentUserId);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) pending = rs.getInt(1); }
            }
            // Wallet
            String walletSql = "SELECT SUM(available_credits), SUM(retired_credits) FROM wallet WHERE owner_id=?";
            try (PreparedStatement ps = conn.prepareStatement(walletSql)) {
                ps.setLong(1, currentUserId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        availableCredits = rs.getDouble(1);
                        retiredCredits   = rs.getDouble(2);
                    }
                }
            } catch (Exception ignored) {}
            // Financements
            String finSql = "SELECT COUNT(*) FROM financements f JOIN projet p ON p.id=f.project_id WHERE p.entreprise_id=?";
            try (PreparedStatement ps = conn.prepareStatement(finSql)) {
                ps.setLong(1, currentUserId);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) financements = rs.getInt(1); }
            } catch (Exception ignored) {}
        } catch (SQLException e) {
            System.err.println("[PorteurAssistant] buildContext: " + e.getMessage());
        }

        return "{\\\"projects\\\":{\\\"total\\\":" + total
            + ",\\\"approved\\\":" + approved
            + ",\\\"submitted\\\":" + submitted
            + ",\\\"in_progress\\\":" + inProgress + "},"
            + "\\\"wallet\\\":{\\\"available_credits\\\":" + availableCredits
            + ",\\\"retired_credits\\\":" + retiredCredits + "},"
            + "\\\"financing\\\":{\\\"requests\\\":" + financements + "},"
            + "\\\"evaluations\\\":{\\\"total\\\":" + evaluations
            + ",\\\"pending\\\":" + pending + "}}";
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "").replace("\t", "\\t");
    }

    private void navigate(String fxml) {
        try { org.GreenLedger.MainFX.setRoot(fxml); }
        catch (Exception e) { System.err.println("[PorteurAssistant] Nav: " + e.getMessage()); }
    }
}
