package com.example.nimbus.data.api

import com.example.nimbus.BuildConfig
import com.example.nimbus.data.model.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {
    @GET("v1/forecast.json")
    suspend fun getWeatherForecast(
        @Query("key") apiKey: String = BuildConfig.WEATHER_API_KEY,
        @Query("q") query: String,
        @Query("days") days: Int = 7,
        @Query("aqi") airQuality: String = "yes",
        @Query("alerts") alerts: String = "no"
    ): WeatherResponse
    
    @GET("v1/history.json")
    suspend fun getHistoricalWeather(
        @Query("key") apiKey: String = BuildConfig.WEATHER_API_KEY,
        @Query("q") query: String,
        @Query("dt") date: String,
        @Query("end_dt") endDate: String? = null,
        @Query("aqi") airQuality: String = "yes"
    ): WeatherResponse
}