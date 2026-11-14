package com.oblonian.stracker.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.oblonian.stracker.R

/**
 * stracker posts exactly one notification: the quiet notice Android requires while the
 * collector service runs. Nothing else is ever shown (spec §4).
 */
object Notifications {

    const val CHANNEL_COLLECTOR = "collector"

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
}
