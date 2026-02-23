package Services.impl;

import Models.EvaluationResult;
import Models.PolicyOutcome;
import Models.Projet;
import Services.PolicyEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * Règles simples (échelle 0..10):
 * - Critères non respectés: génèrent des WARN (avertissements), sans blocage automatique.
 * - Seuils score: <6.0 => BLOCK, 6.0-7.5 => WARN, >=7.5 => OK.
 */
public class SimplePolicyEngine implements PolicyEngine {

    private final double warnThreshold;
    private final double approveThreshold;

    public SimplePolicyEngine() {
        this.warnThreshold = 6.0;
        this.approveThreshold = 7.5;
    }

    public SimplePolicyEngine(double warnThreshold, double approveThreshold) {
        this.warnThreshold = warnThreshold;
        this.approveThreshold = approveThreshold;
    }

    @Override
    public PolicyOutcome evaluate(Projet projet, List<EvaluationResult> criteres, double score) {
        List<String> messages = new ArrayList<>();

        int nonRespectCount = 0;
        if (criteres != null) {
            for (EvaluationResult r : criteres) {
                if (!r.isEstRespecte()) {
                    nonRespectCount++;
                    messages.add("Critère non respecté: " + (r.getNomCritere() != null ? r.getNomCritere() : ("#" + r.getIdCritere())));
                }
            }
        }

        if (score < warnThreshold) {
            messages.add("Score en dessous du seuil minimal (" + warnThreshold + ")");
            return new PolicyOutcome(PolicyOutcome.Status.BLOCK, messages);
        } else if (score < approveThreshold || nonRespectCount > 0) {
            if (nonRespectCount > 0) {
                messages.add("Conformité partielle détectée (" + nonRespectCount + " critère(s) non respecté[s]).");
            } else {
                messages.add("Score intermédiaire (" + score + "), examen recommandé.");
            }
            return new PolicyOutcome(PolicyOutcome.Status.WARN, messages);
        } else {
            messages.add("Score satisfaisant (" + score + ").");
            return new PolicyOutcome(PolicyOutcome.Status.OK, messages);
        }
    }
}
