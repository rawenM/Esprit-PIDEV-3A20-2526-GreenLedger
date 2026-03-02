package Services;

import DataBase.MyConnection;
import Models.FinancementOffre;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FinancementOffreService {
    private String cachedOffreTable;

    public FinancementOffreService() {
    }

    public List<FinancementOffre> getAll() {
        List<FinancementOffre> list = new ArrayList<>();
        String offreTable = resolveOffreTable();
        String sql = "SELECT f.id AS financement_id, f.projet_id, f.banque_id, f.montant, f.date_financement, "
                + "o.id_offre, o.type_offre, o.taux, o.duree "
                + "FROM financement f "
                + "LEFT JOIN " + offreTable + " o ON o.id_financement = f.id "
                + "ORDER BY f.id";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                FinancementOffre fo = new FinancementOffre();
                fo.setFinancementId(getNullableInt(rs, "financement_id"));
                fo.setProjetId(getNullableInt(rs, "projet_id"));
                fo.setBanqueId(getNullableInt(rs, "banque_id"));
                fo.setMontant(getNullableDouble(rs, "montant"));
                fo.setDateFinancement(rs.getString("date_financement"));
                fo.setOffreId(getNullableInt(rs, "id_offre"));
                fo.setTypeOffre(rs.getString("type_offre"));
                fo.setTaux(getNullableDouble(rs, "taux"));
                fo.setDuree(getNullableInt(rs, "duree"));
                list.add(fo);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void add(FinancementOffre data) {
        String finSql = "INSERT INTO financement (projet_id, banque_id, montant, date_financement) VALUES (?,?,?,?)";
        String offreTable = resolveOffreTable();
        String offSql = "INSERT INTO " + offreTable + " (type_offre, taux, duree, id_financement) VALUES (?,?,?,?)";
        try (Connection conn = MyConnection.getConnection()) {
            conn.setAutoCommit(false);
            int financementId;
            try (PreparedStatement ps = conn.prepareStatement(finSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, safeInt(data.getProjetId()));
                ps.setInt(2, safeInt(data.getBanqueId()));
                ps.setDouble(3, safeDouble(data.getMontant()));
                ps.setString(4, data.getDateFinancement());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("No financement id returned");
                    }
                    financementId = keys.getInt(1);
                }
            }
            if (hasOffreData(data)) {
                try (PreparedStatement ps = conn.prepareStatement(offSql)) {
                    ps.setString(1, data.getTypeOffre());
                    ps.setDouble(2, safeDouble(data.getTaux()));
                    ps.setInt(3, safeInt(data.getDuree()));
                    ps.setInt(4, financementId);
                    ps.executeUpdate();
                }
            }
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void update(FinancementOffre data) {
        String finSql = "UPDATE financement SET projet_id=?, banque_id=?, montant=?, date_financement=? WHERE id=?";
        String offreTable = resolveOffreTable();
        String offUpdateSql = "UPDATE " + offreTable + " SET type_offre=?, taux=?, duree=? WHERE id_offre=?";
        String offInsertSql = "INSERT INTO " + offreTable + " (type_offre, taux, duree, id_financement) VALUES (?,?,?,?)";
        try (Connection conn = MyConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(finSql)) {
                ps.setInt(1, safeInt(data.getProjetId()));
                ps.setInt(2, safeInt(data.getBanqueId()));
                ps.setDouble(3, safeDouble(data.getMontant()));
                ps.setString(4, data.getDateFinancement());
                ps.setInt(5, safeInt(data.getFinancementId()));
                ps.executeUpdate();
            }
            if (hasOffreData(data)) {
                if (data.getOffreId() != null && data.getOffreId() > 0) {
                    try (PreparedStatement ps = conn.prepareStatement(offUpdateSql)) {
                        ps.setString(1, data.getTypeOffre());
                        ps.setDouble(2, safeDouble(data.getTaux()));
                        ps.setInt(3, safeInt(data.getDuree()));
                        ps.setInt(4, data.getOffreId());
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = conn.prepareStatement(offInsertSql)) {
                        ps.setString(1, data.getTypeOffre());
                        ps.setDouble(2, safeDouble(data.getTaux()));
                        ps.setInt(3, safeInt(data.getDuree()));
                        ps.setInt(4, safeInt(data.getFinancementId()));
                        ps.executeUpdate();
                    }
                }
            }
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete(FinancementOffre data) {
        String offreTable = resolveOffreTable();
        String offDeleteById = "DELETE FROM " + offreTable + " WHERE id_offre=?";
        String offDeleteByFin = "DELETE FROM " + offreTable + " WHERE id_financement=?";
        String finDelete = "DELETE FROM financement WHERE id=?";
        try (Connection conn = MyConnection.getConnection()) {
            conn.setAutoCommit(false);
            if (data.getOffreId() != null && data.getOffreId() > 0) {
                try (PreparedStatement ps = conn.prepareStatement(offDeleteById)) {
                    ps.setInt(1, data.getOffreId());
                    ps.executeUpdate();
                }
            } else if (data.getFinancementId() != null && data.getFinancementId() > 0) {
                try (PreparedStatement ps = conn.prepareStatement(offDeleteByFin)) {
                    ps.setInt(1, data.getFinancementId());
                    ps.executeUpdate();
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(finDelete)) {
                ps.setInt(1, safeInt(data.getFinancementId()));
                ps.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Integer getNullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private Double getNullableDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private boolean hasOffreData(FinancementOffre data) {
        return data.getTypeOffre() != null && !data.getTypeOffre().trim().isEmpty();
    }

    private String resolveOffreTable() {
        if (cachedOffreTable != null) {
            return cachedOffreTable;
        }
        try (Connection conn = MyConnection.getConnection()) {
            cachedOffreTable = tableExists(conn, "offre_financement") ? "offre_financement" : "offres";
        } catch (SQLException e) {
            cachedOffreTable = "offre_financement";
        }
        return cachedOffreTable;
    }

    private boolean tableExists(Connection conn, String tableName) {
        try (ResultSet rs = conn.getMetaData().getTables(null, null, tableName, null)) {
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }
}
