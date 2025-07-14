#!/bin/bash

set -e

echo "Starting HBase RegionServer..."

# Set environment variables
export HBASE_HOME=/opt/hbase
export HBASE_CONF_DIR=${HBASE_HOME}/conf

# Wait for HBase Master to be ready
echo "Waiting for HBase Master to be ready..."
while ! nc -z hbase-master 16000; do
    echo "HBase Master is not ready yet, waiting..."
    sleep 5
done
echo "HBase Master is ready!"

# Start HBase RegionServer
echo "Starting HBase RegionServer daemon..."
${HBASE_HOME}/bin/hbase-daemon.sh --config ${HBASE_CONF_DIR} start regionserver

# Keep the container running
echo "HBase RegionServer started successfully"
# Keep container alive
tail -f /dev/null 