package com.oblonian.stracker.collect

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.oblonian.stracker.StrackerApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Restarts the collector after a reboot or an app update, once onboarding has been completed. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val app = context.applicationContext as? StrackerApp ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (app.container.settingsRepository.settings.first().onboardingDone) CollectorService.start(app)
            } finally {
                pending.finish()
            }
        }
    }
}
