package Models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private Budget budget;

    // Location & metadata
    private String  activityType;
    private Double  latitude;
    private Double  longitude;
    private java.time.LocalDateTime geocodedAt;
    private Integer airQualityIndex;
    private String  secteur;
    private String  typeProjet;
    private String  localisation;
    private LocalDateTime dateCreation;

    // Environmental data (from Symfony entity)
    private Double consommationEnergie;
    private String uniteEnergie;
    private Double distanceTransport;
    private String typeTransport;
    private String typeMateriau;
    private Double quantiteMateriau;
    private Double consommationEau;
    private Double dechetsGeneres;
    private Double emissionsEstimees;
    private String sourceEmissions;

    // Fraud detection
    private Double  fraudRiskScore;
    private Double  fraudAnomalyScore;
    private Boolean fraudFlag;
    private String  fraudReasons;

    // Carbon metrics
    private Double baselineTco2;
    private Double actualTco2;
    private Double avoidedTco2;

    // Green credits
    private Double  dispatchedGreenCredits;
    private String  greenCreditDispatchStatus;

    // Financing
    private Double  montantDemande;
    private String  statutFinancement;
    private LocalDateTime fundedAt;
    private String  descriptionProjet;

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

    // ── Core getters/setters ──────────────────────────────────────────────

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

    // ── Budget ────────────────────────────────────────────────────────────

    public double getBudget() { return (budget != null) ? budget.getMontant() : 0.0; }
    public void setBudget(double montant) {
        if (this.budget == null) this.budget = new Budget();
        this.budget.setMontant(montant);
    }
    public Budget getBudgetObj() { return budget; }
    public void setBudget(Budget budget) { this.budget = budget; }

    // ── Aliases ───────────────────────────────────────────────────────────

    public int    getEntrepriseId()              { return idUser; }
    public void   setEntrepriseId(int id)        { this.idUser = id; }
    public String getStatut()                    { return statutEvaluation; }
    public void   setStatut(String s)            { this.statutEvaluation = s; }

    // ── Location ──────────────────────────────────────────────────────────

    public String  getActivityType()             { return activityType; }
    public void    setActivityType(String v)     { this.activityType = v; }
    public Double  getLatitude()                 { return latitude; }
    public void    setLatitude(Double v)         { this.latitude = v; }
    public Double  getLongitude()                { return longitude; }
    public void    setLongitude(Double v)        { this.longitude = v; }
    public java.time.LocalDateTime getGeocodedAt()          { return geocodedAt; }
    public void    setGeocodedAt(java.time.LocalDateTime v) { this.geocodedAt = v; }
    public Integer getAirQualityIndex()          { return airQualityIndex; }
    public void    setAirQualityIndex(Integer v) { this.airQualityIndex = v; }
    public String  getSecteur()                  { return secteur; }
    public void    setSecteur(String v)          { this.secteur = v; }
    public String  getTypeProjet()               { return typeProjet; }
    public void    setTypeProjet(String v)       { this.typeProjet = v; }
    public String  getLocalisation()             { return localisation; }
    public void    setLocalisation(String v)     { this.localisation = v; }
    public LocalDateTime getDateCreation()       { return dateCreation; }
    public void    setDateCreation(LocalDateTime v) { this.dateCreation = v; }

    // ── Environmental data ────────────────────────────────────────────────

    public Double  getConsommationEnergie()      { return consommationEnergie; }
    public void    setConsommationEnergie(Double v) { this.consommationEnergie = v; }
    public String  getUniteEnergie()             { return uniteEnergie; }
    public void    setUniteEnergie(String v)     { this.uniteEnergie = v; }
    public Double  getDistanceTransport()        { return distanceTransport; }
    public void    setDistanceTransport(Double v){ this.distanceTransport = v; }
    public String  getTypeTransport()            { return typeTransport; }
    public void    setTypeTransport(String v)    { this.typeTransport = v; }
    public String  getTypeMateriau()             { return typeMateriau; }
    public void    setTypeMateriau(String v)     { this.typeMateriau = v; }
    public Double  getQuantiteMateriau()         { return quantiteMateriau; }
    public void    setQuantiteMateriau(Double v) { this.quantiteMateriau = v; }
    public Double  getConsommationEau()          { return consommationEau; }
    public void    setConsommationEau(Double v)  { this.consommationEau = v; }
    public Double  getDechetsGeneres()           { return dechetsGeneres; }
    public void    setDechetsGeneres(Double v)   { this.dechetsGeneres = v; }
    public Double  getEmissionsEstimees()        { return emissionsEstimees; }
    public void    setEmissionsEstimees(Double v){ this.emissionsEstimees = v; }
    public String  getSourceEmissions()          { return sourceEmissions; }
    public void    setSourceEmissions(String v)  { this.sourceEmissions = v; }

    // ── Fraud detection ───────────────────────────────────────────────────

    public Double  getFraudRiskScore()           { return fraudRiskScore; }
    public void    setFraudRiskScore(Double v)   { this.fraudRiskScore = v; }
    public Double  getFraudAnomalyScore()        { return fraudAnomalyScore; }
    public void    setFraudAnomalyScore(Double v){ this.fraudAnomalyScore = v; }
    public Boolean getFraudFlag()                { return fraudFlag; }
    public void    setFraudFlag(Boolean v)       { this.fraudFlag = v; }
    public String  getFraudReasons()             { return fraudReasons; }
    public void    setFraudReasons(String v)     { this.fraudReasons = v; }

    // ── Carbon metrics ────────────────────────────────────────────────────

    public Double  getBaselineTco2()             { return baselineTco2; }
    public void    setBaselineTco2(Double v)     { this.baselineTco2 = v; }
    public Double  getActualTco2()               { return actualTco2; }
    public void    setActualTco2(Double v)       { this.actualTco2 = v; }
    public Double  getAvoidedTco2()              { return avoidedTco2; }
    public void    setAvoidedTco2(Double v)      { this.avoidedTco2 = v; }

    // ── Green credits ─────────────────────────────────────────────────────

    public Double  getDispatchedGreenCredits()   { return dispatchedGreenCredits; }
    public void    setDispatchedGreenCredits(Double v) { this.dispatchedGreenCredits = v; }
    public String  getGreenCreditDispatchStatus(){ return greenCreditDispatchStatus; }
    public void    setGreenCreditDispatchStatus(String v) { this.greenCreditDispatchStatus = v; }

    // ── Financing ─────────────────────────────────────────────────────────

    public Double  getMontantDemande()           { return montantDemande; }
    public void    setMontantDemande(Double v)   { this.montantDemande = v; }
    public String  getStatutFinancement()        { return statutFinancement; }
    public void    setStatutFinancement(String v){ this.statutFinancement = v; }
    public LocalDateTime getFundedAt()           { return fundedAt; }
    public void    setFundedAt(LocalDateTime v)  { this.fundedAt = v; }
    public String  getDescriptionProjet()        { return descriptionProjet; }
    public void    setDescriptionProjet(String v){ this.descriptionProjet = v; }

    // ── Utilities ─────────────────────────────────────────────────────────

    public boolean hasValidLocation() {
        return latitude != null && longitude != null;
    }

    @Override
    public String toString() {
        return "Projet[id=" + id + ", titre=" + titre + ", statut=" + statutEvaluation + "]";
    }
}
