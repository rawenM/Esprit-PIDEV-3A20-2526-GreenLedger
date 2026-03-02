package Api;

import Models.AiSuggestion;
import Models.CritereReference;
import Models.Evaluation;
import Models.EvaluationResult;
import Services.AdvancedEvaluationFacade;
import Services.CritereImpactService;
import Services.EvaluationService;
import Services.PdfService;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Mini serveur HTTP sans dépendances externes.
 * Endpoints (GET):
 *  - /api/criteres/references
 *  - /api/evaluations/score-preview?criteres=ID:NOTE:RESPECTE,ID:NOTE:RESPECTE
 *  - /api/ai/evaluations/suggest-decision?criteres=ID:NOTE:RESPECTE,ID:NOTE:RESPECTE
 *
 * RESPECTE: 1 (true) ou 0 (false)
 */
public class ApiServer {

    private final AdvancedEvaluationFacade facade = new AdvancedEvaluationFacade();
    private final CritereImpactService critereService = new CritereImpactService();
    private final EvaluationService evaluationService = new EvaluationService();

    public static void main(String[] args) throws IOException {
        new ApiServer().start(8080);
    }

    public void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/criteres/references", this::handleListReferences);
        server.createContext("/api/evaluations/score-preview", this::handleScorePreview);
        server.createContext("/api/ai/evaluations/suggest-decision", this::handleSuggestDecision);
        server.createContext("/api/evaluations/pdf", this::handleEvaluationPdf);
        server.createContext("/api/ai/doccat", this::handleDoccat); // ML debug endpoint
        server.createContext("/api/ai/evaluations/predict", this::handlePredictDecision);
        server.createContext("/predict", this::handlePredictDecision);

        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        System.out.println("API server started on http://localhost:" + port);
        server.start();
    }

    private void handleListReferences(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }
        List<CritereReference> refs = critereService.afficherReferences();
        String json = toJsonReferences(refs);
        send(exchange, 200, json);
    }

    private void handleScorePreview(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }
        Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
        String raw = params.get("criteres");
        if (raw == null || raw.isEmpty()) {
            send(exchange, 400, "{\"error\":\"Missing 'criteres' query param\"}");
            return;
        }
        List<EvaluationResult> list = parseCriteres(raw);
        double score = facade.computeScore(list);
        String explanations = facade.explainScore(list).stream()
                .map(e -> String.format(Locale.ROOT,
                        "{\"idCritere\":%d,\"nomCritere\":\"%s\",\"poids\":%d,\"note\":%d,\"contribution\":%.2f}",
                        e.getIdCritere(), escape(e.getNomCritere()), e.getPoids(), e.getNote(), e.getContribution()))
                .collect(Collectors.joining(","));
        String json = String.format(Locale.ROOT, "{\"score\":%.2f,\"explanations\":[%s]}", score, explanations);
        send(exchange, 200, json);
    }

    private void handleSuggestDecision(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }
        Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
        String raw = params.get("criteres");
        if (raw == null || raw.isEmpty()) {
            send(exchange, 400, "{\"error\":\"Missing 'criteres' query param\"}");
            return;
        }
        List<EvaluationResult> list = parseCriteres(raw);
        AiSuggestion s = facade.suggest(null, list);

        String topFactors = s.getTopFactors().stream()
                .map(f -> "\"" + escape(f) + "\"")
                .collect(Collectors.joining(","));

        String warnings = s.getWarnings().stream()
                .map(w -> "\"" + escape(w) + "\"")
                .collect(Collectors.joining(","));

        String json = String.format(Locale.ROOT,
                "{\"suggestion\":\"%s\",\"confiance\":%.3f,\"score\":%.2f,\"topFactors\":[%s],\"warnings\":[%s]}",
                escape(s.getSuggestionDecision()), s.getConfiance(), s.getScore(), topFactors, warnings);

        send(exchange, 200, json);
    }

    private void handleEvaluationPdf(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }
        Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
        String idStr = params.get("id");
        String download = params.getOrDefault("download", "0");
        if (idStr == null || idStr.isEmpty()) {
            send(exchange, 400, "{\"error\":\"Missing 'id' query param (evaluation id)\"}");
            return;
        }
        int idEv;
        try {
            idEv = Integer.parseInt(idStr);
        } catch (NumberFormatException ex) {
            send(exchange, 400, "{\"error\":\"Invalid 'id' (must be integer)\"}");
            return;
        }

        Optional<Evaluation> evalOpt = findEvaluationById(idEv);
        if (evalOpt.isEmpty()) {
            send(exchange, 404, "{\"error\":\"Evaluation not found\"}");
            return;
        }
        Evaluation ev = evalOpt.get();
        List<EvaluationResult> res = critereService.afficherParEvaluation(idEv);
        if (res == null) res = new ArrayList<>();
        AiSuggestion s = facade.suggest(null, res);

        File out = new File("generated-reports/evaluation-" + idEv + ".pdf");
        try {
            new PdfService().generateEvaluationPdf(ev, res, s, out);
        } catch (IOException e) {
            send(exchange, 500, "{\"error\":\"Failed to generate PDF: " + escape(e.getMessage()) + "\"}");
            return;
        }

        if ("1".equals(download)) {
            byte[] bytes = Files.readAllBytes(out.toPath());
            sendPdf(exchange, bytes, out.getName());
        } else {
            String json = String.format(Locale.ROOT,
                    "{\"saved\":\"%s\",\"size\":%d}",
                    escape(out.getAbsolutePath()), out.length());
            send(exchange, 200, json);
        }
    }

    private Optional<Evaluation> findEvaluationById(int id) {
        List<Evaluation> list = evaluationService.afficher();
        if (list == null) return Optional.empty();
        for (Evaluation e : list) {
            if (e != null && e.getIdEvaluation() == id) {
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }

    private void sendPdf(HttpExchange exchange, byte[] bytes, String filename) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/pdf");
        exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"" + filename.replace("\"", "") + "\"");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void handleDoccat(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }
        Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
        String text = params.get("text");
        if (text == null) {
            send(exchange, 400, "{\"error\":\"Missing 'text' query param\"}");
            return;
        }
        Services.NlpMlService nlp = Services.NlpMlService.getInstance();
        java.util.Map<String, Double> scores = nlp.categorizeWithScores(text);
        String best = scores.keySet().iterator().next();
        String scoreJson = scores.entrySet().stream()
                .map(e -> "\"" + escape(e.getKey()) + "\":" + String.format(java.util.Locale.ROOT, "%.4f", e.getValue()))
                .collect(java.util.stream.Collectors.joining(","));
        String body = "{\"best\":\"" + escape(best) + "\",\"scores\":{" + scoreJson + "}}";
        send(exchange, 200, body);
    }

    private void handlePredictDecision(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        System.out.println("[ML] /predict request received");

        String body;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            body = sb.toString();
        }

        if (body == null || body.isBlank()) {
            send(exchange, 400, "{\"error\":\"Empty request body\"}");
            System.out.println("[ML] /predict failed: empty body");
            return;
        }

        String projectRoot = System.getenv().getOrDefault("PROJECT_ROOT", System.getProperty("user.dir"));
        File rootDir = resolveProjectRoot(projectRoot);
        File modelFile = resolveModelFile(rootDir);
        if (!modelFile.exists()) {
            boolean built = tryBuildModel(rootDir);
            if (!built || !modelFile.exists()) {
                send(exchange, 500, "{\"error\":\"Model file missing. Run ml/run_pipeline.py to generate models/carbon_model.joblib\"}");
                System.out.println("[ML] /predict failed: model missing");
                return;
            }
        }

        String python = resolvePythonExecutable();
        if (python == null) {
            String msg = "Python introuvable. Définissez PYTHON avec le chemin complet (ex: C:\\Users\\Mega-PC\\AppData\\Local\\Programs\\Python\\Python312\\python.exe).";
            send(exchange, 500, "{\"error\":\"" + escape(msg) + "\"}");
            System.out.println("[ML] /predict failed: python not found");
            return;
        }

        File scriptFile = new File(rootDir, "ml/predict.py");
        ProcessBuilder pb = new ProcessBuilder(
                python,
                scriptFile.getAbsolutePath(),
                "--model",
                modelFile.getAbsolutePath()
        );
        pb.directory(rootDir);
        pb.redirectErrorStream(true);

        try {
            Process proc = pb.start();
            proc.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
            proc.getOutputStream().flush();
            proc.getOutputStream().close();

            BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                out.append(line);
            }

            boolean finished = proc.waitFor(8, TimeUnit.SECONDS);
            if (!finished) {
                proc.destroyForcibly();
                send(exchange, 500, "{\"error\":\"Predictor timeout\"}");
                System.out.println("[ML] /predict failed: timeout");
                return;
            }

            int exit = proc.exitValue();
            if (exit != 0) {
                String err = out.length() == 0 ? "Predictor failed" : out.toString();
                send(exchange, 500, "{\"error\":\"" + escape(err) + "\"}");
                System.out.println("[ML] /predict failed: exit=" + exit);
                return;
            }

            send(exchange, 200, out.toString());
            System.out.println("[ML] /predict success");
        } catch (IOException ioEx) {
            String msg = "Python introuvable. Définissez PYTHON avec le chemin complet (ex: C:\\Users\\Mega-PC\\AppData\\Local\\Programs\\Python\\Python312\\python.exe).";
            send(exchange, 500, "{\"error\":\"" + escape(msg) + "\"}");
            System.out.println("[ML] /predict failed: python not found");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            send(exchange, 500, "{\"error\":\"Predictor interrupted\"}");
            System.out.println("[ML] /predict failed: interrupted");
        }
    }

    private File resolveModelFile(File rootDir) {
        String modelPath = System.getenv().getOrDefault("CARBON_MODEL_PATH", "models/carbon_model.joblib");
        File modelFile = new File(modelPath);
        if (!modelFile.isAbsolute()) {
            modelFile = new File(rootDir, modelPath);
        }
        return modelFile;
    }

    private String resolvePythonExecutable() {
        String env = System.getenv("PYTHON");
        if (env != null && !env.isBlank() && new File(env).exists()) return env;
        String envAlt = System.getenv("PYTHON_EXE_PATH");
        if (envAlt != null && !envAlt.isBlank() && new File(envAlt).exists()) return envAlt;

        String[] candidates = new String[] {
                "C:\\Users\\Mega-PC\\AppData\\Local\\Programs\\Python\\Python312\\python.exe",
                "C:\\Users\\Mega-PC\\AppData\\Local\\Programs\\Python\\Python311\\python.exe",
                "C:\\Users\\Mega-PC\\AppData\\Local\\Programs\\Python\\Python310\\python.exe"
        };
        for (String c : candidates) {
            if (new File(c).exists()) return c;
        }
        return "python"; // final fallback; may still fail if not on PATH
    }

    private boolean tryBuildModel(File rootDir) {
        String python = resolvePythonExecutable();
        if (python == null) return false;
        File scriptFile = new File(rootDir, "ml/run_pipeline.py");
        if (!scriptFile.exists()) return false;
        ProcessBuilder pb = new ProcessBuilder(python, scriptFile.getAbsolutePath(), "--rows", "20000");
        pb.directory(rootDir);
        pb.redirectErrorStream(true);
        try {
            Process proc = pb.start();
            boolean finished = proc.waitFor(120, TimeUnit.SECONDS);
            if (!finished) {
                proc.destroyForcibly();
                return false;
            }
            return proc.exitValue() == 0;
        } catch (Exception ex) {
            return false;
        }
    }

    private File resolveProjectRoot(String start) {
        File dir = new File(start);
        if (!dir.exists()) return new File(System.getProperty("user.dir"));
        File cursor = dir.isFile() ? dir.getParentFile() : dir;
        while (cursor != null) {
            File pom = new File(cursor, "pom.xml");
            if (pom.exists()) return cursor;
            cursor = cursor.getParentFile();
        }
        return new File(System.getProperty("user.dir"));
    }

    // Helpers

    private void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> map = new HashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) return map;
        for (String part : rawQuery.split("&")) {
            int idx = part.indexOf('=');
            if (idx > 0) {
                String k = URLDecoder.decode(part.substring(0, idx), StandardCharsets.UTF_8);
                String v = URLDecoder.decode(part.substring(idx + 1), StandardCharsets.UTF_8);
                map.put(k, v);
            } else {
                map.put(URLDecoder.decode(part, StandardCharsets.UTF_8), "");
            }
        }
        return map;
    }

    /**
     * Parse "criteres" param: "ID:NOTE:RESPECTE,ID:NOTE:RESPECTE"
     * RESPECTE: 1 (true) / 0 (false)
     */
    private List<EvaluationResult> parseCriteres(String raw) {
        List<EvaluationResult> list = new ArrayList<>();
        Map<Integer, String> nameIndex = critereService.afficherReferences()
                .stream().collect(Collectors.toMap(CritereReference::getIdCritere, CritereReference::getNomCritere, (a, b) -> a));
        for (String token : raw.split(",")) {
            String[] parts = token.trim().split(":");
            if (parts.length >= 2) {
                try {
                    int id = Integer.parseInt(parts[0]);
                    int note = Integer.parseInt(parts[1]);
                    boolean respecte = true;
                    if (parts.length >= 3) {
                        respecte = "1".equals(parts[2]);
                    }
                    String nom = nameIndex.getOrDefault(id, "Critère #" + id);
                    EvaluationResult r = new EvaluationResult(id, nom, note, null, respecte);
                    list.add(r);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return list;
    }

    private String toJsonReferences(List<CritereReference> refs) {
        String data = refs.stream().map(r -> String.format(Locale.ROOT,
                        "{\"idCritere\":%d,\"nomCritere\":\"%s\",\"description\":\"%s\",\"poids\":%d}",
                        r.getIdCritere(), escape(r.getNomCritere()), escape(r.getDescription()), r.getPoids()))
                .collect(Collectors.joining(","));
        return "{\"data\":[]}".replace("[]","[" + data + "]");
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
