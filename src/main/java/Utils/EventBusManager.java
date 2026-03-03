package Utils;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * Event Bus for decoupled controller communication.
 * 
 * Pattern: Publish-Subscribe for loose coupling
 * Benefits:
 * - Controllers don't need direct references to each other
 * - Easy to add new subscribers without modifying publishers
 * - Testable (can mock event handling)
 * 
 * Events:
 * - WalletUpdatedEvent: When wallet balance/credits change
 * - BatchIssuedEvent: When new batch created
 * - CreditsRetiredEvent: When credits permanently retired
 * - CreditsTransferredEvent: When credits moved between wallets
 * - CalculationCompletedEvent: When emission calculation finishes
 * - MapProjectSelectedEvent: When user clicks project on map
 * 
 * @author GreenLedger Team
 */
public class EventBusManager {
    
    private static final EventBus eventBus = new EventBus("GreenWalletEventBus");
    
    /**
     * Register subscriber to receive events.
     * Subscriber methods must be annotated with @Subscribe.
     * 
     * @param subscriber Object with @Subscribe methods
     */
    public static void register(Object subscriber) {
        try {
            eventBus.register(subscriber);
            System.out.println("[EVENT BUS] Registered: " + subscriber.getClass().getSimpleName());
        } catch (Exception e) {
            System.err.println("⚠️  [EVENT BUS] Failed to register " + 
                subscriber.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Unregister subscriber.
     * 
     * @param subscriber Previously registered object
     */
    public static void unregister(Object subscriber) {
        try {
            eventBus.unregister(subscriber);
            System.out.println("[EVENT BUS] Unregistered: " + subscriber.getClass().getSimpleName());
        } catch (Exception e) {
            // Ignore - object may not have been registered
        }
    }
    
    /**
     * Post event to all registered subscribers.
     * Subscribers with matching @Subscribe methods will be notified.
     * 
     * @param event Event object
     */
    public static void post(Object event) {
        eventBus.post(event);
        System.out.println("📡 [EVENT BUS] Posted: " + event.getClass().getSimpleName());
    }
    
    // Event Classes
    
    /**
     * Fired when wallet data changes (balance, credits, etc.)
     */
    public static class WalletUpdatedEvent {
        public final int walletId;
        public final String updateType; // "BALANCE_CHANGED", "CREDITS_ISSUED", etc.
        public final double newBalance;
        public final Models.Wallet wallet;
        
        public WalletUpdatedEvent(int walletId, String updateType, double newBalance) {
            this.walletId = walletId;
            this.updateType = updateType;
            this.newBalance = newBalance;
            this.wallet = null;
        }
        
        // Constructor with wallet object for full event
        public WalletUpdatedEvent(Models.Wallet wallet) {
            this.wallet = wallet;
            this.walletId = wallet != null ? wallet.getId() : 0;
            this.updateType = "BALANCE_CHANGED";
            this.newBalance = wallet != null ? wallet.getTotalCredits() : 0.0;
        }
        
        // Getter for wallet
        public Models.Wallet getWallet() {
            return wallet;
        }
    }
    
    /**
     * Fired when new carbon credit batch is issued.
     */
    public static class BatchIssuedEvent {
        public final int batchId;
        public final int walletId;
        public final int projectId;
        public final double amount;
        public final String serialNumber;
        
        public BatchIssuedEvent(int batchId, int walletId, int projectId, 
                                double amount, String serialNumber) {
            this.batchId = batchId;
            this.walletId = walletId;
            this.projectId = projectId;
            this.amount = amount;
            this.serialNumber = serialNumber;
        }
        
        // Getter for backward compatibility with controller code
        public BatchIssuedEvent getBatch() {
            return this;
        }
        
        // Accessor for serial number
        public String getSerialNumber() {
            return serialNumber;
        }
    }
    
    /**
     * Fired when credits are retired (permanently offset).
     */
    public static class CreditsRetiredEvent {
        public final int walletId;
        public final double amount;
        public final String reason;
        public final int[] batchIdsAffected;
        
        public CreditsRetiredEvent(int walletId, double amount, String reason, int[] batchIds) {
            this.walletId = walletId;
            this.amount = amount;
            this.reason = reason;
            this.batchIdsAffected = batchIds;
        }
        
        // Getter for amount
        public double getAmount() {
            return amount;
        }
    }
    
    /**
     * Fired when credits transferred between wallets.
     */
    public static class CreditsTransferredEvent {
        public final int sourceWalletId;
        public final int destinationWalletId;
        public final double amount;
        public final String reason;
        
        public CreditsTransferredEvent(int sourceWalletId, int destinationWalletId, 
                                       double amount, String reason) {
            this.sourceWalletId = sourceWalletId;
            this.destinationWalletId = destinationWalletId;
            this.amount = amount;
            this.reason = reason;
        }
    }
    
    /**
     * Fired when emission calculation completes.
     */
    public static class CalculationCompletedEvent {
        public final String calculationId;
        public final double co2eResult;
        public final int tier;
        public final String activityDescription;
        
        public CalculationCompletedEvent(String calculationId, double co2eResult, 
                                         int tier, String activityDescription) {
            this.calculationId = calculationId;
            this.co2eResult = co2eResult;
            this.tier = tier;
            this.activityDescription = activityDescription;
        }
        
        // Getter for result (returns this for method chaining compatibility)
        public CalculationCompletedEvent getResult() {
            return this;
        }
        
        // Convenience method
        public double getCo2eAmount() {
            return co2eResult;
        }
    }
    
    /**
     * Fired when user clicks project on map.
     */
    public static class MapProjectSelectedEvent {
        public final int projectId;
        public final String projectName;
        
        public MapProjectSelectedEvent(int projectId, String projectName) {
            this.projectId = projectId;
            this.projectName = projectName;
        }
    }
    
    /**
     * Fired to request UI refresh (e.g., after data import).
     */
    public static class RefreshRequestedEvent {
        public final String component; // "DASHBOARD", "TRANSACTIONS", "BATCHES", "ALL"
        
        public RefreshRequestedEvent(String component) {
            this.component = component;
        }
    }
    
    /**
     * Fired to show error/notification in UI.
     */
    public static class NotificationEvent {
        public enum Type { SUCCESS, ERROR, WARNING, INFO }
        
        public final Type type;
        public final String message;
        public final String details;
        
        public NotificationEvent(Type type, String message, String details) {
            this.type = type;
            this.message = message;
            this.details = details;
        }
        
        public NotificationEvent(Type type, String message) {
            this(type, message, null);
        }
    }
}
