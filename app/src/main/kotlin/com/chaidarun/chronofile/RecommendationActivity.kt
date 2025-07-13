package com.chaidarun.chronofile

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.chaidarun.chronofile.databinding.ActivityRecommendationBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.reactivex.disposables.CompositeDisposable
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import androidx.core.content.ContextCompat

class RecommendationActivity : BaseActivity() {
    private val binding by viewBinding(ActivityRecommendationBinding::inflate)
    private var recommendationDisposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setupBottomNavigation()
        setupSuggestedActivities()
        setupTrendCharts()
    }
    
    private fun setupSuggestedActivities() {
        // Setup click listeners for the suggested activity cards
        binding.tryFocusedWork.setOnClickListener {
            startActivity("Focused Work", 45 * 60) // 45 minutes
        }
        
        binding.tryMindfulBreak.setOnClickListener {
            startActivity("Mindful Break", 5 * 60) // 5 minutes
        }
        
        binding.tryLearnSkill.setOnClickListener {
            startActivity("Learning", 30 * 60) // 30 minutes
        }
    }
    
    private fun startActivity(activityName: String, durationSeconds: Int) {
        // Add the activity to the history
        Store.dispatch(Action.AddEntry(
            activity = activityName,
            note = "Started from Smart Recommendations",
            latLong = null
        ))
        App.toast("Started $activityName!")
        
        // Navigate back to timeline
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
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
                    val intent = Intent(this, GoalsActivity::class.java)
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
    
    private fun setupTrendCharts() {
        val history = Store.state.history ?: return
        val config = Store.state.config ?: return
        
        // Setup the three trend charts with different data visualizations
        setupTrendChart1(history, config)
        setupTrendChart2(history, config)
        setupTrendChart3(history, config)
    }
    
    private fun setupTrendChart1(history: History, config: Config) {
        val chart = binding.trendChart1
        
        // Get top 3 activities by time spent in last week
        val weekAgo = epochSeconds() - (7 * 24 * 60 * 60)
        val recentEntries = history.entries.filter { it.startTime >= weekAgo }
        
        val activityTimes = recentEntries.groupBy { it.activity }
            .mapValues { (_, entries) -> 
                entries.sumOf { entry ->
                    val nextEntry = recentEntries.find { it.startTime > entry.startTime }
                    val endTime = nextEntry?.startTime ?: history.currentActivityStartTime
                    endTime - entry.startTime
                }
            }
            .toList()
            .sortedByDescending { it.second }
            .take(3)
        
        if (activityTimes.isEmpty()) {
            chart.visibility = View.GONE
            return
        }
        
        chart.visibility = View.VISIBLE
        
        // Create pie chart entries
        val pieEntries = activityTimes.map { (activity, timeSpent) ->
            PieEntry(timeSpent.toFloat(), activity)
        }
        
        // Create dataset with gradient green colors
        val dataSet = PieDataSet(pieEntries, "Weekly Activity")
        dataSet.colors = listOf(
            ContextCompat.getColor(this, android.R.color.holo_green_light),
            ContextCompat.getColor(this, android.R.color.holo_green_dark),
            ContextCompat.getColor(this, R.color.colorPrimary)
        )
        
        val data = PieData(dataSet)
        data.setDrawValues(false)
        
        chart.apply {
            this.data = data
            description.isEnabled = false
            legend.isEnabled = false
            setUsePercentValues(true)
            setDrawHoleEnabled(true)
            setHoleRadius(50f)
            setTransparentCircleRadius(55f)
            animateY(1000)
            invalidate()
        }
    }
    
    private fun setupTrendChart2(history: History, config: Config) {
        val chart = binding.trendChart2
        
        // Get productivity vs leisure time balance
        val weekAgo = epochSeconds() - (7 * 24 * 60 * 60)
        val recentEntries = history.entries.filter { it.startTime >= weekAgo }
        
        val productivityTime = recentEntries.filter { 
            it.activity.lowercase().contains("work") || 
            it.activity.lowercase().contains("study") ||
            it.activity.lowercase().contains("focus")
        }.sumOf { entry ->
            val nextEntry = recentEntries.find { it.startTime > entry.startTime }
            val endTime = nextEntry?.startTime ?: history.currentActivityStartTime
            endTime - entry.startTime
        }
        
        val leisureTime = recentEntries.filter { 
            it.activity.lowercase().contains("relax") || 
            it.activity.lowercase().contains("entertainment") ||
            it.activity.lowercase().contains("break")
        }.sumOf { entry ->
            val nextEntry = recentEntries.find { it.startTime > entry.startTime }
            val endTime = nextEntry?.startTime ?: history.currentActivityStartTime
            endTime - entry.startTime
        }
        
        if (productivityTime == 0L && leisureTime == 0L) {
            chart.visibility = View.GONE
            return
        }
        
        chart.visibility = View.VISIBLE
        
        val pieEntries = mutableListOf<PieEntry>()
        if (productivityTime > 0) pieEntries.add(PieEntry(productivityTime.toFloat(), "Productivity"))
        if (leisureTime > 0) pieEntries.add(PieEntry(leisureTime.toFloat(), "Leisure"))
        
        val dataSet = PieDataSet(pieEntries, "Work-Life Balance")
        dataSet.colors = listOf(
            ContextCompat.getColor(this, android.R.color.holo_orange_light),
            ContextCompat.getColor(this, android.R.color.holo_orange_dark)
        )
        
        val data = PieData(dataSet)
        data.setDrawValues(false)
        
        chart.apply {
            this.data = data
            description.isEnabled = false
            legend.isEnabled = false
            setUsePercentValues(true)
            setDrawHoleEnabled(true)
            setHoleRadius(50f)
            setTransparentCircleRadius(55f)
            animateY(1000)
            invalidate()
        }
    }
    
    private fun setupTrendChart3(history: History, config: Config) {
        val chart = binding.trendChart3
        
        // Get daily activity distribution
        val today = epochSeconds()
        val todayStart = today - (today % (24 * 60 * 60))
        val todayEntries = history.entries.filter { it.startTime >= todayStart }
        
        val activityTimes = todayEntries.groupBy { it.activity }
            .mapValues { (_, entries) -> 
                entries.sumOf { entry ->
                    val nextEntry = todayEntries.find { it.startTime > entry.startTime }
                    val endTime = nextEntry?.startTime ?: history.currentActivityStartTime
                    endTime - entry.startTime
                }
            }
            .toList()
            .sortedByDescending { it.second }
            .take(4)
        
        if (activityTimes.isEmpty()) {
            chart.visibility = View.GONE
            return
        }
        
        chart.visibility = View.VISIBLE
        
        val pieEntries = activityTimes.map { (activity, timeSpent) ->
            PieEntry(timeSpent.toFloat(), activity)
        }
        
        val dataSet = PieDataSet(pieEntries, "Today's Activities")
        dataSet.colors = listOf(
            ContextCompat.getColor(this, android.R.color.holo_blue_light),
            ContextCompat.getColor(this, android.R.color.holo_blue_dark),
            ContextCompat.getColor(this, R.color.colorPrimary),
            ContextCompat.getColor(this, android.R.color.holo_purple)
        )
        
        val data = PieData(dataSet)
        data.setDrawValues(false)
        
        chart.apply {
            this.data = data
            description.isEnabled = false
            legend.isEnabled = false
            setUsePercentValues(true)
            setDrawHoleEnabled(true)
            setHoleRadius(50f)
            setTransparentCircleRadius(55f)
            animateY(1000)
            invalidate()
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // Set insights as selected in bottom navigation
        binding.bottomNavigation.selectedItemId = R.id.nav_insights
        
        // Refresh trend charts when returning to screen
        setupTrendCharts()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        recommendationDisposables.dispose()
    }
}