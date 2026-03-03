package tools;

import Utils.CaptchaService;

/**
 * Test de configuration de Google reCAPTCHA
 * Vérifie que les clés sont correctement chargées
 */
public class TestRecaptchaConfig {
    
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                                ║");
        System.out.println("║         TEST DE CONFIGURATION GOOGLE reCAPTCHA                ║");
        System.out.println("║                                                                ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        System.out.println();
        
        try {
            // Créer une instance du service
            CaptchaService captchaService = new CaptchaService();
            
            // Vérifier si configuré
            boolean isConfigured = captchaService.isConfigured();
            
            System.out.println("TEST 1: Vérification de la configuration");
            System.out.println("─────────────────────────────────────────────────────────────");
            
            if (isConfigured) {
                System.out.println("✅ reCAPTCHA est CONFIGURÉ");
                System.out.println();
                
                // Afficher les informations (masquées pour sécurité)
                String siteKey = captchaService.getSiteKey();
                String secretKey = captchaService.getSecretKey();
                
                System.out.println("TEST 2: Vérification des clés");
                System.out.println("─────────────────────────────────────────────────────────────");
                
                if (siteKey != null && !siteKey.isEmpty()) {
                    System.out.println("✅ Site Key chargée:");
                    System.out.println("   " + maskKey(siteKey));
                } else {
                    System.out.println("❌ Site Key manquante ou vide");
                }
                
                System.out.println();
                
                if (secretKey != null && !secretKey.isEmpty()) {
                    System.out.println("✅ Secret Key chargée:");
                    System.out.println("   " + maskKey(secretKey));
                } else {
                    System.out.println("❌ Secret Key manquante ou vide");
                }
                
                System.out.println();
                System.out.println("TEST 3: Test de vérification de token");
                System.out.println("─────────────────────────────────────────────────────────────");
                System.out.println("⚠️  Pour tester la vérification complète:");
                System.out.println("   1. Lancer l'application (run.bat)");
                System.out.println("   2. Aller sur la page de connexion");
                System.out.println("   3. Résoudre le reCAPTCHA");
                System.out.println("   4. Vérifier les logs de la console");
                
            } else {
                System.out.println("❌ reCAPTCHA n'est PAS configuré");
                System.out.println();
                System.out.println("SOLUTION:");
                System.out.println("─────────────────────────────────────────────────────────────");
                System.out.println("1. Obtenir les clés sur: https://www.google.com/recaptcha/admin");
                System.out.println();
                System.out.println("2. Créer/modifier: src/main/resources/config.properties");
                System.out.println();
                System.out.println("   RECAPTCHA_SITE_KEY=votre_site_key");
                System.out.println("   RECAPTCHA_SECRET_KEY=votre_secret_key");
                System.out.println("   RECAPTCHA_VERIFY_URL=https://www.google.com/recaptcha/api/siteverify");
                System.out.println();
                System.out.println("3. Recompiler: mvn clean compile");
                System.out.println();
                System.out.println("4. Relancer ce test");
            }
            
            System.out.println();
            System.out.println("═══════════════════════════════════════════════════════════════");
            System.out.println();
            
            if (isConfigured) {
                System.out.println("✅ RÉSULTAT: Configuration reCAPTCHA OK");
                System.out.println();
                System.out.println("PROCHAINES ÉTAPES:");
                System.out.println("1. Lancer l'application: run.bat");
                System.out.println("2. Tester la connexion avec reCAPTCHA");
                System.out.println("3. Vérifier les logs pour le token");
            } else {
                System.out.println("❌ RÉSULTAT: Configuration reCAPTCHA MANQUANTE");
                System.out.println();
                System.out.println("Suivez le guide: GUIDE_ACTIVATION_RECAPTCHA.md");
            }
            
            System.out.println();
            System.out.println("═══════════════════════════════════════════════════════════════");
            
        } catch (Exception e) {
            System.err.println("❌ ERREUR lors du test:");
            System.err.println("   " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Masque une clé pour la sécurité (affiche seulement début et fin)
     */
    private static String maskKey(String key) {
        if (key == null || key.length() < 10) {
            return "***";
        }
        
        int visibleChars = 6;
        String start = key.substring(0, visibleChars);
        String end = key.substring(key.length() - visibleChars);
        int maskedLength = key.length() - (visibleChars * 2);
        
        StringBuilder masked = new StringBuilder();
        for (int i = 0; i < maskedLength; i++) {
            masked.append("*");
        }
        
        return start + masked.toString() + end;
    }
}
