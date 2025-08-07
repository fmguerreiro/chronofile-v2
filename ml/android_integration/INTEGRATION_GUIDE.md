# TinyBERT Android Integration Guide

This guide explains how to integrate the TinyBERT activity classifier into the Chronofile Android app.

## Files Overview

### Core Files
- `TinyBertClassifier.kt` - Main inference wrapper for TinyBERT model
- `model_assets/` - Directory containing model files and configuration

### Model Assets
- `activity_classifier.tflite` - Quantized TensorFlow Lite model
- `vocab.json` - Model vocabulary mapping
- `tokenizer_config.json` - Tokenizer configuration
- `label_encoder.json` - Category label mappings

## Integration Steps

### 1. Copy Files to Android Project

```bash
# Copy the classifier implementation
cp ml/android_integration/TinyBertClassifier.kt app/src/main/kotlin/com/chaidarun/chronofile/

# Copy model assets to Android assets directory
cp -r ml/android_integration/model_assets/* app/src/main/assets/
```

### 2. Update build.gradle

Add TensorFlow Lite dependency:

```gradle
dependencies {
    // Existing dependencies...
    
    // TensorFlow Lite for model inference
    implementation 'org.tensorflow:tensorflow-lite:2.13.0'
    implementation 'org.tensorflow:tensorflow-lite-support:0.4.4'
}
```

### 3. Update ActivityClassifier.kt

Replace the existing `ActivityClassifier` with enhanced version:

```kotlin
// Replace this line in ActivityClassifier.kt:
class ActivityClassifier {

// With:
class ActivityClassifier {
    // Keep existing implementation as fallback
    // (This will be used when TinyBERT fails or has low confidence)
```

### 4. Update IconDatabase.kt

Modify the `IconDatabase` to use the enhanced classifier:

```kotlin
// In IconDatabase.kt, replace the ActivityClassifier initialization:
private val activityClassifier by lazy { ActivityClassifier() }

// With:
private val enhancedClassifier by lazy { EnhancedActivityClassifier(context) }
private var isEnhancedClassifierReady = false

// Add initialization method:
fun initializeEnhancedClassifier(context: Context) {
    enhancedClassifier.initializeAsync { success ->
        isEnhancedClassifierReady = success
        if (success) {
            Log.i("IconDatabase", "Enhanced TinyBERT classifier initialized successfully")
        } else {
            Log.w("IconDatabase", "TinyBERT classifier failed to initialize, using fallback")
        }
    }
}

// Update findByActivityText method to use enhanced classifier:
private fun findByActivityText(activity: String): Int {
    // ... existing code ...
    
    // Replace ML-based prediction section with:
    if (isEnhancedClassifierReady) {
        val prediction = enhancedClassifier.predictCategory(activity)
        Log.d("IconMapping", "$activity -> Enhanced prediction: ${prediction.category} (${prediction.confidence}) [${prediction.source}]")
        if (prediction.confidence > 0.3f) {
            return prediction.iconRes
        }
    } else {
        // Fallback to original classifier
        val prediction = activityClassifier.predictCategory(activity)
        Log.d("IconMapping", "$activity -> Fallback prediction: ${prediction.category} (${prediction.confidence})")
        if (prediction.confidence > 0.3f) {
            return prediction.iconRes
        }
    }
    
    // ... rest of existing fallback logic ...
}
```

### 5. Update App.kt

Initialize the enhanced classifier on app startup:

```kotlin
// In App.kt onCreate() method:
override fun onCreate() {
    super.onCreate()
    
    // Existing initialization...
    
    // Initialize enhanced classifier asynchronously
    IconDatabase.initializeEnhancedClassifier(this)
}

// Add cleanup in onTerminate():
override fun onTerminate() {
    super.onTerminate()
    IconDatabase.cleanup() // Add this method to IconDatabase
}
```

### 6. Add Cleanup Method to IconDatabase

```kotlin
// Add to IconDatabase object:
fun cleanup() {
    if (::enhancedClassifier.isInitialized) {
        enhancedClassifier.cleanup()
    }
}
```

### 7. Handle Permissions and File Access

Ensure the app can access model assets:

```kotlin
// No special permissions needed for assets, but verify in AndroidManifest.xml
// that no conflicting permissions are blocking file access
```

### 8. Test Integration

Add debugging/testing methods to verify integration:

```kotlin
// Add to IconDatabase for debugging:
fun getClassifierStatus(): Map<String, Any> {
    return if (::enhancedClassifier.isInitialized) {
        enhancedClassifier.getStatus()
    } else {
        mapOf("error" to "Enhanced classifier not initialized")
    }
}

// Test predictions:
fun testPredictions() {
    val testCases = listOf(
        "morning run", "team meeting", "lunch with friends", "gym workout",
        "family dinner", "study session", "watch movie", "doctor appointment"
    )
    
    testCases.forEach { activity ->
        val icon = findByActivityText(activity)
        Log.d("IconDatabase", "Test: '$activity' -> $icon")
    }
}
```

## Performance Considerations

### Model Size and Loading
- **Model Size**: ~30-50MB (quantized TFLite)
- **Loading Time**: 1-3 seconds on first initialization
- **Memory Usage**: ~50-100MB additional RAM during inference

### Inference Performance
- **Target Latency**: <50ms per prediction
- **Batch Processing**: Not supported (single prediction only)
- **Threading**: Initialize on background thread, inference can be on main thread

### Battery Impact
- **Minimal**: Model is only loaded once and predictions are fast
- **Optimization**: Model is quantized and optimized for mobile

## Fallback Strategy

The integration includes comprehensive fallback mechanisms:

1. **High Confidence TinyBERT** (>0.7) - Use ML prediction
2. **Medium Confidence TinyBERT** (>0.3) - Use ML prediction with caution
3. **Low Confidence/Failed TinyBERT** - Fall back to rule-based system
4. **Model Loading Failure** - Gracefully fall back to existing system

## Debugging and Monitoring

### Enable Detailed Logging
```kotlin
// Add to Application onCreate():
if (BuildConfig.DEBUG) {
    Log.setVerbose("TinyBertClassifier")
    Log.setVerbose("IconMapping")
}
```

### Monitor Performance
```kotlin
// Track prediction times and accuracy:
val status = IconDatabase.getClassifierStatus()
Log.d("Performance", "Classifier status: $status")
```

### Error Handling
The system is designed to never crash the app:
- All ML operations are wrapped in try-catch blocks
- Graceful fallback to existing rule-based system
- Clear error logging for debugging

## Production Deployment

### Pre-launch Checklist
- [ ] Model assets copied to `app/src/main/assets/`
- [ ] TensorFlow Lite dependency added to `build.gradle`
- [ ] Integration code added to `IconDatabase.kt`
- [ ] Initialization added to `App.kt`
- [ ] Testing on multiple device types completed
- [ ] Performance benchmarking completed
- [ ] Error handling tested (corrupt model files, low memory, etc.)

### A/B Testing Setup
Consider implementing A/B testing to compare ML vs rule-based performance:

```kotlin
// Example A/B testing integration:
class IconDatabase {
    private val useEnhancedClassifier = when {
        BuildConfig.DEBUG -> true // Always use in debug
        Random.nextFloat() < 0.5f -> true // 50% of users in production
        else -> false
    }
    
    private fun findByActivityText(activity: String): Int {
        if (useEnhancedClassifier && isEnhancedClassifierReady) {
            // Use TinyBERT
        } else {
            // Use rule-based system
        }
    }
}
```

## Troubleshooting

### Common Issues

1. **Model Not Loading**
   - Check that model files are in `app/src/main/assets/`
   - Verify file sizes are correct (model should be 30-50MB)
   - Check Android logs for TensorFlow Lite errors

2. **Poor Prediction Quality**
   - Verify model was trained with appropriate data
   - Check confidence thresholds are appropriate
   - Compare with rule-based system performance

3. **Performance Issues**
   - Monitor inference times in logs
   - Consider reducing model size if needed
   - Verify quantization was applied correctly

4. **Memory Issues**
   - Monitor app memory usage during model loading
   - Consider loading model only when needed
   - Implement proper cleanup on app termination

### Debug Commands
```bash
# Check model file sizes
ls -la app/src/main/assets/

# Check Android logs
adb logcat | grep -E "(TinyBert|IconMapping|ActivityClassifier)"

# Monitor app memory
adb shell dumpsys meminfo com.chaidarun.chronofile
```