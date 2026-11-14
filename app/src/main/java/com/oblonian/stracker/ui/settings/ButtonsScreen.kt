package com.oblonian.stracker.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oblonian.stracker.data.ButtonKind
import com.oblonian.stracker.data.QuickLogButton
import com.oblonian.stracker.ui.AppViewModelProvider
import com.oblonian.stracker.ui.components.StrackerCard
import com.oblonian.stracker.ui.day.ButtonEditDialog

/** The quick-log buttons: order, hide, edit, add. */
@Composable
fun ButtonsScreen(onBack: () -> Unit) {
    val viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val buttons by viewModel.buttons.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<QuickLogButton?>(null) }
    var adding by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                Text("Quick-log buttons", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            StrackerCard(modifier = Modifier.padding(16.dp), contentPadding = 6.dp) {
                LazyColumn {
                    items(buttons, key = { it.id }) { b ->
                        Row(
                            Modifier.fillMaxWidth().clickable { editing = b }.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(b.emoji, fontSize = 20.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(b.label, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    if (b.kind == ButtonKind.EVENT) "a moment" else "a stretch, tap again to end",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { viewModel.moveButton(b.id, up = true) }) { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move up") }
                            IconButton(onClick = { viewModel.moveButton(b.id, up = false) }) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move down") }
                            Switch(checked = !b.hidden, onCheckedChange = { viewModel.setButtonHidden(b.id, !it) })
                        }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { adding = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            Icon(Icons.Filled.Add, contentDescription = "Add a button")
        }
    }

    if (adding || editing != null) {
        val initial = editing
        ButtonEditDialog(
            initial = initial,
            nextSortOrder = buttons.size,
            onSave = { viewModel.saveButton(it); adding = false; editing = null },
            onHide = initial?.let { b -> { viewModel.deleteButton(b.id); editing = null } },
            onDismiss = { adding = false; editing = null },
        )
    }
}
