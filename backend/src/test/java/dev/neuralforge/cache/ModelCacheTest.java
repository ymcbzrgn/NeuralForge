package dev.neuralforge.cache;

import dev.neuralforge.memory.MemoryManager;
import dev.neuralforge.router.ModelRouter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Tests for ModelCache service.
 * 
 * Tests LRU caching, memory-aware eviction, and cache statistics.
 */
class ModelCacheTest {
    
    private ModelCache cache;
    
    @Mock
    private MemoryManager memoryManager;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        cache = new ModelCache(memoryManager);
    }
    
    @Test
    @DisplayName("Should load model on cache miss")
    void testCacheMissLoadsModel() {
        // Arrange: enough memory
        when(memoryManager.canLoadModel(anyLong())).thenReturn(true);
        
        // Act
        ModelCache.CachedModel model = cache.getOrLoad(ModelRouter.ModelType.CODET5P_33M);
        
        // Assert
        assertNotNull(model, "Should return loaded model");
        assertEquals(ModelRouter.ModelType.CODET5P_33M, model.getModelType());
        assertEquals(1, cache.size(), "Cache should have 1 model");
    }
    
    @Test
    @DisplayName("Should return cached model on cache hit")
    void testCacheHitReturnsExisting() {
        // Arrange
        when(memoryManager.canLoadModel(anyLong())).thenReturn(true);
        
        // Load once
        ModelCache.CachedModel firstLoad = cache.getOrLoad(ModelRouter.ModelType.CODET5P_33M);
        
        // Act: Load again (should hit cache)
        ModelCache.CachedModel secondLoad = cache.getOrLoad(ModelRouter.ModelType.CODET5P_33M);
        
        // Assert
        assertSame(firstLoad, secondLoad, "Should return same cached instance");
        assertEquals(1, cache.size(), "Cache size should stay 1");
        assertEquals(1, secondLoad.getHitCount(), "Hit count should increment");
    }
    
    @Test
    @DisplayName("Should cache multiple models")
    void testCacheMultipleModels() {
        // Arrange: plenty of memory
        when(memoryManager.canLoadModel(anyLong())).thenReturn(true);
        
        // Act: Load 3 different models
        cache.getOrLoad(ModelRouter.ModelType.CODET5P_33M);
        cache.getOrLoad(ModelRouter.ModelType.CODET5P_770M);
        cache.getOrLoad(ModelRouter.ModelType.STABLECODE_3B);
        
        // Assert
        assertEquals(3, cache.size(), "Should cache all 3 models");
        assertTrue(cache.isCached(ModelRouter.ModelType.CODET5P_33M));
        assertTrue(cache.isCached(ModelRouter.ModelType.CODET5P_770M));
        assertTrue(cache.isCached(ModelRouter.ModelType.STABLECODE_3B));
    }
    
    @Test
    @DisplayName("Should evict LRU model when memory insufficient")
    void testEvictsLRUOnMemoryPressure() {
        // Arrange: Load 2 models
        when(memoryManager.canLoadModel(anyLong())).thenReturn(true);
        cache.getOrLoad(ModelRouter.ModelType.CODET5P_33M);
        cache.getOrLoad(ModelRouter.ModelType.CODET5P_770M);
        
        // Access 33M to make 770M the LRU
        cache.getOrLoad(ModelRouter.ModelType.CODET5P_33M);
        
        // Now simulate memory pressure when loading 3B
        when(memoryManager.canLoadModel(3L * 1024 * 1024 * 1024))
            .thenReturn(false)  // First check: insufficient
            .thenReturn(true);  // After eviction: sufficient
        
        // Act: Load 3B (should evict 770M)
        cache.getOrLoad(ModelRouter.ModelType.STABLECODE_3B);
        
        // Assert
        assertEquals(2, cache.size(), "Should have 2 models after eviction");
        assertTrue(cache.isCached(ModelRouter.ModelType.CODET5P_33M), "33M should still be cached");
        assertFalse(cache.isCached(ModelRouter.ModelType.CODET5P_770M), "770M should be evicted (LRU)");
        assertTrue(cache.isCached(ModelRouter.ModelType.STABLECODE_3B), "3B should be newly cached");
    }
    
    @Test
    @DisplayName("Should maintain LRU order based on access")
    void testLRUOrderBasedOnAccess() {
        // Arrange
        when(memoryManager.canLoadModel(anyLong())).thenReturn(true);
        
        // Load 3 models in order: 33M, 770M, 3B
        cache.getOrLoad(ModelRouter.ModelType.CODET5P_33M);
        cache.getOrLoad(ModelRouter.ModelType.CODET5P_770M);
        cache.getOrLoad(ModelRouter.ModelType.STABLECODE_3B);
        
        // Access 33M (moves it to MRU)
        cache.getOrLoad(ModelRouter.ModelType.CODET5P_33M);
        
        // Act: Evict LRU
        cache.evictLRU();
        
        // Assert: 770M should be evicted (oldest access)
        assertEquals(2, cache.size());
        assertTrue(cache.isCached(ModelRouter.ModelType.CODET5P_33M), "33M recently accessed, should stay");
        assertFalse(cache.isCached(ModelRouter.ModelType.CODET5P_770M), "770M is LRU, should be evicted");
        assertTrue(cache.isCached(ModelRouter.ModelType.STABLECODE_3B), "3B should stay");
    }
    
    @Test
    @DisplayName("Should clear all models")
    void testClearAll() {
        // Arrange
        when(memoryManager.canLoadModel(anyLong())).thenReturn(true);
        cache.getOrLoad(ModelRouter.ModelType.CODET5P_33M);
        cache.getOrLoad(ModelRouter.ModelType.CODET5P_770M);
        
        // Act
        cache.clear();
        
        // Assert
        assertEquals(0, cache.size(), "Cache should be empty");
        assertFalse(cache.isCached(ModelRouter.ModelType.CODET5P_33M));
        assertFalse(cache.isCached(ModelRouter.ModelType.CODET5P_770M));
    }
    
    @Test
    @DisplayName("Should track cache statistics")
    void testCacheStatistics() {
        // Arrange
        when(memoryManager.canLoadModel(anyLong())).thenReturn(true);
        
        // Load 33M (miss)
        cache.getOrLoad(ModelRouter.ModelType.CODET5P_33M);
        
        // Access 33M twice (hits)
        cache.getOrLoad(ModelRouter.ModelType.CODET5P_33M);
        cache.getOrLoad(ModelRouter.ModelType.CODET5P_33M);
        
        // Load 770M (miss)
        cache.getOrLoad(ModelRouter.ModelType.CODET5P_770M);
        
        // Act
        ModelCache.CacheStats stats = cache.getStats();
        
        // Assert
        assertEquals(2, stats.size(), "Should have 2 models");
        assertEquals(2, stats.totalHits(), "Should have 2 hits");
        assertEquals(2, stats.totalMisses(), "Should have 2 misses");
        assertEquals(50.0, stats.hitRate(), 0.1, "Hit rate should be 50%");
    }
    
    @Test
    @DisplayName("Should format statistics as string")
    void testStatsFormatting() {
        // Arrange
        when(memoryManager.canLoadModel(anyLong())).thenReturn(true);
        cache.getOrLoad(ModelRouter.ModelType.CODET5P_33M);
        
        // Act
        ModelCache.CacheStats stats = cache.getStats();
        String formatted = stats.format();
        
        // Assert
        assertNotNull(formatted);
        assertTrue(formatted.contains("size="), "Should contain size");
        assertTrue(formatted.contains("hits="), "Should contain hits");
        assertTrue(formatted.contains("misses="), "Should contain misses");
        assertTrue(formatted.contains("hit_rate="), "Should contain hit rate");
    }
    
    @Test
    @DisplayName("Should throw OOM when eviction insufficient")
    void testThrowsOOMWhenEvictionInsufficient() {
        // Arrange: No memory even after eviction
        when(memoryManager.canLoadModel(anyLong())).thenReturn(false);
        
        // Act & Assert
        assertThrows(OutOfMemoryError.class, () -> {
            cache.getOrLoad(ModelRouter.ModelType.STABLECODE_3B);
        }, "Should throw OOM when cannot load even after eviction");
    }
    
    @Test
    @DisplayName("Should handle eviction on empty cache")
    void testEvictEmptyCache() {
        // Act & Assert: Should not throw
        assertDoesNotThrow(() -> cache.evictLRU(), 
            "Evicting empty cache should not throw");
    }
    
    @Test
    @DisplayName("Should record hit count correctly")
    void testHitCountIncrementation() {
        // Arrange
        when(memoryManager.canLoadModel(anyLong())).thenReturn(true);
        
        // Load once
        ModelCache.CachedModel model = cache.getOrLoad(ModelRouter.ModelType.CODET5P_33M);
        assertEquals(0, model.getHitCount(), "Initial hit count should be 0");
        
        // Access 5 more times
        for (int i = 0; i < 5; i++) {
            cache.getOrLoad(ModelRouter.ModelType.CODET5P_33M);
        }
        
        // Assert
        assertEquals(5, model.getHitCount(), "Hit count should be 5");
    }
    
    @Test
    @DisplayName("CachedModel should have loaded timestamp")
    void testCachedModelHasTimestamp() {
        // Arrange
        when(memoryManager.canLoadModel(anyLong())).thenReturn(true);
        long before = System.currentTimeMillis();
        
        // Act
        ModelCache.CachedModel model = cache.getOrLoad(ModelRouter.ModelType.CODET5P_33M);
        long after = System.currentTimeMillis();
        
        // Assert
        assertTrue(model.getLoadedAtMs() >= before && model.getLoadedAtMs() <= after,
            "Loaded timestamp should be within test time range");
    }
    
    @Test
    @DisplayName("Should check if model is cached without loading")
    void testIsCachedCheck() {
        // Arrange
        when(memoryManager.canLoadModel(anyLong())).thenReturn(true);
        cache.getOrLoad(ModelRouter.ModelType.CODET5P_33M);
        
        // Act & Assert
        assertTrue(cache.isCached(ModelRouter.ModelType.CODET5P_33M), 
            "Should report 33M as cached");
        assertFalse(cache.isCached(ModelRouter.ModelType.CODET5P_770M), 
            "Should report 770M as not cached");
    }
}
