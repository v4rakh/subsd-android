package de.varakh.subsd.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.varakh.subsd.R
import de.varakh.subsd.data.model.Playlist

@Composable
fun PlaylistPickerDialog(
    playlists: List<Playlist>,
    onPick: (Playlist) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_pick_playlist_title)) },
        text = {
            if (playlists.isEmpty()) {
                Text(stringResource(R.string.playlists_empty))
            } else {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                    items(playlists, key = { it.id }) { playlist ->
                        ListItem(
                            headlineContent = {
                                Text(playlist.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                            modifier = Modifier.clickable { onPick(playlist); onDismiss() }
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
