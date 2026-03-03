package Models;

public class Projet {
    private int id;
    private int idUser;
    private String titre;
    private String description;
    private Integer scoreEsg;
    private String statutEvaluation;
    private String companyAddress;
    private String companyEmail;
    private String companyPhone;
    // Relation 1-1 (table budget)
    private Budget budget;

    private String activityType;
    private Double latitude;
    private Double longitude;

    public Projet() {}

    public Projet(int id, int idUser, String titre, String description,
                  Integer scoreEsg, String statutEvaluation,
                  String companyAddress, String companyEmail, String companyPhone) {
        this.id = id;
        this.idUser = idUser;
        this.titre = titre;
        this.description = description;
        this.scoreEsg = scoreEsg;
        this.statutEvaluation = statutEvaluation;
        this.companyAddress = companyAddress;
        this.companyEmail = companyEmail;
        this.companyPhone = companyPhone;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdUser() { return idUser; }
    public void setIdUser(int idUser) { this.idUser = idUser; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getScoreEsg() { return scoreEsg; }
    public void setScoreEsg(Integer scoreEsg) { this.scoreEsg = scoreEsg; }

    public String getStatutEvaluation() { return statutEvaluation; }
    public void setStatutEvaluation(String statutEvaluation) { this.statutEvaluation = statutEvaluation; }

    public String getCompanyAddress() { return companyAddress; }
    public void setCompanyAddress(String companyAddress) { this.companyAddress = companyAddress; }

    public String getCompanyEmail() { return companyEmail; }
    public void setCompanyEmail(String companyEmail) { this.companyEmail = companyEmail; }

    public String getCompanyPhone() { return companyPhone; }
    public void setCompanyPhone(String companyPhone) { this.companyPhone = companyPhone; }


    public double getBudget() {
        return (budget != null) ? budget.getMontant() : 0.0;
    }

    public void setBudget(double montant) {
        if (this.budget == null) this.budget = new Budget();
        this.budget.setMontant(montant);
    }

    public Budget getBudgetObj() { return budget; }
    public void setBudget(Budget budget) { this.budget = budget; }

    public int getEntrepriseId() { return idUser; }
    public void setEntrepriseId(int entrepriseId) { this.idUser = entrepriseId; }

    public String getStatut() { return statutEvaluation; }
    public void setStatut(String statut) { this.statutEvaluation = statut; }

    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public boolean hasValidLocation() {
        return latitude != null && longitude != null;
    }
}
