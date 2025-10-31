package dev.neuralforge.ipc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.neuralforge.inference.T5InferenceEngine;
import dev.neuralforge.tokenizer.TokenizerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Integration test for IPC inference endpoint
 * Tests the full pipeline: IPC request → T5InferenceEngine → Response
 */
public class IPCInferenceIntegrationTest {
    
    private static final Logger logger = LoggerFactory.getLogger(IPCInferenceIntegrationTest.class);
    
    public void testInferenceRequest() throws Exception {
        logger.info("=== IPC Inference Integration Test ===");
        
        // Initialize process pool
        logger.info("Initializing tokenizer process pool (10-15s)...");
        dev.neuralforge.tokenizer.TokenizerProcessPool pool = 
            new dev.neuralforge.tokenizer.TokenizerProcessPool();
        pool.initialize();
        logger.info("Process pool ready!");
        
        // Create components manually (no Spring context)
        IPCHandler handler = new IPCHandler();
        T5InferenceEngine engine = new T5InferenceEngine();
        TokenizerService tokenizerService = new TokenizerService(pool);
        
        // Inject dependencies via reflection
        injectDependency(handler, "inferenceEngine", engine);
        injectDependency(engine, "tokenizerService", tokenizerService);
        
        // Build inference request
        ObjectMapper mapper = new ObjectMapper();
        String requestJson = """
            {
                "type": "infer",
                "id": "test-123",
                "code": "def hello",
                "language": "python",
                "strategy": "TASK_PREFIX",
                "modelName": "codet5p-220m"
            }
            """;
        
        JsonNode request = mapper.readTree(requestJson);
        
        // Call handleRequest via reflection (it's private)
        Method handleMethod = IPCHandler.class.getDeclaredMethod("handleRequest", JsonNode.class);
        handleMethod.setAccessible(true);
        
        logger.info("Sending inference request: code='def hello', strategy=TASK_PREFIX");
        long startTime = System.currentTimeMillis();
        
        JsonNode response = (JsonNode) handleMethod.invoke(handler, request);
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Verify response (using simple assertions, no JUnit)
        if (response == null) {
            throw new AssertionError("Response should not be null");
        }
        
        String responseType = response.path("type").asText();
        logger.info("Response type: {}", responseType);
        
        if ("error".equals(responseType)) {
            String errorMsg = response.path("message").asText();
            logger.error("Inference failed: {}", errorMsg);
            throw new AssertionError("Inference returned error: " + errorMsg);
        }
        
        if (!"completion".equals(responseType)) {
            throw new AssertionError("Response type should be 'completion', got: " + responseType);
        }
        
        if (!"test-123".equals(response.path("id").asText())) {
            throw new AssertionError("Response should preserve request ID");
        }
        
        String completionText = response.path("text").asText();
        long latencyMs = response.path("latencyMs").asLong();
        
        if (completionText.isEmpty()) {
            throw new AssertionError("Completion text should not be empty");
        }
        
        if (latencyMs <= 0) {
            throw new AssertionError("Latency should be positive, got: " + latencyMs);
        }
        
        logger.info("✓ Inference successful!");
        logger.info("  Completion: {} chars", completionText.length());
        logger.info("  Latency: {}ms", latencyMs);
        logger.info("  Total test duration: {}ms", duration);
        
        // Print first 100 chars of completion
        String preview = completionText.length() > 100 
            ? completionText.substring(0, 100) + "..." 
            : completionText;
        logger.info("  Preview: {}", preview);
        
        // Cleanup
        pool.shutdown();
        
        logger.info("=== TEST PASSED ✅ ===");
    }
    
    /**
     * Helper to inject dependencies via reflection (no Spring)
     */
    private void injectDependency(Object target, String fieldName, Object dependency) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, dependency);
    }
    
    /**
     * Standalone main for running test without JUnit runner
     */
    public static void main(String[] args) {
        IPCInferenceIntegrationTest test = new IPCInferenceIntegrationTest();
        try {
            test.testInferenceRequest();
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
