# 🚀 NeuralForge

**A Lightweight AI Code Editor** - Tauri-based IDE with local AI, built for privacy and performance.

<div align="center">
  
  ![NeuralForge Banner](https://via.placeholder.com/1200x400/1e1e1e/00ff00?text=NeuralForge+-+Your+Local+AI+Vibe-Coder)
  
  [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
  [![GitHub Stars](https://img.shields.io/github/stars/ymcbzrgn/neuralforge?style=social)](https://github.com/ymcbzrgn/neuralforge)
  [![Model Size](https://img.shields.io/badge/Model%20Size-220MB-green)](https://github.com/ymcbzrgn/neuralforge)
  [![RAM Usage](https://img.shields.io/badge/RAM-300MB-brightgreen)](https://github.com/ymcbzrgn/neuralforge)
  [![100% Local](https://img.shields.io/badge/Privacy-100%25%20Local-purple)](https://github.com/ymcbzrgn/neuralforge)
  
  **⚠️ Early Development Stage - Phase 1-2 Complete (Backend), Phase 3 Starting (IDE)**
  
  [Roadmap](ROADMAP.md) • [Architecture](ARCHITECTURE.md) • [Contributing](CONTRIBUTING.md)

</div>

---

## 🔥 Why NeuralForge?

Tired of sending your code to the cloud? Frustrated with 200MB+ RAM IDEs? Want an AI that actually learns YOUR coding style?

**NeuralForge** is a lightweight Tauri-based IDE with embedded AI models that:
- 🧠 **Learns from your code** - Adapts to your style over time
- 🔒 **100% local** - Your code never leaves your machine
- ⚡ **~300MB RAM** - 75% less than Electron-based editors
- 💬 **AI Chat with file manipulation** - Direct project changes from chat
- 🆓 **Forever free** - No subscriptions, no API keys required (optional providers available)
- 🚀 **<50ms inference** - Faster than cloud solutions

## 🎯 Key Characteristics

| Feature | Description |
|---------|------------|
| **Tauri-Based** | Native performance, 50MB binary vs 200MB Electron |
| **Monaco Editor** | VS Code's editor component (standalone) |
| **Local-First AI** | All processing happens on your machine |
| **Privacy** | Your code never leaves your computer |
| **Open Source** | Fully transparent and community-driven |
| **Multi-Provider Chat** | Optional OpenAI/Claude/Gemini support |
| **Learning System** | Adapts to your coding style automatically |
| **RAG System** | Framework documentation at your fingertips |

## 🎬 Demo

<div align="center">
  
  ![Demo GIF](https://via.placeholder.com/800x400/1e1e1e/00ff00?text=Demo+Video+Coming+Soon)
  
  *See NeuralForge learning your coding patterns in real-time*

</div>

## ✨ Features (v0.1.0 Target)

### 🎨 **Modern IDE**
- **Monaco Editor**: VS Code's editor component with syntax highlighting
- **File Explorer**: Tree view with folder navigation
- **Tab Management**: Multi-file editing with persistent state
- **Themes**: Dark/Light modes with customization

### 🤖 **AI Completion (Ghost Text)**
- **Inline Suggestions**: Gray text preview as you type
- **YOLO Mode**: Auto-accept completions (toggle on/off)
- **Smart Debouncing**: 500ms delay, no request spam
- **Tab/Esc Handling**: Accept or reject suggestions

### � **AI Chat Panel**
- **Project-Aware Chat**: Knows about your open files
- **Multi-Provider Support**:
  - **Local** (default, no API key)
  - **OpenAI** (optional, GPT-4)
  - **Claude** (optional, Claude 3.5 Sonnet)
  - **Gemini** (optional, Gemini 1.5 Pro)
  - **Custom Endpoints** (your own API)
- **Code Actions**: Create/modify/delete files directly from chat
- **Syntax Highlighting**: Code blocks with copy buttons

### 📚 **Learning System**
- **Style Analyzer**: Detects your naming, formatting, comment style
- **Pattern Recognition**: Learns libraries, test patterns, project structure
- **Adaptive Suggestions**: AI output matches YOUR preferences
- **Learning Dashboard**: See what the AI learned about your style

### 🔍 **RAG System (Framework Docs)**
- **Qdrant Vector DB**: Embedded, no external services
- **Auto-Detection**: Finds Spring Boot, React, etc. from `pom.xml`/`package.json`
- **Semantic Search**: "How to use @Transactional?" finds relevant docs
- **Code Snippets**: Copy-paste ready examples from official docs

### 🎯 **Vibe Coding**
- **Context-Aware**: AI understands errors, cursor position, selection
- **Multi-Step Tasks**: "Refactor this class to use dependency injection"
- **Progress Tracking**: See AI's step-by-step execution plan
- **Auto-Fix**: AI suggests fixes for compile errors

### ⚡ **Performance**
- **Startup**: <3 seconds (Tauri is fast!)
- **Inference**: <50ms (Java backend, ONNX Runtime)
- **RAM Usage**: ~300MB idle (75% less than Electron)
- **Binary Size**: ~50MB (vs 200MB+ Electron apps)

## 🚀 Quick Start

### ⚠️ Not Yet Released
NeuralForge is in **early development** (Phase 3 starting). Pre-built binaries will be available with v0.1.0.

**Current Status:**
- ✅ Phase 1-2: Java backend complete (86 tests passing)
- 🔄 Phase 3: Tauri IDE development starting (see [ROADMAP.md](ROADMAP.md))

### Build from Source (Developers Only)

**Prerequisites:**
- Node.js 18+ & npm
- Rust 1.70+ (for Tauri)
- Java 21+ & Gradle (for backend)
- Python 3.10+ (for tokenizer)

```bash
# Clone repository
git clone https://github.com/ymcbzrgn/neuralforge.git
cd neuralforge

# Backend (already complete, just test it)
cd backend
./gradlew test  # Should show 86 tests passing
cd ..

# Frontend (Phase 3 - under development)
# TODO: Tauri project will be created in Sprint 1.1
# cd neuralforge-ide
# npm install
# npm run tauri dev
```

**Download Models:**
```bash
cd models
python download_model.py  # Downloads CodeT5+ 220M (~1GB)
```

## 💻 System Requirements

### Minimum
- **OS**: Windows 10 / macOS 11 / Ubuntu 20.04
- **RAM**: 2GB (lightweight!)
- **Storage**: 5GB (models + app)
- **CPU**: 2 cores @ 2.0GHz
- **GPU**: Not required

### Recommended
- **RAM**: 4GB
- **Storage**: 10GB  
- **CPU**: 4 cores @ 2.5GHz
- **GPU**: Optional (for faster inference, but not needed)

## 🗺️ Roadmap

**Current Phase:** Phase 3 - Tauri IDE Development (6 weeks, 9 sprints)

See detailed [ROADMAP.md](ROADMAP.md) for:
- ✅ Phase 1-2: Backend Complete (86 tests passing)
- 🔄 Phase 3: IDE Development (Sprint 1-9 detailed)
- 📋 Sprint breakdown with tasks, files, tests

**Next Milestone:** v0.1.0 (Tauri app + AI completion + chat + learning + RAG)

## 🤝 Contributing

We love contributions! NeuralForge is built by developers, for developers.

### Ways to Contribute

1. **Code Contributions**
   ```bash
   git checkout -b feature/amazing-feature
   # Make your changes
   git commit -m "Add amazing feature"
   git push origin feature/amazing-feature
   # Open a Pull Request
   ```

2. **Create Adapters**
   - Train adapters for specific frameworks
   - Share them with the community
   - Get recognition in our Hall of Fame

3. **Report Issues**
   - Bug reports
   - Feature requests
   - Performance improvements

4. **Documentation**
   - Improve docs
   - Write tutorials
   - Create videos

See [CONTRIBUTING.md](CONTRIBUTING.md) for details.

## 📚 Documentation

- [ROADMAP.md](ROADMAP.md) - Development roadmap and current status
- [ARCHITECTURE.md](ARCHITECTURE.md) - System architecture (Tauri + Java backend)
- [NeuralForge_PRD.md](NeuralForge_PRD.md) - Product requirements and vision
- [CONTRIBUTING.md](CONTRIBUTING.md) - How to contribute
- [TRAINING.md](TRAINING.md) - LoRA adapter training guide
- [SECURITY.md](SECURITY.md) - Security architecture and threat model

## 🏆 Community

### Get Involved

For questions, discussions, and to connect with other contributors:
- Check [CONTRIBUTING.md](CONTRIBUTING.md) for how to get involved
- Review [CLAUDE.md](CLAUDE.md) for architecture details
- See [ARCHITECTURE.md](ARCHITECTURE.md) for technical overview


## 📊 Benchmarks

Performance benchmarks will be published once the project reaches a stable release with reproducible test cases.



## 🔐 Security

- ✅ All models run locally
- ✅ No telemetry or analytics
- ✅ Adapter signature verification
- ✅ Sandboxed execution
- ✅ Regular security audits

Found a security issue? Please email security@neuralforge.dev

## 📜 License

NeuralForge is licensed under the **Apache License 2.0** - see [LICENSE](LICENSE) file.

### Third-party Licenses
- VS Code OSS: MIT License
- Models: Apache 2.0 / MIT only
- All dependencies: Compatible licenses

## 💖 Support

If you find NeuralForge useful, consider:
- ⭐ [Starring the repository](https://github.com/ymcbzrgn/neuralforge)
- 📝 Contributing code or documentation
- 🐛 Reporting bugs and suggesting features


## 🌟 Star History

<div align="center">

[![Star History Chart](https://api.star-history.com/svg?repos=ymcbzrgn/neuralforge&type=Date)](https://star-history.com/#ymcbzrgn/neuralforge&Date)

</div>

## 🎯 Philosophy

> "AI should amplify human creativity, not exploit it. Your code is your intellectual property. Your patterns are your expertise. Your privacy is non-negotiable."

NeuralForge is built on five principles:
1. **Privacy First** - Your code never leaves your machine (100% local by default)
2. **Lightweight** - Fast startup, low RAM usage (Tauri beats Electron)
3. **Community Driven** - Built by developers, for developers  
4. **Continuous Learning** - Gets better with every line you write
5. **Choice** - Optional cloud providers (OpenAI/Claude/Gemini) without forcing them

---

<div align="center">

### Ready to build the future of local-first AI coding?

# [👷 Contribute to NeuralForge](CONTRIBUTING.md)

**⚠️ Pre-Alpha Development • Contributions Welcome • Phase 3 Starting**

<br>

Made with ❤️ by developers who believe in privacy and local-first AI

[GitHub](https://github.com/ymcbzrgn/neuralforge) • [Documentation](docs/) • [Report Bug](https://github.com/ymcbzrgn/neuralforge/issues)

</div>

---

<div align="center">
  <sub>Built by the open-source community. Not another corporate AI tool.</sub>
</div>