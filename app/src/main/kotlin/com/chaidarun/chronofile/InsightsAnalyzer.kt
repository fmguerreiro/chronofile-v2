// © Art Chaidarun

package com.chaidarun.chronofile

import java.util.Calendar
import java.util.Date
import kotlin.math.abs

class InsightsAnalyzer {

  fun generateProductivityInsights(history: History, selectedDate: Calendar): String {
    val weekEntries = getWeekEntries(history, selectedDate)
    
    if (weekEntries.isEmpty()) {
      return "Start logging activities to see your productivity patterns!"
    }

    val insights = mutableListOf<String>()
    
    val patternInsights = generatePatternInsights(history, selectedDate)
    if (patternInsights.isNotEmpty()) insights.addAll(patternInsights)
    
    val lunchInsight = analyzeLunchBreakPattern(weekEntries, history)
    if (lunchInsight.isNotEmpty()) insights.add(lunchInsight)
    
    val eveningInsight = analyzeEveningRelaxationPattern(weekEntries, history)
    if (eveningInsight.isNotEmpty()) insights.add(eveningInsight)
    
    val workInsight = analyzeWorkSessionPattern(weekEntries, history)
    if (workInsight.isNotEmpty()) insights.add(workInsight)
    
    val transitionInsight = analyzeActivityTransitions(weekEntries, history)
    if (transitionInsight.isNotEmpty()) insights.add(transitionInsight)
    
    return if (insights.isNotEmpty()) {
      insights.joinToString("\n\n")
    } else {
      "Continue logging activities to discover your daily patterns!"
    }
  }

  fun generatePatternInsights(history: History, selectedDate: Calendar, config: Config? = null): List<String> {
    val totalDays = getTotalDaysOfHistory(history)
    
    // Fallback for sparse data
    if (totalDays < 7) {
      return listOf("Building pattern baseline (day $totalDays/7)")
    }
    
    val insights = mutableListOf<String>()
    
    // Priority 1: Broken streaks/routines (highest urgency)
    val brokenStreaks = detectBrokenStreaks(history, selectedDate, config)
    if (brokenStreaks.isNotEmpty()) {
      insights.add(brokenStreaks.first())
    }
    
    // Priority 2: Significant duration outliers (>50% variance)
    val durationOutliers = detectSignificantDurationOutliers(history, selectedDate, config)
    if (durationOutliers.isNotEmpty()) {
      insights.add(durationOutliers.first())
    }
    
    // Priority 3: Timing shifts (>2hr from usual time) - only if we have space
    if (insights.size < 3) {
      val timingShifts = detectTimingShifts(history, selectedDate, config)
      if (timingShifts.isNotEmpty()) {
        insights.add(timingShifts.first())
      }
    }
    
    // Priority 4: New activity patterns (first occurrence this week) - only if we have space
    if (insights.size < 3) {
      val newPatterns = detectNewActivityPatterns(history, selectedDate, config)
      if (newPatterns.isNotEmpty()) {
        insights.add(newPatterns.first())
      }
    }
    
    // Fallback if no significant patterns found
    return if (insights.isEmpty()) {
      listOf("No notable changes detected")
    } else {
      insights.take(4) // Maximum 4 insights
    }
  }

  private fun normalizeActivityName(activity: String, config: Config?): String {
    // Use user-defined activity groups if available, otherwise use the original activity
    return config?.getActivityGroup(activity)?.lowercase()?.trim() ?: activity.lowercase().trim()
  }

  private fun getTotalDaysOfHistory(history: History): Int {
    if (history.entries.isEmpty()) return 0
    
    val oldestEntry = history.entries.minByOrNull { it.startTime } ?: return 0
    val newestEntry = history.entries.maxByOrNull { it.startTime } ?: return 0
    
    val daysDiff = (newestEntry.startTime - oldestEntry.startTime) / DAY_SECONDS
    return (daysDiff + 1).toInt() // +1 to include both start and end days
  }

  private fun detectBrokenStreaks(history: History, selectedDate: Calendar, config: Config?): List<String> {
    val insights = mutableListOf<String>()
    val historicalEntries = getThirtyDayEntries(history, selectedDate)
    val weekEntries = getWeekEntries(history, selectedDate)
    
    if (historicalEntries.size < 14) return insights // Need at least 2 weeks of data
    
    // Find activities that had consistent patterns but are now missing
    val activityDailyOccurrences = mutableMapOf<String, MutableList<Int>>()
    
    // Track daily occurrences for each activity
    historicalEntries.forEach { entry ->
      val normalized = normalizeActivityName(entry.activity, config)
      val dayOfYear = Calendar.getInstance().apply { 
        timeInMillis = entry.startTime * 1000 
      }.get(Calendar.DAY_OF_YEAR)
      
      activityDailyOccurrences.getOrPut(normalized) { mutableListOf() }.add(dayOfYear)
    }
    
    val today = selectedDate.get(Calendar.DAY_OF_YEAR)
    
    activityDailyOccurrences.forEach { (activity, occurrenceDays) ->
      val uniqueDays = occurrenceDays.toSet().sorted().reversed()
      
      // Calculate streak length before today
      var streakLength = 0
      for (i in 1..14) { // Check last 14 days before today
        val dayToCheck = today - i
        if (uniqueDays.contains(dayToCheck)) {
          streakLength++
        } else {
          break
        }
      }
      
      // Check if activity is missing this week but had a streak ≥3 days
      val weekActivities = weekEntries.map { normalizeActivityName(it.activity, config) }.toSet()
      if (streakLength >= 3 && !weekActivities.contains(activity)) {
        insights.add("${activity.capitalize()} streak broken ($streakLength-day streak)")
      }
    }
    
    return insights.sortedByDescending { 
      // Extract streak length for sorting (longer streaks = higher priority)
      val match = "\\((\\d+)-day".toRegex().find(it)
      match?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }
  }

  private fun detectSignificantDurationOutliers(history: History, selectedDate: Calendar, config: Config?): List<String> {
    val insights = mutableListOf<String>()
    val weekEntries = getWeekEntries(history, selectedDate)
    val historicalEntries = getThirtyDayEntries(history, selectedDate)
    
    if (weekEntries.size < 2 || historicalEntries.size < 7) return insights
    
    val activityDurations = mutableMapOf<String, MutableList<Double>>()
    val historicalDurations = mutableMapOf<String, MutableList<Double>>()
    
    fun addDurations(entries: List<Entry>, map: MutableMap<String, MutableList<Double>>, endTime: Long) {
      entries.forEachIndexed { index, entry ->
        val normalized = normalizeActivityName(entry.activity, config)
        val nextEntry = entries.getOrNull(index + 1)
        val duration = (nextEntry?.startTime ?: endTime) - entry.startTime
        val hours = duration / 3600.0
        
        if (hours > 0.1 && hours < 24) { // Only consider reasonable durations
          map.getOrPut(normalized) { mutableListOf() }.add(hours)
        }
      }
    }
    
    addDurations(weekEntries, activityDurations, history.currentActivityStartTime)
    addDurations(historicalEntries, historicalDurations, history.currentActivityStartTime)
    
    activityDurations.forEach { (activity, durations) ->
      val historical = historicalDurations[activity]
      if (historical != null && historical.size >= 3 && durations.isNotEmpty()) {
        val currentAvg = durations.average()
        val historicalAvg = historical.average()
        val variance = abs(currentAvg - historicalAvg) / historicalAvg
        
        // Only flag if variance > 50%
        if (variance > 0.5) {
          val change = if (currentAvg > historicalAvg) {
            "+${((currentAvg - historicalAvg) * 60).toInt()}min"
          } else {
            "-${((historicalAvg - currentAvg) * 60).toInt()}min"
          }
          val percentage = (variance * 100).toInt()
          insights.add("${activity.capitalize()}: ${String.format("%.1f", currentAvg)}hr avg, $change ($percentage% change)")
        }
      }
    }
    
    return insights.sortedByDescending { 
      // Extract percentage for sorting (higher percentage = higher priority)
      val match = "\\((\\d+)%".toRegex().find(it)
      match?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }
  }

  private fun detectTimingShifts(history: History, selectedDate: Calendar, config: Config?): List<String> {
    val insights = mutableListOf<String>()
    val weekEntries = getWeekEntries(history, selectedDate)
    val historicalEntries = getThirtyDayEntries(history, selectedDate)
    
    if (weekEntries.isEmpty() || historicalEntries.size < 7) return insights
    
    val activityTimes = mutableMapOf<String, MutableList<Int>>()
    val historicalTimes = mutableMapOf<String, MutableList<Int>>()
    
    fun extractTimes(entries: List<Entry>, map: MutableMap<String, MutableList<Int>>) {
      entries.forEach { entry ->
        val normalized = normalizeActivityName(entry.activity, config)
        val hour = Calendar.getInstance().apply { 
          timeInMillis = entry.startTime * 1000 
        }.get(Calendar.HOUR_OF_DAY)
        
        map.getOrPut(normalized) { mutableListOf() }.add(hour)
      }
    }
    
    extractTimes(weekEntries, activityTimes)
    extractTimes(historicalEntries, historicalTimes)
    
    activityTimes.forEach { (activity, times) ->
      val historical = historicalTimes[activity]
      if (historical != null && historical.size >= 3 && times.isNotEmpty()) {
        val currentAvgHour = times.average().toInt()
        val historicalAvgHour = historical.average().toInt()
        val hourDiff = abs(currentAvgHour - historicalAvgHour)
        
        // Only flag if timing shift > 2 hours
        if (hourDiff >= 2) {
          val timeShift = if (currentAvgHour > historicalAvgHour) "later" else "earlier"
          insights.add("${activity.capitalize()}: usually ${formatHour(historicalAvgHour)}, this week ${formatHour(currentAvgHour)} (${hourDiff}hr $timeShift)")
        }
      }
    }
    
    return insights.sortedByDescending { 
      // Extract hour difference for sorting (larger shifts = higher priority)
      val match = "\\((\\d+)hr".toRegex().find(it)
      match?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }
  }

  private fun detectNewActivityPatterns(history: History, selectedDate: Calendar, config: Config?): List<String> {
    val insights = mutableListOf<String>()
    val weekEntries = getWeekEntries(history, selectedDate)
    val historicalEntries = getThirtyDayEntries(history, selectedDate)
    
    if (weekEntries.isEmpty() || historicalEntries.size < 7) return insights
    
    // Get activities from this week
    val weekActivities = weekEntries.map { normalizeActivityName(it.activity, config) }.toSet()
    
    // Get activities from historical data (excluding this week)
    val weekStart = Calendar.getInstance().apply {
      timeInMillis = selectedDate.timeInMillis
      add(Calendar.DAY_OF_YEAR, -6)
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
    }
    
    val preWeekEntries = historicalEntries.filter { entry ->
      entry.startTime * 1000 < weekStart.timeInMillis
    }
    
    val historicalActivities = preWeekEntries.map { normalizeActivityName(it.activity, config) }.toSet()
    
    // Find activities that appear this week but never appeared before
    val newActivities = weekActivities - historicalActivities
    
    newActivities.forEach { activity ->
      // Count occurrences this week to prioritize by frequency
      val weekCount = weekEntries.count { normalizeActivityName(it.activity, config) == activity }
      insights.add("New activity: ${activity.capitalize()} (${weekCount}x this week)")
    }
    
    return insights.sortedByDescending { 
      // Extract count for sorting (higher frequency = higher priority)
      val match = "\\((\\d+)x".toRegex().find(it)
      match?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }
  }
  
  private fun analyzeFrequencyPatterns(history: History, selectedDate: Calendar, config: Config?): String {
    val weekEntries = getWeekEntries(history, selectedDate)
    val thirtyDayEntries = getThirtyDayEntries(history, selectedDate)
    
    if (weekEntries.isEmpty() || thirtyDayEntries.isEmpty()) return ""
    
    val weeklyFrequency = mutableMapOf<String, Int>()
    val historicalFrequency = mutableMapOf<String, Int>()
    
    weekEntries.forEach { entry ->
      val normalized = normalizeActivityName(entry.activity, config)
      weeklyFrequency[normalized] = weeklyFrequency.getOrDefault(normalized, 0) + 1
    }
    
    thirtyDayEntries.forEach { entry ->
      val normalized = normalizeActivityName(entry.activity, config)
      historicalFrequency[normalized] = historicalFrequency.getOrDefault(normalized, 0) + 1
    }
    
    val topActivity = weeklyFrequency.maxByOrNull { it.value }
    if (topActivity != null) {
      val weeklyCount = topActivity.value
      val historicalWeeklyAvg = (historicalFrequency[topActivity.key] ?: 0) / 4.0 
      
      return if (weeklyCount > historicalWeeklyAvg * 1.25) {
        "${topActivity.key.capitalize()}: ${weeklyCount}x this week (usual: ${historicalWeeklyAvg.toInt()}x)"
      } else if (weeklyCount < historicalWeeklyAvg * 0.75) {
        "${topActivity.key.capitalize()}: ${weeklyCount}x this week (down from usual ${historicalWeeklyAvg.toInt()}x)"
      } else {
        "${topActivity.key.capitalize()}: ${weeklyCount}x this week (consistent with usual)"
      }
    }
    
    return ""
  }

  private fun analyzeDurationVariance(history: History, selectedDate: Calendar, config: Config?): String {
    val weekEntries = getWeekEntries(history, selectedDate)
    val historicalEntries = getThirtyDayEntries(history, selectedDate)
    
    if (weekEntries.size < 2 || historicalEntries.size < 7) return ""
    
    val activityDurations = mutableMapOf<String, MutableList<Double>>()
    val historicalDurations = mutableMapOf<String, MutableList<Double>>()
    
    fun addDurations(entries: List<Entry>, map: MutableMap<String, MutableList<Double>>, endTime: Long) {
      entries.forEachIndexed { index, entry ->
        val normalized = normalizeActivityName(entry.activity, config)
        val nextEntry = entries.getOrNull(index + 1)
        val duration = (nextEntry?.startTime ?: endTime) - entry.startTime
        val hours = duration / 3600.0
        
        if (hours > 0.1 && hours < 24) {
          map.getOrPut(normalized) { mutableListOf() }.add(hours)
        }
      }
    }
    
    addDurations(weekEntries, activityDurations, history.currentActivityStartTime)
    addDurations(historicalEntries, historicalDurations, history.currentActivityStartTime)
    
    val insights = mutableListOf<String>()
    
    activityDurations.forEach { (activity, durations) ->
      val historical = historicalDurations[activity]
      if (historical != null && historical.size >= 3 && durations.isNotEmpty()) {
        val currentAvg = durations.average()
        val historicalAvg = historical.average()
        val variance = abs(currentAvg - historicalAvg)
        
        if (variance > historicalAvg * 0.25) {
          val change = if (currentAvg > historicalAvg) "+${((currentAvg - historicalAvg) * 60).toInt()}min" else "-${((historicalAvg - currentAvg) * 60).toInt()}min"
          insights.add("${activity.capitalize()}: ${String.format("%.1f", currentAvg)}hr average, $change from usual")
        }
      }
    }
    
    return insights.firstOrNull() ?: ""
  }

  private fun analyzeTimeClusteringPatterns(history: History, selectedDate: Calendar, config: Config?): String {
    val weekEntries = getWeekEntries(history, selectedDate)
    val historicalEntries = getThirtyDayEntries(history, selectedDate)
    
    if (weekEntries.isEmpty() || historicalEntries.size < 7) return ""
    
    val activityTimes = mutableMapOf<String, MutableList<Int>>()
    val historicalTimes = mutableMapOf<String, MutableList<Int>>()
    
    fun extractTimes(entries: List<Entry>, map: MutableMap<String, MutableList<Int>>) {
      entries.forEach { entry ->
        val normalized = normalizeActivityName(entry.activity, config)
        val hour = Calendar.getInstance().apply { 
          timeInMillis = entry.startTime * 1000 
        }.get(Calendar.HOUR_OF_DAY)
        
        map.getOrPut(normalized) { mutableListOf() }.add(hour)
      }
    }
    
    extractTimes(weekEntries, activityTimes)
    extractTimes(historicalEntries, historicalTimes)
    
    activityTimes.forEach { (activity, times) ->
      val historical = historicalTimes[activity]
      if (historical != null && historical.size >= 3 && times.isNotEmpty()) {
        val currentAvgHour = times.average().toInt()
        val historicalAvgHour = historical.average().toInt()
        val hourDiff = abs(currentAvgHour - historicalAvgHour)
        
        if (hourDiff >= 2) {
          val timeShift = if (currentAvgHour > historicalAvgHour) "later" else "earlier"
          return "${activity.capitalize()}: typically ${formatHour(historicalAvgHour)}, today at ${formatHour(currentAvgHour)} (${hourDiff}hr $timeShift)"
        }
      }
    }
    
    return ""
  }

  private fun analyzeSequencePatterns(history: History, selectedDate: Calendar, config: Config?): String {
    val weekEntries = getWeekEntries(history, selectedDate)
    val historicalEntries = getThirtyDayEntries(history, selectedDate)
    
    if (weekEntries.size < 2 || historicalEntries.size < 10) return ""
    
    val sequences = mutableMapOf<Pair<String, String>, Int>()
    val historicalSequences = mutableMapOf<Pair<String, String>, Int>()
    
    fun analyzeSequences(entries: List<Entry>, map: MutableMap<Pair<String, String>, Int>) {
      for (i in 0 until entries.size - 1) {
        val current = normalizeActivityName(entries[i].activity, config)
        val next = normalizeActivityName(entries[i + 1].activity, config)
        val pair = Pair(current, next)
        map[pair] = map.getOrDefault(pair, 0) + 1
      }
    }
    
    analyzeSequences(weekEntries, sequences)
    analyzeSequences(historicalEntries, historicalSequences)
    
    val topSequence = sequences.maxByOrNull { it.value }
    if (topSequence != null && topSequence.value >= 2) {
      val historicalCount = historicalSequences[topSequence.key] ?: 0
      val historicalTotal = historicalSequences.values.sum()
      
      if (historicalTotal > 0) {
        val confidence = (historicalCount * 100) / historicalTotal
        if (confidence >= 50) {
          return "${topSequence.key.first.capitalize()} → ${topSequence.key.second.capitalize()} ${confidence}% of time"
        }
      }
    }
    
    return ""
  }

  private fun analyzeMissingRoutines(history: History, selectedDate: Calendar, config: Config?): String {
    val weekEntries = getWeekEntries(history, selectedDate)
    val historicalEntries = getThirtyDayEntries(history, selectedDate)
    
    if (historicalEntries.size < 14) return ""
    
    val weekActivities = weekEntries.map { normalizeActivityName(it.activity, config) }.toSet()
    val historicalFrequency = mutableMapOf<String, Int>()
    
    historicalEntries.forEach { entry ->
      val normalized = normalizeActivityName(entry.activity, config)
      historicalFrequency[normalized] = historicalFrequency.getOrDefault(normalized, 0) + 1
    }
    
    val routineActivities = historicalFrequency.filter { it.value >= 10 }.keys
    val missingRoutines = routineActivities - weekActivities
    
    if (missingRoutines.isNotEmpty()) {
      val topMissing = missingRoutines.maxByOrNull { historicalFrequency[it] ?: 0 }
      if (topMissing != null) {
        val streakDays = calculateStreakLength(historicalEntries, topMissing, config)
        return if (streakDays > 0) {
          "No ${topMissing.capitalize()} logged, ${streakDays}-day streak broken"
        } else {
          "Missing usual ${topMissing.capitalize()} activity this week"
        }
      }
    }
    
    return ""
  }

  private fun calculateStreakLength(entries: List<Entry>, activity: String, config: Config?): Int {
    val dailyOccurrences = entries
      .filter { normalizeActivityName(it.activity, config) == activity }
      .map { 
        Calendar.getInstance().apply { 
          timeInMillis = it.startTime * 1000 
        }.get(Calendar.DAY_OF_YEAR) 
      }
      .toSet()
      .sorted()
      .reversed()
    
    if (dailyOccurrences.isEmpty()) return 0
    
    var streak = 0
    val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    
    for (i in dailyOccurrences.indices) {
      val expectedDay = today - i - 1
      if (dailyOccurrences.contains(expectedDay)) {
        streak++
      } else {
        break
      }
    }
    
    return streak
  }

  private fun getThirtyDayEntries(history: History, selectedDate: Calendar): List<Entry> {
    val thirtyDaysAgo = Calendar.getInstance().apply {
      timeInMillis = selectedDate.timeInMillis
      add(Calendar.DAY_OF_YEAR, -30)
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
    }
    
    val endDate = Calendar.getInstance().apply {
      timeInMillis = selectedDate.timeInMillis
      set(Calendar.HOUR_OF_DAY, 23)
      set(Calendar.MINUTE, 59)
      set(Calendar.SECOND, 59)
    }
    
    return history.entries.filter { entry ->
      val entryTime = entry.startTime * 1000
      entryTime >= thirtyDaysAgo.timeInMillis && entryTime <= endDate.timeInMillis
    }.sortedBy { it.startTime }
  }
  
  private fun getWeekEntries(history: History, selectedDate: Calendar): List<Entry> {
    val weekStart = Calendar.getInstance().apply {
      timeInMillis = selectedDate.timeInMillis
      add(Calendar.DAY_OF_YEAR, -6) // Go back 6 days for 7-day window
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
    }
    
    val weekEnd = Calendar.getInstance().apply {
      timeInMillis = selectedDate.timeInMillis
      set(Calendar.HOUR_OF_DAY, 23)
      set(Calendar.MINUTE, 59)
      set(Calendar.SECOND, 59)
    }
    
    return history.entries.filter { entry ->
      val entryTime = entry.startTime * 1000
      entryTime >= weekStart.timeInMillis && entryTime <= weekEnd.timeInMillis
    }
  }
  
  private fun analyzeLunchBreakPattern(weekEntries: List<Entry>, history: History): String {
    val lunchKeywords = setOf("lunch", "meal", "food", "eat", "break")
    val lunchEntries = weekEntries.filter { entry ->
      lunchKeywords.any { keyword -> entry.activity.lowercase().contains(keyword) }
    }
    
    if (lunchEntries.isEmpty()) return ""
    
    // Group by day to count days with lunch
    val lunchDays = lunchEntries.map { entry ->
      Calendar.getInstance().apply { timeInMillis = entry.startTime * 1000 }.get(Calendar.DAY_OF_YEAR)
    }.toSet()
    
    // Calculate average duration
    val totalDuration = lunchEntries.mapIndexed { index, entry ->
      val nextEntry = weekEntries.find { it.startTime > entry.startTime }
      if (nextEntry != null) {
        nextEntry.startTime - entry.startTime
      } else {
        3600L // Default 1 hour if no next entry
      }
    }.sum()
    
    val avgDurationHours = totalDuration / 3600.0 / lunchEntries.size
    
    return "Lunch Break: ${lunchDays.size}/7 days this week, avg ${String.format("%.1f", avgDurationHours)}hr"
  }
  
  private fun analyzeEveningRelaxationPattern(weekEntries: List<Entry>, history: History): String {
    val relaxKeywords = setOf("relax", "rest", "tv", "evening", "home", "dinner")
    val eveningEntries = weekEntries.filter { entry ->
      val hour = Calendar.getInstance().apply { timeInMillis = entry.startTime * 1000 }.get(Calendar.HOUR_OF_DAY)
      hour >= 17 && relaxKeywords.any { keyword -> entry.activity.lowercase().contains(keyword) }
    }
    
    if (eveningEntries.isEmpty()) return ""
    
    // Calculate consistency (days with evening relaxation)
    val eveningDays = eveningEntries.map { entry ->
      Calendar.getInstance().apply { timeInMillis = entry.startTime * 1000 }.get(Calendar.DAY_OF_YEAR)
    }.toSet()
    
    val consistency = (eveningDays.size * 100) / 7
    
    // Find typical time range
    val startTimes = eveningEntries.map { entry ->
      Calendar.getInstance().apply { timeInMillis = entry.startTime * 1000 }.get(Calendar.HOUR_OF_DAY)
    }
    
    val avgStartHour = if (startTimes.isNotEmpty()) startTimes.average().toInt() else 19
    val avgEndHour = (avgStartHour + 1.5).toInt() // Assume 1.5 hour sessions
    
    return "Evening Relaxation: ${consistency}% consistency, usually ${formatHour(avgStartHour)}-${formatHour(avgEndHour)}"
  }
  
  private fun analyzeWorkSessionPattern(weekEntries: List<Entry>, history: History): String {
    val workKeywords = setOf("work", "meeting", "coding", "programming", "office")
    val workEntries = weekEntries.filter { entry ->
      workKeywords.any { keyword -> entry.activity.lowercase().contains(keyword) }
    }
    
    if (workEntries.isEmpty()) return ""
    
    // Calculate work session durations
    val workDurations = workEntries.mapNotNull { entry ->
      val nextEntry = weekEntries.find { it.startTime > entry.startTime }
      if (nextEntry != null) {
        (nextEntry.startTime - entry.startTime) / 3600.0 // Convert to hours
      } else null
    }
    
    if (workDurations.isEmpty()) return ""
    
    // Find most common work block duration
    val durationRanges = workDurations.map { duration ->
      when {
        duration < 1.0 -> "short"
        duration < 2.0 -> "1hr"
        duration < 3.0 -> "2hr" 
        duration < 4.0 -> "3hr"
        else -> "4hr+"
      }
    }
    
    val mostCommonDuration = durationRanges.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
    val avgDuration = workDurations.average()
    
    return if (mostCommonDuration != null) {
      "${mostCommonDuration} work blocks most common (avg ${String.format("%.1f", avgDuration)}hr)"
    } else {
      "Work Sessions: ${workEntries.size} this week"
    }
  }
  
  private fun analyzeActivityTransitions(weekEntries: List<Entry>, history: History): String {
    if (weekEntries.size < 2) return ""
    
    val workKeywords = setOf("work", "meeting", "coding", "programming", "office")
    val relaxKeywords = setOf("break", "lunch", "relax", "rest", "home", "dinner")
    
    val transitions = mutableListOf<Double>()
    
    for (i in 0 until weekEntries.size - 1) {
      val currentEntry = weekEntries[i]
      val nextEntry = weekEntries[i + 1]
      
      val isCurrentWork = workKeywords.any { it in currentEntry.activity.lowercase() }
      val isNextRelax = relaxKeywords.any { it in nextEntry.activity.lowercase() }
      
      // Calculate gap between work and relaxation activities
      if (isCurrentWork && isNextRelax) {
        val gapHours = (nextEntry.startTime - currentEntry.startTime) / 3600.0
        if (gapHours < 8.0) { // Only consider same-day transitions
          transitions.add(gapHours)
        }
      }
    }
    
    if (transitions.isEmpty()) return ""
    
    val avgGap = transitions.average()
    val typicalGap = when {
      avgGap < 0.5 -> "immediate"
      avgGap < 1.0 -> "30min"
      avgGap < 2.0 -> "1hr"
      avgGap < 3.0 -> "2hr"
      else -> "${avgGap.toInt()}hr"
    }
    
    return "${typicalGap} gap between work and relaxation typical"
  }
  
  fun compareYesterdayVsToday(history: History, selectedDate: Calendar): Triple<String, String, String> {
    val today = getSelectedDayEntries(history, selectedDate)
    val yesterday = getSelectedDayEntries(history, Calendar.getInstance().apply {
      timeInMillis = selectedDate.timeInMillis
      add(Calendar.DAY_OF_YEAR, -1)
    })
    
    // Activity Count Comparison
    val todayCount = today.size
    val yesterdayCount = yesterday.size
    val countDiff = todayCount - yesterdayCount
    val countComparison = when {
      countDiff > 0 -> "$todayCount today, $yesterdayCount yesterday (+$countDiff)"
      countDiff < 0 -> "$todayCount today, $yesterdayCount yesterday ($countDiff)"
      else -> "$todayCount today, $yesterdayCount yesterday (same)"
    }
    
    // Active Hours Comparison
    val todayHours = calculateActiveHours(today, history.currentActivityStartTime)
    val yesterdayHours = calculateActiveHours(yesterday, getEndTimeForYesterday(yesterday))
    val hoursDiff = todayHours - yesterdayHours
    val hoursComparison = when {
      hoursDiff > 0 -> "${String.format("%.1f", todayHours)}hr today, ${String.format("%.1f", yesterdayHours)}hr yesterday (+${String.format("%.1f", hoursDiff)}hr)"
      hoursDiff < 0 -> "${String.format("%.1f", todayHours)}hr today, ${String.format("%.1f", yesterdayHours)}hr yesterday (${String.format("%.1f", hoursDiff)}hr)"
      else -> "${String.format("%.1f", todayHours)}hr today, ${String.format("%.1f", yesterdayHours)}hr yesterday (same)"
    }
    
    // Day Start Comparison
    val todayStart = if (today.isNotEmpty()) formatTime(Date(today.first().startTime * 1000)) else "No data"
    val yesterdayStart = if (yesterday.isNotEmpty()) formatTime(Date(yesterday.first().startTime * 1000)) else "No data"
    
    val startComparison = if (today.isNotEmpty() && yesterday.isNotEmpty()) {
      val todayStartMinutes = getMinutesFromMidnight(today.first().startTime)
      val yesterdayStartMinutes = getMinutesFromMidnight(yesterday.first().startTime)
      val diffMinutes = todayStartMinutes - yesterdayStartMinutes
      
      when {
        diffMinutes > 0 -> "$todayStart today, $yesterdayStart yesterday (${diffMinutes}min later)"
        diffMinutes < 0 -> "$todayStart today, $yesterdayStart yesterday (${-diffMinutes}min earlier)"
        else -> "$todayStart today, $yesterdayStart yesterday (same time)"
      }
    } else {
      "$todayStart today, $yesterdayStart yesterday"
    }
    
    return Triple(countComparison, hoursComparison, startComparison)
  }
  
  private fun getSelectedDayEntries(history: History, date: Calendar): List<Entry> {
    return history.entries.filter { entry ->
      isSelectedDay(entry.startTime * 1000, date)
    }.sortedBy { it.startTime }
  }
  
  private fun calculateActiveHours(entries: List<Entry>, endTime: Long): Double {
    if (entries.isEmpty()) return 0.0
    val totalSeconds = endTime - entries.first().startTime
    return totalSeconds / 3600.0
  }
  
  private fun getEndTimeForYesterday(yesterdayEntries: List<Entry>): Long {
    if (yesterdayEntries.isEmpty()) return 0L
    // Assume last entry lasted 1 hour if we don't have today's data
    return yesterdayEntries.last().startTime + 3600
  }
  
  private fun getMinutesFromMidnight(timestamp: Long): Int {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp * 1000 }
    return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
  }
  
  private fun formatTime(date: Date): String {
    val cal = Calendar.getInstance().apply { time = date }
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val minute = cal.get(Calendar.MINUTE)
    return when {
      hour == 0 -> "12:${String.format("%02d", minute)} AM"
      hour < 12 -> "$hour:${String.format("%02d", minute)} AM"
      hour == 12 -> "12:${String.format("%02d", minute)} PM"
      else -> "${hour - 12}:${String.format("%02d", minute)} PM"
    }
  }

  data class DayMetrics(
    val timeAllocation: Map<String, Double>, // category -> hours
    val totalActiveTime: Double, // hours of logged activities
    val totalAwakeTime: Double, // estimated awake hours (16hr default)
    val unaccountedTime: Double, // gaps between activities
    val activityCount: Int,
    val categoryCount: Int,
    val dailyGoalsCompleted: Int,
    val dailyGoalsTotal: Int,
    val goalCompletionRate: Int // percentage
  )

  fun calculateDayMetrics(history: History, selectedDate: Calendar, config: Config?): DayMetrics {
    val selectedDayEntries = history.entries.filter { 
      isSelectedDay(it.startTime * 1000, selectedDate)
    }.sortedBy { it.startTime }

    // Calculate goal completion for daily goals
    val activeGoals = config?.weeklyGoals?.filter { it.isActive && it.frequency == GoalFrequency.DAILY } ?: emptyList()
    val dailyGoalsTotal = activeGoals.size
    val dailyGoalsCompleted = if (selectedDayEntries.isNotEmpty()) {
      activeGoals.count { goal ->
        selectedDayEntries.any { entry ->
          val normalizedActivity = normalizeActivityName(entry.activity, config)
          val normalizedGoal = normalizeActivityName(goal.activity, config)
          normalizedActivity == normalizedGoal
        }
      }
    } else 0
    
    val goalCompletionRate = if (dailyGoalsTotal > 0) {
      (dailyGoalsCompleted * 100) / dailyGoalsTotal
    } else 100

    if (selectedDayEntries.isEmpty()) {
      return DayMetrics(
        timeAllocation = emptyMap(),
        totalActiveTime = 0.0,
        totalAwakeTime = 16.0,
        unaccountedTime = 16.0,
        activityCount = 0,
        categoryCount = 0,
        dailyGoalsCompleted = dailyGoalsCompleted,
        dailyGoalsTotal = dailyGoalsTotal,
        goalCompletionRate = goalCompletionRate
      )
    }

    // Calculate time allocation by category using user-defined groups
    val categoryTime = mutableMapOf<String, Double>()
    
    selectedDayEntries.forEachIndexed { index, entry ->
      val category = config?.getActivityGroup(entry.activity) ?: entry.activity
      val nextEntry = selectedDayEntries.getOrNull(index + 1)
      val endTime = nextEntry?.startTime ?: history.currentActivityStartTime
      val duration = (endTime - entry.startTime) / 3600.0 // Convert to hours
      
      if (duration > 0 && duration < 24) { // Sanity check
        categoryTime[category] = categoryTime.getOrDefault(category, 0.0) + duration
      }
    }

    // Calculate total active time
    val firstEntry = selectedDayEntries.first()
    val lastEntryEnd = if (selectedDayEntries.size > 1) {
      val lastEntry = selectedDayEntries.last()
      val nextEntry = history.entries.find { it.startTime > lastEntry.startTime }
      nextEntry?.startTime ?: history.currentActivityStartTime
    } else {
      history.currentActivityStartTime
    }
    
    val totalActiveTime = (lastEntryEnd - firstEntry.startTime) / 3600.0
    val totalLoggedTime = categoryTime.values.sum()
    val unaccountedTime = maxOf(0.0, totalActiveTime - totalLoggedTime)
    
    // Assume 16 hours awake time (6AM-10PM default)
    val awakeTime = 16.0
    
    return DayMetrics(
      timeAllocation = categoryTime,
      totalActiveTime = totalLoggedTime,
      totalAwakeTime = awakeTime,
      unaccountedTime = unaccountedTime,
      activityCount = selectedDayEntries.size,
      categoryCount = categoryTime.size,
      dailyGoalsCompleted = dailyGoalsCompleted,
      dailyGoalsTotal = dailyGoalsTotal,
      goalCompletionRate = goalCompletionRate
    )
  }

  fun formatDayMetricsText(metrics: DayMetrics): String {
    if (metrics.activityCount == 0) {
      val goalText = if (metrics.dailyGoalsTotal > 0) {
        "Goals: 0/${metrics.dailyGoalsTotal}"
      } else ""
      return if (goalText.isNotEmpty()) {
        "No activities logged today\n$goalText"
      } else {
        "No activities logged today"
      }
    }

    val parts = mutableListOf<String>()
    
    // Time allocation (top 3 categories)
    val topCategories = metrics.timeAllocation.entries
      .sortedByDescending { it.value }
      .take(3)
      .map { "${it.key}: ${String.format("%.1f", it.value)}hr" }
    
    if (topCategories.isNotEmpty()) {
      parts.add(topCategories.joinToString(" | "))
    }
    
    // Create second line with key metrics
    val secondLineParts = mutableListOf<String>()
    
    // Daily goals if any exist
    if (metrics.dailyGoalsTotal > 0) {
      secondLineParts.add("Goals: ${metrics.dailyGoalsCompleted}/${metrics.dailyGoalsTotal}")
    }
    
    // Activity coverage
    val coverage = if (metrics.totalAwakeTime > 0) {
      ((metrics.totalActiveTime / metrics.totalAwakeTime) * 100).toInt()
    } else 0
    
    secondLineParts.add("Coverage: ${coverage}%")
    
    // Add active time details
    secondLineParts.add("${String.format("%.1f", metrics.totalActiveTime)}hr active")
    
    if (secondLineParts.isNotEmpty()) {
      parts.add(secondLineParts.joinToString(" | "))
    }
    
    // Gaps if significant (third line)
    if (metrics.unaccountedTime > 0.5) {
      parts.add("Gaps: ${String.format("%.1f", metrics.unaccountedTime)}hr unaccounted")
    }
    
    return parts.joinToString("\n")
  }

  fun analyzeMoodAndEnergy(history: History, selectedDate: Calendar): Pair<String, String> {
    val selectedDayEntries = history.entries.filter { 
      isSelectedDay(it.startTime * 1000, selectedDate)
    }

    if (selectedDayEntries.isEmpty()) {
      return Pair("Unknown", "Unknown")
    }

    // Energy analysis based on activity types and frequency
    val highEnergyActivities = setOf("exercise", "workout", "gym", "work", "coding", "meeting")
    val lowEnergyActivities = setOf("rest", "sleep", "relax", "tv", "reading")

    val highEnergyTime = selectedDayEntries.mapIndexed { index, entry ->
      if (highEnergyActivities.any { it in entry.activity.lowercase() }) {
        val nextEntry = selectedDayEntries.getOrNull(index + 1)
        (nextEntry?.startTime ?: history.currentActivityStartTime) - entry.startTime
      } else 0L
    }.sum()

    val lowEnergyTime = selectedDayEntries.mapIndexed { index, entry ->
      if (lowEnergyActivities.any { it in entry.activity.lowercase() }) {
        val nextEntry = selectedDayEntries.getOrNull(index + 1)
        (nextEntry?.startTime ?: history.currentActivityStartTime) - entry.startTime
      } else 0L
    }.sum()

    val energy = when {
      highEnergyTime > lowEnergyTime * 1.5 -> "High"
      lowEnergyTime > highEnergyTime * 1.5 -> "Low"
      else -> "Medium"
    }

    // Mood analysis based on activity balance and variety
    val socialActivities = setOf("meeting", "family", "friends", "social")
    val stressfulActivities = setOf("work", "email", "urgent", "deadline")
    val relaxingActivities = setOf("break", "lunch", "walk", "music", "hobby")

    val socialTime = selectedDayEntries.mapIndexed { index, entry ->
      if (socialActivities.any { it in entry.activity.lowercase() }) {
        val nextEntry = selectedDayEntries.getOrNull(index + 1)
        (nextEntry?.startTime ?: history.currentActivityStartTime) - entry.startTime
      } else 0L
    }.sum()

    val stressTime = selectedDayEntries.mapIndexed { index, entry ->
      if (stressfulActivities.any { it in entry.activity.lowercase() }) {
        val nextEntry = selectedDayEntries.getOrNull(index + 1)
        (nextEntry?.startTime ?: history.currentActivityStartTime) - entry.startTime
      } else 0L
    }.sum()

    val relaxTime = selectedDayEntries.mapIndexed { index, entry ->
      if (relaxingActivities.any { it in entry.activity.lowercase() }) {
        val nextEntry = selectedDayEntries.getOrNull(index + 1)
        (nextEntry?.startTime ?: history.currentActivityStartTime) - entry.startTime
      } else 0L
    }.sum()

    val mood = when {
      relaxTime > stressTime && socialTime > 0 -> "Happy"
      relaxTime > stressTime -> "Calm"
      stressTime > relaxTime * 2 -> "Stressed"
      socialTime > 0 -> "Social"
      else -> "Neutral"
    }

    return Pair(energy, mood)
  }

  private fun formatHour(hour: Int): String {
    return when {
      hour == 0 -> "12 AM"
      hour < 12 -> "$hour AM"
      hour == 12 -> "12 PM"
      else -> "${hour - 12} PM"
    }
  }

  private fun isSelectedDay(timestamp: Long, selectedDate: Calendar): Boolean {
    val date = Calendar.getInstance().apply { timeInMillis = timestamp }
    return selectedDate.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
           selectedDate.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
  }
}