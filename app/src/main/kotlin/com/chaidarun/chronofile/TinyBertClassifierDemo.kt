// © Art Chaidarun

package com.chaidarun.chronofile

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import kotlin.math.exp
import kotlin.random.Random

/**
 * Demo TinyBERT-based activity classifier that simulates ML predictions.
 * This demonstrates the integration architecture without requiring the actual TensorFlow Lite model.
 */
class TinyBertClassifierDemo(private val context: Context) {
    
    companion object {
        private const val TAG = "TinyBertDemo"
        private const val LABEL_ENCODER_FILE = "label_encoder.json"
        private const val TOKENIZER_CONFIG_FILE = "tokenizer_config.json"
        
        // Model constants
        const val CONFIDENCE_THRESHOLD = 0.7f
        const val LOW_CONFIDENCE_THRESHOLD = 0.3f
    }
    
    data class ClassificationResult(
        val category: String,
        val confidence: Float,
        val iconRes: Int,
        val allProbabilities: Map<String, Float> = emptyMap()
    )
    
    data class LabelEncoder(
        val classes: List<String>,
        val category_to_id: Map<String, Int>
    )
    
    data class TokenizerConfig(
        val vocab_size: Int,
        val max_length: Int,
        val pad_token: String,
        val pad_token_id: Int,
        val unk_token: String,
        val unk_token_id: Int
    )
    
    // Model components
    private var labelEncoder: LabelEncoder? = null
    private var tokenConfig: TokenizerConfig? = null
    private var categoryToIcon: Map<String, Int> = emptyMap()
    private var initializationFailed = false
    
    // Demo ML patterns - keywords that strongly indicate categories
    private val categoryPatterns = mapOf(
        "exercise" to listOf("run", "gym", "workout", "exercise", "walk", "bike", "swim", "yoga", "sport", "training"),
        "work" to listOf("work", "meeting", "office", "project", "job", "business", "call", "email", "presentation"),
        "food" to listOf("eat", "food", "lunch", "dinner", "breakfast", "coffee", "meal", "cooking", "restaurant"),
        "sleep" to listOf("sleep", "rest", "nap", "bed", "tired", "night", "dream"),
        "social" to listOf("friends", "family", "party", "social", "visit", "chat", "date", "hangout"),
        "learning" to listOf("study", "read", "book", "learn", "education", "school", "research", "course"),
        "entertainment" to listOf("movie", "tv", "watch", "game", "music", "entertainment", "fun", "show"),
        "health" to listOf("doctor", "medical", "health", "appointment", "medicine", "therapy", "hospital"),
        "travel" to listOf("travel", "trip", "drive", "flight", "vacation", "journey", "commute")
    )
    
    /**
     * Initialize the demo classifier.
     */
    fun initialize(): Boolean {
        if (initializationFailed) {
            return false
        }
        
        try {
            Log.i(TAG, "Initializing TinyBERT demo classifier...")
            val startTime = System.currentTimeMillis()
            
            // Load configuration files
            loadLabelEncoder()
            loadTokenizerConfig()
            initializeCategoryMapping()
            
            val initTime = System.currentTimeMillis() - startTime
            Log.i(TAG, "TinyBERT demo classifier initialized successfully in ${initTime}ms")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TinyBERT demo classifier", e)
            initializationFailed = true
            return false
        }
    }
    
    /**
     * Simulate ML prediction using pattern matching and confidence scoring.
     */
    fun predict(activityText: String): ClassificationResult? {
        if (!isInitialized()) {
            Log.w(TAG, "Classifier not initialized, cannot predict")
            return null
        }
        
        try {
            val startTime = System.currentTimeMillis()
            val normalizedText = activityText.lowercase().trim()
            
            // Simulate ML processing with pattern-based scoring
            val categoryScores = calculateCategoryScores(normalizedText)
            val probabilities = softmax(categoryScores.values.toFloatArray())
            
            // Find best prediction
            val categories = labelEncoder?.classes ?: return null
            val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
            val bestCategory = categories[maxIndex]
            val confidence = probabilities[maxIndex]
            
            // Create probability map
            val allProbs = categories.mapIndexed { index, category ->
                category to probabilities[index]
            }.toMap()
            
            val inferenceTime = System.currentTimeMillis() - startTime
            
            val result = ClassificationResult(
                category = bestCategory,
                confidence = confidence,
                iconRes = categoryToIcon[bestCategory] ?: R.drawable.ic_note,
                allProbabilities = allProbs
            )
            
            Log.d(TAG, "Demo prediction completed in ${inferenceTime}ms: ${result.category} (${result.confidence})")
            return result
            
        } catch (e: Exception) {
            Log.e(TAG, "Demo prediction failed for text: '$activityText'", e)
            return null
        }
    }
    
    /**
     * Calculate category scores based on keyword patterns (simulates ML inference).
     */
    private fun calculateCategoryScores(text: String): Map<String, Float> {
        val scores = mutableMapOf<String, Float>()
        
        for ((category, patterns) in categoryPatterns) {
            var categoryScore = 0.0f
            
            // Check for exact matches
            for (pattern in patterns) {
                if (text.contains(pattern)) {
                    categoryScore += 1.0f
                    // Bonus for multiple matches
                    val count = text.split(pattern).size - 1
                    if (count > 1) categoryScore += 0.5f * (count - 1)
                }
            }
            
            // Add some contextual bonuses
            categoryScore += getContextualBonus(text, category)
            
            // Add small random noise to simulate ML uncertainty
            categoryScore += Random.nextFloat() * 0.1f - 0.05f
            
            scores[category] = categoryScore
        }
        
        return scores
    }
    
    /**
     * Add contextual bonuses based on common phrases.
     */
    private fun getContextualBonus(text: String, category: String): Float {
        return when (category) {
            "work" -> {
                when {
                    text.contains("meeting") || text.contains("standup") -> 0.3f
                    text.contains("project") || text.contains("client") -> 0.2f
                    text.contains("email") || text.contains("call") -> 0.2f
                    else -> 0.0f
                }
            }
            "food" -> {
                when {
                    text.contains("lunch with") -> 0.1f // Could be social
                    text.contains("dinner with") -> 0.1f // Could be social  
                    text.contains("cooking") || text.contains("prep") -> 0.3f
                    else -> 0.0f
                }
            }
            "social" -> {
                when {
                    text.contains("with friends") || text.contains("with family") -> 0.4f
                    text.contains("party") || text.contains("celebration") -> 0.3f
                    text.contains("lunch with") || text.contains("dinner with") -> 0.2f
                    else -> 0.0f
                }
            }
            "exercise" -> {
                when {
                    text.matches(Regex(".*\\d+\\s*(km|miles|steps|reps|lbs|kg).*")) -> 0.3f
                    text.contains("morning") && (text.contains("run") || text.contains("walk")) -> 0.2f
                    else -> 0.0f
                }
            }
            else -> 0.0f
        }
    }
    
    /**
     * Convert scores to probabilities using softmax.
     */
    private fun softmax(scores: FloatArray): FloatArray {
        val maxScore = scores.maxOrNull() ?: 0f
        val exponentials = scores.map { exp((it - maxScore).toDouble()).toFloat() }
        val sum = exponentials.sum()
        
        return if (sum > 0) {
            exponentials.map { it / sum }.toFloatArray()
        } else {
            FloatArray(scores.size) { 1f / scores.size } // Uniform distribution
        }
    }
    
    /**
     * Check if the classifier is properly initialized.
     */
    fun isInitialized(): Boolean {
        return labelEncoder != null && 
               tokenConfig != null && 
               categoryToIcon.isNotEmpty() &&
               !initializationFailed
    }
    
    /**
     * Get model performance statistics.
     */
    fun getModelStats(): Map<String, Any> {
        return mapOf(
            "initialized" to isInitialized(),
            "categories" to (labelEncoder?.classes?.size ?: 0),
            "demo_mode" to true,
            "patterns_loaded" to categoryPatterns.size
        )
    }
    
    // Private helper methods
    
    private fun loadLabelEncoder() {
        try {
            val encoderJson = loadAssetAsString(LABEL_ENCODER_FILE)
            labelEncoder = Gson().fromJson(encoderJson, LabelEncoder::class.java)
            Log.d(TAG, "Label encoder loaded: ${labelEncoder?.classes?.size} categories")
        } catch (e: Exception) {
            throw Exception("Failed to load label encoder", e)
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
    
    private fun initializeCategoryMapping() {
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
}

/**
 * Enhanced ActivityClassifier that integrates TinyBERT demo with the existing system.
 */
class EnhancedActivityClassifier {
    
    companion object {
        private const val TAG = "EnhancedActivityClassifier"
    }
    
    data class Prediction(
        val category: String,
        val confidence: Float,
        val iconRes: Int,
        val source: String = "unknown"
    )
    
    private val tinyBertDemo: TinyBertClassifierDemo
    private val fallbackClassifier: ActivityClassifier
    private var isInitialized = false
    
    constructor(context: Context) {
        tinyBertDemo = TinyBertClassifierDemo(context)
        fallbackClassifier = ActivityClassifier()
    }
    
    /**
     * Initialize the enhanced classifier asynchronously.
     */
    fun initializeAsync(onComplete: (Boolean) -> Unit) {
        Thread {
            val success = tinyBertDemo.initialize()
            isInitialized = success
            
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onComplete(success)
            }
        }.start()
    }
    
    /**
     * Predict category using TinyBERT demo with fallback to rule-based system.
     */
    fun predictCategory(activity: String): Prediction {
        Log.d(TAG, "Predicting category for: '$activity'")
        
        // Try TinyBERT demo first if available
        if (isInitialized && tinyBertDemo.isInitialized()) {
            val bertResult = tinyBertDemo.predict(activity)
            
            if (bertResult != null && bertResult.confidence >= TinyBertClassifierDemo.CONFIDENCE_THRESHOLD) {
                Log.d(TAG, "High confidence demo prediction: ${bertResult.category} (${bertResult.confidence})")
                return Prediction(
                    category = bertResult.category,
                    confidence = bertResult.confidence,
                    iconRes = bertResult.iconRes,
                    source = "tinybert_demo_high_confidence"
                )
            }
            
            if (bertResult != null && bertResult.confidence >= TinyBertClassifierDemo.LOW_CONFIDENCE_THRESHOLD) {
                Log.d(TAG, "Medium confidence demo prediction: ${bertResult.category} (${bertResult.confidence})")
                return Prediction(
                    category = bertResult.category,
                    confidence = bertResult.confidence,
                    iconRes = bertResult.iconRes,
                    source = "tinybert_demo_medium_confidence"
                )
            }
            
            Log.d(TAG, "Demo prediction below threshold, falling back to rule-based system")
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
        val bertStats = if (tinyBertDemo.isInitialized()) {
            tinyBertDemo.getModelStats()
        } else {
            mapOf("initialized" to false)
        }
        
        return mapOf(
            "enhanced_classifier_initialized" to isInitialized,
            "tinybert_demo_stats" to bertStats
        )
    }
}