package Utils;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.InputStream;
import java.util.Properties;

/**
 * EmailService simple avec SMTP.
 * - Lit la configuration depuis les variables d'environnement ou config.properties.
 * - Envoie les emails si SMTP est configure, sinon utilise un fallback console.
 */
public class EmailService {

    private final Properties smtpProps = new Properties();
    private boolean configured = false;

    public EmailService() {
        // Charger depuis variables d'environnement en priorité
        String envHost = System.getenv("SMTP_HOST");
        if (envHost != null && !envHost.isEmpty()) {
            smtpProps.put("smtp.host", envHost);
            String envPort = System.getenv("SMTP_PORT"); if (envPort != null) smtpProps.put("smtp.port", envPort);
            String envUser = System.getenv("SMTP_USERNAME"); if (envUser != null) smtpProps.put("smtp.username", envUser);
            String envPass = System.getenv("SMTP_PASSWORD"); if (envPass != null) smtpProps.put("smtp.password", envPass);
            String envFrom = System.getenv("SMTP_FROM"); if (envFrom != null) smtpProps.put("smtp.from", envFrom);
            smtpProps.put("smtp.auth", System.getenv().getOrDefault("SMTP_AUTH", "true"));
            smtpProps.put("smtp.starttls.enable", System.getenv().getOrDefault("SMTP_STARTTLS", "true"));
            smtpProps.put("app.reset.url.prefix", System.getenv().getOrDefault("APP_RESET_URL_PREFIX", ""));
            if (isNonEmpty(smtpProps.getProperty("smtp.from"))) {
                configured = true;
                return;
            }
        }
        try (InputStream in = getClass().getResourceAsStream("/config.properties")) {
            if (in == null) return;
            smtpProps.load(in);
            // Vérifier qu'on a au moins host et from
            if (isNonEmpty(smtpProps.getProperty("smtp.host")) && isNonEmpty(smtpProps.getProperty("smtp.from"))) {
                configured = true;
            }
        } catch (Exception e) {
            System.err.println("[CLEAN] Impossible de charger config.properties pour EmailService: " + e.getMessage());
        }
    }

    public boolean isConfigured() {
        return configured;
    }

    public boolean sendResetEmail(String to, String token) {
        String prefix = smtpProps.getProperty("app.reset.url.prefix", "");
        String link = prefix.isEmpty() ? token : prefix + token;
        String subject = "Reinitialisation de votre mot de passe";
        String body = "Bonjour,\n\n" +
                "Vous avez demande la reinitialisation de votre mot de passe.\n" +
                "Lien ou token: " + link + "\n\n" +
                "Si vous n'etes pas a l'origine de cette demande, ignorez ce message.";
        return sendEmail(to, subject, body);
    }

    public boolean sendWelcomeEmail(String to, String fullName) {
        String subject = "Bienvenue sur GreenWallet";
        String body = "Bonjour " + safeName(fullName) + ",\n\n" +
                "Votre compte a ete cree avec succes.\n" +
                "Un administrateur va verifier votre compte si necessaire.\n\n" +
                "Merci et a bientot.";
        return sendEmail(to, subject, body);
    }

    public boolean sendAccountStatusEmail(String to, String fullName, String status) {
        String subject = "Mise a jour de votre compte";
        String body = "Bonjour " + safeName(fullName) + ",\n\n" +
                "Votre compte a ete " + status + ".\n\n" +
                "Pour toute question, contactez le support.";
        return sendEmail(to, subject, body);
    }

    private boolean sendEmail(String to, String subject, String body) {
        if (!configured) {
            System.out.println("[CLEAN] SMTP non configure, email simule a: " + to);
            System.out.println("[CLEAN] Sujet: " + subject);
            System.out.println("[CLEAN] Corps: " + body);
            return false;
        }
        try {
            Session session = createSession();
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(smtpProps.getProperty("smtp.from")));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(body);
            Transport.send(message);
            return true;
        } catch (MessagingException e) {
            System.err.println("[CLEAN] Echec envoi email: " + e.getMessage());
            return false;
        }
    }

    private Session createSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpProps.getProperty("smtp.host"));
        props.put("mail.smtp.port", smtpProps.getProperty("smtp.port", "587"));
        props.put("mail.smtp.auth", smtpProps.getProperty("smtp.auth", "true"));
        props.put("mail.smtp.starttls.enable", smtpProps.getProperty("smtp.starttls.enable", "true"));
        String username = smtpProps.getProperty("smtp.username", "");
        String password = smtpProps.getProperty("smtp.password", "");
        boolean authEnabled = Boolean.parseBoolean(props.getProperty("mail.smtp.auth"));
        if (authEnabled && !username.isEmpty()) {
            return Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
        }
        return Session.getInstance(props);
    }

    private String safeName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "";
        return fullName.trim();
    }

    private boolean isNonEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
