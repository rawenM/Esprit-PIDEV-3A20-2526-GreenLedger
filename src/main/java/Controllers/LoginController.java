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
    @FXML private Button fallbackCaptchaBtn;
    @FXML private Button bypassCaptchaBtn;
    @FXML private Hyperlink forgotPasswordLink;
    @FXML private Hyperlink registerLink;
    @FXML private WebView captchaWebView;
    @FXML private javafx.scene.layout.VBox captchaContainer;
    @FXML private javafx.scene.layout.HBox simpleCaptchaBox;
    @FXML private Label captchaQuestion;
    @FXML private TextField captchaAnswer;

    private final IUserService userService = new UserServiceImpl();
    private final CaptchaService captchaService = new CaptchaService();
    private CaptchaHttpServer captchaHttpServer;
    private String captchaToken;
    private int captchaExpectedAnswer = 0;
    private boolean usingSimpleCaptcha = false;
    private boolean captchaBypassed = false;
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

        setupCaptcha();
    }

    private void setupCaptcha() {
        captchaBypassed = false;
        // Try reCAPTCHA first if configured
        if (captchaService.isConfigured() && captchaWebView != null) {
            String siteKey = captchaService.getSiteKey();
            System.out.println("[CLEAN] reCAPTCHA configured with site key: " + siteKey.substring(0, Math.min(10, siteKey.length())) + "...");
            System.out.println("[CLEAN] Loading reCAPTCHA v2 from local server");
            
            WebEngine engine = captchaWebView.getEngine();
            engine.setJavaScriptEnabled(true);
            
            // Enable user agent to avoid blocking
            engine.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            
            // Log load worker exceptions
            try {
                engine.getLoadWorker().exceptionProperty().addListener((o, old, ex) -> {
                    if (ex != null) {
                        System.err.println("[WEBVIEW] Exception: " + ex.getMessage());
                    }
                });
            } catch (Exception ignored) {}
            
            // Set timeout to fallback to simple captcha
            final boolean[] captchaLoaded = {false};
            
            // WebView is already visible by default in FXML
            
            new Thread(() -> {
                try {
                    // Show fallback button after 3 seconds if token not received
                    Thread.sleep(3000);
                    javafx.application.Platform.runLater(() -> {
                        if (!captchaLoaded[0] && fallbackCaptchaBtn != null && !usingSimpleCaptcha) {
                            fallbackCaptchaBtn.setVisible(true);
                            System.out.println("[CLEAN] Showing fallback captcha button after 3s");
                        }
                    });
                    
                    // Auto-switch to simple captcha after 5 seconds total
                    Thread.sleep(2000);
                    if (!captchaLoaded[0]) {
                        javafx.application.Platform.runLater(() -> {
                            System.out.println("[CLEAN] reCAPTCHA timeout - Using simple math captcha fallback");
                            showSimpleCaptcha();
                        });
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
            
            engine.setOnError(event -> {
                System.err.println("[CLEAN] WebView error: " + event.getMessage());
                if (!captchaLoaded[0]) {
                    showSimpleCaptcha();
                }
            });
            
            engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                System.out.println("[CLEAN] WebView state: " + newState);
                if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    try {
                        netscape.javascript.JSObject window =
                                (netscape.javascript.JSObject) engine.executeScript("window");
                        window.setMember("captchaBridge", new CaptchaBridge());
                        captchaLoaded[0] = true;
                        startCaptchaAutoResize(engine);
                        System.out.println("[CLEAN] reCAPTCHA WebView loaded and captchaBridge connected");
                    } catch (Exception e) {
                        System.err.println("[CLEAN] Failed to initialize reCAPTCHA bridge: " + e.getMessage());
                        e.printStackTrace();
                        showSimpleCaptcha();
                    }
                } else if (newState == javafx.concurrent.Worker.State.FAILED) {
                    System.err.println("[CLEAN] WebView failed to load");
                    showSimpleCaptcha();
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
                showSimpleCaptcha();
            }
        } else {
            // No reCAPTCHA configured, use simple captcha
            System.out.println("[CLEAN] reCAPTCHA not configured - Using simple math captcha");
            showSimpleCaptcha();
        }
    }
    
    @FXML
    private void useFallbackCaptcha(ActionEvent event) {
        System.out.println("[CLEAN] User manually requested simple captcha");
        showSimpleCaptcha();
    }

    @FXML
    private void bypassCaptcha(ActionEvent event) {
        captchaBypassed = true;
        System.out.println("[CLEAN] Captcha bypass enabled (temporary)");
        if (captchaContainer != null) {
            captchaContainer.setVisible(false);
            captchaContainer.setManaged(false);
        }
    }
    
    private void showSimpleCaptcha() {
        usingSimpleCaptcha = true;
        if (simpleCaptchaBox != null && captchaQuestion != null) {
            // Hide reCAPTCHA and fallback button
            if (captchaWebView != null) {
                captchaWebView.setVisible(false);
                captchaWebView.setManaged(false);
            }
            if (fallbackCaptchaBtn != null) {
                fallbackCaptchaBtn.setVisible(false);
                fallbackCaptchaBtn.setManaged(false);
            }
            
            // Generate simple math problem
            int num1 = (int)(Math.random() * 10) + 1;
            int num2 = (int)(Math.random() * 10) + 1;
            captchaExpectedAnswer = num1 + num2;
            captchaQuestion.setText("Combien fait " + num1 + " + " + num2 + " ?");
            
            // Show simple captcha
            simpleCaptchaBox.setVisible(true);
            simpleCaptchaBox.setManaged(true);
            
            System.out.println("[CLEAN] Simple math captcha shown: " + num1 + " + " + num2 + " = " + captchaExpectedAnswer);
        }
    }

    private String buildCaptchaHtml(String siteKey) {
        String safeKey = siteKey == null ? "" : siteKey;
        return "<!DOCTYPE html>" +
                "<html><head>" +
                "<meta charset='UTF-8'/>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1'/>" +
                "<script>" +
                "  console.log('[reCAPTCHA] Loading script for key: " + safeKey.substring(0, Math.min(10, safeKey.length())) + "...');" +
                "  window.onerror = function(msg, url, lineNo, columnNo, error) {" +
                "    console.error('[reCAPTCHA Error] ' + msg + ' at ' + url + ':' + lineNo);" +
                "    return false;" +
                "  };" +
                "</script>" +
                "<script src='https://www.google.com/recaptcha/api.js?render=" + safeKey + "' " +
                "        onerror=\"console.error('[reCAPTCHA] Failed to load script from Google')\" " +
                "        onload=\"console.log('[reCAPTCHA] Script loaded successfully')\"></script>" +
                "</head><body style='margin:0; padding:0; background:#f5f5f5; display:flex; align-items:center; justify-content:center; min-height:100px;'>" +
                "<div style='text-align:center; color:#666; padding:20px;'>" +
                "<div style='font-size:14px; font-weight:500;'>🔒 Chargement de la verification...</div>" +
                "<div id='status' style='font-size:11px; margin-top:8px; color:#999;'>Patientez...</div>" +
                "</div>" +
                "<script>" +
                "document.getElementById('status').textContent = 'Initialisation...';" +
                "console.log('[reCAPTCHA] Starting grecaptcha.ready()');" +
                "if (typeof grecaptcha !== 'undefined') {" +
                "  grecaptcha.ready(function() {" +
                "    console.log('[reCAPTCHA] grecaptcha ready, executing...');" +
                "    document.getElementById('status').textContent = 'Execution...';" +
                "    grecaptcha.execute('" + safeKey + "', {action: 'login'}).then(function(token) {" +
                "      console.log('[reCAPTCHA] Token received: ' + token.substring(0, 20) + '...');" +
                "      document.getElementById('status').textContent = '✓ Verifie';" +
                "      if (window.javafxConnector && window.javafxConnector.setToken) {" +
                "        window.javafxConnector.setToken(token);" +
                "        console.log('[reCAPTCHA] Token sent to JavaFX');" +
                "      } else {" +
                "        console.error('[reCAPTCHA] JavaFX connector not found!');" +
                "      }" +
                "    }).catch(function(error) {" +
                "      console.error('[reCAPTCHA] Execute error:', error);" +
                "      document.getElementById('status').textContent = '✗ Erreur';" +
                "    });" +
                "  });" +
                "} else {" +
                "  console.error('[reCAPTCHA] grecaptcha object not found - script failed to load');" +
                "  document.getElementById('status').textContent = 'Utilisation de la verification alternative...';" +
                "}" +
                "</script>" +
                "</body></html>";
    }

    private class CaptchaBridge {
        public void onCaptchaSuccess(String token) {
            captchaToken = token;
            System.out.println("[CLEAN] ✓ reCAPTCHA token received (length: " + (token != null ? token.length() : 0) + ")");
            // Hide fallback button since reCAPTCHA is working
            javafx.application.Platform.runLater(() -> {
                if (fallbackCaptchaBtn != null) {
                    fallbackCaptchaBtn.setVisible(false);
                }
                System.out.println("[CLEAN] Captcha validated successfully");
            });
        }
        
        public void onCaptchaFailed(String reason) {
            System.err.println("[CLEAN] ❌ reCAPTCHA failed: " + reason);
            javafx.application.Platform.runLater(() -> {
                showSimpleCaptcha();
            });
        }
        
        // Legacy compatibility
        public void setToken(String token) {
            onCaptchaSuccess(token);
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
        if (usingSimpleCaptcha) {
            // Verify simple math captcha
            if (captchaAnswer == null || captchaAnswer.getText().trim().isEmpty()) {
                showError("Veuillez repondre a la question de verification");
                return;
            }
            try {
                int userAnswer = Integer.parseInt(captchaAnswer.getText().trim());
                if (userAnswer != captchaExpectedAnswer) {
                    showError("Reponse incorrecte. Reessayez.");
                    showSimpleCaptcha(); // Generate new question
                    captchaAnswer.clear();
                    return;
                }
                System.out.println("[CLEAN] Simple captcha verified");
            } catch (NumberFormatException e) {
                showError("Veuillez entrer un nombre valide");
                return;
            }
        } else if (captchaService.isConfigured()) {
            if (captchaBypassed) {
                System.out.println("[CLEAN] Captcha bypassed for this login");
            } else {
            // Verify reCAPTCHA (v2 checkbox)
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
            }
        }

        try {
            Optional<User> userOpt = userService.login(email, password);

            if (userOpt.isPresent()) {
                User user = userOpt.get();

                System.out.println("[CLEAN] Bienvenue " + user.getNomComplet());

                // Rediriger vers le tableau de bord approprié selon le rôle
                navigateToDashboard(event, user);

            } else {
                // Message générique pour limiter la fuite d'information
                showError("Email ou mot de passe incorrect");
                System.err.println("🔒 Échec de connexion pour: " + email);
            }
        } catch (RuntimeException e) {
            // Messages métiers renvoyés par le service (compte bloqué/suspendu/non vérifié)
            showError(e.getMessage());
            System.err.println("[CLEAN] Exception métier lors de la connexion: " + e.getMessage());
        } catch (Exception e) {
            showError("Une erreur est survenue lors de la connexion");
            e.printStackTrace();
        }
    }

    private void resetCaptcha() {
        if (usingSimpleCaptcha) {
            showSimpleCaptcha();
            if (captchaAnswer != null) {
                captchaAnswer.clear();
            }
        } else {
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
        // Boîte de dialogue pour entrer l'email ou le téléphone
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Mot de passe oublié");
        dialog.setHeaderText("Réinitialisation du mot de passe");
        dialog.setContentText("Entrez votre email ou numéro de téléphone:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(input -> {
            String trimmed = input == null ? "" : input.trim();
            if (trimmed.isEmpty()) {
                showError("Veuillez entrer un email ou un numéro de téléphone");
                return;
            }

            String token = userService.initiatePasswordReset(trimmed);
            if (token != null) {
                // Afficher une alerte informative contenant le token (utile en local/dev) et option d'ouvrir le formulaire de reset
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Réinitialisation envoyée");
                alert.setHeaderText(null);
                String body = "Si cet email/numéro existe, vous recevrez les instructions pour réinitialiser votre mot de passe.\n" +
                        "(En test local, le token est affiché ci-dessous)\n\nToken: " + token;
                alert.setContentText(body);
                ButtonType openReset = new ButtonType("Ouvrir formulaire de reset", ButtonBar.ButtonData.OTHER);
                alert.getButtonTypes().add(openReset);
                Optional<ButtonType> choice = alert.showAndWait();
                if (choice.isPresent() && choice.get() == openReset) {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/reset_password.fxml"));
                        Parent root = loader.load();
                        // Passer le token au contrôleur pour pré-remplir le champ
                        Controllers.ResetPasswordController controller = loader.getController();
                        if (controller != null) {
                            controller.setToken(token);
                        }
                        Stage modal = new Stage();
                        modal.setTitle("Réinitialisation du mot de passe");
                        modal.initOwner(((Node) event.getSource()).getScene().getWindow());
                        modal.setScene(new Scene(root));
                        modal.setResizable(false);
                        modal.showAndWait();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                showError("Aucun utilisateur trouvé pour cet email/numéro");
            }
        });
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
