package com.oblonian.stracker.infer

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.oblonian.stracker.StrackerApp
import com.oblonian.stracker.collect.CollectorService
import com.oblonian.stracker.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * When inference runs (spec §6.4): on the first screen-on after the sleep window's end hour
 * (yesterday is complete by then), at the end hour itself if the phone wasn't touched, and
 * whenever a day view is opened (the ViewModel calls `recompute` directly).
 */
object InferenceScheduler {

    fun workName(day: LocalDate) = "infer-$day"

    private const val DAILY = "infer-daily"

    suspend fun onScreenOn(context: Context, settings: SettingsRepository, now: ZonedDateTime) {
        val s = settings.settings.first()
        val today = now.toLocalDate()
        if (now.toLocalTime().isBefore(s.sleepWindowEnd)) return
        if (s.lastInferenceDay == today) return
        enqueue(context, today.minusDays(1))
        enqueue(context, today)
        settings.setLastInferenceDay(today)
    }

    fun enqueue(context: Context, day: LocalDate) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(day), ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<InferenceWorker>().setInputData(workDataOf(InferenceWorker.KEY_DAY to day.toString())).build(),
        )
    }

    /** A daily check at the sleep window's end hour, for days the phone isn't touched in the morning. */
    fun scheduleEndHour(context: Context, settings: SettingsRepository, now: ZonedDateTime) {
        CoroutineScope(Dispatchers.IO).launch {
            val endHour = settings.settings.first().sleepWindowEnd
            var next = now.with(endHour)
            if (!next.isAfter(now)) next = next.plusDays(1)
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                DAILY, ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<InferenceWorker>(24, TimeUnit.HOURS)
                    .setInitialDelay(Duration.between(now, next).toMillis(), TimeUnit.MILLISECONDS)
                    .build(),
            )
        }
    }

    /** Hooks the collector's screen-on events to [onScreenOn]. */
    fun install(app: StrackerApp) {
        CollectorService.onScreenOn = { ctx ->
            CoroutineScope(Dispatchers.IO).launch { onScreenOn(ctx, app.container.settingsRepository, ZonedDateTime.now()) }
        }
    }
}

/** Recomputes one day (input [KEY_DAY]) or, without input, yesterday and today. */
class InferenceWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? StrackerApp ?: return Result.success()
        val repo = app.container.dayRepository
        val day = inputData.getString(KEY_DAY)?.let(LocalDate::parse)
        val days = if (day != null) listOf(day) else LocalDate.now().let { listOf(it.minusDays(1), it) }
        for (d in days) runCatching { repo.recompute(d) }
        day?.let { app.container.settingsRepository.setLastInferenceDay(maxOf(it, LocalDate.now().minusDays(1))) }
        return Result.success()
    }

    companion object {
        const val KEY_DAY = "day"
    }
}
