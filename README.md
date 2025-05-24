# Learn Apache Flink

This project demonstrates a simple use case for Apache Flink using Docker. It counts the number of unique buyers within a sliding time window of 1 minute and stores the results in a SQLite database.

## Project Structure

- `src/main/java/com/example/`: Java source files
  - `BuyerEvent.java`: Data model for buyer events
  - `BuyerCount.java`: Result model for window aggregations
  - `BuyerEventSourceFunction.java`: Custom source function generating simulated data
  - `SQLiteSink.java`: Custom sink function writing results to SQLite
  - `BuyerCountJob.java`: Main Flink job implementing the sliding window logic
- `docker-compose.yml`: Docker Compose configuration for the Flink environment
- `pom.xml`: Maven project configuration
- `run.sh`: Helper script to build and run the project

## Use Case

The application demonstrates:

1. **Data Generation**: Using a custom source function to simulate buyer events
2. **Windowing**: Processing data in a 1-minute sliding window that advances every 10 seconds
3. **Stateful Processing**: Counting unique buyers and total amount per window
4. **Data Sink**: Storing results in SQLite for further analysis

## Getting Started

### Prerequisites

- Docker and Docker Compose
- Maven
- Java 11 or newer

### Running the Application

1. Make the run script executable:

    ```bash
    chmod +x run.sh
    ```

2. Run the script:

    ```bash
    ./run.sh
    ```

This will:
- Build the Java application with Maven
- Start the Flink Docker environment
- Submit the Flink job
- Create the SQLite database

### Accessing the Results

1. Access the Flink Web UI at: http://localhost:8081

2. Query the SQLite database:

    ```bash
    docker compose exec flink sqlite3 /opt/flink/data/buyer_counts.db 'SELECT * FROM buyer_counts'
    ```

### Stopping the Application

To stop all services:

```bash
docker compose down
```

## Learning Resources

- [Apache Flink Documentation](https://nightlies.apache.org/flink/flink-docs-stable/)
- [Flink DataStream API](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/datastream/overview/)
- [Flink Windows](https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/datastream/operators/windows/)
