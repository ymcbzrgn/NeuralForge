# NeuralForge Models Directory

This directory contains AI models used by NeuralForge for code completion and analysis.

## Structure

```
models/
├── base/               # Pre-trained base models (ONNX format)
│   ├── codet5-770m.onnx      # CodeT5+ 770M (medium complexity)
│   ├── stablecode-3b.onnx    # StableCode 3B (high complexity)
│   └── tinystories-33m.onnx  # TinyStories 33M (low memory mode)
├── adapters/           # LoRA adapters for specialization
│   ├── java/          # Java-specific adapters
│   ├── python/        # Python-specific adapters
│   └── typescript/    # TypeScript-specific adapters
└── cache/             # Model inference cache
```

## Model Status (Phase 1)

### ✅ Planned
- **TinyStories 33M**: Initial testing model (~66MB)
- **CodeT5+ 770M**: Primary code completion model (~1.5GB)
- **StableCode 3B**: Advanced completions for complex contexts (~6GB)

### 📋 Not Yet Downloaded
Models are not included in git repository due to size.
Download instructions will be provided in setup documentation.

## Memory Budget

| Model | Size | RAM Usage | Use Case |
|-------|------|-----------|----------|
| TinyStories 33M | 66MB | ~200MB | Testing, low-memory fallback |
| CodeT5+ 770M | 1.5GB | ~2GB | Primary completions |
| StableCode 3B | 6GB | ~8GB | Complex analysis (future) |

## Loading Strategy

Models are loaded on-demand based on:
1. Available system memory
2. Context complexity
3. User preferences
4. Current file language

Only ONE base model is loaded at a time to stay within 3-4GB RAM budget.

## Next Steps

1. Add model download script
2. Implement ONNX model loader
3. Add model router for selection logic
4. Integrate with inference engine
