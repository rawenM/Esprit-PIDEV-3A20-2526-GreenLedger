package Services;

import DataBase.MyConnection;
import Models.Wallet;

import java.sql.*;
import java.util.*;

/**
 * Wallet Supervision Service for Admin Dashboard
 * Monitors wallet health, deficits, and financial risks
 * 
 * Features:
 * - Track negative wallets (deficit situations)
 * - Monitor at-risk wallets (low balance)
 * - Calculate cumulative deficits
 * - Identify priority owners with multiple negative wallets
 */
public class WalletSupervisionService {

    private Connection conn;
    private static final double AT_RISK_THRESHOLD = 50.0; // Credits below this are "at risk"

    public WalletSupervisionService() {
        this.conn = MyConnection.getConnection();
    }

    /**
     * Get comprehensive wallet overview for admin dashboard
     */
    public Map<String, Object> getWalletOverview() {
        Map<String, Object> overview = new HashMap<>();
        
        try {
            // Total wallets tracked
            overview.put("totalWallets", getTotalWalletsCount());
            
            // Negative wallets (deficit)
            overview.put("negativeWallets", getNegativeWalletsCount());
            
            // At-risk wallets (low balance)
            overview.put("atRiskWallets", getAtRiskWalletsCount());
            
            // Cumulative deficit
            overview.put("cumulativeDeficit", getCumulativeDeficit());
            
            // Average wallet balance
            overview.put("averageBalance", getAverageWalletBalance());
            
            // Total available credits across all wallets
            overview.put("totalAvailableCredits", getTotalAvailableCredits());
            
            // Total retired credits
            overview.put("totalRetiredCredits", getTotalRetiredCredits());
            
        } catch (SQLException e) {
            System.err.println("[WalletSupervision] Error getting overview: " + e.getMessage());
            e.printStackTrace();
        }
        
        return overview;
    }

    /**
     * Get top 25 negative wallets (highest deficits)
     */
    public List<Map<String, Object>> getNegativeWallets(int limit) {
        List<Map<String, Object>> negativeWallets = new ArrayList<>();
        
        String sql = "SELECT w.id, w.wallet_number, w.name, w.owner_type, w.owner_id, " +
                     "w.available_credits, w.retired_credits, w.created_at, " +
                     "(w.available_credits) as deficit " +
                     "FROM wallet w " +
                     "WHERE w.available_credits < 0 " +
                     "ORDER BY w.available_credits ASC " +
                     "LIMIT ?";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> wallet = new HashMap<>();
                wallet.put("id", rs.getInt("id"));
                wallet.put("walletNumber", rs.getString("wallet_number"));
                wallet.put("name", rs.getString("name"));
                wallet.put("ownerType", rs.getString("owner_type"));
                wallet.put("ownerId", rs.getInt("owner_id"));
                wallet.put("availableCredits", rs.getDouble("available_credits"));
                wallet.put("retiredCredits", rs.getDouble("retired_credits"));
                wallet.put("deficit", Math.abs(rs.getDouble("deficit")));
                wallet.put("createdAt", rs.getTimestamp("created_at"));
                wallet.put("priority", calculatePriority(rs.getDouble("available_credits")));
                
                negativeWallets.add(wallet);
            }
        } catch (SQLException e) {
            System.err.println("[WalletSupervision] Error getting negative wallets: " + e.getMessage());
        }
        
        return negativeWallets;
    }

    /**
     * Get priority owners with multiple negative wallets
     */
    public List<Map<String, Object>> getPriorityOwners(int limit) {
        List<Map<String, Object>> priorityOwners = new ArrayList<>();
        
        String sql = "SELECT owner_type, owner_id, " +
                     "COUNT(*) as negative_wallet_count, " +
                     "SUM(ABS(available_credits)) as total_deficit " +
                     "FROM wallet " +
                     "WHERE available_credits < 0 " +
                     "GROUP BY owner_type, owner_id " +
                     "HAVING negative_wallet_count > 0 " +
                     "ORDER BY total_deficit DESC " +
                     "LIMIT ?";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> owner = new HashMap<>();
                owner.put("ownerType", rs.getString("owner_type"));
                owner.put("ownerId", rs.getInt("owner_id"));
                owner.put("negativeWalletCount", rs.getInt("negative_wallet_count"));
                owner.put("totalDeficit", rs.getDouble("total_deficit"));
                owner.put("riskLevel", calculateRiskLevel(rs.getDouble("total_deficit")));
                
                priorityOwners.add(owner);
            }
        } catch (SQLException e) {
            System.err.println("[WalletSupervision] Error getting priority owners: " + e.getMessage());
        }
        
        return priorityOwners;
    }

    /**
     * Get at-risk wallets (low balance but not negative)
     */
    public List<Map<String, Object>> getAtRiskWallets(int limit) {
        List<Map<String, Object>> atRiskWallets = new ArrayList<>();
        
        String sql = "SELECT w.id, w.wallet_number, w.name, w.owner_type, w.owner_id, " +
                     "w.available_credits, w.retired_credits, w.created_at " +
                     "FROM wallet w " +
                     "WHERE w.available_credits >= 0 AND w.available_credits < ? " +
                     "ORDER BY w.available_credits ASC " +
                     "LIMIT ?";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, AT_RISK_THRESHOLD);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> wallet = new HashMap<>();
                wallet.put("id", rs.getInt("id"));
                wallet.put("walletNumber", rs.getString("wallet_number"));
                wallet.put("name", rs.getString("name"));
                wallet.put("ownerType", rs.getString("owner_type"));
                wallet.put("ownerId", rs.getInt("owner_id"));
                wallet.put("availableCredits", rs.getDouble("available_credits"));
                wallet.put("retiredCredits", rs.getDouble("retired_credits"));
                wallet.put("createdAt", rs.getTimestamp("created_at"));
                wallet.put("warningLevel", calculateWarningLevel(rs.getDouble("available_credits")));
                
                atRiskWallets.add(wallet);
            }
        } catch (SQLException e) {
            System.err.println("[WalletSupervision] Error getting at-risk wallets: " + e.getMessage());
        }
        
        return atRiskWallets;
    }

    /**
     * Get wallet health report for specific owner
     */
    public Map<String, Object> getOwnerWalletHealth(String ownerType, int ownerId) {
        Map<String, Object> health = new HashMap<>();
        
        String sql = "SELECT " +
                     "COUNT(*) as total_wallets, " +
                     "SUM(CASE WHEN available_credits < 0 THEN 1 ELSE 0 END) as negative_count, " +
                     "SUM(CASE WHEN available_credits >= 0 AND available_credits < ? THEN 1 ELSE 0 END) as at_risk_count, " +
                     "SUM(available_credits) as total_available, " +
                     "SUM(retired_credits) as total_retired, " +
                     "SUM(CASE WHEN available_credits < 0 THEN ABS(available_credits) ELSE 0 END) as total_deficit " +
                     "FROM wallet " +
                     "WHERE owner_type = ? AND owner_id = ?";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, AT_RISK_THRESHOLD);
            ps.setString(2, ownerType);
            ps.setInt(3, ownerId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                health.put("totalWallets", rs.getInt("total_wallets"));
                health.put("negativeCount", rs.getInt("negative_count"));
                health.put("atRiskCount", rs.getInt("at_risk_count"));
                health.put("totalAvailable", rs.getDouble("total_available"));
                health.put("totalRetired", rs.getDouble("total_retired"));
                health.put("totalDeficit", rs.getDouble("total_deficit"));
                health.put("healthScore", calculateHealthScore(
                    rs.getInt("total_wallets"),
                    rs.getInt("negative_count"),
                    rs.getInt("at_risk_count")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[WalletSupervision] Error getting owner health: " + e.getMessage());
        }
        
        return health;
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private int getTotalWalletsCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM wallet";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private int getNegativeWalletsCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM wallet WHERE available_credits < 0";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private int getAtRiskWalletsCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM wallet WHERE available_credits >= 0 AND available_credits < ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, AT_RISK_THRESHOLD);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private double getCumulativeDeficit() throws SQLException {
        String sql = "SELECT SUM(ABS(available_credits)) FROM wallet WHERE available_credits < 0";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getDouble(1) : 0.0;
        }
    }

    private double getAverageWalletBalance() throws SQLException {
        String sql = "SELECT AVG(available_credits) FROM wallet";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getDouble(1) : 0.0;
        }
    }

    private double getTotalAvailableCredits() throws SQLException {
        String sql = "SELECT SUM(available_credits) FROM wallet WHERE available_credits > 0";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getDouble(1) : 0.0;
        }
    }

    private double getTotalRetiredCredits() throws SQLException {
        String sql = "SELECT SUM(retired_credits) FROM wallet";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getDouble(1) : 0.0;
        }
    }

    /**
     * Calculate priority level based on deficit amount
     */
    private String calculatePriority(double availableCredits) {
        double deficit = Math.abs(availableCredits);
        if (deficit >= 1000) return "CRITICAL";
        if (deficit >= 500) return "HIGH";
        if (deficit >= 100) return "MEDIUM";
        return "LOW";
    }

    /**
     * Calculate risk level for owners
     */
    private String calculateRiskLevel(double totalDeficit) {
        if (totalDeficit >= 5000) return "CRITICAL";
        if (totalDeficit >= 2000) return "HIGH";
        if (totalDeficit >= 500) return "MEDIUM";
        return "LOW";
    }

    /**
     * Calculate warning level for at-risk wallets
     */
    private String calculateWarningLevel(double availableCredits) {
        if (availableCredits < 10) return "URGENT";
        if (availableCredits < 25) return "HIGH";
        if (availableCredits < 50) return "MEDIUM";
        return "LOW";
    }

    /**
     * Calculate overall health score (0-100)
     */
    private int calculateHealthScore(int totalWallets, int negativeCount, int atRiskCount) {
        if (totalWallets == 0) return 100;
        
        int healthyWallets = totalWallets - negativeCount - atRiskCount;
        double healthPercentage = (double) healthyWallets / totalWallets;
        
        // Penalize negative wallets more heavily
        double negativeImpact = (double) negativeCount / totalWallets * 50;
        double atRiskImpact = (double) atRiskCount / totalWallets * 25;
        
        int score = (int) ((healthPercentage * 100) - negativeImpact - atRiskImpact);
        return Math.max(0, Math.min(100, score));
    }

    /**
     * Get wallet deficit trend (last 30 days)
     */
    public List<Map<String, Object>> getDeficitTrend(int days) {
        List<Map<String, Object>> trend = new ArrayList<>();
        
        try {
            // This would require a historical tracking table in production
            // For now, return current snapshot
            Map<String, Object> currentSnapshot = new HashMap<>();
            currentSnapshot.put("date", new java.util.Date());
            currentSnapshot.put("negativeWallets", getNegativeWalletsCount());
            currentSnapshot.put("cumulativeDeficit", getCumulativeDeficit());
            trend.add(currentSnapshot);
        } catch (SQLException e) {
            System.err.println("Error getting deficit trend: " + e.getMessage());
            e.printStackTrace();
        }
        
        return trend;
    }
}
