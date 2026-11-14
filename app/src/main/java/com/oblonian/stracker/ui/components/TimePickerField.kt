package com.oblonian.stracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalTime

/** A labelled clock time that opens a 24-hour picker. [onClear] adds a third button (e.g. "Ongoing"). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerField(
    label: String,
    value: LocalTime?,
    onChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "—",
    clearLabel: String? = null,
    onClear: (() -> Unit)? = null,
) {
    var open by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = { open = true }) { Text(value?.let { "%02d:%02d".format(it.hour, it.minute) } ?: placeholder) }
    }
    if (open) {
        val base = value ?: LocalTime.of(9, 0)
        val state = rememberTimePickerState(initialHour = base.hour, initialMinute = base.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(label) },
            text = { TimePicker(state) },
            confirmButton = { TextButton(onClick = { onChange(LocalTime.of(state.hour, state.minute)); open = false }) { Text("OK") } },
            dismissButton = {
                Row {
                    if (onClear != null && clearLabel != null) TextButton(onClick = { onClear(); open = false }) { Text(clearLabel) }
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = { open = false }) { Text("Cancel") }
                }
            },
        )
    }
}
