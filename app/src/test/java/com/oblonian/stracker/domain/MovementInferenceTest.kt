package com.oblonian.stracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class MovementInferenceTest {

    private val day = LocalDate.of(2026, 11, 12)
    private val base = Instant.parse("2026-11-12T00:00:00Z")
    private fun t(h: Int, m: Int = 0) = base.plusSeconds((h * 60L + m) * 60)
    private val window = DayWindow(day, t(0), t(24), wake = t(7))

    // Two points ~1.1 km apart in Bengaluru.
    private val home = Place(id = 1, name = "home", lat = 12.9716, lon = 77.5946, radiusM = 150.0, tag = PlaceTag.HOME)
    private val lab = Place(id = 2, name = "lab", lat = 12.9816, lon = 77.5946, radiusM = 150.0, tag = PlaceTag.WORK)

    private fun loc(time: Instant, lat: Double, lon: Double) = LocationSample(time, lat, lon, 10f, 0f)

    @Test
    fun `vehicle interval becomes travel`() {
        val intervals = MovementInference.motionIntervals(
            listOf(MotionTransition(t(8), Motion.IN_VEHICLE), MotionTransition(t(8, 30), Motion.STILL)), window,
        )
        val travel = MovementInference.travel(intervals, emptyList(), emptyList(), window)
        assertEquals(1, travel.size)
        assertEquals(EpisodeType.TRAVEL, travel[0].type)
        assertEquals(t(8), travel[0].start)
        assertEquals(t(8, 30), travel[0].end)
    }

    @Test
    fun `walking with displacement over 400 m is travel`() {
        val intervals = MovementInference.motionIntervals(
            listOf(MotionTransition(t(8), Motion.WALKING), MotionTransition(t(8, 15), Motion.STILL)), window,
        )
        val locations = listOf(loc(t(8, 1), home.lat, home.lon), loc(t(8, 14), lab.lat, lab.lon))
        val visits = listOf(Visit(lab.id, t(8, 14), t(12)))
        val travel = MovementInference.travel(intervals, locations, visits, window, listOf(home, lab))
        assertEquals(1, travel.size)
        assertEquals("to lab", travel[0].label)
    }

    @Test
    fun `walking in place is neither travel nor exercise under 20 min`() {
        val intervals = MovementInference.motionIntervals(
            listOf(MotionTransition(t(8), Motion.WALKING), MotionTransition(t(8, 10), Motion.STILL)), window,
        )
        val locations = listOf(loc(t(8, 1), home.lat, home.lon), loc(t(8, 9), home.lat + 0.0005, home.lon))
        assertTrue(MovementInference.travel(intervals, locations, emptyList(), window).isEmpty())
        assertTrue(MovementInference.exercise(intervals, window).isEmpty())
    }

    @Test
    fun `walking 25 minutes is exercise`() {
        val intervals = MovementInference.motionIntervals(
            listOf(MotionTransition(t(18), Motion.WALKING), MotionTransition(t(18, 25), Motion.STILL)), window,
        )
        val exercise = MovementInference.exercise(intervals, window)
        assertEquals(1, exercise.size)
        assertEquals(EpisodeType.EXERCISE, exercise[0].type)
        assertEquals("walk", exercise[0].label)
    }

    @Test
    fun `running of any length is exercise`() {
        val intervals = MovementInference.motionIntervals(
            listOf(MotionTransition(t(6), Motion.RUNNING), MotionTransition(t(6, 8), Motion.STILL)), window,
        )
        assertEquals("run", MovementInference.exercise(intervals, window).single().label)
    }

    @Test
    fun `five minutes inside place radius is a visit`() {
        val locations = listOf(
            loc(t(9), lab.lat, lab.lon), loc(t(9, 3), lab.lat + 0.0002, lab.lon), loc(t(9, 6), lab.lat, lab.lon),
            loc(t(9, 7), home.lat, home.lon),
        )
        val visits = MovementInference.placeVisits(locations, listOf(home, lab), window)
        assertEquals(1, visits.size)
        assertEquals(lab.id, visits[0].placeId)
        assertEquals(t(9), visits[0].start)
        assertEquals(t(9, 6), visits[0].end)
    }

    @Test
    fun `state before window start is carried in`() {
        val intervals = MovementInference.motionIntervals(
            listOf(MotionTransition(t(0).minusSeconds(3600), Motion.IN_VEHICLE), MotionTransition(t(0, 20), Motion.STILL)), window,
        )
        assertEquals(Motion.IN_VEHICLE, intervals[0].motion)
        assertEquals(t(0), intervals[0].span.start)
        assertEquals(t(0, 20), intervals[0].span.end)
    }
}
