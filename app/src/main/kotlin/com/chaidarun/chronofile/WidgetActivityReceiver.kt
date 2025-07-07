// © Art Chaidarun

package com.chaidarun.chronofile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class WidgetActivityReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ChronofileWidgetProvider.ACTION_ACTIVITY_CLICKED -> {
                val activityName = intent.getStringExtra(ChronofileWidgetProvider.EXTRA_ACTIVITY_NAME)
                if (activityName != null) {
                    logActivity(context, activityName)
                }
            }
        }
    }
    
    private fun logActivity(context: Context, activityName: String) {
        try {
            // Use reflection to set App.instance if not initialized
            val appClass = App::class.java
            val instanceField = appClass.getDeclaredField("instance")
            instanceField.isAccessible = true
            
            try {
                instanceField.get(null)
            } catch (e: Exception) {
                instanceField.set(null, context.applicationContext as App)
            }
            
            // Initialize the store from files
            Store.dispatch(Action.SetHistory(History.fromFile()))
            
            // Add the activity entry
            History.addEntry(activityName, null)
            
            // Show confirmation toast
            Toast.makeText(context, "Recorded $activityName", Toast.LENGTH_SHORT).show()
            
            // Update all widgets to reflect new data
            ChronofileWidgetProvider.updateAllWidgets(context)
            
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to record activity", Toast.LENGTH_SHORT).show()
        }
    }
}