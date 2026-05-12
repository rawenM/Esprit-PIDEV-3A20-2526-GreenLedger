package Controllers;

import Models.Projet;
import Services.InvestisseurService;
import Services.ProjetService;
import Utils.NavigationContext;
import Utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
<<<<<<< HEAD

=======
import javafx.scene.layout.StackPane;

import java.util.Arrays;
>>>>>>> 697f7351277b2a6316572ab9077f2061a493ce44
import java.util.List;
import java.util.Locale;

/**
 * Investisseur Shell — main dashboard.
 * Login redirect → investisseur_shell.fxml
 */
public class InvestisseurShellController extends BaseController {

    @FXML private Label lblPageTitle;
    @FXML private Label lblProfileName;
    @FXML private Label lblProfileType;
    @FXML private Label lblUnreadBadge;
    @FXML private Label lblHeroGreeting;
    @FXML private Label lblHealthScore;

<<<<<<< HEAD
=======
    // Sidebar nav buttons (for active state)
    @FXML private Button btnDashboard;
    @FXML private Button btnFinancing;
    @FXML private Button btnPortfolio;
    @FXML private Button btnWallet;
    @FXML private Button btnMarketplace;
    @FXML private Button btnMessages;

    // Content area for in-shell navigation
    @FXML private StackPane mainArea;
    @FXML private javafx.scene.control.ScrollPane dashboardScroll;

>>>>>>> 697f7351277b2a6316572ab9077f2061a493ce44
    // KPI Row 1
    @FXML private Label statCredits;
    @FXML private Label statCreditsRetired;
    @FXML private Label statWallets;
    @FXML private Label statWalletsSub;
    @FXML private Label statProjects;
    @FXML private Label statProjectsSub;
    @FXML private Label statPipeline;
    @FXML private Label statPipelineSub;

    // KPI Row 2
    @FXML private Label statListings;
    @FXML private Label statOrders;
    @FXML private Label statHealth;
    @FXML private Label statReadiness;

    // Projects table
    @FXML private TableView<Projet>          tableProjects;
    @FXML private TableColumn<Projet,String> colTitre;
    @FXML private TableColumn<Projet,String> colSecteur;
    @FXML private TableColumn<Projet,String> colEsg;
    @FXML private TableColumn<Projet,String> colMontant;
    @FXML private TableColumn<Projet,String> colFraud;
    @FXML private TableColumn<Projet,String> colAction;

    private final ProjetService        projetService  = new ProjetService();
    private final InvestisseurService  investService  = new InvestisseurService();

    @FXML
    public void initialize() {
        super.initialize();
        applyProfile(lblProfileName, lblProfileType);
        setupTable();
        loadDashboard();
    }

    // ── Navigation ────────────────────────────────────────────────────────────

<<<<<<< HEAD
    @FXML private void onDashboard()     { loadDashboard(); lblPageTitle.setText("Tableau de bord"); }
    @FXML private void onFinancing()     { navigate("fxml/swipe_invest"); }
    @FXML private void onPortfolio()     { navigate("fxml/investisseur_portfolio"); }
    @FXML private void onWallet()        { navigate("greenwallet"); }
    @FXML private void onMarketplace()   { navigate("fxml/marketplace"); }
    @FXML private void onMessages()      { navigate("fxml/investisseur_messages"); }
    @FXML private void onAssistant()     { navigate("AssistantChat"); }
    @FXML private void onNotifications() { navigate("fxml/investisseur_notifications"); }
=======
    @FXML private void onDashboard()     { setActiveBtn(btnDashboard); showDashboard(); lblPageTitle.setText("Tableau de bord"); }
    @FXML private void onFinancing()     { setActiveBtn(btnFinancing);  loadContent("fxml/investor_financing"); lblPageTitle.setText("Financement"); }
    @FXML private void onPortfolio()     { setActiveBtn(btnPortfolio);  loadContent("fxml/investisseur_portfolio"); lblPageTitle.setText("Mon portefeuille"); }
    @FXML private void onWallet()        { setActiveBtn(btnWallet);     navigate("greenwallet"); }
    @FXML private void onMarketplace()   { setActiveBtn(btnMarketplace); loadContent("fxml/marketplace"); lblPageTitle.setText("Marché carbone"); }
    @FXML private void onMessages()      { setActiveBtn(btnMessages);   loadContent("fxml/investisseur_messages"); lblPageTitle.setText("Messages"); }
    @FXML private void onAssistant()     { navigate("AssistantChat"); }
    @FXML private void onNotifications() { loadContent("fxml/investisseur_notifications"); lblPageTitle.setText("Notifications"); }
>>>>>>> 697f7351277b2a6316572ab9077f2061a493ce44
    @FXML private void onEditProfile()   { navigate("editProfile"); }

    @FXML private void onBack() {
        String prev = NavigationContext.getInstance().getPreviousPage();
        if (prev != null && !prev.isEmpty() && !prev.equals(NavigationContext.getInstance().getCurrentPage()))
            navigate(prev);
        else navigate("fxml/login");
    }

    @FXML private void onLogout() {
        SessionManager.getInstance().invalidate();
        navigate("fxml/login");
    }

<<<<<<< HEAD
=======
    // ── Content loading (keeps sidebar) ──────────────────────────────────────

    private void showDashboard() {
        if (mainArea != null && dashboardScroll != null) {
            mainArea.getChildren().setAll(dashboardScroll);
        }
        loadDashboard();
    }

    private void loadContent(String fxmlPath) {
        if (mainArea == null) { navigate(fxmlPath); return; }
        try {
            java.net.URL resource = getClass().getResource("/" + fxmlPath + ".fxml");
            if (resource == null) { System.err.println("[InvestisseurShell] FXML not found: " + fxmlPath); return; }
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(resource);
            javafx.scene.Parent content = loader.load();
            mainArea.getChildren().setAll(content);
        } catch (Exception e) {
            System.err.println("[InvestisseurShell] loadContent error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setActiveBtn(Button active) {
        java.util.List<Button> all = Arrays.asList(
            btnDashboard, btnFinancing, btnPortfolio, btnWallet, btnMarketplace, btnMessages);
        String inactive = "-fx-background-color:transparent; -fx-text-fill:#374151; -fx-font-size:12px; -fx-font-weight:500; -fx-background-radius:8; -fx-padding:11 14; -fx-cursor:hand; -fx-border-width:0; -fx-background-insets:0;";
        String activeStyle = "-fx-background-color:#f0fdf4; -fx-text-fill:#059669; -fx-font-size:12px; -fx-font-weight:700; -fx-background-radius:8; -fx-padding:11 14; -fx-cursor:hand; -fx-border-width:0 0 0 3; -fx-border-color:#059669; -fx-background-insets:0; -fx-effect:none;";
        for (Button b : all) { if (b != null) b.setStyle(inactive); }
        if (active != null) active.setStyle(activeStyle);
    }

>>>>>>> 697f7351277b2a6316572ab9077f2061a493ce44
    // ── Dashboard ─────────────────────────────────────────────────────────────

    private void loadDashboard() {
        var user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        long uid = user.getId();

        // Hero greeting
        if (lblHeroGreeting != null)
            lblHeroGreeting.setText("Bonjour " + user.getPrenom() + " \uD83D\uDC4B");

        // Dashboard data
        InvestisseurService.DashboardData d = investService.getDashboardData(uid);

        // KPI Row 1
        set(statCredits,      fmt(d.totalAvailable) + " tCO2e");
        set(statCreditsRetired, fmt(d.totalRetired) + " retires");
        set(statWallets,      String.valueOf(d.totalWallets));
        set(statWalletsSub,   d.zeroBalance + " a solde nul");
        set(statProjects,     String.valueOf(d.fundedProjects));
        set(statProjectsSub,  "Projets finances");
        set(statPipeline,     String.valueOf(d.totalFinancements));
        set(statPipelineSub,  "Total financements");

        // KPI Row 2
        set(statListings,  String.valueOf(d.activeListings));
        set(statOrders,    String.valueOf(d.myOrders));
        set(statHealth,    String.valueOf((int) d.healthScore));
        set(statReadiness, String.valueOf((int) d.readinessScore));

        // Hero health score
        if (lblHealthScore != null) {
            lblHealthScore.setText(String.valueOf((int) d.healthScore));
        }

        // Unread badge
        if (lblUnreadBadge != null) {
            lblUnreadBadge.setText(d.unreadMessages > 0 ? String.valueOf(d.unreadMessages) : "");
            lblUnreadBadge.setVisible(d.unreadMessages > 0);
        }

        // Projects table — APPROVED + SEEKING_FUNDING only
        List<InvestisseurService.ProjetInvestDTO> dtos =
            investService.getProjectsForInvestment(null, null, null, null, null);
        List<Projet> projets = dtos.stream().map(dto -> {
            Projet p = new Projet();
            p.setId(dto.id);
            p.setTitre(dto.titre);
            p.setSecteur(dto.secteur);
            p.setScoreEsg(dto.scoreEsg);
            p.setMontantDemande(dto.montantDemande);
            p.setFraudRiskScore(dto.fraudRiskScore);
            return p;
        }).collect(java.util.stream.Collectors.toList());
        tableProjects.setItems(FXCollections.observableArrayList(projets.stream().limit(10).collect(java.util.stream.Collectors.toList())));
    }

    // ── Table ─────────────────────────────────────────────────────────────────

    private void setupTable() {
        colTitre.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getTitre()));
        colSecteur.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getSecteur() != null ? c.getValue().getSecteur() : "—"));
        colEsg.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                Integer esg = getTableRow().getItem().getScoreEsg();
                if (esg == null) { setText("—"); setStyle("-fx-text-fill:#94A3B8;-fx-alignment:CENTER;"); return; }
                String color = esg >= 7 ? "#10b981" : esg >= 5 ? "#f59e0b" : "#f43f5e";
                Label badge = new Label("ESG " + esg);
                badge.setStyle("-fx-background-color:" + color + "18;-fx-text-fill:" + color
                    + ";-fx-font-size:10px;-fx-font-weight:700;-fx-background-radius:5;-fx-padding:3 7;");
                setGraphic(badge); setText(null);
            }
        });
        colMontant.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getMontantDemande() != null
                ? String.format(Locale.ROOT, "%,.0f TND", c.getValue().getMontantDemande()) : "—"));
        colFraud.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                Double risk = getTableRow().getItem().getFraudRiskScore();
                String level = InvestisseurService.fraudLevel(risk);
                String color = InvestisseurService.fraudColor(risk);
                Label badge = new Label(level);
                badge.setStyle("-fx-background-color:" + color + "18;-fx-text-fill:" + color
                    + ";-fx-font-size:10px;-fx-font-weight:700;-fx-background-radius:5;-fx-padding:3 7;");
                setGraphic(badge); setText(null);
            }
        });
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Investir");
            {
                btn.setStyle("-fx-background-color:#10b981;-fx-text-fill:white;-fx-font-size:10px;-fx-font-weight:700;-fx-background-radius:7;-fx-padding:6 12;-fx-cursor:hand;");
                btn.setOnAction(e -> {
                    Projet p = getTableView().getItems().get(getIndex());
                    NavigationContext.getInstance().setCurrentProjectId(p.getId());
                    navigate("fxml/swipe_invest");
                });
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void set(Label lbl, String text) { if (lbl != null) lbl.setText(text); }
    private String fmt(double v) { return String.format(Locale.ROOT, "%,.1f", v); }

    private void navigate(String fxml) {
        try { org.GreenLedger.MainFX.setRoot(fxml); }
        catch (Exception e) { System.err.println("[InvestisseurShell] Nav: " + e.getMessage()); }
    }
}

