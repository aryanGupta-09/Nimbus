package com.example.nimbus.data.location

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.nimbus.data.model.local.SavedLocation
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.lang.reflect.Type

// Create the DataStore instance at the app level
private val Context.locationDataStore: DataStore<Preferences> by preferencesDataStore(name = "locations")

/**
 * Manages user's saved locations for weather data
 */
class LocationManager(private val context: Context) {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val locationsKey = stringPreferencesKey("saved_locations")
    private val selectedLocationKey = stringPreferencesKey("selected_location_id")
    private val listType: Type = Types.newParameterizedType(List::class.java, SavedLocation::class.java)
    private val jsonAdapter: JsonAdapter<List<SavedLocation>> = moshi.adapter(listType)

    // Get all saved locations as a Flow
    val savedLocations: Flow<List<SavedLocation>> = context.locationDataStore.data.map { preferences ->
        val locationsJson = preferences[locationsKey] ?: "[]"
        val locations = jsonAdapter.fromJson(locationsJson) ?: emptyList()
        
        // Ensure we always have a Current Location option
        if (locations.none { it.isCurrent }) {
            listOf(SavedLocation.currentLocation()) + locations
        } else {
            locations
        }
    }
    
    // Get currently selected location ID as a Flow
    val selectedLocationId: Flow<String> = context.locationDataStore.data.map { preferences ->
        preferences[selectedLocationKey] ?: SavedLocation.currentLocation().id
    }

    // Add a new saved location
    suspend fun addLocation(location: SavedLocation) {
        context.locationDataStore.edit { preferences ->
            val locationsJson = preferences[locationsKey] ?: "[]"
            val currentLocations = jsonAdapter.fromJson(locationsJson) ?: emptyList()
            val updatedLocations = currentLocations + location
            preferences[locationsKey] = jsonAdapter.toJson(updatedLocations)
        }
    }

    // Remove a saved location
    suspend fun removeLocation(locationId: String) {
        context.locationDataStore.edit { preferences ->
            val locationsJson = preferences[locationsKey] ?: "[]"
            val currentLocations = jsonAdapter.fromJson(locationsJson) ?: emptyList()
            val updatedLocations = currentLocations.filter { it.id != locationId }
            preferences[locationsKey] = jsonAdapter.toJson(updatedLocations)
            
            // If the removed location was selected, revert to current location
            if (preferences[selectedLocationKey] == locationId) {
                preferences[selectedLocationKey] = SavedLocation.currentLocation().id
            }
        }
    }

    // Set the selected location
    suspend fun setSelectedLocation(locationId: String) {
        context.locationDataStore.edit { preferences ->
            preferences[selectedLocationKey] = locationId
        }
    }
}