package Services;

import DataBase.MyConnection;
import Models.Evaluation;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EvaluationService {

    Connection conn = MyConnection.getConnection();

    public void ajouter(Evaluation e) {
        String sql = "INSERT INTO evaluation(observations, score_global, decision, id_projet) VALUES (?,?,?,?)";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, e.getObservations());
            ps.setDouble(2, e.getScoreGlobal());
            ps.setString(3, e.getDecision());
            ps.setInt(4, e.getIdProjet());
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public List<Evaluation> afficher() {
        List<Evaluation> list = new ArrayList<>();
        String sql = "SELECT e.*, p.titre AS titre_projet FROM evaluation e " +
                "LEFT JOIN projet p ON p.id = e.id_projet";
        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                Evaluation e = new Evaluation();
                e.setIdEvaluation(rs.getInt("id_evaluation"));
                e.setDateEvaluation(rs.getTimestamp("date_evaluation"));
                e.setObservations(rs.getString("observations"));
                e.setScoreGlobal(rs.getDouble("score_global"));
                e.setDecision(rs.getString("decision"));
                e.setIdProjet(rs.getInt("id_projet"));
                e.setTitreProjet(rs.getString("titre_projet"));
                list.add(e);
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return list;
    }

    public void supprimer(int id) {
        String sqlCritere = "DELETE FROM critere_impact WHERE id_evaluation=?";
        String sqlEvaluation = "DELETE FROM evaluation WHERE id_evaluation=?";
        try {
            PreparedStatement psCritere = conn.prepareStatement(sqlCritere);
            psCritere.setInt(1, id);
            psCritere.executeUpdate();

            PreparedStatement psEval = conn.prepareStatement(sqlEvaluation);
            psEval.setInt(1, id);
            psEval.executeUpdate();
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public void modifier(Evaluation e) {
        String sql = "UPDATE evaluation SET observations=?, score_global=?, decision=?, id_projet=? WHERE id_evaluation=?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, e.getObservations());
            ps.setDouble(2, e.getScoreGlobal());
            ps.setString(3, e.getDecision());
            ps.setInt(4, e.getIdProjet());
            ps.setInt(5, e.getIdEvaluation());
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }
}
