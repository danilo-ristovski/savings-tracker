package com.savings.tracker.data.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeDetector(
    private val onShake: () -> Unit,
) : SensorEventListener {

    private var lastShakeTime = 0L
    private var lastAccel = SensorManager.GRAVITY_EARTH
    private var currentAccel = SensorManager.GRAVITY_EARTH
    private var accelDelta = 0f

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        lastAccel = currentAccel
        currentAccel = sqrt(x * x + y * y + z * z)
        val delta = currentAccel - lastAccel
        accelDelta = accelDelta * 0.9f + delta

        if (accelDelta > SHAKE_THRESHOLD) {
            val now = System.currentTimeMillis()
            if (now - lastShakeTime > SHAKE_COOLDOWN_MS) {
                lastShakeTime = now
                onShake()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    companion object {
        private const val SHAKE_THRESHOLD = 6f
        private const val SHAKE_COOLDOWN_MS = 2000L
    }
}
