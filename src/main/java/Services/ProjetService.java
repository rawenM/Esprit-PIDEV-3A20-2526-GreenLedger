package Services;

import DataBase.MyConnection;
import Models.Projet;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProjetService {

    Connection conn = MyConnection.getConnection();

    public List<Projet> afficher() {
        List<Projet> list = new ArrayList<>();
        String sql = "SELECT p.id, p.titre, p.description, p.budget, p.score_esg, p.statut, " +
                "CASE WHEN EXISTS (SELECT 1 FROM evaluation e WHERE e.id_projet = p.id) " +
                "THEN 'Evaluée' ELSE 'En attente' END AS statut_evaluation " +
                "FROM projet p";
        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                Projet p = new Projet();
                p.setId(rs.getInt("id"));
                p.setTitre(rs.getString("titre"));
                p.setDescription(rs.getString("description"));
                p.setBudget(rs.getDouble("budget"));
                double score = rs.getObject("score_esg") == null ? 0.0 : rs.getDouble("score_esg");
                p.setScoreEsg(score);
                String statut = rs.getString("statut_evaluation");
                p.setStatutEvaluation(statut == null || statut.trim().isEmpty() ? "En attente" : statut);
                list.add(p);
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return list;
    }

    public int ajouter(Projet projet) {
        String sql = "INSERT INTO projet (titre, description, budget, statut, score_esg) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, projet.getTitre());
            ps.setString(2, projet.getDescription());
            ps.setDouble(3, projet.getBudget());
            ps.setString(4, "En attente");
            ps.setObject(5, null);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return -1;
    }

    public void modifier(Projet projet) {
        String sql = "UPDATE projet SET titre = ?, description = ?, budget = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, projet.getTitre());
            ps.setString(2, projet.getDescription());
            ps.setDouble(3, projet.getBudget());
            ps.setInt(4, projet.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public void supprimer(int id) {
        String sql = "DELETE FROM projet WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }
}
