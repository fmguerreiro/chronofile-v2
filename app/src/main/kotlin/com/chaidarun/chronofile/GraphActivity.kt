// © Art Chaidarun

package com.chaidarun.chronofile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.chaidarun.chronofile.databinding.ActivityGraphBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.reactivex.disposables.CompositeDisposable

class GraphActivity : BaseActivity() {
  private val binding by viewBinding(ActivityGraphBinding::inflate)

  private enum class PresetRange(val text: String, val duration: Long) {
    TODAY("Today", DAY_SECONDS),
    PAST_WEEK("Past week", 7 * DAY_SECONDS),
    PAST_MONTH("Past month", 30 * DAY_SECONDS),
    ALL_TIME("All time", Long.MAX_VALUE)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Set up bottom navigation
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
          // Already on stats, do nothing
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
        else -> false
      }
    }
    
    // Set stats as selected in bottom navigation
    binding.bottomNavigation.selectedItemId = R.id.nav_stats
    
    binding.graphViewPager.run {
      adapter = GraphPagerAdapter(supportFragmentManager)
      currentItem = GraphPagerAdapter.Tab.PIE.ordinal
      offscreenPageLimit = GraphPagerAdapter.Tab.entries.size
    }
    binding.graphTabs.setupWithViewPager(binding.graphViewPager)

    // Set tab font
    // https://stackoverflow.com/a/31067431
    with(binding.graphTabs.getChildAt(0) as ViewGroup) {
      val tabsCount = childCount
      for (i in 0 until tabsCount) {
        with(getChildAt(i) as ViewGroup) {
          val tabChildsCount = childCount
          (0 until tabChildsCount)
            .map { getChildAt(it) }
            .forEach { (it as? TextView)?.typeface = resources.getFont(R.font.epilogue_400) }
        }
      }
    }

    var startTime: Long? = null
    var endTime: Long? = null
    setPresetRange(Store.state.history!!, PresetRange.PAST_MONTH)
    disposables =
      CompositeDisposable().apply {
        add(
          Store.observable
            .map { it.graphConfig.startTime }
            .distinctUntilChanged()
            .subscribe {
              startTime = it
              if (it != null) binding.startDate.text = formatDate(it)
            }
        )
        add(
          Store.observable
            .map { it.graphConfig.endTime }
            .distinctUntilChanged()
            .subscribe {
              endTime = it
              if (it != null) binding.endDate.text = formatDate(it)
            }
        )
      }
    binding.startDate.setOnClickListener {
      DatePickerFragment()
        .apply {
          arguments =
            Bundle().apply {
              putString(DatePickerFragment.ENDPOINT, "start")
              putLong(DatePickerFragment.TIMESTAMP, startTime ?: epochSeconds())
            }
        }
        .show(supportFragmentManager, "datePicker")
    }
    binding.endDate.setOnClickListener {
      DatePickerFragment()
        .apply {
          arguments =
            Bundle().apply {
              putString(DatePickerFragment.ENDPOINT, "end")
              putLong(DatePickerFragment.TIMESTAMP, endTime ?: epochSeconds())
            }
        }
        .show(supportFragmentManager, "datePicker")
    }
    binding.quickRange.setOnClickListener {
      val currentRange = getCurrentPresetRange()
      val presetRanges = PresetRange.entries
      val checkedItem = presetRanges.indexOf(currentRange).takeIf { it >= 0 } ?: -1
      
      with(com.google.android.material.dialog.MaterialAlertDialogBuilder(this@GraphActivity)) {
        setTitle("Select Time Range")
        setSingleChoiceItems(presetRanges.map { it.text }.toTypedArray(), checkedItem, null)
        setPositiveButton("Apply") { dialog, _ ->
          val position = (dialog as AlertDialog).listView.checkedItemPosition
          if (position >= 0) {
            setPresetRange(Store.state.history!!, presetRanges[position])
          }
        }
        setNegativeButton("Cancel", null)
        show()
      }
    }
  }

  private fun setPresetRange(history: History, presetRange: PresetRange) {
    Log.i(TAG, "Setting range to $presetRange")
    val now = history.currentActivityStartTime
    val startTime =
      Math.max(now - presetRange.duration, history.entries.getOrNull(0)?.startTime ?: 0)
    Store.dispatch(Action.SetGraphRangeStart(startTime))
    Store.dispatch(Action.SetGraphRangeEnd(now))
  }
  
  private fun getCurrentPresetRange(): PresetRange? {
    val history = Store.state.history ?: return null
    val startTime = Store.state.graphConfig.startTime ?: return null
    val endTime = Store.state.graphConfig.endTime ?: return null
    val duration = endTime - startTime
    
    return PresetRange.entries.find { presetRange ->
      val expectedDuration = when (presetRange) {
        PresetRange.TODAY -> DAY_SECONDS
        PresetRange.PAST_WEEK -> 7 * DAY_SECONDS
        PresetRange.PAST_MONTH -> 30 * DAY_SECONDS
        PresetRange.ALL_TIME -> endTime - (history.entries.firstOrNull()?.startTime ?: 0)
      }
      Math.abs(duration - expectedDuration) < DAY_SECONDS / 2
    }
  }

  fun onCheckboxClicked(view: View) {
    with(view as CheckBox) {
      when (id) {
        R.id.areaIsGrouped -> Store.dispatch(Action.SetGraphGrouping(isChecked))
        R.id.areaIsStacked -> Store.dispatch(Action.SetGraphStacking(isChecked))
        R.id.pieIsGrouped -> Store.dispatch(Action.SetGraphGrouping(isChecked))
        R.id.radarIsGrouped -> Store.dispatch(Action.SetGraphGrouping(isChecked))
      }
    }
  }

  fun onRadioButtonClicked(view: View) {
    with(view as RadioButton) {
      if (!isChecked) return
      when (id) {
        R.id.radioAverage -> Store.dispatch(Action.SetGraphMetric(Metric.AVERAGE))
        R.id.radioTotal -> Store.dispatch(Action.SetGraphMetric(Metric.TOTAL))
      }
    }
  }
}
