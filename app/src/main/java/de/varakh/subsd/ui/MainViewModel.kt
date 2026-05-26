package de.varakh.subsd.ui

import android.app.Application
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.varakh.subsd.SubsdApp
import de.varakh.subsd.data.api.ApiException
import de.varakh.subsd.data.api.WsEvent
import de.varakh.subsd.data.model.*
import de.varakh.subsd.data.prefs.Config
import de.varakh.subsd.playback.PlaybackService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "MainViewModel"

enum class LibraryView { Artists, Albums, Tracks }

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app get() = SubsdApp.instance

    // ── Config / connection ───────────────────────────────────────────────

    var config by mutableStateOf(Config()); private set
    var wsConnected by mutableStateOf(false); private set

    // ── Settings overlay ──────────────────────────────────────────────────

    var showSettings by mutableStateOf(false); private set

    fun openSettings() { showSettings = true }
    fun closeSettings() { showSettings = false }

    // ── Player state (driven by WebSocket) ────────────────────────────────

    var playerState by mutableStateOf(PlayerState()); private set

    // ── Satellites ────────────────────────────────────────────────────────

    var satellites by mutableStateOf<List<SatelliteInfo>>(emptyList()); private set

    // ── Library ───────────────────────────────────────────────────────────

    var libraryView by mutableStateOf(LibraryView.Artists); private set
    var artists by mutableStateOf<List<Artist>>(emptyList()); private set
    var albums by mutableStateOf<List<Album>>(emptyList()); private set
    var songs by mutableStateOf<List<Song>>(emptyList()); private set
    var currentArtist by mutableStateOf<Artist?>(null); private set
    var currentAlbum by mutableStateOf<Album?>(null); private set
    var libraryLoading by mutableStateOf(false); private set
    var libraryError by mutableStateOf<String?>(null); private set

    // ── Search ────────────────────────────────────────────────────────────

    var searchQuery by mutableStateOf("")
    var searchResult by mutableStateOf<SearchResult?>(null); private set
    var searchLoading by mutableStateOf(false); private set
    private var searchJob: Job? = null

    // ── Playlists ─────────────────────────────────────────────────────────

    var playlists by mutableStateOf<List<Playlist>>(emptyList()); private set
    var currentPlaylist by mutableStateOf<Playlist?>(null); private set
    var playlistView by mutableStateOf(false); private set
    var playlistsLoading by mutableStateOf(false); private set

    // ── Devices ───────────────────────────────────────────────────────────

    var devices by mutableStateOf(DevicesResponse(emptyList(), "")); private set

    // ── Toast messages ────────────────────────────────────────────────────

    var toastMessage by mutableStateOf<String?>(null); private set

    // ── Init ──────────────────────────────────────────────────────────────

    init {
        // React to config changes
        viewModelScope.launch {
            app.prefs.config.collect { cfg ->
                Log.i(TAG, "Config updated: url=${cfg.httpUrl} grpc=${cfg.resolvedGrpcAddr} device=${cfg.resolvedDeviceName} configured=${cfg.isConfigured}")
                config = cfg
                if (cfg.isConfigured) {
                    app.api.updateBase(cfg.httpUrl)
                    if (cfg.httpToken.isNotBlank()) {
                        Log.i(TAG, "Logging in with HTTP token")
                        app.api.login(cfg.httpToken)
                    }
                    app.webSocket.connect(cfg.httpUrl)
                    if (cfg.satelliteEnabled) {
                        Log.i(TAG, "Starting PlaybackService (satellite mode enabled)")
                        val intent = Intent(getApplication(), PlaybackService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            getApplication<Application>().startForegroundService(intent)
                        } else {
                            getApplication<Application>().startService(intent)
                        }
                    } else {
                        Log.i(TAG, "Satellite mode disabled — stopping PlaybackService if running")
                        getApplication<Application>().stopService(
                            Intent(getApplication(), PlaybackService::class.java)
                        )
                    }
                    loadArtists()
                    loadPlaylists()
                }
            }
        }

        // Mirror WebSocket player state
        viewModelScope.launch {
            app.webSocket.playerState.collect { state ->
                Log.d(TAG, "Player state update: playing=${state.playing} idx=${state.currentIdx} queueSize=${state.queue.size} shuffle=${state.shuffle} repeat=${state.repeat}")
                playerState = state
            }
        }

        // Mirror WebSocket satellite list
        viewModelScope.launch {
            app.webSocket.satellites.collect { list ->
                Log.i(TAG, "Satellite list updated: ${list.map { "${it.name}(active=${it.active})" }}")
                satellites = list
            }
        }

        // Mirror WebSocket connection status
        viewModelScope.launch {
            app.webSocket.connected.collect { c ->
                Log.i(TAG, "WebSocket connected=$c")
                wsConnected = c
            }
        }

        // Handle one-shot WS events
        viewModelScope.launch {
            for (event in app.webSocket.events) {
                when (event) {
                    is WsEvent.SatelliteDisconnected -> {
                        Log.i(TAG, "Satellite disconnected: ${event.name}")
                        toastMessage = getApplication<Application>()
                            .getString(de.varakh.subsd.R.string.toast_satellite_disconnected, event.name)
                    }
                }
            }
        }
    }

    // ── Playback controls ─────────────────────────────────────────────────

    fun play() = cmd("play") { app.api.play() }
    fun pause() = cmd("pause") { app.api.pause() }
    fun playPause() = cmd("playPause") { app.api.playPause() }
    fun next() = cmd("next") { app.api.next() }
    fun prev() = cmd("prev") { app.api.prev() }
    fun toggleShuffle() = cmd("toggleShuffle") { app.api.shuffle() }
    fun toggleRepeat() = cmd("toggleRepeat") { app.api.repeat() }
    fun seek(pos: Double) = cmd("seek") { app.api.seek(pos) }
    fun setVolume(vol: Int) = cmd("setVolume") { app.api.volume(vol) }

    // ── Queue ─────────────────────────────────────────────────────────────

    fun jump(idx: Int) = cmd("jump($idx)") { app.api.jump(idx) }
    fun dequeue(idx: Int) = cmd("dequeue($idx)") { app.api.dequeue(idx) }
    fun clearQueue() = cmd("clearQueue") { app.api.clearQueue() }
    fun moveInQueue(from: Int, to: Int) = cmd("moveInQueue") { app.api.moveInQueue(from, to) }

    // ── Library ───────────────────────────────────────────────────────────

    fun loadArtists() {
        viewModelScope.launch {
            Log.i(TAG, "Loading artists")
            libraryLoading = true
            libraryError = null
            try {
                artists = app.api.getArtists()
                Log.i(TAG, "Loaded ${artists.size} artists")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load artists: ${e.message}")
                libraryError = e.message
                showError("Failed to load artists", e)
            }
            libraryLoading = false
            libraryView = LibraryView.Artists
        }
    }

    fun loadAlbums(artist: Artist) {
        viewModelScope.launch {
            Log.i(TAG, "Loading albums for artist ${artist.name}")
            libraryLoading = true
            currentArtist = artist
            try {
                albums = app.api.getArtist(artist.id).albums
                Log.i(TAG, "Loaded ${albums.size} albums")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load albums: ${e.message}")
                showError("Failed to load albums", e)
            }
            libraryLoading = false
            libraryView = LibraryView.Albums
        }
    }

    fun loadSongs(album: Album) {
        viewModelScope.launch {
            Log.i(TAG, "Loading songs for album ${album.name}")
            libraryLoading = true
            currentAlbum = album
            try {
                songs = app.api.getAlbum(album.id).songs
                Log.i(TAG, "Loaded ${songs.size} songs")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load songs: ${e.message}")
                showError("Failed to load songs", e)
            }
            libraryLoading = false
            libraryView = LibraryView.Tracks
        }
    }

    fun libraryBack() {
        when (libraryView) {
            LibraryView.Tracks -> {
                libraryView = LibraryView.Albums
                songs = emptyList()
                currentAlbum = null
            }
            LibraryView.Albums -> {
                libraryView = LibraryView.Artists
                albums = emptyList()
                currentArtist = null
            }
            LibraryView.Artists -> {}
        }
    }

    fun playSong(id: String) = cmd("playSong") { app.api.playSong(id) }
    fun enqueueSong(id: String) = cmd("enqueueSong") { app.api.enqueueSong(id) }
    fun playAlbum(id: String) = cmd("playAlbum") { app.api.playAlbum(id) }
    fun enqueueAlbum(id: String) = cmd("enqueueAlbum") { app.api.enqueueAlbum(id) }
    fun playArtist(id: String) = cmd("playArtist") { app.api.playArtist(id) }
    fun enqueueArtist(id: String) = cmd("enqueueArtist") { app.api.enqueueArtist(id) }

    // ── Search ────────────────────────────────────────────────────────────

    fun updateSearch(query: String) {
        searchQuery = query
        searchJob?.cancel()
        if (query.isBlank()) { searchResult = null; return }
        searchJob = viewModelScope.launch {
            delay(300)
            Log.i(TAG, "Searching: $query")
            searchLoading = true
            try {
                searchResult = app.api.search(query)
                Log.i(TAG, "Search results: ${searchResult?.artists?.size} artists, ${searchResult?.albums?.size} albums, ${searchResult?.songs?.size} songs")
            } catch (e: Exception) {
                Log.e(TAG, "Search failed: ${e.message}")
                searchResult = SearchResult(emptyList(), emptyList(), emptyList())
            }
            searchLoading = false
        }
    }

    // ── Playlists ─────────────────────────────────────────────────────────

    fun loadPlaylists() {
        viewModelScope.launch {
            Log.i(TAG, "Loading playlists")
            playlistsLoading = true
            try {
                playlists = app.api.getPlaylists()
                Log.i(TAG, "Loaded ${playlists.size} playlists")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load playlists: ${e.message}")
            }
            playlistsLoading = false
        }
    }

    fun openPlaylist(playlist: Playlist) {
        viewModelScope.launch {
            Log.i(TAG, "Opening playlist ${playlist.name}")
            try {
                currentPlaylist = app.api.getPlaylist(playlist.id)
                playlistView = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open playlist: ${e.message}")
                showError("Failed to load playlist", e)
            }
        }
    }

    fun playlistBack() { playlistView = false; currentPlaylist = null }

    fun playPlaylist(id: String) = cmd("playPlaylist") { app.api.playPlaylist(id) }
    fun enqueuePlaylist(id: String) = cmd("enqueuePlaylist") { app.api.enqueuePlaylist(id) }

    // ── Satellites ────────────────────────────────────────────────────────

    fun setActiveSatellite(name: String) = cmd("setActiveSatellite") { app.api.setActiveSatellite(name) }

    fun setSatelliteDevice(satellite: String, device: String) =
        cmd("setSatelliteDevice") { app.api.setSatelliteDevice(satellite, device) }

    fun refreshSatelliteDevices(name: String) {
        viewModelScope.launch {
            Log.i(TAG, "Refreshing devices for satellite $name")
            try {
                satellites = app.api.refreshSatelliteDevices(name)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh satellite devices: ${e.message}")
                showError("Failed to refresh devices", e)
            }
        }
    }

    // ── Devices (local mpv) ───────────────────────────────────────────────

    fun loadDevices() {
        viewModelScope.launch {
            try { devices = app.api.getDevices() } catch (e: Exception) {
                Log.e(TAG, "Failed to load devices: ${e.message}")
            }
        }
    }

    fun setDevice(name: String) {
        viewModelScope.launch {
            try { app.api.setDevice(name); loadDevices() } catch (e: Exception) {
                Log.e(TAG, "Failed to set device: ${e.message}")
                showError("Failed to set device", e)
            }
        }
    }

    // ── Settings ──────────────────────────────────────────────────────────

    fun saveConfig(cfg: Config) {
        Log.i(TAG, "Saving config: url=${cfg.httpUrl} grpc=${cfg.grpcAddr} device=${cfg.deviceName}")
        viewModelScope.launch { app.prefs.save(cfg) }
        showSettings = false
    }

    fun disconnectAndReset() {
        Log.i(TAG, "Disconnecting and resetting config")
        app.webSocket.disconnect()
        viewModelScope.launch { app.prefs.save(Config()) }
        showSettings = false
    }

    fun clearToast() { toastMessage = null }

    // ── Internal helpers ──────────────────────────────────────────────────

    private fun cmd(name: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            Log.d(TAG, "cmd: $name")
            try {
                block()
            } catch (e: ApiException) {
                Log.e(TAG, "API error in $name: ${e.code} ${e.message}")
                toastMessage = "Error ${e.code}: ${e.message}"
            } catch (e: Exception) {
                Log.e(TAG, "Error in $name: ${e.message}")
                toastMessage = e.message ?: "Unknown error"
            }
        }
    }

    private fun showError(prefix: String, e: Exception) {
        toastMessage = when (e) {
            is ApiException -> "$prefix: HTTP ${e.code}"
            else -> "$prefix: ${e.message}"
        }
    }
}
