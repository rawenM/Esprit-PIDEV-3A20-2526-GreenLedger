package Services;

import DataBase.MyConnection;
import Models.Budget;
import Models.Projet;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProjetService {

    private final Connection cnx;

    public ProjetService() {
        this.cnx = MyConnection.getConnection();
    }

    public List<Projet> afficher() {
        try (Connection cnx = MyConnection.getConnection()) {
            String sql = "SELECT p.id, p.entreprise_id, p.titre, p.description, p.statut, p.score_esg, " +
                    "       p.company_address, p.company_email, p.company_phone, " +
                    "       b.id_budget, b.montant, b.raison, b.devise " +
                    "FROM projet p " +
                    "LEFT JOIN budget b ON b.id_projet = p.id " +
                    "ORDER BY p.date_creation DESC";

            List<Projet> list = new ArrayList<>();

            try (PreparedStatement ps = cnx.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    list.add(mapProjetWithBudget(rs));
                }

            } catch (SQLException e) {
                System.out.println("Erreur afficher projets: " + e.getMessage());
            }

            return list;
        } catch (SQLException e) {
            System.out.println("Erreur afficher projets (connexion): " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Projet> getByEntreprise(int entrepriseId) {
        try (Connection cnx = MyConnection.getConnection()) {
            String sql = "SELECT p.id, p.entreprise_id, p.titre, p.description, p.statut, p.score_esg, " +
                    "       p.company_address, p.company_email, p.company_phone, " +
                    "       b.id_budget, b.montant, b.raison, b.devise " +
                    "FROM projet p " +
                    "LEFT JOIN budget b ON b.id_projet = p.id " +
                    "WHERE p.entreprise_id=? " +
                    "ORDER BY p.date_creation DESC";

            List<Projet> list = new ArrayList<>();

            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setInt(1, entrepriseId);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapProjetWithBudget(rs));
                    }
                }

            } catch (SQLException e) {
                System.out.println("Erreur getByEntreprise: " + e.getMessage());
            }

            return list;
        } catch (SQLException e) {
            System.out.println("Erreur getByEntreprise (connexion): " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Projet> getByEntreprise(Integer entrepriseId) {
        if (entrepriseId == null) return new ArrayList<>();
        return getByEntreprise(entrepriseId.intValue());
    }

    public Projet getById(int idProjet) {
        String sql = "SELECT p.id, p.entreprise_id, p.titre, p.description, p.statut, p.score_esg, " +
                "       p.company_address, p.company_email, p.company_phone, " +
                "       b.id_budget, b.montant, b.raison, b.devise " +
                "FROM projet p " +
                "LEFT JOIN budget b ON b.id_projet = p.id " +
                "WHERE p.id=?";

        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProjet);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapProjetWithBudget(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Erreur getById: " + e.getMessage());
        }
        return null;
    }

    public Projet getById(Integer idProjet) {
        if (idProjet == null) return null;
        return getById(idProjet.intValue());
    }

    public void insert(Projet p) {
        insertAndReturnId(p);
    }

    public int insertAndReturnId(Projet p) {
        if (p == null) return -1;

        Budget b = extractBudgetSafe(p);

        String sqlProjet =
                "INSERT INTO projet (entreprise_id, titre, description, statut, score_esg, company_address, company_email, company_phone) " +
                        "VALUES (?,?,?,?,?,?,?,?)";

        String sqlBudget =
                "INSERT INTO budget (montant, raison, devise, id_projet) VALUES (?,?,?,?)";

        try {
            cnx.setAutoCommit(false);

            int newId;
            try (PreparedStatement ps = cnx.prepareStatement(sqlProjet, Statement.RETURN_GENERATED_KEYS)) {

                ps.setInt(1, p.getEntrepriseId());
                ps.setString(2, p.getTitre());
                ps.setString(3, p.getDescription());

                // statut
                String statut = safeStatut(p);
                ps.setString(4, statut);

                // score ESG doit rester NULL côté entreprise
                if (p.getScoreEsg() == null) ps.setNull(5, Types.INTEGER);
                else ps.setInt(5, p.getScoreEsg());

                ps.setString(6, p.getCompanyAddress());
                ps.setString(7, p.getCompanyEmail());
                ps.setString(8, p.getCompanyPhone());

                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("Insertion projet: aucune clé générée");
                    newId = keys.getInt(1);
                }
            }

            try (PreparedStatement psB = cnx.prepareStatement(sqlBudget)) {
                psB.setDouble(1, b.getMontant());
                psB.setString(2, b.getRaison());
                psB.setString(3, normalizeDevise(b.getDevise()));
                psB.setInt(4, newId);
                psB.executeUpdate();
            }

            cnx.commit();
            return newId;
        } catch (SQLException e) {
            rollbackQuietly();
            System.out.println("Erreur insertAndReturnId: " + e.getMessage());
            return -1;
        } finally {
            setAutoCommitQuietly(true);
        }
    }

    public void update(Projet p) {
        if (p == null) return;

        String current = getStatutById(p.getId());
        if (current == null) current = safeStatut(p);

        // Règles : si DRAFT => tout modifiable
        // sinon => seulement description + infos entreprise
        if ("DRAFT".equalsIgnoreCase(current)) {
            updateDraft(p);
        } else {
            updateDescriptionOnly(p.getId(),
                    p.getDescription(),
                    p.getCompanyAddress(),
                    p.getCompanyEmail(),
                    p.getCompanyPhone()
            );
        }
    }

    private void updateDraft(Projet p) {
        Budget b = extractBudgetSafe(p);

        String sqlProjet =
                "UPDATE projet SET titre=?, description=?, company_address=?, company_email=?, company_phone=? " +
                        "WHERE id=?";

        String sqlBudget =
                "UPDATE budget SET montant=?, raison=?, devise=? WHERE id_projet=?";

        try {
            cnx.setAutoCommit(false);

            try (PreparedStatement ps = cnx.prepareStatement(sqlProjet)) {
                ps.setString(1, p.getTitre());
                ps.setString(2, p.getDescription());
                ps.setString(3, p.getCompanyAddress());
                ps.setString(4, p.getCompanyEmail());
                ps.setString(5, p.getCompanyPhone());
                ps.setInt(6, p.getId());
                ps.executeUpdate();
            }

            int updated;
            try (PreparedStatement psB = cnx.prepareStatement(sqlBudget)) {
                psB.setDouble(1, b.getMontant());
                psB.setString(2, b.getRaison());
                psB.setString(3, normalizeDevise(b.getDevise()));
                psB.setInt(4, p.getId());
                updated = psB.executeUpdate();
            }

            if (updated == 0) {
                String ins = "INSERT INTO budget (montant, raison, devise, id_projet) VALUES (?,?,?,?)";
                try (PreparedStatement psIns = cnx.prepareStatement(ins)) {
                    psIns.setDouble(1, b.getMontant());
                    psIns.setString(2, b.getRaison());
                    psIns.setString(3, normalizeDevise(b.getDevise()));
                    psIns.setInt(4, p.getId());
                    psIns.executeUpdate();
                }
            }

            cnx.commit();

        } catch (SQLException e) {
            rollbackQuietly();
            System.out.println("Erreur updateDraft: " + e.getMessage());
        } finally {
            setAutoCommitQuietly(true);
        }
    }


    public void updateDescriptionOnly(int id, String description, String address, String email, String phone) {
        String sql = "UPDATE projet SET description=?, company_address=?, company_email=?, company_phone=? WHERE id=?";
        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, description);
            ps.setString(2, address);
            ps.setString(3, email);
            ps.setString(4, phone);
            ps.setInt(5, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur updateDescriptionOnly: " + e.getMessage());
        }
    }


    public void delete(int id) {
        String sql = "DELETE FROM projet WHERE id=?";
        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur delete projet: " + e.getMessage());
        }
    }

    public void cancel(int id) {
        String statut = getStatutById(id);
        if (statut == null) statut = "";

        // Règle métier: si DRAFT => delete, sinon => CANCELLED
        if ("DRAFT".equalsIgnoreCase(statut)) {
            delete(id);
            return;
        }

        String sql = "UPDATE projet SET statut='CANCELLED' WHERE id=?";
        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur cancel projet: " + e.getMessage());
        }
    }


    public boolean updateStatut(int idProjet, String statut) {
        // Bloquer SUBMITTED -> DRAFT
        String current = getStatutById(idProjet);
        if (current != null && "SUBMITTED".equalsIgnoreCase(current) && "DRAFT".equalsIgnoreCase(statut)) {
            return false;
        }

        String sql = "UPDATE projet SET statut=? WHERE id=?";
        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, statut);
            ps.setInt(2, idProjet);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Erreur updateStatut: " + e.getMessage());
            return false;
        }
    }

    public String getStatutById(int idProjet) {
        String sql = "SELECT statut FROM projet WHERE id=?";
        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idProjet);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("statut");
            }
        } catch (SQLException e) {
            System.out.println("Erreur getStatutById: " + e.getMessage());
        }
        return null;
    }

    // ✅ Overload attendu (Integer)
    public String getStatutById(Integer idProjet) {
        if (idProjet == null) return null;
        return getStatutById(idProjet.intValue());
    }


    private Projet mapProjetWithBudget(ResultSet rs) throws SQLException {
        Integer score = (Integer) rs.getObject("score_esg");

        Projet p = new Projet();
        p.setId(rs.getInt("id"));
        p.setEntrepriseId(rs.getInt("entreprise_id"));
        p.setTitre(rs.getString("titre"));
        p.setDescription(rs.getString("description"));
        p.setStatutEvaluation(rs.getString("statut"));
        p.setScoreEsg(score);
        p.setCompanyAddress(rs.getString("company_address"));
        p.setCompanyEmail(rs.getString("company_email"));
        p.setCompanyPhone(rs.getString("company_phone"));

        // budget (LEFT JOIN)
        Object idBudget = rs.getObject("id_budget");
        if (idBudget != null) {
            Budget b = new Budget();
            b.setIdBudget(rs.getInt("id_budget"));
            b.setMontant(rs.getDouble("montant"));
            b.setRaison(rs.getString("raison"));
            b.setDevise(rs.getString("devise"));
            b.setIdProjet(rs.getInt("id"));
            p.setBudget(b);

            // compat si tu as encore budget double dans Projet
            try { p.setBudget(b.getMontant()); } catch (Exception ignored) {}
        } else {
            p.setBudget(null);
            try { p.setBudget(0); } catch (Exception ignored) {}
        }

        return p;
    }

    private Budget extractBudgetSafe(Projet p) {
        Budget b = null;
        try { b = p.getBudgetObj(); } catch (Exception ignored) {}

        if (b == null) {
            b = new Budget();
            double montant = 0;
            try { montant = p.getBudget(); } catch (Exception ignored) {}
            b.setMontant(montant);
            b.setRaison("Budget");
            b.setDevise("TND");
        }

        if (b.getRaison() == null || b.getRaison().trim().isEmpty()) b.setRaison("Budget");
        if (b.getDevise() == null || b.getDevise().trim().isEmpty()) b.setDevise("TND");
        b.setDevise(normalizeDevise(b.getDevise()));
        return b;
    }

    private String safeStatut(Projet p) {
        String s = null;
        try { s = p.getStatutEvaluation(); } catch (Exception ignored) {}
        if (s == null) {
            try { s = p.getStatut(); } catch (Exception ignored) {}
        }
        if (s == null || s.trim().isEmpty()) return "DRAFT";
        return s.trim().toUpperCase();
    }

    private String normalizeDevise(String d) {
        if (d == null) return "TND";
        String v = d.trim().toUpperCase();
        if (v.equals("TND") || v.equals("EUR") || v.equals("USD")) return v;
        return "TND";
    }

    private void rollbackQuietly() {
        try { cnx.rollback(); } catch (Exception ignored) {}
    }

    private void setAutoCommitQuietly(boolean value) {
        try { cnx.setAutoCommit(value); } catch (Exception ignored) {}
    }
}