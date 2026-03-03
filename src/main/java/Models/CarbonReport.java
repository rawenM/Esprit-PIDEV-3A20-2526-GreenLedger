package Models;

import java.time.LocalDateTime;

/**
 * Modèle pour les rapports d'émissions carbone
 * Évalués et validés par l'Expert Carbone
 */
public class CarbonReport {

    private Long id;
    private Long projectId; // ID du projet évalué
    private String projectName; // Nom du projet
    private Long porteurProjetId; // ID du porteur de projet
    private String porteurProjetName; // Nom du porteur de projet
    private Long expertCarboneId; // ID de l'expert carbone qui valide
    private Double emissionsEstimate; // Estimation d'émissions (tCO2e)
    private String evaluationDetails; // Détails de l'évaluation
    private StatutRapport statut; // EN_ATTENTE, VALIDE, REJECTE
    private String commentairesExpert; // Commentaires de l'expert
    private LocalDateTime dateCreation;
    private LocalDateTime dateValidation;

    public CarbonReport() {
        this.dateCreation = LocalDateTime.now();
        this.statut = StatutRapport.EN_ATTENTE;
    }

    public CarbonReport(Long projectId, String projectName, Long porteurProjetId, String porteurProjetName) {
        this();
        this.projectId = projectId;
        this.projectName = projectName;
        this.porteurProjetId = porteurProjetId;
        this.porteurProjetName = porteurProjetName;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public Long getPorteurProjetId() { return porteurProjetId; }
    public void setPorteurProjetId(Long porteurProjetId) { this.porteurProjetId = porteurProjetId; }

    public String getPorteurProjetName() { return porteurProjetName; }
    public void setPorteurProjetName(String porteurProjetName) { this.porteurProjetName = porteurProjetName; }

    public Long getExpertCarboneId() { return expertCarboneId; }
    public void setExpertCarboneId(Long expertCarboneId) { this.expertCarboneId = expertCarboneId; }

    public Double getEmissionsEstimate() { return emissionsEstimate; }
    public void setEmissionsEstimate(Double emissionsEstimate) { this.emissionsEstimate = emissionsEstimate; }

    public String getEvaluationDetails() { return evaluationDetails; }
    public void setEvaluationDetails(String evaluationDetails) { this.evaluationDetails = evaluationDetails; }

    public StatutRapport getStatut() { return statut; }
    public void setStatut(StatutRapport statut) { this.statut = statut; }

    public String getCommentairesExpert() { return commentairesExpert; }
    public void setCommentairesExpert(String commentairesExpert) { this.commentairesExpert = commentairesExpert; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public LocalDateTime getDateValidation() { return dateValidation; }
    public void setDateValidation(LocalDateTime dateValidation) { this.dateValidation = dateValidation; }

    @Override
    public String toString() {
        return "CarbonReport{" +
                "id=" + id +
                ", projectName='" + projectName + '\'' +
                ", emissionsEstimate=" + emissionsEstimate +
                ", statut=" + statut +
                '}';
    }

    // Énumération pour le statut du rapport
    public enum StatutRapport {
        EN_ATTENTE("En attente d'évaluation", "#FCD34D"),
        VALIDE("Validé", "#10B981"),
        REJECTE("Rejeté", "#EF4444");

        private final String libelle;
        private final String couleur;

        StatutRapport(String libelle, String couleur) {
            this.libelle = libelle;
            this.couleur = couleur;
        }

        public String getLibelle() { return libelle; }
        public String getCouleur() { return couleur; }
    }
}

