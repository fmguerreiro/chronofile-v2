// © Art Chaidarun

package com.chaidarun.chronofile

import androidx.annotation.DrawableRes

data class IconEntry(
    @DrawableRes val iconRes: Int,
    val keywords: List<String>,
    val category: String = "general"
)

object IconDatabase {
    
    private val iconData = listOf(
        // Work & Professional
        IconEntry(R.drawable.ic_briefcase_line, listOf("work", "job", "office", "business", "professional", "career", "employment")),
        IconEntry(R.drawable.ic_computer, listOf("coding", "programming", "computer", "development", "software", "tech", "laptop")),
        IconEntry(R.drawable.ic_chart, listOf("analysis", "data", "chart", "report", "statistics", "analytics")),
        IconEntry(R.drawable.ic_trending_up, listOf("growth", "progress", "improvement", "success", "increase")),
        IconEntry(R.drawable.ic_note, listOf("writing", "note", "document", "planning", "task", "todo", "admin")),
        IconEntry(R.drawable.ic_people, listOf("meeting", "team", "collaboration", "group", "discussion", "conference")),
        IconEntry(R.drawable.ic_phone, listOf("phone", "call", "contact", "communication", "telephone")),
        IconEntry(R.drawable.ic_email, listOf("email", "mail", "message", "correspondence", "communication")),
        IconEntry(R.drawable.ic_target, listOf("goal", "target", "objective", "focus", "aim", "achievement")),
        
        // Food & Meals
        IconEntry(R.drawable.ic_utensils_line, listOf("meal", "eat", "food", "dining", "lunch", "dinner", "breakfast")),
        IconEntry(R.drawable.ic_coffee, listOf("coffee", "break", "caffeine", "morning", "beverage", "drink")),
        IconEntry(R.drawable.ic_food, listOf("sandwich", "lunch", "snack", "quick meal")),
        IconEntry(R.drawable.ic_food, listOf("pizza", "dinner", "takeout", "casual dining")),
        IconEntry(R.drawable.ic_food, listOf("salad", "healthy", "vegetables", "diet", "nutrition")),
        IconEntry(R.drawable.ic_food, listOf("cooking", "breakfast", "preparation", "kitchen", "chef")),
        
        // Exercise & Health
        IconEntry(R.drawable.ic_run, listOf("exercise", "running", "fitness", "cardio", "workout", "sport", "training")),
        IconEntry(R.drawable.ic_dumbbell, listOf("gym", "weightlifting", "strength", "muscle", "fitness")),
        IconEntry(R.drawable.ic_bike, listOf("cycling", "bike", "cardio", "outdoor", "commute")),
        IconEntry(R.drawable.ic_swim, listOf("swim", "swimming", "swimm", "pool", "water", "laps", "freestyle", "backstroke")),
        IconEntry(R.drawable.ic_meditation, listOf("meditation", "mindfulness", "relaxation", "zen", "wellness")),
        IconEntry(R.drawable.ic_health, listOf("health", "doctor", "medical", "checkup", "appointment")),
        IconEntry(R.drawable.ic_health, listOf("medicine", "medication", "pills", "treatment", "health")),
        
        // Transportation
        IconEntry(R.drawable.ic_car, listOf("drive", "car", "commute", "travel", "transportation", "vehicle")),
        IconEntry(R.drawable.ic_car, listOf("bus", "public transport", "commute", "transit")),
        IconEntry(R.drawable.ic_car, listOf("tram", "train", "metro", "subway", "rail")),
        IconEntry(R.drawable.ic_car, listOf("flight", "airplane", "travel", "trip", "vacation")),
        IconEntry(R.drawable.ic_run, listOf("walk", "walking", "pedestrian", "stroll", "exercise")),
        
        // Rest & Leisure
        IconEntry(R.drawable.ic_moon_line, listOf("sleep", "rest", "nap", "tired", "bed", "night")),
        IconEntry(R.drawable.ic_relax, listOf("relax", "chill", "rest", "lounge", "comfort", "leisure")),
        IconEntry(R.drawable.ic_tv, listOf("tv", "television", "watch", "entertainment", "show", "movie")),
        IconEntry(R.drawable.ic_note, listOf("read", "book", "study", "learn", "education", "literature")),
        IconEntry(R.drawable.ic_game, listOf("gaming", "game", "play", "entertainment", "video game")),
        IconEntry(R.drawable.ic_music, listOf("music", "listen", "song", "audio", "entertainment")),
        
        // Social & Relationships
        IconEntry(R.drawable.ic_people, listOf("social", "friends", "relationship", "together", "people")),
        IconEntry(R.drawable.ic_people, listOf("family", "kids", "children", "parents", "home")),
        IconEntry(R.drawable.ic_chat, listOf("chat", "conversation", "talk", "communication", "discussion")),
        IconEntry(R.drawable.ic_star, listOf("party", "celebration", "fun", "event", "festive")),
        IconEntry(R.drawable.ic_drink, listOf("drinks", "social", "bar", "alcohol", "evening")),
        
        // Household & Personal
        IconEntry(R.drawable.ic_home, listOf("home", "house", "personal", "domestic", "family")),
        IconEntry(R.drawable.ic_cleaning, listOf("cleaning", "chores", "housework", "tidy", "maintenance")),
        IconEntry(R.drawable.ic_shopping, listOf("shopping", "purchase", "buy", "store", "retail")),
        IconEntry(R.drawable.ic_cleaning, listOf("laundry", "washing", "clothes", "chores")),
        IconEntry(R.drawable.ic_calendar, listOf("repair", "fix", "maintenance", "tools", "diy")),
        IconEntry(R.drawable.ic_sprout, listOf("gardening", "plants", "nature", "outdoor", "hobby")),
        
        // Personal Care
        IconEntry(R.drawable.ic_hygiene, listOf("shower", "hygiene", "morning", "routine", "personal care")),
        IconEntry(R.drawable.ic_hygiene, listOf("brush teeth", "dental", "hygiene", "morning", "night")),
        IconEntry(R.drawable.ic_hygiene, listOf("makeup", "beauty", "grooming", "personal care")),
        IconEntry(R.drawable.ic_hygiene, listOf("haircut", "grooming", "salon", "personal care")),
        
        // Education & Learning
        IconEntry(R.drawable.ic_star, listOf("education", "graduation", "achievement", "learning", "school")),
        IconEntry(R.drawable.ic_note, listOf("textbook", "study", "research", "academic", "learning")),
        IconEntry(R.drawable.ic_note, listOf("write", "homework", "assignment", "notes", "study")),
        IconEntry(R.drawable.ic_calendar, listOf("science", "research", "experiment", "laboratory", "analysis")),
        
        // Emergency & Important
        IconEntry(R.drawable.ic_calendar, listOf("urgent", "emergency", "important", "alert", "crisis")),
        IconEntry(R.drawable.ic_calendar, listOf("energy", "power", "quick", "fast", "intense")),
        IconEntry(R.drawable.ic_fire, listOf("hot", "intense", "urgent", "priority", "important")),
        
        // Weather & Seasons
        IconEntry(R.drawable.ic_sun_line, listOf("sunny", "bright", "day", "weather", "outdoor")),
        IconEntry(R.drawable.ic_calendar, listOf("rain", "weather", "indoor", "stay in")),
        IconEntry(R.drawable.ic_calendar, listOf("cold", "winter", "snow", "weather")),
        
        // Default fallbacks
        IconEntry(R.drawable.ic_note, listOf("other", "misc", "general", "activity", "task")),
        IconEntry(R.drawable.ic_schedule, listOf("time", "schedule", "appointment", "calendar"))
    )
    
    // Create keyword to icon mapping for faster lookup
    private val keywordMap = mutableMapOf<String, Int>().apply {
        iconData.forEach { entry ->
            entry.keywords.forEach { keyword ->
                // Store both original and lemmatized versions
                this[keyword.lowercase()] = entry.iconRes
                this[keyword.lemmatize()] = entry.iconRes
            }
        }
    }
    
    @DrawableRes
    fun findByKeyword(keyword: String): Int {
        val normalized = keyword.lemmatize()
        return keywordMap[normalized] ?: findBestMatch(normalized)
    }
    
    @DrawableRes
    private fun findBestMatch(text: String): Int {
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
            R.drawable.ic_note // Default fallback
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