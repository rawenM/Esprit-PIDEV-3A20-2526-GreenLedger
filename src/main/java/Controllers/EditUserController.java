package Controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import Models.StatutUtilisateur;
import Models.TypeUtilisateur;
import Models.User;
import Services.IUserService;
import Services.UserServiceImpl;

public class EditUserController {

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private TextField telephoneField;
    @FXML private ComboBox<TypeUtilisateur> typeCombo;
    @FXML private ComboBox<StatutUtilisateur> statutCombo;
    @FXML private Button saveButton;

    private final IUserService userService = new UserServiceImpl();
    private User user;
    private Runnable onSaved;

    public void setUser(User user) {
        this.user = user;
        if (user == null) return;
        nomField.setText(user.getNom());
        prenomField.setText(user.getPrenom());
        emailField.setText(user.getEmail());
        telephoneField.setText(user.getTelephone());
        // remplir combos
        typeCombo.getItems().addAll(TypeUtilisateur.values());
        typeCombo.setValue(user.getTypeUtilisateur());
        statutCombo.getItems().addAll(StatutUtilisateur.values());
        statutCombo.setValue(user.getStatut());
    }

    public void setOnSaved(Runnable r) { this.onSaved = r; }

    @FXML
    private void handleSave() {
        if (user == null) return;
        // mettre à jour les champs
        user.setNom(nomField.getText().trim());
        user.setPrenom(prenomField.getText().trim());
        user.setEmail(emailField.getText().trim().toLowerCase());
        user.setTelephone(telephoneField.getText().trim());
        if (typeCombo.getValue() != null) user.setTypeUtilisateur(typeCombo.getValue());
        if (statutCombo.getValue() != null) user.setStatut(statutCombo.getValue());

        try {
            userService.updateProfile(user);
            // si rôle modifié, utiliser updateUserRole pour clarté
            userService.updateUserRole(user.getId(), user.getTypeUtilisateur());
        } catch (Exception e) {
            showAlert("Erreur", "Impossible de sauvegarder: " + e.getMessage(), Alert.AlertType.ERROR);
            System.err.println("Erreur sauvegarde utilisateur: " + e.getMessage());
            return;
        }

        if (onSaved != null) onSaved.run();

        // fermer la fenêtre
        Stage st = (Stage) saveButton.getScene().getWindow();
        st.close();
    }

    @FXML
    private void handleCancel() {
        Stage st = (Stage) saveButton.getScene().getWindow();
        st.close();
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
