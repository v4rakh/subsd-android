package de.varakh.subsd.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import de.varakh.subsd.R
import de.varakh.subsd.ui.MainViewModel
import de.varakh.subsd.ui.components.*

private enum class Tab(@StringRes val labelRes: Int, val icon: ImageVector) {
    Library(R.string.tab_library, Icons.Default.LibraryMusic),
    Search(R.string.tab_search, Icons.Default.Search),
    Queue(R.string.tab_queue, Icons.AutoMirrored.Filled.QueueMusic),
    Playlists(R.string.tab_playlists, Icons.AutoMirrored.Filled.PlaylistPlay),
    Satellites(R.string.tab_satellites, Icons.Default.Devices),
}

@Composable
fun MainScreen(vm: MainViewModel) {
    var selectedTab by remember { mutableStateOf(Tab.Library) }
    val hasQueue = vm.playerState.queue.isNotEmpty()

    Scaffold(
        bottomBar = {
            Column {
                // Mini-player above the navigation bar
                PlayerBar(vm)

                NavigationBar {
                    Tab.entries.forEach { tab ->
                        val label = stringResource(tab.labelRes)
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = {
                                BadgedBox(badge = {
                                    if (tab == Tab.Queue && hasQueue) {
                                        Badge { Text("${vm.playerState.queue.size}") }
                                    }
                                }) {
                                    Icon(tab.icon, label)
                                }
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                Tab.Library -> LibraryScreen(vm)
                Tab.Search -> SearchScreen(vm)
                Tab.Queue -> QueueScreen(vm)
                Tab.Playlists -> PlaylistsScreen(vm)
                Tab.Satellites -> SatellitesScreen(vm)
            }
        }
    }
}
