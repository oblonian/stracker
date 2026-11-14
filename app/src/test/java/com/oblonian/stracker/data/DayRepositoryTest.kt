package com.oblonian.stracker.data

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.oblonian.stracker.data.db.AppDatabase
import com.oblonian.stracker.domain.AppUsageEvent
import com.oblonian.stracker.domain.EpisodeType
import com.oblonian.stracker.domain.EventType
import com.oblonian.stracker.domain.Motion
import com.oblonian.stracker.domain.MotionTransition
import com.oblonian.stracker.domain.ScreenEvent
import com.oblonian.stracker.domain.Signals
import com.oblonian.stracker.domain.Source
import com.oblonian.stracker.domain.StepSample
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DayRepositoryTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val day = LocalDate.of(2026, 11, 12) // Thursday
    private val prev = day.minusDays(1)
    private fun at(d: LocalDate, h: Int, m: Int = 0): Instant = ZonedDateTime.of(d, LocalTime.of(h, m), zone).toInstant()
    private val now = at(day, 22)

    private lateinit var db: AppDatabase
    private lateinit var repo: DayRepository

    @Before
    fun setUp() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val signals = SignalRepository(db.signalDao())
        repo = DayRepository(signals, db.dayDao(), PlaceRepository(db.placeDao()), SettingsRepository(context), Clock.fixed(now, zone))
        signals.append(
            Signals(
                screen = listOf(
                    ScreenEvent(at(prev, 23, 40), false),
                    ScreenEvent(at(day, 7, 5), true), ScreenEvent(at(day, 7, 20), false),
                    ScreenEvent(at(day, 12, 0), true), ScreenEvent(at(day, 12, 30), false),
                ),
                apps = listOf(AppUsageEvent(at(day, 12, 0), "com.example.video", true), AppUsageEvent(at(day, 12, 30), "com.example.video", false)),
                motion = listOf(MotionTransition(at(prev, 23, 40), Motion.STILL)),
                steps = listOf(StepSample(at(day, 7), 100), StepSample(at(day, 20), 1100)),
            )
        )
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `recompute stores sleep phone and work for a synthetic day`() = runBlocking {
        repo.recompute(day)
        val data = repo.day(day).first()
        val types = data.episodes.map { it.type }.toSet()
        assertTrue("sleep", EpisodeType.SLEEP in types)
        assertTrue("phone", EpisodeType.PHONE in types)
        assertTrue("work", EpisodeType.WORK in types)
        assertEquals(at(prev, 23, 40), data.window.start)
        assertEquals(at(day, 7, 5), data.window.wake)
        assertEquals("com.example.video", data.appUsage.single().packageName)
        assertEquals(1000, data.steps)
    }

    @Test
    fun `deleting an auto episode writes an override and it stays gone after recompute`() = runBlocking {
        repo.recompute(day)
        val work = repo.day(day).first().episodes.first { it.type == EpisodeType.WORK }
        repo.deleteEpisode(work.id)
        assertTrue(repo.day(day).first().episodes.none { it.type == EpisodeType.WORK && it.start == work.start })
        repo.recompute(day)
        assertTrue(repo.day(day).first().episodes.none { it.type == EpisodeType.WORK && it.start == work.start })
    }

    @Test
    fun `start then end episode produces a manual block with both ends`() = runBlocking {
        val id = repo.startEpisode(EpisodeType.EXERCISE, null, at(day, 18))
        assertEquals(1, repo.ongoing().first().size)
        assertNull(repo.ongoing().first().single().end)
        repo.endEpisode(id, at(day, 18, 40))
        val manual = repo.day(day).first().episodes.single { it.source == Source.MANUAL }
        assertEquals(at(day, 18) to at(day, 18, 40), manual.start to manual.end)
        assertTrue(repo.ongoing().first().isEmpty())
    }

    @Test
    fun `calendar summary splits millis by type and lists bands and event types`() = runBlocking {
        repo.recompute(day)
        repo.logEvent(EventType.HEADACHE, null, at(day, 14, 32))
        repo.startEpisode(EpisodeType.PERIOD, null, at(day.minusDays(2), 8))
        val summary = repo.calendar(YearMonth.of(2026, 11)).first()
        val s = summary[day]
        assertNotNull(s)
        assertTrue((s!!.millisByType[EpisodeType.SLEEP] ?: 0) > 7 * 3_600_000L)
        assertEquals(45L * 60_000, s.millisByType[EpisodeType.PHONE])
        assertEquals(listOf(EventType.HEADACHE), s.eventTypes)
        assertTrue(EpisodeType.PERIOD in s.bands)
        assertTrue(EpisodeType.PERIOD in summary[day.minusDays(1)]!!.bands)
    }
}
