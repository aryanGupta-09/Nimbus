package com.example.nimbus

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.nimbus.ui.screens.LocationsScreen
import com.example.nimbus.ui.screens.SkyAnalysisScreen
import com.example.nimbus.ui.screens.WeatherScreen
import com.example.nimbus.ui.theme.NimbusTheme
import kotlinx.coroutines.launch
import com.example.nimbus.data.model.local.SavedLocation
import com.example.nimbus.data.repository.WeatherRepository
import com.example.nimbus.data.worker.WorkManagerHelper
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.listSaver

class MainActivity : ComponentActivity() {
    
    // Define screen routes
    sealed class Screen {
        data object Weather : Screen()
        data object Locations : Screen()
        data object SkyAnalysis : Screen()
        
        companion object {
            // Custom Saver for Screen class
            val Saver: Saver<Screen, String> = Saver(
                save = { screen ->
                    when (screen) {
                        is Weather -> "Weather"
                        is Locations -> "Locations" 
                        is SkyAnalysis -> "SkyAnalysis"
                    }
                },
                restore = { value ->
                    when (value) {
                        "Weather" -> Weather
                        "Locations" -> Locations
                        "SkyAnalysis" -> SkyAnalysis
                        else -> Weather // Default fallback
                    }
                }
            )
        }
    }
    
    // Weather update broadcast receiver
    private val weatherUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.example.nimbus.WEATHER_UPDATED") {
                Log.d("MainActivity", "Received weather update broadcast")
                // Trigger UI refresh via ViewModel
                // This will notify any active ViewModels that they need to refresh their data
                val localBroadcastIntent = Intent("com.example.nimbus.INTERNAL_WEATHER_REFRESH")
                sendBroadcast(localBroadcastIntent, Manifest.permission.WAKE_LOCK)
            }
        }
    }
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, app can proceed
            setupWeatherApp()
        } else {
            // Permission denied
            Toast.makeText(
                this,
                "Location permission is required for getting local weather data",
                Toast.LENGTH_LONG
            ).show()
            // We'll still show the app with a default location
            setupWeatherApp()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Check for location permission
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission already granted
            setupWeatherApp()
        } else {
            // Request permission
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Register broadcast receiver for weather updates with proper flags
        try {
            registerReceiver(
                weatherUpdateReceiver,
                IntentFilter("com.example.nimbus.WEATHER_UPDATED"),
                Context.RECEIVER_NOT_EXPORTED
            )
        } catch (e: Exception) {
            Log.e("MainActivity", "Error registering receiver", e)
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Unregister broadcast receiver
        try {
            unregisterReceiver(weatherUpdateReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered, ignore
        }
    }
    
    private fun setupWeatherApp() {
        // Set current location as the selected location whenever app starts
        val repository = WeatherRepository(this)
        lifecycleScope.launch {
            repository.setSelectedLocation(SavedLocation.currentLocation().id)
        }
        
        // Initialize background weather refresh
        WorkManagerHelper.schedulePeriodicWeatherRefresh(this)
        
        setContent {
            var darkTheme by rememberSaveable { mutableStateOf(true) }
            NimbusTheme(darkTheme = darkTheme, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by rememberSaveable(stateSaver = Screen.Saver) { mutableStateOf<Screen>(Screen.Weather) }
                    when (currentScreen) {
                        is Screen.Weather -> {
                            WeatherScreen(
                                onNavigateToLocations = {
                                    currentScreen = Screen.Locations
                                },
                                onNavigateToSkyAnalysis = {
                                    currentScreen = Screen.SkyAnalysis
                                },
                                isDarkTheme = darkTheme,
                                onToggleTheme = { darkTheme = !darkTheme }
                            )
                        }
                        is Screen.Locations -> {
                            LocationsScreen(
                                onNavigateBack = {
                                    currentScreen = Screen.Weather
                                }
                            )
                        }
                        is Screen.SkyAnalysis -> {
                            SkyAnalysisScreen(
                                onNavigateBack = {
                                    currentScreen = Screen.Weather
                                },
                                isDarkTheme = darkTheme
                            )
                        }
                    }
                }
            }
        }
    }
}