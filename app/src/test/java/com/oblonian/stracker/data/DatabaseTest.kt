package com.oblonian.stracker.data

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.oblonian.stracker.data.db.AppDatabase
import com.oblonian.stracker.data.db.AppDailyUsageEntity
import com.oblonian.stracker.data.db.EpisodeEntity
import com.oblonian.stracker.domain.AppUsageEvent
import com.oblonian.stracker.domain.LocationSample
import com.oblonian.stracker.domain.Motion
import com.oblonian.stracker.domain.MotionTransition
import com.oblonian.stracker.domain.ScreenEvent
import com.oblonian.stracker.domain.Signals
import com.oblonian.stracker.domain.StepSample
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DatabaseTest {

    private lateinit var db: AppDatabase
    private val t0 = Instant.parse("2026-11-12T08:00:00Z")
    private val day = LocalDate.of(2026, 11, 12)

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `signals round-trip and come back sorted by time`() = runBlocking {
        val repo = SignalRepository(db.signalDao())
        repo.append(
            Signals(
                screen = listOf(ScreenEvent(t0.plusSeconds(60), false), ScreenEvent(t0, true)),
                apps = listOf(AppUsageEvent(t0.plusSeconds(5), "com.example.a", true)),
                motion = listOf(MotionTransition(t0.plusSeconds(30), Motion.WALKING), MotionTransition(t0, Motion.STILL)),
                steps = listOf(StepSample(t0, 1000)),
                locations = listOf(LocationSample(t0, 12.9, 77.6, 10f, 0f)),
            )
        )
        val back = repo.signalsBetween(t0.minusSeconds(1), t0.plusSeconds(3600))
        assertEquals(listOf(true, false), back.screen.map { it.on })
        assertEquals(listOf(Motion.STILL, Motion.WALKING), back.motion.map { it.motion })
        assertEquals("com.example.a", back.apps.single().packageName)
        assertEquals(1000L, back.steps.single().cumulative)
        assertEquals(12.9, back.locations.single().lat, 0.0)
        assertEquals(Motion.WALKING, repo.lastMotion()?.motion)
        assertEquals(t0.plusSeconds(5), repo.lastAppEventTime())
    }

    @Test
    fun `replaceAutoEpisodes keeps manual episodes of the same day`() = runBlocking {
        val dao = db.dayDao()
        dao.insertEpisode(episode("WORK", "AUTO"))
        dao.insertEpisode(episode("EXERCISE", "MANUAL"))
        dao.replaceAutoEpisodes(day.toString(), listOf(episode("SLEEP", "AUTO"), episode("PHONE", "AUTO")))
        val types = dao.episodesForDay(day.toString()).first().map { it.type }.sorted()
        assertEquals(listOf("EXERCISE", "PHONE", "SLEEP"), types)
    }

    @Test
    fun `app usage upsert replaces the day`() = runBlocking {
        val dao = db.dayDao()
        dao.replaceAppUsage(day.toString(), listOf(AppDailyUsageEntity(day.toString(), "a", 100), AppDailyUsageEntity(day.toString(), "b", 50)))
        dao.replaceAppUsage(day.toString(), listOf(AppDailyUsageEntity(day.toString(), "c", 10)))
        assertEquals(listOf("c"), dao.appUsageForDay(day.toString()).first().map { it.packageName })
    }

    @Test
    fun `default buttons are seeded once`() = runBlocking {
        val repo = ButtonRepository(db.buttonDao())
        repo.seedDefaultsIfEmpty()
        repo.seedDefaultsIfEmpty()
        val buttons = repo.buttons.first()
        assertEquals(11, buttons.size)
        assertEquals("headache", buttons.first().label)
        assertEquals("nap", buttons.last().label)
    }

    private fun episode(type: String, source: String) = EpisodeEntity(
        type = type, start = t0.toEpochMilli(), end = t0.plusSeconds(600).toEpochMilli(),
        day = day.toString(), source = source, confidence = "HIGH",
    )
}
