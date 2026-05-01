package Models.dto;

/**
 * Input DTO for all ML services.
 * Maps directly from Projet entity fields.
 */
public class ProjectData {

    // Numeric environmental fields
    private Double consommationEnergie;   // kWh or MWh
    private Double distanceTransport;     // km
    private Double quantiteMateriau;      // tonnes
    private Double consommationEau;       // m³
    private Double dechetsGeneres;        // tonnes
    private Double emissionsEstimees;     // tCO2e declared by project holder
    private Double totalTco2;             // filled after Climatiq call

    // Categorical fields
    private String secteur;               // Energie, Agriculture, Industrie...
    private String typeProjet;            // Solaire, Eolien, Agri...
    private String localisation;          // Sfax, Tunis, France...
    private String uniteEnergie;          // kWh, MWh, GJ
    private String typeTransport;         // camion, train, avion, bateau
    private String typeMateriau;          // acier, beton, bois, aluminium
    private String sourceEmissions;       // scope_1, scope_3, declaration

    // ── Factory from Projet ───────────────────────────────────────────────

    public static ProjectData from(Models.Projet p) {
        ProjectData d = new ProjectData();
        d.consommationEnergie = p.getConsommationEnergie();
        d.distanceTransport   = p.getDistanceTransport();
        d.quantiteMateriau    = p.getQuantiteMateriau();
        d.consommationEau     = p.getConsommationEau();
        d.dechetsGeneres      = p.getDechetsGeneres();
        d.emissionsEstimees   = p.getEmissionsEstimees();
        d.secteur             = p.getSecteur();
        d.typeProjet          = p.getTypeProjet();
        d.localisation        = p.getLocalisation();
        d.uniteEnergie        = p.getUniteEnergie();
        d.typeTransport       = p.getTypeTransport();
        d.typeMateriau        = p.getTypeMateriau();
        d.sourceEmissions     = p.getSourceEmissions();
        return d;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────

    public Double getConsommationEnergie()              { return consommationEnergie; }
    public void   setConsommationEnergie(Double v)      { this.consommationEnergie = v; }

    public Double getDistanceTransport()                { return distanceTransport; }
    public void   setDistanceTransport(Double v)        { this.distanceTransport = v; }

    public Double getQuantiteMateriau()                 { return quantiteMateriau; }
    public void   setQuantiteMateriau(Double v)         { this.quantiteMateriau = v; }

    public Double getConsommationEau()                  { return consommationEau; }
    public void   setConsommationEau(Double v)          { this.consommationEau = v; }

    public Double getDechetsGeneres()                   { return dechetsGeneres; }
    public void   setDechetsGeneres(Double v)           { this.dechetsGeneres = v; }

    public Double getEmissionsEstimees()                { return emissionsEstimees; }
    public void   setEmissionsEstimees(Double v)        { this.emissionsEstimees = v; }

    public Double getTotalTco2()                        { return totalTco2; }
    public void   setTotalTco2(Double v)                { this.totalTco2 = v; }

    public String getSecteur()                          { return secteur; }
    public void   setSecteur(String v)                  { this.secteur = v; }

    public String getTypeProjet()                       { return typeProjet; }
    public void   setTypeProjet(String v)               { this.typeProjet = v; }

    public String getLocalisation()                     { return localisation; }
    public void   setLocalisation(String v)             { this.localisation = v; }

    public String getUniteEnergie()                     { return uniteEnergie; }
    public void   setUniteEnergie(String v)             { this.uniteEnergie = v; }

    public String getTypeTransport()                    { return typeTransport; }
    public void   setTypeTransport(String v)            { this.typeTransport = v; }

    public String getTypeMateriau()                     { return typeMateriau; }
    public void   setTypeMateriau(String v)             { this.typeMateriau = v; }

    public String getSourceEmissions()                  { return sourceEmissions; }
    public void   setSourceEmissions(String v)          { this.sourceEmissions = v; }
}
