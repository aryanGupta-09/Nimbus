package com.example.nimbus.data.model.local

/**
 * Contains information about offline data being displayed
 */
data class OfflineDataInfo(
    val locationName: String,
    val timestamp: Long,
    val isOffline: Boolean = true
)