package dev.neuralforge.model;

import ai.onnxruntime.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for loading and managing ONNX AI models.
 * 
 * Handles:
 * - Model loading from disk
 * - ONNX Runtime session management
 * - Memory-efficient inference
 * - Model metadata and configuration
 * 
 * Memory Budget: ~2GB for CodeT5+ 220M model
 * Target Latency: <100ms per inference
 */
@Service
public class ModelLoader {
    private static final Logger logger = LoggerFactory.getLogger(ModelLoader.class);
    
    private static final String MODELS_DIR = "../models/base";  // Relative to backend/
    private static final long MAX_MEMORY_MB = 2048; // 2GB limit
    
    private OrtEnvironment environment;
    private OrtSession session;
    private String loadedModelName;
    private boolean isLoaded = false;
    
    /**
     * Initialize ONNX Runtime environment.
     * Call this once during application startup.
     */
    public void initialize() throws OrtException {
        logger.info("Initializing ONNX Runtime environment...");
        long startTime = System.currentTimeMillis();
        
        environment = OrtEnvironment.getEnvironment();
        logger.info("ONNX Runtime initialized");
        
        long duration = System.currentTimeMillis() - startTime;
        logger.info("Initialization completed in {}ms", duration);
    }
    
    /**
     * Load a model from the models directory.
     * 
     * @param modelName Name of the model directory (e.g., "codet5p-220m")
     * @return true if loaded successfully
     */
    public boolean loadModel(String modelName) {
        if (isLoaded && modelName.equals(loadedModelName)) {
            logger.info("Model '{}' already loaded", modelName);
            return true;
        }
        
        logger.info("Loading model: {}", modelName);
        long startTime = System.currentTimeMillis();
        
        try {
            // Check available memory
            long availableMemoryMB = Runtime.getRuntime().freeMemory() / (1024 * 1024);
            logger.info("Available memory: {} MB", availableMemoryMB);
            
            if (availableMemoryMB < 1000) {
                logger.warn("Low memory warning: {} MB available (need ~1GB)", availableMemoryMB);
            }
            
            // Find model file
            Path modelPath = findModelFile(modelName);
            if (modelPath == null) {
                logger.error("Model file not found for: {}", modelName);
                return false;
            }
            
            logger.info("Model file: {}", modelPath);
            long modelSizeMB = Files.size(modelPath) / (1024 * 1024);
            logger.info("Model size: {} MB", modelSizeMB);
            
            // Close existing session if any
            if (session != null) {
                logger.info("Closing previous session: {}", loadedModelName);
                session.close();
                session = null;
                System.gc(); // Suggest GC to free memory
            }
            
            // Create session options for optimization
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            
            // Enable optimizations
            options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT);
            options.setIntraOpNumThreads(4); // Use 4 threads for inference
            
            // Load the model
            logger.info("Creating ONNX session...");
            session = environment.createSession(modelPath.toString(), options);
            
            // Log model info
            logModelInfo();
            
            loadedModelName = modelName;
            isLoaded = true;
            
            long duration = System.currentTimeMillis() - startTime;
            long finalMemoryMB = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
            
            logger.info("✓ Model loaded successfully in {}ms", duration);
            logger.info("  Memory usage: {} MB", finalMemoryMB);
            
            // Check if we're within budget
            if (duration > 5000) {
                logger.warn("⚠ Load time exceeded target (<5s): {}ms", duration);
            }
            if (finalMemoryMB > MAX_MEMORY_MB) {
                logger.warn("⚠ Memory usage exceeded budget ({}MB): {} MB", MAX_MEMORY_MB, finalMemoryMB);
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to load model '{}': {}", modelName, e.getMessage(), e);
            isLoaded = false;
            return false;
        }
    }
    
    /**
     * Find the ONNX model file in the model directory.
     */
    private Path findModelFile(String modelName) throws IOException {
        Path modelDir = Paths.get(MODELS_DIR, modelName);
        
        if (!Files.exists(modelDir)) {
            logger.error("Model directory not found: {}", modelDir);
            return null;
        }
        
        // Look for .onnx files
        return Files.walk(modelDir)
                .filter(p -> p.toString().endsWith(".onnx"))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Log information about the loaded model.
     */
    private void logModelInfo() throws OrtException {
        logger.info("Model inputs:");
        for (Map.Entry<String, NodeInfo> entry : session.getInputInfo().entrySet()) {
            logger.info("  - {}: {}", entry.getKey(), entry.getValue().getInfo());
        }
        
        logger.info("Model outputs:");
        for (Map.Entry<String, NodeInfo> entry : session.getOutputInfo().entrySet()) {
            logger.info("  - {}: {}", entry.getKey(), entry.getValue().getInfo());
        }
    }
    
    /**
     * Run inference on the model (placeholder for now).
     * 
     * @param inputText Code context for completion
     * @return Generated completion
     */
    public String infer(String inputText) throws OrtException {
        if (!isLoaded) {
            throw new IllegalStateException("No model loaded. Call loadModel() first.");
        }
        
        logger.info("Running inference (placeholder)...");
        logger.info("Input: {}", inputText);
        
        // TODO: Implement actual tokenization and inference
        // For now, just return a placeholder
        return "// TODO: Model inference not yet implemented\n// Input was: " + inputText;
    }
    
    /**
     * Unload the current model and free resources.
     */
    public void unloadModel() {
        if (session != null) {
            logger.info("Unloading model: {}", loadedModelName);
            try {
                session.close();
            } catch (Exception e) {
                logger.warn("Error closing session: {}", e.getMessage());
            }
            session = null;
            loadedModelName = null;
            isLoaded = false;
            System.gc();
            logger.info("Model unloaded, memory freed");
        }
    }
    
    /**
     * Cleanup on shutdown.
     */
    public void shutdown() {
        unloadModel();
        if (environment != null) {
            try {
                environment.close();
            } catch (Exception e) {
                logger.warn("Error closing ONNX environment: {}", e.getMessage());
            }
        }
        logger.info("ModelLoader shutdown complete");
    }
    
    // Getters
    public boolean isLoaded() {
        return isLoaded;
    }
    
    public String getLoadedModelName() {
        return loadedModelName;
    }
    
    public long getMemoryUsageMB() {
        return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
    }
}
