package com.oblonian.stracker.ui.day

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oblonian.stracker.domain.Confidence
import com.oblonian.stracker.domain.Detail
import com.oblonian.stracker.domain.Episode
import com.oblonian.stracker.domain.EpisodeType
import com.oblonian.stracker.domain.Event
import com.oblonian.stracker.domain.EventType
import com.oblonian.stracker.domain.Source
import com.oblonian.stracker.domain.formatClock
import com.oblonian.stracker.ui.components.Pill
import com.oblonian.stracker.ui.theme.color
import com.oblonian.stracker.ui.theme.displayName
import com.oblonian.stracker.ui.theme.emoji
import java.time.Instant
import java.time.ZoneId

sealed interface DetailItem {
    data class Ep(val episode: Episode) : DetailItem
    data class Ev(val event: Event) : DetailItem
}

/** Edit one episode or event: type, times, severity, note; delete; confirm an automatic block. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailSheet(
    item: DetailItem,
    zone: ZoneId,
    onSave: (start: Instant, end: Instant?, detail: Detail?, customName: String?) -> Unit,
    onDelete: () -> Unit,
    onConfirm: () -> Unit,
    onRetype: (Enum<*>) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        DetailContent(item, zone, onSave = { s, e, d, c -> onSave(s, e, d, c); onDismiss() },
            onDelete = { onDelete(); onDismiss() }, onConfirm = { onConfirm(); onDismiss() }, onRetype = onRetype)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun DetailContent(
    item: DetailItem,
    zone: ZoneId,
    onSave: (start: Instant, end: Instant?, detail: Detail?, customName: String?) -> Unit,
    onDelete: () -> Unit,
    onConfirm: () -> Unit,
    onRetype: (Enum<*>) -> Unit,
) {
    val episode = (item as? DetailItem.Ep)?.episode
    val event = (item as? DetailItem.Ev)?.event
    val initialDetail = episode?.detail ?: event?.detail
    var start by remember(item) { mutableStateOf(episode?.start ?: event!!.time) }
    var end by remember(item) { mutableStateOf(episode?.end) }
    var severity by remember(item) { mutableStateOf(initialDetail?.severity) }
    var note by remember(item) { mutableStateOf(initialDetail?.note ?: "") }
    var customName by remember(item) { mutableStateOf(episode?.customName ?: event?.customName ?: "") }
    val isCustom = episode?.type == EpisodeType.CUSTOM || event?.type == EventType.CUSTOM

    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                episode?.let { it.label ?: it.customName ?: it.type.displayName } ?: "${event!!.type.emoji} ${event.customName ?: event.type.name.lowercase()}",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
            )
            episode?.let {
                val text = when {
                    it.source == Source.MANUAL -> "logged by you"
                    it.source == Source.AUTO_EDITED -> "corrected"
                    it.confidence == Confidence.LOW -> "guessed"
                    else -> "detected"
                }
                Pill(text = text, containerColor = it.type.color.copy(alpha = 0.18f), contentColor = MaterialTheme.colorScheme.onSurface)
            }
        }

        // Type
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (episode != null) {
                EpisodeType.entries.filter { it != EpisodeType.PHONE }.forEach { t ->
                    FilterChip(selected = t == episode.type, onClick = { onRetype(t) }, label = { Text(t.displayName) })
                }
            } else {
                EventType.entries.forEach { t ->
                    FilterChip(selected = t == event!!.type, onClick = { onRetype(t) }, label = { Text("${t.emoji} ${t.name.lowercase()}") })
                }
            }
        }
        if (isCustom) {
            OutlinedTextField(customName, { customName = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }

        // Times
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TimeField(if (episode != null) "Start" else "Time", start, zone) { it?.let { v -> start = v } }
            if (episode != null) {
                TimeField("End", end, zone, allowOngoing = true) { end = it }
            }
        }

        // Severity
        Text("How bad", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            (1..5).forEach { i ->
                SegmentedButton(
                    selected = severity == i,
                    onClick = { severity = if (severity == i) null else i },
                    shape = SegmentedButtonDefaults.itemShape(index = i - 1, count = 5),
                ) { Text(i.toString()) }
            }
        }

        OutlinedTextField(note, { note = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth(), minLines = 2)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.weight(1f))
            if (episode?.source == Source.AUTO) {
                OutlinedButton(onClick = onConfirm) { Text("Confirm") }
            }
            Button(onClick = {
                val detail = if (severity == null && note.isBlank()) null else Detail(severity, note.ifBlank { null })
                onSave(start, end, detail, customName.ifBlank { null })
            }) { Text("Save") }
        }
    }
}

/** A tappable clock time that opens a time picker; "ongoing" when an episode has no end yet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeField(label: String, value: Instant?, zone: ZoneId, allowOngoing: Boolean = false, onChange: (Instant?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = { open = true }) { Text(value?.let { formatClock(it, zone) } ?: "ongoing") }
    }
    if (open) {
        val base = (value ?: Instant.now()).atZone(zone)
        val state = rememberTimePickerState(initialHour = base.hour, initialMinute = base.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(label) },
            text = { TimePicker(state) },
            confirmButton = {
                TextButton(onClick = {
                    onChange(base.withHour(state.hour).withMinute(state.minute).withSecond(0).withNano(0).toInstant())
                    open = false
                }) { Text("OK") }
            },
            dismissButton = {
                Row {
                    if (allowOngoing) TextButton(onClick = { onChange(null); open = false }) { Text("Ongoing") }
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = { open = false }) { Text("Cancel") }
                }
            },
        )
    }
}
