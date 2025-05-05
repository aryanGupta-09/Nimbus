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
 * Manager class for accessing the device's barometer sensor
 */
class BarometerManager(private val context: Context) {
    
    private val sensorManager: SensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    
    private val pressureSensor: Sensor? by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
    }
    
    /**
     * Checks if the device has a pressure (barometer) sensor
     */
    fun hasBarometer(): Boolean {
        return pressureSensor != null
    }
    
    /**
     * Returns the pressure readings from the barometer sensor as a Flow
     * The flow emits pressure values in hPa (which is equivalent to millibars)
     */
    fun getPressureReadings(): Flow<Float> = callbackFlow {
        if (pressureSensor == null) {
            close()
            return@callbackFlow
        }
        
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_PRESSURE) {
                    val pressureValue = event.values[0] // Pressure in hPa (millibar)
                    trySend(pressureValue)
                }
            }
            
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                // We can log accuracy changes but don't need to act on them
                Log.d("BarometerManager", "Accuracy changed: $accuracy")
            }
        }
        
        sensorManager.registerListener(
            listener,
            pressureSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        
        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }.distinctUntilChanged()
}