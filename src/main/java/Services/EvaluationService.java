package Services;

import DataBase.MyConnection;
import Models.Evaluation;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EvaluationService {

    private String lastErrorMessage;

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    private void setLastError(SQLException ex, String context) {
        String state = ex.getSQLState();
        int code = ex.getErrorCode();
        this.lastErrorMessage = context + " | SQLState=" + state + " | Code=" + code + " | " + ex.getMessage();
    }

    public void ajouter(Evaluation e) {
        String sql = "INSERT INTO evaluation(observations_globales, score_final, est_valide, id_projet) VALUES (?,?,?,?)";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, e.getObservations());
            ps.setDouble(2, e.getScoreGlobal());
            ps.setBoolean(3, decisionToFlag(e.getDecision()));
            ps.setInt(4, e.getIdProjet());
            ps.executeUpdate();
            lastErrorMessage = null;
        } catch (SQLException ex) {
            setLastError(ex, "ajouter evaluation");
            System.out.println(ex.getMessage());
        }
    }

    public List<Evaluation> afficher() {
        List<Evaluation> list = new ArrayList<>();
        String sql = "SELECT e.*, p.titre AS titre_projet FROM evaluation e " +
                "LEFT JOIN projet p ON p.id = e.id_projet " +
                "ORDER BY e.date_evaluation DESC";
        try (Connection conn = MyConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Evaluation e = new Evaluation();
                e.setIdEvaluation(rs.getInt("id_evaluation"));
                e.setDateEvaluation(rs.getTimestamp("date_evaluation"));
                e.setObservations(rs.getString("observations_globales"));
                e.setScoreGlobal(rs.getDouble("score_final"));
                e.setDecision(flagToDecision(rs.getBoolean("est_valide")));
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
        String sqlCritere = "DELETE FROM evaluation_resultat WHERE id_evaluation=?";
        String sqlEvaluation = "DELETE FROM evaluation WHERE id_evaluation=?";
        try (Connection conn = MyConnection.getConnection()) {
            try (PreparedStatement psCritere = conn.prepareStatement(sqlCritere)) {
                psCritere.setInt(1, id);
                psCritere.executeUpdate();
            }
            try (PreparedStatement psEval = conn.prepareStatement(sqlEvaluation)) {
                psEval.setInt(1, id);
                psEval.executeUpdate();
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public void modifier(Evaluation e) {
        String sql = "UPDATE evaluation SET observations_globales=?, score_final=?, est_valide=?, id_projet=? WHERE id_evaluation=?";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, e.getObservations());
            ps.setDouble(2, e.getScoreGlobal());
            ps.setBoolean(3, decisionToFlag(e.getDecision()));
            ps.setInt(4, e.getIdProjet());
            ps.setInt(5, e.getIdEvaluation());
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public List<Evaluation> afficherParEntreprise(int entrepriseId) {
        List<Evaluation> list = new ArrayList<>();
        String sql = "SELECT e.*, p.titre AS titre_projet " +
                "FROM evaluation e " +
                "JOIN projet p ON p.id = e.id_projet " +
                "WHERE p.entreprise_id = ? " +
                "ORDER BY e.date_evaluation DESC";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entrepriseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Evaluation e = new Evaluation();
                    e.setIdEvaluation(rs.getInt("id_evaluation"));
                    e.setDateEvaluation(rs.getTimestamp("date_evaluation"));
                    e.setObservations(rs.getString("observations_globales"));
                    e.setScoreGlobal(rs.getDouble("score_final"));
                    e.setDecision(flagToDecision(rs.getBoolean("est_valide")));
                    e.setIdProjet(rs.getInt("id_projet"));
                    e.setTitreProjet(rs.getString("titre_projet"));
                    list.add(e);
                }
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return list;
    }

    public List<Evaluation> afficherParProjet(int projetId) {
        List<Evaluation> list = new ArrayList<>();
        String sql = "SELECT e.*, p.titre AS titre_projet " +
                "FROM evaluation e " +
                "JOIN projet p ON p.id = e.id_projet " +
                "WHERE e.id_projet = ? " +
                "ORDER BY e.date_evaluation DESC";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projetId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Evaluation e = new Evaluation();
                    e.setIdEvaluation(rs.getInt("id_evaluation"));
                    e.setDateEvaluation(rs.getTimestamp("date_evaluation"));
                    e.setObservations(rs.getString("observations_globales"));
                    e.setScoreGlobal(rs.getDouble("score_final"));
                    e.setDecision(flagToDecision(rs.getBoolean("est_valide")));
                    e.setIdProjet(rs.getInt("id_projet"));
                    e.setTitreProjet(rs.getString("titre_projet"));
                    list.add(e);
                }
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return list;
    }

    public int ajouterAvecCriteres(Evaluation e, java.util.List<Models.EvaluationResult> criteres) {
        String sqlEval = "INSERT INTO evaluation(observations_globales, score_final, est_valide, id_projet) VALUES (?,?,?,?)";
        String sqlResult = "INSERT INTO evaluation_resultat(id_evaluation, id_critere, est_respecte, note, commentaire_expert) VALUES (?,?,?,?,?)";
        try (Connection conn = MyConnection.getConnection()) {
            boolean previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            int evaluationId;
            try (PreparedStatement psEval = conn.prepareStatement(sqlEval, Statement.RETURN_GENERATED_KEYS)) {
                psEval.setString(1, e.getObservations());
                psEval.setDouble(2, e.getScoreGlobal());
                psEval.setBoolean(3, decisionToFlag(e.getDecision()));
                psEval.setInt(4, e.getIdProjet());
                psEval.executeUpdate();
                try (ResultSet rs = psEval.getGeneratedKeys()) {
                    if (!rs.next()) {
                        conn.rollback();
                        lastErrorMessage = "ajouter evaluation: aucune clé générée";
                        return -1;
                    }
                    evaluationId = rs.getInt(1);
                }
            }

            try (PreparedStatement psRes = conn.prepareStatement(sqlResult)) {
                for (Models.EvaluationResult c : criteres) {
                    psRes.setInt(1, evaluationId);
                    psRes.setInt(2, c.getIdCritere());
                    psRes.setBoolean(3, c.isEstRespecte());
                    psRes.setInt(4, c.getNote());
                    psRes.setString(5, c.getCommentaireExpert());
                    psRes.addBatch();
                }
                psRes.executeBatch();
            }

            conn.commit();
            conn.setAutoCommit(previousAutoCommit);
            lastErrorMessage = null;
            return evaluationId;
        } catch (SQLException ex) {
            setLastError(ex, "ajouter evaluation + criteres");
            System.out.println(ex.getMessage());
            return -1;
        }
    }

    public java.util.Set<Integer> getProjetIdsWithEvaluations() {
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        String sql = "SELECT DISTINCT id_projet FROM evaluation";
        try (Connection conn = MyConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                ids.add(rs.getInt("id_projet"));
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return ids;
    }

    /**
     * Advanced AI analysis for evaluation results.
     * This stub can be extended with real AI logic (e.g., Weka, Smile).
     * Returns a suggestion or prediction based on criteria notes.
     */
    public String analyseEvaluationAI(List<Models.EvaluationResult> criteres) {
        // Example: flag if any note is unusually low
        for (Models.EvaluationResult critere : criteres) {
            if (critere.getNote() <= 2) {
                return "Attention: Un critère a une note très basse. Vérifiez les détails.";
            }
        }
        // Example: suggest improvement if average is below threshold
        double avg = criteres.stream().mapToInt(Models.EvaluationResult::getNote).average().orElse(0);
        if (avg < 5) {
            return "Suggestion: La moyenne des notes est faible. Revoir les critères ou le projet.";
        }
        // Advanced: detect outliers and trends
        double stddev = Math.sqrt(criteres.stream().mapToDouble(c -> Math.pow(c.getNote() - avg, 2)).average().orElse(0));
        if (stddev > 3) {
            return "Attention: Les notes sont très dispersées. Analysez les critères individuellement.";
        }
        // Advanced: recommend based on historical data (stub)
        // In a real implementation, fetch past evaluations and compare
        // For now, just return a generic message
        return "Evaluation conforme. Aucun problème détecté.";
    }

    private boolean decisionToFlag(String decision) {
        if (decision == null) {
            return false;
        }
        String value = decision.trim().toLowerCase();
        return value.contains("approuve") || value.contains("accepte") || value.contains("approve") || value.contains("accept");
    }

    private String flagToDecision(boolean valid) {
        return valid ? "Approuve" : "Rejete";
    }
}
