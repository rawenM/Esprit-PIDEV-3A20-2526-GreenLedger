package Controllers;
<<<<<<< HEAD
import Services.ExchangeRateService;
import javafx.application.Platform;
import javafx.scene.layout.VBox;
=======

>>>>>>> yassine_antar
import Models.Financement;
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
<<<<<<< HEAD
=======
import javafx.event.ActionEvent;
>>>>>>> yassine_antar
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;
import java.util.List;

<<<<<<< HEAD
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

=======
/**
 * Controller for Investor Financing View
 * Provides a simplified financing interface for investors
 */
public class InvestorFinancingController extends BaseController {

    // Statistics Labels
>>>>>>> yassine_antar
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

<<<<<<< HEAD
=======
    // My Investments Table
>>>>>>> yassine_antar
    @FXML private TableView<Financement> tableMyInvestments;
    @FXML private TableColumn<Financement, Integer> colInvProjetId;
    @FXML private TableColumn<Financement, Double> colInvMontant;
    @FXML private TableColumn<Financement, String> colInvDate;
    @FXML private TableColumn<Financement, String> colInvStatut;

<<<<<<< HEAD
=======
    // Financing Offers Table
>>>>>>> yassine_antar
    @FXML private TableView<OffreFinancement> tableFinancingOffers;
    @FXML private TableColumn<OffreFinancement, String> colOffreType;
    @FXML private TableColumn<OffreFinancement, Double> colOffreTaux;
    @FXML private TableColumn<OffreFinancement, Integer> colOffreDuree;
    @FXML private TableColumn<OffreFinancement, Integer> colOffreFinId;

<<<<<<< HEAD
    @FXML private ComboBox<Projet> cmbProjectSelection;
    @FXML private TextField txtInvestmentAmount;

=======
    // Investment Form Controls
    @FXML private ComboBox<Projet> cmbProjectSelection;
    @FXML private TextField txtInvestmentAmount;

    // Services
>>>>>>> yassine_antar
    private final FinancementService financementService = new FinancementService();
    private final OffreFinancementService offreService = new OffreFinancementService();
    private final ProjetService projetService = new ProjetService();

<<<<<<< HEAD
=======
    // Data
>>>>>>> yassine_antar
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

<<<<<<< HEAD
            // ── ADDED: trigger currency conversion on row selection ──
            tableMyInvestments.getSelectionModel().selectedItemProperty()
                    .addListener((obs, oldVal, newVal) -> {
                        if (newVal != null) {
                            updateCurrencyConversion(newVal.getMontant());
                        }
                    });

=======
>>>>>>> yassine_antar
        } catch (Exception ex) {
            System.err.println("[ERROR] Initialization error: " + ex.getMessage());
            ex.printStackTrace();
            showError("Erreur d'initialisation", ex.getMessage());
        }
    }

<<<<<<< HEAD
    // ─────────────────────────────────────────────────────────────
    // ADDED: Currency conversion method
    // Called every time a row is selected in tableMyInvestments
    // Runs the API call on a background thread — UI never freezes
    // ─────────────────────────────────────────────────────────────
    private void updateCurrencyConversion(double montant) {

        // Show panel and set loading state immediately
        if (conversionPanel != null) {
            conversionPanel.setVisible(true);
            conversionPanel.setManaged(true);
        }
        if (lblConversionStatus != null) lblConversionStatus.setText("Chargement...");
        if (lblTND  != null) lblTND.setText(String.format("%,.2f", montant));
        if (lblEUR  != null) lblEUR.setText("—");
        if (lblUSD  != null) lblUSD.setText("—");
        if (lblGBP  != null) lblGBP.setText("—");
        if (lblEURRate != null) lblEURRate.setText("");
        if (lblUSDRate != null) lblUSDRate.setText("");
        if (lblGBPRate != null) lblGBPRate.setText("");

        // Background thread for the API call
        Thread apiThread = new Thread(() -> {
            try {
                ExchangeRateService.ConversionResult result =
                        exchangeRateService.convert(montant);

                // Back to JavaFX thread to update UI
                Platform.runLater(() -> {
                    if (lblEUR != null)
                        lblEUR.setText(String.format("%,.2f €", result.getEur()));
                    if (lblEURRate != null)
                        lblEURRate.setText("1 TND = " + String.format("%.4f", result.getEurRate()) + " EUR");

                    if (lblUSD != null)
                        lblUSD.setText(String.format("%,.2f $", result.getUsd()));
                    if (lblUSDRate != null)
                        lblUSDRate.setText("1 TND = " + String.format("%.4f", result.getUsdRate()) + " USD");

                    if (lblGBP != null)
                        lblGBP.setText(String.format("%,.2f £", result.getGbp()));
                    if (lblGBPRate != null)
                        lblGBPRate.setText("1 TND = " + String.format("%.4f", result.getGbpRate()) + " GBP");

                    if (lblConversionStatus != null)
                        lblConversionStatus.setText("Taux en temps réel");
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (lblConversionStatus != null)
                        lblConversionStatus.setText("Conversion indisponible");
                    if (lblEUR != null) lblEUR.setText("N/A");
                    if (lblUSD != null) lblUSD.setText("N/A");
                    if (lblGBP != null) lblGBP.setText("N/A");
                });
                System.err.println("ExchangeRate API error: " + e.getMessage());
            }
        });

        apiThread.setDaemon(true);
        apiThread.start();
    }

    private void configureNavigationForRole() {
        TypeUtilisateur type = currentUser.getTypeUtilisateur();
        if (type == null) return;
=======
    private void configureNavigationForRole() {
        TypeUtilisateur type = currentUser.getTypeUtilisateur();
        if (type == null) {
            return;
        }
>>>>>>> yassine_antar

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

    private void applyNavLabels(String sidebarTitle, String pageTitle) {
<<<<<<< HEAD
        if (lblSidebarTitle != null) lblSidebarTitle.setText(sidebarTitle);
        if (lblPageTitle    != null) lblPageTitle.setText(pageTitle);
    }

    private void configureNavButton(Button button, String text, Runnable action) {
        if (button == null) return;
=======
        if (lblSidebarTitle != null) {
            lblSidebarTitle.setText(sidebarTitle);
        }
        if (lblPageTitle != null) {
            lblPageTitle.setText(pageTitle);
        }
    }

    private void configureNavButton(Button button, String text, Runnable action) {
        if (button == null) {
            return;
        }
>>>>>>> yassine_antar
        button.setText(text);
        button.setOnAction(event -> action.run());
        button.setVisible(true);
        button.setManaged(true);
    }

    private void hideNavButton(Button button) {
<<<<<<< HEAD
        if (button == null) return;
=======
        if (button == null) {
            return;
        }
>>>>>>> yassine_antar
        button.setVisible(false);
        button.setManaged(false);
    }

    private void navigate(String fxml) {
        try {
            org.GreenLedger.MainFX.setRoot(fxml);
        } catch (IOException ex) {
            System.err.println("[ERROR] Navigation error: " + ex.getMessage());
<<<<<<< HEAD
=======
            ex.printStackTrace();
>>>>>>> yassine_antar
            showError("Erreur", "Impossible de naviguer vers " + fxml);
        }
    }

<<<<<<< HEAD
    private void setupTableColumns() {
=======
    /**
     * Setup table columns with proper cell value factories
     */
    private void setupTableColumns() {
        // My Investments Table
>>>>>>> yassine_antar
        colInvProjetId.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getProjetId()).asObject());
        colInvMontant.setCellValueFactory(cd -> new SimpleDoubleProperty(cd.getValue().getMontant()).asObject());
        colInvDate.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getDateFinancement() != null ? cd.getValue().getDateFinancement() : "N/A"
        ));
        colInvStatut.setCellValueFactory(cd -> new SimpleStringProperty("Actif"));

<<<<<<< HEAD
=======
        // Financing Offers Table
>>>>>>> yassine_antar
        colOffreType.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getTypeOffre() != null ? cd.getValue().getTypeOffre() : "N/A"
        ));
        colOffreTaux.setCellValueFactory(cd -> new SimpleDoubleProperty(cd.getValue().getTaux()).asObject());
        colOffreDuree.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getDuree()).asObject());
        colOffreFinId.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getIdFinancement()).asObject());

        tableMyInvestments.setItems(myInvestments);
        tableFinancingOffers.setItems(availableOffers);
    }

<<<<<<< HEAD
=======
    /**
     * Load all data from services
     */
>>>>>>> yassine_antar
    private void loadData() {
        try {
            refreshInvestments();
            refreshOffers();
            updateStatistics();
        } catch (Exception ex) {
            System.err.println("[ERROR] Data loading error: " + ex.getMessage());
<<<<<<< HEAD
        }
    }

=======
            ex.printStackTrace();
        }
    }

    /**
     * Setup project selection combo box
     */
>>>>>>> yassine_antar
    private void setupComboBox() {
        try {
            List<Projet> allProjects = projetService.afficher();
            if (allProjects != null) {
                projects.setAll(allProjects);
                cmbProjectSelection.setItems(projects);
<<<<<<< HEAD
=======

                // Custom cell factory for better display
>>>>>>> yassine_antar
                cmbProjectSelection.setCellFactory(lv -> new ListCell<Projet>() {
                    @Override
                    protected void updateItem(Projet item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty ? "" : (item.getId() + " - " + item.getTitre()));
                    }
                });
<<<<<<< HEAD
=======

>>>>>>> yassine_antar
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
<<<<<<< HEAD
        }
    }

=======
            ex.printStackTrace();
        }
    }

    /**
     * Refresh investments from database
     */
>>>>>>> yassine_antar
    @FXML
    private void refreshInvestments() {
        try {
            List<Financement> investments = financementService.getAll();
<<<<<<< HEAD
            if (investments != null) myInvestments.setAll(investments);
            updateStatistics();
        } catch (Exception ex) {
            System.err.println("[ERROR] Refresh investments error: " + ex.getMessage());
        }
    }

=======
            if (investments != null) {
                myInvestments.setAll(investments);
            }
            updateStatistics();
        } catch (Exception ex) {
            System.err.println("[ERROR] Refresh investments error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Refresh financing offers from database
     */
>>>>>>> yassine_antar
    @FXML
    private void refreshOffers() {
        try {
            List<OffreFinancement> offers = offreService.getAll();
<<<<<<< HEAD
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
        } catch (Exception ex) {
=======
            if (offers != null) {
                availableOffers.setAll(offers);
            }
        } catch (Exception ex) {
            System.err.println("[ERROR] Refresh offers error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Update statistics labels
     */
    private void updateStatistics() {
        try {
            // Total investments
            int totalCount = myInvestments.size();
            lblTotalInvestments.setText(String.valueOf(totalCount));

            // Total amount invested
            double totalAmount = myInvestments.stream()
                    .mapToDouble(Financement::getMontant)
                    .sum();
            lblTotalAmount.setText(String.format("%.2f EUR", totalAmount));

            // Projects followed (unique project IDs)
            long projectsCount = myInvestments.stream()
                    .map(Financement::getProjetId)
                    .distinct()
                    .count();
            lblProjectsFollowed.setText(String.valueOf(projectsCount));

        } catch (Exception ex) {
            System.err.println("[ERROR] Statistics update error: " + ex.getMessage());
            ex.printStackTrace();
>>>>>>> yassine_antar
            lblTotalInvestments.setText("--");
            lblTotalAmount.setText("-- EUR");
            lblProjectsFollowed.setText("--");
        }
    }

<<<<<<< HEAD
=======
    /**
     * Handle new investment submission
     */
>>>>>>> yassine_antar
    @FXML
    private void handleNewInvestment() {
        try {
            Projet selectedProject = cmbProjectSelection.getValue();
            String amountText = txtInvestmentAmount.getText();

            if (selectedProject == null) {
                showAlert("Attention", "Veuillez sélectionner un projet", Alert.AlertType.WARNING);
                return;
            }
<<<<<<< HEAD
=======

>>>>>>> yassine_antar
            if (amountText == null || amountText.trim().isEmpty()) {
                showAlert("Attention", "Veuillez entrer un montant", Alert.AlertType.WARNING);
                return;
            }

<<<<<<< HEAD
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
        } catch (Exception ex) {
=======
            try {
                double amount = Double.parseDouble(amountText);
                if (amount <= 0) {
                    showAlert("Attention", "Le montant doit être supérieur à 0", Alert.AlertType.WARNING);
                    return;
                }

                // Create new financing record
                Financement newInvestment = new Financement();
                newInvestment.setProjetId(selectedProject.getId());
                newInvestment.setMontant(amount);
                newInvestment.setDateFinancement(java.time.LocalDate.now().toString());
                // Note: BanqueId should be set appropriately - using 0 as placeholder
                newInvestment.setBanqueId(0);

                financementService.add(newInvestment);

                showAlert("Succès", "Investissement enregistré avec succès!", Alert.AlertType.INFORMATION);

                // Clear form and refresh
                txtInvestmentAmount.clear();
                cmbProjectSelection.getSelectionModel().clearSelection();
                refreshInvestments();

            } catch (NumberFormatException nfe) {
                showAlert("Erreur", "Montant invalide. Veuillez entrer un nombre", Alert.AlertType.ERROR);
            }

        } catch (Exception ex) {
            System.err.println("[ERROR] New investment error: " + ex.getMessage());
            ex.printStackTrace();
>>>>>>> yassine_antar
            showAlert("Erreur", "Impossible de créer l'investissement: " + ex.getMessage(), Alert.AlertType.ERROR);
        }
    }

<<<<<<< HEAD
    @FXML private void handleViewAllProjects() {
        showAlert("Information", "Fonctionnalité en développement", Alert.AlertType.INFORMATION);
    }
    @FXML private void handleViewPerformance() {
        showAlert("Information", "Fonctionnalité en développement", Alert.AlertType.INFORMATION);
    }
    @FXML private void handleReportIssue() {
        showAlert("Information", "Fonctionnalité en développement", Alert.AlertType.INFORMATION);
    }

=======
    /**
     * Handle view all projects action
     */
    @FXML
    private void handleViewAllProjects() {
        showAlert("Information", "Fonctionnalité 'Voir tous les projets' en développement", Alert.AlertType.INFORMATION);
    }

    /**
     * Handle view performance action
     */
    @FXML
    private void handleViewPerformance() {
        showAlert("Information", "Fonctionnalité 'Voir les performances' en développement", Alert.AlertType.INFORMATION);
    }

    /**
     * Handle report issue action
     */
    @FXML
    private void handleReportIssue() {
        showAlert("Information", "Fonctionnalité 'Signaler un problème' en développement", Alert.AlertType.INFORMATION);
    }

    /**
     * Handle back button
     */
>>>>>>> yassine_antar
    @FXML
    private void handleBack() {
        try {
            TypeUtilisateur type = currentUser != null ? currentUser.getTypeUtilisateur() : null;
<<<<<<< HEAD
            if (type == TypeUtilisateur.EXPERT_CARBONE)       org.GreenLedger.MainFX.setRoot("expertProjet");
            else if (type == TypeUtilisateur.PORTEUR_PROJET)  org.GreenLedger.MainFX.setRoot("GestionProjet");
            else if (type == TypeUtilisateur.ADMIN)           org.GreenLedger.MainFX.setRoot("fxml/admin_users");
            else                                               org.GreenLedger.MainFX.setRoot("fxml/dashboard");
        } catch (IOException ex) {
=======
            if (type == TypeUtilisateur.EXPERT_CARBONE) {
                org.GreenLedger.MainFX.setRoot("expertProjet");
            } else if (type == TypeUtilisateur.PORTEUR_PROJET) {
                org.GreenLedger.MainFX.setRoot("GestionProjet");
            } else if (type == TypeUtilisateur.ADMIN) {
                org.GreenLedger.MainFX.setRoot("fxml/admin_users");
            } else {
                org.GreenLedger.MainFX.setRoot("fxml/dashboard");
            }
        } catch (IOException ex) {
            System.err.println("[ERROR] Navigation error: " + ex.getMessage());
            ex.printStackTrace();
>>>>>>> yassine_antar
            showError("Erreur", "Impossible de retourner au tableau de bord");
        }
    }

<<<<<<< HEAD
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

=======
    /**
     * Navigate to dashboard
     */
    @FXML
    private void handleGoDashboard() {
        TypeUtilisateur type = currentUser != null ? currentUser.getTypeUtilisateur() : null;
        if (type == TypeUtilisateur.EXPERT_CARBONE) {
            navigate("expertProjet");
        } else if (type == TypeUtilisateur.PORTEUR_PROJET) {
            navigate("GestionProjet");
        } else if (type == TypeUtilisateur.ADMIN) {
            navigate("fxml/admin_users");
        } else {
            navigate("fxml/dashboard");
        }
    }

    /**
     * Stay on investments (refresh current view)
     */
    @FXML
    private void handleGoInvestments() {
        TypeUtilisateur type = currentUser != null ? currentUser.getTypeUtilisateur() : null;
        if (type == TypeUtilisateur.EXPERT_CARBONE) {
            navigate("gestionCarbone");
            return;
        }
        if (type == TypeUtilisateur.PORTEUR_PROJET) {
            navigate("ownerEvaluations");
            return;
        }
        if (type == TypeUtilisateur.ADMIN) {
            navigate("fxml/admin_users");
            return;
        }
        try {
            refreshInvestments();
            refreshOffers();
        } catch (Exception ex) {
            System.err.println("[ERROR] Refresh error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Navigate to advanced financing module
     */
    @FXML
    private void handleGoFinancement() {
        TypeUtilisateur type = currentUser != null ? currentUser.getTypeUtilisateur() : null;
        if (type == TypeUtilisateur.EXPERT_CARBONE) {
            navigate("gestionCarbone");
            return;
        }
        if (type == TypeUtilisateur.PORTEUR_PROJET) {
            navigate("GestionProjet");
            return;
        }
        if (type == TypeUtilisateur.ADMIN) {
            navigate("fxml/admin_users");
            return;
        }
        navigate("financement");
    }

    @FXML
    private void handleGoSettings() {
        navigate("settings");
    }

    @FXML
    private void handleEditProfile() {
        navigate("editProfile");
    }

    /**
     * Show error alert
     */
>>>>>>> yassine_antar
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

<<<<<<< HEAD
=======
    /**
     * Show alert
     */
>>>>>>> yassine_antar
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
<<<<<<< HEAD

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

=======
}
>>>>>>> yassine_antar
