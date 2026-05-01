package Controllers;

import DataBase.MyConnection;
import Utils.SessionManager;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Page 8 — Notifications for Porteur de Projet.
 */
public class PorteurNotificationsController extends BaseController {

    @FXML private VBox boxNotifications;
    @FXML private Label lblEmpty;

    private long currentUserId;

    @FXML
    public void initialize() {
        super.initialize();
        var user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;
        currentUserId = user.getId();
        loadNotifications();
    }

    @FXML private void onBack() { navigate("fxml/porteur_shell"); }

    @FXML
    private void onMarkAllRead() {
        String sql = "UPDATE notifications SET is_read=1 WHERE user_id=? AND is_read=0";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, currentUserId);
            ps.executeUpdate();
        } catch (SQLException e) { /* ignore */ }
        loadNotifications();
    }

    private void loadNotifications() {
        boxNotifications.getChildren().clear();

        String sql = "SELECT id, type, message, is_read, created_at, related_project_id " +
                     "FROM notifications WHERE user_id=? " +
                     "ORDER BY is_read ASC, created_at DESC LIMIT 20";
        int count = 0;
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, currentUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long id       = rs.getLong("id");
                    String type   = rs.getString("type");
                    String msg    = rs.getString("message");
                    boolean read  = rs.getBoolean("is_read");
                    LocalDateTime created = rs.getObject("created_at", LocalDateTime.class);
                    boxNotifications.getChildren().add(buildCard(id, type, msg, read, created));
                    count++;
                }
            }
        } catch (SQLException e) {
            System.err.println("[PorteurNotif] loadNotifications: " + e.getMessage());
        }

        lblEmpty.setVisible(count == 0);
        lblEmpty.setManaged(count == 0);
    }

    private HBox buildCard(long id, String type, String msg, boolean read, LocalDateTime created) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color:" + (read ? "white" : "#F0FDF4")
            + ";-fx-border-color:#E5E7EB;-fx-border-width:1;-fx-border-radius:8;-fx-background-radius:8;-fx-padding:12 16;");

        // Icon
        String icon = switch (type != null ? type : "") {
            case "project_approved"  -> "✅";
            case "project_rejected"  -> "❌";
            case "project_funded"    -> "💰";
            case "new_message"       -> "💬";
            case "credits_minted"    -> "🌿";
            default                  -> "🔔";
        };
        Label lblIcon = new Label(icon);
        lblIcon.setStyle("-fx-font-size:18px;");

        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label lblMsg = new Label(msg != null ? msg : "");
        lblMsg.setWrapText(true);
        lblMsg.setStyle("-fx-font-size:12px;-fx-font-weight:" + (read ? "400" : "700")
            + ";-fx-text-fill:#1A2E26;");
        Label lblTime = new Label(created != null
            ? created.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "");
        lblTime.setStyle("-fx-font-size:10px;-fx-text-fill:#9CA3AF;");
        info.getChildren().addAll(lblMsg, lblTime);

        card.getChildren().addAll(lblIcon, info);

        if (!read) {
            Button btnRead = new Button("Lu");
            btnRead.setStyle("-fx-background-color:#F3F4F6;-fx-text-fill:#374151;-fx-font-size:10px;-fx-background-radius:5;-fx-padding:4 10;-fx-cursor:hand;");
            btnRead.setOnAction(e -> {
                markRead(id);
                loadNotifications();
            });
            card.getChildren().add(btnRead);
        }
        return card;
    }

    private void markRead(long notifId) {
        String sql = "UPDATE notifications SET is_read=1 WHERE id=? AND user_id=?";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, notifId);
            ps.setLong(2, currentUserId);
            ps.executeUpdate();
        } catch (SQLException e) { /* ignore */ }
    }

    private void navigate(String fxml) {
        try { org.GreenLedger.MainFX.setRoot(fxml); }
        catch (Exception e) { System.err.println("[PorteurNotif] Nav: " + e.getMessage()); }
    }
}
