// © Art Chaidarun

package com.chaidarun.chronofile

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.os.Bundle

class ChronofileWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        // Called when widget is resized - update layout accordingly
        updateAppWidget(context, appWidgetManager, appWidgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    companion object {
        const val ACTION_ACTIVITY_CLICKED = "com.chaidarun.chronofile.ACTIVITY_CLICKED"
        const val EXTRA_ACTIVITY_NAME = "activity_name"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            // Get the most common activities
            val commonActivities = getCommonActivities(context)
            
            // Determine if we should use compact layout based on widget size
            val useCompactLayout = shouldUseCompactLayout(context, appWidgetManager, appWidgetId)
            
            // Construct the RemoteViews object with appropriate layout
            val layoutId = if (useCompactLayout) R.layout.widget_chronofile_compact else R.layout.widget_chronofile
            val views = RemoteViews(context.packageName, layoutId)
            
            // Update activity buttons
            val buttonIds = arrayOf(
                R.id.widget_activity_1,
                R.id.widget_activity_2,
                R.id.widget_activity_3,
                R.id.widget_activity_4
            )
            
            for (i in buttonIds.indices) {
                if (i < commonActivities.size) {
                    val activity = commonActivities[i]
                    val displayText = if (useCompactLayout) {
                        getActivityIcon(activity)
                    } else {
                        // Truncate long activity names to prevent overflow
                        if (activity.length > 8) activity.take(6) + ".." else activity
                    }
                    views.setTextViewText(buttonIds[i], displayText)
                    views.setOnClickPendingIntent(buttonIds[i], 
                        createActivityPendingIntent(context, activity, appWidgetId))
                    
                    // Set content description for accessibility
                    views.setContentDescription(buttonIds[i], "Log $activity activity")
                } else {
                    views.setTextViewText(buttonIds[i], "")
                    views.setOnClickPendingIntent(buttonIds[i], null)
                    views.setContentDescription(buttonIds[i], "")
                }
            }
            
            // Set up the app icon click to open the main app
            val appIntent = Intent(context, MainActivity::class.java)
            val appPendingIntent = PendingIntent.getActivity(
                context, 0, appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_app_icon, appPendingIntent)

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun createActivityPendingIntent(
            context: Context,
            activity: String,
            appWidgetId: Int
        ): PendingIntent {
            val intent = Intent(context, WidgetActivityReceiver::class.java).apply {
                action = ACTION_ACTIVITY_CLICKED
                putExtra(EXTRA_ACTIVITY_NAME, activity)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            
            // Use activity name hash as request code to ensure unique pending intents
            val requestCode = activity.hashCode()
            return PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun getCommonActivities(context: Context): List<String> {
            return try {
                // Use reflection to set App.instance if not initialized
                val appClass = App::class.java
                val instanceField = appClass.getDeclaredField("instance")
                instanceField.isAccessible = true
                
                try {
                    instanceField.get(null)
                } catch (e: Exception) {
                    instanceField.set(null, context.applicationContext as App)
                }
                
                val history = History.fromFile()
                val suggestions = history.getIntelligentActivitySuggestions()
                
                // Ensure we have at least some activities
                if (suggestions.isEmpty()) {
                    getFallbackActivities()
                } else {
                    // Limit to 4 activities for widget
                    suggestions.take(4)
                }
            } catch (e: Exception) {
                android.util.Log.w("ChronofileWidget", "Failed to load activities from history", e)
                getFallbackActivities()
            }
        }
        
        private fun getFallbackActivities(): List<String> {
            return listOf("Work", "Break", "Lunch", "Meeting")
        }

        private enum class WidgetLayout {
            COMPACT,    // Emoji only
            NORMAL      // Full text
        }

        private fun getWidgetLayout(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ): WidgetLayout {
            // Check for user preference override first
            val userPref = IOUtil.getPref("widget_layout_preference")
            when (userPref) {
                "compact" -> return WidgetLayout.COMPACT
                "normal" -> return WidgetLayout.NORMAL
                // "auto" or null falls through to automatic detection
            }
            
            return try {
                val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
                val maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minWidth)
                
                // Use the larger of min/max width for more reliable detection
                val effectiveWidth = maxOf(minWidth, maxWidth)
                
                // Validate that we got reasonable width values
                if (effectiveWidth <= 0) {
                    throw IllegalStateException("Invalid widget width: $effectiveWidth")
                }
                
                // Convert dp to pixels for more accurate calculation
                val density = context.resources.displayMetrics.density
                val widthPx = (effectiveWidth * density).toInt()
                
                // Multiple breakpoints for better responsiveness
                when {
                    effectiveWidth < 120 -> WidgetLayout.COMPACT  // Very tiny widgets
                    effectiveWidth < 200 -> WidgetLayout.COMPACT  // Small widgets
                    widthPx < 500 && density < 2.0 -> WidgetLayout.COMPACT  // Low density small screens
                    else -> WidgetLayout.NORMAL
                }
            } catch (e: Exception) {
                // Multi-level fallback system
                try {
                    val displayMetrics = context.resources.displayMetrics
                    val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
                    val density = displayMetrics.density
                    
                    // More sophisticated screen size detection
                    when {
                        screenWidthDp < 320 -> WidgetLayout.COMPACT  // Very small screens
                        screenWidthDp < 360 && density < 2.0 -> WidgetLayout.COMPACT  // Small low-DPI screens
                        else -> WidgetLayout.NORMAL
                    }
                } catch (fallbackException: Exception) {
                    // Log the error for debugging but don't crash
                    android.util.Log.w("ChronofileWidget", "Failed to determine widget layout", fallbackException)
                    // Conservative fallback - compact is safer for unknown sizes
                    WidgetLayout.COMPACT
                }
            }
        }

        private fun shouldUseCompactLayout(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ): Boolean {
            return getWidgetLayout(context, appWidgetManager, appWidgetId) == WidgetLayout.COMPACT
        }

        private fun getActivityIcon(activity: String): String {
            return EmojiDatabase.findByKeyword(activity)
        }

        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, ChronofileWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(android.content.ComponentName(context, ChronofileWidgetProvider::class.java))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }
}