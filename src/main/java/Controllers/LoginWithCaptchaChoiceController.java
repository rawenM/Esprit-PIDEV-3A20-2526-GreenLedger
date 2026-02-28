package Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import Models.User;
import Services.IUserService;
import Services.UserServiceImpl;
import Utils.CaptchaHttpServer;
import Utils.CaptchaService;
import Utils.PuzzleCaptchaService;
import Utils.SessionManager;

import java.io.IOException;
import java.util.Optional;

/**
 * Contrôleur de connexion avec choix de méthode CAPTCHA
 * 3 méthodes disponibles:
 * 1. CAPTCHA Mathématique (simple)
 * 2. Google reCAPTCHA (API externe)
 * 3. Puzzle Slider (développement interne)
 */
public class LoginWithCaptchaChoiceController {

    // Champs de formulaire
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberMeCheckbox;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private Button bypassCaptchaBtn;
    @FXML private Hyperlink forgotPasswordLink;
    @FXML private Hyperlink registerLink;

    // Sélecteur de méthode CAPTCHA
    @FXML private ToggleGroup captchaMethodGroup;
    @FXML private RadioButton mathCaptchaRadio;
    @FXML private RadioButton recaptchaRadio;
    @FXML private RadioButton puzzleCaptchaRadio;

    // Container principal
    @FXML private VBox captchaContainer;

    // CAPTCHA Mathématique
    @FXML private HBox mathCaptchaBox;
    @FXML private Label captchaQuestion;
    @FXML private TextField captchaAnswer;

    // reCAPTCHA
    @FXML private VBox recaptchaBox;
    @FXML private WebView captchaWebView;

    // Puzzle CAPTCHA
    @FXML private VBox puzzleCaptchaBox;
    @FXML private StackPane puzzleBackgroundPane;
    @FXML private ImageView puzzleBackgroundImage;
    @FXML private StackPane puzzlePiecePane;
    @FXML private ImageView puzzlePieceImage;
    @FXML private Slider puzzleSlider;
    @FXML private Button regeneratePuzzleBtn;
    @FXML private Label puzzleResultLabel;

    // Services
    private final IUserService userService = new UserServiceImpl();
    private final CaptchaService captchaService = new CaptchaService();
    private final PuzzleCaptchaService puzzleCaptchaService = new PuzzleCaptchaService();

    // État
    private CaptchaHttpServer captchaHttpServer;
    private String captchaToken;
    private int mathCaptchaExpectedAnswer = 0;
    private PuzzleCaptchaService.PuzzleCaptchaResult currentPuzzle;
    private boolean captchaBypassed = false;
    private boolean captchaVerified = false;

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);

        // Permettre la connexion avec la touche Enter
        passwordField.setOnAction(e -> handleLogin(e));

        // Vérifier session existante
        try {
            if (SessionManager.getInstance().isLogged()) {
                User u = SessionManager.getInstance().getCurrentUser();
                if (u != null) {
                    System.out.println("[LoginCaptchaChoice] Session active trouvée pour: " + u.getEmail());
                }
            }
        } catch (Exception ignored) {}

        // Initialiser avec CAPTCHA mathématique par défaut
        setupMathCaptcha();
    }

    /**
     * Change la méthode de CAPTCHA selon le choix de l'utilisateur
     */
    @FXML
    private void switchCaptchaMethod(ActionEvent event) {
        captchaVerified = false;
        captchaBypassed = false;

        // Cacher tous les CAPTCHA
        mathCaptchaBox.setVisible(false);
        mathCaptchaBox.setManaged(false);
        recaptchaBox.setVisible(false);
        recaptchaBox.setManaged(false);
        puzzleCaptchaBox.setVisible(false);
        puzzleCaptchaBox.setManaged(false);

        if (puzzleResultLabel != null) {
            puzzleResultLabel.setVisible(false);
        }

        // Afficher le CAPTCHA sélectionné
        if (mathCaptchaRadio.isSelected()) {
            System.out.println("[LoginCaptchaChoice] Méthode: CAPTCHA Mathématique");
            setupMathCaptcha();
            mathCaptchaBox.setVisible(true);
            mathCaptchaBox.setManaged(true);
        } else if (recaptchaRadio.isSelected()) {
            System.out.println("[LoginCaptchaChoice] Méthode: Google reCAPTCHA");
            setupRecaptcha();
            recaptchaBox.setVisible(true);
            recaptchaBox.setManaged(true);
        } else if (puzzleCaptchaRadio.isSelected()) {
            System.out.println("[LoginCaptchaChoice] Méthode: Puzzle CAPTCHA");
            setupPuzzleCaptcha();
            puzzleCaptchaBox.setVisible(true);
            puzzleCaptchaBox.setManaged(true);
        }
    }

    /**
     * Configure le CAPTCHA mathématique simple
     */
    private void setupMathCaptcha() {
        int a = (int) (Math.random() * 10) + 1;
        int b = (int) (Math.random() * 10) + 1;
        mathCaptchaExpectedAnswer = a + b;
        captchaQuestion.setText("Combien fait " + a + " + " + b + " ?");
        captchaAnswer.clear();
        System.out.println("[MathCaptcha] Question générée: " + a + " + " + b + " = " + mathCaptchaExpectedAnswer);
    }

    /**
     * Configure Google reCAPTCHA
     */
    private void setupRecaptcha() {
        if (!captchaService.isConfigured()) {
            showError("reCAPTCHA n'est pas configuré. Utilisez une autre méthode.");
            mathCaptchaRadio.setSelected(true);
            switchCaptchaMethod(null);
            return;
        }

        String siteKey = captchaService.getSiteKey();
        System.out.println("[reCAPTCHA] Configuration avec site key: " + siteKey.substring(0, Math.min(10, siteKey.length())) + "...");

        WebEngine engine = captchaWebView.getEngine();
        engine.setJavaScriptEnabled(true);
        engine.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                try {
                    netscape.javascript.JSObject window = (netscape.javascript.JSObject) engine.executeScript("window");
                    window.setMember("captchaBridge", new CaptchaBridge());
                    System.out.println("[reCAPTCHA] WebView chargé et bridge connecté");
                } catch (Exception e) {
                    System.err.println("[reCAPTCHA] Erreur bridge: " + e.getMessage());
                    showError("Erreur de chargement reCAPTCHA");
                }
            } else if (newState == javafx.concurrent.Worker.State.FAILED) {
                System.err.println("[reCAPTCHA] Échec du chargement");
                showError("Échec du chargement reCAPTCHA");
            }
        });

        try {
            if (captchaHttpServer == null) {
                captchaHttpServer = new CaptchaHttpServer(siteKey);
                captchaHttpServer.start();
            }
            String url = captchaHttpServer.getCaptchaUrl();
            engine.load(url);
            System.out.println("[reCAPTCHA] Chargé depuis: " + url);
        } catch (Exception e) {
            System.err.println("[reCAPTCHA] Erreur serveur: " + e.getMessage());
            showError("Erreur de démarrage du serveur reCAPTCHA");
        }
    }

    /**
     * Configure le Puzzle CAPTCHA
     */
    private void setupPuzzleCaptcha() {
        currentPuzzle = puzzleCaptchaService.generatePuzzle();
        if (currentPuzzle == null) {
            showError("Erreur de génération du puzzle");
            mathCaptchaRadio.setSelected(true);
            switchCaptchaMethod(null);
            return;
        }

        // Charger les images
        puzzleBackgroundImage.setImage(
            PuzzleCaptchaService.base64ToImage(currentPuzzle.getBackgroundImageBase64())
        );
        puzzlePieceImage.setImage(
            PuzzleCaptchaService.base64ToImage(currentPuzzle.getPuzzlePieceBase64())
        );

        // Réinitialiser le slider
        puzzleSlider.setValue(0);
        puzzleResultLabel.setVisible(false);

        System.out.println("[PuzzleCaptcha] Puzzle généré, position correcte: " + currentPuzzle.getCorrectPosition());
    }

    /**
     * Régénère un nouveau puzzle
     */
    @FXML
    private void regeneratePuzzle(ActionEvent event) {
        System.out.println("[PuzzleCaptcha] Régénération du puzzle");
        setupPuzzleCaptcha();
        captchaVerified = false;
    }

    /**
     * Vérifie la position du puzzle
     */
    @FXML
    private void verifyPuzzle(ActionEvent event) {
        if (currentPuzzle == null) {
            showError("Aucun puzzle généré");
            return;
        }

        int userPosition = (int) puzzleSlider.getValue();
        boolean isValid = puzzleCaptchaService.verifyPosition(userPosition, currentPuzzle.getCorrectPosition());

        if (isValid) {
            captchaVerified = true;
            puzzleResultLabel.setText("✅ Correct!");
            puzzleResultLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            puzzleResultLabel.setVisible(true);
            System.out.println("[PuzzleCaptcha] Vérification réussie");
        } else {
            captchaVerified = false;
            puzzleResultLabel.setText("❌ Position incorrecte, réessayez");
            puzzleResultLabel.setStyle("-fx-text-fill: red;");
            puzzleResultLabel.setVisible(true);
            System.out.println("[PuzzleCaptcha] Vérification échouée");
        }
    }

    /**
     * Bypass temporaire du CAPTCHA
     */
    @FXML
    private void bypassCaptcha(ActionEvent event) {
        captchaBypassed = true;
        captchaVerified = true;
        System.out.println("[LoginCaptchaChoice] CAPTCHA bypassé (temporaire)");
        captchaContainer.setVisible(false);
        captchaContainer.setManaged(false);
    }

    /**
     * Gère la connexion
     */
    @FXML
    private void handleLogin(ActionEvent event) {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        // Validation des champs
        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs");
            return;
        }

        // Vérification du CAPTCHA
        if (!captchaBypassed && !verifyCaptcha()) {
            showError("Veuillez compléter la vérification CAPTCHA");
            return;
        }

        // Tentative de connexion
        try {
            Optional<User> userOpt = userService.login(email, password);

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                System.out.println("[LoginCaptchaChoice] Connexion réussie: " + user.getEmail());

                // Redirection selon le rôle
                navigateToDashboard(event, user);
            } else {
                showError("Email ou mot de passe incorrect");
            }
        } catch (RuntimeException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            showError("Erreur de connexion: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Vérifie le CAPTCHA selon la méthode sélectionnée
     */
    private boolean verifyCaptcha() {
        if (mathCaptchaRadio.isSelected()) {
            // CAPTCHA Mathématique
            try {
                int userAnswer = Integer.parseInt(captchaAnswer.getText().trim());
                if (userAnswer == mathCaptchaExpectedAnswer) {
                    System.out.println("[MathCaptcha] Vérification réussie");
                    return true;
                } else {
                    System.out.println("[MathCaptcha] Réponse incorrecte");
                    setupMathCaptcha(); // Régénérer
                    return false;
                }
            } catch (NumberFormatException e) {
                System.out.println("[MathCaptcha] Format invalide");
                return false;
            }
        } else if (recaptchaRadio.isSelected()) {
            // reCAPTCHA
            if (captchaToken != null && !captchaToken.isEmpty()) {
                boolean valid = captchaService.verifyToken(captchaToken);
                System.out.println("[reCAPTCHA] Vérification: " + (valid ? "réussie" : "échouée"));
                return valid;
            } else {
                System.out.println("[reCAPTCHA] Token manquant");
                return false;
            }
        } else if (puzzleCaptchaRadio.isSelected()) {
            // Puzzle CAPTCHA
            if (captchaVerified) {
                System.out.println("[PuzzleCaptcha] Déjà vérifié");
                return true;
            } else {
                System.out.println("[PuzzleCaptcha] Non vérifié");
                return false;
            }
        }

        return false;
    }

    /**
     * Bridge pour reCAPTCHA
     */
    public class CaptchaBridge {
        public void onCaptchaSuccess(String token) {
            javafx.application.Platform.runLater(() -> {
                captchaToken = token;
                captchaVerified = true;
                System.out.println("[reCAPTCHA] Token reçu: " + token.substring(0, Math.min(20, token.length())) + "...");
            });
        }

        public void onCaptchaFailed(String reason) {
            javafx.application.Platform.runLater(() -> {
                System.err.println("[reCAPTCHA] Échec: " + reason);
                showError("Échec de la vérification reCAPTCHA");
            });
        }
    }

    /**
     * Navigation vers le dashboard
     */
    private void navigateToDashboard(ActionEvent event, User user) {
        try {
            String fxmlPath = switch (user.getTypeUtilisateur()) {
                case ADMINISTRATEUR -> "/fxml/admin_users.fxml";
                case INVESTISSEUR -> "/fxml/investisseur_dashboard.fxml";
                case PORTEUR_PROJET -> "/fxml/porteur_projet_dashboard.fxml";
                case EXPERT_CARBONE -> "/fxml/expert_carbone_dashboard.fxml";
                default -> "/fxml/dashboard.fxml";
            };

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("GreenLedger - Dashboard");
            stage.show();

            System.out.println("[LoginCaptchaChoice] Navigation vers: " + fxmlPath);
        } catch (IOException e) {
            showError("Erreur de navigation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Mot de passe oublié
     */
    @FXML
    private void handleForgotPassword(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/forgot_password.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Mot de passe oublié");
            stage.show();
        } catch (IOException e) {
            showError("Erreur: " + e.getMessage());
        }
    }

    /**
     * Créer un compte
     */
    @FXML
    private void handleRegister(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Inscription");
            stage.show();
        } catch (IOException e) {
            showError("Erreur: " + e.getMessage());
        }
    }

    /**
     * Affiche un message d'erreur
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        System.err.println("[LoginCaptchaChoice] Erreur: " + message);
    }
}
