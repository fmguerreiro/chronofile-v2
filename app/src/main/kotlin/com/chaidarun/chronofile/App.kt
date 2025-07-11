// © Art Chaidarun

package com.chaidarun.chronofile

import android.app.Application
import android.content.Context
import android.widget.Toast

class App : Application() {

  override fun onCreate() {
    instance = this
    super.onCreate()
    
    // Initialize notification channel
    WeeklyNotificationManager.createNotificationChannel(this)
  }

  companion object {
    lateinit var instance: App
      private set

    val ctx: Context
      get() = instance.applicationContext

    fun toast(message: String) = Toast.makeText(ctx, message, Toast.LENGTH_LONG).show()
  }
}
