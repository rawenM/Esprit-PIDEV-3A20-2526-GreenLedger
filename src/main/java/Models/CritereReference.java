package Models;

import java.math.BigDecimal;

/**
 * Critere Reference — evaluation criteria definition.
 *
 * poids is stored as int (legacy) but also accessible as BigDecimal.
 */
public class CritereReference {

    private Integer idCritere;
    private String  nom;
    private String  description;
    private int     poids = 1;          // legacy int — used by existing services
    private String  categorie;
    private Integer ordreAffichage;
    private Boolean actif = true;

    public CritereReference() {}

    public CritereReference(String nom, int poids) {
        this.nom   = nom;
        this.poids = poids;
    }

    /** 3-arg constructor used by CarbonAuditController */
    public CritereReference(String nom, String description, int poids) {
        this.nom         = nom;
        this.description = description;
        this.poids       = poids;
    }

    /** 3-arg constructor with Integer poids */
    public CritereReference(String nom, String description, Integer poids) {
        this.nom         = nom;
        this.description = description;
        this.poids       = poids != null ? poids : 1;
    }

    // ── idCritere ─────────────────────────────────────────────────────────
    public Integer getIdCritere()              { return idCritere; }
    public void    setIdCritere(Integer v)     { this.idCritere = v; }

    // ── nom ───────────────────────────────────────────────────────────────
    public String getNom()                     { return nom; }
    public void   setNom(String v)             { this.nom = v; }

    /** Alias used by CritereImpactService, ScoringService, AiSuggestionService, etc. */
    public String getNomCritere()              { return nom; }
    public void   setNomCritere(String v)      { this.nom = v; }

    // ── description ───────────────────────────────────────────────────────
    public String getDescription()             { return description; }
    public void   setDescription(String v)     { this.description = v; }

    // ── poids (int — primary) ─────────────────────────────────────────────
    public int  getPoids()                     { return poids; }
    public void setPoids(int v)                { this.poids = v; }

    /** BigDecimal overload — used by new Symfony-ported code */
    public void setPoids(BigDecimal v)         { this.poids = v != null ? v.intValue() : 1; }

    /** BigDecimal view — used by new Symfony-ported code */
    public BigDecimal getPoidsDecimal()        { return BigDecimal.valueOf(poids); }

    // ── categorie ─────────────────────────────────────────────────────────
    public String  getCategorie()              { return categorie; }
    public void    setCategorie(String v)      { this.categorie = v; }

    // ── ordreAffichage ────────────────────────────────────────────────────
    public Integer getOrdreAffichage()         { return ordreAffichage; }
    public void    setOrdreAffichage(Integer v){ this.ordreAffichage = v; }

    // ── actif ─────────────────────────────────────────────────────────────
    public Boolean getActif()                  { return actif; }
    public void    setActif(Boolean v)         { this.actif = v; }

    @Override
    public String toString() {
        return String.format("CritereReference[id=%d, nom=%s, poids=%d, actif=%b]",
                idCritere, nom, poids, actif);
    }
}
