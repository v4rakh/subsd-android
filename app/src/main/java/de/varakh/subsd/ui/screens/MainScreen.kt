package de.varakh.subsd.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import de.varakh.subsd.ui.MainViewModel
import de.varakh.subsd.ui.Tab
import de.varakh.subsd.ui.components.LibraryScreen
import de.varakh.subsd.ui.components.PlayerBar
import de.varakh.subsd.ui.components.PlaylistsScreen
import de.varakh.subsd.ui.components.QueueScreen
import de.varakh.subsd.ui.components.SatellitesScreen
import de.varakh.subsd.ui.components.SearchScreen

@Composable
fun MainScreen(vm: MainViewModel) {
    val selectedTab = vm.selectedTab
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
                            onClick = { vm.selectTab(tab) },
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
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
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
