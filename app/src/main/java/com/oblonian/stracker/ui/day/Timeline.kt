package com.oblonian.stracker.ui.day

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oblonian.stracker.domain.Confidence
import com.oblonian.stracker.domain.DayWindow
import com.oblonian.stracker.domain.Episode
import com.oblonian.stracker.domain.Event
import com.oblonian.stracker.domain.EpisodeType
import com.oblonian.stracker.domain.Source
import com.oblonian.stracker.domain.formatClock
import com.oblonian.stracker.ui.theme.color
import com.oblonian.stracker.ui.theme.displayName
import com.oblonian.stracker.ui.theme.emoji
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Pixels per hour; a 24 h day is 24 × this tall. */
val HOUR_HEIGHT = 40.dp

/**
 * The day as a vertical strip from the window's start to its end: coloured blocks for episodes,
 * white chips on the right for logged moments. Low-confidence blocks are lighter with a dashed
 * edge; manual and corrected blocks have a solid edge; an ongoing manual block grows to now.
 */
@Composable
fun Timeline(
    window: DayWindow,
    episodes: List<Episode>,
    events: List<Event>,
    zone: ZoneId,
    now: Instant,
    onEpisode: (Episode) -> Unit,
    onEvent: (Event) -> Unit,
    onLongPress: (Instant) -> Unit,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val colors = MaterialTheme.colorScheme
    val labelStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111111))
    val hourStyle = TextStyle(fontSize = 10.sp, color = colors.onSurfaceVariant)
    val chipStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF111111))

    val totalMillis = (window.end.toEpochMilli() - window.start.toEpochMilli()).coerceAtLeast(1L)
    val heightDp = HOUR_HEIGHT * (totalMillis / 3_600_000f)
    val gutterPx = with(density) { 36.dp.toPx() }
    val rightPadPx = with(density) { 8.dp.toPx() }

    fun yOf(t: Instant, heightPx: Float) = ((t.toEpochMilli() - window.start.toEpochMilli()).toFloat() / totalMillis) * heightPx
    fun timeAt(y: Float, heightPx: Float): Instant =
        window.start.plusMillis(((y / heightPx).coerceIn(0f, 1f) * totalMillis).toLong())

    val layout = episodes.map { e -> e to (e.end ?: now) }

    Canvas(
        modifier
            .height(heightDp)
            .pointerInput(episodes, events) {
                detectTapGestures(
                    onTap = { p ->
                        val h = size.height.toFloat()
                        val hit = events.minByOrNull { kotlin.math.abs(yOf(it.time, h) - p.y) }
                            ?.takeIf { kotlin.math.abs(yOf(it.time, h) - p.y) < 14.dp.toPx() && p.x > size.width * 0.55f }
                        if (hit != null) { onEvent(hit); return@detectTapGestures }
                        layout.lastOrNull { (e, end) -> p.y >= yOf(e.start, h) && p.y <= yOf(end, h) }?.let { onEpisode(it.first) }
                    },
                    onLongPress = { p -> onLongPress(timeAt(p.y, size.height.toFloat()).truncatedTo(ChronoUnit.MINUTES)) },
                )
            },
    ) {
        val h = size.height
        val w = size.width
        // Hour grid
        val firstHour = window.start.atZone(zone).truncatedTo(ChronoUnit.HOURS).let { if (it.toInstant() < window.start) it.plusHours(1) else it }
        var tick = firstHour
        while (tick.toInstant() <= window.end) {
            val y = yOf(tick.toInstant(), h)
            if (tick.hour % 3 == 0) {
                drawLine(colors.outlineVariant.copy(alpha = 0.5f), Offset(gutterPx, y), Offset(w - rightPadPx, y), 1f)
                val text = measurer.measure("%02d".format(tick.hour), hourStyle)
                drawText(text, topLeft = Offset(gutterPx - text.size.width - 6.dp.toPx(), y - text.size.height / 2))
            }
            tick = tick.plusHours(1)
        }
        // Episode blocks
        for ((e, end) in layout) {
            val top = yOf(maxOf(e.start, window.start), h)
            val bottom = yOf(minOf(end, window.end), h)
            if (bottom <= top) continue
            val rect = Offset(gutterPx, top) to Size(w - gutterPx - rightPadPx, bottom - top)
            val low = e.confidence == Confidence.LOW && e.source == Source.AUTO
            val fill = if (low) e.type.color.copy(alpha = 0.55f) else e.type.color
            drawRoundRect(fill, rect.first, rect.second, CornerRadius(6.dp.toPx()))
            if (low) {
                drawRoundRect(e.type.color, rect.first, rect.second, CornerRadius(6.dp.toPx()),
                    style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))))
            } else if (e.source != Source.AUTO) {
                drawRoundRect(colors.onBackground, rect.first, rect.second, CornerRadius(6.dp.toPx()), style = Stroke(1.5.dp.toPx()))
            }
            if (bottom - top >= 25.dp.toPx() * 0.6f) {
                val text = measurer.measure(labelFor(e, end, zone, ongoing = e.end == null), labelStyle, maxLines = 1)
                if (text.size.width < rect.second.width - 12.dp.toPx()) {
                    drawText(text, topLeft = Offset(gutterPx + 8.dp.toPx(), top + (bottom - top - text.size.height) / 2))
                }
            }
        }
        // Event chips on the right edge
        for (ev in events) {
            val y = yOf(ev.time, h)
            val text = measurer.measure("${ev.type.emoji} ${ev.customName ?: ev.type.name.lowercase()}", chipStyle, maxLines = 1)
            val padX = 7.dp.toPx(); val padY = 3.dp.toPx()
            val chipW = text.size.width + padX * 2
            val chipH = text.size.height + padY * 2
            val left = w - rightPadPx - chipW - 4.dp.toPx()
            drawChip(left, y - chipH / 2, chipW, chipH, colors.onBackground.copy(alpha = 0.12f))
            drawText(text, topLeft = Offset(left + padX, y - chipH / 2 + padY))
            drawCircle(ev.type.color, 3.dp.toPx(), Offset(left - 6.dp.toPx(), y))
        }
    }
}

private fun DrawScope.drawChip(left: Float, top: Float, w: Float, hgt: Float, shadow: Color) {
    drawRoundRect(shadow, Offset(left, top + 1.5f), Size(w, hgt), CornerRadius(hgt / 2))
    drawRoundRect(Color.White, Offset(left, top), Size(w, hgt), CornerRadius(hgt / 2))
}

private fun labelFor(e: Episode, end: Instant, zone: ZoneId, ongoing: Boolean): String {
    val base = e.label ?: e.customName ?: e.type.displayName
    return when {
        e.type == EpisodeType.SLEEP -> "sleep ${formatClock(e.start, zone)} – ${formatClock(end, zone)}"
        ongoing -> "$base · ${Duration.between(e.start, end).toMinutes()} min…"
        else -> base
    }
}
