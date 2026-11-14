package com.oblonian.stracker.collect

import android.app.Application
import android.app.usage.UsageEvents
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.oblonian.stracker.data.db.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class UsagePollerTest {

    private val t0 = Instant.parse("2026-11-12T08:00:00Z")
    private fun row(sec: Long, pkg: String, type: Int) = UsageEventRow(t0.plusSeconds(sec), pkg, type)

    @Test
    fun `resume and pause map to foreground and background`() {
        val (_, apps) = UsagePoller.parse(
            sequenceOf(row(0, "a", UsageEvents.Event.ACTIVITY_RESUMED), row(30, "a", UsageEvents.Event.ACTIVITY_PAUSED)),
            since = t0.minusSeconds(1),
        )
        assertEquals(listOf(true, false), apps.map { it.foreground })
        assertEquals("a", apps[0].packageName)
        assertEquals(t0, apps[0].time)
    }

    @Test
    fun `screen interactive maps to on and off`() {
        val (screen, apps) = UsagePoller.parse(
            sequenceOf(row(0, "android", UsageEvents.Event.SCREEN_INTERACTIVE), row(60, "android", UsageEvents.Event.SCREEN_NON_INTERACTIVE)),
            since = t0.minusSeconds(1),
        )
        assertEquals(listOf(true, false), screen.map { it.on })
        assertTrue(apps.isEmpty())
    }

    @Test
    fun `events at or before since are skipped and unknown types ignored`() {
        val (screen, apps) = UsagePoller.parse(
            sequenceOf(
                row(0, "a", UsageEvents.Event.ACTIVITY_RESUMED),
                row(10, "a", UsageEvents.Event.CONFIGURATION_CHANGE),
                row(20, "a", UsageEvents.Event.ACTIVITY_PAUSED),
            ),
            since = t0,
        )
        assertTrue(screen.isEmpty())
        assertEquals(1, apps.size)
        assertEquals(false, apps[0].foreground)
    }

    @Test
    fun `missing permission records nothing and does not throw`() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val worker = TestListenableWorkerBuilder<UsagePollWorker>(context)
            .setWorkerFactory(UsagePollWorker.factory(db.signalDao()))
            .build()
        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(db.signalDao().allScreen().isEmpty())
        assertTrue(db.signalDao().allApps().isEmpty())
        db.close()
    }
}
