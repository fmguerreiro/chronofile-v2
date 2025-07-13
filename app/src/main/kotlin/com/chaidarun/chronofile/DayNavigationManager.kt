// © Art Chaidarun

package com.chaidarun.chronofile

import android.widget.TextView
import com.google.android.material.button.MaterialButton
import java.util.Calendar

class DayNavigationManager(
  private val currentDateText: TextView,
  private val previousDayButton: MaterialButton,
  private val nextDayButton: MaterialButton,
  private val onDateChanged: (Calendar) -> Unit
) {
  private var selectedDate: Calendar = Calendar.getInstance()

  init {
    setupClickListeners()
    updateDateDisplay()
  }
  
  // Call this after the manager is fully initialized
  fun triggerInitialDateChange() {
    onDateChanged(selectedDate)
  }

  private fun setupClickListeners() {
    previousDayButton.setOnClickListener {
      selectedDate.add(Calendar.DAY_OF_YEAR, -1)
      updateDateDisplay()
      onDateChanged(selectedDate)
    }

    nextDayButton.setOnClickListener {
      selectedDate.add(Calendar.DAY_OF_YEAR, 1)
      updateDateDisplay()
      onDateChanged(selectedDate)
    }
  }

  fun updateDateDisplay() {
    val today = Calendar.getInstance()
    val dateText = when {
      isSameDay(selectedDate, today) -> "Today"
      isSameDay(selectedDate, today.apply { add(Calendar.DAY_OF_YEAR, -1) }) -> "Yesterday"
      isSameDay(selectedDate, today.apply { add(Calendar.DAY_OF_YEAR, 2) }) -> "Tomorrow"
      else -> formatDateHeader(selectedDate.timeInMillis)
    }
    currentDateText.text = dateText

    // Disable next button if trying to go beyond today
    val isToday = isSameDay(selectedDate, Calendar.getInstance())
    nextDayButton.isEnabled = !isToday
    nextDayButton.alpha = if (isToday) 0.5f else 1.0f
  }

  fun getCurrentDate(): Calendar = selectedDate.clone() as Calendar

  private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
  }

  private fun formatDateHeader(timestamp: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val dayOfWeek = when (cal.get(Calendar.DAY_OF_WEEK)) {
      Calendar.MONDAY -> "Monday"
      Calendar.TUESDAY -> "Tuesday"
      Calendar.WEDNESDAY -> "Wednesday"
      Calendar.THURSDAY -> "Thursday"
      Calendar.FRIDAY -> "Friday"
      Calendar.SATURDAY -> "Saturday"
      Calendar.SUNDAY -> "Sunday"
      else -> ""
    }
    val month = when (cal.get(Calendar.MONTH)) {
      Calendar.JANUARY -> "Jan"
      Calendar.FEBRUARY -> "Feb"
      Calendar.MARCH -> "Mar"
      Calendar.APRIL -> "Apr"
      Calendar.MAY -> "May"
      Calendar.JUNE -> "Jun"
      Calendar.JULY -> "Jul"
      Calendar.AUGUST -> "Aug"
      Calendar.SEPTEMBER -> "Sep"
      Calendar.OCTOBER -> "Oct"
      Calendar.NOVEMBER -> "Nov"
      Calendar.DECEMBER -> "Dec"
      else -> ""
    }
    return "$dayOfWeek, $month ${cal.get(Calendar.DAY_OF_MONTH)}"
  }
}