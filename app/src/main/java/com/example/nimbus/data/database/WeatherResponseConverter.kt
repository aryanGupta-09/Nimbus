package com.example.nimbus.data.database

import androidx.room.TypeConverter
import com.example.nimbus.data.model.WeatherResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Type converters for Room to store complex objects as JSON strings
 */
class WeatherResponseConverter {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    
    private val weatherResponseAdapter = moshi.adapter(WeatherResponse::class.java)
    
    @TypeConverter
    fun fromWeatherResponse(weatherResponse: WeatherResponse): String {
        return weatherResponseAdapter.toJson(weatherResponse)
    }
    
    @TypeConverter
    fun toWeatherResponse(json: String): WeatherResponse {
        return weatherResponseAdapter.fromJson(json) ?: throw IllegalArgumentException("Invalid JSON")
    }
}