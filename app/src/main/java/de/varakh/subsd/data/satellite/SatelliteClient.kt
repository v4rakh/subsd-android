package de.varakh.subsd.data.satellite

import android.util.Log
import de.varakh.subsd.data.prefs.Config
import de.varakh.subsd.proto.*
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext

private const val TAG = "SatelliteClient"

// State snapshot sent upstream to the daemon on every ExoPlayer change.
data class SatelliteState(
    val status: Status,
    val position: Double,
    val duration: Double,
    val currentUrl: String,
    val volume: Int,
    val audioDevice: String = ""
)

// PlaybackService implements this to receive commands from the daemon.
interface SatelliteCommandHandler {
    fun onPlay(url: String, position: Double, id: String, title: String, artist: String, album: String)
    fun onPause()
    fun onResume()
    fun onStop()
    fun onSeek(position: Double)
    fun onSetVolume(volume: Int)
    fun onSetAudioDevice(device: String)
    fun getState(): SatelliteState
}

class SatelliteClient(private val handler: SatelliteCommandHandler) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var connectJob: Job? = null
    private var channel: ManagedChannel? = null

    /** Conflated channel: only the latest state matters for upstream pushes. */
    private val stateUpdates = Channel<SatelliteState>(Channel.CONFLATED)
    /** Unbuffered channel for TrackEnded events. */
    private val trackEndedEvents = Channel<Unit>(Channel.UNLIMITED)

    fun start(cfg: Config) {
        connectJob?.cancel()
        connectJob = scope.launch { connectLoop(cfg) }
    }

    fun stop() {
        connectJob?.cancel()
        connectJob = null
        channel?.shutdown()
        channel = null
    }

    /** Called from PlaybackService whenever ExoPlayer state changes. */
    fun pushState(state: SatelliteState) {
        stateUpdates.trySend(state)
    }

    /** Called from PlaybackService when the current track finishes naturally. */
    fun notifyTrackEnded() {
        trackEndedEvents.trySend(Unit)
    }

    private suspend fun connectLoop(cfg: Config) {
        while (true) {
            try {
                Log.i(TAG, "Connecting to ${cfg.resolvedGrpcAddr} as '${cfg.resolvedDeviceName}'")
                connect(cfg)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Connection error: ${e.message}")
            }
            delay(5_000)
        }
    }

    private suspend fun connect(cfg: Config) {
        val addr = cfg.resolvedGrpcAddr
        val cleanAddr = addr.removePrefix("https://").removePrefix("http://")
        val colonIdx = cleanAddr.lastIndexOf(':')
        val host = if (colonIdx > 0) cleanAddr.substring(0, colonIdx) else cleanAddr
        val port = if (colonIdx > 0) cleanAddr.substring(colonIdx + 1).toIntOrNull() ?: 9090 else 9090

        Log.i(TAG, "Opening gRPC channel to $host:$port tls=${cfg.grpcTls} token=${cfg.grpcToken.isNotBlank()}")
        val ch = if (cfg.grpcTls) {
            ManagedChannelBuilder.forAddress(host, port).build()
        } else {
            ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
        }
        channel = ch

        var stub = SatelliteServiceGrpcKt.SatelliteServiceCoroutineStub(ch)
        if (cfg.grpcToken.isNotBlank()) {
            val key = Metadata.Key.of("x-subsd-token", Metadata.ASCII_STRING_MARSHALLER)
            val headers = Metadata()
            headers.put(key, cfg.grpcToken)
            stub = stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers))
        }

        val outgoing = Channel<SatelliteMessage>(Channel.UNLIMITED)

        coroutineScope {
            // Registration (first message)
            Log.i(TAG, "Sending registration as '${cfg.resolvedDeviceName}'")
            outgoing.send(satelliteMessage {
                registration = registration { name = cfg.resolvedDeviceName }
            })

            // Heartbeat sender
            val heartbeatJob = launch {
                while (true) {
                    delay(5_000)
                    outgoing.trySend(satelliteMessage {
                        heartbeat = heartbeat { timestampMs = System.currentTimeMillis() }
                    })
                }
            }

            // State update relay
            val stateJob = launch {
                stateUpdates.receiveAsFlow().collect { s ->
                    outgoing.trySend(satelliteMessage {
                        state = playerState {
                            status = s.status
                            position = s.position
                            duration = s.duration
                            currentUrl = s.currentUrl
                            volume = s.volume
                            audioDevice = s.audioDevice
                        }
                    })
                }
            }

            // TrackEnded relay
            val trackEndedJob = launch {
                for (event in trackEndedEvents) {
                    outgoing.trySend(satelliteMessage { trackEnded = trackEnded { } })
                }
            }

            try {
                Log.i(TAG, "Starting bidirectional stream")
                stub.connect(outgoing.receiveAsFlow()).collect { msg ->
                    when {
                        msg.hasCommand() -> {
                            Log.d(TAG, "Received command: ${msg.command.type}")
                            withContext(Dispatchers.Main) { handleCommand(msg.command) }
                        }
                        msg.hasRequestDevices() -> {
                            Log.i(TAG, "Received RequestDevices — sending empty device list")
                            outgoing.trySend(satelliteMessage {
                                devices = deviceList { }
                            })
                        }
                        else -> Log.d(TAG, "Received unknown server message")
                    }
                }
                Log.i(TAG, "Stream ended normally")
            } finally {
                heartbeatJob.cancel()
                stateJob.cancel()
                trackEndedJob.cancel()
                outgoing.close()
                ch.shutdown()
                channel = null
            }
        }
    }

    private fun handleCommand(cmd: Command) {
        Log.d(TAG, "Command: ${cmd.type}")
        when (cmd.type) {
            CommandType.PLAY -> handler.onPlay(
                cmd.url, cmd.position, cmd.id, cmd.title, cmd.artist, cmd.album
            )
            CommandType.PAUSE -> handler.onPause()
            CommandType.RESUME -> handler.onResume()
            CommandType.STOP -> handler.onStop()
            CommandType.SEEK -> handler.onSeek(cmd.position)
            CommandType.SET_VOLUME -> handler.onSetVolume(cmd.volume)
            CommandType.SET_AUDIO_DEVICE -> handler.onSetAudioDevice(cmd.device)
            else -> Log.w(TAG, "Unknown command type: ${cmd.type}")
        }
    }
}
