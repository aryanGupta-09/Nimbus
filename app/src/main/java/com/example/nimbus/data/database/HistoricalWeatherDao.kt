package com.example.nimbus.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the historical weather database
 */
@Dao
interface HistoricalWeatherDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoricalWeather(historicalWeather: HistoricalWeatherEntity)
    
    @Query("SELECT * FROM historical_weather WHERE date = :date AND locationQuery = :locationQuery LIMIT 1")
    suspend fun getHistoricalWeatherByDateAndLocation(date: String, locationQuery: String): HistoricalWeatherEntity?
    
    @Query("SELECT * FROM historical_weather WHERE locationQuery = :locationQuery ORDER BY date DESC")
    fun getHistoricalWeatherByLocation(locationQuery: String): Flow<List<HistoricalWeatherEntity>>
    
    @Query("SELECT * FROM historical_weather WHERE locationQuery = :locationQuery AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getHistoricalWeatherInDateRange(locationQuery: String, startDate: String, endDate: String): List<HistoricalWeatherEntity>
    
    @Query("DELETE FROM historical_weather WHERE locationQuery = :locationQuery")
    suspend fun deleteHistoricalWeatherByLocation(locationQuery: String)
    
    @Query("DELETE FROM historical_weather WHERE timestamp < :timestamp")
    suspend fun deleteOldData(timestamp: Long)
    
    /**
     * Safe version of delete operation that checks if the table exists first
     * This prevents crashes when the database is freshly created
     */
    @Query("SELECT EXISTS(SELECT 1 FROM sqlite_master WHERE type='table' AND name='historical_weather')")
    suspend fun doesTableExist(): Boolean
}