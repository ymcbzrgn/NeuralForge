package dev.neuralforge.benchmark;

import dev.neuralforge.router.CodeComplexityAnalyzer;
import dev.neuralforge.router.CodeComplexityAnalyzer.ComplexityScore;
import dev.neuralforge.cache.KVCacheManager;
import dev.neuralforge.cache.KVCacheManager.KVCache;
import dev.neuralforge.cache.ModelCache;
import dev.neuralforge.memory.MemoryManager;
import dev.neuralforge.router.ModelRouter;
import dev.neuralforge.router.ModelRouter.ModelSelection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Performance benchmarks for Sprint 2 Smart Routing system.
 * 
 * Measures:
 * - Code complexity analysis latency
 * - Model routing latency
 * - KV cache operations performance
 * - End-to-end workflow timing
 * 
 * Performance Goals:
 * - Complexity analysis: <25ms for large code (~6KB)
 * - Model routing: <100ms
 * - KV cache creation: <10ms
 * - KV cache append: <5ms per token (12 layers)
 * 
 * Test Methodology:
 * - Warm up JVM (5 iterations)
 * - Run 100 iterations for statistical significance
 * - Report avg/min/max latencies
 * - Validate against performance targets
 */
public class PerformanceBenchmark {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceBenchmark.class);
    
    private CodeComplexityAnalyzer complexityAnalyzer;
    private MemoryManager memoryManager;
    private ModelRouter modelRouter;
    private ModelCache modelCache;
    private KVCacheManager kvCacheManager;
    
    @BeforeEach
    void setUp() {
        complexityAnalyzer = new CodeComplexityAnalyzer();
        
        // Lenient MemoryManager for benchmarking
        memoryManager = new MemoryManager() {
            @Override
            public boolean canLoadModel(long modelSizeBytes) {
                return true; // Always allow for benchmarks
            }
        };
        
        modelRouter = new ModelRouter(complexityAnalyzer, memoryManager);
        modelCache = new ModelCache(memoryManager);
        kvCacheManager = new KVCacheManager();
    }
    
    @Test
    void benchmarkCodeComplexityAnalysis() {
        logger.info("\n╔═══════════════════════════════════════════════════════╗");
        logger.info("║  BENCHMARK: Code Complexity Analysis                  ║");
        logger.info("╚═══════════════════════════════════════════════════════╝");
        
        String[] testCodes = {
            "public class Simple { }",
            generateMediumCode(),
            generateLargeCode()
        };
        
        String[] labels = {"Simple (26 chars)", "Medium (~500 chars)", "Large (~6KB)"};
        long[] targets = {5, 15, 25}; // ms thresholds
        
        for (int i = 0; i < testCodes.length; i++) {
            String code = testCodes[i];
            String label = labels[i];
            long target = targets[i];
            
            // Warmup
            for (int w = 0; w < 5; w++) {
                complexityAnalyzer.analyzeComplexity(code);
            }
            
            // Measure in MICROSECONDS for better precision
            List<Long> times = new ArrayList<>();
            for (int run = 0; run < 100; run++) {
                long start = System.nanoTime();
                ComplexityScore score = complexityAnalyzer.analyzeComplexity(code);
                long durationUs = (System.nanoTime() - start) / 1_000; // microseconds
                times.add(durationUs);
            }
            
            long avgUs = times.stream().mapToLong(Long::longValue).sum() / times.size();
            long minUs = times.stream().mapToLong(Long::longValue).min().orElse(0);
            long maxUs = times.stream().mapToLong(Long::longValue).max().orElse(0);
            
            double avgMs = avgUs / 1000.0;
            double minMs = minUs / 1000.0;
            double maxMs = maxUs / 1000.0;
            
            String status = avgMs < target ? "✅ PASS" : "❌ FAIL";
            logger.info("  {} → avg={}ms ({}μs), min={}ms, max={}ms (target: <{}ms) {}",
                label, String.format("%.2f", avgMs), avgUs, 
                String.format("%.2f", minMs), String.format("%.2f", maxMs), target, status);
            
            assertTrue(avgMs < target, label + " should be <" + target + "ms, got: " + avgMs + "ms");
        }
        
        logger.info("  ✅ All complexity analysis targets met!");
    }
    
    @Test
    void benchmarkModelRouting() {
        logger.info("\n╔═══════════════════════════════════════════════════════╗");
        logger.info("║  BENCHMARK: Model Routing                             ║");
        logger.info("╚═══════════════════════════════════════════════════════╝");
        
        String code = "public class Test { public void method() { } }";
        
        // Warmup
        for (int w = 0; w < 5; w++) {
            modelRouter.selectModel(code);
        }
        
        // Measure in MICROSECONDS
        List<Long> times = new ArrayList<>();
        for (int run = 0; run < 100; run++) {
            long start = System.nanoTime();
            ModelSelection selection = modelRouter.selectModel(code);
            long durationUs = (System.nanoTime() - start) / 1_000;
            times.add(durationUs);
        }
        
        long avgUs = times.stream().mapToLong(Long::longValue).sum() / times.size();
        long minUs = times.stream().mapToLong(Long::longValue).min().orElse(0);
        long maxUs = times.stream().mapToLong(Long::longValue).max().orElse(0);
        
        double avgMs = avgUs / 1000.0;
        
        String status = avgMs < 100 ? "✅ PASS" : "❌ FAIL";
        logger.info("  Routing → avg={}ms ({}μs), min={}ms, max={}ms (target: <100ms) {}",
            String.format("%.2f", avgMs), avgUs, 
            String.format("%.2f", minUs / 1000.0), String.format("%.2f", maxUs / 1000.0), status);
        
        assertTrue(avgMs < 100, "Routing should be <100ms, got: " + avgMs + "ms");
        logger.info("  ✅ Model routing target met!");
    }
    
    @Test
    void benchmarkKVCacheCreation() {
        logger.info("\n╔═══════════════════════════════════════════════════════╗");
        logger.info("║  BENCHMARK: KV Cache Creation                         ║");
        logger.info("╚═══════════════════════════════════════════════════════╝");
        
        int numLayers = 12;
        int maxSeqLength = 512;
        
        // Warmup
        for (int w = 0; w < 5; w++) {
            KVCache warmup = kvCacheManager.createCache("warmup-" + w, numLayers, maxSeqLength);
            kvCacheManager.evictCache("warmup-" + w);
        }
        
        // Measure in MICROSECONDS
        List<Long> times = new ArrayList<>();
        for (int run = 0; run < 100; run++) {
            String sessionId = "session-" + run;
            
            long start = System.nanoTime();
            KVCache cache = kvCacheManager.createCache(sessionId, numLayers, maxSeqLength);
            long durationUs = (System.nanoTime() - start) / 1_000;
            times.add(durationUs);
            
            kvCacheManager.evictCache(sessionId);
        }
        
        long avgUs = times.stream().mapToLong(Long::longValue).sum() / times.size();
        long minUs = times.stream().mapToLong(Long::longValue).min().orElse(0);
        long maxUs = times.stream().mapToLong(Long::longValue).max().orElse(0);
        
        double avgMs = avgUs / 1000.0;
        
        String status = avgMs < 10 ? "✅ PASS" : "❌ FAIL";
        logger.info("  Creation (12 layers, 512 max seq) → avg={}ms ({}μs), min={}ms, max={}ms (target: <10ms) {}",
            String.format("%.2f", avgMs), avgUs, 
            String.format("%.2f", minUs / 1000.0), String.format("%.2f", maxUs / 1000.0), status);
        
        assertTrue(avgMs < 10, "KV cache creation should be <10ms, got: " + avgMs + "ms");
        logger.info("  ✅ KV cache creation target met!");
    }
    
    @Test
    void benchmarkKVCacheAppend() {
        logger.info("\n╔═══════════════════════════════════════════════════════╗");
        logger.info("║  BENCHMARK: KV Cache Append (Token Generation)       ║");
        logger.info("╚═══════════════════════════════════════════════════════╝");
        
        String sessionId = "bench-append";
        int numLayers = 12;
        int maxSeqLength = 512;
        
        KVCache cache = kvCacheManager.createCache(sessionId, numLayers, maxSeqLength);
        
        // Simulate token generation in NANOSECONDS (very fast operation)
        List<Long> times = new ArrayList<>();
        
        for (int token = 0; token < 100; token++) {
            float[][] keyTensor = new float[1][64];   // [batch=1, hiddenSize=64]
            float[][] valueTensor = new float[1][64];
            
            long start = System.nanoTime();
            for (int layer = 0; layer < numLayers; layer++) {
                cache.append(layer, keyTensor, valueTensor);
            }
            long durationNs = System.nanoTime() - start;
            times.add(durationNs);
        }
        
        long avgNs = times.stream().mapToLong(Long::longValue).sum() / times.size();
        long minNs = times.stream().mapToLong(Long::longValue).min().orElse(0);
        long maxNs = times.stream().mapToLong(Long::longValue).max().orElse(0);
        
        double avgUs = avgNs / 1000.0;
        double avgMs = avgNs / 1_000_000.0;
        double tokensPerSecond = 1_000_000_000.0 / avgNs; // nanoseconds to tokens/sec
        
        String status = avgMs < 5 ? "✅ PASS" : "❌ FAIL";
        logger.info("  Append (12 layers/token) → avg={}μs ({}ns), min={}μs, max={}μs (target: <5000μs) {}",
            String.format("%.2f", avgUs), avgNs, 
            String.format("%.2f", minNs / 1000.0), String.format("%.2f", maxNs / 1000.0), status);
        logger.info("  Throughput: {} tokens/second", String.format("%.0f", tokensPerSecond));
        
        assertTrue(avgMs < 5, "KV cache append should be <5ms per token, got: " + avgMs + "ms");
        
        kvCacheManager.evictCache(sessionId);
        logger.info("  ✅ KV cache append target met!");
    }
    
    @Test
    void benchmarkEndToEndWorkflow() {
        logger.info("\n╔═══════════════════════════════════════════════════════╗");
        logger.info("║  BENCHMARK: End-to-End Workflow                       ║");
        logger.info("║  (Complexity → Routing → KV Cache → 10 Token Gen)    ║");
        logger.info("╚═══════════════════════════════════════════════════════╝");
        
        String[] testCodes = {
            "public class Simple { }",
            generateMediumCode(),
            generateLargeCode()
        };
        
        String[] labels = {"Simple", "Medium", "Large"};
        
        for (int i = 0; i < testCodes.length; i++) {
            String code = testCodes[i];
            String label = labels[i];
            
            List<Long> times = new ArrayList<>();
            
            for (int run = 0; run < 50; run++) {
                String sessionId = label.toLowerCase() + "-" + run;
                
                long start = System.nanoTime();
                
                // Full workflow
                ComplexityScore score = complexityAnalyzer.analyzeComplexity(code);
                ModelSelection selection = modelRouter.selectModel(code);
                KVCache cache = kvCacheManager.createCache(sessionId, 12, 512);
                
                // Simulate 10-token generation
                for (int token = 0; token < 10; token++) {
                    float[][] keyTensor = new float[1][64];
                    float[][] valueTensor = new float[1][64];
                    for (int layer = 0; layer < 12; layer++) {
                        cache.append(layer, keyTensor, valueTensor);
                    }
                }
                
                kvCacheManager.evictCache(sessionId);
                
                long durationUs = (System.nanoTime() - start) / 1_000;
                times.add(durationUs);
            }
            
            long avgUs = times.stream().mapToLong(Long::longValue).sum() / times.size();
            long minUs = times.stream().mapToLong(Long::longValue).min().orElse(0);
            long maxUs = times.stream().mapToLong(Long::longValue).max().orElse(0);
            
            logger.info("  {} → avg={}ms ({}μs), min={}ms, max={}ms", 
                label, String.format("%.2f", avgUs / 1000.0), avgUs, 
                String.format("%.2f", minUs / 1000.0), String.format("%.2f", maxUs / 1000.0));
        }
        
        logger.info("  ✅ End-to-end workflow benchmark complete!");
    }
    
    // Helper methods
    
    private String generateMediumCode() {
        return """
            public class MediumComplexity {
                private String name;
                private int value;
                
                public MediumComplexity(String name, int value) {
                    this.name = name;
                    this.value = value;
                }
                
                public String process() {
                    if (value > 100) {
                        return "High: " + name;
                    } else if (value > 50) {
                        return "Medium: " + name;
                    } else {
                        return "Low: " + name;
                    }
                }
                
                public int calculate() {
                    int result = 0;
                    for (int i = 0; i < value; i++) {
                        result += i * 2;
                    }
                    return result;
                }
            }
            """;
    }
    
    private String generateLargeCode() {
        StringBuilder sb = new StringBuilder();
        sb.append("public class LargeComplexity {\n");
        
        for (int i = 0; i < 20; i++) {
            sb.append("    public void method").append(i).append("() {\n");
            sb.append("        int x = 0;\n");
            sb.append("        for (int j = 0; j < 100; j++) {\n");
            sb.append("            if (j % 2 == 0) {\n");
            sb.append("                x += j;\n");
            sb.append("            } else {\n");
            sb.append("                x -= j;\n");
            sb.append("            }\n");
            sb.append("        }\n");
            sb.append("        System.out.println(x);\n");
            sb.append("    }\n\n");
        }
        
        sb.append("}\n");
        return sb.toString();
    }
}
