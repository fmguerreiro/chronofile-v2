package com.chaidarun.chronofile

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.*

object GoalNotificationManager {
  
  private const val CHANNEL_ID = "goal_results"
  private const val NOTIFICATION_ID = 1001
  private const val ALARM_REQUEST_CODE = 2001
  
  fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val name = "Goal Results"
      val description = "Notifications for goal progress and results"
      val importance = NotificationManager.IMPORTANCE_DEFAULT
      val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
        this.description = description
      }
      
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      notificationManager.createNotificationChannel(channel)
    }
  }
  
  fun scheduleDailyGoalCheck(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, DailyGoalCheckReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      ALARM_REQUEST_CODE,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    
    // Schedule for tomorrow at 9 AM
    val calendar = Calendar.getInstance().apply {
      add(Calendar.DAY_OF_YEAR, 1) // Tomorrow
      set(Calendar.HOUR_OF_DAY, 9)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    
    // Use setRepeating for daily notifications
    alarmManager.setRepeating(
      AlarmManager.RTC_WAKEUP,
      calendar.timeInMillis,
      AlarmManager.INTERVAL_DAY, // Daily
      pendingIntent
    )
  }
  
  fun cancelDailyGoalCheck(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, DailyGoalCheckReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      ALARM_REQUEST_CODE,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    
    alarmManager.cancel(pendingIntent)
  }
  
  fun checkAndShowGoalNotifications(context: Context) {
    val config = Store.state.config ?: return
    val history = Store.state.history ?: return
    val goals = config.weeklyGoals ?: return
    
    if (goals.isEmpty()) return
    
    val calendar = Calendar.getInstance()
    val today = calendar.get(Calendar.DAY_OF_WEEK)
    val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
    val isLastDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH) == calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    
    val goalResults = mutableListOf<GoalResult>()
    
    for (goal in goals) {
      if (!goal.isActive) continue
      
      val frequency = goal.frequency ?: GoalFrequency.WEEKLY
      val shouldNotify = when (frequency) {
        GoalFrequency.DAILY -> true // Check daily goals every day
        GoalFrequency.WEEKLY -> today == Calendar.MONDAY // Check weekly goals on Monday
        GoalFrequency.MONTHLY -> isLastDayOfMonth // Check monthly goals on last day of month
      }
      
      if (shouldNotify) {
        val result = calculateGoalResult(goal, history, frequency)
        goalResults.add(result)
      }
    }
    
    if (goalResults.isEmpty()) return
    
    val (title, content) = formatNotificationContent(goalResults)
    
    // Create intent to open GoalsActivity
    val intent = Intent(context, GoalsActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(
      context,
      0,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    
    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher_round)
      .setContentTitle(title)
      .setContentText(content)
      .setStyle(NotificationCompat.BigTextStyle().bigText(content))
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setContentIntent(pendingIntent)
      .setAutoCancel(true)
      .build()
    
    with(NotificationManagerCompat.from(context)) {
      notify(NOTIFICATION_ID, notification)
    }
  }
  
  private fun calculateGoalResult(goal: WeeklyGoal, history: History, frequency: GoalFrequency): GoalResult {
    val (startTime, endTime) = getTimeRangeForFrequency(frequency)
    
    val actualHours = history.entries
      .filter { entry ->
        entry.startTime >= startTime && 
        entry.startTime < endTime &&
        entry.activity == goal.activity
      }
      .sumOf { entry ->
        val nextEntry = history.entries.find { it.startTime > entry.startTime }
        val calculatedEndTime = nextEntry?.startTime ?: endTime
        val durationSeconds = (calculatedEndTime - entry.startTime).coerceAtLeast(0)
        durationSeconds / 3600.0 // Convert to hours
      }
    
    val completionPercentage = ((actualHours / goal.targetHours) * 100).toInt().coerceAtMost(100)
    
    return GoalResult(
      activity = goal.activity,
      targetHours = goal.targetHours,
      actualHours = actualHours,
      completionPercentage = completionPercentage,
      frequency = frequency
    )
  }
  
  private fun getTimeRangeForFrequency(frequency: GoalFrequency): Pair<Long, Long> {
    val calendar = Calendar.getInstance()
    val now = System.currentTimeMillis() / 1000
    
    return when (frequency) {
      GoalFrequency.DAILY -> {
        // Yesterday
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis / 1000
        val endTime = startTime + (24 * 60 * 60) // 24 hours
        Pair(startTime, endTime)
      }
      GoalFrequency.WEEKLY -> {
        // Last week (Monday to Sunday)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.add(Calendar.WEEK_OF_YEAR, -1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis / 1000
        val endTime = startTime + (7 * 24 * 60 * 60) // 7 days
        Pair(startTime, endTime)
      }
      GoalFrequency.MONTHLY -> {
        // Last month
        calendar.add(Calendar.MONTH, -1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis / 1000
        
        val endCalendar = Calendar.getInstance()
        endCalendar.time = calendar.time
        endCalendar.add(Calendar.MONTH, 1)
        val endTime = endCalendar.timeInMillis / 1000
        Pair(startTime, endTime)
      }
    }
  }
  
  private fun calculateLastWeekResults(): List<WeeklyGoalResult> {
    val config = Store.state.config ?: return emptyList()
    val history = Store.state.history ?: return emptyList()
    val goals = config.weeklyGoals ?: return emptyList()
    
    // Calculate last week's date range (Monday to Sunday)
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    calendar.add(Calendar.WEEK_OF_YEAR, -1) // Last week
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    
    val lastWeekStart = calendar.timeInMillis / 1000
    val lastWeekEnd = lastWeekStart + (7 * 24 * 60 * 60) // 7 days in seconds
    
    return goals.mapNotNull { goal ->
      if (!goal.isActive) return@mapNotNull null
      
      val actualHours = history.entries
        .filter { entry ->
          entry.startTime >= lastWeekStart && 
          entry.startTime < lastWeekEnd &&
          entry.activity == goal.activity
        }
        .sumOf { entry ->
          val nextEntry = history.entries.find { it.startTime > entry.startTime }
          val endTime = nextEntry?.startTime ?: lastWeekEnd
          val durationSeconds = (endTime - entry.startTime).coerceAtLeast(0)
          durationSeconds / 3600.0 // Convert to hours
        }
      
      // Convert target hours to weekly equivalent for consistent reporting
      val frequency = goal.frequency ?: GoalFrequency.WEEKLY
      val weeklyTargetHours = when (frequency) {
        GoalFrequency.DAILY -> goal.targetHours * 7.0 // Daily * 7 days
        GoalFrequency.WEEKLY -> goal.targetHours // Already weekly
        GoalFrequency.MONTHLY -> goal.targetHours / 4.29 // Monthly / ~4.29 weeks per month
      }
      
      WeeklyGoalResult(
        activity = goal.activity,
        targetHours = weeklyTargetHours,
        actualHours = actualHours,
        completionPercentage = ((actualHours / weeklyTargetHours) * 100).toInt().coerceAtMost(100)
      )
    }
  }
  
  private fun formatNotificationContent(results: List<GoalResult>): Pair<String, String> {
    val completed = results.count { it.completionPercentage >= 100 }
    val total = results.size
    val averageCompletion = results.map { it.completionPercentage }.average().toInt()
    
    val title = "Last Week's Goals: $completed/$total completed"
    
    val topResults = results
      .sortedByDescending { it.completionPercentage }
      .take(3)
      .joinToString("\n") { result ->
        val status = when {
          result.completionPercentage >= 100 -> "✅"
          result.completionPercentage >= 75 -> "🟡"
          else -> "❌"
        }
        "$status ${result.activity}: ${String.format("%.1f", result.actualHours)}h/${result.targetHours}h (${result.completionPercentage}%)"
      }
    
    val content = "Average completion: $averageCompletion%\n\n$topResults"
    
    return Pair(title, content)
  }
}

data class GoalResult(
  val activity: String,
  val targetHours: Double,
  val actualHours: Double,
  val completionPercentage: Int,
  val frequency: GoalFrequency
)

data class WeeklyGoalResult(
  val activity: String,
  val targetHours: Double,
  val actualHours: Double,
  val completionPercentage: Int
)

class DailyGoalCheckReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    GoalNotificationManager.checkAndShowGoalNotifications(context)
  }
}

class WeeklyResultsReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    // Legacy compatibility - redirect to new daily check
    GoalNotificationManager.checkAndShowGoalNotifications(context)
  }
}