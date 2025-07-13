// © Art Chaidarun

package com.chaidarun.chronofile

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.chaidarun.chronofile.databinding.ActivityMainBinding
import com.chaidarun.chronofile.databinding.FormNfcBinding
import java.util.Calendar
import java.util.Date

class MainActivity : BaseActivity() {
  val binding by viewBinding(ActivityMainBinding::inflate)
  private var nfcFlow: NfcFlow? = null
  
  // Managers for different responsibilities
  private lateinit var dayNavigationManager: DayNavigationManager
  private lateinit var activitySuggestionManager: ActivitySuggestionManager
  private lateinit var insightsAnalyzer: InsightsAnalyzer

  private data class NfcFlow(
    val id1: String,
    val id2: String? = null,
    val dialog: AlertDialog,
    val binding: FormNfcBinding
  )

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    hydrateStoreFromFiles()
    setupRecyclerView()
    setupManagers()
    setupPermissionHandlers()
    setupBottomNavigation()
    setupEntryForm()
    subscribeToStateChanges()
  }

  private fun setupRecyclerView() {
    binding.historyList.layoutManager =
      LinearLayoutManager(this@MainActivity).apply { stackFromEnd = true }
    binding.historyList.adapter = HistoryListAdapter(this@MainActivity)
  }

  private fun setupManagers() {
    // Initialize day navigation manager
    dayNavigationManager = DayNavigationManager(
      binding.currentDateText,
      binding.previousDayButton,
      binding.nextDayButton
    ) { selectedDate ->
      updateTimelineForSelectedDate(selectedDate)
    }

    // Initialize activity suggestion manager
    activitySuggestionManager = ActivitySuggestionManager(
      this,
      binding.frequentActivitiesChipGroup,
      binding.activitySuggestionCard,
      binding.suggestedActivityText,
      binding.suggestionConfidenceText,
      binding.acceptSuggestionButton,
      binding.dismissSuggestionButton
    ) { activity ->
      History.addEntry(activity, null)
    }

    // Initialize insights analyzer
    insightsAnalyzer = InsightsAnalyzer()
    
    // Trigger initial date change to filter timeline to today's entries
    dayNavigationManager.triggerInitialDateChange()
  }

  private fun subscribeToStateChanges() {
    // Listen for history changes to update UI components
    Store.observable
      .map { it.history }
      .distinctUntilChanged()
      .subscribe { 
        activitySuggestionManager.updateFrequentActivities(it)
        activitySuggestionManager.updateActivitySuggestion(it)
        updateInsightsForSelectedDate()
      }
  }

  private fun setupPermissionHandlers() {
    binding.changeSaveDirButton.setOnClickListener { requestStorageAccess() }
    binding.grantLocationButton.setOnClickListener {
      ActivityCompat.requestPermissions(this@MainActivity, APP_PERMISSIONS, PERMISSION_REQUEST_CODE)
    }

    // Check for missing permissions
    if (!APP_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
      }) {
      Log.i(TAG, "Found ungranted permissions")
      binding.grantLocationBanner.visibility = View.VISIBLE
    }
    if (IOUtil.getPref(IOUtil.STORAGE_DIR_PREF).isNullOrEmpty() ||
        !IOUtil.persistAndCheckStoragePermission()) {
      Log.i(TAG, "Found ungranted storage access")
      binding.changeSaveDirBanner.visibility = View.VISIBLE
    }
  }

  private fun setupBottomNavigation() {
    binding.bottomNavigation.setOnItemSelectedListener { item ->
      when (item.itemId) {
        R.id.nav_timeline -> true
        R.id.nav_stats -> {
          startActivity(Intent(this, GraphActivity::class.java))
          overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
          false
        }
        R.id.nav_goals -> {
          startActivity(Intent(this, GoalsActivity::class.java))
          overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
          false
        }
        R.id.nav_insights -> {
          startActivity(Intent(this, RecommendationActivity::class.java))
          overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
          false
        }
        else -> false
      }
    }
  }

  private fun setupEntryForm() {
    binding.addEntryFab.setOnClickListener { showAddEntryDialog() }
    
    binding.addEntry.setOnClickListener {
      History.addEntry(
        binding.addEntryActivity.text.toString(),
        binding.addEntryNote.text.toString()
      )
      binding.addEntryActivity.text?.clear()
      binding.addEntryNote.text?.clear()
      currentFocus?.clearFocus()
    }
    
    binding.addEntryActivity.addTextChangedListener(
      afterTextChanged = {
        val shouldEnable = binding.addEntryActivity.text.toString().isNotBlank()
        binding.addEntry.alpha = if (shouldEnable) 1f else 0.5f
        binding.addEntry.isEnabled = shouldEnable
      }
    )
  }

  override fun onResume() {
    super.onResume()
    binding.historyList.adapter?.notifyDataSetChanged()
    if (intent.action in NFC_INTENT_ACTIONS) {
      processNfcIntent(intent)
    }

    // Set timeline as selected in bottom navigation
    binding.bottomNavigation.selectedItemId = R.id.nav_timeline

    // Update managers
    activitySuggestionManager.updateFrequentActivities(Store.state.history)
    activitySuggestionManager.updateActivitySuggestion(Store.state.history)
    dayNavigationManager.updateDateDisplay()
    updateInsightsForSelectedDate()

    // Setup goal notifications if enabled
    val config = Store.state.config
    if (config?.weeklyNotificationsEnabled == true) {
      GoalNotificationManager.scheduleDailyGoalCheck(this)
    }
  }

  private fun updateTimelineForSelectedDate(selectedDate: Calendar) {
    // Update RecyclerView adapter to show only selected day entries
    (binding.historyList.adapter as? HistoryListAdapter)?.updateSelectedDate(selectedDate.timeInMillis)
    updateInsightsForSelectedDate()
  }

  private fun updateInsightsForSelectedDate() {
    val history = Store.state.history
    val selectedDate = dayNavigationManager.getCurrentDate()
    
    if (history == null || history.entries.isEmpty()) {
      binding.patternInsightsSection.visibility = View.GONE
      binding.daySummarySection.visibility = View.GONE
      binding.moodEnergySection.visibility = View.GONE
      return
    }

    // Update Pattern Insights
    binding.patternInsightsSection.visibility = View.VISIBLE
    binding.patternInsightsText.text = insightsAnalyzer.generateProductivityInsights(history, selectedDate)

    // Update Day Summary
    binding.daySummarySection.visibility = View.VISIBLE
    binding.balanceScore.text = "${insightsAnalyzer.calculateBalanceScore(history, selectedDate)}/10"

    // Update Mood & Energy
    binding.moodEnergySection.visibility = View.VISIBLE
    val (energy, mood) = insightsAnalyzer.analyzeMoodAndEnergy(history, selectedDate)
    binding.energyLevel.text = energy
    binding.moodLevel.text = mood
  }

  // NFC and dialog methods remain the same but simplified
  override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    if (intent != null && intent.action in NFC_INTENT_ACTIONS) {
      processNfcIntent(intent)
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
    super.onActivityResult(requestCode, resultCode, resultData)
    when (requestCode) {
      STORAGE_REQUEST_CODE -> {
        val uri = resultData?.data
        if (resultCode != RESULT_OK || uri == null) {
          App.toast("Storage location not changed")
          return
        }
        binding.changeSaveDirBanner.visibility = View.GONE
        App.toast("Successfully changed storage location")
        IOUtil.persistAndCheckStoragePermission()
        IOUtil.setPref(IOUtil.STORAGE_DIR_PREF, uri.toString())
        hydrateStoreFromFiles()
      }
    }
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    when (requestCode) {
      PERMISSION_REQUEST_CODE ->
        if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
          binding.grantLocationBanner.visibility = View.GONE
          App.toast("Permissions granted successfully :)")
        } else {
          App.toast("You denied permission :(")
        }
    }
  }

  @OptIn(ExperimentalStdlibApi::class, ExperimentalUnsignedTypes::class)
  private fun processNfcIntent(intent: Intent) {
    setIntent(Intent())
    val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return
    val id = tag.id.toHexString().uppercase()
    Log.i(TAG, "Detected NFC tag: $id")
    val nfcFlow = nfcFlow
    val entryToAdd = Store.state.config?.nfcTags?.get(id)
    when {
      entryToAdd != null -> History.addEntry(entryToAdd[0], entryToAdd.getOrNull(1))
      nfcFlow == null -> startNfcRegistrationFlow(id)
      nfcFlow.id1 != id -> showNfcMismatchError(nfcFlow)
      else -> completeNfcRegistration(nfcFlow, id)
    }
  }

  private fun startNfcRegistrationFlow(id: String) {
    val binding = FormNfcBinding.inflate(LayoutInflater.from(this), null, false)
    val dialog = AlertDialog.Builder(this, R.style.MyAlertDialogTheme)
      .setTitle("Found new NFC tag")
      .setView(binding.root)
      .setOnDismissListener { this@MainActivity.nfcFlow = null }
      .setPositiveButton("OK") { _, _ -> registerNfcTag(id) }
      .setNegativeButton("Cancel", null)
      .show()
    dialog.getButton(Dialog.BUTTON_POSITIVE).visibility = View.GONE
    this.nfcFlow = NfcFlow(id1 = id, dialog = dialog, binding = binding)
  }

  private fun showNfcMismatchError(nfcFlow: NfcFlow) {
    nfcFlow.dialog.setTitle("Unusable NFC tag")
    nfcFlow.binding.formNfcTapAgain.visibility = View.GONE
    nfcFlow.binding.formNfcMismatch.visibility = View.VISIBLE
    nfcFlow.dialog.getButton(Dialog.BUTTON_POSITIVE).visibility = View.VISIBLE
    nfcFlow.dialog.getButton(Dialog.BUTTON_NEGATIVE).visibility = View.GONE
  }

  private fun completeNfcRegistration(nfcFlow: NfcFlow, id: String) {
    nfcFlow.dialog.setTitle("Register NFC tag")
    nfcFlow.binding.formNfcTapAgain.visibility = View.GONE
    nfcFlow.binding.formNfcEnterInfo.visibility = View.VISIBLE
    val okButton = nfcFlow.dialog.getButton(Dialog.BUTTON_POSITIVE)
    nfcFlow.binding.formNfcActivity.addTextChangedListener(
      afterTextChanged = {
        okButton.isEnabled = nfcFlow.binding.formNfcActivity.text.toString().isNotBlank()
      }
    )
    nfcFlow.binding.formNfcInputs.visibility = View.VISIBLE
    nfcFlow.binding.formNfcActivity.requestFocus()
    okButton.isEnabled = false
    okButton.visibility = View.VISIBLE
    this.nfcFlow = nfcFlow.copy(id2 = id)
  }

  private fun registerNfcTag(id: String) {
    val nfcFlow = this.nfcFlow
    if (nfcFlow != null && nfcFlow.id1 == nfcFlow.id2) {
      val inputs = mutableListOf(nfcFlow.binding.formNfcActivity.text.toString())
      val note = nfcFlow.binding.formNfcNote.text.toString()
      if (note.isNotBlank()) {
        inputs.add(note)
      }
      Store.dispatch(Action.RegisterNfcTag(id, inputs))
      App.toast("NFC tag registered - try tapping it!")
    }
  }

  private fun requestStorageAccess() {
    startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), STORAGE_REQUEST_CODE)
  }

  private fun hydrateStoreFromFiles() {
    Store.dispatch(Action.SetConfigFromFile(Config.fromFile()))
    Store.dispatch(Action.SetHistory(History.fromFile()))
  }

  private fun showAddEntryDialog() {
    val dialogView = layoutInflater.inflate(R.layout.form_entry, null)
    val startTimeInput = dialogView.findViewById<EditText>(R.id.formEntryStartTime)
    val activityInput = dialogView.findViewById<EditText>(R.id.formEntryActivity)
    val noteInput = dialogView.findViewById<EditText>(R.id.formEntryNote)
    
    noteInput.visibility = View.GONE

    // Pre-fill start time with the last entry's end time
    val currentHistory = Store.state.history
    if (currentHistory != null) {
      val lastEntryEndTime = currentHistory.currentActivityStartTime
      startTimeInput.setText(formatTime(Date(lastEntryEndTime * 1000)))
    }

    val dialog = AlertDialog.Builder(this, R.style.MyAlertDialogTheme)
      .setTitle("Add Entry")
      .setView(dialogView)
      .setPositiveButton("Add") { _, _ ->
        handleAddEntry(startTimeInput.text.toString(), activityInput.text.toString())
      }
      .setNegativeButton("Cancel", null)
      .create()

    dialog.setOnShowListener {
      val addButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
      addButton.isEnabled = false
      activityInput.requestFocus()
      dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
      activityInput.addTextChangedListener(
        afterTextChanged = {
          addButton.isEnabled = activityInput.text.toString().isNotBlank()
        }
      )
    }

    dialog.show()
  }

  private fun handleAddEntry(startTime: String, activity: String) {
    if (activity.isNotBlank()) {
      val currentState = Store.state.history
      if (currentState != null) {
        val currentActivityStartTime = currentState.currentActivityStartTime
        Store.dispatch(Action.AddEntry(activity, "", null))
        
        // If the user changed the start time, edit it after adding
        val defaultTime = formatTime(Date(currentActivityStartTime * 1000))
        if (startTime != defaultTime) {
          Store.dispatch(Action.EditEntry(currentActivityStartTime, startTime, activity, ""))
        }
      }
    }
  }

  companion object {
    private val APP_PERMISSIONS = arrayOf(
      Manifest.permission.ACCESS_COARSE_LOCATION, 
      Manifest.permission.ACCESS_FINE_LOCATION
    )
    private val NFC_INTENT_ACTIONS = arrayOf(
      NfcAdapter.ACTION_NDEF_DISCOVERED,
      NfcAdapter.ACTION_TECH_DISCOVERED,
      NfcAdapter.ACTION_TAG_DISCOVERED
    )
    private const val PERMISSION_REQUEST_CODE = 1
    private const val STORAGE_REQUEST_CODE = 2
  }
}