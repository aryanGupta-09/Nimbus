package com.example.nimbus.data.model.local

import com.example.nimbus.data.model.WeatherResponse

/**
 * Represents cached weather data
 */
data class CachedWeatherData(
    val locationQuery: String,
    val weatherData: WeatherResponse,
    val timestamp: Long = System.currentTimeMillis(),
    val fromCache: Boolean = false
)