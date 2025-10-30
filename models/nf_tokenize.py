#!/usr/bin/env python3
"""
Simple tokenizer script for NeuralForge backend.
Reads text from stdin, outputs token IDs as JSON to stdout.

Usage:
    echo "def hello():" | python tokenize.py models/base/codet5p-220m
    Output: {"token_ids": [123, 456, 789], "length": 3}
"""

import sys
import json
from pathlib import Path
from transformers import AutoTokenizer


def main():
    if len(sys.argv) != 2:
        print(json.dumps({"error": "Usage: tokenize.py <model_path>"}), file=sys.stderr)
        sys.exit(1)
    
    model_path = Path(sys.argv[1])
    if not model_path.exists():
        print(json.dumps({"error": f"Model path not found: {model_path}"}), file=sys.stderr)
        sys.exit(1)
    
    # Load tokenizer (cached after first load)
    try:
        tokenizer = AutoTokenizer.from_pretrained(str(model_path))
    except Exception as e:
        print(json.dumps({"error": f"Failed to load tokenizer: {str(e)}"}), file=sys.stderr)
        sys.exit(1)
    
    # Read input text from stdin
    try:
        input_text = sys.stdin.read().strip()
        if not input_text:
            print(json.dumps({"error": "Empty input"}), file=sys.stderr)
            sys.exit(1)
    except Exception as e:
        print(json.dumps({"error": f"Failed to read stdin: {str(e)}"}), file=sys.stderr)
        sys.exit(1)
    
    # Tokenize
    try:
        # CodeT5+ uses encoder-decoder, so we tokenize for encoder input
        encoding = tokenizer(
            input_text,
            return_tensors="pt",
            padding=False,
            truncation=True,
            max_length=512  # T5 max sequence length
        )
        
        token_ids = encoding["input_ids"][0].tolist()
        attention_mask = encoding["attention_mask"][0].tolist()
        
        # Output as JSON
        result = {
            "token_ids": token_ids,
            "attention_mask": attention_mask,
            "length": len(token_ids),
            "model": model_path.name
        }
        
        print(json.dumps(result))
        
    except Exception as e:
        print(json.dumps({"error": f"Tokenization failed: {str(e)}"}), file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
