package com.oblonian.stracker.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.oblonian.stracker.collect.UsagePoller

/** What the app needs, in the order onboarding asks for it, each with its one-line reason. */
enum class PermissionKind(val title: String, val reason: String) {
    LOCATION("Location, all the time", "to draw where you went"),
    ACTIVITY("Physical activity", "to tell walking from sitting and to count steps"),
    USAGE_ACCESS("Usage access", "to measure time per app"),
    NOTIFICATIONS("Notifications", "Android requires one quiet notice while tracking"),
    BATTERY("Run in the background", "realme phones stop apps at night without this"),
}

data class PermissionStatus(val kind: PermissionKind, val granted: Boolean, val fix: () -> Intent) {
    /** Runtime permissions the system dialog can ask for; empty for the ones that need a settings page. */
    val runtime: List<String>
        get() = when (kind) {
            PermissionKind.LOCATION -> listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            PermissionKind.ACTIVITY -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) listOf(Manifest.permission.ACTIVITY_RECOGNITION) else emptyList()
            PermissionKind.NOTIFICATIONS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) listOf(Manifest.permission.POST_NOTIFICATIONS) else emptyList()
            else -> emptyList()
        }
}

object Permissions {

    fun current(context: Context): List<PermissionStatus> = listOf(
        PermissionStatus(PermissionKind.LOCATION, hasLocation(context)) { appDetails(context) },
        PermissionStatus(PermissionKind.ACTIVITY, hasActivity(context)) { appDetails(context) },
        PermissionStatus(PermissionKind.USAGE_ACCESS, UsagePoller.hasPermission(context)) { Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS) },
        PermissionStatus(PermissionKind.NOTIFICATIONS, hasNotifications(context)) { notificationSettings(context) },
        PermissionStatus(PermissionKind.BATTERY, ignoresBatteryOptimisation(context)) {
            @Suppress("BatteryLife")
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
        },
    )

    fun hasLocation(context: Context): Boolean {
        val fine = granted(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val background = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || granted(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        return fine && background
    }

    fun hasActivity(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || granted(context, Manifest.permission.ACTIVITY_RECOGNITION)

    fun hasNotifications(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) granted(context, Manifest.permission.POST_NOTIFICATIONS)
        else NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun ignoresBatteryOptimisation(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) == true

    private fun granted(context: Context, permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun appDetails(context: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))

    private fun notificationSettings(context: Context) =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
}
