package Controllers;

import Models.MonthlyInvestmentData;
import Services.InvestmentAnalyticsService;
import Services.InvestmentAnalyticsService.ProjectionPoint;
import Services.InvestmentAnalyticsService.SummaryStats;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;

import java.io.IOException;
import java.util.List;

/**
 * InvestmentDashboardController
 * ------------------------------
 * Drives the monthly investment timeline dashboard.
 *
 * WHAT THIS CONTROLLER DOES:
 *   1. Fetches monthly investment totals from the DB
 *   2. Calls linear regression to compute trend coefficients a and b
 *   3. Builds three chart series:
 *        - Series 1: actual monthly data (blue)
 *        - Series 2: regression trend line (green)
 *        - Series 3: 3-month future projection (red)
 *   4. Populates KPI cards with summary statistics
 *   5. Fills the detail table with per-month breakdown
 *   6. Displays the regression formula y = a + b*x
 */
public class InvestmentDashboardController extends BaseController {

    // ── Chart ─────────────────────────────────────────────────────
    @FXML private LineChart<String, Number> chartInvestments;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;

    // ── KPI Cards ─────────────────────────────────────────────────
    @FXML private Label lblTotalInvesti;
    @FXML private Label lblMoyenneMensuelle;
    @FXML private Label lblMeilleurMois;
    @FXML private Label lblTendance;
    @FXML private Label lblRegressionFormula;

    // ── Regression Detail ─────────────────────────────────────────
    @FXML private Label lblFormuleComplete;
    @FXML private Label lblSlope;
    @FXML private Label lblIntercept;
    @FXML private Label lblProjection3;

    // ── Detail Table ──────────────────────────────────────────────
    @FXML private TableView<MonthlyRowData> tableMonthlyData;
    @FXML private TableColumn<MonthlyRowData, String> colMois;
    @FXML private TableColumn<MonthlyRowData, Double> colMontant;
    @FXML private TableColumn<MonthlyRowData, Double> colTendance;
    @FXML private TableColumn<MonthlyRowData, Double> colEcart;
    @FXML private TableColumn<MonthlyRowData, String> colStatut;

    // ── Service ───────────────────────────────────────────────────
    private final InvestmentAnalyticsService analyticsService = new InvestmentAnalyticsService();

    // ── Table data ────────────────────────────────────────────────
    private final ObservableList<MonthlyRowData> tableData = FXCollections.observableArrayList();

    // ─────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        //setupTable();
        loadDashboard();
    }

    // ─────────────────────────────────────────────────────────────
    // Setup table columns
    // ─────────────────────────────────────────────────────────────
    /*private void setupTable() {
        colMois.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().month));
        colMontant.setCellValueFactory(cd ->
                new SimpleDoubleProperty(cd.getValue().actual).asObject());
        colTendance.setCellValueFactory(cd ->
                new SimpleDoubleProperty(cd.getValue().trend).asObject());
        colEcart.setCellValueFactory(cd ->
                new SimpleDoubleProperty(cd.getValue().gap).asObject());
        colStatut.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().status));

        // Format numbers to 2 decimal places
        colMontant.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : String.format("%,.2f", item));
            }
        });
        colTendance.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : String.format("%,.2f", item));
            }
        });
        colEcart.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(""); return; }
                setText(String.format("%+,.2f", item));
                // Color: green if above trend, red if below
                setStyle(item >= 0
                        ? "-fx-text-fill: #065f46; -fx-font-weight: bold;"
                        : "-fx-text-fill: #991b1b; -fx-font-weight: bold;");
            }
        });
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(""); return; }
                setText(item);
                switch (item) {
                    case "Au-dessus"  -> setStyle("-fx-text-fill: #065f46; -fx-font-weight:bold;");
                    case "En-dessous" -> setStyle("-fx-text-fill: #991b1b; -fx-font-weight:bold;");
                    default           -> setStyle("-fx-text-fill: #64748b;");
                }
            }
        });

        tableMonthlyData.setItems(tableData);
    }*/

    // ─────────────────────────────────────────────────────────────
    // Main method — orchestrates all data loading and chart building
    // ─────────────────────────────────────────────────────────────
    private void loadDashboard() {
        // Step 1: Get monthly totals from DB
        List<MonthlyInvestmentData> monthlyData = analyticsService.getMonthlyTotals();

        if (monthlyData.isEmpty()) {
            lblTotalInvesti.setText("Aucune donnée");
            lblMoyenneMensuelle.setText("—");
            lblMeilleurMois.setText("—");
            lblTendance.setText("—");
            return;
        }

        // Step 2: Compute linear regression → returns [a, b]
        double[] regression = analyticsService.computeLinearRegression(monthlyData);
        double a = regression[0]; // intercept
        double b = regression[1]; // slope

        // Step 3: Get trend values for each existing month
        List<Double> trendValues = analyticsService.getTrendValues(monthlyData, regression);

        // Step 4: Get 3-month projection
        List<ProjectionPoint> projections = analyticsService.getProjection(monthlyData, regression);

        // Step 5: Get summary stats
        SummaryStats stats = analyticsService.getSummaryStats(monthlyData);

        // Step 6: Build the chart
        buildChart(monthlyData, trendValues, projections);

        // Step 7: Update KPI cards
        updateKpiCards(stats, b, projections);

        // Step 8: Update regression detail panel
        updateRegressionPanel(a, b, projections);

        // Step 9: Fill detail table
        buildDetailTable(monthlyData, trendValues);
    }

    // ─────────────────────────────────────────────────────────────
    // Builds the LineChart with three series
    // ─────────────────────────────────────────────────────────────
    private void buildChart(List<MonthlyInvestmentData> data,
                            List<Double> trendValues,
                            List<ProjectionPoint> projections) {

        chartInvestments.getData().clear();

        // ── Series 1: Actual monthly data (blue) ──────────────────
        XYChart.Series<String, Number> actualSeries = new XYChart.Series<>();
        actualSeries.setName("Données réelles");
        for (MonthlyInvestmentData d : data) {
            actualSeries.getData().add(
                    new XYChart.Data<>(d.getMonthLabel(), d.getTotalMontant()));
        }

        // ── Series 2: Trend line (green) ──────────────────────────
        XYChart.Series<String, Number> trendSeries = new XYChart.Series<>();
        trendSeries.setName("Tendance (régression linéaire)");
        for (int i = 0; i < data.size(); i++) {
            trendSeries.getData().add(
                    new XYChart.Data<>(data.get(i).getMonthLabel(), trendValues.get(i)));
        }

        // Connect trend line to projections smoothly
        // Add first projection point to trend series as bridge
        if (!projections.isEmpty()) {
            ProjectionPoint first = projections.get(0);
            trendSeries.getData().add(
                    new XYChart.Data<>(first.label, first.value));
        }

        // ── Series 3: Projection (red dotted) ─────────────────────
        XYChart.Series<String, Number> projectionSeries = new XYChart.Series<>();
        projectionSeries.setName("Projection 3 mois");

        // Start projection from last actual point for visual continuity
        MonthlyInvestmentData lastActual = data.get(data.size() - 1);
        projectionSeries.getData().add(
                new XYChart.Data<>(lastActual.getMonthLabel(), lastActual.getTotalMontant()));

        for (ProjectionPoint p : projections) {
            projectionSeries.getData().add(new XYChart.Data<>(p.label, p.value));
        }

        // Add all series to chart
        chartInvestments.getData().addAll(actualSeries, trendSeries, projectionSeries);

        // Apply colors via inline CSS after data is added
        javafx.application.Platform.runLater(() -> {
            // Blue for actual
            if (actualSeries.getNode() != null)
                actualSeries.getNode().setStyle("-fx-stroke: #3b82f6; -fx-stroke-width: 2.5px;");

            // Green for trend
            if (trendSeries.getNode() != null)
                trendSeries.getNode().setStyle("-fx-stroke: #10b981; -fx-stroke-width: 2px; -fx-stroke-dash-array: 8 4;");

            // Red for projection
            if (projectionSeries.getNode() != null)
                projectionSeries.getNode().setStyle("-fx-stroke: #ef4444; -fx-stroke-width: 2px; -fx-stroke-dash-array: 6 6;");

            // Style the data points
            for (XYChart.Data<String, Number> d : actualSeries.getData()) {
                if (d.getNode() != null)
                    d.getNode().setStyle("-fx-background-color: #3b82f6; -fx-background-radius: 4px;");
            }
            for (XYChart.Data<String, Number> d : projectionSeries.getData()) {
                if (d.getNode() != null)
                    d.getNode().setStyle("-fx-background-color: #ef4444; -fx-background-radius: 4px;");
            }
        });
    }

    // ─────────────────────────────────────────────────────────────
    // Updates the 4 KPI cards at the top
    // ─────────────────────────────────────────────────────────────
    private void updateKpiCards(SummaryStats stats, double slope,
                                List<ProjectionPoint> projections) {

        lblTotalInvesti.setText(String.format("%,.0f TND", stats.total));
        lblMoyenneMensuelle.setText(String.format("%,.0f TND", stats.average));
        lblMeilleurMois.setText(String.format("%,.0f TND", stats.max));

        // Trend direction based on regression slope
        if (slope > 50) {
            lblTendance.setText("📈 En hausse");
            lblTendance.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:#065f46;");
        } else if (slope < -50) {
            lblTendance.setText("📉 En baisse");
            lblTendance.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:#991b1b;");
        } else {
            lblTendance.setText("➡️ Stable");
            lblTendance.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:#92400e;");
        }

        // Show regression formula in header
        lblRegressionFormula.setText(
                String.format("y = %.2f + %.2f·x", slope, slope));
    }

    // ─────────────────────────────────────────────────────────────
    // Updates the regression detail panel
    // ─────────────────────────────────────────────────────────────
    private void updateRegressionPanel(double a, double b,
                                       List<ProjectionPoint> projections) {

        lblFormuleComplete.setText(String.format("y = %.2f + %.2f · x", a, b));
        lblSlope.setText(String.format("%.2f TND / mois", b));
        lblIntercept.setText(String.format("%.2f TND", a));

        // Show M+3 projection
        if (projections.size() >= 3) {
            lblProjection3.setText(
                    String.format("%,.0f TND (%s)",
                            projections.get(2).value,
                            projections.get(2).label));
        }

        lblRegressionFormula.setText(String.format("y = %.2f + %.2f·x", a, b));
    }

    // ─────────────────────────────────────────────────────────────
    // Fills the detail table
    // ─────────────────────────────────────────────────────────────
    private void buildDetailTable(List<MonthlyInvestmentData> data,
                                  List<Double> trendValues) {
        tableData.clear();
        for (int i = 0; i < data.size(); i++) {
            MonthlyInvestmentData d = data.get(i);
            double actual = d.getTotalMontant();
            double trend  = trendValues.get(i);
            double gap    = actual - trend;
            String status = actual == 0 ? "Vide"
                    : gap >= 0    ? "Au-dessus"
                    : "En-dessous";
            tableData.add(new MonthlyRowData(d.getMonthLabel(), actual, trend, gap, status));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Navigation
    // ─────────────────────────────────────────────────────────────
    @FXML private void handleRefresh() { loadDashboard(); }

    @FXML
    private void handleGoFinancement() {
        try { org.GreenLedger.MainFX.setRoot("financement"); }
        catch (IOException e) { System.err.println("Nav error: " + e.getMessage()); }
    }

    @FXML
    private void handleGoInvestments() {
        try { org.GreenLedger.MainFX.setRoot("fxml/investor_financing"); }
        catch (IOException e) { System.err.println("Nav error: " + e.getMessage()); }
    }

    @FXML
    private void handleGoRiskAgent() {
        try { org.GreenLedger.MainFX.setRoot("fxml/finance_risk_agent"); }
        catch (IOException e) { System.err.println("Nav error: " + e.getMessage()); }
    }

    // ─────────────────────────────────────────────────────────────
    // Inner class for table rows
    // ─────────────────────────────────────────────────────────────
    public static class MonthlyRowData {
        public final String month;
        public final double actual;
        public final double trend;
        public final double gap;
        public final String status;

        public MonthlyRowData(String month, double actual,
                              double trend, double gap, String status) {
            this.month  = month;
            this.actual = actual;
            this.trend  = trend;
            this.gap    = gap;
            this.status = status;
        }
    }
}
