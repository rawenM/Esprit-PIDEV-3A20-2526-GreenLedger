package Models;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Immutable event record for carbon credit batch lifecycle.
 * Provides blockchain-ready audit trail through SHA-256 hashing and event chaining.
 * 
 * Each event is hashed and linked to the previous event, creating an
 * immutable chain similar to blockchain architecture.
 * 
 * Compliance: ISO 14064, ISO 27001, SOC 2 Type II
 * 
 * @author GreenLedger Traceability Team
 */
public class BatchEvent {
    
    private long id;
    private int batchId;                    // Reference to carbon_credit_batches
    private BatchEventType eventType;
    private String eventDataJson;           // Event-specific data (amounts, wallets, etc.)
    private String eventHash;               // SHA-256 hash of this event
    private String previousEventHash;       // Hash of previous event (creates chain)
    private String actor;                   // User/system that triggered event
    private LocalDateTime createdAt;
    
    private static final Gson gson = new Gson();
    
    // Constructors
    public BatchEvent() {
        this.createdAt = LocalDateTime.now();
    }
    
    public BatchEvent(int batchId, BatchEventType eventType, String eventDataJson, 
                      String previousEventHash, String actor) {
        this();
        this.batchId = batchId;
        this.eventType = eventType;
        this.eventDataJson = eventDataJson;
        this.previousEventHash = previousEventHash;
        this.actor = actor;
        this.eventHash = computeHash();
    }
    
    /**
     * Compute SHA-256 hash of event data for immutability verification.
     * Hash components: batchId + eventType + eventData + previousHash + actor + timestamp
     */
    public String computeHash() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            
            String toHash = String.format("%d|%s|%s|%s|%s|%s",
                batchId,
                eventType != null ? eventType.name() : "",
                eventDataJson != null ? eventDataJson : "",
                previousEventHash != null ? previousEventHash : "GENESIS",
                actor != null ? actor : "",
                createdAt != null ? createdAt.toString() : "");
            
            byte[] hashBytes = digest.digest(toHash.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
            
        } catch (NoSuchAlgorithmException e) {
            System.err.println("SHA-256 algorithm not available: " + e.getMessage());
            return "";
        }
    }
    
    /**
     * Verify if event record is unmodified since creation.
     * Returns false if data has been tampered with.
     */
    public boolean isValid() {
        if (eventHash == null || eventHash.isEmpty()) {
            return false;
        }
        String currentHash = computeHash();
        return eventHash.equals(currentHash);
    }
    
    /**
     * Check if this is the first event in the chain (no previous event)
     */
    public boolean isGenesisEvent() {
        return previousEventHash == null || previousEventHash.isEmpty();
    }
    
    /**
     * Get blockchain-ready payload for export
     */
    public String toBlockchainPayload() {
        JsonObject payload = new JsonObject();
        payload.addProperty("batch_id", batchId);
        payload.addProperty("event_type", eventType.name());
        payload.addProperty("event_hash", eventHash);
        payload.addProperty("previous_hash", previousEventHash != null ? previousEventHash : "GENESIS");
        payload.addProperty("actor", actor);
        payload.addProperty("timestamp", createdAt.toString());
        
        if (eventDataJson != null) {
            try {
                payload.add("event_data", gson.fromJson(eventDataJson, JsonObject.class));
            } catch (Exception e) {
                payload.addProperty("event_data", eventDataJson);
            }
        }
        
        return gson.toJson(payload);
    }
    
    /**
     * Get human-readable event summary
     */
    public String getEventSummary() {
        return String.format(
            "Batch Event [%d]\n" +
            "Batch ID: %d | Type: %s\n" +
            "Actor: %s\n" +
            "Hash: %s\n" +
            "Previous: %s\n" +
            "Integrity: %s\n" +
            "Timestamp: %s",
            id,
            batchId,
            eventType != null ? eventType.name() : "Unknown",
            actor != null ? actor : "System",
            eventHash != null ? eventHash.substring(0, 16) + "..." : "N/A",
            previousEventHash != null ? previousEventHash.substring(0, 16) + "..." : "GENESIS",
            isValid() ? "✓ Valid" : "✗ Tampered",
            createdAt != null ? createdAt.toString() : "Unknown"
        );
    }
    
    /**
     * Parse event data JSON as JsonObject for easy access
     */
    public JsonObject getEventDataAsJson() {
        if (eventDataJson == null || eventDataJson.isEmpty()) {
            return new JsonObject();
        }
        try {
            return gson.fromJson(eventDataJson, JsonObject.class);
        } catch (Exception e) {
            System.err.println("Failed to parse event data JSON: " + e.getMessage());
            return new JsonObject();
        }
    }
    
    // Getters and Setters
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public int getBatchId() {
        return batchId;
    }
    
    public void setBatchId(int batchId) {
        this.batchId = batchId;
    }
    
    public BatchEventType getEventType() {
        return eventType;
    }
    
    public void setEventType(BatchEventType eventType) {
        this.eventType = eventType;
    }
    
    public String getEventDataJson() {
        return eventDataJson;
    }
    
    public void setEventDataJson(String eventDataJson) {
        this.eventDataJson = eventDataJson;
        // Recompute hash when data changes (should only happen before persistence)
        if (this.id == 0) {
            this.eventHash = computeHash();
        }
    }
    
    public String getEventHash() {
        return eventHash;
    }
    
    public void setEventHash(String eventHash) {
        this.eventHash = eventHash;
    }
    
    public String getPreviousEventHash() {
        return previousEventHash;
    }
    
    public void setPreviousEventHash(String previousEventHash) {
        this.previousEventHash = previousEventHash;
    }
    
    public String getActor() {
        return actor;
    }
    
    public void setActor(String actor) {
        this.actor = actor;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BatchEvent that = (BatchEvent) o;
        return id == that.id;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("BatchEvent{id=%d, batchId=%d, type=%s, hash=%s...}",
            id, batchId, eventType, eventHash != null ? eventHash.substring(0, 8) : "null");
    }
}
