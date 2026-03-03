package Controllers;


import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import Models.User;
import Services.IUserService;
import Services.UserServiceImpl;
import Utils.PasswordUtil;
import org.GreenLedger.MainFX;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

/**
 * Controller pour la page de profil détaillée
 */
public class ProfileController {

    @FXML private Label userNameLabel;
    @FXML private Label userEmailLabel;
    @FXML private Label userTypeLabel;
    @FXML private Label memberSinceLabel;

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private TextField telephoneField;
    @FXML private TextArea adresseField;
    @FXML private DatePicker dateNaissancePicker;
    @FXML private TextField photoField;

    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;

    @FXML private Label messageLabel;
    @FXML private Label passwordMessageLabel;

    @FXML private Button saveButton;
    @FXML private Button changePasswordButton;
    @FXML private Button choosePhotoButton;
    @FXML private Button backButton;

    private final IUserService userService = new UserServiceImpl();
    private User currentUser;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        messageLabel.setVisible(false);
        passwordMessageLabel.setVisible(false);
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadUserData();
    }

    private void loadUserData() {
        if (currentUser == null) return;

        // En-tête
        userNameLabel.setText(currentUser.getNomComplet());
        userEmailLabel.setText(currentUser.getEmail());
        userTypeLabel.setText(currentUser.getTypeUtilisateur().getLibelle());

        if (currentUser.getDateInscription() != null) {
            memberSinceLabel.setText("Membre depuis le " +
                    currentUser.getDateInscription().format(DATE_FORMATTER));
        }

        // Formulaire
        nomField.setText(currentUser.getNom());
        prenomField.setText(currentUser.getPrenom());
        emailField.setText(currentUser.getEmail());
        telephoneField.setText(currentUser.getTelephone());
        adresseField.setText(currentUser.getAdresse());

        if (currentUser.getDateNaissance() != null) {
            dateNaissancePicker.setValue(currentUser.getDateNaissance());
        }

        photoField.setText(currentUser.getPhoto());
    }

    @FXML
    private void handleSaveProfile(ActionEvent event) {
        try {
            // Mise à jour des données
            currentUser.setNom(nomField.getText().trim());
            currentUser.setPrenom(prenomField.getText().trim());
            currentUser.setEmail(emailField.getText().trim());
            currentUser.setTelephone(telephoneField.getText().trim());
            currentUser.setAdresse(adresseField.getText().trim());
            currentUser.setDateNaissance(dateNaissancePicker.getValue());
            currentUser.setPhoto(photoField.getText().trim());

            // Sauvegarder via le service
            User updatedUser = userService.updateProfile(currentUser);

            if (updatedUser != null) {
                this.currentUser = updatedUser;
                loadUserData();
                showMessage("[CLEAN] Profil mis à jour avec succès !", "success");
            } else {
                showMessage("[CLEAN] Erreur lors de la mise à jour", "error");
            }

        } catch (Exception e) {
            showMessage("[CLEAN] " + e.getMessage(), "error");
        }
    }

    @FXML
    private void handleChangePassword(ActionEvent event) {
        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Validations
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showPasswordMessage("[CLEAN] Veuillez remplir tous les champs", "error");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showPasswordMessage("[CLEAN] Les nouveaux mots de passe ne correspondent pas", "error");
            confirmPasswordField.clear();
            return;
        }

        String passwordError = PasswordUtil.getPasswordErrorMessage(newPassword);
        if (passwordError != null) {
            showPasswordMessage("[CLEAN] " + passwordError, "error");
            return;
        }

        // Changer le mot de passe
        boolean success = userService.changePassword(
                currentUser.getId(),
                currentPassword,
                newPassword
        );

        if (success) {
            showPasswordMessage("[CLEAN] Mot de passe modifié avec succès !", "success");
            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
        } else {
            showPasswordMessage("[CLEAN] Mot de passe actuel incorrect", "error");
            currentPasswordField.clear();
        }
    }

    @FXML
    private void handleChoosePhoto(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de profil");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        Stage stage = (Stage) choosePhotoButton.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            // TODO: Upload l'image vers un serveur ou stockage
            // Pour l'instant, on stocke juste le chemin
            photoField.setText(selectedFile.getAbsolutePath());
            showMessage("📸 Photo sélectionnée : " + selectedFile.getName(), "success");
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            MainFX.setRoot("fxml/dashboard");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showMessage(String message, String type) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);

        if (type.equals("success")) {
            messageLabel.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;");
        } else {
            messageLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
        }

        // Masquer après 3 secondes
        hideMessageAfterDelay(messageLabel);
    }

    private void showPasswordMessage(String message, String type) {
        passwordMessageLabel.setText(message);
        passwordMessageLabel.setVisible(true);

        if (type.equals("success")) {
            passwordMessageLabel.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;");
        } else {
            passwordMessageLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
        }

        hideMessageAfterDelay(passwordMessageLabel);
    }

    private void hideMessageAfterDelay(Label label) {
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                javafx.application.Platform.runLater(() -> label.setVisible(false));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
