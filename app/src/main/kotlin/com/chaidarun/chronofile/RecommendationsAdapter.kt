package com.chaidarun.chronofile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.chaidarun.chronofile.databinding.ItemRecommendationBinding
import com.chaidarun.chronofile.databinding.ItemAchievementBinding

class RecommendationsAdapter(
    private val onRecommendationClick: (SmartRecommendation) -> Unit
) : RecyclerView.Adapter<RecommendationViewHolder>() {
    
    private var recommendations = listOf<SmartRecommendation>()
    
    fun updateRecommendations(newRecommendations: List<SmartRecommendation>) {
        recommendations = newRecommendations
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecommendationViewHolder {
        val binding = ItemRecommendationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RecommendationViewHolder(binding, onRecommendationClick)
    }
    
    override fun onBindViewHolder(holder: RecommendationViewHolder, position: Int) {
        holder.bind(recommendations[position])
    }
    
    override fun getItemCount(): Int = recommendations.size
}

class RecommendationViewHolder(
    private val binding: ItemRecommendationBinding,
    private val onRecommendationClick: (SmartRecommendation) -> Unit
) : RecyclerView.ViewHolder(binding.root) {
    
    fun bind(recommendation: SmartRecommendation) {
        binding.recommendationTitle.text = recommendation.title
        binding.recommendationMessage.text = recommendation.message
        binding.recommendationAction.text = recommendation.actionText
        
        // Show event-based cue if available
        if (recommendation.eventBasedCue != null) {
            binding.eventCueText.text = recommendation.eventBasedCue
            binding.eventCueText.visibility = android.view.View.VISIBLE
        } else {
            binding.eventCueText.visibility = android.view.View.GONE
        }
        
        // Show two-minute version if available
        if (recommendation.twoMinuteVersion != null) {
            binding.twoMinuteText.text = "Start small: ${recommendation.twoMinuteVersion}"
            binding.twoMinuteText.visibility = android.view.View.VISIBLE
        } else {
            binding.twoMinuteText.visibility = android.view.View.GONE
        }
        
        // Set up click listener
        binding.recommendationAction.setOnClickListener {
            onRecommendationClick(recommendation)
        }
        
        // Style based on recommendation type
        when (recommendation.type) {
            RecommendationType.PROGRESS_CELEBRATION -> {
                binding.recommendationCard.setCardBackgroundColor(
                    binding.root.context.getColor(R.color.celebration_background)
                )
            }
            RecommendationType.GENTLE_NUDGE -> {
                binding.recommendationCard.setCardBackgroundColor(
                    binding.root.context.getColor(R.color.nudge_background)
                )
            }
            else -> {
                binding.recommendationCard.setCardBackgroundColor(
                    binding.root.context.getColor(R.color.default_card_background)
                )
            }
        }
    }
}

class AchievementsAdapter : RecyclerView.Adapter<AchievementViewHolder>() {
    
    private var achievements = listOf<Achievement>()
    
    fun updateAchievements(newAchievements: List<Achievement>) {
        achievements = newAchievements
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val binding = ItemAchievementBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AchievementViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        holder.bind(achievements[position])
    }
    
    override fun getItemCount(): Int = achievements.size
}

class AchievementViewHolder(
    private val binding: ItemAchievementBinding
) : RecyclerView.ViewHolder(binding.root) {
    
    fun bind(achievement: Achievement) {
        binding.achievementIcon.setImageResource(achievement.iconRes)
        binding.achievementTitle.text = achievement.title
        binding.achievementDescription.text = achievement.description
        
        if (achievement.isLocked) {
            // Locked achievement styling
            binding.root.alpha = 0.5f
            binding.achievementCard.setCardBackgroundColor(
                binding.root.context.getColor(R.color.locked_achievement_background)
            )
            
            // Show unlock criteria instead of celebration message
            binding.celebrationMessage.text = achievement.unlockCriteria ?: "Locked"
            binding.celebrationMessage.textSize = 10f
            binding.celebrationMessage.setTextColor(
                binding.root.context.getColor(R.color.locked_achievement_text)
            )
            
            // Show progress if available
            if (achievement.progressText != null) {
                binding.progressText.text = achievement.progressText
                binding.progressText.visibility = android.view.View.VISIBLE
            } else {
                binding.progressText.visibility = android.view.View.GONE
            }
            
            // Scale down locked achievements
            binding.root.scaleX = 0.9f
            binding.root.scaleY = 0.9f
        } else {
            // Earned achievement styling
            binding.root.alpha = 1.0f
            binding.achievementCard.setCardBackgroundColor(
                binding.root.context.getColor(R.color.colorPrimaryContainer)
            )
            
            binding.celebrationMessage.text = achievement.celebrationMessage
            binding.celebrationMessage.textSize = 11f
            binding.celebrationMessage.setTextColor(
                binding.root.context.getColor(R.color.colorOnPrimaryContainer)
            )
            
            binding.progressText.visibility = android.view.View.GONE
            
            // Normal scale for earned achievements
            binding.root.scaleX = 1.0f
            binding.root.scaleY = 1.0f
        }
    }
}