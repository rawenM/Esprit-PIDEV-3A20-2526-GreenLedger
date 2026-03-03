package Controllers;

import Utils.PuzzleCaptchaService;
import Utils.PuzzleCaptchaService.PuzzleCaptchaResult;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/**
 * Contrôleur pour le CAPTCHA puzzle
 */
public class PuzzleCaptchaController {
    
    @FXML private Pane captchaContainer;
    @FXML private ImageView backgroundImageView;
    @FXML private ImageView puzzlePieceImageView;
    @FXML private Button refreshButton;
    @FXML private Label statusLabel;
    
    private final PuzzleCaptchaService captchaService = new PuzzleCaptchaService();
    private PuzzleCaptchaResult currentPuzzle;
    
    private double dragStartX;
    private double puzzleStartX;
    private boolean isDragging = false;
    
    private Runnable onSuccessCallback;
    private Runnable onFailureCallback;
    
    @FXML
    public void initialize() {
        setupDragHandlers();
        generateNewPuzzle();
    }
    
    /**
     * Configure les gestionnaires de glisser-déposer
     */
    private void setupDragHandlers() {
        puzzlePieceImageView.setOnMousePressed(this::handleMousePressed);
        puzzlePieceImageView.setOnMouseDragged(this::handleMouseDragged);
        puzzlePieceImageView.setOnMouseReleased(this::handleMouseReleased);
        puzzlePieceImageView.setCursor(Cursor.HAND);
    }
    
    /**
     * Génère un nouveau puzzle
     */
    @FXML
    private void generateNewPuzzle() {
        currentPuzzle = captchaService.generatePuzzle();
        
        if (currentPuzzle != null) {
            // Charger l'image de fond
            Image backgroundImage = PuzzleCaptchaService.base64ToImage(
                currentPuzzle.getBackgroundImageBase64()
            );
            backgroundImageView.setImage(backgroundImage);
            
            // Charger la pièce du puzzle
            Image puzzleImage = PuzzleCaptchaService.base64ToImage(
                currentPuzzle.getPuzzlePieceBase64()
            );
            puzzlePieceImageView.setImage(puzzleImage);
            
            // Positionner la pièce à gauche
            puzzlePieceImageView.setLayoutX(10);
            puzzlePieceImageView.setLayoutY((backgroundImageView.getFitHeight() - puzzlePieceImageView.getFitHeight()) / 2);
            
            // Réinitialiser le statut
            statusLabel.setText("Glissez la pièce pour compléter l'image");
            statusLabel.setStyle("-fx-text-fill: #666;");
            
            System.out.println("[PuzzleCaptcha] Nouveau puzzle généré");
            System.out.println("  Position correcte: " + currentPuzzle.getCorrectPosition());
        } else {
            statusLabel.setText("Erreur lors de la génération du puzzle");
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }
    
    /**
     * Gère le début du glissement
     */
    private void handleMousePressed(MouseEvent event) {
        isDragging = true;
        dragStartX = event.getSceneX();
        puzzleStartX = puzzlePieceImageView.getLayoutX();
        puzzlePieceImageView.setCursor(Cursor.CLOSED_HAND);
    }
    
    /**
     * Gère le glissement
     */
    private void handleMouseDragged(MouseEvent event) {
        if (!isDragging) return;
        
        double deltaX = event.getSceneX() - dragStartX;
        double newX = puzzleStartX + deltaX;
        
        // Limiter le mouvement horizontal
        double minX = 0;
        double maxX = backgroundImageView.getFitWidth() - puzzlePieceImageView.getFitWidth();
        newX = Math.max(minX, Math.min(maxX, newX));
        
        puzzlePieceImageView.setLayoutX(newX);
    }
    
    /**
     * Gère la fin du glissement
     */
    private void handleMouseReleased(MouseEvent event) {
        if (!isDragging) return;
        
        isDragging = false;
        puzzlePieceImageView.setCursor(Cursor.HAND);
        
        // Vérifier la position
        int userPosition = (int) (puzzlePieceImageView.getLayoutX() + puzzlePieceImageView.getFitWidth() / 2);
        boolean isCorrect = captchaService.verifyPosition(userPosition, currentPuzzle.getCorrectPosition());
        
        if (isCorrect) {
            // Succès
            statusLabel.setText("✓ Vérification réussie!");
            statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            
            // Animer la pièce vers la position correcte
            animatePuzzleToCorrectPosition();
            
            // Appeler le callback de succès
            if (onSuccessCallback != null) {
                javafx.application.Platform.runLater(() -> {
                    try {
                        Thread.sleep(500); // Attendre l'animation
                        onSuccessCallback.run();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            }
        } else {
            // Échec
            statusLabel.setText("✗ Position incorrecte. Réessayez.");
            statusLabel.setStyle("-fx-text-fill: red;");
            
            // Réinitialiser la position
            javafx.animation.TranslateTransition transition = new javafx.animation.TranslateTransition(
                javafx.util.Duration.millis(300), 
                puzzlePieceImageView
            );
            transition.setToX(10 - puzzlePieceImageView.getLayoutX());
            transition.play();
            
            transition.setOnFinished(e -> {
                puzzlePieceImageView.setLayoutX(10);
                puzzlePieceImageView.setTranslateX(0);
            });
            
            // Appeler le callback d'échec
            if (onFailureCallback != null) {
                onFailureCallback.run();
            }
        }
    }
    
    /**
     * Anime la pièce vers la position correcte
     */
    private void animatePuzzleToCorrectPosition() {
        double targetX = currentPuzzle.getCorrectPosition() - puzzlePieceImageView.getFitWidth() / 2;
        
        javafx.animation.TranslateTransition transition = new javafx.animation.TranslateTransition(
            javafx.util.Duration.millis(300), 
            puzzlePieceImageView
        );
        transition.setToX(targetX - puzzlePieceImageView.getLayoutX());
        transition.play();
        
        transition.setOnFinished(e -> {
            puzzlePieceImageView.setLayoutX(targetX);
            puzzlePieceImageView.setTranslateX(0);
        });
    }
    
    /**
     * Définit le callback de succès
     */
    public void setOnSuccess(Runnable callback) {
        this.onSuccessCallback = callback;
    }
    
    /**
     * Définit le callback d'échec
     */
    public void setOnFailure(Runnable callback) {
        this.onFailureCallback = callback;
    }
    
    /**
     * Réinitialise le CAPTCHA
     */
    public void reset() {
        generateNewPuzzle();
    }
}
