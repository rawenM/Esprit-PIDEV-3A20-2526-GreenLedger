package Services;

import Models.CritereReference;
import Models.EvaluationResult;
import Models.ScoreExplanation;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Moteur de score pondéré basé sur les critères de référence.
 * - Moyenne pondérée: sum(note_eff * poids) / sum(poids)
 * - Pénalisation de conformité: si un critère n'est pas respecté, note_eff = note * 0.6 (bornée 0..10)
 * - explainScore: contributions par critère basées sur la note effective
 */
public class ScoringService {

    private static final double NON_COMPLIANCE_FACTOR = 0.6; // pénalise les critères non respectés
    private final CritereImpactService critereImpactService;
    private volatile Map<Integer, CritereReference> referenceIndex;

    public ScoringService() {
        this.critereImpactService = new CritereImpactService();
    }

    public synchronized void refreshReferencesCache() {
        List<CritereReference> refs = critereImpactService.afficherReferences();
        Map<Integer, CritereReference> map = new HashMap<>();
        for (CritereReference r : refs) {
            map.put(r.getIdCritere(), r);
        }
        this.referenceIndex = map;
    }

    private Map<Integer, CritereReference> getReferenceIndex() {
        if (referenceIndex == null) {
            refreshReferencesCache();
        }
        return referenceIndex;
    }

    private int getPoidsForCritere(int idCritere) {
        CritereReference ref = getReferenceIndex().get(idCritere);
        return (ref != null ? Math.max(1, ref.getPoids()) : 1);
    }

    private String getNomForCritere(int idCritere) {
        CritereReference ref = getReferenceIndex().get(idCritere);
        return (ref != null ? ref.getNomCritere() : "Critère #" + idCritere);
    }

    private double effectiveNote(EvaluationResult r) {
        double base = r.getNote();
        if (!r.isEstRespecte()) {
            base = base * NON_COMPLIANCE_FACTOR;
        }
        if (base < 0) base = 0;
        if (base > 10) base = 10;
        return base;
    }

    public double calculateScore(List<EvaluationResult> criteres) {
        if (criteres == null || criteres.isEmpty()) return 0.0;
        double num = 0.0;
        int den = 0;
        for (EvaluationResult r : criteres) {
            int poids = getPoidsForCritere(r.getIdCritere());
            num += effectiveNote(r) * poids;
            den += poids;
        }
        if (den == 0) return 0.0;
        return num / den; // 0..10
    }

    public List<ScoreExplanation> explainScore(List<EvaluationResult> criteres) {
        if (criteres == null) return Collections.emptyList();
        return criteres.stream()
                .map(r -> {
                    int poids = getPoidsForCritere(r.getIdCritere());
                    double contribution = effectiveNote(r) * poids;
                    return new ScoreExplanation(
                            r.getIdCritere(),
                            getNomForCritere(r.getIdCritere()),
                            poids,
                            r.getNote(), // note saisie, contribution basée sur note effective
                            contribution
                    );
                })
                .sorted(Comparator.comparingDouble(ScoreExplanation::getContribution).reversed())
                .collect(Collectors.toList());
    }
}
