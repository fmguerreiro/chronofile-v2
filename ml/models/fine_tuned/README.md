# Fine-tuned TinyBERT Model

This directory contained the fine-tuned TinyBERT model for activity classification.

**Note**: Large model files (*.safetensors, optimizer.pt) have been excluded from git due to GitHub size limits.

## Model Performance
- **Accuracy**: 68.3%
- **Weighted F1-score**: 66.4% 
- **Training Examples**: 2,226
- **Categories**: 9 (exercise, work, food, sleep, social, learning, entertainment, health, travel)

## Key Files (excluded from git)
- `model.safetensors` - Fine-tuned model weights (~55MB)
- `checkpoint-*/optimizer.pt` - Training checkpoints (~110MB each)

## Available Files
- `config.json` - Model configuration
- `tokenizer.json` - Tokenizer configuration  
- `label_encoder.json` - Category label mappings
- `classification_report.json` - Detailed performance metrics
- `confusion_matrix.png` - Visual performance analysis

To use the trained model, the model files would need to be downloaded separately or retrained using the pipeline in `ml/training/`.