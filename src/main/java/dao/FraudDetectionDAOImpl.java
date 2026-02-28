package dao;

import DataBase.MyConnection;
import Models.FraudDetectionResult;
import Models.FraudDetectionResult.RiskLevel;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implémentation DAO pour la gestion des résultats de détection de fraude
 */
public class FraudDetectionDAOImpl implements IFraudDetectionDAO {
    
    private final Connection connection;
    
    public FraudDetectionDAOImpl() {
        this.connection = MyConnection.getInstance().getConnection();
    }
    
    @Override
    public FraudDetectionResult save(FraudDetectionResult result) {
        String sql = "INSERT INTO fraud_detection_results (user_id, risk_score, risk_level, is_fraudulent, " +
                    "recommendation, analysis_details, analyzed_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, result.getUserId());
            stmt.setDouble(2, result.getRiskScore());
            stmt.setString(3, result.getRiskLevel().name());
            stmt.setBoolean(4, result.isFraudulent());
            stmt.setString(5, result.getRecommendation());
            stmt.setString(6, result.getAnalysisDetails());
            stmt.setTimestamp(7, Timestamp.valueOf(result.getAnalyzedAt()));
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        result.setId(generatedKeys.getLong(1));
                        System.out.println("[FraudDetectionDAO] Résultat sauvegardé avec ID: " + result.getId());
                        return result;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[FraudDetectionDAO] Erreur lors de la sauvegarde: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    @Override
    public Optional<FraudDetectionResult> findById(Long id) {
        String sql = "SELECT * FROM fraud_detection_results WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[FraudDetectionDAO] Erreur lors de la recherche par ID: " + e.getMessage());
        }
        
        return Optional.empty();
    }
    
    @Override
    public Optional<FraudDetectionResult> findByUserId(Long userId) {
        String sql = "SELECT * FROM fraud_detection_results WHERE user_id = ? ORDER BY analyzed_at DESC LIMIT 1";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[FraudDetectionDAO] Erreur lors de la recherche par user ID: " + e.getMessage());
        }
        
        return Optional.empty();
    }
    
    @Override
    public List<FraudDetectionResult> findAll() {
        List<FraudDetectionResult> results = new ArrayList<>();
        String sql = "SELECT * FROM fraud_detection_results ORDER BY analyzed_at DESC";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                results.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("[FraudDetectionDAO] Erreur lors de la récupération de tous les résultats: " + e.getMessage());
        }
        
        return results;
    }
    
    @Override
    public List<FraudDetectionResult> findByRiskLevel(RiskLevel riskLevel) {
        List<FraudDetectionResult> results = new ArrayList<>();
        String sql = "SELECT * FROM fraud_detection_results WHERE risk_level = ? ORDER BY analyzed_at DESC";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, riskLevel.name());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[FraudDetectionDAO] Erreur lors de la recherche par niveau de risque: " + e.getMessage());
        }
        
        return results;
    }
    
    @Override
    public List<FraudDetectionResult> findFraudulent() {
        List<FraudDetectionResult> results = new ArrayList<>();
        String sql = "SELECT * FROM fraud_detection_results WHERE is_fraudulent = TRUE ORDER BY analyzed_at DESC";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                results.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("[FraudDetectionDAO] Erreur lors de la recherche des frauduleux: " + e.getMessage());
        }
        
        return results;
    }
    
    @Override
    public List<FraudDetectionResult> findRequiringReview() {
        List<FraudDetectionResult> results = new ArrayList<>();
        String sql = "SELECT * FROM fraud_detection_results WHERE recommendation LIKE '%EXAMINER%' ORDER BY analyzed_at DESC";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                results.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("[FraudDetectionDAO] Erreur lors de la recherche des examens requis: " + e.getMessage());
        }
        
        return results;
    }
    
    @Override
    public FraudDetectionResult update(FraudDetectionResult result) {
        String sql = "UPDATE fraud_detection_results SET risk_score = ?, risk_level = ?, " +
                    "is_fraudulent = ?, recommendation = ?, analysis_details = ? WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDouble(1, result.getRiskScore());
            stmt.setString(2, result.getRiskLevel().name());
            stmt.setBoolean(3, result.isFraudulent());
            stmt.setString(4, result.getRecommendation());
            stmt.setString(5, result.getAnalysisDetails());
            stmt.setLong(6, result.getId());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                System.out.println("[FraudDetectionDAO] Résultat mis à jour: " + result.getId());
                return result;
            }
        } catch (SQLException e) {
            System.err.println("[FraudDetectionDAO] Erreur lors de la mise à jour: " + e.getMessage());
        }
        
        return null;
    }
    
    @Override
    public boolean delete(Long id) {
        String sql = "DELETE FROM fraud_detection_results WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                System.out.println("[FraudDetectionDAO] Résultat supprimé: " + id);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[FraudDetectionDAO] Erreur lors de la suppression: " + e.getMessage());
        }
        
        return false;
    }
    
    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM fraud_detection_results";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("[FraudDetectionDAO] Erreur lors du comptage: " + e.getMessage());
        }
        
        return 0;
    }
    
    @Override
    public long countFraudulent() {
        String sql = "SELECT COUNT(*) FROM fraud_detection_results WHERE is_fraudulent = TRUE";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("[FraudDetectionDAO] Erreur lors du comptage des frauduleux: " + e.getMessage());
        }
        
        return 0;
    }
    
    /**
     * Mappe un ResultSet vers un FraudDetectionResult
     */
    private FraudDetectionResult mapResultSet(ResultSet rs) throws SQLException {
        FraudDetectionResult result = new FraudDetectionResult();
        result.setId(rs.getLong("id"));
        result.setUserId(rs.getLong("user_id"));
        result.setRiskScore(rs.getDouble("risk_score"));
        result.setRiskLevel(RiskLevel.valueOf(rs.getString("risk_level")));
        result.setFraudulent(rs.getBoolean("is_fraudulent"));
        result.setRecommendation(rs.getString("recommendation"));
        result.setAnalysisDetails(rs.getString("analysis_details"));
        
        Timestamp timestamp = rs.getTimestamp("analyzed_at");
        if (timestamp != null) {
            result.setAnalyzedAt(timestamp.toLocalDateTime());
        }
        
        return result;
    }
}
