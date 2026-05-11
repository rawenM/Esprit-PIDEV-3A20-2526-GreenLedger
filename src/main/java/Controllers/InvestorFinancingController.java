package Controllers;

import DataBase.MyConnection;
import Services.InvestisseurService;
import Utils.NavigationContext;
import Utils.SessionManager;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.*;
import java.util.List;
import java.util.Locale;

/**
 * Investor Financing Dashboard — matches the web "Tableau de bord investisseur".
 * Shows: total invested, CO2 impact, active financements, ROI moyen,
 *        investment opportunities list, recent history.
 */
public class InvestorFinancingController extends BaseController {

    @FXML private Label lblProfileName;
    @FXML private Label lblProfileType;

    // KPI cards
    @FXML private Label lblTotalInvested;
    @FXML private Label lblInvestedSub;
    @FXML private Label lblCo2Impact;
    @FXML private Label lblCo2Sub;
    @FXML private Label lblActiveFinancements;
    @FXML private Label lblActiveSub;
    @FXML private Label lblRoiMoyen;

    // Dynamic content areas
    @FXML private VBox boxOpportunities;
    @FXML private VBox boxHistory;

    private final InvestisseurService investService = new InvestisseurService();

    @FXML
    public void initialize() {
        super.initialize();
        applyProfile(lblProfileName, lblProfileType);
        loadData();
    }

    // ── Navigation — all delegate back to the shell ──────────────────────────
    // These are called from the hero buttons; the shell handles sidebar nav
    @FXML private void onDashboard()      { navigateShell("fxml/investisseur_shell"); }
    @FXML private void onProjects()       { navigateShell("fxml/swipe_invest"); }
    @FXML private void onWallet()         { navigateShell("greenwallet"); }
    @FXML private void onFinancement()    { /* already here */ }
    @FXML private void onMarketplace()    { navigateShell("fxml/marketplace"); }
    @FXML private void onBlockchain()     { navigateShell("fxml/investisseur_portfolio"); }
    @FXML private void onAssistant()      { navigateShell("AssistantChat"); }
    @FXML private void onProfile()        { navigateShell("editProfile"); }
    @FXML private void onLogout()         { Utils.SessionManager.getInstance().invalidate(); navigateShell("fxml/login"); }
    @FXML private void onInvestNow()      { navigateInShell("fxml/swipe_invest"); }
    @FXML private void onSwipeProjects()  { navigateInShell("fxml/swipe_invest"); }
    @FXML private void onViewAllProjects(){ navigateInShell("fxml/swipe_invest"); }
    @FXML private void onViewHistory()    { navigateInShell("fxml/investisseur_portfolio"); }

    /** Full scene replace (for pages with their own shell) */
    private void navigateShell(String fxml) {
        try { org.GreenLedger.MainFX.setRoot(fxml); }
        catch (Exception e) { System.err.println("[InvestorFinancing] Nav: " + e.getMessage()); }
    }

    /** Load inside the parent shell's mainArea (keeps sidebar) */
    private void navigateInShell(String fxmlPath) {
        try {
            // Walk up to find the shell's StackPane mainArea
            javafx.scene.Node node = lblTotalInvested;
            while (node != null) {
                if (node instanceof javafx.scene.layout.StackPane sp
                        && "mainArea".equals(sp.getId())) {
                    java.net.URL res = getClass().getResource("/" + fxmlPath + ".fxml");
                    if (res != null) {
                        javafx.scene.Parent p = new javafx.fxml.FXMLLoader(res).load();
                        sp.getChildren().setAll(p);
                    }
                    return;
                }
                node = node.getParent();
            }
            // Fallback: full navigate
            navigateShell(fxmlPath);
        } catch (Exception e) {
            System.err.println("[InvestorFinancing] navigateInShell: " + e.getMessage());
        }
    }

    // ── Data loading ──────────────────────────────────────────────────────────
    private void loadData() {
        var user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;
        long uid = user.getId();

        loadKpis(uid);
        loadOpportunities();
        loadHistory(uid);
    }

    private void loadKpis(long uid) {
        // Platform-wide aggregates — no WHERE investisseur_id filter (matches web app)
        String sql = "SELECT COALESCE(SUM(montant),0) AS total_invested, COUNT(*) AS active_count FROM financements";

        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                double totalInvested = rs.getDouble("total_invested");
                int    activeCount   = rs.getInt("active_count");

                // 1. Total investi — SUM(montant) all rows
                set(lblTotalInvested, String.format(Locale.ROOT, "%,.0f $", totalInvested));
                set(lblInvestedSub,   activeCount + " invest.");

                // 2. CO₂ efficiency — (activeCount × 12.5) / totalInvested, 4 decimals
                double co2Eff = totalInvested > 0
                    ? Math.round((activeCount * 12.5 / totalInvested) * 10000.0) / 10000.0
                    : 0.0;
                set(lblCo2Impact, String.format(Locale.ROOT, "%.4f", co2Eff));
                set(lblCo2Sub,    String.format(Locale.ROOT, "%.4f tCO2 / invest", co2Eff));

                // 3. Financements actifs — COUNT(*) all rows
                set(lblActiveFinancements, String.valueOf(activeCount));
                set(lblActiveSub,          "Dossiers en cours");

                // 4. ROI moyen — 5 + (activeCount × 0.3), 1 decimal
                double roi = Math.round((5.0 + activeCount * 0.3) * 10.0) / 10.0;
                set(lblRoiMoyen, String.format(Locale.ROOT, "%.1f%%", roi));
            }
        } catch (SQLException e) {
            System.err.println("[InvestorFinancing] KPI error: " + e.getMessage());
        }
    }

    private void loadOpportunities() {
        if (boxOpportunities == null) return;
        boxOpportunities.getChildren().clear();

        List<InvestisseurService.ProjetInvestDTO> projects =
            investService.getProjectsForInvestment(null, null, null, null, null);

        int shown = 0;
        for (InvestisseurService.ProjetInvestDTO p : projects) {
            if (shown >= 5) break;
            boxOpportunities.getChildren().add(buildOpportunityRow(p));
            shown++;
        }

        if (shown == 0) {
            Label empty = new Label("Aucun projet disponible pour le moment.");
            empty.setStyle("-fx-font-size:12px;-fx-text-fill:#94A3B8;-fx-padding:16 20;");
            boxOpportunities.getChildren().add(empty);
        }
    }

    private HBox buildOpportunityRow(InvestisseurService.ProjetInvestDTO p) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding:12 20;-fx-border-color:transparent transparent #F1F5F9 transparent;-fx-border-width:0 0 1 0;-fx-cursor:hand;");

        // Avatar
        String initial = p.titre != null && !p.titre.isEmpty()
            ? p.titre.substring(0, 1).toUpperCase() : "P";
        Label avatar = new Label(initial);
        avatar.setStyle("-fx-background-color:#D1FAE5;-fx-text-fill:#059669;-fx-font-size:12px;-fx-font-weight:800;"
            + "-fx-background-radius:20;-fx-min-width:36;-fx-min-height:36;-fx-max-width:36;-fx-max-height:36;-fx-alignment:CENTER;");

        // Title + subtitle
        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        String titre = p.titre != null ? p.titre : "Projet";
        Label lblTitle = new Label(titre.length() > 30 ? titre.substring(0, 28) + "…" : titre);
        lblTitle.setStyle("-fx-font-size:12px;-fx-font-weight:700;-fx-text-fill:#0F172A;");
        String sub = (p.secteur != null ? p.secteur : "—")
            + " · ROI " + (p.roi != null ? String.format(Locale.ROOT, "%.1f%%", p.roi) : "—")
            + " · " + (p.localisation != null ? p.localisation : "—");
        Label lblSub = new Label(sub);
        lblSub.setStyle("-fx-font-size:10px;-fx-text-fill:#64748B;");
        info.getChildren().addAll(lblTitle, lblSub);

        // ESG + fraud badges
        VBox badges = new VBox(4);
        badges.setAlignment(Pos.CENTER_RIGHT);
        if (p.scoreEsg != null) {
            Label esg = new Label("ESG " + p.scoreEsg + "/10");
            esg.setStyle("-fx-background-color:#D1FAE5;-fx-text-fill:#059669;-fx-font-size:9px;-fx-font-weight:700;-fx-background-radius:4;-fx-padding:2 7;");
            badges.getChildren().add(esg);
        }
        String fraudLevel = InvestisseurService.fraudLevel(p.fraudRiskScore);
        String fraudColor = InvestisseurService.fraudColor(p.fraudRiskScore);
        Label fraud = new Label(fraudLevel + " RISK");
        fraud.setStyle("-fx-background-color:" + fraudColor + "18;-fx-text-fill:" + fraudColor
            + ";-fx-font-size:9px;-fx-font-weight:700;-fx-background-radius:4;-fx-padding:2 7;");
        badges.getChildren().add(fraud);

        row.getChildren().addAll(avatar, info, badges);

        row.setOnMouseClicked(e -> {
            NavigationContext.getInstance().setCurrentProjectId(p.id);
            navigateInShell("fxml/swipe_invest");
        });
        row.setOnMouseEntered(e -> row.setStyle(row.getStyle().replace("#F8FAFC", "white")
            + "-fx-background-color:#F8FAFC;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-padding:12 20;-fx-border-color:transparent transparent #F1F5F9 transparent;-fx-border-width:0 0 1 0;-fx-cursor:hand;"));

        return row;
    }

    private void loadHistory(long uid) {
        if (boxHistory == null) return;
        boxHistory.getChildren().clear();

        String sql =
            "SELECT f.id, p.titre, f.montant, f.statut, f.created_at " +
            "FROM financements f " +
            "LEFT JOIN projet p ON p.id = f.project_id " +
            "WHERE f.investisseur_id = ? " +
            "ORDER BY f.created_at DESC LIMIT 6";

        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, uid);
            try (ResultSet rs = ps.executeQuery()) {
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    int    id      = rs.getInt("id");
                    String titre   = rs.getString("titre");
                    double montant = rs.getDouble("montant");
                    String statut  = rs.getString("statut");
                    String date    = rs.getString("created_at");
                    boxHistory.getChildren().add(buildHistoryRow(id, titre, montant, statut, date));
                }
                if (!any) {
                    Label empty = new Label("Aucun placement pour le moment.");
                    empty.setStyle("-fx-font-size:12px;-fx-text-fill:#94A3B8;-fx-padding:16 20;");
                    boxHistory.getChildren().add(empty);
                }
            }
        } catch (SQLException e) {
            System.err.println("[InvestorFinancing] history error: " + e.getMessage());
        }
    }

    private HBox buildHistoryRow(int id, String titre, double montant, String statut, String date) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding:12 20;-fx-border-color:transparent transparent #F1F5F9 transparent;-fx-border-width:0 0 1 0;");

        // ID badge
        Label lblId = new Label("#" + String.format("%06d", id));
        lblId.setStyle("-fx-font-size:10px;-fx-font-weight:700;-fx-text-fill:#94A3B8;-fx-min-width:70;");

        // Title + date
        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        String t = titre != null ? titre : "Projet #" + id;
        Label lblTitle = new Label(t.length() > 28 ? t.substring(0, 26) + "…" : t);
        lblTitle.setStyle("-fx-font-size:12px;-fx-font-weight:600;-fx-text-fill:#0F172A;");
        String d = date != null ? date.substring(0, Math.min(10, date.length())) : "—";
        Label lblDate = new Label(d);
        lblDate.setStyle("-fx-font-size:10px;-fx-text-fill:#94A3B8;");
        info.getChildren().addAll(lblTitle, lblDate);

        // Amount
        Label lblAmt = new Label(String.format(Locale.ROOT, "%,.0f $", montant));
        lblAmt.setStyle("-fx-font-size:13px;-fx-font-weight:700;-fx-text-fill:#0F172A;-fx-min-width:80;-fx-alignment:CENTER_RIGHT;");

        // Status badge
        String[] sc = statusStyle(statut);
        Label lblStatus = new Label(sc[0]);
        lblStatus.setStyle("-fx-background-color:" + sc[1] + ";-fx-text-fill:" + sc[2]
            + ";-fx-font-size:9px;-fx-font-weight:700;-fx-background-radius:5;-fx-padding:3 8;");

        row.getChildren().addAll(lblId, info, lblAmt, lblStatus);
        return row;
    }

    private String[] statusStyle(String statut) {
        if (statut == null) return new String[]{"PENDING", "#FEF3C7", "#92400E"};
        return switch (statut.toUpperCase()) {
            case "COMPLETED" -> new String[]{"COMPLETED", "#D1FAE5", "#065F46"};
            case "PENDING"   -> new String[]{"PENDING",   "#FEF3C7", "#92400E"};
            case "REJECTED"  -> new String[]{"REJECTED",  "#FEE2E2", "#991B1B"};
            default          -> new String[]{statut,      "#F1F5F9", "#475569"};
        };
    }

    private void set(Label lbl, String text) { if (lbl != null) lbl.setText(text); }
}
