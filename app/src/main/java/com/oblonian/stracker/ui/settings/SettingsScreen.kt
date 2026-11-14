package com.oblonian.stracker.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oblonian.stracker.BuildConfig
import com.oblonian.stracker.data.UserSettings
import com.oblonian.stracker.ui.AppViewModelProvider
import com.oblonian.stracker.ui.components.SectionTitle
import com.oblonian.stracker.ui.components.SettingRow
import com.oblonian.stracker.ui.components.StrackerCard
import com.oblonian.stracker.ui.components.TimePickerField
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class SettingsActions(
    val onBack: () -> Unit = {},
    val onWorkingHours: () -> Unit = {},
    val onSleepWindow: () -> Unit = {},
    val onPlaces: () -> Unit = {},
    val onButtons: () -> Unit = {},
    val onFix: (PermissionStatus) -> Unit = {},
    val onBackup: () -> Unit = {},
    val onRestore: () -> Unit = {},
)

@Composable
fun SettingsScreen(onBack: () -> Unit, onWorkingHours: () -> Unit, onPlaces: () -> Unit, onButtons: () -> Unit, onBackup: () -> Unit, onRestore: () -> Unit) {
    val viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var sleepDialog by remember { mutableStateOf(false) }

    val runtimeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { viewModel.refreshPermissions() }
    val settingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { viewModel.refreshPermissions() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPermissions() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val s = settings ?: return
    SettingsContent(
        settings = s, permissions = permissions, lastBackup = s.lastBackupAt,
        actions = SettingsActions(
            onBack = onBack, onWorkingHours = onWorkingHours, onSleepWindow = { sleepDialog = true },
            onPlaces = onPlaces, onButtons = onButtons,
            onFix = { p ->
                if (p.runtime.isNotEmpty() && !p.granted && p.kind != PermissionKind.LOCATION) runtimeLauncher.launch(p.runtime.toTypedArray())
                else runCatching { settingsLauncher.launch(p.fix()) }.onFailure { runCatching { context.startActivity(p.fix()) } }
            },
            onBackup = onBackup, onRestore = onRestore,
        ),
    )
    if (sleepDialog) {
        SleepWindowDialog(s.sleepWindowStart, s.sleepWindowEnd, onSave = { a, b -> viewModel.setSleepWindow(a, b); sleepDialog = false }, onDismiss = { sleepDialog = false })
    }
}

private val backupFormat = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.getDefault())

@Composable
fun SettingsContent(settings: UserSettings, permissions: List<PermissionStatus>, lastBackup: Instant?, actions: SettingsActions) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = actions.onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StrackerCard(contentPadding = 6.dp) {
                val workdays = settings.workingHours.count { it.value != null }
                val hours = settings.workingHours[DayOfWeek.MONDAY]
                SettingRow(Icons.Outlined.Schedule, "Working hours",
                    subtitle = if (hours != null) "$workdays days · ${hours.start}–${hours.end}" else "$workdays days", onClick = actions.onWorkingHours)
                SettingRow(Icons.Outlined.Bedtime, "Sleep window",
                    subtitle = "Looked for between ${settings.sleepWindowStart} and ${settings.sleepWindowEnd}", onClick = actions.onSleepWindow)
                SettingRow(Icons.Outlined.Place, "Places", subtitle = "Home, work and anywhere else you want named", onClick = actions.onPlaces)
                SettingRow(Icons.Outlined.Apps, "Quick-log buttons", subtitle = "What the + sheet offers", onClick = actions.onButtons)
            }

            SectionTitle("Permissions")
            StrackerCard(contentPadding = 6.dp) {
                permissions.forEach { p ->
                    SettingRow(
                        icon = if (p.granted) Icons.Filled.Check else Icons.Outlined.Info,
                        title = p.kind.title, subtitle = p.kind.reason,
                        tint = if (p.granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        onClick = if (p.granted) null else { { actions.onFix(p) } },
                        trailing = if (p.granted) null else { { TextButton(onClick = { actions.onFix(p) }) { Text("Fix") } } },
                    )
                }
            }

            SectionTitle("Backup")
            StrackerCard(contentPadding = 6.dp) {
                SettingRow(Icons.Outlined.Save, "Back up to a file",
                    subtitle = lastBackup?.let { "Last backup ${backupFormat.format(it.atZone(ZoneId.systemDefault()))}" } ?: "Never backed up", onClick = actions.onBackup)
                SettingRow(Icons.Outlined.Restore, "Restore from a file", subtitle = "Replaces everything on this phone", onClick = actions.onRestore)
            }

            SectionTitle("About")
            StrackerCard(contentPadding = 6.dp) {
                SettingRow(Icons.Outlined.Info, "stracker ${BuildConfig.VERSION_NAME}", subtitle = "Everything stays on this phone. Nothing is sent anywhere.")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SleepWindowDialog(start: LocalTime, end: LocalTime, onSave: (LocalTime, LocalTime) -> Unit, onDismiss: () -> Unit) {
    var a by remember { mutableStateOf(start) }
    var b by remember { mutableStateOf(end) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep window") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("The night's main sleep is looked for between these times.", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TimePickerField("From", a, { a = it })
                    TimePickerField("To", b, { b = it })
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(a, b) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
