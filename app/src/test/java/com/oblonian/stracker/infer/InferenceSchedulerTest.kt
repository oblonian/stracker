package com.oblonian.stracker.infer

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.oblonian.stracker.data.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class InferenceSchedulerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val zone = ZoneId.of("Asia/Kolkata")
    private val today = LocalDate.of(2026, 11, 12)

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context, Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
    }

    private fun enqueued(): List<WorkInfo> = listOf(InferenceScheduler.workName(today.minusDays(1)), InferenceScheduler.workName(today))
        .flatMap { WorkManager.getInstance(context).getWorkInfosForUniqueWork(it).get() }

    @Test
    fun `screen on before window end does nothing`() = runBlocking {
        val settings = SettingsRepository(context)
        InferenceScheduler.onScreenOn(context, settings, ZonedDateTime.of(today, LocalTime.of(8, 30), zone))
        assertEquals(0, enqueued().size)
    }

    @Test
    fun `first screen on after window end runs yesterday and today once`() = runBlocking {
        val settings = SettingsRepository(context)
        InferenceScheduler.onScreenOn(context, settings, ZonedDateTime.of(today, LocalTime.of(11, 10), zone))
        assertEquals(2, enqueued().size)
        assertEquals(today, settings.settings.first().lastInferenceDay)
        InferenceScheduler.onScreenOn(context, settings, ZonedDateTime.of(today, LocalTime.of(12, 0), zone))
        assertEquals(2, enqueued().size)
    }
}
