package Models;

public class MonthlyInvestmentData {
    private String monthLabel;  // "Jan 2024"
    private double totalMontant;
    private int monthIndex;     // 1, 2, 3... for regression calculation

    public MonthlyInvestmentData(String monthLabel, double totalMontant, int monthIndex) {
        this.monthLabel = monthLabel;
        this.totalMontant = totalMontant;
        this.monthIndex = monthIndex;
    }

    public String getMonthLabel()  { return monthLabel; }
    public double getTotalMontant(){ return totalMontant; }
    public int getMonthIndex()     { return monthIndex; }
}