package Models;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Represents a marketplace dispute between buyer and seller
 * Filed when transactions have issues requiring admin resolution
 */
public class MarketplaceDispute {
    private int id;
    private Integer orderId;            // From marketplace_orders
    private Integer tradeId;            // From peer_trades (alternative)
    private int escrowId;               // Associated escrow hold
    private long reporterId;            // User who filed dispute
    private long reportedUserId;        // User being reported
    private String disputeReason;       // Reason category (item_not_received, not_as_described, etc.)
    private String description;         // Detailed explanation from reporter
    private String resolution_type;     // PENDING, REFUND_TO_BUYER, RELEASE_TO_SELLER, SPLIT_FUNDS
    private String adminNotes;          // Admin resolution notes
    private Long resolvedBy;            // Admin who resolved
    private Timestamp resolvedAt;       // When resolved
    private Timestamp createdAt;        // When dispute filed
    private Timestamp updatedAt;

    // Constructors
    public MarketplaceDispute() {}

    public MarketplaceDispute(Integer orderId, Integer tradeId, int escrowId,
                             long reporterId, long reportedUserId,
                             String disputeReason, String description) {
        this.orderId = orderId;
        this.tradeId = tradeId;
        this.escrowId = escrowId;
        this.reporterId = reporterId;
        this.reportedUserId = reportedUserId;
        this.disputeReason = disputeReason;
        this.description = description;
        this.resolution_type = "PENDING";
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public Integer getTradeId() {
        return tradeId;
    }

    public void setTradeId(Integer tradeId) {
        this.tradeId = tradeId;
    }

    public int getEscrowId() {
        return escrowId;
    }

    public void setEscrowId(int escrowId) {
        this.escrowId = escrowId;
    }

    public long getReporterId() {
        return reporterId;
    }

    public void setReporterId(long reporterId) {
        this.reporterId = reporterId;
    }

    public long getReportedUserId() {
        return reportedUserId;
    }

    public void setReportedUserId(long reportedUserId) {
        this.reportedUserId = reportedUserId;
    }

    public String getDisputeReason() {
        return disputeReason;
    }

    public void setDisputeReason(String disputeReason) {
        this.disputeReason = disputeReason;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getResolutionType() {
        return resolution_type;
    }

    public void setResolutionType(String resolutionType) {
        this.resolution_type = resolutionType;
    }

    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }

    public Long getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(Long resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt != null ? resolvedAt.toLocalDateTime() : null;
    }

    public void setResolvedAt(Timestamp resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt != null ? new Timestamp(java.sql.Timestamp.valueOf(resolvedAt).getTime()) : null;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "MarketplaceDispute{" +
                "id=" + id +
                ", orderId=" + orderId +
                ", escrowId=" + escrowId +
                ", reporterId=" + reporterId +
                ", reportedUserId=" + reportedUserId +
                ", disputeReason='" + disputeReason + '\'' +
                ", resolution_type='" + resolution_type + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
