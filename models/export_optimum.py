#!/usr/bin/env python3
"""
Export CodeT5+ 220M to ONNX using Optimum CLI.
Optimum handles T5 encoder-decoder architecture properly.
"""

import subprocess
import sys
from pathlib import Path

def export_with_optimum():
    """Use Optimum CLI to export the model."""
    
    model_dir = Path(__file__).parent / "base" / "codet5p-220m"
    output_dir = model_dir / "onnx"
    
    print("=" * 60)
    print("CodeT5+ 220M → ONNX Export (via Optimum)")
    print("=" * 60)
    print(f"\nInput: {model_dir}")
    print(f"Output: {output_dir}")
    
    # Create output directory
    output_dir.mkdir(exist_ok=True)
    
    # Use optimum-cli to export
    cmd = [
        "optimum-cli",
        "export",
        "onnx",
        "--model", str(model_dir),
        "--task", "text2text-generation",
        str(output_dir)
    ]
    
    print("\nRunning: " + " ".join(cmd))
    print("(This may take several minutes...)\n")
    
    try:
        result = subprocess.run(cmd, check=True, capture_output=False, text=True)
        
        print("\n" + "=" * 60)
        print("✓ Export successful!")
        print("=" * 60)
        print(f"\nONNX files in: {output_dir}")
        print("\nGenerated files:")
        for file in sorted(output_dir.glob("*")):
            if file.is_file():
                size_mb = file.stat().st_size / (1024 * 1024)
                print(f"  - {file.name} ({size_mb:.1f} MB)")
        
        return True
        
    except subprocess.CalledProcessError as e:
        print(f"\n✗ Export failed: {e}")
        return False
    except FileNotFoundError:
        print("\n✗ optimum-cli not found!")
        print("Install with: pip install optimum[onnxruntime]")
        return False

if __name__ == "__main__":
    success = export_with_optimum()
    sys.exit(0 if success else 1)
