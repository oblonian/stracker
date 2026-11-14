package com.oblonian.stracker.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oblonian.stracker.data.DaySummary
import com.oblonian.stracker.ui.components.DayBar
import com.oblonian.stracker.ui.theme.color
import java.time.LocalDate

private const val DAY_MILLIS = 24 * 3_600_000L

/** One day on the month grid: the number, up to three event dots, a band for multi-day episodes, the bar. */
@Composable
fun DayCell(
    day: LocalDate,
    summary: DaySummary?,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = modifier
            .height(56.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .then(if (isToday) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 5.dp, vertical = 4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Text(
                day.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            summary?.eventTypes?.take(3)?.forEach { type ->
                Box(
                    Modifier.padding(start = 2.dp, top = 3.dp).size(5.dp).clip(CircleShape).background(type.color),
                )
            }
        }
        summary?.bands?.firstOrNull()?.let { band ->
            Box(
                Modifier.padding(top = 2.dp).fillMaxWidth(0.45f).height(2.dp)
                    .clip(CircleShape).background(band.color),
            )
        }
        Spacer(Modifier.weight(1f))
        DayBar(summary?.millisByType ?: emptyMap(), DAY_MILLIS)
    }
}

/** Keeps the grid aligned where a month doesn't start on Monday. */
@Composable
fun EmptyCell(modifier: Modifier = Modifier) {
    Box(modifier.height(56.dp).fillMaxSize())
}

@Suppress("unused")
private val spacing = Arrangement.spacedBy(4.dp)
