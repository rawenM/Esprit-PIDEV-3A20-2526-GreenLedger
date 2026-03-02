package Models;

public class Budget {

    private int idBudget;
    private double montant;
    private String raison;
    private String devise;
    private int idProjet;

    public Budget() {}

    public Budget(double montant, String raison, String devise, int idProjet) {
        this.montant = montant;
        this.raison = raison;
        this.devise = devise;
        this.idProjet = idProjet;
    }

    public int getIdBudget() {
        return idBudget;
    }

    public void setIdBudget(int idBudget) {
        this.idBudget = idBudget;
    }

    public double getMontant() {
        return montant;
    }

    public void setMontant(double montant) {
        this.montant = montant;
    }

    public String getRaison() {
        return raison;
    }

    public void setRaison(String raison) {
        this.raison = raison;
    }

    public String getDevise() {
        return devise;
    }

    public void setDevise(String devise) {
        this.devise = devise;
    }

    public int getIdProjet() {
        return idProjet;
    }

    public void setIdProjet(int idProjet) {
        this.idProjet = idProjet;
    }
}