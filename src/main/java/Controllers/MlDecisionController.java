package Controllers;

import Models.Evaluation;
import Models.EvaluationResult;
import Models.Projet;
import Services.AdvancedEvaluationFacade;
import Services.CritereImpactService;
import Services.EvaluationService;
import Services.ProjectEsgService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MlDecisionController extends BaseController {

    @FXML private Label lblProfileName;
    @FXML private Label lblProfileType;

    @FXML private Label lblProjectName;
    @FXML private Label lblDecision;
    @FXML private Label lblConfidence;
    @FXML private ProgressBar barConfidence;
    @FXML private Label lblScore;
    @FXML private Label lblCompliance;
    @FXML private Label lblMinNote;
    @FXML private Label lblStatus;

    @FXML private Label lblEsgScore;
    @FXML private TextArea txtEsgFormula;
    @FXML private Label lblEsgImportance;

    @FXML private ListView<String> listFactors;
    @FXML private LineChart<Number, Number> chartImpact;
    @FXML private TextArea txtExplanation;
    @FXML private TextArea txtRecommendations;
    @FXML private TableView<ImpactRow> tableImpactDetails;
    @FXML private TableColumn<ImpactRow, String> colDetailCritere;
    @FXML private TableColumn<ImpactRow, String> colDetailCategory;
    @FXML private TableColumn<ImpactRow, String> colDetailNote;
    @FXML private TableColumn<ImpactRow, String> colDetailWeight;
    @FXML private TableColumn<ImpactRow, String> colDetailImpact;
    @FXML private TableColumn<ImpactRow, String> colDetailPenalty;

    @FXML private Button btnRunMl;

    private final EvaluationService evaluationService = new EvaluationService();
    private final CritereImpactService critereImpactService = new CritereImpactService();
    private final AdvancedEvaluationFacade facade = new AdvancedEvaluationFacade();

    private Projet selectedProjet;
    private List<EvaluationResult> lastResults;

    @FXML
    public void initialize() {
        super.initialize();
        applyProfile(lblProfileName, lblProfileType);
        loadContext();
    }

    private void loadContext() {
        selectedProjet = CarbonAuditController.getSelectedProjet();
        if (selectedProjet == null) {
            setStatus("Sélectionnez un projet dans la gestion carbone.");
            setProjectName("—");
            disableMl(true);
            return;
        }
        setProjectName(selectedProjet.getTitre());
        if (lblEsgScore != null) {
            Integer esg = selectedProjet.getScoreEsg();
            lblEsgScore.setText(esg == null ? "—" : String.valueOf(esg));
        }
        Evaluation latest = getLatestEvaluation(selectedProjet.getId());
        if (latest == null) {
            setStatus("Aucune évaluation disponible pour ce projet.");
            disableMl(true);
            return;
        }
        lastResults = critereImpactService.afficherParEvaluation(latest.getIdEvaluation());
        if (lastResults == null || lastResults.isEmpty()) {
            setStatus("Aucun critère saisi pour l'évaluation.");
            disableMl(true);
            return;
        }
        updateSummary(lastResults);
        updateFactorsAndCurve(lastResults);
        updateLocalDecisionAndRecommendations(lastResults);
        setStatus("");
        disableMl(true);
        handleRunMl();
    }

    private Evaluation getLatestEvaluation(int projetId) {
        List<Evaluation> evals = evaluationService.afficherParProjet(projetId);
        if (evals == null || evals.isEmpty()) {
            return null;
        }
        evals.sort(Comparator.comparingInt(Evaluation::getIdEvaluation));
        return evals.get(evals.size() - 1);
    }

    private void updateSummary(List<EvaluationResult> results) {
        double avgNote = results.stream().mapToInt(EvaluationResult::getNote).average().orElse(0.0);
        int minNote = results.stream().mapToInt(EvaluationResult::getNote).min().orElse(0);
        double complianceRate = results.stream().mapToDouble(r -> r.isEstRespecte() ? 1.0 : 0.0).average().orElse(0.0);
        double score = facade.computeScore(results);

        if (lblScore != null) {
            lblScore.setText(String.format(Locale.ROOT, "%.2f/10", score));
        }
        if (lblCompliance != null) {
            lblCompliance.setText(String.format(Locale.ROOT, "%.0f%%", complianceRate * 100.0));
        }
        if (lblMinNote != null) {
            lblMinNote.setText(String.valueOf(minNote));
        }

        if (txtExplanation != null) {
            String explain = "Statistiques cles:\n" +
                    "• Score moyen des criteres: " + String.format(Locale.ROOT, "%.2f", avgNote) + "/10\n" +
                    "• Taux de conformite: " + String.format(Locale.ROOT, "%.0f", complianceRate * 100.0) + "%\n" +
                    "• Plus faible note: " + minNote + "\n\n" +
                    "La decision ML combine ces indicateurs avec les impacts par critere.";
            txtExplanation.setText(explain);
        }
    }

    private void updateFactorsAndCurve(List<EvaluationResult> results) {
        if (listFactors != null) {
            listFactors.getItems().clear();
        }
        if (chartImpact != null) {
            chartImpact.getData().clear();
            chartImpact.setLegendVisible(true);
            chartImpact.setCreateSymbols(true);
        }

        java.util.Map<Integer, Models.CritereReference> refById = new java.util.HashMap<>();
        for (Models.CritereReference ref : critereImpactService.afficherReferences()) {
            refById.put(ref.getIdCritere(), ref);
        }

        int co2Note = -1;
        for (EvaluationResult r : results) {
            Models.CritereReference ref = refById.get(r.getIdCritere());
            String name = ref != null ? ref.getNomCritere() : "";
            String desc = ref != null ? ref.getDescription() : "";
            if (isCo2Criterion(name, desc)) {
                co2Note = r.getNote();
                break;
            }
        }

        List<ImpactPoint> points = new java.util.ArrayList<>();
        for (EvaluationResult r : results) {
            Models.CritereReference ref = refById.get(r.getIdCritere());
            String name = ref != null ? ref.getNomCritere() : ("Critere #" + r.getIdCritere());
            String desc = ref != null ? ref.getDescription() : "";
            int weight = ref != null ? Math.max(1, ref.getPoids()) : 1;
            Category category = classifyCategory(name, desc);
            double impact = computeImpact(r.getNote(), weight, category, co2Note);
            points.add(new ImpactPoint(r.getIdCritere(), name, r.getNote(), impact, category, weight, co2Note >= 0 && co2Note < 4, (category == Category.ENV || category == Category.ENV_CO2) && r.getNote() < 4));
        }

        points.sort(java.util.Comparator.comparingDouble(ImpactPoint::impact).reversed());

        if (listFactors != null) {
            for (int i = 0; i < Math.min(6, points.size()); i++) {
                ImpactPoint p = points.get(i);
                listFactors.getItems().add(p.name + " • impact " + String.format(Locale.ROOT, "%.2f", p.impact) + " • note " + p.note + "/10");
            }
        }

        if (tableImpactDetails != null) {
            java.util.List<ImpactRow> rows = new java.util.ArrayList<>();
            for (ImpactPoint p : points) {
                String penalties = p.co2Critical ? "CO2 < 4" : "";
                if (p.envFailure) {
                    penalties = penalties.isEmpty() ? "Echec env" : penalties + ", echec env";
                }
                rows.add(new ImpactRow(
                        p.name,
                        p.category.name(),
                        String.valueOf(p.note),
                        String.valueOf(p.weight),
                        String.format(Locale.ROOT, "%.2f", p.impact),
                        penalties.isEmpty() ? "—" : penalties
                ));
            }
            tableImpactDetails.getItems().setAll(rows);
        }

        if (chartImpact != null) {
            XYChart.Series<Number, Number> impactSeries = new XYChart.Series<>();
            impactSeries.setName("Impact par critere");

            XYChart.Series<Number, Number> noteSeries = new XYChart.Series<>();
            noteSeries.setName("Note du critere (/10)");

            XYChart.Series<Number, Number> esgSeries = new XYChart.Series<>();
            esgSeries.setName("ESG projet (/100)");

            int idx = 1;
            for (ImpactPoint p : points) {
                if (idx > 10) break;
                XYChart.Data<Number, Number> impactPoint = new XYChart.Data<>(idx, p.impact);
                XYChart.Data<Number, Number> notePoint = new XYChart.Data<>(idx, p.note);
                impactSeries.getData().add(impactPoint);
                noteSeries.getData().add(notePoint);
                idx++;
            }

            Integer esgValue = (lblEsgScore != null) ? parseEsgLabel(lblEsgScore.getText()) : null;
            if (esgValue != null) {
                for (int i = 1; i < idx; i++) {
                    esgSeries.getData().add(new XYChart.Data<>(i, esgValue));
                }
            }

            chartImpact.getData().add(impactSeries);
            chartImpact.getData().add(noteSeries);
            if (!esgSeries.getData().isEmpty()) {
                chartImpact.getData().add(esgSeries);
            }

            attachTooltips(impactSeries, "Impact");
            attachTooltips(noteSeries, "Note");
            attachTooltips(esgSeries, "ESG");
            attachPointLabels(impactSeries, "I");
            attachPointLabels(noteSeries, "N");
        }
    }

    private void attachPointLabels(XYChart.Series<Number, Number> series, String prefix) {
        if (series == null) return;
        for (XYChart.Data<Number, Number> d : series.getData()) {
            Label label = new Label(prefix + ":" + String.format(Locale.ROOT, "%.2f", d.getYValue().doubleValue()));
            label.getStyleClass().add("muted-note");
            StackPane pane = new StackPane();
            pane.getChildren().add(label);
            d.setNode(pane);
        }
    }

    private enum Category {
        ENV_CO2,
        ENV,
        SOC,
        GOV,
        OTHER
    }

    private record ImpactPoint(int id, String name, int note, double impact, Category category, int weight, boolean co2Critical, boolean envFailure) {}

    private record ImpactRow(String critere, String categorie, String note, String poids, String impact, String penalites) {}

    private boolean isCo2Criterion(String name, String desc) {
        String text = (name + " " + desc).toLowerCase(Locale.ROOT);
        return text.contains("co2") || text.contains("émission") || text.contains("emission") || text.contains("carbone") || text.contains("ghg") || text.contains("scope");
    }

    private Category classifyCategory(String name, String desc) {
        String text = (name + " " + desc).toLowerCase(Locale.ROOT);
        if (isCo2Criterion(name, desc)) {
            return Category.ENV_CO2;
        }
        if (text.contains("eau") || text.contains("energie") || text.contains("énergie") || text.contains("dechet") || text.contains("déchet") || text.contains("biodivers") || text.contains("pollution") || text.contains("air") || text.contains("sol") || text.contains("renouvel") || text.contains("ressource")) {
            return Category.ENV;
        }
        if (text.contains("social") || text.contains("emploi") || text.contains("sant") || text.contains("secur") || text.contains("sécur") || text.contains("inclusion") || text.contains("commun") || text.contains("egal") || text.contains("égalité") || text.contains("formation")) {
            return Category.SOC;
        }
        if (text.contains("gouvernance") || text.contains("compliance") || text.contains("audit") || text.contains("ethique") || text.contains("éthique") || text.contains("transparence") || text.contains("risque") || text.contains("conform")) {
            return Category.GOV;
        }
        return Category.OTHER;
    }

    private double computeImpact(int note, int weight, Category category, int co2Note) {
        double n = Math.max(0, Math.min(10, note));
        double base = (1.0 - Math.exp(-n / 3.5)) * weight; // diminishing returns

        double catBoost = switch (category) {
            case ENV_CO2 -> 1.35;
            case ENV -> 1.2;
            case SOC -> 0.9;
            case GOV -> 0.85;
            case OTHER -> 0.8;
        };

        double impact = base * catBoost;

        boolean co2Critical = (co2Note >= 0 && co2Note < 4);
        if (co2Critical) {
            double penalty = switch (category) {
                case ENV_CO2 -> 0.4;
                case ENV -> 0.6;
                case SOC -> 0.8;
                case GOV -> 0.9;
                case OTHER -> 0.85;
            };
            impact *= penalty;
        }

        boolean envFailure = (category == Category.ENV || category == Category.ENV_CO2) && n < 4;
        if (envFailure) {
            impact *= 0.7; // environmental failures propagate
        }

        return impact;
    }

    private void attachTooltips(XYChart.Series<Number, Number> series, String label) {
        if (series == null) return;
        for (XYChart.Data<Number, Number> d : series.getData()) {
            String text = label + ": " + d.getYValue();
            Tooltip.install(d.getNode(), new Tooltip(text));
        }
    }

    @FXML
    private void handleRunMl() {
        if (selectedProjet == null || lastResults == null || lastResults.isEmpty()) {
            setStatus("Impossible de lancer ML sans données.");
            return;
        }
        setStatus("ML en cours...");
        System.out.println("[ML] Training started");

        String description = selectedProjet.getDescription() != null && !selectedProjet.getDescription().isBlank()
                ? selectedProjet.getDescription()
                : selectedProjet.getTitre();
        double budget = selectedProjet.getBudget();
        String sector = (selectedProjet.getActivityType() != null && !selectedProjet.getActivityType().isBlank())
                ? selectedProjet.getActivityType() : "unknown";

        Map<String, Object> payloadMap = new java.util.LinkedHashMap<>();
        payloadMap.put("description", description);
        payloadMap.put("budget", budget);
        payloadMap.put("sector", sector);
        payloadMap.put("criteres", buildCriteriaPayload(lastResults));

        String payload = new Gson().toJson(payloadMap);
        if (payload == null || payload.isBlank()) {
            setStatus("Erreur ML: requete vide");
            if (txtRecommendations != null) {
                txtRecommendations.setText("Erreur ML: requete vide. Verifiez les donnees projet/criteres.");
            }
            System.out.println("[ML] Dashboard empty payload");
            return;
        }
        String baseUrl = System.getenv().getOrDefault("ML_API_BASE_URL", "http://localhost:8082");
        String endpoint = baseUrl.endsWith("/") ? baseUrl + "analyze-project" : baseUrl + "/analyze-project";

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(8))
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String body = resp.body() == null ? "" : resp.body().trim();

            if (resp.statusCode() != 200) {
                setStatus("");
                return;
            }

            if (body.isEmpty()) {
                setStatus("");
                return;
            }

            String jsonBody = body;
            if (!jsonBody.startsWith("{")) {
                int start = jsonBody.indexOf('{');
                int end = jsonBody.lastIndexOf('}');
                if (start >= 0 && end > start) {
                    jsonBody = jsonBody.substring(start, end + 1).trim();
                }
            }

            if (!jsonBody.startsWith("{")) {
                setStatus("");
                return;
            }

            Map<String, Object> parsed = new Gson().fromJson(jsonBody, new TypeToken<Map<String, Object>>(){}.getType());
            int esgScore = getInt(parsed.get("predicted_esg_score"), 0);
            int credibility = getInt(parsed.get("credibility_score"), 0);
            String carbonRisk = String.valueOf(parsed.getOrDefault("carbon_risk", "N/A"));
            String recommendations = String.valueOf(parsed.getOrDefault("recommendations", ""));

            String decision = deriveDecision(esgScore, carbonRisk);
            CarbonAuditController.storeMlDecision(selectedProjet.getId(), decision);
            updateDecision(decision, String.valueOf(Math.max(0.0, Math.min(1.0, credibility / 100.0))));
            updateRecommendationsText(recommendations);
            setStatus("ML termine");
            System.out.println("[ML] Training completed");
        } catch (Exception ex) {
            setStatus("");
        }
    }

    private java.util.List<java.util.Map<String, Object>> buildCriteriaPayload(List<EvaluationResult> results) {
        java.util.Map<Integer, String> names = new java.util.HashMap<>();
        try {
            for (Models.CritereReference ref : critereImpactService.afficherReferences()) {
                names.put(ref.getIdCritere(), ref.getNomCritere());
            }
        } catch (Exception ignore) { }

        java.util.List<java.util.Map<String, Object>> out = new java.util.ArrayList<>();
        for (EvaluationResult r : results) {
            java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("name", names.getOrDefault(r.getIdCritere(), "Critere #" + r.getIdCritere()));
            row.put("note", r.getNote());
            row.put("respect", r.isEstRespecte());
            out.add(row);
        }
        return out;
    }

    private String deriveDecision(int esgScore, String carbonRisk) {
        String risk = carbonRisk == null ? "" : carbonRisk.toLowerCase(Locale.ROOT);
        if (esgScore >= 65 && !risk.contains("high")) {
            return "APPROVE";
        }
        if (esgScore >= 55 && risk.contains("low")) {
            return "APPROVE";
        }
        return "REJECT";
    }

    private int getInt(Object value, int fallback) {
        if (value == null) return fallback;
        try {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            return fallback;
        }
    }

    private void updateRecommendationsText(String recommendations) {
        if (txtRecommendations == null) return;
        String text = recommendations == null ? "" : recommendations.trim();
        txtRecommendations.setText(text.isEmpty() ? "Aucune recommandation disponible." : text);
    }

    private void updateDecision(String decision, String confStr) {
        if (lblDecision != null) {
            lblDecision.setText(mapMlDecision(decision));
        }
        if (lblConfidence != null) {
            lblConfidence.setText(String.format(Locale.ROOT, "%.0f%%", parseConfidence(confStr) * 100.0));
        }
        if (barConfidence != null) {
            barConfidence.setProgress(parseConfidence(confStr));
        }
    }

    private void updateRecommendations(List<?> recommendations) {
        if (txtRecommendations == null) {
            return;
        }
        if (recommendations == null || recommendations.isEmpty()) {
            txtRecommendations.setText("Aucune recommandation disponible.");
            return;
        }
        Object first = recommendations.get(0);
        if (!(first instanceof Map)) {
            txtRecommendations.setText(String.valueOf(first));
            return;
        }
        Map<?, ?> rec = (Map<?, ?>) first;
        StringBuilder sb = new StringBuilder();
        Object summary = rec.get("summary");
        if (summary != null) {
            sb.append(summary).append("\n\n");
        }
        Object actions = rec.get("actions");
        if (actions instanceof List<?>) {
            sb.append("Actions recommandees:\n");
            for (Object a : (List<?>) actions) {
                sb.append(" • ").append(String.valueOf(a)).append("\n");
            }
        } else if (actions != null) {
            sb.append(String.valueOf(actions));
        }
        txtRecommendations.setText(sb.toString().trim());
    }

    private double parseConfidence(String value) {
        try {
            double v = Double.parseDouble(value);
            if (v < 0) v = 0;
            if (v > 1) v = 1;
            return v;
        } catch (Exception ex) {
            return 0.0;
        }
    }

    private String mapMlDecision(String mlDecision) {
        if (mlDecision == null) return "—";
        String v = mlDecision.trim().toLowerCase();
        if (v.contains("approve") || v.contains("accept") || v.contains("ok") || v.contains("accepted")) {
            return "Approuvé";
        }
        if (v.contains("reject") || v.contains("refuse") || v.contains("rejected")) {
            return "Rejeté";
        }
        return "Rejeté";
    }

    private void setProjectName(String name) {
        if (lblProjectName != null) {
            lblProjectName.setText(name == null || name.isBlank() ? "—" : name);
        }
    }

    private void setStatus(String text) {
        if (lblStatus != null) {
            lblStatus.setText(text);
        }
    }

    private void disableMl(boolean disabled) {
        if (btnRunMl != null) {
            btnRunMl.setDisable(disabled);
            btnRunMl.setVisible(!disabled);
            btnRunMl.setManaged(!disabled);
        }
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gestionCarbone.fxml"));
            Parent root = loader.load();
            Scene scene = lblProjectName != null ? lblProjectName.getScene() : null;
            if (scene != null) {
                scene.setRoot(root);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Integer parseEsgLabel(String text) {
        if (text == null) return null;
        String trimmed = text.trim();
        if (trimmed.isEmpty() || trimmed.equals("—")) return null;
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void updateLocalDecisionAndRecommendations(List<EvaluationResult> results) {
        if (results == null || results.isEmpty()) {
            return;
        }
        double avgNote = results.stream().mapToInt(EvaluationResult::getNote).average().orElse(0.0);
        double complianceRate = results.stream().mapToDouble(r -> r.isEstRespecte() ? 1.0 : 0.0).average().orElse(0.0);
        double score = facade.computeScore(results);

        String decision = (score >= 6.5 && complianceRate >= 0.6) ? "APPROVE" : "REJECT";
        updateDecision(decision, String.valueOf(Math.max(0.0, Math.min(1.0, complianceRate))));

        Models.AiSuggestion suggestion = facade.suggest(selectedProjet, results);
        if (txtRecommendations != null) {
            StringBuilder sb = new StringBuilder();
            if (suggestion.getConclusion() != null && !suggestion.getConclusion().isBlank()) {
                sb.append(suggestion.getConclusion()).append("\n\n");
            }
            if (suggestion.getRecommendations() != null && !suggestion.getRecommendations().isEmpty()) {
                sb.append("Recommandations:\n");
                for (String r : suggestion.getRecommendations()) {
                    sb.append(" • ").append(r).append("\n");
                }
            }
            if (sb.length() == 0) {
                sb.append("Aucune recommandation disponible.");
            }
            txtRecommendations.setText(sb.toString().trim());
        }
        System.out.println("[ML] Recommendations API and ML model successful");
    }
}

