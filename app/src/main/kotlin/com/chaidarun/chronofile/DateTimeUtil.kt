// © Art Chaidarun

package com.chaidarun.chronofile

import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale

/** Gets current Unix timestamp in seconds */
fun epochSeconds() = System.currentTimeMillis() / 1000

fun formatDate(date: Date) = SimpleDateFormat("d MMM", Locale.US).format(date)

fun formatDate(seconds: Long) = formatDate(Date(seconds * 1000))

fun formatTime(date: Date) = SimpleDateFormat("H:mm", Locale.US).format(date)

fun formatForSearch(seconds: Long) =
  SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(seconds * 1000))

/** Pretty-prints time given in seconds, e.g. 86461 -> "1d 1m" */
fun formatDuration(seconds: Long, showDays: Boolean = false, showMinutes: Boolean = true): String {
  if (seconds < 30) return "0m"

  // Rounds to nearest minute
  val adjustedSeconds = if (seconds % 60 < 30) seconds else seconds + 60

  val pieces = mutableListOf<String>()
  val totalMinutes = adjustedSeconds / 60
  val minutes = totalMinutes % 60
  if (showMinutes && minutes != 0L) {
    pieces.add(0, "${minutes}m")
  }
  val totalHours = if (showMinutes) totalMinutes / 60 else Math.round(totalMinutes.toDouble() / 60)
  val hours = if (showDays) totalHours % 24 else totalHours
  if (hours != 0L) {
    pieces.add(0, "${hours}h")
  }
  if (showDays) {
    val days = totalHours / 24
    if (days != 0L) {
      pieces.add(0, "${days}d")
    }
  }
  return pieces.joinToString(" ")
}

private val LOCAL_TZ = ZoneId.systemDefault()

private fun getDate(timestamp: Long): LocalDate =
  LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), LOCAL_TZ).toLocalDate()

/** Gets the timestamp of the last midnight that occurred before the given timestamp */
fun getPreviousMidnight(timestamp: Long) = getDate(timestamp).atStartOfDay(LOCAL_TZ).toEpochSecond()

fun getDayOfWeek(timestamp: Long): DayOfWeek = getDate(timestamp).dayOfWeek
