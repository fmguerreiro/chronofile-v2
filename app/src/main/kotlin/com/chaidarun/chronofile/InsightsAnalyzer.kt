// © Art Chaidarun

package com.chaidarun.chronofile

import java.util.Calendar

class InsightsAnalyzer {

  fun generateProductivityInsights(history: History, selectedDate: Calendar): String {
    val selectedDayEntries = history.entries.filter { 
      isSelectedDay(it.startTime * 1000, selectedDate)
    }

    if (selectedDayEntries.isEmpty()) {
      return "Start logging activities to see your productivity patterns!"
    }

    // Find most productive time period
    val hourlyProductivity = mutableMapOf<Int, Long>()

    selectedDayEntries.forEachIndexed { index, entry ->
      val startHour = Calendar.getInstance().apply {
        timeInMillis = entry.startTime * 1000
      }.get(Calendar.HOUR_OF_DAY)

      val nextEntry = selectedDayEntries.getOrNull(index + 1)
      val duration = (nextEntry?.startTime ?: history.currentActivityStartTime) - entry.startTime
      hourlyProductivity[startHour] = hourlyProductivity.getOrDefault(startHour, 0) + duration
    }

    val peakHour = hourlyProductivity.maxByOrNull { it.value }?.key
    val peakHourText = if (peakHour != null) {
      val endHour = (peakHour + 1) % 24
      "${formatHour(peakHour)} and ${formatHour(endHour)}"
    } else {
      "various times"
    }

    // Analyze work-life balance
    val workActivities = setOf("work", "meeting", "email", "coding", "programming")
    val relaxActivities = setOf("break", "lunch", "dinner", "relax", "rest", "home")

    val workTime = selectedDayEntries.mapIndexed { index, entry ->
      if (workActivities.any { work -> entry.activity.lowercase().contains(work) }) {
        val nextEntry = selectedDayEntries.getOrNull(index + 1)
        (nextEntry?.startTime ?: history.currentActivityStartTime) - entry.startTime
      } else 0L
    }.sum()

    val relaxTime = selectedDayEntries.mapIndexed { index, entry ->
      if (relaxActivities.any { relax -> entry.activity.lowercase().contains(relax) }) {
        val nextEntry = selectedDayEntries.getOrNull(index + 1)
        (nextEntry?.startTime ?: history.currentActivityStartTime) - entry.startTime
      } else 0L
    }.sum()

    val balanceText = when {
      workTime > relaxTime * 2 -> "You've been working hard today."
      relaxTime > workTime * 2 -> "You've had plenty of relaxation time."
      else -> "Your day is balanced with a good mix of work and relaxation."
    }

    return "You've been most productive between $peakHourText. $balanceText"
  }

  fun calculateBalanceScore(history: History, selectedDate: Calendar): Int {
    val selectedDayEntries = history.entries.filter { 
      isSelectedDay(it.startTime * 1000, selectedDate)
    }

    if (selectedDayEntries.isEmpty()) return 5

    // Score based on variety of activities, work-life balance, and productivity
    val uniqueActivities = selectedDayEntries.map { it.activity.lowercase() }.toSet().size
    val varietyScore = minOf(uniqueActivities * 2, 4) // Max 4 points for variety

    // Work-life balance score
    val workKeywords = setOf("work", "meeting", "email", "coding")
    val personalKeywords = setOf("break", "lunch", "relax", "exercise", "family")

    val workTime = selectedDayEntries.mapIndexed { index, entry ->
      if (workKeywords.any { it in entry.activity.lowercase() }) {
        val nextEntry = selectedDayEntries.getOrNull(index + 1)
        (nextEntry?.startTime ?: history.currentActivityStartTime) - entry.startTime
      } else 0L
    }.sum()

    val personalTime = selectedDayEntries.mapIndexed { index, entry ->
      if (personalKeywords.any { it in entry.activity.lowercase() }) {
        val nextEntry = selectedDayEntries.getOrNull(index + 1)
        (nextEntry?.startTime ?: history.currentActivityStartTime) - entry.startTime
      } else 0L
    }.sum()

    val totalTime = workTime + personalTime
    val balanceScore = if (totalTime > 0) {
      val workRatio = workTime.toDouble() / totalTime
      when {
        workRatio in 0.4..0.7 -> 4 // Good balance
        workRatio in 0.3..0.8 -> 3 // Okay balance
        else -> 2 // Poor balance
      }
    } else 2

    // Activity duration consistency (not too short, not too long)
    val durations = selectedDayEntries.mapIndexed { index, entry ->
      val nextEntry = selectedDayEntries.getOrNull(index + 1)
      (nextEntry?.startTime ?: history.currentActivityStartTime) - entry.startTime
    }
    val avgDuration = if (durations.isNotEmpty()) durations.average() else 0.0
    val consistencyScore = when {
      avgDuration > 1800 && avgDuration < 7200 -> 2 // 30min - 2hrs is good
      else -> 1
    }

    return minOf(varietyScore + balanceScore + consistencyScore, 10)
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