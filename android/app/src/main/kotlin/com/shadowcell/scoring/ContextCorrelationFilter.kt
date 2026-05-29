package com.shadowcell.scoring

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import android.telephony.TelephonyManager

class ContextCorrelationFilter(private val context: Context) : SensorEventListener {

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private var isMoving = false
    private var lastAcceleration = 0f
    private var currentAcceleration = SensorManager.GRAVITY_EARTH
    private var accelerationFiltered = 0f

    init {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            lastAcceleration = currentAcceleration
            currentAcceleration = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val delta = currentAcceleration - lastAcceleration
            accelerationFiltered = accelerationFiltered * 0.9f + delta
            
            // If movement is above a threshold, device is considered moving
            isMoving = accelerationFiltered > 1.5f
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun evaluate(event: ThreatEvent): ThreatEvent {
        var adjustedScore = event.score

        // 1. Moving device: Cell tower changes are normal
        if (isMoving && event.type == EventType.CELL_TOWER_ANOMALY) {
            adjustedScore = (adjustedScore * 0.3).toInt() // Reduce score by 70%
        }

        // 2. Power save mode: Network downgrades might happen to save power
        if (powerManager.isPowerSaveMode && event.type == EventType.NETWORK_DOWNGRADE) {
            adjustedScore = (adjustedScore * 0.5).toInt()
        }

        // 3. Time of day analysis: 3 AM signal drop is different than 3 PM
        val hourOfDay = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        if ((hourOfDay in 2..5) && event.type == EventType.SIGNAL_DROP_HANDOFF) {
            adjustedScore = (adjustedScore * 0.8).toInt() // Slightly reduce due to maintenance hours
        }

        // Note: WiFi calling check requires Android 11+ (API 30) and specific permissions (READ_PRECISE_PHONE_STATE)
        // or checking network capabilities for TransportInfo. Assuming we do a basic check here or skip for now.

        return event.copy(score = adjustedScore.coerceIn(0, 100))
    }

    fun destroy() {
        sensorManager.unregisterListener(this)
    }
}