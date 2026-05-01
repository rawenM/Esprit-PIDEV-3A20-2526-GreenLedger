package Services;

import DataBase.MyConnection;
import Models.MlPrediction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

public class MlPredictionService {

    private static final String SELECT_BASE =
            "SELECT id, evaluation_id, project_id, predicted_esg_score, credibility_score, " +
            "carbon_risk, decision, recommendations, model_version, created_by_user_id, created_at " +
            "FROM ml_predictions ";

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

    public MlPrediction findLatestByProject(int projectId) {
        String sql = SELECT_BASE + "WHERE project_id=? ORDER BY created_at DESC, id DESC LIMIT 1";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException ex) {
            System.err.println("[ML] findLatestByProject failed: " + ex.getMessage());
        }
        return null;
    }

    public MlPrediction findLatestByEvaluation(int evaluationId) {
        String sql = SELECT_BASE + "WHERE evaluation_id=? ORDER BY created_at DESC, id DESC LIMIT 1";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, evaluationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException ex) {
            System.err.println("[ML] findLatestByEvaluation failed: " + ex.getMessage());
        }
        return null;
    }

    private MlPrediction mapRow(ResultSet rs) throws SQLException {
        MlPrediction p = new MlPrediction();

        long id = rs.getLong("id");
        p.setId(rs.wasNull() ? null : id);

        int evalId = rs.getInt("evaluation_id");
        p.setEvaluationId(rs.wasNull() ? null : evalId);

        int projectId = rs.getInt("project_id");
        p.setProjectId(rs.wasNull() ? null : projectId);

        int esg = rs.getInt("predicted_esg_score");
        p.setPredictedEsgScore(rs.wasNull() ? null : esg);

        int credibility = rs.getInt("credibility_score");
        p.setCredibilityScore(rs.wasNull() ? null : credibility);

        p.setCarbonRisk(rs.getString("carbon_risk"));
        p.setDecision(rs.getString("decision"));
        p.setRecommendations(rs.getString("recommendations"));
        p.setModelVersion(rs.getString("model_version"));

        long createdBy = rs.getLong("created_by_user_id");
        p.setCreatedByUserId(rs.wasNull() ? null : createdBy);

        Timestamp createdAt = rs.getTimestamp("created_at");
        p.setCreatedAt(createdAt);
        return p;
    }
}

