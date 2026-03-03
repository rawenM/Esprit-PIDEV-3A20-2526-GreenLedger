package tools;

import Models.FraudDetectionResult;
import Models.User;
import Models.TypeUtilisateur;
import Services.FraudDetectionService;

/**
 * Test de debug pour la détection de fraude
 */
public class TestFraudDetectionDebug {
    
    public static void main(String[] args) {
        System.out.println("=== TEST DE DETECTION DE FRAUDE - DEBUG ===\n");
        
        FraudDetectionService fraudService = new FraudDetectionService();
        
        // Test 1: Utilisateur normal
        System.out.println("TEST 1: Utilisateur normal (Jean Dupont)");
        System.out.println("----------------------------------------");
        User user1 = new User();
        user1.setId(1L);
        user1.setNom("Dupont");
        user1.setPrenom("Jean");
        user1.setEmail("jean.dupont@gmail.com");
        user1.setTelephone("0612345678");
        user1.setAdresse("123 Rue de la Paix, Paris");
        user1.setTypeUtilisateur(TypeUtilisateur.INVESTISSEUR);
        
        System.out.println("Données:");
        System.out.println("  Nom: " + user1.getNom());
        System.out.println("  Prénom: " + user1.getPrenom());
        System.out.println("  Email: " + user1.getEmail());
        System.out.println("  Téléphone: " + user1.getTelephone());
        System.out.println("  Adresse: " + user1.getAdresse());
        System.out.println();
        
        FraudDetectionResult result1 = fraudService.analyzeRegistration(user1);
        printResult(result1);
        
        System.out.println("\n" + "=".repeat(60) + "\n");
        
        // Test 2: Utilisateur suspect (az az)
        System.out.println("TEST 2: Utilisateur suspect (az az)");
        System.out.println("----------------------------------------");
        User user2 = new User();
        user2.setId(2L);
        user2.setNom("az");
        user2.setPrenom("az");
        user2.setEmail("az@gmail.com");
        user2.setTelephone("0612345678");
        user2.setAdresse("123 Rue Test");
        user2.setTypeUtilisateur(TypeUtilisateur.INVESTISSEUR);
        
        System.out.println("Données:");
        System.out.println("  Nom: " + user2.getNom());
        System.out.println("  Prénom: " + user2.getPrenom());
        System.out.println("  Email: " + user2.getEmail());
        System.out.println("  Téléphone: " + user2.getTelephone());
        System.out.println("  Adresse: " + user2.getAdresse());
        System.out.println();
        
        FraudDetectionResult result2 = fraudService.analyzeRegistration(user2);
        printResult(result2);
        
        System.out.println("\n" + "=".repeat(60) + "\n");
        
        // Test 3: Utilisateur très suspect
        System.out.println("TEST 3: Utilisateur très suspect (Test Fake)");
        System.out.println("----------------------------------------");
        User user3 = new User();
        user3.setId(3L);
        user3.setNom("Test");
        user3.setPrenom("Fake");
        user3.setEmail("test@tempmail.com");
        user3.setTelephone("1111111111");
        user3.setAdresse("test");
        user3.setTypeUtilisateur(TypeUtilisateur.INVESTISSEUR);
        
        System.out.println("Données:");
        System.out.println("  Nom: " + user3.getNom());
        System.out.println("  Prénom: " + user3.getPrenom());
        System.out.println("  Email: " + user3.getEmail());
        System.out.println("  Téléphone: " + user3.getTelephone());
        System.out.println("  Adresse: " + user3.getAdresse());
        System.out.println();
        
        FraudDetectionResult result3 = fraudService.analyzeRegistration(user3);
        printResult(result3);
        
        System.out.println("\n=== FIN DES TESTS ===");
    }
    
    private static void printResult(FraudDetectionResult result) {
        System.out.println("RÉSULTAT:");
        System.out.println("  Score: " + String.format("%.1f", result.getRiskScore()) + "/100");
        System.out.println("  Niveau: " + result.getRiskLevel().getLabel());
        System.out.println("  Frauduleux: " + (result.isFraudulent() ? "OUI" : "NON"));
        System.out.println("  Recommandation: " + result.getRecommendation());
        System.out.println();
        System.out.println("INDICATEURS DÉTECTÉS:");
        
        long detectedCount = result.getIndicators().stream()
                .filter(FraudDetectionResult.FraudIndicator::isDetected)
                .count();
        
        if (detectedCount == 0) {
            System.out.println("  ✅ Aucun indicateur de fraude détecté");
        } else {
            for (FraudDetectionResult.FraudIndicator indicator : result.getIndicators()) {
                if (indicator.isDetected()) {
                    System.out.println("  ⚠️  " + indicator.getType() + ": " + indicator.getDescription() + 
                                     " (Poids: " + (indicator.getWeight() * 100) + "%)");
                }
            }
        }
    }
}
