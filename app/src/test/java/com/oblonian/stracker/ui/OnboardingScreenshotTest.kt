package com.oblonian.stracker.ui

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.RoborazziComposeOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.size
import com.oblonian.stracker.data.UserSettings
import com.oblonian.stracker.ui.onboarding.OnboardingActions
import com.oblonian.stracker.ui.onboarding.OnboardingContent
import com.oblonian.stracker.ui.settings.PermissionKind
import com.oblonian.stracker.ui.settings.PermissionStatus
import com.oblonian.stracker.ui.theme.StrackerTheme
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w412dp-h915dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OnboardingScreenshotTest {

    private fun shoot(name: String, content: @Composable () -> Unit): File {
        val file = File("build/screenshots/$name.png")
        captureRoboImage(
            filePath = file.path,
            roborazziComposeOptions = RoborazziComposeOptions { size(widthDp = 412, heightDp = 915) },
        ) { StrackerTheme { content() } }
        return file
    }

    private val permissions = PermissionKind.entries.mapIndexed { i, k -> PermissionStatus(k, granted = i < 2, fix = { Intent() }) }

    @Test
    fun `three pages`() {
        val files = (0..2).map { page ->
            shoot("onboarding-${page + 1}") {
                OnboardingContent(page = page, permissions = permissions, hours = UserSettings().workingHours, actions = OnboardingActions())
            }
        }
        assertTrue(files.all { it.exists() })
    }
}
