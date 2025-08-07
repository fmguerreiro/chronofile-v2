# TinyBERT Activity Classification System

This directory contains the machine learning pipeline for improving activity text → icon mapping using TinyBERT.

## Architecture Overview

### Data Flow
```
User Input → TinyBERT Model → Category Prediction → Icon Mapping → UI Display
     ↓                                                      ↑
User Feedback → Learning System → Model Fine-tuning → Android Integration
```

### Integration with Existing System

The TinyBERT system enhances the current `ActivityClassifier` by replacing the feature-based approach with deep learning while maintaining the same interface:

1. **Prediction Priority** (unchanged from current system):
   - User learned preferences (highest)
   - TinyBERT predictions (replaces current ML)
   - Direct keyword matching
   - Semantic fallbacks
   - Default icon

2. **Model Architecture**:
   - Pre-trained TinyBERT (distilled BERT)
   - Classification head for 9 categories
   - Confidence thresholding (>0.7 for high confidence)
   - Quantized for mobile deployment

## Directory Structure

```
ml/
├── data/
│   ├── training_data.csv        # Labeled activity examples
│   ├── validation_data.csv      # Validation set
│   └── test_data.csv           # Test set
├── models/
│   ├── tinybert_base/          # Pre-trained TinyBERT
│   ├── fine_tuned/             # Fine-tuned model checkpoints
│   └── mobile/                 # Quantized models for Android
├── training/
│   ├── train_model.py          # Training script
│   ├── evaluate_model.py       # Evaluation script
│   └── config.yaml            # Training configuration
├── android_integration/
│   ├── TinyBertClassifier.kt   # Android inference wrapper
│   └── model_assets/           # Converted model files
└── scripts/
    ├── prepare_data.py         # Data preparation pipeline
    ├── convert_to_mobile.py    # Model conversion script
    └── benchmark.py           # Performance testing
```

## Model Specifications

### TinyBERT Configuration
- **Model**: TinyBERT (66M parameters)
- **Input**: Max 128 tokens (sufficient for activity descriptions)
- **Output**: 9 category probabilities + confidence score
- **Quantization**: INT8 dynamic quantization
- **Target Size**: <50MB for mobile deployment

### Training Parameters
- **Learning Rate**: 2e-5
- **Batch Size**: 32
- **Epochs**: 3-5 (with early stopping)
- **Validation Split**: 15%
- **Test Split**: 15%

## Data Requirements

### Training Data Format (CSV)
```
user_input,icon_label,confidence_score,category
"morning jog around the park",exercise,1.0,exercise
"team standup meeting",work,1.0,work
"lunch with colleagues",food,0.8,social
```

### Data Collection Strategy
1. **Existing Keywords**: Bootstrap from current `IconDatabase` keywords
2. **Synthetic Augmentation**: Generate variations using templates
3. **User Feedback**: Collect from production app usage
4. **Multilingual**: Include Spanish, French, German, Portuguese examples

### Target Data Volume
- **Training**: 7,000+ examples (700+ per category)
- **Validation**: 1,500+ examples
- **Test**: 1,500+ examples
- **Total**: 10,000+ labeled examples

## Android Integration

### Model Deployment
1. **TensorFlow Lite**: Convert trained model to .tflite format
2. **Asset Bundle**: Include in app/src/main/assets/
3. **Runtime Loading**: Load model on app initialization
4. **Caching**: Cache predictions for common activities

### Performance Requirements
- **Inference Time**: <100ms on mid-range Android devices
- **Memory Usage**: <100MB additional RAM
- **Battery Impact**: Minimal (cached predictions, efficient quantization)

## Fallback Strategy

The system maintains robust fallback mechanisms:

1. **Model Loading Failure**: Fall back to existing `ActivityClassifier`
2. **Low Confidence**: Use semantic fallbacks from current system
3. **Inference Error**: Graceful degradation to keyword matching
4. **Cold Start**: Pre-computed predictions for common activities

## Evaluation Metrics

### Performance Metrics
- **Accuracy**: Top-1 category prediction accuracy
- **Top-3 Accuracy**: Correct category in top 3 predictions
- **Confidence Calibration**: Alignment between confidence and correctness
- **F1-Score**: Per-category performance

### Mobile Metrics
- **Inference Latency**: Prediction time (target: <50ms)
- **Model Size**: Compressed model size (target: <50MB)
- **Memory Usage**: Runtime memory footprint
- **Battery Impact**: Power consumption per prediction

## Development Phases

### Phase 1: Data Preparation
- [ ] Create data collection pipeline
- [ ] Bootstrap from existing keywords
- [ ] Generate synthetic training data
- [ ] Validate data quality and balance

### Phase 2: Model Training
- [ ] Set up training environment
- [ ] Fine-tune TinyBERT on activity classification
- [ ] Hyperparameter optimization
- [ ] Model validation and testing

### Phase 3: Mobile Optimization
- [ ] Model quantization and compression
- [ ] Convert to TensorFlow Lite
- [ ] Benchmark on target devices
- [ ] Optimize for size and speed

### Phase 4: Android Integration
- [ ] Implement inference wrapper
- [ ] Update ActivityClassifier interface
- [ ] Add confidence thresholding
- [ ] Implement fallback mechanisms

### Phase 5: Production Deployment
- [ ] A/B testing framework
- [ ] Performance monitoring
- [ ] User feedback collection
- [ ] Continuous model improvement