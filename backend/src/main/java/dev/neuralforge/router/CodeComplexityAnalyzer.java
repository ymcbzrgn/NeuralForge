package dev.neuralforge.router;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Analyzes code complexity using lightweight heuristics for model selection.
 * 
 * Complexity Score (0.0 - 1.0):
 * - 0.0-0.3: Simple code → Use 33M model (fast)
 * - 0.3-0.7: Medium code → Use 770M model (balanced)
 * - 0.7-1.0: Complex code → Use 3B model (quality)
 * 
 * Heuristics:
 * - Line count (more lines = more complex)
 * - Unique identifiers (more unique names = more complex)
 * - Nesting depth (deeper nesting = more complex)
 * - Language keywords (class, interface, async, etc.)
 * - Structural complexity (functions, classes, imports)
 * 
 * Performance: <10ms per analysis (no full AST parsing)
 * Follows KISS principle: Simple heuristics, good enough approximation
 */
@Component
public class CodeComplexityAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(CodeComplexityAnalyzer.class);
    
    // Regex patterns for analysis
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("\\b[a-zA-Z_][a-zA-Z0-9_]*\\b");
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("\\b(def|function|func|fn|void|public|private|protected)\\s+\\w+\\s*\\(");
    private static final Pattern CLASS_PATTERN = Pattern.compile("\\b(class|interface|struct|enum|trait)\\s+\\w+");
    private static final Pattern IMPORT_PATTERN = Pattern.compile("\\b(import|from|include|require|use)\\s+");
    private static final Pattern CONTROL_FLOW_PATTERN = Pattern.compile("\\b(if|else|elif|switch|case|for|while|do|try|catch|finally)\\b");
    private static final Pattern ASYNC_PATTERN = Pattern.compile("\\b(async|await|promise|future|thread)\\b");
    
    // Complexity keywords by language
    private static final Set<String> COMPLEX_KEYWORDS = Set.of(
        "class", "interface", "extends", "implements", "abstract",
        "async", "await", "promise", "thread", "synchronized",
        "generic", "template", "lambda", "closure", "decorator",
        "reflection", "metaclass", "trait", "protocol"
    );
    
    /**
     * Analyze code complexity and return normalized score (0.0 - 1.0)
     * 
     * @param code Source code to analyze
     * @return ComplexityScore with breakdown
     */
    public ComplexityScore analyzeComplexity(String code) {
        if (code == null || code.trim().isEmpty()) {
            return new ComplexityScore(0.0, "Empty input");
        }
        
        long startTime = System.nanoTime();
        
        // Remove comments to avoid skewing analysis
        String codeWithoutComments = removeComments(code);
        
        // Calculate individual metrics
        double lineComplexity = calculateLineComplexity(codeWithoutComments);
        double identifierComplexity = calculateIdentifierComplexity(codeWithoutComments);
        double nestingComplexity = calculateNestingComplexity(codeWithoutComments);
        double structuralComplexity = calculateStructuralComplexity(codeWithoutComments);
        double keywordComplexity = calculateKeywordComplexity(codeWithoutComments);
        
        // Weighted average (tuned for code completion use case)
        double totalScore = 
            lineComplexity * 0.08 +           // 8% - lines
            identifierComplexity * 0.17 +     // 17% - unique names  
            nestingComplexity * 0.40 +        // 40% - nesting key signal
            structuralComplexity * 0.25 +     // 25% - functions/classes
            keywordComplexity * 0.10;         // 10% - advanced patterns
        
        // Clamp to [0.0, 1.0]
        totalScore = Math.max(0.0, Math.min(1.0, totalScore));
        
        long durationMs = (System.nanoTime() - startTime) / 1_000_000;
        
        ComplexityScore score = new ComplexityScore(
            totalScore,
            lineComplexity,
            identifierComplexity,
            nestingComplexity,
            structuralComplexity,
            keywordComplexity,
            durationMs
        );
        
        logger.debug("Analyzed {} chars → complexity {:.2f} in {}ms", 
            code.length(), totalScore, durationMs);
        
        return score;
    }
    
    /**
     * Calculate complexity based on line count
     * More lines = more complex (but diminishing returns)
     */
    private double calculateLineComplexity(String code) {
        int lines = code.split("\n").length;
        
        // Normalize: 0-10 lines = 0.0, 50+ lines = 1.0
        if (lines <= 10) return 0.0;
        if (lines >= 50) return 1.0;
        
        return (lines - 10) / 40.0; // Linear scale 10-50 lines
    }
    
    /**
     * Calculate complexity based on unique identifiers
     * More unique names = more complex domain
     */
    private double calculateIdentifierComplexity(String code) {
        Set<String> uniqueIdentifiers = new HashSet<>();
        Matcher matcher = IDENTIFIER_PATTERN.matcher(code);
        
        while (matcher.find()) {
            String identifier = matcher.group();
            // Skip common short names and keywords
            if (identifier.length() > 2 && !isCommonKeyword(identifier)) {
                uniqueIdentifiers.add(identifier.toLowerCase());
            }
        }
        
        int uniqueCount = uniqueIdentifiers.size();
        
        // Normalize: 0-5 identifiers = 0.0, 30+ identifiers = 1.0
        if (uniqueCount <= 5) return 0.0;
        if (uniqueCount >= 30) return 1.0;
        
        return (uniqueCount - 5) / 25.0;
    }
    
    /**
     * Calculate complexity based on nesting depth
     * Deeper nesting = more complex control flow
     */
    private double calculateNestingComplexity(String code) {
        int maxDepth = 0;
        int currentDepth = 0;
        
        // Simple brace counting (works for C-like languages)
        for (char c : code.toCharArray()) {
            if (c == '{' || c == '(') {
                currentDepth++;
                maxDepth = Math.max(maxDepth, currentDepth);
            } else if (c == '}' || c == ')') {
                currentDepth--;
            }
        }
        
        // For Python-like indentation
        int maxIndent = 0;
        for (String line : code.split("\n")) {
            int indent = countLeadingSpaces(line);
            maxIndent = Math.max(maxIndent, indent / 4); // 4 spaces per level
        }
        
        int effectiveDepth = Math.max(maxDepth, maxIndent);
        
        // Normalize: 0-1 levels = 0.0, 5 levels = 1.0 (balanced)
        if (effectiveDepth <= 1) return 0.0;
        if (effectiveDepth >= 5) return 1.0;
        
        return (effectiveDepth - 1) / 4.0;
    }
    
    /**
     * Calculate complexity based on structural elements
     * Functions, classes, imports indicate larger codebase
     */
    private double calculateStructuralComplexity(String code) {
        int functionCount = countMatches(FUNCTION_PATTERN, code);
        int classCount = countMatches(CLASS_PATTERN, code);
        int importCount = countMatches(IMPORT_PATTERN, code);
        int controlFlowCount = countMatches(CONTROL_FLOW_PATTERN, code);
        
        int totalStructures = functionCount * 2 +  // Functions (reduced weight)
                             classCount * 4 +       // Classes  
                             importCount * 1 +      // Imports
                             controlFlowCount * 3;  // Control flow (increased!)
        
        // Normalize: 0-2 structures = 0.0, 25+ structures = 1.0
        if (totalStructures <= 2) return 0.0;
        if (totalStructures >= 25) return 1.0;
        
        return (totalStructures - 2) / 23.0;
    }
    
    /**
     * Calculate complexity based on advanced language keywords
     * async, generics, decorators = more complex patterns
     */
    private double calculateKeywordComplexity(String code) {
        int complexKeywordCount = 0;
        String lowerCode = code.toLowerCase();
        
        for (String keyword : COMPLEX_KEYWORDS) {
            if (lowerCode.contains(keyword)) {
                complexKeywordCount++;
            }
        }
        
        // Count async/await patterns
        complexKeywordCount += countMatches(ASYNC_PATTERN, code);
        
        // Normalize: 0-1 keywords = 0.0, 5+ keywords = 1.0 (more sensitive)
        if (complexKeywordCount <= 1) return 0.0;
        if (complexKeywordCount >= 5) return 1.0;
        
        return (complexKeywordCount - 1) / 4.0;
    }
    
    /**
     * Remove comments from code to avoid analysis skew
     */
    private String removeComments(String code) {
        // Remove single-line comments (// and #)
        code = code.replaceAll("//.*?$", "");
        code = code.replaceAll("#.*?$", "");
        
        // Remove multi-line comments (/* */ and ''' ''')
        code = code.replaceAll("/\\*.*?\\*/", "");
        code = code.replaceAll("'''.*?'''", "");
        code = code.replaceAll("\"\"\".*?\"\"\"", "");
        
        return code;
    }
    
    /**
     * Count pattern matches in code
     */
    private int countMatches(Pattern pattern, String code) {
        Matcher matcher = pattern.matcher(code);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
    
    /**
     * Count leading spaces in a line
     */
    private int countLeadingSpaces(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') count++;
            else if (c == '\t') count += 4; // Tab = 4 spaces
            else break;
        }
        return count;
    }
    
    /**
     * Check if identifier is a common programming keyword
     */
    private boolean isCommonKeyword(String identifier) {
        String lower = identifier.toLowerCase();
        return lower.matches("(if|for|while|do|try|int|str|var|let|const|new|this|self|return|break|continue)");
    }
    
    /**
     * Result of complexity analysis with breakdown
     */
    public static class ComplexityScore {
        private final double totalScore;
        private final double lineComplexity;
        private final double identifierComplexity;
        private final double nestingComplexity;
        private final double structuralComplexity;
        private final double keywordComplexity;
        private final long analysisTimeMs;
        private final String reason;
        
        public ComplexityScore(double totalScore, String reason) {
            this.totalScore = totalScore;
            this.reason = reason;
            this.lineComplexity = 0.0;
            this.identifierComplexity = 0.0;
            this.nestingComplexity = 0.0;
            this.structuralComplexity = 0.0;
            this.keywordComplexity = 0.0;
            this.analysisTimeMs = 0;
        }
        
        public ComplexityScore(double totalScore, double lineComplexity, 
                              double identifierComplexity, double nestingComplexity,
                              double structuralComplexity, double keywordComplexity,
                              long analysisTimeMs) {
            this.totalScore = totalScore;
            this.lineComplexity = lineComplexity;
            this.identifierComplexity = identifierComplexity;
            this.nestingComplexity = nestingComplexity;
            this.structuralComplexity = structuralComplexity;
            this.keywordComplexity = keywordComplexity;
            this.analysisTimeMs = analysisTimeMs;
            this.reason = generateReason();
        }
        
        private String generateReason() {
            if (totalScore < 0.3) {
                return "Simple code (short, few structures)";
            } else if (totalScore < 0.7) {
                return "Medium complexity (moderate structures/nesting)";
            } else {
                return "Complex code (many structures, deep nesting, advanced patterns)";
            }
        }
        
        public double getTotalScore() {
            return totalScore;
        }
        
        public double getLineComplexity() {
            return lineComplexity;
        }
        
        public double getIdentifierComplexity() {
            return identifierComplexity;
        }
        
        public double getNestingComplexity() {
            return nestingComplexity;
        }
        
        public double getStructuralComplexity() {
            return structuralComplexity;
        }
        
        public double getKeywordComplexity() {
            return keywordComplexity;
        }
        
        public long getAnalysisTimeMs() {
            return analysisTimeMs;
        }
        
        public String getReason() {
            return reason;
        }
        
        public String getRecommendedModel() {
            if (totalScore < 0.3) {
                return "codet5p-33m";  // Fast, simple code
            } else if (totalScore < 0.7) {
                return "codet5p-770m"; // Balanced
            } else {
                return "stablecode-3b"; // Quality, complex code
            }
        }
        
        @Override
        public String toString() {
            return String.format(
                "ComplexityScore[total=%.2f, lines=%.2f, identifiers=%.2f, nesting=%.2f, " +
                "structural=%.2f, keywords=%.2f, time=%dms, model=%s]",
                totalScore, lineComplexity, identifierComplexity, nestingComplexity,
                structuralComplexity, keywordComplexity, analysisTimeMs, getRecommendedModel()
            );
        }
    }
}
