package Models;

public class OffreFinancement {
    private int idOffre;
    private String typeOffre;
    private double taux;
    private int duree;
    private int idFinancement;

    public OffreFinancement() {}

    public OffreFinancement(int idOffre, String typeOffre, double taux, int duree, int idFinancement) {
        this.idOffre = idOffre;
        this.typeOffre = typeOffre;
        this.taux = taux;
        this.duree = duree;
        this.idFinancement = idFinancement;
    }

    public int getIdOffre() { return idOffre; }
    public void setIdOffre(int idOffre) { this.idOffre = idOffre; }

    public String getTypeOffre() { return typeOffre; }
    public void setTypeOffre(String typeOffre) { this.typeOffre = typeOffre; }

    public double getTaux() { return taux; }
    public void setTaux(double taux) { this.taux = taux; }

    public int getDuree() { return duree; }
    public void setDuree(int duree) { this.duree = duree; }

    public int getIdFinancement() { return idFinancement; }
    public void setIdFinancement(int idFinancement) { this.idFinancement = idFinancement; }
}


