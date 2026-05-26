package de.varakh.subsd.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.varakh.subsd.R
import de.varakh.subsd.data.model.SatelliteInfo
import de.varakh.subsd.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SatellitesScreen(vm: MainViewModel) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.satellites_title)) },
            actions = {
                val connected = vm.wsConnected
                Icon(
                    if (connected) Icons.Default.Wifi else Icons.Default.WifiOff,
                    stringResource(if (connected) R.string.satellites_connected else R.string.satellites_disconnected),
                    tint = if (connected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = { vm.openSettings() }) {
                    Icon(Icons.Default.Settings, stringResource(R.string.settings_title))
                }
            }
        )

        if (vm.satellites.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Devices, null, modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.satellites_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.satellites_empty_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(vm.satellites, key = { it.name }) { satellite ->
                    SatelliteItem(satellite, vm)
                }
            }
        }

        // Connection status card at bottom
        Spacer(Modifier.weight(1f))
        ConnectionStatusCard(vm)
    }
}

@Composable
private fun SatelliteItem(satellite: SatelliteInfo, vm: MainViewModel) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (satellite.active)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        Icons.Default.SpeakerPhone,
                        null,
                        tint = if (satellite.active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column {
                        Text(
                            satellite.name,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        if (satellite.active) {
                            Text(stringResource(R.string.satellites_active),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Row {
                    if (!satellite.active) {
                        FilledTonalButton(
                            onClick = { vm.setActiveSatellite(satellite.name) },
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text(stringResource(R.string.satellites_set_active), style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            stringResource(if (expanded) R.string.satellites_collapse else R.string.satellites_expand)
                        )
                    }
                }
            }

            if (expanded) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.satellites_audio_device), style = MaterialTheme.typography.labelMedium)
                    IconButton(
                        onClick = { vm.refreshSatelliteDevices(satellite.name) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Refresh, stringResource(R.string.satellites_refresh_devices),
                            modifier = Modifier.size(16.dp))
                    }
                }

                if (satellite.devices.isEmpty()) {
                    Text(stringResource(R.string.satellites_no_devices),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp))
                } else {
                    satellite.devices.forEach { device ->
                        val isActive = device.id == satellite.activeDevice
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = isActive,
                                onClick = { vm.setSatelliteDevice(satellite.name, device.id) }
                            )
                            Spacer(Modifier.width(4.dp))
                            Column {
                                Text(device.name, style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1)
                                if (device.id != device.name) {
                                    Text(device.id, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(vm: MainViewModel) {
    val cfg = vm.config
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                if (vm.wsConnected) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                null,
                tint = if (vm.wsConnected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error
            )
            Column {
                Text(
                    stringResource(if (vm.wsConnected) R.string.satellites_status_connected else R.string.satellites_status_disconnected),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (cfg.httpUrl.isNotBlank()) {
                    Text(cfg.httpUrl, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
        }
    }
}
