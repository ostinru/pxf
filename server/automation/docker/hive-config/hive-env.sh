export LOGS_ROOT=/tmp/logs
export PIDS_ROOT=/tmp/pids
# export HADOOP_STORAGE_ROOT=/tmp/hadoop
# export HIVE_STORAGE_ROOT=/tmp/hive
export HADOOP_ROOT=/opt/hadoop
export TEZ_CONF=/opt/tez/conf
export TEZ_JARS=/opt/tez/*.jar:/opt/tez/lib/*.jar
export COMMON_JAVA_OPTS=${COMMON_JAVA_OPTS:-}
export COMMON_CLASSPATH=${COMMON_CLASSPATH:-}

# load singlecluster environment
export HIVE_OPTS="-hiveconf derby.stream.error.file=$LOGS_ROOT/derby.log -hiveconf javax.jdo.option.ConnectionURL=jdbc:derby:;databaseName=$HIVE_STORAGE_ROOT/metastore_db;create=true"
export HIVE_SERVER_OPTS="-hiveconf derby.stream.error.file=$LOGS_ROOT/derby.log -hiveconf ;databaseName=$HIVE_STORAGE_ROOT/metastore_db;create=true"
export HADOOP_HOME=$HADOOP_ROOT
export HADOOP_CLASSPATH="$TEZ_CONF:$TEZ_JARS:$HADOOP_CLASSPATH"