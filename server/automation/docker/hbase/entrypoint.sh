#!/bin/bash

set -e

# Function to wait for service to be ready
wait_for_service() {
    local host=$1
    local port=$2
    local service_name=$3
    
    echo "Waiting for $service_name to be ready..."
    while ! nc -z $host $port; do
        echo "$service_name is not ready yet, waiting..."
        sleep 5
    done
    echo "$service_name is ready!"
}

# Wait for dependencies
wait_for_service namenode 9870 "HDFS NameNode"
wait_for_service zookeeper 2181 "Zookeeper"

# Check if HDFS is out of safemode
echo "Checking HDFS safemode..."
while hdfs dfsadmin -safemode get 2>&1 | grep -q "Safe mode is ON"; do
    echo "HDFS is in safemode, waiting..."
    sleep 5
done
echo "HDFS is out of safemode"

# Create HBase directory in HDFS if it doesn't exist and set permissions
echo "Setting up HBase directory in HDFS..."
if hdfs dfs -test -d /hbase 2>/dev/null; then
    echo "HBase directory already exists in HDFS"
else
    echo "Creating HBase directory in HDFS..."
    hdfs dfs -mkdir -p /hbase
    echo "HBase directory created successfully"
fi

# Set ownership to gpadmin
echo "Setting ownership of /hbase to gpadmin..."
hdfs dfs -chown gpadmin /hbase

echo "HBase directory setup completed"

# Determine which service to start
if [ "$HBASE_SERVICE" = "regionserver" ]; then
    echo "Starting HBase RegionServer..."
    exec /start-hbase-regionserver.sh
else
    echo "Starting HBase Master..."
    exec /start-hbase-master.sh
fi 