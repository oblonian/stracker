package com.oblonian.stracker.ui.settings

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.oblonian.stracker.domain.Place
import com.oblonian.stracker.domain.PlaceTag
import com.oblonian.stracker.ui.AppViewModelProvider
import com.oblonian.stracker.ui.components.EmptyState
import com.oblonian.stracker.ui.components.SettingRow
import com.oblonian.stracker.ui.components.StrackerCard
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

@Composable
fun PlacesScreen(onBack: () -> Unit) {
    val viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val places by viewModel.places.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Place?>(null) }
    var adding by remember { mutableStateOf(false) }

    PlacesContent(places, onAdd = { adding = true }, onEdit = { editing = it }, onBack = onBack)

    if (adding || editing != null) {
        PlaceEditDialog(
            initial = editing,
            onSave = { viewModel.savePlace(it); adding = false; editing = null },
            onDelete = editing?.let { p -> { viewModel.deletePlace(p.id); editing = null } },
            onDismiss = { adding = false; editing = null },
        )
    }
}

@Composable
fun PlacesContent(places: List<Place>, onAdd: () -> Unit, onEdit: (Place) -> Unit, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                Text("Places", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Text(
                "Name the places you spend time at. Tag one as work so work blocks are only counted there.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            if (places.isEmpty()) {
                EmptyState(emoji = "📍", title = "No places yet", body = "Add home and work to start; tap + below.")
            } else {
                StrackerCard(modifier = Modifier.padding(16.dp), contentPadding = 6.dp) {
                    LazyColumn {
                        items(places, key = { it.id }) { p ->
                            SettingRow(
                                icon = when (p.tag) { PlaceTag.HOME -> Icons.Outlined.Home; PlaceTag.WORK -> Icons.Outlined.Work; PlaceTag.NONE -> Icons.Outlined.Place },
                                title = p.name,
                                subtitle = "${p.radiusM.toInt()} m around ${"%.4f".format(p.lat)}, ${"%.4f".format(p.lon)}" +
                                    when (p.tag) { PlaceTag.HOME -> " · home"; PlaceTag.WORK -> " · work"; PlaceTag.NONE -> "" },
                                onClick = { onEdit(p) },
                            )
                        }
                    }
                }
            }
        }
        FloatingActionButton(onClick = onAdd, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            Icon(Icons.Filled.Add, contentDescription = "Add a place")
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun PlaceEditDialog(initial: Place?, onSave: (Place) -> Unit, onDelete: (() -> Unit)?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var radius by remember { mutableStateOf(initial?.radiusM?.toFloat() ?: 150f) }
    var tag by remember { mutableStateOf(initial?.tag ?: PlaceTag.NONE) }
    var point by remember { mutableStateOf(initial?.let { GeoPoint(it.lat, it.lon) }) }
    var pickOnMap by remember { mutableStateOf(false) }

    if (pickOnMap) {
        MapPickerDialog(point, onPick = { point = it; pickOnMap = false }, onDismiss = { pickOnMap = false })
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New place" else "Edit place") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            LocationServices.getFusedLocationProviderClient(context)
                                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                                .addOnSuccessListener { loc -> loc?.let { point = GeoPoint(it.latitude, it.longitude) } }
                        }
                    }) { Text("Use current location") }
                    OutlinedButton(onClick = { pickOnMap = true }) { Text("Pick on map") }
                }
                Text(
                    point?.let { "%.4f, %.4f".format(it.latitude, it.longitude) } ?: "No location set yet",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("Radius ${radius.toInt()} m", style = MaterialTheme.typography.labelLarge)
                Slider(value = radius, onValueChange = { radius = it }, valueRange = 50f..500f, steps = 8)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(tag == PlaceTag.HOME, { tag = if (tag == PlaceTag.HOME) PlaceTag.NONE else PlaceTag.HOME }, label = { Text("home") })
                    FilterChip(tag == PlaceTag.WORK, { tag = if (tag == PlaceTag.WORK) PlaceTag.NONE else PlaceTag.WORK }, label = { Text("work") })
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && point != null,
                onClick = { onSave(Place(initial?.id ?: 0, name.trim(), point!!.latitude, point!!.longitude, radius.toDouble(), tag)) },
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

/** A tappable OpenStreetMap; the tapped point becomes the place's centre. */
@Composable
private fun MapPickerDialog(initial: GeoPoint?, onPick: (GeoPoint) -> Unit, onDismiss: () -> Unit) {
    var picked by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tap the place") },
        text = {
            Box(Modifier.fillMaxWidth().height(320.dp)) {
                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(if (initial != null) 16.0 else 12.0)
                            controller.setCenter(initial ?: GeoPoint(20.5937, 78.9629))
                            val marker = Marker(this).apply { initial?.let { position = it } }
                            if (initial != null) overlays.add(marker)
                            overlays.add(
                                MapEventsOverlay(object : MapEventsReceiver {
                                    override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                        picked = p
                                        marker.position = p
                                        if (marker !in overlays) overlays.add(marker)
                                        invalidate()
                                        return true
                                    }
                                    override fun longPressHelper(p: GeoPoint) = false
                                })
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        },
        confirmButton = { Button(enabled = picked != null, onClick = { onPick(picked!!) }) { Text("Use this spot") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

