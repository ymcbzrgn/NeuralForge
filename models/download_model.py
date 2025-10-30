#!/usr/bin/env python3
"""
Simple model downloader for CodeT5+ 220M.
This just downloads the model, we'll handle ONNX conversion separately.
"""

from transformers import T5ForConditionalGeneration, AutoTokenizer
from pathlib import Path

def download_model():
    model_name = "Salesforce/codet5p-220m"
    output_dir = Path(__file__).parent / "base" / "codet5p-220m"
    
    print(f"Downloading {model_name}...")
    print(f"Output: {output_dir}")
    
    output_dir.mkdir(parents=True, exist_ok=True)
    
    # Download and save
    print("\n1. Downloading tokenizer...")
    tokenizer = AutoTokenizer.from_pretrained(model_name)
    tokenizer.save_pretrained(output_dir)
    print("   ✓ Tokenizer saved")
    
    print("\n2. Downloading model (this may take a few minutes)...")
    model = T5ForConditionalGeneration.from_pretrained(model_name)
    model.save_pretrained(output_dir)
    print("   ✓ Model saved")
    
    print(f"\n✓ Model downloaded to: {output_dir}")
    print("\nFiles:")
    for file in sorted(output_dir.glob("*")):
        size_mb = file.stat().st_size / (1024 * 1024)
        print(f"  - {file.name} ({size_mb:.1f} MB)")

if __name__ == "__main__":
    download_model()
