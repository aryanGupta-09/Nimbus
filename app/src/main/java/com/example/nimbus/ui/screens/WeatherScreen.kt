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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.nimbus.data.model.WeatherResponse
import com.example.nimbus.data.model.local.SavedLocation
import com.example.nimbus.data.repository.WeatherRepository
import com.example.nimbus.ui.components.LoadingScreen
import com.example.nimbus.ui.components.WeatherContent
import com.example.nimbus.ui.components.WeatherErrorScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WeatherScreen(
    onNavigateToLocations: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { WeatherRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    
    // Custom Saver for WeatherScreenState
    val weatherStateSaver = remember {
        Saver<WeatherScreenState, String>(
            save = { state ->
                when (state) {
                    is WeatherScreenState.Loading -> "loading"
                    is WeatherScreenState.Error -> "error:${state.message}"
                    is WeatherScreenState.Success -> "success" // We'll refetch data instead of trying to save it
                }
            },
            restore = { savedValue ->
                when {
                    savedValue == "loading" -> WeatherScreenState.Loading
                    savedValue.startsWith("error:") -> {
                        val message = savedValue.removePrefix("error:")
                        WeatherScreenState.Error(message)
                    }
                    savedValue == "success" -> WeatherScreenState.Loading // Start with loading to refetch data
                    else -> WeatherScreenState.Loading
                }
            }
        )
    }
    
    // Using rememberSaveable with custom saver
    var weatherState by rememberSaveable(stateSaver = weatherStateSaver) { 
        mutableStateOf<WeatherScreenState>(WeatherScreenState.Loading) 
    }
    
    var isRefreshing by remember { mutableStateOf(false) }
    
    // Track last fetched location ID to avoid unnecessary fetches
    var lastFetchedLocationId by rememberSaveable { mutableStateOf<String?>(null) }
    
    // Get currently selected location
    val selectedLocationId by repository.selectedLocationId.collectAsState(initial = "")
    val savedLocations by repository.savedLocations.collectAsState(initial = emptyList())
    
    // Find the current selected location
    val selectedLocation = savedLocations.find { it.id == selectedLocationId }
    
    // Only fetch data when location changes or first load
    LaunchedEffect(selectedLocationId) {
        // Skip if we already fetched data for this location
        if (lastFetchedLocationId == selectedLocationId && weatherState is WeatherScreenState.Success) {
            return@LaunchedEffect
        }
        
        try {
            weatherState = WeatherScreenState.Loading
            repository.getSelectedLocationWeather()
                .fold(
                    onSuccess = { weatherData ->
                        weatherState = WeatherScreenState.Success(weatherData)
                        lastFetchedLocationId = selectedLocationId
                    },
                    onFailure = { exception ->
                        if (exception is CancellationException) throw exception
                        weatherState = WeatherScreenState.Error(exception.message ?: "Unknown error occurred")
                    }
                )
        } catch (e: CancellationException) {
            // Let cancellation propagate
            throw e
        } catch (e: Exception) {
            weatherState = WeatherScreenState.Error("Error fetching weather data: ${e.message}")
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            coroutineScope.launch {
                isRefreshing = true
                try {
                    repository.getSelectedLocationWeather()
                        .fold(
                            onSuccess = { weatherData ->
                                weatherState = WeatherScreenState.Success(weatherData)
                                lastFetchedLocationId = selectedLocationId
                            },
                            onFailure = { exception ->
                                if (exception is CancellationException) throw exception
                                weatherState = WeatherScreenState.Error(exception.message ?: "Unknown error occurred")
                            }
                        )
                } finally {
                    isRefreshing = false
                }
            }
        }
    )
    
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
                        locationName = selectedLocation?.name
                    )
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
                    coroutineScope.launch {
                        try {
                            weatherState = WeatherScreenState.Loading
                            repository.getSelectedLocationWeather()
                                .fold(
                                    onSuccess = { weatherData ->
                                        weatherState = WeatherScreenState.Success(weatherData)
                                        lastFetchedLocationId = selectedLocationId
                                    },
                                    onFailure = { exception ->
                                        if (exception is CancellationException) throw exception
                                        weatherState = WeatherScreenState.Error(exception.message ?: "Unknown error occurred")
                                    }
                                )
                        } catch (e: Exception) {
                            weatherState = WeatherScreenState.Error("Error fetching weather data: ${e.message}")
                        }
                    }
                }
            )
        }
    }
}

sealed class WeatherScreenState {
    data object Loading : WeatherScreenState()
    data class Success(val data: WeatherResponse) : WeatherScreenState()
    data class Error(val message: String) : WeatherScreenState()
}