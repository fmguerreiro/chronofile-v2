// © Art Chaidarun

package com.chaidarun.chronofile

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import kotlin.collections.HashMap

/**
 * TinyBERT-based activity classifier for intelligent icon prediction.
 * Provides deep learning inference for activity text → category classification.
 */
class TinyBertClassifier(private val context: Context) {
    
    companion object {
        private const val TAG = "TinyBertClassifier"
        private const val MODEL_FILE = "activity_classifier.tflite"
        private const val VOCAB_FILE = "vocab.json"
        private const val TOKENIZER_CONFIG_FILE = "tokenizer_config.json"
        private const val LABEL_ENCODER_FILE = "label_encoder.json"
        
        // Model constants
        private const val MAX_SEQUENCE_LENGTH = 128
        private const val CONFIDENCE_THRESHOLD = 0.7f
        private const val LOW_CONFIDENCE_THRESHOLD = 0.3f
    }
    
    data class ClassificationResult(
        val category: String,
        val confidence: Float,
        val iconRes: Int,
        val allProbabilities: Map<String, Float> = emptyMap()
    )
    
    data class TokenizerConfig(
        val vocab_size: Int,
        val max_length: Int,
        val pad_token: String,
        val pad_token_id: Int,
        val unk_token: String,
        val unk_token_id: Int
    )
    
    data class LabelEncoder(
        val classes: List<String>,
        val category_to_id: Map<String, Int>
    )
    
    // Model components
    private var interpreter: Interpreter? = null
    private var vocabulary: Map<String, Int> = emptyMap()
    private var tokenConfig: TokenizerConfig? = null
    private var labelEncoder: LabelEncoder? = null
    private var categoryToIcon: Map<String, Int> = emptyMap()
    
    // Performance tracking
    private var lastInferenceTime: Long = 0
    private var initializationFailed = false
    
    /**
     * Initialize the classifier. Should be called on a background thread.
     */
    fun initialize(): Boolean {
        if (initializationFailed) {
            return false
        }
        
        try {
            Log.i(TAG, "Initializing TinyBERT classifier...")
            val startTime = System.currentTimeMillis()
            
            // Load model components
            loadModel()
            loadVocabulary()
            loadTokenizerConfig()
            loadLabelEncoder()
            initializeCategoryMapping()
            
            val initTime = System.currentTimeMillis() - startTime
            Log.i(TAG, "TinyBERT classifier initialized successfully in ${initTime}ms")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TinyBERT classifier", e)
            initializationFailed = true
            return false
        }
    }
    
    /**
     * Predict activity category from text input.
     * Returns null if model is not initialized or prediction fails.
     */
    fun predict(activityText: String): ClassificationResult? {
        if (!isInitialized()) {
            Log.w(TAG, "Classifier not initialized, cannot predict")
            return null
        }
        
        try {
            val startTime = System.currentTimeMillis()
            
            // Preprocess input
            val tokens = tokenize(activityText)
            val inputBuffer = prepareInputBuffer(tokens)
            val attentionMask = prepareAttentionMask(tokens.size)
            
            // Run inference
            val outputBuffer = runInference(inputBuffer, attentionMask)
            
            // Post-process results
            val probabilities = softmax(outputBuffer)
            val result = createResult(probabilities)
            
            lastInferenceTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "Prediction completed in ${lastInferenceTime}ms: ${result.category} (${result.confidence})")
            
            return result
            
        } catch (e: Exception) {
            Log.e(TAG, "Prediction failed for text: '$activityText'", e)
            return null
        }
    }
    
    /**
     * Get the last inference time in milliseconds.
     */
    fun getLastInferenceTime(): Long = lastInferenceTime
    
    /**
     * Check if the classifier is properly initialized.
     */
    fun isInitialized(): Boolean {
        return interpreter != null && 
               vocabulary.isNotEmpty() && 
               tokenConfig != null && 
               labelEncoder != null &&
               !initializationFailed
    }
    
    /**
     * Get model performance statistics.
     */
    fun getModelStats(): Map<String, Any> {
        return mapOf(
            "initialized" to isInitialized(),
            "vocab_size" to vocabulary.size,
            "categories" to (labelEncoder?.classes?.size ?: 0),
            "max_length" to (tokenConfig?.max_length ?: 0),
            "last_inference_ms" to lastInferenceTime
        )
    }
    
    /**
     * Clean up resources.
     */
    fun cleanup() {
        interpreter?.close()
        interpreter = null
        Log.d(TAG, "TinyBERT classifier cleaned up")
    }
    
    // Private implementation methods
    
    private fun loadModel() {
        try {
            val modelBuffer = FileUtil.loadMappedFile(context, MODEL_FILE)
            val options = Interpreter.Options().apply {
                // Optimize for latency
                setNumThreads(2)
                setUseNNAPI(false) // Disable NNAPI for consistency
            }
            
            interpreter = Interpreter(modelBuffer, options)
            Log.d(TAG, "TensorFlow Lite model loaded successfully")
            
        } catch (e: IOException) {
            throw Exception("Failed to load TensorFlow Lite model", e)
        }
    }
    
    private fun loadVocabulary() {
        try {
            val vocabJson = loadAssetAsString(VOCAB_FILE)
            val type = object : TypeToken<Map<String, Int>>() {}.type
            vocabulary = Gson().fromJson(vocabJson, type)
            
            Log.d(TAG, "Vocabulary loaded: ${vocabulary.size} tokens")
            
        } catch (e: Exception) {
            throw Exception("Failed to load vocabulary", e)
        }
    }
    
    private fun loadTokenizerConfig() {
        try {
            val configJson = loadAssetAsString(TOKENIZER_CONFIG_FILE)
            tokenConfig = Gson().fromJson(configJson, TokenizerConfig::class.java)
            
            Log.d(TAG, "Tokenizer config loaded")
            
        } catch (e: Exception) {
            throw Exception("Failed to load tokenizer config", e)
        }
    }
    
    private fun loadLabelEncoder() {
        try {
            val encoderJson = loadAssetAsString(LABEL_ENCODER_FILE)
            labelEncoder = Gson().fromJson(encoderJson, LabelEncoder::class.java)
            
            Log.d(TAG, "Label encoder loaded: ${labelEncoder?.classes?.size} categories")
            
        } catch (e: Exception) {
            throw Exception("Failed to load label encoder", e)
        }
    }
    
    private fun initializeCategoryMapping() {
        // Map categories to drawable resources
        categoryToIcon = mapOf(
            "exercise" to R.drawable.ic_run,
            "work" to R.drawable.ic_briefcase_line,
            "food" to R.drawable.ic_utensils_line,
            "sleep" to R.drawable.ic_moon_line,
            "social" to R.drawable.ic_people,
            "learning" to R.drawable.ic_note,
            "entertainment" to R.drawable.ic_tv,
            "health" to R.drawable.ic_health,
            "travel" to R.drawable.ic_car
        )
    }
    
    private fun loadAssetAsString(fileName: String): String {
        return try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            throw Exception("Failed to load asset: $fileName", e)
        }
    }
    
    private fun tokenize(text: String): List<Int> {
        val config = tokenConfig ?: throw Exception("Tokenizer not configured")
        
        // Simple whitespace tokenization + vocabulary lookup
        // In production, this would use a proper BERT tokenizer
        val words = text.lowercase(Locale.getDefault())
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
        
        val tokens = mutableListOf<Int>()
        
        // Add [CLS] token (assuming ID 101 for BERT-like models)
        tokens.add(101)
        
        // Convert words to token IDs
        for (word in words) {
            val tokenId = vocabulary[word] ?: config.unk_token_id
            tokens.add(tokenId)
            
            if (tokens.size >= config.max_length - 1) break // Reserve space for [SEP]
        }
        
        // Add [SEP] token (assuming ID 102 for BERT-like models)
        tokens.add(102)
        
        // Pad to max length
        while (tokens.size < config.max_length) {
            tokens.add(config.pad_token_id)
        }
        
        return tokens.take(config.max_length)
    }
    
    private fun prepareInputBuffer(tokens: List<Int>): ByteBuffer {
        val inputBuffer = ByteBuffer.allocateDirect(4 * MAX_SEQUENCE_LENGTH)
        inputBuffer.order(ByteOrder.nativeOrder())
        
        tokens.forEach { token ->
            inputBuffer.putInt(token)
        }
        
        inputBuffer.rewind()
        return inputBuffer
    }
    
    private fun prepareAttentionMask(actualLength: Int): ByteBuffer {
        val maskBuffer = ByteBuffer.allocateDirect(4 * MAX_SEQUENCE_LENGTH)
        maskBuffer.order(ByteOrder.nativeOrder())
        
        repeat(MAX_SEQUENCE_LENGTH) { i ->
            maskBuffer.putInt(if (i < actualLength) 1 else 0)
        }
        
        maskBuffer.rewind()
        return maskBuffer
    }
    
    private fun runInference(inputBuffer: ByteBuffer, attentionMask: ByteBuffer): FloatArray {
        val interpreter = this.interpreter ?: throw Exception("Interpreter not initialized")
        val encoder = labelEncoder ?: throw Exception("Label encoder not initialized")
        
        // Prepare output buffer
        val outputBuffer = ByteBuffer.allocateDirect(4 * encoder.classes.size)
        outputBuffer.order(ByteOrder.nativeOrder())
        
        // Create input/output arrays
        val inputs = arrayOf(inputBuffer, attentionMask)
        val outputs = mapOf(0 to outputBuffer)
        
        // Run inference
        interpreter.runForMultipleInputsOutputs(inputs, outputs)
        
        // Convert output to float array
        outputBuffer.rewind()
        val logits = FloatArray(encoder.classes.size)
        outputBuffer.asFloatBuffer().get(logits)
        
        return logits
    }
    
    private fun softmax(logits: FloatArray): FloatArray {
        val maxLogit = logits.maxOrNull() ?: 0f
        val exponentials = logits.map { kotlin.math.exp(it - maxLogit) }
        val sum = exponentials.sum()
        
        return exponentials.map { (it / sum).toFloat() }.toFloatArray()
    }
    
    private fun createResult(probabilities: FloatArray): ClassificationResult {
        val encoder = labelEncoder ?: throw Exception("Label encoder not initialized")
        
        // Find best prediction
        val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
        val bestCategory = encoder.classes[maxIndex]
        val confidence = probabilities[maxIndex]
        
        // Get icon for category
        val iconRes = categoryToIcon[bestCategory] ?: R.drawable.ic_note
        
        // Create probability map
        val allProbs = encoder.classes.mapIndexed { index, category ->
            category to probabilities[index]
        }.toMap()
        
        return ClassificationResult(
            category = bestCategory,
            confidence = confidence,
            iconRes = iconRes,
            allProbabilities = allProbs
        )
    }
}

/**
 * Enhanced ActivityClassifier that integrates TinyBERT with the existing system.
 * Provides seamless fallback to the original rule-based approach.
 */
class EnhancedActivityClassifier {
    
    companion object {
        private const val TAG = "EnhancedActivityClassifier"
    }
    
    // Prediction sources in priority order
    data class Prediction(
        val category: String,
        val confidence: Float,
        val iconRes: Int,
        val source: String = "unknown"
    )
    
    private val tinyBertClassifier: TinyBertClassifier
    private val fallbackClassifier: ActivityClassifier
    private var isInitialized = false
    
    constructor(context: Context) {
        tinyBertClassifier = TinyBertClassifier(context)
        fallbackClassifier = ActivityClassifier()
    }
    
    /**
     * Initialize the enhanced classifier asynchronously.
     */
    fun initializeAsync(onComplete: (Boolean) -> Unit) {
        Thread {
            val success = tinyBertClassifier.initialize()
            isInitialized = success
            
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onComplete(success)
            }
        }.start()
    }
    
    /**
     * Predict category using TinyBERT with fallback to rule-based system.
     */
    fun predictCategory(activity: String): Prediction {
        Log.d(TAG, "Predicting category for: '$activity'")
        
        // Try TinyBERT first if available
        if (isInitialized && tinyBertClassifier.isInitialized()) {
            val bertResult = tinyBertClassifier.predict(activity)
            
            if (bertResult != null && bertResult.confidence >= TinyBertClassifier.CONFIDENCE_THRESHOLD) {
                Log.d(TAG, "High confidence TinyBERT prediction: ${bertResult.category} (${bertResult.confidence})")
                return Prediction(
                    category = bertResult.category,
                    confidence = bertResult.confidence,
                    iconRes = bertResult.iconRes,
                    source = "tinybert_high_confidence"
                )
            }
            
            if (bertResult != null && bertResult.confidence >= TinyBertClassifier.LOW_CONFIDENCE_THRESHOLD) {
                Log.d(TAG, "Medium confidence TinyBERT prediction: ${bertResult.category} (${bertResult.confidence})")
                return Prediction(
                    category = bertResult.category,
                    confidence = bertResult.confidence,
                    iconRes = bertResult.iconRes,
                    source = "tinybert_medium_confidence"
                )
            }
            
            Log.d(TAG, "TinyBERT prediction below threshold, falling back to rule-based system")
        }
        
        // Fallback to original rule-based classifier
        val fallbackResult = fallbackClassifier.predictCategory(activity)
        Log.d(TAG, "Fallback prediction: ${fallbackResult.category} (${fallbackResult.confidence})")
        
        return Prediction(
            category = fallbackResult.category,
            confidence = fallbackResult.confidence,
            iconRes = fallbackResult.iconRes,
            source = "rule_based_fallback"
        )
    }
    
    /**
     * Get detailed performance and status information.
     */
    fun getStatus(): Map<String, Any> {
        val bertStats = if (tinyBertClassifier.isInitialized()) {
            tinyBertClassifier.getModelStats()
        } else {
            mapOf("initialized" to false)
        }
        
        return mapOf(
            "enhanced_classifier_initialized" to isInitialized,
            "tinybert_stats" to bertStats,
            "last_inference_time_ms" to tinyBertClassifier.getLastInferenceTime()
        )
    }
    
    /**
     * Clean up resources.
     */
    fun cleanup() {
        tinyBertClassifier.cleanup()
    }
}