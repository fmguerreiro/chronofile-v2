package com.chaidarun.chronofile

import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import android.util.Log
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.chaidarun.chronofile.databinding.FormEntryBinding
import com.chaidarun.chronofile.databinding.ItemDateBinding
import com.chaidarun.chronofile.databinding.ItemEntryBinding
import com.chaidarun.chronofile.databinding.ItemTimeBinding
import androidx.core.content.ContextCompat
import java.util.Date
import kotlin.system.measureTimeMillis

enum class ViewType {
  DATE,
  ENTRY,
  SPACER,
  TIME
}

sealed class ListItem(val viewType: ViewType)

private data class DateItem(val date: Date) : ListItem(ViewType.DATE)

private data class EntryItem(val entry: Entry, val itemStart: Long, val itemEnd: Long) :
  ListItem(ViewType.ENTRY)

private data class SpacerItem(val height: Int) : ListItem(ViewType.SPACER)

private data class TimeItem(val time: Date) : ListItem(ViewType.TIME)

class HistoryListAdapter(private val appActivity: MainActivity) :
  RecyclerView.Adapter<HistoryListAdapter.ViewHolder>() {

  private var itemList = listOf<ListItem>()
  private var itemListLength = 0
  private var selectedEntry: Entry? = null
  private var selectedDateTimestamp: Long? = null
  private val receiver by lazy {
    object : ResultReceiver(Handler()) {
      override fun onReceiveResult(resultCode: Int, resultData: Bundle) {
        if (resultCode == FetchAddressIntentService.SUCCESS_CODE) {
          resultData.getString(FetchAddressIntentService.RESULT_DATA_KEY)?.let { App.toast(it) }
        }
      }
    }
  }
  private val actionModeCallback by lazy {
    object : ActionMode.Callback {
      override fun onActionItemClicked(mode: ActionMode, item: MenuItem?): Boolean {
        val entry = selectedEntry
        if (entry != null) {
          when (item?.itemId) {
            R.id.delete -> Store.dispatch(Action.RemoveEntry(entry.startTime))
            R.id.edit -> {
              val binding = FormEntryBinding.inflate(LayoutInflater.from(appActivity), null, false)
              with(AlertDialog.Builder(appActivity, R.style.MyAlertDialogTheme)) {
                setTitle("Edit entry")
                binding.formEntryActivity.setText(entry.activity)
                binding.formEntryNote.setText(entry.note ?: "")
                setView(binding.root)
                setPositiveButton("OK") { _, _ ->
                  Store.dispatch(
                    Action.EditEntry(
                      entry.startTime,
                      binding.formEntryStartTime.text.toString(),
                      binding.formEntryActivity.text.toString(),
                      binding.formEntryNote.text.toString()
                    )
                  )
                }
                setNegativeButton("Cancel", null)
                show()
              }
            }
            R.id.location -> {
              if (entry.latLong == null) {
                App.toast("No location data available")
              } else {
                val location =
                  Location("dummyprovider").apply {
                    latitude = entry.latLong.first
                    longitude = entry.latLong.second
                  }
                val intent = Intent(App.ctx, FetchAddressIntentService::class.java)
                intent.putExtra(FetchAddressIntentService.RECEIVER, receiver)
                intent.putExtra(FetchAddressIntentService.LOCATION_DATA_EXTRA, location)
                App.ctx.startService(intent)
              }
            }
            else -> App.toast("Unknown action!")
          }
        }
        selectedEntry = null
        mode.finish()
        return true
      }

      override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        mode?.menuInflater?.inflate(R.menu.menu_edit, menu)
        return true
      }

      override fun onPrepareActionMode(p0: ActionMode?, p1: Menu?) = false

      override fun onDestroyActionMode(mode: ActionMode?) {}
    }
  }

  private val subscription =
    Store.observable
      .map { Pair(it.history, it.searchQuery) }
      .distinctUntilChanged()
      .subscribe { (history, query) ->
        if (history == null) {
          Log.i(TAG, "History is null")
          return@subscribe
        }

        updateHistoryView(history, query)
      }

  override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
    subscription.dispose()
    super.onDetachedFromRecyclerView(recyclerView)
  }

  override fun getItemCount() = itemListLength

  override fun getItemViewType(position: Int) = itemList[position].viewType.ordinal

  override fun onCreateViewHolder(parent: ViewGroup, viewTypeOrdinal: Int) =
    when (ViewType.entries[viewTypeOrdinal]) {
      ViewType.DATE ->
        DateViewHolder(ItemDateBinding.inflate(LayoutInflater.from(parent.context), parent, false))
      ViewType.ENTRY ->
        EntryViewHolder(
          ItemEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
      ViewType.TIME ->
        TimeViewHolder(ItemTimeBinding.inflate(LayoutInflater.from(parent.context), parent, false))
      ViewType.SPACER ->
        SpacerViewHolder(
          LinearLayout(appActivity).apply {
            layoutParams =
              LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
              )
          }
        )
    }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.bindItem(itemList[position])
    
    // Hide top/bottom lines for first/last entry items
    if (holder is EntryViewHolder) {
      val entryPositions = itemList.mapIndexedNotNull { index, item -> 
        if (item.viewType == ViewType.ENTRY) index else null 
      }
      val entryIndex = entryPositions.indexOf(position)
      
      val isFirstEntry = entryIndex == 0
      val isLastEntry = entryIndex == entryPositions.size - 1
      
      holder.binding.topLineCover.visibility = if (isFirstEntry) View.VISIBLE else View.GONE
      holder.binding.bottomLineCover.visibility = if (isLastEntry) View.VISIBLE else View.GONE
    }
  }

  companion object {
    /** We limit shown entries because showing all can be slow */
    private const val MAX_ENTRIES_TO_SHOW = 1000
  }

  abstract class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    abstract fun bindItem(listItem: ListItem): Any
  }

  class DateViewHolder(val binding: ItemDateBinding) : ViewHolder(binding.root) {
    override fun bindItem(listItem: ListItem) {
      binding.date.text = (listItem as? DateItem)?.date?.let { formatDate(it) } ?: ""
    }
  }

  inner class EntryViewHolder(val binding: ItemEntryBinding) : ViewHolder(binding.root) {
    override fun bindItem(listItem: ListItem) {
      val (entry, itemStart, itemEnd) = listItem as EntryItem
      val activity = entry.activity
      val note = entry.note

      with(itemView) {
        binding.entryActivity.text = formatActivityAsRoutineBlock(activity, itemStart)
        
        // Set time range
        binding.entryTimeRange.text = "${formatTime(Date(itemStart * 1000))} - ${formatTime(Date(itemEnd * 1000))}"
        
        // Show/hide note based on content
        if (note.isNullOrEmpty()) {
          binding.entryNote.visibility = View.GONE
        } else {
          binding.entryNote.visibility = View.VISIBLE
          binding.entryNote.text = note
        }
        
        // Set activity icon based on activity name
        binding.activityIcon.setImageResource(getActivityIcon(activity))
        
        // Calculate and display duration
        val durationHours = (itemEnd - itemStart) / 3600.0
        binding.entryDuration.text = when {
            durationHours >= 1.0 -> "${String.format("%.1f", durationHours)}h"
            else -> "${((itemEnd - itemStart) / 60).toInt()}m"
        }
        
        setOnClickListener { History.addEntry(activity, note) }
        setOnLongClickListener {
          (context as AppCompatActivity).startActionMode(actionModeCallback)
          selectedEntry = entry
          true
        }
      }
    }
    
    private fun getActivityIcon(activity: String): Int {
      return IconDatabase.findByKeyword(activity)
    }
    
    private fun formatActivityAsRoutineBlock(activity: String, startTime: Long): String {
      return activity.replaceFirstChar { it.uppercase() }
    }
    
  }

  class SpacerViewHolder(view: View) : ViewHolder(view) {
    override fun bindItem(listItem: ListItem) {
      with(itemView as LinearLayout) {
        layoutParams.height = (listItem as SpacerItem).height
        requestLayout()
      }
    }
  }

  class TimeViewHolder(val binding: ItemTimeBinding) : ViewHolder(binding.root) {
    override fun bindItem(listItem: ListItem) {
      binding.time.text = formatTime((listItem as TimeItem).time)
    }
  }
  
  // Filter timeline to show only entries from the selected date
  fun updateSelectedDate(selectedDateTimestamp: Long) {
    this.selectedDateTimestamp = selectedDateTimestamp
    // Manually trigger update since selectedDateTimestamp is not in Store
    val currentState = Store.state
    if (currentState.history != null) {
      updateHistoryView(currentState.history, currentState.searchQuery)
    }
  }
  
  private fun updateHistoryView(history: History, query: String?) {
    val elapsedMs = measureTimeMillis {
      // Select entries to show, and also compute activity end times for convenience later on
      val entriesToShow =
        mutableListOf<Pair<Entry, Long>>().apply {
          var numMatchingEntries = 0
          var matchingEntriesSeconds = 0L
          var lastSeenStartTime = history.currentActivityStartTime
          for (entry in history.entries.reversed()) {
            val matchesQuery = query == null ||
                query.lowercase() in "${entry.activity}|${entry.note ?: ""}".lowercase() ||
                query.toIntOrNull() != null &&
                  formatForSearch(entry.startTime).startsWith(query)
                  
            val matchesDate = selectedDateTimestamp == null || 
                isSameDay(entry.startTime * 1000, selectedDateTimestamp!!)
                
            if (matchesQuery && matchesDate) {
              matchingEntriesSeconds += lastSeenStartTime - entry.startTime
              if (++numMatchingEntries <= MAX_ENTRIES_TO_SHOW) {
                add(Pair(entry, lastSeenStartTime))
              }
            }
            lastSeenStartTime = entry.startTime
          }
          reverse()
          if (query != null) {
            App.toast(
              "$numMatchingEntries results, ${formatDuration(matchingEntriesSeconds, showDays = true, showMinutes = false)}"
            )
          }
        }

      // Construct list items
      val items = mutableListOf<ListItem>(SpacerItem(32))
      var lastDateShown: String? = null
      var lastTimeShown: Long? = null
      for ((entry, endTime) in entriesToShow) {
        // Show date marker - REMOVED
        val startDate = formatDate(entry.startTime)
        if (startDate != lastDateShown) {
          // items.add(DateItem(Date(entry.startTime * 1000)))
          lastDateShown = startDate
        }

        // Start time is now shown within the entry item
        lastTimeShown = entry.startTime

        // Show entry either once or twice depending on whether it crosses midnight
        val endDate = formatDate(endTime)
        if (startDate != endDate) {
          val midnight = getPreviousMidnight(endTime)
          items.add(EntryItem(entry, entry.startTime, midnight))
          // items.add(DateItem(Date(endTime * 1000)))
          lastDateShown = endDate
          items.add(EntryItem(entry, midnight, endTime))
        } else {
          items.add(EntryItem(entry, entry.startTime, endTime))
        }

        // End time is now shown within the entry item
        lastTimeShown = endTime
      }
      items.add(SpacerItem(32))
      itemList = items
      itemListLength = items.size
      notifyDataSetChanged()
      appActivity.binding.historyList.scrollToPosition(items.size - 1)
    }

    Log.i(TAG, "Rendered history view in $elapsedMs ms")
  }
  
  private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
    val cal1 = java.util.Calendar.getInstance().apply { timeInMillis = timestamp1 }
    val cal2 = java.util.Calendar.getInstance().apply { timeInMillis = timestamp2 }
    val result = cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
           cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
    
    // Debug logging
    android.util.Log.d("DateFilter", "Comparing entry ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(timestamp1))} with selected ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(timestamp2))} = $result")
    
    return result
  }
}
