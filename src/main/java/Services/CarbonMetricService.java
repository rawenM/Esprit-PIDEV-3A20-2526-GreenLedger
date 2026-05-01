package Services;

import DataBase.MyConnection;
import Models.dto.CarbonMetricResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Read service for persisted carbon metrics used by both web and Java flows.
 */
public class CarbonMetricService {

    private static final String SELECT_BASE =
            "SELECT id, project_id, evaluation_id, metric_date, scope1_tco2, scope2_tco2, scope3_tco2, total_tco2, method, data_quality_score, created_at " +
            "FROM carbon_metric ";

    public CarbonMetricResult findLatestByProject(int projectId) {
        String sql = SELECT_BASE + "WHERE project_id=? ORDER BY metric_date DESC, id DESC LIMIT 1";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException ex) {
            System.err.println("[CARBON] findLatestByProject failed: " + ex.getMessage());
        }
        return null;
    }

    public CarbonMetricResult findLatestByEvaluation(int evaluationId) {
        String sql = SELECT_BASE + "WHERE evaluation_id=? ORDER BY metric_date DESC, id DESC LIMIT 1";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, evaluationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException ex) {
            System.err.println("[CARBON] findLatestByEvaluation failed: " + ex.getMessage());
        }
        return null;
    }

    private CarbonMetricResult mapRow(ResultSet rs) throws SQLException {
        CarbonMetricResult result = new CarbonMetricResult();
        result.setScope1Tco2(nullableDouble(rs, "scope1_tco2"));
        result.setScope2Tco2(nullableDouble(rs, "scope2_tco2"));
        result.setScope3Tco2(nullableDouble(rs, "scope3_tco2"));
        result.setTotalTco2(nullableDouble(rs, "total_tco2"));
        result.setMethod(rs.getString("method"));
        result.setDataQualityScore(nullableDouble(rs, "data_quality_score"));
        return result;
    }

    private Double nullableDouble(ResultSet rs, String columnName) throws SQLException {
        double value = rs.getDouble(columnName);
        return rs.wasNull() ? null : value;
    }
}

