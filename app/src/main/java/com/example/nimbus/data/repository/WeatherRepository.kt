package com.example.nimbus.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import com.example.nimbus.data.api.NetworkModule
import com.example.nimbus.data.database.HistoricalWeatherEntity
import com.example.nimbus.data.database.WeatherDatabase
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
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class WeatherRepository(private val context: Context) {
    private val weatherApiService = NetworkModule.weatherApiService
    private val locationManager = LocationManager(context)
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }
    
    // Initialize Room database
    private val database = WeatherDatabase.getDatabase(context)
    private val historicalWeatherDao = database.historicalWeatherDao()
    
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
     * Get historical weather data for the past days
     * First checks local database for cached data and only fetches missing days from API
     * Excludes the current day as it's not considered historical data
     */
    suspend fun getHistoricalWeather(days: Int = 7): Result<List<WeatherResponse>> {
        try {
            // Clean up old cached data (older than 30 days)
            cleanupOldData()
            
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
            
            // Calculate end date (yesterday, not today)
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val endDate = dateFormat.format(calendar.time)
            
            // Calculate start date ((days+1) days ago to maintain the requested number of days)
            calendar.add(Calendar.DAY_OF_YEAR, -(days-1))
            val startDate = dateFormat.format(calendar.time)
            
            // Check which dates we already have in the database
            val cachedData = historicalWeatherDao.getHistoricalWeatherInDateRange(
                locationQuery = query,
                startDate = startDate,
                endDate = endDate
            )
            
            Log.d("WeatherRepository", "Found ${cachedData.size} cached historical records")
            
            // If we have all the requested days in cache, return them
            if (cachedData.size >= days) {
                val historicalData = cachedData.map { it.weatherData }
                _historicalWeatherData.value = historicalData
                return Result.success(historicalData)
            }
            
            // Otherwise, identify the missing dates and fetch only those
            val cachedDates = cachedData.map { it.date }.toSet()
            val allDates = getDatesInRange(startDate, endDate)
            val missingDates = allDates.filter { it !in cachedDates }
            
            Log.d("WeatherRepository", "Need to fetch ${missingDates.size} missing dates")
            
            // First try with a date range request (if supported by API plan)
            val historicalData = if (missingDates.isNotEmpty()) {
                try {
                    fetchMissingDatesWithRangeCall(query, missingDates)
                } catch (e: Exception) {
                    Log.w("WeatherRepository", "Range call failed, falling back to individual date calls", e)
                    fetchMissingDatesWithIndividualCalls(query, missingDates)
                }
            } else {
                emptyList()
            }
            
            // Combine cached data with newly fetched data
            val allData = (cachedData.map { it.weatherData } + historicalData).sortedByDescending { 
                it.forecast.forecastday.firstOrNull()?.date ?: ""
            }
            
            _historicalWeatherData.value = allData
            return Result.success(allData)
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Error fetching historical weather data", e)
            return Result.failure(e)
        }
    }
    
    /**
     * Fetch missing historical dates using a date range API call
     */
    private suspend fun fetchMissingDatesWithRangeCall(
        query: String,
        missingDates: List<String>
    ): List<WeatherResponse> {
        if (missingDates.isEmpty()) return emptyList()
        
        // Sort dates to ensure we have the earliest first and latest last
        val sortedDates = missingDates.sorted()
        val startDate = sortedDates.first()
        val endDate = sortedDates.last()
        
        val response = weatherApiService.getHistoricalWeather(
            query = query,
            date = startDate,
            endDate = endDate
        )
        
        val dailyResponses = mutableListOf<WeatherResponse>()
        val processedDates = mutableSetOf<String>() // Track processed dates to avoid duplicates
        
        // Extract each day into separate WeatherResponse objects
        if (response.forecast.forecastday.isNotEmpty()) {
            for (forecastDay in response.forecast.forecastday) {
                // Only include the days that were missing and haven't been processed yet
                if (forecastDay.date in missingDates && !processedDates.contains(forecastDay.date)) {
                    val singleDayForecast = Forecast(listOf(forecastDay))
                    val singleDayResponse = WeatherResponse(
                        location = response.location,
                        current = null,
                        forecast = singleDayForecast
                    )
                    
                    dailyResponses.add(singleDayResponse)
                    processedDates.add(forecastDay.date) // Mark this date as processed
                    
                    // Save to the database
                    historicalWeatherDao.insertHistoricalWeather(
                        HistoricalWeatherEntity(
                            date = forecastDay.date,
                            locationQuery = query,
                            weatherData = singleDayResponse
                        )
                    )
                }
            }
        }
        
        return dailyResponses
    }
    
    /**
     * Fetch missing historical dates using individual API calls
     */
    private suspend fun fetchMissingDatesWithIndividualCalls(
        query: String,
        missingDates: List<String>
    ): List<WeatherResponse> {
        val historicalData = mutableListOf<WeatherResponse>()
        val processedDates = mutableSetOf<String>() // Track processed dates to avoid duplicates
        
        for (date in missingDates) {
            // Skip dates we've already processed
            if (date in processedDates) continue
            
            try {
                val response = weatherApiService.getHistoricalWeather(
                    query = query,
                    date = date
                )
                
                historicalData.add(response)
                processedDates.add(date) // Mark this date as processed
                
                // Save to the database
                historicalWeatherDao.insertHistoricalWeather(
                    HistoricalWeatherEntity(
                        date = date,
                        locationQuery = query,
                        weatherData = response
                    )
                )
                
                // Add a small delay between requests to avoid hitting rate limits
                if (date != missingDates.last()) {
                    delay(500)  // 500ms delay between requests
                }
            } catch (e: Exception) {
                // If a specific day fails, log it but continue with other days
                Log.e("WeatherRepository", "Error fetching historical data for $date", e)
            }
        }
        
        return historicalData
    }
    
    /**
     * Generate a list of all dates in the given range
     */
    private fun getDatesInRange(startDate: String, endDate: String): List<String> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val result = mutableListOf<String>()
        
        val startCalendar = Calendar.getInstance().apply {
            time = dateFormat.parse(startDate) ?: Date()
        }
        
        val endCalendar = Calendar.getInstance().apply {
            time = dateFormat.parse(endDate) ?: Date()
        }
        
        while (!startCalendar.after(endCalendar)) {
            result.add(dateFormat.format(startCalendar.time))
            startCalendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        return result
    }
    
    /**
     * Clean up historical data older than 30 days
     * Safely handles the case when the database hasn't been fully created yet
     */
    private suspend fun cleanupOldData() {
        try {
            // First check if the table exists to avoid SQLite errors on first run
            if (historicalWeatherDao.doesTableExist()) {
                val thirtyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
                historicalWeatherDao.deleteOldData(thirtyDaysAgo)
            } else {
                Log.d("WeatherRepository", "Skipping cleanup - historical_weather table doesn't exist yet")
            }
        } catch (e: Exception) {
            // Catch any SQLite exceptions that might occur
            Log.w("WeatherRepository", "Error during database cleanup, ignoring", e)
            // Continue execution - this is just a maintenance operation that can be skipped if there's an issue
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