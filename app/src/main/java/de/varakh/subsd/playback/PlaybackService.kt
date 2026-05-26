package de.varakh.subsd.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import de.varakh.subsd.R
import de.varakh.subsd.SubsdApp
import de.varakh.subsd.data.satellite.SatelliteClient
import de.varakh.subsd.data.satellite.SatelliteCommandHandler
import de.varakh.subsd.data.satellite.SatelliteState
import de.varakh.subsd.proto.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val TAG = "PlaybackService"
private const val CHANNEL_ID = "subsd_playback"
private const val NOTIF_ID = 1

class PlaybackService : Service(), SatelliteCommandHandler {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var satelliteClient: SatelliteClient? = null

    /** Offset baked into the stream URL via start= param (for transcoded streams). */
    @Volatile private var streamStartOffset: Double = 0.0
    @Volatile private var currentStreamUrl: String = ""
    @Volatile private var userVolume: Int = 100

    companion object {
        var instance: PlaybackService? = null
            private set
    }

    // ── Service lifecycle ─────────────────────────────────────────────────

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        startForegroundCompat()

        player = ExoPlayer.Builder(applicationContext).build().also { p ->
            p.playWhenReady = false
            p.addListener(ExoListener(p))
        }
        mediaSession = MediaSession.Builder(this, player!!).build()

        // Observe config changes and (re)start the satellite client
        scope.launch {
            SubsdApp.instance.prefs.config.collect { cfg ->
                if (cfg.isConfigured) {
                    satelliteClient?.stop()
                    satelliteClient = SatelliteClient(this@PlaybackService).also {
                        it.start(cfg)
                    }
                }
            }
        }

        Log.i(TAG, "Service created")
    }

    override fun onDestroy() {
        satelliteClient?.stop()
        scope.cancel()
        mediaSession?.release()
        mediaSession = null
        player?.release()
        player = null
        instance = null
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Keep running for satellite connectivity
    }

    // ── SatelliteCommandHandler ───────────────────────────────────────────

    override fun onPlay(url: String, position: Double, id: String, title: String, artist: String, album: String) {
        val p = player ?: return
        Log.i(TAG, "onPlay: '$title' by '$artist' from '$album' pos=$position url=$url")
        val resolvedUrl = resolveStreamUrl(url)
        streamStartOffset = parseStartParam(url)
        currentStreamUrl = resolvedUrl

        p.pause()
        p.clearMediaItems()
        p.addMediaItem(MediaItem.fromUri(resolvedUrl))
        p.prepare()

        if (position > 0 && streamStartOffset == 0.0) {
            // Non-transcoded: seek directly after prepare
            p.seekTo((position * 1000).toLong())
        }
        p.play()
    }

    override fun onPause() {
        Log.i(TAG, "onPause")
        player?.pause()
    }

    override fun onResume() {
        Log.i(TAG, "onResume")
        val p = player ?: return
        if (p.playbackState == Player.STATE_IDLE && p.mediaItemCount > 0) p.prepare()
        p.play()
    }

    override fun onStop() {
        Log.i(TAG, "onStop")
        player?.stop()
        player?.clearMediaItems()
        currentStreamUrl = ""
        streamStartOffset = 0.0
    }

    override fun onSeek(position: Double) {
        Log.i(TAG, "onSeek: $position")
        val p = player ?: return
        if (isTranscodedUrl(currentStreamUrl)) {
            reloadWithOffset(p, currentStreamUrl, position)
        } else {
            p.seekTo((position * 1000).toLong())
        }
    }

    override fun onSetVolume(volume: Int) {
        Log.i(TAG, "onSetVolume: $volume")
        userVolume = volume
        player?.volume = (volume / 100f).coerceIn(0f, 1f)
    }

    override fun onSetAudioDevice(device: String) {
        Log.d(TAG, "onSetAudioDevice: $device (ExoPlayer uses system audio routing)")
    }

    override fun getState(): SatelliteState {
        val p = player
        val status = when {
            p == null -> Status.IDLE
            p.isPlaying -> Status.PLAYING
            p.playbackState == Player.STATE_READY && !p.playWhenReady -> Status.PAUSED
            else -> Status.IDLE
        }
        val position = if (p != null) {
            (p.currentPosition / 1000.0) + streamStartOffset
        } else 0.0
        val duration = if (p != null && p.duration != C.TIME_UNSET) {
            (p.duration / 1000.0) + streamStartOffset
        } else 0.0
        return SatelliteState(
            status = status,
            position = position,
            duration = duration,
            currentUrl = currentStreamUrl,
            volume = userVolume
        )
    }

    // ── ExoPlayer listener ────────────────────────────────────────────────

    private inner class ExoListener(private val p: ExoPlayer) : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Log.i(TAG, "ExoPlayer isPlaying=$isPlaying")
            pushState()
        }
        override fun onPlaybackStateChanged(state: Int) {
            val stateName = when (state) {
                Player.STATE_IDLE -> "IDLE"
                Player.STATE_BUFFERING -> "BUFFERING"
                Player.STATE_READY -> "READY"
                Player.STATE_ENDED -> "ENDED"
                else -> "UNKNOWN($state)"
            }
            Log.i(TAG, "ExoPlayer state=$stateName")
            if (state == Player.STATE_ENDED) {
                Log.i(TAG, "Track ended — notifying satellite client")
                satelliteClient?.notifyTrackEnded()
                streamStartOffset = 0.0
            }
            pushState()
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                streamStartOffset = 0.0
            }
            pushState()
        }
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            Log.e(TAG, "ExoPlayer error: ${error.errorCodeName} — ${error.message}")
        }

        private fun pushState() {
            satelliteClient?.pushState(getState())
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * Resolve the stream URL against the configured daemon base URL so it works
     * whether the daemon is reached via local IP or an external domain.
     * The daemon sends absolute URLs that may use a different host/scheme than
     * what the Android app is configured with. We rewrite the path portion to
     * use the configured base to ensure auth cookies are sent correctly.
     */
    private fun resolveStreamUrl(url: String): String {
        val prefix = "/api/v1/stream/"
        val idx = url.indexOf(prefix)
        if (idx >= 0) {
            val pathAndQuery = url.substring(idx)
            val base = SubsdApp.instance.api.baseUrl
            val resolved = "$base$pathAndQuery"
            Log.i(TAG, "resolveStreamUrl: $url → $resolved")
            return resolved
        }
        Log.i(TAG, "resolveStreamUrl: using url as-is: $url")
        return url
    }

    private fun parseStartParam(url: String): Double {
        return try {
            android.net.Uri.parse(url).getQueryParameter("start")?.toDoubleOrNull() ?: 0.0
        } catch (_: Exception) { 0.0 }
    }

    private fun isTranscodedUrl(url: String): Boolean {
        if (url.isBlank()) return false
        val uri = android.net.Uri.parse(url)
        return !uri.getQueryParameter("format").isNullOrBlank() ||
            (uri.getQueryParameter("max_bitrate")?.toIntOrNull() ?: 0) > 0
    }

    private fun reloadWithOffset(p: ExoPlayer, url: String, seekSecs: Double) {
        val newUrl = buildUrlWithStart(url, seekSecs)
        val wasPlaying = p.playWhenReady
        streamStartOffset = seekSecs
        currentStreamUrl = newUrl

        p.stop()
        p.clearMediaItems()
        p.addMediaItem(MediaItem.fromUri(newUrl))
        p.prepare()
        if (wasPlaying) p.play()
    }

    private fun buildUrlWithStart(url: String, startSecs: Double): String {
        val uri = android.net.Uri.parse(url)
        val builder = uri.buildUpon().clearQuery()
        uri.queryParameterNames.forEach { key ->
            if (key != "start") builder.appendQueryParameter(key, uri.getQueryParameter(key))
        }
        if (startSecs > 0) {
            builder.appendQueryParameter("start", String.format(java.util.Locale.US, "%.3f", startSecs))
        }
        return builder.build().toString()
    }

    // ── Notification ──────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_LOW).apply {
                    description = getString(R.string.notification_channel_desc)
                }
            )
        }
    }

    private fun startForegroundCompat() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }
}
