package org.apache.cloudberry.pxf.automation.docker;

import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;

import java.util.List;

/**
 * Example usage of DockerManager
 */
public class DockerExample {
    
    public static void main(String[] args) {
        DockerManager dockerManager = new DockerManager();
        
        try {
            // List all containers
            System.out.println("=== All Containers ===");
            List<Container> containers = dockerManager.listContainers();
            for (Container container : containers) {
                System.out.println("Container: " + container.getNames()[0] + 
                                 " (ID: " + container.getId() + ")");
            }
            
            // List running containers
            System.out.println("\n=== Running Containers ===");
            List<Container> runningContainers = dockerManager.listRunningContainers();
            for (Container container : runningContainers) {
                System.out.println("Running: " + container.getNames()[0]);
            }
            
            // List images
            System.out.println("\n=== Images ===");
            List<Image> images = dockerManager.listImages();
            for (Image image : images) {
                System.out.println("Image: " + image.getRepoTags()[0] + 
                                 " (ID: " + image.getId() + ")");
            }
            
            // Find specific container
            Container hadoopContainer = dockerManager.getContainerByPartialName("hadoop");
            if (hadoopContainer != null) {
                System.out.println("\n=== Hadoop Container Info ===");
                System.out.println("Name: " + hadoopContainer.getNames()[0]);
                System.out.println("Status: " + hadoopContainer.getStatus());
                System.out.println("Image: " + hadoopContainer.getImage());
                
                // Check if running
                if (dockerManager.isContainerRunning(hadoopContainer.getId())) {
                    System.out.println("Container is running!");
                    
                    // Get logs
                    System.out.println("\n=== Container Logs (last 10 lines) ===");
                    String logs = dockerManager.getContainerLogs(hadoopContainer.getId(), 10);
                    System.out.println(logs);
                    
                    // Execute command
                    System.out.println("\n=== Executing command ===");
                    String result = dockerManager.execCommand(hadoopContainer.getId(), "ls", "-la");
                    System.out.println("Command result: " + result);
                } else {
                    System.out.println("Container is not running");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Close Docker client
            dockerManager.close();
        }
    }
    
    /**
     * Example of managing containers programmatically
     */
    public static void manageContainers() {
        DockerManager dockerManager = new DockerManager();
        
        try {
            // Create a simple container
            System.out.println("Creating test container...");
            String containerId = dockerManager.createContainer("hello-world", "test-container")
                    .getId();
            
            // Start the container
            System.out.println("Starting container...");
            dockerManager.startContainer(containerId);
            
            // Wait for it to be running
            if (dockerManager.waitForRunning(containerId, 30)) {
                System.out.println("Container is running!");
                
                // Get container info
                System.out.println("Container info: " + 
                                 dockerManager.inspectContainer(containerId).getName());
                
                // Stop the container
                System.out.println("Stopping container...");
                dockerManager.stopContainer(containerId);
                
                // Remove the container
                System.out.println("Removing container...");
                dockerManager.removeContainer(containerId);
            }
            
        } catch (Exception e) {
            System.err.println("Error managing containers: " + e.getMessage());
        } finally {
            dockerManager.close();
        }
    }
    
    /**
     * Example of monitoring container health
     */
    public static void monitorContainerHealth() {
        DockerManager dockerManager = new DockerManager();
        
        try {
            // Find Hadoop containers
            Container namenode = dockerManager.getContainerByPartialName("namenode");
            Container datanode = dockerManager.getContainerByPartialName("datanode");
            
            if (namenode != null) {
                System.out.println("Monitoring NameNode health...");
                if (dockerManager.waitForHealthy(namenode.getId(), 300)) {
                    System.out.println("NameNode is healthy!");
                } else {
                    System.out.println("NameNode failed to become healthy");
                }
            }
            
            if (datanode != null) {
                System.out.println("Monitoring DataNode health...");
                if (dockerManager.waitForHealthy(datanode.getId(), 300)) {
                    System.out.println("DataNode is healthy!");
                } else {
                    System.out.println("DataNode failed to become healthy");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error monitoring containers: " + e.getMessage());
        } finally {
            dockerManager.close();
        }
    }
    
    /**
     * Example of pulling and managing images
     */
    public static void manageImages() {
        DockerManager dockerManager = new DockerManager();
        
        try {
            // Pull a new image
            System.out.println("Pulling nginx image...");
            dockerManager.pullImage("nginx:latest");
            
            // List images to confirm
            System.out.println("Available images:");
            List<Image> images = dockerManager.listImages();
            for (Image image : images) {
                if (image.getRepoTags() != null && image.getRepoTags().length > 0) {
                    System.out.println("  " + image.getRepoTags()[0]);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error managing images: " + e.getMessage());
        } finally {
            dockerManager.close();
        }
    }
} 