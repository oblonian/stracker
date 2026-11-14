package com.oblonian.stracker.ui

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.RoborazziComposeOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.size
import com.oblonian.stracker.data.UserSettings
import com.oblonian.stracker.domain.Place
import com.oblonian.stracker.domain.PlaceTag
import com.oblonian.stracker.ui.settings.PermissionKind
import com.oblonian.stracker.ui.settings.PermissionStatus
import com.oblonian.stracker.ui.settings.PlacesContent
import com.oblonian.stracker.ui.settings.SettingsActions
import com.oblonian.stracker.ui.settings.SettingsContent
import com.oblonian.stracker.ui.settings.WorkingHoursContent
import com.oblonian.stracker.ui.theme.StrackerTheme
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w412dp-h915dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsScreenshotTest {

    private fun shoot(name: String, content: @Composable () -> Unit): File {
        val file = File("build/screenshots/$name.png")
        captureRoboImage(
            filePath = file.path,
            roborazziComposeOptions = RoborazziComposeOptions { size(widthDp = 412, heightDp = 915) },
        ) { StrackerTheme { content() } }
        return file
    }

    private val permissions = PermissionKind.entries.mapIndexed { i, k ->
        PermissionStatus(k, granted = i != 2, fix = { Intent() })
    }

    @Test
    fun `settings`() {
        val file = shoot("settings") {
            SettingsContent(UserSettings(), permissions, lastBackup = Instant.parse("2026-11-02T06:10:00Z"), actions = SettingsActions())
        }
        assertTrue(file.exists())
    }

    @Test
    fun `working hours`() {
        val file = shoot("working-hours") { WorkingHoursContent(UserSettings().workingHours, onChange = { _, _ -> }, onCopyToWeekdays = {}, onBack = {}) }
        assertTrue(file.exists())
    }

    @Test
    fun `places`() {
        val places = listOf(
            Place(1, "home", 12.9716, 77.5946, 150.0, PlaceTag.HOME),
            Place(2, "lab", 12.9816, 77.5946, 200.0, PlaceTag.WORK),
            Place(3, "gym", 12.9650, 77.6000, 100.0),
        )
        val file = shoot("places") { PlacesContent(places, onAdd = {}, onEdit = {}, onBack = {}) }
        assertTrue(file.exists())
    }
}
