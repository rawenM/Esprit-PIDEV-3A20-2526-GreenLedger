package Controllers;


import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import Models.User;
import Services.IUserService;
import Services.UserServiceImpl;
import Utils.SessionManager;
import Utils.ThemeManager;
import org.GreenLedger.MainFX;

import java.io.IOException;
import java.util.Optional;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberMeCheckbox;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private Hyperlink forgotPasswordLink;
    @FXML private Hyperlink registerLink;

    private final IUserService userService = new UserServiceImpl();

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);

        // Permettre la connexion avec la touche Enter
        passwordField.setOnAction(e -> handleLogin(e));

        // Si une session existe (reconnexion automatique), rediriger
        try {
            if (SessionManager.getInstance().isLogged()) {
                User u = SessionManager.getInstance().getCurrentUser();
                // Pas d'Event Action disponible ici, mais on peut charger directement
                // Redirection automatique vers le dashboard
                // Note: cette redirection n'affichera pas d'animation d'Event
                if (u != null) {
                    // Utiliser une action fictive: charger dashboard
                    // Ici on ne peut pas accéder au Stage facilement sans Event, garder pour connexion explicite
                    System.out.println("[CLEAN] Session active trouvée pour: " + u.getEmail());
                }
            }
        } catch (Exception ignored) {}
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String email = emailField.getText().trim().toLowerCase();
        String password = passwordField.getText();

        // Validation basique
        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs");
            return;
        }

        try {
            Optional<User> userOpt = userService.login(email, password);

            if (userOpt.isPresent()) {
                User user = userOpt.get();

                System.out.println("[CLEAN] Bienvenue " + user.getNomComplet());

                // Rediriger vers le tableau de bord approprié selon le rôle
                navigateToDashboard(event, user);

            } else {
                // Message générique pour limiter la fuite d'information
                showError("Email ou mot de passe incorrect");
                System.err.println("🔒 Échec de connexion pour: " + email);
            }
        } catch (RuntimeException e) {
            // Messages métiers renvoyés par le service (compte bloqué/suspendu/non vérifié)
            showError(e.getMessage());
            System.err.println("[CLEAN] Exception métier lors de la connexion: " + e.getMessage());
        } catch (Exception e) {
            showError("Une erreur est survenue lors de la connexion");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleForgotPassword(ActionEvent event) {
        // Boîte de dialogue pour entrer l'email ou le téléphone
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Mot de passe oublié");
        dialog.setHeaderText("Réinitialisation du mot de passe");
        dialog.setContentText("Entrez votre email ou numéro de téléphone:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(input -> {
            String trimmed = input == null ? "" : input.trim();
            if (trimmed.isEmpty()) {
                showError("Veuillez entrer un email ou un numéro de téléphone");
                return;
            }

            String token = userService.initiatePasswordReset(trimmed);
            if (token != null) {
                // Afficher une alerte informative contenant le token (utile en local/dev) et option d'ouvrir le formulaire de reset
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Réinitialisation envoyée");
                alert.setHeaderText(null);
                String body = "Si cet email/numéro existe, vous recevrez les instructions pour réinitialiser votre mot de passe.\n" +
                        "(En test local, le token est affiché ci-dessous)\n\nToken: " + token;
                alert.setContentText(body);
                ButtonType openReset = new ButtonType("Ouvrir formulaire de reset", ButtonBar.ButtonData.OTHER);
                alert.getButtonTypes().add(openReset);
                Optional<ButtonType> choice = alert.showAndWait();
                if (choice.isPresent() && choice.get() == openReset) {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/reset_password.fxml"));
                        Parent root = loader.load();
                        // Passer le token au contrôleur pour pré-remplir le champ
                        Controllers.ResetPasswordController controller = loader.getController();
                        if (controller != null) {
                            controller.setToken(token);
                        }
                        Stage modal = new Stage();
                        modal.setTitle("Réinitialisation du mot de passe");
                        modal.initOwner(((Node) event.getSource()).getScene().getWindow());
                        modal.setScene(new Scene(root));
                        modal.setResizable(false);
                        modal.showAndWait();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                showError("Aucun utilisateur trouvé pour cet email/numéro");
            }
        });
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/register.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            switchScene(stage, root, "Inscription");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void navigateToDashboard(ActionEvent event, User user) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            if (user.isAdmin()) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/admin_users.fxml"));
                Parent root = loader.load();

                AdminUsersController controller = loader.getController();
                controller.setCurrentUser(user);

                switchScene(stage, root, "Gestion des Utilisateurs - " + user.getNomComplet());

            } else if (user.getTypeUtilisateur() == Models.TypeUtilisateur.EXPERT_CARBONE) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
                Parent root = loader.load();

                DashboardController controller = loader.getController();
                controller.setCurrentUser(user);

                switchScene(stage, root, "Expert Carbone - " + user.getNomComplet());

            } else if (user.getTypeUtilisateur() == Models.TypeUtilisateur.PORTEUR_PROJET) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
                Parent root = loader.load();

                DashboardController controller = loader.getController();
                controller.setCurrentUser(user);

                switchScene(stage, root, "Porteur de Projet - " + user.getNomComplet());

            } else {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
                Parent root = loader.load();

                DashboardController controller = loader.getController();
                controller.setCurrentUser(user);

                switchScene(stage, root, user.getTypeUtilisateur().getLibelle() + " - " + user.getNomComplet());
            }

        } catch (IOException e) {
            showError("Erreur lors du chargement du tableau de bord");
            e.printStackTrace();
        }
    }

    private void switchScene(Stage stage, Parent root, String title) {
        Scene scene = MainFX.getScene();
        if (scene == null) {
            scene = stage.getScene();
        }
        if (scene == null) {
            scene = new Scene(root);
        } else {
            scene.setRoot(root);
        }
        if (scene.getStylesheets().stream().noneMatch(s -> s.endsWith("/app.css"))) {
            java.net.URL appCssUrl = getClass().getResource("/app.css");
            if (appCssUrl != null) {
                scene.getStylesheets().add(appCssUrl.toExternalForm());
            }
        }
        if (scene.getStylesheets().stream().noneMatch(s -> s.endsWith("/css/style.css"))) {
            java.net.URL cssUrl = getClass().getResource("/css/style.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
        }
        ThemeManager.getInstance().initialize(scene);
        stage.setScene(scene);
        stage.setTitle(title);
        stage.setMaximized(true);
        stage.show();
    }

    private void showError(String message) {
        errorLabel.setText("[CLEAN] " + message);
        errorLabel.setVisible(true);
    }
}
