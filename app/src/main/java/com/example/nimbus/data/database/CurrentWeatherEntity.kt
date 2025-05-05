package com.example.nimbus.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.nimbus.data.model.WeatherResponse
import java.util.UUID

/**
 * Entity class for storing current weather data in the Room database for offline access
 * Using a unique index on locationQuery to ensure we have only one record per location
 */
@Entity(
    tableName = "current_weather",
    indices = [Index(value = ["locationQuery"], unique = true)]
)
data class CurrentWeatherEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val locationQuery: String, // The query used to fetch this data (city name or lat,lon)
    val locationName: String, // Human-readable location name
    val timestamp: Long = System.currentTimeMillis(), // When this data was fetched
    val weatherData: WeatherResponse
)