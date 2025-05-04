package com.example.nimbus.data.worker

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Helper class for managing background work with WorkManager.
 * Responsible for scheduling periodic weather updates.
 */
object WorkManagerHelper {
    
    private const val WEATHER_REFRESH_WORK_NAME = "weather_refresh_work"
    private const val REFRESH_INTERVAL_MINUTES = 15L
    
    /**
     * Schedules a periodic weather refresh to happen every 15 minutes.
     * Will replace any existing work with the same name.
     */
    fun schedulePeriodicWeatherRefresh(context: Context) {
        // Determine refresh interval based on battery level
        val batteryStatus: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) level / scale.toFloat() else 1f
        val intervalMinutes = if (batteryPct < 0.75f) 20L else REFRESH_INTERVAL_MINUTES

        // Define constraints - preferably with network connectivity
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        // Create the periodic work request
        val weatherRefreshRequest = PeriodicWorkRequestBuilder<WeatherRefreshWorker>(
            intervalMinutes, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()
        
        // Enqueue the work request
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WEATHER_REFRESH_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE, // Replace existing work if it exists
            weatherRefreshRequest
        )
    }
    
    /**
     * Cancels any ongoing weather refresh work
     */
    fun cancelWeatherRefreshWork(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WEATHER_REFRESH_WORK_NAME)
    }
}