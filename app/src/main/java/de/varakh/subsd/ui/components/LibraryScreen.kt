package de.varakh.subsd.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
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
import de.varakh.subsd.data.model.Album
import de.varakh.subsd.data.model.Artist
import de.varakh.subsd.data.model.Song
import de.varakh.subsd.ui.LibraryView
import de.varakh.subsd.ui.MainViewModel

@Composable
fun LibraryScreen(vm: MainViewModel) {
    when (vm.libraryView) {
        LibraryView.Artists -> ArtistsScreen(vm)
        LibraryView.Albums -> AlbumsScreen(vm)
        LibraryView.Tracks -> TracksScreen(vm)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArtistsScreen(vm: MainViewModel) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.library_artists_title)) },
            actions = {
                IconButton(onClick = { vm.loadArtists() }) {
                    Icon(Icons.Default.Refresh, stringResource(R.string.action_refresh))
                }
                IconButton(onClick = { vm.openSettings() }) {
                    Icon(Icons.Default.Settings, stringResource(R.string.settings_title))
                }
            }
        )
        if (vm.libraryLoading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        if (vm.artists.isEmpty() && !vm.libraryLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.library_no_artists), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(vm.artists, key = { it.id }) { artist ->
                    ArtistItem(artist, vm)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArtistItem(artist: Artist, vm: MainViewModel) {
    var showMenu by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(artist.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                pluralStringResource(R.plurals.album_count, artist.albumCount, artist.albumCount),
                style = MaterialTheme.typography.bodySmall
            )
        },
        leadingContent = {
            CoverArt(artist.coverArt, size = 48)
        },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, stringResource(R.string.action_options))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_play_all)) },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, null) },
                        onClick = { vm.playArtist(artist.id); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_add_to_queue)) },
                        leadingIcon = { Icon(Icons.Default.AddToQueue, null) },
                        onClick = { vm.enqueueArtist(artist.id); showMenu = false }
                    )
                }
            }
        },
        modifier = Modifier.combinedClickable(
            onClick = { vm.loadAlbums(artist) },
            onLongClick = { showMenu = true }
        )
    )
    HorizontalDivider(thickness = 0.5.dp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumsScreen(vm: MainViewModel) {
    val artist = vm.currentArtist
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = { vm.libraryBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                }
            },
            title = { Text(artist?.name ?: stringResource(R.string.library_albums_title), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            actions = {
                IconButton(onClick = { vm.openSettings() }) {
                    Icon(Icons.Default.Settings, stringResource(R.string.settings_title))
                }
            }
        )
        if (vm.libraryLoading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(vm.albums, key = { it.id }) { album ->
                AlbumItem(album, vm)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumItem(album: Album, vm: MainViewModel) {
    var showMenu by remember { mutableStateOf(false) }
    var showRatingDialog by remember { mutableStateOf(false) }
    var showPlaylistPicker by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(album.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(buildString {
                if (album.year > 0) append("${album.year} · ")
                append(pluralStringResource(R.plurals.track_count, album.songCount, album.songCount))
            }, style = MaterialTheme.typography.bodySmall)
        },
        leadingContent = { CoverArt(album.coverArt, size = 56) },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, stringResource(R.string.action_options))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_play_album)) },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, null) },
                        onClick = { vm.playAlbum(album.id); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_add_to_queue)) },
                        leadingIcon = { Icon(Icons.Default.AddToQueue, null) },
                        onClick = { vm.enqueueAlbum(album.id); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_add_to_playlist)) },
                        leadingIcon = { Icon(Icons.Default.PlaylistAdd, null) },
                        onClick = { showPlaylistPicker = true; showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_rate)) },
                        leadingIcon = { Icon(Icons.Default.Star, null) },
                        onClick = { showRatingDialog = true; showMenu = false }
                    )
                }
            }
        },
        modifier = Modifier.combinedClickable(
            onClick = { vm.loadSongs(album) },
            onLongClick = { showMenu = true }
        )
    )
    if (showRatingDialog) {
        RatingDialog(
            title = stringResource(R.string.rating_dialog_album_title),
            currentRating = album.userRating,
            onRate = { vm.setAlbumRating(album.id, it) },
            onDismiss = { showRatingDialog = false }
        )
    }
    if (showPlaylistPicker) {
        PlaylistPickerDialog(
            playlists = vm.playlists,
            onPick = { vm.addAlbumToPlaylist(it.id, album.id) },
            onDismiss = { showPlaylistPicker = false }
        )
    }
    HorizontalDivider(thickness = 0.5.dp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TracksScreen(vm: MainViewModel) {
    val album = vm.currentAlbum
    var showAlbumPlaylistPicker by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = { vm.libraryBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                }
            },
            title = { Text(album?.name ?: stringResource(R.string.library_tracks_title), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            actions = {
                if (album != null) {
                    IconButton(onClick = { vm.playAlbum(album.id) }) {
                        Icon(Icons.Default.PlayArrow, stringResource(R.string.action_play_album))
                    }
                    IconButton(onClick = { vm.enqueueAlbum(album.id) }) {
                        Icon(Icons.Default.AddToQueue, stringResource(R.string.library_add_album_to_queue))
                    }
                    IconButton(onClick = { showAlbumPlaylistPicker = true }) {
                        Icon(Icons.Default.PlaylistAdd, stringResource(R.string.action_add_to_playlist))
                    }
                }
                IconButton(onClick = { vm.openSettings() }) {
                    Icon(Icons.Default.Settings, stringResource(R.string.settings_title))
                }
            }
        )
        if (vm.libraryLoading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(vm.songs, key = { it.id }) { song ->
                SongItem(song, vm)
            }
        }
    }
    if (showAlbumPlaylistPicker && album != null) {
        PlaylistPickerDialog(
            playlists = vm.playlists,
            onPick = { vm.addAlbumToPlaylist(it.id, album.id) },
            onDismiss = { showAlbumPlaylistPicker = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongItem(song: Song, vm: MainViewModel) {
    var showMenu by remember { mutableStateOf(false) }
    var showRatingDialog by remember { mutableStateOf(false) }
    var showPlaylistPicker by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(buildString {
                if (song.track > 0) append("${song.track}. ")
                append(song.artist)
                if (song.duration > 0) append(" · ${formatDuration(song.duration)}")
            }, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        leadingContent = { CoverArt(song.coverArt, size = 48) },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, stringResource(R.string.action_options))
                }
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
                        text = { Text(stringResource(R.string.action_add_to_playlist)) },
                        leadingIcon = { Icon(Icons.Default.PlaylistAdd, null) },
                        onClick = { showPlaylistPicker = true; showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_rate)) },
                        leadingIcon = { Icon(Icons.Default.Star, null) },
                        onClick = { showRatingDialog = true; showMenu = false }
                    )
                }
            }
        },
        modifier = Modifier.combinedClickable(
            onClick = { vm.playSong(song.id) },
            onLongClick = { showMenu = true }
        )
    )
    if (showRatingDialog) {
        RatingDialog(
            title = stringResource(R.string.rating_dialog_song_title),
            currentRating = song.userRating,
            onRate = { vm.setSongRating(song.id, it) },
            onDismiss = { showRatingDialog = false }
        )
    }
    if (showPlaylistPicker) {
        PlaylistPickerDialog(
            playlists = vm.playlists,
            onPick = { vm.addSongsToPlaylist(it.id, song.id) },
            onDismiss = { showPlaylistPicker = false }
        )
    }
    HorizontalDivider(thickness = 0.5.dp)
}

@Composable
private fun RatingDialog(title: String, currentRating: Int, onRate: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                (1..5).forEach { n ->
                    IconButton(onClick = {
                        onRate(if (n == currentRating) 0 else n)
                        onDismiss()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "$n stars",
                            tint = if (n <= currentRating) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

fun formatDuration(secs: Int): String {
    return "%d:%02d".format(secs / 60, secs % 60)
}
