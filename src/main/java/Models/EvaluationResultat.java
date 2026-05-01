package Models;

/**
 * Evaluation Resultat - Individual criteria scores
 * Links evaluation to specific criteria with scores and comments
 */
public class EvaluationResultat {
    
    private Integer idResultat;
    private Integer idEvaluation;
    private Integer idCritere;
    private Integer note; // 0-10
    private String commentaire;
    
    // Relationships
    private Evaluation evaluation;
    private CritereReference critere;
    
    public EvaluationResultat() {
    }
    
    // Getters and Setters
    public Integer getIdResultat() {
        return idResultat;
    }
    
    public void setIdResultat(Integer idResultat) {
        this.idResultat = idResultat;
    }
    
    public Integer getIdEvaluation() {
        return idEvaluation;
    }
    
    public void setIdEvaluation(Integer idEvaluation) {
        this.idEvaluation = idEvaluation;
    }
    
    public Integer getIdCritere() {
        return idCritere;
    }
    
    public void setIdCritere(Integer idCritere) {
        this.idCritere = idCritere;
    }
    
    public Integer getNote() {
        return note;
    }
    
    public void setNote(Integer note) {
        this.note = note;
    }
    
    public String getCommentaire() {
        return commentaire;
    }
    
    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }
    
    public Evaluation getEvaluation() {
        return evaluation;
    }
    
    public void setEvaluation(Evaluation evaluation) {
        this.evaluation = evaluation;
    }
    
    public CritereReference getCritere() {
        return critere;
    }
    
    public void setCritere(CritereReference critere) {
        this.critere = critere;
    }
    
    @Override
    public String toString() {
        return String.format("EvaluationResultat[id=%d, evaluation=%d, critere=%d, note=%d]",
                idResultat, idEvaluation, idCritere, note);
    }
}
