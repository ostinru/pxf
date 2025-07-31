#!/bin/bash
set -e


# ----------------------------------------------------------------------
# Start SSH daemon and setup for SSH access
# ----------------------------------------------------------------------
# The SSH daemon is started to allow remote access to the container via
# SSH. This is useful for development and debugging purposes. If the SSH
# daemon fails to start, the script exits with an error.
# ----------------------------------------------------------------------
if [ ! -d /var/run/sshd ]; then
   sudo mkdir /var/run/sshd
   sudo chmod 0755 /var/run/sshd
fi
if ! sudo /usr/sbin/sshd; then
    echo "Failed to start SSH daemon"
    exit 1
fi

# ----------------------------------------------------------------------
# Remove /run/nologin to allow logins for all users via SSH
# ----------------------------------------------------------------------
sudo rm -rf /run/nologin

# --------------------------------------------------------------------
# Run tests
# --------------------------------------------------------------------
cd /home/gpadmin/workspace/pxf/automation
make  # make without arguments runs all tests

# Keep container running
#tail -f /dev/null