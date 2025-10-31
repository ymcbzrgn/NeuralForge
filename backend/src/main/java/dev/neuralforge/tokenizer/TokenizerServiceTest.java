package dev.neuralforge.tokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test suite for TokenizerService with TokenizerProcessPool
 * 
 * Tests tokenization of code strings using process pool (integration test)
 */
public class TokenizerServiceTest {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenizerServiceTest.class);
    
    public static void main(String[] args) {
        logger.info("========================================");
        logger.info("TokenizerService Test Suite (with Pool)");
        logger.info("========================================\n");
        
        // Initialize process pool
        TokenizerProcessPool pool = new TokenizerProcessPool();
        try {
            logger.info("Initializing process pool (this may take 10-15s)...");
            pool.initialize();
            logger.info("Process pool ready!\n");
        } catch (Exception e) {
            logger.error("Failed to initialize process pool", e);
            System.exit(1);
        }
        
        TokenizerService tokenizer = new TokenizerService(pool);
        boolean allPassed = true;
        
        // Test 1: Simple Python code
        allPassed &= testSimplePython(tokenizer);
        
        // Test 2: Java code
        allPassed &= testJavaCode(tokenizer);
        
        // Test 3: Empty input handling
        allPassed &= testEmptyInput(tokenizer);
        
        // Test 4: Performance check
        allPassed &= testPerformance(tokenizer);
        
        logger.info("\n========================================");
        if (allPassed) {
            logger.info("✓ ALL TESTS PASSED");
        } else {
            logger.error("✗ SOME TESTS FAILED");
            pool.shutdown();
            System.exit(1);
        }
        logger.info("========================================");
        
        // Shutdown pool
        logger.info("\nShutting down process pool...");
        pool.shutdown();
        logger.info("Done!");
    }
    
    private static boolean testSimplePython(TokenizerService tokenizer) {
        logger.info("TEST 1: Simple Python Code");
        
        try {
            String code = "def hello_world():\n    print('Hello')";
            TokenizerService.TokenizationResult result = tokenizer.tokenize(code);
            
            logger.info("Input: {} chars", code.length());
            logger.info("Output: {} tokens", result.getLength());
            logger.info("Duration: {}ms", result.getDurationMs());
            logger.info("Token IDs (first 5): {}", 
                result.getTokenIds().subList(0, Math.min(5, result.getLength())));
            
            // Validate
            if (result.getLength() == 0) {
                logger.error("✗ FAIL: No tokens generated");
                return false;
            }
            
            if (result.getTokenIds().size() != result.getAttentionMask().size()) {
                logger.error("✗ FAIL: Token IDs and attention mask length mismatch");
                return false;
            }
            
            logger.info("✓ PASS: Simple Python tokenization\n");
            return true;
            
        } catch (Exception e) {
            logger.error("✗ FAIL: Exception during tokenization", e);
            return false;
        }
    }
    
    private static boolean testJavaCode(TokenizerService tokenizer) {
        logger.info("TEST 2: Java Code");
        
        try {
            String code = "public class HelloWorld {\n" +
                         "    public static void main(String[] args) {\n" +
                         "        System.out.println(\"Hello, World!\");\n" +
                         "    }\n" +
                         "}";
            
            TokenizerService.TokenizationResult result = tokenizer.tokenize(code);
            
            logger.info("Input: {} chars", code.length());
            logger.info("Output: {} tokens", result.getLength());
            logger.info("Duration: {}ms", result.getDurationMs());
            
            // Validate
            if (result.getLength() == 0) {
                logger.error("✗ FAIL: No tokens generated");
                return false;
            }
            
            // Check that token count is reasonable (not 1:1 char:token)
            if (result.getLength() > code.length()) {
                logger.error("✗ FAIL: More tokens than characters (suspicious)");
                return false;
            }
            
            logger.info("✓ PASS: Java code tokenization\n");
            return true;
            
        } catch (Exception e) {
            logger.error("✗ FAIL: Exception during tokenization", e);
            return false;
        }
    }
    
    private static boolean testEmptyInput(TokenizerService tokenizer) {
        logger.info("TEST 3: Empty Input Handling");
        
        try {
            String code = "";
            tokenizer.tokenize(code);
            
            logger.error("✗ FAIL: Should throw exception for empty input");
            return false;
            
        } catch (TokenizerService.TokenizationException e) {
            logger.info("✓ PASS: Empty input rejected ({})\n", e.getMessage());
            return true;
        } catch (Exception e) {
            logger.error("✗ FAIL: Unexpected exception", e);
            return false;
        }
    }
    
    private static boolean testPerformance(TokenizerService tokenizer) {
        logger.info("TEST 4: Performance Check");
        
        try {
            // Generate ~100 token code sample
            StringBuilder codeBuilder = new StringBuilder();
            for (int i = 0; i < 10; i++) {
                codeBuilder.append("def function_").append(i).append("():\n");
                codeBuilder.append("    return ").append(i).append("\n\n");
            }
            String code = codeBuilder.toString();
            
            TokenizerService.TokenizationResult result = tokenizer.tokenize(code);
            
            logger.info("Input: {} chars", code.length());
            logger.info("Output: {} tokens", result.getLength());
            logger.info("Duration: {}ms", result.getDurationMs());
            
            // Performance target with pool: <500ms (no Python startup!)
            long maxDuration = 1000; // 1 second max (should be way under)
            
            if (result.getDurationMs() > maxDuration) {
                logger.error("✗ FAIL: Tokenization took {}ms (target: <{}ms)", 
                    result.getDurationMs(), maxDuration);
                return false;
            }
            
            logger.info("✓ PASS: Performance check ({}ms, target <{}ms)\n", 
                result.getDurationMs(), maxDuration);
            return true;
            
        } catch (Exception e) {
            logger.error("✗ FAIL: Exception during performance test", e);
            return false;
        }
    }
}
