package com.example.nimbus

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
import com.example.nimbus.ui.screens.LocationsScreen
import com.example.nimbus.ui.screens.WeatherScreen
import com.example.nimbus.ui.theme.NimbusTheme

class MainActivity : ComponentActivity() {
    
    // Define screen routes
    sealed class Screen {
        data object Weather : Screen()
        data object Locations : Screen()
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
    
    private fun setupWeatherApp() {
        setContent {
            NimbusTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Setup navigation
                    var currentScreen by remember { mutableStateOf<Screen>(Screen.Weather) }
                    
                    when (currentScreen) {
                        is Screen.Weather -> {
                            WeatherScreen(
                                onNavigateToLocations = {
                                    currentScreen = Screen.Locations
                                }
                            )
                        }
                        is Screen.Locations -> {
                            LocationsScreen(
                                onNavigateBack = {
                                    currentScreen = Screen.Weather
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}