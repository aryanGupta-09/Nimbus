package com.example.nimbus.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.nimbus.data.model.local.SavedLocation
import com.example.nimbus.data.repository.WeatherRepository
import kotlinx.coroutines.flow.first

/**
 * Worker class that handles background weather data refreshing.
 * This worker is scheduled to run every 15 minutes to refresh GPS location and weather data.
 */
class WeatherRefreshWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val repository = WeatherRepository(applicationContext)
    private val tag = "WeatherRefreshWorker"

    override suspend fun doWork(): Result {
        Log.d(tag, "Starting background GPS location and weather refresh")
        
        // Notify that a refresh has started
        BackgroundRefreshManager.notifyRefreshStarted()
        
        return try {
            // Get the selected location ID
            val selectedLocationId = repository.selectedLocationId.first()
            val locations = repository.savedLocations.first()
            val selectedLocation = locations.find { it.id == selectedLocationId }
            
            // Check if the selected location is the "Current Location"
            if (selectedLocation?.isCurrent == true) {
                // Force a fresh GPS location check and weather refresh
                val result = repository.refreshCurrentLocationWeather()
                
                if (result.isSuccess) {
                    Log.d(tag, "Background GPS and weather refresh successful")
                    
                    // Notify that refresh has completed successfully
                    BackgroundRefreshManager.notifyRefreshCompleted()
                    
                    Result.success()
                } else {
                    Log.w(tag, "Background GPS and weather refresh failed: ${result.exceptionOrNull()?.message}")
                    // Retry on failure, but only if it might be recoverable (like network issues)
                    if (result.exceptionOrNull()?.message?.contains("network", ignoreCase = true) == true) {
                        Result.retry()
                    } else {
                        Result.failure()
                    }
                }
            } else {
                // For non-current locations, just refresh the weather data
                val result = repository.getSelectedLocationWeather()
                
                if (result.isSuccess) {
                    Log.d(tag, "Background weather refresh successful for saved location")
                    
                    // Notify that refresh has completed successfully
                    BackgroundRefreshManager.notifyRefreshCompleted()
                    
                    Result.success()
                } else {
                    Log.w(tag, "Background weather refresh failed: ${result.exceptionOrNull()?.message}")
                    if (result.exceptionOrNull()?.message?.contains("network", ignoreCase = true) == true) {
                        Result.retry()
                    } else {
                        Result.failure()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error during background GPS and weather refresh", e)
            // Return retry to attempt the operation again later
            Result.retry()
        }
    }
}