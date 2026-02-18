package Services;

import Models.PolicyOutcome;
import Models.Projet;
import Models.EvaluationResult;

import java.util.List;

public interface PolicyEngine {
    /**
     * Évalue les règles métier sur un projet et ses résultats de critères.
     * @param projet contexte projet (peut être null si inconnu)
     * @param criteres résultats par critère
     * @param score score calculé
     * @return PolicyOutcome avec statut et messages
     */
    PolicyOutcome evaluate(Projet projet, List<EvaluationResult> criteres, double score);
}
