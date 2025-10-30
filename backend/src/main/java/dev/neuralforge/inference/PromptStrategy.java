package dev.neuralforge.inference;

/**
 * Prompt engineering strategies for improving T5 code completion quality.
 * 
 * T5 models are trained with task prefixes (e.g., "translate English to French: ...")
 * We apply similar strategies to guide the model toward code generation instead of
 * generic text generation.
 * 
 * Note: These are workarounds until proper fine-tuning is applied (Phase 4).
 * Fine-tuned models won't need aggressive prompting.
 */
public enum PromptStrategy {
    
    /**
     * No prompt modification - use raw input
     */
    NONE(""),
    
    /**
     * T5-style task prefix
     * Example: "code completion: def hello" 
     */
    TASK_PREFIX("code completion: "),
    
    /**
     * Instruction-style prompt
     * Example: "Complete the following code:\n\ndef hello"
     */
    INSTRUCTION("Complete the following code:\n\n"),
    
    /**
     * Few-shot learning with examples
     * Provides 2-3 examples before the actual input
     */
    FEW_SHOT(""),  // Template defined in T5InferenceEngine
    
    /**
     * Language-aware prefix
     * Includes language hint to guide generation
     * Example: "Generate Python code:\ndef hello"
     */
    LANGUAGE_AWARE("Generate {language} code:\n");
    
    private final String prefix;
    
    PromptStrategy(String prefix) {
        this.prefix = prefix;
    }
    
    public String getPrefix() {
        return prefix;
    }
    
    /**
     * Apply this strategy to input code
     */
    public String apply(String code) {
        return prefix + code;
    }
    
    /**
     * Apply language-aware strategy
     */
    public String applyWithLanguage(String code, String language) {
        String languagePrefix = prefix.replace("{language}", language);
        return languagePrefix + code;
    }
}
