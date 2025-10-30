#!/usr/bin/env python3
"""
Export CodeT5+ 220M from PyTorch/SafeTensors to ONNX format.
The model is already downloaded, we just need to export it.
"""

import torch
from transformers import T5ForConditionalGeneration, AutoTokenizer
from pathlib import Path
import onnx

def export_to_onnx():
    """Export the downloaded model to ONNX format."""
    
    model_dir = Path(__file__).parent / "base" / "codet5p-220m"
    output_path = model_dir / "model.onnx"
    
    print("=" * 60)
    print("CodeT5+ 220M → ONNX Export")
    print("=" * 60)
    print(f"\nModel directory: {model_dir}")
    print(f"Output: {output_path}")
    
    try:
        # Load model and tokenizer
        print("\n1. Loading model from disk...")
        model = T5ForConditionalGeneration.from_pretrained(str(model_dir))
        tokenizer = AutoTokenizer.from_pretrained(str(model_dir))
        model.eval()  # Set to evaluation mode
        print("   ✓ Model loaded")
        
        # Create dummy input for tracing
        print("\n2. Creating dummy input for ONNX export...")
        dummy_text = "def print_hello():"
        inputs = tokenizer(dummy_text, return_tensors="pt")
        input_ids = inputs['input_ids']
        attention_mask = inputs['attention_mask']
        
        print(f"   Input shape: {input_ids.shape}")
        
        # Export to ONNX
        print("\n3. Exporting to ONNX (this may take a minute)...")
        print("   Note: T5 export can be complex due to encoder-decoder architecture")
        
        with torch.no_grad():
            torch.onnx.export(
                model,
                (input_ids, attention_mask),
                str(output_path),
                input_names=['input_ids', 'attention_mask'],
                output_names=['output'],
                dynamic_axes={
                    'input_ids': {0: 'batch', 1: 'sequence'},
                    'attention_mask': {0: 'batch', 1: 'sequence'},
                    'output': {0: 'batch', 1: 'sequence'}
                },
                opset_version=14,
                do_constant_folding=True,
            )
        
        # Verify ONNX model
        print("\n4. Verifying ONNX model...")
        onnx_model = onnx.load(str(output_path))
        onnx.checker.check_model(onnx_model)
        print("   ✓ ONNX model is valid")
        
        # Check file size
        size_mb = output_path.stat().st_size / (1024 * 1024)
        print(f"\n✓ Export successful!")
        print(f"   ONNX model: {output_path}")
        print(f"   Size: {size_mb:.1f} MB")
        
        return True
        
    except Exception as e:
        print(f"\n✗ Export failed: {e}")
        print("\nNote: T5 models are complex to export to ONNX.")
        print("Consider using Optimum library or pre-converted ONNX models.")
        return False

if __name__ == "__main__":
    export_to_onnx()
