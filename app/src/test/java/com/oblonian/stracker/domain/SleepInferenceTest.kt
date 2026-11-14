package com.oblonian.stracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class SleepInferenceTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val day = LocalDate.of(2026, 11, 12) // a Thursday
    private val prev = day.minusDays(1)
    private val settings = InferenceSettings(
        workingHours = mapOf(DayOfWeek.THURSDAY to WorkingHours(LocalTime.of(9, 0), LocalTime.of(18, 0))),
        zone = zone,
    )

    private fun at(d: LocalDate, h: Int, m: Int = 0): Instant = ZonedDateTime.of(d, LocalTime.of(h, m), zone).toInstant()

    @Test
    fun `quiet night becomes main sleep on the wake day`() {
        val signals = Signals(
            screen = listOf(ScreenEvent(at(prev, 23, 40), false), ScreenEvent(at(day, 7, 5), true)),
            motion = listOf(MotionTransition(at(prev, 23, 45), Motion.STILL), MotionTransition(at(day, 7, 6), Motion.WALKING)),
        )
        val sleep = SleepInference.mainSleep(day, signals, settings)
        assertNotNull(sleep)
        assertEquals(EpisodeType.SLEEP, sleep!!.type)
        assertEquals(day, sleep.day)
        assertEquals(at(prev, 23, 40), sleep.start)
        assertEquals(at(day, 7, 5), sleep.end)
        assertEquals(Confidence.HIGH, sleep.confidence)
    }

    @Test
    fun `short wake ups under ten minutes stay inside sleep`() {
        val signals = Signals(
            screen = listOf(
                ScreenEvent(at(prev, 23, 40), false),
                ScreenEvent(at(day, 3, 0), true), ScreenEvent(at(day, 3, 5), false),
                ScreenEvent(at(day, 7, 5), true),
            ),
            motion = listOf(MotionTransition(at(prev, 23, 45), Motion.STILL)),
        )
        val sleep = SleepInference.mainSleep(day, signals, settings)!!
        assertEquals(at(prev, 23, 40), sleep.start)
        assertEquals(at(day, 7, 5), sleep.end)
    }

    @Test
    fun `no data at all overnight is treated as still`() {
        val signals = Signals(screen = listOf(ScreenEvent(at(prev, 23, 30), false), ScreenEvent(at(day, 7, 0), true)))
        val sleep = SleepInference.mainSleep(day, signals, settings)!!
        assertEquals(at(prev, 23, 30), sleep.start)
        assertEquals(at(day, 7, 0), sleep.end)
        assertEquals(Confidence.LOW, sleep.confidence)
    }

    @Test
    fun `walking in the night splits sleep and the longer part wins`() {
        val signals = Signals(
            screen = listOf(ScreenEvent(at(prev, 22, 0), false), ScreenEvent(at(day, 7, 0), true)),
            motion = listOf(
                MotionTransition(at(prev, 22, 0), Motion.STILL),
                MotionTransition(at(day, 1, 0), Motion.WALKING),
                MotionTransition(at(day, 1, 30), Motion.STILL),
            ),
        )
        val sleep = SleepInference.mainSleep(day, signals, settings)!!
        assertEquals(at(day, 1, 30), sleep.start)
        assertEquals(at(day, 7, 0), sleep.end)
    }

    @Test
    fun `too short a quiet stretch is not sleep`() {
        val signals = Signals(
            screen = listOf(ScreenEvent(at(day, 2, 0), false), ScreenEvent(at(day, 4, 0), true)),
            motion = listOf(MotionTransition(at(day, 2, 0), Motion.STILL)),
        )
        assertNull(SleepInference.mainSleep(day, signals, settings))
    }

    @Test
    fun `afternoon still of 45 minutes outside working hours is a nap`() {
        val window = DayWindow(day, at(prev, 23, 40), at(day, 23, 30), wake = at(day, 7, 5))
        val signals = Signals(
            screen = listOf(ScreenEvent(at(day, 19, 0), false), ScreenEvent(at(day, 19, 50), true)),
            motion = listOf(MotionTransition(at(day, 19, 0), Motion.STILL), MotionTransition(at(day, 19, 50), Motion.WALKING)),
        )
        val naps = SleepInference.naps(window, signals, settings, mainSleep = null)
        assertEquals(1, naps.size)
        assertEquals(EpisodeType.NAP, naps[0].type)
        assertEquals(Confidence.LOW, naps[0].confidence)
        assertEquals(at(day, 19, 0), naps[0].start)
        assertEquals(at(day, 19, 50), naps[0].end)
    }

    @Test
    fun `still during working hours is not a nap`() {
        val window = DayWindow(day, at(prev, 23, 40), at(day, 23, 30), wake = at(day, 7, 5))
        val signals = Signals(
            screen = listOf(ScreenEvent(at(day, 10, 0), false), ScreenEvent(at(day, 11, 0), true)),
            motion = listOf(MotionTransition(at(day, 10, 0), Motion.STILL)),
        )
        assertTrue(SleepInference.naps(window, signals, settings, mainSleep = null).isEmpty())
    }
}
