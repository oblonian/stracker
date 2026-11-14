package com.oblonian.stracker.domain

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Great-circle distance in metres. Pure Kotlin so it can run in plain JVM tests. */
fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadius = 6_371_000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return 2 * earthRadius * asin(sqrt(a))
}

/**
 * One-dimensional Kalman filter applied to latitude and longitude. Each fix is weighted by
 * its reported accuracy, so jittery fixes while standing still or under trees barely move
 * the estimate, while accurate fixes at speed pass through almost unchanged.
 */
class GpsSmoother {

    private var latitude = 0.0
    private var longitude = 0.0
    private var varianceMeters2 = -1.0
    private var timeMillis = 0L

    fun reset() {
        varianceMeters2 = -1.0
    }

    /**
     * Feeds a raw fix and returns the smoothed position.
     * [speedMps] is how fast the athlete is moving; it sets how quickly the estimate may drift.
     */
    fun update(
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float,
        timeMillis: Long,
        speedMps: Float,
    ): Pair<Double, Double> {
        val accuracy = max(accuracyMeters, MIN_ACCURACY_METERS).toDouble()
        if (varianceMeters2 < 0) {
            this.latitude = latitude
            this.longitude = longitude
            this.timeMillis = timeMillis
            varianceMeters2 = accuracy * accuracy
        } else {
            val elapsedSeconds = (timeMillis - this.timeMillis) / 1000.0
            if (elapsedSeconds > 0) {
                val drift = max(speedMps.toDouble(), MIN_DRIFT_MPS)
                varianceMeters2 += elapsedSeconds * drift * drift
                this.timeMillis = timeMillis
            }
            val gain = varianceMeters2 / (varianceMeters2 + accuracy * accuracy)
            this.latitude += gain * (latitude - this.latitude)
            this.longitude += gain * (longitude - this.longitude)
            varianceMeters2 *= (1 - gain)
        }
        return this.latitude to this.longitude
    }

    private companion object {
        const val MIN_ACCURACY_METERS = 1f
        const val MIN_DRIFT_MPS = 0.5
    }
}
