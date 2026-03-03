package Models;

import java.sql.Timestamp;

/**
 * Represents a direct peer-to-peer trade between two users
 * with proposal, negotiation, and settlement stages
 */
public class PeerTrade {
    private int id;
    private int initiatorId;  // User proposing the trade
    private int responderId;  // User being proposed to
    private String assetType;  // CARBON_CREDITS or WALLET
    private double quantity;
    private double proposedPriceUsd;
    private Double agreedPriceUsd;  // After negotiation
    private Integer initiatorWalletId;
    private Integer responderWalletId;
    private String stripePaymentId;
    private String status;  // PROPOSED, ACCEPTED, NEGOTIATING, SETTLED, CANCELLED, DISPUTED
    private Integer escrowId;
    private Timestamp createdAt;
    private Timestamp settlementDate;
    private Timestamp updatedAt;

    // Constructors
    public PeerTrade() {}

    public PeerTrade(int initiatorId, int responderId, String assetType,
                     double quantity, double proposedPriceUsd) {
        this.initiatorId = initiatorId;
        this.responderId = responderId;
        this.assetType = assetType;
        this.quantity = quantity;
        this.proposedPriceUsd = proposedPriceUsd;
        this.status = "PROPOSED";
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getInitiatorId() {
        return initiatorId;
    }

    public void setInitiatorId(int initiatorId) {
        this.initiatorId = initiatorId;
    }

    public int getResponderId() {
        return responderId;
    }

    public void setResponderId(int responderId) {
        this.responderId = responderId;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public double getProposedPriceUsd() {
        return proposedPriceUsd;
    }

    public void setProposedPriceUsd(double proposedPriceUsd) {
        this.proposedPriceUsd = proposedPriceUsd;
    }

    public Double getAgreedPriceUsd() {
        return agreedPriceUsd;
    }

    public void setAgreedPriceUsd(Double agreedPriceUsd) {
        this.agreedPriceUsd = agreedPriceUsd;
    }

    public Integer getInitiatorWalletId() {
        return initiatorWalletId;
    }

    public void setInitiatorWalletId(Integer initiatorWalletId) {
        this.initiatorWalletId = initiatorWalletId;
    }

    public Integer getResponderWalletId() {
        return responderWalletId;
    }

    public void setResponderWalletId(Integer responderWalletId) {
        this.responderWalletId = responderWalletId;
    }

    public String getStripePaymentId() {
        return stripePaymentId;
    }

    public void setStripePaymentId(String stripePaymentId) {
        this.stripePaymentId = stripePaymentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getEscrowId() {
        return escrowId;
    }

    public void setEscrowId(Integer escrowId) {
        this.escrowId = escrowId;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(Timestamp settlementDate) {
        this.settlementDate = settlementDate;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Utility methods
    public double getFinalPrice() {
        return agreedPriceUsd != null ? agreedPriceUsd : proposedPriceUsd;
    }

    public double getTotalValue() {
        return quantity * getFinalPrice();
    }

    public boolean isPending() {
        return "PROPOSED".equals(status) || "ACCEPTED".equals(status) || "NEGOTIATING".equals(status);
    }

    public boolean canBeNegotiated() {
        return "PROPOSED".equals(status) || "NEGOTIATING".equals(status);
    }

    public boolean canBeCancelled() {
        return !"SETTLED".equals(status) && !"DISPUTED".equals(status);
    }

    public long getMinutesActive() {
        if (createdAt == null) return 0;
        return (System.currentTimeMillis() - createdAt.getTime()) / 60000;
    }

    public String getStatusDescription() {
        switch (status) {
            case "PROPOSED": return "Trade proposal sent";
            case "ACCEPTED": return "Trade accepted, awaiting payment";
            case "NEGOTIATING": return "Terms being negotiated";
            case "SETTLED": return "Trade completed";
            case "CANCELLED": return "Trade cancelled";
            case "DISPUTED": return "Trade in dispute";
            default: return "Unknown status";
        }
    }

    @Override
    public String toString() {
        return "PeerTrade{" +
                "id=" + id +
                ", initiatorId=" + initiatorId +
                ", responderId=" + responderId +
                ", assetType='" + assetType + '\'' +
                ", quantity=" + quantity +
                ", agreedPrice=$" + agreedPriceUsd +
                ", status='" + status + '\'' +
                '}';
    }
}
