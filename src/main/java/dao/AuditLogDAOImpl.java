package dao;

import Models.AuditLog;
import DataBase.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implémentation du DAO pour le journal d'activité
 * IMPORTANT: L'enregistrement est AUTOMATIQUE et ne peut pas être désactivé par l'admin
 */
public class AuditLogDAOImpl implements AuditLogDAO {
    
    private Connection getConnection() throws SQLException {
        return MyConnection.getInstance().getConnection();
    }
    
    @Override
    public void log(AuditLog auditLog) {
        String sql = "INSERT INTO audit_log (user_id, user_email, user_name, action_type, action_description, " +
                    "target_user_id, target_user_email, ip_address, user_agent, browser, operating_system, " +
                    "status, error_message, old_value, new_value, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setObject(1, auditLog.getUserId());
            stmt.setString(2, auditLog.getUserEmail());
            stmt.setString(3, auditLog.getUserName());
            stmt.setString(4, auditLog.getActionType() != null ? auditLog.getActionType().name() : null);
            stmt.setString(5, auditLog.getActionDescription());
            stmt.setObject(6, auditLog.getTargetUserId());
            stmt.setString(7, auditLog.getTargetUserEmail());
            stmt.setString(8, auditLog.getIpAddress());
            stmt.setString(9, auditLog.getUserAgent());
            stmt.setString(10, auditLog.getBrowser());
            stmt.setString(11, auditLog.getOperatingSystem());
            stmt.setString(12, auditLog.getStatus() != null ? auditLog.getStatus().name() : "SUCCESS");
            stmt.setString(13, auditLog.getErrorMessage());
            stmt.setString(14, auditLog.getOldValue());
            stmt.setString(15, auditLog.getNewValue());
            stmt.setTimestamp(16, Timestamp.valueOf(auditLog.getCreatedAt()));
            
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                auditLog.setId(rs.getLong(1));
            }
            
        } catch (SQLException e) {
            System.err.println("[AUDIT] Erreur lors de l'enregistrement du log: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public List<AuditLog> findAll(int limit, int offset) {
        List<AuditLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM audit_log ORDER BY created_at DESC LIMIT ? OFFSET ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, limit);
            stmt.setInt(2, offset);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                logs.add(mapResultSetToAuditLog(rs));
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return logs;
    }
    
    @Override
    public List<AuditLog> findByUserId(Long userId) {
        List<AuditLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM audit_log WHERE user_id = ? ORDER BY created_at DESC";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                logs.add(mapResultSetToAuditLog(rs));
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return logs;
    }
    
    @Override
    public List<AuditLog> findByActionType(AuditLog.ActionType actionType) {
        List<AuditLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM audit_log WHERE action_type = ? ORDER BY created_at DESC";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, actionType.name());
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                logs.add(mapResultSetToAuditLog(rs));
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return logs;
    }
    
    @Override
    public List<AuditLog> findByStatus(AuditLog.ActionStatus status) {
        List<AuditLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM audit_log WHERE status = ? ORDER BY created_at DESC";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status.name());
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                logs.add(mapResultSetToAuditLog(rs));
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return logs;
    }
    
    @Override
    public List<AuditLog> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<AuditLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM audit_log WHERE created_at BETWEEN ? AND ? ORDER BY created_at DESC";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setTimestamp(1, Timestamp.valueOf(startDate));
            stmt.setTimestamp(2, Timestamp.valueOf(endDate));
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                logs.add(mapResultSetToAuditLog(rs));
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return logs;
    }
    
    @Override
    public List<AuditLog> search(String keyword) {
        List<AuditLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM audit_log WHERE " +
                    "user_email LIKE ? OR user_name LIKE ? OR action_description LIKE ? OR " +
                    "target_user_email LIKE ? OR ip_address LIKE ? " +
                    "ORDER BY created_at DESC";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            String searchPattern = "%" + keyword + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);
            stmt.setString(4, searchPattern);
            stmt.setString(5, searchPattern);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                logs.add(mapResultSetToAuditLog(rs));
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return logs;
    }
    
    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM audit_log";
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return 0;
    }
    
    @Override
    public long countByActionType(AuditLog.ActionType actionType) {
        String sql = "SELECT COUNT(*) FROM audit_log WHERE action_type = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, actionType.name());
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return 0;
    }
    
    @Override
    public List<AuditLog> findRecent(int limit) {
        return findAll(limit, 0);
    }
    
    @Override
    public List<AuditLog> findToday() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        return findByDateRange(startOfDay, endOfDay);
    }
    
    @Override
    public List<AuditLog> findRecentFailedLogins(int limit) {
        List<AuditLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM audit_log WHERE action_type = 'USER_LOGIN_FAILED' " +
                    "ORDER BY created_at DESC LIMIT ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, limit);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                logs.add(mapResultSetToAuditLog(rs));
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return logs;
    }
    
    private AuditLog mapResultSetToAuditLog(ResultSet rs) throws SQLException {
        AuditLog log = new AuditLog();
        
        log.setId(rs.getLong("id"));
        log.setUserId(rs.getObject("user_id") != null ? rs.getLong("user_id") : null);
        log.setUserEmail(rs.getString("user_email"));
        log.setUserName(rs.getString("user_name"));
        
        String actionTypeStr = rs.getString("action_type");
        if (actionTypeStr != null) {
            try {
                log.setActionType(AuditLog.ActionType.valueOf(actionTypeStr));
            } catch (IllegalArgumentException e) {
                log.setActionType(null);
            }
        }
        
        log.setActionDescription(rs.getString("action_description"));
        log.setTargetUserId(rs.getObject("target_user_id") != null ? rs.getLong("target_user_id") : null);
        log.setTargetUserEmail(rs.getString("target_user_email"));
        log.setIpAddress(rs.getString("ip_address"));
        log.setUserAgent(rs.getString("user_agent"));
        log.setBrowser(rs.getString("browser"));
        log.setOperatingSystem(rs.getString("operating_system"));
        
        String statusStr = rs.getString("status");
        if (statusStr != null) {
            try {
                log.setStatus(AuditLog.ActionStatus.valueOf(statusStr));
            } catch (IllegalArgumentException e) {
                log.setStatus(AuditLog.ActionStatus.SUCCESS);
            }
        }
        
        log.setErrorMessage(rs.getString("error_message"));
        log.setOldValue(rs.getString("old_value"));
        log.setNewValue(rs.getString("new_value"));
        
        Timestamp timestamp = rs.getTimestamp("created_at");
        if (timestamp != null) {
            log.setCreatedAt(timestamp.toLocalDateTime());
        }
        
        return log;
    }

    @Override
    public List<AuditLog> findAll() {
        List<AuditLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM audit_log ORDER BY created_at DESC";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                logs.add(mapResultSetToAuditLog(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("[AUDIT] Erreur findAll: " + e.getMessage());
            e.printStackTrace();
        }
        
        return logs;
    }
    
    @Override
    public long countToday() {
        String sql = "SELECT COUNT(*) FROM audit_log WHERE DATE(created_at) = CURDATE()";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
            
        } catch (SQLException e) {
            System.err.println("[AUDIT] Erreur countToday: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0;
    }
    
    @Override
    public long countByStatus(AuditLog.ActionStatus status) {
        String sql = "SELECT COUNT(*) FROM audit_log WHERE status = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status.name());
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
            
        } catch (SQLException e) {
            System.err.println("[AUDIT] Erreur countByStatus: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0;
    }
    
    @Override
    public int deleteOlderThan(int days) {
        String sql = "DELETE FROM audit_log WHERE created_at < DATE_SUB(NOW(), INTERVAL ? DAY)";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, days);
            
            return stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("[AUDIT] Erreur deleteOlderThan: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0;
    }

}
