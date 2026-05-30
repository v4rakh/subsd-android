package de.varakh.subsd.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

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
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.playlists_title)) },
            actions = {
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, stringResource(R.string.action_new_playlist))
                }
                IconButton(onClick = { vm.loadPlaylists() }) {
                    Icon(Icons.Default.Refresh, stringResource(R.string.action_refresh))
                }
                IconButton(onClick = { vm.openSettings() }) {
                    Icon(Icons.Default.Settings, stringResource(R.string.settings_title))
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

    if (showCreateDialog) {
        PlaylistNameDialog(
            title = stringResource(R.string.dialog_create_playlist_title),
            initial = "",
            confirmLabel = stringResource(R.string.action_create),
            onConfirm = { name -> vm.createPlaylist(name); showCreateDialog = false },
            onDismiss = { showCreateDialog = false }
        )
    }
}

@Composable
private fun PlaylistItem(playlist: Playlist, vm: MainViewModel) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

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
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_append_queue_to_playlist)) },
                        leadingIcon = { Icon(Icons.Default.QueueMusic, null) },
                        onClick = { vm.appendQueueToPlaylist(playlist.id); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_rename_playlist)) },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        onClick = { showRenameDialog = true; showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_delete_playlist)) },
                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                        onClick = { showDeleteDialog = true; showMenu = false }
                    )
                }
            }
        },
        modifier = Modifier.clickable { vm.openPlaylist(playlist) }
    )
    HorizontalDivider(thickness = 0.5.dp)

    if (showRenameDialog) {
        PlaylistNameDialog(
            title = stringResource(R.string.dialog_rename_playlist_title),
            initial = playlist.name,
            confirmLabel = stringResource(R.string.action_rename),
            onConfirm = { name -> vm.renamePlaylist(playlist.id, name); showRenameDialog = false },
            onDismiss = { showRenameDialog = false }
        )
    }
    if (showDeleteDialog) {
        DeletePlaylistDialog(
            name = playlist.name,
            onConfirm = { vm.deletePlaylist(playlist.id); showDeleteDialog = false },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistDetailScreen(vm: MainViewModel) {
    val playlist = vm.currentPlaylist ?: return
    val lazyListState = rememberLazyListState()

    // Local copy with stable slot keys for drag-and-drop (mirrors QueueScreen pattern)
    var nextKey by remember { mutableLongStateOf(0L) }
    val localSongs = remember { mutableStateListOf<Pair<Long, Song>>() }
    LaunchedEffect(playlist.songs) {
        localSongs.clear()
        playlist.songs.forEach { song -> localSongs.add(nextKey++ to song) }
    }

    var dragFromIdx by remember { mutableIntStateOf(-1) }
    var dragToIdx by remember { mutableIntStateOf(-1) }

    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        if (dragFromIdx == -1) dragFromIdx = from.index
        dragToIdx = to.index
        localSongs.add(to.index, localSongs.removeAt(from.index))
    }

    LaunchedEffect(reorderState.isAnyItemDragging) {
        if (!reorderState.isAnyItemDragging && dragFromIdx != -1 && dragToIdx != -1 && dragFromIdx != dragToIdx) {
            vm.reorderPlaylist(playlist.id, dragFromIdx, dragToIdx)
        }
        if (!reorderState.isAnyItemDragging) { dragFromIdx = -1; dragToIdx = -1 }
    }

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
                IconButton(onClick = { vm.openSettings() }) {
                    Icon(Icons.Default.Settings, stringResource(R.string.settings_title))
                }
            }
        )
        LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize()) {
            itemsIndexed(localSongs, key = { _, entry -> entry.first }) { idx, (slotKey, song) ->
                ReorderableItem(reorderState, key = slotKey) {
                    PlaylistSongItem(
                        song = song,
                        trackNumber = idx + 1,
                        vm = vm,
                        playlistId = playlist.id,
                        index = idx,
                        dragHandleModifier = Modifier.draggableHandle()
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistSongItem(
    song: Song,
    trackNumber: Int,
    vm: MainViewModel,
    playlistId: String,
    index: Int,
    dragHandleModifier: Modifier = Modifier
) {
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.DragHandle,
                    contentDescription = stringResource(R.string.queue_drag_handle),
                    modifier = dragHandleModifier,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_remove_from_playlist)) },
                            leadingIcon = { Icon(Icons.Default.Remove, null) },
                            onClick = { vm.removeSongFromPlaylist(playlistId, index); showMenu = false }
                        )
                    }
                }
            }
        },
        modifier = Modifier.clickable { vm.playSong(song.id) }
    )
    HorizontalDivider(thickness = 0.5.dp)
}

// ── Dialog helpers ─────────────────────────────────────────────────────────────

@Composable
private fun PlaylistNameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.dialog_playlist_name_hint)) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }, enabled = name.isNotBlank()) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
private fun DeletePlaylistDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_delete_playlist_title)) },
        text = { Text(stringResource(R.string.dialog_delete_playlist_msg, name)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
