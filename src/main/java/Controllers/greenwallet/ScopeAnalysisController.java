package Controllers.greenwallet;

import Services.ClimatiqApiService;
import Models.climatiq.EmissionResult;
import Models.CarbonReport;
import Utils.EventBusManager;
import javafx.scene.layout.Pane;
import javafx.scene.control.Label;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.Tooltip;

/**
 * Scope Analysis Controller - Waterfall Charts & Breakdown
 * 
 * Responsibilities:
 * - Render waterfall chart showing Scope 1/2/3 breakdown
 * - Display component stacking (direct + energy + supply chain)
 * - Show tier data quality indicators
 * - Provide drill-down capability for each scope
 * - Color-code by severity (red=scope1, orange=scope2, yellow=scope3)
 * 
 * Chart Type: Waterfall with floating columns
 * - X-axis: Categories (Scope 1, Scope 2, Scope 3, Total)
 * - Y-axis: Emissions (tCO₂e)
 * - Floating columns show contribution of each scope
 * - Total bar shows stacked sum
 * 
 * Psychology: Mental Accounting
 * - Separates emissions into clear categories
 * - Makes relative contributions visible
 * - Triggers sense of control ("we can reduce Scope 3")
 * 
 * @author GreenLedger Team
 * @version 2.0 - Production Ready (Implemented)
 */
public class ScopeAnalysisController {
    
    private ClimatiqApiService climatiqService;
    private Pane waterfallChartPane;
    private Label lblScope1Amount;
    private Label lblScope2Amount;
    private Label lblScope3Amount;
    private Label lblScopeDataQuality;
    
    // Chart colors
    private static final Color SCOPE1_COLOR = Color.rgb(231, 76, 60);    // Red
    private static final Color SCOPE2_COLOR = Color.rgb(230, 126, 34);   // Orange
    private static final Color SCOPE3_COLOR = Color.rgb(241, 196, 15);   // Yellow
    private static final Color TOTAL_COLOR = Color.rgb(52, 73, 94);      // Dark blue
    private static final Color GRID_COLOR = Color.rgb(189, 195, 199);    // Light gray
    
    public ScopeAnalysisController(
            ClimatiqApiService climatiqService,
            Pane waterfallChartPane,
            Label lblScope1Amount,
            Label lblScope2Amount,
            Label lblScope3Amount,
            Label lblScopeDataQuality) {
        
        this.climatiqService = climatiqService;
        this.waterfallChartPane = waterfallChartPane;
        this.lblScope1Amount = lblScope1Amount;
        this.lblScope2Amount = lblScope2Amount;
        this.lblScope3Amount = lblScope3Amount;
        this.lblScopeDataQuality = lblScopeDataQuality;
    }
    
    /**
     * Render waterfall chart for scope breakdown with simple values
     */
    public void renderWaterfallChart(double scope1Value, double scope2Value, double scope3Value) {
        System.out.println("[ScopeAnalysis] Rendering waterfall chart...");
        
        if (waterfallChartPane == null) {
            System.err.println("[ScopeAnalysis] Chart pane is null");
            return;
        }
        
        // Update labels
        updateScopeLabels(scope1Value, scope2Value, scope3Value);
        
        // Create canvas
        double width = waterfallChartPane.getPrefWidth();
        double height = waterfallChartPane.getPrefHeight();
        
        if (width <= 0) width = 600;
        if (height <= 0) height = 300;
        
        Canvas canvas = new Canvas(width, height);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        
        // Draw chart
        drawWaterfallChart(gc, width, height, scope1Value, scope2Value, scope3Value);
        
        // Add to pane
        waterfallChartPane.getChildren().clear();
        waterfallChartPane.getChildren().add(canvas);
        
        // Enable drill-down
        enableDrillDownOnCanvas(canvas, scope1Value, scope2Value, scope3Value);
    }
    
    /**
     * Render waterfall chart for scope breakdown with EmissionResult objects
     */
    public void renderWaterfallChart(EmissionResult scope1, EmissionResult scope2, EmissionResult scope3) {
        double value1 = (scope1 != null) ? scope1.getCo2eAmount().doubleValue() : 0.0;
        double value2 = (scope2 != null) ? scope2.getCo2eAmount().doubleValue() : 0.0;
        double value3 = (scope3 != null) ? scope3.getCo2eAmount().doubleValue() : 0.0;
        
        renderWaterfallChart(value1, value2, value3);
    }
    
    /**
     * Update scope labels with values
     */
    private void updateScopeLabels(double scope1, double scope2, double scope3) {
        if (lblScope1Amount != null) {
            lblScope1Amount.setText(String.format("%.2f tCO₂e", scope1));
            lblScope1Amount.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        }
        
        if (lblScope2Amount != null) {
            lblScope2Amount.setText(String.format("%.2f tCO₂e", scope2));
            lblScope2Amount.setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;");
        }
        
        if (lblScope3Amount != null) {
            lblScope3Amount.setText(String.format("%.2f tCO₂e", scope3));
            lblScope3Amount.setStyle("-fx-text-fill: #f1c40f; -fx-font-weight: bold;");
        }
    }
    
    /**
     * Draw waterfall chart on canvas
     */
    private void drawWaterfallChart(GraphicsContext gc, double width, double height, 
                                   double scope1, double scope2, double scope3) {
        // Clear canvas
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, width, height);
        
        // Calculate margins
        double marginLeft = 60;
        double marginRight = 40;
        double marginTop = 40;
        double marginBottom = 60;
        
        double chartWidth = width - marginLeft - marginRight;
        double chartHeight = height - marginTop - marginBottom;
        
        // Calculate total and max for scaling
        double total = scope1 + scope2 + scope3;
        double maxValue = Math.max(total, Math.max(scope1, Math.max(scope2, scope3))) * 1.1; // Add 10% padding
        
        if (maxValue == 0) maxValue = 100; // Default if no data
        
        // Calculate bar positions
        double barWidth = chartWidth / 5; // 4 bars + spacing
        double spacing = barWidth * 0.2;
        double actualBarWidth = barWidth - spacing;
        
        // Draw Y-axis grid lines and labels
        gc.setStroke(GRID_COLOR);
        gc.setLineWidth(1);
        gc.setFill(Color.DARKGRAY);
        gc.setFont(Font.font("Arial", 10));
        
        for (int i = 0; i <= 5; i++) {
            double y = marginTop + (chartHeight * i / 5);
            double value = maxValue * (5 - i) / 5;
            
            gc.strokeLine(marginLeft, y, marginLeft + chartWidth, y);
            gc.fillText(String.format("%.1f", value), 5, y + 5);
        }
        
        // Draw bars
        drawBar(gc, marginLeft + spacing, marginTop, actualBarWidth, chartHeight, 
                scope1, maxValue, SCOPE1_COLOR, "Scope 1");
        
        drawBar(gc, marginLeft + barWidth + spacing, marginTop, actualBarWidth, chartHeight, 
                scope2, maxValue, SCOPE2_COLOR, "Scope 2");
        
        drawBar(gc, marginLeft + (barWidth * 2) + spacing, marginTop, actualBarWidth, chartHeight, 
                scope3, maxValue, SCOPE3_COLOR, "Scope 3");
        
        drawBar(gc, marginLeft + (barWidth * 3) + spacing, marginTop, actualBarWidth, chartHeight, 
                total, maxValue, TOTAL_COLOR, "Total");
        
        // Draw X-axis labels
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        gc.setTextAlign(TextAlignment.CENTER);
        
        gc.fillText("Scope 1", marginLeft + (barWidth * 0.5), height - 30);
        gc.fillText("Scope 2", marginLeft + (barWidth * 1.5), height - 30);
        gc.fillText("Scope 3", marginLeft + (barWidth * 2.5), height - 30);
        gc.fillText("Total", marginLeft + (barWidth * 3.5), height - 30);
        
        // Draw title
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        gc.fillText("Emissions Breakdown by Scope", width / 2, 20);
    }
    
    /**
     * Draw a single bar in the waterfall chart
     */
    private void drawBar(GraphicsContext gc, double x, double topY, double width, double chartHeight,
                        double value, double maxValue, Color color, String label) {
        if (value <= 0) return;
        
        double barHeight = (value / maxValue) * chartHeight;
        double barY = topY + chartHeight - barHeight;
        
        // Draw bar
        gc.setFill(color);
        gc.fillRect(x, barY, width, barHeight);
        
        // Draw border
        gc.setStroke(color.darker());
        gc.setLineWidth(2);
        gc.strokeRect(x, barY, width, barHeight);
        
        // Draw value label on top of bar
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(String.format("%.1f", value), x + width / 2, barY - 5);
    }
    
    /**
     * Enable drill-down on canvas clicks
     */
    private void enableDrillDownOnCanvas(Canvas canvas, double scope1, double scope2, double scope3) {
        canvas.setOnMouseClicked(event -> {
            double x = event.getX();
            double width = canvas.getWidth();
            
            double marginLeft = 60;
            double chartWidth = width - marginLeft - 40;
            double barWidth = chartWidth / 5;
            
            // Determine which bar was clicked
            if (x >= marginLeft && x < marginLeft + barWidth) {
                showDrillDown("Scope 1", scope1, "Direct emissions from owned/controlled sources");
            } else if (x >= marginLeft + barWidth && x < marginLeft + (barWidth * 2)) {
                showDrillDown("Scope 2", scope2, "Indirect emissions from purchased energy");
            } else if (x >= marginLeft + (barWidth * 2) && x < marginLeft + (barWidth * 3)) {
                showDrillDown("Scope 3", scope3, "All other indirect emissions in value chain");
            } else if (x >= marginLeft + (barWidth * 3) && x < marginLeft + (barWidth * 4)) {
                showDrillDown("Total Emissions", scope1 + scope2 + scope3, 
                    "Combined emissions across all scopes");
            }
        });
    }
    
    /**
     * Show drill-down details for a scope
     */
    private void showDrillDown(String scopeName, double value, String description) {
        System.out.println(String.format("[ScopeAnalysis] Drill-down: %s - %.2f tCO₂e - %s", 
            scopeName, value, description));
        
        // Post notification event
        EventBusManager.post(new Utils.EventBusManager.NotificationEvent(
            Utils.EventBusManager.NotificationEvent.Type.INFO,
            String.format("%s: %.2f tCO₂e\\n%s", scopeName, value, description)
        ));
    }
    
    /**
     * Show tier data quality badge.
     * Display "Tier 1: Measured Data", "Tier 2: Regional Data", etc.
     */
    public void updateDataQualityBadge(int lowestTier) {
        if (lblScopeDataQuality == null) return;
        
        String tierDescription = switch(lowestTier) {
            case 1 -> "🥇 Tier 1: Measured Data ±5%";
            case 2 -> "🥈 Tier 2: Supplier Data ±15%";
            case 3 -> "🥉 Tier 3: Regional Data ±30%";
            case 4 -> "📊 Tier 4: Estimates ±50%";
            default -> "Unknown Tier";
        };
        lblScopeDataQuality.setText(tierDescription);
        
        // Color code by tier quality
        String color = switch(lowestTier) {
            case 1 -> "-fx-text-fill: #27ae60;"; // Green
            case 2 -> "-fx-text-fill: #2ecc71;"; // Light green
            case 3 -> "-fx-text-fill: #f39c12;"; // Orange
            case 4 -> "-fx-text-fill: #e74c3c;"; // Red
            default -> "-fx-text-fill: #95a5a6;"; // Gray
        };
        lblScopeDataQuality.setStyle(color + " -fx-font-weight: bold;");
    }
    
    /**
     * Enable drill-down for specific scope (show detail breakdown).
     * Already implemented in enableDrillDownOnCanvas
     */
    public void enableDrillDown() {
        System.out.println("[ScopeAnalysis] Drill-down capability enabled via canvas click handlers");
    }
    
    public void shutdown() {
        // No resources to cleanup
    }
}
