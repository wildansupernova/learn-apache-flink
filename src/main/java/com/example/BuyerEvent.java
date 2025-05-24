package com.example;

import java.util.Date;

/**
 * Simple Buyer event class representing a purchase made by a buyer.
 */
public class BuyerEvent {
    private String buyerId;
    private String productId;
    private double amount;
    private long timestamp;

    // Default constructor for serialization
    public BuyerEvent() {
    }

    public BuyerEvent(String buyerId, String productId, double amount) {
        this.buyerId = buyerId;
        this.productId = productId;
        this.amount = amount;
        this.timestamp = System.currentTimeMillis();
    }

    public BuyerEvent(String buyerId, String productId, double amount, long timestamp) {
        this.buyerId = buyerId;
        this.productId = productId;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public String getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(String buyerId) {
        this.buyerId = buyerId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "BuyerEvent{" +
                "buyerId='" + buyerId + '\'' +
                ", productId='" + productId + '\'' +
                ", amount=" + amount +
                ", timestamp=" + new Date(timestamp) +
                '}';
    }
}
