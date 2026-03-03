package Utils;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import Services.IUserService;
import Services.UserServiceImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Serveur HTTP minimal pour accepter un lien de réinitialisation de mot de passe.
 * - GET /reset?token=...  -> rend un formulaire HTML
 * - POST /reset          -> traite token + password + confirm
 *
 * Destiné à être lancé localement (localhost) et uniquement pour des flows de reset.
 */
public class ResetHttpServer {

    private final HttpServer server;
    private final IUserService userService = new UserServiceImpl();

    public ResetHttpServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/reset", new ResetHandler());
        server.setExecutor(Executors.newFixedThreadPool(2));
    }

    public void start() {
        server.start();
        System.out.println("[CLEAN] ResetHttpServer démarré sur http://127.0.0.1:" + server.getAddress().getPort());
    }

    public void stop() {
        server.stop(0);
        System.out.println("⛔ ResetHttpServer arrêté");
    }

    private class ResetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if (method.equalsIgnoreCase("GET")) {
                handleGet(exchange);
            } else if (method.equalsIgnoreCase("POST")) {
                handlePost(exchange);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private void handleGet(HttpExchange exchange) throws IOException {
            URI uri = exchange.getRequestURI();
            String query = uri.getRawQuery();
            Map<String, String> params = parseQuery(query);
            String token = params.getOrDefault("token", "");

            String html = "<!doctype html><html><head><meta charset=\"utf-8\"><title>Reset password</title></head><body>"
                    + "<h2>Réinitialisation du mot de passe</h2>"
                    + "<form method=\"post\" action=\"/reset\">"
                    + "<label>Token (depuis email):</label><br/>"
                    + "<input type=\"text\" name=\"token\" value=\"" + escapeHtml(token) + "\" style=\"width:100%\"/><br/><br/>"
                    + "<label>Nouveau mot de passe:</label><br/>"
                    + "<input type=\"password\" name=\"password\" style=\"width:100%\"/><br/><br/>"
                    + "<label>Confirmer le mot de passe:</label><br/>"
                    + "<input type=\"password\" name=\"confirm\" style=\"width:100%\"/><br/><br/>"
                    + "<button type=\"submit\">Réinitialiser</button>"
                    + "</form></body></html>";

            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            Headers h = exchange.getResponseHeaders();
            h.set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private void handlePost(HttpExchange exchange) throws IOException {
            // lire body
            String body = readAll(exchange.getRequestBody());
            Map<String, String> params = parseQuery(body);
            String token = params.getOrDefault("token", "");
            String password = params.getOrDefault("password", "");
            String confirm = params.getOrDefault("confirm", "");

            String resultHtml;
            if (token.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                resultHtml = buildResultPage(false, "Veuillez remplir tous les champs.");
            } else if (!password.equals(confirm)) {
                resultHtml = buildResultPage(false, "Les mots de passe ne correspondent pas.");
            } else {
                boolean ok = userService.resetPasswordWithToken(token, password);
                if (ok) {
                    resultHtml = buildResultPage(true, "Mot de passe réinitialisé avec succès.");
                } else {
                    resultHtml = buildResultPage(false, "Echec de la réinitialisation (token invalide/expiré ou mot de passe non conforme).");
                }
            }

            byte[] bytes = resultHtml.getBytes(StandardCharsets.UTF_8);
            Headers h = exchange.getResponseHeaders();
            h.set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private String buildResultPage(boolean success, String message) {
            String color = success ? "#10B981" : "#EF4444";
            return "<!doctype html><html><head><meta charset=\"utf-8\"><title>Reset Result</title></head><body>"
                    + "<h2 style=\"color:" + color + "\">" + (success ? "Succès" : "Erreur") + "</h2>"
                    + "<p>" + escapeHtml(message) + "</p>"
                    + "</body></html>";
        }

        private Map<String, String> parseQuery(String query) {
            Map<String, String> map = new HashMap<>();
            if (query == null || query.isEmpty()) return map;
            String[] parts = query.split("&");
            for (String part : parts) {
                int idx = part.indexOf('=');
                if (idx > 0) {
                    String k = urlDecode(part.substring(0, idx));
                    String v = urlDecode(part.substring(idx + 1));
                    map.put(k, v);
                }
            }
            return map;
        }

        private String urlDecode(String s) {
            try {
                return URLDecoder.decode(s, StandardCharsets.UTF_8.name());
            } catch (Exception e) {
                return s;
            }
        }

        private String readAll(InputStream is) throws IOException {
            byte[] buf = is.readAllBytes();
            return new String(buf, StandardCharsets.UTF_8);
        }

        private String escapeHtml(String s) {
            if (s == null) return "";
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
        }
    }
}

