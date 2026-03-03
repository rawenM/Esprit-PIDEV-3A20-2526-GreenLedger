package Controllers;

import Models.BatchEvent;
import Models.BatchEventType;
import Models.CarbonCreditBatch;
import Services.BatchEventService;
import Services.WalletService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Controller for batch traceability operations.
 * Provides APIs for querying batch lineage, provenance, events, and validation.
 * 
 * This controller acts as a facade layer between the UI and the traceability services,
 * providing simplified access to complex batch tracking operations.
 * 
 * @author GreenLedger Traceability Team
 */
public class BatchTraceabilityController {

    private final WalletService walletService;
    private final BatchEventService eventService;
    private final Gson gson;

    public BatchTraceabilityController() {
        this.walletService = new WalletService();
        this.eventService = new BatchEventService();
        this.gson = new Gson();
    }

    /**
     * Get full batch lineage (parent-child tree).
     * 
     * @param batchId The batch ID
     * @return List of batches in lineage
     */
    public List<CarbonCreditBatch> getBatchLineage(int batchId) {
        return walletService.getBatchLineage(batchId);
    }

    /**
     * Get complete batch provenance (emission calculation -> current state).
     * 
     * @param batchId The batch ID
     * @return JSON string with complete provenance data
     */
    public String getBatchProvenance(int batchId) {
        return walletService.getBatchProvenance(batchId);
    }

    /**
     * Get provenance as parsed object for UI consumption.
     * 
     * @param batchId The batch ID
     * @return Map containing provenance data
     */
    public Map<String, Object> getBatchProvenanceAsMap(int batchId) {
        String json = walletService.getBatchProvenance(batchId);
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            Map<String, Object> result = new HashMap<>();
            
            // Extract key fields for easy UI access
            if (obj.has("batch")) {
                result.put("batch", obj.get("batch"));
            }
            if (obj.has("emission_calculation")) {
                result.put("emissionCalculation", obj.get("emission_calculation"));
            }
            if (obj.has("events")) {
                result.put("events", obj.get("events"));
            }
            if (obj.has("lineage")) {
                result.put("lineage", obj.get("lineage"));
            }
            if (obj.has("batch_type")) {
                result.put("batchType", obj.get("batch_type").getAsString());
            }
            if (obj.has("chain_valid")) {
                result.put("chainValid", obj.get("chain_valid").getAsBoolean());
            }
            if (obj.has("retirement_percentage")) {
                result.put("retirementPercentage", obj.get("retirement_percentage").getAsDouble());
            }
            
            return result;
        } catch (Exception e) {
            System.err.println("Error parsing provenance JSON: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Get all events for a batch.
     * 
     * @param batchId The batch ID
     * @return List of events
     */
    public List<BatchEvent> getBatchEvents(int batchId) {
        return eventService.getBatchEvents(batchId);
    }

    /**
     * Get batches for a wallet with lineage information.
     * 
     * @param walletId The wallet ID
     * @return List of batches
     */
    public List<CarbonCreditBatch> getWalletBatchesWithLineage(int walletId) {
        return walletService.getWalletBatches(walletId);
    }

    /**
     * Validate event chain integrity for a batch.
     * 
     * @param batchId The batch ID
     * @return true if chain is valid, false if tampered
     */
    public boolean validateEventChain(int batchId) {
        return eventService.validateEventChain(batchId);
    }

    /**
     * Export batch events to blockchain-ready format.
     * 
     * @param batchId The batch ID
     * @return JSON string in blockchain format
     */
    public String exportToBlockchainFormat(int batchId) {
        return eventService.exportToBlockchainFormat(batchId);
    }

    /**
     * Get batch details by ID.
     * 
     * @param batchId The batch ID
     * @return Batch object, or null if not found
     */
    public CarbonCreditBatch getBatchDetails(int batchId) {
        return walletService.getBatchById(batchId);
    }

    /**
     * Get child batches (batches split from parent).
     * 
     * @param parentBatchId The parent batch ID
     * @return List of child batches
     */
    public List<CarbonCreditBatch> getChildBatches(int parentBatchId) {
        return walletService.getChildBatches(parentBatchId);
    }

    /**
     * Split a batch into child batches.
     * 
     * @param batchId ID of batch to split
     * @param amountToSplit Amount to split off
     * @param targetWalletId Destination wallet
     * @param actor User performing the split
     * @return ID of new child batch, or -1 if failed
     */
    public int splitBatch(int batchId, double amountToSplit, int targetWalletId, String actor) {
        return walletService.splitBatch(batchId, amountToSplit, targetWalletId, actor);
    }

    /**
     * Get genesis event (first event) for a batch.
     * 
     * @param batchId The batch ID
     * @return Genesis event, or null if no events
     */
    public BatchEvent getGenesisEvent(int batchId) {
        return eventService.getGenesisEvent(batchId);
    }

    /**
     * Get event count for a batch.
     * 
     * @param batchId The batch ID
     * @return Number of events
     */
    public int getEventCount(int batchId) {
        return eventService.getEventCount(batchId);
    }

    /**
     * Get events by type for a batch.
     * 
     * @param batchId The batch ID
     * @param eventType The event type
     * @return List of events
     */
    public List<BatchEvent> getBatchEventsByType(int batchId, BatchEventType eventType) {
        return eventService.getBatchEventsByType(batchId, eventType);
    }

    /**
     * Get recent events across all batches (for dashboard).
     * 
     * @param limit Maximum number of events
     * @return List of recent events
     */
    public List<BatchEvent> getRecentEvents(int limit) {
        return eventService.getRecentEvents(limit);
    }

    /**
     * Get batch traceability summary for UI display.
     * 
     * @param batchId The batch ID
     * @return Summary object with key metrics
     */
    public BatchTraceabilitySummary getBatchSummary(int batchId) {
        CarbonCreditBatch batch = walletService.getBatchById(batchId);
        if (batch == null) {
            return null;
        }

        List<BatchEvent> events = eventService.getBatchEvents(batchId);
        boolean chainValid = eventService.validateEventChain(batchId);
        List<CarbonCreditBatch> lineage = walletService.getBatchLineage(batchId);
        List<CarbonCreditBatch> children = walletService.getChildBatches(batchId);

        return new BatchTraceabilitySummary(
            batch,
            events.size(),
            chainValid,
            lineage.size(),
            children.size(),
            batch.getParentBatchId() != null,
            batch.isPrimary()
        );
    }

    /**
     * Inner class for batch traceability summary.
     */
    public static class BatchTraceabilitySummary {
        private final CarbonCreditBatch batch;
        private final int eventCount;
        private final boolean chainValid;
        private final int lineageSize;
        private final int childrenCount;
        private final boolean hasParent;
        private final boolean isPrimary;

        public BatchTraceabilitySummary(CarbonCreditBatch batch, int eventCount, boolean chainValid,
                                       int lineageSize, int childrenCount, boolean hasParent, boolean isPrimary) {
            this.batch = batch;
            this.eventCount = eventCount;
            this.chainValid = chainValid;
            this.lineageSize = lineageSize;
            this.childrenCount = childrenCount;
            this.hasParent = hasParent;
            this.isPrimary = isPrimary;
        }

        // Getters
        public CarbonCreditBatch getBatch() { return batch; }
        public int getEventCount() { return eventCount; }
        public boolean isChainValid() { return chainValid; }
        public int getLineageSize() { return lineageSize; }
        public int getChildrenCount() { return childrenCount; }
        public boolean hasParent() { return hasParent; }
        public boolean isPrimary() { return isPrimary; }

        public String getSummaryText() {
            return String.format(
                "Batch: %s | Type: %s | Events: %d | Lineage: %d batches | Children: %d | Chain: %s",
                batch.getSerialNumber(),
                isPrimary ? "PRIMARY" : "SECONDARY",
                eventCount,
                lineageSize,
                childrenCount,
                chainValid ? "✓ Valid" : "✗ Invalid"
            );
        }
    }
}
