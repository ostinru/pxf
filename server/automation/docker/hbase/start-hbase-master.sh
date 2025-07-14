#!/bin/bash

set -e

echo "Starting HBase Master..."

# Set environment variables
export HBASE_HOME=/opt/hbase
export HBASE_CONF_DIR=${HBASE_HOME}/conf

# Create HDFS directory for HBase if it doesn't exist
echo "Creating HBase directory in HDFS..."
hdfs dfs -mkdir -p /hbase 2>/dev/null || echo "HBase directory already exists or could not be created"

# Start HBase Master
echo "Starting HBase Master daemon..."
${HBASE_HOME}/bin/hbase-daemon.sh --config ${HBASE_CONF_DIR} start master

# Keep the container running
echo "HBase Master started successfully"
# Keep container alive
tail -f /dev/null 