package com.oblonian.stracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import com.oblonian.stracker.domain.EpisodeType
import com.oblonian.stracker.ui.theme.BAR_ORDER
import com.oblonian.stracker.ui.theme.color

/**
 * How a day's hours split up, as one thin bar. Segments follow [BAR_ORDER]; time that was
 * nothing in particular stays as the quiet track colour.
 */
@Composable
fun DayBar(
    millisByType: Map<EpisodeType, Long>,
    totalMillis: Long,
    modifier: Modifier = Modifier,
) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(modifier.fillMaxWidth().height(6.dp)) {
        val radius = CornerRadius(size.height / 2)
        val clip = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, size.width, size.height, radius)) }
        clipPath(clip) {
            drawRect(track)
            if (totalMillis <= 0) return@clipPath
            var x = 0f
            for (type in BAR_ORDER) {
                val ms = millisByType[type] ?: continue
                val w = size.width * (ms.toFloat() / totalMillis)
                drawRect(type.color, Offset(x, 0f), Size(w, size.height))
                x += w
            }
        }
    }
}
