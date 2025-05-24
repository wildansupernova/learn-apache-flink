#!/bin/zsh

# Exit on any error
set -e

echo "Creating necessary directories..."
mkdir -p data
mkdir -p log
# Make sure directories have correct permissions
chmod 777 data
chmod 777 log

# Export current user ID and group ID for Docker to use
export UID=$(id -u)
export GID=$(id -g)

echo "Building the Maven project..."
mvn clean package

# Stop any existing containers to avoid conflicts
echo "Stopping any existing Docker containers..."
# Use the traditional docker-compose command syntax
docker-compose down -v 2>/dev/null || true

echo "Starting the Docker environment..."
# Use the traditional docker-compose command syntax with separate flag
docker-compose up -d

echo "Waiting for services to initialize..."
sleep 5

# Get the container name for the JobManager
FLINK_CONTAINER_NAME=$(docker ps --format '{{.Names}}' | grep 'flink-1')

echo "Running the Flink job on container $FLINK_CONTAINER_NAME..."
docker exec -e "SQLITE_DB_PATH=/opt/flink/data/buyer_counts.db" $FLINK_CONTAINER_NAME bash -c "chmod 777 /opt/flink/log && chmod 777 /opt/flink/data && /opt/flink/bin/flink run -c com.example.BuyerCountJob /opt/flink/usrlib/learn-flink-1.0-SNAPSHOT.jar"

echo "Services are up and running! Access the Flink Web UI at: http://localhost:8081"
echo "To check the SQLite database, you can run:"
echo "docker exec \$FLINK_CONTAINER_NAME sqlite3 /opt/flink/data/buyer_counts.db 'SELECT * FROM buyer_counts'"
echo "To stop all services, run: docker compose down"
