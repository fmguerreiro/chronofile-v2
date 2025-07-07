package com.chaidarun.chronofile

object DefaultActivityGroups {
  
  fun getDefaultGroups(): Map<String, List<String>> {
    return mapOf(
      "Exercise" to listOf(
        "Walk", "Run", "Jog", "Cycle", "Bike", "Gym", "Workout", "Fitness",
        "Yoga", "Pilates", "Swimming", "Dance", "Sports", "Tennis", "Basketball",
        "Soccer", "Football", "Golf", "Hiking", "Climbing", "Stretching"
      ).map { it.lemmatize() },
      
      "Work" to listOf(
        "Work", "Office", "Meeting", "Email", "Calls", "Conference", "Project",
        "Coding", "Programming", "Development", "Design", "Research", "Writing",
        "Documentation", "Planning", "Review", "Admin", "Paperwork"
      ).map { it.lemmatize() },
      
      "Health" to listOf(
        "Sleep", "Rest", "Nap", "Doctor", "Dentist", "Medical", "Therapy",
        "Meditation", "Mindfulness", "Relaxation", "Wellness", "Self-care"
      ).map { it.lemmatize() },
      
      "Learning" to listOf(
        "Study", "Reading", "Books", "Course", "Class", "Training", "Tutorial",
        "Research", "Learning", "Education", "University", "School", "Practice"
      ).map { it.lemmatize() },
      
      "Social" to listOf(
        "Friends", "Family", "Social", "Party", "Date", "Dinner", "Lunch",
        "Coffee", "Chat", "Visit", "Hangout", "Event", "Celebration"
      ).map { it.lemmatize() },
      
      "Entertainment" to listOf(
        "TV", "Movies", "Netflix", "YouTube", "Music", "Games", "Gaming",
        "Concert", "Theater", "Show", "Entertainment", "Fun", "Leisure"
      ).map { it.lemmatize() },
      
      "Household" to listOf(
        "Cleaning", "Laundry", "Dishes", "Cooking", "Kitchen", "Shopping",
        "Groceries", "Errands", "Maintenance", "Organizing", "Tidying"
      ).map { it.lemmatize() },
      
      "Travel" to listOf(
        "Commute", "Driving", "Bus", "Train", "Flight", "Travel", "Trip",
        "Vacation", "Journey", "Transport", "Walking"
      ).map { it.lemmatize() },
      
      "Hobbies" to listOf(
        "Art", "Drawing", "Painting", "Photography", "Music", "Instrument",
        "Crafts", "Gardening", "DIY", "Building", "Making", "Creating"
      ).map { it.lemmatize() },
      
      "Personal" to listOf(
        "Grooming", "Shower", "Hygiene", "Dressing", "Personal", "Phone",
        "Texting", "Internet", "Browsing", "Thinking", "Planning"
      ).map { it.lemmatize() }
    )
  }
  
  fun getEssentialGroups(): Map<String, List<String>> {
    return mapOf(
      "Work" to listOf("Work", "Meeting", "Email", "Coding", "Project").map { it.lemmatize() },
      "Exercise" to listOf("Walk", "Run", "Gym", "Workout", "Sports").map { it.lemmatize() },
      "Health" to listOf("Sleep", "Rest", "Medical", "Meditation").map { it.lemmatize() },
      "Social" to listOf("Friends", "Family", "Social", "Dinner").map { it.lemmatize() },
      "Learning" to listOf("Study", "Reading", "Course", "Research").map { it.lemmatize() }
    )
  }
  
  fun getPersonalLifeGroups(): Map<String, List<String>> {
    return mapOf(
      "Exercise" to listOf("Walk", "Run", "Gym", "Yoga", "Sports").map { it.lemmatize() },
      "Health" to listOf("Sleep", "Rest", "Doctor", "Meditation").map { it.lemmatize() },
      "Social" to listOf("Friends", "Family", "Date", "Party").map { it.lemmatize() },
      "Entertainment" to listOf("TV", "Movies", "Music", "Games").map { it.lemmatize() },
      "Household" to listOf("Cleaning", "Cooking", "Shopping", "Errands").map { it.lemmatize() },
      "Hobbies" to listOf("Art", "Reading", "Photography", "Crafts").map { it.lemmatize() }
    )
  }
  
  fun getStudentLifeGroups(): Map<String, List<String>> {
    return mapOf(
      "Study" to listOf("Study", "Class", "Reading", "Research", "Homework").map { it.lemmatize() },
      "Exercise" to listOf("Walk", "Run", "Gym", "Sports").map { it.lemmatize() },
      "Social" to listOf("Friends", "Party", "Social", "Date").map { it.lemmatize() },
      "Entertainment" to listOf("TV", "Movies", "Games", "Music").map { it.lemmatize() },
      "Personal" to listOf("Sleep", "Grooming", "Phone", "Internet").map { it.lemmatize() }
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