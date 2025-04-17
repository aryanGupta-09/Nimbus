package com.example.nimbus.data.model.local

import java.util.UUID

/**
 * Represents a location saved by the user
 */
data class SavedLocation(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val query: String,
    val isCurrent: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    companion object {
        fun currentLocation() = SavedLocation(
            id = "current_location",
            name = "Current Location",
            query = "",
            isCurrent = true
        )
    }
}