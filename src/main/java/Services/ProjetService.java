package Services;

import DataBase.MyConnection;
import Models.Projet;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProjetService {

    private final Connection cnx = MyConnection.getConnection();

    // -------------------------
    // READ: tous les projets
    // -------------------------
    public List<Projet> afficher() {
        String sql = "SELECT id, entreprise_id, titre, description, budget, statut, score_esg, " +
                "       company_address, company_email, company_phone " +
                "FROM projet " +
                "ORDER BY date_creation DESC";

        List<Projet> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Projet p = new Projet(
                        rs.getInt("id"),
                        rs.getInt("entreprise_id"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        rs.getDouble("budget"),
                        rs.getInt("score_esg"),
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
    }

    // -------------------------
    // READ: projets par entreprise
    // -------------------------
    public List<Projet> getByEntreprise(int entrepriseId) {
        String sql = "SELECT id, entreprise_id, titre, description, budget, statut, score_esg, " +
                "       company_address, company_email, company_phone " +
                "FROM projet " +
                "WHERE entreprise_id=? " +
                "ORDER BY date_creation DESC";

        List<Projet> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, entrepriseId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Projet p = new Projet(
                            rs.getInt("id"),
                            rs.getInt("entreprise_id"),
                            rs.getString("titre"),
                            rs.getString("description"),
                            rs.getDouble("budget"),
                            rs.getInt("score_esg"),
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
    }

    // -------------------------
    // CREATE
    // -------------------------
    public void insert(Projet p) {
        String sql = "INSERT INTO projet (" +
                "  entreprise_id, titre, description, budget, statut, score_esg, " +
                "  company_address, company_email, company_phone" +
                ") VALUES (?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, p.getEntrepriseId());
            ps.setString(2, p.getTitre());
            ps.setString(3, p.getDescription());
            ps.setDouble(4, p.getBudget());
            ps.setString(5, p.getStatut());
            ps.setInt(6, p.getScoreEsg());

            ps.setString(7, p.getCompanyAddress());
            ps.setString(8, p.getCompanyEmail());
            ps.setString(9, p.getCompanyPhone());

            ps.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Erreur insert projet: " + e.getMessage());
        }
    }

    // -------------------------
    // UPDATE complet (DRAFT)
    // -------------------------
    public void update(Projet p) {
        String sql = "UPDATE projet SET " +
                "  titre=?, description=?, budget=?, statut=?, score_esg=?, " +
                "  company_address=?, company_email=?, company_phone=? " +
                "WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, p.getTitre());
            ps.setString(2, p.getDescription());
            ps.setDouble(3, p.getBudget());
            ps.setString(4, p.getStatut());
            ps.setInt(5, p.getScoreEsg());

            ps.setString(6, p.getCompanyAddress());
            ps.setString(7, p.getCompanyEmail());
            ps.setString(8, p.getCompanyPhone());

            ps.setInt(9, p.getId());

            ps.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Erreur update projet: " + e.getMessage());
        }
    }

    // -------------------------
    // UPDATE description seulement
    // -------------------------
    public void updateDescriptionOnly(int id, String description) {
        String sql = "UPDATE projet SET description=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, description);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur updateDescriptionOnly: " + e.getMessage());
        }
    }

    // -------------------------
    // DELETE (si tu veux vraiment)
    // -------------------------
    public void delete(int id) {
        String sql = "DELETE FROM projet WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur delete projet: " + e.getMessage());
        }
    }

    // -------------------------
    // OPTION B: Annuler => CANCELLED
    // -------------------------
    public void cancel(int id) {
        String sql = "UPDATE projet SET statut='CANCELLED' WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur cancel projet: " + e.getMessage());
        }
    }
}
