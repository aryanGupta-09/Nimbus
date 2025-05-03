package com.example.nimbus.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nimbus.data.model.WeatherResponse
import com.example.nimbus.data.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import com.example.nimbus.data.model.local.OfflineDataInfo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class WeatherViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WeatherRepository(application)

    private val _weatherState = MutableStateFlow<WeatherScreenState>(WeatherScreenState.Loading)
    val weatherState: StateFlow<WeatherScreenState> = _weatherState
    
    private val _historicalWeatherState = MutableStateFlow<HistoricalWeatherState>(HistoricalWeatherState.Loading)
    val historicalWeatherState: StateFlow<HistoricalWeatherState> = _historicalWeatherState

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