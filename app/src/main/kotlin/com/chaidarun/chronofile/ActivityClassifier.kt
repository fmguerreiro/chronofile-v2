// © Art Chaidarun

package com.chaidarun.chronofile

import kotlin.math.*

/**
 * Machine learning-based activity classifier that predicts activity categories
 * from text input using a simple but effective feature-based approach.
 */
class ActivityClassifier {
    
    data class Prediction(
        val category: String,
        val confidence: Float,
        val iconRes: Int
    )
    
    data class CategoryModel(
        val name: String,
        val iconRes: Int,
        val keywords: Set<String>,
        val characterFeatures: Set<String>,
        val weights: Map<String, Double>
    )
    
    // Pre-trained category models with weights learned from common activity patterns
    private val categoryModels = listOf(
        CategoryModel(
            name = "exercise",
            iconRes = R.drawable.ic_run,
            keywords = setOf("exercise", "workout", "gym", "fitness", "sport", "training", "run", "walk", "bike", "swim", "yoga", "stretch", "jog", "hike", "dance", "pilates", "cardio", "weights", "boxing", "martial", "tennis", "football", "basketball", "soccer", "baseball", "golf", "skiing", "climbing", "rowing", "cycling"),
            characterFeatures = setOf("movement", "physical", "active", "athletic", "outdoor"),
            weights = mapOf(
                "exact_match" to 1.0,
                "substring_match" to 0.7,
                "character_features" to 0.5,
                "length_penalty" to -0.1
            )
        ),
        CategoryModel(
            name = "work",
            iconRes = R.drawable.ic_briefcase_line,
            keywords = setOf("work", "job", "office", "meeting", "project", "task", "business", "career", "employment", "professional", "client", "deadline", "presentation", "email", "call", "conference", "coding", "programming", "development", "design", "writing", "analysis", "research", "planning", "strategy", "management", "admin", "paperwork", "document", "report", "contract", "agreement", "legal", "negotiation", "deal", "proposal", "review", "audit", "compliance"),
            characterFeatures = setOf("professional", "productive", "business", "technical", "corporate"),
            weights = mapOf(
                "exact_match" to 1.0,
                "substring_match" to 0.8,
                "character_features" to 0.6,
                "length_penalty" to -0.05
            )
        ),
        CategoryModel(
            name = "food",
            iconRes = R.drawable.ic_utensils_line,
            keywords = setOf("food", "eat", "meal", "lunch", "dinner", "breakfast", "snack", "cooking", "kitchen", "restaurant", "cafe", "coffee", "tea", "drink", "water", "juice", "smoothie", "pizza", "burger", "salad", "sandwich", "soup", "pasta", "rice", "bread", "fruit", "vegetable", "meat", "fish", "dessert", "cake", "chocolate"),
            characterFeatures = setOf("consumption", "culinary", "nutrition", "dining", "beverage"),
            weights = mapOf(
                "exact_match" to 1.0,
                "substring_match" to 0.9,
                "character_features" to 0.7,
                "length_penalty" to -0.02
            )
        ),
        CategoryModel(
            name = "sleep",
            iconRes = R.drawable.ic_moon_line,
            keywords = setOf("sleep", "rest", "nap", "bed", "tired", "relax", "night", "dream", "pillow", "bedroom", "wake", "alarm", "morning", "evening", "bedtime", "drowsy", "exhausted", "recharge", "recover", "peaceful", "quiet", "dark"),
            characterFeatures = setOf("restful", "recovery", "nighttime", "peaceful", "relaxation"),
            weights = mapOf(
                "exact_match" to 1.0,
                "substring_match" to 0.8,
                "character_features" to 0.6,
                "length_penalty" to -0.1
            )
        ),
        CategoryModel(
            name = "social",
            iconRes = R.drawable.ic_people,
            keywords = setOf("social", "friends", "family", "people", "party", "gathering", "visit", "chat", "talk", "date", "hangout", "dinner", "lunch", "coffee", "drinks", "celebration", "wedding", "birthday", "event", "community", "group", "team", "colleague", "neighbor", "conversation", "relationship"),
            characterFeatures = setOf("interpersonal", "community", "relationship", "gathering", "interactive"),
            weights = mapOf(
                "exact_match" to 1.0,
                "substring_match" to 0.7,
                "character_features" to 0.5,
                "length_penalty" to -0.08
            )
        ),
        CategoryModel(
            name = "learning",
            iconRes = R.drawable.ic_note,
            keywords = setOf("study", "learn", "read", "book", "education", "school", "university", "course", "research", "homework", "assignment", "exam", "test", "lecture", "class", "lesson", "tutorial", "workshop", "seminar", "training", "skill", "knowledge", "practice", "review", "analyze", "understand"),
            characterFeatures = setOf("educational", "intellectual", "academic", "cognitive", "informational"),
            weights = mapOf(
                "exact_match" to 1.0,
                "substring_match" to 0.8,
                "character_features" to 0.6,
                "length_penalty" to -0.05
            )
        ),
        CategoryModel(
            name = "entertainment",
            iconRes = R.drawable.ic_tv,
            keywords = setOf("entertainment", "tv", "movie", "film", "show", "video", "game", "play", "fun", "leisure", "music", "listen", "watch", "streaming", "netflix", "youtube", "podcast", "radio", "concert", "theater", "comedy", "drama", "action", "adventure", "hobby", "relaxation"),
            characterFeatures = setOf("recreational", "leisure", "enjoyment", "media", "amusing"),
            weights = mapOf(
                "exact_match" to 1.0,
                "substring_match" to 0.7,
                "character_features" to 0.5,
                "length_penalty" to -0.06
            )
        ),
        CategoryModel(
            name = "health",
            iconRes = R.drawable.ic_health,
            keywords = setOf("health", "medical", "doctor", "hospital", "medicine", "therapy", "wellness", "checkup", "appointment", "dentist", "pharmacy", "treatment", "medication", "surgery", "clinic", "nurse", "patient", "symptoms", "diagnosis", "recovery", "healing", "prevention"),
            characterFeatures = setOf("medical", "wellness", "therapeutic", "clinical", "healthcare"),
            weights = mapOf(
                "exact_match" to 1.0,
                "substring_match" to 0.9,
                "character_features" to 0.7,
                "length_penalty" to -0.03
            )
        ),
        CategoryModel(
            name = "travel",
            iconRes = R.drawable.ic_car,
            keywords = setOf("travel", "trip", "journey", "drive", "flight", "train", "bus", "commute", "transport", "vacation", "holiday", "airport", "station", "road", "highway", "traffic", "destination", "explore", "adventure", "sightseeing", "tourism", "hotel", "booking"),
            characterFeatures = setOf("movement", "transportation", "journey", "exploration", "mobility"),
            weights = mapOf(
                "exact_match" to 1.0,
                "substring_match" to 0.8,
                "character_features" to 0.6,
                "length_penalty" to -0.07
            )
        )
    )
    
    /**
     * Predicts the most likely category for a given activity text.
     * Uses feature extraction and weighted scoring for classification.
     */
    fun predictCategory(activity: String): Prediction {
        val normalizedActivity = activity.lowercase().trim()
        val features = extractFeatures(normalizedActivity)
        
        var bestMatch: Prediction? = null
        var highestScore = 0.0
        
        for (model in categoryModels) {
            val score = calculateScore(features, model)
            if (score > highestScore) {
                highestScore = score
                bestMatch = Prediction(
                    category = model.name,
                    confidence = score.toFloat().coerceIn(0f, 1f),
                    iconRes = model.iconRes
                )
            }
        }
        
        // Return best match or default fallback
        return bestMatch?.takeIf { it.confidence > 0.3f } 
            ?: Prediction("general", 0.1f, R.drawable.ic_note)
    }
    
    /**
     * Extracts relevant features from activity text for classification.
     */
    private fun extractFeatures(activity: String): Map<String, Any> {
        return mapOf(
            "text" to activity,
            "length" to activity.length,
            "word_count" to activity.split("\\s+".toRegex()).size,
            "has_numbers" to activity.any { it.isDigit() },
            "has_special_chars" to activity.any { !it.isLetterOrDigit() && !it.isWhitespace() },
            "starts_with_verb" to startsWithCommonVerb(activity),
            "contains_time" to containsTimeReference(activity),
            "sentiment" to estimateSentiment(activity)
        )
    }
    
    /**
     * Calculates a weighted score for how well the activity matches a category model.
     */
    private fun calculateScore(features: Map<String, Any>, model: CategoryModel): Double {
        val text = features["text"] as String
        var score = 0.0
        
        // Exact keyword matching
        val exactMatches = model.keywords.count { keyword ->
            text.split("\\s+".toRegex()).any { word -> 
                word.equals(keyword, ignoreCase = true) 
            }
        }
        score += exactMatches * (model.weights["exact_match"] ?: 1.0)
        
        // Substring matching
        val substringMatches = model.keywords.count { keyword ->
            text.contains(keyword, ignoreCase = true)
        }
        score += substringMatches * (model.weights["substring_match"] ?: 0.7)
        
        // Character-based features
        val characterMatches = model.characterFeatures.count { feature ->
            hasCharacteristic(text, feature)
        }
        score += characterMatches * (model.weights["character_features"] ?: 0.5)
        
        // Length penalty for very short or very long activities
        val length = features["length"] as Int
        if (length < 3 || length > 50) {
            score += (model.weights["length_penalty"] ?: -0.1)
        }
        
        // Boost for common activity patterns
        score += getPatternBoost(text, model.name)
        
        // Normalize score
        return (score / model.keywords.size).coerceIn(0.0, 1.0)
    }
    
    /**
     * Checks if text has certain characteristics for semantic matching.
     */
    private fun hasCharacteristic(text: String, feature: String): Boolean {
        return when (feature) {
            "movement", "physical", "active" -> containsMovementWords(text)
            "professional", "business", "corporate" -> containsProfessionalWords(text)
            "consumption", "culinary", "nutrition" -> containsFoodWords(text)
            "restful", "recovery", "peaceful" -> containsRestWords(text)
            "interpersonal", "community", "relationship" -> containsSocialWords(text)
            "educational", "intellectual", "academic" -> containsLearningWords(text)
            "recreational", "leisure", "enjoyment" -> containsEntertainmentWords(text)
            "medical", "wellness", "therapeutic" -> containsHealthWords(text)
            "transportation", "journey", "mobility" -> containsTravelWords(text)
            else -> false
        }
    }
    
    // Helper methods for semantic feature detection
    private fun containsMovementWords(text: String) = 
        listOf("move", "active", "physical", "body", "muscle", "sweat", "energy").any { text.contains(it) }
    
    private fun containsProfessionalWords(text: String) = 
        listOf("professional", "corporate", "business", "formal", "meeting", "client").any { text.contains(it) }
    
    private fun containsFoodWords(text: String) = 
        listOf("taste", "flavor", "hungry", "delicious", "recipe", "ingredient").any { text.contains(it) }
    
    private fun containsRestWords(text: String) = 
        listOf("calm", "peaceful", "quiet", "relax", "tired", "sleepy").any { text.contains(it) }
    
    private fun containsSocialWords(text: String) = 
        listOf("together", "group", "friend", "family", "social", "community").any { text.contains(it) }
    
    private fun containsLearningWords(text: String) = 
        listOf("learn", "study", "understand", "knowledge", "skill", "education").any { text.contains(it) }
    
    private fun containsEntertainmentWords(text: String) = 
        listOf("fun", "enjoy", "entertaining", "amusing", "leisure", "hobby").any { text.contains(it) }
    
    private fun containsHealthWords(text: String) = 
        listOf("healthy", "medical", "doctor", "treatment", "wellness", "care").any { text.contains(it) }
    
    private fun containsTravelWords(text: String) = 
        listOf("travel", "journey", "trip", "destination", "transport", "move").any { text.contains(it) }
    
    private fun startsWithCommonVerb(text: String): Boolean {
        val verbs = setOf("go", "do", "make", "take", "get", "see", "come", "think", "look", "want", "give", "use", "find", "tell", "ask", "work", "seem", "feel", "try", "leave", "call")
        return text.split("\\s+".toRegex()).firstOrNull()?.lowercase() in verbs
    }
    
    private fun containsTimeReference(text: String): Boolean {
        val timeWords = setOf("morning", "afternoon", "evening", "night", "today", "tomorrow", "yesterday", "hour", "minute", "time", "early", "late", "quick", "slow")
        return timeWords.any { text.contains(it, ignoreCase = true) }
    }
    
    private fun estimateSentiment(text: String): String {
        val positiveWords = setOf("good", "great", "excellent", "amazing", "wonderful", "fantastic", "awesome", "love", "enjoy", "happy", "fun", "exciting")
        val negativeWords = setOf("bad", "terrible", "awful", "hate", "boring", "difficult", "hard", "stressful", "tired", "sad", "annoying")
        
        val positiveCount = positiveWords.count { text.contains(it, ignoreCase = true) }
        val negativeCount = negativeWords.count { text.contains(it, ignoreCase = true) }
        
        return when {
            positiveCount > negativeCount -> "positive"
            negativeCount > positiveCount -> "negative"
            else -> "neutral"
        }
    }
    
    private fun getPatternBoost(text: String, category: String): Double {
        // Give extra boost for common patterns specific to each category
        return when (category) {
            "exercise" -> if (text.matches(".*\\d+\\s*(km|miles|steps|reps|sets|lbs|kg).*".toRegex(RegexOption.IGNORE_CASE))) 0.2 else 0.0
            "work" -> if (text.contains("meeting", ignoreCase = true) || text.contains("project", ignoreCase = true)) 0.15 else 0.0
            "food" -> if (text.matches(".*(breakfast|lunch|dinner|snack).*".toRegex(RegexOption.IGNORE_CASE))) 0.25 else 0.0
            "sleep" -> if (text.matches(".*\\d+\\s*(hours?|hrs?).*".toRegex(RegexOption.IGNORE_CASE))) 0.2 else 0.0
            else -> 0.0
        }
    }
}