package com.chaidarun.chronofile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.chaidarun.chronofile.databinding.ItemWeeklyGoalBinding

class WeeklyGoalsAdapter(
  private val showProgress: Boolean,
  private val onEditClick: (WeeklyGoal) -> Unit,
  private val onDeleteClick: (WeeklyGoal) -> Unit
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
      binding.goalTarget.text = "${goal.targetHours}h"
      
      binding.editGoalButton.setOnClickListener {
        onEditClick(goal)
      }
      
      binding.deleteGoalButton.setOnClickListener {
        onDeleteClick(goal)
      }
      
      if (showProgress && goal.isCurrentWeek()) {
        binding.progressSection.visibility = View.VISIBLE
        calculateAndShowProgress(goal)
      } else {
        binding.progressSection.visibility = View.GONE
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
      
      binding.progressText.text = "${String.format("%.1f", actualHours)}h / ${goal.targetHours}h"
      binding.progressPercentage.text = "$progressPercentage%"
      binding.progressBar.progress = progressPercentage
      
      // Color coding based on progress
      val colorRes = when {
        progressPercentage >= 100 -> android.R.color.holo_green_dark
        progressPercentage >= 75 -> android.R.color.holo_orange_dark
        else -> android.R.color.holo_red_dark
      }
      
      val color = binding.root.context.getColor(colorRes)
      binding.progressPercentage.setTextColor(color)
    }
  }
}