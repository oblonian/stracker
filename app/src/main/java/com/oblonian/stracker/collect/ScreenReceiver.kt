package com.oblonian.stracker.collect

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

/**
 * Registered dynamically by the collector service. Screen-off is the moment to flush the usage
 * log (the owner is done with the phone); screen-on is where inference for yesterday is kicked
 * off once the sleep window has passed.
 */
class ScreenReceiver : BroadcastReceiver() {

    var onScreenOn: ((Context) -> Unit)? = null

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> UsagePoller.pollNow(context)
            Intent.ACTION_SCREEN_ON -> onScreenOn?.invoke(context)
        }
    }

    companion object {
        fun filter(): IntentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
    }
}
