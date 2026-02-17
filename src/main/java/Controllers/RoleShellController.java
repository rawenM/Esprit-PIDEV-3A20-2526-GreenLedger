package Controllers;

import Models.TypeUtilisateur;
import Models.User;
import Utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

public class RoleShellController extends BaseController {

    @FXML private BorderPane root;
    @FXML private Label lblSidebarTitle;
    @FXML private Label lblPageTitle;
    @FXML private Label lblProfileName;
    @FXML private Label lblProfileType;

    @FXML private Button btnNavPrimary;
    @FXML private Button btnNavSecondary;
    @FXML private Button btnNavTertiary;
    @FXML private Button btnNavSettings;

    private TypeUtilisateur role;

    @FXML
    public void initialize() {
        super.initialize();
        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            applyProfile(lblProfileName, lblProfileType);
        }
        resolveRole(user);
        configureForRole();
    }

    private void resolveRole(User user) {
        Object data = root != null ? root.getUserData() : null;
        if (data instanceof String) {
            try {
                role = TypeUtilisateur.valueOf(((String) data).trim());
                return;
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (user != null) {
            role = user.getTypeUtilisateur();
        }
    }

    private void configureForRole() {
        if (role == TypeUtilisateur.EXPERT_CARBONE) {
            setLabels("GreenLedger Expert", "🧪 Espace Expert Carbone");
            setButton(btnNavPrimary, "📁 Voir projets");
            setButton(btnNavSecondary, "🧾 Evaluations carbone");
            hideButton(btnNavTertiary);
        } else if (role == TypeUtilisateur.PORTEUR_PROJET) {
            setLabels("GreenLedger Projet", "📁 Gestion des projets");
            setButton(btnNavPrimary, "📁 Mes projets");
            setButton(btnNavSecondary, "➕ Nouveau projet");
            setButton(btnNavTertiary, "📊 Mes evaluations");
        }
        setButton(btnNavSettings, "⚙️ Parametres");
    }

    private void setLabels(String sidebarTitle, String pageTitle) {
        if (lblSidebarTitle != null) {
            lblSidebarTitle.setText(sidebarTitle);
        }
        if (lblPageTitle != null) {
            lblPageTitle.setText(pageTitle);
        }
    }

    private void setButton(Button button, String text) {
        if (button == null) {
            return;
        }
        button.setText(text);
        button.setVisible(true);
        button.setManaged(true);
    }

    private void hideButton(Button button) {
        if (button == null) {
            return;
        }
        button.setVisible(false);
        button.setManaged(false);
    }

    @FXML
    private void onPrimary() {
        if (role == TypeUtilisateur.EXPERT_CARBONE) {
            navigate("expertProjet");
        } else if (role == TypeUtilisateur.PORTEUR_PROJET) {
            navigate("GestionProjet");
        }
    }

    @FXML
    private void onSecondary() {
        if (role == TypeUtilisateur.EXPERT_CARBONE) {
            navigate("gestionCarbone");
        } else if (role == TypeUtilisateur.PORTEUR_PROJET) {
            navigate("ProjetCreate");
        }
    }

    @FXML
    private void onTertiary() {
        if (role == TypeUtilisateur.PORTEUR_PROJET) {
            navigate("ownerEvaluations");
        }
    }

    @FXML
    private void onSettings() {
        navigate("settings");
    }

    @FXML
    private void onEditProfile() {
        navigate("editProfile");
    }

    @FXML
    private void onBack() {
        try {
            org.GreenLedger.MainFX.setRoot("fxml/dashboard");
        } catch (Exception ex) {
            System.err.println("[ERROR] Navigation error: " + ex.getMessage());
        }
    }

    @FXML
    private void onLogout() {
        try {
            Utils.SessionManager.getInstance().invalidate();
            org.GreenLedger.MainFX.setRoot("fxml/login");
        } catch (Exception ex) {
            System.err.println("[ERROR] Logout error: " + ex.getMessage());
        }
    }

    private void navigate(String fxml) {
        try {
            org.GreenLedger.MainFX.setRoot(fxml);
        } catch (Exception ex) {
            System.err.println("[ERROR] Navigation error: " + ex.getMessage());
        }
    }
}
