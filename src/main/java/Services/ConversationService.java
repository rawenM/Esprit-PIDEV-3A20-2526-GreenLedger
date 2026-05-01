package Services;

import Models.*;
import DataBase.MyConnection;
import Utils.EventBusManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Conversation Service — messaging between investors and project holders.
 */
public class ConversationService {

    private final ProjetService projetService;
    private final NotificationService notificationService;

    public ConversationService() {
        this.projetService     = new ProjetService();
        this.notificationService = new NotificationService();
    }

    // ── Thread creation ───────────────────────────────────────────────────

    /**
     * Create a conversation thread after a successful payment.
     * @param projectId   the funded project
     * @param investorId  the investor who paid
     */
    public ConversationThread createThreadForFundedProject(int projectId, long investorId) {
        System.out.println("[Conversation] Creating thread for project " + projectId);

        Projet projet = projetService.getById(projectId);
        if (projet == null) {
            System.err.println("[Conversation] Project not found: " + projectId);
            return null;
        }

        // Check if thread already exists
        ConversationThread existing = getThreadByProjectAndInvestor(projectId, investorId);
        if (existing != null) {
            System.out.println("[Conversation] Thread already exists: " + existing.getId());
            return existing;
        }

        ConversationThread thread = new ConversationThread();
        thread.setProjectId(projectId);
        thread.setInvestisseurId(investorId);
        thread.setPorteurId((long) projet.getEntrepriseId());
        thread.setCreatedAt(LocalDateTime.now());

        Integer threadId = insertThread(thread);
        if (threadId == null) {
            System.err.println("[Conversation] Failed to create thread");
            return null;
        }
        thread.setId(threadId);

        // Welcome message
        sendMessage(threadId, investorId, "Bonjour ! Merci pour votre investissement dans ce projet.");

        System.out.println("[Conversation] Thread created: " + threadId);
        return thread;
    }

    // ── Messaging ─────────────────────────────────────────────────────────

    public ThreadMessage sendMessage(int threadId, long senderId, String content) {
        ConversationThread thread = getThreadById(threadId);
        if (thread == null) return null;

        ThreadMessage message = new ThreadMessage();
        message.setThreadId(threadId);
        message.setSenderId(senderId);
        message.setContent(content);
        message.setSentAt(LocalDateTime.now());
        message.setIsRead(false);

        Integer messageId = insertMessage(message);
        if (messageId == null) return null;
        message.setId(messageId);

        // Broadcast
        EventBusManager.getInstance().post(new MessageSentEvent(message));

        // Notify recipient
        long recipientId = senderId == thread.getInvestisseurId()
            ? thread.getPorteurId()
            : thread.getInvestisseurId();

        notificationService.notify(
            (int) recipientId,
            "new_message",
            "Nouveau message reçu",
            "/messages/" + threadId
        );

        return message;
    }

    // ── Queries ───────────────────────────────────────────────────────────

    public ConversationThread getThreadById(int threadId) {
        String sql = "SELECT * FROM conversation_threads WHERE id = ?";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, threadId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapThread(rs);
            }
        } catch (SQLException e) {
            System.err.println("[Conversation] getThreadById: " + e.getMessage());
        }
        return null;
    }

    public ConversationThread getThreadByProjectAndInvestor(int projectId, long investorId) {
        String sql = "SELECT * FROM conversation_threads WHERE project_id = ? AND investisseur_id = ?";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ps.setLong(2, investorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapThread(rs);
            }
        } catch (SQLException e) {
            System.err.println("[Conversation] getThreadByProjectAndInvestor: " + e.getMessage());
        }
        return null;
    }

    public List<ConversationThread> getThreadsForUser(long userId) {
        String sql = "SELECT * FROM conversation_threads " +
                     "WHERE investisseur_id = ? OR porteur_id = ? ORDER BY created_at DESC";
        List<ConversationThread> threads = new ArrayList<>();
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) threads.add(mapThread(rs));
            }
        } catch (SQLException e) {
            System.err.println("[Conversation] getThreadsForUser: " + e.getMessage());
        }
        return threads;
    }

    public List<ThreadMessage> getMessagesForThread(int threadId) {
        String sql = "SELECT * FROM thread_messages WHERE thread_id = ? ORDER BY sent_at ASC";
        List<ThreadMessage> messages = new ArrayList<>();
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, threadId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) messages.add(mapMessage(rs));
            }
        } catch (SQLException e) {
            System.err.println("[Conversation] getMessagesForThread: " + e.getMessage());
        }
        return messages;
    }

    public boolean markAsRead(int messageId) {
        String sql = "UPDATE thread_messages SET is_read = TRUE WHERE id = ?";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[Conversation] markAsRead: " + e.getMessage());
            return false;
        }
    }

    // ── DB helpers ────────────────────────────────────────────────────────

    private Integer insertThread(ConversationThread thread) {
        String sql = "INSERT INTO conversation_threads (project_id, investisseur_id, porteur_id, created_at) VALUES (?,?,?,?)";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, thread.getProjectId());
            ps.setLong(2, thread.getInvestisseurId());
            ps.setLong(3, thread.getPorteurId());
            ps.setObject(4, thread.getCreatedAt());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[Conversation] insertThread: " + e.getMessage());
        }
        return null;
    }

    private Integer insertMessage(ThreadMessage message) {
        String sql = "INSERT INTO thread_messages (thread_id, sender_id, content, sent_at, is_read) VALUES (?,?,?,?,?)";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, message.getThreadId());
            ps.setLong(2, message.getSenderId());
            ps.setString(3, message.getContent());
            ps.setObject(4, message.getSentAt());
            ps.setBoolean(5, message.getIsRead());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[Conversation] insertMessage: " + e.getMessage());
        }
        return null;
    }

    private ConversationThread mapThread(ResultSet rs) throws SQLException {
        ConversationThread t = new ConversationThread();
        t.setId(rs.getInt("id"));
        t.setProjectId(rs.getInt("project_id"));
        t.setInvestisseurId(rs.getLong("investisseur_id"));
        t.setPorteurId(rs.getLong("porteur_id"));
        t.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return t;
    }

    private ThreadMessage mapMessage(ResultSet rs) throws SQLException {
        ThreadMessage m = new ThreadMessage();
        m.setId(rs.getInt("id"));
        m.setThreadId(rs.getInt("thread_id"));
        m.setSenderId(rs.getLong("sender_id"));
        m.setContent(rs.getString("content"));
        m.setSentAt(rs.getObject("sent_at", LocalDateTime.class));
        m.setIsRead(rs.getBoolean("is_read"));
        return m;
    }

    // ── Event ─────────────────────────────────────────────────────────────

    public static class MessageSentEvent {
        private final ThreadMessage message;
        public MessageSentEvent(ThreadMessage message) { this.message = message; }
        public ThreadMessage getMessage() { return message; }
    }
}
