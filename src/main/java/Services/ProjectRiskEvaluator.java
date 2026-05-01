package Services;

/**
 * Decision tree for evaluating project investment risk.
 * Mirrors the JavaScript logic from the web app exactly.
 */
public class ProjectRiskEvaluator {

    public enum RiskLevel { LOW_RISK, MEDIUM_RISK, HIGH_RISK }

    /** Evaluate risk from ESG score (0-10) and fraud risk (0-1). */
    public static RiskLevel evaluate(double esgScore, double fraudRisk) {
        // Normalize ESG from 0-10 to 0-100
        double esgNorm = esgScore <= 10 ? esgScore * 10 : esgScore;

        if (esgNorm >= 70 && fraudRisk < 0.40) return RiskLevel.LOW_RISK;
        if (esgNorm >= 70 && fraudRisk < 0.65) return RiskLevel.MEDIUM_RISK;
        if (esgNorm >= 40 && fraudRisk < 0.55) return RiskLevel.MEDIUM_RISK;
        return RiskLevel.HIGH_RISK;
    }

    public static String getVerdict(RiskLevel risk) {
        return switch (risk) {
            case LOW_RISK    -> "Recommandé — Profil ESG solide, risque faible";
            case MEDIUM_RISK -> "À étudier — Potentiel modéré, vérifier les données";
            case HIGH_RISK   -> "Risque élevé — Score ESG faible ou fraude détectée";
        };
    }

    public static String getVerdictIcon(RiskLevel risk) {
        return switch (risk) {
            case LOW_RISK    -> "✅";
            case MEDIUM_RISK -> "⚠️";
            case HIGH_RISK   -> "❌";
        };
    }

    public static String getRulePath(double esgScore) {
        double esgNorm = esgScore <= 10 ? esgScore * 10 : esgScore;
        if (esgNorm >= 70) return "Éligible";
        if (esgNorm >= 40) return "Conditionnel";
        return "Risque élevé";
    }

    public static String getRiskColor(RiskLevel risk) {
        return switch (risk) {
            case LOW_RISK    -> "#10b981";
            case MEDIUM_RISK -> "#f59e0b";
            case HIGH_RISK   -> "#f43f5e";
        };
    }

    public static String getRiskBg(RiskLevel risk) {
        return switch (risk) {
            case LOW_RISK    -> "#D1FAE5";
            case MEDIUM_RISK -> "#FEF3C7";
            case HIGH_RISK   -> "#FEE2E2";
        };
    }

    /** Convenience: evaluate with null-safe inputs. */
    public static RiskLevel evaluateSafe(Integer esgScore, Double fraudRisk) {
        double esg   = esgScore  != null ? esgScore  : 0.0;
        double fraud = fraudRisk != null ? fraudRisk : 0.0;
        return evaluate(esg, fraud);
    }
}
