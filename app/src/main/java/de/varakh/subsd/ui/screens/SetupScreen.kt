package de.varakh.subsd.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import de.varakh.subsd.R
import de.varakh.subsd.data.prefs.Config
import de.varakh.subsd.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(vm: MainViewModel, onClose: (() -> Unit)? = null) {
    val cfg = vm.config

    var httpUrl by remember(cfg.httpUrl) { mutableStateOf(cfg.httpUrl) }
    var deviceName by remember(cfg.deviceName) { mutableStateOf(cfg.deviceName) }
    var grpcAddr by remember(cfg.grpcAddr) { mutableStateOf(cfg.grpcAddr) }
    var grpcToken by remember(cfg.grpcToken) { mutableStateOf(cfg.grpcToken) }
    var grpcTls by remember(cfg.grpcTls) { mutableStateOf(cfg.grpcTls) }
    var httpToken by remember(cfg.httpToken) { mutableStateOf(cfg.httpToken) }
    var satelliteEnabled by remember(cfg.satelliteEnabled) { mutableStateOf(cfg.satelliteEnabled) }
    var showGrpcToken by remember { mutableStateOf(false) }
    var showHttpToken by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        if (onClose != null) {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                title = { Text(stringResource(R.string.setup_title)) }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        if (onClose == null) {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.headlineSmall)
            }
        }

        Text(
            stringResource(R.string.setup_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider()

        // ── Required fields ────────────────────────────────────────────

        Text(stringResource(R.string.setup_section_required), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

        OutlinedTextField(
            value = httpUrl,
            onValueChange = { httpUrl = it },
            label = { Text(stringResource(R.string.setup_url_label)) },
            placeholder = { Text(stringResource(R.string.setup_url_placeholder)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider()

        // ── Optional fields ────────────────────────────────────────────

        Text(stringResource(R.string.setup_section_optional), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

        // HTTP token first — most users who have auth will need this regardless of satellite mode
        OutlinedTextField(
            value = httpToken,
            onValueChange = { httpToken = it },
            label = { Text(stringResource(R.string.setup_http_token_label)) },
            placeholder = { Text(stringResource(R.string.setup_http_token_placeholder)) },
            singleLine = true,
            supportingText = { Text(stringResource(R.string.setup_http_token_hint)) },
            visualTransformation = if (showHttpToken) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { showHttpToken = !showHttpToken }) {
                    Text(stringResource(if (showHttpToken) R.string.action_hide else R.string.action_show))
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.setup_satellite_label), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.setup_satellite_hint), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = satelliteEnabled, onCheckedChange = { satelliteEnabled = it })
        }

        if (satelliteEnabled) {
            OutlinedTextField(
                value = deviceName,
                onValueChange = { deviceName = it },
                label = { Text(stringResource(R.string.setup_device_name_label)) },
                placeholder = { Text(stringResource(R.string.setup_device_name_placeholder)) },
                singleLine = true,
                supportingText = { Text(stringResource(R.string.setup_device_name_hint)) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = grpcAddr,
                onValueChange = { grpcAddr = it },
                label = { Text(stringResource(R.string.setup_grpc_addr_label)) },
                placeholder = { Text(stringResource(R.string.setup_grpc_addr_placeholder)) },
                singleLine = true,
                supportingText = { Text(stringResource(R.string.setup_grpc_addr_hint)) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = grpcToken,
                onValueChange = { grpcToken = it },
                label = { Text(stringResource(R.string.setup_grpc_token_label)) },
                placeholder = { Text(stringResource(R.string.setup_grpc_token_placeholder)) },
                singleLine = true,
                visualTransformation = if (showGrpcToken) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { showGrpcToken = !showGrpcToken }) {
                        Text(stringResource(if (showGrpcToken) R.string.action_hide else R.string.action_show))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(stringResource(R.string.setup_tls_label), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.setup_tls_hint), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = grpcTls, onCheckedChange = { grpcTls = it })
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                vm.saveConfig(Config(
                    httpUrl = httpUrl.trim(),
                    deviceName = deviceName.trim(),
                    grpcAddr = grpcAddr.trim(),
                    grpcToken = grpcToken,
                    grpcTls = grpcTls,
                    httpToken = httpToken,
                    satelliteEnabled = satelliteEnabled
                ))
            },
            enabled = httpUrl.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.setup_save))
        }

        if (onClose != null) {
            var showConfirm by remember { mutableStateOf(false) }

            OutlinedButton(
                onClick = { showConfirm = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.setup_disconnect))
            }

            if (showConfirm) {
                AlertDialog(
                    onDismissRequest = { showConfirm = false },
                    title = { Text(stringResource(R.string.setup_disconnect_confirm_title)) },
                    text = { Text(stringResource(R.string.setup_disconnect_confirm_text)) },
                    confirmButton = {
                        TextButton(onClick = { showConfirm = false; vm.disconnectAndReset() }) {
                            Text(stringResource(R.string.setup_disconnect_confirm_ok), color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirm = false }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(32.dp))
        } // inner Column
    } // outer Column
}
