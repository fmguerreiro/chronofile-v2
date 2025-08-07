# TinyBERT Android Model Assets

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
