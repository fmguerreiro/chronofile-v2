package com.chaidarun.chronofile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.chaidarun.chronofile.databinding.ActivityWeeklyGoalsBinding
import io.reactivex.disposables.CompositeDisposable
import java.text.SimpleDateFormat
import java.util.*

class WeeklyGoalsActivity : BaseActivity() {
  
  private val binding by viewBinding(ActivityWeeklyGoalsBinding::inflate)
  private lateinit var currentGoalsAdapter: WeeklyGoalsAdapter
  private lateinit var allGoalsAdapter: WeeklyGoalsAdapter
  
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setSupportActionBar(binding.toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    
    setupRecyclerViews()
    setupFab()
    setupWeekRangeText()
    setupDefaultGoalsButton()
    
    disposables = CompositeDisposable()
    disposables?.add(
      Store.observable.subscribe { updateUI(it) }
    )
  }
  
  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.menu_weekly_goals, menu)
    
    // Set notification toggle state
    val notificationsItem = menu.findItem(R.id.action_notifications)
    val isEnabled = Store.state.config?.weeklyNotificationsEnabled ?: true
    notificationsItem.isChecked = isEnabled
    
    return true
  }
  
  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.action_add_defaults -> {
        showDefaultGoalsDialog()
        return true
      }
      R.id.action_notifications -> {
        toggleNotifications(item)
        return true
      }
      R.id.action_test_notification -> {
        testNotification()
        return true
      }
      R.id.action_clear_all -> {
        confirmClearAllGoals()
        return true
      }
    }
    return super.onOptionsItemSelected(item)
  }
  
  override fun onSupportNavigateUp(): Boolean {
    onBackPressed()
    return true
  }
  
  private fun setupRecyclerViews() {
    currentGoalsAdapter = WeeklyGoalsAdapter(
      showProgress = true,
      onEditClick = { goal -> editGoal(goal) },
      onDeleteClick = { goal -> deleteGoal(goal) }
    )
    
    allGoalsAdapter = WeeklyGoalsAdapter(
      showProgress = false,
      onEditClick = { goal -> editGoal(goal) },
      onDeleteClick = { goal -> deleteGoal(goal) }
    )
    
    binding.currentGoalsList.apply {
      layoutManager = LinearLayoutManager(this@WeeklyGoalsActivity)
      adapter = currentGoalsAdapter
    }
    
    binding.allGoalsList.apply {
      layoutManager = LinearLayoutManager(this@WeeklyGoalsActivity)
      adapter = allGoalsAdapter
    }
  }
  
  private fun setupFab() {
    binding.addGoalFab.setOnClickListener {
      showAddGoalDialog()
    }
  }
  
  private fun setupWeekRangeText() {
    val calendar = Calendar.getInstance()
    calendar.firstDayOfWeek = Calendar.MONDAY
    calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    val weekStart = calendar.time
    
    calendar.add(Calendar.DAY_OF_WEEK, 6)
    val weekEnd = calendar.time
    
    val formatter = SimpleDateFormat("MMM d", Locale.getDefault())
    val yearFormatter = SimpleDateFormat("yyyy", Locale.getDefault())
    
    val weekRangeText = "${formatter.format(weekStart)} - ${formatter.format(weekEnd)}, ${yearFormatter.format(weekEnd)}"
    binding.weekRangeText.text = weekRangeText
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
    binding.emptyGoalsContainer.visibility = if (activeGoals.isEmpty()) View.VISIBLE else View.GONE
    binding.allGoalsList.visibility = if (activeGoals.isEmpty()) View.GONE else View.VISIBLE
  }
  
  private fun showAddGoalDialog() {
    val dialogView = LayoutInflater.from(this).inflate(R.layout.form_entry, null)
    val activityInput = dialogView.findViewById<EditText>(R.id.formEntryActivity)
    val noteInput = dialogView.findViewById<EditText>(R.id.formEntryNote)
    val startTimeInput = dialogView.findViewById<EditText>(R.id.formEntryStartTime)
    
    // Hide start time field and repurpose note field for target hours
    startTimeInput.visibility = View.GONE
    noteInput.hint = "Target hours per week (e.g. 5.0)"
    
    val dialog = AlertDialog.Builder(this, R.style.MyAlertDialogTheme)
      .setTitle("Add Weekly Goal")
      .setView(dialogView)
      .setPositiveButton("Add") { _, _ ->
        val activity = activityInput.text.toString().trim()
        val targetHoursText = noteInput.text.toString().trim()
        
        if (activity.isNotBlank() && targetHoursText.isNotBlank()) {
          try {
            val targetHours = targetHoursText.toDouble()
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
                weekStartTimestamp = calendar.timeInMillis
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
        val hasTargetHours = noteInput.text.toString().trim().isNotBlank()
        addButton.isEnabled = hasActivity && hasTargetHours
      }
      
      activityInput.addTextChangedListener(
        afterTextChanged = { updateButtonState() }
      )
      noteInput.addTextChangedListener(
        afterTextChanged = { updateButtonState() }
      )
    }
    
    dialog.show()
  }
  
  private fun editGoal(goal: WeeklyGoal) {
    val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_goal, null)
    val activityInput = dialogView.findViewById<EditText>(R.id.editGoalActivity)
    val hoursInput = dialogView.findViewById<EditText>(R.id.editGoalHours)
    
    // Pre-fill with current values
    activityInput.setText(goal.activity)
    hoursInput.setText(goal.targetHours.toString())
    
    val dialog = AlertDialog.Builder(this, R.style.MyAlertDialogTheme)
      .setView(dialogView)
      .setPositiveButton("Save") { _, _ ->
        val newActivity = activityInput.text.toString().trim()
        val newHoursText = hoursInput.text.toString().trim()
        
        if (newActivity.isNotBlank() && newHoursText.isNotBlank()) {
          try {
            val newHours = newHoursText.toDouble()
            if (newHours > 0) {
              val updatedGoal = goal.copy(
                activity = newActivity,
                targetHours = newHours
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
      
    dialog.setOnShowListener {
      val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
      
      val updateButtonState = {
        val hasActivity = activityInput.text.toString().trim().isNotBlank()
        val hasTargetHours = hoursInput.text.toString().trim().isNotBlank()
        saveButton.isEnabled = hasActivity && hasTargetHours
      }
      
      activityInput.addTextChangedListener(
        afterTextChanged = { updateButtonState() }
      )
      hoursInput.addTextChangedListener(
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
  
  private fun setupDefaultGoalsButton() {
    binding.addDefaultGoalsButton.setOnClickListener {
      showDefaultGoalsDialog()
    }
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
  
  private fun toggleNotifications(menuItem: MenuItem) {
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
      
      WeeklyNotificationManager.scheduleWeeklyNotification(this)
      App.toast("Weekly notifications enabled")
    } else {
      WeeklyNotificationManager.cancelWeeklyNotification(this)
      App.toast("Weekly notifications disabled")
    }
    
    Store.dispatch(Action.SetWeeklyNotificationsEnabled(newState))
    menuItem.isChecked = newState
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
    
    WeeklyNotificationManager.showWeeklyResultsNotification(this)
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
          WeeklyNotificationManager.scheduleWeeklyNotification(this)
          Store.dispatch(Action.SetWeeklyNotificationsEnabled(true))
          App.toast("Weekly notifications enabled")
          invalidateOptionsMenu() // Refresh menu
        } else {
          App.toast("Notification permission denied")
        }
      }
    }
  }
  
  companion object {
    private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 3001
  }
}