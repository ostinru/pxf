#!/bin/bash
set -e

# Source Greenplum environment
source /usr/local/greenplum-db/greenplum_path.sh

# Create data directory if not exists
mkdir -p /data/master/gpseg-1
chown -R gpadmin:gpadmin /data

# Initialize Greenplum master if not already initialized
if [ ! -f /data/master/gpseg-1/PG_VERSION ]; then
    echo "Initializing Greenplum master..."
    su - gpadmin -c "initdb -D /data/master/gpseg-1 --locale=en_US.UTF-8"
fi

# Start Greenplum master
su - gpadmin -c "pg_ctl -D /data/master/gpseg-1 -l /data/master/gpseg-1/gpdb.log start"

# (Optional) Create default database/user if needed
# su - gpadmin -c "createdb -h localhost -p 5432 -U gpadmin pxfautomation || true"

# Start PXF (placeholder)
echo "[INFO] PXF start command should be placed here."
# su - gpadmin -c "/usr/local/greenplum-db/pxf/bin/pxf start"

# Keep container running
tail -f /data/master/gpseg-1/gpdb.log 