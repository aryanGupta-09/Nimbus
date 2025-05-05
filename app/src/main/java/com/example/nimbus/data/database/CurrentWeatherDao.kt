package com.example.nimbus.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO interface for current weather data persistence
 */
@Dao
interface CurrentWeatherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrentWeather(currentWeather: CurrentWeatherEntity)
    
    @Query("SELECT * FROM current_weather WHERE locationQuery = :locationQuery")
    suspend fun getCurrentWeatherByLocation(locationQuery: String): CurrentWeatherEntity?
    
    @Query("DELETE FROM current_weather WHERE locationQuery = :locationQuery")
    suspend fun deleteCurrentWeatherByLocation(locationQuery: String)
    
    @Query("SELECT EXISTS(SELECT 1 FROM sqlite_master WHERE type='table' AND name='current_weather')")
    suspend fun doesTableExist(): Boolean
    
    @Query("SELECT * FROM current_weather ORDER BY timestamp DESC")
    suspend fun getAllCurrentWeather(): List<CurrentWeatherEntity>
}