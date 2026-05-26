package de.varakh.subsd.data.model

import org.json.JSONArray
import org.json.JSONObject

// ── Library types ──────────────────────────────────────────────────────────

data class Artist(
    val id: String,
    val name: String,
    val albumCount: Int,
    val coverArt: String,
    val albums: List<Album> = emptyList()
)

data class Album(
    val id: String,
    val name: String,
    val artist: String,
    val artistId: String,
    val coverArt: String,
    val year: Int,
    val songCount: Int,
    val songs: List<Song> = emptyList()
)

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: String,
    val artistId: String,
    val duration: Int,
    val track: Int,
    val coverArt: String,
    val contentType: String = "",
    val suffix: String = "",
    val bitRate: Int = 0,
    val year: Int = 0,
    val genre: String = ""
)

data class Playlist(
    val id: String,
    val name: String,
    val songCount: Int,
    val coverArt: String,
    val comment: String = "",
    val songs: List<Song> = emptyList()
)

// ── Player state (mirrors player.State from subsd) ─────────────────────────

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Int,
    val coverArt: String,
    val streamUrl: String,
    val suffix: String = "",
    val bitRate: Int = 0,
    val samplingRate: Int = 0,
    val channelCount: Int = 0
)

data class PlayerState(
    val playing: Boolean = false,
    val currentIdx: Int = -1,
    val queue: List<Track> = emptyList(),
    val position: Double = 0.0,
    val duration: Double = 0.0,
    val volume: Int = 100,
    val shuffle: Boolean = false,
    val repeat: Boolean = false,
    val lastScrobble: String = ""
) {
    val currentTrack: Track? get() = queue.getOrNull(currentIdx)
}

// ── Device / satellite types ───────────────────────────────────────────────

data class AudioDevice(
    val name: String,
    val description: String
)

data class DevicesResponse(
    val devices: List<AudioDevice>,
    val current: String
)

data class SatelliteDevice(
    val id: String,
    val name: String
)

data class SatelliteInfo(
    val name: String,
    val active: Boolean,
    val devices: List<SatelliteDevice>,
    val activeDevice: String
)

// ── Search ─────────────────────────────────────────────────────────────────

data class SearchResult(
    val artists: List<Artist>,
    val albums: List<Album>,
    val songs: List<Song>
) {
    val isEmpty: Boolean get() = artists.isEmpty() && albums.isEmpty() && songs.isEmpty()
}

// ── JSON parsing helpers ───────────────────────────────────────────────────

private fun JSONObject.str(key: String): String = optString(key, "")

fun JSONObject.toArtist(): Artist = Artist(
    id = str("id"),
    name = str("name"),
    albumCount = optInt("albumCount", 0),
    coverArt = str("coverArt"),
    albums = optJSONArray("album")?.toAlbumList() ?: emptyList()
)

fun JSONObject.toAlbum(): Album = Album(
    id = str("id"),
    name = str("name"),
    artist = str("artist"),
    artistId = str("artistId"),
    coverArt = str("coverArt"),
    year = optInt("year", 0),
    songCount = optInt("songCount", 0),
    songs = optJSONArray("song")?.toSongList() ?: emptyList()
)

fun JSONObject.toSong(): Song = Song(
    id = str("id"),
    title = str("title"),
    artist = str("artist"),
    album = str("album"),
    albumId = str("albumId"),
    artistId = str("artistId"),
    duration = optInt("duration", 0),
    track = optInt("track", 0),
    coverArt = str("coverArt"),
    contentType = str("contentType"),
    suffix = str("suffix"),
    bitRate = optInt("bitRate", 0),
    year = optInt("year", 0),
    genre = str("genre")
)

fun JSONObject.toPlaylist(): Playlist = Playlist(
    id = str("id"),
    name = str("name"),
    songCount = optInt("songCount", 0),
    coverArt = str("coverArt"),
    comment = str("comment"),
    songs = optJSONArray("entry")?.toSongList() ?: emptyList()
)

fun JSONObject.toTrack(): Track = Track(
    id = str("id"),
    title = str("title"),
    artist = str("artist"),
    album = str("album"),
    duration = optInt("duration", 0),
    coverArt = str("coverArt"),
    streamUrl = str("streamUrl"),
    suffix = str("suffix"),
    bitRate = optInt("bitRate", 0),
    samplingRate = optInt("samplingRate", 0),
    channelCount = optInt("channelCount", 0)
)

fun JSONObject.toPlayerState(): PlayerState = PlayerState(
    playing = optBoolean("playing", false),
    currentIdx = optInt("currentIdx", -1),
    queue = optJSONArray("queue")?.toTrackList() ?: emptyList(),
    position = optDouble("position", 0.0),
    duration = optDouble("duration", 0.0),
    volume = optInt("volume", 100),
    shuffle = optBoolean("shuffle", false),
    repeat = optBoolean("repeat", false),
    lastScrobble = str("lastScrobble")
)

fun JSONObject.toSatelliteInfo(): SatelliteInfo = SatelliteInfo(
    name = str("name"),
    active = optBoolean("active", false),
    devices = optJSONArray("devices")?.let { arr ->
        (0 until arr.length()).map { arr.getJSONObject(it).run { SatelliteDevice(str("id"), str("name")) } }
    } ?: emptyList(),
    activeDevice = str("activeDevice")
)

fun JSONArray.toArtistList(): List<Artist> = (0 until length()).map { getJSONObject(it).toArtist() }
fun JSONArray.toAlbumList(): List<Album> = (0 until length()).map { getJSONObject(it).toAlbum() }
fun JSONArray.toSongList(): List<Song> = (0 until length()).map { getJSONObject(it).toSong() }
fun JSONArray.toTrackList(): List<Track> = (0 until length()).map { getJSONObject(it).toTrack() }
fun JSONArray.toPlaylistList(): List<Playlist> = (0 until length()).map { getJSONObject(it).toPlaylist() }
fun JSONArray.toSatelliteList(): List<SatelliteInfo> = (0 until length()).map { getJSONObject(it).toSatelliteInfo() }
