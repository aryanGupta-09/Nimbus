package com.example.nimbus.data.model.local

/**
 * Data class to represent local sensor readings from the device
 */
data class DeviceSensorData(
    val pressure: Float? = null,
    val humidity: Float? = null,
    val temperature: Float? = null
)