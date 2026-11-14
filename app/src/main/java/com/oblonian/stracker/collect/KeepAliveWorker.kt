package com.oblonian.stracker.collect

import android.app.ActivityManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.oblonian.stracker.StrackerApp
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * realme/ColorOS kills background services overnight. Every 15 minutes WorkManager checks that
 * the collector is alive and restarts it if not (spec §5).
 */
class KeepAliveWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? StrackerApp ?: return Result.success()
        val settings = app.container.settingsRepository.settings.first()
        if (settings.onboardingDone && !isRunning(applicationContext)) CollectorService.start(applicationContext)
        return Result.success()
    }

    companion object {
        private const val NAME = "keep-alive"

        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NAME, ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<KeepAliveWorker>(15, TimeUnit.MINUTES).build(),
            )
        }

        fun cancel(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(NAME)

        @Suppress("DEPRECATION")
        fun isRunning(context: Context): Boolean {
            val am = context.getSystemService(ActivityManager::class.java) ?: return false
            return am.getRunningServices(Int.MAX_VALUE).any { it.service.className == CollectorService::class.java.name }
        }
    }
}
