package org.greenplum.pxf.automation.components.common;

  
public enum DockerContainers {
    HADOOP_NAMENODE("hadoop-namenode"),
    HADOOP_RESOURCEMANAGER("hadoop-resourcemanager"),
    HADOOP_METASTORE("hadoop-metastore"),
    HADOOP_DATANODE("hadoop-datanode"),
    HADOOP_NODEMANAGER("hadoop-nodemanager"),
    HADOOP_HIVE_SERVER("hadoop-hiveserver"),
    GPDB_PXF("gpdb-pxf"),
    MINIO("minio");

    private final String containerName;

    DockerContainers(String containerName) {
        this.containerName = containerName;
    }

    public String getContainerName() {
        return containerName;
    }
}
