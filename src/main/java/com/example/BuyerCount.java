package com.example;

/**
 * Class representing the result of buyer count aggregation for a time window.
 */
public class BuyerCount {
    private String productId;
    private long windowStart;
    private long windowEnd;
    private long uniqueBuyers;
    private double totalAmount;

    // Default constructor for serialization
    public BuyerCount() {
    }

    public BuyerCount(String productId, long windowStart, long windowEnd, long uniqueBuyers, double totalAmount) {
        this.productId = productId;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.uniqueBuyers = uniqueBuyers;
        this.totalAmount = totalAmount;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public long getWindowStart() {
        return windowStart;
    }

    public void setWindowStart(long windowStart) {
        this.windowStart = windowStart;
    }

    public long getWindowEnd() {
        return windowEnd;
    }

    public void setWindowEnd(long windowEnd) {
        this.windowEnd = windowEnd;
    }

    public long getUniqueBuyers() {
        return uniqueBuyers;
    }

    public void setUniqueBuyers(long uniqueBuyers) {
        this.uniqueBuyers = uniqueBuyers;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    @Override
    public String toString() {
        return "BuyerCount{" +
                "productId='" + productId + '\'' +
                ", windowStart=" + windowStart +
                ", windowEnd=" + windowEnd +
                ", uniqueBuyers=" + uniqueBuyers +
                ", totalAmount=" + totalAmount +
                '}';
    }
}
