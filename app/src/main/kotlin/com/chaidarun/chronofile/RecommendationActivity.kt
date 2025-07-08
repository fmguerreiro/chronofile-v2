package com.chaidarun.chronofile

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.chaidarun.chronofile.databinding.ActivityRecommendationBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.reactivex.disposables.CompositeDisposable

class RecommendationActivity : BaseActivity() {
    private val binding by viewBinding(ActivityRecommendationBinding::inflate)
    private lateinit var recommendationsAdapter: RecommendationsAdapter
    private lateinit var achievementsAdapter: AchievementsAdapter
    private var recommendationDisposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setupRecyclerViews()
        setupBottomNavigation()
        loadRecommendations()
    }
    
    private fun setupRecyclerViews() {
        // Recommendations RecyclerView
        recommendationsAdapter = RecommendationsAdapter { recommendation ->
            handleRecommendationAction(recommendation)
        }
        
        binding.recommendationsList.apply {
            layoutManager = LinearLayoutManager(this@RecommendationActivity)
            adapter = recommendationsAdapter
        }
        
        // Achievements RecyclerView
        achievementsAdapter = AchievementsAdapter()
        
        binding.celebrationsList.apply {
            layoutManager = LinearLayoutManager(this@RecommendationActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = achievementsAdapter
        }
    }
    
    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_timeline -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    finish()
                    true
                }
                R.id.nav_stats -> {
                    val intent = Intent(this, GraphActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    finish()
                    true
                }
                R.id.nav_goals -> {
                    val intent = Intent(this, WeeklyGoalsActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    finish()
                    true
                }
                R.id.nav_insights -> {
                    // Already on insights, do nothing
                    true
                }
                R.id.nav_settings -> {
                    val intent = Intent(this, EditorActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    finish()
                    true
                }
                else -> false
            }
        }
        
        // Set insights as selected in bottom navigation
        binding.bottomNavigation.selectedItemId = R.id.nav_insights
    }
    
    private fun loadRecommendations() {
        val history = Store.state.history
        val config = Store.state.config
        
        if (history == null || config == null) {
            showEmptyState()
            return
        }
        
        // Load life balance
        val lifeBalance = history.calculateLifeBalance(config)
        updateLifeBalanceCard(lifeBalance)
        
        // Load recommendations (max 3 for first-session value)
        val recommendations = history.getImmediateValueRecommendations(config, maxRecommendations = 3)
        updateRecommendationsCard(recommendations)
        
        // Load achievements/celebrations (both earned and locked)
        val achievements = history.getAllAchievements()
        updateCelebrationsCard(achievements)
        
        // Show privacy info
        binding.privacyInfoCard.visibility = View.VISIBLE
    }
    
    private fun updateLifeBalanceCard(lifeBalance: LifeBalanceMetrics) {
        if (lifeBalance.categories.isEmpty()) {
            binding.balanceCard.visibility = View.GONE
            return
        }
        
        binding.balanceCard.visibility = View.VISIBLE
        
        // Update balance score
        val balancePercentage = (lifeBalance.overallBalance * 100).toInt()
        binding.balanceScoreText.text = "${balancePercentage}% balanced"
        
        // Update categories summary
        val topCategories = lifeBalance.categories.entries
            .sortedByDescending { it.value.weeklyHours }
            .take(3)
            .map { "${it.key.displayName}: ${it.value.weeklyHours.toInt()}h" }
            .joinToString(" • ")
        
        binding.balanceSummaryText.text = topCategories
        
        // Show improvement areas
        if (lifeBalance.improvementAreas.isNotEmpty()) {
            binding.improvementAreasText.text = lifeBalance.improvementAreas.joinToString("\n• ", "• ")
            binding.improvementAreasText.visibility = View.VISIBLE
        } else {
            binding.improvementAreasText.visibility = View.GONE
        }
        
        // Show strengths
        if (lifeBalance.strengths.isNotEmpty()) {
            binding.strengthsText.text = lifeBalance.strengths.joinToString("\n• ", "• ")
            binding.strengthsText.visibility = View.VISIBLE
        } else {
            binding.strengthsText.visibility = View.GONE
        }
    }
    
    private fun updateRecommendationsCard(recommendations: List<SmartRecommendation>) {
        if (recommendations.isEmpty()) {
            binding.recommendationsCard.visibility = View.GONE
            return
        }
        
        binding.recommendationsCard.visibility = View.VISIBLE
        recommendationsAdapter.updateRecommendations(recommendations)
    }
    
    private fun updateCelebrationsCard(achievements: List<Achievement>) {
        if (achievements.isEmpty()) {
            binding.celebrationsCard.visibility = View.GONE
            return
        }
        
        binding.celebrationsCard.visibility = View.VISIBLE
        achievementsAdapter.updateAchievements(achievements)
    }
    
    private fun showEmptyState() {
        binding.emptyStateText.visibility = View.VISIBLE
        binding.emptyStateText.text = "Start tracking activities to see personalized insights!"
        
        // Hide other cards
        binding.balanceCard.visibility = View.GONE
        binding.recommendationsCard.visibility = View.GONE
        binding.celebrationsCard.visibility = View.GONE
    }
    
    private fun handleRecommendationAction(recommendation: SmartRecommendation) {
        when {
            recommendation.suggestedActivity != null -> {
                // Add the suggested activity
                History.addEntry(recommendation.suggestedActivity, null)
                App.toast("Started ${recommendation.suggestedActivity}!")
                
                // Refresh recommendations
                loadRecommendations()
            }
            recommendation.type == RecommendationType.PROGRESS_CELEBRATION -> {
                App.toast(recommendation.message)
            }
            else -> {
                App.toast("Great choice! Keep building positive habits.")
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // Set insights as selected in bottom navigation
        binding.bottomNavigation.selectedItemId = R.id.nav_insights
        
        // Refresh recommendations when returning to screen
        loadRecommendations()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        recommendationDisposables.dispose()
    }
}