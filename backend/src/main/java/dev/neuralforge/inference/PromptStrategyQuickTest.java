package dev.neuralforge.inference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quick test for prompt strategies
 */
public class PromptStrategyQuickTest {
    
    private static final Logger logger = LoggerFactory.getLogger(PromptStrategyQuickTest.class);
    
    public static void main(String[] args) {
        logger.info("========================================");
        logger.info("Prompt Strategy Quick Test");
        logger.info("========================================\n");
        
        // Create engine and inject dependencies
        T5InferenceEngine engine = new T5InferenceEngine();
        dev.neuralforge.tokenizer.TokenizerService tokenizerService = 
            new dev.neuralforge.tokenizer.TokenizerService();
        
        try {
            java.lang.reflect.Field field = T5InferenceEngine.class.getDeclaredField("tokenizerService");
            field.setAccessible(true);
            field.set(engine, tokenizerService);
        } catch (Exception e) {
            logger.error("Failed to inject TokenizerService", e);
            System.exit(1);
        }
        
        try {
            // Initialize
            logger.info("Initializing engine...");
            engine.initialize("codet5p-220m");
            
            // Test input
            String input = "public class Hello";
            logger.info("\nInput: '{}'\n", input);
            
            // Test strategies
            testStrategy(engine, input, PromptStrategy.NONE, "Baseline (no prompt)");
            testStrategy(engine, input, PromptStrategy.TASK_PREFIX, "Task prefix");
            testStrategy(engine, input, PromptStrategy.LANGUAGE_AWARE, "Language-aware");
            
            engine.shutdown();
            
            logger.info("\n========================================");
            logger.info("✓ Prompt strategy comparison complete");
            logger.info("========================================");
            
        } catch (Exception e) {
            logger.error("Test failed", e);
            System.exit(1);
        }
    }
    
    private static void testStrategy(T5InferenceEngine engine, String input, 
                                     PromptStrategy strategy, String label) {
        logger.info("--- {} ---", label);
        
        try {
            long start = System.currentTimeMillis();
            String output = engine.generate(input, strategy);
            long duration = System.currentTimeMillis() - start;
            
            String preview = output.length() > 80 ? output.substring(0, 80) + "..." : output;
            logger.info("Output: {}", preview);
            logger.info("Duration: {}ms\n", duration);
            
        } catch (Exception e) {
            logger.error("Strategy {} failed: {}\n", strategy, e.getMessage());
        }
    }
}
