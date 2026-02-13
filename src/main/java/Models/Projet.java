package Models;

public class Projet {

    private int id;
    private String titre;
    private String description;
    private double budget;
    private double scoreEsg;
    private String statutEvaluation;

    public Projet() {}

    public Projet(int id, String titre, String description, double budget, double scoreEsg, String statutEvaluation) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.budget = budget;
        this.scoreEsg = scoreEsg;
        this.statutEvaluation = statutEvaluation;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getBudget() { return budget; }
    public void setBudget(double budget) { this.budget = budget; }

    public double getScoreEsg() { return scoreEsg; }
    public void setScoreEsg(double scoreEsg) { this.scoreEsg = scoreEsg; }

    public String getStatutEvaluation() { return statutEvaluation; }
    public void setStatutEvaluation(String statutEvaluation) { this.statutEvaluation = statutEvaluation; }
}
