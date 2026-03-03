package Services;

import DataBase.MyConnection;
import Models.Budget;
import Models.Projet;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProjetService {

    public List<Projet> afficher() {
        try (Connection cnx = MyConnection.getConnection()) {
            String sql = "SELECT id, entreprise_id, titre, description, statut, score_esg, " +
                    "       company_address, company_email, company_phone " +
                    "FROM projet " +
                    "ORDER BY date_creation DESC";

            List<Projet> list = new ArrayList<>();

            try (PreparedStatement ps = cnx.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    Integer score = (Integer) rs.getObject("score_esg");

                    Projet p = new Projet(
                            rs.getInt("id"),
                            rs.getInt("entreprise_id"),
                            rs.getString("titre"),
                            rs.getString("description"),
                            score,
                            rs.getString("statut"),
                            rs.getString("company_address"),
                            rs.getString("company_email"),
                            rs.getString("company_phone")
                    );

                    list.add(p);
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
            String sql = "SELECT id, entreprise_id, titre, description, statut, score_esg, " +
                    "       company_address, company_email, company_phone " +
                    "FROM projet " +
                    "WHERE entreprise_id=? " +
                    "ORDER BY date_creation DESC";

            List<Projet> list = new ArrayList<>();

            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setInt(1, entrepriseId);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Integer score = (Integer) rs.getObject("score_esg");
                        Projet p = new Projet(
                                rs.getInt("id"),
                                rs.getInt("entreprise_id"),
                                rs.getString("titre"),
                                rs.getString("description"),
                                score,
                                rs.getString("statut"),
                                rs.getString("company_address"),
                                rs.getString("company_email"),
                                rs.getString("company_phone")
                        );

                        list.add(p);
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

    public void insert(Projet p) {
        String sql = "INSERT INTO projet (" +
                "  entreprise_id, titre, description, budget, statut, score_esg, " +
                "  company_address, company_email, company_phone" +
                ") VALUES (?,?,?,?,?,?,?,?,?)";

        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, p.getEntrepriseId());
            ps.setString(2, p.getTitre());
            ps.setString(3, p.getDescription());
            ps.setDouble(4, p.getBudget());
            ps.setString(5, p.getStatut());

            // score ESG doit rester NULL côté entreprise
            if (p.getScoreEsg() == null) ps.setNull(6, Types.INTEGER);
            else ps.setInt(6, p.getScoreEsg());

            ps.setString(7, p.getCompanyAddress());
            ps.setString(8, p.getCompanyEmail());
            ps.setString(9, p.getCompanyPhone());

            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur insert projet: " + e.getMessage());
        }
    }

    public int insertAndReturnId(Projet p) {
        String sql = "INSERT INTO projet (" +
                "  entreprise_id, titre, description, budget, statut, score_esg, " +
                "  company_address, company_email, company_phone" +
                ") VALUES (?,?,?,?,?,?,?,?,?)";

        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getEntrepriseId());
            ps.setString(2, p.getTitre());
            ps.setString(3, p.getDescription());
            ps.setDouble(4, p.getBudget());
            ps.setString(5, p.getStatut());

            if (p.getScoreEsg() == null) ps.setNull(6, Types.INTEGER);
            else ps.setInt(6, p.getScoreEsg());

            ps.setString(7, p.getCompanyAddress());
            ps.setString(8, p.getCompanyEmail());
            ps.setString(9, p.getCompanyPhone());

            int updated = ps.executeUpdate();
            if (updated == 0) return -1;

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println("Erreur insertAndReturnId projet: " + e.getMessage());
        }
        return -1;
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
        String sql = "UPDATE projet SET titre=?, description=?, budget=?, statut=?, score_esg=?, " +
                "company_address=?, company_email=?, company_phone=? WHERE id=?";

        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, p.getTitre());
            ps.setString(2, p.getDescription());
            ps.setDouble(3, p.getBudget());
            ps.setString(4, p.getStatut());
            if (p.getScoreEsg() == null) ps.setNull(5, Types.INTEGER);
            else ps.setInt(5, p.getScoreEsg());

            ps.setString(6, p.getCompanyAddress());
            ps.setString(7, p.getCompanyEmail());
            ps.setString(8, p.getCompanyPhone());
            ps.setInt(9, p.getId());

            ps.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Erreur updateDraft: " + e.getMessage());
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


    public Projet getById(int id) {
        String sql = "SELECT p.id, p.entreprise_id, p.titre, p.description, " +
                "       p.statut, p.score_esg, p.date_creation, " +
                "       p.company_address, p.company_email, p.company_phone, " +
                "       COALESCE(b.montant, 0) AS budget " +
                "FROM projet p " +
                "LEFT JOIN budget b ON b.id_projet = p.id " +
                "WHERE p.id = ?";

        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Integer score = (Integer) rs.getObject("score_esg");
                Projet p = new Projet(
                        rs.getInt("id"),
                        rs.getInt("entreprise_id"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        score,
                        rs.getString("statut"),
                        rs.getString("company_address"),
                        rs.getString("company_email"),
                        rs.getString("company_phone")
                );
                p.setBudget(rs.getDouble("budget"));
                return p;
            }

        } catch (SQLException e) {
            System.out.println("Erreur getById projet: " + e.getMessage());
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
}