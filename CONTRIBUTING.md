# Contributing to NeuralForge

First off, **thank you** for considering contributing to NeuralForge! It's people like you that make NeuralForge such a great tool for developers worldwide. 🚀

## Table of Contents
- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Contribution Guidelines](#contribution-guidelines)
- [Commit Message Rules](#commit-message-rules)
- [Pull Request Process](#pull-request-process)
- [Code Standards](#code-standards)
- [Testing Requirements](#testing-requirements)
- [Documentation](#documentation)
- [Community](#community)

---

## Code of Conduct

This project and everyone participating in it is governed by our Code of Conduct. By participating, you are expected to uphold this code. Please report unacceptable behavior to conduct@neuralforge.dev.

### Our Standards
- **Respectful** communication
- **Constructive** feedback
- **Inclusive** environment
- **Professional** interactions
- **Privacy-focused** development

---

## Getting Started

### Prerequisites
- Java 21+ (GraalVM recommended)
- Node.js 18+
- Git
- 16GB RAM for building (8GB minimum)
- ~20GB free disk space

### Quick Contribution Checklist
- [ ] Fork the repository
- [ ] Create a feature branch
- [ ] Write clean, tested code
- [ ] Follow commit message rules
- [ ] Update documentation
- [ ] Submit a pull request

---

## Development Setup

### 1. Fork and Clone
```bash
# Fork via GitHub UI, then:
git clone https://github.com/YOUR_USERNAME/neuralforge.git
cd neuralforge
git remote add upstream https://github.com/neuralforge/neuralforge.git
```

### 2. Install Dependencies
```bash
# Frontend dependencies
npm install

# Backend dependencies
./gradlew build

# Download development models (small versions)
npm run download-models:dev
```

### 3. Development Mode
```bash
# Start backend in dev mode
./gradlew bootRun

# Start frontend with hot reload
npm run dev

# Run in integrated mode
npm run start:dev
```

### 4. Verify Setup
```bash
# Run all tests
npm test
./gradlew test

# Lint check
npm run lint
./gradlew checkstyle
```

---

## Contribution Guidelines

### What We're Looking For

#### 🎯 High Priority
- Performance improvements (reduce RAM/CPU usage)
- Model optimization (quantization, caching)
- New language support
- Bug fixes
- Security enhancements

#### 💡 Great Ideas
- New adapters for frameworks
- UI/UX improvements
- Documentation improvements
- Test coverage increase
- Accessibility features

#### 🚫 What We Don't Accept
- Features requiring cloud services
- Proprietary code or models
- Changes that increase RAM usage significantly
- Code without tests
- Breaking changes without discussion

---

## Commit Message Rules

### ⚠️ CRITICAL RULES - MUST READ

```
🚨 ABSOLUTE RULES - NEVER BREAK THESE:

1. NEVER use the word "Claude" in ANY commit message
2. NEVER use the word "Claude" in code comments
3. NEVER reference AI assistance in commits
4. NEVER identify automated contributions
5. ALWAYS write commits as if YOU wrote the code
```

### Commit Message Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

#### Types
- **feat**: New feature
- **fix**: Bug fix
- **perf**: Performance improvement
- **refactor**: Code restructuring
- **docs**: Documentation only
- **style**: Formatting, missing semicolons, etc
- **test**: Adding missing tests
- **chore**: Maintenance tasks
- **security**: Security improvements

#### Examples of GOOD Commit Messages
```bash
✅ feat(completion): add multi-model routing logic
✅ fix(memory): resolve leak in adapter loading
✅ perf(inference): optimize KV cache usage
✅ docs(api): update REST endpoint documentation
✅ test(router): add unit tests for model selection
```

#### Examples of BAD Commit Messages
```bash
❌ Claude implements feature        # NEVER mention AI
❌ AI suggested optimization        # NEVER mention AI
❌ update stuff                     # Too vague
❌ fix                              # No context
❌ WIP                              # Don't commit WIP
```

### Commit Best Practices
1. **Atomic commits**: One logical change per commit
2. **Present tense**: "add feature" not "added feature"
3. **Imperative mood**: "move cursor" not "moves cursor"
4. **No period**: No period at end of subject line
5. **Body explains why**: Not what (that's in the diff)

---

## Pull Request Process

### Before Submitting

1. **Update from upstream**
```bash
git fetch upstream
git rebase upstream/main
```

2. **Run full test suite**
```bash
npm run test:full
./gradlew test
```

3. **Check code quality**
```bash
npm run lint:fix
./gradlew spotlessApply
```

4. **Update documentation**
- Update README if needed
- Add/update API docs
- Update CHANGELOG.md

### PR Template
```markdown
## Description
Brief description of what this PR does

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Performance improvement
- [ ] Documentation update

## Testing
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Manual testing completed

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Comments added for complex code
- [ ] Documentation updated
- [ ] No warnings generated
- [ ] Dependent changes merged

## Performance Impact
- RAM usage: [no change | +X MB | -X MB]
- CPU usage: [no change | +X% | -X%]
- Inference speed: [no change | +Xms | -Xms]

## Screenshots (if applicable)
```

### Review Process
1. Automated checks must pass
2. Code review by 1+ maintainers
3. Performance benchmarks verified
4. Documentation reviewed
5. Merged via squash or rebase (no merge commits)

---

## Code Standards

### Java Standards

```java
// File header (required)
/*
 * Copyright 2024 NeuralForge Contributors
 * Licensed under Apache License 2.0
 */

package dev.neuralforge.service;

// Imports (organized)
import java.util.*;  // Never use wildcard
import org.springframework.*;

/**
 * Class documentation required
 * @author YourName (optional)
 * @since 1.0.0
 */
public class ExampleService {
    // Constants first
    private static final int MAX_TOKENS = 2048;
    
    // Fields with Javadoc
    /** Model inference engine */
    private final InferenceEngine engine;
    
    // Constructor with validation
    public ExampleService(InferenceEngine engine) {
        this.engine = Objects.requireNonNull(engine);
    }
    
    // Methods under 30 lines
    public Completion complete(String prompt) {
        // Comments for complex logic
        validatePrompt(prompt);
        
        // Clear variable names
        TokenizedInput tokens = tokenize(prompt);
        InferenceResult result = engine.infer(tokens);
        
        return mapToCompletion(result);
    }
}
```

### TypeScript Standards

```typescript
/**
 * File header with copyright
 */

// Organized imports
import { Component } from '@core';
import type { Config } from '@types';

// Interfaces over types when possible
interface ServiceConfig {
  readonly endpoint: string;
  timeout?: number;
}

// Comprehensive JSDoc
/**
 * Manages AI completion requests
 * @class CompletionService
 */
export class CompletionService {
  // Explicit types, no 'any'
  private readonly config: ServiceConfig;
  
  constructor(config: ServiceConfig) {
    this.config = Object.freeze(config);
  }
  
  // Async/await over callbacks
  async requestCompletion(
    context: CodeContext
  ): Promise<Completion> {
    // Guard clauses early
    if (!context.isValid()) {
      throw new Error('Invalid context');
    }
    
    // Descriptive variable names
    const sanitizedInput = this.sanitize(context);
    const response = await this.send(sanitizedInput);
    
    return this.parse(response);
  }
}
```

### Resource Efficiency Rules

```java
// ✅ GOOD: Efficient resource usage
public class EfficientService {
    private final int CACHE_SIZE = 1000;  // Limited cache
    private final Cache<String, Result> cache = 
        Caffeine.newBuilder()
            .maximumSize(CACHE_SIZE)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();
}

// ❌ BAD: Wasteful resource usage
public class WastefulService {
    private final Map<String, Result> cache = 
        new HashMap<>();  // Unbounded growth!
}
```

---

## Testing Requirements

### Test Coverage Targets
- **Unit Tests**: 80% minimum
- **Integration Tests**: Core flows covered
- **Performance Tests**: No regression allowed
- **Security Tests**: All inputs validated

### Writing Tests

#### Java Test Example
```java
@Test
@DisplayName("Should route to StableCode for long context")
void testLongContextRouting() {
    // Given
    CodeContext context = CodeContext.builder()
        .fileLength(5000)
        .contextWindow(16000)
        .build();
    
    // When
    Model selected = router.route(context);
    
    // Then
    assertThat(selected.getName())
        .isEqualTo("stablecode-3b");
}
```

#### TypeScript Test Example
```typescript
describe('CompletionService', () => {
  it('should handle network timeouts gracefully', async () => {
    // Arrange
    const service = new CompletionService({
      endpoint: 'http://localhost:8080',
      timeout: 100
    });
    
    // Act & Assert
    await expect(service.requestCompletion(slowContext))
      .rejects
      .toThrow('Request timeout');
  });
});
```

### Performance Testing
```bash
# Run benchmarks before and after changes
npm run benchmark
./gradlew jmh

# Memory profiling
npm run profile:memory
./gradlew profiling
```

---

## Documentation

### Code Documentation
- All public APIs must have JSDoc/Javadoc
- Complex algorithms need explanatory comments
- Include examples in documentation
- Keep README up-to-date

### Documentation Structure
```
docs/
├── api/           # API reference
├── guides/        # How-to guides
├── architecture/  # System design
└── adapters/      # Adapter creation
```

### Writing Style
- Clear and concise
- Include code examples
- Explain the "why" not just "what"
- Keep beginner-friendly

---

## Creating Adapters

### Adapter Contribution Process

1. **Create adapter**
```bash
npm run create-adapter -- --name my-framework
```

2. **Train on quality data**
```python
# Use only Apache/MIT licensed code
dataset = load_dataset("apache-licensed-only")
train_adapter(dataset, epochs=3)
```

3. **Validate performance**
```bash
npm run validate-adapter -- --path ./my-adapter.lora
```

4. **Submit to marketplace**
```bash
npm run submit-adapter -- --public
```

### Adapter Quality Standards
- Must improve base model by >5%
- Size must be <100MB
- Training data must be licensed appropriately
- Must include documentation
- Must pass security scan

---

## Community

### Getting Help
- **Discord**: [discord.gg/neuralforge](https://discord.gg/neuralforge)
- **Discussions**: [GitHub Discussions](https://github.com/neuralforge/neuralforge/discussions)
- **Stack Overflow**: Tag `neuralforge`

### Recognition
- Contributors get credited in CONTRIBUTORS.md
- Significant contributors get commit access
- Adapter creators featured in marketplace
- Top contributors highlighted in releases

### Development Philosophy
1. **Privacy First**: No telemetry, ever
2. **Performance Obsessed**: Every MB matters
3. **Community Driven**: Users shape the product
4. **Transparent**: Open development process
5. **Inclusive**: Welcome all skill levels

---

## License

By contributing, you agree that your contributions will be licensed under Apache License 2.0.

---

## Questions?

Feel free to:
- Open an issue for bugs
- Start a discussion for features
- Join Discord for real-time chat
- Email maintainers for security issues

**Thank you for contributing to making AI-assisted coding accessible to everyone!** 🎉

---

*Remember: Every contribution, no matter how small, makes NeuralForge better for thousands of developers.*