package com.example.nimbus.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import com.example.nimbus.data.api.NetworkModule
import com.example.nimbus.data.location.LocationManager
import com.example.nimbus.data.model.WeatherResponse
import com.example.nimbus.data.model.local.SavedLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class WeatherRepository(private val context: Context) {
    private val weatherApiService = NetworkModule.weatherApiService
    private val locationManager = LocationManager(context)
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }
    
    // Get saved locations
    val savedLocations = locationManager.savedLocations
    
    // Get currently selected location ID
    val selectedLocationId = locationManager.selectedLocationId
    
    // Add a new location
    suspend fun addLocation(location: SavedLocation) {
        locationManager.addLocation(location)
    }
    
    // Remove a location
    suspend fun removeLocation(locationId: String) {
        locationManager.removeLocation(locationId)
    }
    
    // Set the selected location
    suspend fun setSelectedLocation(locationId: String) {
        locationManager.setSelectedLocation(locationId)
    }
    
    // Get weather for the selected location
    suspend fun getSelectedLocationWeather(): Result<WeatherResponse> {
        val locations = savedLocations.first()
        val selectedId = selectedLocationId.first()
        val selectedLocation = locations.find { it.id == selectedId }
        
        return getWeatherForecast(selectedLocation)
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
    
    suspend fun getWeatherForecast(savedLocation: SavedLocation? = null): Result<WeatherResponse> {
        return try {
            val query = if (savedLocation?.isCurrent == true) {
                getCurrentLocationString()
            } else {
                savedLocation?.query ?: getCurrentLocationString()
            }
            
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