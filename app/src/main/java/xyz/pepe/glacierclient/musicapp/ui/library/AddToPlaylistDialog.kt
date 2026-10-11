package xyz.pepe.glacierclient.musicapp.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import xyz.pepe.glacierclient.musicapp.di.AppContainer
import xyz.pepe.glacierclient.musicapp.domain.model.Track

/** Trailing icon that opens a dialog to add [track] to an existing or brand-new playlist. */
@Composable
fun AddToPlaylistButton(container: AppContainer, track: Track, modifier: Modifier = Modifier) {
  var open by remember { mutableStateOf(false) }
  IconButton(onClick = { open = true }, modifier = modifier) {
    Icon(Icons.Rounded.PlaylistAdd, contentDescription = "Add to playlist")
  }
  if (open) AddToPlaylistDialogHost(container, track, onDismiss = { open = false })
}

/** The dialog on its own, for call sites (e.g. a track's long-press/⋮ menu) that manage their own
 * open state instead of using [AddToPlaylistButton]'s built-in icon. */
@Composable
fun AddToPlaylistDialogHost(container: AppContainer, track: Track, onDismiss: () -> Unit) {
  val playlists by container.playlists.observePlaylists().collectAsState(initial = emptyList())
  val scope = rememberCoroutineScope()
  var showNewPlaylist by remember { mutableStateOf(false) }

  if (showNewPlaylist) {
    NewPlaylistDialog(
      onCreate = { name -> scope.launch { val id = container.playlists.create(name); container.playlists.addTrack(id, track); container.notifications.post("Playlist created", "\"$name\" with ${track.title}") } },
      onDismiss = { showNewPlaylist = false; onDismiss() },
    )
    return
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add \"${track.title}\" to…") },
    text = {
      Column(androidx.compose.ui.Modifier.height(280.dp)) {
        androidx.compose.foundation.layout.Row(
          Modifier.fillMaxWidth().clickable { showNewPlaylist = true }.padding(vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Text("New playlist", modifier = Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.primary)
        }
        LazyColumn {
          items(playlists.size, key = { playlists[it].id }) { i ->
            val p = playlists[i]
            androidx.compose.foundation.layout.Row(
              Modifier.fillMaxWidth().clickable { scope.launch { if (container.playlists.addTrack(p.id, track)) container.notifications.post("Added to ${p.name}", "${track.title} — ${track.artist}") }; onDismiss() }.padding(vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(Icons.Rounded.QueueMusic, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(p.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 16.dp))
              Text("${p.trackCount}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
