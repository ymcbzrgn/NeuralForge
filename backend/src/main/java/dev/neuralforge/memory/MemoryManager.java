package dev.neuralforge.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * Memory management service for tracking JVM heap memory and preventing OOM crashes.
 * 
 * CRITICAL: Desktop app constraint - 3-4GB total budget (JVM + models + vectors)
 * 
 * Memory allocation strategy:
 * - JVM Heap: 1.5GB max (-Xmx1536m)
 * - Models: 770M-3B (ONNX loaded into heap)
 * - Qdrant: ~500MB for vectors
 * - Safety buffer: 512MB minimum free
 * 
 * Used by ModelRouter to decide if model can be safely loaded.
 */
@Component
public class MemoryManager {
    private static final Logger logger = LoggerFactory.getLogger(MemoryManager.class);
    
    // Memory thresholds (bytes)
    private static final long SAFETY_BUFFER_BYTES = 512L * 1024 * 1024; // 512MB minimum
    private static final long MB = 1024L * 1024;
    private static final long GB = 1024L * 1024 * 1024;
    
    private final MemoryMXBean memoryBean;
    
    public MemoryManager() {
        this.memoryBean = ManagementFactory.getMemoryMXBean();
    }
    
    /**
     * Get current free memory in bytes.
     * 
     * Formula: free = max - used
     * 
     * @return free memory in bytes (accounts for GC)
     */
    public long getFreeMemory() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        long max = heapUsage.getMax();
        long used = heapUsage.getUsed();
        long free = max - used;
        
        logger.debug("Memory: used={}MB, max={}MB, free={}MB", 
            used / MB, max / MB, free / MB);
        
        return free;
    }
    
    /**
     * Check if model of given size can be loaded safely.
     * 
     * Safety check: (free memory - model size) >= SAFETY_BUFFER
     * 
     * @param modelSizeBytes model size in bytes
     * @return true if safe to load, false if would risk OOM
     */
    public boolean canLoadModel(long modelSizeBytes) {
        long free = getFreeMemory();
        long afterLoad = free - modelSizeBytes;
        boolean canLoad = afterLoad >= SAFETY_BUFFER_BYTES;
        
        if (!canLoad) {
            logger.warn("Insufficient memory to load model: need={}MB, free={}MB, buffer={}MB",
                modelSizeBytes / MB, free / MB, SAFETY_BUFFER_BYTES / MB);
        } else {
            logger.debug("Memory check passed: need={}MB, free={}MB, after={}MB",
                modelSizeBytes / MB, free / MB, afterLoad / MB);
        }
        
        return canLoad;
    }
    
    /**
     * Get detailed memory statistics for monitoring/debugging.
     * 
     * @return MemoryStats object with current state
     */
    public MemoryStats getMemoryStats() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        
        long init = heapUsage.getInit();
        long used = heapUsage.getUsed();
        long committed = heapUsage.getCommitted();
        long max = heapUsage.getMax();
        long free = max - used;
        
        double usedPercent = (double) used / max * 100;
        
        return new MemoryStats(init, used, committed, max, free, usedPercent);
    }
    
    /**
     * Suggest garbage collection (non-blocking hint to JVM).
     * 
     * Use sparingly! JVM knows best when to GC.
     * Only call before loading large model if memory is tight.
     */
    public void suggestGC() {
        logger.debug("Suggesting garbage collection");
        long beforeFree = getFreeMemory();
        
        System.gc(); // Hint only, JVM may ignore
        
        // Give GC time to run
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long afterFree = getFreeMemory();
        long freed = afterFree - beforeFree;
        
        logger.debug("GC suggestion: freed={}MB", freed / MB);
    }
    
    /**
     * Memory statistics snapshot.
     */
    public record MemoryStats(
        long initBytes,
        long usedBytes,
        long committedBytes,
        long maxBytes,
        long freeBytes,
        double usedPercent
    ) {
        public String format() {
            return String.format(
                "Memory: used=%dMB (%.1f%%), free=%dMB, max=%dMB",
                usedBytes / MB,
                usedPercent,
                freeBytes / MB,
                maxBytes / MB
            );
        }
        
        public long usedMB() {
            return usedBytes / MB;
        }
        
        public long freeMB() {
            return freeBytes / MB;
        }
        
        public long maxMB() {
            return maxBytes / MB;
        }
    }
}
