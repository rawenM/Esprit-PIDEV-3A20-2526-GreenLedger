package Controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import Models.User;
import Services.AuditLogService;
import Services.IUserService;
import Services.UserServiceImpl;
import Utils.UnifiedEmailService;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Contrôleur pour la réinitialisation du mot de passe avec code de vérification
 * 
 * Flux:
 * 1. Utilisateur entre son email
 * 2. Code à 6 chiffres généré et envoyé par email
 * 3. Utilisateur entre le code + nouveau mot de passe
 * 4. Vérification et réinitialisation
 */
public class ForgotPasswordController {

    // Étape 1: Demander l'email
    @FXML private VBox step1Box;
    @FXML private TextField emailField;
    @FXML private Button sendCodeButton;

    // Étape 2: Vérifier le code et changer le mot de passe
    @FXML private VBox step2Box;
    @FXML private TextField emailDisplayField;
    @FXML private TextField codeField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button resetPasswordButton;
    @FXML private Label timerLabel;

    // Messages
    @FXML private Label messageLabel;
    @FXML private javafx.scene.text.Text subtitleText;

    // Services
    private final IUserService userService = new UserServiceImpl();
    private final UnifiedEmailService emailService = new UnifiedEmailService();
    private final SecureRandom random = new SecureRandom();

    // État
    private String currentEmail;
    private String generatedCode;
    private LocalDateTime codeExpiry;
    private Timeline countdownTimeline;
    private static final int CODE_EXPIRY_MINUTES = 10;

    @FXML
    public void initialize() {
        messageLabel.setVisible(false);
    }

    /**
     * ÉTAPE 1: Envoyer le code de vérification par email
     */
    @FXML
    private void handleSendCode(ActionEvent event) {
        String email = emailField.getText().trim();

        // Validation de l'email
        if (email.isEmpty()) {
            showError("Veuillez entrer votre adresse email");
            return;
        }

        if (!email.contains("@") || !email.contains(".")) {
            showError("Adresse email invalide");
            return;
        }

        // Vérifier si l'utilisateur existe
        Optional<User> userOpt = userService.getUserByEmail(email);
        if (userOpt.isEmpty()) {
            showError("Aucun compte associé à cet email");
            return;
        }

        User user = userOpt.get();

        // Générer un code à 6 chiffres
        generatedCode = generateVerificationCode();
        codeExpiry = LocalDateTime.now().plusMinutes(CODE_EXPIRY_MINUTES);
        currentEmail = email;

        System.out.println("[ForgotPassword] Code de vérification généré et sera envoyé à: " + email);

        // Envoyer le code par email
        boolean sent = sendVerificationCodeEmail(user.getNomComplet(), email, generatedCode);

        if (sent) {
            showSuccess("Code envoyé! Vérifiez votre boîte email.");
            
            // Passer à l'étape 2
            step1Box.setVisible(false);
            step1Box.setManaged(false);
            step2Box.setVisible(true);
            step2Box.setManaged(true);
            
            emailDisplayField.setText(email);
            subtitleText.setText("Un code à 6 chiffres a été envoyé à " + email);
            
            // Démarrer le compte à rebours
            startCountdown();
            
        } else {
            showError("Erreur lors de l'envoi de l'email. Veuillez réessayer.");
        }
    }

    /**
     * ÉTAPE 2: Vérifier le code et réinitialiser le mot de passe
     */
    @FXML
    private void handleResetPassword(ActionEvent event) {
        String code = codeField.getText().trim();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Validation du code
        if (code.isEmpty()) {
            showError("Veuillez entrer le code de vérification");
            return;
        }

        if (code.length() != 6 || !code.matches("\\d{6}")) {
            showError("Le code doit contenir exactement 6 chiffres");
            return;
        }

        // Vérifier si le code a expiré
        if (LocalDateTime.now().isAfter(codeExpiry)) {
            showError("Le code a expiré. Veuillez demander un nouveau code.");
            return;
        }

        // Vérifier le code
        if (!code.equals(generatedCode)) {
            showError("Code incorrect. Veuillez réessayer.");
            return;
        }

        // Validation du mot de passe
        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showError("Veuillez remplir tous les champs");
            return;
        }

        if (newPassword.length() < 8) {
            showError("Le mot de passe doit contenir au moins 8 caractères");
            return;
        }

        if (!newPassword.matches(".*[A-Z].*")) {
            showError("Le mot de passe doit contenir au moins une majuscule");
            return;
        }

        if (!newPassword.matches(".*\\d.*")) {
            showError("Le mot de passe doit contenir au moins un chiffre");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showError("Les mots de passe ne correspondent pas");
            return;
        }

        // Réinitialiser le mot de passe
        try {
            // Récupérer l'utilisateur par email
            Optional<User> userOpt = userService.getUserByEmail(currentEmail);
            if (userOpt.isEmpty()) {
                showError("Utilisateur introuvable");
                return;
            }
            
            User user = userOpt.get();
            
            // Mettre à jour le mot de passe directement
            user.setMotDePasse(Utils.PasswordUtil.hashPassword(newPassword));
            
            // Sauvegarder dans la base de données
            User updatedUser = userService.updateProfile(user);

            if (updatedUser != null) {
                // Arrêter le compte à rebours
                if (countdownTimeline != null) {
                    countdownTimeline.stop();
                }
                
                // Enregistrer la réinitialisation dans le journal d'activité
                AuditLogService.getInstance().logPasswordReset(currentEmail);

                showSuccess("Mot de passe réinitialisé avec succès!");
                
                // Attendre 2 secondes puis retourner à la connexion
                Timeline delay = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
                    try {
                        handleBackToLogin(null);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }));
                delay.play();
                
            } else {
                showError("Erreur lors de la réinitialisation. Veuillez réessayer.");
            }
        } catch (Exception e) {
            showError("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Renvoyer le code de vérification
     */
    @FXML
    private void handleResendCode(ActionEvent event) {
        if (currentEmail == null || currentEmail.isEmpty()) {
            showError("Erreur: email non défini");
            return;
        }

        // Générer un nouveau code
        generatedCode = generateVerificationCode();
        codeExpiry = LocalDateTime.now().plusMinutes(CODE_EXPIRY_MINUTES);

        System.out.println("[ForgotPassword] Nouveau code de vérification généré");

        // Récupérer l'utilisateur
        Optional<User> userOpt = userService.getUserByEmail(currentEmail);
        if (userOpt.isEmpty()) {
            showError("Utilisateur introuvable");
            return;
        }

        User user = userOpt.get();

        // Envoyer le nouveau code
        boolean sent = sendVerificationCodeEmail(user.getNomComplet(), currentEmail, generatedCode);

        if (sent) {
            showSuccess("Nouveau code envoyé!");
            codeField.clear();
            
            // Redémarrer le compte à rebours
            if (countdownTimeline != null) {
                countdownTimeline.stop();
            }
            startCountdown();
        } else {
            showError("Erreur lors de l'envoi. Veuillez réessayer.");
        }
    }

    /**
     * Retour à la page de connexion
     */
    @FXML
    private void handleBackToLogin(ActionEvent event) {
        try {
            // Arrêter le compte à rebours
            if (countdownTimeline != null) {
                countdownTimeline.stop();
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            
            Stage stage;
            if (event != null) {
                stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            } else {
                stage = (Stage) emailField.getScene().getWindow();
            }
            
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Connexion");
            stage.show();
        } catch (IOException e) {
            showError("Erreur de navigation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Génère un code de vérification à 6 chiffres
     */
    private String generateVerificationCode() {
        int code = 100000 + random.nextInt(900000); // 100000 à 999999
        return String.valueOf(code);
    }

    /**
     * Envoie le code de vérification par email
     */
    private boolean sendVerificationCodeEmail(String fullName, String email, String code) {
        try {
            String subject = "Code de vérification - Réinitialisation mot de passe";
            
            String htmlContent = buildVerificationEmailHtml(fullName, code);
            
            boolean sent = emailService.sendEmail(email, subject, htmlContent);
            
            if (sent) {
                System.out.println("[ForgotPassword] Email envoyé à: " + email);
            } else {
                System.err.println("[ForgotPassword] Échec envoi email à: " + email);
            }
            
            return sent;
            
        } catch (Exception e) {
            System.err.println("[ForgotPassword] Erreur envoi email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Construit le HTML de l'email avec le code
     */
    private String buildVerificationEmailHtml(String fullName, String code) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                ".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }" +
                ".content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }" +
                ".code-box { background: white; border: 2px dashed #667eea; padding: 20px; text-align: center; margin: 20px 0; border-radius: 8px; }" +
                ".code { font-size: 32px; font-weight: bold; color: #667eea; letter-spacing: 5px; }" +
                ".warning { background: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }" +
                ".footer { text-align: center; color: #666; font-size: 12px; margin-top: 20px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h1>🔐 Code de Vérification</h1>" +
                "</div>" +
                "<div class='content'>" +
                "<p>Bonjour <strong>" + fullName + "</strong>,</p>" +
                "<p>Vous avez demandé à réinitialiser votre mot de passe sur <strong>GreenLedger</strong>.</p>" +
                "<p>Voici votre code de vérification:</p>" +
                "<div class='code-box'>" +
                "<div class='code'>" + code + "</div>" +
                "</div>" +
                "<div class='warning'>" +
                "<strong>⚠️ Important:</strong>" +
                "<ul>" +
                "<li>Ce code expire dans <strong>10 minutes</strong></li>" +
                "<li>Ne partagez jamais ce code avec personne</li>" +
                "<li>Si vous n'avez pas demandé cette réinitialisation, ignorez cet email</li>" +
                "</ul>" +
                "</div>" +
                "<p>Entrez ce code sur la page de réinitialisation pour continuer.</p>" +
                "<p>Cordialement,<br><strong>L'équipe GreenLedger</strong></p>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>© 2025 GreenLedger - Plateforme de Financement Participatif</p>" +
                "<p>Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    /**
     * Démarre le compte à rebours pour l'expiration du code
     */
    private void startCountdown() {
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            LocalDateTime now = LocalDateTime.now();
            
            if (now.isAfter(codeExpiry)) {
                timerLabel.setText("Code expiré!");
                timerLabel.setStyle("-fx-text-fill: #ff0000; -fx-font-weight: bold;");
                countdownTimeline.stop();
                return;
            }
            
            long secondsLeft = java.time.Duration.between(now, codeExpiry).getSeconds();
            long minutes = secondsLeft / 60;
            long seconds = secondsLeft % 60;
            
            timerLabel.setText(String.format("Expire dans: %02d:%02d", minutes, seconds));
            
            // Changer la couleur selon le temps restant
            if (secondsLeft < 60) {
                timerLabel.setStyle("-fx-text-fill: #ff0000; -fx-font-weight: bold;"); // Rouge
            } else if (secondsLeft < 180) {
                timerLabel.setStyle("-fx-text-fill: #ff6b6b;"); // Orange
            } else {
                timerLabel.setStyle("-fx-text-fill: #51cf66;"); // Vert
            }
        }));
        
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    /**
     * Affiche un message d'erreur
     */
    private void showError(String message) {
        messageLabel.setText("❌ " + message);
        messageLabel.setStyle("-fx-text-fill: #ff0000; -fx-background-color: #ffe0e0; -fx-padding: 10; -fx-border-radius: 5; -fx-background-radius: 5;");
        messageLabel.setVisible(true);
        System.err.println("[ForgotPassword] Erreur: " + message);
    }

    /**
     * Affiche un message de succès
     */
    private void showSuccess(String message) {
        messageLabel.setText("✅ " + message);
        messageLabel.setStyle("-fx-text-fill: #00aa00; -fx-background-color: #e0ffe0; -fx-padding: 10; -fx-border-radius: 5; -fx-background-radius: 5;");
        messageLabel.setVisible(true);
        System.out.println("[ForgotPassword] Succès: " + message);
    }
}
