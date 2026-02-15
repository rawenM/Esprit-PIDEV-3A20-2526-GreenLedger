package Models;

public class Financement {
    private int id;
    private int projetId;
    private int banqueId;
    private double montant;
    private String dateFinancement;

    public Financement() {}

    public Financement(int id, int projetId, int banqueId, double montant, String dateFinancement) {
        this.id = id;
        this.projetId = projetId;
        this.banqueId = banqueId;
        this.montant = montant;
        this.dateFinancement = dateFinancement;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProjetId() { return projetId; }
    public void setProjetId(int projetId) { this.projetId = projetId; }

    public int getBanqueId() { return banqueId; }
    public void setBanqueId(int banqueId) { this.banqueId = banqueId; }

    public double getMontant() { return montant; }
    public void setMontant(double montant) { this.montant = montant; }

    public String getDateFinancement() { return dateFinancement; }
    public void setDateFinancement(String dateFinancement) { this.dateFinancement = dateFinancement; }

    @Override public String toString() {
        return String.valueOf(id);
    }
}


