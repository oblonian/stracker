package com.oblonian.stracker.collect

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.oblonian.stracker.StrackerApp
import com.oblonian.stracker.domain.Motion
import com.oblonian.stracker.domain.MotionTransition
import com.oblonian.stracker.domain.Signals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Receives Activity Recognition transitions (the OS wakes us only when the owner's movement
 * changes) and appends them as motion rows.
 */
class MotionTransitionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (!ActivityTransitionResult.hasResult(intent)) return
        val result = ActivityTransitionResult.extractResult(intent) ?: return
        val rows = toTransitions(result.transitionEvents, bootEpochMillis())
        if (rows.isEmpty()) return
        CollectorState.update(rows.last().motion)
        val app = context.applicationContext as? StrackerApp ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.container.signalRepository.append(Signals(motion = rows))
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION = "com.oblonian.stracker.MOTION_TRANSITION"

        fun toMotion(detectedActivity: Int): Motion = when (detectedActivity) {
            DetectedActivity.STILL -> Motion.STILL
            DetectedActivity.WALKING -> Motion.WALKING
            DetectedActivity.RUNNING -> Motion.RUNNING
            DetectedActivity.ON_BICYCLE -> Motion.ON_BICYCLE
            DetectedActivity.IN_VEHICLE -> Motion.IN_VEHICLE
            else -> Motion.UNKNOWN
        }

        /** Only ENTER events matter: the owner is now doing X. Times come as nanos since boot. */
        fun toTransitions(events: List<ActivityTransitionEvent>, bootEpochMillis: Long): List<MotionTransition> =
            events.filter { it.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER }
                .map { MotionTransition(Instant.ofEpochMilli(bootEpochMillis + it.elapsedRealTimeNanos / 1_000_000), toMotion(it.activityType)) }
                .sortedBy { it.time }

        fun bootEpochMillis(): Long = System.currentTimeMillis() - SystemClock.elapsedRealtime()

        fun hasPermission(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

        fun pendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, MotionTransitionReceiver::class.java).setAction(ACTION)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
            return PendingIntent.getBroadcast(context, 0, intent, flags)
        }

        /** Asks for ENTER transitions of every motion we know. Safe to call repeatedly. */
        fun request(context: Context) {
            if (!hasPermission(context)) return
            val transitions = listOf(
                DetectedActivity.STILL, DetectedActivity.WALKING, DetectedActivity.RUNNING,
                DetectedActivity.ON_BICYCLE, DetectedActivity.IN_VEHICLE,
            ).map {
                ActivityTransition.Builder().setActivityType(it)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER).build()
            }
            runCatching {
                ActivityRecognition.getClient(context)
                    .requestActivityTransitionUpdates(ActivityTransitionRequest(transitions), pendingIntent(context))
            }
        }

        fun stop(context: Context) {
            runCatching { ActivityRecognition.getClient(context).removeActivityTransitionUpdates(pendingIntent(context)) }
        }
    }
}
