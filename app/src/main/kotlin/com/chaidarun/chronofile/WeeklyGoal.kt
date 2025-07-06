package com.chaidarun.chronofile

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class WeeklyGoal(
  @Expose @SerializedName("id") val id: String,
  @Expose @SerializedName("activity") val activity: String,
  @Expose @SerializedName("targetHours") val targetHours: Double,
  @Expose @SerializedName("weekStartTimestamp") val weekStartTimestamp: Long,
  @Expose @SerializedName("isActive") val isActive: Boolean = true
) {
  fun getWeekEndTimestamp(): Long = weekStartTimestamp + (7 * 24 * 60 * 60 * 1000)
  
  fun isCurrentWeek(): Boolean {
    val now = System.currentTimeMillis()
    return now >= weekStartTimestamp && now < getWeekEndTimestamp()
  }
}