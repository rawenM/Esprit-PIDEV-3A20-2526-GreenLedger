package Utils;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.*;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Base64;

/**
 * Service d'envoi d'emails via l'API Gmail
 * Utilise OAuth2 pour l'authentification
 */
public class GmailApiService {

    private static final String APPLICATION_NAME = "GreenLedger";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_SEND);
    private static final String CREDENTIALS_FILE_PATH = "credentials.json";

    private Gmail service;
    private boolean enabled;
    private String fromEmail;
    private String fromName;

    public GmailApiService() {
        // Load .env file first
        EnvLoader.load();
        
        this.enabled = Boolean.parseBoolean(EnvLoader.get("GMAIL_API_ENABLED", "false"));
        this.fromEmail = EnvLoader.get("GMAIL_FROM_EMAIL", "");
        this.fromName = EnvLoader.get("GMAIL_FROM_NAME", "GreenLedger Team");

        if (enabled) {
            try {
                final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
                this.service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                        .setApplicationName(APPLICATION_NAME)
                        .build();
                System.out.println("[Gmail API] Service initialisé avec succès");
            } catch (Exception e) {
                System.err.println("[Gmail API] Erreur d'initialisation: " + e.getMessage());
                this.enabled = false;
            }
        } else {
            System.out.println("[Gmail API] Service désactivé");
        }
    }

    /**
     * Obtient les credentials OAuth2
     */
    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Charger le fichier credentials.json
        InputStream in = GmailApiService.class.getResourceAsStream("/" + CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Fichier credentials.json non trouvé dans resources/");
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Créer le flow d'autorisation
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(Paths.get(TOKENS_DIRECTORY_PATH).toFile()))
                .setAccessType("offline")
                .build();
        
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    public boolean isConfigured() {
        return enabled && service != null;
    }

    /**
     * Envoie un email de bienvenue
     */
    public boolean sendWelcomeEmail(String toEmail, String fullName) {
        String subject = "Bienvenue sur GreenLedger ! 🌱";
        String htmlContent = buildWelcomeEmailHtml(fullName);
        return sendEmail(toEmail, subject, htmlContent);
    }

    /**
     * Envoie un email de vérification
     */
    public boolean sendVerificationEmail(String toEmail, String fullName, String verificationToken) {
        String subject = "Verifiez votre compte GreenLedger";
        String verificationUrl = System.getenv().getOrDefault("APP_RESET_URL_PREFIX", "http://127.0.0.1:8088/verify?token=") + verificationToken;
        String htmlContent = buildVerificationEmailHtml(fullName, verificationUrl);
        return sendEmail(toEmail, subject, htmlContent);
    }

    /**
     * Envoie un email de réinitialisation de mot de passe
     */
    public boolean sendResetPasswordEmail(String toEmail, String fullName, String resetToken) {
        String subject = "Reinitialisation de votre mot de passe";
        String resetUrl = System.getenv().getOrDefault("APP_RESET_URL_PREFIX", "http://127.0.0.1:8088/reset?token=") + resetToken;
        String htmlContent = buildResetPasswordEmailHtml(fullName, resetUrl);
        return sendEmail(toEmail, subject, htmlContent);
    }

    /**
     * Envoie un email de compte approuvé
     */
    public boolean sendAccountApprovedEmail(String toEmail, String fullName) {
        String subject = "Votre compte a ete approuve ! ✅";
        String htmlContent = buildAccountApprovedEmailHtml(fullName);
        return sendEmail(toEmail, subject, htmlContent);
    }

    /**
     * Envoie un email de compte rejeté
     */
    public boolean sendAccountRejectedEmail(String toEmail, String fullName, String reason) {
        String subject = "Mise a jour de votre compte GreenLedger";
        String htmlContent = buildAccountRejectedEmailHtml(fullName, reason);
        return sendEmail(toEmail, subject, htmlContent);
    }

    /**
     * Envoie un email de compte bloqué
     */
    public boolean sendAccountBlockedEmail(String toEmail, String fullName, String reason) {
        String subject = "Votre compte a ete suspendu";
        String htmlContent = buildAccountBlockedEmailHtml(fullName, reason);
        return sendEmail(toEmail, subject, htmlContent);
    }

    /**
     * Envoie un email de compte débloqué
     */
    public boolean sendAccountUnblockedEmail(String toEmail, String fullName) {
        String subject = "Votre compte a ete réactive ! 🎉";
        String htmlContent = buildAccountUnblockedEmailHtml(fullName);
        return sendEmail(toEmail, subject, htmlContent);
    }

    /**
     * Méthode générique pour envoyer un email via Gmail API
     */
    private boolean sendEmail(String toEmail, String subject, String htmlContent) {
        if (!isConfigured()) {
            System.out.println("[Gmail API] Service non configuré - Email simulé");
            System.out.println("  To: " + toEmail);
            System.out.println("  Subject: " + subject);
            return false;
        }

        try {
            MimeMessage email = createEmail(toEmail, fromEmail, subject, htmlContent);
            Message message = createMessageWithEmail(email);
            service.users().messages().send("me", message).execute();
            
            System.out.println("[Gmail API] Email envoyé avec succès à: " + toEmail);
            return true;
        } catch (Exception e) {
            System.err.println("[Gmail API] Erreur lors de l'envoi: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Crée un MimeMessage
     */
    private MimeMessage createEmail(String to, String from, String subject, String bodyText) throws Exception {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(from, fromName));
        email.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject);
        email.setContent(bodyText, "text/html; charset=utf-8");
        return email;
    }

    /**
     * Convertit MimeMessage en Message Gmail
     */
    private Message createMessageWithEmail(MimeMessage emailContent) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.getUrlEncoder().encodeToString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    // Templates HTML
    private String buildWelcomeEmailHtml(String fullName) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                "<h2 style='color: #2ecc71;'>Bienvenue sur GreenLedger ! 🌱</h2>" +
                "<p>Bonjour <strong>" + fullName + "</strong>,</p>" +
                "<p>Nous sommes ravis de vous accueillir sur GreenLedger, la plateforme dediee aux projets ecologiques et durables.</p>" +
                "<p>Votre compte a été cree avec succes et est en attente de validation par notre equipe.</p>" +
                "<p>Vous recevrez un email de confirmation des que votre compte sera active.</p>" +
                "<p>À très bientôt,<br>L equipe GreenLedger</p>" +
                "</div></body></html>";
    }

    private String buildVerificationEmailHtml(String fullName, String verificationUrl) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                "<h2 style='color: #3498db;'>Vérifiez votre compte</h2>" +
                "<p>Bonjour <strong>" + fullName + "</strong>,</p>" +
                "<p>Cliquez sur le lien ci dessous pour verifier votre compte :</p>" +
                "<p><a href='" + verificationUrl + "' style='background-color: #3498db; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; display: inline-block;'>Vérifier mon compte</a></p>" +
                "<p>Ce lien expire dans 24 heures.</p>" +
                "<p>L'équipe GreenLedger</p>" +
                "</div></body></html>";
    }

    private String buildResetPasswordEmailHtml(String fullName, String resetUrl) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                "<h2 style='color: #e74c3c;'>Réinitialisation de mot de passe</h2>" +
                "<p>Bonjour <strong>" + fullName + "</strong>,</p>" +
                "<p>Vous avez demande a reinitialiser votre mot de passe. Cliquez sur le lien ci-dessous :</p>" +
                "<p><a href='" + resetUrl + "' style='background-color: #e74c3c; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; display: inline-block;'>Réinitialiser mon mot de passe</a></p>" +
                "<p>Ce lien expire dans 1 heure.</p>" +
                "<p>Si vous n avez pas demande cette reinitialisation, ignorez cet email.</p>" +
                "<p>L equipe GreenLedger</p>" +
                "</div></body></html>";
    }

    private String buildAccountApprovedEmailHtml(String fullName) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                "<h2 style='color: #2ecc71;'>Compte approuvé ! ✅</h2>" +
                "<p>Bonjour <strong>" + fullName + "</strong>,</p>" +
                "<p>Bonne nouvelle ! Votre compte GreenLedger a ete approuve par notre equipe.</p>" +
                "<p>Vous pouvez maintenant vous connecter et profiter de toutes les fonctionnalites de la plateforme.</p>" +
                "<p>Bienvenue dans la communaute GreenLedger !</p>" +
                "<p>L equipe GreenLedger</p>" +
                "</div></body></html>";
    }

    private String buildAccountRejectedEmailHtml(String fullName, String reason) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                "<h2 style='color: #e67e22;'>Mise à jour de votre compte</h2>" +
                "<p>Bonjour <strong>" + fullName + "</strong>,</p>" +
                "<p>Nous avons examine votre demande d inscription sur GreenLedger.</p>" +
                "<p>Malheureusement, nous ne pouvons pas approuver votre compte pour la raison suivante :</p>" +
                "<p style='background-color: #f8f9fa; padding: 15px; border-left: 4px solid #e67e22;'>" + reason + "</p>" +
                "<p>Si vous pensez qu'il s agit d'une erreur, n hesitez pas a nous contacter.</p>" +
                "<p>L equipe GreenLedger</p>" +
                "</div></body></html>";
    }

    private String buildAccountBlockedEmailHtml(String fullName, String reason) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                "<h2 style='color: #e74c3c;'>Compte suspendu</h2>" +
                "<p>Bonjour <strong>" + fullName + "</strong>,</p>" +
                "<p>Votre compte GreenLedger a ete temporairement suspendu.</p>" +
                "<p>Raison : <strong>" + reason + "</strong></p>" +
                "<p>Pour plus d informations ou pour contester cette decision, veuillez contacter notre support.</p>" +
                "<p>L equipe GreenLedger</p>" +
                "</div></body></html>";
    }

    private String buildAccountUnblockedEmailHtml(String fullName) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                "<h2 style='color: #2ecc71;'>Compte réactivé ! 🎉</h2>" +
                "<p>Bonjour <strong>" + fullName + "</strong>,</p>" +
                "<p>Bonne nouvelle ! Votre compte GreenLedger a été reactive.</p>" +
                "<p>Vous pouvez a nouveau acceder a toutes les fonctionnalites de la plateforme.</p>" +
                "<p>Merci de votre patience.</p>" +
                "<p>L equipe GreenLedger</p>" +
                "</div></body></html>";
    }
}
