package dev.neuralforge.tokenizer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Process pool for persistent Python tokenizer workers.
 * 
 * Eliminates 3-4s Python startup overhead by keeping processes alive.
 * 
 * Performance:
 * - Pool initialization: 10-12s one-time (3-4s per process × 3 processes)
 * - Per-request latency: <500ms (vs 4500ms without pool)
 * - Expected savings: ~4.1s per tokenization, ~2.8s per detokenization
 * 
 * Architecture:
 * - 3 persistent Python processes in pool
 * - Stdin/stdout JSON communication protocol
 * - Blocking queue for available processes
 * - Auto-restart on process crashes
 * - Graceful shutdown on app termination
 */
@Component
public class TokenizerProcessPool {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenizerProcessPool.class);
    
    // Configuration
    private static final int POOL_SIZE = 3;
    private static final int WARMUP_TIMEOUT_MS = 15000;  // 15s to start all processes
    private static final int OPERATION_TIMEOUT_MS = 5000;  // 5s per tokenization
    private static final String PYTHON_PATH = "/Users/yamacbezirgan/Desktop/NeuralForge/models/.venv/bin/python";
    private static final String WORKER_SCRIPT = "/Users/yamacbezirgan/Desktop/NeuralForge/models/nf_tokenizer_worker.py";
    
    // Process pool
    private final BlockingQueue<PooledProcess> availableProcesses = new LinkedBlockingQueue<>();
    private final Set<PooledProcess> busyProcesses = Collections.synchronizedSet(new HashSet<>());
    private final List<PooledProcess> allProcesses = Collections.synchronizedList(new ArrayList<>());
    
    // State
    private volatile boolean isInitialized = false;
    private volatile boolean isShutdown = false;
    private final ObjectMapper mapper = new ObjectMapper();
    
    /**
     * Wrapper for Python worker process with I/O streams
     */
    private static class PooledProcess {
        final int id;
        final Process process;
        final BufferedWriter stdin;
        final BufferedReader stdout;
        final BufferedReader stderr;
        volatile boolean isHealthy = true;
        final long startTime;
        
        PooledProcess(int id, Process process) {
            this.id = id;
            this.process = process;
            this.stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            this.stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
            this.stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            this.startTime = System.currentTimeMillis();
        }
    }
    
    /**
     * Initialize pool on application startup (Spring @PostConstruct)
     */
    @PostConstruct
    public void initialize() {
        if (isInitialized) {
            logger.warn("Process pool already initialized, skipping");
            return;
        }
        
        logger.info("Initializing tokenizer process pool (size={})", POOL_SIZE);
        long startTime = System.currentTimeMillis();
        
        try {
            for (int i = 0; i < POOL_SIZE; i++) {
                PooledProcess pooled = startWorkerProcess(i);
                allProcesses.add(pooled);
                availableProcesses.offer(pooled);
                logger.info("✓ Worker process {} started and ready", i);
            }
            
            isInitialized = true;
            long duration = System.currentTimeMillis() - startTime;
            logger.info("✓ Tokenizer process pool initialized in {}ms ({} processes)", duration, POOL_SIZE);
            
        } catch (Exception e) {
            logger.error("Failed to initialize process pool", e);
            // Cleanup partial initialization
            shutdownInternal();
            throw new RuntimeException("Process pool initialization failed", e);
        }
    }
    
    /**
     * Start a single Python worker process and wait for "ready" signal
     */
    private PooledProcess startWorkerProcess(int processId) throws Exception {
        logger.debug("Starting worker process {}...", processId);
        
        ProcessBuilder pb = new ProcessBuilder(PYTHON_PATH, WORKER_SCRIPT);
        pb.redirectErrorStream(false);
        
        Process process = pb.start();
        PooledProcess pooled = new PooledProcess(processId, process);
        
        // Start stderr reader thread (to prevent buffer overflow)
        Thread stderrReader = new Thread(() -> {
            try {
                String line;
                while ((line = pooled.stderr.readLine()) != null) {
                    logger.warn("[Worker {}] stderr: {}", processId, line);
                }
            } catch (IOException e) {
                logger.debug("Worker {} stderr stream closed", processId);
            }
        }, "worker-" + processId + "-stderr");
        stderrReader.setDaemon(true);
        stderrReader.start();
        
        // Wait for "ready" signal with timeout
        CompletableFuture<String> readyFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return pooled.stdout.readLine();
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
        
        String readyLine;
        try {
            readyLine = readyFuture.get(WARMUP_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            pooled.process.destroy();
            throw new RuntimeException("Worker " + processId + " did not send ready signal within " + WARMUP_TIMEOUT_MS + "ms");
        }
        
        if (readyLine == null) {
            pooled.process.destroy();
            throw new RuntimeException("Worker " + processId + " sent EOF instead of ready signal");
        }
        
        // Parse ready message
        Map<String, Object> readyMsg = mapper.readValue(readyLine, new TypeReference<Map<String, Object>>() {});
        String status = (String) readyMsg.get("status");
        
        if ("error".equals(status)) {
            pooled.process.destroy();
            String errorMsg = (String) readyMsg.get("message");
            throw new RuntimeException("Worker " + processId + " startup failed: " + errorMsg);
        }
        
        if (!"ready".equals(status)) {
            pooled.process.destroy();
            throw new RuntimeException("Worker " + processId + " sent unexpected status: " + status);
        }
        
        logger.debug("Worker {} ready (model: {})", processId, readyMsg.get("model"));
        return pooled;
    }
    
    /**
     * Tokenize text using a process from the pool
     * 
     * @param text Code text to tokenize
     * @return List of token IDs
     * @throws Exception if tokenization fails or timeout
     */
    public List<Integer> tokenize(String text) throws Exception {
        if (!isInitialized) {
            throw new IllegalStateException("Process pool not initialized");
        }
        
        if (isShutdown) {
            throw new IllegalStateException("Process pool is shut down");
        }
        
        long startTime = System.currentTimeMillis();
        
        // Acquire process from pool (blocking if all busy)
        PooledProcess process = availableProcesses.poll(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        if (process == null) {
            throw new TimeoutException("No available process in pool (all busy)");
        }
        
        busyProcesses.add(process);
        
        try {
            // Build request
            String requestId = UUID.randomUUID().toString();
            Map<String, Object> request = new HashMap<>();
            request.put("command", "TOKENIZE");
            request.put("id", requestId);
            request.put("text", text);
            
            // Send request
            String requestJson = mapper.writeValueAsString(request);
            process.stdin.write(requestJson + "\n");
            process.stdin.flush();
            
            // Read response with timeout
            CompletableFuture<String> responseFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return process.stdout.readLine();
                } catch (IOException e) {
                    throw new CompletionException(e);
                }
            });
            
            String responseLine = responseFuture.get(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            
            if (responseLine == null) {
                throw new IOException("Worker process sent EOF (crashed?)");
            }
            
            // Parse response
            Map<String, Object> response = mapper.readValue(responseLine, new TypeReference<Map<String, Object>>() {});
            String status = (String) response.get("status");
            
            if ("error".equals(status)) {
                String errorMsg = (String) response.get("message");
                throw new RuntimeException("Tokenization failed: " + errorMsg);
            }
            
            // Extract tokens
            @SuppressWarnings("unchecked")
            List<Integer> tokens = (List<Integer>) response.get("result");
            
            long duration = System.currentTimeMillis() - startTime;
            logger.debug("Tokenized {} chars → {} tokens in {}ms", text.length(), tokens.size(), duration);
            
            return tokens;
            
        } catch (Exception e) {
            // Mark process as unhealthy if communication failed
            if (e instanceof IOException || e instanceof CompletionException) {
                process.isHealthy = false;
                logger.warn("Worker {} became unhealthy: {}", process.id, e.getMessage());
            }
            throw e;
            
        } finally {
            // Return process to pool
            busyProcesses.remove(process);
            
            if (process.isHealthy && process.process.isAlive()) {
                availableProcesses.offer(process);
            } else {
                // Process crashed or unhealthy, restart it
                logger.warn("Restarting unhealthy worker {}", process.id);
                try {
                    restartProcess(process);
                } catch (Exception restartEx) {
                    logger.error("Failed to restart worker {}: {}", process.id, restartEx.getMessage());
                    // Continue with reduced pool size
                }
            }
        }
    }
    
    /**
     * Detokenize token IDs using a process from the pool
     * 
     * @param tokens List of token IDs
     * @return Decoded text
     * @throws Exception if detokenization fails or timeout
     */
    public String detokenize(List<Integer> tokens) throws Exception {
        if (!isInitialized) {
            throw new IllegalStateException("Process pool not initialized");
        }
        
        if (isShutdown) {
            throw new IllegalStateException("Process pool is shut down");
        }
        
        long startTime = System.currentTimeMillis();
        
        // Acquire process from pool
        PooledProcess process = availableProcesses.poll(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        if (process == null) {
            throw new TimeoutException("No available process in pool (all busy)");
        }
        
        busyProcesses.add(process);
        
        try {
            // Build request
            String requestId = UUID.randomUUID().toString();
            Map<String, Object> request = new HashMap<>();
            request.put("command", "DETOKENIZE");
            request.put("id", requestId);
            request.put("tokens", tokens);
            
            // Send request
            String requestJson = mapper.writeValueAsString(request);
            process.stdin.write(requestJson + "\n");
            process.stdin.flush();
            
            // Read response with timeout
            CompletableFuture<String> responseFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return process.stdout.readLine();
                } catch (IOException e) {
                    throw new CompletionException(e);
                }
            });
            
            String responseLine = responseFuture.get(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            
            if (responseLine == null) {
                throw new IOException("Worker process sent EOF (crashed?)");
            }
            
            // Parse response
            Map<String, Object> response = mapper.readValue(responseLine, new TypeReference<Map<String, Object>>() {});
            String status = (String) response.get("status");
            
            if ("error".equals(status)) {
                String errorMsg = (String) response.get("message");
                throw new RuntimeException("Detokenization failed: " + errorMsg);
            }
            
            // Extract text
            String text = (String) response.get("result");
            
            long duration = System.currentTimeMillis() - startTime;
            logger.debug("Detokenized {} tokens → {} chars in {}ms", tokens.size(), text.length(), duration);
            
            return text;
            
        } catch (Exception e) {
            // Mark process as unhealthy if communication failed
            if (e instanceof IOException || e instanceof CompletionException) {
                process.isHealthy = false;
                logger.warn("Worker {} became unhealthy: {}", process.id, e.getMessage());
            }
            throw e;
            
        } finally {
            // Return process to pool
            busyProcesses.remove(process);
            
            if (process.isHealthy && process.process.isAlive()) {
                availableProcesses.offer(process);
            } else {
                // Process crashed, restart it
                logger.warn("Restarting unhealthy worker {}", process.id);
                try {
                    restartProcess(process);
                } catch (Exception restartEx) {
                    logger.error("Failed to restart worker {}: {}", process.id, restartEx.getMessage());
                }
            }
        }
    }
    
    /**
     * Restart a crashed or unhealthy process
     */
    private void restartProcess(PooledProcess crashed) throws Exception {
        // Destroy old process
        crashed.process.destroy();
        crashed.process.waitFor(2, TimeUnit.SECONDS);
        allProcesses.remove(crashed);
        
        // Start new process with same ID
        PooledProcess newProcess = startWorkerProcess(crashed.id);
        allProcesses.add(newProcess);
        availableProcesses.offer(newProcess);
        
        logger.info("✓ Worker {} restarted successfully", crashed.id);
    }
    
    /**
     * Graceful shutdown on application termination (Spring @PreDestroy)
     */
    @PreDestroy
    public void shutdown() {
        shutdownInternal();
    }
    
    /**
     * Internal shutdown implementation
     */
    private void shutdownInternal() {
        if (isShutdown) {
            return;
        }
        
        logger.info("Shutting down tokenizer process pool...");
        isShutdown = true;
        
        for (PooledProcess process : allProcesses) {
            try {
                // Send SHUTDOWN command
                String requestId = UUID.randomUUID().toString();
                Map<String, Object> request = new HashMap<>();
                request.put("command", "SHUTDOWN");
                request.put("id", requestId);
                
                String requestJson = mapper.writeValueAsString(request);
                process.stdin.write(requestJson + "\n");
                process.stdin.flush();
                
                // Wait for acknowledgment (with timeout)
                CompletableFuture<String> ackFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        return process.stdout.readLine();
                    } catch (IOException e) {
                        return null;
                    }
                });
                
                ackFuture.get(2, TimeUnit.SECONDS);
                
                // Wait for process to exit
                boolean exited = process.process.waitFor(3, TimeUnit.SECONDS);
                if (!exited) {
                    logger.warn("Worker {} did not exit gracefully, forcing destroy", process.id);
                    process.process.destroyForcibly();
                }
                
            } catch (Exception e) {
                logger.warn("Error during shutdown of worker {}: {}", process.id, e.getMessage());
                process.process.destroyForcibly();
            }
        }
        
        allProcesses.clear();
        availableProcesses.clear();
        busyProcesses.clear();
        
        logger.info("✓ Tokenizer process pool shut down");
    }
    
    /**
     * Get pool statistics (for monitoring)
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("initialized", isInitialized);
        stats.put("shutdown", isShutdown);
        stats.put("poolSize", POOL_SIZE);
        stats.put("available", availableProcesses.size());
        stats.put("busy", busyProcesses.size());
        stats.put("total", allProcesses.size());
        stats.put("healthy", allProcesses.stream().filter(p -> p.isHealthy).count());
        return stats;
    }
}
