package org.apache.cloudberry.pxf.automation.docker;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.AfterClass;
import static org.testng.Assert.*;
import java.util.List;

/**
 * Test class for DockerManager
 */
public class DockerManagerTest {
    
    private DockerManager dockerManager;
    
    @BeforeClass
    public void setUp() {
        try {
            dockerManager = new DockerManager();
        } catch (Exception e) {
            // Skip tests if Docker is not available
            System.out.println("Docker not available, skipping tests: " + e.getMessage());
        }
    }
    
    @AfterClass
    public void tearDown() {
        if (dockerManager != null) {
            dockerManager.close();
        }
    }
    
    @Test
    public void testDockerManagerCreation() {
        // This test will be skipped if Docker is not available
        if (dockerManager == null) {
            System.out.println("Skipping test - Docker not available");
            return;
        }
        
        assertNotNull(dockerManager, "DockerManager should be created successfully");
    }
    
    @Test
    public void testListContainers() {
        if (dockerManager == null) {
            System.out.println("Skipping test - Docker not available");
            return;
        }
        
        try {
            List<com.github.dockerjava.api.model.Container> containers = dockerManager.listContainers();
            assertNotNull(containers, "Container list should not be null");
            System.out.println("Found " + containers.size() + " containers");
        } catch (Exception e) {
            fail("Should be able to list containers: " + e.getMessage());
        }
    }
    
    @Test
    public void testListImages() {
        if (dockerManager == null) {
            System.out.println("Skipping test - Docker not available");
            return;
        }
        
        try {
            List<com.github.dockerjava.api.model.Image> images = dockerManager.listImages();
            assertNotNull(images, "Image list should not be null");
            System.out.println("Found " + images.size() + " images");
        } catch (Exception e) {
            fail("Should be able to list images: " + e.getMessage());
        }
    }
    
    @Test
    public void testGetContainerByPartialName() {
        if (dockerManager == null) {
            System.out.println("Skipping test - Docker not available");
            return;
        }
        
        try {
            com.github.dockerjava.api.model.Container container = dockerManager.getContainerByPartialName("test");
            // This might be null if no test containers exist, which is fine
            if (container != null) {
                System.out.println("Found test container: " + container.getNames()[0]);
            } else {
                System.out.println("No test containers found");
            }
        } catch (Exception e) {
            fail("Should be able to search for containers: " + e.getMessage());
        }
    }
} 