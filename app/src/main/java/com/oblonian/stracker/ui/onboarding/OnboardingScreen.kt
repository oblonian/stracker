package com.oblonian.stracker.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oblonian.stracker.data.UserSettings
import com.oblonian.stracker.domain.WorkingHours
import com.oblonian.stracker.ui.AppViewModelProvider
import com.oblonian.stracker.ui.components.StrackerCard
import com.oblonian.stracker.ui.components.TimePickerField
import com.oblonian.stracker.ui.settings.PermissionKind
import com.oblonian.stracker.ui.settings.PermissionStatus
import com.oblonian.stracker.ui.settings.Permissions
import com.oblonian.stracker.ui.settings.SettingsViewModel
import com.oblonian.stracker.ui.theme.HeroGradient
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

data class OnboardingActions(
    val onNext: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onRequest: (PermissionStatus) -> Unit = {},
    val onHoursChange: (DayOfWeek, WorkingHours?) -> Unit = { _, _ -> },
    val onCopyToWeekdays: () -> Unit = {},
    val onStart: () -> Unit = {},
)

/** Three pages: what the app does, the permissions one by one with a reason each, working hours. */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var page by remember { mutableIntStateOf(0) }

    val backgroundLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { viewModel.refreshPermissions() }
    val runtimeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        viewModel.refreshPermissions()
        // Android asks for "all the time" location in a second step, after the foreground one.
        if (granted[Manifest.permission.ACCESS_FINE_LOCATION] == true && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !Permissions.hasLocation(context)) {
            backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }
    val settingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { viewModel.refreshPermissions() }

    OnboardingContent(
        page = page,
        permissions = permissions,
        hours = settings?.workingHours ?: UserSettings.DEFAULT_WORKING_HOURS,
        actions = OnboardingActions(
            onNext = { page = (page + 1).coerceAtMost(2) },
            onBack = { page = (page - 1).coerceAtLeast(0) },
            onRequest = { p ->
                if (p.runtime.isNotEmpty()) runtimeLauncher.launch(p.runtime.toTypedArray())
                else runCatching { settingsLauncher.launch(p.fix()) }
            },
            onHoursChange = viewModel::setWorkingHours,
            onCopyToWeekdays = viewModel::copyMondayToWeekdays,
            onStart = onDone,
        ),
    )
}

@Composable
fun OnboardingContent(
    page: Int,
    permissions: List<PermissionStatus>,
    hours: Map<DayOfWeek, WorkingHours?>,
    actions: OnboardingActions,
) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
                .background(HeroGradient)
                .padding(start = 28.dp, end = 28.dp, top = 64.dp, bottom = 32.dp),
        ) {
            Column {
                Text(
                    when (page) { 0 -> "Your days, mapped."; 1 -> "What stracker needs"; else -> "When do you work?" },
                    style = MaterialTheme.typography.headlineLarge, color = Color.White,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    when (page) {
                        0 -> "stracker records sleep, phone use, movement and places on its own, and lets you log the rest with one tap. Everything stays on this phone."
                        1 -> "Each one is optional. Whatever you skip just stays empty on the calendar; you can grant it later in Settings."
                        else -> "Time inside these hours, with the phone idle and you not moving, counts as work. You can change this any time."
                    },
                    style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.9f),
                )
            }
        }

        Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            when (page) {
                0 -> {
                    Feature("🗓️", "A calendar of your life", "Each day is a bar: sleep, work, phone, travel, exercise. Patterns show themselves.")
                    Feature("🔋", "Quiet and light", "No reminders, no tips, one silent notice from Android. GPS only runs while you move.")
                    Feature("🔒", "Yours alone", "Nothing is uploaded. Back up to a file whenever you like.")
                }
                1 -> StrackerCard(contentPadding = 8.dp) {
                    permissions.forEach { p -> PermissionRow(p, onRequest = { actions.onRequest(p) }) }
                }
                else -> StrackerCard(contentPadding = 12.dp) {
                    DayOfWeek.entries.forEach { day ->
                        val h = hours[day]
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Switch(
                                checked = h != null,
                                onCheckedChange = { on -> actions.onHoursChange(day, if (on) WorkingHours(LocalTime.of(9, 0), LocalTime.of(18, 0)) else null) },
                            )
                            Spacer(Modifier.size(10.dp))
                            Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            if (h != null) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TimePickerField("Start", h.start, { actions.onHoursChange(day, h.copy(start = it)) })
                                    TimePickerField("End", h.end, { actions.onHoursChange(day, h.copy(end = it)) })
                                }
                            } else {
                                Text("day off", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    TextButton(onClick = actions.onCopyToWeekdays, modifier = Modifier.align(Alignment.End)) { Text("Copy Monday to all weekdays") }
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (page > 0) TextButton(onClick = actions.onBack) { Text("Back") }
                Spacer(Modifier.weight(1f))
                PageDots(page)
                Spacer(Modifier.weight(1f))
                if (page == 1) TextButton(onClick = actions.onNext) { Text("Skip") }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(50))
                    .background(HeroGradient)
                    .clickable(role = Role.Button, onClick = if (page < 2) actions.onNext else actions.onStart),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (page < 2) "Next" else "Start", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun PermissionRow(p: PermissionStatus, onRequest: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(p.kind.title, style = MaterialTheme.typography.titleMedium)
            Text(p.kind.reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.size(12.dp))
        if (p.granted) {
            Icon(Icons.Filled.Check, contentDescription = "Granted", tint = MaterialTheme.colorScheme.primary)
        } else {
            OutlinedButton(onClick = onRequest) { Text(if (p.kind == PermissionKind.USAGE_ACCESS || p.kind == PermissionKind.BATTERY) "Open" else "Allow") }
        }
    }
}

@Composable
private fun PageDots(page: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) { i ->
            Box(
                Modifier.size(if (i == page) 10.dp else 7.dp).clip(CircleShape)
                    .background(if (i == page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
            )
        }
    }
}

@Composable
private fun Feature(emoji: String, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(emoji, style = MaterialTheme.typography.headlineSmall)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
