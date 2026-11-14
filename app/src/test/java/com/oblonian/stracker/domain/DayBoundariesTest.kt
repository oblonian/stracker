package com.oblonian.stracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class DayBoundariesTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val settings = InferenceSettings(workingHours = mapOf<DayOfWeek, WorkingHours?>(), zone = zone)

    private fun at(date: LocalDate, hour: Int, minute: Int = 0, z: ZoneId = zone) =
        ZonedDateTime.of(date, LocalTime.of(hour, minute), z).toInstant()

    private fun sleep(day: LocalDate, start: java.time.Instant, end: java.time.Instant) =
        Episode(type = EpisodeType.SLEEP, start = start, end = end, day = day, source = Source.AUTO)

    @Test
    fun `day with main sleep runs from its sleep start to next sleep start`() {
        val d1 = LocalDate.of(2026, 11, 12)
        val d2 = d1.plusDays(1)
        val s1 = sleep(d1, at(d1.minusDays(1), 23, 40), at(d1, 7, 5))
        val s2 = sleep(d2, at(d1, 23, 10), at(d2, 6, 50))
        val windows = DayBoundaries.windows(listOf(d1, d2), listOf(s1, s2), zone)

        assertEquals(s1.start, windows[0].start)
        assertEquals(s1.end, windows[0].wake)
        assertEquals(s2.start, windows[0].end)
        assertEquals(s2.start, windows[1].start)
    }

    @Test
    fun `day without sleep falls back to midnight to midnight`() {
        val d = LocalDate.of(2026, 11, 12)
        val w = DayBoundaries.windows(listOf(d), emptyList(), zone).single()
        assertEquals(at(d, 0), w.start)
        assertEquals(at(d.plusDays(1), 0), w.end)
        assertNull(w.wake)
    }

    @Test
    fun `dst change day is 25h and contiguous`() {
        val berlin = ZoneId.of("Europe/Berlin")
        val d = LocalDate.of(2026, 10, 25) // clocks go back 03:00 -> 02:00
        val windows = DayBoundaries.windows(listOf(d.minusDays(1), d, d.plusDays(1)), emptyList(), berlin)
        assertEquals(25L, Duration.between(windows[1].start, windows[1].end).toHours())
        assertEquals(windows[0].end, windows[1].start)
        assertEquals(windows[1].end, windows[2].start)
    }

    @Test
    fun `sleep search range spans previous evening to this morning`() {
        val d = LocalDate.of(2026, 11, 12)
        val (from, to) = DayBoundaries.sleepSearchRange(d, settings)
        assertEquals(at(d.minusDays(1), 21), from)
        assertEquals(at(d, 11), to)
    }
}
