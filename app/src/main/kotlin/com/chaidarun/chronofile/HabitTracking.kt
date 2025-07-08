package com.chaidarun.chronofile

data class HabitMetrics(
    val activity: String,
    val currentStreak: Int,
    val longestStreak: Int,
    val consistencyScore: Float, // 0-1, allows for skip days
    val weeklyTarget: Int?,
    val weeklyActual: Int,
    val skipDaysUsed: Int,
    val allowedSkipDays: Int = 2, // Forgiveness mechanism
    val trendDirection: TrendDirection,
    val lastActivity: Long? = null
)

enum class TrendDirection {
    IMPROVING, STABLE, DECLINING
}

data class RecoveryState(
    val activity: String,
    val skipDaysRemaining: Int,
    val encouragementMessage: String,
    val adjustedGoal: String? // "Life happened this week - adjust your goals?"
)

// Extension methods for History class
fun History.getHabitConsistencyWithRecovery(
    targetActivities: List<String>
): Map<String, HabitMetrics> {
    return targetActivities.associateWith { activity ->
        calculateHabitMetrics(activity, allowSkipDays = true)
    }
}

fun History.calculateHabitMetrics(
    activity: String,
    allowSkipDays: Boolean = true
): HabitMetrics {
    val activityEntries = entries.filter { 
        it.activity.equals(activity, ignoreCase = true) 
    }.sortedBy { it.startTime }
    
    if (activityEntries.isEmpty()) {
        return HabitMetrics(
            activity = activity,
            currentStreak = 0,
            longestStreak = 0,
            consistencyScore = 0.0f,
            weeklyTarget = null,
            weeklyActual = 0,
            skipDaysUsed = 0,
            trendDirection = TrendDirection.STABLE
        )
    }
    
    val now = epochSeconds()
    val weekStart = now - (7 * DAY_SECONDS)
    val weekEntries = activityEntries.filter { it.startTime >= weekStart }
    
    // Calculate streaks
    val currentStreak = calculateFlexibleStreak(activityEntries, allowSkipDays, now)
    val longestStreak = calculateLongestStreak(activityEntries)
    
    // Weekly metrics
    val weeklyActual = weekEntries.size
    val weeklyTarget = getWeeklyTarget(activity) // TODO: Get from user preferences
    
    // Consistency score (allows for 2 skip days per week)
    val daysWithActivity = weekEntries.map { entry ->
        ((entry.startTime / DAY_SECONDS) % 7).toInt()
    }.toSet().size
    
    val maxPossibleDays = 7
    val allowedSkips = if (allowSkipDays) 2 else 0
    val consistencyScore = kotlin.math.min(1.0f, 
        daysWithActivity.toFloat() / (maxPossibleDays - allowedSkips))
    
    // Calculate skip days used
    val skipDaysUsed = kotlin.math.max(0, maxPossibleDays - daysWithActivity - weeklyActual)
    
    // Trend direction
    val trendDirection = calculateTrendDirection(activityEntries)
    
    return HabitMetrics(
        activity = activity,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        consistencyScore = consistencyScore,
        weeklyTarget = weeklyTarget,
        weeklyActual = weeklyActual,
        skipDaysUsed = skipDaysUsed,
        trendDirection = trendDirection,
        lastActivity = activityEntries.lastOrNull()?.startTime
    )
}

private fun calculateFlexibleStreak(
    entries: List<Entry>, 
    allowSkipDays: Boolean,
    currentTime: Long
): Int {
    if (entries.isEmpty()) return 0
    
    val sortedEntries = entries.sortedByDescending { it.startTime }
    var streak = 0
    var lastActivityDay = -1
    var skipsUsed = 0
    val maxSkips = if (allowSkipDays) 2 else 0
    
    for (entry in sortedEntries) {
        val entryDay = (entry.startTime / DAY_SECONDS).toInt()
        
        if (lastActivityDay == -1) {
            // First entry (most recent)
            lastActivityDay = entryDay
            streak = 1
        } else {
            val dayGap = lastActivityDay - entryDay
            
            when {
                dayGap == 1 -> {
                    // Consecutive day
                    streak++
                    lastActivityDay = entryDay
                }
                dayGap <= maxSkips + 1 && skipsUsed + (dayGap - 1) <= maxSkips -> {
                    // Gap within allowed skips
                    skipsUsed += (dayGap - 1)
                    streak++
                    lastActivityDay = entryDay
                }
                else -> {
                    // Streak broken
                    break
                }
            }
        }
    }
    
    return streak
}

private fun calculateLongestStreak(entries: List<Entry>): Int {
    if (entries.isEmpty()) return 0
    
    val sortedEntries = entries.sortedBy { it.startTime }
    var longestStreak = 1
    var currentStreak = 1
    var lastDay = (sortedEntries.first().startTime / DAY_SECONDS).toInt()
    
    for (i in 1 until sortedEntries.size) {
        val currentDay = (sortedEntries[i].startTime / DAY_SECONDS).toInt()
        val dayDiff = currentDay - lastDay
        
        if (dayDiff == 1) {
            currentStreak++
            longestStreak = kotlin.math.max(longestStreak, currentStreak)
        } else if (dayDiff > 1) {
            currentStreak = 1
        }
        
        lastDay = currentDay
    }
    
    return longestStreak
}

private fun History.calculateTrendDirection(entries: List<Entry>): TrendDirection {
    if (entries.size < 4) return TrendDirection.STABLE
    
    val now = epochSeconds()
    val twoWeeksAgo = now - (14 * DAY_SECONDS)
    val oneWeekAgo = now - (7 * DAY_SECONDS)
    
    val lastWeekCount = entries.count { it.startTime >= oneWeekAgo }
    val previousWeekCount = entries.count { 
        it.startTime >= twoWeeksAgo && it.startTime < oneWeekAgo 
    }
    
    return when {
        lastWeekCount > previousWeekCount -> TrendDirection.IMPROVING
        lastWeekCount < previousWeekCount -> TrendDirection.DECLINING
        else -> TrendDirection.STABLE
    }
}

private fun getWeeklyTarget(activity: String): Int? {
    // TODO: Get from user preferences or smart defaults
    return when (activity.lowercase()) {
        "exercise", "workout", "gym" -> 3
        "reading", "read" -> 5
        "meditation", "meditate" -> 7
        else -> null
    }
}

fun History.getRecoveryRecommendations(): List<RecoveryState> {
    val importantActivities = listOf("Exercise", "Reading", "Meditation")
    val recoveryStates = mutableListOf<RecoveryState>()
    
    importantActivities.forEach { activity ->
        val metrics = calculateHabitMetrics(activity, allowSkipDays = true)
        
        if (metrics.skipDaysUsed >= 1 && metrics.currentStreak == 0) {
            val skipDaysRemaining = metrics.allowedSkipDays - metrics.skipDaysUsed
            
            recoveryStates.add(RecoveryState(
                activity = activity,
                skipDaysRemaining = skipDaysRemaining,
                encouragementMessage = when {
                    skipDaysRemaining > 0 -> "Life happens! You have $skipDaysRemaining skip days left this week"
                    else -> "Consider adjusting your goals - progress matters more than perfection"
                },
                adjustedGoal = if (skipDaysRemaining <= 0) {
                    "Try ${activity.lowercase()} just once this week"
                } else null
            ))
        }
    }
    
    return recoveryStates
}