package dev.neuralforge.tokenizer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Service for tokenizing code strings into token IDs using Python tokenizer.
 * 
 * Uses external Python process with transformers library for HuggingFace tokenizer.
 * Follows KISS principle: delegate to Python instead of reimplementing BPE in Java.
 * 
 * Performance target: <10ms for 100 tokens
 */
@Service
public class TokenizerService {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenizerService.class);
    
    // Paths relative to backend/ directory
    private static final String PYTHON_VENV = "../models/.venv/bin/python";
    private static final String TOKENIZER_SCRIPT = "../models/nf_tokenize.py";
    private static final String DEFAULT_MODEL = "../models/base/codet5p-220m";
    
    // Timeout for tokenization process
    private static final long TIMEOUT_MS = 5000; // 5 seconds max
    
    private final ObjectMapper objectMapper;
    private final String pythonPath;
    private final String scriptPath;
    
    public TokenizerService() {
        this.objectMapper = new ObjectMapper();
        
        // Resolve absolute paths
        this.pythonPath = Paths.get(PYTHON_VENV).toAbsolutePath().normalize().toString();
        this.scriptPath = Paths.get(TOKENIZER_SCRIPT).toAbsolutePath().normalize().toString();
        
        validatePaths();
    }
    
    /**
     * Validate that Python and tokenizer script exist
     */
    private void validatePaths() {
        Path python = Path.of(pythonPath);
        Path script = Path.of(scriptPath);
        
        if (!Files.exists(python)) {
            logger.warn("Python venv not found at: {}. Tokenization will fail!", pythonPath);
        }
        
        if (!Files.exists(script)) {
            logger.warn("Tokenizer script not found at: {}. Tokenization will fail!", scriptPath);
        }
        
        if (Files.exists(python) && Files.exists(script)) {
            logger.info("Tokenizer initialized: Python={}, Script={}", pythonPath, scriptPath);
        }
    }
    
    /**
     * Tokenize code string into token IDs
     * 
     * @param code Code string to tokenize
     * @return TokenizationResult with token_ids and attention_mask
     * @throws TokenizationException if tokenization fails
     */
    public TokenizationResult tokenize(String code) throws TokenizationException {
        return tokenize(code, DEFAULT_MODEL);
    }
    
    /**
     * Tokenize code string with specific model
     * 
     * @param code Code string to tokenize
     * @param modelPath Path to model directory (relative to backend/)
     * @return TokenizationResult with token_ids and attention_mask
     * @throws TokenizationException if tokenization fails
     */
    public TokenizationResult tokenize(String code, String modelPath) throws TokenizationException {
        if (code == null || code.trim().isEmpty()) {
            throw new TokenizationException("Input code is empty");
        }
        
        long startTime = System.nanoTime();
        
        try {
            // Build process
            ProcessBuilder pb = new ProcessBuilder(
                pythonPath,
                scriptPath,
                modelPath
            );
            pb.redirectErrorStream(false); // Separate stderr for errors
            
            Process process = pb.start();
            
            // Write input to stdin
            try (OutputStreamWriter writer = new OutputStreamWriter(
                    process.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(code);
                writer.flush();
            }
            
            // Read output from stdout
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }
            
            // Read errors from stderr
            StringBuilder errors = new StringBuilder();
            try (BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errors.append(line).append("\n");
                }
            }
            
            // Wait for process to complete
            boolean finished = process.waitFor(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                throw new TokenizationException("Tokenization timeout after " + TIMEOUT_MS + "ms");
            }
            
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new TokenizationException(
                    "Tokenization failed (exit code " + exitCode + "): " + errors.toString()
                );
            }
            
            // Parse JSON output
            String jsonOutput = output.toString().trim();
            if (jsonOutput.isEmpty()) {
                throw new TokenizationException("Empty output from tokenizer. Errors: " + errors.toString());
            }
            
            JsonNode jsonNode = objectMapper.readTree(jsonOutput);
            
            // Check for error in output
            if (jsonNode.has("error")) {
                throw new TokenizationException("Tokenizer error: " + jsonNode.get("error").asText());
            }
            
            // Extract token_ids and attention_mask
            List<Long> tokenIds = new ArrayList<>();
            for (JsonNode idNode : jsonNode.get("token_ids")) {
                tokenIds.add(idNode.asLong());
            }
            
            List<Long> attentionMask = new ArrayList<>();
            for (JsonNode maskNode : jsonNode.get("attention_mask")) {
                attentionMask.add(maskNode.asLong());
            }
            
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            
            TokenizationResult result = new TokenizationResult(
                tokenIds,
                attentionMask,
                jsonNode.get("model").asText(),
                durationMs
            );
            
            logger.info("Tokenized {} chars → {} tokens in {}ms",
                code.length(), result.getTokenIds().size(), durationMs);
            
            return result;
            
        } catch (IOException e) {
            throw new TokenizationException("IO error during tokenization: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TokenizationException("Tokenization interrupted", e);
        }
    }
    
    /**
     * Result of tokenization
     */
    public static class TokenizationResult {
        private final List<Long> tokenIds;
        private final List<Long> attentionMask;
        private final String model;
        private final long durationMs;
        
        public TokenizationResult(List<Long> tokenIds, List<Long> attentionMask, String model, long durationMs) {
            this.tokenIds = tokenIds;
            this.attentionMask = attentionMask;
            this.model = model;
            this.durationMs = durationMs;
        }
        
        public List<Long> getTokenIds() {
            return tokenIds;
        }
        
        public List<Long> getAttentionMask() {
            return attentionMask;
        }
        
        public String getModel() {
            return model;
        }
        
        public long getDurationMs() {
            return durationMs;
        }
        
        public int getLength() {
            return tokenIds.size();
        }
    }
    
    /**
     * Exception thrown when tokenization fails
     */
    public static class TokenizationException extends Exception {
        public TokenizationException(String message) {
            super(message);
        }
        
        public TokenizationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
