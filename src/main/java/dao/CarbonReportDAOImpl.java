package dao;

import Models.CarbonReport;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implémentation DAO pour rapports carbone (stockage en mémoire pour MVP)
 * TODO: Intégrer avec base de données MySQL
 */
public class CarbonReportDAOImpl implements ICarbonReportDAO {

    private static final List<CarbonReport> reports = new ArrayList<>();
    private static Long nextId = 1L;

    @Override
    public CarbonReport save(CarbonReport report) {
        if (report.getId() == null) {
            report.setId(nextId++);
        }
        reports.add(report);
        System.out.println("[CLEAN] Rapport carbone créé: " + report.getProjectName());
        return report;
    }

    @Override
    public Optional<CarbonReport> findById(Long id) {
        return reports.stream().filter(r -> r.getId().equals(id)).findFirst();
    }

    @Override
    public List<CarbonReport> findAll() {
        return new ArrayList<>(reports);
    }

    @Override
    public CarbonReport update(CarbonReport report) {
        Optional<CarbonReport> existing = findById(report.getId());
        if (existing.isPresent()) {
            reports.remove(existing.get());
            reports.add(report);
            System.out.println("[CLEAN] Rapport carbone mis à jour: " + report.getProjectName());
            return report;
        }
        return null;
    }

    @Override
    public boolean delete(Long id) {
        Optional<CarbonReport> report = findById(id);
        if (report.isPresent()) {
            reports.remove(report.get());
            System.out.println("[CLEAN] Rapport carbone supprimé");
            return true;
        }
        return false;
    }

    @Override
    public List<CarbonReport> findByProjectId(Long projectId) {
        List<CarbonReport> result = new ArrayList<>();
        for (CarbonReport r : reports) {
            if (r.getProjectId().equals(projectId)) {
                result.add(r);
            }
        }
        return result;
    }

    @Override
    public List<CarbonReport> findByStatut(CarbonReport.StatutRapport statut) {
        List<CarbonReport> result = new ArrayList<>();
        for (CarbonReport r : reports) {
            if (r.getStatut() == statut) {
                result.add(r);
            }
        }
        return result;
    }

    @Override
    public List<CarbonReport> findByExpertCarboneId(Long expertCarboneId) {
        List<CarbonReport> result = new ArrayList<>();
        for (CarbonReport r : reports) {
            if (expertCarboneId.equals(r.getExpertCarboneId())) {
                result.add(r);
            }
        }
        return result;
    }

    @Override
    public List<CarbonReport> findByPorteurProjetId(Long porteurProjetId) {
        List<CarbonReport> result = new ArrayList<>();
        for (CarbonReport r : reports) {
            if (porteurProjetId.equals(r.getPorteurProjetId())) {
                result.add(r);
            }
        }
        return result;
    }

    @Override
    public List<CarbonReport> findPendingReports() {
        return findByStatut(CarbonReport.StatutRapport.EN_ATTENTE);
    }

    @Override
    public long countByStatut(CarbonReport.StatutRapport statut) {
        return findByStatut(statut).size();
    }

    @Override
    public long count() {
        return reports.size();
    }
}

