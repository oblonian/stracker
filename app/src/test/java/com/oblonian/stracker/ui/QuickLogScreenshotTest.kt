package com.oblonian.stracker.ui

import android.app.Application
import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.RoborazziComposeOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.size
import com.oblonian.stracker.data.ButtonRepository
import com.oblonian.stracker.domain.Confidence
import com.oblonian.stracker.domain.Episode
import com.oblonian.stracker.domain.EpisodeType
import com.oblonian.stracker.domain.Source
import com.oblonian.stracker.ui.day.DetailContent
import com.oblonian.stracker.ui.day.DetailItem
import com.oblonian.stracker.ui.day.QuickLogGrid
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
class QuickLogScreenshotTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val day = LocalDate.of(2026, 11, 12)
    private fun at(h: Int, m: Int = 0): Instant = ZonedDateTime.of(day, LocalTime.of(h, m), zone).toInstant()
    private val now = at(14, 32)

    private fun shoot(name: String, heightDp: Int = 915, content: @Composable () -> Unit): File {
        val file = File("build/screenshots/$name.png")
        captureRoboImage(
            filePath = file.path,
            roborazziComposeOptions = RoborazziComposeOptions { size(widthDp = 412, heightDp = heightDp) },
        ) { StrackerTheme { content() } }
        return file
    }

    @Test
    fun `quick log sheet with ongoing exercise`() {
        val buttons = ButtonRepository.DEFAULTS.mapIndexed { i, b -> b.copy(id = i + 1L, sortOrder = i) }
        val ongoing = listOf(Episode(type = EpisodeType.EXERCISE, start = at(14, 10), end = null, day = day, source = Source.MANUAL))
        val file = shoot("quicklog", heightDp = 420) {
            QuickLogGrid(buttons, ongoing, at = null, now = now, zone = zone, onTap = {}, onLongPress = {}, onAddCustom = {})
        }
        assertTrue(file.exists())
    }

    @Test
    fun `detail sheet for auto work block`() {
        val work = Episode(
            type = EpisodeType.WORK, start = at(9, 30), end = at(13, 0), day = day, source = Source.AUTO,
            confidence = Confidence.LOW, label = "work (lab)",
        )
        val file = shoot("detail", heightDp = 520) {
            DetailContent(DetailItem.Ep(work), zone, onSave = { _, _, _, _ -> }, onDelete = {}, onConfirm = {}, onRetype = {})
        }
        assertTrue(file.exists())
    }
}
