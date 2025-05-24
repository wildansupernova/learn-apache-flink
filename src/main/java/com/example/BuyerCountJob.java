package com.example;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple1;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

public class BuyerCountJob {
    private static final Logger LOG = LoggerFactory.getLogger(BuyerCountJob.class);

    public static void main(String[] args) throws Exception {
        // Set up the execution environment
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        
        // Create a data source that generates BuyerEvents
        DataStream<BuyerEvent> inputStream = env.addSource(new BuyerEventSourceFunction())
                .name("Buyer-Event-Source");
        
        LOG.info("Starting Buyer Count Job...");
        
        // Add timestamp and watermark information to the stream
        DataStream<BuyerEvent> timestampedStream = inputStream
                .assignTimestampsAndWatermarks(
                    WatermarkStrategy
                        .<BuyerEvent>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                        .withTimestampAssigner((event, timestamp) -> event.getTimestamp())
                );
        
        // Process the events in a 30-second sliding window with 2-second slide, grouped by productId
        DataStream<BuyerCount> resultStream = timestampedStream
            .keyBy(event -> event.getProductId()) // Group by product ID
            .window(SlidingEventTimeWindows.of(Time.seconds(350), Time.seconds(60)))
            .aggregate(new BuyerAggregator(), new BuyerWindowFunction())
            .name("Buyer-Counting-Window");
        
        // Add a sink to print results to console
        resultStream.print().name("Console-Output");
        
        // Add a sink to write results to SQLite
        // Try multiple possible paths to ensure one will work
        String dbPath = System.getenv("SQLITE_DB_PATH"); // Environment variable has priority
        
        if (dbPath == null || dbPath.isEmpty()) {
            // Try different paths in order of preference
            String[] possiblePaths = {
                "/opt/flink/data/buyer_counts.db",      // Standard container path
                "./data/buyer_counts.db",               // Relative path
                System.getProperty("java.io.tmpdir") + "/buyer_counts.db"  // Temp directory as last resort
            };
            
            for (String path : possiblePaths) {
                File dbFile = new File(path);
                File parentDir = dbFile.getParentFile();
                
                if (parentDir != null && (parentDir.exists() || parentDir.mkdirs()) && parentDir.canWrite()) {
                    dbPath = path;
                    LOG.info("Selected database path: {}", dbPath);
                    break;
                }
            }
            
            // If still null, default to in-memory
            if (dbPath == null) {
                dbPath = ":memory:";
                LOG.warn("Could not find writable location, using in-memory database");
            }
        }
        
        LOG.info("Using SQLite database path: {}", dbPath);
        resultStream.addSink(new SQLiteSink(dbPath))
                   .name("SQLite-Sink");
        
        // Execute the job
        env.execute("Product-Based Buyer Count Sliding Window Job (30s window, 2s slide)");
    }
    
    /**
     * Aggregator that counts unique buyers and sums up the total amount.
     */
    private static class BuyerAggregator implements AggregateFunction<BuyerEvent, BuyerCountAccumulator, BuyerCountResult> {
        @Override
        public BuyerCountAccumulator createAccumulator() {
            return new BuyerCountAccumulator();
        }

        @Override
        public BuyerCountAccumulator add(BuyerEvent event, BuyerCountAccumulator accumulator) {
            accumulator.productId = event.getProductId(); // Store the product ID
            accumulator.uniqueBuyers.add(event.getBuyerId());
            accumulator.totalAmount += event.getAmount();
            return accumulator;
        }

        @Override
        public BuyerCountResult getResult(BuyerCountAccumulator accumulator) {
            return new BuyerCountResult(
                    accumulator.productId,
                    accumulator.uniqueBuyers.size(),
                    accumulator.totalAmount
            );
        }

        @Override
        public BuyerCountAccumulator merge(BuyerCountAccumulator a, BuyerCountAccumulator b) {
            BuyerCountAccumulator result = new BuyerCountAccumulator();
            result.productId = a.productId; // Use product ID from first accumulator
            result.uniqueBuyers.addAll(a.uniqueBuyers);
            result.uniqueBuyers.addAll(b.uniqueBuyers);
            result.totalAmount = a.totalAmount + b.totalAmount;
            return result;
        }
    }
    
    /**
     * Process window function to convert the aggregation result to BuyerCount.
     */
    private static class BuyerWindowFunction extends ProcessWindowFunction<BuyerCountResult, BuyerCount, String, TimeWindow> {
        @Override
        public void process(
                String productId, // Key is now the product ID
                Context context,
                Iterable<BuyerCountResult> elements,
                Collector<BuyerCount> out) {
            BuyerCountResult result = elements.iterator().next();
            out.collect(new BuyerCount(
                    result.productId, // Include product ID in the result
                    context.window().getStart(),
                    context.window().getEnd(),
                    result.uniqueBuyers,
                    result.totalAmount
            ));
        }
    }
    
    /**
     * Accumulator class to hold the state during aggregation.
     */
    private static class BuyerCountAccumulator {
        public String productId;
        public Set<String> uniqueBuyers = new HashSet<>();
        public double totalAmount = 0.0;
    }
    
    /**
     * Result of the aggregation.
     */
    private static class BuyerCountResult {
        public String productId;
        public long uniqueBuyers;
        public double totalAmount;
        
        public BuyerCountResult(String productId, long uniqueBuyers, double totalAmount) {
            this.productId = productId;
            this.uniqueBuyers = uniqueBuyers;
            this.totalAmount = totalAmount;
        }
    }
}
