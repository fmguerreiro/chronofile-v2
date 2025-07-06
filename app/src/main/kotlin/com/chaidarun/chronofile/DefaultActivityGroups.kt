package com.chaidarun.chronofile

object DefaultActivityGroups {
  
  fun getDefaultGroups(): Map<String, List<String>> {
    return mapOf(
      "Exercise" to listOf(
        "Walk", "Run", "Jog", "Cycle", "Bike", "Gym", "Workout", "Fitness",
        "Yoga", "Pilates", "Swimming", "Dance", "Sports", "Tennis", "Basketball",
        "Soccer", "Football", "Golf", "Hiking", "Climbing", "Stretching"
      ),
      
      "Work" to listOf(
        "Work", "Office", "Meeting", "Email", "Calls", "Conference", "Project",
        "Coding", "Programming", "Development", "Design", "Research", "Writing",
        "Documentation", "Planning", "Review", "Admin", "Paperwork"
      ),
      
      "Health" to listOf(
        "Sleep", "Rest", "Nap", "Doctor", "Dentist", "Medical", "Therapy",
        "Meditation", "Mindfulness", "Relaxation", "Wellness", "Self-care"
      ),
      
      "Learning" to listOf(
        "Study", "Reading", "Books", "Course", "Class", "Training", "Tutorial",
        "Research", "Learning", "Education", "University", "School", "Practice"
      ),
      
      "Social" to listOf(
        "Friends", "Family", "Social", "Party", "Date", "Dinner", "Lunch",
        "Coffee", "Chat", "Visit", "Hangout", "Event", "Celebration"
      ),
      
      "Entertainment" to listOf(
        "TV", "Movies", "Netflix", "YouTube", "Music", "Games", "Gaming",
        "Concert", "Theater", "Show", "Entertainment", "Fun", "Leisure"
      ),
      
      "Household" to listOf(
        "Cleaning", "Laundry", "Dishes", "Cooking", "Kitchen", "Shopping",
        "Groceries", "Errands", "Maintenance", "Organizing", "Tidying"
      ),
      
      "Travel" to listOf(
        "Commute", "Driving", "Bus", "Train", "Flight", "Travel", "Trip",
        "Vacation", "Journey", "Transport", "Walking"
      ),
      
      "Hobbies" to listOf(
        "Art", "Drawing", "Painting", "Photography", "Music", "Instrument",
        "Crafts", "Gardening", "DIY", "Building", "Making", "Creating"
      ),
      
      "Personal" to listOf(
        "Grooming", "Shower", "Hygiene", "Dressing", "Personal", "Phone",
        "Texting", "Internet", "Browsing", "Thinking", "Planning"
      )
    )
  }
  
  fun getEssentialGroups(): Map<String, List<String>> {
    return mapOf(
      "Work" to listOf("Work", "Meeting", "Email", "Coding", "Project"),
      "Exercise" to listOf("Walk", "Run", "Gym", "Workout", "Sports"),
      "Health" to listOf("Sleep", "Rest", "Medical", "Meditation"),
      "Social" to listOf("Friends", "Family", "Social", "Dinner"),
      "Learning" to listOf("Study", "Reading", "Course", "Research")
    )
  }
  
  fun getPersonalLifeGroups(): Map<String, List<String>> {
    return mapOf(
      "Exercise" to listOf("Walk", "Run", "Gym", "Yoga", "Sports"),
      "Health" to listOf("Sleep", "Rest", "Doctor", "Meditation"),
      "Social" to listOf("Friends", "Family", "Date", "Party"),
      "Entertainment" to listOf("TV", "Movies", "Music", "Games"),
      "Household" to listOf("Cleaning", "Cooking", "Shopping", "Errands"),
      "Hobbies" to listOf("Art", "Reading", "Photography", "Crafts")
    )
  }
  
  fun getStudentLifeGroups(): Map<String, List<String>> {
    return mapOf(
      "Study" to listOf("Study", "Class", "Reading", "Research", "Homework"),
      "Exercise" to listOf("Walk", "Run", "Gym", "Sports"),
      "Social" to listOf("Friends", "Party", "Social", "Date"),
      "Entertainment" to listOf("TV", "Movies", "Games", "Music"),
      "Personal" to listOf("Sleep", "Grooming", "Phone", "Internet")
    )
  }
  
  fun getAllPresets(): Map<String, Map<String, List<String>>> {
    return mapOf(
      "Complete Set" to getDefaultGroups(),
      "Essential Groups" to getEssentialGroups(),
      "Personal Life" to getPersonalLifeGroups(),
      "Student Life" to getStudentLifeGroups()
    )
  }
}