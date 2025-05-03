package com.example.nimbus.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import com.example.nimbus.data.api.NetworkModule
import com.example.nimbus.data.location.LocationManager
import com.example.nimbus.data.model.Forecast
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
     * Get historical weather data for the past 7 days using a single API call
     * Note: Using the date range (end_dt) feature requires a Pro plan or higher
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
            
            // Calculate end date (today or yesterday)
            val endDate = dateFormat.format(calendar.time)
            
            // Calculate start date (7 days ago)
            calendar.add(Calendar.DAY_OF_YEAR, -(days))
            val startDate = dateFormat.format(calendar.time)
            
            try {
                // Make a single API call with date range
                val response = weatherApiService.getHistoricalWeather(
                    query = query,
                    date = startDate,
                    endDate = endDate
                )
                
                // Since we might get more than 7 days of data in a single response,
                // we'll process it and create a list of daily responses
                val dailyResponses = mutableListOf<WeatherResponse>()
                
                // The API returns a single response with multiple forecast days
                // We need to extract each day into separate WeatherResponse objects
                if (response.forecast.forecastday.isNotEmpty()) {
                    for (forecastDay in response.forecast.forecastday) {
                        // Create a new forecast object with only this day
                        val singleDayForecast = Forecast(listOf(forecastDay))
                        
                        // Create a new WeatherResponse with the single day forecast
                        val singleDayResponse = WeatherResponse(
                            location = response.location,
                            current = null, // Historical data doesn't have current
                            forecast = singleDayForecast
                        )
                        
                        dailyResponses.add(singleDayResponse)
                    }
                }
                
                // Update the state
                _historicalWeatherData.value = dailyResponses
                Result.success(dailyResponses)
            } catch (e: Exception) {
                // If the single API call fails (e.g., not on Pro plan), fall back to multiple calls
                Log.w("WeatherRepository", "Single API call for historical data failed, falling back to multiple calls", e)
                fetchHistoricalWeatherWithMultipleCalls(query, days)
            }
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Error fetching historical weather data", e)
            Result.failure(e)
        }
    }
    
    /**
     * Fallback method to fetch historical data using multiple API calls
     * This is used if the single API call fails (e.g., not on Pro plan)
     */
    private suspend fun fetchHistoricalWeatherWithMultipleCalls(query: String, days: Int): Result<List<WeatherResponse>> {
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
        return Result.success(historicalData)
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