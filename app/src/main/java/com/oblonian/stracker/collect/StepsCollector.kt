package com.oblonian.stracker.collect

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.oblonian.stracker.data.SignalRepository
import com.oblonian.stracker.domain.Signals
import com.oblonian.stracker.domain.StepSample
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant

/** Reads the cumulative step counter, batched so the phone can deliver it every 15 minutes. */
class StepsCollector(
    context: Context,
    private val repository: SignalRepository,
    private val scope: CoroutineScope,
) : SensorEventListener {

    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val sensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    fun start() {
        val s = sensor ?: return
        sensorManager?.registerListener(this, s, SensorManager.SENSOR_DELAY_NORMAL, MAX_REPORT_LATENCY_US)
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
    }

    /** Asks the sensor to deliver whatever it has batched right now. */
    fun flush() {
        sensorManager?.flush(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val bootEpochMillis = MotionTransitionReceiver.bootEpochMillis()
        val time = Instant.ofEpochMilli(bootEpochMillis + event.timestamp / 1_000_000)
        val sample = StepSample(time, event.values[0].toLong())
        scope.launch { repository.append(Signals(steps = listOf(sample))) }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private companion object {
        const val MAX_REPORT_LATENCY_US = 15 * 60 * 1_000_000
    }
}
