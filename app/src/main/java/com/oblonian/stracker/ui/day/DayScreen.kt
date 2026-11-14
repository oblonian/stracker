package com.oblonian.stracker.ui.day

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oblonian.stracker.data.QuickLogButton
import com.oblonian.stracker.domain.DayData
import com.oblonian.stracker.domain.Episode
import com.oblonian.stracker.domain.EpisodeType
import com.oblonian.stracker.domain.Event
import com.oblonian.stracker.domain.Span
import com.oblonian.stracker.domain.formatHoursMinutes
import com.oblonian.stracker.ui.AppViewModelProvider
import com.oblonian.stracker.ui.components.StatBlock
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Everything the day view can ask for; defaults do nothing so screenshots need no wiring. */
data class DayActions(
    val onPrev: () -> Unit = {},
    val onNext: () -> Unit = {},
    val onMap: () -> Unit = {},
    val onApps: () -> Unit = {},
    val onEpisode: (Episode) -> Unit = {},
    val onEvent: (Event) -> Unit = {},
    /** Open the quick-log sheet, at a time when long-pressed on the timeline. */
    val onAdd: (Instant?) -> Unit = {},
)

@Composable
fun DayScreen(onNavigateDay: (LocalDate) -> Unit, onApps: () -> Unit, onMap: () -> Unit) {
    val viewModel: DayViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val data by viewModel.data.collectAsStateWithLifecycle()
    val buttons by viewModel.buttons.collectAsStateWithLifecycle()
    val ongoing by viewModel.ongoing.collectAsStateWithLifecycle()
    val lastAction by viewModel.lastAction.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var sheetOpen by remember { mutableStateOf(false) }
    var sheetAt by remember { mutableStateOf<Instant?>(null) }
    var detail by remember { mutableStateOf<DetailItem?>(null) }
    var editButton by remember { mutableStateOf<QuickLogButton?>(null) }
    var newButton by remember { mutableStateOf(false) }

    LaunchedEffect(lastAction) {
        val action = lastAction ?: return@LaunchedEffect
        snackbar.showSnackbar(action.message, duration = SnackbarDuration.Short)
    }

    val d = data ?: run {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }

    Box(Modifier.fillMaxSize()) {
        DayContent(
            data = d, zone = viewModel.zone, now = viewModel.now(),
            actions = DayActions(
                onPrev = { onNavigateDay(viewModel.date.minusDays(1)) },
                onNext = { onNavigateDay(viewModel.date.plusDays(1)) },
                onMap = onMap, onApps = onApps,
                onEpisode = { detail = DetailItem.Ep(it) },
                onEvent = { detail = DetailItem.Ev(it) },
                onAdd = { at -> sheetAt = at; sheetOpen = true },
            ),
        )
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 88.dp)) { snackData ->
            Snackbar(
                action = {
                    Row {
                        TextButton(onClick = {
                            snackData.dismiss()
                            scope.launch { viewModel.lastItem()?.let { detail = it } }
                        }) { Text("Add details") }
                        TextButton(onClick = { snackData.dismiss(); viewModel.undoLast() }) { Text("Undo") }
                    }
                },
            ) { Text(snackData.visuals.message) }
        }
    }

    if (sheetOpen) {
        QuickLogSheet(
            buttons = buttons, ongoing = ongoing, at = sheetAt, now = viewModel.now(), zone = viewModel.zone,
            onTap = { b -> viewModel.tap(b, sheetAt); sheetOpen = false },
            onLongPress = { b -> editButton = b },
            onAddCustom = { newButton = true },
            onDismiss = { sheetOpen = false },
        )
    }
    detail?.let { item ->
        DetailSheet(
            item = item, zone = viewModel.zone,
            onSave = { s, e, det, name -> viewModel.save(item, s, e, det, name) },
            onDelete = { viewModel.delete(item) },
            onConfirm = { viewModel.confirm(item) },
            onRetype = { t -> viewModel.retype(item, t); detail = null },
            onDismiss = { detail = null },
        )
    }
    if (editButton != null || newButton) {
        val initial = editButton
        ButtonEditDialog(
            initial = initial,
            nextSortOrder = buttons.size,
            onSave = { viewModel.saveButton(it); editButton = null; newButton = false },
            onHide = initial?.let { b -> { viewModel.hideButton(b.id); editButton = null } },
            onDismiss = { editButton = null; newButton = false },
        )
    }
}

private val headerFormat = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())

@Composable
fun DayContent(data: DayData, zone: ZoneId, now: Instant, actions: DayActions) {
    var drag by remember { mutableFloatStateOf(0f) }
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        Column(
            Modifier.fillMaxSize().pointerInput(data.window.day) {
                detectHorizontalDragGestures(
                    onDragStart = { drag = 0f },
                    onHorizontalDrag = { _, dx -> drag += dx },
                    onDragEnd = { if (drag < -120f) actions.onNext(); if (drag > 120f) actions.onPrev() },
                )
            },
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = actions.onPrev) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous day") }
                Text(
                    headerFormat.format(data.window.day),
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                )
                IconButton(onClick = actions.onNext) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next day") }
                IconButton(onClick = actions.onMap) { Icon(Icons.Outlined.Map, contentDescription = "Map of the day") }
            }

            val totals = totals(data, now)
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)) {
                StatBlock("Sleep", formatHoursMinutes(totals[EpisodeType.SLEEP] ?: 0L), Modifier.weight(1f))
                StatBlock("Phone", formatHoursMinutes(totals[EpisodeType.PHONE] ?: 0L), Modifier.weight(1f).clickable(onClick = actions.onApps))
                StatBlock("Work", formatHoursMinutes(totals[EpisodeType.WORK] ?: 0L), Modifier.weight(1f))
                StatBlock("Steps", "%,d".format(data.steps), Modifier.weight(1f))
            }
            Spacer(Modifier.height(4.dp))

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
                Timeline(
                    window = data.window, episodes = data.episodes, events = data.events, zone = zone, now = now,
                    onEpisode = actions.onEpisode, onEvent = actions.onEvent, onLongPress = { actions.onAdd(it) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(88.dp))
            }
        }

        FloatingActionButton(
            onClick = { actions.onAdd(null) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
        ) { Icon(Icons.Filled.Add, contentDescription = "Log something") }
    }
}

/** Millis per type inside the window; an ongoing manual block counts up to now. */
internal fun totals(data: DayData, now: Instant): Map<EpisodeType, Long> {
    val span = Span(data.window.start, data.window.end)
    val out = HashMap<EpisodeType, Long>()
    for (e in data.episodes) {
        Span(e.start, e.end ?: now).clip(span)?.let { out.merge(e.type, it.millis, Long::plus) }
    }
    return out
}
