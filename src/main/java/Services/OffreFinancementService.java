package Services;

import DataBase.MyConnection;
import Models.OffreFinancement;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OffreFinancementService {

    public OffreFinancementService() {
    }

    public List<OffreFinancement> getAll() {
        return afficherAll();
    }

    public List<OffreFinancement> afficherAll() {
        List<OffreFinancement> list = new ArrayList<>();
        String sql = "SELECT id_offre, type_offre, taux, duree, id_financement FROM offre_financement";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OffreFinancement o = new OffreFinancement(
                            rs.getInt("id_offre"),
                            rs.getString("type_offre"),
                            rs.getDouble("taux"),
                            rs.getInt("duree"),
                            rs.getInt("id_financement")
                    );
                    list.add(o);
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }

    public void add(OffreFinancement o) {
        String sql = "INSERT INTO offre_financement (type_offre, taux, duree, id_financement) VALUES (?,?,?,?)";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, o.getTypeOffre());
            ps.setDouble(2, o.getTaux());
            ps.setInt(3, o.getDuree());
            ps.setInt(4, o.getIdFinancement());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void update(OffreFinancement o) {
        String sql = "UPDATE offre_financement SET type_offre=?, taux=?, duree=?, id_financement=? WHERE id_offre=?";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, o.getTypeOffre());
            ps.setDouble(2, o.getTaux());
            ps.setInt(3, o.getDuree());
            ps.setInt(4, o.getIdFinancement());
            ps.setInt(5, o.getIdOffre());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete(int idOffre) {
        String sql = "DELETE FROM offre_financement WHERE id_offre=?";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idOffre);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
