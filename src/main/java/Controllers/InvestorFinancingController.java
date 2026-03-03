package Controllers;
import Services.ExchangeRateService;
import javafx.application.Platform;
import javafx.scene.layout.VBox;import Models.Financement;
import Models.OffreFinancement;
import Models.Projet;
import Models.User;
import Models.TypeUtilisateur;
import Services.FinancementService;
import Services.OffreFinancementService;
import Services.ProjetService;
import Utils.SessionManager;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;
import java.util.List;

public class InvestorFinancingController extends BaseController {

    private final ExchangeRateService exchangeRateService = new ExchangeRateService();

    @FXML private VBox conversionPanel;
    @FXML private Label lblConversionStatus;
    @FXML private Label lblTND;
    @FXML private Label lblEUR;
    @FXML private Label lblEURRate;
    @FXML private Label lblUSD;
    @FXML private Label lblUSDRate;
    @FXML private Label lblGBP;
    @FXML private Label lblGBPRate;
    @FXML private Label lblTotalInvestments;
    @FXML private Label lblTotalAmount;
    @FXML private Label lblProjectsFollowed;
    @FXML private Label lblSidebarTitle;
    @FXML private Label lblPageTitle;
    @FXML private Label lblProfileName;
    @FXML private Label lblProfileType;

    @FXML private Button btnNavDashboard;
    @FXML private Button btnNavInvestments;
    @FXML private Button btnNavFinancement;
    @FXML private Button btnNavSettings;
    @FXML private TableView<Financement> tableMyInvestments;
    @FXML private TableColumn<Financement, Integer> colInvProjetId;
    @FXML private TableColumn<Financement, Double> colInvMontant;
    @FXML private TableColumn<Financement, String> colInvDate;
    @FXML private TableColumn<Financement, String> colInvStatut;

    @FXML private TableView<OffreFinancement> tableFinancingOffers;
    @FXML private TableColumn<OffreFinancement, String> colOffreType;
    @FXML private TableColumn<OffreFinancement, Double> colOffreTaux;
    @FXML private TableColumn<OffreFinancement, Integer> colOffreDuree;
    @FXML private TableColumn<OffreFinancement, Integer> colOffreFinId;

    @FXML private ComboBox<Projet> cmbProjectSelection;
    @FXML private TextField txtInvestmentAmount;
    private final FinancementService financementService = new FinancementService();
    private final OffreFinancementService offreService = new OffreFinancementService();
    private final ProjetService projetService = new ProjetService();

    // Data
    private final ObservableList<Financement> myInvestments = FXCollections.observableArrayList();
    private final ObservableList<OffreFinancement> availableOffers = FXCollections.observableArrayList();
    private final ObservableList<Projet> projects = FXCollections.observableArrayList();

    private User currentUser;

    @FXML
    public void initialize() {
        super.initialize();

        try {
            currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null) {
                showError("Erreur", "Utilisateur non connecté");
                return;
            }

            configureNavigationForRole();
            applyProfile(lblProfileName, lblProfileType);
            setupTableColumns();
            loadData();
            setupComboBox();

            // ── ADDED: trigger currency conversion on row selection ──
            tableMyInvestments.getSelectionModel().selectedItemProperty()
                    .addListener((obs, oldVal, newVal) -> {
                        if (newVal != null) {
                            updateCurrencyConversion(newVal.getMontant());
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur d'initialisation", e.getMessage());
        }
    }

    private void configureNavigationForRole() {
        TypeUtilisateur type = currentUser != null ? currentUser.getTypeUtilisateur() : TypeUtilisateur.INVESTISSEUR;
        
        switch (type) {
            case ADMIN:
                applyNavLabels("GreenLedger Admin", "👑 Administration");
                configureNavButton(btnNavDashboard, "👑 Utilisateurs", () -> navigate("fxml/admin_users"));
                hideNavButton(btnNavInvestments);
                hideNavButton(btnNavFinancement);
                configureNavButton(btnNavSettings, "⚙️ Paramètres", () -> navigate("settings"));
                break;
            case EXPERT_CARBONE:
                applyNavLabels("GreenLedger Expert", "🧪 Espace Expert Carbone");
                configureNavButton(btnNavDashboard, "📁 Voir projets", () -> navigate("expertProjet"));
                configureNavButton(btnNavInvestments, "🧾 Evaluations carbone", () -> navigate("gestionCarbone"));
                hideNavButton(btnNavFinancement);
                configureNavButton(btnNavSettings, "⚙️ Paramètres", () -> navigate("settings"));
                break;
            case PORTEUR_PROJET:
                applyNavLabels("GreenLedger Projet", "📁 Gestion des projets");
                configureNavButton(btnNavDashboard, "📁 Mes projets", () -> navigate("GestionProjet"));
                configureNavButton(btnNavInvestments, "📊 Mes evaluations", () -> navigate("ownerEvaluations"));
                hideNavButton(btnNavFinancement);
                configureNavButton(btnNavSettings, "⚙️ Paramètres", () -> navigate("settings"));
                break;
            case INVESTISSEUR:
            default:
                applyNavLabels("GreenLedger Investisseur", "💰 Gestion des investissements");
                configureNavButton(btnNavDashboard, "📊 Tableau de bord", () -> navigate("fxml/dashboard"));
                configureNavButton(btnNavInvestments, "💰 Investissements", this::handleGoInvestments);
                configureNavButton(btnNavFinancement, "💳 Financement avancé", () -> navigate("financement"));
                configureNavButton(btnNavSettings, "⚙️ Paramètres", () -> navigate("settings"));
                break;
        }
    }
    
    private void updateCurrencyConversion(double montant) {
        // Placeholder for currency conversion logic
        System.out.println("Converting amount: " + montant);
    }

    private void applyNavLabels(String sidebarTitle, String pageTitle) {
        if (lblSidebarTitle != null) lblSidebarTitle.setText(sidebarTitle);
        if (lblPageTitle    != null) lblPageTitle.setText(pageTitle);
    }

    private void configureNavButton(Button button, String text, Runnable action) {
        if (button == null) return;        button.setText(text);
        button.setOnAction(event -> action.run());
        button.setVisible(true);
        button.setManaged(true);
    }

    private void hideNavButton(Button button) {
        if (button == null) return;        button.setVisible(false);
        button.setManaged(false);
    }

    private void navigate(String fxml) {
        try {
            org.GreenLedger.MainFX.setRoot(fxml);
        } catch (IOException ex) {
            System.err.println("[ERROR] Navigation error: " + ex.getMessage());            showError("Erreur", "Impossible de naviguer vers " + fxml);
        }
    }

    private void setupTableColumns() {        colInvProjetId.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getProjetId()).asObject());
        colInvMontant.setCellValueFactory(cd -> new SimpleDoubleProperty(cd.getValue().getMontant()).asObject());
        colInvDate.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getDateFinancement() != null ? cd.getValue().getDateFinancement() : "N/A"
        ));
        colInvStatut.setCellValueFactory(cd -> new SimpleStringProperty("Actif"));
        colOffreType.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getTypeOffre() != null ? cd.getValue().getTypeOffre() : "N/A"
        ));
        colOffreTaux.setCellValueFactory(cd -> new SimpleDoubleProperty(cd.getValue().getTaux()).asObject());
        colOffreDuree.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getDuree()).asObject());
        colOffreFinId.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getIdFinancement()).asObject());

        tableMyInvestments.setItems(myInvestments);
        tableFinancingOffers.setItems(availableOffers);
    }

    private void loadData() {
        try {
            refreshInvestments();
            refreshOffers();
            updateStatistics();
        } catch (Exception ex) {
            System.err.println("[ERROR] Data loading error: " + ex.getMessage());
        }
    }
    private void setupComboBox() {
        try {
            List<Projet> allProjects = projetService.afficher();
            if (allProjects != null) {
                projects.setAll(allProjects);
                cmbProjectSelection.setItems(projects);

                // Custom cell factory for better display
                cmbProjectSelection.setCellFactory(lv -> new ListCell<Projet>() {
                    @Override
                    protected void updateItem(Projet item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty ? "" : (item.getId() + " - " + item.getTitre()));
                    }
                });
                cmbProjectSelection.setButtonCell(new ListCell<Projet>() {
                    @Override
                    protected void updateItem(Projet item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty ? "" : (item.getId() + " - " + item.getTitre()));
                    }
                });
            }
        } catch (Exception ex) {
            System.err.println("[ERROR] ComboBox setup error: " + ex.getMessage());
        }
    }
    @FXML
    private void refreshInvestments() {
        try {
            List<Financement> investments = financementService.getAll();
            if (investments != null) myInvestments.setAll(investments);
            updateStatistics();
        } catch (Exception ex) {
            System.err.println("[ERROR] Refresh investments error: " + ex.getMessage());
        }
    }
    @FXML
    private void refreshOffers() {
        try {
            List<OffreFinancement> offers = offreService.getAll();
            if (offers != null) availableOffers.setAll(offers);
        } catch (Exception ex) {
            System.err.println("[ERROR] Refresh offers error: " + ex.getMessage());
        }
    }

    private void updateStatistics() {
        try {
            lblTotalInvestments.setText(String.valueOf(myInvestments.size()));
            double totalAmount = myInvestments.stream().mapToDouble(Financement::getMontant).sum();
            lblTotalAmount.setText(String.format("%.2f EUR", totalAmount));
            long projectsCount = myInvestments.stream().map(Financement::getProjetId).distinct().count();
            lblProjectsFollowed.setText(String.valueOf(projectsCount));
        } catch (Exception ex) {            lblTotalInvestments.setText("--");
            lblTotalAmount.setText("-- EUR");
            lblProjectsFollowed.setText("--");
        }
    }

    /**
     * Handle new investment submission
     */
    @FXML
    private void handleNewInvestment() {
        try {
            Projet selectedProject = cmbProjectSelection.getValue();
            String amountText = txtInvestmentAmount.getText();

            if (selectedProject == null) {
                showAlert("Attention", "Veuillez sélectionner un projet", Alert.AlertType.WARNING);
                return;
            }
            if (amountText == null || amountText.trim().isEmpty()) {
                showAlert("Attention", "Veuillez entrer un montant", Alert.AlertType.WARNING);
                return;
            }

            double amount = Double.parseDouble(amountText);
            if (amount <= 0) {
                showAlert("Attention", "Le montant doit être supérieur à 0", Alert.AlertType.WARNING);
                return;
            }

            Financement newInvestment = new Financement();
            newInvestment.setProjetId(selectedProject.getId());
            newInvestment.setMontant(amount);
            newInvestment.setDateFinancement(java.time.LocalDate.now().toString());
            newInvestment.setBanqueId(0);
            financementService.add(newInvestment);

            showAlert("Succès", "Investissement enregistré avec succès!", Alert.AlertType.INFORMATION);
            txtInvestmentAmount.clear();
            cmbProjectSelection.getSelectionModel().clearSelection();
            refreshInvestments();

        } catch (NumberFormatException nfe) {
            showAlert("Erreur", "Montant invalide.", Alert.AlertType.ERROR);
        } catch (Exception ex) {            showAlert("Erreur", "Impossible de créer l'investissement: " + ex.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML private void handleViewAllProjects() {
        showAlert("Information", "Fonctionnalité en développement", Alert.AlertType.INFORMATION);
    }
    @FXML private void handleViewPerformance() {
        showAlert("Information", "Fonctionnalité en développement", Alert.AlertType.INFORMATION);
    }
    @FXML private void handleReportIssue() {
        showAlert("Information", "Fonctionnalité en développement", Alert.AlertType.INFORMATION);
    }
    @FXML
    private void handleBack() {
        try {
            TypeUtilisateur type = currentUser != null ? currentUser.getTypeUtilisateur() : null;
            if (type == TypeUtilisateur.EXPERT_CARBONE)       org.GreenLedger.MainFX.setRoot("expertProjet");
            else if (type == TypeUtilisateur.PORTEUR_PROJET)  org.GreenLedger.MainFX.setRoot("GestionProjet");
            else if (type == TypeUtilisateur.ADMIN)           org.GreenLedger.MainFX.setRoot("fxml/admin_users");
            else                                               org.GreenLedger.MainFX.setRoot("fxml/dashboard");
        } catch (IOException ex) {            showError("Erreur", "Impossible de retourner au tableau de bord");
        }
    }

    @FXML
    private void handleGoDashboard() {
        TypeUtilisateur type = currentUser != null ? currentUser.getTypeUtilisateur() : null;
        if (type == TypeUtilisateur.EXPERT_CARBONE)       navigate("expertProjet");
        else if (type == TypeUtilisateur.PORTEUR_PROJET)  navigate("GestionProjet");
        else if (type == TypeUtilisateur.ADMIN)           navigate("fxml/admin_users");
        else                                               navigate("fxml/dashboard");
    }

    @FXML
    private void handleGoInvestments() {
        TypeUtilisateur type = currentUser != null ? currentUser.getTypeUtilisateur() : null;
        if (type == TypeUtilisateur.EXPERT_CARBONE)      { navigate("gestionCarbone"); return; }
        if (type == TypeUtilisateur.PORTEUR_PROJET)      { navigate("ownerEvaluations"); return; }
        if (type == TypeUtilisateur.ADMIN)               { navigate("fxml/admin_users"); return; }
        try { refreshInvestments(); refreshOffers(); } catch (Exception ex) { System.err.println(ex.getMessage()); }
    }

    @FXML
    private void handleGoFinancement() {
        TypeUtilisateur type = currentUser != null ? currentUser.getTypeUtilisateur() : null;
        if (type == TypeUtilisateur.EXPERT_CARBONE)      { navigate("gestionCarbone"); return; }
        if (type == TypeUtilisateur.PORTEUR_PROJET)      { navigate("GestionProjet"); return; }
        if (type == TypeUtilisateur.ADMIN)               { navigate("fxml/admin_users"); return; }
        navigate("financement");
    }

    @FXML private void handleGoSettings()  { navigate("settings"); }
    @FXML private void handleEditProfile() { navigate("editProfile"); }
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show alert
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void GoDashboard() {
        try {
            org.GreenLedger.MainFX.setRoot("Investment_dashboard");
        } catch (IOException ex) {
            System.err.println("[ERROR] Navigation error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
