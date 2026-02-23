package Services;

import Models.AiSuggestion;
import Models.EvaluationResult;
import Models.PolicyOutcome;
import Models.Projet;
import Models.ScoreExplanation;

import java.util.*;
import java.util.stream.Collectors;

/**
 * IA heuristique + NLP/ML:
 * - Suggère APPROVE/REVIEW (jamais REJECT), confiance calibrée vs seuils
 * - NLP+ML: classification E/S/G via NlpMlService (Weka TF-IDF + NaiveBayesMultinomial)
 * - Si REVIEW: conclusion et recommandations concrètes
 */
public class AiSuggestionService {

    private static final double WARN_THRESHOLD = 6.0;
    private static final double APPROVE_THRESHOLD = 7.5;

    private final ScoringService scoringService;
    private final PolicyEngine policyEngine;
    private final NlpMlService nlp = NlpMlService.getInstance();

    // DB access to enrich criterion context (nom + description + poids)
    private final CritereImpactService critereService = new CritereImpactService();
    private volatile Map<Integer, Models.CritereReference> refIndex;

    public AiSuggestionService(ScoringService scoringService, PolicyEngine policyEngine) {
        this.scoringService = scoringService;
        this.policyEngine = policyEngine;
    }

    private Map<Integer, Models.CritereReference> getRefIndex() {
        Map<Integer, Models.CritereReference> local = refIndex;
        if (local == null) {
            synchronized (this) {
                if (refIndex == null) {
                    List<Models.CritereReference> refs = critereService.afficherReferences();
                    Map<Integer, Models.CritereReference> map = new HashMap<>();
                    if (refs != null) {
                        for (Models.CritereReference r : refs) {
                            map.put(r.getIdCritere(), r);
                        }
                    }
                    refIndex = map;
                }
                local = refIndex;
            }
        }
        return local;
    }

    private String buildContextText(Models.EvaluationResult r) {
        Models.CritereReference ref = getRefIndex().get(r.getIdCritere());
        String nom = r.getNomCritere() != null ? r.getNomCritere() : (ref != null ? ref.getNomCritere() : ("Critère #" + r.getIdCritere()));
        String desc = ref != null && ref.getDescription() != null ? ref.getDescription() : "";
        String comm = r.getCommentaireExpert() != null ? r.getCommentaireExpert() : "";
        return (nom + " " + desc + " " + comm).trim();
    }

    // Detect fine-grained subtopics from text, to propose targeted solutions
    private String detectSubtopic(String text, String pillar) {
        String t = (text == null ? "" : text.toLowerCase(java.util.Locale.ROOT));
        if ("E".equals(pillar)) {
            if (containsAny(t, "co2", "émission", "emission", "carbone", "gaz à effet", "gaz a effet")) return "CO2/Énergie";
            if (containsAny(t, "energie", "énergie", "consommation", "kwh")) return "CO2/Énergie";
            if (containsAny(t, "eau", "consommation d'eau", "fuite")) return "Eau";
            if (containsAny(t, "déchet", "dechet", "recycl", "tri", "valoris", "circular")) return "Déchets";
            if (containsAny(t, "pollu", "nop", "no2", "so2", "particul", "vocs", "air")) return "Pollution air";
            if (containsAny(t, "biodivers", "habitat", "ecosy", "écosy")) return "Biodiversité";
            return "CO2/Énergie";
        } else if ("S".equals(pillar)) {
            if (containsAny(t, "sécur", "accident", "epi", "risque", "incident")) return "Sécurité";
            if (containsAny(t, "santé", "sante", "ergonomie", "fatigue", "stress")) return "Santé";
            if (containsAny(t, "formation", "compétence", "competence", "sensibil")) return "Formation";
            if (containsAny(t, "communaut", "local", "sociét", "societ")) return "Communauté";
            if (containsAny(t, "diversit", "inclusion", "égalité", "egalite")) return "Diversité";
            return "Sécurité";
        } else { // G
            if (containsAny(t, "conform", "compliance", "réglement", "reglement")) return "Conformité";
            if (containsAny(t, "audit", "contrôle interne", "controle interne", "revue")) return "Audit interne";
            if (containsAny(t, "transpar", "reporting", "publication")) return "Transparence";
            if (containsAny(t, "corruption", "éthique", "ethique", "conflit d'intérêt", "interet")) return "Éthique";
            if (containsAny(t, "risque", "cartographie", "contrôle", "controle")) return "Gestion des risques";
            return "Conformité";
        }
    }

    private boolean containsAny(String text, String... keys) {
        for (String k : keys) if (text.contains(k)) return true;
        return false;
    }

    private String specificSolution(String pillar, String subtopic, int note, boolean respecte, int poids, String name) {
        // Severity tiers
        boolean severe = !respecte || note <= 4;
        boolean medium = respecte && note >= 5 && note < 8;
        boolean excellent = respecte && note >= 8;

        String priority = poids >= 8 ? " (prioritaire)" : (poids >= 5 ? " (à planifier)" : "");
        if (excellent) {
            return name + ": OK – Maintenir et documenter les bonnes pratiques" + priority + ".";
        }
        if ("E".equals(pillar)) {
            if ("CO2/Énergie".equals(subtopic)) {
                if (severe) return name + ": Plan CO₂/énergie" + priority + " – audit énergétique, variateurs de vitesse, récupération de chaleur, objectifs -15%/12 mois.";
                if (medium) return name + ": Optimiser la performance énergétique – étalonnage équipements, éclairage LED, suivi kWh/UF (-5%/6 mois).";
            } else if ("Eau".equals(subtopic)) {
                if (severe) return name + ": Réduction eau" + priority + " – détection fuites, boucles de recirculation, objectifs -20%/9 mois.";
                if (medium) return name + ": Optimiser eau – compteurs sous-boucles, campagnes de sensibilisation (-8%/6 mois).";
            } else if ("Déchets".equals(subtopic)) {
                if (severe) return name + ": Programme déchets" + priority + " – tri à la source, filières de valorisation, réduction 25% scrap/12 mois.";
                if (medium) return name + ": Améliorer tri/valorisation – écobins, contrats avec repreneurs (+15% recyclage/6 mois).";
            } else if ("Pollution air".equals(subtopic)) {
                if (severe) return name + ": Qualité air" + priority + " – captation à la source, filtres HEPA/charbon, mesures NOx/VOC mensuelles.";
                if (medium) return name + ": Optimiser ventilation/entretien filtres, plan d’étanchéité conduits.";
            } else if ("Biodiversité".equals(subtopic)) {
                if (severe) return name + ": Plan biodiversité" + priority + " – zones refuges, calendrier travaux hors nidification, suivi indicateurs.";
                if (medium) return name + ": Améliorer intégration paysagère et gestion différenciée des espaces verts.";
            }
        } else if ("S".equals(pillar)) {
            if ("Sécurité".equals(subtopic)) {
                if (severe) return name + ": Sécurité" + priority + " – plan d'actions AT/MP: formation ciblée, port EPI systématique, analyse 5P des incidents, audit 5S hebdo; objectif TRIR -30% en 6 mois.";
                if (medium) return name + ": Renforcer causeries sécurité hebdo, vérif EPI, inspection 5S mensuelle des zones à risque.";
            } else if ("Santé".equals(subtopic)) {
                if (severe) return name + ": Santé" + priority + " – programme TMS (ergonomie postes, rotation tâches), évaluation RPS, surveillance biométrique adaptée; objectif -25% TMS/12 mois.";
                if (medium) return name + ": Améliorer ergonomie (ajustement hauteurs, micro-pauses), campagne hygiène/étirements; suivi bien-être trimestriel.";
            } else if ("Formation".equals(subtopic)) {
                if (severe) return name + ": Compétences" + priority + " – plan de formation réglementaire et habilitations à jour (<30j), traçabilité numérique, taux de complétion 100%.";
                if (medium) return name + ": E-learning ciblé et recyclage annuel des habilitations, KPI de complétion > 95%.";
            } else if ("Communauté".equals(subtopic)) {
                if (severe) return name + ": Communauté" + priority + " – dialogue parties prenantes, charte impacts locaux, plan d’atténuation mesuré (bruit, trafic).";
                if (medium) return name + ": Programme mécénat/volontariat avec indicateurs d’impact et retour annuel.";
            } else if ("Diversité".equals(subtopic)) {
                if (severe) return name + ": Diversité" + priority + " – politique anti-discrimination, objectifs de mixité, canal d’alerte; publier indicateurs égalité.";
                if (medium) return name + ": Sensibilisation biais inconscients, objectifs de diversité par métier, reporting trimestriel.";
            }
        } else { // G
            if ("Conformité".equals(subtopic)) {
                if (severe) return name + ": Conformité" + priority + " – cartographie obligations, mise en conformité prioritaire, registre contrôles.";
                if (medium) return name + ": Veille réglementaire outillée, checklists conformité mensuelles.";
            } else if ("Audit interne".equals(subtopic)) {
                if (severe) return name + ": Audit" + priority + " – plan d’audit interne/tiers, remédiation sous 90j, suivi CAPA.";
                if (medium) return name + ": Programme d’auto-inspection trimestrielle et tableau de suivi.";
            } else if ("Transparence".equals(subtopic)) {
                if (severe) return name + ": Transparence" + priority + " – politique de publication, contrôle qualité des données ESG.";
                if (medium) return name + ": Améliorer reporting KPI/ESG et revue Direction semestrielle.";
            } else if ("Éthique".equals(subtopic)) {
                if (severe) return name + ": Éthique" + priority + " – code de conduite, canal d’alerte, formation anti-corruption (100%).";
                if (medium) return name + ": Sensibilisation éthique annuelle et déclaration conflits d’intérêts.";
            } else if ("Gestion des risques".equals(subtopic)) {
                if (severe) return name + ": Risques" + priority + " – mise à jour cartographie, plans de contingence, tests de crise.";
                if (medium) return name + ": Registre des risques avec propriétaires et revues trimestrielles.";
            }
        }
        // Fallback generic but still contextual
        return name + ": Améliorer progressivement avec plan d’actions mesuré" + priority + ".";
    }

    public AiSuggestion suggestDecision(Projet projet, List<EvaluationResult> criteres) {
        double score = scoringService.calculateScore(criteres);
        PolicyOutcome outcome = policyEngine.evaluate(projet, criteres, score);

        String suggestion;
        double confidence;

        if (score >= APPROVE_THRESHOLD && outcome.getStatus() == PolicyOutcome.Status.OK) {
            suggestion = "APPROVE";
            double margin = Math.min(2.5, score - APPROVE_THRESHOLD);
            confidence = Math.min(0.95, 0.7 + (margin / 10.0));
        } else if (score < WARN_THRESHOLD || outcome.getStatus() == PolicyOutcome.Status.BLOCK) {
            suggestion = "REVIEW";
            double gap = Math.min(3.0, WARN_THRESHOLD - Math.min(score, WARN_THRESHOLD));
            confidence = 0.35 + (0.05 * (3.0 - gap));
        } else {
            suggestion = "REVIEW";
            double span = Math.max(0.0, Math.min(1.5, score - WARN_THRESHOLD));
            confidence = 0.5 + (span / 15.0);
        }

        AiSuggestion s = new AiSuggestion(suggestion, confidence, score);
        if (outcome.getMessages() != null) {
            outcome.getMessages().forEach(s::addWarning);
        }

        // Top 3 facteurs de contribution (positifs)
        List<ScoreExplanation> exps = scoringService.explainScore(criteres);
        List<ScoreExplanation> top = exps.stream()
                .sorted(Comparator.comparingDouble(ScoreExplanation::getContribution).reversed())
                .limit(3)
                .collect(Collectors.toList());
        for (ScoreExplanation e : top) {
            s.addTopFactor(e.getNomCritere() + " +" + String.format(java.util.Locale.ROOT, "%.2f", e.getContribution()) + " pts");
        }

        // Recos par critère (pilotées par classif ML)
        Map<String, Integer> topics = extractTopics(criteres);
        List<String> recs = buildRecommendationsWithMl(criteres);
        for (String r : recs) s.addRecommendation(r);

        // Conclusion
        if ("REVIEW".equals(suggestion)) {
            StringBuilder concl = new StringBuilder("Revue requise. Priorités d'amélioration: ");
            List<String> prios = topics.entrySet().stream()
                    .sorted((a,b) -> Integer.compare(b.getValue(), a.getValue()))
                    .map(Map.Entry::getKey)
                    .limit(3)
                    .collect(Collectors.toList());
            if (prios.isEmpty()) {
                concl.append("compléter la conformité et renforcer les critères à faible note.");
            } else {
                concl.append(String.join(", ", prios)).append(".");
            }
            s.setConclusion(concl.toString());
        } else {
            s.setConclusion("Dossier favorable. Maintenir les bonnes pratiques identifiées.");
        }

        return s;
    }

    /**
     * Recommandations par critère, formatées "NomCritere: Recommandation"
     */
    public List<String> criterionRecommendations(List<EvaluationResult> criteres) {
        List<String> res = new ArrayList<>();
        Map<Integer, Models.CritereReference> idx = getRefIndex();

        for (EvaluationResult r : criteres) {
            Models.CritereReference ref = idx.get(r.getIdCritere());
            int poids = ref != null ? Math.max(1, ref.getPoids()) : 1;
            String name = r.getNomCritere() != null ? r.getNomCritere() : (ref != null ? ref.getNomCritere() : ("Critère #" + r.getIdCritere()));

            String context = buildContextText(r);
            String pillar = nlp.classifyPillar(context);
            String subtopic = detectSubtopic(context, pillar);

            // Build severity-aware, solution-oriented recommendation
            String line = specificSolution(pillar, subtopic, r.getNote(), r.isEstRespecte(), poids, name);
            res.add(line);
        }

        // Condense output to 5 lines max in UI
        return res.stream().limit(5).collect(Collectors.toList());
    }

    // NLP mots-clés pour topics globaux (statistiques simples)
    private Map<String, Integer> extractTopics(List<EvaluationResult> criteres) {
        Map<String, Integer> counts = new HashMap<>();
        for (EvaluationResult r : criteres) {
            String text = (safe(r.getNomCritere()) + " " + safe(r.getCommentaireExpert())).toLowerCase(Locale.ROOT);
            if (containsAny(text, "co2", "carbone", "émission", "emission", "energie", "énergie", "eau", "déchet", "dechet", "pollu", "climat", "biodivers")) {
                inc(counts, "Énergie/Émissions");
            }
            if (containsAny(text, "déchet", "dechet", "recycl", "tri", "circular")) {
                inc(counts, "Déchets/Circularité");
            }
            if (containsAny(text, "sécur", "santé", "sante", "social", "communaut", "employ", "accident", "formation")) {
                inc(counts, "Social/Santé-Sécurité");
            }
            if (containsAny(text, "gouvernance", "audit", "compliance", "conform", "transpar", "ethique", "éthique", "corruption", "risque")) {
                inc(counts, "Gouvernance/Conformité");
            }
        }
        return counts;
    }

    private List<String> buildRecommendationsWithMl(List<EvaluationResult> criteres) {
        List<String> recs = new ArrayList<>();
        Map<Integer, Models.CritereReference> idx = getRefIndex();

        for (EvaluationResult r : criteres) {
            Models.CritereReference ref = idx.get(r.getIdCritere());
            int poids = ref != null ? Math.max(1, ref.getPoids()) : 1;
            String name = r.getNomCritere() != null ? r.getNomCritere() : (ref != null ? ref.getNomCritere() : ("Critère #" + r.getIdCritere()));
            String context = buildContextText(r);
            String pillar = nlp.classifyPillar(context);
            String subtopic = detectSubtopic(context, pillar);
            recs.add(specificSolution(pillar, subtopic, r.getNote(), r.isEstRespecte(), poids, name));
        }
        // de-duplicate while preserving order
        LinkedHashSet<String> uniq = new LinkedHashSet<>(recs);
        return new ArrayList<>(uniq);
    }

    private void inc(Map<String,Integer> map, String k) {
        map.put(k, map.getOrDefault(k, 0) + 1);
    }

    private String safe(String s) { return s == null ? "" : s; }
}
