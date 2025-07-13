package com.chaidarun.chronofile

object DefaultGoals {
  
  data class DefaultGoalTemplate(
    val activity: String,
    val targetHours: Double,
    val description: String,
    val frequency: GoalFrequency = GoalFrequency.WEEKLY
  )
  
  val healthAndWellness = listOf(
    DefaultGoalTemplate("Exercise", 3.0, "Physical fitness and health"),
    DefaultGoalTemplate("Sleep", 56.0, "7-8 hours per night for good health"),
    DefaultGoalTemplate("Meditation", 1.5, "Daily mindfulness practice", GoalFrequency.DAILY),
    DefaultGoalTemplate("Walking", 5.0, "Regular movement and outdoor time")
  )
  
  val workAndProductivity = listOf(
    DefaultGoalTemplate("Work", 40.0, "Standard full-time work week"),
    DefaultGoalTemplate("Deep Work", 20.0, "Focused, uninterrupted work time"),
    DefaultGoalTemplate("Learning", 5.0, "Skill development and education"),
    DefaultGoalTemplate("Reading", 3.0, "Books, articles, and knowledge building")
  )
  
  val socialAndFamily = listOf(
    DefaultGoalTemplate("Family Time", 10.0, "Quality time with family"),
    DefaultGoalTemplate("Friends", 4.0, "Social connections and relationships"),
    DefaultGoalTemplate("Hobbies", 6.0, "Personal interests and creative activities"),
    DefaultGoalTemplate("Cooking", 5.0, "Meal preparation and healthy eating")
  )
  
  val personalDevelopment = listOf(
    DefaultGoalTemplate("Writing", 3.0, "Journaling, blogging, or creative writing"),
    DefaultGoalTemplate("Music Practice", 4.0, "Playing instrument or music creation"),
    DefaultGoalTemplate("Language Learning", 3.5, "Learning new languages"),
    DefaultGoalTemplate("Planning", 2.0, "Goal setting and life organization")
  )
  
  val maintenance = listOf(
    DefaultGoalTemplate("Cleaning", 3.0, "Household maintenance and organization"),
    DefaultGoalTemplate("Commuting", 5.0, "Travel time to/from work"),
    DefaultGoalTemplate("Shopping", 2.0, "Grocery and essential shopping"),
    DefaultGoalTemplate("Admin", 2.0, "Bills, emails, and life administration")
  )
  
  fun getAllCategories(): Map<String, List<DefaultGoalTemplate>> {
    return mapOf(
      "Health & Wellness" to healthAndWellness,
      "Work & Productivity" to workAndProductivity,
      "Social & Family" to socialAndFamily,
      "Personal Development" to personalDevelopment,
      "Maintenance" to maintenance
    )
  }
  
  fun getBalancedLifeDefaults(): List<DefaultGoalTemplate> {
    return listOf(
      DefaultGoalTemplate("Sleep", 56.0, "7-8 hours per night"),
      DefaultGoalTemplate("Work", 40.0, "Standard work week"),
      DefaultGoalTemplate("Exercise", 3.0, "Stay physically active"),
      DefaultGoalTemplate("Family Time", 10.0, "Quality time with loved ones"),
      DefaultGoalTemplate("Learning", 5.0, "Continuous improvement"),
      DefaultGoalTemplate("Hobbies", 6.0, "Personal enjoyment"),
      DefaultGoalTemplate("Reading", 3.0, "Knowledge and entertainment")
    )
  }
  
  fun createWeeklyGoal(template: DefaultGoalTemplate): WeeklyGoal {
    val calendar = java.util.Calendar.getInstance()
    calendar.firstDayOfWeek = java.util.Calendar.MONDAY
    calendar.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
    calendar.set(java.util.Calendar.MINUTE, 0)
    calendar.set(java.util.Calendar.SECOND, 0)
    calendar.set(java.util.Calendar.MILLISECOND, 0)
    
    return WeeklyGoal(
      id = java.util.UUID.randomUUID().toString(),
      activity = template.activity,
      targetHours = template.targetHours,
      weekStartTimestamp = calendar.timeInMillis,
      frequency = template.frequency
    )
  }
}