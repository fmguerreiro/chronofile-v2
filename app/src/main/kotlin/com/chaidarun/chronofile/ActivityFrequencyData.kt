// © Art Chaidarun

package com.chaidarun.chronofile

import java.util.*
import kotlin.math.abs

data class TimeSlotData(
    val slotIndex: Int,
    val dominantActivity: String?,
    val frequency: Float,
    val consistency: Float,
    val totalMinutes: Int,
    val activityBreakdown: Map<String, Int>
) {
    val isEmpty: Boolean get() = dominantActivity == null
    val startTime: String get() {
        val hour = slotIndex / 2
        val minute = if (slotIndex % 2 == 0) 0 else 30
        return String.format("%02d:%02d", hour, minute)
    }
}

data class ActivityFrequencyData(
    val timeSlots: Array<TimeSlotData>,
    val weekEntries: List<Entry>,
    val totalDays: Int
) {
    companion object {
        const val SLOTS_PER_DAY = 48
        const val MINUTES_PER_SLOT = 30
        
        fun fromHistory(history: History, selectedDate: Calendar): ActivityFrequencyData {
            // Get 7-day period ending with selected date
            val weekStart = Calendar.getInstance().apply {
                timeInMillis = selectedDate.timeInMillis
                add(Calendar.DAY_OF_YEAR, -6)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            val weekEnd = Calendar.getInstance().apply {
                timeInMillis = selectedDate.timeInMillis
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            
            val weekEntries = history.entries.filter { entry ->
                val entryTime = entry.startTime * 1000
                entryTime >= weekStart.timeInMillis && entryTime <= weekEnd.timeInMillis
            }
            
            // Create 48 time slots (00:00-23:30 in 30-minute intervals)
            val timeSlots = Array(SLOTS_PER_DAY) { slotIndex ->
                analyzeTimeSlot(slotIndex, weekEntries, weekStart.timeInMillis, weekEnd.timeInMillis)
            }
            
            return ActivityFrequencyData(timeSlots, weekEntries, 7)
        }
        
        private fun analyzeTimeSlot(
            slotIndex: Int, 
            weekEntries: List<Entry>, 
            weekStartMillis: Long, 
            weekEndMillis: Long
        ): TimeSlotData {
            val slotStartMinutes = slotIndex * MINUTES_PER_SLOT
            val slotEndMinutes = slotStartMinutes + MINUTES_PER_SLOT
            
            // Track activities that occur during this time slot across all days
            val activityMinutes = mutableMapOf<String, Int>()
            val dayOccurrences = mutableMapOf<String, MutableSet<Int>>() // Track which days each activity occurs
            
            // Check each day of the week
            for (dayOffset in 0 until 7) {
                val dayStart = weekStartMillis + (dayOffset * DAY_SECONDS * 1000)
                val dayEnd = dayStart + (DAY_SECONDS * 1000) - 1
                
                val dayEntries = weekEntries.filter { entry ->
                    val entryTime = entry.startTime * 1000
                    entryTime >= dayStart && entryTime <= dayEnd
                }.sortedBy { it.startTime }
                
                // Find activities during this time slot for this specific day
                for (i in dayEntries.indices) {
                    val entry = dayEntries[i]
                    val nextEntry = dayEntries.getOrNull(i + 1)
                    
                    val entryStartMillis = entry.startTime * 1000
                    val entryEndMillis = nextEntry?.startTime?.times(1000) ?: dayEnd
                    
                    // Convert to minutes within the day
                    val entryStartMinutes = ((entryStartMillis - dayStart) / 60000).toInt()
                    val entryEndMinutes = ((entryEndMillis - dayStart) / 60000).toInt()
                    
                    // Calculate overlap with current time slot
                    val overlapStart = maxOf(slotStartMinutes, entryStartMinutes)
                    val overlapEnd = minOf(slotEndMinutes, entryEndMinutes)
                    
                    if (overlapStart < overlapEnd) {
                        val overlapMinutes = overlapEnd - overlapStart
                        activityMinutes[entry.activity] = activityMinutes.getOrDefault(entry.activity, 0) + overlapMinutes
                        
                        // Track day occurrence
                        dayOccurrences.getOrPut(entry.activity) { mutableSetOf() }.add(dayOffset)
                    }
                }
            }
            
            if (activityMinutes.isEmpty()) {
                return TimeSlotData(
                    slotIndex = slotIndex,
                    dominantActivity = null,
                    frequency = 0f,
                    consistency = 0f,
                    totalMinutes = 0,
                    activityBreakdown = emptyMap()
                )
            }
            
            // Find dominant activity (most total minutes)
            val dominantEntry = activityMinutes.maxByOrNull { it.value }!!
            val dominantActivity = dominantEntry.key
            val totalMinutes = activityMinutes.values.sum()
            
            // Calculate frequency (how often this activity appears in this slot across 7 days)
            val daysWithActivity = dayOccurrences[dominantActivity]?.size ?: 0
            val frequency = daysWithActivity / 7f
            
            // Calculate consistency (how consistently this activity dominates this slot)
            val dominantMinutes = dominantEntry.value
            val consistency = if (totalMinutes > 0) dominantMinutes.toFloat() / totalMinutes else 0f
            
            return TimeSlotData(
                slotIndex = slotIndex,
                dominantActivity = dominantActivity,
                frequency = frequency,
                consistency = consistency,
                totalMinutes = totalMinutes,
                activityBreakdown = activityMinutes.toMap()
            )
        }
    }
}