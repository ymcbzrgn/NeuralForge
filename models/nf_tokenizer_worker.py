#!/usr/bin/env python3
"""
Persistent tokenizer worker process for NeuralForge.

This script stays alive indefinitely, listening on stdin for tokenization commands
and responding on stdout. This eliminates the 3-4s Python startup overhead per request.

Communication Protocol:
- Input (stdin): JSON line-delimited commands
- Output (stdout): JSON line-delimited responses

Commands:
- TOKENIZE: Convert text to token IDs
- DETOKENIZE: Convert token IDs to text
- PING: Health check
- SHUTDOWN: Graceful exit

Performance:
- Startup: ~3-4s (one-time, loads transformers + tokenizer)
- Per-request: <100ms (tokenizer already loaded in memory)
"""

import sys
import json
import os

# Add parent directory to path for imports
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

# Import transformers (takes ~1-1.5s)
from transformers import AutoTokenizer

# Determine model path
MODEL_PATH = os.path.join(os.path.dirname(__file__), "base", "codet5p-220m")

# Load tokenizer ONCE at startup (takes ~0.5-1s, one-time cost)
try:
    tokenizer = AutoTokenizer.from_pretrained(MODEL_PATH)
    
    # Send "ready" signal to Java
    ready_signal = {"status": "ready", "model": "codet5p-220m"}
    print(json.dumps(ready_signal), flush=True)
    
except Exception as e:
    # If startup fails, send error and exit
    error_signal = {"status": "error", "message": f"Tokenizer load failed: {str(e)}"}
    print(json.dumps(error_signal), flush=True)
    sys.exit(1)

# Main command loop
while True:
    try:
        # Read command from stdin (blocking)
        line = sys.stdin.readline()
        
        # EOF received, exit gracefully
        if not line:
            break
        
        # Parse JSON command
        request = json.loads(line.strip())
        command = request.get("command")
        request_id = request.get("id", "unknown")
        
        # Handle TOKENIZE command
        if command == "TOKENIZE":
            text = request.get("text", "")
            
            # Tokenize (fast, <100ms)
            token_ids = tokenizer.encode(text, add_special_tokens=True)
            
            # Send response
            response = {
                "id": request_id,
                "status": "ok",
                "result": token_ids,
                "length": len(token_ids)
            }
            print(json.dumps(response), flush=True)
        
        # Handle DETOKENIZE command
        elif command == "DETOKENIZE":
            tokens = request.get("tokens", [])
            
            # Detokenize (fast, <100ms)
            text = tokenizer.decode(tokens, skip_special_tokens=True)
            
            # Send response
            response = {
                "id": request_id,
                "status": "ok",
                "result": text,
                "length": len(text)
            }
            print(json.dumps(response), flush=True)
        
        # Handle PING command (health check)
        elif command == "PING":
            response = {
                "id": request_id,
                "status": "ok",
                "result": "pong",
                "model": "codet5p-220m"
            }
            print(json.dumps(response), flush=True)
        
        # Handle SHUTDOWN command (graceful exit)
        elif command == "SHUTDOWN":
            response = {
                "id": request_id,
                "status": "ok",
                "result": "shutting down"
            }
            print(json.dumps(response), flush=True)
            break  # Exit loop, terminate process
        
        # Unknown command
        else:
            response = {
                "id": request_id,
                "status": "error",
                "message": f"Unknown command: {command}"
            }
            print(json.dumps(response), flush=True)
    
    except json.JSONDecodeError as e:
        # Invalid JSON received
        error_response = {
            "id": "unknown",
            "status": "error",
            "message": f"Invalid JSON: {str(e)}"
        }
        print(json.dumps(error_response), flush=True)
    
    except Exception as e:
        # Unexpected error during processing
        error_response = {
            "id": request_id if 'request_id' in locals() else "unknown",
            "status": "error",
            "message": f"Processing error: {str(e)}"
        }
        print(json.dumps(error_response), flush=True)

# Clean exit
sys.exit(0)
