package com.oblonian.stracker.ui

import android.app.Application
import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.RoborazziComposeOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.size
import com.oblonian.stracker.domain.AppUsage
import com.oblonian.stracker.domain.Confidence
import com.oblonian.stracker.domain.DayData
import com.oblonian.stracker.domain.DayWindow
import com.oblonian.stracker.domain.Episode
import com.oblonian.stracker.domain.EpisodeType
import com.oblonian.stracker.domain.Event
import com.oblonian.stracker.domain.EventType
import com.oblonian.stracker.domain.Source
import com.oblonian.stracker.ui.day.AppUsageContent
import com.oblonian.stracker.ui.day.DayActions
import com.oblonian.stracker.ui.day.DayContent
import com.oblonian.stracker.ui.theme.StrackerTheme
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w412dp-h915dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DayScreenshotTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val day = LocalDate.of(2026, 11, 12)
    private val prev = day.minusDays(1)
    private fun at(d: LocalDate, h: Int, m: Int = 0): Instant = ZonedDateTime.of(d, LocalTime.of(h, m), zone).toInstant()
    private val now = at(day, 21, 10)

    private fun shoot(name: String, dark: Boolean = false, content: @Composable () -> Unit): File {
        val file = File("build/screenshots/$name.png")
        captureRoboImage(
            filePath = file.path,
            roborazziComposeOptions = RoborazziComposeOptions { size(widthDp = 412, heightDp = 915) },
        ) { StrackerTheme(darkTheme = dark) { content() } }
        return file
    }

    private fun auto(type: EpisodeType, s: Instant, e: Instant, label: String? = null, confidence: Confidence = Confidence.HIGH) =
        Episode(type = type, start = s, end = e, day = day, source = Source.AUTO, label = label, confidence = confidence)

    private val window = DayWindow(day, at(prev, 23, 40), at(day, 23, 30), wake = at(day, 7, 5))

    /** The approved mockup's Wednesday. */
    private val data = DayData(
        window = window,
        episodes = listOf(
            auto(EpisodeType.SLEEP, at(prev, 23, 40), at(day, 7, 5)),
            auto(EpisodeType.PHONE, at(day, 7, 5), at(day, 7, 25)),
            auto(EpisodeType.TRAVEL, at(day, 8, 50), at(day, 9, 15), "to lab"),
            auto(EpisodeType.WORK, at(day, 9, 30), at(day, 13, 0), "work (lab)"),
            Episode(type = EpisodeType.EATING, start = at(day, 13, 0), end = at(day, 13, 35), day = day, source = Source.MANUAL, label = "lunch"),
            auto(EpisodeType.WORK, at(day, 13, 40), at(day, 17, 40), "work (lab)"),
            auto(EpisodeType.NAP, at(day, 18, 0), at(day, 18, 45), confidence = Confidence.LOW),
            auto(EpisodeType.EXERCISE, at(day, 18, 50), at(day, 19, 20), "walk 2.1 km"),
            auto(EpisodeType.PHONE, at(day, 20, 0), at(day, 21, 10)),
            Episode(type = EpisodeType.CUSTOM, customName = "study", start = at(day, 21, 0), end = null, day = day, source = Source.MANUAL),
        ),
        events = listOf(
            Event(type = EventType.HEADACHE, time = at(day, 9, 50), day = day),
            Event(type = EventType.COFFEE, time = at(day, 15, 0), day = day),
        ),
        appUsage = listOf(AppUsage("com.google.android.youtube", 80 * 60_000L), AppUsage("com.whatsapp", 45 * 60_000L), AppUsage("com.android.chrome", 20 * 60_000L)),
        steps = 6420,
        path = emptyList(),
    )

    private val actions = DayActions()

    @Test
    fun `day view wednesday`() {
        val light = shoot("day") { DayContent(data, zone, now, actions) }
        val dark = shoot("day-dark", dark = true) { DayContent(data, zone, now, actions) }
        assertTrue(light.exists() && dark.exists())
    }

    @Test
    fun `day view empty`() {
        val empty = DayData(DayWindow(day, at(day, 0), at(day.plusDays(1), 0), wake = null), emptyList(), emptyList(), emptyList(), 0, emptyList())
        assertTrue(shoot("day-empty") { DayContent(empty, zone, now, actions) }.exists())
    }

    @Test
    fun `app usage list`() {
        assertTrue(shoot("apps") { AppUsageContent(data.appUsage, labels = mapOf("com.google.android.youtube" to "YouTube", "com.whatsapp" to "WhatsApp"), onBack = {}) }.exists())
    }
}
