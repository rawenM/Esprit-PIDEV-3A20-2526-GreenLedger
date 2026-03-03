package Models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FraudDetectionResult {
    
    private Long id;
    private Long userId;
    private double riskScore;
    private RiskLevel riskLevel;
    private boolean isFraudulent;
    private List<FraudIndicator> indicators;
    private String recommendation;
    private LocalDateTime analyzedAt;
    private String analysisDetails;
    
    public enum RiskLevel {
        FAIBLE("Faible", 0, 25),
        MOYEN("Moyen", 25, 50),
        ELEVE("Eleve", 50, 75),
        CRITIQUE("Critique", 75, 100);
        
        private final String label;
        private final double minScore;
        private final double maxScore;
        
        RiskLevel(String label, double minScore, double maxScore) {
            this.label = label;
            this.minScore = minScore;
            this.maxScore = maxScore;
        }
        
        public String getLabel() {
            return label;
        }
        
        public static RiskLevel fromScore(double score) {
            if (score < 25) return FAIBLE;
            if (score < 50) return MOYEN;
            if (score < 75) return ELEVE;
            return CRITIQUE;
        }
    }
    
    public static class FraudIndicator {
        private String type;
        private String description;
        private double weight;
        private boolean detected;
        
        public FraudIndicator(String type, String description, double weight, boolean detected) {
            this.type = type;
            this.description = description;
            this.weight = weight;
            this.detected = detected;
        }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public double getWeight() { return weight; }
        public void setWeight(double weight) { this.weight = weight; }
        
        public boolean isDetected() { return detected; }
        public void setDetected(boolean detected) { this.detected = detected; }
    }
    
    public FraudDetectionResult() {
        this.indicators = new ArrayList<>();
        this.analyzedAt = LocalDateTime.now();
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public double getRiskScore() { return riskScore; }
    public void setRiskScore(double riskScore) { 
        this.riskScore = riskScore;
        this.riskLevel = RiskLevel.fromScore(riskScore);
    }
    
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    
    public boolean isFraudulent() { return isFraudulent; }
    public void setFraudulent(boolean fraudulent) { isFraudulent = fraudulent; }
    
    public List<FraudIndicator> getIndicators() { return indicators; }
    public void setIndicators(List<FraudIndicator> indicators) { this.indicators = indicators; }
    
    public void addIndicator(FraudIndicator indicator) {
        this.indicators.add(indicator);
    }
    
    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    
    public LocalDateTime getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; }
    
    public String getAnalysisDetails() { return analysisDetails; }
    public void setAnalysisDetails(String analysisDetails) { this.analysisDetails = analysisDetails; }
    
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Score de risque: ").append(String.format("%.1f", riskScore)).append("/100\n");
        sb.append("Niveau de risque: ").append(riskLevel.getLabel()).append("\n");
        sb.append("Recommandation: ").append(recommendation).append("\n");
        sb.append("Indicateurs detectes: ").append(indicators.stream().filter(FraudIndicator::isDetected).count());
        return sb.toString();
    }
}
