package dev.neuralforge.inference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test suite for T5InferenceEngine
 * 
 * Tests full encoder-decoder inference pipeline
 */
public class T5InferenceEngineTest {
    
    private static final Logger logger = LoggerFactory.getLogger(T5InferenceEngineTest.class);
    private static dev.neuralforge.tokenizer.TokenizerProcessPool pool;
    
    public static void main(String[] args) {
        logger.info("========================================");
        logger.info("T5 Inference Engine Test Suite");
        logger.info("========================================\n");
        
        // Initialize process pool (shared for all tests)
        logger.info("Initializing tokenizer process pool (10-15s)...");
        pool = new dev.neuralforge.tokenizer.TokenizerProcessPool();
        try {
            pool.initialize();
            logger.info("Process pool ready!\n");
        } catch (Exception e) {
            logger.error("Failed to initialize process pool", e);
            System.exit(1);
        }
        
        // Create engine and manually inject dependencies (no Spring context in test)
        T5InferenceEngine engine = new T5InferenceEngine();
        dev.neuralforge.tokenizer.TokenizerService tokenizerService = 
            new dev.neuralforge.tokenizer.TokenizerService(pool);
        
        // Manually inject tokenizer (reflection hack for testing)
        try {
            java.lang.reflect.Field field = T5InferenceEngine.class.getDeclaredField("tokenizerService");
            field.setAccessible(true);
            field.set(engine, tokenizerService);
        } catch (Exception e) {
            logger.error("Failed to inject TokenizerService", e);
            System.exit(1);
        }
        
        boolean allPassed = true;
        
        // Test 1: Initialization
        allPassed &= testInitialization(engine);
        
        // Test 2: Simple code completion
        allPassed &= testSimpleCompletion(engine);
        
        // Test 3: Performance check
        allPassed &= testPerformance(engine);
        
        // Cleanup
        engine.shutdown();
        pool.shutdown();
        
        logger.info("\n========================================");
        if (allPassed) {
            logger.info("✓ ALL TESTS PASSED");
        } else {
            logger.error("✗ SOME TESTS FAILED");
            System.exit(1);
        }
        logger.info("========================================");
    }
    
    private static boolean testInitialization(T5InferenceEngine engine) {
        logger.info("TEST 1: Initialization");
        
        try {
            long startTime = System.currentTimeMillis();
            engine.initialize("codet5p-220m");
            long duration = System.currentTimeMillis() - startTime;
            
            logger.info("Initialization completed in {}ms", duration);
            
            if (!engine.isInitialized()) {
                logger.error("✗ FAIL: Engine not initialized");
                return false;
            }
            
            if (duration > 10000) {
                logger.warn("⚠ WARN: Initialization took {}ms (target: <10s)", duration);
            }
            
            logger.info("✓ PASS: Initialization successful\n");
            return true;
            
        } catch (Exception e) {
            logger.error("✗ FAIL: Initialization failed", e);
            return false;
        }
    }
    
    private static boolean testSimpleCompletion(T5InferenceEngine engine) {
        logger.info("TEST 2: Code Completion with Prompt Strategies");
        
        try {
            String inputCode = "def hello";
            logger.info("Input: '{}'", inputCode);
            
            // Test different prompt strategies
            PromptStrategy[] strategies = {
                PromptStrategy.NONE,
                PromptStrategy.TASK_PREFIX,
                PromptStrategy.LANGUAGE_AWARE
            };
            
            String bestCompletion = null;
            PromptStrategy bestStrategy = null;
            long bestDuration = Long.MAX_VALUE;
            
            for (PromptStrategy strategy : strategies) {
                logger.info("\n  Testing strategy: {}", strategy);
                
                long startTime = System.currentTimeMillis();
                String completion = engine.generate(inputCode, strategy);
                long duration = System.currentTimeMillis() - startTime;
                
                logger.info("  Output: '{}'", 
                    completion.length() > 60 ? completion.substring(0, 60) + "..." : completion);
                logger.info("  Duration: {}ms", duration);
                
                if (completion != null && !completion.isEmpty()) {
                    if (bestCompletion == null || duration < bestDuration) {
                        bestCompletion = completion;
                        bestStrategy = strategy;
                        bestDuration = duration;
                    }
                }
            }
            
            logger.info("\n  Best strategy: {} ({}ms)", bestStrategy, bestDuration);
            logger.info("  Best output: '{}'", 
                bestCompletion != null && bestCompletion.length() > 80 
                    ? bestCompletion.substring(0, 80) + "..." 
                    : bestCompletion);
            
            if (bestCompletion == null || bestCompletion.isEmpty()) {
                logger.error("✗ FAIL: No valid completion generated");
                return false;
            }
            
            logger.info("✓ PASS: Code completion with prompt strategies\n");
            return true;
            
        } catch (Exception e) {
            logger.error("✗ FAIL: Completion failed", e);
            e.printStackTrace();
            return false;
        }
    }
    
    private static boolean testPerformance(T5InferenceEngine engine) {
        logger.info("TEST 3: Performance Check (Multiple Runs)");
        
        try {
            String[] testInputs = {
                "public class",
                "import java.util",
                "for i in range"
            };
            
            long totalDuration = 0;
            int successCount = 0;
            
            for (String input : testInputs) {
                logger.info("Testing: '{}'", input);
                
                long startTime = System.currentTimeMillis();
                String completion = engine.generate(input);
                long duration = System.currentTimeMillis() - startTime;
                
                logger.info("  → '{}' ({}ms)", 
                    completion.length() > 30 ? completion.substring(0, 30) + "..." : completion,
                    duration);
                
                totalDuration += duration;
                successCount++;
            }
            
            long avgDuration = totalDuration / successCount;
            logger.info("Average duration: {}ms", avgDuration);
            
            if (avgDuration > 2000) {
                logger.warn("⚠ WARN: Average generation time {}ms (target: <2s)", avgDuration);
            }
            
            logger.info("✓ PASS: Performance check ({} completions, avg {}ms)\n", 
                successCount, avgDuration);
            return true;
            
        } catch (Exception e) {
            logger.error("✗ FAIL: Performance test failed", e);
            return false;
        }
    }
}
