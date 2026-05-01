package Controllers;

import DataBase.MyConnection;
import Services.ConversationService;
import Services.InvestisseurService;
import Utils.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Investor Messages — thread list + chat panel.
 */
public class InvestisseurMessagesController extends BaseController {

    @FXML private Label  lblTotalInvestment;
    @FXML private Label  lblActiveProjects;
    @FXML private Label  lblUnreadTotal;
    @FXML private VBox   boxThreads;
    @FXML private HBox   boxChatHeader;
    @FXML private Label  lblChatProject;
    @FXML private Label  lblChatPorteur;
    @FXML private Label  lblChatInvestment;
    @FXML private VBox   boxMessages;
    @FXML private Label  lblNoThread;
    @FXML private HBox   boxInput;
    @FXML private TextArea txtMessage;
    @FXML private javafx.scene.control.ScrollPane scrollMessages;

    private final ConversationService convService = new ConversationService();
    private int    selectedThreadId = -1;
    private long   currentUserId;

    @FXML
    public void initialize() {
        super.initialize();
        var user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;
        currentUserId = user.getId();
        loadStats();
        loadThreads();
    }

    @FXML private void onBack() { navigate("fxml/investisseur_shell"); }

    @FXML
    private void onSendMessage() {
        if (selectedThreadId < 0) return;
        String content = txtMessage.getText().trim();
        if (content.isBlank()) return;
        convService.sendMessage(selectedThreadId, currentUserId, content);
        txtMessage.clear();
        loadMessages(selectedThreadId);
        scrollToBottom();
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    private void loadStats() {
        String sql =
            "SELECT COALESCE(SUM(f.montant),0) AS total, COUNT(DISTINCT f.project_id) AS projects " +
            "FROM financements f WHERE f.investisseur_id=? AND f.statut='COMPLETED'";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, currentUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double total = rs.getDouble("total");
                    int projects = rs.getInt("projects");
                    if (lblTotalInvestment != null)
                        lblTotalInvestment.setText(String.format(Locale.ROOT, "$%,.0f investi", total));
                    if (lblActiveProjects != null)
                        lblActiveProjects.setText(projects + " projets actifs");
                }
            }
        } catch (SQLException e) { /* ignore */ }

        // Unread count
        String unreadSql =
            "SELECT COUNT(*) FROM thread_messages tm " +
            "JOIN conversation_threads ct ON ct.id=tm.thread_id " +
            "WHERE ct.investisseur_id=? AND tm.sender_id!=? AND tm.is_read=0";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(unreadSql)) {
            ps.setLong(1, currentUserId); ps.setLong(2, currentUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && lblUnreadTotal != null)
                    lblUnreadTotal.setText(rs.getInt(1) + " non lus");
            }
        } catch (SQLException e) { /* ignore */ }
    }

    // ── Threads ───────────────────────────────────────────────────────────────

    private void loadThreads() {
        boxThreads.getChildren().clear();
        String sql =
            "SELECT ct.id, ct.project_id, p.titre AS project_name, p.score_esg, " +
            "u.nom AS porteur_nom, u.prenom AS porteur_prenom, " +
            "COALESCE(f.montant,0) AS investment_amount, " +
            "(SELECT COUNT(*) FROM thread_messages tm WHERE tm.thread_id=ct.id AND tm.sender_id!=? AND tm.is_read=0) AS unread_count, " +
            "(SELECT content FROM thread_messages tm WHERE tm.thread_id=ct.id ORDER BY tm.sent_at DESC LIMIT 1) AS last_message " +
            "FROM conversation_threads ct " +
            "JOIN projet p ON p.id=ct.project_id " +
            "JOIN user u ON u.id=ct.porteur_id " +
            "LEFT JOIN financements f ON f.project_id=ct.project_id AND f.investisseur_id=ct.investisseur_id AND f.statut='COMPLETED' " +
            "WHERE ct.investisseur_id=? ORDER BY f.montant DESC";

        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, currentUserId); ps.setLong(2, currentUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int threadId    = rs.getInt("id");
                    String projName = rs.getString("project_name");
                    String porteur  = rs.getString("porteur_prenom") + " " + rs.getString("porteur_nom");
                    double amount   = rs.getDouble("investment_amount");
                    int unread      = rs.getInt("unread_count");
                    String lastMsg  = rs.getString("last_message");
                    Integer esg     = (Integer) rs.getObject("score_esg");
                    boxThreads.getChildren().add(
                        buildThreadCard(threadId, projName, porteur, amount, unread, lastMsg, esg));
                }
            }
        } catch (SQLException e) {
            System.err.println("[InvestisseurMessages] loadThreads: " + e.getMessage());
        }

        if (boxThreads.getChildren().isEmpty()) {
            Label empty = new Label("Aucune conversation.\nInvestissez dans des projets pour commencer.");
            empty.setWrapText(true);
            empty.setStyle("-fx-font-size:12px;-fx-text-fill:#94A3B8;-fx-padding:20 16;-fx-alignment:CENTER;");
            boxThreads.getChildren().add(empty);
        }
    }

    private VBox buildThreadCard(int threadId, String projName, String porteur,
                                  double amount, int unread, String lastMsg, Integer esg) {
        VBox card = new VBox(6);
        boolean selected = threadId == selectedThreadId;
        card.setStyle("-fx-padding:14 16;-fx-border-color:#E2E8F0;-fx-border-width:0 0 1 0;-fx-cursor:hand;"
            + (selected ? "-fx-background-color:#F0FDF4;" : "-fx-background-color:white;"));

        // Header row
        HBox header = new HBox(6);
        header.setAlignment(Pos.CENTER_LEFT);
        Label lblProj = new Label(projName != null ? (projName.length() > 24 ? projName.substring(0,22)+"..." : projName) : "-");
        lblProj.setStyle("-fx-font-size:12px;-fx-font-weight:700;-fx-text-fill:#0F172A;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        header.getChildren().addAll(lblProj, sp);

        if (unread > 0) {
            Label badge = new Label(unread > 9 ? "9+" : String.valueOf(unread));
            badge.setStyle("-fx-background-color:#f43f5e;-fx-text-fill:white;-fx-font-size:9px;-fx-font-weight:700;-fx-background-radius:10;-fx-padding:1 6;");
            header.getChildren().add(badge);
        }

        // ESG badge
        HBox badges = new HBox(6);
        if (esg != null) {
            Label esgBadge = new Label("ESG " + esg + "/10");
            esgBadge.setStyle("-fx-background-color:#D1FAE5;-fx-text-fill:#10b981;-fx-font-size:9px;-fx-font-weight:700;-fx-background-radius:4;-fx-padding:2 6;");
            badges.getChildren().add(esgBadge);
        }

        // Priority badge
        String priority = InvestisseurService.priorityBadge(amount);
        String pColor   = InvestisseurService.priorityColor(amount);
        Label lblPriority = new Label(priority);
        lblPriority.setStyle("-fx-background-color:" + pColor + "18;-fx-text-fill:" + pColor
            + ";-fx-font-size:9px;-fx-font-weight:700;-fx-background-radius:4;-fx-padding:2 6;");
        badges.getChildren().add(lblPriority);

        // Investment amount
        if (amount > 0) {
            Label amtBadge = new Label(String.format(Locale.ROOT, "$%,.0f", amount));
            amtBadge.setStyle("-fx-background-color:#D1FAE5;-fx-text-fill:#10b981;-fx-font-size:9px;-fx-font-weight:700;-fx-background-radius:4;-fx-padding:2 6;");
            badges.getChildren().add(amtBadge);
        }

        Label lblPorteur = new Label(porteur);
        lblPorteur.setStyle("-fx-font-size:10px;-fx-text-fill:#64748B;");

        card.getChildren().addAll(header, badges, lblPorteur);

        if (lastMsg != null && !lastMsg.isBlank()) {
            Label lblLast = new Label(lastMsg.length() > 55 ? lastMsg.substring(0,53)+"..." : lastMsg);
            lblLast.setStyle("-fx-font-size:10px;-fx-text-fill:#94A3B8;");
            card.getChildren().add(lblLast);
        }

        card.setOnMouseClicked(e -> selectThread(threadId, projName, porteur, amount));
        return card;
    }

    // ── Chat ──────────────────────────────────────────────────────────────────

    private void selectThread(int threadId, String projName, String porteur, double amount) {
        selectedThreadId = threadId;
        markThreadRead(threadId);

        boxChatHeader.setVisible(true); boxChatHeader.setManaged(true);
        lblChatProject.setText(projName != null ? projName : "-");
        lblChatPorteur.setText("Porteur: " + porteur);
        if (amount > 0)
            lblChatInvestment.setText(String.format(Locale.ROOT, "Vous avez investi $%,.0f", amount));

        boxInput.setVisible(true); boxInput.setManaged(true);
        lblNoThread.setVisible(false);

        loadMessages(threadId);
        loadThreads();
    }

    private void loadMessages(int threadId) {
        boxMessages.getChildren().clear();
        var messages = convService.getMessagesForThread(threadId);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM HH:mm");

        for (var msg : messages) {
            boolean isMine = msg.getSenderId() == currentUserId;
            HBox row = new HBox();
            row.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

            VBox bubble = new VBox(4);
            bubble.setMaxWidth(480);
            Label content = new Label(msg.getContent());
            content.setWrapText(true);
            content.setStyle("-fx-font-size:13px;-fx-text-fill:" + (isMine ? "white" : "#0F172A") + ";");
            Label time = new Label(msg.getSentAt() != null ? msg.getSentAt().format(fmt) : "");
            time.setStyle("-fx-font-size:9px;-fx-text-fill:" + (isMine ? "rgba(255,255,255,0.7)" : "#94A3B8") + ";");
            bubble.getChildren().addAll(content, time);
            bubble.setStyle("-fx-background-color:" + (isMine ? "#10b981" : "white")
                + ";-fx-border-color:#E2E8F0;-fx-border-width:1;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:10 14;");

            row.getChildren().add(bubble);
            boxMessages.getChildren().add(row);
        }
        scrollToBottom();
    }

    private void markThreadRead(int threadId) {
        String sql = "UPDATE thread_messages SET is_read=1 WHERE thread_id=? AND sender_id!=? AND is_read=0";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, threadId); ps.setLong(2, currentUserId);
            ps.executeUpdate();
        } catch (SQLException e) { /* ignore */ }
    }

    private void scrollToBottom() {
        Platform.runLater(() -> scrollMessages.setVvalue(1.0));
    }

    private void navigate(String fxml) {
        try { org.GreenLedger.MainFX.setRoot(fxml); }
        catch (Exception e) { System.err.println("[InvestisseurMessages] Nav: " + e.getMessage()); }
    }
}
