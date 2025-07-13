package com.chaidarun.chronofile

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class ActivitySuggestionManager(
  private val context: Context,
  private val frequentActivitiesChipGroup: ChipGroup,
  private val activitySuggestionCard: MaterialCardView,
  private val suggestedActivityText: TextView,
  private val suggestionConfidenceText: TextView,
  private val acceptSuggestionButton: MaterialButton,
  private val dismissSuggestionButton: MaterialButton,
  private val onActivitySelected: (String) -> Unit
) {

  init {
    setupSuggestionCardListeners()
  }

  private fun setupSuggestionCardListeners() {
    acceptSuggestionButton.setOnClickListener {
      val suggestedActivity = suggestedActivityText.text.toString()
      if (suggestedActivity.isNotBlank()) {
        onActivitySelected(suggestedActivity)
        hideSuggestionCard()
      }
    }

    dismissSuggestionButton.setOnClickListener {
      hideSuggestionCard()
    }
  }

  fun updateFrequentActivities(history: History?) {
    frequentActivitiesChipGroup.removeAllViews()

    if (history == null) {
      return
    }

    val intelligentActivities = history.getIntelligentActivitySuggestions()
    val currentActivity = getCurrentActivity(history)

    // Populate chips with enhanced styling
    intelligentActivities.forEach { activity ->
      val chip = Chip(context).apply {
        val iconRes = getActivityIcon(activity)
        text = activity.replaceFirstChar { it.uppercase() }
        chipIcon = ContextCompat.getDrawable(context, iconRes)
        isClickable = true
        isFocusable = true
        isCheckable = false

        // Enhanced chip styling
        chipCornerRadius = 20f
        val categoryColor = getActivityCategoryColor(activity)
        chipBackgroundColor = ContextCompat.getColorStateList(context, categoryColor.background)
        setTextColor(ContextCompat.getColor(context, categoryColor.text))

        // Compact padding and medium weight text
        chipStartPadding = 8.dpToPx()
        chipEndPadding = 8.dpToPx()
        textStartPadding = 0f
        textEndPadding = 0f
        chipMinHeight = 36.dpToPx()
        textSize = 13f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.isFakeBoldText = true // Creates medium weight effect

        // Visual state indicator for current activity
        if (activity == currentActivity) {
          chipStrokeWidth = 3.dpToPx()
          chipStrokeColor = ContextCompat.getColorStateList(context, R.color.colorPrimary)
          alpha = 1.0f
          elevation = 4.dpToPx()
          // Add subtle animation effect
          scaleX = 1.05f
          scaleY = 1.05f
        } else {
          chipStrokeWidth = 0f
          alpha = 0.85f
          elevation = 0f
          scaleX = 1.0f
          scaleY = 1.0f
        }

        // Add click listener to quickly add this activity
        setOnClickListener {
          onActivitySelected(activity)
        }
      }
      frequentActivitiesChipGroup.addView(chip)
    }
  }

  fun updateActivitySuggestion(history: History?) {
    if (history == null) {
      hideSuggestionCard()
      return
    }

    val suggestedActivity = history.predictActivityForCurrentTime()
    if (suggestedActivity != null) {
      val confidence = history.getPredictionConfidence(suggestedActivity)
      showSuggestionCard(suggestedActivity, confidence)
    } else {
      hideSuggestionCard()
    }
  }

  private fun showSuggestionCard(activity: String, confidence: Double) {
    suggestedActivityText.text = activity.replaceFirstChar { it.uppercase() }
    suggestionConfidenceText.text = "${(confidence * 100).toInt()}% confidence"
    activitySuggestionCard.visibility = View.VISIBLE
  }

  private fun hideSuggestionCard() {
    activitySuggestionCard.visibility = View.GONE
  }

  private fun getCurrentActivity(history: History): String? {
    // Get the most recent activity if it's still active
    val entries = history.entries
    if (entries.isEmpty()) return null

    val lastEntry = entries.last()
    val now = epochSeconds()
    val timeSinceLastEntry = now - history.currentActivityStartTime

    // Consider activity current if it was started less than 8 hours ago
    return if (timeSinceLastEntry < 8 * 3600) {
      lastEntry.activity
    } else {
      null
    }
  }

  private data class ActivityCategoryColor(val background: Int, val text: Int)

  private fun getActivityCategoryColor(activity: String): ActivityCategoryColor {
    return when (activity.lowercase()) {
      "work", "meeting", "email", "coding", "programming" -> 
        ActivityCategoryColor(R.color.work_category_bg, R.color.work_category_text)
      "break", "coffee", "lunch", "dinner", "eat", "food", "meal" -> 
        ActivityCategoryColor(R.color.food_category_bg, R.color.food_category_text)
      "exercise", "gym", "sport", "workout", "walk", "walking" -> 
        ActivityCategoryColor(R.color.fitness_category_bg, R.color.fitness_category_text)
      "sleep", "relax", "rest", "home" -> 
        ActivityCategoryColor(R.color.personal_category_bg, R.color.personal_category_text)
      "commute", "travel", "drive", "shopping" -> 
        ActivityCategoryColor(R.color.transport_category_bg, R.color.transport_category_text)
      "study", "learn", "reading" -> 
        ActivityCategoryColor(R.color.learning_category_bg, R.color.learning_category_text)
      else -> 
        ActivityCategoryColor(R.color.other_category_bg, R.color.other_category_text)
    }
  }

  private fun getActivityIcon(activity: String): Int {
    return IconDatabase.findByKeyword(activity)
  }

  private fun Int.dpToPx(): Float {
    return (this * context.resources.displayMetrics.density)
  }
}
