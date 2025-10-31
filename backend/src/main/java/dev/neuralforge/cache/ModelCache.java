package dev.neuralforge.cache;

import dev.neuralforge.memory.MemoryManager;
import dev.neuralforge.router.ModelRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * LRU cache for loaded ONNX models to avoid expensive reload operations.
 * 
 * Strategy:
 * - Keep recently used models in memory
 * - Evict least recently used (LRU) when memory pressure
 * - Check with MemoryManager before loading new model
 * - Transparent to callers (cache hit = instant, miss = load)
 * 
 * Cache key: ModelType (CODET5P_33M, CODET5P_770M, STABLECODE_3B)
 * Cache value: Placeholder for actual ONNX model session
 * 
 * Memory budget:
 * - 33M model: ~33MB
 * - 770M model: ~770MB
 * - 3B model: ~3GB
 * - Realistic: Keep 1-2 models cached (total heap 1.5GB)
 */
@Component
public class ModelCache {
    private static final Logger logger = LoggerFactory.getLogger(ModelCache.class);
    
    private final MemoryManager memoryManager;
    
    // LRU cache: LinkedHashMap with access-order
    private final Map<ModelRouter.ModelType, CachedModel> cache;
    
    // Cache configuration
    private static final int MAX_CACHE_SIZE = 3; // Max 3 models (but memory limits this)
    
    public ModelCache(MemoryManager memoryManager) {
        this.memoryManager = memoryManager;
        
        // LinkedHashMap with access-order (true) = LRU behavior
        this.cache = new LinkedHashMap<>(MAX_CACHE_SIZE, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<ModelRouter.ModelType, CachedModel> eldest) {
                // Note: This is size-based LRU, but we also evict based on memory
                return size() > MAX_CACHE_SIZE;
            }
        };
    }
    
    /**
     * Get model from cache or load if not present.
     * 
     * @param modelType model to get
     * @return cached or newly loaded model
     */
    public synchronized CachedModel getOrLoad(ModelRouter.ModelType modelType) {
        // Check cache first
        CachedModel cached = cache.get(modelType);
        if (cached != null) {
            logger.debug("Cache HIT: {}", modelType.getModelId());
            cached.recordHit();
            return cached;
        }
        
        logger.debug("Cache MISS: {}", modelType.getModelId());
        
        // Not in cache, need to load
        return loadModel(modelType);
    }
    
    /**
     * Load model into cache (with memory check and eviction).
     */
    private CachedModel loadModel(ModelRouter.ModelType modelType) {
        long modelSize = modelType.getSizeBytes();
        
        // Check if we have enough memory
        if (!memoryManager.canLoadModel(modelSize)) {
            logger.warn("Insufficient memory for {}, attempting eviction", modelType.getModelId());
            evictLRU();
            
            // Try again after eviction
            if (!memoryManager.canLoadModel(modelSize)) {
                logger.error("Still insufficient memory after eviction for {}", modelType.getModelId());
                throw new OutOfMemoryError(
                    "Cannot load model " + modelType.getModelId() + 
                    ": need " + modelSize + " bytes"
                );
            }
        }
        
        // Simulate model loading (in real implementation, load ONNX session here)
        logger.info("Loading model: {} ({} MB)", 
            modelType.getModelId(), 
            modelSize / (1024 * 1024));
        
        CachedModel model = new CachedModel(modelType);
        cache.put(modelType, model);
        
        logger.info("Model loaded successfully: {}", modelType.getModelId());
        return model;
    }
    
    /**
     * Evict least recently used model to free memory.
     */
    public synchronized void evictLRU() {
        if (cache.isEmpty()) {
            logger.debug("Cache empty, nothing to evict");
            return;
        }
        
        // LinkedHashMap with access-order: first entry = LRU
        Map.Entry<ModelRouter.ModelType, CachedModel> eldest = 
            cache.entrySet().iterator().next();
        
        ModelRouter.ModelType evictedType = eldest.getKey();
        CachedModel evictedModel = eldest.getValue();
        
        logger.info("Evicting LRU model: {} (hits: {}, misses: {})", 
            evictedType.getModelId(),
            evictedModel.getHitCount(),
            evictedModel.getMissCount());
        
        cache.remove(evictedType);
        
        // In real implementation: close ONNX session, free native memory
        evictedModel.close();
    }
    
    /**
     * Clear entire cache (for shutdown or testing).
     */
    public synchronized void clear() {
        logger.info("Clearing model cache ({} models)", cache.size());
        cache.values().forEach(CachedModel::close);
        cache.clear();
    }
    
    /**
     * Get cache statistics.
     */
    public synchronized CacheStats getStats() {
        int size = cache.size();
        long totalHits = cache.values().stream()
            .mapToLong(CachedModel::getHitCount)
            .sum();
        long totalMisses = cache.values().stream()
            .mapToLong(CachedModel::getMissCount)
            .sum();
        
        double hitRate = (totalHits + totalMisses) > 0 
            ? (double) totalHits / (totalHits + totalMisses) * 100 
            : 0.0;
        
        return new CacheStats(size, totalHits, totalMisses, hitRate);
    }
    
    /**
     * Check if model is cached.
     */
    public synchronized boolean isCached(ModelRouter.ModelType modelType) {
        return cache.containsKey(modelType);
    }
    
    /**
     * Get current cache size.
     */
    public synchronized int size() {
        return cache.size();
    }
    
    /**
     * Cached model wrapper (placeholder for actual ONNX session).
     */
    public static class CachedModel {
        private final ModelRouter.ModelType modelType;
        private final long loadedAtMs;
        private long hitCount = 0;
        private long missCount = 1; // Starts at 1 (initial load is a miss)
        
        public CachedModel(ModelRouter.ModelType modelType) {
            this.modelType = modelType;
            this.loadedAtMs = System.currentTimeMillis();
        }
        
        public void recordHit() {
            hitCount++;
        }
        
        public ModelRouter.ModelType getModelType() {
            return modelType;
        }
        
        public long getLoadedAtMs() {
            return loadedAtMs;
        }
        
        public long getHitCount() {
            return hitCount;
        }
        
        public long getMissCount() {
            return missCount;
        }
        
        public void close() {
            // In real implementation: ortSession.close(), free native memory
            logger.debug("Closed model: {}", modelType.getModelId());
        }
    }
    
    /**
     * Cache statistics snapshot.
     */
    public record CacheStats(
        int size,
        long totalHits,
        long totalMisses,
        double hitRate
    ) {
        public String format() {
            return String.format(
                "Cache: size=%d, hits=%d, misses=%d, hit_rate=%.1f%%",
                size, totalHits, totalMisses, hitRate
            );
        }
    }
}
