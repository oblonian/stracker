package com.oblonian.stracker.notify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.oblonian.stracker.MainActivity
import com.oblonian.stracker.R

/**
 * stracker posts exactly one notification: the quiet notice Android requires while the
 * collector service runs. Nothing else is ever shown (spec §4).
 */
object Notifications {

    const val CHANNEL_COLLECTOR = "collector"
    const val COLLECTOR_NOTIFICATION_ID = 1

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_COLLECTOR,
                context.getString(R.string.collector_channel_name),
                NotificationManager.IMPORTANCE_MIN,
            ).apply {
                description = context.getString(R.string.collector_channel_description)
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
        )
    }

    /** Static, silent, never updated. */
    fun tracking(context: Context): Notification {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), flags)
        return NotificationCompat.Builder(context, CHANNEL_COLLECTOR)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(context.getString(R.string.collector_notification_text))
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()
    }
}
