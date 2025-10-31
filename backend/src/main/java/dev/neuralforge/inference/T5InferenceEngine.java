package dev.neuralforge.inference;

import ai.onnxruntime.*;
import dev.neuralforge.tokenizer.TokenizerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * T5 Encoder-Decoder inference engine for code completion.
 * 
 * Architecture:
 * 1. Tokenize input code
 * 2. Encoder forward pass: input_ids → encoder_hidden_states
 * 3. Decoder autoregressive generation:
 *    - Start with <pad> token (ID 0)
 *    - Run decoder: encoder_hidden_states + decoder_input_ids → logits
 *    - Pick next token (greedy search)
 *    - Repeat until EOS token or max length
 * 4. Detokenize output tokens → text
 * 
 * Target: <100ms for 20 token completion
 */
@Service
public class T5InferenceEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(T5InferenceEngine.class);
    
    private static final String MODELS_DIR = "../models/base";
    private static final int PAD_TOKEN_ID = 0;
    private static final int EOS_TOKEN_ID = 1;  // </s> token
    private static final int MAX_NEW_TOKENS = 50;  // Maximum tokens to generate
    
    @Autowired
    private TokenizerService tokenizerService;
    
    private OrtEnvironment environment;
    private OrtSession encoderSession;
    private OrtSession decoderSession;
    private String loadedModelName;
    private boolean isInitialized = false;
    
    /**
     * Initialize ONNX Runtime and load encoder + decoder models
     */
    public void initialize(String modelName) throws OrtException {
        logger.info("Initializing T5 inference engine for model: {}", modelName);
        long startTime = System.currentTimeMillis();
        
        try {
            // Initialize ONNX Runtime
            environment = OrtEnvironment.getEnvironment();
            
            // Find encoder and decoder models
            Path modelDir = Paths.get(MODELS_DIR, modelName, "onnx");
            if (!Files.exists(modelDir)) {
                throw new RuntimeException("Model ONNX directory not found: " + modelDir);
            }
            
            Path encoderPath = modelDir.resolve("encoder_model.onnx");
            Path decoderPath = modelDir.resolve("decoder_model.onnx");
            
            if (!Files.exists(encoderPath)) {
                throw new RuntimeException("Encoder model not found: " + encoderPath);
            }
            if (!Files.exists(decoderPath)) {
                throw new RuntimeException("Decoder model not found: " + decoderPath);
            }
            
            // Load encoder
            logger.info("Loading encoder from: {}", encoderPath);
            OrtSession.SessionOptions encoderOptions = new OrtSession.SessionOptions();
            encoderOptions.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT);
            encoderOptions.setIntraOpNumThreads(2);
            encoderSession = environment.createSession(encoderPath.toString(), encoderOptions);
            
            // Load decoder
            logger.info("Loading decoder from: {}", decoderPath);
            OrtSession.SessionOptions decoderOptions = new OrtSession.SessionOptions();
            decoderOptions.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT);
            decoderOptions.setIntraOpNumThreads(2);
            decoderSession = environment.createSession(decoderPath.toString(), decoderOptions);
            
            loadedModelName = modelName;
            isInitialized = true;
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("✓ T5 inference engine initialized in {}ms", duration);
            
            logModelInfo();
            
        } catch (Exception e) {
            logger.error("Failed to initialize T5 inference engine", e);
            throw new RuntimeException("T5 initialization failed", e);
        }
    }
    
    /**
     * Log model input/output information
     */
    private void logModelInfo() throws OrtException {
        logger.info("Encoder inputs: {}", encoderSession.getInputNames());
        logger.info("Encoder outputs: {}", encoderSession.getOutputNames());
        logger.info("Decoder inputs: {}", decoderSession.getInputNames());
        logger.info("Decoder outputs: {}", decoderSession.getOutputNames());
    }
    
    /**
     * Generate code completion for given input
     * 
     * @param inputCode Code context to complete
     * @return Generated completion text
     */
    public String generate(String inputCode) throws Exception {
        return generate(inputCode, PromptStrategy.NONE);
    }
    
    /**
     * Generate code completion with specific prompt strategy
     * 
     * @param inputCode Code context to complete
     * @param strategy Prompt engineering strategy to apply
     * @return Generated completion text
     */
    public String generate(String inputCode, PromptStrategy strategy) throws Exception {
        if (!isInitialized) {
            throw new IllegalStateException("Engine not initialized. Call initialize() first.");
        }
        
        // Apply prompt strategy
        String promptedInput = applyPromptStrategy(inputCode, strategy);
        
        long startTime = System.nanoTime();
        
        // Step 1: Tokenize input
        logger.info("Step 1: Tokenizing input ({} chars)", promptedInput.length());
        TokenizerService.TokenizationResult tokenization = tokenizerService.tokenize(promptedInput);
        List<Long> inputIds = tokenization.getTokenIds();
        List<Long> attentionMask = tokenization.getAttentionMask();
        
        logger.info("Tokenized: {} tokens", inputIds.size());
        
        // Step 2: Encoder forward pass
        logger.info("Step 2: Running encoder");
        OnnxTensor encoderHiddenStates = runEncoder(inputIds, attentionMask);
        
        // Step 3: Decoder autoregressive generation
        logger.info("Step 3: Running decoder (autoregressive)");
        List<Long> generatedTokens = runDecoder(encoderHiddenStates, attentionMask);
        
        // Step 4: Detokenize
        logger.info("Step 4: Detokenizing {} tokens", generatedTokens.size());
        String completion = detokenize(generatedTokens);
        
        long durationMs = (System.nanoTime() - startTime) / 1_000_000;
        logger.info("✓ Generation complete in {}ms: '{}'", durationMs, 
            completion.length() > 50 ? completion.substring(0, 50) + "..." : completion);
        
        // Cleanup
        encoderHiddenStates.close();
        
        return completion;
    }
    
    /**
     * Run encoder: input_ids → encoder_hidden_states
     */
    private OnnxTensor runEncoder(List<Long> inputIds, List<Long> attentionMask) throws OrtException {
        int batchSize = 1;
        int seqLen = inputIds.size();
        
        // Create input tensors
        long[][] inputIdsArray = new long[batchSize][seqLen];
        long[][] attentionMaskArray = new long[batchSize][seqLen];
        
        for (int i = 0; i < seqLen; i++) {
            inputIdsArray[0][i] = inputIds.get(i);
            attentionMaskArray[0][i] = attentionMask.get(i);
        }
        
        OnnxTensor inputIdsTensor = OnnxTensor.createTensor(environment, inputIdsArray);
        OnnxTensor attentionMaskTensor = OnnxTensor.createTensor(environment, attentionMaskArray);
        
        // Run encoder
        Map<String, OnnxTensor> inputs = new HashMap<>();
        inputs.put("input_ids", inputIdsTensor);
        inputs.put("attention_mask", attentionMaskTensor);
        
        OrtSession.Result result = encoderSession.run(inputs);
        
        // Get encoder hidden states (last_hidden_state)
        OnnxTensor hiddenStates = (OnnxTensor) result.get("last_hidden_state").get();
        
        // Cleanup input tensors
        inputIdsTensor.close();
        attentionMaskTensor.close();
        
        return hiddenStates;
    }
    
    /**
     * Run decoder autoregressively: generate tokens one by one
     */
    private List<Long> runDecoder(OnnxTensor encoderHiddenStates, List<Long> encoderAttentionMask) 
            throws OrtException {
        
        List<Long> generatedTokens = new ArrayList<>();
        List<Long> decoderInputIds = new ArrayList<>();
        decoderInputIds.add((long) PAD_TOKEN_ID); // Start with <pad>
        
        int batchSize = 1;
        int encoderSeqLen = encoderAttentionMask.size();
        
        // Create encoder attention mask tensor (constant throughout generation)
        long[][] encoderAttentionMaskArray = new long[batchSize][encoderSeqLen];
        for (int i = 0; i < encoderSeqLen; i++) {
            encoderAttentionMaskArray[0][i] = encoderAttentionMask.get(i);
        }
        OnnxTensor encoderAttentionMaskTensor = OnnxTensor.createTensor(
            environment, encoderAttentionMaskArray
        );
        
        // Autoregressive generation loop
        for (int step = 0; step < MAX_NEW_TOKENS; step++) {
            int decoderSeqLen = decoderInputIds.size();
            
            // Create decoder input tensors
            long[][] decoderInputIdsArray = new long[batchSize][decoderSeqLen];
            for (int i = 0; i < decoderSeqLen; i++) {
                decoderInputIdsArray[0][i] = decoderInputIds.get(i);
            }
            OnnxTensor decoderInputIdsTensor = OnnxTensor.createTensor(
                environment, decoderInputIdsArray
            );
            
            // Run decoder forward pass
            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put("input_ids", decoderInputIdsTensor);
            inputs.put("encoder_attention_mask", encoderAttentionMaskTensor);
            inputs.put("encoder_hidden_states", encoderHiddenStates);
            
            OrtSession.Result result = decoderSession.run(inputs);
            
            // Get logits [batch, seq_len, vocab_size]
            OnnxTensor logitsTensor = (OnnxTensor) result.get("logits").get();
            float[][][] logits = (float[][][]) logitsTensor.getValue();
            
            // Greedy search: pick token with highest probability at last position
            int lastPos = decoderSeqLen - 1;
            float[] lastLogits = logits[0][lastPos];
            int nextTokenId = argmax(lastLogits);
            
            // Cleanup
            decoderInputIdsTensor.close();
            logitsTensor.close();
            result.close();
            
            // Check for EOS token
            if (nextTokenId == EOS_TOKEN_ID) {
                logger.info("EOS token generated at step {}", step);
                break;
            }
            
            // Add to generated tokens
            generatedTokens.add((long) nextTokenId);
            decoderInputIds.add((long) nextTokenId);
            
            // Log progress every 10 tokens
            if (step % 10 == 0) {
                logger.debug("Generated {} tokens...", step);
            }
        }
        
        // Cleanup
        encoderAttentionMaskTensor.close();
        
        logger.info("Generated {} tokens", generatedTokens.size());
        return generatedTokens;
    }
    
    /**
     * Find index of maximum value (argmax)
     */
    private int argmax(float[] array) {
        int maxIdx = 0;
        float maxVal = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > maxVal) {
                maxVal = array[i];
                maxIdx = i;
            }
        }
        return maxIdx;
    }
    
    /**
     * Apply prompt engineering strategy to input code
     */
    private String applyPromptStrategy(String code, PromptStrategy strategy) {
        switch (strategy) {
            case NONE:
                return code;
                
            case TASK_PREFIX:
                return strategy.apply(code);
                
            case INSTRUCTION:
                return strategy.apply(code);
                
            case FEW_SHOT:
                return getFewShotPrompt(code);
                
            case LANGUAGE_AWARE:
                // Auto-detect language (simple heuristic)
                String language = detectLanguage(code);
                return strategy.applyWithLanguage(code, language);
                
            default:
                return code;
        }
    }
    
    /**
     * Create few-shot prompt with examples
     */
    private String getFewShotPrompt(String code) {
        String examples = """
            # Example 1: Python function
            def calculate
            def calculate_sum(a, b):
                return a + b
            
            # Example 2: Java class
            public class User
            public class User {
                private String name;
            }
            
            # Your code:
            """;
        return examples + code;
    }
    
    /**
     * Simple language detection based on code patterns
     */
    private String detectLanguage(String code) {
        code = code.trim().toLowerCase();
        
        if (code.startsWith("def ") || code.startsWith("import ") || code.contains("print(")) {
            return "Python";
        } else if (code.startsWith("public ") || code.startsWith("private ") || 
                   code.contains("class ") || code.contains("void ")) {
            return "Java";
        } else if (code.startsWith("function ") || code.startsWith("const ") || 
                   code.startsWith("let ") || code.contains("=>")) {
            return "JavaScript";
        } else if (code.startsWith("func ") || code.contains(":=")) {
            return "Go";
        } else {
            return "code";  // Generic fallback
        }
    }
    
    /**
     * Detokenize token IDs back to text using TokenizerService (with process pool)
     * 
     * BEFORE: Used ProcessBuilder (~2800ms with Python startup overhead)
     * NOW: Uses TokenizerProcessPool (<100ms, no startup!)
     */
    private String detokenize(List<Long> tokenIds) throws Exception {
        if (tokenIds.isEmpty()) {
            return "";
        }
        
        // Use TokenizerService which delegates to process pool
        return tokenizerService.detokenize(tokenIds);
    }
    
    /**
     * Cleanup resources
     */
    public void shutdown() {
        logger.info("Shutting down T5 inference engine");
        
        if (encoderSession != null) {
            try {
                encoderSession.close();
            } catch (Exception e) {
                logger.warn("Error closing encoder session", e);
            }
        }
        
        if (decoderSession != null) {
            try {
                decoderSession.close();
            } catch (Exception e) {
                logger.warn("Error closing decoder session", e);
            }
        }
        
        if (environment != null) {
            try {
                environment.close();
            } catch (Exception e) {
                logger.warn("Error closing environment", e);
            }
        }
        
        isInitialized = false;
        logger.info("T5 inference engine shutdown complete");
    }
    
    public boolean isInitialized() {
        return isInitialized;
    }
    
    public String getLoadedModelName() {
        return loadedModelName;
    }
}
