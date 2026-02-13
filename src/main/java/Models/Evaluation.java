package Models;

import java.sql.Timestamp;

public class Evaluation {

    private int idEvaluation;
    private Timestamp dateEvaluation;
    private String observations;
    private double scoreGlobal;
    private String decision;
    private int idProjet;
    private String titreProjet;

    public Evaluation() {}

    public Evaluation(String observations, double scoreGlobal, String decision, int idProjet) {
        this.observations = observations;
        this.scoreGlobal = scoreGlobal;
        this.decision = decision;
        this.idProjet = idProjet;
    }

    public int getIdEvaluation() { return idEvaluation; }
    public void setIdEvaluation(int idEvaluation) { this.idEvaluation = idEvaluation; }

    public Timestamp getDateEvaluation() { return dateEvaluation; }
    public void setDateEvaluation(Timestamp dateEvaluation) { this.dateEvaluation = dateEvaluation; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public double getScoreGlobal() { return scoreGlobal; }
    public void setScoreGlobal(double scoreGlobal) { this.scoreGlobal = scoreGlobal; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public int getIdProjet() { return idProjet; }
    public void setIdProjet(int idProjet) { this.idProjet = idProjet; }

    public String getTitreProjet() { return titreProjet; }
    public void setTitreProjet(String titreProjet) { this.titreProjet = titreProjet; }
}
