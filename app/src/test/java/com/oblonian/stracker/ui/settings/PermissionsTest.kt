package com.oblonian.stracker.ui.settings

import android.app.Application
import android.app.AppOpsManager
import android.content.Context
import android.os.Process
import android.os.PowerManager
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PermissionsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun setUsageAccess(mode: Int) {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        shadowOf(appOps).setMode(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName, mode)
    }

    @Test
    fun `usage access follows the app op and its fix opens usage settings`() {
        setUsageAccess(AppOpsManager.MODE_IGNORED)
        val denied = Permissions.current(context).first { it.kind == PermissionKind.USAGE_ACCESS }
        assertFalse(denied.granted)
        assertEquals(Settings.ACTION_USAGE_ACCESS_SETTINGS, denied.fix().action)
        setUsageAccess(AppOpsManager.MODE_ALLOWED)
        assertTrue(Permissions.current(context).first { it.kind == PermissionKind.USAGE_ACCESS }.granted)
    }

    @Test
    fun `battery status reflects power manager`() {
        val before = Permissions.current(context).first { it.kind == PermissionKind.BATTERY }
        assertFalse(before.granted)
        shadowOf(context.getSystemService(PowerManager::class.java)).setIgnoringBatteryOptimizations(context.packageName, true)
        val after = Permissions.current(context).first { it.kind == PermissionKind.BATTERY }
        assertTrue(after.granted)
        assertEquals(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, before.fix().action)
    }

    @Test
    fun `all five permissions are listed in onboarding order`() {
        assertEquals(
            listOf(PermissionKind.LOCATION, PermissionKind.ACTIVITY, PermissionKind.USAGE_ACCESS, PermissionKind.NOTIFICATIONS, PermissionKind.BATTERY),
            Permissions.current(context).map { it.kind },
        )
    }
}
