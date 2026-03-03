package Controllers;


import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import Models.TypeUtilisateur;
import Models.User;
import Services.IUserService;
import Services.UserServiceImpl;

import java.io.IOException;
import java.time.LocalDate;

public class RegisterController {

    @FXML private BorderPane rootPane;
    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField telephoneField;
    @FXML private TextArea adresseField;
    @FXML private DatePicker dateNaissancePicker;
    @FXML private ComboBox<TypeUtilisateur> typeUtilisateurCombo;
    @FXML private CheckBox acceptCGUCheckbox;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private Button registerButton;

    private final IUserService userService = new UserServiceImpl();

    // Mode admin et callback
    private boolean adminMode = false;
    private Runnable onSuccessCallback;

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        successLabel.setVisible(false);

        // Remplir le ComboBox avec tous les rôles disponibles
        typeUtilisateurCombo.getItems().addAll(TypeUtilisateur.values());
        typeUtilisateurCombo.setValue(TypeUtilisateur.INVESTISSEUR);

        // Si on n'est pas en mode admin, retirer les rôles réservés
        if (!adminMode) {
            typeUtilisateurCombo.getItems().remove(TypeUtilisateur.ADMIN);
            typeUtilisateurCombo.getItems().remove(TypeUtilisateur.EXPERT_CARBONE);
        }

        // Utiliser Platform.runLater pour s'assurer que tous les composants FXML sont chargés
        javafx.application.Platform.runLater(() -> {
            try {
                // Si un overlay empêche la saisie, essayer de réactiver tous les enfants
                if (rootPane != null) {
                    reactivateAllChildren(rootPane);
                }

                // S'assurer que tous les champs sont éditables et peuvent recevoir le focus
                ensureFieldsEditable();

                // Donner le focus au premier champ
                if (nomField != null) {
                    nomField.requestFocus();
                }
            } catch (Exception ex) {
                System.err.println("[CLEAN] Erreur lors de l'initialisation des contrôles: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
    }

    private void ensureFieldsEditable() {
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
            if (passwordField != null) {
                passwordField.setEditable(true);
                passwordField.setDisable(false);
                passwordField.setMouseTransparent(false);
                passwordField.setFocusTraversable(true);
            }
            if (confirmPasswordField != null) {
                confirmPasswordField.setEditable(true);
                confirmPasswordField.setDisable(false);
                confirmPasswordField.setMouseTransparent(false);
                confirmPasswordField.setFocusTraversable(true);
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
            if (dateNaissancePicker != null) {
                dateNaissancePicker.setDisable(false);
                dateNaissancePicker.setMouseTransparent(false);
                dateNaissancePicker.setFocusTraversable(true);
            }
            if (typeUtilisateurCombo != null) {
                typeUtilisateurCombo.setDisable(false);
                typeUtilisateurCombo.setMouseTransparent(false);
                typeUtilisateurCombo.setFocusTraversable(true);
            }
            if (acceptCGUCheckbox != null) {
                acceptCGUCheckbox.setDisable(false);
                acceptCGUCheckbox.setMouseTransparent(false);
                acceptCGUCheckbox.setFocusTraversable(true);
            }
            if (registerButton != null) {
                registerButton.setDisable(false);
                registerButton.setMouseTransparent(false);
                registerButton.setFocusTraversable(true);
            }
        } catch (Exception ex) {
            System.err.println("[CLEAN] Erreur lors de l'activation des champs: " + ex.getMessage());
        }
    }

    private void reactivateAllChildren(Pane parent) {
        if (parent == null) return;
        for (Node node : parent.getChildren()) {
            try {
                node.setDisable(false);
                node.setMouseTransparent(false);
                node.setFocusTraversable(true);
                if (node instanceof Pane) {
                    reactivateAllChildren((Pane) node);
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Permet d'activer le mode admin avant affichage
     */
    public void setAdminMode(boolean adminMode) {
        this.adminMode = adminMode;
        // Si le contrôleur est initialisé, mettre à jour la liste des rôles
        if (typeUtilisateurCombo != null) {
            if (adminMode) {
                if (!typeUtilisateurCombo.getItems().contains(TypeUtilisateur.ADMIN)) {
                    typeUtilisateurCombo.getItems().add(TypeUtilisateur.ADMIN);
                }
                if (!typeUtilisateurCombo.getItems().contains(TypeUtilisateur.EXPERT_CARBONE)) {
                    typeUtilisateurCombo.getItems().add(TypeUtilisateur.EXPERT_CARBONE);
                }
            } else {
                typeUtilisateurCombo.getItems().remove(TypeUtilisateur.ADMIN);
                typeUtilisateurCombo.getItems().remove(TypeUtilisateur.EXPERT_CARBONE);
            }
        }
        // Si en mode admin, masquer la CGU et la cocher automatiquement
        if (acceptCGUCheckbox != null) {
            acceptCGUCheckbox.setSelected(true);
            acceptCGUCheckbox.setVisible(!adminMode);
        }
    }

    /**
     * Callback exécuté après création réussie (utile pour rafraîchir la liste admin)
     */
    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccessCallback = onSuccess;
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        // Réinitialiser les messages
        errorLabel.setVisible(false);
        successLabel.setVisible(false);

        // Récupération des données
        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String telephone = telephoneField.getText().trim();
        String adresse = adresseField.getText().trim();
        LocalDate dateNaissance = dateNaissancePicker.getValue();
        TypeUtilisateur typeUtilisateur = typeUtilisateurCombo.getValue();

        // Validations côté client
        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() ||
                password.isEmpty() || confirmPassword.isEmpty()) {
            showError("Veuillez remplir tous les champs obligatoires (*)");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Les mots de passe ne correspondent pas");
            passwordField.clear();
            confirmPasswordField.clear();
            return;
        }

        if (!acceptCGUCheckbox.isSelected() && !adminMode) {
            showError("Vous devez accepter les conditions générales d'utilisation");
            return;
        }

        // Création de l'utilisateur
        User newUser = new User(nom, prenom, email, typeUtilisateur);
        newUser.setTelephone(telephone.isEmpty() ? null : telephone);
        newUser.setAdresse(adresse.isEmpty() ? null : adresse);
        newUser.setDateNaissance(dateNaissance);

        try {
            User registeredUser = userService.register(newUser, password);

            if (registeredUser != null) {
                // Si on est en mode admin, valider automatiquement le compte pour qu'il puisse se connecter
                if (adminMode) {
                    User validatedUser = userService.validateAccount(registeredUser.getId());
                    if (validatedUser != null) {
                        registeredUser = validatedUser;
                    }
                }

                showSuccess("[CLEAN] Utilisateur créé avec succès.");

                // Si on est en mode admin et qu'il y a un callback, on l'appelle et on ferme la fenêtre
                if (adminMode && onSuccessCallback != null) {
                    javafx.application.Platform.runLater(() -> {
                        try {
                            onSuccessCallback.run();
                        } catch (Exception ex) {
                            System.err.println("Erreur lors du callback onSuccess: " + ex.getMessage());
                        }
                        // Fermer la fenêtre modale si possible
                        if (rootPane != null && rootPane.getScene() != null) {
                            Stage st = (Stage) rootPane.getScene().getWindow();
                            st.close();
                        }
                    });
                    return;
                }

                // Désactiver le bouton pour éviter les doubles inscriptions
                registerButton.setDisable(true);

                // Rediriger vers la page de connexion après 3 secondes
                new Thread(() -> {
                    try {
                        Thread.sleep(3000);
                        javafx.application.Platform.runLater(() -> {
                            try {
                                handleBackToLogin(event);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Erreur inconnue lors de l'inscription";
            System.err.println("[CLEAN] Erreur inscription: " + errorMsg);
            e.printStackTrace();
            showError(errorMsg);
            registerButton.setDisable(false);
        }
    }

    @FXML
    private void handleBackToLogin(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = stage.getScene();
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
            stage.setScene(scene);
            stage.setTitle("Connexion");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        successLabel.setVisible(false);
    }

    private void showSuccess(String message) {
        successLabel.setText(message);
        successLabel.setVisible(true);
        errorLabel.setVisible(false);
    }
}
