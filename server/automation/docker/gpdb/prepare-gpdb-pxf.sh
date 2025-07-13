#!/bin/bash
set -e

GPHOME=${GPHOME:=/opt/greenplum-db-6}


# ----------------------------------------------------------------------
# Start SSH daemon and setup for SSH access
# ----------------------------------------------------------------------
# The SSH daemon is started to allow remote access to the container via
# SSH. This is useful for development and debugging purposes. If the SSH
# daemon fails to start, the script exits with an error.
# ----------------------------------------------------------------------
mkdir /var/run/sshd && chmod 0755 /var/run/sshd  # workaround from https://askubuntu.com/questions/1110828/
if ! sudo /usr/sbin/sshd; then
    echo "Failed to start SSH daemon" >&2
    exit 1
fi

# ----------------------------------------------------------------------
# Remove /run/nologin to allow logins
# ----------------------------------------------------------------------
# The /run/nologin file, if present, prevents users from logging into
# the system. This file is removed to ensure that users can log in via SSH.
# ----------------------------------------------------------------------
rm -rf /run/nologin

echo "mdw" > /tmp/gpdb-hosts

# ## Set gpadmin ownership - Clouberry install directory and supporting
# ## cluster creation files.
chown -R gpadmin:gpadmin /tmp/gpinitsystem_singlenode /tmp/gpdb-hosts

mkdir -p /home/gpadmin/.ssh/
chown -R gpadmin:gpadmin /home/gpadmin/.ssh/
ssh-keyscan -t rsa mdw > /home/gpadmin/.ssh/known_hosts
echo "export COORDINATOR_DATA_DIRECTORY=/data0/database/master/gpseg-1" >> /home/gpadmin/.bashrc
echo "source /opt/greenplum-db-6/greenplum_path.sh"                     >> /home/gpadmin/.bashrc
chown -R gpadmin:gpadmin /home/gpadmin/.bashrc

# Source Cloudberry environment variables and set
# COORDINATOR_DATA_DIRECTORY
source /opt/greenplum-db-6/greenplum_path.sh
export COORDINATOR_DATA_DIRECTORY=/data0/database/master/gpseg-1

export USER=gpadmin
# Initialize single node Cloudberry cluster
sleep 1000
sudo -u gpadmin bash -c "source /opt/greenplum-db-6/greenplum_path.sh && /opt/greenplum-db-6/bin/gpinitsystem -a \
             -c /tmp/gpinitsystem_singlenode \
             -h /tmp/gpdb-hosts \
             --max_connections=100"

sudo -u gpadmin bash -c "/opt/greenplum-db-6/bin/gpinitsystem -a \
             -c /tmp/gpinitsystem_singlenode \
             -h /tmp/gpdb-hosts \
             --max_connections=100"

## Allow any host access the Cloudberry Cluster
echo 'host all all 0.0.0.0/0 trust' >> /data0/database/master/gpseg-1/pg_hba.conf
gpstop -u

psql -d template1 \
     -c "ALTER USER gpadmin PASSWORD 'cbdb@123'"


export PXF_HOME=/opt/greenplum-pxf-6/
export PXF_BASE=/home/gpadmin/pxf

# Create PXF directories and set permissions
mkdir -p /home/gpadmin/pxf/{conf,logs,run,tmp}
cp /opt/greenplum-pxf-6/conf/* /home/gpadmin/pxf/conf/
chown -R gpadmin:gpadmin /home/gpadmin/pxf

# Configure PXF to listen on all interfaces
sed -i 's/# server.address=localhost/server.address=0.0.0.0/' /home/gpadmin/pxf/conf/pxf-application.properties
echo -e "\npxf.profile.dynamic.regex=test:.*" >> $PXF_BASE/conf/pxf-application.properties

pxf cluster prepare
cp -v $PXF_HOME/templates/{hdfs,mapred,yarn,core,hbase,hive}-site.xml $PXF_BASE/servers/default

echo """
===========================
=     BUILD SUCCESSFUL    =
===========================
"""