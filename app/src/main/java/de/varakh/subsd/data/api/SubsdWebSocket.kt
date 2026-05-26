package de.varakh.subsd.data.api

import android.util.Log
import de.varakh.subsd.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject

private const val TAG = "SubsdWebSocket"

sealed class WsEvent {
    data class SatelliteDisconnected(val name: String) : WsEvent()
}

private const val RECONNECT_DELAY_MS = 5_000L

class SubsdWebSocket(private val api: SubsdApi) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var ws: WebSocket? = null
    private var lastUrl: String = ""
    private var intentionalDisconnect = false

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState

    private val _satellites = MutableStateFlow<List<SatelliteInfo>>(emptyList())
    val satellites: StateFlow<List<SatelliteInfo>> = _satellites

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected

    /** One-shot events that need toast display (e.g. satellite disconnected). */
    val events = Channel<WsEvent>(Channel.UNLIMITED)

    fun connect(httpUrl: String) {
        intentionalDisconnect = false
        lastUrl = httpUrl
        doConnect(httpUrl)
    }

    fun disconnect() {
        intentionalDisconnect = true
        ws?.close(1000, "bye")
        ws = null
        _connected.value = false
    }

    private fun doConnect(httpUrl: String) {
        ws?.close(1000, "reconnecting")
        val wsUrl = httpUrl.trimEnd('/')
            .replace("https://", "wss://")
            .replace("http://", "ws://") + "/ws"
        Log.i(TAG, "Connecting to $wsUrl")
        val request = Request.Builder().url(wsUrl).build()
        ws = api.client.newWebSocket(request, Listener())
    }

    private fun scheduleReconnect() {
        if (intentionalDisconnect || lastUrl.isBlank()) return
        scope.launch {
            Log.i(TAG, "Reconnecting in ${RECONNECT_DELAY_MS}ms")
            delay(RECONNECT_DELAY_MS)
            if (!intentionalDisconnect && lastUrl.isNotBlank()) {
                doConnect(lastUrl)
            }
        }
    }

    private inner class Listener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.i(TAG, "Connected")
            _connected.value = true
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            try {
                val obj = JSONObject(text)
                when (obj.optString("type")) {
                    "satellites" -> {
                        val arr = obj.optJSONArray("satellites") ?: return
                        val list = arr.toSatelliteList()
                        Log.i(TAG, "Satellite list received: ${list.map { "${it.name}(active=${it.active})" }}")
                        _satellites.value = list
                    }
                    "satellite_disconnected" -> {
                        val name = obj.optString("name")
                        Log.i(TAG, "Satellite disconnected: $name")
                        if (name.isNotBlank()) events.trySend(WsEvent.SatelliteDisconnected(name))
                    }
                    else -> {
                        // Player state broadcast — the primary message type
                        if (obj.has("playing") || obj.has("queue")) {
                            val state = obj.toPlayerState()
                            Log.d(TAG, "Player state: playing=${state.playing} idx=${state.currentIdx} queue=${state.queue.size} shuffle=${state.shuffle} repeat=${state.repeat}")
                            _playerState.value = state
                        } else {
                            Log.d(TAG, "Unknown WS message: ${text.take(120)}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Parse error: ${e.message} — text=${text.take(200)}")
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.w(TAG, "Failure: ${t.message} response=${response?.code}")
            _connected.value = false
            ws = null
            scheduleReconnect()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.i(TAG, "Closed: $code $reason")
            _connected.value = false
            ws = null
            // Only reconnect on unexpected closes (not our own intentional disconnect)
            if (code != 1000) scheduleReconnect()
        }
    }
}
