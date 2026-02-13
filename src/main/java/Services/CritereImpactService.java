package Services;

import DataBase.MyConnection;
import Models.CritereImpact;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CritereImpactService {

    Connection conn = MyConnection.getConnection();

    public void ajouter(CritereImpact c) {
        String sql = "INSERT INTO critere_impact(nom, note, commentaire_technique, id_evaluation) VALUES (?,?,?,?)";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, c.getNom());
            ps.setInt(2, c.getNote());
            ps.setString(3, c.getCommentaireTechnique());
            ps.setInt(4, c.getIdEvaluation());
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public void modifier(CritereImpact c) {
        String sql = "UPDATE critere_impact SET nom=?, note=?, commentaire_technique=? WHERE id_critere=?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, c.getNom());
            ps.setInt(2, c.getNote());
            ps.setString(3, c.getCommentaireTechnique());
            ps.setInt(4, c.getIdCritere());
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public void supprimer(int idCritere) {
        String sql = "DELETE FROM critere_impact WHERE id_critere=?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idCritere);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public List<CritereImpact> afficherParEvaluation(int idEvaluation) {
        List<CritereImpact> list = new ArrayList<>();
        String sql = "SELECT id_critere, nom, note, commentaire_technique, id_evaluation FROM critere_impact WHERE id_evaluation=?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idEvaluation);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                CritereImpact c = new CritereImpact();
                c.setIdCritere(rs.getInt("id_critere"));
                c.setNom(rs.getString("nom"));
                c.setNote(rs.getInt("note"));
                c.setCommentaireTechnique(rs.getString("commentaire_technique"));
                c.setIdEvaluation(rs.getInt("id_evaluation"));
                list.add(c);
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return list;
    }
}

