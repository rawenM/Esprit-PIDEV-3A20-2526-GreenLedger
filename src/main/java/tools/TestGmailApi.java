package tools;

import Utils.GmailApiService;

/**
 * Classe de test pour vérifier l'intégration Gmail API
 */
public class TestGmailApi {

    public static void main(String[] args) {
        System.out.println("=== Test Gmail API ===\n");

        GmailApiService gmailService = new GmailApiService();

        // Test 1: Vérifier la configuration
        System.out.println("1. Vérification de la configuration...");
        if (gmailService.isConfigured()) {
            System.out.println("   ✅ Gmail API est configuré et prêt\n");
        } else {
            System.out.println("   X Gmail API n'est pas configure");
            System.out.println("   Vérifiez :");
            System.out.println("   - GMAIL_API_ENABLED=true dans .env");
            System.out.println("   - credentials.json dans src/main/resources/");
            System.out.println("   - Première authentification OAuth2 effectuée\n");
            return;
        }

        // Test 2: Email de bienvenue
        System.out.println("2. Test d'envoi d'email de bienvenue...");
        boolean testWelcome = gmailService.sendWelcomeEmail(
            "test@example.com",
            "Jean Dupont"
        );
        System.out.println("   Resultat: " + (testWelcome ? "OK Succes" : "X Echec") + "\n");

        // Test 3: Email de réinitialisation
        System.out.println("3. Test d'envoi d'email de réinitialisation...");
        boolean testReset = gmailService.sendResetPasswordEmail(
            "test@example.com",
            "Jean Dupont",
            "test-token-123456"
        );
        System.out.println("   Resultat: " + (testReset ? "OK Succes" : "X Echec") + "\n");

        // Test 4: Email de compte approuvé
        System.out.println("4. Test d'envoi d'email de compte approuvé...");
        boolean testApproved = gmailService.sendAccountApprovedEmail(
            "test@example.com",
            "Jean Dupont"
        );
        System.out.println("   Resultat: " + (testApproved ? "OK Succes" : "X Echec") + "\n");

        System.out.println("=== Tests terminés ===");
        System.out.println("\nPour tester avec un vrai email :");
        System.out.println("1. Modifiez 'test@example.com' par votre vraie adresse");
        System.out.println("2. Relancez ce test");
        System.out.println("3. Vérifiez votre boîte de réception (et spam)");
    }
}
