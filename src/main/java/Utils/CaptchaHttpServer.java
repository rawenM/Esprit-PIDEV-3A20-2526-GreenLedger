package Utils;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class CaptchaHttpServer {

    private final HttpServer server;
    private final String html;

    public CaptchaHttpServer(String siteKey) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/captcha", new CaptchaHandler());
        server.setExecutor(Executors.newFixedThreadPool(1));
        html = loadCaptchaHtml(siteKey);
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    public String getCaptchaUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/captcha";
    }

    private String loadCaptchaHtml(String siteKey) {
        String safeKey = siteKey == null ? "" : siteKey;
        try (InputStream in = getClass().getResourceAsStream("/captcha.html")) {
            if (in == null) {
                return fallbackHtml(safeKey);
            }
            String htmlContent = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return htmlContent.replace("SITE_KEY_PLACEHOLDER", safeKey);
        } catch (IOException e) {
            return fallbackHtml(safeKey);
        }
    }

    private String fallbackHtml(String siteKey) {
        return "<!doctype html><html><head><meta charset='UTF-8'/>" +
                "<script src='https://www.google.com/recaptcha/api.js'></script>" +
                "</head><body>" +
                "<div class='g-recaptcha' data-sitekey='" + siteKey + "'></div>" +
                "</body></html>";
    }

    private class CaptchaHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            Headers headers = exchange.getResponseHeaders();
            headers.set("Content-Type", "text/html; charset=utf-8");
            headers.set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}
