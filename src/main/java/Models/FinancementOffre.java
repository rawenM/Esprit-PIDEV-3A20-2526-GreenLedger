package Models;

public class FinancementOffre {
    private Integer financementId;
    private Integer projetId;
    private Integer banqueId;
    private Double montant;
    private String dateFinancement;

    private Integer offreId;
    private String typeOffre;
    private Double taux;
    private Integer duree;

    public Integer getFinancementId() {
        return financementId;
    }

    public void setFinancementId(Integer financementId) {
        this.financementId = financementId;
    }

    public Integer getProjetId() {
        return projetId;
    }

    public void setProjetId(Integer projetId) {
        this.projetId = projetId;
    }

    public Integer getBanqueId() {
        return banqueId;
    }

    public void setBanqueId(Integer banqueId) {
        this.banqueId = banqueId;
    }

    public Double getMontant() {
        return montant;
    }

    public void setMontant(Double montant) {
        this.montant = montant;
    }

    public String getDateFinancement() {
        return dateFinancement;
    }

    public void setDateFinancement(String dateFinancement) {
        this.dateFinancement = dateFinancement;
    }

    public Integer getOffreId() {
        return offreId;
    }

    public void setOffreId(Integer offreId) {
        this.offreId = offreId;
    }

    public String getTypeOffre() {
        return typeOffre;
    }

    public void setTypeOffre(String typeOffre) {
        this.typeOffre = typeOffre;
    }

    public Double getTaux() {
        return taux;
    }

    public void setTaux(Double taux) {
        this.taux = taux;
    }

    public Integer getDuree() {
        return duree;
    }

    public void setDuree(Integer duree) {
        this.duree = duree;
    }
}

