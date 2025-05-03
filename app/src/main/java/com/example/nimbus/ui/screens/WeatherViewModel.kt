package com.example.nimbus.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nimbus.data.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class WeatherViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WeatherRepository(application)

    private val _weatherState = MutableStateFlow<WeatherScreenState>(WeatherScreenState.Loading)
    val weatherState: StateFlow<WeatherScreenState> = _weatherState

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
}