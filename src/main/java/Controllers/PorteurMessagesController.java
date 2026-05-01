package Controllers;

import DataBase.MyConnection;
import Models.ConversationThread;
import Models.ThreadMessage;
import Services.ConversationService;
import Utils.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Page 7 — Messages for Porteur de Projet.
 * Shows conversation threads where porteur_id = currentUser.id
 */
public class PorteurMessagesController extends BaseController {

    @FXML private Label  lblUnreadTotal;
    @FXML private VBox   boxThreads;
    @FXML private HBox   boxThreadHeader;
    @FXML private Label  lblThreadProject;
    @FXML private Label  lblThreadInvestor;
    @FXML private Label  lblInvestmentBadge;
    @FXML private VBox   boxMessages;
    @FXML private Label  lblNoThread;
    @FXML private HBox   boxInput;
    @FXML private TextArea txtMessage;
    @FXML private javafx.scene.control.ScrollPane scrollMessages;

    private final ConversationService convService = new ConversationService();
    private int selectedThreadId = -1;
    private long currentUserId;

    @FXML
    public void initialize() {
        super.initialize();
        var user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;
        currentUserId = user.getId();
        loadThreads();
    }

    @FXML private void onBack() { navigate("fxml/porteur_shell"); }

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

    // ── Load threads ──────────────────────────────────────────────────────

    private void loadThreads() {
        boxThreads.getChildren().clear();
        int totalUnread = 0;

        String sql =
            "SELECT ct.id, ct.project_id, ct.investisseur_id, " +
            "p.titre AS project_name, " +
            "u.nom AS inv_nom, u.prenom AS inv_prenom, " +
            "f.montant AS investment_amount, " +
            "(SELECT COUNT(*) FROM thread_messages tm " +
            " WHERE tm.thread_id = ct.id AND tm.sender_id != ? AND tm.is_read = 0) AS unread_count, " +
            "(SELECT content FROM thread_messages tm " +
            " WHERE tm.thread_id = ct.id ORDER BY tm.sent_at DESC LIMIT 1) AS last_message " +
            "FROM conversation_threads ct " +
            "JOIN projet p ON p.id = ct.project_id " +
            "JOIN user u ON u.id = ct.investisseur_id " +
            "LEFT JOIN financements f ON f.project_id = ct.project_id " +
            "  AND f.investisseur_id = ct.investisseur_id AND f.statut = 'COMPLETED' " +
            "WHERE ct.porteur_id = ? " +
            "ORDER BY f.montant DESC";

        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, currentUserId);
            ps.setLong(2, currentUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int threadId     = rs.getInt("id");
                    String projName  = rs.getString("project_name");
                    String invName   = rs.getString("inv_prenom") + " " + rs.getString("inv_nom");
                    double amount    = rs.getDouble("investment_amount");
                    int unread       = rs.getInt("unread_count");
                    String lastMsg   = rs.getString("last_message");
                    totalUnread += unread;

                    VBox card = buildThreadCard(threadId, projName, invName, amount, unread, lastMsg);
                    boxThreads.getChildren().add(card);
                }
            }
        } catch (SQLException e) {
            System.err.println("[PorteurMessages] loadThreads: " + e.getMessage());
        }

        lblUnreadTotal.setText(totalUnread > 0 ? String.valueOf(totalUnread) : "");
        lblUnreadTotal.setVisible(totalUnread > 0);
    }

    private VBox buildThreadCard(int threadId, String projName, String invName,
                                  double amount, int unread, String lastMsg) {
        VBox card = new VBox(4);
        card.setStyle("-fx-padding:12 16;-fx-border-color:#E5E7EB;-fx-border-width:0 0 1 0;-fx-cursor:hand;"
            + (threadId == selectedThreadId ? "-fx-background-color:#F0FDF4;" : "-fx-background-color:white;"));

        HBox header = new HBox(6);
        header.setAlignment(Pos.CENTER_LEFT);
        Label lblProj = new Label(projName != null ? projName : "-");
        lblProj.setStyle("-fx-font-size:12px;-fx-font-weight:700;-fx-text-fill:#1A2E26;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        header.getChildren().addAll(lblProj, sp);

        if (unread > 0) {
            Label badge = new Label(String.valueOf(unread));
            badge.setStyle("-fx-background-color:#EF4444;-fx-text-fill:white;-fx-font-size:9px;-fx-font-weight:700;-fx-background-radius:10;-fx-padding:1 6;");
            header.getChildren().add(badge);
        }

        Label lblInv = new Label(invName);
        lblInv.setStyle("-fx-font-size:11px;-fx-text-fill:#6B7280;");

        // Priority badge
        String priority = amount >= 20000 ? "Major Investment" : amount >= 10000 ? "High Priority"
                        : amount >= 5000  ? "Priority"         : "Standard";
        String pColor   = amount >= 20000 ? "#FEE2E2;-fx-text-fill:#991B1B"
                        : amount >= 10000 ? "#FFEDD5;-fx-text-fill:#9A3412"
                        : amount >= 5000  ? "#FEF9C3;-fx-text-fill:#854D0E"
                        : "#F3F4F6;-fx-text-fill:#374151";
        Label lblPriority = new Label(priority);
        lblPriority.setStyle("-fx-background-color:" + pColor + ";-fx-font-size:9px;-fx-font-weight:700;-fx-background-radius:4;-fx-padding:2 6;");

        if (lastMsg != null && !lastMsg.isBlank()) {
            Label lblLast = new Label(lastMsg.length() > 50 ? lastMsg.substring(0, 48) + "..." : lastMsg);
            lblLast.setStyle("-fx-font-size:10px;-fx-text-fill:#9CA3AF;");
            card.getChildren().addAll(header, lblInv, lblPriority, lblLast);
        } else {
            card.getChildren().addAll(header, lblInv, lblPriority);
        }

        card.setOnMouseClicked(e -> selectThread(threadId, projName, invName, amount));
        return card;
    }

    // ── Select thread ─────────────────────────────────────────────────────

    private void selectThread(int threadId, String projName, String invName, double amount) {
        selectedThreadId = threadId;

        // Mark as read
        markThreadRead(threadId);

        // Update header
        boxThreadHeader.setVisible(true);
        boxThreadHeader.setManaged(true);
        lblThreadProject.setText(projName != null ? projName : "-");
        lblThreadInvestor.setText("Investisseur: " + invName);
        if (amount > 0) {
            lblInvestmentBadge.setText(String.format(Locale.ROOT, "%.0f TND", amount));
            lblInvestmentBadge.setStyle("-fx-background-color:#D1FAE5;-fx-text-fill:#065F46;-fx-font-size:10px;-fx-font-weight:700;-fx-background-radius:4;-fx-padding:3 8;");
        }

        boxInput.setVisible(true);
        boxInput.setManaged(true);
        lblNoThread.setVisible(false);

        loadMessages(threadId);
        loadThreads(); // refresh unread counts
    }

    private void loadMessages(int threadId) {
        boxMessages.getChildren().clear();

        List<ThreadMessage> messages = convService.getMessagesForThread(threadId);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM HH:mm");

        for (ThreadMessage msg : messages) {
            boolean isMine = msg.getSenderId() == currentUserId;
            HBox row = new HBox();
            row.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

            VBox bubble = new VBox(4);
            bubble.setMaxWidth(500);
            Label content = new Label(msg.getContent());
            content.setWrapText(true);
            content.setStyle("-fx-font-size:13px;-fx-text-fill:" + (isMine ? "white" : "#1A2E26") + ";");
            Label time = new Label(msg.getSentAt() != null ? msg.getSentAt().format(fmt) : "");
            time.setStyle("-fx-font-size:9px;-fx-text-fill:" + (isMine ? "rgba(255,255,255,0.7)" : "#9CA3AF") + ";");
            bubble.getChildren().addAll(content, time);
            bubble.setStyle("-fx-background-color:" + (isMine ? "#2D5F3F" : "white")
                + ";-fx-border-color:#E5E7EB;-fx-border-width:1;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:10 14;");

            row.getChildren().add(bubble);
            boxMessages.getChildren().add(row);
        }
        scrollToBottom();
    }

    private void markThreadRead(int threadId) {
        String sql = "UPDATE thread_messages SET is_read=1 WHERE thread_id=? AND sender_id!=? AND is_read=0";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, threadId);
            ps.setLong(2, currentUserId);
            ps.executeUpdate();
        } catch (SQLException e) { /* ignore */ }
    }

    private void scrollToBottom() {
        Platform.runLater(() -> scrollMessages.setVvalue(1.0));
    }

    private void navigate(String fxml) {
        try { org.GreenLedger.MainFX.setRoot(fxml); }
        catch (Exception e) { System.err.println("[PorteurMessages] Nav: " + e.getMessage()); }
    }
}
