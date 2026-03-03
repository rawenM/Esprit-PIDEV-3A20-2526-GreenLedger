package Services;

import Models.AiSuggestion;
import Models.EvaluationResult;
import Models.PolicyOutcome;
import Models.Projet;
import Models.ScoreExplanation;
import Services.impl.SimplePolicyEngine;

import java.util.List;

public class AdvancedEvaluationFacade {

    private final ScoringService scoringService;
    private final PolicyEngine policyEngine;
    private final AiSuggestionService aiSuggestionService;

    public AdvancedEvaluationFacade() {
        this.scoringService = new ScoringService();
        this.policyEngine = new SimplePolicyEngine();
        this.aiSuggestionService = new AiSuggestionService(scoringService, policyEngine);
        // Pre-warm NLP/ML in background so UI thread isn't blocked and logs appear once
        NlpMlService.getInstance().initializeAsync();
        // Start lightweight API server so /api/ai/doccat is reachable
        ApiServerBootstrap.startIfNeeded();
    }

    public double computeScore(List<EvaluationResult> criteres) {
        return scoringService.calculateScore(criteres);
    }

    public List<ScoreExplanation> explainScore(List<EvaluationResult> criteres) {
        return scoringService.explainScore(criteres);
    }

    public PolicyOutcome applyPolicies(Projet projet, List<EvaluationResult> criteres, double score) {
        return policyEngine.evaluate(projet, criteres, score);
    }

    public AiSuggestion suggest(Projet projet, List<EvaluationResult> criteres) {
        return aiSuggestionService.suggestDecision(projet, criteres);
    }

    public List<String> criterionRecommendations(List<EvaluationResult> criteres) {
        return aiSuggestionService.criterionRecommendations(criteres);
    }
}
