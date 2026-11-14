package com.oblonian.stracker.collect

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.oblonian.stracker.StrackerApp
import com.oblonian.stracker.notify.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * The one always-on service. Owns every collector and the single quiet notification. Starts as a
 * `health` foreground service and adds the `location` type once the owner has granted location
 * (Android 14 refuses a location-type start before that).
 */
class CollectorService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val screenReceiver = ScreenReceiver()
    private var steps: StepsCollector? = null
    private var location: LocationCollector? = null
    private var started = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground()
        if (!started) {
            started = true
            val repo = (application as StrackerApp).container.signalRepository
            ContextCompat.registerReceiver(this, screenReceiver, ScreenReceiver.filter(), ContextCompat.RECEIVER_NOT_EXPORTED)
            screenReceiver.onScreenOn = onScreenOn
            steps = StepsCollector(this, repo, scope).also { it.start() }
            location = LocationCollector(this, repo, scope).also { it.start() }
            MotionTransitionReceiver.request(this)
            UsagePoller.schedulePeriodic(this)
            KeepAliveWorker.schedule(this)
        }
        return START_STICKY
    }

    private fun startForeground() {
        var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        }
        ServiceCompat.startForeground(this, Notifications.COLLECTOR_NOTIFICATION_ID, Notifications.tracking(this), type)
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenReceiver) }
        steps?.stop()
        location?.stop()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        /** Set by the inference layer (Task 10); called on every screen-on. */
        var onScreenOn: ((Context) -> Unit)? = null

        fun start(context: Context) {
            val intent = Intent(context, CollectorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                runCatching { context.startForegroundService(intent) }
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CollectorService::class.java))
            MotionTransitionReceiver.stop(context)
            UsagePoller.cancel(context)
            KeepAliveWorker.cancel(context)
        }
    }
}
