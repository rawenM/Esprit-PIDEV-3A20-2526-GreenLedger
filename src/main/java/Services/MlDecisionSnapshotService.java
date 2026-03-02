package Services;

import DataBase.MyConnection;
import Models.MlDecisionSnapshot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class MlDecisionSnapshotService {

    public void insert(MlDecisionSnapshot snapshot) {
        if (snapshot == null) return;

        String sql = "INSERT INTO ml_decision_snapshots(" +
                "project_id, evaluation_id, project_name, decision, confidence, score, compliance, min_note, esg_score, " +
                "factors, explanation, recommendations, created_by_user_id" +
                ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (snapshot.getProjectId() == null) ps.setNull(1, Types.INTEGER);
            else ps.setInt(1, snapshot.getProjectId());

            if (snapshot.getEvaluationId() == null) ps.setNull(2, Types.INTEGER);
            else ps.setInt(2, snapshot.getEvaluationId());

            ps.setString(3, snapshot.getProjectName());
            ps.setString(4, snapshot.getDecision());

            if (snapshot.getConfidence() == null) ps.setNull(5, Types.DECIMAL);
            else ps.setDouble(5, snapshot.getConfidence());

            if (snapshot.getScore() == null) ps.setNull(6, Types.DECIMAL);
            else ps.setDouble(6, snapshot.getScore());

            if (snapshot.getCompliance() == null) ps.setNull(7, Types.DECIMAL);
            else ps.setDouble(7, snapshot.getCompliance());

            if (snapshot.getMinNote() == null) ps.setNull(8, Types.INTEGER);
            else ps.setInt(8, snapshot.getMinNote());

            if (snapshot.getEsgScore() == null) ps.setNull(9, Types.INTEGER);
            else ps.setInt(9, snapshot.getEsgScore());

            ps.setString(10, snapshot.getFactors());
            ps.setString(11, snapshot.getExplanation());
            ps.setString(12, snapshot.getRecommendations());

            if (snapshot.getCreatedByUserId() == null) ps.setNull(13, Types.BIGINT);
            else ps.setLong(13, snapshot.getCreatedByUserId());

            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("[ML] snapshot insert failed: " + ex.getMessage());
        }
    }
}

