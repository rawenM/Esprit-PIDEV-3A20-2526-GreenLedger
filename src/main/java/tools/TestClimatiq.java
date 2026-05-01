package tools;

import Models.dto.CarbonMetricResult;
import Models.dto.ProjectData;
import Services.ClimatiqApiClient;
import Utils.EnvLoader;

/**
 * Quick smoke test for Climatiq API integration.
 */
public class TestClimatiq {
    public static void main(String[] args) {
        EnvLoader.load();

        String key = EnvLoader.get("CLIMATIQ_API_KEY", "");
        System.out.println("CLIMATIQ_API_KEY present: " + (!key.isBlank()));
        System.out.println("CLIMATIQ_BASE_URL: " + EnvLoader.get("CLIMATIQ_BASE_URL", "(not set)"));

        ClimatiqApiClient client = new ClimatiqApiClient();

        // Test project: solar farm in Tunisia
        ProjectData project = new ProjectData();
        project.setConsommationEnergie(5000.0);   // 5000 kWh (treated as MWh → 5,000,000 kWh)
        project.setUniteEnergie("kWh");
        project.setDistanceTransport(200.0);       // 200 km
        project.setQuantiteMateriau(10.0);         // 10 tonnes steel
        project.setDechetsGeneres(2.0);            // 2 tonnes waste
        project.setEmissionsEstimees(15.5);        // declared 15.5 tCO2e
        project.setSecteur("Energie");
        project.setTypeProjet("Solaire");
        project.setLocalisation("Tunis");
        project.setTypeTransport("camion");
        project.setTypeMateriau("acier");
        project.setSourceEmissions("scope_1");

        System.out.println("\n=== Calling Climatiq API ===");
        CarbonMetricResult result = client.estimateProjectEmissions(project);

        System.out.println("Method:  " + result.getMethod());
        System.out.printf("Scope 1: %.3f tCO2e%n", result.getScope1Tco2() != null ? result.getScope1Tco2() : 0.0);
        System.out.printf("Scope 2: %.3f tCO2e%n", result.getScope2Tco2() != null ? result.getScope2Tco2() : 0.0);
        System.out.printf("Scope 3: %.3f tCO2e%n", result.getScope3Tco2() != null ? result.getScope3Tco2() : 0.0);
        System.out.printf("Total:   %.3f tCO2e%n", result.getTotalTco2() != null ? result.getTotalTco2() : 0.0);
        System.out.printf("Quality: %.0f%%%n", result.getDataQualityScore() != null ? result.getDataQualityScore() : 0.0);

        if (!result.getProviderErrors().isEmpty()) {
            System.out.println("\nErrors:");
            result.getProviderErrors().forEach(e -> System.out.println("  - " + e));
        }

        boolean usedApi = result.getMethod() != null && result.getMethod().contains("CLIMATIQ_API");
        System.out.println("\n" + (usedApi ? "✅ Climatiq API used successfully" : "⚠️  Fallback heuristics used"));
    }
}
