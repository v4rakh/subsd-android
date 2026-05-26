package de.varakh.subsd

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import de.varakh.subsd.ui.MainViewModel
import de.varakh.subsd.ui.screens.MainScreen
import de.varakh.subsd.ui.screens.SetupScreen
import de.varakh.subsd.ui.theme.SubsdTheme

class MainActivity : ComponentActivity() {
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request notification permission for the foreground service notification (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val vm = ViewModelProvider(this)[MainViewModel::class.java]

        setContent {
            SubsdTheme {
                val snackbarState = remember { SnackbarHostState() }

                // Show toast messages from the ViewModel
                val toast = vm.toastMessage
                LaunchedEffect(toast) {
                    if (toast != null) {
                        snackbarState.showSnackbar(toast)
                        vm.clearToast()
                    }
                }

                Scaffold(
                    snackbarHost = {
                        SnackbarHost(snackbarState) { data ->
                            Snackbar(snackbarData = data)
                        }
                    }
                ) { padding ->
                    // consumeWindowInsets prevents the inner Scaffold (MainScreen) from
                    // applying system bar insets a second time (double-padding).
                    Column(
                        Modifier
                            .padding(padding)
                            .consumeWindowInsets(padding)
                            .fillMaxSize()
                    ) {
                        // Reconnecting banner — shown when configured but WS is disconnected
                        AnimatedVisibility(visible = vm.config.isConfigured && !vm.wsConnected) {
                            Column(Modifier.fillMaxWidth()) {
                                LinearProgressIndicator(Modifier.fillMaxWidth())
                                Text(
                                    stringResource(de.varakh.subsd.R.string.ws_reconnecting),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Box(Modifier.fillMaxSize()) {
                            if (vm.config.isConfigured && !vm.showSettings) {
                                MainScreen(vm)
                            } else {
                                SetupScreen(
                                    vm = vm,
                                    onClose = if (vm.config.isConfigured) vm::closeSettings else null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
