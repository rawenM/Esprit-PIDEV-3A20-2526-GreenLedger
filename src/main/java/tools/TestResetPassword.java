package tools;

import Utils.UnifiedEmailService;

/**
 * Test de l'envoi d'email de réinitialisation de mot de passe
 */
public class TestResetPassword {

    public static void main(String[] args) {
        System.out.println("=== Test Email Reinitialisation ===\n");

        // Définir les variables d'environnement
        System.setProperty("GMAIL_API_ENABLED", "true");
        System.setProperty("GMAIL_FROM_EMAIL", "ibrahimimajid058@gmail.com");
        System.setProperty("GMAIL_FROM_NAME", "GreenLedger Team");

        UnifiedEmailService emailService = new UnifiedEmailService();

        if (!emailService.isConfigured()) {
            System.out.println("X Service email non configure");
            return;
        }

        System.out.println("OK Service email configure\n");

        // Test d'envoi d'email de réinitialisation
        System.out.println("Envoi d'email de reinitialisation a: ibrahimimajid058@gmail.com");
        
        String testToken = "test-token-123456789";
        boolean success = emailService.sendResetPasswordEmail(
            "ibrahimimajid058@gmail.com",
            "Ibrahim Imajid",
            testToken
        );

        if (success) {
            System.out.println("\nOK Email envoye avec succes !");
            System.out.println("Verifiez votre boite email: ibrahimimajid058@gmail.com");
            System.out.println("Le lien contient le token: " + testToken);
        } else {
            System.out.println("\nX Echec de l'envoi");
        }
    }
}
