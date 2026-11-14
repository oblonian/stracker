package com.oblonian.stracker.collect

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.oblonian.stracker.data.SignalRepository
import com.oblonian.stracker.domain.GpsSmoother
import com.oblonian.stracker.domain.LocationSample
import com.oblonian.stracker.domain.Motion
import com.oblonian.stracker.domain.Signals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Records the owner's path, but only while they move: [LocationGate] turns motion changes into
 * start/stop decisions, so a still phone never holds the GPS (spec §6.2).
 */
class LocationCollector(
    private val context: Context,
    private val repository: SignalRepository,
    private val scope: CoroutineScope,
    private val motion: StateFlow<Motion> = CollectorState.motion,
    private val gate: LocationGate = LocationGate(),
) {
    private val client: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private val smoother = GpsSmoother()
    private var job: Job? = null
    private var requesting = false

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val samples = result.locations.mapNotNull(::accept)
            if (samples.isNotEmpty()) scope.launch { repository.append(Signals(locations = samples)) }
        }
    }

    fun start() {
        if (job != null) return
        job = scope.launch { motion.collect { apply(gate.onMotion(it)) } }
    }

    fun stop() {
        job?.cancel()
        job = null
        removeUpdates()
    }

    private fun hasPermission() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun apply(action: GateAction) {
        if (!hasPermission()) return
        when (action) {
            GateAction.START_WALK, GateAction.START_VEHICLE -> {
                removeUpdates()
                smoother.reset()
                val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, action.intervalMillis)
                    .setMinUpdateDistanceMeters(action.minDisplacementM)
                    .setWaitForAccurateLocation(false)
                    .build()
                client.requestLocationUpdates(request, callback, Looper.getMainLooper())
                requesting = true
            }
            GateAction.FINAL_FIX_THEN_STOP -> {
                removeUpdates()
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).addOnSuccessListener { loc ->
                    loc?.let(::accept)?.let { s -> scope.launch { repository.append(Signals(locations = listOf(s))) } }
                }
            }
            GateAction.NONE -> Unit
        }
    }

    @SuppressLint("MissingPermission")
    private fun removeUpdates() {
        if (requesting) client.removeLocationUpdates(callback)
        requesting = false
    }

    /** Accuracy gate, then Ostrava's Kalman smoothing. */
    private fun accept(location: Location): LocationSample? {
        if (!location.hasAccuracy() || location.accuracy > MAX_ACCURACY_M) return null
        val speed = if (location.hasSpeed()) location.speed else 0f
        val (lat, lon) = smoother.update(location.latitude, location.longitude, location.accuracy, location.time, speed)
        return LocationSample(Instant.ofEpochMilli(location.time), lat, lon, location.accuracy, speed)
    }

    private companion object {
        const val MAX_ACCURACY_M = 50f
    }
}
