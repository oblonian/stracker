package com.oblonian.stracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class PhoneUsageTest {

    private val day = LocalDate.of(2026, 11, 12)
    private val base = Instant.parse("2026-11-12T00:00:00Z")
    private fun t(h: Int, m: Int = 0) = base.plusSeconds((h * 60L + m) * 60)
    private val window = DayWindow(day, t(0), t(24), wake = t(7))

    @Test
    fun `on off pairs become phone episodes`() {
        val screen = listOf(
            ScreenEvent(t(7, 5), true), ScreenEvent(t(7, 20), false),
            ScreenEvent(t(12, 0), true), ScreenEvent(t(12, 10), false),
        )
        val episodes = PhoneUsage.episodes(screen, window)
        assertEquals(2, episodes.size)
        assertTrue(episodes.all { it.type == EpisodeType.PHONE && it.source == Source.AUTO && it.day == day })
        assertEquals(t(7, 5), episodes[0].start)
        assertEquals(t(7, 20), episodes[0].end)
        assertEquals(t(12, 0), episodes[1].start)
        assertEquals(t(12, 10), episodes[1].end)
    }

    @Test
    fun `unterminated on is clipped to window end`() {
        val episodes = PhoneUsage.episodes(listOf(ScreenEvent(t(23, 30), true)), window)
        assertEquals(t(23, 30), episodes.single().start)
        assertEquals(t(24), episodes.single().end)
    }

    @Test
    fun `off without a prior on starts at window start`() {
        val episodes = PhoneUsage.episodes(listOf(ScreenEvent(t(0, 12), false)), window)
        assertEquals(t(0), episodes.single().start)
        assertEquals(t(0, 12), episodes.single().end)
    }

    @Test
    fun `per app counts only screen on time`() {
        val screen = listOf(
            ScreenEvent(t(10, 0), true), ScreenEvent(t(10, 10), false),
            ScreenEvent(t(10, 20), true), ScreenEvent(t(10, 30), false),
        )
        val apps = listOf(
            AppUsageEvent(t(10, 0), "com.example.video", true),
            AppUsageEvent(t(10, 30), "com.example.video", false),
        )
        val usage = PhoneUsage.perApp(apps, screen, window)
        assertEquals(20L * 60_000, usage.single { it.packageName == "com.example.video" }.foregroundMillis)
    }

    @Test
    fun `apps under one minute are dropped and the rest sort descending`() {
        val screen = listOf(ScreenEvent(t(9, 0), true), ScreenEvent(t(9, 30), false))
        val apps = listOf(
            AppUsageEvent(t(9, 0), "a", true), AppUsageEvent(t(9, 0, ), "a", false).copy(time = t(9, 0).plusSeconds(30)),
            AppUsageEvent(t(9, 1), "b", true), AppUsageEvent(t(9, 6), "b", false),
            AppUsageEvent(t(9, 6), "c", true), AppUsageEvent(t(9, 20), "c", false),
        )
        val usage = PhoneUsage.perApp(apps, screen, window)
        assertEquals(listOf("c", "b"), usage.map { it.packageName })
    }
}
