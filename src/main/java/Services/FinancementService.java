package Services;

import DataBase.MyConnection;
import Models.Financement;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FinancementService {
<<<<<<< HEAD
=======
    private final Connection conn = MyConnection.getConnection();
>>>>>>> yassine_antar

    public List<Financement> getAll() {
        return afficherAll();
    }

    public List<Financement> afficherAll() {
        List<Financement> list = new ArrayList<>();
        String sql = "SELECT id, projet_id, banque_id, montant, date_financement FROM financement";
<<<<<<< HEAD
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
=======
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
>>>>>>> yassine_antar
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Financement f = new Financement(
                            rs.getInt("id"),
                            rs.getInt("projet_id"),
                            rs.getInt("banque_id"),
                            rs.getDouble("montant"),
                            rs.getString("date_financement")
                    );
                    list.add(f);
                    //System.out.println(f.getId() + " / " + f.getProjetId() + " / " + f.getBanqueId()
                      //      + " / " + f.getMontant() + " / " + f.getDateFinancement());
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        //System.out.println(list);
        return list;
    }

    public void add(Financement f) {
        String sql = "INSERT INTO financement (projet_id, banque_id, montant, date_financement) VALUES (?,?,?,?)";
<<<<<<< HEAD
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
=======
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
>>>>>>> yassine_antar
            ps.setInt(1, f.getProjetId());
            ps.setInt(2, f.getBanqueId());
            ps.setDouble(3, f.getMontant());
            ps.setString(4, f.getDateFinancement());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void update(Financement f) {
        String sql = "UPDATE financement SET projet_id=?, banque_id=?, montant=?, date_financement=? WHERE id=?";
<<<<<<< HEAD
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
=======
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
>>>>>>> yassine_antar
            ps.setInt(1, f.getProjetId());
            ps.setInt(2, f.getBanqueId());
            ps.setDouble(3, f.getMontant());
            ps.setString(4, f.getDateFinancement());
            ps.setInt(5, f.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM financement WHERE id=?";
<<<<<<< HEAD
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
=======
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
>>>>>>> yassine_antar
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
