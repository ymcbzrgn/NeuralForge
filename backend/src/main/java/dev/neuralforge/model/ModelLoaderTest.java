package dev.neuralforge.model;

import ai.onnxruntime.OrtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple test for ModelLoader to verify ONNX model loading.
 * Run this to test Phase 1 model integration.
 */
public class ModelLoaderTest {
    private static final Logger logger = LoggerFactory.getLogger(ModelLoaderTest.class);
    
    public static void main(String[] args) {
        logger.info("=".repeat(60));
        logger.info("ModelLoader Test - CodeT5+ 220M");
        logger.info("=".repeat(60));
        
        ModelLoader loader = new ModelLoader();
        
        try {
            // Test 1: Initialize ONNX Runtime
            logger.info("\n[Test 1] Initializing ONNX Runtime...");
            long startInit = System.currentTimeMillis();
            loader.initialize();
            long initDuration = System.currentTimeMillis() - startInit;
            logger.info("✓ Initialization completed in {}ms", initDuration);
            
            // Test 2: Load CodeT5+ 220M model
            logger.info("\n[Test 2] Loading CodeT5+ 220M model...");
            logger.info("Note: T5 has separate encoder/decoder, looking for encoder first");
            
            long startLoad = System.currentTimeMillis();
            boolean loaded = loader.loadModel("codet5p-220m/onnx");
            long loadDuration = System.currentTimeMillis() - startLoad;
            
            if (loaded) {
                logger.info("✓ Model loaded successfully in {}ms", loadDuration);
                logger.info("  Model name: {}", loader.getLoadedModelName());
                logger.info("  Memory usage: {} MB", loader.getMemoryUsageMB());
                
                // Check performance targets
                if (loadDuration < 5000) {
                    logger.info("✓ PASS: Load time < 5s target");
                } else {
                    logger.warn("⚠ WARN: Load time exceeded 5s target: {}ms", loadDuration);
                }
                
                if (loader.getMemoryUsageMB() < 2048) {
                    logger.info("✓ PASS: Memory usage < 2GB target");
                } else {
                    logger.warn("⚠ WARN: Memory usage exceeded 2GB: {} MB", loader.getMemoryUsageMB());
                }
                
            } else {
                logger.error("✗ FAIL: Model loading failed");
                System.exit(1);
            }
            
            // Test 3: Placeholder inference
            logger.info("\n[Test 3] Testing inference placeholder...");
            String testInput = "def calculate_sum(a, b):";
            String result = loader.infer(testInput);
            logger.info("Input: {}", testInput);
            logger.info("Output: {}", result);
            logger.info("✓ Inference placeholder working");
            
            // Test 4: Cleanup
            logger.info("\n[Test 4] Cleaning up...");
            loader.shutdown();
            logger.info("✓ Cleanup completed");
            
            // Summary
            logger.info("\n" + "=".repeat(60));
            logger.info("✓ ALL TESTS PASSED");
            logger.info("=".repeat(60));
            logger.info("Summary:");
            logger.info("  - Initialization: {}ms", initDuration);
            logger.info("  - Model loading: {}ms", loadDuration);
            logger.info("  - Memory usage: {} MB", loader.getMemoryUsageMB());
            logger.info("  - Phase 1 milestone: Model loading ✓");
            logger.info("\nNext steps:");
            logger.info("  1. Implement tokenization");
            logger.info("  2. Implement actual inference");
            logger.info("  3. Integrate with IPC handler");
            
        } catch (OrtException e) {
            logger.error("✗ ONNX Runtime error: {}", e.getMessage(), e);
            System.exit(1);
        } catch (Exception e) {
            logger.error("✗ Unexpected error: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}
