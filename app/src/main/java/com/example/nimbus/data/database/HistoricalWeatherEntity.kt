package com.example.nimbus.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.nimbus.data.model.WeatherResponse
import java.util.UUID

/**
 * Entity class for storing historical weather data in the Room database
 * Using a unique index on date+locationQuery to prevent duplicate entries
 */
@Entity(
    tableName = "historical_weather",
    indices = [Index(value = ["date", "locationQuery"], unique = true)]
)
data class HistoricalWeatherEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val date: String, // Format: yyyy-MM-dd
    val locationQuery: String, // The query used to fetch this data (city name or lat,lon)
    val timestamp: Long = System.currentTimeMillis(), // When this data was fetched
    val weatherData: WeatherResponse
)