#!/bin/bash
set -e

if [ "$(whoami)" != "gpadmin" ]; then
    echo "This script must be run as gpadmin user. Current user: $(whoami)"
    exit 1
fi

cat <<-'EOF'

======================================================================
  ____ _                 _ _                            ____  ____
 / ___| | ___  _   _  __| | |__   ___ _ __ _ __ _   _  |  _ \| __ )
| |   | |/ _ \| | | |/ _` | '_ \ / _ \ '__| '__| | | | | | | |  _ \
| |___| | (_) | |_| | (_| | |_) |  __/ |  | |  | |_| | | |_| | |_) |
 \____|_|\___/ \__,_|\__,_|_.__/ \___|_|  |_|   \__, | |____/|____/
                                                |___/
======================================================================
EOF

cat <<-'EOF'

======================================================================
Testing: Cloudberry Database Cluster details
======================================================================

EOF

# Source Greenplum environment
source /opt/greenplum-db-6/greenplum_path.sh

echo "Current time: $(date)"
source /etc/os-release
echo "OS Version: ${NAME} ${VERSION}"

## Set gpadmin password, display version and cluster configuration
psql -P pager=off -d template1 -c "SELECT VERSION()"
psql -P pager=off -d template1 -c "SELECT * FROM gp_segment_configuration ORDER BY dbid"
psql -P pager=off -d template1 -c "SHOW optimizer"

echo """
===========================
=  DEPLOYMENT SUCCESSFUL  =
===========================
"""

# Start Greenplum master
/opt/greenplum-db-6/bin/pg_ctl -D /data/master/gpseg-1 -l /data/master/gpseg-1/gpdb.log start

# Start PXF
echo "[INFO] PXF start."

export USER=gpadmin
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
export PXF_HOME=/opt/greenplum-pxf-6/
export PATH="$PXF_HOME/bin:$PATH"
export PXF_BASE=/home/gpadmin/pxf
export PXF_JVM_OPTS="-Xmx512m -Xms256m"
export PXF_HOST=0.0.0.0  # listen on all interfaces


# Start PXF
/opt/greenplum-pxf-6/bin/pxf cluster register
/opt/greenplum-pxf-6/bin/pxf cluster start

