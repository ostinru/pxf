#!/bin/bash
set -e


mkdir -p /data0/database/master /data0/database/primary /data0/database/mirror
chown -R gpadmin:gpadmin /data0


/prepare-gpdb-pxf.sh

su - gpadmin -c /start-gpdb-pxf.sh

# Keep container running
tail -f /dev/null