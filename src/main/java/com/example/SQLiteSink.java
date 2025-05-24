package com.example;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A custom sink function to write BuyerCount data to SQLite database.
 */
public class SQLiteSink extends RichSinkFunction<BuyerCount> {
    private static final Logger LOG = LoggerFactory.getLogger(SQLiteSink.class);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    private final String dbPath;
    private transient Connection connection;
    private transient PreparedStatement preparedStatement;
    
    public SQLiteSink(String dbPath) {
        this.dbPath = dbPath;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        
        // Ensure the database directory exists
        File dbFile = new File(dbPath);
        File parentDir = dbFile.getParentFile();
        
        LOG.info("Using SQLite database path: {}", dbPath);
        
        // Try to create the directory with liberal permissions
        try {
            if (!parentDir.exists()) {
                LOG.info("Creating directory: {}", parentDir.getAbsolutePath());
                boolean created = parentDir.mkdirs();
                if (!created) {
                    LOG.warn("Could not create directory: {}", parentDir.getAbsolutePath());
                }
                
                // Set permissions to allow anyone to write (helpful in Docker)
                new ProcessBuilder("chmod", "777", parentDir.getAbsolutePath())
                    .redirectErrorStream(true)
                    .start()
                    .waitFor();
                
                LOG.info("Set permissions on directory: {}", parentDir.getAbsolutePath());
            }
        } catch (Exception e) {
            LOG.warn("Error setting up database directory: {}", e.getMessage());
        }
        
        // Initialize the database connection
        try {
            LOG.info("Attempting to connect to SQLite database at: {}", dbPath);
            
            // Print some debug info
            LOG.info("Database file exists: {}", dbFile.exists());
            LOG.info("Database file parent directory: {}", parentDir.getAbsolutePath());
            LOG.info("Parent directory exists: {}", parentDir.exists());
            LOG.info("Parent directory can write: {}", parentDir.canWrite());
            
            Class.forName("org.sqlite.JDBC");
            
            // First try with in-memory as it will always work
            String connectionUrl = "jdbc:sqlite::memory:";
            connection = DriverManager.getConnection(connectionUrl);
            LOG.info("Connected to in-memory SQLite database as a fallback");
            
            // Then try to use the file-based database
            try {
                connection.close();
                connectionUrl = "jdbc:sqlite:" + dbPath;
                connection = DriverManager.getConnection(connectionUrl);
                LOG.info("Successfully connected to file-based SQLite database");
            } catch (SQLException e) {
                LOG.error("Error connecting to file-based SQLite database: {}", e.getMessage());
                LOG.info("Using in-memory SQLite database instead");
                connectionUrl = "jdbc:sqlite::memory:";
                connection = DriverManager.getConnection(connectionUrl);
            }
            
            // Create the table if it doesn't exist
            try (Statement statement = connection.createStatement()) {
                statement.execute(
                    "CREATE TABLE IF NOT EXISTS buyer_counts (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "product_id TEXT, " +
                    "window_start TEXT, " +
                    "window_end TEXT, " +
                    "unique_buyers INTEGER, " +
                    "total_amount REAL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            }
            
            // Prepare the insert statement
            preparedStatement = connection.prepareStatement(
                "INSERT INTO buyer_counts (product_id, window_start, window_end, unique_buyers, total_amount) " +
                "VALUES (?, ?, ?, ?, ?)");
                
            LOG.info("SQLite connection established successfully to: {}", connectionUrl);
        } catch (Exception e) {
            LOG.error("Failed to initialize SQLite connection", e);
            throw e;
        }
    }

    @Override
    public void invoke(BuyerCount value, Context context) throws Exception {
        try {
            // Convert timestamps to readable date format
            String windowStart = DATE_FORMAT.format(new Date(value.getWindowStart()));
            String windowEnd = DATE_FORMAT.format(new Date(value.getWindowEnd()));
            
            preparedStatement.setString(1, value.getProductId());
            preparedStatement.setString(2, windowStart);
            preparedStatement.setString(3, windowEnd);
            preparedStatement.setLong(4, value.getUniqueBuyers());
            preparedStatement.setDouble(5, value.getTotalAmount());
            
            preparedStatement.executeUpdate();
            LOG.info("Inserted buyer count record: {}", value);
        } catch (SQLException e) {
            LOG.error("Error writing to SQLite", e);
            throw e;
        }
    }

    @Override
    public void close() throws Exception {
        if (preparedStatement != null) {
            preparedStatement.close();
        }
        if (connection != null) {
            connection.close();
        }
        super.close();
    }
}
