package Models;

public class ProjectMLData {

    private String projectType;
    private int durationMonths;
    private double requestedAmount;
    private double co2Reduction;
    private double roiExpected;
    private int maturityLevel;
    private int innovationScore;
    private double totalBudget;

    // Constructor
    public ProjectMLData(String projectType, int durationMonths,
                         double requestedAmount, double co2Reduction,
                         double roiExpected, int maturityLevel,
                         int innovationScore, double totalBudget) {

        this.projectType = projectType;
        this.durationMonths = durationMonths;
        this.requestedAmount = requestedAmount;
        this.co2Reduction = co2Reduction;
        this.roiExpected = roiExpected;
        this.maturityLevel = maturityLevel;
        this.innovationScore = innovationScore;
        this.totalBudget = totalBudget;
    }

    // Getters here
}