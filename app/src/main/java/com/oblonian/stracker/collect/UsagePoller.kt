package com.oblonian.stracker.collect

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.oblonian.stracker.StrackerApp
import com.oblonian.stracker.data.SignalRepository
import com.oblonian.stracker.data.db.SignalDao
import com.oblonian.stracker.domain.AppUsageEvent
import com.oblonian.stracker.domain.ScreenEvent
import com.oblonian.stracker.domain.Signals
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/** One row of Android's usage-event log, detached from the platform type so it can be tested. */
data class UsageEventRow(val time: Instant, val packageName: String, val type: Int)

/**
 * Screen on/off and app foreground/background come from [UsageStatsManager]'s event log, read
 * every 15 minutes and on screen-off rather than listened to live, so nothing runs while the
 * screen is off. Needs the "Usage access" special permission.
 */
object UsagePoller {

    private const val SCREEN_INTERACTIVE = 15      // UsageEvents.Event.SCREEN_INTERACTIVE (API 29)
    private const val SCREEN_NON_INTERACTIVE = 16  // UsageEvents.Event.SCREEN_NON_INTERACTIVE (API 29)

    fun parse(rows: Sequence<UsageEventRow>, since: Instant): Pair<List<ScreenEvent>, List<AppUsageEvent>> {
        val screen = ArrayList<ScreenEvent>()
        val apps = ArrayList<AppUsageEvent>()
        for (r in rows) {
            if (!r.time.isAfter(since)) continue
            when (r.type) {
                UsageEvents.Event.ACTIVITY_RESUMED -> apps += AppUsageEvent(r.time, r.packageName, true)
                UsageEvents.Event.ACTIVITY_PAUSED -> apps += AppUsageEvent(r.time, r.packageName, false)
                SCREEN_INTERACTIVE -> screen += ScreenEvent(r.time, true)
                SCREEN_NON_INTERACTIVE -> screen += ScreenEvent(r.time, false)
            }
        }
        return screen.sortedBy { it.time } to apps.sortedBy { it.time }
    }

    fun hasPermission(context: Context): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Reads the raw event log in [from, to]. Throws SecurityException without the permission. */
    fun query(context: Context, from: Instant, to: Instant): Sequence<UsageEventRow> = sequence {
        val usm = context.getSystemService(UsageStatsManager::class.java) ?: return@sequence
        val events = usm.queryEvents(from.toEpochMilli(), to.toEpochMilli()) ?: return@sequence
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            yield(UsageEventRow(Instant.ofEpochMilli(event.timeStamp), event.packageName ?: "", event.eventType))
        }
    }

    private const val PERIODIC = "usage-poll"
    private const val ONCE = "usage-poll-now"

    fun schedulePeriodic(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC, ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<UsagePollWorker>(15, TimeUnit.MINUTES).build(),
        )
    }

    fun pollNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            ONCE, ExistingWorkPolicy.KEEP, OneTimeWorkRequestBuilder<UsagePollWorker>().build(),
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC)
    }
}

class UsagePollWorker(
    context: Context,
    params: WorkerParameters,
    private val daoOverride: SignalDao? = null,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val dao = daoOverride ?: (context as? StrackerApp)?.container?.database?.signalDao() ?: return Result.success()
        if (!UsagePoller.hasPermission(context)) return Result.success()
        val repo = SignalRepository(dao)
        val now = Instant.now()
        val since = listOfNotNull(repo.lastScreenEventTime(), repo.lastAppEventTime()).maxOrNull()
            ?: now.minus(Duration.ofHours(1))
        val (screen, apps) = try {
            UsagePoller.parse(UsagePoller.query(context, since, now), since)
        } catch (e: SecurityException) {
            return Result.success()
        }
        repo.append(Signals(screen = screen, apps = apps))
        return Result.success()
    }

    companion object {
        /** Lets tests run the worker against an in-memory database. */
        fun factory(dao: SignalDao): WorkerFactory = object : WorkerFactory() {
            override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker? =
                if (workerClassName == UsagePollWorker::class.java.name) UsagePollWorker(appContext, workerParameters, dao) else null
        }
    }
}
