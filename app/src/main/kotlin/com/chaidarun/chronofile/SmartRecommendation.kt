package com.chaidarun.chronofile

data class SmartRecommendation(
    val id: String,
    val type: RecommendationType,
    val title: String,
    val message: String,
    val actionText: String,
    val eventBasedCue: String?, // "After morning coffee" vs time-based
    val twoMinuteVersion: String?, // Micro-habit alternative
    val confidenceScore: Float,
    val celebratesProgress: Boolean = false,
    val suggestedActivity: String? = null,
    val priority: Int = 1 // 1 = highest
)

enum class RecommendationType {
    HABIT_BUILDING,
    LIFE_BALANCE,
    PROGRESS_CELEBRATION,
    GENTLE_NUDGE,
    CORRELATION_INSIGHT
}

data class RecommendationContext(
    val currentTime: Long,
    val dayOfWeek: Int,
    val recentActivities: List<String>,
    val userPreferences: Map<String, Any> = emptyMap()
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val isNewlyEarned: Boolean,
    val celebrationMessage: String,
    val category: LifeCategory? = null,
    val earnedAt: Long = epochSeconds(),
    val isLocked: Boolean = false,
    val unlockCriteria: String? = null,
    val progressText: String? = null
)

object MotivationFraming {
    
    fun frameRecommendation(
        type: RecommendationType,
        activity: String,
        context: RecommendationContext? = null
    ): String {
        return when (type) {
            RecommendationType.HABIT_BUILDING -> 
                "Building a more balanced life: try ${activity.lowercase()}"
            RecommendationType.LIFE_BALANCE -> 
                "Nurturing your ${getCategoryName(activity)} side"
            RecommendationType.PROGRESS_CELEBRATION -> 
                "Celebrating your growth in ${activity.lowercase()}"
            RecommendationType.GENTLE_NUDGE -> 
                "A gentle reminder for your wellbeing"
            RecommendationType.CORRELATION_INSIGHT -> 
                "Discovering patterns in your personal journey"
        }
    }
    
    private fun getCategoryName(activity: String): String {
        return when (mapActivityToLifeCategory(activity)) {
            LifeCategory.HEALTH_FITNESS -> "health & fitness"
            LifeCategory.WORK_CAREER -> "professional growth"
            LifeCategory.RELATIONSHIPS_FAMILY -> "relationships"
            LifeCategory.LEARNING_GROWTH -> "personal development"
            LifeCategory.HOBBIES_CREATIVITY -> "creative"
            LifeCategory.REST_RELAXATION -> "wellness"
            LifeCategory.PERSONAL_CARE -> "self-care"
            LifeCategory.OTHER -> "personal"
        }
    }
}