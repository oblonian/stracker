package com.oblonian.stracker.export

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.oblonian.stracker.data.ButtonRepository
import com.oblonian.stracker.data.PlaceRepository
import com.oblonian.stracker.data.SettingsRepository
import com.oblonian.stracker.data.SignalRepository
import com.oblonian.stracker.data.db.AppDailyUsageEntity
import com.oblonian.stracker.data.db.AppDatabase
import com.oblonian.stracker.data.db.EpisodeEntity
import com.oblonian.stracker.data.db.EventEntity
import com.oblonian.stracker.data.db.OverrideEntity
import com.oblonian.stracker.domain.AppUsageEvent
import com.oblonian.stracker.domain.LocationSample
import com.oblonian.stracker.domain.Motion
import com.oblonian.stracker.domain.MotionTransition
import com.oblonian.stracker.domain.Place
import com.oblonian.stracker.domain.PlaceTag
import com.oblonian.stracker.domain.ScreenEvent
import com.oblonian.stracker.domain.Signals
import com.oblonian.stracker.domain.StepSample
import com.oblonian.stracker.domain.WorkingHours
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class BackupManagerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val t0 = Instant.parse("2026-11-12T08:00:00Z")

    private fun db() = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()

    @Test
    fun `export then import into an empty database reproduces every table and the settings`() = runBlocking {
        val source = db()
        val settings = SettingsRepository(context)
        settings.setWorkingHours(DayOfWeek.SATURDAY, WorkingHours(LocalTime.of(10, 0), LocalTime.of(13, 0)))
        settings.setSleepWindow(LocalTime.of(22, 0), LocalTime.of(10, 0))
        SignalRepository(source.signalDao()).append(
            Signals(
                screen = listOf(ScreenEvent(t0, true), ScreenEvent(t0.plusSeconds(60), false)),
                apps = listOf(AppUsageEvent(t0, "com.example.a", true)),
                motion = listOf(MotionTransition(t0, Motion.STILL)),
                steps = listOf(StepSample(t0, 1234)),
                locations = listOf(LocationSample(t0, 12.9, 77.6, 8f, 1.2f)),
            )
        )
        source.dayDao().insertEpisode(EpisodeEntity(type = "WORK", start = t0.toEpochMilli(), end = t0.plusSeconds(3600).toEpochMilli(), day = "2026-11-12", source = "AUTO", confidence = "HIGH", autoKey = "WORK:${t0.toEpochMilli()}"))
        source.dayDao().insertEvent(EventEntity(type = "HEADACHE", time = t0.toEpochMilli(), day = "2026-11-12", severity = 3, note = "left side"))
        source.dayDao().upsertOverride(OverrideEntity(day = "2026-11-12", episodeKey = "WORK:${t0.toEpochMilli()}", deleted = true))
        source.dayDao().replaceAppUsage("2026-11-12", listOf(AppDailyUsageEntity("2026-11-12", "com.example.a", 60_000)))
        PlaceRepository(source.placeDao()).add(Place(name = "lab", lat = 12.98, lon = 77.59, radiusM = 150.0, tag = PlaceTag.WORK))
        ButtonRepository(source.buttonDao()).seedDefaultsIfEmpty()

        val bytes = ByteArrayOutputStream().also { BackupManager.export(source, settings, it) }.toByteArray()

        // Wipe the settings and restore into a fresh database.
        settings.setWorkingHours(DayOfWeek.SATURDAY, null)
        settings.setSleepWindow(LocalTime.of(21, 0), LocalTime.of(11, 0))
        val target = db()
        BackupManager.import(target, settings, ByteArrayInputStream(bytes))

        assertEquals(2, target.signalDao().allScreen().size)
        assertEquals("com.example.a", target.signalDao().allApps().single().packageName)
        assertEquals("STILL", target.signalDao().allMotion().single().motion)
        assertEquals(1234L, target.signalDao().allSteps().single().cumulative)
        assertEquals(77.6, target.signalDao().allLocations().single().lon, 0.0)
        assertEquals("WORK:${t0.toEpochMilli()}", target.dayDao().allEpisodes().single().autoKey)
        assertEquals("left side", target.dayDao().allEvents().single().note)
        assertEquals(true, target.dayDao().allOverrides().single().deleted)
        assertEquals(60_000L, target.dayDao().allAppUsage().single().foregroundMillis)
        assertEquals("lab", target.placeDao().all().single().name)
        assertEquals(11, target.buttonDao().count())
        val restored = settings.settings.first()
        assertEquals(WorkingHours(LocalTime.of(10, 0), LocalTime.of(13, 0)), restored.workingHours[DayOfWeek.SATURDAY])
        assertEquals(LocalTime.of(22, 0), restored.sleepWindowStart)
        source.close(); target.close()
    }

    @Test
    fun `import replaces what was there`() = runBlocking {
        val source = db()
        val settings = SettingsRepository(context)
        val bytes = ByteArrayOutputStream().also { BackupManager.export(source, settings, it) }.toByteArray()
        val target = db()
        PlaceRepository(target.placeDao()).add(Place(name = "old", lat = 0.0, lon = 0.0, radiusM = 100.0))
        BackupManager.import(target, settings, ByteArrayInputStream(bytes))
        assertNull(target.placeDao().all().firstOrNull())
        source.close(); target.close()
    }

    @Test
    fun `import of an unknown version is rejected`() {
        val target = db()
        val bad = """{"version": 99, "exportedAt": 0}""".toByteArray()
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { BackupManager.import(target, SettingsRepository(context), ByteArrayInputStream(bad)) }
        }
        target.close()
    }
}
