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
    val currentMinute = ((now % 3600) / 60).toInt()
    val currentTimeInMinutes = currentHour * 60 + currentMinute
    
    // Look at the last 30 days for pattern analysis
    val thirtyDaysAgo = now - (30 * DAY_SECONDS)
    val recentEntries = entries.filter { it.startTime >= thirtyDaysAgo }
    
    if (recentEntries.isEmpty()) {
      return listOf("Work", "Break", "Lunch", "Meeting")
    }
    
    // Group activities by time windows and calculate scores
    val timeWindowSize = 60 // 1-hour windows for broader context
    val currentTimeWindow = currentTimeInMinutes / timeWindowSize
    
    // Calculate activity scores based on multiple factors
    val activityScores = mutableMapOf<String, Double>()
    
    recentEntries.forEach { entry ->
      val entryTime = entry.startTime
      val entryHour = ((entryTime % DAY_SECONDS) / 3600).toInt()
      val entryMinute = ((entryTime % 3600) / 60).toInt()
      val entryTimeInMinutes = entryHour * 60 + entryMinute
      val entryTimeWindow = entryTimeInMinutes / timeWindowSize
      
      val activity = entry.activity
      val currentScore = activityScores.getOrDefault(activity, 0.0)
      
      // Base frequency score
      var score = 1.0
      
      // Time context bonus (higher score for activities at similar times)
      val timeDifference = kotlin.math.abs(entryTimeWindow - currentTimeWindow)
      val timeBonus = when {
        timeDifference == 0 -> 3.0 // Same hour window
        timeDifference == 1 -> 2.0 // Adjacent hour
        timeDifference <= 2 -> 1.5 // Within 2 hours
        timeDifference <= 4 -> 1.2 // Within 4 hours
        else -> 1.0 // No time bonus
      }
      score *= timeBonus
      
      // Recency bonus (more recent entries get higher weight)
      val daysSinceEntry = (now - entryTime) / DAY_SECONDS.toDouble()
      val recencyBonus = when {
        daysSinceEntry <= 1 -> 1.5 // Last day
        daysSinceEntry <= 3 -> 1.3 // Last 3 days
        daysSinceEntry <= 7 -> 1.2 // Last week
        daysSinceEntry <= 14 -> 1.1 // Last 2 weeks
        else -> 1.0 // No recency bonus
      }
      score *= recencyBonus
      
      // Day of week pattern bonus
      val currentDayOfWeek = ((now / DAY_SECONDS) % 7).toInt()
      val entryDayOfWeek = ((entryTime / DAY_SECONDS) % 7).toInt()
      val dayOfWeekBonus = if (currentDayOfWeek == entryDayOfWeek) 1.3 else 1.0
      score *= dayOfWeekBonus
      
      activityScores[activity] = currentScore + score
    }
    
    // Sort by combined score and return top 4
    return activityScores.entries
      .sortedByDescending { it.value }
      .take(4)
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
