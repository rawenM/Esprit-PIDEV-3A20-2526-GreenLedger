package Controllers;

import Services.InvestisseurService;
import Utils.NavigationContext;
import Utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Locale;

/**
 * Investor Portfolio — grid of funded projects.
 */
public class InvestisseurPortfolioController extends BaseController {

    @FXML private Label    lblTotalProjects;
    @FXML private Label    lblTotalInvested;
    @FXML private Label    lblTotalCredits;
    @FXML private Label    lblEmpty;
    @FXML private FlowPane flowCards;

    private final InvestisseurService investService = new InvestisseurService();

    @FXML
    public void initialize() {
        super.initialize();
        loadPortfolio();
    }

    @FXML private void onBack() { navigate("fxml/investisseur_shell"); }

    private void loadPortfolio() {
        var user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        List<InvestisseurService.PortfolioItem> items = investService.getPortfolio(user.getId());

        // Stats
        double totalInvested = items.stream().mapToDouble(i -> i.fundedAmount).sum();
        double totalCredits  = items.stream()
            .mapToDouble(i -> i.dispatchedGreenCredits != null ? i.dispatchedGreenCredits : 0).sum();

        set(lblTotalProjects, String.valueOf(items.size()));
        set(lblTotalInvested, String.format(Locale.ROOT, "$%,.0f", totalInvested));
        set(lblTotalCredits,  String.format(Locale.ROOT, "%.1f tCO2", totalCredits));

        if (items.isEmpty()) {
            if (lblEmpty != null) { lblEmpty.setVisible(true); lblEmpty.setManaged(true); }
            return;
        }
        if (lblEmpty != null) { lblEmpty.setVisible(false); lblEmpty.setManaged(false); }

        for (InvestisseurService.PortfolioItem item : items) {
            flowCards.getChildren().add(buildCard(item));
        }
    }

    private VBox buildCard(InvestisseurService.PortfolioItem item) {
        VBox card = new VBox(0);
        card.setPrefWidth(340);
        card.setStyle("-fx-background-color:white;-fx-border-color:#E2E8F0;-fx-border-width:1;"
            + "-fx-border-radius:14;-fx-background-radius:14;"
            + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),8,0,0,2);");

        // Card header
        VBox header = new VBox(6);
        header.setStyle("-fx-background-color:linear-gradient(to right,#F8FAFC,#F0FDF4);"
            + "-fx-border-radius:14 14 0 0;-fx-background-radius:14 14 0 0;-fx-padding:16 18;");

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        String titre = item.titre != null ? item.titre : "Projet";
        Label lblTitle = new Label(titre.length() > 28 ? titre.substring(0,26)+"..." : titre);
        lblTitle.setStyle("-fx-font-size:13px;-fx-font-weight:800;-fx-text-fill:#0F172A;");
        javafx.scene.layout.Region sp = new javafx.scene.layout.Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        titleRow.getChildren().addAll(lblTitle, sp);

        if (item.scoreEsg != null) {
            Label esgBadge = new Label("ESG " + item.scoreEsg + "/10");
            esgBadge.setStyle("-fx-background-color:#D1FAE5;-fx-text-fill:#10b981;-fx-font-size:9px;-fx-font-weight:700;-fx-background-radius:5;-fx-padding:3 8;");
            titleRow.getChildren().add(esgBadge);
        }

        Label lblDate = new Label(item.fundedAt != null ? "Finance le " + item.fundedAt.substring(0, Math.min(10, item.fundedAt.length())) : "");
        lblDate.setStyle("-fx-font-size:10px;-fx-text-fill:#94A3B8;");
        header.getChildren().addAll(titleRow, lblDate);

        // Body
        VBox body = new VBox(12);
        body.setStyle("-fx-padding:16 18;");

        // Metrics row
        HBox metrics = new HBox(10);
        VBox leftMetric = new VBox(4);
        leftMetric.setStyle("-fx-background-color:#F8FAFC;-fx-border-radius:8;-fx-background-radius:8;-fx-padding:10 14;");
        HBox.setHgrow(leftMetric, Priority.ALWAYS);
        Label lblAmtLabel = new Label("INVESTISSEMENT");
        lblAmtLabel.setStyle("-fx-font-size:8px;-fx-font-weight:700;-fx-text-fill:#94A3B8;");
        Label lblAmt = new Label(String.format(Locale.ROOT, "$%,.0f", item.fundedAmount));
        lblAmt.setStyle("-fx-font-size:16px;-fx-font-weight:800;-fx-text-fill:#0F172A;");
        leftMetric.getChildren().addAll(lblAmtLabel, lblAmt);

        VBox rightMetric = new VBox(4);
        rightMetric.setStyle("-fx-background-color:#F0FDF4;-fx-border-radius:8;-fx-background-radius:8;-fx-padding:10 14;");
        HBox.setHgrow(rightMetric, Priority.ALWAYS);
        Label lblCredLabel = new Label("CREDITS CARBONE");
        lblCredLabel.setStyle("-fx-font-size:8px;-fx-font-weight:700;-fx-text-fill:#94A3B8;");
        double cred = item.dispatchedGreenCredits != null ? item.dispatchedGreenCredits : 0;
        Label lblCred = new Label(String.format(Locale.ROOT, "%.2f tCO2", cred));
        lblCred.setStyle("-fx-font-size:16px;-fx-font-weight:800;-fx-text-fill:#10b981;");
        rightMetric.getChildren().addAll(lblCredLabel, lblCred);
        metrics.getChildren().addAll(leftMetric, rightMetric);

        // Description
        if (item.description != null && !item.description.isBlank()) {
            Label desc = new Label(item.description.length() > 90
                ? item.description.substring(0,88)+"..." : item.description);
            desc.setWrapText(true);
            desc.setStyle("-fx-font-size:11px;-fx-text-fill:#64748B;");
            body.getChildren().add(desc);
        }

        // Action buttons
        HBox actions = new HBox(8);
        Button btnMsg = new Button("Contact Team");
        btnMsg.setStyle("-fx-background-color:#10b981;-fx-text-fill:white;-fx-font-size:11px;-fx-font-weight:700;-fx-background-radius:8;-fx-padding:8 14;-fx-cursor:hand;");
        btnMsg.setOnAction(e -> navigate("fxml/investisseur_messages"));

        Button btnView = new Button("Voir details");
        btnView.setStyle("-fx-background-color:#F1F5F9;-fx-text-fill:#374151;-fx-font-size:11px;-fx-font-weight:600;-fx-background-radius:8;-fx-padding:8 14;-fx-cursor:hand;");
        btnView.setOnAction(e -> {
            NavigationContext.getInstance().setCurrentProjectId(item.projectId);
            navigate("ProjetDetail");
        });
        actions.getChildren().addAll(btnMsg, btnView);

        body.getChildren().addAll(metrics, actions);
        card.getChildren().addAll(header, body);
        return card;
    }

    private void set(Label lbl, String text) { if (lbl != null) lbl.setText(text); }

    private void navigate(String fxml) {
        try { org.GreenLedger.MainFX.setRoot(fxml); }
        catch (Exception e) { System.err.println("[InvestisseurPortfolio] Nav: " + e.getMessage()); }
    }
}
