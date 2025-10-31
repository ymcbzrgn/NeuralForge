package dev.neuralforge.router;

import dev.neuralforge.memory.MemoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Intelligent model router that selects optimal model based on:
 * 1. Code complexity (from CodeComplexityAnalyzer)
 * 2. Available memory (from MemoryManager)
 * 
 * Model selection strategy:
 * - Simple code (0.0-0.3) → codet5p-33m (fast, 33MB)
 * - Medium code (0.3-0.7) → codet5p-770m (balanced, 770MB)
 * - Complex code (0.7-1.0) → stablecode-3b (quality, 3GB)
 * 
 * Fallback strategy if insufficient memory:
 * - Try next smaller model
 * - Ultimate fallback: codet5p-33m (always fits in 512MB+ buffer)
 */
@Component
public class ModelRouter {
    private static final Logger logger = LoggerFactory.getLogger(ModelRouter.class);
    
    private final CodeComplexityAnalyzer complexityAnalyzer;
    private final MemoryManager memoryManager;
    
    // Model sizes (approximate ONNX sizes in bytes)
    private static final long MODEL_33M_SIZE = 33L * 1024 * 1024;      // 33MB
    private static final long MODEL_770M_SIZE = 770L * 1024 * 1024;    // 770MB
    private static final long MODEL_3B_SIZE = 3L * 1024 * 1024 * 1024; // 3GB
    
    // Complexity thresholds
    private static final double SIMPLE_THRESHOLD = 0.3;
    private static final double COMPLEX_THRESHOLD = 0.7;
    
    public ModelRouter(CodeComplexityAnalyzer complexityAnalyzer, MemoryManager memoryManager) {
        this.complexityAnalyzer = complexityAnalyzer;
        this.memoryManager = memoryManager;
    }
    
    /**
     * Select optimal model for given code context.
     * 
     * Algorithm:
     * 1. Analyze code complexity
     * 2. Determine ideal model based on complexity
     * 3. Check if enough memory to load ideal model
     * 4. Fallback to smaller model if memory insufficient
     * 
     * @param code source code to analyze
     * @return ModelSelection with chosen model and reasoning
     */
    public ModelSelection selectModel(String code) {
        // Step 1: Analyze complexity
        CodeComplexityAnalyzer.ComplexityScore complexityScore = 
            complexityAnalyzer.analyzeComplexity(code);
        
        double score = complexityScore.getTotalScore();
        
        logger.debug("Code complexity: {} (lines={}, identifiers={}, nesting={}, structural={}, keywords={})",
            score,
            complexityScore.getLineComplexity(),
            complexityScore.getIdentifierComplexity(),
            complexityScore.getNestingComplexity(),
            complexityScore.getStructuralComplexity(),
            complexityScore.getKeywordComplexity()
        );
        
        // Step 2: Determine ideal model
        ModelType idealModel = determineIdealModel(score);
        
        // Step 3: Check memory and fallback if needed
        ModelType selectedModel = selectWithMemoryConstraints(idealModel, score);
        
        // Step 4: Build reasoning
        String reasoning = buildReasoning(score, idealModel, selectedModel);
        
        logger.info("Model selection: {} (complexity={}, ideal={}, reasoning={})",
            selectedModel, score, idealModel, reasoning);
        
        return new ModelSelection(selectedModel, score, reasoning);
    }
    
    /**
     * Determine ideal model based only on complexity score.
     */
    private ModelType determineIdealModel(double score) {
        if (score < SIMPLE_THRESHOLD) {
            return ModelType.CODET5P_33M;
        } else if (score < COMPLEX_THRESHOLD) {
            return ModelType.CODET5P_770M;
        } else {
            return ModelType.STABLECODE_3B;
        }
    }
    
    /**
     * Select model considering memory constraints.
     * Falls back to smaller model if insufficient memory.
     */
    private ModelType selectWithMemoryConstraints(ModelType idealModel, double score) {
        switch (idealModel) {
            case STABLECODE_3B:
                if (memoryManager.canLoadModel(MODEL_3B_SIZE)) {
                    return ModelType.STABLECODE_3B;
                }
                logger.warn("Insufficient memory for StableCode-3B, trying 770M");
                // Fall through to 770M
                
            case CODET5P_770M:
                if (memoryManager.canLoadModel(MODEL_770M_SIZE)) {
                    return ModelType.CODET5P_770M;
                }
                logger.warn("Insufficient memory for CodeT5+ 770M, falling back to 33M");
                // Fall through to 33M
                
            case CODET5P_33M:
            default:
                // 33M is always our fallback (should always fit)
                if (!memoryManager.canLoadModel(MODEL_33M_SIZE)) {
                    logger.error("CRITICAL: Insufficient memory even for 33M model!");
                    // Suggest GC and hope for the best
                    memoryManager.suggestGC();
                }
                return ModelType.CODET5P_33M;
        }
    }
    
    /**
     * Build human-readable reasoning for model selection.
     */
    private String buildReasoning(double score, ModelType ideal, ModelType selected) {
        if (ideal == selected) {
            return String.format("Complexity %.2f → %s (ideal match)", score, selected.getDisplayName());
        } else {
            return String.format("Complexity %.2f → %s preferred, but insufficient memory → %s fallback",
                score, ideal.getDisplayName(), selected.getDisplayName());
        }
    }
    
    /**
     * Available model types.
     */
    public enum ModelType {
        CODET5P_33M("codet5p-33m", "CodeT5+ 33M", MODEL_33M_SIZE),
        CODET5P_770M("codet5p-770m", "CodeT5+ 770M", MODEL_770M_SIZE),
        STABLECODE_3B("stablecode-3b", "StableCode 3B", MODEL_3B_SIZE);
        
        private final String modelId;
        private final String displayName;
        private final long sizeBytes;
        
        ModelType(String modelId, String displayName, long sizeBytes) {
            this.modelId = modelId;
            this.displayName = displayName;
            this.sizeBytes = sizeBytes;
        }
        
        public String getModelId() {
            return modelId;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public long getSizeBytes() {
            return sizeBytes;
        }
    }
    
    /**
     * Model selection result with reasoning.
     */
    public record ModelSelection(
        ModelType model,
        double complexityScore,
        String reasoning
    ) {
        public String getModelId() {
            return model.getModelId();
        }
        
        public String getDisplayName() {
            return model.getDisplayName();
        }
        
        public long getModelSize() {
            return model.getSizeBytes();
        }
    }
}
