package com.example.nimbus.ui.screens

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.remember
import com.example.nimbus.data.model.WeatherResponse
import com.example.nimbus.data.repository.WeatherRepository
import com.example.nimbus.ui.components.LoadingScreen
import com.example.nimbus.ui.components.WeatherContent
import com.example.nimbus.ui.components.WeatherErrorScreen

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WeatherScreen(
    onNavigateToLocations: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { WeatherRepository(context) }
    val viewModel: WeatherViewModel = viewModel()

    // Observe UI state from ViewModel
    val weatherState by viewModel.weatherState.collectAsState()
    val historicalWeatherState by viewModel.historicalWeatherState.collectAsState()
    
    val pullRefreshState = rememberPullRefreshState(
        refreshing = weatherState is WeatherScreenState.Loading,
        onRefresh = { 
            viewModel.fetchWeather()
            viewModel.fetchHistoricalWeather()
        }
    )

    // Get currently selected location for header display
    val selectedLocationId by repository.selectedLocationId.collectAsState(initial = "")
    val savedLocations by repository.savedLocations.collectAsState(initial = emptyList())
    val selectedLocation = savedLocations.find { it.id == selectedLocationId }
    
    // Create a full location display string that includes the country
    val fullLocationDisplay = when {
        selectedLocation != null -> {
            if (weatherState is WeatherScreenState.Success) {
                // When we have weather data, use the country from the API response
                val weatherData = (weatherState as WeatherScreenState.Success).data
                "${selectedLocation.name}, ${weatherData.location.country}"
            } else {
                // If we don't have weather data yet, just show the name
                selectedLocation.name
            }
        }
        else -> null
    }
    
    // Fetch historical weather data when location changes
    LaunchedEffect(selectedLocationId) {
        viewModel.fetchHistoricalWeather()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToLocations) {
                Icon(Icons.Default.LocationOn, contentDescription = "Manage Locations")
            }
        }
    ) { paddingValues ->
        when (val state = weatherState) {
            is WeatherScreenState.Loading -> LoadingScreen()
            is WeatherScreenState.Success -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .pullRefresh(pullRefreshState)
                ) {
                    WeatherContent(
                        weatherData = state.data,
                        locationName = selectedLocation?.name,
                        fullLocationDisplay = fullLocationDisplay,
                        historicalWeatherState = historicalWeatherState,
                        onRetryHistorical = { viewModel.fetchHistoricalWeather() }
                    )
                    PullRefreshIndicator(
                        refreshing = weatherState is WeatherScreenState.Loading,
                        state = pullRefreshState,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }
            is WeatherScreenState.Error -> WeatherErrorScreen(
                message = state.message,
                onRetry = { viewModel.fetchWeather() }
            )
        }
    }
}

sealed class WeatherScreenState {
    data object Loading : WeatherScreenState()
    data class Success(val data: WeatherResponse) : WeatherScreenState()
    data class Error(val message: String) : WeatherScreenState()
}