package dev.neuralforge;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * NeuralForge Backend Application
 * 
 * This is NOT a web server! It's an embedded Spring Boot application
 * that runs as a background process, communicating with the Electron
 * frontend via stdin/stdout IPC.
 * 
 * Memory Budget: 2GB max
 * Startup Target: <3 seconds
 */
@SpringBootApplication
public class Application {
    
    public static void main(String[] args) {
        // Disable Spring Boot banner to keep stdout clean for IPC
        SpringApplication app = new SpringApplication(Application.class);
        app.setBannerMode(Banner.Mode.OFF);
        
        ConfigurableApplicationContext context = app.run(args);
        
        // Log startup success to stderr (stdout reserved for IPC)
        System.err.println("[Backend] NeuralForge backend started successfully");
        System.err.println("[Backend] Memory: " + 
            (Runtime.getRuntime().totalMemory() / 1024 / 1024) + "MB allocated, " +
            (Runtime.getRuntime().freeMemory() / 1024 / 1024) + "MB free");
        
        // IPC handler will automatically start via @Component
    }
}
