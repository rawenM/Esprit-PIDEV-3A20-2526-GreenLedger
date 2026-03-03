package Controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import org.GreenLedger.MainFX;
import Utils.NavigationContext;
<<<<<<< HEAD
import Utils.SessionManager;
import Models.User;
import Models.TypeUtilisateur;
=======
<<<<<<< HEAD
=======
import Utils.SessionManager;
import Models.User;
import Models.TypeUtilisateur;
>>>>>>> f3559248f463304c68513eb2c92f99791d2c4657
>>>>>>> yassine_antar

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
<<<<<<< HEAD
            String target = resolveHomeForSession();
            MainFX.setRoot(target);
=======
<<<<<<< HEAD
            String previousPage = NavigationContext.getInstance().getPreviousPage();
            MainFX.setRoot(previousPage);
=======
            String target = resolveHomeForSession();
            MainFX.setRoot(target);
>>>>>>> f3559248f463304c68513eb2c92f99791d2c4657
>>>>>>> yassine_antar
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
<<<<<<< HEAD
=======
<<<<<<< HEAD
=======
>>>>>>> yassine_antar

    private String resolveHomeForSession() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            return "fxml/dashboard";
        }
        if (user.isAdmin()) {
            return "fxml/admin_users";
        }
        return "fxml/dashboard";
    }
<<<<<<< HEAD
=======
>>>>>>> f3559248f463304c68513eb2c92f99791d2c4657
>>>>>>> yassine_antar
}
