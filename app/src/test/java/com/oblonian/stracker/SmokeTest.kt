package com.oblonian.stracker

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SmokeTest {
    @Test
    fun `package is renamed`() {
        assertEquals("com.oblonian.stracker", ApplicationProvider.getApplicationContext<Context>().packageName)
    }
}
