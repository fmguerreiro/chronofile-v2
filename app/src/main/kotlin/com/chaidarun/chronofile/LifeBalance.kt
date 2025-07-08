package com.chaidarun.chronofile

data class LifeBalanceMetrics(
    val categories: Map<LifeCategory, CategoryMetrics>,
    val overallBalance: Float, // 0-1 score
    val improvementAreas: List<String>,
    val strengths: List<String>,
    val weeklyTotalHours: Float
)

enum class LifeCategory(val displayName: String) {
    HEALTH_FITNESS("Health & Fitness"),
    RELATIONSHIPS_FAMILY("Relationships & Family"),
    LEARNING_GROWTH("Learning & Growth"),
    HOBBIES_CREATIVITY("Hobbies & Creativity"),
    REST_RELAXATION("Rest & Relaxation"),
    PERSONAL_CARE("Personal Care"),
    WORK_CAREER("Work & Career"),
    OTHER("Other")
}

data class CategoryMetrics(
    val weeklyHours: Float,
    val consistencyScore: Float, // 0-1 - how evenly distributed across days
    val trend: BalanceTrend, // IMPROVING, STABLE, DECLINING
    val personalBest: Float, // Best weekly hours for this category
    val percentage: Float // Percentage of total time
)

enum class BalanceTrend {
    IMPROVING,
    STABLE,
    DECLINING
}

fun mapActivityToLifeCategory(activity: String): LifeCategory {
    val activityLower = activity.lowercase()
    
    return when {
        activityLower.contains("exercise") || activityLower.contains("gym") || 
        activityLower.contains("workout") || activityLower.contains("walk") ||
        activityLower.contains("sport") || activityLower.contains("run") ||
        activityLower.contains("bike") || activityLower.contains("yoga") -> 
            LifeCategory.HEALTH_FITNESS
            
        activityLower.contains("family") || activityLower.contains("friends") ||
        activityLower.contains("social") || activityLower.contains("date") ||
        activityLower.contains("call") || activityLower.contains("visit") ->
            LifeCategory.RELATIONSHIPS_FAMILY
            
        activityLower.contains("learn") || activityLower.contains("study") ||
        activityLower.contains("read") || activityLower.contains("course") ||
        activityLower.contains("tutorial") || activityLower.contains("research") ->
            LifeCategory.LEARNING_GROWTH
            
        activityLower.contains("hobby") || activityLower.contains("music") ||
        activityLower.contains("art") || activityLower.contains("craft") ||
        activityLower.contains("creative") || activityLower.contains("draw") ||
        activityLower.contains("paint") || activityLower.contains("game") ->
            LifeCategory.HOBBIES_CREATIVITY
            
        activityLower.contains("sleep") || activityLower.contains("rest") ||
        activityLower.contains("relax") || activityLower.contains("break") ||
        activityLower.contains("nap") || activityLower.contains("meditat") ->
            LifeCategory.REST_RELAXATION
            
        activityLower.contains("shower") || activityLower.contains("grooming") ||
        activityLower.contains("breakfast") || activityLower.contains("lunch") ||
        activityLower.contains("dinner") || activityLower.contains("meal") ||
        activityLower.contains("eat") || activityLower.contains("personal") ->
            LifeCategory.PERSONAL_CARE
            
        activityLower.contains("work") || activityLower.contains("meeting") ||
        activityLower.contains("email") || activityLower.contains("coding") ||
        activityLower.contains("project") || activityLower.contains("office") ||
        activityLower.contains("job") || activityLower.contains("career") ->
            LifeCategory.WORK_CAREER
            
        else -> LifeCategory.OTHER
    }
}