// © Art Chaidarun

package com.chaidarun.chronofile

data class EmojiEntry(
    val emoji: String,
    val keywords: List<String>,
    val category: String = "general"
)

object EmojiDatabase {
    
    private val emojiData = listOf(
        // Work & Professional
        EmojiEntry("💼", listOf("work", "job", "office", "business", "professional", "career", "employment")),
        EmojiEntry("💻", listOf("coding", "programming", "computer", "development", "software", "tech", "laptop")),
        EmojiEntry("📊", listOf("analysis", "data", "chart", "report", "statistics", "analytics")),
        EmojiEntry("📈", listOf("growth", "progress", "improvement", "success", "increase")),
        EmojiEntry("📝", listOf("writing", "note", "document", "planning", "task", "todo", "admin")),
        EmojiEntry("👥", listOf("meeting", "team", "collaboration", "group", "discussion", "conference")),
        EmojiEntry("📞", listOf("phone", "call", "contact", "communication", "telephone")),
        EmojiEntry("📧", listOf("email", "mail", "message", "correspondence", "communication")),
        EmojiEntry("🎯", listOf("goal", "target", "objective", "focus", "aim", "achievement")),
        
        // Food & Meals
        EmojiEntry("🍽️", listOf("meal", "eat", "food", "dining", "lunch", "dinner", "breakfast")),
        EmojiEntry("☕", listOf("coffee", "break", "caffeine", "morning", "beverage", "drink")),
        EmojiEntry("🥪", listOf("sandwich", "lunch", "snack", "quick meal")),
        EmojiEntry("🍕", listOf("pizza", "dinner", "takeout", "casual dining")),
        EmojiEntry("🥗", listOf("salad", "healthy", "vegetables", "diet", "nutrition")),
        EmojiEntry("🍳", listOf("cooking", "breakfast", "preparation", "kitchen", "chef")),
        
        // Exercise & Health
        EmojiEntry("🏃", listOf("exercise", "running", "fitness", "cardio", "workout", "sport", "training")),
        EmojiEntry("🏋️", listOf("gym", "weightlifting", "strength", "muscle", "fitness")),
        EmojiEntry("🚴", listOf("cycling", "bike", "cardio", "outdoor", "commute")),
        EmojiEntry("🧘", listOf("meditation", "mindfulness", "relaxation", "zen", "wellness")),
        EmojiEntry("🩺", listOf("health", "doctor", "medical", "checkup", "appointment")),
        EmojiEntry("💊", listOf("medicine", "medication", "pills", "treatment", "health")),
        
        // Transportation
        EmojiEntry("🚗", listOf("drive", "car", "commute", "travel", "transportation", "vehicle")),
        EmojiEntry("🚌", listOf("bus", "public transport", "commute", "transit")),
        EmojiEntry("🚊", listOf("tram", "train", "metro", "subway", "rail")),
        EmojiEntry("✈️", listOf("flight", "airplane", "travel", "trip", "vacation")),
        EmojiEntry("🚶", listOf("walk", "walking", "pedestrian", "stroll", "exercise")),
        
        // Rest & Leisure
        EmojiEntry("😴", listOf("sleep", "rest", "nap", "tired", "bed", "night")),
        EmojiEntry("🛋️", listOf("relax", "chill", "rest", "lounge", "comfort", "leisure")),
        EmojiEntry("📺", listOf("tv", "television", "watch", "entertainment", "show", "movie")),
        EmojiEntry("📚", listOf("read", "book", "study", "learn", "education", "literature")),
        EmojiEntry("🎮", listOf("gaming", "game", "play", "entertainment", "video game")),
        EmojiEntry("🎵", listOf("music", "listen", "song", "audio", "entertainment")),
        
        // Social & Relationships
        EmojiEntry("👫", listOf("social", "friends", "relationship", "together", "people")),
        EmojiEntry("👨‍👩‍👧‍👦", listOf("family", "kids", "children", "parents", "home")),
        EmojiEntry("💬", listOf("chat", "conversation", "talk", "communication", "discussion")),
        EmojiEntry("🎉", listOf("party", "celebration", "fun", "event", "festive")),
        EmojiEntry("🍻", listOf("drinks", "social", "bar", "alcohol", "evening")),
        
        // Household & Personal
        EmojiEntry("🏠", listOf("home", "house", "personal", "domestic", "family")),
        EmojiEntry("🧹", listOf("cleaning", "chores", "housework", "tidy", "maintenance")),
        EmojiEntry("🛍️", listOf("shopping", "purchase", "buy", "store", "retail")),
        EmojiEntry("🧺", listOf("laundry", "washing", "clothes", "chores")),
        EmojiEntry("🔧", listOf("repair", "fix", "maintenance", "tools", "diy")),
        EmojiEntry("🌱", listOf("gardening", "plants", "nature", "outdoor", "hobby")),
        
        // Personal Care
        EmojiEntry("🚿", listOf("shower", "hygiene", "morning", "routine", "personal care")),
        EmojiEntry("🪥", listOf("brush teeth", "dental", "hygiene", "morning", "night")),
        EmojiEntry("💄", listOf("makeup", "beauty", "grooming", "personal care")),
        EmojiEntry("✂️", listOf("haircut", "grooming", "salon", "personal care")),
        
        // Education & Learning
        EmojiEntry("🎓", listOf("education", "graduation", "achievement", "learning", "school")),
        EmojiEntry("📖", listOf("textbook", "study", "research", "academic", "learning")),
        EmojiEntry("✏️", listOf("write", "homework", "assignment", "notes", "study")),
        EmojiEntry("🔬", listOf("science", "research", "experiment", "laboratory", "analysis")),
        
        // Emergency & Important
        EmojiEntry("🚨", listOf("urgent", "emergency", "important", "alert", "crisis")),
        EmojiEntry("⚡", listOf("energy", "power", "quick", "fast", "intense")),
        EmojiEntry("🔥", listOf("hot", "intense", "urgent", "priority", "important")),
        
        // Weather & Seasons
        EmojiEntry("☀️", listOf("sunny", "bright", "day", "weather", "outdoor")),
        EmojiEntry("🌧️", listOf("rain", "weather", "indoor", "stay in")),
        EmojiEntry("❄️", listOf("cold", "winter", "snow", "weather")),
        
        // Default fallbacks
        EmojiEntry("📝", listOf("other", "misc", "general", "activity", "task")),
        EmojiEntry("⏰", listOf("time", "schedule", "appointment", "calendar"))
    )
    
    // Create keyword to emoji mapping for faster lookup
    private val keywordMap = mutableMapOf<String, String>().apply {
        emojiData.forEach { entry ->
            entry.keywords.forEach { keyword ->
                // Store both original and lemmatized versions
                this[keyword.lowercase()] = entry.emoji
                this[keyword.lemmatize()] = entry.emoji
            }
        }
    }
    
    fun findByKeyword(keyword: String): String {
        val normalized = keyword.lemmatize()
        return keywordMap[normalized] ?: findBestMatch(normalized)
    }
    
    private fun findBestMatch(text: String): String {
        // Try partial matches
        val partialMatch = keywordMap.entries.find { (keyword, _) ->
            keyword.contains(text, ignoreCase = true) || text.contains(keyword, ignoreCase = true)
        }
        
        if (partialMatch != null) {
            return partialMatch.value
        }
        
        // Try fuzzy matching with edit distance
        val fuzzyMatch = keywordMap.entries.minByOrNull { (keyword, _) ->
            editDistance(text.lowercase(), keyword.lowercase())
        }
        
        // Only use fuzzy match if it's reasonably close
        return if (fuzzyMatch != null && editDistance(text.lowercase(), fuzzyMatch.key.lowercase()) <= 2) {
            fuzzyMatch.value
        } else {
            "📝" // Default fallback
        }
    }
    
    private fun editDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                dp[i][j] = if (s1[i - 1] == s2[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
                }
            }
        }
        
        return dp[s1.length][s2.length]
    }
}

// Text normalization and lemmatization extension
fun String.lemmatize(): String {
    return this.lowercase()
        .trim()
        .removeSuffix("ing") // walking -> walk
        .removeSuffix("ed")  // worked -> work
        .removeSuffix("er")  // worker -> work
        .removeSuffix("s")   // works -> work
        .replace(Regex("[^a-z]"), "") // Remove non-letters
        .let { normalized ->
            // Handle some common word mappings
            when (normalized) {
                "programme", "program" -> "programming"
                "dev", "develop" -> "development"
                "admin", "administration" -> "administrative"
                "gym", "gymnasium" -> "exercise"
                "bfast" -> "breakfast"
                "commut" -> "commute"
                "workout" -> "exercise"
                "socialis", "hangout" -> "social"
                else -> normalized
            }
        }
}