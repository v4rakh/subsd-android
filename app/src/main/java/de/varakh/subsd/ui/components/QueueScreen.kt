package de.varakh.subsd.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import de.varakh.subsd.data.model.Track
import de.varakh.subsd.ui.MainViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(vm: MainViewModel) {
    val state = vm.playerState
    val queue = state.queue
    val lazyListState = rememberLazyListState()

    // Local copy for optimistic drag-and-drop reordering.
    // Each entry carries a slot key (Long) that is unique and stable for the item's lifetime,
    // which allows the same track to appear twice without key collisions.
    var nextKey by remember { mutableLongStateOf(0L) }
    val localQueue = remember { mutableStateListOf<Pair<Long, Track>>() }
    LaunchedEffect(queue) {
        localQueue.clear()
        queue.forEach { track -> localQueue.add(nextKey++ to track) }
    }

    // Track drag start/end so we send a single API call per drag gesture
    var dragFromIdx by remember { mutableIntStateOf(-1) }
    var dragToIdx by remember { mutableIntStateOf(-1) }

    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        if (dragFromIdx == -1) dragFromIdx = from.index
        dragToIdx = to.index
        localQueue.add(to.index, localQueue.removeAt(from.index))
    }

    // Send moveInQueue only once when the drag ends
    LaunchedEffect(reorderState.isAnyItemDragging) {
        if (!reorderState.isAnyItemDragging && dragFromIdx != -1 && dragToIdx != -1 && dragFromIdx != dragToIdx) {
            vm.moveInQueue(dragFromIdx, dragToIdx)
        }
        if (!reorderState.isAnyItemDragging) {
            dragFromIdx = -1
            dragToIdx = -1
        }
    }

    // Scroll to current track when it changes (but not while dragging)
    LaunchedEffect(state.currentIdx) {
        if (!reorderState.isAnyItemDragging && state.currentIdx in localQueue.indices) {
            lazyListState.animateScrollToItem(state.currentIdx)
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.queue_title)) },
            actions = {
                Text(
                    pluralStringResource(R.plurals.track_count, queue.size, queue.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 4.dp)
                )
                IconButton(onClick = { vm.toggleShuffle() }) {
                    Icon(
                        Icons.Default.Shuffle,
                        stringResource(R.string.player_shuffle),
                        tint = if (state.shuffle) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { vm.toggleRepeat() }) {
                    Icon(
                        Icons.Default.Repeat,
                        stringResource(R.string.player_repeat),
                        tint = if (state.repeat) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (queue.isNotEmpty()) {
                    IconButton(onClick = { vm.clearQueue() }) {
                        Icon(Icons.Default.DeleteSweep, stringResource(R.string.queue_clear))
                    }
                }
            }
        )

        if (queue.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, stringResource(R.string.queue_empty_desc),
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.queue_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize()) {
                itemsIndexed(localQueue, key = { _, entry -> entry.first }) { idx, (key, track) ->
                    ReorderableItem(reorderState, key = key) {
                        QueueTrackItem(
                            track = track,
                            isCurrent = idx == state.currentIdx,
                            onPlay = { vm.jump(idx) },
                            onRemove = { vm.dequeue(idx) },
                            dragHandleModifier = Modifier.draggableHandle()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueTrackItem(
    track: Track,
    isCurrent: Boolean,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
    dragHandleModifier: Modifier = Modifier
) {
    val bg = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    else MaterialTheme.colorScheme.background

    ListItem(
        headlineContent = {
            Text(
                track.title,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = if (isCurrent) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            Text(
                buildString {
                    append(track.artist)
                    if (track.duration > 0) append(" · ${formatDuration(track.duration)}")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        },
        leadingContent = {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                if (isCurrent) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, stringResource(R.string.queue_now_playing),
                        tint = MaterialTheme.colorScheme.primary)
                } else {
                    CoverArt(track.coverArt, size = 48)
                }
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
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Close, stringResource(R.string.queue_remove),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        modifier = Modifier
            .background(bg)
            .clickable { onPlay() }
    )
    HorizontalDivider(thickness = 0.5.dp)
}
