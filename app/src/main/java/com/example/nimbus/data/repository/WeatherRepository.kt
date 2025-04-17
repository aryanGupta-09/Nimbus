package com.example.nimbus.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import com.example.nimbus.data.api.NetworkModule
import com.example.nimbus.data.model.WeatherResponse
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class WeatherRepository(private val context: Context) {
    private val weatherApiService = NetworkModule.weatherApiService
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }
    
    suspend fun getWeatherForecast(location: String? = null): Result<WeatherResponse> {
        return try {
            val query = location ?: getCurrentLocationString()
            val response = weatherApiService.getWeatherForecast(query = query)
            Result.success(response)
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Error fetching weather data", e)
            Result.failure(e)
        }
    }
    
    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocationString(): String = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { cont ->
            val cancellationToken = CancellationTokenSource()
            
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellationToken.token)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val locationString = "${location.latitude},${location.longitude}"
                        cont.resume(locationString)
                    } else {
                        // If location is null, use a default location
                        cont.resume("London")
                    }
                }
                .addOnFailureListener { e ->
                    if (cont.isActive) {
                        // Provide a default location on failure
                        Log.e("WeatherRepository", "Error getting location", e)
                        cont.resume("London")
                    }
                }
                
            cont.invokeOnCancellation {
                cancellationToken.cancel()
            }
        }
    }
}