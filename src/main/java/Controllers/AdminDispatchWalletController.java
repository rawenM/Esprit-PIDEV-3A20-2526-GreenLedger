package Controllers;

import DataBase.MyConnection;
import Services.WalletService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin Dispatch Wallet Controller
 * Matches the PHP "Dispatch Wallet" page design.
 */
public class AdminDispatchWalletController {

    // ── KPI labels ────────────────────────────────────────────────────────────
    @FXML private Label lblLedgerSync;
    @FXML private Label lblAvailableCredits;
    @FXML private Label lblCalculatedAmount;
    @FXML private Label lblCoinsMinted;
    @FXML private Label lblWalletStatus;
    @FXML private Label lblWalletAddress;

    // ── Dispatch form ─────────────────────────────────────────────────────────
    @FXML private ComboBox<ProjectItem> cmbProject;
    @FXML private Label lblCreditsInfo;
    @FXML private Label lblDestinationInfo;

    // ── Wallet connect ────────────────────────────────────────────────────────
    @FXML private Label     lblConnectStatus;
    @FXML private TextField txtWalletName;
    @FXML private Label     lblNetwork;
    @FXML private Label     lblAddress;

    // ── Transaction preview ───────────────────────────────────────────────────
    @FXML private Label lblPreviewProject;
    @FXML private Label lblPreviewRecipient;
    @FXML private Label lblPreviewCredits;
    @FXML private Label lblPreviewGas;
    @FXML private Label lblPreviewNote;
    @FXML private Label lblCoverage;
    @FXML private Label lblDispatchableCount;

    // ── Ledger activity table ─────────────────────────────────────────────────
    @FXML private TableView<TxRow>        tableActivity;
    @FXML private TableColumn<TxRow,String> colTx;
    @FXML private TableColumn<TxRow,String> colType;
    @FXML private TableColumn<TxRow,String> colStatus;
    @FXML private TableColumn<TxRow,String> colCredits;
    @FXML private TableColumn<TxRow,String> colBatch;
    @FXML private TableColumn<TxRow,String> colUpdated;

    private final WalletService walletService = new WalletService();
    private final ObservableList<TxRow> txRows = FXCollections.observableArrayList();

    // ── Init ─────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        // Apply stylesheet programmatically (avoids relative-path resolution issue when loaded inside shell)
        javafx.application.Platform.runLater(() -> {
            if (cmbProject.getScene() != null) {
                String css = getClass().getResource("/themes/admin-backoffice.css").toExternalForm();
                if (!cmbProject.getScene().getStylesheets().contains(css)) {
                    cmbProject.getScene().getStylesheets().add(css);
                }
            }
        });
        setupTable();
        loadProjects();
        loadLedgerActivity();
        updateDispatchableCount();
        updateLedgerSync();
    }

    // ── Tab navigation ────────────────────────────────────────────────────────
    @FXML private void onTabDashboard() { loadContent("fxml/admin_green_credits"); }
    @FXML private void onTabDispatch()  { /* already here */ }
    @FXML private void onTabAdvanced()  { /* future */ }

    private void loadContent(String path) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/" + path + ".fxml"));
            javafx.scene.Parent p = loader.load();
            // Find parent StackPane (contentArea) and swap
            javafx.scene.Node node = cmbProject.getScene().getRoot();
            if (node instanceof javafx.scene.layout.BorderPane bp) {
                javafx.scene.Node center = bp.getCenter();
                if (center instanceof javafx.scene.layout.StackPane sp) {
                    sp.getChildren().setAll(p);
                }
            }
        } catch (Exception e) {
            System.err.println("[DispatchWallet] Nav error: " + e.getMessage());
        }
    }

    // ── Setup ─────────────────────────────────────────────────────────────────
    private void setupTable() {
        colTx.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().tx));
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().type));
        colCredits.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().credits));
        colBatch.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().batch));
        colUpdated.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().updated));

        // Status badge
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                String st = getTableRow().getItem().status;
                Label badge = new Label(st);
                String style = switch (st) {
                    case "SUCCESS"    -> "-fx-background-color:rgba(52,211,153,0.15); -fx-text-fill:#34d399;";
                    case "PENDING_TX" -> "-fx-background-color:rgba(251,191,36,0.15); -fx-text-fill:#fbbf24;";
                    case "FAILED"     -> "-fx-background-color:rgba(248,113,113,0.15); -fx-text-fill:#f87171;";
                    default           -> "-fx-background-color:rgba(148,163,184,0.1);  -fx-text-fill:#94a3b8;";
                };
                badge.setStyle(style + " -fx-background-radius:5; -fx-padding:3 8 3 8; -fx-font-size:10px; -fx-font-weight:700;");
                setGraphic(badge);
            }
        });
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status));

        tableActivity.setItems(txRows);
    }

    // ── Data loading ──────────────────────────────────────────────────────────
    private void loadProjects() {
        Connection conn = MyConnection.getConnection();
        if (conn == null) return;

        String sql = "SELECT p.id, p.titre, " +
                     "COALESCE(p.avoided_tco2, 0) as avoided, " +
                     "COALESCE(p.dispatched_green_credits, 0) as dispatched " +
                     "FROM projet p " +
                     "WHERE p.statut = 'APPROVED' OR p.avoided_tco2 > 0 " +
                     "ORDER BY p.id DESC LIMIT 50";

        List<ProjectItem> items = new ArrayList<>();
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                ProjectItem item = new ProjectItem();
                item.id         = rs.getInt("id");
                item.titre      = rs.getString("titre");
                item.avoided    = rs.getDouble("avoided");
                item.dispatched = rs.getDouble("dispatched");
                items.add(item);
            }
        } catch (SQLException e) {
            System.err.println("[DispatchWallet] loadProjects error: " + e.getMessage());
        }

        cmbProject.setItems(FXCollections.observableArrayList(items));
        cmbProject.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(ProjectItem p) {
                return p == null ? "" : p.titre + " — #PI" + p.id;
            }
            @Override public ProjectItem fromString(String s) { return null; }
        });

        if (!items.isEmpty()) {
            cmbProject.getSelectionModel().selectFirst();
            updateProjectInfo(items.get(0));
        }
    }

    private void loadLedgerActivity() {
        txRows.clear();
        Connection conn = MyConnection.getConnection();
        if (conn == null) return;

        // Load from wallet_transactions joined with carbon_credit_batches
        String sql = "SELECT wt.id, wt.type, wt.amount, wt.created_at, " +
                     "COALESCE(wt.batch_id, 0) as batch_id " +
                     "FROM wallet_transactions wt " +
                     "ORDER BY wt.created_at DESC LIMIT 20";

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                TxRow row = new TxRow();
                row.tx      = "#" + rs.getInt("id");
                row.type    = rs.getString("type");
                row.status  = "SUCCESS";
                row.credits = String.format("%.3f", rs.getDouble("amount"));
                int batchId = rs.getInt("batch_id");
                row.batch   = batchId > 0 ? String.valueOf(batchId) : "—";
                Timestamp ts = rs.getTimestamp("created_at");
                row.updated = ts != null ? ts.toLocalDateTime().format(fmt) : "—";
                txRows.add(row);
            }
        } catch (SQLException e) {
            System.err.println("[DispatchWallet] loadLedgerActivity error: " + e.getMessage());
        }

        // Update coins minted from total
        double totalMinted = txRows.stream()
            .filter(r -> "ISSUE".equals(r.type) || "MINT".equals(r.type))
            .mapToDouble(r -> { try { return Double.parseDouble(r.credits); } catch (Exception ex) { return 0; } })
            .sum();
        lblCoinsMinted.setText(String.format("%.3f", totalMinted));
    }

    private void updateDispatchableCount() {
        Connection conn = MyConnection.getConnection();
        if (conn == null) return;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT COUNT(*) FROM projet WHERE avoided_tco2 >= 0.5 AND statut = 'APPROVED'")) {
            if (rs.next()) {
                int count = rs.getInt(1);
                lblDispatchableCount.setText(String.valueOf(count));
            }
        } catch (SQLException e) {
            lblDispatchableCount.setText("—");
        }
    }

    private void updateLedgerSync() {
        Connection conn = MyConnection.getConnection();
        if (conn == null) return;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM wallet_transactions")) {
            if (rs.next()) {
                int total = rs.getInt(1);
                lblLedgerSync.setText("Block: " + total + " · Last block: " + total);
            }
        } catch (SQLException e) {
            lblLedgerSync.setText("Block: — · Last block: —");
        }
    }

    // ── Project selection ─────────────────────────────────────────────────────
    @FXML
    private void onProjectSelected() {
        ProjectItem selected = cmbProject.getValue();
        if (selected == null) return;
        updateProjectInfo(selected);
    }

    private void updateProjectInfo(ProjectItem p) {
        double available = p.avoided - p.dispatched;
        lblAvailableCredits.setText(String.format("%.3f", Math.max(0, available)));
        lblCalculatedAmount.setText(String.format("%.3f", p.avoided));
        lblCreditsInfo.setText("Credits: " + String.format("%.3f", p.avoided));
        lblDestinationInfo.setText("Destination: " + (txtWalletName.getText().isEmpty() ? "mehdi" : txtWalletName.getText()));

        // Update preview
        lblPreviewProject.setText(p.titre + " (#PI" + p.id + ")");
        lblPreviewRecipient.setText(txtWalletName.getText().isEmpty() ? "mehdi" : txtWalletName.getText());
        lblPreviewCredits.setText(String.format("%.3f tCO₂", p.avoided));
        lblPreviewGas.setText("0.002 MATIC");
        lblPreviewNote.setText("How Project → Smart Contract → Wallet (Polygon Array)");

        // Coverage
        double coverage = p.avoided > 0 ? Math.min(100, (available / p.avoided) * 100) : 0;
        lblCoverage.setText(String.format("%.0f%%", coverage));
    }

    // ── Actions ───────────────────────────────────────────────────────────────
    @FXML
    private void onIssueCredits() {
        ProjectItem selected = cmbProject.getValue();
        if (selected == null) {
            showAlert("Sélectionnez un projet", Alert.AlertType.WARNING);
            return;
        }
        double available = selected.avoided - selected.dispatched;
        if (available <= 0) {
            showAlert("Aucun crédit disponible pour ce projet.", Alert.AlertType.WARNING);
            return;
        }

        // Issue credits via WalletService
        try {
            // Find or create wallet for the destination
            String walletName = txtWalletName.getText().isEmpty() ? "mehdi" : txtWalletName.getText();
            boolean success = walletService.quickIssueCredits(1, available,
                "Dispatch from project #PI" + selected.id + " — " + selected.titre);
            if (success) {
                showAlert("✓ " + String.format("%.3f", available) + " tCO₂e dispatched successfully!", Alert.AlertType.INFORMATION);
                loadLedgerActivity();
                updateProjectInfo(selected);
            } else {
                showAlert("Dispatch failed. Check wallet configuration.", Alert.AlertType.ERROR);
            }
        } catch (Exception e) {
            showAlert("Error: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void onSyncNow() {
        loadLedgerActivity();
        updateLedgerSync();
        showAlert("Ledger synced successfully.", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void onAdminAdjustment() {
        showAlert("Admin adjustment panel — coming soon.", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void onSwitchWallet() {
        showAlert("Wallet switching — connect your blockchain wallet.", Alert.AlertType.INFORMATION);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void showAlert(String msg, Alert.AlertType type) {
        new Alert(type, msg, ButtonType.OK).showAndWait();
    }

    // ── Row models ────────────────────────────────────────────────────────────
    public static class ProjectItem {
        public int    id;
        public String titre;
        public double avoided;
        public double dispatched;
    }

    public static class TxRow {
        public String tx, type, status, credits, batch, updated;
    }
}
