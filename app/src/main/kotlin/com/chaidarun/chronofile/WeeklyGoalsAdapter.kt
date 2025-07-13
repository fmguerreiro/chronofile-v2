package com.chaidarun.chronofile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.chaidarun.chronofile.databinding.ItemWeeklyGoalBinding

class WeeklyGoalsAdapter(
  private val showProgress: Boolean,
  private val onEditClick: (WeeklyGoal) -> Unit,
  private val onDeleteClick: (WeeklyGoal) -> Unit,
  private val onFrequencyClick: (WeeklyGoal) -> Unit
) : RecyclerView.Adapter<WeeklyGoalsAdapter.GoalViewHolder>() {
  
  private var goals: List<WeeklyGoal> = emptyList()
  private var history: History? = null
  
  fun updateGoals(newGoals: List<WeeklyGoal>, newHistory: History?) {
    goals = newGoals
    history = newHistory
    notifyDataSetChanged()
  }
  
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
    val binding = ItemWeeklyGoalBinding.inflate(
      LayoutInflater.from(parent.context),
      parent,
      false
    )
    return GoalViewHolder(binding)
  }
  
  override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
    holder.bind(goals[position])
  }
  
  override fun getItemCount(): Int = goals.size
  
  inner class GoalViewHolder(private val binding: ItemWeeklyGoalBinding) : 
    RecyclerView.ViewHolder(binding.root) {
    
    fun bind(goal: WeeklyGoal) {
      binding.goalActivity.text = goal.activity
      binding.goalFrequency.text = (goal.frequency ?: GoalFrequency.WEEKLY).displayName()
      
      // Set goal icon based on activity type
      val iconRes = getIconForActivity(goal.activity)
      binding.goalIcon.setImageResource(iconRes)
      
      binding.editGoalButton.setOnClickListener {
        onEditClick(goal)
      }
      
      binding.deleteGoalButton.setOnClickListener {
        onDeleteClick(goal)
      }
      
      (binding.goalFrequency.parent as View).setOnClickListener {
        onFrequencyClick(goal)
      }
      
      // Click on the entire goal item to edit
      binding.root.setOnClickListener {
        onEditClick(goal)
      }
      
      if (showProgress && goal.isCurrentWeek()) {
        binding.progressBar.visibility = View.VISIBLE
        calculateAndShowProgress(goal)
      } else {
        binding.progressBar.visibility = View.GONE
        binding.progressPercentage.text = "${goal.targetHours.toInt()}"
      }
    }
    
    private fun calculateAndShowProgress(goal: WeeklyGoal) {
      val currentHistory = history ?: return
      
      val weekStartSeconds = goal.weekStartTimestamp / 1000
      val weekEndSeconds = goal.getWeekEndTimestamp() / 1000
      
      // Calculate total hours for this activity in the current week
      val actualHours = currentHistory.entries
        .filter { entry ->
          entry.startTime >= weekStartSeconds && 
          entry.startTime < weekEndSeconds &&
          entry.activity == goal.activity
        }
        .sumOf { entry ->
          val nextEntry = currentHistory.entries.find { it.startTime > entry.startTime }
          val endTime = nextEntry?.startTime ?: (System.currentTimeMillis() / 1000)
          val durationSeconds = endTime - entry.startTime
          durationSeconds / 3600.0 // Convert to hours
        }
      
      val progressPercentage = ((actualHours / goal.targetHours) * 100).toInt().coerceAtMost(100)
      
      binding.progressPercentage.text = "$progressPercentage"
      binding.progressBar.progress = progressPercentage
    }
    
    private fun getIconForActivity(activity: String): Int {
      return when (activity.lowercase()) {
        "meditate", "meditation", "mindfulness" -> R.drawable.ic_meditation
        "exercise", "workout", "gym", "fitness" -> R.drawable.ic_dumbbell
        "read", "reading", "book" -> R.drawable.ic_note
        "walk", "nature", "hiking" -> R.drawable.ic_sprout
        "work", "job", "office" -> R.drawable.ic_briefcase
        "learn", "study", "education" -> R.drawable.ic_school
        "sleep", "rest" -> R.drawable.ic_sleep
        "cook", "cooking", "meal" -> R.drawable.ic_food
        "music", "instrument" -> R.drawable.ic_music
        "clean", "cleaning" -> R.drawable.ic_cleaning
        else -> R.drawable.ic_target
      }
    }
  }
}