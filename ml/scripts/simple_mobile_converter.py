#!/usr/bin/env python3
"""
Simplified mobile model converter - creates necessary assets for Android integration
"""

import os
import json
import shutil
from pathlib import Path

def create_android_assets():
    """Create Android-ready assets from trained model."""
    print("Creating Android assets for TinyBERT integration...")
    
    project_root = Path("..").resolve()
    model_dir = project_root / "models" / "fine_tuned" 
    android_dir = project_root / "android_integration" / "model_assets"
    
    # Create Android assets directory
    android_dir.mkdir(parents=True, exist_ok=True)
    
    # Check if model exists
    if not model_dir.exists():
        print(f"ERROR: Model directory not found at {model_dir}")
        return False
    
    print(f"Model directory: {model_dir}")
    print(f"Android assets directory: {android_dir}")
    
    # Copy label encoder if it exists
    label_encoder_src = model_dir / "label_encoder.json"
    if label_encoder_src.exists():
        shutil.copy2(label_encoder_src, android_dir / "label_encoder.json")
        print("✓ Copied label encoder")
    else:
        # Create a basic label encoder
        categories = ["exercise", "work", "food", "sleep", "social", "learning", "entertainment", "health", "travel"]
        label_encoder = {
            "classes": categories,
            "category_to_id": {cat: idx for idx, cat in enumerate(categories)}
        }
        with open(android_dir / "label_encoder.json", 'w') as f:
            json.dump(label_encoder, f, indent=2)
        print("✓ Created label encoder")
    
    # Create simplified tokenizer config
    tokenizer_config = {
        "vocab_size": 30522,  # Standard BERT vocab size
        "max_length": 128,
        "pad_token": "[PAD]",
        "pad_token_id": 0,
        "unk_token": "[UNK]",
        "unk_token_id": 100,
        "cls_token": "[CLS]",
        "cls_token_id": 101,
        "sep_token": "[SEP]",
        "sep_token_id": 102
    }
    
    with open(android_dir / "tokenizer_config.json", 'w') as f:
        json.dump(tokenizer_config, f, indent=2)
    print("✓ Created tokenizer config")
    
    # Create simplified vocabulary (subset for demonstration)
    # In production, you'd extract this from the actual tokenizer
    basic_vocab = {
        "[PAD]": 0, "[UNK]": 100, "[CLS]": 101, "[SEP]": 102,
        # Common activity words
        "work": 2000, "job": 2001, "office": 2002, "meeting": 2003,
        "exercise": 2100, "gym": 2101, "run": 2102, "workout": 2103,
        "food": 2200, "eat": 2201, "lunch": 2202, "dinner": 2203,
        "sleep": 2300, "rest": 2301, "nap": 2302, "bed": 2303,
        "social": 2400, "friends": 2401, "family": 2402, "party": 2403,
        "study": 2500, "learn": 2501, "read": 2502, "book": 2503,
        "movie": 2600, "tv": 2601, "watch": 2602, "entertainment": 2603,
        "doctor": 2700, "health": 2701, "medical": 2702, "appointment": 2703,
        "travel": 2800, "trip": 2801, "drive": 2802, "flight": 2803
    }
    
    # Add more common words
    common_words = [
        "the", "and", "is", "a", "to", "of", "in", "for", "with", "on",
        "at", "by", "from", "as", "be", "have", "has", "do", "will", "would",
        "morning", "afternoon", "evening", "night", "today", "time", "go",
        "get", "make", "take", "come", "see", "know", "think", "want", "new",
        "good", "great", "long", "short", "big", "small", "home", "house"
    ]
    
    for i, word in enumerate(common_words):
        basic_vocab[word] = 3000 + i
    
    with open(android_dir / "vocab.json", 'w') as f:
        json.dump(basic_vocab, f, indent=2)
    print("✓ Created basic vocabulary")
    
    # Create a placeholder for the TensorFlow Lite model
    # In a full implementation, this would be the actual converted model
    placeholder_model_info = {
        "note": "This is a placeholder. In production, this would be the actual TensorFlow Lite model file.",
        "model_size_mb": "~30-50MB",
        "input_shape": [1, 128],
        "output_shape": [1, 9],
        "quantized": True
    }
    
    with open(android_dir / "model_info.json", 'w') as f:
        json.dump(placeholder_model_info, f, indent=2)
    print("⚠ Created placeholder model info (actual .tflite model would be created with full TensorFlow)")
    
    # Create README for integration
    readme_content = """# TinyBERT Android Model Assets

This directory contains the necessary files for integrating TinyBERT into the Android app:

## Files:
- `label_encoder.json` - Category to ID mappings
- `tokenizer_config.json` - Tokenizer configuration  
- `vocab.json` - Basic vocabulary for tokenization
- `model_info.json` - Model information (placeholder for actual .tflite file)

## Integration Notes:
1. The actual TensorFlow Lite model file (.tflite) would be ~30-50MB
2. For full integration, you need to complete the TensorFlow conversion step
3. The current files allow you to test the integration architecture

## Next Steps:
1. Install TensorFlow properly: `pip install tensorflow==2.10.0`
2. Run the full conversion: `python convert_to_mobile.py`
3. Copy all assets to `app/src/main/assets/` in your Android project
"""
    
    with open(android_dir / "README.md", 'w') as f:
        f.write(readme_content)
    print("✓ Created integration README")
    
    print("\n" + "="*50)
    print("ANDROID ASSETS CREATED SUCCESSFULLY")
    print("="*50)
    print(f"Location: {android_dir}")
    print("\nFiles created:")
    for file in android_dir.glob("*"):
        print(f"  - {file.name}")
    
    print(f"\nNext steps:")
    print(f"1. Review the integration guide: {project_root}/android_integration/INTEGRATION_GUIDE.md")
    print(f"2. Copy TinyBertClassifier.kt to your Android project")
    print(f"3. Copy these assets to app/src/main/assets/")
    print(f"4. For full ML functionality, complete TensorFlow conversion")
    
    return True

def main():
    """Main function."""
    success = create_android_assets()
    if success:
        print("\n✅ Android assets creation completed!")
    else:
        print("\n❌ Android assets creation failed!")
        return 1
    return 0

if __name__ == "__main__":
    exit(main())