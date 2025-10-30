# CLAUDE.md - Smart AI Development Guide

**⚡ Quick Ref**: Desktop app (Electron/VS Code fork) | 3GB RAM budget | <100ms inference | No web/cloud

---

## 🚨 RULE #1: NO AI ATTRIBUTION (Non-Negotiable)

Never mention "Claude", "AI", "automated", "generated" in:
- ❌ Git commits
- ❌ Code comments  
- ❌ Documentation
- ❌ Pull requests
- ❌ Any project file

Write as if YOU (human developer) wrote it.

**Forbidden**: "Claude adds X", "AI-generated", "automated by AI"
**Correct**: "Add X", "Implement Y", "Fix Z"

---

## 📋 PROJECT CONTEXT - 60 Second Briefing

### What is NeuralForge?
**Desktop AI code editor** - Like Cursor/VS Code, but 100% local, privacy-first, runs on 3GB RAM.

**Status**: Early planning phase. Architecture documented, implementation starting.

### Tech Stack (DESKTOP APP!)
```
Electron Application
├── Frontend: TypeScript + VS Code OSS + Monaco
├── Backend: Java 21 + Spring Boot (embedded JVM, not web!)
├── AI: ONNX models + LoRA adapters (1-3GB)
├── Storage: Qdrant + H2 (both embedded)
├── IPC: JSON-RPC over named pipes (not HTTP!)
└── Package: electron-builder → .exe/.dmg/.AppImage
```

### Critical Constraints (Never Violate)
| Constraint | Limit | Why |
|------------|-------|-----|
| Total RAM | 3-4GB | Target modest hardware |
| Inference | <100ms | User experience requirement |
| Startup | <5s | Competitive with VS Code |
| Model size | <3GB | Fit on 8GB RAM systems |
| Privacy | 100% local | Core value proposition |

### ⚠️ What This Is NOT
- ❌ Web application (no express/nginx/apache)
- ❌ Cloud service (no Docker/Kubernetes/deployment)
- ❌ REST API server (no @RestController/@GetMapping)
- ❌ Microservice (no containers/orchestration)

✅ It's a **desktop application** like VS Code, Slack, or Spotify.

### Must-Read Before Coding
1. `ARCHITECTURE.md` - System design & components
2. `CHANGELOG.md` - Current state & recent changes
3. `.github/copilot-instructions.md` - Quick dev guide
4. `SECURITY.md` - Sandboxing & threat model

---

## 🧠 SMART WORKFLOW - Think Before Code

### Pre-Task Checklist (30 seconds)
```bash
□ 1. Understand
   "What problem am I solving? What's success criteria?"
   
□ 2. Context
   git status && git log --oneline -5
   
□ 3. Research
   grep -r "keyword" *.md  # Check existing patterns
   
□ 4. Phase Check
   "Is this feature in current phase?" (See PRD)
   
□ 5. Plan
   "Which files? What sequence? Any dependencies?"
```

### Smart Implementation Pattern
```
1. Read related code first (understand existing patterns)
2. Break complex tasks into steps
3. Implement smallest testable unit
4. Test immediately
5. Update CHANGELOG
6. Commit atomically
7. Move to next step

DON'T: Write everything then test
DO: Test-as-you-go for faster feedback
```

### When to STOP and ASK 🚨

**Red Flags - Ask Before Proceeding:**

| Category | Examples |
|----------|----------|
| **Architecture** | New framework/library, database changes, major refactor (>5 files) |
| **Breaking Changes** | API modifications, file structure changes, schema updates |
| **Security** | Encryption, authentication, sandboxing, input validation |
| **Performance** | Memory vs speed tradeoffs, latency targets at risk |
| **Dependencies** | Adding npm/maven packages (license check!) |
| **Unclear Spec** | Ambiguous requirements, missing criteria, conflicting needs |

**Decision Template** (Use this format):
```markdown
## 🤔 DECISION: [Topic in 3-5 words]

**Context**: [1-2 sentence situation]

**Question**: [Specific decision needed]

**Options**:
A) [Approach]: 
   ✅ Pros: [key benefits]
   ❌ Cons: [key drawbacks]  
   📊 Impact: [performance/memory/complexity]

B) [Approach]:
   ✅ Pros: [key benefits]
   ❌ Cons: [key drawbacks]
   📊 Impact: [performance/memory/complexity]

**Recommendation**: [Your call with reasoning] OR "Need guidance"

⏸️ BLOCKED - Waiting for decision
```

---

## 🐛 ERROR HANDLING - Stop, Think, Fix

### When Error Occurs (Protocol)
```
1. 🛑 STOP immediately
   - Don't continue
   - Don't commit broken code
   - Don't workaround without fixing

2. 🔍 Analyze
   - What type? (compilation/runtime/logic/test)
   - Where? (file:line)
   - Why? (root cause, not symptom)

3. 🔧 Fix completely
   - Address root cause
   - Not just bandaid solution

4. ✅ Verify
   - Run tests
   - Check related code
   - Document if architectural

5. ➡️ Continue only after verified fix
```

**Error Report Template**:
```markdown
🛑 ERROR: [Type] at [Location]

**What**: [Brief description]
**Why**: [Root cause analysis]
**Fix**: [Solution approach]
**Test**: [How to verify]

[Then fix before continuing]
```

---

## 📝 CHANGELOG - Keep History Clean

### Update After Every:
✅ Feature implemented
✅ Bug fixed  
✅ Refactoring done
✅ Docs updated
✅ Tests added
✅ Performance improved

### Format (Keep a Changelog standard):
```markdown
## [Unreleased]

### Added
- Ghost text controller with 300ms debounce
- Model router with fallback logic

### Changed
- Refactored adapter composition for better performance

### Fixed
- Memory leak in KV cache cleanup
- Race condition in model loading

### Performance
- Reduced inference time from 80ms to 45ms
```

**Pro Tip**: Update CHANGELOG before committing. It helps write better commit messages!

---

## 🔀 GIT WORKFLOW - Keep It Professional

### Commit Format (Conventional Commits)
```bash
<type>: <description>

Types:
feat     - New feature
fix      - Bug fix
perf     - Performance improvement
refactor - Code restructuring (no functionality change)
docs     - Documentation only
test     - Adding/fixing tests
chore    - Dependencies, build scripts, tooling
style    - Code formatting (not CSS!)
```

### Commit Checklist (Before git commit)
```bash
□ git diff                    # Review every change
□ npm test && ./gradlew test  # All tests pass
□ npm run lint                # No lint errors
□ Build succeeds              # No compilation errors
□ Manual test                 # Feature actually works
□ CHANGELOG updated           # Documented the change
□ Files staged selectively    # Not "git add ."
□ Message clear & specific    # Not "update" or "fix"
□ No AI mentions             # No "Claude", "AI", etc.
□ Atomic (one logical change) # Not mixing features
```

### Good vs Bad Commits

**✅ GOOD Examples**:
```bash
feat: add model router with memory-based fallback
fix: resolve race condition in adapter loading
perf: optimize KV cache with LRU eviction policy
refactor: extract inference engine to separate class
docs: add architecture diagram for model serving
test: add unit tests for model selection logic
```

**❌ BAD Examples (Never do this)**:
```bash
❌ "Claude adds feature"       # AI attribution
❌ "WIP"                        # Don't commit unfinished
❌ "Update stuff"               # Too vague
❌ "Fix"                        # No context
❌ "Changes"                    # What changes?
❌ "AI-generated code"          # AI attribution
❌ Mixing 3 features in 1 commit # Not atomic
```

### Git Best Practices
```bash
# DO:
✅ git add src/specific/file.ts      # Stage selectively
✅ git commit -m "feat: add X"       # Clear message
✅ Keep commits <150 lines           # Easier to review

# DON'T:
❌ git add .                          # Too broad
❌ git commit -m "updates"            # Too vague  
❌ git commit --no-verify             # Skip hooks
❌ git push --force (without permission)
```

---

## 💻 CODE QUALITY - Desktop App Standards

### Language-Specific Guidelines

**Java (Backend - Embedded JVM)**:
```java
// ✅ Good: Desktop app pattern
@Component
public class ModelLoader {
    // Clear naming, <30 lines per method
    // Javadoc for public APIs
    // Spring @Async for background tasks
    // Project Reactor for reactive streams
}

// ❌ Bad: Web server pattern (DON'T USE)
@RestController              // We're not a web server!
@GetMapping("/api/models")   // No HTTP endpoints!
```

**TypeScript (Frontend - Electron)**:
```typescript
// ✅ Good: Electron/VS Code APIs
import * as vscode from 'vscode';
import { ipcRenderer } from 'electron';

// Use Node.js modules (we have access!)
import * as fs from 'fs';
import * as path from 'path';

// ❌ Bad: Browser-only APIs (limited access)
fetch()        // Use node-fetch or axios instead
localStorage   // Use electron-store instead
```

**Testing (Desktop App Testing)**:
```typescript
// ✅ Good: Desktop app tests
import { Application } from 'spectron';  // Electron testing
import * as vscode from 'vscode';        // VS Code testing

// ❌ Bad: Web testing (DON'T USE)
import { Page } from 'playwright';       // Browser automation
```

### Performance Guidelines

| Metric | Target | Max | If Exceeded |
|--------|--------|-----|-------------|
| Method size | <20 lines | 30 lines | Extract helper methods |
| RAM per component | See budget | +10% | Optimize or ask |
| Inference latency | <50ms | 100ms | Profile & optimize |
| Startup time | <3s | 5s | Lazy load non-critical |
| Model file size | <2GB | 3GB | Quantize further |

### Security Checklist
```bash
□ All inputs sanitized (code snippets, file paths)
□ Model runs in sandbox (no arbitrary file access)
□ Adapter signatures verified before loading
□ No hardcoded secrets or credentials
□ Sensitive operations logged
□ Error messages don't leak sensitive info
```

---

## 🎯 PROJECT-SPECIFIC PATTERNS

### Model Management Pattern
```java
// Always check availability and fallback
Model model = modelRouter.route(context);

if (!model.isLoaded()) {
    // Fallback to smaller, always-available model
    model = fallbackModel;
    logger.warn("Using fallback model due to memory constraints");
}

// Add timeout to prevent hanging
CompletableFuture<Result> future = model.inferAsync(input);
Result result = future.get(100, TimeUnit.MILLISECONDS);
```

### LoRA Adapter Composition
```java
// Composable adapters with limits
AdapterStack stack = new AdapterStack();
stack.add(baseAdapter);           // Always present
stack.add(languageAdapter);       // Language-specific (Java/Python)
stack.add(frameworkAdapter);      // Framework-specific (Spring/React)
stack.add(projectAdapter);        // Project-specific patterns
stack.add(personalAdapter);       // User's coding style

// Enforce limits
if (stack.size() > 5) {
    throw new IllegalStateException("Max 5 adapters allowed");
}
if (stack.totalSize() > 200_000_000) {  // 200MB
    throw new IllegalStateException("Total adapter size >200MB");
}
```

### Memory Budget Enforcement
```java
// Always check before allocating
MemoryManager mem = MemoryManager.getInstance();

if (!mem.canAllocate(modelSize)) {
    // Free up memory
    mem.unloadLeastRecentlyUsedModel();
    
    if (!mem.canAllocate(modelSize)) {
        // Still not enough - use smaller model
        return selectSmallerModel();
    }
}

Model model = loadModel(modelPath);
mem.register(model, modelSize);
```

### Desktop IPC Pattern (Not HTTP!)
```typescript
// ✅ Good: IPC communication (fast, local)
import { ipcRenderer } from 'electron';

const result = await ipcRenderer.invoke('model:infer', {
    context: code,
    language: 'java'
});

// ❌ Bad: HTTP communication (we're not a web app!)
const response = await fetch('http://localhost:8080/api/infer', {
    method: 'POST',
    body: JSON.stringify(data)
});
```

---

## 📚 QUICK REFERENCE - Common Tasks

### Finding Information
```bash
# Find where something is used
grep -r "ModelRouter" --include="*.java"

# Check recent changes
git log --oneline -10
git log --grep="model" --oneline

# See what changed in a file
git log -p ARCHITECTURE.md

# Find similar patterns
find . -name "*Model*.java" -type f
```

### Before Committing
```bash
# Full check sequence
npm test                    # Frontend tests
./gradlew test              # Backend tests
npm run lint:fix            # Auto-fix style
./gradlew checkstyle        # Java style
git diff                    # Review changes
git status                  # Check staged files

# If all pass:
git add <specific-files>
git commit -m "type: description"
```

### When Stuck
```bash
# 1. Read existing code for patterns
grep -r "similar_feature" .

# 2. Check documentation
cat ARCHITECTURE.md | grep -A 10 "relevant topic"

# 3. Review git history for context
git log --grep="feature" --oneline

# 4. Ask with context (use decision template above)
```

---

## 🚀 CURRENT PHASE AWARENESS

### Phase 1: Foundation (Current)
**Focus**: Basic infrastructure, architecture validation

**In Scope**:
- VS Code fork setup
- Basic completion engine
- Single model integration (CodeT5+ 770M)
- Simple UI (ghost text)
- Local model loading
- Basic testing infrastructure

**Out of Scope** (Don't implement yet):
- Multi-model routing (Phase 2)
- Incremental learning (Phase 2)
- Adapter marketplace (Phase 3)
- Swarm intelligence (Phase 3)
- Federated learning (Phase 3)

**How to Check**: See `NeuralForge_PRD.md` Section 15 for phase details.

---

## ⚠️ COMMON PITFALLS - Learn From These

### 1. "It's a web app" Mistake
```java
// ❌ WRONG - Web server thinking
@RestController
public class ModelAPI {
    @GetMapping("/models")
    public List<Model> getModels() { }
}

// ✅ RIGHT - Desktop app thinking
@Component
public class ModelManager {
    public List<Model> getAvailableModels() { }
}
```

### 2. "Browser APIs work" Mistake
```typescript
// ❌ WRONG - Browser-only APIs
localStorage.setItem('key', 'value');
fetch('http://api.example.com');

// ✅ RIGHT - Electron/Node.js APIs
import Store from 'electron-store';
const store = new Store();
store.set('key', 'value');

import axios from 'axios';  // or node-fetch
```

### 3. "Unlimited memory" Mistake
```java
// ❌ WRONG - No memory management
List<Model> models = new ArrayList<>();
models.add(loadModel("huge-model-15GB.onnx"));  // OOM!

// ✅ RIGHT - Check budget first
if (memoryManager.available() < modelSize) {
    return loadSmallerModel();
}
```

### 4. "I'll optimize later" Mistake
```java
// ❌ WRONG - Ignoring performance from start
public Result infer(String input) {
    // Takes 2 seconds - way over 100ms target
}

// ✅ RIGHT - Profile and optimize early
public Result infer(String input) {
    long start = System.nanoTime();
    Result result = doInference(input);
    long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    
    if (duration > 100) {
        logger.warn("Inference took {}ms (target: <100ms)", duration);
    }
    return result;
}
```

### 5. "Tests can wait" Mistake
```bash
# ❌ WRONG - Write lots of code, then test
# (Makes debugging harder, wastes time)

# ✅ RIGHT - Test as you go
1. Write small piece
2. Test immediately
3. Fix if broken
4. Move to next piece
```

---

## 📖 ATTRIBUTION EXAMPLES

### When Documenting/Commenting

**✅ GOOD (No AI mention)**:
```java
/**
 * Implements model routing based on context complexity.
 * Uses heuristics to balance accuracy and latency.
 */
public Model selectModel(Context ctx) {
    // Memory-efficient approach: check RAM before loading
    if (complexity > 0.8 && memoryAvailable > 2GB) {
        return largeModel;
    }
    return compactModel;
}
```

**❌ BAD (AI attribution - DON'T DO THIS)**:
```java
// Claude suggested this approach
// AI-generated model routing logic
// Automated selection algorithm
```

### In Pull Requests

**✅ GOOD**:
```markdown
## Added model router with fallback logic

Implemented routing algorithm that selects appropriate model based on:
- Context complexity score
- Available memory
- Latency requirements

Falls back to smaller model if memory constrained.
Tested with 1000 sample completions, avg latency 45ms.
```

**❌ BAD**:
```markdown
## Claude implements model router

AI assistant added routing logic as suggested.
```

---

## ✅ SESSION CHECKLIST

### At Start of Every Session
```bash
□ Read this file (skim CRITICAL sections)
□ git pull origin main
□ git log --oneline -10  # See recent changes
□ tail -20 CHANGELOG.md  # Current state
□ Understand today's task clearly
```

### During Development
```bash
□ Test frequently (not "test at end")
□ Update CHANGELOG as you go
□ Commit atomically (not "big bang commit")
□ No AI attribution anywhere
□ Stay within memory/performance budgets
```

### Before Ending Session
```bash
□ All tests pass
□ No uncommitted changes (or document WIP)
□ CHANGELOG reflects what was done
□ Update task status if tracking
```

---

## 🎓 REMEMBER

1. **This is a DESKTOP APPLICATION** - Not web, not cloud, not microservice
2. **Memory matters** - 3-4GB total budget, measure everything
3. **Test as you go** - Don't write 500 lines then test
4. **Ask when uncertain** - Use decision template, get clarity
5. **No AI attribution** - Write as human developer
6. **Keep commits atomic** - One logical change per commit
7. **Document as you code** - Update CHANGELOG immediately
8. **Performance is feature** - <100ms inference is requirement, not nice-to-have

---

## 📞 NEED HELP?

**Before asking**, check:
1. This file (CLAUDE.md)
2. ARCHITECTURE.md (system design)
3. .github/copilot-instructions.md (quick guide)
4. Git history (`git log` for similar changes)

**When asking**, provide:
- What you're trying to do
- What you've tried
- Specific question
- Relevant code/files

**Use decision template** for architectural questions.

---

*Last Updated: 2024-10-30*
*This guide must be reviewed at start of every development session*
*Remember: You're a skilled developer, not an AI. Your code speaks for itself.*
