package com.chaidarun.chronofile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chaidarun.chronofile.databinding.ItemGoalCategoryBinding

class DefaultGoalCategoriesAdapter(
  private val onAddGoal: (DefaultGoals.DefaultGoalTemplate) -> Unit
) : RecyclerView.Adapter<DefaultGoalCategoriesAdapter.CategoryViewHolder>() {
  
  private val categories = DefaultGoals.getAllCategories()
  private val categoryNames = categories.keys.toList()
  
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
    val binding = ItemGoalCategoryBinding.inflate(
      LayoutInflater.from(parent.context),
      parent,
      false
    )
    return CategoryViewHolder(binding)
  }
  
  override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
    val categoryName = categoryNames[position]
    val goals = categories[categoryName] ?: emptyList()
    holder.bind(categoryName, goals)
  }
  
  override fun getItemCount(): Int = categoryNames.size
  
  inner class CategoryViewHolder(private val binding: ItemGoalCategoryBinding) : 
    RecyclerView.ViewHolder(binding.root) {
    
    fun bind(categoryName: String, goals: List<DefaultGoals.DefaultGoalTemplate>) {
      binding.categoryName.text = categoryName
      
      // Setup goals recycler view
      val goalsAdapter = DefaultGoalTemplatesAdapter(onAddGoal)
      binding.goalsRecyclerView.apply {
        layoutManager = LinearLayoutManager(binding.root.context)
        adapter = goalsAdapter
        isNestedScrollingEnabled = false
      }
      goalsAdapter.updateGoals(goals)
      
      // Setup add all button
      binding.addCategoryButton.setOnClickListener {
        goals.forEach { template ->
          onAddGoal(template)
        }
      }
    }
  }
}

class DefaultGoalTemplatesAdapter(
  private val onAddGoal: (DefaultGoals.DefaultGoalTemplate) -> Unit
) : RecyclerView.Adapter<DefaultGoalTemplatesAdapter.TemplateViewHolder>() {
  
  private var goals: List<DefaultGoals.DefaultGoalTemplate> = emptyList()
  
  fun updateGoals(newGoals: List<DefaultGoals.DefaultGoalTemplate>) {
    goals = newGoals
    notifyDataSetChanged()
  }
  
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TemplateViewHolder {
    val binding = com.chaidarun.chronofile.databinding.ItemGoalTemplateBinding.inflate(
      LayoutInflater.from(parent.context),
      parent,
      false
    )
    return TemplateViewHolder(binding)
  }
  
  override fun onBindViewHolder(holder: TemplateViewHolder, position: Int) {
    holder.bind(goals[position])
  }
  
  override fun getItemCount(): Int = goals.size
  
  inner class TemplateViewHolder(private val binding: com.chaidarun.chronofile.databinding.ItemGoalTemplateBinding) : 
    RecyclerView.ViewHolder(binding.root) {
    
    fun bind(template: DefaultGoals.DefaultGoalTemplate) {
      binding.goalName.text = template.activity
      binding.goalHours.text = "${template.targetHours}h"
      binding.goalDescription.text = template.description
      
      binding.addSingleGoalButton.setOnClickListener {
        onAddGoal(template)
      }
      
      binding.root.setOnClickListener {
        onAddGoal(template)
      }
    }
  }
}