FROM openjdk:8-jre

# Set environment variables
ENV HBASE_VERSION=2.0.2
ENV HBASE_HOME=/opt/hbase
ENV HADOOP_HOME=/opt/hadoop
ENV PATH=$PATH:$HBASE_HOME/bin:$HADOOP_HOME/bin
ENV JAVA_HOME=/usr/local/openjdk-8
ENV HADOOP_USER_NAME=gpadmin
ENV HADOOP_CONF_DIR=/opt/hadoop/etc/hadoop

# Install dependencies
RUN apt-get update && \
    apt-get install -y wget curl netcat-openbsd procps && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Download Hadoop client for HDFS operations
RUN wget -q https://archive.apache.org/dist/hadoop/common/hadoop-3.3.6/hadoop-3.3.6.tar.gz && \
    tar -xzf hadoop-3.3.6.tar.gz && \
    mv hadoop-3.3.6 /opt/hadoop && \
    rm hadoop-3.3.6.tar.gz && \
    ln -s /opt/hadoop/bin/hdfs /usr/local/bin/hdfs

# Download and install HBase
RUN wget -q https://archive.apache.org/dist/hbase/${HBASE_VERSION}/hbase-${HBASE_VERSION}-bin.tar.gz && \
    tar -xzf hbase-${HBASE_VERSION}-bin.tar.gz && \
    mv hbase-${HBASE_VERSION} ${HBASE_HOME} && \
    rm hbase-${HBASE_VERSION}-bin.tar.gz

# Create necessary directories
RUN mkdir -p /opt/hbase/logs /opt/hbase/pids /tmp/hbase

# Copy configuration files
COPY configs/ ${HBASE_HOME}/conf/

# Create minimal Hadoop configuration
RUN mkdir -p ${HADOOP_CONF_DIR} && \
    echo '<?xml version="1.0"?>' > ${HADOOP_CONF_DIR}/core-site.xml && \
    echo '<configuration>' >> ${HADOOP_CONF_DIR}/core-site.xml && \
    echo '  <property>' >> ${HADOOP_CONF_DIR}/core-site.xml && \
    echo '    <name>fs.defaultFS</name>' >> ${HADOOP_CONF_DIR}/core-site.xml && \
    echo '    <value>hdfs://namenode:8020</value>' >> ${HADOOP_CONF_DIR}/core-site.xml && \
    echo '  </property>' >> ${HADOOP_CONF_DIR}/core-site.xml && \
    echo '</configuration>' >> ${HADOOP_CONF_DIR}/core-site.xml

# Copy startup scripts
COPY entrypoint.sh /entrypoint.sh
COPY start-hbase-master.sh /start-hbase-master.sh
COPY start-hbase-regionserver.sh /start-hbase-regionserver.sh

# Make scripts executable
RUN chmod +x /entrypoint.sh /start-hbase-master.sh /start-hbase-regionserver.sh

# Expose ports
EXPOSE 16000 16010 16020 16030

ENTRYPOINT ["/entrypoint.sh"] 