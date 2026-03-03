package dao;

import Models.CarbonReport;

import java.util.List;
import java.util.Optional;

/**
 * Interface DAO pour les rapports d'émissions carbone
 */
public interface ICarbonReportDAO {

    // CRUD de base
    CarbonReport save(CarbonReport report);
    Optional<CarbonReport> findById(Long id);
    List<CarbonReport> findAll();
    CarbonReport update(CarbonReport report);
    boolean delete(Long id);

    // Recherches spécifiques
    List<CarbonReport> findByProjectId(Long projectId);
    List<CarbonReport> findByStatut(CarbonReport.StatutRapport statut);
    List<CarbonReport> findByExpertCarboneId(Long expertCarboneId);
    List<CarbonReport> findByPorteurProjetId(Long porteurProjetId);
    List<CarbonReport> findPendingReports(); // Rapports EN_ATTENTE

    // Statistiques
    long countByStatut(CarbonReport.StatutRapport statut);
    long count();
}

