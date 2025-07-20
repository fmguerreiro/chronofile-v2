// © Art Chaidarun

package com.chaidarun.chronofile

import android.util.Log
import com.google.android.gms.location.LocationServices

data class History(val entries: List<Entry>, val currentActivityStartTime: Long) {

  fun withEditedEntry(
    oldStartTime: Long,
    editedStartTime: String,
    activity: String,
    note: String?
  ): History {
    // Collect inputs
    val (sanitizedActivity, sanitizedNote) = sanitizeActivityAndNote(activity, note)
    val newStartTime =
      try {
        val trimmedEditedStartTime = editedStartTime.trim()
        val enteredTime =
          when {
            trimmedEditedStartTime == "" -> oldStartTime
            ':' in trimmedEditedStartTime -> {
              val now = epochSeconds()
              val (hours, minutes) = trimmedEditedStartTime.split(':')
              val time =
                getPreviousMidnight(now) +
                  3600 * hours.toInt() +
                  60 * minutes.toInt() +
                  Math.round(Math.random() * 60)
              if (time > now) time - DAY_SECONDS else time
            }
            trimmedEditedStartTime.length == 10 -> trimmedEditedStartTime.toLong() // Unix timestamp
            else -> oldStartTime + trimmedEditedStartTime.toInt() * 60 // Minute delta
          }
        if (enteredTime > 15e8 && enteredTime <= epochSeconds()) enteredTime else null
      } catch (_: Exception) {
        null
      }
    if (newStartTime == null) {
      App.toast("Invalid start time")
      return this
    }

    // Edit entry
    val entryIndex = entries.indexOfFirst { it.startTime == oldStartTime }
    val oldEntry = entries[entryIndex]
    val newEntries =
      entries.toMutableList().apply {
        this[entryIndex] = Entry(newStartTime, sanitizedActivity, oldEntry.latLong, sanitizedNote)
      }

    App.toast("Updated entry")
    return copy(entries = normalizeAndSave(newEntries, currentActivityStartTime))
  }

  fun withNewEntry(activity: String, note: String?, latLong: Pair<Double, Double>?): History {
    val (sanitizedActivity, sanitizedNote) = sanitizeActivityAndNote(activity, note)
    val entry = Entry(currentActivityStartTime, sanitizedActivity, latLong, sanitizedNote)
    val newEntries = entries.toMutableList().apply { add(entry) }
    val nextStartTime = epochSeconds()
    return copy(
      currentActivityStartTime = nextStartTime,
      entries = normalizeAndSave(newEntries, nextStartTime)
    )
  }

  fun withoutEntry(startTime: Long) =
    copy(
      entries =
        normalizeAndSave(entries.filter { it.startTime != startTime }, currentActivityStartTime)
    )

  /** Get the top 6 most frequent activities in the past 30 days */
  fun getTop6ActivitiesLast30Days(): List<String> {
    val thirtyDaysAgo = epochSeconds() - (30 * DAY_SECONDS)
    
    // Get entries from the last 30 days
    val recentEntries = entries.filter { it.startTime >= thirtyDaysAgo }
    
    // Count frequency of each activity
    val activityCounts = mutableMapOf<String, Int>()
    recentEntries.forEach { entry ->
      activityCounts[entry.activity] = activityCounts.getOrDefault(entry.activity, 0) + 1
    }
    
    // Return top 6 activities sorted by frequency
    return activityCounts.entries
      .sortedByDescending { it.value }
      .take(6)
      .map { it.key }
  }

  /** Get intelligently filtered activities based on current time context and usage patterns */
  fun getIntelligentActivitySuggestions(): List<String> {
    val now = epochSeconds()
    val currentHour = ((now % DAY_SECONDS) / 3600).toInt()
    val suggestions = mutableListOf<String>()
    
    // Check if we have at least 7 days of data, otherwise use fallback
    val sevenDaysAgo = now - (7 * DAY_SECONDS)
    val hasEnoughData = entries.any { it.startTime >= sevenDaysAgo }
    
    if (!hasEnoughData) {
      // Fallback: Show 5 most recent unique activities
      return entries.reversed()
        .map { it.activity }
        .distinct()
        .take(5)
        .ifEmpty { listOf("Work", "Break", "Lunch", "Meeting", "Study") }
    }
    
    val fourteenDaysAgo = now - (14 * DAY_SECONDS)
    val twoDaysAgo = now - (2 * DAY_SECONDS)
    val thirtyDaysAgo = now - (30 * DAY_SECONDS)
    
    // 1. Time-based prediction (highest priority)
    val timeActivities = getActivitiesForHour(currentHour, fourteenDaysAgo)
    suggestions.addAll(timeActivities.take(2))
    
    // 2. Frequency-based
    val frequentActivities = getMostFrequent(fourteenDaysAgo)
    suggestions.addAll(frequentActivities.take(3))
    
    // 3. Sequential patterns (if last activity exists)
    val lastActivity = entries.lastOrNull()?.activity
    if (lastActivity != null) {
      val followingActivities = getActivitiesAfter(lastActivity, thirtyDaysAgo)
      suggestions.addAll(followingActivities.take(1))
    }
    
    // 4. Recency boost for activities in last 2 days
    val recentActivities = entries.filter { it.startTime >= twoDaysAgo }
      .map { it.activity }
      .distinct()
    
    val recentlyBoostedSuggestions = suggestions.toMutableList()
    recentActivities.forEach { recentActivity ->
      val index = recentlyBoostedSuggestions.indexOf(recentActivity)
      if (index != -1) {
        // Move recent activity to front
        recentlyBoostedSuggestions.removeAt(index)
        recentlyBoostedSuggestions.add(0, recentActivity)
      }
    }
    
    // Deduplicate and limit to 5
    return recentlyBoostedSuggestions.distinct().take(5)
  }
  
  private fun getActivitiesForHour(targetHour: Int, since: Long): List<String> {
    val hourRange = -1..1 // ±1 hour
    return entries.filter { entry ->
      entry.startTime >= since &&
      ((entry.startTime % DAY_SECONDS) / 3600).toInt() in (targetHour + hourRange.first)..(targetHour + hourRange.last)
    }
    .groupBy { it.activity }
    .mapValues { it.value.size }
    .entries
    .sortedByDescending { it.value }
    .map { it.key }
  }
  
  private fun getMostFrequent(since: Long): List<String> {
    return entries.filter { it.startTime >= since }
      .groupBy { it.activity }
      .mapValues { it.value.size }
      .entries
      .sortedByDescending { it.value }
      .map { it.key }
  }
  
  private fun getActivitiesAfter(lastActivity: String, since: Long): List<String> {
    val recentEntries = entries.filter { it.startTime >= since }
    val followingActivities = mutableMapOf<String, Int>()
    
    for (i in 0 until recentEntries.size - 1) {
      if (recentEntries[i].activity == lastActivity) {
        val nextActivity = recentEntries[i + 1].activity
        followingActivities[nextActivity] = followingActivities.getOrDefault(nextActivity, 0) + 1
      }
    }
    
    return followingActivities.entries
      .sortedByDescending { it.value }
      .map { it.key }
  }

  /** Predict activity based on current time patterns */
  fun predictActivityForCurrentTime(): String? {
    val now = epochSeconds()
    val currentHour = ((now % DAY_SECONDS) / 3600).toInt()
    val currentMinute = ((now % 3600) / 60).toInt()
    val currentTimeInMinutes = currentHour * 60 + currentMinute
    
    // Look at the last 30 days for pattern analysis
    val thirtyDaysAgo = now - (30 * DAY_SECONDS)
    val recentEntries = entries.filter { it.startTime >= thirtyDaysAgo }
    
    // Group activities by time windows (30-minute windows)
    val timeWindowSize = 30 // minutes
    val activityPatterns = mutableMapOf<Int, MutableMap<String, Int>>()
    
    recentEntries.forEach { entry ->
      val entryTime = entry.startTime
      val entryHour = ((entryTime % DAY_SECONDS) / 3600).toInt()
      val entryMinute = ((entryTime % 3600) / 60).toInt()
      val entryTimeInMinutes = entryHour * 60 + entryMinute
      val timeWindow = entryTimeInMinutes / timeWindowSize
      
      val windowActivities = activityPatterns.getOrPut(timeWindow) { mutableMapOf() }
      windowActivities[entry.activity] = windowActivities.getOrDefault(entry.activity, 0) + 1
    }
    
    // Find the current time window
    val currentTimeWindow = currentTimeInMinutes / timeWindowSize
    
    // Check current window and adjacent windows for patterns
    val windowsToCheck = listOf(currentTimeWindow - 1, currentTimeWindow, currentTimeWindow + 1)
    
    var bestActivity: String? = null
    var bestScore = 0
    var bestConfidence = 0.0
    
    windowsToCheck.forEach { window ->
      val windowActivities = activityPatterns[window]
      if (windowActivities != null) {
        val totalOccurrences = windowActivities.values.sum()
        windowActivities.forEach { (activity, count) ->
          val confidence = count.toDouble() / totalOccurrences
          val score = count
          
          // Prefer activities with both high frequency and confidence
          // Require at least 3 occurrences and 30% confidence for a suggestion
          if (score >= 3 && confidence >= 0.3 && score > bestScore) {
            bestActivity = activity
            bestScore = score
            bestConfidence = confidence
          }
        }
      }
    }
    
    return bestActivity
  }

  /** Get prediction confidence for display purposes */
  fun getPredictionConfidence(activity: String): Double {
    val now = epochSeconds()
    val currentHour = ((now % DAY_SECONDS) / 3600).toInt()
    val currentMinute = ((now % 3600) / 60).toInt()
    val currentTimeInMinutes = currentHour * 60 + currentMinute
    
    val thirtyDaysAgo = now - (30 * DAY_SECONDS)
    val recentEntries = entries.filter { it.startTime >= thirtyDaysAgo }
    
    val timeWindowSize = 30
    val currentTimeWindow = currentTimeInMinutes / timeWindowSize
    val windowsToCheck = listOf(currentTimeWindow - 1, currentTimeWindow, currentTimeWindow + 1)
    
    var totalOccurrences = 0
    var activityOccurrences = 0
    
    windowsToCheck.forEach { window ->
      recentEntries.forEach { entry ->
        val entryTime = entry.startTime
        val entryHour = ((entryTime % DAY_SECONDS) / 3600).toInt()
        val entryMinute = ((entryTime % 3600) / 60).toInt()
        val entryTimeInMinutes = entryHour * 60 + entryMinute
        val entryTimeWindow = entryTimeInMinutes / timeWindowSize
        
        if (entryTimeWindow == window) {
          totalOccurrences++
          if (entry.activity == activity) {
            activityOccurrences++
          }
        }
      }
    }
    
    return if (totalOccurrences > 0) {
      activityOccurrences.toDouble() / totalOccurrences
    } else {
      0.0
    }
  }

  /** Calculate life balance metrics for the current week */
  fun calculateLifeBalance(config: Config): LifeBalanceMetrics {
    val now = epochSeconds()
    val weekStart = now - (7 * DAY_SECONDS)
    val weekEntries = entries.filter { it.startTime >= weekStart }
    
    if (weekEntries.isEmpty()) {
      return LifeBalanceMetrics(
        categories = emptyMap(),
        overallBalance = 0.0f,
        improvementAreas = listOf("Start tracking activities to see your balance"),
        strengths = emptyList(),
        weeklyTotalHours = 0.0f
      )
    }
    
    val categoryHours = mutableMapOf<LifeCategory, Float>()
    val categoryDays = mutableMapOf<LifeCategory, MutableSet<Int>>()
    
    // Calculate time spent in each category
    for (i in weekEntries.indices) {
      val entry = weekEntries[i]
      val nextEntry = weekEntries.getOrNull(i + 1)
      val duration = (nextEntry?.startTime ?: currentActivityStartTime) - entry.startTime
      val hours = duration / 3600.0f
      
      val category = mapActivityToLifeCategory(entry.activity)
      categoryHours[category] = categoryHours.getOrDefault(category, 0.0f) + hours
      
      // Track which days this category was active
      val dayOfWeek = ((entry.startTime / DAY_SECONDS) % 7).toInt()
      categoryDays.getOrPut(category) { mutableSetOf() }.add(dayOfWeek)
    }
    
    val totalHours = categoryHours.values.sum()
    val categories = categoryHours.map { (category, hours) ->
      val percentage = if (totalHours > 0) hours / totalHours else 0.0f
      val consistency = categoryDays[category]?.size?.toFloat()?.div(7.0f) ?: 0.0f
      
      category to CategoryMetrics(
        weeklyHours = hours,
        consistencyScore = consistency,
        trend = BalanceTrend.STABLE, // TODO: Calculate trend
        personalBest = hours, // TODO: Track historical bests
        percentage = percentage
      )
    }.toMap()
    
    // Calculate overall balance (how evenly distributed the categories are)
    val overallBalance = if (categories.isNotEmpty()) {
      val targetPercentage = 1.0f / categories.size
      val variance = categories.values.map { 
        kotlin.math.abs(it.percentage - targetPercentage) 
      }.average().toFloat()
      kotlin.math.max(0.0f, 1.0f - variance * 2)
    } else 0.0f
    
    val improvementAreas = findImprovementAreas(categories)
    val strengths = findStrengths(categories)
    
    return LifeBalanceMetrics(
      categories = categories,
      overallBalance = overallBalance,
      improvementAreas = improvementAreas,
      strengths = strengths,
      weeklyTotalHours = totalHours
    )
  }
  
  private fun findImprovementAreas(categories: Map<LifeCategory, CategoryMetrics>): List<String> {
    val improvements = mutableListOf<String>()
    
    // Find categories with very low time
    categories.forEach { (category, metrics) ->
      if (metrics.weeklyHours < 2.0f) {
        improvements.add("Consider adding more ${category.displayName.lowercase()}")
      }
    }
    
    // Check for missing important categories
    val importantCategories = listOf(
      LifeCategory.HEALTH_FITNESS,
      LifeCategory.REST_RELAXATION,
      LifeCategory.RELATIONSHIPS_FAMILY
    )
    
    importantCategories.forEach { category ->
      if (!categories.containsKey(category)) {
        improvements.add("Missing ${category.displayName.lowercase()} activities")
      }
    }
    
    return improvements.take(3)
  }
  
  private fun findStrengths(categories: Map<LifeCategory, CategoryMetrics>): List<String> {
    return categories.entries
      .filter { it.value.consistencyScore > 0.4f }
      .sortedByDescending { it.value.consistencyScore }
      .take(2)
      .map { "Great consistency in ${it.key.displayName.lowercase()}" }
  }

  /** Get immediate value recommendations focusing on first-session value */
  fun getImmediateValueRecommendations(
    config: Config,
    maxRecommendations: Int = 3
  ): List<SmartRecommendation> {
    val recommendations = mutableListOf<SmartRecommendation>()
    
    // Priority 1: Progress celebrations (positive reinforcement)
    recommendations.addAll(getProgressCelebrations())
    
    // Priority 2: Life balance insights with gentle nudges
    recommendations.addAll(getLifeBalanceNudges(config))
    
    // Priority 3: Habit building with two-minute rule
    recommendations.addAll(getHabitBuildingRecommendations())
    
    return recommendations.take(maxRecommendations)
  }
  
  private fun getProgressCelebrations(): List<SmartRecommendation> {
    val celebrations = mutableListOf<SmartRecommendation>()
    
    // Check for streaks
    val activities = entries.takeLast(7).map { it.activity }.distinct()
    if (activities.size >= 3) {
      celebrations.add(SmartRecommendation(
        id = "variety_celebration",
        type = RecommendationType.PROGRESS_CELEBRATION,
        title = "🎉 Great variety!",
        message = "You've engaged in ${activities.size} different activities this week",
        actionText = "Keep exploring",
        eventBasedCue = null,
        twoMinuteVersion = null,
        confidenceScore = 1.0f,
        celebratesProgress = true
      ))
    }
    
    return celebrations
  }
  
  private fun getLifeBalanceNudges(config: Config): List<SmartRecommendation> {
    val nudges = mutableListOf<SmartRecommendation>()
    val balance = calculateLifeBalance(config)
    
    // Gentle nudges based on what's missing
    if (balance.categories.isEmpty() || 
        !balance.categories.containsKey(LifeCategory.HEALTH_FITNESS)) {
      nudges.add(SmartRecommendation(
        id = "health_nudge",
        type = RecommendationType.GENTLE_NUDGE,
        title = "Nurture your health",
        message = "A short walk can boost your energy and mood",
        actionText = "Log a walk",
        eventBasedCue = "After your next meal",
        twoMinuteVersion = "2-minute stretch",
        confidenceScore = 0.8f,
        suggestedActivity = "Walk"
      ))
    }
    
    return nudges
  }
  
  private fun getHabitBuildingRecommendations(): List<SmartRecommendation> {
    val recommendations = mutableListOf<SmartRecommendation>()
    
    // Two-minute rule recommendations
    val recentActivities = entries.takeLast(10).map { it.activity }.distinct()
    if (!recentActivities.any { it.contains("read", true) }) {
      recommendations.add(SmartRecommendation(
        id = "reading_habit",
        type = RecommendationType.HABIT_BUILDING,
        title = "Build a reading habit",
        message = "Start with just 2 minutes of reading",
        actionText = "Read for 2 minutes",
        eventBasedCue = "After morning coffee",
        twoMinuteVersion = "Read one page",
        confidenceScore = 0.7f,
        suggestedActivity = "Reading"
      ))
    }
    
    return recommendations
  }

  companion object {
    private const val FILENAME = "chronofile.tsv"
    private val locationClient by lazy { LocationServices.getFusedLocationProviderClient(App.ctx) }

    private fun normalizeAndSave(entries: Collection<Entry>, currentActivityStartTime: Long) =
      entries
        .toMutableList()
        .apply {
          // Normalize
          Log.i(TAG, "Normalizing entries")
          sortBy { it.startTime }
          var lastSeenActivity: String? = null
          var lastSeenNote: String? = null
          removeAll {
            val shouldRemove = it.activity == lastSeenActivity && it.note == lastSeenNote
            lastSeenActivity = it.activity
            lastSeenNote = it.note
            shouldRemove
          }

          // Save
          IOUtil.writeFile(
            FILENAME,
            joinToString("") { it.toTsvRow() } + "\t\t\t\t$currentActivityStartTime\n"
          )
        }
        .toList()

    private fun sanitizeActivityAndNote(activity: String, note: String?) =
      Pair(activity.trim(), if (note.isNullOrBlank()) null else note.trim())

    private fun getLocation(callback: (Pair<Double, Double>?) -> Unit) {
      try {
        locationClient.lastLocation.addOnCompleteListener {
          if (it.isSuccessful && it.result != null) {
            callback(Pair(it.result.latitude, it.result.longitude))
          } else {
            callback(null)
          }
        }
        return
      } catch (_: SecurityException) {
        Log.i(TAG, "Failed to get location")
      }
      callback(null)
    }
    
    /** Acquires current location before dispatching action to create new entry */
    fun addEntry(activity: String, note: String?) {
      getLocation {
        Store.dispatch(Action.AddEntry(activity, note, it))
        App.toast("Recorded $activity")
        
        // Update widgets after adding entry
        try {
          ChronofileWidgetProvider.updateAllWidgets(App.ctx)
        } catch (e: Exception) {
          // Ignore widget update errors to not break the main flow
        }
      }
    }

    fun fromFile(): History {
      // Read lines
      var currentActivityStartTime = epochSeconds()
      val lines = IOUtil.readFile(FILENAME)?.lines() ?: listOf("\t\t\t\t$currentActivityStartTime")

      // Parse lines
      val entries = mutableListOf<Entry>()
      lines.forEach {
        if (it.isEmpty()) {
          return@forEach
        }

        val (activity, lat, long, note, startTime) = it.split("\t")
        val latLong =
          if (lat.isNotEmpty() && long.isNotEmpty()) Pair(lat.toDouble(), long.toDouble()) else null
        if (activity.isNotEmpty()) {
          entries +=
            Entry(startTime.toLong(), activity, latLong, if (note.isEmpty()) null else note)
        } else {
          currentActivityStartTime = startTime.toLong()
        }
      }

      return History(normalizeAndSave(entries, currentActivityStartTime), currentActivityStartTime)
    }
  }
}
