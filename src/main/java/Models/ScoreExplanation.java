package Models;

public class ScoreExplanation {
    private int idCritere;
    private String nomCritere;
    private int poids;
    private int note;
    private double contribution;

    public ScoreExplanation() {
    }

    public ScoreExplanation(int idCritere, String nomCritere, int poids, int note, double contribution) {
        this.idCritere = idCritere;
        this.nomCritere = nomCritere;
        this.poids = poids;
        this.note = note;
        this.contribution = contribution;
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

    public int getPoids() {
        return poids;
    }

    public void setPoids(int poids) {
        this.poids = poids;
    }

    public int getNote() {
        return note;
    }

    public void setNote(int note) {
        this.note = note;
    }

    public double getContribution() {
        return contribution;
    }

    public void setContribution(double contribution) {
        this.contribution = contribution;
    }
}
