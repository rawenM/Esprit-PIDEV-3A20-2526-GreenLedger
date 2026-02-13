package Models;

public class CritereImpact {

    private int idCritere;
    private String nom;
    private int note;
    private String commentaireTechnique;
    private int idEvaluation;

    public CritereImpact() {}

    public CritereImpact(String nom, int note, String commentaireTechnique, int idEvaluation) {
        this.nom = nom;
        this.note = note;
        this.commentaireTechnique = commentaireTechnique;
        this.idEvaluation = idEvaluation;
    }

    public int getIdCritere() { return idCritere; }
    public void setIdCritere(int idCritere) { this.idCritere = idCritere; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public int getNote() { return note; }
    public void setNote(int note) { this.note = note; }

    public String getCommentaireTechnique() { return commentaireTechnique; }
    public void setCommentaireTechnique(String commentaireTechnique) { this.commentaireTechnique = commentaireTechnique; }

    public int getIdEvaluation() { return idEvaluation; }
    public void setIdEvaluation(int idEvaluation) { this.idEvaluation = idEvaluation; }
}
