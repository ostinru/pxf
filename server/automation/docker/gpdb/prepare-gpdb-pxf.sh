#!/bin/bash
set -e

if [ "$(whoami)" != "gpadmin" ]; then
    echo "This script must be run as gpadmin user. Current user: $(whoami)"
    exit 1
fi

# ----------------------------------------------------------------------
# Configure passwordless SSH access for 'gpadmin' user
# ----------------------------------------------------------------------
# The script sets up SSH key-based authentication for the 'gpadmin' user,
# allowing passwordless SSH access. It generates a new SSH key pair if one
# does not already exist, and configures the necessary permissions.
# ----------------------------------------------------------------------
mkdir /home/gpadmin/.ssh
ssh-keygen -t rsa -b 4096 -m PEM -C gpadmin -f /home/gpadmin/.ssh/id_rsa -P ""
cat /home/gpadmin/.ssh/id_rsa.pub >> /home/gpadmin/.ssh/authorized_keys
chmod 0600 /home/gpadmin/.ssh/authorized_keys

# Allow password logins
echo "PasswordAuthentication yes" | sudo tee -a /etc/ssh/sshd_config

# ----------------------------------------------------------------------
# Start SSH daemon and setup for SSH access
# ----------------------------------------------------------------------
# The SSH daemon is started to allow remote access to the container via
# SSH. This is useful for development and debugging purposes. If the SSH
# daemon fails to start, the script exits with an error.
# ----------------------------------------------------------------------
sudo mkdir /var/run/sshd && sudo chmod 0755 /var/run/sshd  # workaround from https://askubuntu.com/questions/1110828/
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
sudo rm -rf /run/nologin

# ----------------------------------------------------------------------
# Prepare files for gpinitsystem
# ----------------------------------------------------------------------
sudo mkdir -p /data0/database/master /data0/database/primary /data0/database/mirror
sudo chown -R gpadmin:gpadmin /data0

echo "mdw" | sudo tee -a /tmp/gpdb-hosts

sudo chown -R gpadmin:gpadmin /tmp/gpinitsystem_singlenode /tmp/gpdb-hosts
echo "export COORDINATOR_DATA_DIRECTORY=/data0/database/master/gpseg-1" | sudo tee -a /etc/profile
echo "export MASTER_DATA_DIRECTORY=/data0/database/master/gpseg-1"      | sudo tee -a /etc/profile
echo "source /opt/greenplum-db-6/greenplum_path.sh"                     | sudo tee -a /etc/profile

# ----------------------------------------------------------------------
# Configure /home/gpadmin
# ----------------------------------------------------------------------
mkdir -p /home/gpadmin/.ssh/
ssh-keyscan -t rsa mdw > /home/gpadmin/.ssh/known_hosts
chown -R gpadmin:gpadmin /home/gpadmin/.ssh/

echo "export COORDINATOR_DATA_DIRECTORY=/data0/database/master/gpseg-1" >> /home/gpadmin/.bashrc
echo "export MASTER_DATA_DIRECTORY=/data0/database/master/gpseg-1"      >> /home/gpadmin/.bashrc
echo "source /opt/greenplum-db-6/greenplum_path.sh"                     >> /home/gpadmin/.bashrc

# ----------------------------------------------------------------------
# Run gpinitsystem
# ----------------------------------------------------------------------
# Source Cloudberry environment variables
source /opt/greenplum-db-6/greenplum_path.sh
export COORDINATOR_DATA_DIRECTORY=/data0/database/master/gpseg-1
export MASTER_DATA_DIRECTORY=/data0/database/master/gpseg-1

export USER=gpadmin

# Initialize single node Cloudberry cluster
gpinitsystem -a \
             -c /tmp/gpinitsystem_singlenode \
             -h /tmp/gpdb-hosts \
             --max_connections=100 || echo "gpinitsystem finished with exit code $?"

echo "gpinitsystem finished with exit code $?"

## Allow any host access the Cloudberry Cluster
echo 'host all all 0.0.0.0/0 trust' >> /data0/database/master/gpseg-1/pg_hba.conf

# Configure PostgreSQL to listen on all interfaces
echo "listen_addresses = '*'" >> /data0/database/master/gpseg-1/postgresql.conf
echo "port = 5432" >> /data0/database/master/gpseg-1/postgresql.conf

gpstop -u && echo "pg_hba.conf reloaded"

psql -d template1 \
     -c "ALTER USER gpadmin PASSWORD 'cbdb@123'"

## Set gpadmin password, display version and cluster configuration
psql -P pager=off -d template1 -c "SELECT VERSION()"
psql -P pager=off -d template1 -c "SELECT * FROM gp_segment_configuration ORDER BY dbid"
psql -P pager=off -d template1 -c "SHOW optimizer"

# ----------------------------------------------------------------------
# Prepare PXF
# ----------------------------------------------------------------------
export PXF_HOME=/opt/greenplum-pxf-6/
export PXF_BASE=/home/gpadmin/pxf
export USER=gpadmin
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
export PATH="$PXF_HOME/bin:$PATH"
export PXF_JVM_OPTS="-Xmx512m -Xms256m"
export PXF_HOST=0.0.0.0  # listen on all interfaces

# Prepare PXF cluster first
/opt/greenplum-pxf-6/bin/pxf cluster prepare

# Create PXF directories and set permissions
mkdir -p /home/gpadmin/pxf/{conf,logs,run,tmp}
chown -R gpadmin:gpadmin /home/gpadmin/pxf

# Now copy configuration files
cp /opt/greenplum-pxf-6/conf/* /home/gpadmin/pxf/conf/

# Configure PXF to listen on all interfaces
sed -i 's/# server.address=localhost/server.address=0.0.0.0/' /home/gpadmin/pxf/conf/pxf-application.properties
echo -e "\npxf.profile.dynamic.regex=test:.*" >> $PXF_BASE/conf/pxf-application.properties
cp -v $PXF_HOME/templates/{hdfs,mapred,yarn,core,hbase,hive}-site.xml $PXF_BASE/servers/default


# Start PXF
/opt/greenplum-pxf-6/bin/pxf cluster register
/opt/greenplum-pxf-6/bin/pxf cluster start

echo """
===========================
=  DEPLOYMENT SUCCESSFUL  =
===========================
"""