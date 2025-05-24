package com.example;

import org.apache.flink.streaming.api.functions.source.RichSourceFunction;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * A SourceFunction that generates a stream of random BuyerEvents.
 */
public class BuyerEventSourceFunction extends RichSourceFunction<BuyerEvent> {
    private volatile boolean isRunning = true;
    private final Random random = new Random();
    
    // Simulate a pool of buyers and products
    private final String[] buyerIds = new String[1000];
    private final String[] productIds = new String[5];
    
    public BuyerEventSourceFunction() {
        // Initialize buyer IDs
        for (int i = 0; i < buyerIds.length; i++) {
            buyerIds[i] = "buyer-" + (i + 1);
        }
        
        // Initialize product IDs
        for (int i = 0; i < productIds.length; i++) {
            productIds[i] = "product-" + (i + 1);
        }
    }

    @Override
    public void run(SourceContext<BuyerEvent> ctx) throws Exception {
        while (isRunning) {
            // Select a random buyer and product
            String buyerId = buyerIds[random.nextInt(buyerIds.length)];
            String productId = productIds[random.nextInt(productIds.length)];
            
            // Generate a random purchase amount between 5.0 and 500.0
            double amount = 5.0 + (random.nextDouble() * 495.0);
            
            // Create and emit the buyer event
            BuyerEvent event = new BuyerEvent(buyerId, productId, amount);
            ctx.collect(event);
            
            // Sleep for a random duration between 50ms and 200ms
            TimeUnit.MILLISECONDS.sleep(1 + random.nextInt(3));
        }
    }

    @Override
    public void cancel() {
        isRunning = false;
    }
}
