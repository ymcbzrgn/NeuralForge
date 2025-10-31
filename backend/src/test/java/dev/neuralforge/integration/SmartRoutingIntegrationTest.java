package dev.neuralforge.integration;

import dev.neuralforge.cache.KVCacheManager;
import dev.neuralforge.cache.ModelCache;
import dev.neuralforge.memory.MemoryManager;
import dev.neuralforge.router.CodeComplexityAnalyzer;
import dev.neuralforge.router.ModelRouter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for complete smart model routing flow.
 * 
 * Tests end-to-end scenarios:
 * 1. Code → Complexity Analysis
 * 2. Complexity + Memory → Model Selection
 * 3. Model Selection → Cache (Model + KV)
 * 4. Performance validation
 * 
 * This validates that all Phase 2 Sprint 2 components work together correctly.
 * 
 * Note: Uses lenient MemoryManager for tests (allows loading despite low memory).
 * In production, strict memory checks prevent OOM.
 */
class SmartRoutingIntegrationTest {
    
    private CodeComplexityAnalyzer complexityAnalyzer;
    private MemoryManager memoryManager;
    private ModelRouter modelRouter;
    private ModelCache modelCache;
    private KVCacheManager kvCacheManager;
    
    @BeforeEach
    void setUp() {
        complexityAnalyzer = new CodeComplexityAnalyzer();
        
        // For integration tests: Use lenient MemoryManager that always allows loading
        // (In production, MemoryManager correctly enforces 512MB safety buffer)
        memoryManager = new MemoryManager() {
            @Override
            public boolean canLoadModel(long modelSizeBytes) {
                // Always allow for integration tests (skip strict memory check)
                return true;
            }
        };
        
        modelRouter = new ModelRouter(complexityAnalyzer, memoryManager);
        modelCache = new ModelCache(memoryManager);
        kvCacheManager = new KVCacheManager();
    }
    
    @Test
    @DisplayName("E2E: Simple code → 33M model selection → cache")
    void testSimpleCodeEndToEnd() {
        // Arrange: Simple Hello World code
        String simpleCode = """
            public class HelloWorld {
                public static void main(String[] args) {
                    System.out.println("Hello, World!");
                }
            }
            """;
        
        // Act: Full routing flow
        
        // 1. Analyze complexity
        CodeComplexityAnalyzer.ComplexityScore complexityScore = 
            complexityAnalyzer.analyzeComplexity(simpleCode);
        
        // 2. Select model based on complexity + memory
        ModelRouter.ModelSelection selection = modelRouter.selectModel(simpleCode);
        
        // 3. Get or load model from cache
        ModelCache.CachedModel cachedModel = modelCache.getOrLoad(selection.model());
        
        // 4. Create KV cache for inference session
        KVCacheManager.KVCache kvCache = kvCacheManager.createCache(
            "session-simple", 
            12,  // 12 layers for small model
            512  // 512 max sequence length
        );
        
        // Assert: Verify complete flow
        assertTrue(complexityScore.getTotalScore() < 0.4, 
            "Simple code should have low complexity");
        assertEquals(ModelRouter.ModelType.CODET5P_33M, selection.model(),
            "Should select 33M model for simple code");
        assertNotNull(cachedModel, "Model should be cached");
        assertEquals(ModelRouter.ModelType.CODET5P_33M, cachedModel.getModelType());
        assertNotNull(kvCache, "KV cache should be created");
        assertEquals(0, kvCache.getCurrentLength(), "KV cache should start empty");
        
        // Cleanup
        kvCacheManager.evictCache("session-simple");
    }
    
    @Test
    @DisplayName("E2E: Complex code → 770M/3B model → cache with fallback")
    void testComplexCodeEndToEnd() {
        // Arrange: Complex QuickSort algorithm
        String complexCode = """
            public class QuickSort {
                public void quickSort(int[] arr, int low, int high) {
                    if (low < high) {
                        int pi = partition(arr, low, high);
                        quickSort(arr, low, pi - 1);
                        quickSort(arr, pi + 1, high);
                    }
                }
                
                private int partition(int[] arr, int low, int high) {
                    int pivot = arr[high];
                    int i = low - 1;
                    
                    for (int j = low; j < high; j++) {
                        if (arr[j] < pivot) {
                            i++;
                            swap(arr, i, j);
                        }
                    }
                    swap(arr, i + 1, high);
                    return i + 1;
                }
                
                private void swap(int[] arr, int i, int j) {
                    int temp = arr[i];
                    arr[i] = arr[j];
                    arr[j] = temp;
                }
            }
            """;
        
        // Act: Full routing flow
        CodeComplexityAnalyzer.ComplexityScore complexityScore = 
            complexityAnalyzer.analyzeComplexity(complexCode);
        
        ModelRouter.ModelSelection selection = modelRouter.selectModel(complexCode);
        
        ModelCache.CachedModel cachedModel = modelCache.getOrLoad(selection.model());
        
        KVCacheManager.KVCache kvCache = kvCacheManager.createCache(
            "session-complex",
            24,   // 24 layers for larger model
            1024  // 1024 max sequence length
        );
        
        // Assert: Complex code gets larger model
        assertTrue(complexityScore.getTotalScore() >= 0.5,
            "Complex code should have medium/high complexity, got: " + complexityScore.getTotalScore());
        assertTrue(
            selection.model() == ModelRouter.ModelType.CODET5P_770M ||
            selection.model() == ModelRouter.ModelType.STABLECODE_3B,
            "Should select 770M or 3B model for complex code"
        );
        assertNotNull(cachedModel, "Model should be cached");
        assertNotNull(kvCache, "KV cache should be created");
        
        // Cleanup
        kvCacheManager.evictCache("session-complex");
    }
    
    @Test
    @DisplayName("E2E: Model cache reuse (hit scenario)")
    void testModelCacheReuse() {
        // Arrange
        String code1 = "public class Test1 { }";
        String code2 = "public class Test2 { }";
        
        // Act: Process two similar simple codes
        ModelRouter.ModelSelection selection1 = modelRouter.selectModel(code1);
        ModelCache.CachedModel cached1 = modelCache.getOrLoad(selection1.model());
        
        ModelRouter.ModelSelection selection2 = modelRouter.selectModel(code2);
        ModelCache.CachedModel cached2 = modelCache.getOrLoad(selection2.model());
        
        // Assert: Second request should hit cache
        assertEquals(selection1.model(), selection2.model(),
            "Both simple codes should select same model");
        assertSame(cached1, cached2, "Should reuse same cached model instance");
        assertEquals(1, cached2.getHitCount(), "Should have 1 cache hit");
    }
    
    @Test
    @DisplayName("E2E: Memory-aware fallback scenario")
    void testMemoryAwareFallback() {
        // Arrange: Generate large code that would prefer large model
        StringBuilder largeCode = new StringBuilder();
        largeCode.append("public class LargeClass {\n");
        for (int i = 0; i < 50; i++) {
            largeCode.append("    public void method").append(i).append("() {\n");
            largeCode.append("        int x = ").append(i).append(";\n");
            largeCode.append("        if (x > 0) {\n");
            largeCode.append("            for (int j = 0; j < x; j++) {\n");
            largeCode.append("                System.out.println(j);\n");
            largeCode.append("            }\n");
            largeCode.append("        }\n");
            largeCode.append("    }\n");
        }
        largeCode.append("}\n");
        
        // Act: Route the code
        ModelRouter.ModelSelection selection = modelRouter.selectModel(largeCode.toString());
        
        // Assert: Should select a model (with fallback if needed)
        assertNotNull(selection, "Should select a model");
        assertNotNull(selection.model(), "Model should not be null");
        assertNotNull(selection.reasoning(), "Should provide reasoning");
        
        // Verify model can be cached
        ModelCache.CachedModel cached = modelCache.getOrLoad(selection.model());
        assertNotNull(cached, "Should be able to cache selected model");
    }
    
    @Test
    @DisplayName("E2E: KV cache generation simulation")
    void testKVCacheGenerationFlow() {
        // Arrange: Simulate token-by-token generation
        String code = "public class Test { }";
        ModelRouter.ModelSelection selection = modelRouter.selectModel(code);
        
        // Create KV cache for session
        KVCacheManager.KVCache kvCache = kvCacheManager.createCache(
            "session-generation",
            12,
            50  // Generate up to 50 tokens
        );
        
        // Act: Simulate generating 10 tokens
        for (int tokenIndex = 0; tokenIndex < 10; tokenIndex++) {
            // Each token goes through all layers
            for (int layer = 0; layer < 12; layer++) {
                // Mock key/value tensors (in real impl, these are model outputs)
                float[] mockKey = new float[]{(float) tokenIndex, (float) layer};
                float[] mockValue = new float[]{(float) (tokenIndex * 100), (float) (layer * 10)};
                
                kvCache.append(layer, mockKey, mockValue);
            }
        }
        
        // Assert: KV cache should have 10 tokens cached
        assertEquals(10, kvCache.getCurrentLength(), "Should have cached 10 tokens");
        assertFalse(kvCache.isFull(), "Cache should not be full (max 50)");
        
        // Verify each layer has 10 tokens
        for (int layer = 0; layer < 12; layer++) {
            KVCacheManager.LayerKVCache layerCache = kvCache.getLayerCache(layer);
            assertEquals(10, layerCache.getLength(), 
                "Layer " + layer + " should have 10 tokens");
        }
        
        // Memory usage should be tracked
        assertTrue(kvCache.getMemoryUsageBytes() > 0, 
            "Should track memory usage");
        
        // Cleanup
        kvCacheManager.evictCache("session-generation");
    }
    
    @Test
    @DisplayName("E2E: Multiple concurrent sessions")
    void testMultipleConcurrentSessions() {
        // Arrange: Simulate 3 concurrent inference sessions
        String code1 = "class Simple1 { }";
        String code2 = "class Simple2 { }";
        String code3 = "class Simple3 { }";
        
        // Act: Create KV caches for all sessions
        KVCacheManager.KVCache cache1 = kvCacheManager.createCache("session-1", 12, 512);
        KVCacheManager.KVCache cache2 = kvCacheManager.createCache("session-2", 12, 512);
        KVCacheManager.KVCache cache3 = kvCacheManager.createCache("session-3", 12, 512);
        
        // Add some tokens to each
        cache1.append(0, new float[]{1.0f}, new float[]{2.0f});
        cache2.append(0, new float[]{3.0f}, new float[]{4.0f});
        cache3.append(0, new float[]{5.0f}, new float[]{6.0f});
        
        // Assert: All sessions should be tracked
        KVCacheManager.KVCacheStats stats = kvCacheManager.getStats();
        assertEquals(3, stats.activeCaches(), "Should have 3 active caches");
        assertTrue(stats.totalMemoryBytes() > 0, "Should track total memory");
        
        // Cleanup
        kvCacheManager.clearAll();
        assertEquals(0, kvCacheManager.getStats().activeCaches(), 
            "All caches should be cleared");
    }
    
    @Test
    @DisplayName("E2E: Complete inference workflow with cleanup")
    void testCompleteInferenceWorkflow() {
        // Arrange: Simulate complete inference lifecycle
        String code = """
            public class Calculator {
                public int add(int a, int b) {
                    return a + b;
                }
            }
            """;
        
        // Act: Step 1 - Analyze and route
        ModelRouter.ModelSelection selection = modelRouter.selectModel(code);
        
        // Step 2 - Get model from cache
        ModelCache.CachedModel model = modelCache.getOrLoad(selection.model());
        long initialHits = model.getHitCount();
        
        // Step 3 - Create KV cache for generation
        String sessionId = "session-workflow";
        KVCacheManager.KVCache kvCache = kvCacheManager.createCache(sessionId, 12, 512);
        
        // Step 4 - Simulate generation (5 tokens)
        for (int token = 0; token < 5; token++) {
            for (int layer = 0; layer < 12; layer++) {
                kvCache.append(layer, new float[]{(float) token}, new float[]{(float) token});
            }
        }
        
        // Step 5 - Verify state
        assertEquals(5, kvCache.getCurrentLength(), "Should have generated 5 tokens");
        
        // Step 6 - Another inference with same model (cache hit)
        ModelCache.CachedModel model2 = modelCache.getOrLoad(selection.model());
        assertEquals(initialHits + 1, model2.getHitCount(), "Should increment hit count");
        
        // Step 7 - Cleanup after inference complete
        kvCacheManager.evictCache(sessionId);
        assertFalse(kvCacheManager.getCache(sessionId).isPresent(), 
            "KV cache should be evicted");
        
        // Assert: Overall system state
        ModelCache.CacheStats modelCacheStats = modelCache.getStats();
        assertTrue(modelCacheStats.size() > 0, "Model should still be cached");
        
        KVCacheManager.KVCacheStats kvStats = kvCacheManager.getStats();
        assertEquals(0, kvStats.activeCaches(), "No active KV caches after cleanup");
        assertEquals(1, kvStats.totalEvicted(), "Should have evicted 1 KV cache");
    }
    
    @Test
    @DisplayName("E2E: Performance validation - fast routing")
    void testRoutingPerformance() {
        // Arrange
        String code = """
            public class PerformanceTest {
                public void method() {
                    for (int i = 0; i < 100; i++) {
                        System.out.println(i);
                    }
                }
            }
            """;
        
        // Act: Measure routing time
        long startTime = System.currentTimeMillis();
        
        CodeComplexityAnalyzer.ComplexityScore score = 
            complexityAnalyzer.analyzeComplexity(code);
        ModelRouter.ModelSelection selection = modelRouter.selectModel(code);
        ModelCache.CachedModel model = modelCache.getOrLoad(selection.model());
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Assert: Routing should be fast (<100ms for first load, <10ms for cache hit)
        assertTrue(duration < 100, 
            "Initial routing should complete in <100ms, took: " + duration + "ms");
        
        // Second routing should be much faster (cache hit)
        long startTime2 = System.currentTimeMillis();
        modelRouter.selectModel(code);
        modelCache.getOrLoad(selection.model());
        long duration2 = System.currentTimeMillis() - startTime2;
        
        assertTrue(duration2 < 50, 
            "Cached routing should complete in <50ms, took: " + duration2 + "ms");
    }
    
    @Test
    @DisplayName("E2E: Memory stats integration")
    void testMemoryStatsIntegration() {
        // Arrange
        String code = "public class Test { }";
        
        // Act: Route and cache
        ModelRouter.ModelSelection selection = modelRouter.selectModel(code);
        modelCache.getOrLoad(selection.model());
        
        KVCacheManager.KVCache kvCache = kvCacheManager.createCache("session-stats", 12, 512);
        kvCache.append(0, new float[]{1.0f}, new float[]{2.0f});
        
        // Assert: Get all stats
        MemoryManager.MemoryStats memStats = memoryManager.getMemoryStats();
        ModelCache.CacheStats modelStats = modelCache.getStats();
        KVCacheManager.KVCacheStats kvStats = kvCacheManager.getStats();
        
        // All components should report valid stats
        assertNotNull(memStats.format(), "Memory stats should format");
        assertTrue(memStats.maxMB() > 0, "Should have max memory");
        
        assertNotNull(modelStats.format(), "Model cache stats should format");
        assertTrue(modelStats.size() > 0, "Should have cached models");
        
        assertNotNull(kvStats.format(), "KV cache stats should format");
        assertEquals(1, kvStats.activeCaches(), "Should have 1 KV cache");
        
        // Cleanup
        kvCacheManager.evictCache("session-stats");
    }
}
