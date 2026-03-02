package Services;

import Models.Financement;
import Models.MonthlyInvestmentData;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * InvestmentAnalyticsService
 * --------------------------
 * Computes monthly investment totals, linear regression trend,
 * and 3-month future projections from financement data.
 *
 * ALGORITHMS USED:
 *   1. Monthly aggregation — groups financement records by year/month
 *   2. Simple Linear Regression — computes best-fit line through monthly totals
 *      Formula: y = a + b*x
 *      where:
 *        b = (n*Σxy - Σx*Σy) / (n*Σx² - (Σx)²)
 *        a = (Σy - b*Σx) / n
 *   3. Projection — extends the regression line 3 months into the future
 *
 * CALLED FROM: DashboardController
 */
public class InvestmentAnalyticsService {

    private final FinancementService financementService = new FinancementService();

    // ─────────────────────────────────────────────────────────────
    // STEP 1: Fetch and group financement records by month
    // Returns a sorted list of MonthlyInvestmentData
    // Each entry = one month with its total montant
    // ─────────────────────────────────────────────────────────────
    public List<MonthlyInvestmentData> getMonthlyTotals() {
        List<Financement> all = financementService.getAll();
        if (all == null || all.isEmpty()) return new ArrayList<>();

        // Group by YearMonth — sum montant for each month
        Map<YearMonth, Double> grouped = new TreeMap<>();

        DateTimeFormatter[] parsers = {
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        };

        for (Financement f : all) {
            String dateStr = f.getDateFinancement();
            if (dateStr == null || dateStr.isBlank()) continue;

            LocalDate date = null;
            for (DateTimeFormatter fmt : parsers) {
                try {
                    // Try parsing as LocalDateTime first, then LocalDate
                    try {
                        date = java.time.LocalDateTime.parse(dateStr, fmt).toLocalDate();
                    } catch (Exception e) {
                        date = LocalDate.parse(dateStr, fmt);
                    }
                    break;
                } catch (Exception ignored) {}
            }

            if (date == null) continue;

            YearMonth ym = YearMonth.from(date);
            grouped.merge(ym, f.getMontant(), Double::sum);
        }

        if (grouped.isEmpty()) return new ArrayList<>();

        // Fill gaps — if a month has no investment, add it with 0
        // This makes the chart continuous with no missing months
        YearMonth first = grouped.keySet().iterator().next();
        YearMonth last  = grouped.keySet().stream().reduce((a, b) -> b).orElse(first);

        List<MonthlyInvestmentData> result = new ArrayList<>();
        int index = 1;
        YearMonth current = first;

        while (!current.isAfter(last)) {
            String label = current.format(DateTimeFormatter.ofPattern("MMM yyyy"));
            double total = grouped.getOrDefault(current, 0.0);
            result.add(new MonthlyInvestmentData(label, total, index));
            index++;
            current = current.plusMonths(1);
        }

        return result;
    }

    // ─────────────────────────────────────────────────────────────
    // STEP 2: Compute linear regression coefficients
    // Takes the monthly data and returns [a, b] where y = a + b*x
    //
    // This is the core algorithm:
    //   x = month index (1, 2, 3, ...)
    //   y = total montant for that month
    //
    // b = (n*Σxy - Σx*Σy) / (n*Σx² - (Σx)²)
    // a = (Σy - b*Σx) / n
    // ─────────────────────────────────────────────────────────────
    public double[] computeLinearRegression(List<MonthlyInvestmentData> data) {
        int n = data.size();
        if (n < 2) return new double[]{0, 0};

        double sumX  = 0, sumY  = 0;
        double sumXY = 0, sumX2 = 0;

        for (MonthlyInvestmentData d : data) {
            double x = d.getMonthIndex();
            double y = d.getTotalMontant();
            sumX  += x;
            sumY  += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        // Compute slope (b) and intercept (a)
        double denominator = (n * sumX2) - (sumX * sumX);
        if (denominator == 0) return new double[]{sumY / n, 0};

        double b = ((n * sumXY) - (sumX * sumY)) / denominator;
        double a = (sumY - (b * sumX)) / n;

        System.out.printf("Linear Regression: y = %.2f + %.2f*x%n", a, b);
        return new double[]{a, b};
    }

    // ─────────────────────────────────────────────────────────────
    // STEP 3: Generate trend line values for existing months
    // Returns predicted y value for each existing month index
    // using the regression formula y = a + b*x
    // ─────────────────────────────────────────────────────────────
    public List<Double> getTrendValues(List<MonthlyInvestmentData> data, double[] regression) {
        double a = regression[0];
        double b = regression[1];

        List<Double> trend = new ArrayList<>();
        for (MonthlyInvestmentData d : data) {
            double predicted = a + b * d.getMonthIndex();
            trend.add(Math.max(0, predicted)); // never negative
        }
        return trend;
    }

    // ─────────────────────────────────────────────────────────────
    // STEP 4: Generate 3-month future projection
    // Extends the regression line beyond the last known month
    // Returns a list of ProjectionPoint (label + projected value)
    // ─────────────────────────────────────────────────────────────
    public List<ProjectionPoint> getProjection(
            List<MonthlyInvestmentData> data, double[] regression) {

        if (data.isEmpty()) return new ArrayList<>();

        double a = regression[0];
        double b = regression[1];

        // Find the last month in the dataset
        MonthlyInvestmentData last = data.get(data.size() - 1);
        int lastIndex = last.getMonthIndex();

        // Parse the last month label back to YearMonth to get correct future labels
        YearMonth lastYM = YearMonth.parse(
                last.getMonthLabel(),
                DateTimeFormatter.ofPattern("MMM yyyy")
        );

        List<ProjectionPoint> projections = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            int futureIndex = lastIndex + i;
            double projectedValue = a + b * futureIndex;
            String futureLabel = lastYM.plusMonths(i)
                    .format(DateTimeFormatter.ofPattern("MMM yyyy"));

            projections.add(new ProjectionPoint(
                    futureLabel,
                    Math.max(0, projectedValue), // clamp to 0
                    futureIndex
            ));
        }

        return projections;
    }

    // ─────────────────────────────────────────────────────────────
    // STEP 5: Compute summary statistics for the KPI cards
    // ─────────────────────────────────────────────────────────────
    public SummaryStats getSummaryStats(List<MonthlyInvestmentData> data) {
        if (data.isEmpty()) return new SummaryStats(0, 0, 0, 0);

        double total    = data.stream().mapToDouble(MonthlyInvestmentData::getTotalMontant).sum();
        double average  = total / data.size();
        double max      = data.stream().mapToDouble(MonthlyInvestmentData::getTotalMontant).max().orElse(0);
        double min      = data.stream()
                .mapToDouble(MonthlyInvestmentData::getTotalMontant)
                .filter(v -> v > 0)
                .min().orElse(0);

        return new SummaryStats(total, average, max, min);
    }

    // ─────────────────────────────────────────────────────────────
    // Inner classes for clean data passing to the controller
    // ─────────────────────────────────────────────────────────────

    public static class ProjectionPoint {
        public final String label;
        public final double value;
        public final int index;

        public ProjectionPoint(String label, double value, int index) {
            this.label = label;
            this.value = value;
            this.index = index;
        }
    }

    public static class SummaryStats {
        public final double total;
        public final double average;
        public final double max;
        public final double min;

        public SummaryStats(double total, double average, double max, double min) {
            this.total   = total;
            this.average = average;
            this.max     = max;
            this.min     = min;
        }
    }
}