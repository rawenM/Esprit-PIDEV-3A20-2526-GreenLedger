package Services;

import opennlp.tools.doccat.DoccatFactory;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.doccat.DocumentSample;
import opennlp.tools.ml.naivebayes.NaiveBayesTrainer;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.TrainingParameters;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * NlpMlService (OpenNLP):
 * - Entraîne à la volée un classifieur de documents (Naive Bayes) pour E/S/G
 * - Catégorise du texte "E", "S" ou "G" en pur Java (sans Weka/netlib)
 */
public class NlpMlService {

    private static final NlpMlService INSTANCE = new NlpMlService();
    private volatile boolean initialized = false;
    private volatile boolean initStarted = false;
    private DocumentCategorizerME categorizer;

    static {
        // Backup: reduce SLF4J Simple logs if properties file not picked up early enough
        if (System.getProperty("org.slf4j.simpleLogger.defaultLogLevel") == null) {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
        }
        System.setProperty("org.slf4j.simpleLogger.log.opennlp.tools", "warn");
        System.setProperty("org.slf4j.simpleLogger.log.opennlp.tools.ml", "warn");
    }

    private NlpMlService() {}

    public static NlpMlService getInstance() {
        return INSTANCE;
    }

    public String[] tokenize(String text) {
        if (text == null || text.isBlank()) return new String[0];
        return text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+");
    }

    public void initializeAsync() {
        if (initialized || initStarted) return;
        initStarted = true;
        Thread t = new Thread(() -> {
            try {
                initializeIfNeeded();
            } catch (IOException ignored) {
            }
        }, "nlp-init");
        t.setDaemon(true);
        t.start();
    }

    public synchronized void initializeIfNeeded() throws IOException {
        if (initialized) return;

        List<DocumentSample> samples = new ArrayList<>();
        // E
        samples.add(new DocumentSample("E", tokenize("Réduction des émissions de CO2 et efficacité énergétique des procédés")));
        samples.add(new DocumentSample("E", tokenize("Gestion de l'eau, recyclage des déchets et pollution")));
        samples.add(new DocumentSample("E", tokenize("Empreinte carbone, énergie renouvelable, climat")));
        // S
        samples.add(new DocumentSample("S", tokenize("Sécurité des employés, santé au travail et formation")));
        samples.add(new DocumentSample("S", tokenize("Impacts sur la communauté locale et inclusion")));
        samples.add(new DocumentSample("S", tokenize("Conditions de travail, diversité, droits humains")));
        // G
        samples.add(new DocumentSample("G", tokenize("Gouvernance, conformité, transparence et audit interne")));
        samples.add(new DocumentSample("G", tokenize("Lutte contre la corruption, éthique et gestion des risques")));
        samples.add(new DocumentSample("G", tokenize("Conseil d'administration, comités et politiques internes")));

        ObjectStream<DocumentSample> stream = new ObjectStream<>() {
            private int idx = 0;
            @Override public DocumentSample read() { return idx < samples.size() ? samples.get(idx++) : null; }
            @Override public void reset() { idx = 0; }
            @Override public void close() {}
        };

        TrainingParameters params = new TrainingParameters();
        params.put(TrainingParameters.CUTOFF_PARAM, "1");
        params.put(TrainingParameters.ITERATIONS_PARAM, "100");
        params.put(TrainingParameters.ALGORITHM_PARAM, NaiveBayesTrainer.NAIVE_BAYES_VALUE);

        DoccatModel model = DocumentCategorizerME.train("fr", stream, params, new DoccatFactory());
        this.categorizer = new DocumentCategorizerME(model);

        // Mark initialized; avoid any recursive calls during init
        initialized = true;
    }

    /**
     * Classifie du texte en pilier ESG: "E", "S" ou "G"
     */
    public String classifyPillar(String text) {
        try {
            initializeIfNeeded();
            double[] outcomes = categorizer.categorize(tokenize(text == null ? "" : text));
            return categorizer.getBestCategory(outcomes);
        } catch (Exception e) {
            String t = (text == null ? "" : text.toLowerCase(Locale.ROOT));
            if (t.contains("co2") || t.contains("énergie") || t.contains("energie") || t.contains("déchet") || t.contains("dechet") || t.contains("climat")) {
                return "E";
            }
            if (t.contains("sécur") || t.contains("santé") || t.contains("sante") || t.contains("social") || t.contains("employ")) {
                return "S";
            }
            return "G";
        }
    }

    /**
     * Retourne les scores par catégorie, ex: {E=0.82, S=0.12, G=0.06}
     */
    public Map<String, Double> categorizeWithScores(String text) {
        try {
            initializeIfNeeded();
            double[] outcomes = categorizer.categorize(tokenize(text == null ? "" : text));
            Map<String, Double> scores = new LinkedHashMap<>();
            for (int i = 0; i < outcomes.length; i++) {
                scores.put(categorizer.getCategory(i), outcomes[i]);
            }
            // sort desc by prob
            return scores.entrySet().stream()
                    .sorted((a,b) -> Double.compare(b.getValue(), a.getValue()))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey, Map.Entry::getValue,
                            (a,b)->a, LinkedHashMap::new
                    ));
        } catch (Exception e) {
            Map<String, Double> fallback = new LinkedHashMap<>();
            fallback.put("E", 0.34); fallback.put("S", 0.33); fallback.put("G", 0.33);
            return fallback;
        }
    }

    /**
     * Résumé lisible: "best=E, E=0.82 S=0.12 G=0.06"
     */
    public String debugSummary(String text) {
        Map<String, Double> scores = categorizeWithScores(text);
        String best = scores.entrySet().iterator().next().getKey();
        String details = scores.entrySet().stream()
                .map(e -> e.getKey() + "=" + String.format(Locale.ROOT, "%.2f", e.getValue()))
                .collect(Collectors.joining(" "));
        return "best=" + best + ", " + details;
    }
}
