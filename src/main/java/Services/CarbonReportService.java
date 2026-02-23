package Services;

import dao.CarbonReportDAOImpl;
import dao.ICarbonReportDAO;
import Models.CarbonReport;
import Models.Projet;
import Models.dto.external.AirPollutionResponse;
import Models.dto.external.AirQualityData;
import Models.dto.external.CarbonEstimateResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service pour la gestion des rapports d'émissions carbone
 * Permet à l'Expert Carbone d'évaluer et valider les projets
 */
public class CarbonReportService {

    private final ICarbonReportDAO reportDAO = new CarbonReportDAOImpl();
    
    // External API services for enrichment
    private final ExternalCarbonApiService carbonApiService = new ExternalCarbonApiService();
    private final AirQualityService airQualityService = new AirQualityService();

    // Créer un nouveau rapport (porteur de projet soumet une demande d'évaluation)
    public CarbonReport createReport(Long projectId, String projectName, Long porteurProjetId, String porteurProjetName) {
        CarbonReport report = new CarbonReport(projectId, projectName, porteurProjetId, porteurProjetName);
        return reportDAO.save(report);
    }

    // L'expert carbone évalue et valide le rapport
    public CarbonReport validateReport(Long reportId, Long expertCarboneId, Double emissionsEstimate,
                                      String evaluationDetails, String commentaires) {
        Optional<CarbonReport> reportOpt = reportDAO.findById(reportId);
        if (reportOpt.isEmpty()) {
            System.err.println("[CLEAN] Rapport carbone non trouvé");
            return null;
        }

        CarbonReport report = reportOpt.get();
        report.setExpertCarboneId(expertCarboneId);
        report.setEmissionsEstimate(emissionsEstimate);
        report.setEvaluationDetails(evaluationDetails);
        report.setCommentairesExpert(commentaires);
        report.setStatut(CarbonReport.StatutRapport.VALIDE);
        report.setDateValidation(LocalDateTime.now());

        CarbonReport updated = reportDAO.update(report);
        System.out.println("[CLEAN] Rapport carbone validé: " + report.getProjectName());
        return updated;
    }

    // L'expert carbone rejette le rapport (demande modifications)
    public CarbonReport rejectReport(Long reportId, Long expertCarboneId, String motifRejet) {
        Optional<CarbonReport> reportOpt = reportDAO.findById(reportId);
        if (reportOpt.isEmpty()) {
            System.err.println("[CLEAN] Rapport carbone non trouvé");
            return null;
        }

        CarbonReport report = reportOpt.get();
        report.setExpertCarboneId(expertCarboneId);
        report.setCommentairesExpert(motifRejet);
        report.setStatut(CarbonReport.StatutRapport.REJECTE);
        report.setDateValidation(LocalDateTime.now());

        CarbonReport updated = reportDAO.update(report);
        System.out.println("[CLEAN] Rapport carbone rejeté: " + report.getProjectName());
        return updated;
    }

    // Récupérer tous les rapports
    public List<CarbonReport> getAllReports() {
        return reportDAO.findAll();
    }

    // Récupérer les rapports en attente d'évaluation
    public List<CarbonReport> getPendingReports() {
        return reportDAO.findPendingReports();
    }

    // Récupérer les rapports validés
    public List<CarbonReport> getValidatedReports() {
        return reportDAO.findByStatut(CarbonReport.StatutRapport.VALIDE);
    }

    // Récupérer les rapports rejetés
    public List<CarbonReport> getRejectedReports() {
        return reportDAO.findByStatut(CarbonReport.StatutRapport.REJECTE);
    }

    // Récupérer rapport par ID
    public Optional<CarbonReport> getReportById(Long id) {
        return reportDAO.findById(id);
    }

    // Récupérer rapports d'un projet
    public List<CarbonReport> getReportsByProject(Long projectId) {
        return reportDAO.findByProjectId(projectId);
    }

    // Récupérer rapports d'un porteur de projet
    public List<CarbonReport> getReportsByPorteurProjet(Long porteurProjetId) {
        return reportDAO.findByPorteurProjetId(porteurProjetId);
    }

    // Statistiques
    public long getPendingCount() {
        return reportDAO.countByStatut(CarbonReport.StatutRapport.EN_ATTENTE);
    }

    public long getValidatedCount() {
        return reportDAO.countByStatut(CarbonReport.StatutRapport.VALIDE);
    }

    public long getRejectedCount() {
        return reportDAO.countByStatut(CarbonReport.StatutRapport.REJECTE);
    }

    public long getTotalCount() {
        return reportDAO.count();
    }

    // Moyennes d'émissions (neutre, basée sur rapports validés)
    public Double getAverageEmissions() {
        List<CarbonReport> validated = getValidatedReports();
        if (validated.isEmpty()) return 0.0;

        double total = validated.stream()
                .mapToDouble(r -> r.getEmissionsEstimate() != null ? r.getEmissionsEstimate() : 0)
                .sum();
        return total / validated.size();
    }
    
    // ============ EXTERNAL API INTEGRATION ============
    
    /**
     * Enrich carbon report with external API data.
     * Calls Carbon Interface API and OpenWeatherMap Air Quality API to add real-time data.
     * Uses graceful degradation - if APIs fail, the report continues without external data.
     * 
     * @param report The carbon report to enrich
     * @param projet The associated project with location and activity data
     * @return The enriched report (same instance, modified)
     */
    public CarbonReport enrichWithExternalData(CarbonReport report, Projet projet) {
        if (report == null || projet == null) {
            System.err.println("[CARBON ENRICHMENT] Cannot enrich - report or projet is null");
            return report;
        }
        
        System.out.println("[CARBON ENRICHMENT] Starting enrichment for project: " + projet.getTitre());
        
        StringBuilder enrichmentData = new StringBuilder();
        enrichmentData.append("\n\n========== EXTERNAL API DATA ==========\n");
        enrichmentData.append("Enriched at: ").append(LocalDateTime.now()).append("\n\n");
        
        boolean hasEnrichment = false;
        
        // 1. Carbon Interface API - Emission Estimates
        if (carbonApiService.isEnabled() && projet.getActivityType() != null) {
            try {
                System.out.println("[CARBON ENRICHMENT] Calling Carbon Interface API...");
                
                CarbonEstimateResponse carbonData = null;
                
                // Choose appropriate API call based on activity type
                String activityType = projet.getActivityType().toLowerCase();
                if (activityType.contains("electricity") || activityType.contains("energy")) {
                    carbonData = carbonApiService.estimateElectricity(1000.0, "kwh", "us");
                } else if (activityType.contains("fuel") || activityType.contains("gas")) {
                    carbonData = carbonApiService.estimateFuel("natural_gas", 100.0, "gallon");
                } else if (activityType.contains("shipping") || activityType.contains("transport")) {
                    carbonData = carbonApiService.estimateShipping(1000.0, 100.0, "truck");
                } else {
                    // Default: use electricity estimate
                    carbonData = carbonApiService.estimateElectricity(1000.0, "kwh", "us");
                }
                
                if (carbonData != null && carbonData.getAttributes() != null) {
                    enrichmentData.append("📊 CARBON INTERFACE API DATA:\n");
                    enrichmentData.append("   Activity Type: ").append(projet.getActivityType()).append("\n");
                    enrichmentData.append("   Estimated Carbon: ")
                        .append(String.format("%.2f kg CO2", carbonData.getAttributes().getCarbonKgOrDefault()))
                        .append("\n");
                    enrichmentData.append("   (").append(String.format("%.2f g", carbonData.getAttributes().getCarbonG()))
                        .append(" / ").append(String.format("%.4f metric tons", carbonData.getAttributes().getCarbonMt()))
                        .append(")\n");
                    enrichmentData.append("   API Response ID: ").append(carbonData.getId()).append("\n");
                    enrichmentData.append("   Timestamp: ").append(carbonData.getAttributes().getEstimatedAt()).append("\n\n");
                    
                    hasEnrichment = true;
                    System.out.println("[CARBON ENRICHMENT] ✓ Carbon data retrieved successfully");
                } else {
                    enrichmentData.append("⚠️ Carbon Interface API: No data available\n\n");
                    System.out.println("[CARBON ENRICHMENT] ✗ Carbon API returned no data");
                }
                
            } catch (Exception e) {
                enrichmentData.append("❌ Carbon Interface API Error: ").append(e.getMessage()).append("\n\n");
                System.err.println("[CARBON ENRICHMENT] Error calling Carbon API: " + e.getMessage());
            }
        } else if (!carbonApiService.isEnabled()) {
            // Use mock data for demonstration
            enrichmentData.append("🔧 MOCK CARBON DATA (API key not configured):\n");
            enrichmentData.append("   Activity Type: ").append(projet.getActivityType()).append("\n");
            enrichmentData.append("   Mock Estimate: 500.00 kg CO2\n");
            enrichmentData.append("   Note: Set CARBON_API_KEY environment variable for real data\n\n");
            System.out.println("[CARBON ENRICHMENT] Using mock carbon data");
        }
        
        // 2. OpenWeatherMap Air Quality API
        if (projet.hasValidLocation()) {
            try {
                System.out.println("[CARBON ENRICHMENT] Calling OpenWeatherMap Air Quality API...");
                
                AirPollutionResponse airData = null;
                
                if (airQualityService.isEnabled()) {
                    airData = airQualityService.getCurrentAirQuality(
                        projet.getLatitude(), 
                        projet.getLongitude()
                    );
                } else {
                    // Use mock data
                    airData = airQualityService.getMockAirQuality(
                        projet.getLatitude(), 
                        projet.getLongitude()
                    );
                }
                
                if (airData != null) {
                    AirQualityData currentAir = airData.getCurrentReading();
                    
                    if (currentAir != null) {
                        enrichmentData.append("🌍 AIR QUALITY DATA:\n");
                        enrichmentData.append("   Location: ")
                            .append(String.format("%.4f°N, %.4f°E", projet.getLatitude(), projet.getLongitude()))
                            .append("\n");
                        
                        if (currentAir.getMain() != null) {
                            int aqi = currentAir.getMain().getAqi();
                            enrichmentData.append("   Air Quality Index: ").append(aqi)
                                .append(" - ").append(currentAir.getAirQualityDescription()).append("\n");
                        }
                        
                        if (currentAir.getComponents() != null) {
                            enrichmentData.append("   Pollutants:\n");
                            Models.dto.external.Components comp = currentAir.getComponents();
                            if (comp.getCo() != null) 
                                enrichmentData.append("      • CO (Carbon Monoxide): ")
                                    .append(String.format("%.2f μg/m³", comp.getCo())).append("\n");
                            if (comp.getNo2() != null) 
                                enrichmentData.append("      • NO₂ (Nitrogen Dioxide): ")
                                    .append(String.format("%.2f μg/m³", comp.getNo2())).append("\n");
                            if (comp.getPm2_5() != null) 
                                enrichmentData.append("      • PM2.5 (Fine Particles): ")
                                    .append(String.format("%.2f μg/m³", comp.getPm2_5())).append("\n");
                            if (comp.getPm10() != null) 
                                enrichmentData.append("      • PM10 (Coarse Particles): ")
                                    .append(String.format("%.2f μg/m³", comp.getPm10())).append("\n");
                            if (comp.getO3() != null) 
                                enrichmentData.append("      • O₃ (Ozone): ")
                                    .append(String.format("%.2f μg/m³", comp.getO3())).append("\n");
                        }
                        
                        enrichmentData.append("   Timestamp: ").append(
                            java.time.Instant.ofEpochSecond(currentAir.getDt()).toString()
                        ).append("\n\n");
                        
                        hasEnrichment = true;
                        System.out.println("[CARBON ENRICHMENT] ✓ Air quality data retrieved successfully");
                    } else {
                        enrichmentData.append("⚠️ OpenWeatherMap API: No readings available\n\n");
                        System.out.println("[CARBON ENRICHMENT] ✗ Air quality API returned no readings");
                    }
                } else {
                    enrichmentData.append("⚠️ OpenWeatherMap API: No data available\n\n");
                    System.out.println("[CARBON ENRICHMENT] ✗ Air quality API returned no data");
                }
                
            } catch (Exception e) {
                enrichmentData.append("❌ OpenWeatherMap API Error: ").append(e.getMessage()).append("\n\n");
                System.err.println("[CARBON ENRICHMENT] Error calling Air Quality API: " + e.getMessage());
            }
        } else {
            enrichmentData.append("⚠️ Air Quality: Project location not set (lat/lon required)\n\n");
            System.out.println("[CARBON ENRICHMENT] Skipping air quality - no valid coordinates");
        }
        
        enrichmentData.append("========================================\n");
        
        // Append enrichment data to evaluation details
        if (hasEnrichment) {
            String currentDetails = report.getEvaluationDetails() != null ? report.getEvaluationDetails() : "";
            report.setEvaluationDetails(currentDetails + enrichmentData.toString());
            System.out.println("[CARBON ENRICHMENT] ✓ Enrichment complete - data added to report");
        } else {
            System.out.println("[CARBON ENRICHMENT] ⚠ No external data retrieved");
        }
        
        return report;
    }
    
    /**
     * Test external API connections
     * @return true if at least one API is working
     */
    public boolean testExternalApis() {
        boolean carbonOk = carbonApiService.testConnection();
        boolean airQualityOk = airQualityService.testConnection();
        
        System.out.println("[API TEST] Carbon Interface API: " + (carbonOk ? "✓ OK" : "✗ FAILED"));
        System.out.println("[API TEST] OpenWeatherMap API: " + (airQualityOk ? "✓ OK" : "✗ FAILED"));
        
        return carbonOk || airQualityOk;
    }
}
