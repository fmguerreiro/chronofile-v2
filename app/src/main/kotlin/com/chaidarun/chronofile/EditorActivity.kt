package com.chaidarun.chronofile

import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NavUtils
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chaidarun.chronofile.databinding.ActivityEditorBinding
import com.chaidarun.chronofile.databinding.ItemActivityGroupBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class EditorActivity : BaseActivity() {
  private val binding by viewBinding(ActivityEditorBinding::inflate)
  private lateinit var groupsAdapter: ActivityGroupsAdapter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    setupGroupsList()
    setupButtons()
    setupJsonEditor()
    setupBottomNavigation()
  }
  
  private fun setupGroupsList() {
    groupsAdapter = ActivityGroupsAdapter { action ->
      when (action) {
        is GroupAction.Edit -> showEditGroupDialog(action.groupName, action.activities)
        is GroupAction.Delete -> showDeleteGroupDialog(action.groupName)
        is GroupAction.AddActivity -> showAddActivityDialog(action.groupName)
        is GroupAction.RemoveActivity -> removeActivityFromGroup(action.groupName, action.activity)
      }
    }
    
    binding.groupsList.apply {
      layoutManager = LinearLayoutManager(this@EditorActivity)
      adapter = groupsAdapter
    }
    
    // Load current groups
    loadGroups()
  }
  
  private fun setupButtons() {
    binding.addGroupButton.setOnClickListener {
      showAddGroupDialog()
    }
    
    binding.loadDefaultGroupsButton.setOnClickListener {
      showDefaultGroupsDialog()
    }
    
    binding.editJsonButton.setOnClickListener {
      showJsonEditor()
    }
    
    binding.jsonCancelButton.setOnClickListener {
      hideJsonEditor()
    }
    
    binding.jsonSaveButton.setOnClickListener {
      saveJsonConfig()
    }
    
  }
  
  private fun setupJsonEditor() {
    binding.editorInstructions.apply {
      text = HtmlCompat.fromHtml(
        "Advanced users can directly edit the JSON configuration. Use this to configure activity groups, NFC tags, and weekly goals.<br /><br />Example:<br />{\"activityGroups\":{\"Exercise\":[\"Gym\",\"Running\"],\"Work\":[\"Coding\",\"Meetings\"]}}",
        HtmlCompat.FROM_HTML_MODE_LEGACY
      )
      movementMethod = LinkMovementMethod.getInstance()
    }
  }
  
  private fun loadGroups() {
    val config = Store.state.config
    val groups = mutableMapOf<String, MutableList<String>>()
    
    // Extract groups from current config
    config?.let { cfg ->
      val jsonString = cfg.serialize()
      try {
        val jsonMap = com.google.gson.Gson().fromJson(jsonString, Map::class.java) as? Map<String, Any>
        val activityGroups = jsonMap?.get("activityGroups") as? Map<String, List<String>>
        activityGroups?.forEach { (groupName, activities) ->
          groups[groupName] = activities.toMutableList()
        }
      } catch (e: Exception) {
        Log.w(TAG, "Failed to parse activity groups", e)
      }
    }
    
    groupsAdapter.updateGroups(groups)
  }
  
  private fun saveGroups(groups: Map<String, List<String>>) {
    val currentConfig = Store.state.config
    val configMap = if (currentConfig != null) {
      try {
        val gson = com.google.gson.Gson()
        gson.fromJson(currentConfig.serialize(), Map::class.java) as MutableMap<String, Any>
      } catch (e: Exception) {
        mutableMapOf<String, Any>()
      }
    } else {
      mutableMapOf<String, Any>()
    }
    
    if (groups.isNotEmpty()) {
      configMap["activityGroups"] = groups
    } else {
      configMap.remove("activityGroups")
    }
    
    val gson = com.google.gson.GsonBuilder()
      .disableHtmlEscaping()
      .setPrettyPrinting()
      .create()
    
    val newConfigJson = gson.toJson(configMap)
    Store.dispatch(Action.SetConfigFromText(newConfigJson))
    Log.i(TAG, "Updated activity groups")
  }
  
  private fun showAddGroupDialog() {
    val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_group, null)
    val groupNameInput = dialogView.findViewById<TextInputEditText>(R.id.groupNameInput)
    val activitiesInput = dialogView.findViewById<TextInputEditText>(R.id.activitiesInput)
    
    AlertDialog.Builder(this, R.style.MyAlertDialogTheme)
      .setTitle("Add Activity Group")
      .setView(dialogView)
      .setPositiveButton("Add") { _, _ ->
        val groupName = groupNameInput.text.toString().trim()
        val activitiesText = activitiesInput.text.toString().trim()
        val activities = if (activitiesText.isNotEmpty()) {
          activitiesText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
          emptyList()
        }
        
        if (groupName.isNotEmpty()) {
          addGroup(groupName, activities)
        }
      }
      .setNegativeButton("Cancel", null)
      .show()
  }
  
  private fun showEditGroupDialog(groupName: String, activities: List<String>) {
    val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_group, null)
    val groupNameInput = dialogView.findViewById<TextInputEditText>(R.id.groupNameInput)
    val activitiesInput = dialogView.findViewById<TextInputEditText>(R.id.activitiesInput)
    
    groupNameInput.setText(groupName)
    activitiesInput.setText(activities.joinToString(", "))
    
    AlertDialog.Builder(this, R.style.MyAlertDialogTheme)
      .setTitle("Edit Activity Group")
      .setView(dialogView)
      .setPositiveButton("Save") { _, _ ->
        val newGroupName = groupNameInput.text.toString().trim()
        val activitiesText = activitiesInput.text.toString().trim()
        val newActivities = if (activitiesText.isNotEmpty()) {
          activitiesText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
          emptyList()
        }
        
        if (newGroupName.isNotEmpty()) {
          editGroup(groupName, newGroupName, newActivities)
        }
      }
      .setNegativeButton("Cancel", null)
      .show()
  }
  
  private fun showDeleteGroupDialog(groupName: String) {
    AlertDialog.Builder(this, R.style.MyAlertDialogTheme)
      .setTitle("Delete Group")
      .setMessage("Are you sure you want to delete the \"$groupName\" group?")
      .setPositiveButton("Delete") { _, _ ->
        deleteGroup(groupName)
      }
      .setNegativeButton("Cancel", null)
      .show()
  }
  
  private fun showAddActivityDialog(groupName: String) {
    val input = EditText(this)
    input.hint = "Activity name"
    
    AlertDialog.Builder(this, R.style.MyAlertDialogTheme)
      .setTitle("Add Activity to $groupName")
      .setView(input)
      .setPositiveButton("Add") { _, _ ->
        val activity = input.text.toString().trim()
        if (activity.isNotEmpty()) {
          addActivityToGroup(groupName, activity)
        }
      }
      .setNegativeButton("Cancel", null)
      .show()
  }
  
  private fun showDefaultGroupsDialog() {
    val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_default_activity_groups, null)
    
    val dialog = AlertDialog.Builder(this, R.style.MyAlertDialogTheme)
      .setView(dialogView)
      .setNegativeButton("Cancel", null)
      .create()
    
    val presetsRecyclerView = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.presetsRecyclerView)
    presetsRecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
    presetsRecyclerView.adapter = DefaultActivityGroupsAdapter { presetName, groups ->
      loadDefaultGroups(presetName, groups)
      dialog.dismiss()
    }
    
    dialog.show()
  }
  
  private fun loadDefaultGroups(presetName: String, defaultGroups: Map<String, List<String>>) {
    val currentGroups = groupsAdapter.getGroups().toMutableMap()
    var addedCount = 0
    var skippedCount = 0
    
    defaultGroups.forEach { (groupName, activities) ->
      if (currentGroups.containsKey(groupName)) {
        // Merge activities into existing group
        val existingActivities = currentGroups[groupName]?.toMutableList() ?: mutableListOf()
        val newActivities = activities.filter { !existingActivities.contains(it) }
        if (newActivities.isNotEmpty()) {
          existingActivities.addAll(newActivities)
          currentGroups[groupName] = existingActivities
          addedCount++
        } else {
          skippedCount++
        }
      } else {
        // Add new group
        currentGroups[groupName] = activities
        addedCount++
      }
    }
    
    saveGroups(currentGroups)
    loadGroups()
    
    val message = when {
      addedCount > 0 && skippedCount == 0 -> "Added $addedCount groups from $presetName preset"
      addedCount > 0 && skippedCount > 0 -> "Added $addedCount groups, merged $skippedCount existing groups"
      skippedCount > 0 -> "All groups from $presetName already exist"
      else -> "No groups were added"
    }
    
    App.toast(message)
  }
  
  private fun addGroup(groupName: String, activities: List<String>) {
    val currentGroups = groupsAdapter.getGroups().toMutableMap()
    currentGroups[groupName] = activities
    saveGroups(currentGroups)
    loadGroups()
  }
  
  private fun editGroup(oldGroupName: String, newGroupName: String, activities: List<String>) {
    val currentGroups = groupsAdapter.getGroups().toMutableMap()
    currentGroups.remove(oldGroupName)
    currentGroups[newGroupName] = activities
    saveGroups(currentGroups)
    loadGroups()
  }
  
  private fun deleteGroup(groupName: String) {
    val currentGroups = groupsAdapter.getGroups().toMutableMap()
    currentGroups.remove(groupName)
    saveGroups(currentGroups)
    loadGroups()
  }
  
  private fun addActivityToGroup(groupName: String, activity: String) {
    val currentGroups = groupsAdapter.getGroups().toMutableMap()
    val activities = currentGroups[groupName]?.toMutableList() ?: mutableListOf()
    if (!activities.contains(activity)) {
      activities.add(activity)
      currentGroups[groupName] = activities
      saveGroups(currentGroups)
      loadGroups()
    }
  }
  
  private fun removeActivityFromGroup(groupName: String, activity: String) {
    val currentGroups = groupsAdapter.getGroups().toMutableMap()
    val activities = currentGroups[groupName]?.toMutableList() ?: return
    activities.remove(activity)
    currentGroups[groupName] = activities
    saveGroups(currentGroups)
    loadGroups()
  }
  
  private fun showJsonEditor() {
    binding.editorText.setText(Store.state.config?.serialize() ?: "")
    binding.jsonEditorContainer.visibility = View.VISIBLE
  }
  
  private fun hideJsonEditor() {
    binding.jsonEditorContainer.visibility = View.GONE
  }
  
  private fun saveJsonConfig() {
    Store.dispatch(Action.SetConfigFromText(binding.editorText.text.toString()))
    Log.i(TAG, "Saved JSON config")
    hideJsonEditor()
    loadGroups() // Refresh the groups list
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
          val intent = Intent(this, RecommendationActivity::class.java)
          startActivity(intent)
          overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
          finish()
          true
        }
        R.id.nav_settings -> {
          // Already on settings, do nothing
          true
        }
        else -> false
      }
    }
    
    // Set settings as selected in bottom navigation
    binding.bottomNavigation.selectedItemId = R.id.nav_settings
  }

  override fun onSupportNavigateUp(): Boolean {
    NavUtils.navigateUpFromSameTask(this)
    return true
  }
}

sealed class GroupAction {
  data class Edit(val groupName: String, val activities: List<String>) : GroupAction()
  data class Delete(val groupName: String) : GroupAction()
  data class AddActivity(val groupName: String) : GroupAction()
  data class RemoveActivity(val groupName: String, val activity: String) : GroupAction()
}

class ActivityGroupsAdapter(
  private val onAction: (GroupAction) -> Unit
) : RecyclerView.Adapter<ActivityGroupViewHolder>() {
  
  private var groups = mutableMapOf<String, List<String>>()
  
  fun updateGroups(newGroups: Map<String, List<String>>) {
    groups.clear()
    groups.putAll(newGroups)
    notifyDataSetChanged()
  }
  
  fun getGroups(): Map<String, List<String>> = groups.toMap()
  
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityGroupViewHolder {
    val binding = ItemActivityGroupBinding.inflate(
      LayoutInflater.from(parent.context), parent, false
    )
    return ActivityGroupViewHolder(binding, onAction)
  }
  
  override fun onBindViewHolder(holder: ActivityGroupViewHolder, position: Int) {
    val groupName = groups.keys.elementAt(position)
    val activities = groups[groupName] ?: emptyList()
    holder.bind(groupName, activities)
  }
  
  override fun getItemCount(): Int = groups.size
}

class ActivityGroupViewHolder(
  private val binding: ItemActivityGroupBinding,
  private val onAction: (GroupAction) -> Unit
) : RecyclerView.ViewHolder(binding.root) {
  
  fun bind(groupName: String, activities: List<String>) {
    binding.groupName.text = groupName
    
    // Setup action buttons
    binding.editGroupButton.setOnClickListener {
      onAction(GroupAction.Edit(groupName, activities))
    }
    
    binding.deleteGroupButton.setOnClickListener {
      onAction(GroupAction.Delete(groupName))
    }
    
    binding.addActivityButton.setOnClickListener {
      onAction(GroupAction.AddActivity(groupName))
    }
    
    // Setup activity chips
    binding.activitiesChipGroup.removeAllViews()
    activities.forEach { activity ->
      val chip = Chip(binding.root.context).apply {
        text = activity
        isCloseIconVisible = true
        setOnCloseIconClickListener {
          onAction(GroupAction.RemoveActivity(groupName, activity))
        }
      }
      binding.activitiesChipGroup.addView(chip)
    }
  }
}
