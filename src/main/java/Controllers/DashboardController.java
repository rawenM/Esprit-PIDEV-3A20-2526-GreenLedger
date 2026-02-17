package Controllers;


import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import Models.TypeUtilisateur;
import Models.User;
import Services.IUserService;
import Services.UserServiceImpl;
import Services.EvaluationService;
import Services.ProjetService;
import Services.CritereImpactService;
import Models.Evaluation;
import Utils.SessionManager;
import org.GreenLedger.MainFX;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

/**
 * Controller pour le tableau de bord utilisateur (Investisseur ou Porteur de Projet)
 */
public class DashboardController {

    // Informations utilisateur
    @FXML private Label welcomeLabel;
    @FXML private Label userNameLabel;
    @FXML private Label userEmailLabel;
    @FXML private Label userTypeLabel;
    @FXML private Label memberSinceLabel;
    @FXML private Label lastLoginLabel;
    @FXML private Label sidebarProfileName;
    @FXML private Label sidebarProfileType;

    // Statistiques (à adapter selon le type d'utilisateur)
    @FXML private Label stat1Label;
    @FXML private Label stat2Label;
    @FXML private Label stat3Label;
    @FXML private Label stat1Value;
    @FXML private Label stat2Value;
    @FXML private Label stat3Value;

    // Boutons de navigation
    @FXML private Button profileButton;
    @FXML private Button projectsButton;
    @FXML private Button investmentsButton;
    @FXML private Button settingsButton;
    @FXML private Button financingButton;
    @FXML private Button logoutButton;

    // Section de modification de profil (optionnel)
    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private TextField telephoneField;
    @FXML private TextArea adresseField;
    @FXML private Button saveProfileButton;
    @FXML private Label profileMessageLabel;

    private final IUserService userService = new UserServiceImpl();
    private final EvaluationService evaluationService = new EvaluationService();
    private final ProjetService projetService = new ProjetService();
    private final CritereImpactService critereImpactService = new CritereImpactService();
    private User currentUser;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        // Cette méthode sera appelée automatiquement après le chargement du FXML
        if (profileMessageLabel != null) {
            profileMessageLabel.setVisible(false);
        }

        if (currentUser == null) {
            try {
                User sessionUser = SessionManager.getInstance().getCurrentUser();
                if (sessionUser != null) {
                    setCurrentUser(sessionUser);
                }
            } catch (Exception ex) {
                System.err.println("[CLEAN] Erreur session initialize dashboard: " + ex.getMessage());
            }
        }

        // S'assurer que les champs de profil sont éditables et peuvent recevoir le focus
        try {
            if (nomField != null) {
                nomField.setEditable(true);
                nomField.setDisable(false);
                nomField.setMouseTransparent(false);
                nomField.setFocusTraversable(true);
            }
            if (prenomField != null) {
                prenomField.setEditable(true);
                prenomField.setDisable(false);
                prenomField.setMouseTransparent(false);
                prenomField.setFocusTraversable(true);
            }
            if (emailField != null) {
                emailField.setEditable(true);
                emailField.setDisable(false);
                emailField.setMouseTransparent(false);
                emailField.setFocusTraversable(true);
            }
            if (telephoneField != null) {
                telephoneField.setEditable(true);
                telephoneField.setDisable(false);
                telephoneField.setMouseTransparent(false);
                telephoneField.setFocusTraversable(true);
            }
            if (adresseField != null) {
                adresseField.setEditable(true);
                adresseField.setDisable(false);
                adresseField.setMouseTransparent(false);
                adresseField.setFocusTraversable(true);
            }

            // Donner le focus au premier champ pour permettre la saisie
            if (nomField != null) {
                javafx.application.Platform.runLater(() -> nomField.requestFocus());
            }
        } catch (Exception ex) {
            System.err.println("[CLEAN] Erreur lors de l'activation des champs du profil: " + ex.getMessage());
        }
    }

    /**
     * Methode appelee par le LoginController pour passer l'utilisateur connecte
     */
    public void setCurrentUser(User user) {
        System.out.println("[DEBUG] setCurrentUser appele avec user: " + (user != null ? user.getEmail() : "null"));
        this.currentUser = user;
        if (this.currentUser == null) {
            try {
                this.currentUser = SessionManager.getInstance().getCurrentUser();
                System.out.println("[DEBUG] Utilisateur recupere de la session: " + (this.currentUser != null ? this.currentUser.getEmail() : "null"));
            } catch (Exception ex) {
                System.err.println("[CLEAN] Erreur lors de la recuperation de la session: " + ex.getMessage());
                ex.printStackTrace();
            }
        }

        try {
            System.out.println("[DEBUG] Affichage des infos utilisateur...");
            displayUserInfo();
        } catch (Exception ex) {
            System.err.println("[CLEAN] Erreur dans displayUserInfo: " + ex.getMessage());
            ex.printStackTrace();
        }

        try {
            System.out.println("[DEBUG] Chargement des statistiques...");
            loadUserStatistics();
        } catch (Exception ex) {
            System.err.println("[CLEAN] Erreur dans loadUserStatistics: " + ex.getMessage());
            ex.printStackTrace();
        }

        try {
            System.out.println("[DEBUG] Chargement du formulaire de profil...");
            loadProfileForm();
        } catch (Exception ex) {
            System.err.println("[CLEAN] Erreur dans loadProfileForm: " + ex.getMessage());
            ex.printStackTrace();
        }

        try {
            configureNavigationForRole();
        } catch (Exception ex) {
            System.err.println("[CLEAN] Erreur lors de la configuration de navigation: " + ex.getMessage());
        }

        System.out.println("[DEBUG] setCurrentUser termine avec succes");
    }

    /**
     * Afficher les informations de l'utilisateur
     */
    private void displayUserInfo() {
        if (currentUser == null) return;

        try {
            // Message de bienvenue personnalise
            String greeting = getGreeting();
            if (welcomeLabel != null) {
                welcomeLabel.setText(greeting + ", " + currentUser.getPrenom() + " !");
            }

            // Informations de base
            if (userNameLabel != null) {
                userNameLabel.setText(currentUser.getNomComplet());
            }
            if (userEmailLabel != null) {
                userEmailLabel.setText("");
                userEmailLabel.setVisible(false);
                userEmailLabel.setManaged(false);
            }
            if (userTypeLabel != null && currentUser.getTypeUtilisateur() != null) {
                userTypeLabel.setText(currentUser.getTypeUtilisateur().getLibelle());
            }
            if (sidebarProfileName != null) {
                sidebarProfileName.setText(currentUser.getNomComplet());
            }
            if (sidebarProfileType != null && currentUser.getTypeUtilisateur() != null) {
                sidebarProfileType.setText(currentUser.getTypeUtilisateur().getLibelle());
            }

            // Dates
            if (currentUser.getDateInscription() != null && memberSinceLabel != null) {
                memberSinceLabel.setText("Membre depuis le " +
                        currentUser.getDateInscription().format(DATE_FORMATTER));
            }

            if (currentUser.getDerniereConnexion() != null && lastLoginLabel != null) {
                lastLoginLabel.setText("Derniere connexion : " +
                        currentUser.getDerniereConnexion().format(DATETIME_FORMATTER));
            }
        } catch (Exception ex) {
            System.err.println("[CLEAN] Erreur lors de l'affichage des infos utilisateur: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Retourner un message de bienvenue selon l'heure
     */
    private String getGreeting() {
        int hour = java.time.LocalTime.now().getHour();
        if (hour < 12) {
            return "Bonjour";
        } else if (hour < 18) {
            return "Bon après-midi";
        } else {
            return "Bonsoir";
        }
    }

    /**
     * Charger les statistiques selon le type d'utilisateur
     */
    private void loadUserStatistics() {
        if (currentUser == null) return;

        try {
            if (currentUser.getTypeUtilisateur() == TypeUtilisateur.INVESTISSEUR) {
                loadInvestorStatistics();
            } else if (currentUser.getTypeUtilisateur() == TypeUtilisateur.PORTEUR_PROJET) {
                loadProjectOwnerStatistics();
            } else if (currentUser.getTypeUtilisateur() == TypeUtilisateur.ADMIN) {
                loadAdminStatistics();
            } else if (currentUser.getTypeUtilisateur() == TypeUtilisateur.EXPERT_CARBONE) {
                loadExpertStatistics();
            }
        } catch (Exception ex) {
            System.err.println("[CLEAN] Erreur lors du chargement des statistiques: " + ex.getMessage());
            ex.printStackTrace();
            // Afficher des valeurs par defaut
            stat1Value.setText("--");
            stat2Value.setText("--");
            stat3Value.setText("--");
        }
    }

    /**
     * Statistiques pour un investisseur
     */
    private void loadInvestorStatistics() {
        try {
            java.util.List<Models.Projet> projets = projetService.afficher();
            int evaluationCount = evaluationService.afficher().size();
            long submitted = projets.stream().filter(p -> "SUBMITTED".equalsIgnoreCase(p.getStatut())).count();

            stat1Label.setText("Projets disponibles");
            stat1Value.setText(String.valueOf(projets.size()));

            stat2Label.setText("Evaluations totales");
            stat2Value.setText(String.valueOf(evaluationCount));

            stat3Label.setText("Projets soumis");
            stat3Value.setText(String.valueOf(submitted));
        } catch (Exception ex) {
            System.err.println("[CLEAN] Erreur lors du chargement des stats investisseur: " + ex.getMessage());
            stat1Value.setText("--");
            stat2Value.setText("--");
            stat3Value.setText("--");
        }
    }

    /**
     * Statistiques pour un porteur de projet
     */
    private void loadProjectOwnerStatistics() {
        try {
            java.util.List<Models.Projet> projets = projetService.afficher();
            java.util.List<Evaluation> evaluations = evaluationService.afficher();

            java.util.Set<Integer> evalProjetIds = new java.util.HashSet<>();
            for (Evaluation evaluation : evaluations) {
                evalProjetIds.add(evaluation.getIdProjet());
            }

            long evaluated = projets.stream().filter(p -> evalProjetIds.contains(p.getId())).count();

            stat1Label.setText("Projets deposes");
            stat1Value.setText(String.valueOf(projets.size()));

            stat2Label.setText("Projets evalues");
            stat2Value.setText(String.valueOf(evaluated));

            stat3Label.setText("Evaluations");
            stat3Value.setText(String.valueOf(evaluations.size()));
        } catch (Exception ex) {
            System.err.println("[CLEAN] Erreur lors du chargement des stats porteur: " + ex.getMessage());
            stat1Value.setText("--");
            stat2Value.setText("--");
            stat3Value.setText("--");
        }
    }

    /**
     * Statistiques pour un admin
     */
    private void loadAdminStatistics() {
        try {
            long totalUsers = 0;
            long activeUsers = 0;
            long pendingUsers = 0;

            try {
                totalUsers = userService.getUserCount();
            } catch (Exception ex) {
                System.err.println("[CLEAN] Erreur getUserCount: " + ex.getMessage());
                totalUsers = 0;
            }

            try {
                activeUsers = userService.getActiveUserCount();
            } catch (Exception ex) {
                System.err.println("[CLEAN] Erreur getActiveUserCount: " + ex.getMessage());
                activeUsers = 0;
            }

            try {
                pendingUsers = userService.getPendingUserCount();
            } catch (Exception ex) {
                System.err.println("[CLEAN] Erreur getPendingUserCount: " + ex.getMessage());
                pendingUsers = 0;
            }

            stat1Label.setText("Utilisateurs Totaux");
            stat1Value.setText(String.valueOf(totalUsers));

            stat2Label.setText("Utilisateurs Actifs");
            stat2Value.setText(String.valueOf(activeUsers));

            stat3Label.setText("En Attente");
            stat3Value.setText(String.valueOf(pendingUsers));
        } catch (Exception ex) {
            System.err.println("[CLEAN] Erreur lors du chargement des stats admin: " + ex.getMessage());
            ex.printStackTrace();
            stat1Value.setText("--");
            stat2Value.setText("--");
            stat3Value.setText("--");
        }
    }

    private void loadExpertStatistics() {
        try {
            java.util.List<Models.Projet> projets = projetService.afficher();
            long submitted = projets.stream().filter(p -> "SUBMITTED".equalsIgnoreCase(p.getStatut())).count();
            int evaluationCount = evaluationService.afficher().size();

            stat1Label.setText("Projets a evaluer");
            stat1Value.setText(String.valueOf(submitted));

            stat2Label.setText("Evaluations");
            stat2Value.setText(String.valueOf(evaluationCount));

            stat3Label.setText("Criteres d'impact");
            stat3Value.setText(String.valueOf(critereImpactService.afficherReferences().size()));
        } catch (Exception ex) {
            System.err.println("[CLEAN] Erreur stats expert: " + ex.getMessage());
            stat1Value.setText("--");
            stat2Value.setText("--");
            stat3Value.setText("--");
        }
    }

    private void configureNavigationForRole() {
        if (currentUser == null || currentUser.getTypeUtilisateur() == null) {
            return;
        }
        TypeUtilisateur type = currentUser.getTypeUtilisateur();
        if (type == TypeUtilisateur.EXPERT_CARBONE) {
            projectsButton.setText("📁 Voir projets");
            investmentsButton.setText("🧾 Evaluations carbone");
            financingButton.setVisible(false);
            financingButton.setManaged(false);
        } else if (type == TypeUtilisateur.PORTEUR_PROJET) {
            projectsButton.setText("📁 Mes projets");
            investmentsButton.setText("📊 Mes evaluations");
            financingButton.setVisible(false);
            financingButton.setManaged(false);
        } else if (type == TypeUtilisateur.INVESTISSEUR) {
            projectsButton.setText("💰 Investissements");
            investmentsButton.setText("💳 Financement avance");
            financingButton.setVisible(false);
            financingButton.setManaged(false);
        }
    }

    /**
     * Charger le formulaire de profil
     */
    private void loadProfileForm() {
        if (currentUser == null) return;

        try {
            if (nomField != null) {
                nomField.setText(currentUser.getNom() != null ? currentUser.getNom() : "");
            }
            if (prenomField != null) {
                prenomField.setText(currentUser.getPrenom() != null ? currentUser.getPrenom() : "");
            }
            if (emailField != null) {
                emailField.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");
            }
            if (telephoneField != null) {
                telephoneField.setText(currentUser.getTelephone() != null ? currentUser.getTelephone() : "");
            }
            if (adresseField != null) {
                adresseField.setText(currentUser.getAdresse() != null ? currentUser.getAdresse() : "");
            }
        } catch (Exception ex) {
            System.err.println("[CLEAN] Erreur lors du chargement du formulaire de profil: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Gérer la navigation vers le profil
     */
    @FXML
    private void handleProfile(ActionEvent event) {
        try {
            MainFX.setRoot("editProfile");

        } catch (IOException e) {
            showAlert("Erreur", "Impossible de charger le profil", Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    /**
     * Gérer la navigation vers les projets
     */
    @FXML
    private void handleProjects(ActionEvent event) {
        if (currentUser == null || currentUser.getTypeUtilisateur() == null) {
            return;
        }
        TypeUtilisateur type = currentUser.getTypeUtilisateur();
        try {
            if (type == TypeUtilisateur.EXPERT_CARBONE) {
                MainFX.setRoot("expertProjet");
            } else if (type == TypeUtilisateur.PORTEUR_PROJET) {
                MainFX.setRoot("GestionProjet");
            } else if (type == TypeUtilisateur.INVESTISSEUR) {
                MainFX.setRoot("fxml/investor_financing");
            } else {
                showAlert("Information",
                        "La gestion des projets sera disponible prochainement",
                        Alert.AlertType.INFORMATION);
            }
        } catch (IOException e) {
            showAlert("Erreur", "Impossible de charger l'ecran", Alert.AlertType.ERROR);
        }
    }

    /**
     * Gérer la navigation vers les investissements
     */
    @FXML
    private void handleInvestments(ActionEvent event) {
        try {
            if (currentUser != null && currentUser.getTypeUtilisateur() == TypeUtilisateur.EXPERT_CARBONE) {
                MainFX.setRoot("gestionCarbone");
                return;
            }
            if (currentUser != null && currentUser.getTypeUtilisateur() == TypeUtilisateur.PORTEUR_PROJET) {
                Evaluation latest = getLatestEvaluation();
                if (latest != null) {
                    ProjectEvaluationViewController.setCurrentProjet(latest.getIdProjet(), latest.getTitreProjet());
                }
                MainFX.setRoot("projectEvaluationView");
                return;
            }
            MainFX.setRoot("fxml/investor_financing");

        } catch (IOException e) {
            showAlert("Erreur", "Impossible de charger le module de gestion des investissements", Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    /**
     * Gérer les paramètres
     */
    @FXML
    private void handleSettings(ActionEvent event) {
        try {
            MainFX.setRoot("settings");
        } catch (IOException e) {
            showAlert("Erreur", "Impossible de charger les parametres", Alert.AlertType.ERROR);
        }
    }

    /**
     * Gérer la navigation vers la gestion financement avancée
     */
    @FXML
    private void handleAdvancedFinancing(ActionEvent event) {
        try {
            if (currentUser != null && currentUser.getTypeUtilisateur() == TypeUtilisateur.INVESTISSEUR) {
                MainFX.setRoot("financement");
            } else {
                showAlert("Information",
                        "Module non disponible pour ce profil",
                        Alert.AlertType.INFORMATION);
            }

        } catch (IOException e) {
            showAlert("Erreur", "Impossible de charger le module de financement avancé", Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    /**
     * Sauvegarder les modifications du profil
     */
    @FXML
    private void handleSaveProfile(ActionEvent event) {
        if (currentUser == null) return;

        try {
            // Récupérer les nouvelles valeurs
            currentUser.setNom(nomField.getText().trim());
            currentUser.setPrenom(prenomField.getText().trim());
            currentUser.setEmail(emailField.getText().trim());
            currentUser.setTelephone(telephoneField.getText().trim());
            currentUser.setAdresse(adresseField.getText().trim());

            // Mettre à jour via le service
            User updatedUser = userService.updateProfile(currentUser);

            if (updatedUser != null) {
                this.currentUser = updatedUser;
                displayUserInfo();
                showProfileMessage("[CLEAN] Profil mis à jour avec succès !", "success");
            } else {
                showProfileMessage("[CLEAN] Erreur lors de la mise à jour", "error");
            }

        } catch (Exception e) {
            showProfileMessage("[CLEAN] " + e.getMessage(), "error");
        }
    }

    /**
     * Gérer la déconnexion
     */
    @FXML
    private void handleLogout(ActionEvent event) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Déconnexion");
        confirmation.setHeaderText("Voulez-vous vraiment vous déconnecter ?");
        confirmation.setContentText("Vous devrez vous reconnecter pour accéder à votre compte.");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    userService.logout(currentUser);

                    MainFX.setRoot("fxml/login");
                } catch (IOException e) {
                    showAlert("Erreur", "Impossible de se déconnecter", Alert.AlertType.ERROR);
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Afficher un message sur le formulaire de profil
     */
    private void showProfileMessage(String message, String type) {
        if (profileMessageLabel == null) return;

        profileMessageLabel.setText(message);
        profileMessageLabel.setVisible(true);
        profileMessageLabel.getStyleClass().removeAll("message-success", "message-error");

        // Style selon le type
        if (type.equals("success")) {
            profileMessageLabel.getStyleClass().add("message-success");
        } else {
            profileMessageLabel.getStyleClass().add("message-error");
        }

        // Masquer après 3 secondes
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                javafx.application.Platform.runLater(() -> {
                    if (profileMessageLabel != null) {
                        profileMessageLabel.setVisible(false);
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Afficher une alerte
     */
    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private Evaluation getLatestEvaluation() {
        java.util.List<Evaluation> items = evaluationService.afficher();
        return (items == null || items.isEmpty()) ? null : items.get(0);
    }
}
