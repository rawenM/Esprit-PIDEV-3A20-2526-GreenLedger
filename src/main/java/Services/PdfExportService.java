package Services;

import DataBase.MyConnection;
import Models.PdfExportLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class PdfExportService {

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
}

