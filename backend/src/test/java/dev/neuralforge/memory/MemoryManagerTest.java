package dev.neuralforge.memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MemoryManager service.
 * 
 * Tests memory tracking, OOM prevention, and stats reporting.
 */
class MemoryManagerTest {
    
    private MemoryManager memoryManager;
    
    // Model sizes (approximate ONNX sizes)
    private static final long MODEL_33M_BYTES = 33L * 1024 * 1024;      // 33MB
    private static final long MODEL_770M_BYTES = 770L * 1024 * 1024;    // 770MB
    private static final long MODEL_3B_BYTES = 3L * 1024 * 1024 * 1024; // 3GB
    
    @BeforeEach
    void setUp() {
        memoryManager = new MemoryManager();
    }
    
    @Test
    @DisplayName("Should get free memory in bytes")
    void testGetFreeMemory() {
        long freeMemory = memoryManager.getFreeMemory();
        
        assertNotNull(freeMemory, "Free memory should not be null");
        assertTrue(freeMemory > 0, "Free memory should be positive");
        
        // Sanity check: free memory should be reasonable (not more than 16GB)
        long maxReasonable = 16L * 1024 * 1024 * 1024; // 16GB
        assertTrue(freeMemory < maxReasonable, 
            "Free memory seems unrealistic: " + freeMemory + " bytes");
    }
    
    @Test
    @DisplayName("Should allow loading small model (33M) if memory available")
    void testCanLoadSmallModel() {
        long freeMemory = memoryManager.getFreeMemory();
        long safetyBuffer = 512L * 1024 * 1024;
        
        // Only test if we have enough memory (33MB + 512MB buffer = 545MB needed)
        if (freeMemory > MODEL_33M_BYTES + safetyBuffer) {
            boolean canLoad = memoryManager.canLoadModel(MODEL_33M_BYTES);
            assertTrue(canLoad, "Should be able to load 33M model with sufficient memory");
        } else {
            // Not enough memory, should reject
            boolean canLoad = memoryManager.canLoadModel(MODEL_33M_BYTES);
            assertFalse(canLoad, "Should reject 33M model with insufficient memory");
        }
    }
    
    @Test
    @DisplayName("Should allow loading medium model (770M) if memory available")
    void testCanLoadMediumModel() {
        long freeMemory = memoryManager.getFreeMemory();
        
        // Only test if we have enough memory
        if (freeMemory > MODEL_770M_BYTES + 512L * 1024 * 1024) {
            boolean canLoad = memoryManager.canLoadModel(MODEL_770M_BYTES);
            assertTrue(canLoad, "Should be able to load 770M model with sufficient memory");
        } else {
            // Not enough memory, should reject
            boolean canLoad = memoryManager.canLoadModel(MODEL_770M_BYTES);
            assertFalse(canLoad, "Should reject 770M model with insufficient memory");
        }
    }
    
    @Test
    @DisplayName("Should reject loading model if would leave less than safety buffer")
    void testRejectsLoadingWithInsufficientBuffer() {
        long freeMemory = memoryManager.getFreeMemory();
        
        // Try to load model that would consume almost all memory
        // (leaving less than 512MB safety buffer)
        long unsafeModelSize = freeMemory - 100L * 1024 * 1024; // Leave only 100MB
        
        boolean canLoad = memoryManager.canLoadModel(unsafeModelSize);
        
        assertFalse(canLoad, "Should reject model that would leave insufficient buffer");
    }
    
    @Test
    @DisplayName("Should reject impossibly large model (e.g., 100GB)")
    void testRejectsImpossiblyLargeModel() {
        long hugeModel = 100L * 1024 * 1024 * 1024; // 100GB
        
        boolean canLoad = memoryManager.canLoadModel(hugeModel);
        
        assertFalse(canLoad, "Should reject impossibly large model");
    }
    
    @Test
    @DisplayName("Should provide detailed memory stats")
    void testGetMemoryStats() {
        MemoryManager.MemoryStats stats = memoryManager.getMemoryStats();
        
        assertNotNull(stats, "Stats should not be null");
        assertTrue(stats.maxBytes() > 0, "Max memory should be positive");
        assertTrue(stats.usedBytes() > 0, "Used memory should be positive");
        assertTrue(stats.freeBytes() >= 0, "Free memory should be non-negative");
        assertTrue(stats.committedBytes() > 0, "Committed memory should be positive");
        
        // Used + free should approximately equal max
        long total = stats.usedBytes() + stats.freeBytes();
        assertTrue(Math.abs(total - stats.maxBytes()) < 1024 * 1024, 
            "Used + free should approximately equal max");
        
        // Used percent should be 0-100
        assertTrue(stats.usedPercent() >= 0 && stats.usedPercent() <= 100,
            "Used percent should be 0-100");
    }
    
    @Test
    @DisplayName("Should format stats as human-readable string")
    void testStatsFormatting() {
        MemoryManager.MemoryStats stats = memoryManager.getMemoryStats();
        String formatted = stats.format();
        
        assertNotNull(formatted, "Formatted string should not be null");
        assertTrue(formatted.contains("Memory:"), "Should contain 'Memory:' label");
        assertTrue(formatted.contains("MB"), "Should use MB units");
        assertTrue(formatted.contains("%"), "Should show percentage");
    }
    
    @Test
    @DisplayName("Should provide helper methods for MB conversion")
    void testMBHelpers() {
        MemoryManager.MemoryStats stats = memoryManager.getMemoryStats();
        
        assertTrue(stats.usedMB() >= 0, "usedMB should be non-negative");
        assertTrue(stats.freeMB() >= 0, "freeMB should be non-negative");
        assertTrue(stats.maxMB() > 0, "maxMB should be positive");
        
        // MB values should be reasonable
        assertTrue(stats.maxMB() < 16_000, "maxMB should be less than 16GB");
    }
    
    @Test
    @DisplayName("Should suggest GC without throwing exception")
    void testSuggestGC() {
        // GC suggestion should not throw
        assertDoesNotThrow(() -> memoryManager.suggestGC(),
            "suggestGC should not throw exception");
        
        // Memory should still be available after GC
        long freeAfterGC = memoryManager.getFreeMemory();
        assertTrue(freeAfterGC > 0, "Memory should be available after GC");
    }
    
    @Test
    @DisplayName("Free memory should decrease after allocation")
    void testMemoryTrackingAfterAllocation() {
        long initialFree = memoryManager.getFreeMemory();
        
        // Allocate some memory
        byte[] allocation = new byte[10 * 1024 * 1024]; // 10MB
        allocation[0] = 1; // Ensure array is actually allocated
        
        long afterFree = memoryManager.getFreeMemory();
        
        assertTrue(afterFree < initialFree, 
            "Free memory should decrease after allocation");
        
        // Clean up to avoid affecting other tests
        allocation = null;
        memoryManager.suggestGC();
    }
    
    @Test
    @DisplayName("Should handle edge case: model size exactly equal to free memory minus buffer")
    void testEdgeCaseExactBuffer() {
        long freeMemory = memoryManager.getFreeMemory();
        long safetyBuffer = 512L * 1024 * 1024;
        
        // Model that would leave exactly the safety buffer
        long exactModelSize = freeMemory - safetyBuffer;
        
        if (exactModelSize > 0) {
            boolean canLoad = memoryManager.canLoadModel(exactModelSize);
            assertTrue(canLoad, "Should allow model that leaves exactly safety buffer");
        }
    }
    
    @Test
    @DisplayName("Should handle zero-size model if memory available")
    void testZeroSizeModel() {
        long freeMemory = memoryManager.getFreeMemory();
        long safetyBuffer = 512L * 1024 * 1024;
        
        // Zero-size model still needs safety buffer
        if (freeMemory >= safetyBuffer) {
            boolean canLoad = memoryManager.canLoadModel(0);
            assertTrue(canLoad, "Should allow zero-size model with sufficient buffer");
        } else {
            boolean canLoad = memoryManager.canLoadModel(0);
            assertFalse(canLoad, "Should reject even zero-size if insufficient buffer");
        }
    }
    
    @Test
    @DisplayName("Should handle negative model size gracefully")
    void testNegativeModelSize() {
        boolean canLoad = memoryManager.canLoadModel(-1000);
        
        // Negative size is invalid, but shouldn't crash
        // Implementation may accept (treats as 0) or reject
        assertNotNull(canLoad, "Should handle negative size without crashing");
    }
}
