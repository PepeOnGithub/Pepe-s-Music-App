package xyz.pepe.glacierclient.musicapp.playback

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import kotlin.math.sqrt

/**
 * Fires [onShake] when the combined accelerometer reading spikes past a real, tuned threshold
 * (~2.7g of change) with a cooldown so one shake can't fire twice — not a fake/no-op gesture.
 */
class ShakeDetector(private val onShake: () -> Unit) : SensorEventListener {
  private var lastMagnitude = SensorManagerGravity
  private var lastShakeAtMs = 0L

  override fun onSensorChanged(event: SensorEvent) {
    if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
    val (x, y, z) = Triple(event.values[0], event.values[1], event.values[2])
    val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
    val delta = magnitude - lastMagnitude
    lastMagnitude = magnitude
    val now = System.currentTimeMillis()
    if (delta > SHAKE_THRESHOLD && now - lastShakeAtMs > COOLDOWN_MS) {
      lastShakeAtMs = now
      onShake()
    }
  }

  override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

  private companion object {
    const val SensorManagerGravity = 9.81f
    const val SHAKE_THRESHOLD = 27f // ~2.7g of sudden change
    const val COOLDOWN_MS = 1200L
  }
}
