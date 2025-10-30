package dev.neuralforge.ipc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.neuralforge.inference.PromptStrategy;
import dev.neuralforge.inference.T5InferenceEngine;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * REAL IPC Handler - NO MOCKS!
 * 
 * Reads JSON from stdin, processes requests, writes JSON to stdout.
 * This is the actual communication channel between Electron and Java backend.
 * 
 * Protocol: Line-delimited JSON
 * Example Request:  {"type":"ping","id":"1"}
 * Example Response: {"type":"pong","id":"1","timestamp":1234567890}
 */
@Component
public class IPCHandler {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Thread ipcThread;
    private volatile boolean running = false;
    
    @Autowired
    private T5InferenceEngine inferenceEngine;
    
    @PostConstruct
    public void start() {
        running = true;
        ipcThread = new Thread(this::processIPCMessages, "IPC-Handler");
        ipcThread.setDaemon(true);
        ipcThread.start();
        
        System.err.println("[IPC] Handler started, listening on stdin...");
    }
    
    @PreDestroy
    public void stop() {
        running = false;
        if (ipcThread != null) {
            ipcThread.interrupt();
        }
        System.err.println("[IPC] Handler stopped");
    }
    
    /**
     * Main IPC loop - reads from stdin, processes, writes to stdout
     * This is a REAL implementation, not a mock!
     */
    private void processIPCMessages() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(System.out, true, StandardCharsets.UTF_8)) {
            
            System.err.println("[IPC] Ready to receive messages");
            
            while (running) {
                String line = reader.readLine();
                
                if (line == null) {
                    // stdin closed, exit gracefully
                    System.err.println("[IPC] stdin closed, shutting down");
                    System.exit(0);
                    break;
                }
                
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                try {
                    JsonNode request = objectMapper.readTree(line);
                    JsonNode response = handleRequest(request);
                    
                    // Write response to stdout (IPC channel)
                    writer.println(objectMapper.writeValueAsString(response));
                    writer.flush();
                    
                } catch (Exception e) {
                    System.err.println("[IPC] Error processing message: " + e.getMessage());
                    
                    // Send error response
                    ObjectNode error = objectMapper.createObjectNode();
                    error.put("type", "error");
                    error.put("message", e.getMessage());
                    writer.println(objectMapper.writeValueAsString(error));
                    writer.flush();
                }
            }
            
        } catch (Exception e) {
            System.err.println("[IPC] Fatal error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }
    
    /**
     * Handle incoming IPC request
     * REAL logic, no stubs!
     */
    private JsonNode handleRequest(JsonNode request) {
        String type = request.path("type").asText();
        String id = request.path("id").asText("");
        
        System.err.println("[IPC] Received: " + type + " (id=" + id + ")");
        
        ObjectNode response = objectMapper.createObjectNode();
        response.put("id", id);
        
        switch (type) {
            case "ping" -> {
                response.put("type", "pong");
                response.put("timestamp", System.currentTimeMillis());
                response.put("message", "Backend is alive!");
            }
            
            case "status" -> {
                response.put("type", "status");
                response.put("uptime", System.currentTimeMillis());
                response.put("memoryMB", Runtime.getRuntime().totalMemory() / 1024 / 1024);
                response.put("freeMB", Runtime.getRuntime().freeMemory() / 1024 / 1024);
            }
            
            case "infer" -> {
                JsonNode inferResult = handleInferenceRequest(request);
                return inferResult;  // Return early to preserve id
            }
            
            default -> {
                response.put("type", "error");
                response.put("message", "Unknown request type: " + type);
            }
        }
        
        System.err.println("[IPC] Sent: " + response.path("type").asText());
        return response;
    }
    
    /**
     * Handle code inference request
     * Request format: {"type":"infer","id":"123","code":"def hello","language":"python","strategy":"TASK_PREFIX","modelName":"codet5plus-220m"}
     * Response format: {"type":"completion","id":"123","text":"...","latencyMs":14500,"tokensGenerated":50}
     */
    private JsonNode handleInferenceRequest(JsonNode request) {
        String id = request.path("id").asText("");
        String code = request.path("code").asText("");
        String language = request.path("language").asText("unknown");
        String strategyName = request.path("strategy").asText("TASK_PREFIX");
        String modelName = request.path("modelName").asText("codet5plus-220m");
        
        ObjectNode response = objectMapper.createObjectNode();
        response.put("id", id);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Validate input
            if (code.isEmpty()) {
                response.put("type", "error");
                response.put("message", "Code input is empty");
                return response;
            }
            
            // Initialize model if not already loaded
            if (!inferenceEngine.isInitialized()) {
                System.err.println("[IPC] Initializing inference engine with model: " + modelName);
                inferenceEngine.initialize(modelName);
            }
            
            // Parse prompt strategy
            PromptStrategy strategy;
            try {
                strategy = PromptStrategy.valueOf(strategyName.toUpperCase());
            } catch (IllegalArgumentException e) {
                System.err.println("[IPC] Unknown strategy '" + strategyName + "', using TASK_PREFIX");
                strategy = PromptStrategy.TASK_PREFIX;
            }
            
            // Run inference
            System.err.println("[IPC] Running inference: " + code.length() + " chars, strategy=" + strategy);
            String completion = inferenceEngine.generate(code, strategy);
            
            long latencyMs = System.currentTimeMillis() - startTime;
            
            // Build response
            response.put("type", "completion");
            response.put("text", completion);
            response.put("latencyMs", latencyMs);
            response.put("language", language);
            response.put("strategy", strategyName);
            response.put("modelName", modelName);
            
            System.err.println("[IPC] Inference complete: " + completion.length() + " chars in " + latencyMs + "ms");
            
        } catch (Exception e) {
            System.err.println("[IPC] Inference error: " + e.getMessage());
            e.printStackTrace(System.err);
            
            response.put("type", "error");
            response.put("message", "Inference failed: " + e.getMessage());
            response.put("latencyMs", System.currentTimeMillis() - startTime);
        }
        
        return response;
    }
}
