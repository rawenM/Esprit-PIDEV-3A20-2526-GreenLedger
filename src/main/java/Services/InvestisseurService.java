package Services;

import DataBase.MyConnection;
import Models.Projet;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Central service for all Investisseur (investor) data operations.
 */
public class InvestisseurService {

    // ── Dashboard ─────────────────────────────────────────────────────────────

    public static class DashboardData {
        public double totalAvailable;
        public double totalRetired;
        public int    totalWallets;
        public int    zeroBalance;
        public int    fundedProjects;
        public int    totalFinancements;
        public int    activeListings;
        public int    myOrders;
        public double healthScore;
        public double readinessScore;
        public int    unreadMessages;
    }

    public DashboardData getDashboardData(long userId) {
        DashboardData d = new DashboardData();
        String sql =
            "SELECT " +
            "(SELECT COALESCE(SUM(available_credits),0) FROM wallet WHERE owner_id=?) AS avail," +
            "(SELECT COALESCE(SUM(retired_credits),0)   FROM wallet WHERE owner_id=?) AS retired," +
            "(SELECT COUNT(*) FROM wallet WHERE owner_id=?) AS wallets," +
            "(SELECT COUNT(*) FROM wallet WHERE owner_id=? AND available_credits=0) AS zero_bal," +
            "(SELECT COUNT(*) FROM financements WHERE investisseur_id=? AND statut='COMPLETED') AS funded," +
            "(SELECT COUNT(*) FROM financements WHERE investisseur_id=?) AS total_fin," +
            "(SELECT COUNT(*) FROM marketplace_listings WHERE status='active') AS listings," +
            "(SELECT COUNT(*) FROM marketplace_orders WHERE buyer_id=?) AS orders";

        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId); ps.setLong(2, userId); ps.setLong(3, userId);
            ps.setLong(4, userId); ps.setLong(5, userId); ps.setLong(6, userId);
            ps.setLong(7, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    d.totalAvailable    = rs.getDouble("avail");
                    d.totalRetired      = rs.getDouble("retired");
                    d.totalWallets      = rs.getInt("wallets");
                    d.zeroBalance       = rs.getInt("zero_bal");
                    d.fundedProjects    = rs.getInt("funded");
                    d.totalFinancements = rs.getInt("total_fin");
                    d.activeListings    = rs.getInt("listings");
                    d.myOrders          = rs.getInt("orders");
                }
            }
        } catch (SQLException e) {
            System.err.println("[InvestisseurService] getDashboardData: " + e.getMessage());
        }

        // Health score
        double capitalBase = d.totalAvailable + d.totalRetired;
        double capitalEff  = capitalBase > 0 ? (d.totalAvailable / capitalBase * 100) : 0;
        double total       = Math.max(1, d.totalFinancements);
        d.healthScore = Math.min(100, Math.round(
            (d.fundedProjects / total * 0.45 + capitalEff / 100.0 * 0.30) * 100));
        d.readinessScore = Math.min(100, Math.round(
            (d.totalWallets > 0 ? 40 : 0) + (d.fundedProjects > 0 ? 40 : 0) + (d.totalAvailable > 0 ? 20 : 0)));

        // Unread messages
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT COUNT(*) FROM thread_messages tm " +
                 "JOIN conversation_threads ct ON ct.id=tm.thread_id " +
                 "WHERE ct.investisseur_id=? AND tm.sender_id!=? AND tm.is_read=0")) {
            ps.setLong(1, userId); ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) d.unreadMessages = rs.getInt(1);
            }
        } catch (SQLException e) { /* ignore */ }

        return d;
    }

    // ── Projects for investment ───────────────────────────────────────────────

    public static class ProjetInvestDTO {
        public int    id;
        public String titre;
        public String description;
        public String secteur;
        public String localisation;
        public Integer scoreEsg;
        public Double montantDemande;
        public Double avoidedTco2;
        public Double fraudRiskScore;
        public Boolean fraudFlag;
        public Double dispatchedGreenCredits;
        public Double roi;
    }

    public List<ProjetInvestDTO> getProjectsForInvestment(
            String secteur, Integer esgMin, Integer esgMax,
            Double montantMin, Double montantMax) {

        StringBuilder sql = new StringBuilder(
            "SELECT id, titre, description, secteur, localisation, score_esg, " +
            "montant_demande, avoided_tco2, fraud_risk_score, fraud_flag, " +
            "dispatched_green_credits, roi " +
            "FROM projet WHERE statut='APPROVED' AND statut_financement='SEEKING_FUNDING'");

        List<Object> params = new ArrayList<>();
        if (secteur != null && !secteur.isBlank()) { sql.append(" AND secteur=?"); params.add(secteur); }
        if (esgMin  != null) { sql.append(" AND score_esg>=?"); params.add(esgMin); }
        if (esgMax  != null) { sql.append(" AND score_esg<=?"); params.add(esgMax); }
        if (montantMin != null) { sql.append(" AND montant_demande>=?"); params.add(montantMin); }
        if (montantMax != null) { sql.append(" AND montant_demande<=?"); params.add(montantMax); }
        sql.append(" ORDER BY date_creation DESC LIMIT 20");

        List<ProjetInvestDTO> list = new ArrayList<>();
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object v = params.get(i);
                if (v instanceof String)  ps.setString(i + 1, (String) v);
                else if (v instanceof Integer) ps.setInt(i + 1, (Integer) v);
                else if (v instanceof Double)  ps.setDouble(i + 1, (Double) v);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ProjetInvestDTO dto = new ProjetInvestDTO();
                    dto.id          = rs.getInt("id");
                    dto.titre       = rs.getString("titre");
                    dto.description = rs.getString("description");
                    dto.secteur     = rs.getString("secteur");
                    dto.localisation= rs.getString("localisation");
                    dto.scoreEsg    = nullInt(rs, "score_esg");
                    dto.montantDemande = nullDouble(rs, "montant_demande");
                    dto.avoidedTco2    = nullDouble(rs, "avoided_tco2");
                    dto.fraudRiskScore = nullDouble(rs, "fraud_risk_score");
                    dto.fraudFlag      = rs.getBoolean("fraud_flag");
                    dto.dispatchedGreenCredits = nullDouble(rs, "dispatched_green_credits");
                    dto.roi = nullDouble(rs, "roi");
                    list.add(dto);
                }
            }
        } catch (SQLException e) {
            System.err.println("[InvestisseurService] getProjectsForInvestment: " + e.getMessage());
        }
        return list;
    }

    // ── Portfolio ─────────────────────────────────────────────────────────────

    public static class PortfolioItem {
        public int    projectId;
        public String titre;
        public String description;
        public Integer scoreEsg;
        public Double dispatchedGreenCredits;
        public String fundedAt;
        public double fundedAmount;
    }

    public List<PortfolioItem> getPortfolio(long investisseurId) {
        List<PortfolioItem> list = new ArrayList<>();
        String sql =
            "SELECT p.id, p.titre, p.description, p.score_esg, " +
            "p.dispatched_green_credits, p.funded_at, f.montant " +
            "FROM projet p JOIN financements f ON f.project_id=p.id " +
            "WHERE f.investisseur_id=? AND f.statut='COMPLETED' " +
            "ORDER BY f.montant DESC";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, investisseurId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PortfolioItem item = new PortfolioItem();
                    item.projectId   = rs.getInt("id");
                    item.titre       = rs.getString("titre");
                    item.description = rs.getString("description");
                    item.scoreEsg    = nullInt(rs, "score_esg");
                    item.dispatchedGreenCredits = nullDouble(rs, "dispatched_green_credits");
                    item.fundedAt    = rs.getString("funded_at");
                    item.fundedAmount = rs.getDouble("montant");
                    list.add(item);
                }
            }
        } catch (SQLException e) {
            System.err.println("[InvestisseurService] getPortfolio: " + e.getMessage());
        }
        return list;
    }

    // ── Investment (create financement) ──────────────────────────────────────

    public int createInvestment(int projectId, long investisseurId, double amount) {
        String sql = "INSERT INTO financements (project_id, investisseur_id, montant, statut, created_at) " +
                     "VALUES (?,?,?,'PENDING',NOW())";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, projectId);
            ps.setLong(2, investisseurId);
            ps.setDouble(3, amount);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[InvestisseurService] createInvestment: " + e.getMessage());
        }
        return -1;
    }

    public boolean confirmInvestment(int financementId) {
        String sql = "UPDATE financements SET statut='COMPLETED', completed_at=NOW() WHERE id=?";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, financementId);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                // Auto-create conversation thread
                createConversationThread(financementId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[InvestisseurService] confirmInvestment: " + e.getMessage());
        }
        return false;
    }

    private void createConversationThread(int financementId) {
        String getSql = "SELECT f.project_id, f.investisseur_id, p.entreprise_id " +
                        "FROM financements f JOIN projet p ON p.id=f.project_id WHERE f.id=?";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(getSql)) {
            ps.setInt(1, financementId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int projectId    = rs.getInt("project_id");
                    long investId    = rs.getLong("investisseur_id");
                    long porteurId   = rs.getLong("entreprise_id");
                    // Check if thread already exists
                    String check = "SELECT id FROM conversation_threads WHERE project_id=? AND investisseur_id=?";
                    try (PreparedStatement ps2 = conn.prepareStatement(check)) {
                        ps2.setInt(1, projectId); ps2.setLong(2, investId);
                        try (ResultSet rs2 = ps2.executeQuery()) {
                            if (rs2.next()) return; // already exists
                        }
                    }
                    String ins = "INSERT INTO conversation_threads (project_id, investisseur_id, porteur_id, created_at) VALUES (?,?,?,NOW())";
                    try (PreparedStatement ps3 = conn.prepareStatement(ins)) {
                        ps3.setInt(1, projectId); ps3.setLong(2, investId); ps3.setLong(3, porteurId);
                        ps3.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[InvestisseurService] createConversationThread: " + e.getMessage());
        }
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    public static class NotifDTO {
        public long   id;
        public String type;
        public String message;
        public boolean read;
        public String createdAt;
        public int    relatedProjectId;
    }

    public List<NotifDTO> getNotifications(long userId) {
        List<NotifDTO> list = new ArrayList<>();
        String sql = "SELECT id, type, message, is_read, created_at, related_project_id " +
                     "FROM notifications WHERE user_id=? ORDER BY is_read ASC, created_at DESC LIMIT 20";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    NotifDTO n = new NotifDTO();
                    n.id = rs.getLong("id");
                    n.type = rs.getString("type");
                    n.message = rs.getString("message");
                    n.read = rs.getBoolean("is_read");
                    n.createdAt = rs.getString("created_at");
                    n.relatedProjectId = rs.getInt("related_project_id");
                    list.add(n);
                }
            }
        } catch (SQLException e) { /* ignore */ }
        return list;
    }

    public void markNotificationRead(long notifId, long userId) {
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE notifications SET is_read=1 WHERE id=? AND user_id=?")) {
            ps.setLong(1, notifId); ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) { /* ignore */ }
    }

    public void markAllNotificationsRead(long userId) {
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE notifications SET is_read=1 WHERE user_id=? AND is_read=0")) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) { /* ignore */ }
    }

    public int getUnreadNotificationCount(long userId) {
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT COUNT(*) FROM notifications WHERE user_id=? AND is_read=0")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { /* ignore */ }
        return 0;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Double nullDouble(ResultSet rs, String col) throws SQLException {
        double v = rs.getDouble(col); return rs.wasNull() ? null : v;
    }
    private Integer nullInt(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col); return rs.wasNull() ? null : v;
    }

    public static String fraudLevel(Double risk) {
        if (risk == null) return "FAIBLE";
        if (risk >= 0.65) return "ELEVE";
        if (risk >= 0.35) return "MOYEN";
        return "FAIBLE";
    }

    public static String fraudColor(Double risk) {
        if (risk == null) return "#10b981";
        if (risk >= 0.65) return "#f43f5e";
        if (risk >= 0.35) return "#f59e0b";
        return "#10b981";
    }

    public static String priorityBadge(double amount) {
        if (amount >= 20000) return "Major Investment";
        if (amount >= 10000) return "High Priority";
        if (amount >= 5000)  return "Priority";
        return "Standard";
    }

    public static String priorityColor(double amount) {
        if (amount >= 20000) return "#7c3aed";
        if (amount >= 10000) return "#2563eb";
        if (amount >= 5000)  return "#10b981";
        return "#64748b";
    }

    public static String fmt(double v) {
        return String.format(Locale.ROOT, "%,.0f", v);
    }
}
