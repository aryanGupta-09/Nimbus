package com.example.nimbus.ui.screens

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import com.example.nimbus.data.model.WeatherResponse
import com.example.nimbus.data.repository.WeatherRepository
import com.example.nimbus.ui.components.LoadingScreen
import com.example.nimbus.ui.components.WeatherContent
import com.example.nimbus.ui.components.WeatherErrorScreen
import com.example.nimbus.ui.components.OfflineBanner
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.IconButton
import coil.compose.AsyncImage
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import com.example.nimbus.R

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    onNavigateToLocations: () -> Unit = {},
    onNavigateToSkyAnalysis: () -> Unit = {},
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    // State to track temperature unit
    var isCelsius by rememberSaveable { mutableStateOf(true) }
    val context = LocalContext.current
    val repository = remember { WeatherRepository(context) }
    val viewModel: WeatherViewModel = viewModel()

    // Observe UI state from ViewModel
    val weatherState by viewModel.weatherState.collectAsState()
    val historicalWeatherState by viewModel.historicalWeatherState.collectAsState()
    // Observe offline data info
    val offlineInfo by viewModel.offlineDataInfo.collectAsState()
    
    // Observe device sensor data
    val sensorData by viewModel.sensorData.collectAsState()
    val hasSensors by viewModel.hasSensors.collectAsState()
    
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
    
    // Create a full location display string using API fields (name, region, country)
    val fullLocationDisplay = when (val state = weatherState) {
        is WeatherScreenState.Success -> {
            val loc = state.data.location
            "${loc.name}, ${loc.region}, ${loc.country}"
        }
        else -> null
    }
    
    // Fetch historical weather data when location changes
    LaunchedEffect(selectedLocationId) {
        viewModel.fetchHistoricalWeather()
    }

    Scaffold(
        // Transparent top bar with unit switch
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    Button(
                        onClick = onToggleTheme,
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isDarkTheme) Color(0xFFD0C5E5) else Color(0xFF525254)
                        )
                    ) {
                        Image(
                            painter = painterResource(id = if (isDarkTheme) R.drawable.darkness_icon else R.drawable.brightness_icon),
                            contentDescription = if (isDarkTheme) "Switch to light mode" else "Switch to dark mode",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                title = {
                    Button(
                        onClick = onNavigateToSkyAnalysis,
                        modifier = Modifier.size(40.dp),  // increased button size
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isDarkTheme) Color(0xFFD0C5E5) else Color(0xFF525254)
                        )
                    ) {
                        AsyncImage(
                            model = if (isDarkTheme) "https://cdn-icons-png.flaticon.com/512/8750/8750768.png" else "https://cdn-icons-png.flaticon.com/512/8750/8750710.png",
                            contentDescription = "AI Mode",
                            modifier = Modifier.size(32.dp)  // increased icon size
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (!isDarkTheme) Color(0xFFE6E1E8) else MaterialTheme.colorScheme.surface
                ),
                actions = {
                    Text(text = if (isCelsius) "°C" else "°F", modifier = Modifier.padding(end = 8.dp))
                    Switch(
                        checked = !isCelsius,
                        onCheckedChange = { isCelsius = !it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            uncheckedThumbColor = MaterialTheme.colorScheme.surface,
                            uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToLocations) {
                Icon(Icons.Default.LocationOn, contentDescription = "Manage Locations")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Show offline banner when displaying cached data
            if (offlineInfo != null) {
                OfflineBanner(
                    timestamp = offlineInfo!!.timestamp,
                    modifier = Modifier.padding(top = paddingValues.calculateTopPadding())
                )
            }
            
            when (val state = weatherState) {
                is WeatherScreenState.Loading -> LoadingScreen()
                is WeatherScreenState.Success -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                top = if (offlineInfo != null) {
                                    paddingValues.calculateTopPadding() + 40.dp // Add extra space for the banner
                                } else {
                                    paddingValues.calculateTopPadding()
                                }
                            )
                            .pullRefresh(pullRefreshState)
                    ) {
                        WeatherContent(
                            weatherData = state.data,
                            locationName = if (selectedLocation?.isCurrent == true) "Current location" else selectedLocation?.name,
                            fullLocationDisplay = if (selectedLocation?.isCurrent == true) "Current location" else fullLocationDisplay,
                            historicalWeatherState = historicalWeatherState,
                            onRetryHistorical = { viewModel.fetchHistoricalWeather() },
                            isCelsius = isCelsius,
                            devicePressure = sensorData.pressure,
                            hasBarometer = hasSensors && sensorData.pressure != null,
                            sensorData = sensorData,
                            hasSensors = hasSensors,
                            isCurrentLocation = selectedLocation?.isCurrent ?: false
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
}

sealed class WeatherScreenState {
    data object Loading : WeatherScreenState()
    data class Success(val data: WeatherResponse) : WeatherScreenState()
    data class Error(val message: String) : WeatherScreenState()
}