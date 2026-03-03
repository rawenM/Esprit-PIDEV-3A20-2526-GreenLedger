package Services;

import DataBase.MyConnection;
import Models.BatchEvent;
import Models.BatchEventType;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service layer for batch event operations.
 * Manages immutable event log for carbon credit batch lifecycle with blockchain-ready hashing.
 * 
 * Features:
 * - Event chaining (each event links to previous via hash)
 * - Tamper detection through hash verification
 * - Blockchain-ready export format
 * - Complete audit trail for compliance
 * 
 * @author GreenLedger Traceability Team
 */
public class BatchEventService {

    private Connection conn;
    private static final Gson gson = new Gson();

    public BatchEventService() {
        this.conn = MyConnection.getConnection();
    }

    /**
     * Record a new event in a batch's lifecycle.
     * Automatically chains to previous event via hash.
     * 
     * @param batchId The batch this event belongs to
     * @param eventType Type of event (ISSUED, SPLIT, TRANSFERRED, etc.)
     * @param eventData Event-specific data as JsonObject
     * @param actor User or system that triggered the event
     * @return The created BatchEvent, or null if failed
     */
    public BatchEvent recordEvent(int batchId, BatchEventType eventType, JsonObject eventData, String actor) {
        try {
            // Get the hash of the previous event to create chain
            String previousHash = getLatestEventHash(batchId);
            
            // Create the event object
            BatchEvent event = new BatchEvent(
                batchId,
                eventType,
                gson.toJson(eventData),
                previousHash,
                actor
            );
            
            // Insert into database
            String sql = "INSERT INTO batch_events " +
                        "(batch_id, event_type, event_data_json, event_hash, previous_event_hash, actor) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, event.getBatchId());
                ps.setString(2, event.getEventType().name());
                ps.setString(3, event.getEventDataJson());
                ps.setString(4, event.getEventHash());
                ps.setString(5, event.getPreviousEventHash());
                ps.setString(6, event.getActor());
                
                ps.executeUpdate();
                
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    event.setId(rs.getLong(1));
                    return event;
                }
            }
        } catch (SQLException ex) {
            System.err.println("Error recording batch event: " + ex.getMessage());
            ex.printStackTrace();
        }
        return null;
    }
    
    /**
     * Get the hash of the latest event for a batch.
     * Used to chain new events.
     * 
     * @param batchId The batch ID
     * @return The latest event hash, or null if no events exist
     */
    private String getLatestEventHash(int batchId) {
        String sql = "SELECT event_hash FROM batch_events " +
                    "WHERE batch_id = ? ORDER BY created_at DESC, id DESC LIMIT 1";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, batchId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("event_hash");
            }
        } catch (SQLException ex) {
            System.err.println("Error getting latest event hash: " + ex.getMessage());
        }
        return null;
    }
    
    /**
     * Get all events for a specific batch, ordered chronologically.
     * 
     * @param batchId The batch ID
     * @return List of events
     */
    public List<BatchEvent> getBatchEvents(int batchId) {
        List<BatchEvent> events = new ArrayList<>();
        String sql = "SELECT * FROM batch_events WHERE batch_id = ? ORDER BY created_at ASC, id ASC";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, batchId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                events.add(mapResultSetToEvent(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Error fetching batch events: " + ex.getMessage());
        }
        return events;
    }
    
    /**
     * Get all events for a specific batch and event type.
     * 
     * @param batchId The batch ID
     * @param eventType The event type to filter by
     * @return List of matching events
     */
    public List<BatchEvent> getBatchEventsByType(int batchId, BatchEventType eventType) {
        List<BatchEvent> events = new ArrayList<>();
        String sql = "SELECT * FROM batch_events WHERE batch_id = ? AND event_type = ? " +
                    "ORDER BY created_at ASC, id ASC";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, batchId);
            ps.setString(2, eventType.name());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                events.add(mapResultSetToEvent(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Error fetching batch events by type: " + ex.getMessage());
        }
        return events;
    }
    
    /**
     * Validate the integrity of the event chain for a batch.
     * Checks that:
     * 1. Each event's hash is valid
     * 2. Each event's previous_hash links correctly to the prior event
     * 
     * @param batchId The batch ID to validate
     * @return true if chain is intact, false if tampered
     */
    public boolean validateEventChain(int batchId) {
        List<BatchEvent> events = getBatchEvents(batchId);
        
        if (events.isEmpty()) {
            return true; // No events = valid (nothing to tamper)
        }
        
        String expectedPrevHash = null;
        
        for (BatchEvent event : events) {
            // Verify event's own hash
            if (!event.isValid()) {
                System.err.println("Event " + event.getId() + " has invalid hash (tampered)");
                return false;
            }
            
            // Verify chain linkage
            String actualPrevHash = event.getPreviousEventHash();
            if (!equals(expectedPrevHash, actualPrevHash)) {
                System.err.println("Event " + event.getId() + " has broken chain link");
                System.err.println("Expected previous: " + expectedPrevHash);
                System.err.println("Actual previous: " + actualPrevHash);
                return false;
            }
            
            // Set up for next iteration
            expectedPrevHash = event.getEventHash();
        }
        
        return true;
    }
    
    /**
     * Export batch events to blockchain-ready format (JSON).
     * 
     * @param batchId The batch ID
     * @return JSON string containing all events in blockchain format
     */
    public String exportToBlockchainFormat(int batchId) {
        List<BatchEvent> events = getBatchEvents(batchId);
        
        JsonObject export = new JsonObject();
        export.addProperty("batch_id", batchId);
        export.addProperty("event_count", events.size());
        export.addProperty("chain_valid", validateEventChain(batchId));
        export.addProperty("export_timestamp", LocalDateTime.now().toString());
        
        // Add all events as array
        StringBuilder eventsJson = new StringBuilder("[");
        for (int i = 0; i < events.size(); i++) {
            if (i > 0) eventsJson.append(",");
            eventsJson.append(events.get(i).toBlockchainPayload());
        }
        eventsJson.append("]");
        
        export.add("events", gson.fromJson(eventsJson.toString(), com.google.gson.JsonArray.class));
        
        return gson.toJson(export);
    }
    
    /**
     * Get event count for a batch.
     * 
     * @param batchId The batch ID
     * @return Number of events
     */
    public int getEventCount(int batchId) {
        String sql = "SELECT COUNT(*) FROM batch_events WHERE batch_id = ?";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, batchId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            System.err.println("Error counting events: " + ex.getMessage());
        }
        return 0;
    }
    
    /**
     * Get the genesis event (first event) for a batch.
     * 
     * @param batchId The batch ID
     * @return The first event, or null if no events
     */
    public BatchEvent getGenesisEvent(int batchId) {
        String sql = "SELECT * FROM batch_events WHERE batch_id = ? " +
                    "ORDER BY created_at ASC, id ASC LIMIT 1";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, batchId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToEvent(rs);
            }
        } catch (SQLException ex) {
            System.err.println("Error fetching genesis event: " + ex.getMessage());
        }
        return null;
    }
    
    /**
     * Get events by actor (user or system).
     * 
     * @param actor The actor name
     * @return List of events
     */
    public List<BatchEvent> getEventsByActor(String actor) {
        List<BatchEvent> events = new ArrayList<>();
        String sql = "SELECT * FROM batch_events WHERE actor = ? ORDER BY created_at DESC";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, actor);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                events.add(mapResultSetToEvent(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Error fetching events by actor: " + ex.getMessage());
        }
        return events;
    }
    
    /**
     * Get recent events across all batches (for audit dashboard).
     * 
     * @param limit Maximum number of events to return
     * @return List of recent events
     */
    public List<BatchEvent> getRecentEvents(int limit) {
        List<BatchEvent> events = new ArrayList<>();
        String sql = "SELECT * FROM batch_events ORDER BY created_at DESC LIMIT ?";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                events.add(mapResultSetToEvent(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Error fetching recent events: " + ex.getMessage());
        }
        return events;
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Map ResultSet to BatchEvent model.
     */
    private BatchEvent mapResultSetToEvent(ResultSet rs) throws SQLException {
        BatchEvent event = new BatchEvent();
        event.setId(rs.getLong("id"));
        event.setBatchId(rs.getInt("batch_id"));
        event.setEventType(BatchEventType.valueOf(rs.getString("event_type")));
        event.setEventDataJson(rs.getString("event_data_json"));
        event.setEventHash(rs.getString("event_hash"));
        event.setPreviousEventHash(rs.getString("previous_event_hash"));
        event.setActor(rs.getString("actor"));
        
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            event.setCreatedAt(ts.toLocalDateTime());
        }
        
        return event;
    }
    
    /**
     * Null-safe string comparison.
     */
    private boolean equals(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}
