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

object WeeklyNotificationManager {
  
  private const val CHANNEL_ID = "weekly_goals_results"
  private const val NOTIFICATION_ID = 1001
  private const val ALARM_REQUEST_CODE = 2001
  
  fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val name = "Weekly Goals Results"
      val description = "Notifications for weekly goal progress and results"
      val importance = NotificationManager.IMPORTANCE_DEFAULT
      val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
        this.description = description
      }
      
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      notificationManager.createNotificationChannel(channel)
    }
  }
  
  fun scheduleWeeklyNotification(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, WeeklyResultsReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      ALARM_REQUEST_CODE,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    
    // Schedule for next Monday at 9 AM
    val calendar = Calendar.getInstance().apply {
      set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
      set(Calendar.HOUR_OF_DAY, 9)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
      
      // If it's already past this Monday 9 AM, schedule for next Monday
      if (timeInMillis <= System.currentTimeMillis()) {
        add(Calendar.WEEK_OF_YEAR, 1)
      }
    }
    
    // Use setRepeating for weekly notifications
    alarmManager.setRepeating(
      AlarmManager.RTC_WAKEUP,
      calendar.timeInMillis,
      AlarmManager.INTERVAL_DAY * 7, // Weekly
      pendingIntent
    )
  }
  
  fun cancelWeeklyNotification(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, WeeklyResultsReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      ALARM_REQUEST_CODE,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    
    alarmManager.cancel(pendingIntent)
  }
  
  fun showWeeklyResultsNotification(context: Context) {
    val results = calculateLastWeekResults()
    
    if (results.isEmpty()) {
      return // No goals to report
    }
    
    val (title, content) = formatNotificationContent(results)
    
    // Create intent to open WeeklyGoalsActivity
    val intent = Intent(context, WeeklyGoalsActivity::class.java)
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
      
      WeeklyGoalResult(
        activity = goal.activity,
        targetHours = goal.targetHours,
        actualHours = actualHours,
        completionPercentage = ((actualHours / goal.targetHours) * 100).toInt().coerceAtMost(100)
      )
    }
  }
  
  private fun formatNotificationContent(results: List<WeeklyGoalResult>): Pair<String, String> {
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

data class WeeklyGoalResult(
  val activity: String,
  val targetHours: Double,
  val actualHours: Double,
  val completionPercentage: Int
)

class WeeklyResultsReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    WeeklyNotificationManager.showWeeklyResultsNotification(context)
  }
}