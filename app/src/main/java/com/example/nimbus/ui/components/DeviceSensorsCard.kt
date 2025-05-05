package com.example.nimbus.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nimbus.data.model.local.DeviceSensorData

@Composable
fun DeviceSensorsCard(
    sensorData: DeviceSensorData,
    hasSensors: Boolean
) {
    // Only show the card if at least one sensor is available
    if (!hasSensors) return
    
    // Check if we have any actual readings available
    val hasPressureReading = sensorData.pressure != null
    val hasHumidityReading = sensorData.humidity != null
    val hasTemperatureReading = sensorData.temperature != null
    
    // Only display card if we have at least one reading
    if (!hasPressureReading && !hasHumidityReading && !hasTemperatureReading) {
        return
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Device Sensor Readings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // First row of sensors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Show pressure reading if available
                if (hasPressureReading) {
                    SensorReadingItem(
                        icon = Icons.Default.DeviceHub,
                        label = "Pressure",
                        value = "${sensorData.pressure!!.toInt()} hPa"
                    )
                }
                
                // Show humidity reading if available
                if (hasHumidityReading) {
                    SensorReadingItem(
                        icon = Icons.Default.Opacity,
                        label = "Humidity",
                        value = "${sensorData.humidity!!.toInt()}%"
                    )
                }
            }
            
            // Only add spacing if we have temperature reading
            if (hasTemperatureReading) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Second row for temperature sensor
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Show temperature reading if available
                    if (hasTemperatureReading) {
                        SensorReadingItem(
                            icon = Icons.Default.Thermostat,
                            label = "Temperature",
                            value = "${sensorData.temperature!!.toInt()}°C"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SensorReadingItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}