#!/usr/bin/env python3
"""
Convert CodeT5+ 220M model to ONNX format for Java inference.

Requirements:
pip install torch transformers optimum[exporters]
"""

import sys
from pathlib import Path
from transformers import T5ForConditionalGeneration, AutoTokenizer
from optimum.exporters.onnx import main_export

def convert_codet5p_to_onnx():
    """Download and convert CodeT5+ 220M to ONNX format."""
    
    print("=" * 60)
    print("CodeT5+ 220M → ONNX Conversion")
    print("=" * 60)
    
    model_name = "Salesforce/codet5p-220m"
    output_dir = Path(__file__).parent / "base" / "codet5p-220m-onnx"
    
    print(f"\n1. Downloading model: {model_name}")
    print(f"   Output directory: {output_dir}")
    
    # Create output directory
    output_dir.mkdir(parents=True, exist_ok=True)
    
    try:
        # Download tokenizer first
        print("\n2. Downloading tokenizer...")
        tokenizer = AutoTokenizer.from_pretrained(model_name)
        tokenizer.save_pretrained(output_dir)
        print("   ✓ Tokenizer saved")
        
        # Export to ONNX using optimum
        print("\n3. Exporting to ONNX format...")
        print("   (This may take a few minutes...)")
        
        main_export(
            model_name_or_path=model_name,
            output=str(output_dir),
            task="text2text-generation",
            opset=14,  # ONNX Runtime 1.19 supports opset 14
        )
        
        print("\n" + "=" * 60)
        print("✓ Conversion successful!")
        print("=" * 60)
        print(f"\nONNX model saved to: {output_dir}")
        print("\nFiles created:")
        for file in sorted(output_dir.glob("*")):
            size_mb = file.stat().st_size / (1024 * 1024)
            print(f"  - {file.name} ({size_mb:.1f} MB)")
        
        return True
        
    except Exception as e:
        print(f"\n✗ Error during conversion: {e}")
        print("\nMake sure you have installed:")
        print("  pip install torch transformers optimum[exporters]")
        return False

if __name__ == "__main__":
    success = convert_codet5p_to_onnx()
    sys.exit(0 if success else 1)
