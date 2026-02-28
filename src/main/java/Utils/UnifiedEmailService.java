package Utils;

/**
 * Service unifié d'envoi d'emails avec fallback automatique
 * Utilise Gmail API en priorité, puis SMTP si Gmail API n'est pas disponible
 */
public class UnifiedEmailService {

    private final GmailApiService gmailService;
    private final EmailService smtpService;
    private final boolean useGmailApi;

    public UnifiedEmailService() {
        this.gmailService = new GmailApiService();
        this.smtpService = new EmailService();
        this.useGmailApi = gmailService.isConfigured();
        
        if (useGmailApi) {
            System.out.println("[UnifiedEmail] Utilisation de Gmail API pour les emails");
        } else if (smtpService.isConfigured()) {
            System.out.println("[UnifiedEmail] Utilisation de SMTP pour les emails (fallback)");
        } else {
            System.out.println("[UnifiedEmail] Aucun service d'email configuré");
        }
    }

    public boolean isConfigured() {
        return gmailService.isConfigured() || smtpService.isConfigured();
    }

    /**
     * Envoie un email de bienvenue
     */
    public boolean sendWelcomeEmail(String toEmail, String fullName) {
        if (useGmailApi) {
            return gmailService.sendWelcomeEmail(toEmail, fullName);
        }
        return smtpService.sendWelcomeEmail(toEmail, fullName);
    }

    /**
     * Envoie un email de vérification
     */
    public boolean sendVerificationEmail(String toEmail, String fullName, String verificationToken) {
        if (useGmailApi) {
            return gmailService.sendVerificationEmail(toEmail, fullName, verificationToken);
        }
        // Fallback SMTP (utilise le même format que reset)
        return smtpService.sendResetEmail(toEmail, verificationToken);
    }

    /**
     * Envoie un email de réinitialisation de mot de passe
     */
    public boolean sendResetPasswordEmail(String toEmail, String fullName, String resetToken) {
        if (useGmailApi) {
            return gmailService.sendResetPasswordEmail(toEmail, fullName, resetToken);
        }
        return smtpService.sendResetEmail(toEmail, resetToken);
    }

    /**
     * Envoie un email de compte approuvé
     */
    public boolean sendAccountApprovedEmail(String toEmail, String fullName) {
        if (useGmailApi) {
            return gmailService.sendAccountApprovedEmail(toEmail, fullName);
        }
        return smtpService.sendAccountStatusEmail(toEmail, fullName, "VALIDE");
    }

    /**
     * Envoie un email de compte rejeté
     */
    public boolean sendAccountRejectedEmail(String toEmail, String fullName, String reason) {
        if (useGmailApi) {
            return gmailService.sendAccountRejectedEmail(toEmail, fullName, reason);
        }
        return smtpService.sendAccountStatusEmail(toEmail, fullName, "REFUSE");
    }

    /**
     * Envoie un email de compte bloqué
     */
    public boolean sendAccountBlockedEmail(String toEmail, String fullName, String reason) {
        if (useGmailApi) {
            return gmailService.sendAccountBlockedEmail(toEmail, fullName, reason);
        }
        return smtpService.sendAccountStatusEmail(toEmail, fullName, "BLOQUE");
    }

    /**
     * Envoie un email de compte débloqué
     */
    public boolean sendAccountUnblockedEmail(String toEmail, String fullName) {
        if (useGmailApi) {
            return gmailService.sendAccountUnblockedEmail(toEmail, fullName);
        }
        return smtpService.sendAccountStatusEmail(toEmail, fullName, "DEBLOQUE");
    }

    /**
     * Envoie un email de changement de statut (méthode générique)
     */
    public boolean sendAccountStatusEmail(String toEmail, String fullName, String status) {
        if (status == null) {
            return false;
        }

        String normalized = status.trim().toLowerCase();
        
        // Router vers la méthode appropriée selon le statut
        switch (normalized) {
            case "valide":
            case "approuve":
            case "active":
                return sendAccountApprovedEmail(toEmail, fullName);
                
            case "refuse":
            case "rejete":
                return sendAccountRejectedEmail(toEmail, fullName, "Votre demande n'a pas été approuvée");
                
            case "bloque":
            case "blocked":
                return sendAccountBlockedEmail(toEmail, fullName, "Votre compte a été bloqué");
                
            case "debloque":
            case "unblocked":
                return sendAccountUnblockedEmail(toEmail, fullName);
                
            default:
                // Fallback générique
                if (useGmailApi) {
                    // Gmail API n'a pas de méthode générique, utiliser SMTP
                    return smtpService.sendAccountStatusEmail(toEmail, fullName, status);
                }
                return smtpService.sendAccountStatusEmail(toEmail, fullName, status);
        }
    }
}
