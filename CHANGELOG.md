# Changelog

All notable changes to NeuralForge will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed
- **CRITICAL**: Electron SIGTRAP crash on macOS resolved
  - Downgraded from Electron 28.0.0 → 27.3.11 (stable on macOS)
  - Full integration test successful: Window opens, backend spawns, IPC working
  - Ping/pong verified: `{"type":"pong","timestamp":...,"message":"Backend is alive!"}`
- **Backend logging cleanup**: Added `application.properties` to suppress Spring Boot logs
  - Prevents log output from interfering with JSON IPC on stdout
  - Clean stdin/stdout communication achieved

## [Unreleased]

### Added
- **ROADMAP.md**: Flexible, phase-based development roadmap (local only)
  - High-level vision and goals without strict timelines
  - 6 main phases: Foundation → Model Management → Context → LoRA → Advanced Features → Distribution
  - Phase 1 updated with completed tasks (✅ Electron, Backend, IPC, KISS principles)
  - Future/experimental ideas section for exploration
  - Key constraints and flexibility notes emphasizing adaptability
  - Status: Skeleton complete, ready for model integration (NEXT: model loading)
  - Added to .gitignore (internal planning document)
- **oldDocs/ directory**: Archive location for old documentation
  - Created for CLAUDE_OLD.md and future archived files
  - Excluded from git tracking via .gitignore

### Changed
- **Repository Cleanup (GitHub Structure)**:
  - Removed `CLAUDE.md` from git tracking (kept locally for AI development)
  - Removed `TEST_RESULTS.md` from git tracking (kept locally for testing docs)
  - Removed `.github/copilot-instructions.md` from git tracking (AI instructions local only)
  - Moved `CLAUDE_OLD.md` → `oldDocs/` directory
  - All files remain accessible locally, just not pushed to GitHub
- **Enhanced .gitignore rules**:
  - AI Development Guides: `CLAUDE.md`, `.github/copilot-instructions*.md`
  - Internal Documentation: `ROADMAP.md`, `TEST_RESULTS.md`, `ENVIRONMENT.md`, `NOTES.md`
  - Old/Archived: `oldDocs/` directory
  - Codacy config: `.codacy/` directory
  - Pattern-based exclusions: `DRAFT*.md`, `WIP*.md`, `*_INTERNAL.md`
  - Focus: Keep GitHub clean, only public-facing documentation visible
- **ROADMAP.md Phase 1 Status**: Updated with all completed tasks
  - All skeleton components marked complete (✅)
  - Backend: 258MB memory, ping/pong working
  - IPC: stdin/stdout JSON verified
  - Next tasks: Model loading and basic completion

### Fixed
- **CRITICAL RULES ADDED**: Updated documentation with mandatory workflow rules
  - Rule #0: Always fix issues first (BLOCKING - no progress with unresolved issues)
  - Rule #1: Update CHANGELOG before every commit (MANDATORY)
  - Rule #2: No AI Attribution (never mention Claude/Copilot in commits/code)
  - Rule #3: KISS First - Keep It Simple, Stupid (HIGHEST PRIORITY coding principle)
  - Rule #4: Clean Code - Write for humans first, readability over cleverness
  - Rule #5 (copilot-instructions.md): KISS with concrete examples (over-engineering vs simple)
  - Rule #6 (copilot-instructions.md): Clean Code with readability checklist
  - Rule #7 (copilot-instructions.md): Performance is Feature (latency/memory targets)
  - Added comprehensive KISS and Clean Code sections with examples to both guides
  - Decision frameworks: "Can I delete code?" > "Can I use built-in features?" > "Add code"
  - Anti-patterns documented: premature abstraction, over-engineering, clever one-liners
  - Clean code rules: descriptive names, one function = one job, max 20 lines, no magic numbers
  - Added to `.github/copilot-instructions.md` and `CLAUDE.md` for consistent AI guidance
  - Updated pre-commit checklist with correct order (fix issues → update CHANGELOG → commit)
- **Repository Cleanup**: Internal docs moved to .gitignore
  - Created `oldDocs/` directory for archived documentation
  - Moved `CLAUDE_OLD.md` to `oldDocs/`
  - Updated `.gitignore` with comprehensive internal docs exclusion:
    - AI development guides: `CLAUDE.md`, `.github/copilot-instructions.md`
    - Internal docs: `TEST_RESULTS.md`, `ENVIRONMENT.md`, `ROADMAP.md`, `NOTES.md`, etc.
    - Old/archived files: `oldDocs/` directory
    - Pattern-based exclusions: `DRAFT*.md`, `WIP*.md`, `*_INTERNAL.md`
  - Keeps GitHub clean and focused on public-facing documentation
  - TEST_RESULTS.md unstaged from git (was committed in d204f2f)

### Fixed

### Added
- **Backend**: Spring Boot application with real IPC handler (`dev.neuralforge` package)
  - `Application.java`: Main entry point with memory logging
  - `IPCHandler.java`: Real stdin/stdout JSON communication (NO MOCKS!)
  - Ping/pong and status endpoints working
  - Build successful: 13.2MB JAR, 2s compile time, 254MB RAM usage
- **Editor**: Electron app structure
  - `main.cjs`: Process spawning and IPC management
  - `index.html`: Test UI for backend communication
  - Electron 28.0.0 installed and configured
- **Models**: Directory structure and documentation
  - `models/README.md`: Model loading strategy and memory budgets
  - `models/base/`: Placeholder for ONNX models
- **Environment**: Full development setup documented in `ENVIRONMENT.md`
  - Java 21.0.9, Node.js 22.20.0, npm 10.9.3, Gradle 9.2.0
- **Testing**: `TEST_RESULTS.md` with manual IPC verification

### Removed
- Known issue "Electron SIGTRAP crash" - **RESOLVED** with version downgrade

### Changed
- **Major Overhaul**: Completely rewrote `.github/copilot-instructions.md` for GitHub Copilot optimization
  - Added quick reference tables and visual tech stack diagram
  - Included ✅ Good vs ❌ Bad code pattern examples throughout
  - Added actionable checklists (pre-commit, session workflow, error handling)
  - Created Quick Decision Framework for when to ask vs proceed
  - Added "Pro Tips for Copilot" section with context optimization strategies
  - Included copy-paste ready commands for common tasks (debugging, profiling)
  - Expanded testing examples (Spectron, VS Code Extension, unit tests)
- Completely rewrote CLAUDE.md with improved structure and actionable guidance
- Enhanced decision-making templates and error handling protocols
- Added desktop application emphasis throughout documentation
- Clarified memory budgets and performance targets
- Improved commit message guidelines with examples
- Added common pitfalls section with anti-patterns

### Added
- Quick reference sections for common development tasks
- Session checklists for development workflow
- Project-specific code patterns and examples
- Phase awareness section to prevent out-of-scope work
- Smart workflow diagrams and decision trees

### Fixed
- Removed web/REST API confusion from documentation
- Corrected Docker/Kubernetes references (not needed for desktop app)
- Updated testing strategy to focus on Electron and VS Code Extension tests

### Deprecated
- N/A

### Removed
- N/A

### Fixed
- N/A

### Security
- N/A

---

## [0.1.0] - 2024-10-30

### Added
- Project initialization
- Core documentation files
- Development workflow guidelines
- Basic folder structure

---

## Version History Template

<!--
## [X.Y.Z] - YYYY-MM-DD

### Added
- New features or capabilities

### Changed
- Changes in existing functionality

### Deprecated
- Soon-to-be removed features

### Removed
- Removed features

### Fixed
- Bug fixes

### Security
- Security vulnerability fixes

### Performance
- Performance improvements

### Documentation
- Documentation updates
-->

---

*Note: This file must be updated after every macro development step*
*Maintain clear, descriptive entries for all changes*
*Never reference AI assistance in changelog entries*