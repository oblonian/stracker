package com.oblonian.stracker.ui.day

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oblonian.stracker.data.ButtonKind
import com.oblonian.stracker.data.QuickLogButton
import com.oblonian.stracker.domain.Episode
import com.oblonian.stracker.domain.formatClock
import com.oblonian.stracker.ui.theme.color
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** The one-tap logging sheet (spec §6.5 screen 3). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickLogSheet(
    buttons: List<QuickLogButton>,
    ongoing: List<Episode>,
    at: Instant?,
    now: Instant,
    zone: ZoneId,
    onTap: (QuickLogButton) -> Unit,
    onLongPress: (QuickLogButton) -> Unit,
    onAddCustom: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        QuickLogGrid(buttons, ongoing, at, now, zone, onTap, onLongPress, onAddCustom)
        Spacer(Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuickLogGrid(
    buttons: List<QuickLogButton>,
    ongoing: List<Episode>,
    at: Instant?,
    now: Instant,
    zone: ZoneId,
    onTap: (QuickLogButton) -> Unit,
    onLongPress: (QuickLogButton) -> Unit,
    onAddCustom: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            if (at == null) "Log now · tap once" else "Log at ${formatClock(at, zone)} · tap once",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            textAlign = TextAlign.Center,
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = false,
            modifier = Modifier.fillMaxWidth().height(((buttons.size + 1 + 3) / 4 * 80).dp),
        ) {
            items(buttons, key = { it.id }) { b ->
                val running = if (b.kind == ButtonKind.EPISODE) ongoing.firstOrNull { it.type == b.episodeType && it.customName == b.customName } else null
                QuickLogButtonTile(
                    emoji = b.emoji,
                    label = running?.let { "${Duration.between(it.start, now).toMinutes()} min" } ?: b.label,
                    tint = running?.let { b.episodeType!!.color },
                    onClick = { onTap(b) },
                    onLongClick = { onLongPress(b) },
                )
            }
            item(key = "custom") {
                QuickLogButtonTile(emoji = "＋", label = "custom", tint = null, onClick = onAddCustom, onLongClick = onAddCustom)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickLogButtonTile(emoji: String, label: String, tint: Color?, onClick: () -> Unit, onLongClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .height(72.dp)
            .clip(shape)
            .background(tint ?: MaterialTheme.colorScheme.surfaceContainerHigh)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.height(2.dp))
        Text(
            label, fontSize = 11.sp, maxLines = 1,
            fontWeight = if (tint != null) FontWeight.Bold else FontWeight.Medium,
            color = if (tint != null) Color(0xFF111111) else MaterialTheme.colorScheme.onSurface,
        )
    }
}
