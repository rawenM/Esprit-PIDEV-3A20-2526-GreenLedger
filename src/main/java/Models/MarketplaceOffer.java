package Models;

import java.sql.Timestamp;

/**
 * Represents a negotiation offer for a marketplace listing.
 */
public class MarketplaceOffer {
    private int id;
    private int listingId;
    private long buyerId;
    private long sellerId;
    private double quantity;
    private double offerPriceUsd;
    private String status;
    private Double counterPriceUsd;
    private Timestamp expiresAt;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getListingId() {
        return listingId;
    }

    public void setListingId(int listingId) {
        this.listingId = listingId;
    }

    public long getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(long buyerId) {
        this.buyerId = buyerId;
    }

    public long getSellerId() {
        return sellerId;
    }

    public void setSellerId(long sellerId) {
        this.sellerId = sellerId;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public double getOfferPriceUsd() {
        return offerPriceUsd;
    }

    public void setOfferPriceUsd(double offerPriceUsd) {
        this.offerPriceUsd = offerPriceUsd;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getCounterPriceUsd() {
        return counterPriceUsd;
    }

    public void setCounterPriceUsd(Double counterPriceUsd) {
        this.counterPriceUsd = counterPriceUsd;
    }

    public Timestamp getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Timestamp expiresAt) {
        this.expiresAt = expiresAt;
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
}
