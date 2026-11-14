package com.oblonian.stracker.ui

import android.app.Application
import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.RoborazziComposeOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.size
import com.oblonian.stracker.data.DaySummary
import com.oblonian.stracker.domain.EpisodeType
import com.oblonian.stracker.domain.EventType
import com.oblonian.stracker.ui.calendar.CalendarContent
import com.oblonian.stracker.ui.theme.StrackerTheme
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w412dp-h915dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CalendarScreenshotTest {

    private val today = LocalDate.of(2026, 11, 12)
    private val month = YearMonth.of(2026, 11)

    private fun shoot(name: String, dark: Boolean = false, content: @Composable () -> Unit): File {
        val file = File("build/screenshots/$name.png")
        captureRoboImage(
            filePath = file.path,
            roborazziComposeOptions = RoborazziComposeOptions { size(widthDp = 412, heightDp = 915) },
        ) { StrackerTheme(darkTheme = dark) { content() } }
        return file
    }

    private fun h(hours: Double) = (hours * 3_600_000).toLong()

    private fun workday(sleep: Double, work: Double, phone: Double, exercise: Double = 0.0, travel: Double = 0.5, eating: Double = 0.75) =
        mapOf(
            EpisodeType.SLEEP to h(sleep), EpisodeType.WORK to h(work), EpisodeType.PHONE to h(phone),
            EpisodeType.EXERCISE to h(exercise), EpisodeType.TRAVEL to h(travel), EpisodeType.EATING to h(eating),
        ).filterValues { it > 0 }

    private fun weekend(sleep: Double, phone: Double, exercise: Double = 0.0) =
        mapOf(EpisodeType.SLEEP to h(sleep), EpisodeType.PHONE to h(phone), EpisodeType.EXERCISE to h(exercise), EpisodeType.EATING to h(1.0))
            .filterValues { it > 0 }

    /** The spec mockup's November, a period band on 9–11 and a headache every Monday afternoon. */
    private val days: Map<LocalDate, DaySummary> = buildMap {
        put(LocalDate.of(2026, 11, 2), DaySummary(workday(7.5, 7.0, 3.0, 0.5), listOf(EventType.HEADACHE, EventType.COFFEE), emptySet()))
        put(LocalDate.of(2026, 11, 3), DaySummary(workday(7.2, 7.5, 2.5), listOf(EventType.COFFEE), emptySet()))
        put(LocalDate.of(2026, 11, 4), DaySummary(workday(7.4, 7.0, 3.1, 0.5), emptyList(), emptySet()))
        put(LocalDate.of(2026, 11, 5), DaySummary(workday(6.8, 6.5, 4.2), listOf(EventType.FEVER, EventType.MEAL, EventType.WATER, EventType.COFFEE), emptySet()))
        put(LocalDate.of(2026, 11, 6), DaySummary(workday(7.6, 6.0, 3.3, eating = 1.5), emptyList(), emptySet()))
        put(LocalDate.of(2026, 11, 7), DaySummary(weekend(8.7, 5.0, 1.0), listOf(EventType.MEAL), emptySet()))
        put(LocalDate.of(2026, 11, 8), DaySummary(weekend(9.5, 6.2), emptyList(), emptySet()))
        put(LocalDate.of(2026, 11, 9), DaySummary(workday(7.5, 7.0, 2.9, 0.5), listOf(EventType.HEADACHE), setOf(EpisodeType.PERIOD)))
        put(LocalDate.of(2026, 11, 10), DaySummary(workday(7.1, 7.8, 3.4), emptyList(), setOf(EpisodeType.PERIOD)))
        put(LocalDate.of(2026, 11, 11), DaySummary(workday(7.3, 6.9, 3.6), listOf(EventType.COFFEE), setOf(EpisodeType.PERIOD)))
        put(today, DaySummary(workday(7.2, 3.5, 1.2), listOf(EventType.HEADACHE), emptySet()))
    }

    @Test
    fun `calendar november`() {
        val light = shoot("calendar") { CalendarContent(month, days, today, onDay = {}, onMonth = {}, onSettings = {}) }
        val dark = shoot("calendar-dark", dark = true) { CalendarContent(month, days, today, onDay = {}, onMonth = {}, onSettings = {}) }
        assertTrue(light.exists() && dark.exists())
    }

    @Test
    fun `calendar empty month`() {
        val file = shoot("calendar-empty") { CalendarContent(YearMonth.of(2026, 10), emptyMap(), today, onDay = {}, onMonth = {}, onSettings = {}) }
        assertTrue(file.exists())
    }
}
