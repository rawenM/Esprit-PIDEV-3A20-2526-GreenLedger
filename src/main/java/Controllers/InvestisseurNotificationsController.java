package Controllers;

import Services.InvestisseurService;
import Utils.SessionManager;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

/**
 * Investor Notifications page.
 */
public class InvestisseurNotificationsController extends BaseController {

    @FXML private Label lblUnreadCount;
    @FXML private VBox  boxNotifications;
    @FXML private Label lblEmpty;

    private final InvestisseurService investService = new InvestisseurService();
    private long currentUserId;

    @FXML
    public void initialize() {
        super.initialize();
        var user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;
        currentUserId = user.getId();
        loadNotifications();
    }

    @FXML private void onBack() { navigate("fxml/investisseur_shell"); }

    @FXML
    private void onMarkAllRead() {
        investService.markAllNotificationsRead(currentUserId);
        loadNotifications();
    }

    private void loadNotifications() {
        boxNotifications.getChildren().clear();
        List<InvestisseurService.NotifDTO> notifs = investService.getNotifications(currentUserId);

        long unread = notifs.stream().filter(n -> !n.read).count();
        if (lblUnreadCount != null) {
            lblUnreadCount.setText(unread > 0 ? String.valueOf(unread) : "");
            lblUnreadCount.setVisible(unread > 0);
        }

        if (notifs.isEmpty()) {
            if (lblEmpty != null) { lblEmpty.setVisible(true); lblEmpty.setManaged(true); }
            return;
        }
        if (lblEmpty != null) { lblEmpty.setVisible(false); lblEmpty.setManaged(false); }

        for (InvestisseurService.NotifDTO n : notifs) {
            boxNotifications.getChildren().add(buildCard(n));
        }
    }

    private HBox buildCard(InvestisseurService.NotifDTO n) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        String bg     = n.read ? "white" : "#F0FDF4";
        String border = n.read ? "#E2E8F0" : "#A7F3D0";
        card.setStyle("-fx-background-color:" + bg + ";-fx-border-color:" + border
            + ";-fx-border-width:1;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:14 16;");

        // Icon
        String icon = switch (n.type != null ? n.type : "") {
            case "payment_confirmed"    -> "✅";
            case "credits_in_wallet"    -> "🌿";
            case "new_project_available"-> "📁";
            case "new_message"          -> "💬";
            default                     -> "🔔";
        };
        Label lblIcon = new Label(icon);
        lblIcon.setStyle("-fx-font-size:18px;");

        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label lblMsg = new Label(n.message != null ? n.message : "");
        lblMsg.setWrapText(true);
        lblMsg.setStyle("-fx-font-size:12px;-fx-font-weight:" + (n.read ? "400" : "700")
            + ";-fx-text-fill:#0F172A;");
        Label lblTime = new Label(n.createdAt != null
            ? n.createdAt.substring(0, Math.min(16, n.createdAt.length())) : "");
        lblTime.setStyle("-fx-font-size:10px;-fx-text-fill:#94A3B8;");
        info.getChildren().addAll(lblMsg, lblTime);

        card.getChildren().addAll(lblIcon, info);

        if (!n.read) {
            Button btnRead = new Button("Lu");
            btnRead.setStyle("-fx-background-color:#F1F5F9;-fx-text-fill:#374151;-fx-font-size:10px;-fx-background-radius:6;-fx-padding:4 10;-fx-cursor:hand;");
            btnRead.setOnAction(e -> {
                investService.markNotificationRead(n.id, currentUserId);
                loadNotifications();
            });
            card.getChildren().add(btnRead);
        }
        return card;
    }

    private void navigate(String fxml) {
        try { org.GreenLedger.MainFX.setRoot(fxml); }
        catch (Exception e) { System.err.println("[InvestisseurNotif] Nav: " + e.getMessage()); }
    }
}
