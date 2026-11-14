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

class WorkInferenceTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val thursday = LocalDate.of(2026, 11, 12)
    private val saturday = LocalDate.of(2026, 11, 14)
    private val settings = InferenceSettings(
        workingHours = mapOf(DayOfWeek.THURSDAY to WorkingHours(LocalTime.of(9, 0), LocalTime.of(18, 0)), DayOfWeek.SATURDAY to null),
        zone = zone,
    )
    private fun at(d: LocalDate, h: Int, m: Int = 0): Instant = ZonedDateTime.of(d, LocalTime.of(h, m), zone).toInstant()
    private fun window(d: LocalDate) = DayWindow(d, at(d.minusDays(1), 23, 30), at(d, 23, 30), wake = at(d, 7))
    private val lab = Place(id = 2, name = "lab", lat = 12.98, lon = 77.59, radiusM = 150.0, tag = PlaceTag.WORK)

    private fun still(d: LocalDate, fromH: Int, toH: Int) =
        listOf(MotionSpan(Motion.STILL, Span(at(d, fromH), at(d, toH))))

    private fun phone(d: LocalDate, fromH: Int, fromM: Int, toH: Int, toM: Int) =
        Episode(type = EpisodeType.PHONE, start = at(d, fromH, fromM), end = at(d, toH, toM), day = d, source = Source.AUTO)

    @Test
    fun `still phone idle inside working hours is work`() {
        val work = WorkInference.work(window(thursday), settings, emptyList(), still(thursday, 8, 20), emptyList(), emptyList())
        assertEquals(1, work.size)
        assertEquals(EpisodeType.WORK, work[0].type)
        assertEquals(at(thursday, 9), work[0].start)
        assertEquals(at(thursday, 18), work[0].end)
    }

    @Test
    fun `phone use punches a hole in work`() {
        val work = WorkInference.work(window(thursday), settings, listOf(phone(thursday, 12, 0, 12, 30)), still(thursday, 8, 20), emptyList(), emptyList())
        assertEquals(listOf(at(thursday, 9) to at(thursday, 12), at(thursday, 12, 30) to at(thursday, 18)), work.map { it.start to it.end })
    }

    @Test
    fun `gaps under five minutes are bridged`() {
        val work = WorkInference.work(window(thursday), settings, listOf(phone(thursday, 12, 0, 12, 3)), still(thursday, 8, 20), emptyList(), emptyList())
        assertEquals(1, work.size)
        assertEquals(at(thursday, 9), work[0].start)
        assertEquals(at(thursday, 18), work[0].end)
    }

    @Test
    fun `no work place means low confidence and a work place gives a label`() {
        val noPlace = WorkInference.work(window(thursday), settings, emptyList(), still(thursday, 8, 20), emptyList(), emptyList())
        assertEquals(Confidence.LOW, noPlace[0].confidence)

        val visits = listOf(Visit(lab.id, at(thursday, 9, 30), at(thursday, 13)))
        val atLab = WorkInference.work(window(thursday), settings, emptyList(), still(thursday, 8, 20), visits, listOf(lab))
        assertEquals(1, atLab.size)
        assertEquals(Confidence.HIGH, atLab[0].confidence)
        assertEquals("work (lab)", atLab[0].label)
        assertEquals(at(thursday, 9, 30), atLab[0].start)
        assertEquals(at(thursday, 13), atLab[0].end)
    }

    @Test
    fun `weekend is never work`() {
        assertTrue(WorkInference.work(window(saturday), settings, emptyList(), still(saturday, 8, 20), emptyList(), emptyList()).isEmpty())
    }

    @Test
    fun `walking is not work`() {
        val moving = listOf(MotionSpan(Motion.WALKING, Span(at(thursday, 8), at(thursday, 20))))
        assertTrue(WorkInference.work(window(thursday), settings, emptyList(), moving, emptyList(), emptyList()).isEmpty())
    }
}
