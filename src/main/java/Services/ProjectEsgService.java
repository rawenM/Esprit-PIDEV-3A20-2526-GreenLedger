package Services;

import Models.CritereReference;
import Models.Evaluation;
import Models.EvaluationResult;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Service de calcul du score ESG pour un projet:
 * - Récupère la dernière évaluation du projet
 * - Classe les critères par piliers E/S/G (heuristiques sur nom/description)
 * - Pénalise les critères non respectés
 * - Formule: ESG(0..10) = 0.5*E + 0.3*S + 0.2*G -> ESG(0..100) = ESG*10
 */
public class ProjectEsgService {

    private static final double NON_COMPLIANCE_FACTOR = 0.6;
    private final EvaluationService evaluationService = new EvaluationService();
    private final CritereImpactService critereService = new CritereImpactService();

    public ProjectEsgService() {
    }

    public static class EsgBreakdown {
        public final double e; // 0..10
        public final double s; // 0..10
        public final double g; // 0..10
        public final double esg10; // 0..10
        public EsgBreakdown(double e, double s, double g) {
            this.e = e; this.s = s; this.g = g;
            this.esg10 = 0.5 * e + 0.3 * s + 0.2 * g;
        }
    }

    public Integer calculateEsgForProject(int projectId) {
        List<Evaluation> evals = evaluationService.afficherParProjet(projectId);
        if (evals == null || evals.isEmpty()) {
            return null;
        }
        // Dernière évaluation
        evals.sort(Comparator.comparingInt(Evaluation::getIdEvaluation));
        Evaluation last = evals.get(evals.size() - 1);
        List<EvaluationResult> results = critereService.afficherParEvaluation(last.getIdEvaluation());
        if (results == null || results.isEmpty()) {
            return null;
        }
        double esg10 = calculateEsg0to10(results);
        return (int) Math.round(esg10 * 10.0);
    }

    public double calculateEsg0to10(List<EvaluationResult> criteres) {
        return breakdown(criteres).esg10;
    }

    public EsgBreakdown breakdown(List<EvaluationResult> criteres) {
        if (criteres == null || criteres.isEmpty()) {
            return new EsgBreakdown(0.0, 0.0, 0.0);
        }
        PillarAgg E = new PillarAgg();
        PillarAgg S = new PillarAgg();
        PillarAgg G = new PillarAgg();

        for (EvaluationResult r : criteres) {
            Pillar p = classify(r);
            int poids = Math.max(1, getPoids(r.getIdCritere()));
            double noteEff = effectiveNote(r);
            switch (p) {
                case E -> E.add(noteEff, poids);
                case S -> S.add(noteEff, poids);
                case G -> G.add(noteEff, poids);
            }
        }
        return new EsgBreakdown(E.avg(), S.avg(), G.avg());
    }

    private int getPoids(int idCritere) {
        for (CritereReference ref : critereService.afficherReferences()) {
            if (ref.getIdCritere() == idCritere) {
                return Math.max(1, ref.getPoids());
            }
        }
        return 1;
    }

    private double effectiveNote(EvaluationResult r) {
        double base = r.getNote();
        if (!r.isEstRespecte()) {
            base *= NON_COMPLIANCE_FACTOR;
        }
        if (base < 0) base = 0;
        if (base > 10) base = 10;
        return base;
    }

    private Pillar classify(EvaluationResult r) {
        String name = (r.getNomCritere() == null ? "" : r.getNomCritere()).toLowerCase(Locale.ROOT);
        String desc = "";
        // enrichir depuis le référentiel si nécessaire
        for (CritereReference ref : critereService.afficherReferences()) {
            if (ref.getIdCritere() == r.getIdCritere()) {
                if (name.isEmpty()) {
                    name = ref.getNomCritere() == null ? "" : ref.getNomCritere().toLowerCase(Locale.ROOT);
                }
                desc = ref.getDescription() == null ? "" : ref.getDescription().toLowerCase(Locale.ROOT);
                break;
            }
        }
        String text = (name + " " + desc).toLowerCase(Locale.ROOT);

        // 1) Tags explicites prioritaires (#E/#S/#G ou [E]/[S]/[G])
        if (text.contains("#e") || text.contains("[e]")) return Pillar.E;
        if (text.contains("#s") || text.contains("[s]")) return Pillar.S;
        if (text.contains("#g") || text.contains("[g]")) return Pillar.G;

        // 2) Heuristiques mots-clés
        if (containsAny(text, "co2", "carbone", "émission", "emission", "energie", "énergie", "eau", "déchet", "dechet", "biodivers", "pollu", "climat")) {
            return Pillar.E;
        }
        if (containsAny(text, "social", "santé", "sante", "sécur", "secur", "communaut", "employ", "diversit", "formation", "droits", "inclusion", "rh")) {
            return Pillar.S;
        }
        if (containsAny(text, "gouvernance", "compliance", "conform", "audit", "transpar", "ethique", "éthique", "risque", "corruption", "comité", "conseil")) {
            return Pillar.G;
        }

        // 3) Par défaut (si non classable), on bascule sur Environnement
        return Pillar.E;
    }

    private boolean containsAny(String text, String... keys) {
        for (String k : keys) {
            if (text.contains(k)) return true;
        }
        return false;
    }

    private enum Pillar { E, S, G }

    private static class PillarAgg {
        double sum = 0.0;
        int w = 0;
        void add(double note, int poids) { sum += note * poids; w += poids; }
        double avg() { return w == 0 ? 0.0 : sum / w; }
    }
}