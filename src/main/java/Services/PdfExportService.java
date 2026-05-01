package Services;

import DataBase.MyConnection;
import Models.PdfExportLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

public class PdfExportService {

    private static final String SELECT_BASE =
            "SELECT id, evaluation_id, project_id, provider, output_path, status, error_message, created_by_user_id, created_at " +
            "FROM pdf_exports ";

    public void insert(PdfExportLog log) {
        if (log == null) return;

        String sql = "INSERT INTO pdf_exports(" +
                "evaluation_id, project_id, provider, output_path, status, error_message, created_by_user_id" +
                ") VALUES (?,?,?,?,?,?,?)";

        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (log.getEvaluationId() == null) ps.setNull(1, Types.INTEGER);
            else ps.setInt(1, log.getEvaluationId());

            if (log.getProjectId() == null) ps.setNull(2, Types.INTEGER);
            else ps.setInt(2, log.getProjectId());

            ps.setString(3, log.getProvider());
            ps.setString(4, log.getOutputPath());
            ps.setString(5, log.getStatus());
            ps.setString(6, log.getErrorMessage());

            if (log.getCreatedByUserId() == null) ps.setNull(7, Types.BIGINT);
            else ps.setLong(7, log.getCreatedByUserId());

            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("[PDF] insert failed: " + ex.getMessage());
        }
    }

    public PdfExportLog findLatestByProject(int projectId) {
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
            System.err.println("[PDF] findLatestByProject failed: " + ex.getMessage());
        }
        return null;
    }

    public PdfExportLog findLatestByEvaluation(int evaluationId) {
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
            System.err.println("[PDF] findLatestByEvaluation failed: " + ex.getMessage());
        }
        return null;
    }

    private PdfExportLog mapRow(ResultSet rs) throws SQLException {
        PdfExportLog log = new PdfExportLog();

        long id = rs.getLong("id");
        log.setId(rs.wasNull() ? null : id);

        int evalId = rs.getInt("evaluation_id");
        log.setEvaluationId(rs.wasNull() ? null : evalId);

        int projectId = rs.getInt("project_id");
        log.setProjectId(rs.wasNull() ? null : projectId);

        log.setProvider(rs.getString("provider"));
        log.setOutputPath(rs.getString("output_path"));
        log.setStatus(rs.getString("status"));
        log.setErrorMessage(rs.getString("error_message"));

        long createdBy = rs.getLong("created_by_user_id");
        log.setCreatedByUserId(rs.wasNull() ? null : createdBy);

        Timestamp createdAt = rs.getTimestamp("created_at");
        log.setCreatedAt(createdAt);
        return log;
    }
}

