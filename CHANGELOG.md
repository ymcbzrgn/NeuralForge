# Changelog

All notable changes to NeuralForge will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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

### Fixed
- Spring Boot 3.x Banner.Mode import corrected

### Known Issues
- Electron crashes with SIGTRAP on macOS (backend IPC proven working standalone)

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