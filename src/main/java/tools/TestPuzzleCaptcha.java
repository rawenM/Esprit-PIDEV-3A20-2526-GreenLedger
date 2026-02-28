package tools;

import Utils.PuzzleCaptchaService;
import Utils.PuzzleCaptchaService.PuzzleCaptchaResult;

/**
 * Test du service de CAPTCHA puzzle
 */
public class TestPuzzleCaptcha {
    
    public static void main(String[] args) {
        System.out.println("=== TEST DU CAPTCHA PUZZLE ===\n");
        
        PuzzleCaptchaService service = new PuzzleCaptchaService();
        
        // Test 1: Génération du puzzle
        System.out.println("TEST 1: Génération du puzzle");
        System.out.println("----------------------------------------");
        PuzzleCaptchaResult puzzle = service.generatePuzzle();
        
        if (puzzle != null) {
            System.out.println("✓ Puzzle généré avec succès");
            System.out.println("  Position correcte: " + puzzle.getCorrectPosition() + " pixels");
            System.out.println("  Session ID: " + puzzle.getSessionId());
            System.out.println("  Image de fond: " + puzzle.getBackgroundImageBase64().substring(0, 50) + "...");
            System.out.println("  Pièce du puzzle: " + puzzle.getPuzzlePieceBase64().substring(0, 50) + "...");
        } else {
            System.out.println("✗ Échec de la génération");
        }
        
        System.out.println("\n" + "=".repeat(60) + "\n");
        
        // Test 2: Vérification de position correcte
        System.out.println("TEST 2: Vérification de position correcte");
        System.out.println("----------------------------------------");
        int correctPos = puzzle.getCorrectPosition();
        boolean result1 = service.verifyPosition(correctPos, correctPos);
        System.out.println("Résultat: " + (result1 ? "✓ VALIDE" : "✗ INVALIDE"));
        
        System.out.println("\n" + "=".repeat(60) + "\n");
        
        // Test 3: Vérification de position proche (dans la tolérance)
        System.out.println("TEST 3: Vérification de position proche (+3 pixels)");
        System.out.println("----------------------------------------");
        boolean result2 = service.verifyPosition(correctPos + 3, correctPos);
        System.out.println("Résultat: " + (result2 ? "✓ VALIDE" : "✗ INVALIDE"));
        
        System.out.println("\n" + "=".repeat(60) + "\n");
        
        // Test 4: Vérification de position incorrecte
        System.out.println("TEST 4: Vérification de position incorrecte (+20 pixels)");
        System.out.println("----------------------------------------");
        boolean result3 = service.verifyPosition(correctPos + 20, correctPos);
        System.out.println("Résultat: " + (result3 ? "✓ VALIDE" : "✗ INVALIDE"));
        
        System.out.println("\n" + "=".repeat(60) + "\n");
        
        // Test 5: Génération de plusieurs puzzles
        System.out.println("TEST 5: Génération de 5 puzzles différents");
        System.out.println("----------------------------------------");
        for (int i = 1; i <= 5; i++) {
            PuzzleCaptchaResult p = service.generatePuzzle();
            System.out.println("Puzzle " + i + ": Position = " + p.getCorrectPosition() + " pixels");
        }
        
        System.out.println("\n=== FIN DES TESTS ===");
    }
}
