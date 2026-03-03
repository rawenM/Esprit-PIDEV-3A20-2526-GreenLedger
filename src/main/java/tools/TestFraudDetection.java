package tools;

import Models.FraudDetectionResult;
import Models.TypeUtilisateur;
import Models.User;
import Services.FraudDetectionService;

/**
 * Test de la détection de fraude
 */
public class TestFraudDetection {
    
    public static void main(String[] args) {
        System.out.println("=== Test Détection de Fraude avec IA ===\n");
        
        FraudDetectionService fraudService = new FraudDetectionService();
        
        // Test 1: Utilisateur légitime
        System.out.println("--- Test 1: Utilisateur Légitime ---");
        User legitimateUser = createUser(
            "Jean", "Dupont", "jean.dupont@gmail.com", 
            "+33612345678", "123 Rue de la Paix, Paris"
        );
        testUser(fraudService, legitimateUser);
        
        // Test 2: Email jetable
        System.out.println("\n--- Test 2: Email Jetable ---");
        User disposableEmailUser = createUser(
            "Marie", "Martin", "test@tempmail.com",
            "+33698765432", "456 Avenue des Champs, Lyon"
        );
        testUser(fraudService, disposableEmailUser);
        
        // Test 3: Nom suspect
        System.out.println("\n--- Test 3: Nom Suspect ---");
        User suspiciousNameUser = createUser(
            "Test", "Fake", "user@example.com",
            "+33687654321", "789 Boulevard Test, Marseille"
        );
        testUser(fraudService, suspiciousNameUser);
        
        // Test 4: Téléphone invalide
        System.out.println("\n--- Test 4: Téléphone Invalide ---");
        User invalidPhoneUser = createUser(
            "Pierre", "Durand", "pierre.durand@yahoo.fr",
            "1111111111", "321 Rue du Commerce, Toulouse"
        );
        testUser(fraudService, invalidPhoneUser);
        
        // Test 5: Données incohérentes
        System.out.println("\n--- Test 5: Données Incohérentes ---");
        User inconsistentUser = createUser(
            "Alice", "Bernard", "xyz123@gmail.com",
            "+33676543210", "test"
        );
        testUser(fraudService, inconsistentUser);
        
        // Test 6: Tentative d'admin
        System.out.println("\n--- Test 6: Nom Admin ---");
        User adminAttemptUser = createUser(
            "Admin", "Root", "admin@system.com",
            "+33665432109", "1 Rue Admin, Paris"
        );
        testUser(fraudService, adminAttemptUser);
        
        // Test 7: Combinaison de plusieurs indicateurs
        System.out.println("\n--- Test 7: Multiples Indicateurs ---");
        User multipleIndicatorsUser = createUser(
            "Fake", "Test", "test@guerrillamail.com",
            "0000000000", "fake address"
        );
        testUser(fraudService, multipleIndicatorsUser);
        
        System.out.println("\n=== Tests Terminés ===");
    }
    
    private static User createUser(String nom, String prenom, String email, String telephone, String adresse) {
        User user = new User();
        user.setId(1L); // ID fictif pour le test
        user.setNom(nom);
        user.setPrenom(prenom);
        user.setEmail(email);
        user.setTelephone(telephone);
        user.setAdresse(adresse);
        user.setTypeUtilisateur(TypeUtilisateur.PORTEUR_PROJET);
        return user;
    }
    
    private static void testUser(FraudDetectionService fraudService, User user) {
        System.out.println("Utilisateur: " + user.getNomComplet());
        System.out.println("Email: " + user.getEmail());
        System.out.println("Téléphone: " + user.getTelephone());
        System.out.println();
        
        FraudDetectionResult result = fraudService.analyzeRegistration(user);
        
        System.out.println("Résultat:");
        System.out.println("  Score de risque: " + String.format("%.1f", result.getRiskScore()) + "/100");
        System.out.println("  Niveau: " + result.getRiskLevel().getLabel());
        System.out.println("  Frauduleux: " + (result.isFraudulent() ? "OUI" : "NON"));
        System.out.println("  Recommandation: " + result.getRecommendation());
        System.out.println();
        
        System.out.println("Indicateurs détectés:");
        for (FraudDetectionResult.FraudIndicator indicator : result.getIndicators()) {
            if (indicator.isDetected()) {
                System.out.println("  ⚠️  " + indicator.getType() + ": " + indicator.getDescription());
            }
        }
        
        if (result.getIndicators().stream().noneMatch(FraudDetectionResult.FraudIndicator::isDetected)) {
            System.out.println("  ✅ Aucun indicateur de fraude détecté");
        }
    }
}
