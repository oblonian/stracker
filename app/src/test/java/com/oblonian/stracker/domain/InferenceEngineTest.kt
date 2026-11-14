package com.oblonian.stracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class InferenceEngineTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val day = LocalDate.of(2026, 11, 12) // Thursday
    private val prev = day.minusDays(1)
    private val settings = InferenceSettings(
        workingHours = mapOf(DayOfWeek.THURSDAY to WorkingHours(LocalTime.of(9, 0), LocalTime.of(18, 0))),
        zone = zone,
    )
    private val engine = InferenceEngine(settings)
    private fun at(d: LocalDate, h: Int, m: Int = 0): Instant = ZonedDateTime.of(d, LocalTime.of(h, m), zone).toInstant()

    private val sleep = Episode(type = EpisodeType.SLEEP, start = at(prev, 23, 40), end = at(day, 7, 5), day = day, source = Source.AUTO)
    private val window = DayWindow(day, sleep.start, at(day, 23, 30), wake = sleep.end)
    private val now = at(day, 22)

    /** A whole day still with the phone off: one long work block 09:00–18:00 and nothing else. */
    private val quietDay = Signals(
        screen = listOf(ScreenEvent(at(prev, 23, 40), false), ScreenEvent(at(day, 7, 5), true), ScreenEvent(at(day, 7, 20), false)),
        motion = listOf(MotionTransition(at(prev, 23, 40), Motion.STILL)),
        steps = listOf(StepSample(at(day, 7), 100), StepSample(at(day, 20), 1100)),
    )

    private fun infer(manual: List<Episode> = emptyList(), overrides: List<Override> = emptyList()) =
        engine.inferDay(day, window, sleep, quietDay, emptyList(), manual, overrides, now)

    @Test
    fun `a quiet day yields sleep, a phone burst, a work block and steps`() {
        val data = infer()
        val types = data.episodes.map { it.type }
        assertTrue(EpisodeType.SLEEP in types)
        assertTrue(EpisodeType.PHONE in types)
        assertTrue(EpisodeType.WORK in types)
        assertEquals(1000, data.steps)
        val work = data.episodes.single { it.type == EpisodeType.WORK }
        assertEquals(at(day, 9) to at(day, 18), work.start to work.end)
    }

    @Test
    fun `auto episodes are clipped around a manual episode`() {
        val manual = Episode(type = EpisodeType.EXERCISE, start = at(day, 12), end = at(day, 13), day = day, source = Source.MANUAL)
        val work = infer(manual = listOf(manual)).episodes.filter { it.type == EpisodeType.WORK }
        assertEquals(listOf(at(day, 9) to at(day, 12), at(day, 13) to at(day, 18)), work.map { it.start to it.end })
    }

    @Test
    fun `an ongoing manual episode is clipped up to now`() {
        val manual = Episode(type = EpisodeType.CUSTOM, customName = "study", start = at(day, 16), end = null, day = day, source = Source.MANUAL)
        val work = infer(manual = listOf(manual)).episodes.single { it.type == EpisodeType.WORK }
        assertEquals(at(day, 16), work.end)
    }

    @Test
    fun `override can retype and delete`() {
        val original = infer().episodes.single { it.type == EpisodeType.WORK }
        val retyped = infer(overrides = listOf(Override(day = day, episodeKey = original.key(), newType = EpisodeType.CUSTOM)))
        val edited = retyped.episodes.single { it.start == original.start && it.type == EpisodeType.CUSTOM }
        assertEquals(Source.AUTO_EDITED, edited.source)

        val deleted = infer(overrides = listOf(Override(day = day, episodeKey = original.key(), deleted = true)))
        assertTrue(deleted.episodes.none { it.type == EpisodeType.WORK })
    }

    @Test
    fun `overrides survive re-running inference`() {
        val overrides = listOf(Override(day = day, episodeKey = infer().episodes.single { it.type == EpisodeType.WORK }.key(), newEnd = at(day, 17)))
        assertEquals(infer(overrides = overrides), infer(overrides = overrides))
        assertEquals(at(day, 17), infer(overrides = overrides).episodes.single { it.type == EpisodeType.WORK }.end)
    }
}
