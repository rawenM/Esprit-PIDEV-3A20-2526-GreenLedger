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
            if (validateConfig()) {
                configured = true;
                return;
            }
        }
        try (InputStream in = getClass().getResourceAsStream("/config.properties")) {
            if (in == null) return;
            smtpProps.load(in);
            // Vérifier qu'on a une config valide
            if (validateConfig()) {
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
        String title = "Reinitialisation du mot de passe";
        String actionLabel = prefix.isEmpty() ? "Utiliser le token" : "Reinitialiser mon mot de passe";
        String actionValue = prefix.isEmpty() ? token : link;
        String body = buildTemplate(title,
            "Bonjour " + safeName(to) + ",",
            "Vous avez demande la reinitialisation de votre mot de passe.",
            actionLabel,
            actionValue,
            "Si vous n'etes pas a l'origine de cette demande, ignorez ce message.");
        return sendEmail(to, subject, body, true);
    }

    public boolean sendWelcomeEmail(String to, String fullName) {
        String subject = "Bienvenue sur GreenWallet";
        String body = buildTemplate(
            "Bienvenue sur GreenWallet",
            "Bonjour " + safeName(fullName) + ",",
            "Votre compte a ete cree avec succes.",
            "",
            "",
            "Un administrateur va verifier votre compte si necessaire."
        );
        return sendEmail(to, subject, body, true);
    }

    public boolean sendAccountStatusEmail(String to, String fullName, String status) {
        StatusEmail statusEmail = buildStatusEmail(fullName, status);
        return sendEmail(to, statusEmail.subject, statusEmail.htmlBody, true);
    }

    private boolean sendEmail(String to, String subject, String body, boolean isHtml) {
        if (!configured) {
            System.out.println("[CLEAN] SMTP non configure, email simule a: " + to);
            System.out.println("[CLEAN] Sujet: " + subject);
            System.out.println("[CLEAN] Corps: " + body);
            return false;
        }
        try {
            Session session = createSession();
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(smtpProps.getProperty("smtp.from")));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject, "UTF-8");
            if (isHtml) {
                message.setContent(body, "text/html; charset=UTF-8");
            } else {
                message.setText(body);
            }
            Transport.send(message);
            return true;
        } catch (MessagingException e) {
            System.err.println("[CLEAN] Echec envoi email: " + e.getMessage());
            return false;
        }
    }

    private StatusEmail buildStatusEmail(String fullName, String status) {
        String normalized = status == null ? "" : status.trim().toLowerCase();
        String subject = "Mise a jour de votre compte";
        String title = "Mise a jour de votre compte";
        String message = "Votre compte a ete mis a jour.";
        String tone = "info";

        if (normalized.contains("valide") || normalized.contains("approuve") || normalized.contains("active")) {
            subject = "Votre compte est approuve";
            title = "Compte approuve";
            message = "Votre compte est maintenant approuve. Vous pouvez vous connecter.";
            tone = "success";
        } else if (normalized.contains("refuse") || normalized.contains("rejete")) {
            subject = "Votre compte a ete refuse";
            title = "Compte refuse";
            message = "Votre compte a ete refuse. Vous pouvez contacter le support pour plus d'informations.";
            tone = "error";
        } else if (normalized.contains("bloque") || normalized.contains("blocked")) {
            subject = "Votre compte a ete bloque";
            title = "Compte bloque";
            message = "Votre compte a ete bloque. Contactez le support pour le debloquer.";
            tone = "error";
        } else if (normalized.contains("debloque") || normalized.contains("unblocked")) {
            subject = "Votre compte a ete debloque";
            title = "Compte debloque";
            message = "Votre compte a ete debloque. Vous pouvez vous reconnecter.";
            tone = "success";
        } else if (normalized.contains("suspendu") || normalized.contains("suspended")) {
            subject = "Votre compte est suspendu";
            title = "Compte suspendu";
            message = "Votre compte est temporairement suspendu.";
            tone = "warning";
        } else if (normalized.contains("supprime") || normalized.contains("deleted")) {
            subject = "Votre compte a ete supprime";
            title = "Compte supprime";
            message = "Votre compte a ete supprime. Si c'est une erreur, contactez le support.";
            tone = "error";
        }

        String html = buildStatusTemplate("Bonjour " + safeName(fullName) + ",", title, message, tone);
        return new StatusEmail(subject, html);
    }

    private String buildTemplate(String title, String greeting, String message, String actionLabel, String actionValue, String footer) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!doctype html><html><head><meta charset='UTF-8'/>");
        sb.append("<meta name='viewport' content='width=device-width, initial-scale=1'/>");
        sb.append("</head><body style='margin:0; padding:0; background:#F5F7FB; font-family:Arial, sans-serif;'>");
        sb.append("<div style='max-width:600px; margin:0 auto; padding:24px;'>");
        sb.append("<div style='background:#ffffff; border-radius:12px; padding:24px; box-shadow:0 8px 30px rgba(0,0,0,0.06);'>");
        sb.append("<div style='font-size:18px; font-weight:700; color:#0F172A; margin-bottom:12px;'>").append(title).append("</div>");
        sb.append("<div style='font-size:14px; color:#1F2937; margin-bottom:16px;'>").append(greeting).append("</div>");
        sb.append("<div style='font-size:14px; color:#374151; line-height:1.6;'>").append(message).append("</div>");
        if (isNonEmpty(actionValue)) {
            sb.append("<div style='margin:20px 0; padding:12px 16px; background:#F1F5F9; border-radius:8px; font-size:13px; color:#0F172A;'>");
            sb.append("<strong>").append(actionLabel).append(":</strong><br/>").append(actionValue);
            sb.append("</div>");
        }
        if (isNonEmpty(footer)) {
            sb.append("<div style='font-size:12px; color:#6B7280; margin-top:12px;'>").append(footer).append("</div>");
        }
        sb.append("</div>");
        sb.append("<div style='text-align:center; font-size:11px; color:#94A3B8; margin-top:16px;'>GreenLedger</div>");
        sb.append("</div></body></html>");
        return sb.toString();
    }

    private String buildStatusTemplate(String greeting, String title, String message, String tone) {
        String color = "#2563EB";
        if ("success".equals(tone)) color = "#10B981";
        if ("warning".equals(tone)) color = "#F59E0B";
        if ("error".equals(tone)) color = "#EF4444";

        StringBuilder sb = new StringBuilder();
        sb.append("<!doctype html><html><head><meta charset='UTF-8'/>");
        sb.append("<meta name='viewport' content='width=device-width, initial-scale=1'/>");
        sb.append("</head><body style='margin:0; padding:0; background:#F5F7FB; font-family:Arial, sans-serif;'>");
        sb.append("<div style='max-width:600px; margin:0 auto; padding:24px;'>");
        sb.append("<div style='background:#ffffff; border-radius:12px; padding:24px; box-shadow:0 8px 30px rgba(0,0,0,0.06);'>");
        sb.append("<div style='font-size:14px; color:#1F2937; margin-bottom:10px;'>").append(greeting).append("</div>");
        sb.append("<div style='font-size:18px; font-weight:700; color:").append(color).append("; margin-bottom:12px;'>").append(title).append("</div>");
        sb.append("<div style='font-size:14px; color:#374151; line-height:1.6;'>").append(message).append("</div>");
        sb.append("<div style='font-size:12px; color:#6B7280; margin-top:16px;'>Pour toute question, contactez le support.</div>");
        sb.append("</div>");
        sb.append("<div style='text-align:center; font-size:11px; color:#94A3B8; margin-top:16px;'>GreenLedger</div>");
        sb.append("</div></body></html>");
        return sb.toString();
    }

    private boolean validateConfig() {
        String host = smtpProps.getProperty("smtp.host");
        String from = smtpProps.getProperty("smtp.from");
        if (!isConfiguredValue(host) || !isConfiguredValue(from) || !looksLikeEmail(from)) {
            return false;
        }
        boolean authEnabled = Boolean.parseBoolean(smtpProps.getProperty("smtp.auth", "true"));
        if (authEnabled) {
            String user = smtpProps.getProperty("smtp.username");
            String pass = smtpProps.getProperty("smtp.password");
            if (!isConfiguredValue(user) || !isConfiguredValue(pass)) {
                return false;
            }
        }
        return true;
    }

    private boolean looksLikeEmail(String from) {
        if (from == null) return false;
        return from.contains("@");
    }

    private boolean isConfiguredValue(String value) {
        if (!isNonEmpty(value)) return false;
        String upper = value.toUpperCase();
        return !upper.contains("REPLACE_ME") && !upper.contains("YOUR_") && !upper.contains("CHANGE_ME");
    }

    private static class StatusEmail {
        private final String subject;
        private final String htmlBody;

        private StatusEmail(String subject, String htmlBody) {
            this.subject = subject;
            this.htmlBody = htmlBody;
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
