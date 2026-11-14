package com.oblonian.stracker.ui.day

import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oblonian.stracker.domain.AppUsage
import com.oblonian.stracker.domain.EpisodeType
import com.oblonian.stracker.domain.formatHoursMinutes
import com.oblonian.stracker.ui.AppViewModelProvider
import com.oblonian.stracker.ui.components.EmptyState
import com.oblonian.stracker.ui.components.HorizontalBar
import com.oblonian.stracker.ui.theme.color

@Composable
fun AppUsageScreen(onBack: () -> Unit) {
    val viewModel: DayViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val data by viewModel.data.collectAsStateWithLifecycle()
    val usage = data?.appUsage ?: emptyList()
    val pm = LocalContext.current.packageManager
    val labels = remember(usage) {
        usage.associate { u ->
            u.packageName to runCatching { pm.getApplicationLabel(pm.getApplicationInfo(u.packageName, 0)).toString() }
                .getOrElse { u.packageName.substringAfterLast('.') }
        }
    }
    AppUsageContent(usage, labels, onBack)
}

/** Apps by time on screen, largest first, each with a bar relative to the top app. */
@Composable
fun AppUsageContent(usage: List<AppUsage>, labels: Map<String, String>, onBack: () -> Unit) {
    val max = usage.maxOfOrNull { it.foregroundMillis }?.coerceAtLeast(1L) ?: 1L
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Phone", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(
                formatHoursMinutes(usage.sumOf { it.foregroundMillis }),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 20.dp),
            )
        }
        if (usage.isEmpty()) {
            EmptyState(emoji = "📵", title = "No phone time recorded", body = "Per-app time needs the Usage access permission (Settings → Permissions).")
            return
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp)) {
            items(usage, key = { it.packageName }) { u ->
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(labels[u.packageName] ?: u.packageName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 1)
                        Text(formatHoursMinutes(u.foregroundMillis), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(6.dp))
                    HorizontalBar(fraction = u.foregroundMillis.toFloat() / max, color = EpisodeType.PHONE.color)
                }
            }
        }
    }
}
