package Services;

import Models.dto.ProjectData;

/**
 * Baseline Emission Service — pure math, no API.
 *
 * Calculates what emissions WOULD have been without the project
 * (used to compute avoided_tco2 = baseline - actual).
 *
 * Baseline factors are slightly higher than actual factors
 * to represent the counterfactual scenario.
 */
public class BaselineEmissionService {

    // Baseline factors (tCO2e per unit) — higher than actual
    private static final double BASELINE_ENERGY    = 0.0006;   // tCO2e/kWh
    private static final double BASELINE_TRANSPORT = 0.00012;  // tCO2e/km
    private static final double BASELINE_MATERIAL  = 0.0015;   // tCO2e/tonne (embedded)
    private static final double BASELINE_WASTE     = 0.0009;   // tCO2e/tonne

    /**
     * Calculate baseline tCO2e for a project.
     */
    public double calculateBaseline(ProjectData project) {
        double energy    = pos(project.getConsommationEnergie());
        double transport = pos(project.getDistanceTransport());
        double material  = pos(project.getQuantiteMateriau());
        double waste     = pos(project.getDechetsGeneres());

        energy = toEnergyKwh(energy, project.getUniteEnergie());

        return r3((energy    * BASELINE_ENERGY)
                + (transport * BASELINE_TRANSPORT)
                + (material  * BASELINE_MATERIAL)
                + (waste     * BASELINE_WASTE));
    }

    /**
     * Calculate avoided tCO2e = max(0, baseline - actual).
     */
    public double calculateAvoided(ProjectData project, double actualTco2) {
        double baseline = calculateBaseline(project);
        return Math.max(0.0, r3(baseline - actualTco2));
    }

    private double toEnergyKwh(double value, String unit) {
        if (value <= 0) return 0.0;
        String normalized = unit != null ? unit.toLowerCase().trim() : "kwh";
        return switch (normalized) {
            case "mwh" -> value * 1_000.0;
            case "gwh" -> value * 1_000_000.0;
            case "wh"  -> value / 1_000.0;
            default     -> value;
        };
    }

    private double pos(Double v) { return (v != null && v > 0) ? v : 0.0; }
    private double r3(double v)  { return Math.round(v * 1000.0) / 1000.0; }
}
