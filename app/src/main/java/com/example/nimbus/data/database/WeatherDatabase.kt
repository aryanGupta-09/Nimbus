package com.example.nimbus.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Room database for storing weather data
 */
@Database(entities = [HistoricalWeatherEntity::class, CurrentWeatherEntity::class], version = 4, exportSchema = false)
@TypeConverters(WeatherResponseConverter::class)
abstract class WeatherDatabase : RoomDatabase() {
    
    abstract fun historicalWeatherDao(): HistoricalWeatherDao
    abstract fun currentWeatherDao(): CurrentWeatherDao
    
    companion object {
        @Volatile
        private var INSTANCE: WeatherDatabase? = null
        
        fun getDatabase(context: Context): WeatherDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WeatherDatabase::class.java,
                    "nimbus_weather_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}