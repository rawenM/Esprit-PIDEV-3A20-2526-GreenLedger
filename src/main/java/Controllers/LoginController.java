package Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import Models.User;
import Services.AuditLogService;
import Services.IUserService;
import Services.UserServiceImpl;
import Utils.CaptchaHttpServer;
import Utils.CaptchaService;
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
    @FXML private Button recaptchaChoiceBtn;
    @FXML private Button puzzleChoiceBtn;
    @FXML private Button bypassCaptchaBtn;
    @FXML private Button regeneratePuzzleBtn;
    @FXML private Hyperlink forgotPasswordLink;
    @FXML private Hyperlink registerLink;
    @FXML private WebView captchaWebView;
    @FXML private javafx.scene.layout.VBox captchaContainer;
    @FXML private javafx.scene.layout.VBox recaptchaBox;
    @FXML private javafx.scene.layout.VBox puzzleBox;
    @FXML private javafx.scene.layout.StackPane puzzleBackgroundPane;
    @FXML private javafx.scene.image.ImageView puzzleBackgroundImage;
    @FXML private javafx.scene.layout.StackPane puzzlePiecePane;
    @FXML private javafx.scene.image.ImageView puzzlePieceImage;
    @FXML private Slider puzzleSlider;
    @FXML private Label puzzleResultLabel;

    private final IUserService userService = new UserServiceImpl();
    private final CaptchaService captchaService = new CaptchaService();
    private final Utils.PuzzleCaptchaService puzzleCaptchaService = new Utils.PuzzleCaptchaService();
    private CaptchaHttpServer captchaHttpServer;
    private String captchaToken;
    private Utils.PuzzleCaptchaService.PuzzleCaptchaResult currentPuzzle;
    private boolean captchaBypassed = false;
    private boolean puzzleVerified = false;
    private boolean usingRecaptcha = false;
    private boolean usingPuzzle = false;
    private javafx.animation.Timeline captchaResizeTimeline;

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);

        // Permettre la connexion avec la touche Enter
        passwordField.setOnAction(e -> handleLogin(e));

        // Si une session existe (reconnexion automatique), rediriger
        try {
            if (SessionManager.getInstance().isLogged()) {
                User u = SessionManager.getInstance().getCurrentUser();
                if (u != null) {
                    System.out.println("[CLEAN] Session active trouvée pour: " + u.getEmail());
                }
            }
        } catch (Exception ignored) {}

        // Afficher les 2 choix de captcha
        System.out.println("[CLEAN] Page de login avec 2 choix de captcha");
    }

    /**
     * L'utilisateur choisit Google reCAPTCHA
     */
    @FXML
    private void chooseRecaptcha(ActionEvent event) {
        System.out.println("[CLEAN] Utilisateur a choisi: Google reCAPTCHA");
        usingRecaptcha = true;
        usingPuzzle = false;
        puzzleVerified = false;
        captchaBypassed = false;
        
        // Afficher le container
        captchaContainer.setVisible(true);
        captchaContainer.setManaged(true);
        
        // Afficher reCAPTCHA, cacher puzzle
        recaptchaBox.setVisible(true);
        recaptchaBox.setManaged(true);
        puzzleBox.setVisible(false);
        puzzleBox.setManaged(false);
        
        setupRecaptcha();
    }

    /**
     * L'utilisateur choisit Captcha Puzzle
     */
    @FXML
    private void choosePuzzle(ActionEvent event) {
        System.out.println("[CLEAN] Utilisateur a choisi: Captcha Puzzle");
        usingRecaptcha = false;
        usingPuzzle = true;
        puzzleVerified = false;
        captchaBypassed = false;
        
        // Afficher le container
        captchaContainer.setVisible(true);
        captchaContainer.setManaged(true);
        
        // Afficher puzzle, cacher reCAPTCHA
        puzzleBox.setVisible(true);
        puzzleBox.setManaged(true);
        recaptchaBox.setVisible(false);
        recaptchaBox.setManaged(false);
        
        setupPuzzleCaptcha();
    }

    /**
     * Configure Google reCAPTCHA
     */
    private void setupRecaptcha() {
        if (!captchaService.isConfigured()) {
            showError("reCAPTCHA n'est pas configuré");
            return;
        }

        String siteKey = captchaService.getSiteKey();
        System.out.println("[CLEAN] reCAPTCHA configured with site key: " + siteKey.substring(0, Math.min(10, siteKey.length())) + "...");
        
        WebEngine engine = captchaWebView.getEngine();
        engine.setJavaScriptEnabled(true);
        engine.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            System.out.println("[CLEAN] WebView state: " + newState);
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                try {
                    netscape.javascript.JSObject window =
                            (netscape.javascript.JSObject) engine.executeScript("window");
                    window.setMember("captchaBridge", new CaptchaBridge());
                    startCaptchaAutoResize(engine);
                    System.out.println("[CLEAN] reCAPTCHA WebView loaded and captchaBridge connected");
                } catch (Exception e) {
                    System.err.println("[CLEAN] Failed to initialize reCAPTCHA bridge: " + e.getMessage());
                    e.printStackTrace();
                }
            } else if (newState == javafx.concurrent.Worker.State.FAILED) {
                System.err.println("[CLEAN] WebView failed to load");
            }
        });
        
        try {
            if (captchaHttpServer == null) {
                captchaHttpServer = new CaptchaHttpServer(siteKey);
                captchaHttpServer.start();
            }
            String url = captchaHttpServer.getCaptchaUrl();
            engine.load(url);
            System.out.println("[CLEAN] Loaded captcha from local server: " + url);
        } catch (Exception e) {
            System.err.println("[CLEAN] Failed to start local captcha server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Configure le Puzzle CAPTCHA
     */
    private void setupPuzzleCaptcha() {
        currentPuzzle = puzzleCaptchaService.generatePuzzle();
        if (currentPuzzle == null) {
            showError("Erreur de génération du puzzle");
            return;
        }

        // Charger les images
        puzzleBackgroundImage.setImage(
            Utils.PuzzleCaptchaService.base64ToImage(currentPuzzle.getBackgroundImageBase64())
        );
        puzzlePieceImage.setImage(
            Utils.PuzzleCaptchaService.base64ToImage(currentPuzzle.getPuzzlePieceBase64())
        );

        // Réinitialiser le slider
        puzzleSlider.setValue(0);
        puzzleResultLabel.setVisible(false);
        puzzleVerified = false;
        
        // Ajouter un listener pour déplacer visuellement la pièce avec le slider
        puzzleSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            // Déplacer la pièce horizontalement selon la valeur du slider
            double translateX = newVal.doubleValue();
            puzzlePiecePane.setTranslateX(translateX);
        });

        System.out.println("[CLEAN] Puzzle généré, position correcte: " + currentPuzzle.getCorrectPosition());
        System.out.println("[CLEAN] Images chargées - Background: " + (puzzleBackgroundImage.getImage() != null) + 
                          ", Piece: " + (puzzlePieceImage.getImage() != null));
    }

    /**
     * Régénère un nouveau puzzle
     */
    @FXML
    private void regeneratePuzzle(ActionEvent event) {
        System.out.println("[CLEAN] Régénération du puzzle");
        setupPuzzleCaptcha();
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
            puzzleVerified = true;
            puzzleResultLabel.setText("✅ Correct!");
            puzzleResultLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            puzzleResultLabel.setVisible(true);
            System.out.println("[CLEAN] Puzzle vérifié avec succès");
        } else {
            puzzleVerified = false;
            puzzleResultLabel.setText("❌ Position incorrecte, réessayez");
            puzzleResultLabel.setStyle("-fx-text-fill: red;");
            puzzleResultLabel.setVisible(true);
            System.out.println("[CLEAN] Puzzle vérification échouée");
        }
    }

    /**
     * Bypass temporaire du CAPTCHA (pour démo)
     */
    @FXML
    private void bypassCaptcha(ActionEvent event) {
        captchaBypassed = true;
        System.out.println("[CLEAN] Captcha bypass enabled (temporary)");
        showError("Mode démo: captcha bypassé");
        if (captchaContainer != null) {
            captchaContainer.setVisible(false);
            captchaContainer.setManaged(false);
        }
    }

    private class CaptchaBridge {
        public void onCaptchaSuccess(String token) {
            captchaToken = token;
            System.out.println("[CLEAN] reCAPTCHA token received (length: " + (token != null ? token.length() : 0) + ")");
            javafx.application.Platform.runLater(() -> {
                System.out.println("[CLEAN] Captcha validated successfully");
            });
        }
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

        // Verify captcha
        if (captchaBypassed) {
            System.out.println("[CLEAN] Captcha bypassed for this login");
        } else if (usingRecaptcha) {
            // Verify reCAPTCHA
            if ((captchaToken == null || captchaToken.trim().isEmpty()) && captchaWebView != null) {
                try {
                    Object response = captchaWebView.getEngine().executeScript(
                            "(window.grecaptcha && grecaptcha.getResponse) ? grecaptcha.getResponse() : ''"
                    );
                    if (response != null) {
                        captchaToken = response.toString();
                    }
                } catch (Exception e) {
                    System.err.println("[CLEAN] Failed to read captcha response from WebView: " + e.getMessage());
                }
            }

            if (captchaToken == null || captchaToken.trim().isEmpty()) {
                showError("Veuillez completer le captcha");
                return;
            }
            if (!captchaService.verifyToken(captchaToken)) {
                showError("Captcha invalide. Reessayez.");
                resetCaptcha();
                return;
            }
            System.out.println("[CLEAN] reCAPTCHA verified");
        } else if (usingPuzzle) {
            // Verify Puzzle
            if (!puzzleVerified) {
                showError("Veuillez completer et verifier le puzzle");
                return;
            }
            System.out.println("[CLEAN] Puzzle verified");
        } else {
            // Aucun captcha sélectionné
            showError("Veuillez choisir une methode de verification");
            return;
        }

        try {
            Optional<User> userOpt = userService.login(email, password);

            if (userOpt.isPresent()) {
                User user = userOpt.get();

                System.out.println("[CLEAN] Bienvenue " + user.getNomComplet());
                
                // Enregistrer la connexion réussie dans le journal d'activité
                AuditLogService.getInstance().logLogin(user, "127.0.0.1");

                // Rediriger vers le tableau de bord approprié selon le rôle
                navigateToDashboard(event, user);

            } else {
                // Message générique pour limiter la fuite d'information
                showError("Email ou mot de passe incorrect");
                System.err.println("🔒 Échec de connexion pour: " + email);
                
                // Enregistrer l'échec de connexion
                AuditLogService.getInstance().logLoginFailed(email, "Mot de passe incorrect", "127.0.0.1");
            }
        } catch (RuntimeException e) {
            // Messages métiers renvoyés par le service (compte bloqué/suspendu/non vérifié)
            showError(e.getMessage());
            System.err.println("[CLEAN] Exception métier lors de la connexion: " + e.getMessage());
            
            // Enregistrer l'échec de connexion avec la raison
            AuditLogService.getInstance().logLoginFailed(email, e.getMessage(), "127.0.0.1");
        } catch (Exception e) {
            showError("Une erreur est survenue lors de la connexion");
            e.printStackTrace();
            
            // Enregistrer l'erreur système
            AuditLogService.getInstance().logLoginFailed(email, "Erreur système", "127.0.0.1");
        }
    }

    private void resetCaptcha() {
        if (usingRecaptcha) {
            captchaToken = null;
            captchaBypassed = false;
            if (captchaWebView != null && captchaService.isConfigured()) {
                captchaWebView.getEngine().executeScript(
                    "if (window.grecaptcha && grecaptcha.reset) {" +
                    "  grecaptcha.reset();" +
                    "} else if (window.grecaptcha && grecaptcha.execute) {" +
                    "  grecaptcha.execute('" + captchaService.getSiteKey() + "', {action: 'login'}).then(function(token) {" +
                    "    if (window.captchaBridge) { window.captchaBridge.onCaptchaSuccess(token); }" +
                    "  });" +
                    "}"
                );
            }
        } else if (usingPuzzle) {
            setupPuzzleCaptcha();
        }
    }

    private void startCaptchaAutoResize(WebEngine engine) {
        if (captchaWebView == null) return;
        if (captchaResizeTimeline != null) {
            captchaResizeTimeline.stop();
        }
        captchaResizeTimeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(400), e -> {
                    try {
                        Object h = engine.executeScript("document.body ? document.body.scrollHeight : 0");
                        if (h instanceof Number) {
                            double height = ((Number) h).doubleValue();
                            if (height > 0) {
                                double target = Math.min(300, Math.max(120, height + 20));
                                captchaWebView.setPrefHeight(target);
                            }
                        }
                    } catch (Exception ex) {
                        // Ignore resize errors
                    }
                })
        );
        captchaResizeTimeline.setCycleCount(30);
        captchaResizeTimeline.play();
    }

    @FXML
    private void handleForgotPassword(ActionEvent event) {
        try {
            // Charger la nouvelle page de réinitialisation avec code à 6 chiffres
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/forgot_password.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Mot de passe oublié");
            stage.show();
        } catch (IOException e) {
            showError("Erreur lors du chargement de la page: " + e.getMessage());
            e.printStackTrace();
        }
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
