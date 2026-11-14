package com.oblonian.stracker.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ModelsTest {

    private val start = Instant.parse("2026-11-12T09:30:00Z")

    @Test
    fun `episode key is type plus start millis`() {
        val episode = Episode(
            type = EpisodeType.WORK, start = start, end = start.plusSeconds(3600),
            day = LocalDate.of(2026, 11, 12), source = Source.AUTO,
        )
        assertEquals("WORK:${start.toEpochMilli()}", episode.key())
    }

    @Test
    fun `durations are computed from start and end`() {
        val day = LocalDate.of(2026, 11, 12)
        val closed = Episode(type = EpisodeType.PHONE, start = start, end = start.plusSeconds(90), day = day, source = Source.AUTO)
        val open = Episode(type = EpisodeType.EXERCISE, start = start, end = null, day = day, source = Source.MANUAL)
        assertEquals(90_000L, closed.durationMillis)
        assertEquals(0L, open.durationMillis)
    }

    @Test
    fun `thresholds carry the spec values`() {
        assertEquals(3L * 60, Thresholds.SLEEP_MIN.toMinutes())
        assertEquals(10L, Thresholds.SLEEP_GAP_MAX.toMinutes())
        assertEquals(40L, Thresholds.NAP_MIN.toMinutes())
        assertEquals(5L, Thresholds.WORK_GAP_MAX.toMinutes())
        assertEquals(20L, Thresholds.WALK_EXERCISE_MIN.toMinutes())
        assertEquals(400.0, Thresholds.TRAVEL_WALK_MIN_METERS, 0.0)
        assertEquals(5L, Thresholds.PLACE_VISIT_MIN.toMinutes())
        assertEquals(21, Thresholds.SLEEP_WINDOW_START.hour)
        assertEquals(11, Thresholds.SLEEP_WINDOW_END.hour)
    }
}
