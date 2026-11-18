package com.example.guessthebeat

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class FlipDetector(
    sensorManager: SensorManager,
    private val onPhoneFlipped: () -> Unit
) : SensorEventListener {

    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var isRegistered = false
    private var isFaceDown = false
    private var lastTriggerTime = 0L

    init {
        if (accelerometer != null) {
            isRegistered = sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    fun unregister(sensorManager: SensorManager) {
        if (isRegistered) {
            sensorManager.unregisterListener(this)
            isRegistered = false
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val zAxis = event.values[2] // Oś Z odpowiada za nachylenie pionowe telefonu
        val currentTime = System.currentTimeMillis()

        // 1. Gdy telefon jest odwrócony ekranem w dół (oś Z mocno ujemna, poniżej -7.0)
        if (zAxis < -4.0f && !isFaceDown) {
            if (currentTime - lastTriggerTime > 1000L) { // Odstęp czasowy między ruchami
                isFaceDown = true
                lastTriggerTime = currentTime
                onPhoneFlipped() // Wywołanie akcji pominięcia etapu!
            }
        }
        // 2. Gdy telefon wraca do normalnej pozycji (ekranem do góry) -> resetujemy blokadę
        else if (zAxis > 3.0f && isFaceDown) {
            isFaceDown = false
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}