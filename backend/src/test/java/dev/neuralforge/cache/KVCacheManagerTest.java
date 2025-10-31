package dev.neuralforge.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for KVCacheManager - the CRITICAL 68% speedup optimization!
 * 
 * Tests cache creation, eviction, memory tracking, and correctness.
 */
class KVCacheManagerTest {
    
    private KVCacheManager manager;
    
    @BeforeEach
    void setUp() {
        manager = new KVCacheManager();
    }
    
    @Test
    @DisplayName("Should create new KV cache for session")
    void testCreateCache() {
        // Act
        KVCacheManager.KVCache cache = manager.createCache("session-1", 12, 512);
        
        // Assert
        assertNotNull(cache, "Should create cache");
        assertEquals("session-1", cache.getSessionId());
        assertEquals(12, cache.getNumLayers());
        assertEquals(512, cache.getMaxSequenceLength());
        assertEquals(0, cache.getCurrentLength(), "Initial length should be 0");
    }
    
    @Test
    @DisplayName("Should retrieve existing cache")
    void testGetCache() {
        // Arrange
        manager.createCache("session-1", 12, 512);
        
        // Act
        Optional<KVCacheManager.KVCache> retrieved = manager.getCache("session-1");
        
        // Assert
        assertTrue(retrieved.isPresent(), "Should retrieve existing cache");
        assertEquals("session-1", retrieved.get().getSessionId());
    }
    
    @Test
    @DisplayName("Should return empty for non-existent session")
    void testGetNonExistentCache() {
        // Act
        Optional<KVCacheManager.KVCache> retrieved = manager.getCache("non-existent");
        
        // Assert
        assertFalse(retrieved.isPresent(), "Should return empty for non-existent session");
    }
    
    @Test
    @DisplayName("Should evict cache and free memory")
    void testEvictCache() {
        // Arrange
        manager.createCache("session-1", 12, 512);
        assertTrue(manager.getCache("session-1").isPresent());
        
        // Act
        boolean evicted = manager.evictCache("session-1");
        
        // Assert
        assertTrue(evicted, "Should successfully evict");
        assertFalse(manager.getCache("session-1").isPresent(), "Cache should be gone");
    }
    
    @Test
    @DisplayName("Should return false when evicting non-existent cache")
    void testEvictNonExistent() {
        // Act
        boolean evicted = manager.evictCache("non-existent");
        
        // Assert
        assertFalse(evicted, "Should return false for non-existent session");
    }
    
    @Test
    @DisplayName("Should replace existing cache when creating with same session ID")
    void testReplaceExistingCache() {
        // Arrange
        KVCacheManager.KVCache first = manager.createCache("session-1", 12, 512);
        
        // Act
        KVCacheManager.KVCache second = manager.createCache("session-1", 24, 1024);
        
        // Assert
        assertNotSame(first, second, "Should be different cache instances");
        assertEquals(24, second.getNumLayers(), "Should have new config");
        assertEquals(1024, second.getMaxSequenceLength());
    }
    
    @Test
    @DisplayName("Should clear all caches")
    void testClearAll() {
        // Arrange
        manager.createCache("session-1", 12, 512);
        manager.createCache("session-2", 12, 512);
        manager.createCache("session-3", 12, 512);
        
        // Act
        manager.clearAll();
        
        // Assert
        assertFalse(manager.getCache("session-1").isPresent());
        assertFalse(manager.getCache("session-2").isPresent());
        assertFalse(manager.getCache("session-3").isPresent());
    }
    
    @Test
    @DisplayName("Should track cache statistics")
    void testCacheStatistics() {
        // Arrange
        KVCacheManager.KVCache cache1 = manager.createCache("session-1", 12, 512);
        KVCacheManager.KVCache cache2 = manager.createCache("session-2", 12, 512);
        
        // Add some data to cache1 so memory > 0
        cache1.append(0, new float[]{1.0f}, new float[]{2.0f});
        
        manager.evictCache("session-1");
        
        // Act
        KVCacheManager.KVCacheStats stats = manager.getStats();
        
        // Assert
        assertEquals(1, stats.activeCaches(), "Should have 1 active cache");
        assertEquals(2, stats.totalCreated(), "Should have created 2 caches");
        assertEquals(1, stats.totalEvicted(), "Should have evicted 1 cache");
        assertTrue(stats.totalMemoryBytes() >= 0, "Memory should be non-negative");
    }
    
    @Test
    @DisplayName("Should format statistics as string")
    void testStatsFormatting() {
        // Arrange
        manager.createCache("session-1", 12, 512);
        
        // Act
        KVCacheManager.KVCacheStats stats = manager.getStats();
        String formatted = stats.format();
        
        // Assert
        assertNotNull(formatted);
        assertTrue(formatted.contains("active="), "Should contain active count");
        assertTrue(formatted.contains("created="), "Should contain created count");
        assertTrue(formatted.contains("memory="), "Should contain memory usage");
    }
    
    @Test
    @DisplayName("Should append KV pairs to layer cache")
    void testAppendToLayerCache() {
        // Arrange
        KVCacheManager.KVCache cache = manager.createCache("session-1", 12, 512);
        Object mockKey = new float[]{1.0f, 2.0f, 3.0f};
        Object mockValue = new float[]{4.0f, 5.0f, 6.0f};
        
        // Act
        cache.append(0, mockKey, mockValue);
        
        // Assert
        assertEquals(1, cache.getCurrentLength(), "Sequence length should increment");
        
        KVCacheManager.LayerKVCache layerCache = cache.getLayerCache(0);
        assertEquals(1, layerCache.getLength(), "Layer cache should have 1 token");
    }
    
    @Test
    @DisplayName("Should append multiple tokens across layers")
    void testMultipleTokensMultipleLayers() {
        // Arrange
        KVCacheManager.KVCache cache = manager.createCache("session-1", 12, 512);
        
        // Act: Append 3 tokens, each token goes to ALL layers
        for (int token = 0; token < 3; token++) {
            for (int layer = 0; layer < 12; layer++) {
                Object mockKey = new float[]{(float) token};
                Object mockValue = new float[]{(float) (token + 100)};
                cache.append(layer, mockKey, mockValue);
            }
        }
        
        // Assert
        assertEquals(3, cache.getCurrentLength(), "Should have 3 tokens");
        for (int layer = 0; layer < 12; layer++) {
            assertEquals(3, cache.getLayerCache(layer).getLength(),
                "Layer " + layer + " should have 3 tokens");
        }
    }
    
    @Test
    @DisplayName("Should throw when cache is full")
    void testThrowsWhenCacheFull() {
        // Arrange: Create small cache
        KVCacheManager.KVCache cache = manager.createCache("session-1", 1, 2);
        
        // Fill cache
        cache.append(0, new float[]{1.0f}, new float[]{2.0f});
        cache.append(0, new float[]{3.0f}, new float[]{4.0f});
        
        // Act & Assert
        assertTrue(cache.isFull(), "Cache should be full");
        assertThrows(IllegalStateException.class, () -> {
            cache.append(0, new float[]{5.0f}, new float[]{6.0f});
        }, "Should throw when cache full");
    }
    
    @Test
    @DisplayName("Should throw for invalid layer index")
    void testThrowsForInvalidLayer() {
        // Arrange
        KVCacheManager.KVCache cache = manager.createCache("session-1", 12, 512);
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            cache.append(99, new float[]{1.0f}, new float[]{2.0f});
        }, "Should throw for invalid layer index");
    }
    
    @Test
    @DisplayName("Should retrieve cached tensors from layer")
    void testRetrieveCachedTensors() {
        // Arrange
        KVCacheManager.KVCache cache = manager.createCache("session-1", 12, 512);
        Object mockKey = new float[]{1.0f, 2.0f};
        Object mockValue = new float[]{3.0f, 4.0f};
        
        cache.append(0, mockKey, mockValue);
        
        // Act
        KVCacheManager.LayerKVCache layerCache = cache.getLayerCache(0);
        Object[] keys = layerCache.getKeyTensors();
        Object[] values = layerCache.getValueTensors();
        
        // Assert
        assertNotNull(keys, "Should retrieve key tensors");
        assertNotNull(values, "Should retrieve value tensors");
        assertSame(mockKey, keys[0], "Should be same key instance");
        assertSame(mockValue, values[0], "Should be same value instance");
    }
    
    @Test
    @DisplayName("Should clear cache and reset state")
    void testClearCache() {
        // Arrange
        KVCacheManager.KVCache cache = manager.createCache("session-1", 12, 512);
        cache.append(0, new float[]{1.0f}, new float[]{2.0f});
        cache.append(0, new float[]{3.0f}, new float[]{4.0f});
        
        assertEquals(2, cache.getCurrentLength());
        
        // Act
        cache.clear();
        
        // Assert
        assertEquals(0, cache.getCurrentLength(), "Length should reset to 0");
        
        KVCacheManager.LayerKVCache layerCache = cache.getLayerCache(0);
        assertEquals(0, layerCache.getLength(), "Layer cache should be empty");
    }
    
    @Test
    @DisplayName("Should estimate memory usage")
    void testMemoryEstimation() {
        // Arrange
        KVCacheManager.KVCache cache = manager.createCache("session-1", 12, 512);
        
        long memoryBefore = cache.getMemoryUsageBytes();
        
        // Append some tokens
        for (int i = 0; i < 10; i++) {
            cache.append(0, new float[]{1.0f}, new float[]{2.0f});
        }
        
        long memoryAfter = cache.getMemoryUsageBytes();
        
        // Assert
        assertTrue(memoryAfter > memoryBefore, 
            "Memory usage should increase after appending tokens");
        assertTrue(memoryAfter > 0, "Memory usage should be positive");
    }
    
    @Test
    @DisplayName("Should have creation timestamp")
    void testCreationTimestamp() {
        // Arrange
        long before = System.currentTimeMillis();
        
        // Act
        KVCacheManager.KVCache cache = manager.createCache("session-1", 12, 512);
        
        long after = System.currentTimeMillis();
        
        // Assert
        assertTrue(cache.getCreatedAtMs() >= before && cache.getCreatedAtMs() <= after,
            "Creation timestamp should be within test time range");
    }
    
    @Test
    @DisplayName("Should track memory across multiple caches")
    void testMultipleCachesMemory() {
        // Arrange
        KVCacheManager.KVCache cache1 = manager.createCache("session-1", 12, 512);
        KVCacheManager.KVCache cache2 = manager.createCache("session-2", 24, 1024);
        
        // Add some tokens to caches so memory is tracked
        cache1.append(0, new float[]{1.0f}, new float[]{2.0f});
        cache2.append(0, new float[]{3.0f}, new float[]{4.0f});
        
        // Act
        KVCacheManager.KVCacheStats stats = manager.getStats();
        
        // Assert
        assertEquals(2, stats.activeCaches());
        assertTrue(stats.totalMemoryBytes() > 0, "Should track total memory");
        assertTrue(stats.memoryMB() >= 0, "Memory MB should be non-negative");
    }
}
