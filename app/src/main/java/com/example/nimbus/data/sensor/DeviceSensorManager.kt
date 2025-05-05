package com.example.nimbus.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Manager class for accessing various device sensors
 */
class DeviceSensorManager(private val context: Context) {
    
    private val sensorManager: SensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    
    // Define sensor references
    private val pressureSensor: Sensor? by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
    }
    
    private val humiditySensor: Sensor? by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY)
    }
    
    private val temperatureSensor: Sensor? by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
    }
    
    // Check for available sensors
    fun hasBarometer(): Boolean = pressureSensor != null
    fun hasHumiditySensor(): Boolean = humiditySensor != null
    fun hasTemperatureSensor(): Boolean = temperatureSensor != null
    
    // Check if the device has any environmental sensors available
    fun hasEnvSensors(): Boolean {
        return hasBarometer() || hasHumiditySensor() || hasTemperatureSensor()
    }

    // Sensor data flow functions
    fun getPressureReadings(): Flow<Float> = getSensorReadings(pressureSensor, Sensor.TYPE_PRESSURE)
    fun getHumidityReadings(): Flow<Float> = getSensorReadings(humiditySensor, Sensor.TYPE_RELATIVE_HUMIDITY)
    fun getTemperatureReadings(): Flow<Float> = getSensorReadings(temperatureSensor, Sensor.TYPE_AMBIENT_TEMPERATURE)
    
    // Generic function to get readings from any sensor
    private fun getSensorReadings(sensor: Sensor?, sensorType: Int): Flow<Float> = callbackFlow {
        if (sensor == null) {
            close()
            return@callbackFlow
        }
        
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == sensorType) {
                    val sensorValue = event.values[0]
                    trySend(sensorValue)
                }
            }
            
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                // We can log accuracy changes but don't need to act on them
                Log.d("DeviceSensorManager", "Accuracy changed for sensor ${sensor.name}: $accuracy")
            }
        }
        
        sensorManager.registerListener(
            listener,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        
        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }.distinctUntilChanged()
}