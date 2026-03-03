package Controllers;

import Models.User;
import Services.IUserService;
import Services.UserServiceImpl;
import Utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.GreenLedger.MainFX;

import java.io.IOException;

public class EditProfileController extends BaseController {

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private TextField telephoneField;
    @FXML private ComboBox<Models.TypeUtilisateur> typeCombo;
    @FXML private ComboBox<Models.StatutUtilisateur> statutCombo;
    @FXML private Label messageLabel;

    private final IUserService userService = new UserServiceImpl();
    private User currentUser;

    @FXML
    public void initialize() {
        super.initialize();
        currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            setMessage("Aucun utilisateur en session.", true);
            return;
        }

        typeCombo.getItems().setAll(Models.TypeUtilisateur.values());
        statutCombo.getItems().setAll(Models.StatutUtilisateur.values());

        nomField.setText(currentUser.getNom());
        prenomField.setText(currentUser.getPrenom());
        emailField.setText(currentUser.getEmail());
        telephoneField.setText(currentUser.getTelephone());
        typeCombo.setValue(currentUser.getTypeUtilisateur());
        statutCombo.setValue(currentUser.getStatut());

        boolean isAdmin = currentUser.isAdmin();
        typeCombo.setDisable(!isAdmin);
        statutCombo.setDisable(!isAdmin);
    }

    @FXML
    private void handleSave() {
        if (currentUser == null) {
            setMessage("Aucun utilisateur en session.", true);
            return;
        }

        String nom = safeTrim(nomField.getText());
        String prenom = safeTrim(prenomField.getText());
        String email = safeTrim(emailField.getText());
        String telephone = safeTrim(telephoneField.getText());

        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty()) {
            setMessage("Nom, prenom et email sont obligatoires.", true);
            return;
        }

        currentUser.setNom(nom);
        currentUser.setPrenom(prenom);
        currentUser.setEmail(email);
        currentUser.setTelephone(telephone);

        if (!typeCombo.isDisabled() && typeCombo.getValue() != null) {
            currentUser.setTypeUtilisateur(typeCombo.getValue());
        }
        if (!statutCombo.isDisabled() && statutCombo.getValue() != null) {
            currentUser.setStatut(statutCombo.getValue());
        }

        try {
            User updated = userService.updateProfile(currentUser);
            if (updated != null) {
                SessionManager.getInstance().createSession(updated);
                setMessage("Profil mis a jour.", false);
            } else {
                setMessage("Echec de mise a jour.", true);
            }
        } catch (Exception ex) {
            setMessage("Erreur: " + ex.getMessage(), true);
        }
    }

    @FXML
    private void handleCancel() {
        try {
            MainFX.setRoot(resolveHomeForSession());
        } catch (IOException e) {
            setMessage("Navigation impossible.", true);
        }
    }

    private String resolveHomeForSession() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            return "main";
        }
        if (user.isAdmin()) {
            return "fxml/admin_users";
        }
        if (user.getTypeUtilisateur() == Models.TypeUtilisateur.EXPERT_CARBONE) {
            return "expertProjet";
        }
        if (user.getTypeUtilisateur() == Models.TypeUtilisateur.PORTEUR_PROJET) {
            return "GestionProjet";
        }
        return "fxml/dashboard";
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private void setMessage(String message, boolean isError) {
        if (messageLabel == null) {
            return;
        }
        messageLabel.setText(message);
        messageLabel.getStyleClass().removeAll("accent-green", "accent-red");
        messageLabel.getStyleClass().add(isError ? "accent-red" : "accent-green");
    }

    @FXML
    private void onGestionProjets() {
        try {
            MainFX.setRoot("GestionProjet");
        } catch (IOException e) {
            setMessage("Navigation impossible.", true);
        }
    }

    @FXML
    private void onSettings() {
        try {
            MainFX.setRoot("settings");
        } catch (IOException e) {
            setMessage("Navigation impossible.", true);
        }
    }
}
