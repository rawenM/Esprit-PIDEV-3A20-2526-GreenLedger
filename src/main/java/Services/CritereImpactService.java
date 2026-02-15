package Services;

import DataBase.MyConnection;
import Models.CritereReference;
import Models.EvaluationResult;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CritereImpactService {

    Connection conn = MyConnection.getConnection();

    public void ensureDefaultReferences() {
        // Intentionally no-op: criteria are entered manually by experts.
    }

    private boolean hasAnyReferences() {
        String countSql = "SELECT COUNT(*) FROM critere_reference";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(countSql)) {
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
            return false;
        }
    }

    private void seedDefaults() {
        String insertSql = "INSERT INTO critere_reference(nom_critere, description, poids) VALUES (?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            // Seed a small standard set if table is empty.
            addDefault(ps, "Pollution Air", "Impact sur la qualite de l'air", 1);
            addDefault(ps, "Pollution Eau", "Impact sur les ressources hydriques", 1);
            addDefault(ps, "Biodiversite", "Impact sur les ecosystemes locaux", 1);
            addDefault(ps, "Dechets", "Gestion et reduction des dechets", 1);
            addDefault(ps, "Energie", "Efficacite et consommation energetique", 1);
            ps.executeBatch();
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private void addDefault(PreparedStatement ps, String nom, String description, int poids) throws SQLException {
        ps.setString(1, nom);
        ps.setString(2, description);
        ps.setInt(3, poids);
        ps.addBatch();
    }

    public List<CritereReference> afficherReferences() {
        List<CritereReference> list = new ArrayList<>();
        String sql = "SELECT id_critere, nom_critere, description, poids FROM critere_reference ORDER BY id_critere";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                CritereReference c = new CritereReference();
                c.setIdCritere(rs.getInt("id_critere"));
                c.setNomCritere(rs.getString("nom_critere"));
                c.setDescription(rs.getString("description"));
                c.setPoids(rs.getInt("poids"));
                list.add(c);
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return list;
    }

    public void ajouterReference(CritereReference c) {
        String sql = "INSERT INTO critere_reference(nom_critere, description, poids) VALUES (?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getNomCritere());
            ps.setString(2, c.getDescription());
            ps.setInt(3, c.getPoids());
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public void modifierReference(CritereReference c) {
        String sql = "UPDATE critere_reference SET nom_critere=?, description=?, poids=? WHERE id_critere=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getNomCritere());
            ps.setString(2, c.getDescription());
            ps.setInt(3, c.getPoids());
            ps.setInt(4, c.getIdCritere());
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public boolean isReferenceUsed(int idCritere) {
        String sql = "SELECT COUNT(*) FROM evaluation_resultat WHERE id_critere=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCritere);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
            return true;
        }
    }

    public boolean supprimerReference(int idCritere) {
        String sql = "DELETE FROM critere_reference WHERE id_critere=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCritere);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
            return false;
        }
    }

    public List<EvaluationResult> afficherParEvaluation(int idEvaluation) {
        List<EvaluationResult> list = new ArrayList<>();
        String sql = "SELECT r.id_critere, r.nom_critere, er.note, er.commentaire_expert, er.est_respecte " +
                "FROM evaluation_resultat er " +
                "JOIN critere_reference r ON r.id_critere = er.id_critere " +
                "WHERE er.id_evaluation=? " +
                "ORDER BY r.id_critere";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idEvaluation);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    EvaluationResult c = new EvaluationResult();
                    c.setIdCritere(rs.getInt("id_critere"));
                    c.setNomCritere(rs.getString("nom_critere"));
                    c.setNote(rs.getInt("note"));
                    c.setCommentaireExpert(rs.getString("commentaire_expert"));
                    c.setEstRespecte(rs.getBoolean("est_respecte"));
                    list.add(c);
                }
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return list;
    }

    public void ajouterResultats(int idEvaluation, List<EvaluationResult> criteres) {
        String sql = "INSERT INTO evaluation_resultat(id_evaluation, id_critere, est_respecte, note, commentaire_expert) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (EvaluationResult c : criteres) {
                ps.setInt(1, idEvaluation);
                ps.setInt(2, c.getIdCritere());
                ps.setBoolean(3, c.isEstRespecte());
                ps.setInt(4, c.getNote());
                ps.setString(5, c.getCommentaireExpert());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public void modifierResultats(int idEvaluation, List<EvaluationResult> criteres) {
        String deleteSql = "DELETE FROM evaluation_resultat WHERE id_evaluation=?";
        String insertSql = "INSERT INTO evaluation_resultat(id_evaluation, id_critere, est_respecte, note, commentaire_expert) VALUES (?,?,?,?,?)";
        try {
            conn.setAutoCommit(false);
            try (PreparedStatement deletePs = conn.prepareStatement(deleteSql)) {
                deletePs.setInt(1, idEvaluation);
                deletePs.executeUpdate();
            }
            try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                for (EvaluationResult c : criteres) {
                    insertPs.setInt(1, idEvaluation);
                    insertPs.setInt(2, c.getIdCritere());
                    insertPs.setBoolean(3, c.isEstRespecte());
                    insertPs.setInt(4, c.getNote());
                    insertPs.setString(5, c.getCommentaireExpert());
                    insertPs.addBatch();
                }
                insertPs.executeBatch();
            }
            conn.commit();
        } catch (SQLException ex) {
            try {
                conn.rollback();
            } catch (SQLException ignore) {
                // ignore rollback failures
            }
            System.out.println(ex.getMessage());
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ignore) {
                // ignore restore failures
            }
        }
    }
}
