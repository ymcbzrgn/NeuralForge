#!/usr/bin/env python3
"""
Detokenization script for NeuralForge backend.
Reads token IDs from stdin as JSON, outputs decoded text to stdout.

Usage:
    echo '{"token_ids": [123, 456, 789]}' | python nf_detokenize.py models/base/codet5p-220m
    Output: {"text": "def hello():"}
"""

import sys
import json
from pathlib import Path
from transformers import AutoTokenizer


def main():
    if len(sys.argv) != 2:
        print(json.dumps({"error": "Usage: nf_detokenize.py <model_path>"}), file=sys.stderr)
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
    
    # Read input JSON from stdin
    try:
        input_json = sys.stdin.read().strip()
        if not input_json:
            print(json.dumps({"error": "Empty input"}), file=sys.stderr)
            sys.exit(1)
        
        data = json.loads(input_json)
        token_ids = data.get("token_ids")
        
        if token_ids is None:
            print(json.dumps({"error": "Missing 'token_ids' field"}), file=sys.stderr)
            sys.exit(1)
            
    except json.JSONDecodeError as e:
        print(json.dumps({"error": f"Invalid JSON: {str(e)}"}), file=sys.stderr)
        sys.exit(1)
    except Exception as e:
        print(json.dumps({"error": f"Failed to read stdin: {str(e)}"}), file=sys.stderr)
        sys.exit(1)
    
    # Detokenize
    try:
        # Decode token IDs back to text
        text = tokenizer.decode(token_ids, skip_special_tokens=True)
        
        # Output as JSON
        result = {
            "text": text,
            "length": len(text),
            "num_tokens": len(token_ids),
            "model": model_path.name
        }
        
        print(json.dumps(result))
        
    except Exception as e:
        print(json.dumps({"error": f"Detokenization failed: {str(e)}"}), file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
