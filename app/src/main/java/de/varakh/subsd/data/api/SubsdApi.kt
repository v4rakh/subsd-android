package de.varakh.subsd.data.api

import de.varakh.subsd.data.model.Album
import de.varakh.subsd.data.model.Artist
import de.varakh.subsd.data.model.AudioDevice
import de.varakh.subsd.data.model.DevicesResponse
import de.varakh.subsd.data.model.PlayerState
import de.varakh.subsd.data.model.Playlist
import de.varakh.subsd.data.model.SatelliteInfo
import de.varakh.subsd.data.model.SearchResult
import de.varakh.subsd.data.model.toAlbum
import de.varakh.subsd.data.model.toAlbumList
import de.varakh.subsd.data.model.toArtist
import de.varakh.subsd.data.model.toArtistList
import de.varakh.subsd.data.model.toPlaylist
import de.varakh.subsd.data.model.toPlaylistList
import de.varakh.subsd.data.model.toPlayerState
import de.varakh.subsd.data.model.toSatelliteList
import de.varakh.subsd.data.model.toSongList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class SubsdApi(baseUrl: String) {
    private var base: String = baseUrl.trimEnd('/')
    val cookieJar = SessionCookieJar()

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .cookieJar(cookieJar)
        .build()

    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

    fun updateBase(url: String) {
        base = url.trimEnd('/')
    }

    val baseUrl: String get() = base

    // ── Auth ──────────────────────────────────────────────────────────────

    suspend fun login(token: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = FormBody.Builder().add("token", token).build()
            val resp = client.newCall(Request.Builder().url("$base/login").post(body).build()).execute()
            resp.code == 204
        } catch (_: Exception) { false }
    }

    // ── State ─────────────────────────────────────────────────────────────

    suspend fun getState(): PlayerState? = withContext(Dispatchers.IO) {
        try { JSONObject(get("/api/v1/state")).toPlayerState() } catch (_: Exception) { null }
    }

    // ── Player controls ───────────────────────────────────────────────────

    suspend fun play() = post("/api/v1/play")
    suspend fun pause() = post("/api/v1/pause")
    suspend fun playPause() = post("/api/v1/playpause")
    suspend fun next() = post("/api/v1/next")
    suspend fun prev() = post("/api/v1/prev")
    suspend fun shuffle() = post("/api/v1/shuffle")
    suspend fun repeat() = post("/api/v1/repeat")

    suspend fun seek(position: Double) =
        postJson("/api/v1/seek", JSONObject().put("position", position).toString())

    suspend fun volume(vol: Int) =
        postJson("/api/v1/volume", JSONObject().put("volume", vol).toString())

    suspend fun setReplayGain(mode: String) =
        postJson("/api/v1/replaygain", JSONObject().put("mode", mode).toString())

    suspend fun setRating(id: String, rating: Int) =
        postJson("/api/v1/rating", JSONObject().put("id", id).put("rating", rating).toString())

    // ── Queue ─────────────────────────────────────────────────────────────

    suspend fun enqueueSong(id: String) = post("/api/v1/queue/song/$id")
    suspend fun enqueueAlbum(id: String) = post("/api/v1/queue/album/$id")
    suspend fun enqueueArtist(id: String) = post("/api/v1/queue/artist/$id")
    suspend fun enqueuePlaylist(id: String) = post("/api/v1/queue/playlist/$id")
    suspend fun clearQueue() = delete("/api/v1/queue")
    suspend fun dequeue(idx: Int) = delete("/api/v1/queue/$idx")
    suspend fun jump(idx: Int) = post("/api/v1/queue/jump/$idx")

    suspend fun moveInQueue(from: Int, to: Int) =
        postJson("/api/v1/queue/move", JSONObject().put("from", from).put("to", to).toString())

    // ── Play (replace queue) ──────────────────────────────────────────────

    suspend fun playSong(id: String) = post("/api/v1/play/song/$id")
    suspend fun playAlbum(id: String) = post("/api/v1/play/album/$id")
    suspend fun playArtist(id: String) = post("/api/v1/play/artist/$id")
    suspend fun playPlaylist(id: String) = post("/api/v1/play/playlist/$id")

    // ── Library ───────────────────────────────────────────────────────────

    suspend fun getArtists(): List<Artist> = withContext(Dispatchers.IO) {
        JSONArray(get("/api/v1/artists")).toArtistList()
    }

    suspend fun getArtist(id: String): Artist = withContext(Dispatchers.IO) {
        JSONObject(get("/api/v1/artist/$id")).toArtist()
    }

    suspend fun getAlbum(id: String): Album = withContext(Dispatchers.IO) {
        JSONObject(get("/api/v1/album/$id")).toAlbum()
    }

    suspend fun search(query: String): SearchResult = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode(query, "UTF-8")
        val obj = JSONObject(get("/api/v1/search?q=$q"))
        SearchResult(
            artists = obj.optJSONArray("artist")?.toArtistList() ?: emptyList(),
            albums = obj.optJSONArray("album")?.toAlbumList() ?: emptyList(),
            songs = obj.optJSONArray("song")?.toSongList() ?: emptyList()
        )
    }

    // ── Playlists ─────────────────────────────────────────────────────────

    suspend fun getPlaylists(): List<Playlist> = withContext(Dispatchers.IO) {
        JSONArray(get("/api/v1/playlists")).toPlaylistList()
    }

    suspend fun getPlaylist(id: String): Playlist = withContext(Dispatchers.IO) {
        JSONObject(get("/api/v1/playlist/$id")).toPlaylist()
    }

    suspend fun createPlaylist(name: String): Playlist = withContext(Dispatchers.IO) {
        val body = JSONObject().put("name", name).put("songIds", JSONArray()).toString()
        JSONObject(postJson("/api/v1/playlist", body)).toPlaylist()
    }

    suspend fun renamePlaylist(id: String, newName: String) =
        putJson("/api/v1/playlist/$id", JSONObject().put("name", newName).toString())

    suspend fun addSongsToPlaylist(id: String, songIds: List<String>) {
        val arr = JSONArray().also { songIds.forEach(it::put) }
        postJson("/api/v1/playlist/$id/songs", JSONObject().put("songIds", arr).toString())
    }

    suspend fun removeSongFromPlaylist(id: String, index: Int) = delete("/api/v1/playlist/$id/songs/$index")

    suspend fun reorderPlaylist(id: String, newSongIds: List<String>) {
        val arr = JSONArray().also { newSongIds.forEach(it::put) }
        putJson("/api/v1/playlist/$id/songs", JSONObject().put("songIds", arr).toString())
    }

    suspend fun deletePlaylist(id: String) = delete("/api/v1/playlist/$id")

    suspend fun appendQueueToPlaylist(id: String) = post("/api/v1/playlist/$id/add-queue")

    suspend fun addAlbumToPlaylist(playlistId: String, albumId: String) =
        post("/api/v1/playlist/$playlistId/album/$albumId")

    suspend fun saveQueueAsPlaylist(name: String): Playlist = withContext(Dispatchers.IO) {
        val body = JSONObject().put("name", name).toString()
        JSONObject(postJson("/api/v1/playlist/from-queue", body)).toPlaylist()
    }

    // ── Devices ───────────────────────────────────────────────────────────

    suspend fun getDevices(): DevicesResponse = withContext(Dispatchers.IO) {
        val obj = JSONObject(get("/api/v1/devices"))
        DevicesResponse(
            devices = obj.optJSONArray("devices")?.let { arr ->
                (0 until arr.length()).map { arr.getJSONObject(it).run {
                    AudioDevice(optString("name", ""), optString("description", ""))
                }}
            } ?: emptyList(),
            current = obj.optString("current", "")
        )
    }

    suspend fun setDevice(name: String) =
        postJson("/api/v1/device", JSONObject().put("name", name).toString())

    // ── Satellites ────────────────────────────────────────────────────────

    suspend fun getSatellites(): List<SatelliteInfo> = withContext(Dispatchers.IO) {
        JSONArray(get("/api/v1/satellites")).toSatelliteList()
    }

    suspend fun setActiveSatellite(name: String) =
        postJson("/api/v1/satellites/active", JSONObject().put("name", name).toString())

    suspend fun setSatelliteDevice(satelliteName: String, device: String) =
        postJson("/api/v1/satellites/$satelliteName/device", JSONObject().put("device", device).toString())

    suspend fun refreshSatelliteDevices(satelliteName: String): List<SatelliteInfo> = withContext(Dispatchers.IO) {
        JSONArray(postForBody("/api/v1/satellites/$satelliteName/devices/refresh")).toSatelliteList()
    }

    // ── Cover art URL helper ──────────────────────────────────────────────

    // The server stores cover art in Track objects as a pre-built path
    // "/api/v1/coverart/{id}", while library objects (Artist/Album/Song) store
    // the raw Subsonic ID. Handle both forms.
    fun coverArtUrl(id: String, size: Int = 300): String =
        if (id.startsWith("/")) "$base$id?size=$size"
        else "$base/api/v1/coverart/$id?size=$size"

    // ── Low-level HTTP helpers ────────────────────────────────────────────

    private suspend fun get(path: String): String = withContext(Dispatchers.IO) {
        val resp = client.newCall(Request.Builder().url("$base$path").build()).execute()
        if (!resp.isSuccessful) throw ApiException(resp.code, resp.message)
        resp.body?.string() ?: ""
    }

    private suspend fun post(path: String): String = withContext(Dispatchers.IO) {
        val req = Request.Builder().url("$base$path").post("".toRequestBody()).build()
        val resp = client.newCall(req).execute()
        if (!resp.isSuccessful) throw ApiException(resp.code, resp.message)
        resp.body?.string() ?: ""
    }

    private suspend fun postJson(path: String, json: String): String = withContext(Dispatchers.IO) {
        val req = Request.Builder().url("$base$path").post(json.toRequestBody(JSON_TYPE)).build()
        val resp = client.newCall(req).execute()
        if (!resp.isSuccessful) throw ApiException(resp.code, resp.message)
        resp.body?.string() ?: ""
    }

    private suspend fun putJson(path: String, json: String): String = withContext(Dispatchers.IO) {
        val req = Request.Builder().url("$base$path").put(json.toRequestBody(JSON_TYPE)).build()
        val resp = client.newCall(req).execute()
        if (!resp.isSuccessful) throw ApiException(resp.code, resp.message)
        resp.body?.string() ?: ""
    }

    private suspend fun postForBody(path: String): String = withContext(Dispatchers.IO) {
        val req = Request.Builder().url("$base$path").post("".toRequestBody()).build()
        val resp = client.newCall(req).execute()
        if (!resp.isSuccessful) throw ApiException(resp.code, resp.message)
        resp.body?.string() ?: ""
    }

    private suspend fun delete(path: String): String = withContext(Dispatchers.IO) {
        val req = Request.Builder().url("$base$path").delete().build()
        val resp = client.newCall(req).execute()
        if (!resp.isSuccessful) throw ApiException(resp.code, resp.message)
        resp.body?.string() ?: ""
    }
}

class ApiException(val code: Int, message: String) : Exception("HTTP $code: $message")

class SessionCookieJar : CookieJar {
    private val cookies = mutableListOf<Cookie>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        this.cookies.removeAll { c -> cookies.any { it.name == c.name } }
        this.cookies.addAll(cookies)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> = cookies.toList()

    fun setToken(name: String, value: String, domain: String) {
        cookies.removeAll { it.name == name }
        cookies.add(Cookie.Builder().name(name).value(value).domain(domain).path("/").build())
    }
}
