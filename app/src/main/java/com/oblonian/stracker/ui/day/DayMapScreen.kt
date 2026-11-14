package com.oblonian.stracker.ui.day

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oblonian.stracker.ui.AppViewModelProvider
import com.oblonian.stracker.ui.components.EmptyState
import com.oblonian.stracker.ui.components.MapPoint
import com.oblonian.stracker.ui.components.RouteMap
import java.time.Duration

/** The day's path on OpenStreetMap; a gap of more than 20 minutes between fixes starts a new line. */
@Composable
fun DayMapScreen(onBack: () -> Unit) {
    val viewModel: DayViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val data by viewModel.data.collectAsStateWithLifecycle()
    val path = data?.path ?: emptyList()
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Where you went", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        if (path.size < 2) {
            EmptyState(emoji = "🗺️", title = "No path yet", body = "The path is recorded while you move, once location is allowed all the time.")
            return
        }
        var segment = 0
        val points = path.mapIndexed { i, p ->
            if (i > 0 && Duration.between(path[i - 1].time, p.time) > Duration.ofMinutes(20)) segment++
            MapPoint(p.lat, p.lon, segment)
        }
        Box(Modifier.fillMaxSize()) { RouteMap(points = points, modifier = Modifier.fillMaxSize()) }
    }
}
