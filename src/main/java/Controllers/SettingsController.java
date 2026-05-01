package Controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import org.GreenLedger.MainFX;
import Utils.NavigationContext;
import Utils.SessionManager;
import Models.User;
import Models.TypeUtilisateur;

import java.io.IOException;

public class SettingsController extends BaseController {

    @FXML
    private Button btnBack;

    @FXML
    public void initialize() {
        super.initialize();
    }

    @FXML
    private void showWallet() {
        try {
            String target = resolveHomeForSession();
            MainFX.setRoot(target);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String resolveHomeForSession() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return "fxml/investisseur_shell";
        if (user.isAdmin()) return "fxml/admin_users";
        if (user.getTypeUtilisateur() == TypeUtilisateur.EXPERT_CARBONE) return "fxml/expert_shell";
        if (user.getTypeUtilisateur() == TypeUtilisateur.PORTEUR_PROJET) return "fxml/porteur_shell";
        return "fxml/investisseur_shell";
    }
}
