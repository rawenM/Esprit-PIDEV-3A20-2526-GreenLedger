package Controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import Services.IUserService;
import Services.UserServiceImpl;

public class ResetPasswordController {

    @FXML private TextField tokenField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button resetButton;
    @FXML private Label messageLabel;

    private final IUserService userService = new UserServiceImpl();

    // Permettre au code appelant de pré-remplir le token
    public void setToken(String token) {
        if (tokenField != null) {
            tokenField.setText(token);
        }
    }

    @FXML
    private void handleReset() {
        messageLabel.setVisible(false);
        String token = tokenField.getText().trim();
        String p1 = passwordField.getText();
        String p2 = confirmPasswordField.getText();

        if (token.isEmpty() || p1.isEmpty() || p2.isEmpty()) {
            showMessage("Veuillez remplir tous les champs", true);
            return;
        }

        if (!p1.equals(p2)) {
            showMessage("Les mots de passe ne correspondent pas", true);
            return;
        }

        boolean ok = userService.resetPasswordWithToken(token, p1);
        if (ok) {
            showMessage("Mot de passe réinitialisé avec succès", false);
            // fermer après court délai
            new Thread(() -> {
                try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                javafx.application.Platform.runLater(() -> {
                    Stage st = (Stage) resetButton.getScene().getWindow();
                    st.close();
                });
            }).start();
        } else {
            showMessage("Impossible de réinitialiser le mot de passe (token invalide/expiré ou mot de passe non conforme)", true);
        }
    }

    @FXML
    private void handleCancel() {
        Stage st = (Stage) resetButton.getScene().getWindow();
        st.close();
    }

    private void showMessage(String msg, boolean error) {
        messageLabel.setText(msg);
        messageLabel.setVisible(true);
        if (error) messageLabel.setStyle("-fx-text-fill: #EF4444;");
        else messageLabel.setStyle("-fx-text-fill: #10B981;");
    }
}
