package de.varakh.subsd.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.varakh.subsd.R
import de.varakh.subsd.data.model.Album
import de.varakh.subsd.data.model.Artist
import de.varakh.subsd.data.model.SearchResult
import de.varakh.subsd.data.model.Song
import de.varakh.subsd.ui.MainViewModel

@Composable
fun SearchScreen(vm: MainViewModel) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    Column(Modifier.fillMaxSize()) {
        SearchBar(vm, focusRequester, onSearch = { focusManager.clearFocus() })

        if (vm.searchLoading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        val result = vm.searchResult
        when {
            vm.searchQuery.isBlank() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Search, null, modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.search_placeholder),
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            result != null && result.isEmpty -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.search_no_results, vm.searchQuery),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            result != null -> SearchResults(result, vm)
        }
    }
}

@Composable
private fun SearchBar(vm: MainViewModel, focusRequester: FocusRequester, onSearch: () -> Unit) {
    OutlinedTextField(
        value = vm.searchQuery,
        onValueChange = { vm.updateSearch(it) },
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = {
            if (vm.searchQuery.isNotBlank()) {
                IconButton(onClick = { vm.updateSearch("") }) {
                    Icon(Icons.Default.Clear, stringResource(R.string.search_clear))
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .focusRequester(focusRequester)
    )
}

@Composable
private fun SearchResults(result: SearchResult, vm: MainViewModel) {
    LazyColumn(Modifier.fillMaxSize()) {
        if (result.artists.isNotEmpty()) {
            item {
                SectionHeader(stringResource(R.string.search_section_artists, result.artists.size))
            }
            items(result.artists.take(5), key = { "a_${it.id}" }) { artist ->
                SearchArtistItem(artist, vm)
            }
        }

        if (result.albums.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.search_section_albums, result.albums.size)) }
            items(result.albums.take(10), key = { "alb_${it.id}" }) { album ->
                SearchAlbumItem(album, vm)
            }
        }

        if (result.songs.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.search_section_songs, result.songs.size)) }
            items(result.songs, key = { "s_${it.id}" }) { song ->
                SearchSongItem(song, vm)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SearchArtistItem(artist: Artist, vm: MainViewModel) {
    var showMenu by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(artist.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                pluralStringResource(R.plurals.album_count, artist.albumCount, artist.albumCount),
                style = MaterialTheme.typography.bodySmall
            )
        },
        leadingContent = { CoverArt(artist.coverArt, size = 48) },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null) }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_play_all)) },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, null) },
                        onClick = { vm.playArtist(artist.id); showMenu = false })
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_add_to_queue)) },
                        leadingIcon = { Icon(Icons.Default.AddToQueue, null) },
                        onClick = { vm.enqueueArtist(artist.id); showMenu = false })
                }
            }
        },
        modifier = Modifier.clickable { vm.loadAlbums(artist) }
    )
    HorizontalDivider(thickness = 0.5.dp)
}

@Composable
private fun SearchAlbumItem(album: Album, vm: MainViewModel) {
    var showMenu by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(album.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text("${album.artist}${if (album.year > 0) " · ${album.year}" else ""}",
                style = MaterialTheme.typography.bodySmall)
        },
        leadingContent = { CoverArt(album.coverArt, size = 48) },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null) }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_play_album)) },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, null) },
                        onClick = { vm.playAlbum(album.id); showMenu = false })
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_add_to_queue)) },
                        leadingIcon = { Icon(Icons.Default.AddToQueue, null) },
                        onClick = { vm.enqueueAlbum(album.id); showMenu = false })
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_go_to_artist)) },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        onClick = { vm.goToArtist(album.artistId, album.artist); showMenu = false })
                }
            }
        },
        modifier = Modifier.clickable { vm.loadSongs(album) }
    )
    HorizontalDivider(thickness = 0.5.dp)
}

@Composable
private fun SearchSongItem(song: Song, vm: MainViewModel) {
    var showMenu by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text("${song.artist} · ${song.album}${if (song.duration > 0) " · ${formatDuration(song.duration)}" else ""}",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        leadingContent = { CoverArt(song.coverArt, size = 48) },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null) }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_play_now)) },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, null) },
                        onClick = { vm.playSong(song.id); showMenu = false })
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_add_to_queue)) },
                        leadingIcon = { Icon(Icons.Default.AddToQueue, null) },
                        onClick = { vm.enqueueSong(song.id); showMenu = false })
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_go_to_album)) },
                        leadingIcon = { Icon(Icons.Default.Album, null) },
                        onClick = { vm.goToAlbum(song.albumId, song.album, song.artistId); showMenu = false })
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_go_to_artist)) },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        onClick = { vm.goToArtist(song.artistId, song.artist); showMenu = false })
                }
            }
        },
        modifier = Modifier.clickable { vm.playSong(song.id) }
    )
    HorizontalDivider(thickness = 0.5.dp)
}
