package com.example.nimbus.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nimbus.data.model.WeatherResponse
import com.example.nimbus.data.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import com.example.nimbus.data.model.local.OfflineDataInfo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import android.util.Log
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.example.nimbus.data.worker.BackgroundRefreshManager

class WeatherViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WeatherRepository(application)
    private val context = application

    private val _weatherState = MutableStateFlow<WeatherScreenState>(WeatherScreenState.Loading)
    val weatherState: StateFlow<WeatherScreenState> = _weatherState
    
    private val _historicalWeatherState = MutableStateFlow<HistoricalWeatherState>(HistoricalWeatherState.Loading)
    val historicalWeatherState: StateFlow<HistoricalWeatherState> = _historicalWeatherState

    // Broadcast receiver for internal weather refresh events
    private val weatherUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.example.nimbus.INTERNAL_WEATHER_REFRESH") {
                Log.d("WeatherViewModel", "Received internal refresh broadcast, updating UI")
                fetchWeather()
                fetchHistoricalWeather()
            }
        }
    }

    // Expose offline data information
    val offlineDataInfo = repository.offlineDataInfo.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    init {
        viewModelScope.launch {
            repository.selectedLocationId.collect {
                fetchWeather()
            }
        }
        
        // Listen for background refresh events from BackgroundRefreshManager
        viewModelScope.launch {
            BackgroundRefreshManager.refreshTimestamp.collect { timestamp -> 
                if (timestamp > 0) {
                    Log.d("WeatherViewModel", "Detected background refresh via manager at $timestamp, updating UI")
                    // Refresh the weather data
                    fetchWeather()
                    // Also refresh historical data
                    fetchHistoricalWeather()
                }
            }
        }
        
        // Original background refresh event listener from repository
        viewModelScope.launch {
            repository.backgroundRefreshEvent.collect { timestamp -> 
                if (timestamp > 0) {
                    Log.d("WeatherViewModel", "Detected background refresh at $timestamp, updating UI")
                    // Refresh the weather data
                    fetchWeather()
                    // Also refresh historical data
                    fetchHistoricalWeather()
                }
            }
        }

        // Register the broadcast receiver with proper flags
        try {
            val filter = IntentFilter("com.example.nimbus.INTERNAL_WEATHER_REFRESH")
            context.registerReceiver(weatherUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } catch (e: Exception) {
            Log.e("WeatherViewModel", "Error registering receiver", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Unregister the broadcast receiver
        context.unregisterReceiver(weatherUpdateReceiver)
    }

    fun fetchWeather() {
        viewModelScope.launch {
            _weatherState.value = WeatherScreenState.Loading
            repository.getSelectedLocationWeather()
                .fold(
                    onSuccess = { data -> _weatherState.value = WeatherScreenState.Success(data) },
                    onFailure = { e -> _weatherState.value = WeatherScreenState.Error(e.message ?: "Unknown error") }
                )
        }
    }
    
    fun fetchHistoricalWeather(days: Int = 7) {
        viewModelScope.launch {
            _historicalWeatherState.value = HistoricalWeatherState.Loading
            repository.getHistoricalWeather(days)
                .fold(
                    onSuccess = { data -> _historicalWeatherState.value = HistoricalWeatherState.Success(data) },
                    onFailure = { e -> _historicalWeatherState.value = HistoricalWeatherState.Error(e.message ?: "Unknown error") }
                )
        }
    }
}

sealed class HistoricalWeatherState {
    data object Loading : HistoricalWeatherState()
    data class Success(val data: List<WeatherResponse>) : HistoricalWeatherState()
    data class Error(val message: String) : HistoricalWeatherState()
}