package Controllers;

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
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;
import java.util.List;

/**
 * Controller for Investor Financing View
 * Provides a simplified financing interface for investors
 */
public class InvestorFinancingController extends BaseController {

    // Statistics Labels
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

    // My Investments Table
    @FXML private TableView<Financement> tableMyInvestments;
    @FXML private TableColumn<Financement, Integer> colInvProjetId;
    @FXML private TableColumn<Financement, Double> colInvMontant;
    @FXML private TableColumn<Financement, String> colInvDate;
    @FXML private TableColumn<Financement, String> colInvStatut;

    // Financing Offers Table
    @FXML private TableView<OffreFinancement> tableFinancingOffers;
    @FXML private TableColumn<OffreFinancement, String> colOffreType;
    @FXML private TableColumn<OffreFinancement, Double> colOffreTaux;
    @FXML private TableColumn<OffreFinancement, Integer> colOffreDuree;
    @FXML private TableColumn<OffreFinancement, Integer> colOffreFinId;

    // Investment Form Controls
    @FXML private ComboBox<Projet> cmbProjectSelection;
    @FXML private TextField txtInvestmentAmount;

    // Services
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

        } catch (Exception ex) {
            System.err.println("[ERROR] Initialization error: " + ex.getMessage());
            ex.printStackTrace();
            showError("Erreur d'initialisation", ex.getMessage());
        }
    }

    private void configureNavigationForRole() {
        TypeUtilisateur type = currentUser.getTypeUtilisateur();
        if (type == null) {
            return;
        }

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
        button.setText(text);
        button.setOnAction(event -> action.run());
        button.setVisible(true);
        button.setManaged(true);
    }

    private void hideNavButton(Button button) {
        if (button == null) {
            return;
        }
        button.setVisible(false);
        button.setManaged(false);
    }

    private void navigate(String fxml) {
        try {
            org.GreenLedger.MainFX.setRoot(fxml);
        } catch (IOException ex) {
            System.err.println("[ERROR] Navigation error: " + ex.getMessage());
            ex.printStackTrace();
            showError("Erreur", "Impossible de naviguer vers " + fxml);
        }
    }

    /**
     * Setup table columns with proper cell value factories
     */
    private void setupTableColumns() {
        // My Investments Table
        colInvProjetId.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getProjetId()).asObject());
        colInvMontant.setCellValueFactory(cd -> new SimpleDoubleProperty(cd.getValue().getMontant()).asObject());
        colInvDate.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getDateFinancement() != null ? cd.getValue().getDateFinancement() : "N/A"
        ));
        colInvStatut.setCellValueFactory(cd -> new SimpleStringProperty("Actif"));

        // Financing Offers Table
        colOffreType.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getTypeOffre() != null ? cd.getValue().getTypeOffre() : "N/A"
        ));
        colOffreTaux.setCellValueFactory(cd -> new SimpleDoubleProperty(cd.getValue().getTaux()).asObject());
        colOffreDuree.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getDuree()).asObject());
        colOffreFinId.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getIdFinancement()).asObject());

        tableMyInvestments.setItems(myInvestments);
        tableFinancingOffers.setItems(availableOffers);
    }

    /**
     * Load all data from services
     */
    private void loadData() {
        try {
            refreshInvestments();
            refreshOffers();
            updateStatistics();
        } catch (Exception ex) {
            System.err.println("[ERROR] Data loading error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Setup project selection combo box
     */
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
            ex.printStackTrace();
        }
    }

    /**
     * Refresh investments from database
     */
    @FXML
    private void refreshInvestments() {
        try {
            List<Financement> investments = financementService.getAll();
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
    @FXML
    private void refreshOffers() {
        try {
            List<OffreFinancement> offers = offreService.getAll();
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
            lblTotalInvestments.setText("--");
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
            showAlert("Erreur", "Impossible de créer l'investissement: " + ex.getMessage(), Alert.AlertType.ERROR);
        }
    }

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
    @FXML
    private void handleBack() {
        try {
            TypeUtilisateur type = currentUser != null ? currentUser.getTypeUtilisateur() : null;
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
            showError("Erreur", "Impossible de retourner au tableau de bord");
        }
    }

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
}
