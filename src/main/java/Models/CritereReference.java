package Models;

public class CritereReference {

    private int idCritere;
    private String nomCritere;
    private String description;
    private int poids;

    public CritereReference() {}

    public CritereReference(String nomCritere, String description, int poids) {
        this.nomCritere = nomCritere;
        this.description = description;
        this.poids = poids;
    }

    public int getIdCritere() {
        return idCritere;
    }

    public void setIdCritere(int idCritere) {
        this.idCritere = idCritere;
    }

    public String getNomCritere() {
        return nomCritere;
    }

    public void setNomCritere(String nomCritere) {
        this.nomCritere = nomCritere;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getPoids() {
        return poids;
    }

    public void setPoids(int poids) {
        this.poids = poids;
    }
}

