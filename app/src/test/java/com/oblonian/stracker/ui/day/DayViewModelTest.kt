package com.oblonian.stracker.ui.day

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.oblonian.stracker.data.ButtonRepository
import com.oblonian.stracker.data.DayRepository
import com.oblonian.stracker.data.PlaceRepository
import com.oblonian.stracker.data.SettingsRepository
import com.oblonian.stracker.data.SignalRepository
import com.oblonian.stracker.data.db.AppDatabase
import com.oblonian.stracker.domain.EpisodeType
import com.oblonian.stracker.domain.EventType
import com.oblonian.stracker.domain.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
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
import java.time.ZoneId
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DayViewModelTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val day = LocalDate.of(2026, 11, 12)
    private fun at(h: Int, m: Int = 0): Instant = ZonedDateTime.of(day, LocalTime.of(h, m), zone).toInstant()
    private val now = at(14, 32)
    private val dispatcher = StandardTestDispatcher()

    private lateinit var db: AppDatabase
    private lateinit var dayRepo: DayRepository
    private lateinit var buttons: ButtonRepository
    private lateinit var vm: DayViewModel

    @Before
    fun setUp() = runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        val context: Context = ApplicationProvider.getApplicationContext()
        // Direct executors make Room's suspend calls complete inline, so advanceUntilIdle() is deterministic.
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        dayRepo = DayRepository(SignalRepository(db.signalDao()), db.dayDao(), PlaceRepository(db.placeDao()), SettingsRepository(context), Clock.fixed(now, zone))
        buttons = ButtonRepository(db.buttonDao()).also { it.seedDefaultsIfEmpty() }
        vm = DayViewModel(SavedStateHandle(mapOf("date" to day.toString())), dayRepo, Clock.fixed(now, zone), buttons)
        advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    private suspend fun button(label: String) = buttons.buttons.first().first { it.label == label }

    @Test
    fun `event button logs at now`() = runTest(dispatcher) {
        vm.tap(button("headache"), at = null)
        advanceUntilIdle()
        val event = db.dayDao().allEvents().single()
        assertEquals(EventType.HEADACHE.name, event.type)
        assertEquals(now.toEpochMilli(), event.time)
    }

    @Test
    fun `episode button starts then ends on second tap`() = runTest(dispatcher) {
        vm.tap(button("exercise"), at = null)
        advanceUntilIdle()
        val open = db.dayDao().ongoingManualEpisodesOnce().single()
        assertEquals(EpisodeType.EXERCISE.name, open.type)
        assertNull(open.end)

        vm.tap(button("exercise"), at = null)
        advanceUntilIdle()
        assertTrue(db.dayDao().ongoingManualEpisodesOnce().isEmpty())
        assertEquals(now.toEpochMilli(), db.dayDao().episode(open.id)!!.end)
    }

    @Test
    fun `undo removes the last event`() = runTest(dispatcher) {
        vm.tap(button("coffee"), at = null)
        advanceUntilIdle()
        assertEquals(1, db.dayDao().allEvents().size)
        vm.undoLast()
        advanceUntilIdle()
        assertTrue(db.dayDao().allEvents().isEmpty())
    }

    @Test
    fun `undo reopens an ended episode`() = runTest(dispatcher) {
        vm.tap(button("nap"), at = at(13, 0))
        advanceUntilIdle()
        vm.tap(button("nap"), at = null)
        advanceUntilIdle()
        assertTrue(db.dayDao().ongoingManualEpisodesOnce().isEmpty())
        vm.undoLast()
        advanceUntilIdle()
        assertEquals(1, db.dayDao().ongoingManualEpisodesOnce().size)
    }

    @Test
    fun `log at long pressed time uses that time and study is a custom episode`() = runTest(dispatcher) {
        vm.tap(button("study"), at = at(9, 15))
        advanceUntilIdle()
        val e = db.dayDao().ongoingManualEpisodesOnce().single()
        assertEquals(at(9, 15).toEpochMilli(), e.start)
        assertEquals(EpisodeType.CUSTOM.name, e.type)
        assertEquals("study", e.customName)
        assertEquals(Source.MANUAL.name, e.source)
    }
}
