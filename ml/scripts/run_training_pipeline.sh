#!/bin/bash
# Complete training pipeline for TinyBERT activity classification

set -e  # Exit on error

echo "=========================================="
echo "TinyBERT Activity Classification Pipeline"
echo "=========================================="

# Configuration
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCRIPTS_DIR="$PROJECT_DIR/scripts"
TRAINING_DIR="$PROJECT_DIR/training"
DATA_DIR="$PROJECT_DIR/data"
MODELS_DIR="$PROJECT_DIR/models"

echo "Project directory: $PROJECT_DIR"

# Create directories
mkdir -p "$DATA_DIR" "$MODELS_DIR" "$PROJECT_DIR/logs"

# Step 1: Prepare training data
echo ""
echo "Step 1: Preparing training data..."
cd "$SCRIPTS_DIR"
python prepare_data.py

if [ ! -f "$DATA_DIR/training_data.csv" ]; then
    echo "ERROR: Training data preparation failed"
    exit 1
fi

echo "✓ Training data prepared successfully"
echo "  - Training examples: $(tail -n +2 "$DATA_DIR/training_data.csv" | wc -l)"
echo "  - Validation examples: $(tail -n +2 "$DATA_DIR/validation_data.csv" | wc -l)"
echo "  - Test examples: $(tail -n +2 "$DATA_DIR/test_data.csv" | wc -l)"

# Step 2: Train the model
echo ""
echo "Step 2: Training TinyBERT model..."
cd "$TRAINING_DIR"

if [ ! -f "config.yaml" ]; then
    echo "ERROR: config.yaml not found in $TRAINING_DIR"
    exit 1
fi

python train_model.py

if [ ! -d "$MODELS_DIR/fine_tuned" ]; then
    echo "ERROR: Model training failed"
    exit 1
fi

echo "✓ Model training completed"

# Step 3: Convert and optimize for mobile
echo ""
echo "Step 3: Converting model for mobile deployment..."
cd "$SCRIPTS_DIR"
python convert_to_mobile.py "$TRAINING_DIR/config.yaml" "$MODELS_DIR/fine_tuned"

if [ ! -f "$MODELS_DIR/mobile/activity_classifier.tflite" ]; then
    echo "ERROR: Mobile model conversion failed"
    exit 1
fi

echo "✓ Mobile model conversion completed"

# Step 4: Benchmark results
echo ""
echo "Step 4: Performance benchmark results..."
if [ -f "$MODELS_DIR/mobile/benchmark_results.json" ]; then
    echo "Model performance:"
    python -c "
import json
with open('$MODELS_DIR/mobile/benchmark_results.json') as f:
    results = json.load(f)
    for model, metrics in results.items():
        print(f'  {model}:')
        print(f'    Latency: {metrics.get(\"mean_latency_ms\", 0):.1f}ms')
        print(f'    Size: {metrics.get(\"model_size_mb\", 0):.1f}MB')
"
else
    echo "⚠ Benchmark results not available"
fi

# Step 5: Prepare Android assets
echo ""
echo "Step 5: Android integration assets..."
ANDROID_ASSETS="$PROJECT_DIR/android_integration/model_assets"
if [ -d "$ANDROID_ASSETS" ]; then
    echo "✓ Android assets ready:"
    echo "  - Model: $(ls -lh "$ANDROID_ASSETS"/*.tflite 2>/dev/null | awk '{print $5, $9}' || echo 'Not found')"
    echo "  - Config files: $(ls "$ANDROID_ASSETS"/*.json 2>/dev/null | wc -l) files"
else
    echo "⚠ Android assets directory not found"
fi

# Final summary
echo ""
echo "=========================================="
echo "TRAINING PIPELINE COMPLETED SUCCESSFULLY"
echo "=========================================="
echo ""
echo "Next steps:"
echo "1. Review training results in: $MODELS_DIR/fine_tuned/"
echo "2. Check mobile model performance in: $MODELS_DIR/mobile/"
echo "3. Follow integration guide: $PROJECT_DIR/android_integration/INTEGRATION_GUIDE.md"
echo "4. Copy Android assets to your app: $ANDROID_ASSETS/"
echo ""
echo "Key files:"
echo "  - TensorFlow Lite model: $MODELS_DIR/mobile/activity_classifier.tflite"
echo "  - Android classifier: $PROJECT_DIR/android_integration/TinyBertClassifier.kt"
echo "  - Integration guide: $PROJECT_DIR/android_integration/INTEGRATION_GUIDE.md"
echo ""

# Display final model stats if available
if [ -f "$MODELS_DIR/fine_tuned/classification_report.json" ]; then
    echo "Model accuracy metrics:"
    python -c "
import json
with open('$MODELS_DIR/fine_tuned/classification_report.json') as f:
    report = json.load(f)
    accuracy = report.get('accuracy', 0)
    weighted_f1 = report.get('weighted avg', {}).get('f1-score', 0)
    print(f'  Overall accuracy: {accuracy:.3f}')
    print(f'  Weighted F1-score: {weighted_f1:.3f}')
"
fi

echo ""
echo "Training pipeline completed at $(date)"
echo "=========================================="