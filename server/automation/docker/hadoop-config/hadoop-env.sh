export LOGS_ROOT=/tmp/logs
export PIDS_ROOT=/tmp/pids
export HADOOP_STORAGE_ROOT=/tmp/hadoop
export COMMON_JAVA_OPTS=${COMMON_JAVA_OPTS:-}
export COMMON_CLASSPATH=${COMMON_CLASSPATH:-}

export HADOOP_CLASSPATH=\
$HADOOP_CLASSPATH:\
$COMMON_CLASSPATH:\

# Extra Java runtime options.  Empty by default.
export HADOOP_OPTS="$HADOOP_OPTS $COMMON_JAVA_OPTS"

export COMMON_MASTER_OPTS="-Dhadoop.tmp.dir=$HADOOP_STORAGE_ROOT"

# Command specific options appended to HADOOP_OPTS when specified
export HDFS_NAMENODE_OPTS="$COMMON_MASTER_OPTS"
export HADOOP_SECONDARYNAMENODE_OPTS="$COMMON_MASTER_OPTS"

# Where log files are stored.  $HADOOP_HOME/logs by default.
export HADOOP_LOG_DIR=$LOGS_ROOT

# The directory where pid files are stored. /tmp by default.
export HADOOP_PID_DIR=$PIDS_ROOT
