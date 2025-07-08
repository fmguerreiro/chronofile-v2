package com.chaidarun.chronofile

// Extension methods for History class to handle progress celebrations
fun History.getProgressCelebrations(): List<Achievement> {
    val achievements = mutableListOf<Achievement>()
    
    // Streak achievements
    achievements.addAll(getStreakAchievements())
    
    // Balance achievements
    achievements.addAll(getBalanceAchievements())
    
    // Consistency improvements
    achievements.addAll(getConsistencyImprovements())
    
    // First-time achievements
    achievements.addAll(getFirstTimeAchievements())
    
    return achievements.filter { it.isNewlyEarned }
}

// New method to get ALL achievements (earned and locked)
fun History.getAllAchievements(): List<Achievement> {
    val achievements = mutableListOf<Achievement>()
    
    // Streak achievements (both earned and locked)
    achievements.addAll(getAllStreakAchievements())
    
    // Balance achievements (both earned and locked) 
    achievements.addAll(getAllBalanceAchievements())
    
    // Consistency improvements (both earned and locked)
    achievements.addAll(getAllConsistencyAchievements())
    
    // First-time achievements (both earned and locked)
    achievements.addAll(getAllFirstTimeAchievements())
    
    return achievements.sortedWith(compareBy<Achievement> { it.isLocked }.thenBy { it.id })
}

private fun History.getStreakAchievements(): List<Achievement> {
    val achievements = mutableListOf<Achievement>()
    
    // Check for various streak milestones
    val activities = entries.takeLast(30).map { it.activity }.distinct()
    
    activities.forEach { activity ->
        val metrics = calculateHabitMetrics(activity, allowSkipDays = true)
        
        when (metrics.currentStreak) {
            3 -> achievements.add(Achievement(
                id = "streak_3_$activity",
                title = "Three Day Warrior",
                description = "Completed 3 days of ${activity.lowercase()}!",
                emoji = "🔥",
                isNewlyEarned = checkIfNewlyEarned("streak_3_$activity"),
                celebrationMessage = "You're building momentum with $activity!",
                category = mapActivityToLifeCategory(activity)
            ))
            
            7 -> achievements.add(Achievement(
                id = "streak_7_$activity",
                title = "Week Champion",
                description = "Amazing! 7 days strong with ${activity.lowercase()}",
                emoji = "🏆",
                isNewlyEarned = checkIfNewlyEarned("streak_7_$activity"),
                celebrationMessage = "A full week of $activity - you're unstoppable!",
                category = mapActivityToLifeCategory(activity)
            ))
            
            30 -> achievements.add(Achievement(
                id = "streak_30_$activity",
                title = "Monthly Master",
                description = "Incredible 30-day streak with ${activity.lowercase()}!",
                emoji = "💎",
                isNewlyEarned = checkIfNewlyEarned("streak_30_$activity"),
                celebrationMessage = "30 days of $activity - you've formed a real habit!",
                category = mapActivityToLifeCategory(activity)
            ))
        }
    }
    
    return achievements
}

private fun History.getBalanceAchievements(): List<Achievement> {
    val achievements = mutableListOf<Achievement>()
    val config = Store.state.config ?: return achievements
    val balance = calculateLifeBalance(config)
    
    // Life balance achievement
    if (balance.overallBalance > 0.7f && balance.categories.size >= 4) {
        achievements.add(Achievement(
            id = "balanced_life",
            title = "Life Balance Master",
            description = "Great balance across ${balance.categories.size} life categories!",
            emoji = "⚖️",
            isNewlyEarned = checkIfNewlyEarned("balanced_life"),
            celebrationMessage = "You're living a wonderfully balanced life!"
        ))
    }
    
    // Category diversity
    if (balance.categories.size >= 6) {
        achievements.add(Achievement(
            id = "diverse_life",
            title = "Renaissance Person",
            description = "Active in ${balance.categories.size} different life areas!",
            emoji = "🌟",
            isNewlyEarned = checkIfNewlyEarned("diverse_life"),
            celebrationMessage = "Your life is beautifully diverse and rich!"
        ))
    }
    
    return achievements
}

private fun History.getConsistencyImprovements(): List<Achievement> {
    val achievements = mutableListOf<Achievement>()
    
    // Check for improved consistency compared to previous week
    val now = epochSeconds()
    val thisWeekStart = now - (7 * DAY_SECONDS)
    val lastWeekStart = now - (14 * DAY_SECONDS)
    
    val thisWeekEntries = entries.filter { it.startTime >= thisWeekStart }
    val lastWeekEntries = entries.filter { 
        it.startTime >= lastWeekStart && it.startTime < thisWeekStart 
    }
    
    val thisWeekActivities = thisWeekEntries.map { it.activity }.distinct().size
    val lastWeekActivities = lastWeekEntries.map { it.activity }.distinct().size
    
    if (thisWeekActivities > lastWeekActivities && thisWeekActivities >= 3) {
        achievements.add(Achievement(
            id = "consistency_improvement",
            title = "Consistency Champion",
            description = "More variety this week than last - $thisWeekActivities different activities!",
            emoji = "📈",
            isNewlyEarned = checkIfNewlyEarned("consistency_improvement"),
            celebrationMessage = "Your consistency is improving week by week!"
        ))
    }
    
    return achievements
}

private fun History.getFirstTimeAchievements(): List<Achievement> {
    val achievements = mutableListOf<Achievement>()
    
    // First entry achievement
    if (entries.size == 1) {
        achievements.add(Achievement(
            id = "first_entry",
            title = "Journey Begins",
            description = "Congratulations on your first tracked activity!",
            emoji = "🎯",
            isNewlyEarned = true,
            celebrationMessage = "Welcome to your personal growth journey!"
        ))
    }
    
    // First week achievement
    val oneWeekAgo = epochSeconds() - (7 * DAY_SECONDS)
    val weekEntries = entries.filter { it.startTime >= oneWeekAgo }
    if (weekEntries.size >= 5 && entries.size <= 10) {
        achievements.add(Achievement(
            id = "first_week",
            title = "Week One Warrior",
            description = "Successfully tracked activities for a full week!",
            emoji = "🌱",
            isNewlyEarned = checkIfNewlyEarned("first_week"),
            celebrationMessage = "You're building great tracking habits!"
        ))
    }
    
    return achievements
}

// Get ALL streak achievements (earned and locked)
private fun History.getAllStreakAchievements(): List<Achievement> {
    val achievements = mutableListOf<Achievement>()
    val activities = entries.takeLast(30).map { it.activity }.distinct()
    
    activities.forEach { activity ->
        val metrics = calculateHabitMetrics(activity, allowSkipDays = true)
        
        // 3-day streak
        val has3DayStreak = metrics.currentStreak >= 3
        achievements.add(Achievement(
            id = "streak_3_$activity",
            title = "Three Day Warrior",
            description = if (has3DayStreak) "Completed 3 days of ${activity.lowercase()}!" else "Complete 3 days of ${activity.lowercase()}",
            emoji = "🔥",
            isNewlyEarned = has3DayStreak && checkIfNewlyEarned("streak_3_$activity"),
            celebrationMessage = "You're building momentum with $activity!",
            category = mapActivityToLifeCategory(activity),
            isLocked = !has3DayStreak,
            unlockCriteria = if (!has3DayStreak) "Complete 3 consecutive days of $activity" else null,
            progressText = if (!has3DayStreak) "${metrics.currentStreak}/3 days" else null
        ))
        
        // 7-day streak
        val has7DayStreak = metrics.currentStreak >= 7
        achievements.add(Achievement(
            id = "streak_7_$activity",
            title = "Week Champion",
            description = if (has7DayStreak) "Amazing! 7 days strong with ${activity.lowercase()}" else "Complete 7 days of ${activity.lowercase()}",
            emoji = "🏆",
            isNewlyEarned = has7DayStreak && checkIfNewlyEarned("streak_7_$activity"),
            celebrationMessage = "A full week of $activity - you're unstoppable!",
            category = mapActivityToLifeCategory(activity),
            isLocked = !has7DayStreak,
            unlockCriteria = if (!has7DayStreak) "Complete 7 consecutive days of $activity" else null,
            progressText = if (!has7DayStreak) "${metrics.currentStreak}/7 days" else null
        ))
        
        // 30-day streak
        val has30DayStreak = metrics.currentStreak >= 30
        achievements.add(Achievement(
            id = "streak_30_$activity",
            title = "Monthly Master",
            description = if (has30DayStreak) "Incredible 30-day streak with ${activity.lowercase()}!" else "Complete 30 days of ${activity.lowercase()}",
            emoji = "💎",
            isNewlyEarned = has30DayStreak && checkIfNewlyEarned("streak_30_$activity"),
            celebrationMessage = "30 days of $activity - you've formed a real habit!",
            category = mapActivityToLifeCategory(activity),
            isLocked = !has30DayStreak,
            unlockCriteria = if (!has30DayStreak) "Complete 30 consecutive days of $activity" else null,
            progressText = if (!has30DayStreak) "${metrics.currentStreak}/30 days" else null
        ))
    }
    
    return achievements
}

// Get ALL balance achievements (earned and locked)
private fun History.getAllBalanceAchievements(): List<Achievement> {
    val achievements = mutableListOf<Achievement>()
    val config = Store.state.config ?: return achievements
    val balance = calculateLifeBalance(config)
    
    // Life balance achievement
    val hasLifeBalance = balance.overallBalance > 0.7f && balance.categories.size >= 4
    achievements.add(Achievement(
        id = "balanced_life",
        title = "Life Balance Master",
        description = if (hasLifeBalance) "Great balance across ${balance.categories.size} life categories!" else "Achieve balance across 4+ life categories",
        emoji = "⚖️",
        isNewlyEarned = hasLifeBalance && checkIfNewlyEarned("balanced_life"),
        celebrationMessage = "You're living a wonderfully balanced life!",
        isLocked = !hasLifeBalance,
        unlockCriteria = if (!hasLifeBalance) "Track activities in 4+ life categories with 70%+ balance" else null,
        progressText = if (!hasLifeBalance) "${balance.categories.size}/4 categories, ${(balance.overallBalance * 100).toInt()}% balance" else null
    ))
    
    // Category diversity
    val hasDiversity = balance.categories.size >= 6
    achievements.add(Achievement(
        id = "diverse_life",
        title = "Renaissance Person",
        description = if (hasDiversity) "Active in ${balance.categories.size} different life areas!" else "Be active in 6+ different life areas",
        emoji = "🌟",
        isNewlyEarned = hasDiversity && checkIfNewlyEarned("diverse_life"),
        celebrationMessage = "Your life is beautifully diverse and rich!",
        isLocked = !hasDiversity,
        unlockCriteria = if (!hasDiversity) "Track activities across 6+ different life categories" else null,
        progressText = if (!hasDiversity) "${balance.categories.size}/6 life areas" else null
    ))
    
    return achievements
}

// Get ALL consistency achievements (earned and locked)
private fun History.getAllConsistencyAchievements(): List<Achievement> {
    val achievements = mutableListOf<Achievement>()
    
    val now = epochSeconds()
    val thisWeekStart = now - (7 * DAY_SECONDS)
    val lastWeekStart = now - (14 * DAY_SECONDS)
    
    val thisWeekEntries = entries.filter { it.startTime >= thisWeekStart }
    val lastWeekEntries = entries.filter { 
        it.startTime >= lastWeekStart && it.startTime < thisWeekStart 
    }
    
    val thisWeekActivities = thisWeekEntries.map { it.activity }.distinct().size
    val lastWeekActivities = lastWeekEntries.map { it.activity }.distinct().size
    
    val hasConsistencyImprovement = thisWeekActivities > lastWeekActivities && thisWeekActivities >= 3
    achievements.add(Achievement(
        id = "consistency_improvement",
        title = "Consistency Champion",
        description = if (hasConsistencyImprovement) "More variety this week than last - $thisWeekActivities different activities!" else "Do more varied activities than last week",
        emoji = "📈",
        isNewlyEarned = hasConsistencyImprovement && checkIfNewlyEarned("consistency_improvement"),
        celebrationMessage = "Your consistency is improving week by week!",
        isLocked = !hasConsistencyImprovement,
        unlockCriteria = if (!hasConsistencyImprovement) "Track more varied activities this week than last week (minimum 3)" else null,
        progressText = if (!hasConsistencyImprovement) "This week: $thisWeekActivities, Last week: $lastWeekActivities" else null
    ))
    
    return achievements
}

// Get ALL first-time achievements (earned and locked)
private fun History.getAllFirstTimeAchievements(): List<Achievement> {
    val achievements = mutableListOf<Achievement>()
    
    // First entry achievement
    val hasFirstEntry = entries.isNotEmpty()
    achievements.add(Achievement(
        id = "first_entry",
        title = "Journey Begins",
        description = if (hasFirstEntry) "Congratulations on your first tracked activity!" else "Track your first activity",
        emoji = "🎯",
        isNewlyEarned = entries.size == 1,
        celebrationMessage = "Welcome to your personal growth journey!",
        isLocked = !hasFirstEntry,
        unlockCriteria = if (!hasFirstEntry) "Track your first activity to get started" else null,
        progressText = if (!hasFirstEntry) "0/1 activities tracked" else null
    ))
    
    // First week achievement
    val oneWeekAgo = epochSeconds() - (7 * DAY_SECONDS)
    val weekEntries = entries.filter { it.startTime >= oneWeekAgo }
    val hasFirstWeek = weekEntries.size >= 5
    achievements.add(Achievement(
        id = "first_week",
        title = "Week One Warrior",
        description = if (hasFirstWeek) "Successfully tracked activities for a full week!" else "Track activities for a full week",
        emoji = "🌱",
        isNewlyEarned = hasFirstWeek && entries.size <= 10 && checkIfNewlyEarned("first_week"),
        celebrationMessage = "You're building great tracking habits!",
        isLocked = !hasFirstWeek,
        unlockCriteria = if (!hasFirstWeek) "Track 5+ activities in one week" else null,
        progressText = if (!hasFirstWeek) "${weekEntries.size}/5 activities this week" else null
    ))
    
    return achievements
}

// Simulated method to check if achievement is newly earned
// In a real implementation, this would check against stored achievement state
private fun History.checkIfNewlyEarned(achievementId: String): Boolean {
    // For demo purposes, return true for recent achievements
    // In production, this would check against a persistent achievement store
    return when (achievementId) {
        "first_entry" -> entries.size == 1
        "first_week" -> {
            val oneWeekAgo = epochSeconds() - (7 * DAY_SECONDS)
            entries.filter { it.startTime >= oneWeekAgo }.size >= 5 && entries.size <= 10
        }
        else -> {
            // For other achievements, check if they were earned recently
            val recentThreshold = epochSeconds() - (24 * 3600) // Last 24 hours
            entries.any { it.startTime >= recentThreshold }
        }
    }
}