package com.oblonian.stracker.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class StepsTest {

    private val base = Instant.parse("2026-11-12T00:00:00Z")
    private fun t(h: Int) = base.plusSeconds(h * 3600L)
    private val window = DayWindow(LocalDate.of(2026, 11, 12), t(0), t(24), wake = t(7))

    @Test
    fun `steps are the sum of deltas`() {
        val samples = listOf(StepSample(t(7), 1000), StepSample(t(9), 1500), StepSample(t(12), 2200))
        assertEquals(1200, Steps.forWindow(samples, window))
    }

    @Test
    fun `counter reset after reboot does not produce negative or inflated steps`() {
        val samples = listOf(StepSample(t(7), 1000), StepSample(t(9), 1500), StepSample(t(10), 20), StepSample(t(12), 220))
        assertEquals(700, Steps.forWindow(samples, window))
    }

    @Test
    fun `samples outside the window are ignored and one sample is zero`() {
        val samples = listOf(StepSample(t(-2), 0), StepSample(t(7), 1000), StepSample(t(30), 9000))
        assertEquals(0, Steps.forWindow(samples, window))
    }
}
