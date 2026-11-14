package com.oblonian.stracker.ui.day

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.oblonian.stracker.data.ButtonKind
import com.oblonian.stracker.data.QuickLogButton
import com.oblonian.stracker.domain.EpisodeType
import com.oblonian.stracker.domain.EventType
import com.oblonian.stracker.ui.theme.displayName
import com.oblonian.stracker.ui.theme.emoji

/** Add or edit one quick-log button: label, emoji, moment or stretch, and what it logs. */
@Composable
fun ButtonEditDialog(
    initial: QuickLogButton?,
    nextSortOrder: Int,
    onSave: (QuickLogButton) -> Unit,
    onHide: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var label by remember { mutableStateOf(initial?.label ?: "") }
    var emoji by remember { mutableStateOf(initial?.emoji ?: "✨") }
    var kind by remember { mutableStateOf(initial?.kind ?: ButtonKind.EVENT) }
    var eventType by remember { mutableStateOf(initial?.eventType ?: EventType.CUSTOM) }
    var episodeType by remember { mutableStateOf(initial?.episodeType ?: EpisodeType.CUSTOM) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New button" else "Edit button") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(emoji, { emoji = it.take(4) }, label = { Text("Icon") }, singleLine = true, modifier = Modifier.width(80.dp))
                    OutlinedTextField(label, { label = it }, label = { Text("Label") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(kind == ButtonKind.EVENT, { kind = ButtonKind.EVENT }, SegmentedButtonDefaults.itemShape(0, 2)) { Text("a moment") }
                    SegmentedButton(kind == ButtonKind.EPISODE, { kind = ButtonKind.EPISODE }, SegmentedButtonDefaults.itemShape(1, 2)) { Text("a stretch") }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (kind == ButtonKind.EVENT) {
                        EventType.entries.forEach { t ->
                            FilterChip(selected = t == eventType, onClick = { eventType = t }, label = { Text("${t.emoji} ${t.name.lowercase()}") })
                        }
                    } else {
                        EpisodeType.entries.filter { it != EpisodeType.PHONE }.forEach { t ->
                            FilterChip(selected = t == episodeType, onClick = { episodeType = t }, label = { Text(t.displayName) })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = label.isNotBlank(),
                onClick = {
                    val isCustom = (kind == ButtonKind.EVENT && eventType == EventType.CUSTOM) || (kind == ButtonKind.EPISODE && episodeType == EpisodeType.CUSTOM)
                    onSave(
                        QuickLogButton(
                            id = initial?.id ?: 0, label = label.trim(), emoji = emoji.ifBlank { "✨" }, kind = kind,
                            eventType = if (kind == ButtonKind.EVENT) eventType else null,
                            episodeType = if (kind == ButtonKind.EPISODE) episodeType else null,
                            customName = if (isCustom) label.trim().lowercase() else null,
                            sortOrder = initial?.sortOrder ?: nextSortOrder,
                        )
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (onHide != null) TextButton(onClick = onHide) { Text("Hide") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
