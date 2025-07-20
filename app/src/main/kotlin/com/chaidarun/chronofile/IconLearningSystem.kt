// © Art Chaidarun

package com.chaidarun.chronofile

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Self-improving icon mapping system that learns from user selections.
 * Tracks manual icon overrides and expands keyword mappings automatically.
 */
class IconLearningSystem(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("icon_learning", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    data class UserIconMapping(
        val activity: String,
        val selectedIcon: Int,
        val timestamp: Long,
        val confidence: Float = 1.0f
    )
    
    data class LearnedKeyword(
        val keyword: String,
        val iconRes: Int,
        val frequency: Int,
        val lastUsed: Long
    )
    
    // Cache for learned mappings
    private var userMappings: MutableList<UserIconMapping> = mutableListOf()
    private var learnedKeywords: MutableMap<String, LearnedKeyword> = mutableMapOf()
    
    init {
        loadLearnings()
    }
    
    /**
     * Records a user's manual icon selection for an activity.
     * This helps the system learn user preferences.
     */
    fun recordUserSelection(activity: String, selectedIcon: Int) {
        val mapping = UserIconMapping(
            activity = activity.lowercase().trim(),
            selectedIcon = selectedIcon,
            timestamp = System.currentTimeMillis()
        )
        
        userMappings.add(mapping)
        
        // Extract keywords from the activity and associate them with the icon
        extractAndLearnKeywords(activity, selectedIcon)
        
        // Persist the learning
        saveLearnings()
        
        android.util.Log.d("IconLearning", "Learned: '$activity' -> $selectedIcon")
    }
    
    /**
     * Gets the learned icon for an activity based on user history.
     * Returns null if no strong learning exists.
     */
    fun getLearnedIcon(activity: String): Int? {
        val normalizedActivity = activity.lowercase().trim()
        
        // Check for exact activity match
        val exactMatch = userMappings
            .filter { it.activity == normalizedActivity }
            .groupBy { it.selectedIcon }
            .maxByOrNull { it.value.size }
            ?.key
        
        if (exactMatch != null) {
            return exactMatch
        }
        
        // Check learned keywords
        val words = normalizedActivity.split("\\s+".toRegex())
        for (word in words) {
            learnedKeywords[word]?.let { learned ->
                if (learned.frequency >= 2) { // Require at least 2 confirmations
                    return learned.iconRes
                }
            }
        }
        
        return null
    }
    
    /**
     * Gets user confidence in the learned mapping for an activity.
     */
    fun getLearningConfidence(activity: String): Float {
        val normalizedActivity = activity.lowercase().trim()
        
        val mappingCount = userMappings.count { it.activity == normalizedActivity }
        val mostFrequentCount = userMappings
            .filter { it.activity == normalizedActivity }
            .groupBy { it.selectedIcon }
            .maxOfOrNull { it.value.size } ?: 0
        
        return if (mappingCount > 0) {
            (mostFrequentCount.toFloat() / mappingCount).coerceIn(0f, 1f)
        } else {
            0f
        }
    }
    
    /**
     * Suggests activities that might benefit from manual icon assignment.
     * Returns activities that frequently use the default icon.
     */
    fun suggestActivitiesForManualMapping(): List<String> {
        // This would analyze the user's activity history to find activities
        // that consistently get the default icon but are used frequently
        return userMappings
            .filter { it.selectedIcon == R.drawable.ic_note }
            .groupBy { it.activity }
            .filter { it.value.size >= 3 } // Used 3+ times with default icon
            .keys
            .toList()
    }
    
    /**
     * Extracts keywords from an activity and learns their association with an icon.
     */
    private fun extractAndLearnKeywords(activity: String, iconRes: Int) {
        val words = activity.lowercase()
            .replace(Regex("[^a-z\\s]"), "") // Remove non-letters
            .split("\\s+".toRegex())
            .filter { it.length > 2 } // Ignore very short words
        
        for (word in words) {
            val current = learnedKeywords[word]
            if (current == null) {
                learnedKeywords[word] = LearnedKeyword(
                    keyword = word,
                    iconRes = iconRes,
                    frequency = 1,
                    lastUsed = System.currentTimeMillis()
                )
            } else if (current.iconRes == iconRes) {
                // Reinforce existing learning
                learnedKeywords[word] = current.copy(
                    frequency = current.frequency + 1,
                    lastUsed = System.currentTimeMillis()
                )
            }
            // If different icon, we don't reinforce (ambiguous keyword)
        }
    }
    
    /**
     * Loads learned mappings from persistent storage.
     */
    private fun loadLearnings() {
        try {
            val mappingsJson = prefs.getString("user_mappings", "[]")
            val keywordsJson = prefs.getString("learned_keywords", "{}")
            
            val mappingsType = object : TypeToken<List<UserIconMapping>>() {}.type
            val keywordsType = object : TypeToken<Map<String, LearnedKeyword>>() {}.type
            
            userMappings = gson.fromJson(mappingsJson, mappingsType) ?: mutableListOf()
            learnedKeywords = gson.fromJson(keywordsJson, keywordsType) ?: mutableMapOf()
            
            // Clean old learnings (older than 6 months)
            val sixMonthsAgo = System.currentTimeMillis() - (6 * 30 * 24 * 60 * 60 * 1000L)
            userMappings.removeAll { it.timestamp < sixMonthsAgo }
            learnedKeywords.values.removeAll { it.lastUsed < sixMonthsAgo }
            
        } catch (e: Exception) {
            android.util.Log.w("IconLearning", "Failed to load learnings: ${e.message}")
            userMappings = mutableListOf()
            learnedKeywords = mutableMapOf()
        }
    }
    
    /**
     * Saves learned mappings to persistent storage.
     */
    private fun saveLearnings() {
        try {
            val mappingsJson = gson.toJson(userMappings)
            val keywordsJson = gson.toJson(learnedKeywords)
            
            prefs.edit()
                .putString("user_mappings", mappingsJson)
                .putString("learned_keywords", keywordsJson)
                .apply()
                
        } catch (e: Exception) {
            android.util.Log.w("IconLearning", "Failed to save learnings: ${e.message}")
        }
    }
    
    /**
     * Exports learned mappings for debugging or manual review.
     */
    fun exportLearnings(): Map<String, Any> {
        return mapOf(
            "user_mappings" to userMappings,
            "learned_keywords" to learnedKeywords,
            "total_mappings" to userMappings.size,
            "unique_activities" to userMappings.map { it.activity }.distinct().size,
            "learned_keywords_count" to learnedKeywords.size
        )
    }
    
    /**
     * Clears all learned mappings (for testing or reset).
     */
    fun clearLearnings() {
        userMappings.clear()
        learnedKeywords.clear()
        prefs.edit().clear().apply()
        android.util.Log.d("IconLearning", "All learnings cleared")
    }
}