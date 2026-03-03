package Models;

public class EvaluationResult {

    private int idCritere;
    private String nomCritere;
    private int note;
    private String commentaireExpert;
    private boolean estRespecte;

    public EvaluationResult() {}

    public EvaluationResult(int idCritere, String nomCritere, int note, String commentaireExpert) {
        this.idCritere = idCritere;
        this.nomCritere = nomCritere;
        this.note = note;
        this.commentaireExpert = commentaireExpert;
    }

    public EvaluationResult(int idCritere, String nomCritere, int note, String commentaireExpert, boolean estRespecte) {
        this.idCritere = idCritere;
        this.nomCritere = nomCritere;
        this.note = note;
        this.commentaireExpert = commentaireExpert;
        this.estRespecte = estRespecte;
    }

    public int getIdCritere() { return idCritere; }
    public void setIdCritere(int idCritere) { this.idCritere = idCritere; }

    public String getNomCritere() { return nomCritere; }
    public void setNomCritere(String nomCritere) { this.nomCritere = nomCritere; }

    public int getNote() { return note; }
    public void setNote(int note) { this.note = note; }

    public String getCommentaireExpert() { return commentaireExpert; }
    public void setCommentaireExpert(String commentaireExpert) { this.commentaireExpert = commentaireExpert; }

    public boolean isEstRespecte() { return estRespecte; }
    public void setEstRespecte(boolean estRespecte) { this.estRespecte = estRespecte; }
}
