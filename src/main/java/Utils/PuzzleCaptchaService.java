package Utils;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;

/**
 * Service de génération de CAPTCHA puzzle (slider)
 * L'utilisateur doit glisser une pièce pour compléter l'image
 */
public class PuzzleCaptchaService {
    
    private static final int IMAGE_WIDTH = 300;
    private static final int IMAGE_HEIGHT = 150;
    private static final int PUZZLE_SIZE = 50;
    private static final int TOLERANCE = 5; // Tolérance en pixels
    
    private final Random random = new SecureRandom();
    
    /**
     * Résultat de génération du CAPTCHA puzzle
     */
    public static class PuzzleCaptchaResult {
        private final String backgroundImageBase64;
        private final String puzzlePieceBase64;
        private final int correctPosition;
        private final String sessionId;
        
        public PuzzleCaptchaResult(String backgroundImageBase64, String puzzlePieceBase64, 
                                   int correctPosition, String sessionId) {
            this.backgroundImageBase64 = backgroundImageBase64;
            this.puzzlePieceBase64 = puzzlePieceBase64;
            this.correctPosition = correctPosition;
            this.sessionId = sessionId;
        }
        
        public String getBackgroundImageBase64() { return backgroundImageBase64; }
        public String getPuzzlePieceBase64() { return puzzlePieceBase64; }
        public int getCorrectPosition() { return correctPosition; }
        public String getSessionId() { return sessionId; }
    }
    
    /**
     * Génère un nouveau CAPTCHA puzzle
     */
    public PuzzleCaptchaResult generatePuzzle() {
        try {
            // 1. Créer une image de fond avec un motif
            BufferedImage backgroundImage = createBackgroundImage();
            
            // 2. Choisir une position aléatoire pour le puzzle
            int puzzleX = PUZZLE_SIZE + random.nextInt(IMAGE_WIDTH - PUZZLE_SIZE * 2);
            int puzzleY = (IMAGE_HEIGHT - PUZZLE_SIZE) / 2;
            
            // 3. Créer la forme du puzzle
            Shape puzzleShape = createPuzzleShape(puzzleX, puzzleY);
            
            // 4. Extraire la pièce du puzzle
            BufferedImage puzzlePiece = extractPuzzlePiece(backgroundImage, puzzleShape, puzzleX, puzzleY);
            
            // 5. Créer l'image de fond avec le trou
            BufferedImage backgroundWithHole = createBackgroundWithHole(backgroundImage, puzzleShape);
            
            // 6. Convertir en Base64
            String backgroundBase64 = imageToBase64(backgroundWithHole);
            String puzzleBase64 = imageToBase64(puzzlePiece);
            
            // 7. Générer un ID de session
            String sessionId = generateSessionId();
            
            return new PuzzleCaptchaResult(backgroundBase64, puzzleBase64, puzzleX, sessionId);
            
        } catch (Exception e) {
            System.err.println("[PuzzleCaptcha] Erreur lors de la génération: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Vérifie si la position du puzzle est correcte
     */
    public boolean verifyPosition(int userPosition, int correctPosition) {
        int difference = Math.abs(userPosition - correctPosition);
        boolean isValid = difference <= TOLERANCE;
        
        System.out.println("[PuzzleCaptcha] Vérification:");
        System.out.println("  Position utilisateur: " + userPosition);
        System.out.println("  Position correcte: " + correctPosition);
        System.out.println("  Différence: " + difference + " pixels");
        System.out.println("  Résultat: " + (isValid ? "VALIDE" : "INVALIDE"));
        
        return isValid;
    }
    
    /**
     * Crée une image de fond avec un motif coloré
     */
    private BufferedImage createBackgroundImage() {
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        
        // Activer l'antialiasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Créer un dégradé de fond
        GradientPaint gradient = new GradientPaint(
            0, 0, new Color(100, 150, 200),
            IMAGE_WIDTH, IMAGE_HEIGHT, new Color(150, 200, 250)
        );
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        
        // Ajouter des formes décoratives
        g2d.setColor(new Color(255, 255, 255, 50));
        for (int i = 0; i < 20; i++) {
            int x = random.nextInt(IMAGE_WIDTH);
            int y = random.nextInt(IMAGE_HEIGHT);
            int size = 10 + random.nextInt(30);
            g2d.fillOval(x, y, size, size);
        }
        
        // Ajouter des lignes
        g2d.setColor(new Color(255, 255, 255, 30));
        g2d.setStroke(new BasicStroke(2));
        for (int i = 0; i < 10; i++) {
            int x1 = random.nextInt(IMAGE_WIDTH);
            int y1 = random.nextInt(IMAGE_HEIGHT);
            int x2 = random.nextInt(IMAGE_WIDTH);
            int y2 = random.nextInt(IMAGE_HEIGHT);
            g2d.drawLine(x1, y1, x2, y2);
        }
        
        g2d.dispose();
        return image;
    }
    
    /**
     * Crée la forme du puzzle (pièce avec encoches)
     */
    private Shape createPuzzleShape(int x, int y) {
        Area puzzleArea = new Area(new RoundRectangle2D.Double(x, y, PUZZLE_SIZE, PUZZLE_SIZE, 10, 10));
        
        // Ajouter une encoche en haut
        int notchSize = 15;
        Area topNotch = new Area(new RoundRectangle2D.Double(
            x + PUZZLE_SIZE / 2 - notchSize / 2, 
            y - notchSize / 2, 
            notchSize, 
            notchSize, 
            notchSize, 
            notchSize
        ));
        puzzleArea.add(topNotch);
        
        // Ajouter une encoche à droite
        Area rightNotch = new Area(new RoundRectangle2D.Double(
            x + PUZZLE_SIZE - notchSize / 2, 
            y + PUZZLE_SIZE / 2 - notchSize / 2, 
            notchSize, 
            notchSize, 
            notchSize, 
            notchSize
        ));
        puzzleArea.add(rightNotch);
        
        return puzzleArea;
    }
    
    /**
     * Extrait la pièce du puzzle de l'image
     */
    private BufferedImage extractPuzzlePiece(BufferedImage source, Shape shape, int x, int y) {
        BufferedImage piece = new BufferedImage(PUZZLE_SIZE + 20, PUZZLE_SIZE + 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = piece.createGraphics();
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Translater pour centrer la pièce
        g2d.translate(-x + 10, -y + 10);
        
        // Clipper avec la forme du puzzle
        g2d.setClip(shape);
        
        // Dessiner l'image source
        g2d.drawImage(source, 0, 0, null);
        
        // Ajouter une bordure
        g2d.setClip(null);
        g2d.translate(x - 10, y - 10);
        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.setStroke(new BasicStroke(2));
        g2d.draw(shape);
        
        g2d.dispose();
        return piece;
    }
    
    /**
     * Crée l'image de fond avec le trou du puzzle
     */
    private BufferedImage createBackgroundWithHole(BufferedImage source, Shape shape) {
        BufferedImage result = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = result.createGraphics();
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Dessiner l'image source
        g2d.drawImage(source, 0, 0, null);
        
        // Assombrir la zone du puzzle
        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.fill(shape);
        
        // Ajouter une bordure au trou
        g2d.setColor(new Color(255, 255, 255, 150));
        g2d.setStroke(new BasicStroke(2));
        g2d.draw(shape);
        
        g2d.dispose();
        return result;
    }
    
    /**
     * Convertit une image en Base64
     */
    private String imageToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        byte[] bytes = baos.toByteArray();
        return Base64.getEncoder().encodeToString(bytes);
    }
    
    /**
     * Génère un ID de session unique
     */
    private String generateSessionId() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    
    /**
     * Convertit une image Base64 en JavaFX Image
     */
    public static Image base64ToImage(String base64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            BufferedImage bufferedImage = ImageIO.read(bais);
            return SwingFXUtils.toFXImage(bufferedImage, null);
        } catch (Exception e) {
            System.err.println("[PuzzleCaptcha] Erreur conversion Base64 -> Image: " + e.getMessage());
            return null;
        }
    }
}
