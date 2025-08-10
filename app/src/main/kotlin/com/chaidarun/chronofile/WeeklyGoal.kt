package com.chaidarun.chronofile

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

enum class GoalFrequency {
  DAILY,
  WEEKLY,
  MONTHLY;
  
  fun displayName(): String {
    return when (this) {
      DAILY -> "Daily"
      WEEKLY -> "Weekly"
      MONTHLY -> "Monthly"
    }
  }
}

data class WeeklyGoal(
  @Expose @SerializedName("id") val id: String,
  @Expose @SerializedName("activity") val activity: String,
  @Expose @SerializedName("targetHours") val targetHours: Double,
  @Expose @SerializedName("weekStartTimestamp") val weekStartTimestamp: Long,
  @Expose @SerializedName("isActive") val isActive: Boolean = true,
  @Expose @SerializedName("frequency") val frequency: GoalFrequency? = null
) {
  fun getWeekEndTimestamp(): Long = weekStartTimestamp + (7 * 24 * 60 * 60 * 1000)
  
  fun isCurrentWeek(): Boolean {
    // For recurring goals, they should always be considered "current" if active
    // The weekStartTimestamp is just for historical tracking, not for filtering visibility
    return isActive
  }
}