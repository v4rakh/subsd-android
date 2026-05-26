package de.varakh.subsd.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.varakh.subsd.R
import de.varakh.subsd.data.model.Playlist
import de.varakh.subsd.data.model.Song
import de.varakh.subsd.ui.MainViewModel

@Composable
fun PlaylistsScreen(vm: MainViewModel) {
    if (vm.playlistView && vm.currentPlaylist != null) {
        PlaylistDetailScreen(vm)
    } else {
        PlaylistListScreen(vm)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistListScreen(vm: MainViewModel) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.playlists_title)) },
            actions = {
                IconButton(onClick = { vm.loadPlaylists() }) {
                    Icon(Icons.Default.Refresh, stringResource(R.string.action_refresh))
                }
            }
        )
        if (vm.playlistsLoading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        if (vm.playlists.isEmpty() && !vm.playlistsLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.playlists_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(vm.playlists, key = { it.id }) { playlist ->
                    PlaylistItem(playlist, vm)
                }
            }
        }
    }
}

@Composable
private fun PlaylistItem(playlist: Playlist, vm: MainViewModel) {
    var showMenu by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(playlist.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                pluralStringResource(R.plurals.track_count, playlist.songCount, playlist.songCount),
                style = MaterialTheme.typography.bodySmall
            )
        },
        leadingContent = { CoverArt(playlist.coverArt, size = 56) },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null) }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_play_playlist)) },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, null) },
                        onClick = { vm.playPlaylist(playlist.id); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_add_to_queue)) },
                        leadingIcon = { Icon(Icons.Default.AddToQueue, null) },
                        onClick = { vm.enqueuePlaylist(playlist.id); showMenu = false }
                    )
                }
            }
        },
        modifier = Modifier.clickable { vm.openPlaylist(playlist) }
    )
    HorizontalDivider(thickness = 0.5.dp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistDetailScreen(vm: MainViewModel) {
    val playlist = vm.currentPlaylist ?: return
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = { vm.playlistBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                }
            },
            title = { Text(playlist.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            actions = {
                IconButton(onClick = { vm.playPlaylist(playlist.id) }) {
                    Icon(Icons.Default.PlayArrow, stringResource(R.string.playlist_play_desc))
                }
                IconButton(onClick = { vm.enqueuePlaylist(playlist.id) }) {
                    Icon(Icons.Default.AddToQueue, stringResource(R.string.playlist_queue_desc))
                }
            }
        )
        LazyColumn(Modifier.fillMaxSize()) {
            items(playlist.songs.withIndex().toList(), key = { it.index }) { (idx, song) ->
                PlaylistSongItem(song, idx + 1, vm)
            }
        }
    }
}

@Composable
private fun PlaylistSongItem(song: Song, trackNumber: Int, vm: MainViewModel) {
    var showMenu by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text("${song.artist}${if (song.duration > 0) " · ${formatDuration(song.duration)}" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        leadingContent = {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                Text("$trackNumber", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null) }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_play_now)) },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, null) },
                        onClick = { vm.playSong(song.id); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_add_to_queue)) },
                        leadingIcon = { Icon(Icons.Default.AddToQueue, null) },
                        onClick = { vm.enqueueSong(song.id); showMenu = false }
                    )
                }
            }
        },
        modifier = Modifier.clickable { vm.playSong(song.id) }
    )
    HorizontalDivider(thickness = 0.5.dp)
}
