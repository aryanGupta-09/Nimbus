package com.example.nimbus.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.nimbus.data.model.WeatherResponse
import com.example.nimbus.data.repository.WeatherRepository
import com.example.nimbus.ui.components.LoadingScreen
import com.example.nimbus.ui.components.WeatherContent
import com.example.nimbus.ui.components.WeatherErrorScreen
import kotlinx.coroutines.launch

@Composable
fun WeatherScreen() {
    val context = LocalContext.current
    val repository = remember { WeatherRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var weatherState by remember { mutableStateOf<WeatherScreenState>(WeatherScreenState.Loading) }
    
    LaunchedEffect(key1 = true) {
        fetchWeatherData(repository, weatherState) { newState ->
            weatherState = newState
        }
    }
    
    when (val state = weatherState) {
        is WeatherScreenState.Loading -> LoadingScreen()
        is WeatherScreenState.Success -> WeatherContent(weatherData = state.data)
        is WeatherScreenState.Error -> WeatherErrorScreen(
            message = state.message,
            onRetry = {
                weatherState = WeatherScreenState.Loading
                coroutineScope.launch {
                    fetchWeatherData(repository, weatherState) { newState ->
                        weatherState = newState
                    }
                }
            }
        )
    }
}

private suspend fun fetchWeatherData(
    repository: WeatherRepository,
    currentState: WeatherScreenState,
    updateState: (WeatherScreenState) -> Unit
) {
    repository.getWeatherForecast()
        .fold(
            onSuccess = { weatherData ->
                updateState(WeatherScreenState.Success(weatherData))
            },
            onFailure = { exception ->
                updateState(WeatherScreenState.Error(exception.message ?: "Unknown error occurred"))
            }
        )
}

sealed class WeatherScreenState {
    data object Loading : WeatherScreenState()
    data class Success(val data: WeatherResponse) : WeatherScreenState()
    data class Error(val message: String) : WeatherScreenState()
}