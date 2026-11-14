package com.oblonian.stracker.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oblonian.stracker.domain.WorkingHours
import com.oblonian.stracker.ui.AppViewModelProvider
import com.oblonian.stracker.ui.components.StrackerCard
import com.oblonian.stracker.ui.components.TimePickerField
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun WorkingHoursScreen(onBack: () -> Unit) {
    val viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val s = settings ?: return
    WorkingHoursContent(s.workingHours, onChange = viewModel::setWorkingHours, onCopyToWeekdays = viewModel::copyMondayToWeekdays, onBack = onBack)
}

/** Seven rows: a switch for a working day, start and end pickers. */
@Composable
fun WorkingHoursContent(
    hours: Map<DayOfWeek, WorkingHours?>,
    onChange: (DayOfWeek, WorkingHours?) -> Unit,
    onCopyToWeekdays: () -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Working hours", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Text(
            "Time inside these hours, with the phone idle and you not moving, counts as work.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        StrackerCard(modifier = Modifier.padding(16.dp), contentPadding = 12.dp) {
            DayOfWeek.entries.forEach { day ->
                val h = hours[day]
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = h != null, onCheckedChange = { on -> onChange(day, if (on) WorkingHours(LocalTime.of(9, 0), LocalTime.of(18, 0)) else null) })
                    Spacer(Modifier.width(10.dp))
                    Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    if (h != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TimePickerField("Start", h.start, { onChange(day, h.copy(start = it)) })
                            TimePickerField("End", h.end, { onChange(day, h.copy(end = it)) })
                        }
                    } else {
                        Text("day off", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            TextButton(onClick = onCopyToWeekdays, modifier = Modifier.align(Alignment.End)) { Text("Copy Monday to all weekdays") }
        }
    }
}
