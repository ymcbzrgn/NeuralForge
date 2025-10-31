# NeuralForge Development Roadmap

> **Note**: This is a flexible, high-level roadmap. Timelines are approximate and phases may overlap or shift based on discoveries during implementation. The goal is to maintain direction, not strict scheduling.

---

## 🎯 Vision
Build a **100% local desktop AI code editor** (like Cursor/VS Code) with embedded models for code completion and analysis. Memory budget: 3-4GB total, <100ms inference latency.

---

## Phase 1: Foundation ✅ COMPLETE
**Goal**: Minimal working skeleton - Electron app + Java backend + basic IPC

**Core Components**:
- ✅ Electron desktop app with VS Code OSS fork (editor/ directory, 1554 packages)
- ✅ Java Spring Boot backend (embedded, NOT web server, dev.neuralforge package)
- ✅ Real IPC communication (stdin/stdout JSON, NO MOCKS) - IPCHandler.java
- ✅ Basic model directory structure (models/ with README.md)
- ✅ Backend logging cleanup (application.properties for clean IPC)
- ✅ Development workflow rules (Rule #0: Fix First, Rule #1: CHANGELOG)
- ✅ KISS & Clean Code principles documented
- ✅ Repository structure cleaned (internal docs to .gitignore)
- ✅ **ONNX Runtime integration (1.19.2) and model loading infrastructure**
- ✅ **CodeT5+ 220M downloaded and converted to ONNX (encoder 418MB + decoder 621MB)**
- ✅ **ModelLoader service implemented with full lifecycle management (~250 lines)**
- ✅ **Comprehensive test suite (ModelLoaderTest.java) - ALL TESTS PASSED**
- ✅ **Performance validated: 1.46s load time (71% faster than target), 8MB memory (99.6% under budget)**
- ✅ **Tokenization implementation (TokenizerService + nf_tokenize.py) - ALL TESTS PASSED**
- ✅ **T5InferenceEngine with full encoder-decoder pipeline - ALL TESTS PASSED**
- ✅ **Prompt engineering strategies (TASK_PREFIX, LANGUAGE_AWARE, FEW_SHOT) implemented**
- ✅ **Detokenization (nf_detokenize.py) working**
- ✅ **IPC integration for code completion endpoint - COMPLETE**
- ✅ **End-to-end integration test - IPCInferenceIntegrationTest PASSED**

**Success Criteria**:
- ✅ Window opens, backend spawns, IPC working (ping/pong verified)
- ✅ Electron crash fixed (downgraded 28.0.0 → 27.3.11)
- ✅ Memory usage <2GB (current: 258MB backend, 8MB model loader)
- ✅ Load one model successfully (<5s startup) - **ACHIEVED: 1.46s encoder + 0.88s decoder = 2.34s total**
- ✅ Generate one completion (even if low quality) - **ACHIEVED: 119 chars in 16.7s with prompt strategies**

**Final Status**: 
- **Phase 1 COMPLETE** ✅ (October 31, 2025)
- Core pipeline fully operational: IPC → Tokenize → Encode → Decode → Detokenize → Response
- **Total Lines Added**: ~1954 lines across 3 major commits
- **Test Coverage**: All components tested, all tests passed
- **Performance**: Model loading exceeds targets, inference needs Phase 2 optimization
- **Next**: Phase 2 (Smart Model Management & Performance Optimization)

**Phase 1 Achievement Summary**:
```
Sprint 1 (Model Loading):
- ONNX Runtime integration (1.19.2)
- ModelLoader service (~250 lines)
- CodeT5+ 220M ONNX conversion (encoder 418MB + decoder 621MB)
- Performance: 1.46s load time (71% faster than 5s target)
- Memory: 8MB (99.6% under 2GB budget)
- Commit: 6b2a5a0 (250 lines)

Sprint 2 (Inference Pipeline):
- TokenizerService + Python integration (~270 lines)
- T5InferenceEngine + autoregressive generation (~450 lines)
- PromptStrategy enum (5 strategies)
- Python scripts: nf_tokenize.py, nf_detokenize.py
- Tests: TokenizerServiceTest (4/4 passed), T5InferenceEngineTest (3/3 passed)
- Performance: 14.5s avg per 50-token completion
- Commit: 5417889 (1433 lines)

Sprint 3 (IPC Integration):
- IPCHandler inference endpoint (~70 lines)
- IPCInferenceIntegrationTest (~115 lines)
- End-to-end validation: IPC → Inference → Response
- Performance: 16.7s total latency (2.57s init + 4.51s tokenize + 6.3s decode + 3.24s detokenize)
- Completion: 119 chars generated
- Commit: ed567af (271 lines)

Total: 3 commits, ~1954 lines, ALL TESTS PASSED ✅
```

---

## Phase 2: Smart Model Management ✅ COMPLETE
**Goal**: Dynamic model selection based on memory and code complexity

**Core Components**:
- ✅ Model router (complexity analysis → model selection)
- ✅ Memory manager (track usage, prevent OOM)
- ✅ Multi-model support (small/medium/large)
- ✅ Graceful fallback (if memory low, use smaller model)
- ✅ Model caching and unloading (LRU + KV cache)

**Success Criteria**:
- ✅ Switch between 3+ models dynamically
- ✅ Stay within 3GB memory budget
- ✅ <50ms model selection overhead (achieved 0.44ms!)
- ✅ Never crash from OOM (memory-aware routing working)

**Final Status**:
- **Phase 2 COMPLETE** ✅ (October 31, 2025)
- Sprint 1: Python process pooling (99.98% faster tokenization)
- Sprint 2: Smart model selection (86 tests passing, all targets exceeded)
- Performance: Sub-millisecond routing, 145K tokens/sec KV cache throughput
- Code: ~3,756 lines (production + tests)
- Next: Phase 3 - Frontend Integration

---

## Phase 3: Frontend Integration & Context Intelligence

### Phase 3.1: Frontend Integration (CURRENT PHASE)
**Goal**: Connect VS Code fork to backend, show real-time completions to users

**Why First**: Backend is ready, but users can't see completions! Need UI before adding RAG/context features.

**Core Components**:
- Monaco Editor completion provider
- Inline ghost text rendering (gray suggestion text)
- VS Code Extension API integration
- IPC communication (frontend ↔ backend)
- Keyboard shortcuts (Tab = accept, Esc = reject)
- Debouncing and performance optimization
- Error handling and fallback UI

**Success Criteria**:
- User types code → sees gray ghost text suggestion
- Tab key accepts completion
- Esc key rejects suggestion
- <100ms latency (typing → ghost text appears)
- No UI lag or flickering
- Works in Java, Python, TypeScript, JavaScript files
- Graceful degradation if backend unavailable

**Implementation Tasks**:
1. Create CompletionProvider extension in editor/
2. Integrate with Monaco Editor InlineCompletionsProvider API
3. Wire IPC calls to backend (reuse existing IPCHandler)
4. Implement ghost text rendering (CSS + DOM manipulation)
5. Add keyboard event handlers
6. Debounce requests (500ms idle time before request)
7. Add loading states and error handling
8. Test in real coding scenarios

**Files to Create/Modify**:
```
editor/
├── extensions/
│   └── neuralforge-completion/
│       ├── package.json
│       ├── extension.ts (entry point)
│       ├── completionProvider.ts (core logic)
│       └── ghostTextRenderer.ts (UI)
└── src/vs/
    └── (potential Monaco editor modifications)
```

### Phase 3.2: Context & Intelligence (AFTER 3.1)
**Goal**: Use codebase context to improve completion quality

**Core Components**:
- Qdrant vector database (embedded) for code embeddings
- H2 database (embedded) for metadata/relationships
- Context window management (files, symbols, imports)
- Retrieval-Augmented Generation (RAG) for completions
- Language-specific parsers (Java, TypeScript, Python)

**Success Criteria**:
- Completions use relevant code from workspace
- Suggest correct imports and function calls
- Understand project structure (packages, modules)
- <100ms end-to-end latency (context + inference)

**Why After 3.1**: Need working UI first to test context improvements!

---

## Phase 4: LoRA Adapters & Fine-Tuning
**Goal**: Fine-tune models for specific languages/frameworks without full retraining

**Core Components**:
- LoRA adapter loading and composition
- Language-specific adapters (Java, TypeScript, Python)
- Framework adapters (Spring Boot, React, FastAPI)
- Adapter marketplace/discovery (local files first)
- Training pipeline (optional, for power users)

**Pre-Deployment Fine-Tuning**:
- ⚠️ **ACTION REQUIRED (before Phase 6)**: User must provide fine-tuning datasets
- **Goal**: Default models should be excellent at code completion (not just generic)
- **Datasets Needed**:
  - High-quality code completion pairs (context → completion)
  - Multi-language: Java, TypeScript, Python, Go, Rust
  - Real-world patterns: imports, error handling, idioms
  - Repository-level context examples
- **User Task**: Find and provide dataset links (GitHub repos, HuggingFace datasets, etc.)
- **Implementation**: Fine-tune CodeT5+ base models before packaging into installers
- **Quality Target**: Model completions should feel "production-ready" out of the box

**Success Criteria**:
- Load 3-5 adapters simultaneously (<200MB total)
- Completions show language-specific idioms
- Adapter switching <50ms
- Support custom user-trained adapters
- **Pre-deployment**: Base models fine-tuned on quality datasets (user-provided)

---

## Phase 5: Advanced Features
**Goal**: Production-ready editor with polished UX

**Core Components**:
- Inline ghost text (like Copilot)
- Multi-line completions
- Code explanation and documentation generation
- Refactoring suggestions
- Error detection and fixes
- Chat interface (optional)

**Success Criteria**:
- Feels as responsive as native typing
- Completions are helpful >70% of the time
- No UI lag or stuttering
- Keyboard shortcuts work smoothly

---

## Phase 6: Optimization & Distribution
**Goal**: Ship stable, fast, user-ready application

**Core Components**:
- Performance profiling and bottleneck removal
- Memory optimization (aggressive caching, lazy loading)
- Model quantization (INT8/INT4 if needed)
- Installer creation (.exe, .dmg, .AppImage)
- Auto-update mechanism
- Telemetry (opt-in, privacy-first)

**Success Criteria**:
- <3s startup time
- <50ms average inference latency
- <3GB memory footprint
- Smooth installation on Windows/macOS/Linux
- Crash rate <0.1%

---

## Future/Experimental Ideas
**Not committed to roadmap, but worth exploring**:

- **Multi-language support**: Beyond code (docs, configs, SQL)
- **Collaborative coding**: Share context with team (still local-first)
- **Custom model training**: Full fine-tuning pipeline in-app
- **Plugin system**: Extend with custom tools/models
- **Mobile companion**: View/review code on phone (read-only)
- **Git integration**: Commit message generation, PR summaries
- **Code search**: Semantic search across entire workspace
- **Benchmarking**: Compare model performance on standardized tests

---

## Key Constraints (Never Compromise)

1. **100% Local**: No cloud/API calls, no telemetry without consent
2. **Memory Budget**: 3-4GB total (OS + app + models)
3. **Performance**: <100ms inference, <3s startup
4. **Desktop Focus**: Electron app, NOT web/browser
5. **Real IPC**: stdin/stdout JSON, NO HTTP/REST/web server patterns
6. **KISS**: Simple solutions first, complexity only when justified
7. **Clean Code**: Readable, maintainable, <20 lines per function

---

## Flexibility Notes

- **Phases may overlap**: Start Phase 3 context work before Phase 2 is 100% done
- **Priorities shift**: If model loading is slow, optimize that before adding features
- **Discoveries happen**: New libraries, better architectures, unforeseen blockers
- **User feedback**: If real users test early, their input changes priorities
- **Performance first**: If latency exceeds 100ms, stop features and optimize

**Remember**: Roadmaps are guides, not contracts. Adapt as we learn. Fix issues before moving forward (Rule #0).

---

**Last Updated**: 2024-10-31  
**Current Phase**: Phase 2 ✅ COMPLETE → Moving to Phase 3.1 (Frontend Integration)  
**Status**: Backend fully ready (model loading, inference, routing, caching all working). Next: Connect frontend to show completions to users!
