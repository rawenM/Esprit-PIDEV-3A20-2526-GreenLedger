package Models;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Represents a historical snapshot of carbon credit pricing data
 * from Climate Impact X API at a specific point in time
 */
public class CarbonPriceSnapshot {
    private int id;
    private String creditType;  // VOLUNTARY_CARBON_MARKET, COMPLIANCE, etc.
    private double usdPerTon;   // Price per metric ton CO2e
    private String marketIndex; // CIX index reference
    private String sourceApi;   // API provider (CLIMATE_IMPACT_X, etc.)
    private Timestamp timestamp; // When this price was recorded

    // Constructors
    public CarbonPriceSnapshot() {}

    public CarbonPriceSnapshot(String creditType, double usdPerTon, String marketIndex) {
        this.creditType = creditType;
        this.usdPerTon = usdPerTon;
        this.marketIndex = marketIndex;
        this.sourceApi = "CLIMATE_IMPACT_X";
        this.timestamp = new Timestamp(System.currentTimeMillis());
    }

    public CarbonPriceSnapshot(String creditType, double usdPerTon, String marketIndex,
                             String sourceApi, Timestamp timestamp) {
        this.creditType = creditType;
        this.usdPerTon = usdPerTon;
        this.marketIndex = marketIndex;
        this.sourceApi = sourceApi;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCreditType() {
        return creditType;
    }

    public void setCreditType(String creditType) {
        this.creditType = creditType;
    }

    public double getUsdPerTon() {
        return usdPerTon;
    }

    public void setUsdPerTon(double usdPerTon) {
        this.usdPerTon = usdPerTon;
    }

    public String getMarketIndex() {
        return marketIndex;
    }

    public void setMarketIndex(String marketIndex) {
        this.marketIndex = marketIndex;
    }

    public String getSourceApi() {
        return sourceApi;
    }

    public void setSourceApi(String sourceApi) {
        this.sourceApi = sourceApi;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    // Utility methods
    public double calculateUSDValue(double tons) {
        return tons * usdPerTon;
    }

    public double calculateTonsFromUSD(double usdAmount) {
        return usdAmount / usdPerTon;
    }

    @Override
    public String toString() {
        return "CarbonPriceSnapshot{" +
                "id=" + id +
                ", creditType='" + creditType + '\'' +
                ", usdPerTon=" + usdPerTon +
                ", marketIndex='" + marketIndex + '\'' +
                ", sourceApi='" + sourceApi + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
