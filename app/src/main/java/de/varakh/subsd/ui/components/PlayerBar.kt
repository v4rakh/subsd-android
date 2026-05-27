package de.varakh.subsd.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import de.varakh.subsd.R
import de.varakh.subsd.SubsdApp
import de.varakh.subsd.data.model.PlayerState
import de.varakh.subsd.ui.MainViewModel
import kotlin.math.roundToInt

@Composable
fun PlayerBar(vm: MainViewModel, modifier: Modifier = Modifier) {
    val state = vm.playerState
    val track = state.currentTrack
    var expanded by remember { mutableStateOf(false) }

    if (track == null) return

    if (expanded) {
        ExpandedPlayer(vm, state, onCollapse = { expanded = false })
    } else {
        MiniPlayer(vm, state, modifier = modifier, onExpand = { expanded = true })
    }
}

@Composable
private fun MiniPlayer(
    vm: MainViewModel,
    state: PlayerState,
    modifier: Modifier = Modifier,
    onExpand: () -> Unit
) {
    val track = state.currentTrack ?: return

    Surface(
        tonalElevation = 8.dp,
        modifier = modifier.fillMaxWidth().clickable { onExpand() }
    ) {
        Column {
            LinearProgressIndicator(
                progress = { if (state.duration > 0) (state.position / state.duration).toFloat() else 0f },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                CoverArt(track.coverArt, size = 48)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(track.title, style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { vm.prev() }) {
                    Icon(Icons.Default.SkipPrevious, stringResource(R.string.player_previous))
                }
                IconButton(onClick = { vm.playPause() }) {
                    Icon(
                        if (state.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        stringResource(if (state.playing) R.string.player_pause else R.string.player_play)
                    )
                }
                IconButton(onClick = { vm.next() }) {
                    Icon(Icons.Default.SkipNext, stringResource(R.string.player_next))
                }
            }
        }
    }
}

@Composable
private fun ExpandedPlayer(vm: MainViewModel, state: PlayerState, onCollapse: () -> Unit) {
    val track = state.currentTrack ?: return
    var seekValue by remember(state.currentIdx) { mutableStateOf<Float?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Collapse handle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(Icons.Default.KeyboardArrowDown, stringResource(R.string.player_collapse))
                }
                Text(stringResource(R.string.player_now_playing), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(48.dp))
            }

            Spacer(Modifier.height(24.dp))

            // Cover art
            CoverArt(
                coverArtId = track.coverArt,
                size = 300,
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(Modifier.height(24.dp))

            // Track info
            Text(track.title, style = MaterialTheme.typography.titleLarge,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(track.artist, style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(track.album, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)

            Spacer(Modifier.height(24.dp))

            // Seek slider
            val displayPosition = seekValue?.toDouble() ?: state.position
            Slider(
                value = seekValue ?: (if (state.duration > 0) state.position.toFloat() else 0f),
                onValueChange = { seekValue = it },
                onValueChangeFinished = {
                    seekValue?.let { vm.seek(it.toDouble()) }
                    seekValue = null
                },
                valueRange = 0f..state.duration.toFloat().coerceAtLeast(1f),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(displayPosition), style = MaterialTheme.typography.bodySmall)
                Text(formatTime(state.duration), style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(16.dp))

            // Playback controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { vm.toggleShuffle() }) {
                    Icon(
                        Icons.Default.Shuffle, stringResource(R.string.player_shuffle),
                        tint = if (state.shuffle) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { vm.prev() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.SkipPrevious, stringResource(R.string.player_previous), modifier = Modifier.size(32.dp))
                }
                FilledIconButton(onClick = { vm.playPause() }, modifier = Modifier.size(64.dp)) {
                    Icon(
                        if (state.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        stringResource(if (state.playing) R.string.player_pause else R.string.player_play),
                        modifier = Modifier.size(36.dp)
                    )
                }
                IconButton(onClick = { vm.next() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.SkipNext, stringResource(R.string.player_next), modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = { vm.toggleRepeat() }) {
                    Icon(
                        Icons.Default.Repeat, stringResource(R.string.player_repeat),
                        tint = if (state.repeat) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Volume
            val volumeDesc = stringResource(R.string.player_volume)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.VolumeDown, volumeDesc, modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = state.volume.toFloat(),
                    onValueChange = { vm.setVolume(it.roundToInt()) },
                    valueRange = 0f..100f,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
                Icon(Icons.AutoMirrored.Filled.VolumeUp, volumeDesc, modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(8.dp))

            // ReplayGain selector
            val replayGainOptions = listOf("no", "track", "album")
            val replayGainLabels = mapOf(
                "no" to stringResource(R.string.player_replaygain_off),
                "track" to stringResource(R.string.player_replaygain_track),
                "album" to stringResource(R.string.player_replaygain_album)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.player_replaygain),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(80.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    replayGainOptions.forEach { mode ->
                        val selected = state.replayGain == mode
                        FilterChip(
                            selected = selected,
                            onClick = { vm.setReplayGain(mode) },
                            label = { Text(replayGainLabels[mode] ?: mode, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }
            }

            // Codec info
            val codecInfo = track.suffix.ifBlank { "" }.let { s ->
                buildString {
                    if (s.isNotBlank()) append(s.uppercase())
                    if (track.bitRate > 0) {
                        if (isNotEmpty()) append(" · ")
                        append("${track.bitRate}kbps")
                    }
                    if (track.samplingRate > 0) {
                        if (isNotEmpty()) append(" · ")
                        append("${track.samplingRate / 1000}kHz")
                    }
                }
            }
            if (codecInfo.isNotBlank()) {
                Text(codecInfo, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun CoverArt(
    coverArtId: String,
    size: Int = 64,
    modifier: Modifier = Modifier.size(size.dp).clip(RoundedCornerShape(4.dp))
) {
    val url = if (coverArtId.isNotBlank()) SubsdApp.instance.api.coverArtUrl(coverArtId, size) else null
    if (url != null) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
            Icon(
                Icons.Default.MusicNote, null,
                modifier = Modifier.align(Alignment.Center).size((size * 0.5).dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatTime(secs: Double): String {
    val s = secs.toLong().coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}
