package dao;

import Models.FraudDetectionResult;
import java.util.List;
import java.util.Optional;

/**
 * Interface DAO pour la gestion des résultats de détection de fraude
 */
public interface IFraudDetectionDAO {
    
    /**
     * Sauvegarde un résultat de détection de fraude
     */
    FraudDetectionResult save(FraudDetectionResult result);
    
    /**
     * Récupère un résultat par ID
     */
    Optional<FraudDetectionResult> findById(Long id);
    
    /**
     * Récupère le résultat pour un utilisateur spécifique
     */
    Optional<FraudDetectionResult> findByUserId(Long userId);
    
    /**
     * Récupère tous les résultats
     */
    List<FraudDetectionResult> findAll();
    
    /**
     * Récupère les résultats par niveau de risque
     */
    List<FraudDetectionResult> findByRiskLevel(FraudDetectionResult.RiskLevel riskLevel);
    
    /**
     * Récupère les résultats frauduleux
     */
    List<FraudDetectionResult> findFraudulent();
    
    /**
     * Récupère les résultats nécessitant un examen
     */
    List<FraudDetectionResult> findRequiringReview();
    
    /**
     * Met à jour un résultat
     */
    FraudDetectionResult update(FraudDetectionResult result);
    
    /**
     * Supprime un résultat
     */
    boolean delete(Long id);
    
    /**
     * Compte le nombre total de résultats
     */
    long count();
    
    /**
     * Compte le nombre de résultats frauduleux
     */
    long countFraudulent();
}
