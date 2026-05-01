package Controllers;

import Models.Projet;
import Models.User;
import Services.ProjetService;
import Utils.NavigationContext;
import Utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Porteur de Projet Shell Controller
 * Login redirect → /front-office/projets  (porteur_shell.fxml)
 */
public class PorteurShellController extends BaseController {

    @FXML private Label lblPageTitle;
    @FXML private Label lblProfileName;
    @FXML private Label lblProfileType;
    @FXML private Label lblUnreadBadge;
    @FXML private Label lblHealthScore;

    // KPI labels
    @FXML private Label statProjets;
    @FXML private Label statApprouves;
    @FXML private Label statFinancements;
    @FXML private Label statCredits;

    // Recent projects table
    @FXML private TableView<Projet>          tableProjets;
    @FXML private TableColumn<Projet,String> colTitre;
    @FXML private TableColumn<Projet,String> colStatut;
    @FXML private TableColumn<Projet,String> colEsg;
    @FXML private TableColumn<Projet,String> colDate;
    @FXML private TableColumn<Projet,String> colActions;

    private final ProjetService projetService = new ProjetService();

    @FXML
    public void initialize() {
        super.initialize();
        applyProfile(lblProfileName, lblProfileType);
        setupTable();
        loadDashboard();
    }

    // ── Sidebar navigation ──────────────────────────────────────────────────

    @FXML private void onDashboard()   { loadDashboard(); lblPageTitle.setText("Tableau de bord"); }
    @FXML private void onProjets()     { navigate("fxml/porteur_projets"); }
    @FXML private void onNewProjet()   { Utils.NavigationContext.getInstance().setCurrentProjectId(null); navigate("fxml/porteur_projet_form"); }
    @FXML private void onEvaluations() { navigate("ownerEvaluations"); }
    @FXML private void onFinancing()   { navigate("financement"); }
    @FXML private void onWallet()      { navigate("greenwallet"); }
    @FXML private void onMessages()    { navigate("fxml/porteur_messages"); }
    @FXML private void onAssistant()   { navigate("fxml/porteur_assistant"); }
    @FXML private void onNotifications() { navigate("fxml/porteur_notifications"); }
    @FXML private void onEditProfile() { navigate("editProfile"); }

    @FXML
    private void onBack() {
        String prev = NavigationContext.getInstance().getPreviousPage();
        if (prev != null && !prev.isEmpty() && !prev.equals(NavigationContext.getInstance().getCurrentPage())) {
            navigate(prev);
        } else {
            navigate("fxml/login");
        }
    }

    @FXML
    private void onLogout() {
        SessionManager.getInstance().invalidate();
        navigate("fxml/login");
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private void loadDashboard() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        try {
            List<Projet> projets = projetService.getByEntreprise(user.getId().intValue());

            long approved   = projets.stream().filter(p -> "APPROVED".equals(p.getStatutEvaluation())).count();
            long submitted  = projets.stream().filter(p -> "SUBMITTED".equals(p.getStatutEvaluation())).count();
            long inProgress = projets.stream().filter(p -> "IN_PROGRESS".equals(p.getStatutEvaluation())).count();

            statProjets.setText(String.valueOf(projets.size()));
            statApprouves.setText(String.valueOf(approved));

            // Financing count
            try (java.sql.Connection conn = DataBase.MyConnection.getConnection();
                 java.sql.PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM financements f JOIN projet p ON p.id=f.project_id WHERE p.entreprise_id=?")) {
                ps.setLong(1, user.getId());
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) statFinancements.setText(String.valueOf(rs.getInt(1)));
                }
            } catch (Exception ignored) { statFinancements.setText("0"); }

            // Green credits
            try (java.sql.Connection conn = DataBase.MyConnection.getConnection();
                 java.sql.PreparedStatement ps = conn.prepareStatement(
                     "SELECT SUM(available_credits) FROM wallet WHERE owner_id=?")) {
                ps.setLong(1, user.getId());
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        double v = rs.getDouble(1);
                        statCredits.setText(rs.wasNull() ? "0" : String.format(java.util.Locale.ROOT, "%.1f", v));
                    }
                }
            } catch (Exception ignored) { statCredits.setText("0"); }

            // Unread notifications badge
            try (java.sql.Connection conn = DataBase.MyConnection.getConnection();
                 java.sql.PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM notifications WHERE user_id=? AND is_read=0")) {
                ps.setLong(1, user.getId());
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int unread = rs.getInt(1);
                        if (lblUnreadBadge != null) {
                            lblUnreadBadge.setText(unread > 0 ? String.valueOf(unread) : "");
                            lblUnreadBadge.setVisible(unread > 0);
                        }
                    }
                }
            } catch (Exception ignored) {}

            // Health score
            int total = projets.size();
            double healthScore = 0;
            if (total > 0) {
                double approvedRatio = (double) approved / total;
                double submittedRatio = (double) (submitted + inProgress) / total;
                healthScore = Math.min(100, Math.round(
                    (approvedRatio * 0.45 + submittedRatio * 0.25) * 100));
            }
            if (lblHealthScore != null) {
                lblHealthScore.setText(String.valueOf((int) healthScore));
                String hColor = healthScore >= 70 ? "#6EE7B7" : healthScore >= 40 ? "#FCD34D" : "#F87171";
                lblHealthScore.setStyle("-fx-font-size:28px;-fx-font-weight:800;-fx-text-fill:" + hColor + ";");
            }

            // Show last 5 recent projects
            List<Projet> recent = projets.stream().limit(5).collect(Collectors.toList());
            tableProjets.setItems(FXCollections.observableArrayList(recent));

        } catch (Exception e) {
            System.err.println("[PorteurShell] Dashboard error: " + e.getMessage());
        }
    }

    private void setupTable() {
        colTitre.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getTitre()));

        // Statut — colored badge
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                String s = getTableRow().getItem().getStatutEvaluation();
                if (s == null) s = "DRAFT";
                String bg = switch (s) {
                    case "APPROVED"    -> "rgba(16,185,129,0.2);-fx-text-fill:#34D399";
                    case "REJECTED"    -> "rgba(239,68,68,0.2);-fx-text-fill:#F87171";
                    case "SUBMITTED"   -> "rgba(37,99,235,0.2);-fx-text-fill:#93C5FD";
                    case "IN_PROGRESS" -> "rgba(245,158,11,0.2);-fx-text-fill:#FCD34D";
                    default            -> "rgba(255,255,255,0.08);-fx-text-fill:rgba(255,255,255,0.5)";
                };
                javafx.scene.control.Label badge = new javafx.scene.control.Label(s);
                badge.setStyle("-fx-background-color:" + bg + ";-fx-font-size:10px;-fx-font-weight:700;-fx-background-radius:5;-fx-padding:3 8;");
                setGraphic(badge); setText(null);
            }
        });

        colEsg.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getScoreEsg() != null
                ? String.valueOf(c.getValue().getScoreEsg()) : "—"));
        colDate.setCellValueFactory(c -> {
            var d = c.getValue().getDateCreation();
            return new SimpleStringProperty(d != null
                ? d.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—");
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnView = new Button("Voir");
            private final Button btnEdit = new Button("Modifier");
            private final Button btnPdf  = new Button("PDF");

            {
                String base = "-fx-font-size:10px;-fx-font-weight:600;-fx-background-radius:6;-fx-padding:5 10;-fx-cursor:hand;";
                btnView.setStyle(base + "-fx-background-color:rgba(37,99,235,0.2);-fx-text-fill:#93C5FD;-fx-border-color:rgba(37,99,235,0.3);-fx-border-width:1;");
                btnEdit.setStyle(base + "-fx-background-color:rgba(255,255,255,0.06);-fx-text-fill:rgba(255,255,255,0.7);-fx-border-color:rgba(255,255,255,0.1);-fx-border-width:1;");
                btnPdf.setStyle(base  + "-fx-background-color:rgba(45,95,63,0.2);-fx-text-fill:#6EE7B7;-fx-border-color:rgba(45,95,63,0.3);-fx-border-width:1;");

                btnView.setOnAction(e -> {
                    Projet p = getTableView().getItems().get(getIndex());
                    NavigationContext.getInstance().setCurrentProjectId(p.getId());
                    navigate("ProjetDetail");
                });
                btnEdit.setOnAction(e -> {
                    Projet p = getTableView().getItems().get(getIndex());
                    NavigationContext.getInstance().setCurrentProjectId(p.getId());
                    navigate("fxml/porteur_projet_form");
                });
                btnPdf.setOnAction(e -> navigate("fxml/porteur_projets"));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                setGraphic(new javafx.scene.layout.HBox(4, btnView, btnEdit, btnPdf));
            }
        });
    }

    private void navigate(String fxml) {
        try { org.GreenLedger.MainFX.setRoot(fxml); }
        catch (Exception e) { System.err.println("[PorteurShell] Nav error: " + e.getMessage()); }
    }
}
