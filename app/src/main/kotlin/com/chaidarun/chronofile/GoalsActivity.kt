package com.chaidarun.chronofile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.chaidarun.chronofile.databinding.ActivityGoalsBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.reactivex.disposables.CompositeDisposable
import java.text.SimpleDateFormat
import java.util.*

class GoalsActivity : BaseActivity() {
  
  private val binding by viewBinding(ActivityGoalsBinding::inflate)
  private lateinit var currentGoalsAdapter: WeeklyGoalsAdapter
  private lateinit var allGoalsAdapter: WeeklyGoalsAdapter
  
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    setupRecyclerViews()
    setupFab()
    setupGoalsSettingsButton()
    setupBottomNavigation()
    
    disposables = CompositeDisposable()
    disposables?.add(
      Store.observable.subscribe { updateUI(it) }
    )
  }
  
  
  private fun setupRecyclerViews() {
    currentGoalsAdapter = WeeklyGoalsAdapter(
      showProgress = true,
      onEditClick = { goal -> editGoal(goal) },
      onDeleteClick = { goal -> deleteGoal(goal) },
      onFrequencyClick = { goal -> showFrequencySelector(goal) }
    )
    
    allGoalsAdapter = WeeklyGoalsAdapter(
      showProgress = false,
      onEditClick = { goal -> editGoal(goal) },
      onDeleteClick = { goal -> deleteGoal(goal) },
      onFrequencyClick = { goal -> showFrequencySelector(goal) }
    )
    
    binding.currentGoalsList.apply {
      layoutManager = LinearLayoutManager(this@GoalsActivity)
      adapter = currentGoalsAdapter
    }
    
    binding.allGoalsList.apply {
      layoutManager = LinearLayoutManager(this@GoalsActivity)
      adapter = allGoalsAdapter
    }
  }
  
  private fun setupFab() {
    binding.addGoalFab.setOnClickListener {
      showAddGoalDialog()
    }
  }
  
  
  
  private fun updateUI(state: State) {
    val goals = state.config?.weeklyGoals ?: emptyList()
    val activeGoals = goals.filter { it.isActive }
    val currentWeekGoals = activeGoals.filter { it.isCurrentWeek() }
    
    // Update current week goals
    currentGoalsAdapter.updateGoals(currentWeekGoals, state.history)
    binding.emptyCurrentGoalsText.visibility = if (currentWeekGoals.isEmpty()) View.VISIBLE else View.GONE
    binding.currentGoalsList.visibility = if (currentWeekGoals.isEmpty()) View.GONE else View.VISIBLE
    
    // Update all goals
    allGoalsAdapter.updateGoals(activeGoals, state.history)
    binding.allGoalsList.visibility = View.GONE
    
    // Update progress trends section
    updateProgressTrendsSection(activeGoals, state.history)
  }
  
  private fun updateProgressTrendsSection(activeGoals: List<WeeklyGoal>, history: History?) {
    if (activeGoals.isEmpty() || history == null) {
      // Hide progress trends section if no goals or history
      hideProgressTrendsSection()
      return
    }
    
    // Find the latest activity with a goal that has recent time entries
    val latestActivityWithGoal = findLatestTrackedActivityWithGoal(activeGoals, history)
    
    if (latestActivityWithGoal != null) {
      val (goal, latestEntryTime) = latestActivityWithGoal
      
      // Show progress trends section
      showProgressTrendsSection()
      
      // Update progress title
      binding.progressActivityTitle.text = "${goal.activity} Progress"
      
      // Calculate current progress
      val currentProgress = calculateCurrentProgress(goal, history)
      binding.progressPercentageMain.text = "${currentProgress}%"
      
      // Calculate trend
      val trend = calculateProgressTrend(goal, history)
      binding.progressTrendIndicator.text = formatTrendText(trend)
      
      // Update trend indicator icon based on trend direction
      val trendIcon = if (trend >= 0) R.drawable.ic_trending_up else R.drawable.ic_trending_up // TODO: Add ic_trending_down
      binding.progressTrendIndicator.setCompoundDrawablesWithIntrinsicBounds(trendIcon, 0, 0, 0)
    } else {
      // No tracked activities with goals, hide the section
      hideProgressTrendsSection()
    }
  }
  
  private fun findLatestTrackedActivityWithGoal(goals: List<WeeklyGoal>, history: History): Pair<WeeklyGoal, Long>? {
    val currentTime = System.currentTimeMillis() / 1000
    val oneDayAgo = currentTime - (24 * 60 * 60) // 24 hours ago
    
    // Find recent entries (last 24 hours) for activities that have goals
    val recentEntriesWithGoals = history.entries
      .filter { entry ->
        entry.startTime >= oneDayAgo &&
        goals.any { goal -> goal.activity == entry.activity }
      }
      .sortedByDescending { it.startTime }
    
    if (recentEntriesWithGoals.isEmpty()) {
      // If no recent entries, find the most recent goal activity from all time
      val allEntriesWithGoals = history.entries
        .filter { entry -> goals.any { goal -> goal.activity == entry.activity } }
        .sortedByDescending { it.startTime }
      
      if (allEntriesWithGoals.isNotEmpty()) {
        val latestEntry = allEntriesWithGoals.first()
        val goal = goals.find { it.activity == latestEntry.activity }
        return goal?.let { Pair(it, latestEntry.startTime) }
      }
    } else {
      // Return the most recent entry's activity and its goal
      val latestEntry = recentEntriesWithGoals.first()
      val goal = goals.find { it.activity == latestEntry.activity }
      return goal?.let { Pair(it, latestEntry.startTime) }
    }
    
    return null
  }
  
  private fun calculateCurrentProgress(goal: WeeklyGoal, history: History): Int {
    val weekStartSeconds = goal.weekStartTimestamp / 1000
    val weekEndSeconds = goal.getWeekEndTimestamp() / 1000
    
    // Calculate total hours for this activity in the current week
    val actualHours = history.entries
      .filter { entry ->
        entry.startTime >= weekStartSeconds && 
        entry.startTime < weekEndSeconds &&
        entry.activity == goal.activity
      }
      .sumOf { entry ->
        val nextEntry = history.entries.find { it.startTime > entry.startTime }
        val endTime = nextEntry?.startTime ?: (System.currentTimeMillis() / 1000)
        val durationSeconds = endTime - entry.startTime
        durationSeconds / 3600.0 // Convert to hours
      }
    
    return ((actualHours / goal.targetHours) * 100).toInt().coerceAtMost(100)
  }
  
  private fun calculateProgressTrend(goal: WeeklyGoal, history: History): Int {
    val currentWeekProgress = calculateCurrentProgress(goal, history)
    
    // Calculate previous week's progress
    val previousWeekStart = goal.weekStartTimestamp - (7 * 24 * 60 * 60 * 1000)
    val previousWeekEnd = goal.weekStartTimestamp
    
    val previousWeekHours = history.entries
      .filter { entry ->
        entry.startTime >= (previousWeekStart / 1000) && 
        entry.startTime < (previousWeekEnd / 1000) &&
        entry.activity == goal.activity
      }
      .sumOf { entry ->
        val nextEntry = history.entries.find { it.startTime > entry.startTime }
        val endTime = nextEntry?.startTime ?: (System.currentTimeMillis() / 1000)
        val durationSeconds = endTime - entry.startTime
        durationSeconds / 3600.0
      }
    
    val previousWeekProgress = ((previousWeekHours / goal.targetHours) * 100).toInt().coerceAtMost(100)
    
    return currentWeekProgress - previousWeekProgress
  }
  
  private fun formatTrendText(trend: Int): String {
    return when {
      trend > 0 -> "Last 30 Days +${trend}%"
      trend < 0 -> "Last 30 Days ${trend}%"
      else -> "Last 30 Days Same"
    }
  }
  
  private fun showProgressTrendsSection() {
    // Find the progress trends section in the layout and show it
    val progressTrendsSection = binding.root.findViewById<android.widget.LinearLayout>(R.id.progressTrendsSection)
    progressTrendsSection?.visibility = View.VISIBLE
  }
  
  private fun hideProgressTrendsSection() {
    // Find the progress trends section in the layout and hide it
    val progressTrendsSection = binding.root.findViewById<android.widget.LinearLayout>(R.id.progressTrendsSection)
    progressTrendsSection?.visibility = View.GONE
  }

  private fun showAddGoalDialog() {
    val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_goal, null)
    val activityInput = dialogView.findViewById<EditText>(R.id.editGoalActivity)
    val hoursInput = dialogView.findViewById<EditText>(R.id.editGoalHours)
    val frequencyInput = dialogView.findViewById<AutoCompleteTextView>(R.id.editGoalFrequency)
    
    // Update header for Add Goal and hide delete button
    val headerTitle = dialogView.findViewById<TextView>(R.id.headerTitle)
    val deleteButton = dialogView.findViewById<android.widget.ImageButton>(R.id.deleteGoalButton)
    headerTitle?.text = "Add Goal"
    deleteButton.visibility = android.view.View.GONE
    
    // Setup frequency dropdown
    val frequencies = GoalFrequency.values().map { it.displayName() }
    val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, frequencies)
    frequencyInput.setAdapter(adapter)
    frequencyInput.setText(GoalFrequency.WEEKLY.displayName(), false)
    
    // Update hint text and convert target hours based on frequency selection
    var currentFrequency = GoalFrequency.WEEKLY
    val updateHintTextAndConvertHours = { selectedFrequency: GoalFrequency ->
      val hintText = when (selectedFrequency) {
        GoalFrequency.DAILY -> "Target hours per day (e.g. 1.5)"
        GoalFrequency.WEEKLY -> "Target hours per week (e.g. 5.0)"
        GoalFrequency.MONTHLY -> "Target hours per month (e.g. 20.0)"
      }
      (hoursInput.parent.parent as com.google.android.material.textfield.TextInputLayout).hint = hintText
      
      // Convert target hours if there's a value and frequency changed
      val currentHoursText = hoursInput.text.toString().trim()
      if (currentHoursText.isNotEmpty() && selectedFrequency != currentFrequency) {
        try {
          val currentHours = currentHoursText.toDouble()
          val convertedHours = convertHoursBetweenFrequencies(currentHours, currentFrequency, selectedFrequency)
          hoursInput.setText(String.format("%.1f", convertedHours))
        } catch (e: NumberFormatException) {
          // Ignore if not a valid number
        }
      }
      
      currentFrequency = selectedFrequency
    }
    
    // Set initial hint
    updateHintTextAndConvertHours(GoalFrequency.WEEKLY)
    
    // Listen for frequency changes
    frequencyInput.setOnItemClickListener { _, _, position, _ ->
      val selectedFrequency = GoalFrequency.values()[position]
      updateHintTextAndConvertHours(selectedFrequency)
    }
    
    val dialog = AlertDialog.Builder(this, R.style.MyAlertDialogTheme)
      .setView(dialogView)
      .setPositiveButton("Add") { _, _ ->
        val activity = activityInput.text.toString().trim()
        val targetHoursText = hoursInput.text.toString().trim()
        val frequencyText = frequencyInput.text.toString().trim()
        
        if (activity.isNotBlank() && targetHoursText.isNotBlank() && frequencyText.isNotBlank()) {
          try {
            val targetHours = targetHoursText.toDouble()
            val frequency = GoalFrequency.values().find { it.displayName() == frequencyText } ?: GoalFrequency.WEEKLY
            
            if (targetHours > 0) {
              val calendar = Calendar.getInstance()
              calendar.firstDayOfWeek = Calendar.MONDAY
              calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
              calendar.set(Calendar.HOUR_OF_DAY, 0)
              calendar.set(Calendar.MINUTE, 0)
              calendar.set(Calendar.SECOND, 0)
              calendar.set(Calendar.MILLISECOND, 0)
              
              val goal = WeeklyGoal(
                id = UUID.randomUUID().toString(),
                activity = activity,
                targetHours = targetHours,
                weekStartTimestamp = calendar.timeInMillis,
                frequency = frequency
              )
              
              Store.dispatch(Action.AddWeeklyGoal(goal))
              App.toast("Goal added successfully")
            } else {
              App.toast("Target hours must be greater than 0")
            }
          } catch (e: NumberFormatException) {
            App.toast("Invalid number format for target hours")
          }
        } else {
          App.toast("Please fill in all fields")
        }
      }
      .setNegativeButton("Cancel", null)
      .create()
      
    dialog.setOnShowListener {
      val addButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
      addButton.isEnabled = false
      
      val updateButtonState = {
        val hasActivity = activityInput.text.toString().trim().isNotBlank()
        val hasTargetHours = hoursInput.text.toString().trim().isNotBlank()
        val hasFrequency = frequencyInput.text.toString().trim().isNotBlank()
        addButton.isEnabled = hasActivity && hasTargetHours && hasFrequency
      }
      
      activityInput.addTextChangedListener(
        afterTextChanged = { updateButtonState() }
      )
      hoursInput.addTextChangedListener(
        afterTextChanged = { updateButtonState() }
      )
      frequencyInput.addTextChangedListener(
        afterTextChanged = { updateButtonState() }
      )
    }
    
    dialog.show()
  }
  
  private fun editGoal(goal: WeeklyGoal) {
    val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_goal, null)
    val activityInput = dialogView.findViewById<EditText>(R.id.editGoalActivity)
    val hoursInput = dialogView.findViewById<EditText>(R.id.editGoalHours)
    val frequencyInput = dialogView.findViewById<AutoCompleteTextView>(R.id.editGoalFrequency)
    
    // Setup frequency dropdown
    val frequencies = GoalFrequency.values().map { it.displayName() }
    val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, frequencies)
    frequencyInput.setAdapter(adapter)
    
    // Update hint text and convert target hours based on frequency selection
    var currentFrequency = goal.frequency ?: GoalFrequency.WEEKLY
    val updateHintTextAndConvertHours = { selectedFrequency: GoalFrequency ->
      val hintText = when (selectedFrequency) {
        GoalFrequency.DAILY -> "Target hours per day (e.g. 1.5)"
        GoalFrequency.WEEKLY -> "Target hours per week (e.g. 5.0)"
        GoalFrequency.MONTHLY -> "Target hours per month (e.g. 20.0)"
      }
      (hoursInput.parent.parent as com.google.android.material.textfield.TextInputLayout).hint = hintText
      
      // Convert target hours if there's a value and frequency changed
      val currentHoursText = hoursInput.text.toString().trim()
      if (currentHoursText.isNotEmpty() && selectedFrequency != currentFrequency) {
        try {
          val currentHours = currentHoursText.toDouble()
          val convertedHours = convertHoursBetweenFrequencies(currentHours, currentFrequency, selectedFrequency)
          hoursInput.setText(String.format("%.1f", convertedHours))
        } catch (e: NumberFormatException) {
          // Ignore if not a valid number
        }
      }
      
      currentFrequency = selectedFrequency
    }
    
    // Pre-fill with current values
    activityInput.setText(goal.activity)
    hoursInput.setText(goal.targetHours.toString())
    frequencyInput.setText(currentFrequency.displayName(), false)
    updateHintTextAndConvertHours(currentFrequency)
    
    // Setup delete button (will be set after dialog creation)
    val deleteButton = dialogView.findViewById<android.widget.ImageButton>(R.id.deleteGoalButton)
    
    // Listen for frequency changes
    frequencyInput.setOnItemClickListener { _, _, position, _ ->
      val selectedFrequency = GoalFrequency.values()[position]
      updateHintTextAndConvertHours(selectedFrequency)
    }
    
    val dialog = AlertDialog.Builder(this, R.style.MyAlertDialogTheme)
      .setView(dialogView)
      .setPositiveButton("Save") { _, _ ->
        val newActivity = activityInput.text.toString().trim()
        val newHoursText = hoursInput.text.toString().trim()
        val newFrequencyText = frequencyInput.text.toString().trim()
        
        if (newActivity.isNotBlank() && newHoursText.isNotBlank() && newFrequencyText.isNotBlank()) {
          try {
            val newHours = newHoursText.toDouble()
            val newFrequency = GoalFrequency.values().find { it.displayName() == newFrequencyText } ?: GoalFrequency.WEEKLY
            
            if (newHours > 0) {
              val updatedGoal = goal.copy(
                activity = newActivity,
                targetHours = newHours,
                frequency = newFrequency
              )
              Store.dispatch(Action.UpdateWeeklyGoal(updatedGoal))
              App.toast("Goal updated successfully")
            } else {
              App.toast("Target hours must be greater than 0")
            }
          } catch (e: NumberFormatException) {
            App.toast("Invalid number format for target hours")
          }
        } else {
          App.toast("Please fill in all fields")
        }
      }
      .setNegativeButton("Cancel", null)
      .create()
      
    // Setup delete button click listener now that dialog is created
    deleteButton.setOnClickListener {
      dialog.dismiss()
      deleteGoal(goal)
    }
      
    dialog.setOnShowListener {
      val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
      
      val updateButtonState = {
        val hasActivity = activityInput.text.toString().trim().isNotBlank()
        val hasTargetHours = hoursInput.text.toString().trim().isNotBlank()
        val hasFrequency = frequencyInput.text.toString().trim().isNotBlank()
        saveButton.isEnabled = hasActivity && hasTargetHours && hasFrequency
      }
      
      activityInput.addTextChangedListener(
        afterTextChanged = { updateButtonState() }
      )
      hoursInput.addTextChangedListener(
        afterTextChanged = { updateButtonState() }
      )
      frequencyInput.addTextChangedListener(
        afterTextChanged = { updateButtonState() }
      )
      
      // Select all text for easy editing
      activityInput.selectAll()
      activityInput.requestFocus()
    }
    
    dialog.show()
  }
  
  private fun deleteGoal(goal: WeeklyGoal) {
    AlertDialog.Builder(this, R.style.MyAlertDialogTheme)
      .setTitle("Delete Goal")
      .setMessage("Are you sure you want to delete the goal for \"${goal.activity}\"?")
      .setPositiveButton("Delete") { _, _ ->
        Store.dispatch(Action.RemoveWeeklyGoal(goal.id))
        App.toast("Goal deleted")
      }
      .setNegativeButton("Cancel", null)
      .show()
  }
  
  private fun showFrequencySelector(goal: WeeklyGoal) {
    val frequencies = GoalFrequency.values()
    val frequencyNames = frequencies.map { it.displayName() }.toTypedArray()
    val currentFrequency = goal.frequency ?: GoalFrequency.WEEKLY
    val selectedIndex = frequencies.indexOf(currentFrequency)
    
    AlertDialog.Builder(this, R.style.MyAlertDialogTheme)
      .setTitle("Change Frequency")
      .setSingleChoiceItems(frequencyNames, selectedIndex) { dialog, which ->
        val newFrequency = frequencies[which]
        if (newFrequency != currentFrequency) {
          // Convert target hours to new frequency
          val convertedHours = convertHoursBetweenFrequencies(goal.targetHours, currentFrequency, newFrequency)
          
          val updatedGoal = goal.copy(
            frequency = newFrequency,
            targetHours = convertedHours
          )
          Store.dispatch(Action.UpdateWeeklyGoal(updatedGoal))
          App.toast("Frequency updated to ${newFrequency.displayName()}")
        }
        dialog.dismiss()
      }
      .setNegativeButton("Cancel", null)
      .show()
  }
  
  
  private fun setupGoalsSettingsButton() {
    binding.goalsSettingsButton.setOnClickListener {
      showGoalsSettingsDialog()
    }
  }
  
  private fun setupBottomNavigation() {
    binding.bottomNavigation.setOnItemSelectedListener { item ->
      when (item.itemId) {
        R.id.nav_timeline -> {
          val intent = Intent(this, MainActivity::class.java)
          intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
          startActivity(intent)
          if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.fade_in, R.anim.fade_out)
          } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
          }
          finish()
          true
        }
        R.id.nav_stats -> {
          val intent = Intent(this, GraphActivity::class.java)
          startActivity(intent)
          if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.fade_in, R.anim.fade_out)
          } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
          }
          finish()
          true
        }
        R.id.nav_goals -> {
          // Already on goals, do nothing
          true
        }
        R.id.nav_insights -> {
          val intent = Intent(this, RecommendationActivity::class.java)
          startActivity(intent)
          if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.fade_in, R.anim.fade_out)
          } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
          }
          finish()
          true
        }
        else -> false
      }
    }
    
    // Set goals as selected in bottom navigation
    binding.bottomNavigation.selectedItemId = R.id.nav_goals
  }
  
  private fun showDefaultGoalsDialog() {
    val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_default_goals, null)
    
    val dialog = AlertDialog.Builder(this, R.style.MyAlertDialogTheme)
      .setView(dialogView)
      .setNegativeButton("Cancel", null)
      .create()
    
    // Setup balanced life button
    val balancedLifeButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.balancedLifeButton)
    balancedLifeButton.setOnClickListener {
      addBalancedLifeDefaults()
      dialog.dismiss()
    }
    
    // Setup categories recycler view
    val categoriesRecyclerView = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.categoriesRecyclerView)
    categoriesRecyclerView.layoutManager = LinearLayoutManager(this)
    categoriesRecyclerView.adapter = DefaultGoalCategoriesAdapter { template ->
      val goal = DefaultGoals.createWeeklyGoal(template)
      Store.dispatch(Action.AddWeeklyGoal(goal))
      App.toast("Added goal: ${template.activity}")
    }
    
    dialog.show()
  }
  
  private fun addBalancedLifeDefaults() {
    val existingActivities = Store.state.config?.weeklyGoals?.map { it.activity }?.toSet() ?: emptySet()
    val newGoals = DefaultGoals.getBalancedLifeDefaults()
      .filter { it.activity !in existingActivities }
      .map { DefaultGoals.createWeeklyGoal(it) }
    
    newGoals.forEach { goal ->
      Store.dispatch(Action.AddWeeklyGoal(goal))
    }
    
    if (newGoals.isNotEmpty()) {
      App.toast("Added ${newGoals.size} default goals!")
    } else {
      App.toast("Most default goals already exist")
    }
  }
  
  private fun confirmClearAllGoals() {
    val goals = Store.state.config?.weeklyGoals ?: emptyList()
    if (goals.isEmpty()) {
      App.toast("No goals to clear")
      return
    }
    
    AlertDialog.Builder(this, R.style.MyAlertDialogTheme)
      .setTitle("Clear All Goals")
      .setMessage("Are you sure you want to delete all ${goals.size} weekly goals? This cannot be undone.")
      .setPositiveButton("Clear All") { _, _ ->
        goals.forEach { goal ->
          Store.dispatch(Action.RemoveWeeklyGoal(goal.id))
        }
        App.toast("All goals cleared")
      }
      .setNegativeButton("Cancel", null)
      .show()
  }
  
  private fun toggleNotifications(menuItem: MenuItem?) {
    val currentlyEnabled = Store.state.config?.weeklyNotificationsEnabled ?: true
    val newState = !currentlyEnabled
    
    if (newState) {
      // Check notification permission for Android 13+
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
            != PackageManager.PERMISSION_GRANTED) {
          
          ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            NOTIFICATION_PERMISSION_REQUEST_CODE
          )
          return
        }
      }
      
      GoalNotificationManager.scheduleDailyGoalCheck(this)
      App.toast("Goal notifications enabled")
    } else {
      GoalNotificationManager.cancelDailyGoalCheck(this)
      App.toast("Goal notifications disabled")
    }
    
    Store.dispatch(Action.SetWeeklyNotificationsEnabled(newState))
    menuItem?.isChecked = newState
    invalidateOptionsMenu() // Refresh menu to update checkbox state
  }
  
  private fun testNotification() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
          != PackageManager.PERMISSION_GRANTED) {
        
        App.toast("Notification permission required")
        ActivityCompat.requestPermissions(
          this,
          arrayOf(Manifest.permission.POST_NOTIFICATIONS),
          NOTIFICATION_PERMISSION_REQUEST_CODE
        )
        return
      }
    }
    
    GoalNotificationManager.checkAndShowGoalNotifications(this)
    App.toast("Test notification sent!")
  }
  
  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    
    when (requestCode) {
      NOTIFICATION_PERMISSION_REQUEST_CODE -> {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          GoalNotificationManager.scheduleDailyGoalCheck(this)
          Store.dispatch(Action.SetWeeklyNotificationsEnabled(true))
          App.toast("Goal notifications enabled")
          invalidateOptionsMenu() // Refresh menu
        } else {
          App.toast("Notification permission denied")
        }
      }
    }
  }
  
  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.menu_goals, menu)
    
    // Update notification menu item state
    val notificationItem = menu?.findItem(R.id.action_notifications)
    val notificationsEnabled = Store.state.config?.weeklyNotificationsEnabled ?: true
    notificationItem?.isChecked = notificationsEnabled
    
    return true
  }
  
  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.action_goals_settings -> {
        // Show a settings dialog with goal management options
        showGoalsSettingsDialog()
        true
      }
      R.id.action_add_goal -> {
        showAddGoalDialog()
        true
      }
      R.id.action_add_defaults -> {
        showDefaultGoalsDialog()
        true
      }
      R.id.action_notifications -> {
        toggleNotifications(item)
        true
      }
      R.id.action_test_notification -> {
        testNotification()
        true
      }
      R.id.action_clear_all -> {
        confirmClearAllGoals()
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }
  
  private fun showGoalsSettingsDialog() {
    val options = arrayOf(
      "Add New Goal",
      "Add Default Goals",
      "Manage Notifications",
      "Clear All Goals"
    )
    
    AlertDialog.Builder(this, R.style.MyAlertDialogTheme)
      .setTitle("Goals Settings")
      .setItems(options) { _, which ->
        when (which) {
          0 -> showAddGoalDialog()
          1 -> showDefaultGoalsDialog()
          2 -> showNotificationSettings()
          3 -> confirmClearAllGoals()
        }
      }
      .setNegativeButton("Cancel", null)
      .show()
  }
  
  private fun showNotificationSettings() {
    val notificationsEnabled = Store.state.config?.weeklyNotificationsEnabled ?: true
    
    AlertDialog.Builder(this, R.style.MyAlertDialogTheme)
      .setTitle("Notification Settings")
      .setMessage("Weekly goal progress notifications are currently " + 
                  if (notificationsEnabled) "enabled" else "disabled")
      .setPositiveButton(if (notificationsEnabled) "Disable" else "Enable") { _, _ ->
        val menuItem = null // We'll handle this differently
        toggleNotifications(menuItem)
      }
      .setNeutralButton("Test Notification") { _, _ ->
        testNotification()
      }
      .setNegativeButton("Cancel", null)
      .show()
  }
  
  private fun convertHoursBetweenFrequencies(hours: Double, fromFrequency: GoalFrequency, toFrequency: GoalFrequency): Double {
    // Convert to daily hours first
    val dailyHours = when (fromFrequency) {
      GoalFrequency.DAILY -> hours
      GoalFrequency.WEEKLY -> hours / 7.0
      GoalFrequency.MONTHLY -> hours / 30.0 // Use average of 30 days per month
    }
    
    // Convert from daily to target frequency
    return when (toFrequency) {
      GoalFrequency.DAILY -> dailyHours
      GoalFrequency.WEEKLY -> dailyHours * 7.0
      GoalFrequency.MONTHLY -> dailyHours * 30.0 // Use average of 30 days per month
    }
  }
  
  companion object {
    private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 3001
  }
}