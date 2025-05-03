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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
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
    
    // Historical weather data
    private val _historicalWeatherData = MutableStateFlow<List<WeatherResponse>>(emptyList())
    val historicalWeatherData: StateFlow<List<WeatherResponse>> = _historicalWeatherData
    
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
    
    /**
     * Get historical weather data for the past 7 days
     */
    suspend fun getHistoricalWeather(days: Int = 7): Result<List<WeatherResponse>> {
        return try {
            val locations = savedLocations.first()
            val selectedId = selectedLocationId.first()
            val selectedLocation = locations.find { it.id == selectedId }
            
            val query = if (selectedLocation?.isCurrent == true) {
                getCurrentLocationString()
            } else {
                selectedLocation?.query ?: getCurrentLocationString()
            }
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val calendar = Calendar.getInstance()
            
            val historicalData = mutableListOf<WeatherResponse>()
            
            // Fetch data for each of the past days, but with a delay to avoid rate limiting
            for (i in 1..days) {
                try {
                    // Start from yesterday (skip today as we already have current data)
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                    val date = dateFormat.format(calendar.time)
                    
                    val response = weatherApiService.getHistoricalWeather(
                        query = query,
                        date = date
                    )
                    
                    historicalData.add(response)
                    
                    // Add a small delay between requests to avoid hitting rate limits
                    if (i < days) {
                        delay(500)  // 500ms delay between requests
                    }
                } catch (e: Exception) {
                    // If a specific day fails, log it but continue with other days
                    Log.e("WeatherRepository", "Error fetching historical data for a specific day", e)
                }
            }
            
            // Reset calendar
            calendar.time = Date()
            
            _historicalWeatherData.value = historicalData
            Result.success(historicalData)
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Error fetching historical weather data", e)
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