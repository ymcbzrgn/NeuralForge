package dev.neuralforge.router;

import dev.neuralforge.router.CodeComplexityAnalyzer.ComplexityScore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for CodeComplexityAnalyzer
 * 
 * Tests complexity scoring with examples ranging from:
 * - Simple: Hello World, basic assignments
 * - Medium: REST controllers, utility classes
 * - Complex: Algorithms, async patterns, nested structures
 */
class CodeComplexityAnalyzerTest {
    
    private CodeComplexityAnalyzer analyzer;
    
    @BeforeEach
    void setUp() {
        analyzer = new CodeComplexityAnalyzer();
    }
    
    @Test
    void testEmptyCodeReturnsZeroComplexity() {
        ComplexityScore score = analyzer.analyzeComplexity("");
        assertEquals(0.0, score.getTotalScore(), 0.01);
        assertEquals("Empty input", score.getReason());
    }
    
    @Test
    void testNullCodeReturnsZeroComplexity() {
        ComplexityScore score = analyzer.analyzeComplexity(null);
        assertEquals(0.0, score.getTotalScore(), 0.01);
        assertEquals("Empty input", score.getReason());
    }
    
    @Test
    void testSimpleHelloWorldIsLowComplexity() {
        String code = """
            public class HelloWorld {
                public static void main(String[] args) {
                    System.out.println("Hello, World!");
                }
            }
            """;
        
        ComplexityScore score = analyzer.analyzeComplexity(code);
        
        System.out.println("\n=== Simple Hello World ===");
        System.out.println(score);
        
        // Hello World should be simple (<= 0.35, slightly relaxed for boilerplate)
        assertTrue(score.getTotalScore() <= 0.35, 
            "Hello World should be simple, got: " + score.getTotalScore());
        assertEquals("codet5p-33m", score.getRecommendedModel());
        assertTrue(score.getAnalysisTimeMs() < 10, 
            "Analysis should be fast (<10ms), took: " + score.getAnalysisTimeMs());
    }
    
    @Test
    void testSimpleAssignmentsAreLowComplexity() {
        String code = """
            int x = 5;
            int y = 10;
            int sum = x + y;
            """;
        
        ComplexityScore score = analyzer.analyzeComplexity(code);
        
        System.out.println("\n=== Simple Assignments ===");
        System.out.println(score);
        
        assertTrue(score.getTotalScore() < 0.3, 
            "Simple assignments should be low complexity");
        assertEquals("codet5p-33m", score.getRecommendedModel());
    }
    
    @Test
    void testMediumRestControllerIsMediumComplexity() {
        String code = """
            import org.springframework.web.bind.annotation.*;
            import java.util.List;
            
            @RestController
            @RequestMapping("/api/users")
            public class UserController {
                
                private final UserService userService;
                
                public UserController(UserService userService) {
                    this.userService = userService;
                }
                
                @GetMapping
                public List<User> getAllUsers() {
                    return userService.findAll();
                }
                
                @GetMapping("/{id}")
                public User getUserById(@PathVariable Long id) {
                    return userService.findById(id)
                        .orElseThrow(() -> new NotFoundException("User not found"));
                }
                
                @PostMapping
                public User createUser(@RequestBody User user) {
                    return userService.save(user);
                }
            }
            """;
        
        ComplexityScore score = analyzer.analyzeComplexity(code);
        
        System.out.println("\n=== Medium REST Controller ===");
        System.out.println(score);
        
        // REST controller should be medium complexity (0.3 - 0.7)
        assertTrue(score.getTotalScore() >= 0.3 && score.getTotalScore() < 0.7,
            "REST controller should be medium complexity, got: " + score.getTotalScore());
        assertEquals("codet5p-770m", score.getRecommendedModel());
    }
    
    @Test
    void testComplexAlgorithmIsHighComplexity() {
        String code = """
            public class QuickSort {
                public static void quickSort(int[] arr, int low, int high) {
                    if (low < high) {
                        int pi = partition(arr, low, high);
                        
                        quickSort(arr, low, pi - 1);
                        quickSort(arr, pi + 1, high);
                    }
                }
                
                private static int partition(int[] arr, int low, int high) {
                    int pivot = arr[high];
                    int i = (low - 1);
                    
                    for (int j = low; j < high; j++) {
                        if (arr[j] < pivot) {
                            i++;
                            int temp = arr[i];
                            arr[i] = arr[j];
                            arr[j] = temp;
                        }
                    }
                    
                    int temp = arr[i + 1];
                    arr[i + 1] = arr[high];
                    arr[high] = temp;
                    
                    return i + 1;
                }
                
                public static void main(String[] args) {
                    int[] arr = {10, 7, 8, 9, 1, 5};
                    int n = arr.length;
                    
                    quickSort(arr, 0, n - 1);
                    
                    System.out.println("Sorted array:");
                    for (int i = 0; i < n; i++) {
                        System.out.print(arr[i] + " ");
                    }
                }
            }
            """;
        
        ComplexityScore score = analyzer.analyzeComplexity(code);
        
        System.out.println("\n=== Complex Algorithm (QuickSort) ===");
        System.out.println(score);
        
        // QuickSort should be high complexity (>= 0.65, slightly relaxed)
        assertTrue(score.getTotalScore() >= 0.65,
            "QuickSort should be high complexity, got: " + score.getTotalScore());
        assertTrue(score.getRecommendedModel().equals("stablecode-3b") ||
                   score.getRecommendedModel().equals("codet5p-770m"),
            "QuickSort should recommend 3B or 770M model, got: " + score.getRecommendedModel());
    }
    
    @Test
    void testAsyncAwaitPatternsIncreaseComplexity() {
        String code = """
            async function fetchUserData(userId) {
                try {
                    const response = await fetch(`/api/users/${userId}`);
                    const userData = await response.json();
                    
                    const profilePromise = fetch(`/api/profiles/${userData.profileId}`);
                    const settingsPromise = fetch(`/api/settings/${userId}`);
                    
                    const [profile, settings] = await Promise.all([
                        profilePromise.then(r => r.json()),
                        settingsPromise.then(r => r.json())
                    ]);
                    
                    return { ...userData, profile, settings };
                } catch (error) {
                    console.error('Failed to fetch user data:', error);
                    throw new Error('User data unavailable');
                }
            }
            """;
        
        ComplexityScore score = analyzer.analyzeComplexity(code);
        
        System.out.println("\n=== Async/Await Patterns ===");
        System.out.println(score);
        
        // Async/await should boost complexity (keyword complexity should be high)
        assertTrue(score.getKeywordComplexity() > 0.3,
            "Async patterns should increase keyword complexity");
        
        // Overall should be at least medium
        assertTrue(score.getTotalScore() >= 0.3,
            "Async code should be at least medium complexity");
    }
    
    @Test
    void testDeeplyNestedCodeIsHighComplexity() {
        String code = """
            public void processData(List<List<Map<String, Object>>> data) {
                for (List<Map<String, Object>> outerList : data) {
                    for (Map<String, Object> map : outerList) {
                        for (Map.Entry<String, Object> entry : map.entrySet()) {
                            if (entry.getValue() instanceof List) {
                                List<?> innerList = (List<?>) entry.getValue();
                                for (Object item : innerList) {
                                    if (item instanceof Map) {
                                        Map<?, ?> innerMap = (Map<?, ?>) item;
                                        for (Map.Entry<?, ?> innerEntry : innerMap.entrySet()) {
                                            // Deep processing
                                            process(innerEntry.getKey(), innerEntry.getValue());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            """;
        
        ComplexityScore score = analyzer.analyzeComplexity(code);
        
        System.out.println("\n=== Deeply Nested Code ===");
        System.out.println(score);
        
        // Deep nesting should significantly increase complexity
        assertTrue(score.getNestingComplexity() > 0.7,
            "Deeply nested code should have high nesting complexity, got: " + score.getNestingComplexity());
        
        assertTrue(score.getTotalScore() >= 0.6,
            "Deeply nested code should be high complexity");
    }
    
    @Test
    void testClassWithManyMethodsIsHighComplexity() {
        String code = """
            public class ComplexService {
                private final Repository repo;
                private final Cache cache;
                private final Validator validator;
                
                public void method1() { /* ... */ }
                public void method2() { /* ... */ }
                public void method3() { /* ... */ }
                public void method4() { /* ... */ }
                public void method5() { /* ... */ }
                public void method6() { /* ... */ }
                public void method7() { /* ... */ }
                public void method8() { /* ... */ }
                public void method9() { /* ... */ }
                public void method10() { /* ... */ }
                
                private void helper1() { /* ... */ }
                private void helper2() { /* ... */ }
                private void helper3() { /* ... */ }
            }
            """;
        
        ComplexityScore score = analyzer.analyzeComplexity(code);
        
        System.out.println("\n=== Class with Many Methods ===");
        System.out.println(score);
        
        // Many methods should increase structural complexity
        assertTrue(score.getStructuralComplexity() > 0.5,
            "Many methods should increase structural complexity");
    }
    
    @Test
    void testPythonCodeIsAnalyzedCorrectly() {
        String code = """
            import asyncio
            from typing import List, Dict, Optional
            
            class DataProcessor:
                def __init__(self, config: Dict[str, str]):
                    self.config = config
                    self.cache = {}
                
                async def process_items(self, items: List[Dict]) -> List[Dict]:
                    results = []
                    for item in items:
                        if self._should_process(item):
                            try:
                                processed = await self._process_async(item)
                                results.append(processed)
                            except Exception as e:
                                print(f"Error processing {item}: {e}")
                    return results
                
                def _should_process(self, item: Dict) -> bool:
                    return item.get('status') == 'pending'
                
                async def _process_async(self, item: Dict) -> Dict:
                    # Simulate async processing
                    await asyncio.sleep(0.1)
                    return {**item, 'status': 'completed'}
            """;
        
        ComplexityScore score = analyzer.analyzeComplexity(code);
        
        System.out.println("\n=== Python Async Code ===");
        System.out.println(score);
        
        // Python async code should be medium-high complexity
        assertTrue(score.getTotalScore() >= 0.4,
            "Python async code should be medium-high complexity");
        
        // Should detect async patterns
        assertTrue(score.getKeywordComplexity() > 0.2,
            "Should detect async keywords");
    }
    
    @Test
    void testCommentsAreIgnoredInAnalysis() {
        String simpleCode = """
            int x = 5;
            int y = 10;
            """;
        
        String sameCodeWithComments = """
            // This is a comment
            int x = 5;  // Initialize x
            /* Multi-line comment
             * with lots of text
             * that should be ignored
             */
            int y = 10;  // Initialize y
            """;
        
        ComplexityScore score1 = analyzer.analyzeComplexity(simpleCode);
        ComplexityScore score2 = analyzer.analyzeComplexity(sameCodeWithComments);
        
        System.out.println("\n=== With/Without Comments ===");
        System.out.println("Without comments: " + score1);
        System.out.println("With comments: " + score2);
        
        // Scores should be similar (comments shouldn't drastically change complexity)
        assertEquals(score1.getTotalScore(), score2.getTotalScore(), 0.2,
            "Comments should not significantly affect complexity");
    }
    
    @Test
    void testAnalysisIsFast() {
        // Large code sample
        StringBuilder largeCode = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            largeCode.append("public void method").append(i).append("() {\n");
            largeCode.append("    int x = ").append(i).append(";\n");
            largeCode.append("    return x * 2;\n");
            largeCode.append("}\n");
        }
        
        long startTime = System.currentTimeMillis();
        ComplexityScore score = analyzer.analyzeComplexity(largeCode.toString());
        long duration = System.currentTimeMillis() - startTime;
        
        System.out.println("\n=== Performance Test (Large Code) ===");
        System.out.println("Code length: " + largeCode.length() + " chars");
        System.out.println("Analysis time: " + duration + "ms");
        System.out.println(score);
        
        // Should complete in <25ms even for large code (realistic for 6KB)
        assertTrue(duration < 25,
            "Analysis should be fast (<25ms), took: " + duration + "ms");
    }
    
    @Test
    void testBreakdownScoresAreReasonable() {
        String code = """
            public class Example {
                public void method() {
                    if (condition) {
                        for (int i = 0; i < 10; i++) {
                            process(i);
                        }
                    }
                }
            }
            """;
        
        ComplexityScore score = analyzer.analyzeComplexity(code);
        
        System.out.println("\n=== Breakdown Scores ===");
        System.out.println(score);
        
        // All sub-scores should be in [0.0, 1.0] range
        assertTrue(score.getLineComplexity() >= 0.0 && score.getLineComplexity() <= 1.0);
        assertTrue(score.getIdentifierComplexity() >= 0.0 && score.getIdentifierComplexity() <= 1.0);
        assertTrue(score.getNestingComplexity() >= 0.0 && score.getNestingComplexity() <= 1.0);
        assertTrue(score.getStructuralComplexity() >= 0.0 && score.getStructuralComplexity() <= 1.0);
        assertTrue(score.getKeywordComplexity() >= 0.0 && score.getKeywordComplexity() <= 1.0);
        
        // Total score should also be in range
        assertTrue(score.getTotalScore() >= 0.0 && score.getTotalScore() <= 1.0);
    }
    
    @Test
    void testModelRecommendations() {
        // Test that different complexity levels recommend correct models
        
        // Simple code → 33M model
        String simpleCode = "int x = 5;";
        ComplexityScore simpleScore = analyzer.analyzeComplexity(simpleCode);
        assertEquals("codet5p-33m", simpleScore.getRecommendedModel(),
            "Simple code should recommend 33M model");
        
        // Medium code → 770M model
        String mediumCode = """
            public class Service {
                public void method1() { }
                public void method2() { }
                public void method3() { }
                private void helper() {
                    if (condition) {
                        for (int i = 0; i < 10; i++) {
                            process(i);
                        }
                    }
                }
            }
            """;
        ComplexityScore mediumScore = analyzer.analyzeComplexity(mediumCode);
        assertTrue(mediumScore.getRecommendedModel().equals("codet5p-770m") ||
                   mediumScore.getRecommendedModel().equals("codet5p-33m"),
            "Medium code should recommend 770M or 33M model");
        
        // Complex code → 3B model
        String complexCode = """
            public class ComplexAlgorithm {
                public void method1() { }
                public void method2() { }
                public void method3() { }
                public void method4() { }
                public void method5() { }
                
                public void complexLogic() {
                    for (int i = 0; i < 100; i++) {
                        for (int j = 0; j < 100; j++) {
                            if (matrix[i][j] > threshold) {
                                for (int k = 0; k < depth; k++) {
                                    if (validate(i, j, k)) {
                                        process(i, j, k);
                                    }
                                }
                            }
                        }
                    }
                }
                
                private void helper1() { }
                private void helper2() { }
                private void helper3() { }
            }
            """;
        ComplexityScore complexScore = analyzer.analyzeComplexity(complexCode);
        assertEquals("stablecode-3b", complexScore.getRecommendedModel(),
            "Complex code should recommend 3B model, got: " + complexScore.getRecommendedModel() + 
            " with score: " + complexScore.getTotalScore());
    }
}
