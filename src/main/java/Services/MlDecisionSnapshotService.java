package Services;

import DataBase.MyConnection;
import Models.MlDecisionSnapshot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

public class MlDecisionSnapshotService {

    private static final String SELECT_BASE =
            "SELECT id, project_id, evaluation_id, project_name, decision, confidence, score, compliance, " +
            "min_note, esg_score, factors, explanation, recommendations, created_by_user_id, created_at " +
            "FROM ml_decision_snapshots ";

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

    public MlDecisionSnapshot findLatestByProject(int projectId) {
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
            System.err.println("[ML] snapshot findLatestByProject failed: " + ex.getMessage());
        }
        return null;
    }

    public MlDecisionSnapshot findLatestByEvaluation(int evaluationId) {
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
            System.err.println("[ML] snapshot findLatestByEvaluation failed: " + ex.getMessage());
        }
        return null;
    }

    private MlDecisionSnapshot mapRow(ResultSet rs) throws SQLException {
        MlDecisionSnapshot s = new MlDecisionSnapshot();

        long id = rs.getLong("id");
        s.setId(rs.wasNull() ? null : id);

        int projectId = rs.getInt("project_id");
        s.setProjectId(rs.wasNull() ? null : projectId);

        int evaluationId = rs.getInt("evaluation_id");
        s.setEvaluationId(rs.wasNull() ? null : evaluationId);

        s.setProjectName(rs.getString("project_name"));
        s.setDecision(rs.getString("decision"));

        double confidence = rs.getDouble("confidence");
        s.setConfidence(rs.wasNull() ? null : confidence);

        double score = rs.getDouble("score");
        s.setScore(rs.wasNull() ? null : score);

        double compliance = rs.getDouble("compliance");
        s.setCompliance(rs.wasNull() ? null : compliance);

        int minNote = rs.getInt("min_note");
        s.setMinNote(rs.wasNull() ? null : minNote);

        int esgScore = rs.getInt("esg_score");
        s.setEsgScore(rs.wasNull() ? null : esgScore);

        s.setFactors(rs.getString("factors"));
        s.setExplanation(rs.getString("explanation"));
        s.setRecommendations(rs.getString("recommendations"));

        long createdBy = rs.getLong("created_by_user_id");
        s.setCreatedByUserId(rs.wasNull() ? null : createdBy);

        Timestamp createdAt = rs.getTimestamp("created_at");
        s.setCreatedAt(createdAt);
        return s;
    }
}

