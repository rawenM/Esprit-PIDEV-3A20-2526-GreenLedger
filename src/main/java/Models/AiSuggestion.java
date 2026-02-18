package Models;

import java.util.ArrayList;
import java.util.List;

public class AiSuggestion {
    private String suggestionDecision; // APPROVE | REVIEW
    private double confiance; // 0..1
    private List<String> topFactors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private List<String> recommendations = new ArrayList<>();
    private String conclusion;
    private double score;

    public AiSuggestion() {}

    public AiSuggestion(String suggestionDecision, double confiance, double score) {
        this.suggestionDecision = suggestionDecision;
        this.confiance = confiance;
        this.score = score;
    }

    public String getSuggestionDecision() {
        return suggestionDecision;
    }

    public void setSuggestionDecision(String suggestionDecision) {
        this.suggestionDecision = suggestionDecision;
    }

    public double getConfiance() {
        return confiance;
    }

    public void setConfiance(double confiance) {
        this.confiance = confiance;
    }

    public List<String> getTopFactors() {
        return topFactors;
    }

    public void setTopFactors(List<String> topFactors) {
        this.topFactors = topFactors;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public void addTopFactor(String factor) {
        if (factor != null && !factor.isEmpty()) {
            this.topFactors.add(factor);
        }
    }

    public void addWarning(String warning) {
        if (warning != null && !warning.isEmpty()) {
            this.warnings.add(warning);
        }
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }

    public void addRecommendation(String rec) {
        if (rec != null && !rec.isEmpty()) {
            this.recommendations.add(rec);
        }
    }

    public String getConclusion() {
        return conclusion;
    }

    public void setConclusion(String conclusion) {
        this.conclusion = conclusion;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}
