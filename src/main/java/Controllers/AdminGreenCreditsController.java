package Controllers;

import DataBase.MyConnection;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin Green Credits Dashboard Controller
 * Matches the PHP "Green Credit Dispatch Dashboard" design.
 */
public class AdminGreenCreditsController {

    // ── Tab buttons ──────────────────────────────────────────────────────────
    @FXML private Button btnTabDashboard;
    @FXML private Button btnTabDispatch;
    @FXML private Button btnTabAdvanced;

    // ── Header ───────────────────────────────────────────────────────────────
    @FXML private Label lblProjectCount;

    // ── KPI cards ────────────────────────────────────────────────────────────
    @FXML private Label lblBaseline;
    @FXML private Label lblActual;
    @FXML private Label lblAvoided;
    @FXML private Label lblDispatchable;

    // ── Eligibility breakdown ─────────────────────────────────────────────────
    @FXML private Label      lblEligibilityTotal;
    @FXML private ProgressBar pbEligible;
    @FXML private ProgressBar pbNeedsImprovement;
    @FXML private ProgressBar pbNotEligible;
    @FXML private ProgressBar pbOther;
    @FXML private Label lblEligibleCount;
    @FXML private Label lblNeedsImprovementCount;
    @FXML private Label lblNotEligibleCount;
    @FXML private Label lblOtherCount;

    // ── System health ─────────────────────────────────────────────────────────
    @FXML private VBox  systemHealthBox;
    @FXML private Label lblHealthStatus;

    // ── Projects table ────────────────────────────────────────────────────────
    @FXML private Label                        lblTableCount;
    @FXML private TextField                    txtProjectSearch;
    @FXML private ComboBox<String>             cmbStatusFilter;
    @FXML private ComboBox<String>             cmbEligibilityFilter;
    @FXML private CheckBox                     chkDispatchOnly;
    @FXML private TableView<ProjectRow>        tableProjects;
    @FXML private TableColumn<ProjectRow,String> colProject;
    @FXML private TableColumn<ProjectRow,String> colId;
    @FXML private TableColumn<ProjectRow,String> colStatus;
    @FXML private TableColumn<ProjectRow,String> colBaseline;
    @FXML private TableColumn<ProjectRow,String> colActual;
    @FXML private TableColumn<ProjectRow,String> colAvoided;
    @FXML private TableColumn<ProjectRow,String> colDispatchable;
    @FXML private TableColumn<ProjectRow,String> colEligibility;
    @FXML private TableColumn<ProjectRow,String> colActions;

    private final ObservableList<ProjectRow> allRows = FXCollections.observableArrayList();

    // ── Init ─────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        setupFilters();
        setupTable();
        loadData();
    }

    // ── Tab navigation ────────────────────────────────────────────────────────
    @FXML private void onTabDashboard() { setActiveTab(btnTabDashboard); }
    @FXML private void onTabDispatch()  { setActiveTab(btnTabDispatch);  }
    @FXML private void onTabAdvanced()  { setActiveTab(btnTabAdvanced);  }

    private void setActiveTab(Button active) {
        for (Button b : List.of(btnTabDashboard, btnTabDispatch, btnTabAdvanced)) {
            b.getStyleClass().removeAll("gl-tab-active");
            if (!b.getStyleClass().contains("gl-tab")) b.getStyleClass().add("gl-tab");
        }
        active.getStyleClass().add("gl-tab-active");
    }

    @FXML private void onAllProjects()     { cmbStatusFilter.setValue(null); applyFilters(); }
    @FXML private void onDispatchToWallet(){ showInfo("Dispatch", "Sélectionnez un projet dans le tableau pour dispatcher."); }
    @FXML private void onRetryHealth()     { checkSystemHealth(); }
    @FXML private void onRefresh()         { loadData(); }

    // ── Setup ─────────────────────────────────────────────────────────────────
    private void setupFilters() {
        cmbStatusFilter.setItems(FXCollections.observableArrayList(
            "All status", "DRAFT", "SUBMITTED", "APPROVED", "REJECTED", "IN_PROGRESS"));
        cmbEligibilityFilter.setItems(FXCollections.observableArrayList(
            "All eligibility", "ELIGIBLE", "NOT_ELIGIBLE", "NEEDS_IMPROVEMENT", "PENDING"));

        txtProjectSearch.textProperty().addListener((o, ov, nv) -> applyFilters());
        cmbStatusFilter.setOnAction(e -> applyFilters());
        cmbEligibilityFilter.setOnAction(e -> applyFilters());
        chkDispatchOnly.setOnAction(e -> applyFilters());
    }

    private void setupTable() {
        colProject.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().name));
        colId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().id));
        colBaseline.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().baseline));
        colActual.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().actual));
        colAvoided.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().avoided));
        colDispatchable.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().dispatchable));

        // Status badge
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                String st = getTableRow().getItem().status;
                Label badge = new Label(st);
                badge.getStyleClass().add(statusBadge(st));
                setGraphic(badge);
            }
        });
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status));

        // Eligibility badge
        colEligibility.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                String el = getTableRow().getItem().eligibility;
                Label badge = new Label(el.replace("_", " "));
                badge.getStyleClass().add(eligibilityBadge(el));
                setGraphic(badge);
            }
        });
        colEligibility.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().eligibility));

        // Actions
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnDispatch = new Button("Dispatch");
            { btnDispatch.getStyleClass().add("gl-btn-primary");
              btnDispatch.setStyle("-fx-font-size:11px; -fx-padding:4 10 4 10;");
              btnDispatch.setOnAction(e -> {
                  ProjectRow row = getTableView().getItems().get(getIndex());
                  dispatchProject(row);
              });
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                ProjectRow row = getTableRow().getItem();
                btnDispatch.setDisable(!"ELIGIBLE".equals(row.eligibility));
                HBox box = new HBox(btnDispatch);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });
        colActions.setCellValueFactory(c -> new SimpleStringProperty(""));

        tableProjects.setItems(allRows);
    }

    // ── Data loading ──────────────────────────────────────────────────────────
    private void loadData() {
        Platform.runLater(() -> {
            try {
                loadProjects();
                loadKpis();
                checkSystemHealth();
            } catch (Exception e) {
                System.err.println("[AdminGreenCredits] Load error: " + e.getMessage());
            }
        });
    }

    private void loadProjects() {
        allRows.clear();
        Connection conn = MyConnection.getConnection();
        if (conn == null) return;

        // Try to query projects with carbon data
        String sql = "SELECT p.id, p.titre, p.statut, " +
                     "COALESCE(p.baseline_tco2, 0) as baseline, " +
                     "COALESCE(p.actual_tco2, 0) as actual, " +
                     "COALESCE(p.avoided_tco2, 0) as avoided, " +
                     "COALESCE(p.dispatched_green_credits, 0) as dispatched " +
                     "FROM projet p ORDER BY p.id DESC LIMIT 100";

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            int total = 0, eligible = 0, needsImprovement = 0, notEligible = 0, other = 0;
            double sumBaseline = 0, sumActual = 0, sumAvoided = 0, sumDispatchable = 0;

            while (rs.next()) {
                total++;
                double baseline   = rs.getDouble("baseline");
                double actual     = rs.getDouble("actual");
                double avoided    = rs.getDouble("avoided");
                double dispatched = rs.getDouble("dispatched");
                String statut     = rs.getString("statut");

                sumBaseline    += baseline;
                sumActual      += actual;
                sumAvoided     += avoided;

                // Eligibility logic — matches web app behavior
                String eligibility;
                if ("APPROVED".equalsIgnoreCase(statut) && avoided >= 0.5) {
                    eligibility = "ELIGIBLE";
                    eligible++;
                    sumDispatchable += avoided;
                } else if (avoided > 0 && avoided < 0.5) {
                    eligibility = "NEEDS_IMPROVEMENT";
                    needsImprovement++;
                } else if ("REJECTED".equalsIgnoreCase(statut) || "CANCELLED".equalsIgnoreCase(statut)
                           || avoided <= 0) {
                    eligibility = "NOT_ELIGIBLE";
                    notEligible++;
                } else {
                    eligibility = "PENDING";
                    other++;
                }

                ProjectRow row = new ProjectRow();
                row.id           = "#PI" + rs.getInt("id");
                row.name         = rs.getString("titre");
                row.status       = statut != null ? statut : "DRAFT";
                row.baseline     = String.format("%.3f", baseline);
                row.actual       = String.format("%.3f", actual);
                row.avoided      = String.format("%.3f", avoided);
                row.dispatchable = eligibility.equals("ELIGIBLE")
                                   ? String.format("%.3f", avoided) : "0.000";
                row.eligibility  = eligibility;
                allRows.add(row);
            }

            // Update KPIs
            final int t = total, el = eligible, ni = needsImprovement, ne = notEligible, ot = other;
            final double sb = sumBaseline, sa = sumActual, sav = sumAvoided, sd = sumDispatchable;

            lblProjectCount.setText(total + " Projets · " + eligible + " dispatchables");
            lblBaseline.setText(String.format("%.3f", sb));
            lblActual.setText(String.format("%.3f", sa));
            lblAvoided.setText(String.format("%.3f", sav));
            lblDispatchable.setText(String.format("%.3f", sd));

            lblEligibilityTotal.setText(total + " total projects");
            double denom = Math.max(1, total);
            pbEligible.setProgress(el / denom);
            pbNeedsImprovement.setProgress(ni / denom);
            pbNotEligible.setProgress(ne / denom);
            pbOther.setProgress(ot / denom);
            lblEligibleCount.setText(String.valueOf(el));
            lblNeedsImprovementCount.setText(String.valueOf(ni));
            lblNotEligibleCount.setText(String.valueOf(ne));
            lblOtherCount.setText(String.valueOf(ot));

            lblTableCount.setText(total + " / " + total);
            applyFilters();

        } catch (SQLException e) {
            System.err.println("[AdminGreenCredits] SQL error: " + e.getMessage());
            lblHealthStatus.setText("Failed to load projects: " + e.getMessage());
        }
    }

    private void loadKpis() {
        // KPIs already loaded in loadProjects()
    }

    private void checkSystemHealth() {
        Connection conn = MyConnection.getConnection();
        if (conn == null) {
            lblHealthStatus.setText("Failed to load pending counts: database connection unavailable.");
            return;
        }
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT COUNT(*) FROM projet WHERE statut = 'SUBMITTED'")) {
            if (rs.next()) {
                int pending = rs.getInt(1);
                if (pending == 0) {
                    lblHealthStatus.setText("✓ All systems operational. No pending operations.");
                    lblHealthStatus.setStyle("-fx-text-fill:#34d399;");
                } else {
                    lblHealthStatus.setText(pending + " projet(s) en attente de validation.");
                    lblHealthStatus.setStyle("-fx-text-fill:#fbbf24;");
                }
            }
        } catch (SQLException e) {
            lblHealthStatus.setText("Failed to load pending counts: " + e.getMessage());
            lblHealthStatus.setStyle("-fx-text-fill:#f87171;");
        }
    }

    // ── Filters ───────────────────────────────────────────────────────────────
    private void applyFilters() {
        String search      = txtProjectSearch.getText().toLowerCase().trim();
        String statusVal   = cmbStatusFilter.getValue();
        String eligVal     = cmbEligibilityFilter.getValue();
        boolean dispOnly   = chkDispatchOnly.isSelected();

        List<ProjectRow> filtered = new ArrayList<>();
        for (ProjectRow row : allRows) {
            if (!search.isEmpty() && !row.name.toLowerCase().contains(search)
                    && !row.id.toLowerCase().contains(search)) continue;
            if (statusVal != null && !statusVal.startsWith("All")
                    && !statusVal.equals(row.status)) continue;
            if (eligVal != null && !eligVal.startsWith("All")
                    && !eligVal.equals(row.eligibility)) continue;
            if (dispOnly && !"ELIGIBLE".equals(row.eligibility)) continue;
            filtered.add(row);
        }

        tableProjects.setItems(FXCollections.observableArrayList(filtered));
        lblTableCount.setText(filtered.size() + " / " + allRows.size());
    }

    // ── Actions ───────────────────────────────────────────────────────────────
    private void dispatchProject(ProjectRow row) {
        showInfo("Dispatch", "Dispatching credits for project " + row.name + " (" + row.id + ")…\n\n" +
                 "Dispatchable: " + row.dispatchable + " tCO₂e");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static String statusBadge(String st) {
        if (st == null) return "badge-attente";
        return switch (st.toUpperCase()) {
            case "APPROVED"    -> "badge-active";
            case "SUBMITTED"   -> "badge-attente";
            case "REJECTED"    -> "badge-bloque";
            case "DRAFT"       -> "badge-suspendu";
            default            -> "badge-suspendu";
        };
    }

    private static String eligibilityBadge(String el) {
        if (el == null) return "badge-suspendu";
        return switch (el.toUpperCase()) {
            case "ELIGIBLE"          -> "badge-active";
            case "NEEDS_IMPROVEMENT" -> "badge-attente";
            case "NOT_ELIGIBLE"      -> "badge-bloque";
            default                  -> "badge-suspendu";
        };
    }

    private void showInfo(String title, String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }

    // ── Row model ─────────────────────────────────────────────────────────────
    public static class ProjectRow {
        public String id, name, status, baseline, actual, avoided, dispatchable, eligibility;
    }
}
