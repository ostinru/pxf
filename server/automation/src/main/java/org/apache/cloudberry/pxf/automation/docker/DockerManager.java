package org.apache.cloudberry.pxf.automation.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Java wrapper for managing Docker operations using docker-java library
 */
public class DockerManager {
    
    private final DockerClient dockerClient;
    
    public DockerManager() {
        this("unix:///var/run/docker.sock");
    }
    
    public DockerManager(String dockerHost) {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHost)
                .build();
        
        DockerHttpClient httpClient = new ZerodepDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();
        
        this.dockerClient = DockerClientBuilder.getInstance(config)
                .withDockerHttpClient(httpClient)
                .build();
    }
    
    /**
     * List all containers
     */
    public List<Container> listContainers() {
        return dockerClient.listContainersCmd().exec();
    }
    
    /**
     * List running containers
     */
    public List<Container> listRunningContainers() {
        return dockerClient.listContainersCmd()
                .withShowAll(false)
                .exec();
    }
    
    /**
     * List all images
     */
    public List<Image> listImages() {
        return dockerClient.listImagesCmd().exec();
    }
    
    /**
     * Start a container
     */
    public void startContainer(String containerId) {
        dockerClient.startContainerCmd(containerId).exec();
    }
    
    /**
     * Stop a container
     */
    public void stopContainer(String containerId) {
        dockerClient.stopContainerCmd(containerId).exec();
    }
    
    /**
     * Stop a container with timeout
     */
    public void stopContainer(String containerId, int timeoutSeconds) {
        dockerClient.stopContainerCmd(containerId)
                .withTimeout(timeoutSeconds)
                .exec();
    }
    
    /**
     * Remove a container
     */
    public void removeContainer(String containerId) {
        dockerClient.removeContainerCmd(containerId).exec();
    }
    
    /**
     * Remove a container with force
     */
    public void removeContainer(String containerId, boolean force) {
        dockerClient.removeContainerCmd(containerId)
                .withForce(force)
                .exec();
    }
    
    /**
     * Get container info
     */
    public InspectContainerResponse inspectContainer(String containerId) {
        return dockerClient.inspectContainerCmd(containerId).exec();
    }
    
    /**
     * Check if container is running
     */
    public boolean isContainerRunning(String containerId) {
        try {
            InspectContainerResponse response = inspectContainer(containerId);
            return response.getState().getRunning();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get container logs
     */
    public String getContainerLogs(String containerId) {
        try {
            return dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .exec(null)
                    .toString();
        } catch (Exception e) {
            return "Error getting logs: " + e.getMessage();
        }
    }
    
    /**
     * Get container logs with tail
     */
    public String getContainerLogs(String containerId, int tail) {
        try {
            return dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withTail(tail)
                    .exec(null)
                    .toString();
        } catch (Exception e) {
            return "Error getting logs: " + e.getMessage();
        }
    }
    
    /**
     * Create a container
     */
    public CreateContainerResponse createContainer(String image, String containerName) {
        return dockerClient.createContainerCmd(image)
                .withName(containerName)
                .exec();
    }
    
    /**
     * Create a container with custom configuration
     */
    public CreateContainerResponse createContainer(String image, String containerName, 
                                                 String[] cmd, String[] env) {
        return dockerClient.createContainerCmd(image)
                .withName(containerName)
                .withCmd(cmd)
                .withEnv(env)
                .exec();
    }
    
    /**
     * Pull an image
     */
    public void pullImage(String image) {
        dockerClient.pullImageCmd(image).exec(null);
    }
    
    /**
     * Remove an image
     */
    public void removeImage(String imageId) {
        dockerClient.removeImageCmd(imageId).exec();
    }
    
    /**
     * Remove an image with force
     */
    public void removeImage(String imageId, boolean force) {
        dockerClient.removeImageCmd(imageId)
                .withForce(force)
                .exec();
    }
    
    /**
     * Execute command in running container
     */
    public String execCommand(String containerId, String... command) {
        try {
            String execId = dockerClient.execCreateCmd(containerId)
                    .withCmd(command)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec()
                    .getId();
            
            return dockerClient.execStartCmd(execId)
                    .exec(null)
                    .toString();
        } catch (Exception e) {
            return "Error executing command: " + e.getMessage();
        }
    }
    
    /**
     * Wait for container to be healthy
     */
    public boolean waitForHealthy(String containerId, long timeoutSeconds) {
        long startTime = System.currentTimeMillis();
        long timeout = timeoutSeconds * 1000;
        
        while (System.currentTimeMillis() - startTime < timeout) {
            try {
                InspectContainerResponse response = inspectContainer(containerId);
                if (response.getState().getHealth() != null && 
                    "healthy".equals(response.getState().getHealth().getStatus())) {
                    return true;
                }
                Thread.sleep(5000); // Check every 5 seconds
            } catch (Exception e) {
                // Continue waiting
            }
        }
        return false;
    }
    
    /**
     * Wait for container to be running
     */
    public boolean waitForRunning(String containerId, long timeoutSeconds) {
        long startTime = System.currentTimeMillis();
        long timeout = timeoutSeconds * 1000;
        
        while (System.currentTimeMillis() - startTime < timeout) {
            if (isContainerRunning(containerId)) {
                return true;
            }
            try {
                Thread.sleep(5000); // Check every 5 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
    
    /**
     * Get container by name
     */
    public Container getContainerByName(String name) {
        List<Container> containers = dockerClient.listContainersCmd()
                .withShowAll(true)
                .withNameFilter(Collections.singletonList(name))
                .exec();
        
        return containers.isEmpty() ? null : containers.get(0);
    }
    
    /**
     * Get container by partial name
     */
    public Container getContainerByPartialName(String partialName) {
        List<Container> containers = dockerClient.listContainersCmd()
                .withShowAll(true)
                .exec();
        
        return containers.stream()
                .filter(container -> container.getNames()[0].contains(partialName))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Close Docker client
     */
    public void close() {
        if (dockerClient != null) {
            try {
                dockerClient.close();
            } catch (Exception e) {
                // Ignore close errors
            }
        }
    }
} 