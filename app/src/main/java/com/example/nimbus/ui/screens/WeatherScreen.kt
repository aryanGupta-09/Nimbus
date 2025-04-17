package com.example.nimbus.ui.screens

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.nimbus.data.model.WeatherResponse
import com.example.nimbus.data.repository.WeatherRepository
import com.example.nimbus.ui.components.LoadingScreen
import com.example.nimbus.ui.components.WeatherContent
import com.example.nimbus.ui.components.WeatherErrorScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WeatherScreen() {
    val context = LocalContext.current
    val repository = remember { WeatherRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var weatherState by remember { mutableStateOf<WeatherScreenState>(WeatherScreenState.Loading) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    LaunchedEffect(key1 = true) {
        fetchWeatherData(repository, weatherState) { newState ->
            weatherState = newState
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            coroutineScope.launch {
                isRefreshing = true
                fetchWeatherData(repository, weatherState) { newState ->
                    weatherState = newState
                    isRefreshing = false
                }
            }
        }
    )
    
    when (val state = weatherState) {
        is WeatherScreenState.Loading -> LoadingScreen()
        is WeatherScreenState.Success -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pullRefresh(pullRefreshState)
            ) {
                WeatherContent(weatherData = state.data)
                PullRefreshIndicator(
                    refreshing = isRefreshing,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
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