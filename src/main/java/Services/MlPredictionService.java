package Services;

import DataBase.MyConnection;
import Models.MlPrediction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class MlPredictionService {

    public void insert(MlPrediction prediction) {
        if (prediction == null) return;

        String sql = "INSERT INTO ml_predictions(" +
                "evaluation_id, project_id, predicted_esg_score, credibility_score, carbon_risk, decision, recommendations, model_version, created_by_user_id" +
                ") VALUES (?,?,?,?,?,?,?,?,?)";

        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (prediction.getEvaluationId() == null) ps.setNull(1, Types.INTEGER);
            else ps.setInt(1, prediction.getEvaluationId());

            if (prediction.getProjectId() == null) ps.setNull(2, Types.INTEGER);
            else ps.setInt(2, prediction.getProjectId());

            if (prediction.getPredictedEsgScore() == null) ps.setNull(3, Types.INTEGER);
            else ps.setInt(3, prediction.getPredictedEsgScore());

            if (prediction.getCredibilityScore() == null) ps.setNull(4, Types.INTEGER);
            else ps.setInt(4, prediction.getCredibilityScore());

            ps.setString(5, prediction.getCarbonRisk());
            ps.setString(6, prediction.getDecision());
            ps.setString(7, prediction.getRecommendations());
            ps.setString(8, prediction.getModelVersion());

            if (prediction.getCreatedByUserId() == null) ps.setNull(9, Types.BIGINT);
            else ps.setLong(9, prediction.getCreatedByUserId());

            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("[ML] insert failed: " + ex.getMessage());
        }
    }
}

