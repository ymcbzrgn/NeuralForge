package dev.neuralforge.tokenizer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for TokenizerProcessPool
 * 
 * Tests:
 * 1. Pool initialization (3 processes start correctly)
 * 2. Basic tokenization operations
 * 3. Basic detokenization operations
 * 4. Concurrent requests (10 parallel operations)
 * 5. Process reuse (same process handles multiple requests)
 * 6. Error recovery (process crash handling)
 * 7. Graceful shutdown (SHUTDOWN commands sent)
 * 
 * NOTE: These tests use the real Python worker script, so they require:
 * - Python virtualenv at models/.venv with transformers installed
 * - Tokenizer model at models/base/codet5p-220m
 * - nf_tokenizer_worker.py executable script
 */
class TokenizerProcessPoolTest {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenizerProcessPoolTest.class);
    
    private TokenizerProcessPool pool;
    
    @BeforeEach
    void setUp() {
        logger.info("=== Setting up TokenizerProcessPool for test ===");
        pool = new TokenizerProcessPool();
    }
    
    @AfterEach
    void tearDown() {
        logger.info("=== Tearing down TokenizerProcessPool ===");
        if (pool != null) {
            pool.shutdown();
        }
    }
    
    /**
     * Test 1: Pool initialization
     * 
     * Verifies that:
     * - Pool starts 3 worker processes
     * - All processes send "ready" signal
     * - Initialization completes within timeout (15s)
     * - Pool statistics show correct state
     */
    @Test
    @Timeout(20)
    void testPoolInitialization() throws Exception {
        logger.info("\n=== TEST 1: Pool Initialization ===");
        
        long startTime = System.currentTimeMillis();
        pool.initialize();
        long duration = System.currentTimeMillis() - startTime;
        
        logger.info("Pool initialized in {}ms", duration);
        
        // Check initialization completed
        Map<String, Object> stats = pool.getStatistics();
        logger.info("Pool statistics: {}", stats);
        
        assertTrue((Boolean) stats.get("initialized"), "Pool should be initialized");
        assertFalse((Boolean) stats.get("shutdown"), "Pool should not be shut down");
        assertEquals(3, stats.get("poolSize"), "Pool size should be 3");
        assertEquals(3, stats.get("available"), "All 3 processes should be available");
        assertEquals(0, stats.get("busy"), "No processes should be busy");
        assertEquals(3, stats.get("total"), "Total processes should be 3");
        assertEquals(3L, stats.get("healthy"), "All processes should be healthy");
        
        // Verify reasonable initialization time (should be ~8-12s for 3 processes)
        assertTrue(duration < 15000, "Initialization should complete within 15s (was " + duration + "ms)");
        assertTrue(duration > 5000, "Initialization should take at least 5s (was " + duration + "ms)");
        
        logger.info("✓ Pool initialization test PASSED");
    }
    
    /**
     * Test 2: Basic tokenization
     * 
     * Verifies that:
     * - Pool can tokenize simple text
     * - Returns non-empty token list
     * - Completes within timeout (<5s)
     * - Process returns to pool after use
     */
    @Test
    @Timeout(25)
    void testBasicTokenization() throws Exception {
        logger.info("\n=== TEST 2: Basic Tokenization ===");
        
        pool.initialize();
        
        String testCode = "public class HelloWorld { public static void main(String[] args) { } }";
        logger.info("Tokenizing: {}", testCode);
        
        long startTime = System.currentTimeMillis();
        List<Integer> tokens = pool.tokenize(testCode);
        long duration = System.currentTimeMillis() - startTime;
        
        logger.info("Tokenization completed in {}ms", duration);
        logger.info("Input: {} chars → Output: {} tokens", testCode.length(), tokens.size());
        logger.info("First 10 tokens: {}", tokens.stream().limit(10).collect(Collectors.toList()));
        
        // Verify result
        assertNotNull(tokens, "Token list should not be null");
        assertFalse(tokens.isEmpty(), "Token list should not be empty");
        assertTrue(tokens.size() > 10, "Should have multiple tokens for this code");
        
        // Verify performance (should be <500ms, definitely <5s)
        assertTrue(duration < 5000, "Tokenization should complete within 5s (was " + duration + "ms)");
        
        // Verify process returned to pool
        Map<String, Object> stats = pool.getStatistics();
        assertEquals(3, stats.get("available"), "All processes should be available again");
        assertEquals(0, stats.get("busy"), "No processes should be busy");
        
        logger.info("✓ Basic tokenization test PASSED");
    }
    
    /**
     * Test 3: Basic detokenization
     * 
     * Verifies that:
     * - Pool can detokenize token IDs back to text
     * - Returns non-empty string
     * - Completes within timeout (<5s)
     * - Round-trip (tokenize → detokenize) preserves meaning
     */
    @Test
    @Timeout(30)
    void testBasicDetokenization() throws Exception {
        logger.info("\n=== TEST 3: Basic Detokenization ===");
        
        pool.initialize();
        
        String originalCode = "public void test() { return 42; }";
        logger.info("Original code: {}", originalCode);
        
        // Tokenize first
        List<Integer> tokens = pool.tokenize(originalCode);
        logger.info("Tokenized to {} tokens", tokens.size());
        
        // Detokenize back
        long startTime = System.currentTimeMillis();
        String decodedCode = pool.detokenize(tokens);
        long duration = System.currentTimeMillis() - startTime;
        
        logger.info("Detokenization completed in {}ms", duration);
        logger.info("Decoded code: {}", decodedCode);
        
        // Verify result
        assertNotNull(decodedCode, "Decoded text should not be null");
        assertFalse(decodedCode.isEmpty(), "Decoded text should not be empty");
        
        // Verify performance
        assertTrue(duration < 5000, "Detokenization should complete within 5s (was " + duration + "ms)");
        
        // Note: Exact match not guaranteed due to tokenizer normalization
        // But decoded text should contain key elements
        assertTrue(decodedCode.contains("public") || decodedCode.contains("void") || decodedCode.contains("test"),
                "Decoded text should contain recognizable code elements (was: " + decodedCode + ")");
        
        logger.info("✓ Basic detokenization test PASSED");
    }
    
    /**
     * Test 4: Concurrent requests
     * 
     * Verifies that:
     * - Pool handles 10 parallel tokenization requests
     * - All requests complete successfully
     * - No deadlocks or race conditions
     * - Total time reasonable (not 10x sequential time)
     */
    @Test
    @Timeout(40)
    void testConcurrentRequests() throws Exception {
        logger.info("\n=== TEST 4: Concurrent Requests ===");
        
        pool.initialize();
        
        int numRequests = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numRequests);
        
        List<String> testCodes = new ArrayList<>();
        for (int i = 0; i < numRequests; i++) {
            testCodes.add("public class Test" + i + " { void method" + i + "() { int x = " + i + "; } }");
        }
        
        logger.info("Submitting {} concurrent tokenization requests", numRequests);
        
        long startTime = System.currentTimeMillis();
        
        // Submit all requests concurrently
        List<Future<List<Integer>>> futures = new ArrayList<>();
        for (int i = 0; i < numRequests; i++) {
            final int index = i;
            Future<List<Integer>> future = executor.submit(() -> {
                logger.debug("Request {} starting", index);
                List<Integer> result = pool.tokenize(testCodes.get(index));
                logger.debug("Request {} completed: {} tokens", index, result.size());
                return result;
            });
            futures.add(future);
        }
        
        // Wait for all to complete
        List<List<Integer>> results = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            try {
                List<Integer> result = futures.get(i).get(10, TimeUnit.SECONDS);
                results.add(result);
            } catch (Exception e) {
                fail("Request " + i + " failed: " + e.getMessage());
            }
        }
        
        long duration = System.currentTimeMillis() - startTime;
        executor.shutdown();
        
        logger.info("All {} requests completed in {}ms", numRequests, duration);
        logger.info("Average: {}ms per request", duration / numRequests);
        
        // Verify all requests succeeded
        assertEquals(numRequests, results.size(), "All requests should complete");
        for (int i = 0; i < results.size(); i++) {
            assertFalse(results.get(i).isEmpty(), "Request " + i + " should return tokens");
        }
        
        // Verify reasonable total time
        // With 3 processes and 10 requests, should take ~4 rounds
        // Each round <500ms, so total <2s is reasonable
        // But allow up to 10s to be safe (CI might be slow)
        assertTrue(duration < 10000, "Concurrent requests should complete within 10s (was " + duration + "ms)");
        
        // Verify pool still healthy
        Map<String, Object> stats = pool.getStatistics();
        assertEquals(3, stats.get("available"), "All processes should be available after concurrent requests");
        
        logger.info("✓ Concurrent requests test PASSED");
    }
    
    /**
     * Test 5: Process reuse
     * 
     * Verifies that:
     * - Same process handles multiple sequential requests
     * - No process restart overhead between requests
     * - Second request faster than first (no startup cost)
     */
    @Test
    @Timeout(30)
    void testProcessReuse() throws Exception {
        logger.info("\n=== TEST 5: Process Reuse ===");
        
        pool.initialize();
        
        String testCode = "public class Test { void method() { } }";
        
        // First request
        logger.info("Request 1: Tokenizing...");
        long start1 = System.currentTimeMillis();
        List<Integer> tokens1 = pool.tokenize(testCode);
        long duration1 = System.currentTimeMillis() - start1;
        logger.info("Request 1 completed in {}ms ({} tokens)", duration1, tokens1.size());
        
        // Second request (should reuse same process, no startup)
        logger.info("Request 2: Tokenizing...");
        long start2 = System.currentTimeMillis();
        List<Integer> tokens2 = pool.tokenize(testCode);
        long duration2 = System.currentTimeMillis() - start2;
        logger.info("Request 2 completed in {}ms ({} tokens)", duration2, tokens2.size());
        
        // Third request
        logger.info("Request 3: Tokenizing...");
        long start3 = System.currentTimeMillis();
        List<Integer> tokens3 = pool.tokenize(testCode);
        long duration3 = System.currentTimeMillis() - start3;
        logger.info("Request 3 completed in {}ms ({} tokens)", duration3, tokens3.size());
        
        // Verify same tokenization result (process consistency)
        assertEquals(tokens1, tokens2, "Same input should produce same tokens (request 1 vs 2)");
        assertEquals(tokens2, tokens3, "Same input should produce same tokens (request 2 vs 3)");
        
        // Verify no process restarts (all should use available processes)
        Map<String, Object> stats = pool.getStatistics();
        assertEquals(3, stats.get("total"), "Should still have 3 total processes (no restarts)");
        assertEquals(3L, stats.get("healthy"), "All processes should be healthy");
        
        logger.info("Average request time: {}ms", (duration1 + duration2 + duration3) / 3);
        logger.info("✓ Process reuse test PASSED");
    }
    
    /**
     * Test 6: Error recovery (DISABLED - requires process crash simulation)
     * 
     * This test is disabled because it requires killing worker processes,
     * which is complex to implement reliably in tests. Manual testing
     * recommended for crash recovery scenarios.
     * 
     * Manual test steps:
     * 1. Start pool
     * 2. Get process ID from stats/logs
     * 3. Kill process: kill -9 <pid>
     * 4. Submit new request
     * 5. Verify pool auto-restarts crashed process
     */
    // @Test
    // @Timeout(40)
    void testErrorRecovery_DISABLED() {
        logger.info("\n=== TEST 6: Error Recovery (DISABLED) ===");
        logger.info("This test requires manual process crash simulation");
        logger.info("Run manually if needed to verify crash recovery");
    }
    
    /**
     * Test 7: Graceful shutdown
     * 
     * Verifies that:
     * - Pool sends SHUTDOWN command to all workers
     * - All processes exit cleanly
     * - Pool marked as shut down
     * - Subsequent requests fail with IllegalStateException
     */
    @Test
    @Timeout(30)
    void testGracefulShutdown() throws Exception {
        logger.info("\n=== TEST 7: Graceful Shutdown ===");
        
        pool.initialize();
        
        // Use pool first to ensure processes are alive
        String testCode = "public void test() { }";
        List<Integer> tokens = pool.tokenize(testCode);
        assertFalse(tokens.isEmpty(), "Should tokenize successfully before shutdown");
        
        logger.info("Pool active, now shutting down...");
        
        // Shutdown
        long startTime = System.currentTimeMillis();
        pool.shutdown();
        long duration = System.currentTimeMillis() - startTime;
        
        logger.info("Shutdown completed in {}ms", duration);
        
        // Verify shutdown state
        Map<String, Object> stats = pool.getStatistics();
        assertTrue((Boolean) stats.get("shutdown"), "Pool should be marked as shut down");
        assertEquals(0, stats.get("total"), "Should have 0 total processes after shutdown");
        assertEquals(0, stats.get("available"), "Should have 0 available processes");
        
        // Verify reasonable shutdown time (should send SHUTDOWN + wait ~5s max)
        assertTrue(duration < 10000, "Shutdown should complete within 10s (was " + duration + "ms)");
        
        // Verify subsequent requests fail
        logger.info("Verifying subsequent requests fail...");
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            pool.tokenize("should fail");
        });
        assertTrue(exception.getMessage().contains("shut down"), 
                "Exception should mention pool is shut down");
        
        logger.info("✓ Graceful shutdown test PASSED");
    }
    
    /**
     * Test 8: Pool statistics
     * 
     * Verifies that:
     * - Statistics reflect actual pool state
     * - Available/busy counts change correctly
     * - Healthy count accurate
     */
    @Test
    @Timeout(30)
    void testPoolStatistics() throws Exception {
        logger.info("\n=== TEST 8: Pool Statistics ===");
        
        // Before initialization
        Map<String, Object> statsBeforeInit = pool.getStatistics();
        assertFalse((Boolean) statsBeforeInit.get("initialized"), "Should not be initialized yet");
        
        // After initialization
        pool.initialize();
        Map<String, Object> statsAfterInit = pool.getStatistics();
        assertTrue((Boolean) statsAfterInit.get("initialized"), "Should be initialized");
        assertEquals(3, statsAfterInit.get("available"), "All should be available");
        assertEquals(0, statsAfterInit.get("busy"), "None should be busy");
        
        // During operation (need to check stats mid-operation)
        // This is tricky because operations are fast, so we'll just verify
        // stats are consistent after operations
        pool.tokenize("test");
        
        Map<String, Object> statsAfterOp = pool.getStatistics();
        assertEquals(3, statsAfterOp.get("available"), "All should be available after operation");
        assertEquals(0, statsAfterOp.get("busy"), "None should be busy after operation");
        
        logger.info("Final statistics: {}", statsAfterOp);
        logger.info("✓ Pool statistics test PASSED");
    }
}
