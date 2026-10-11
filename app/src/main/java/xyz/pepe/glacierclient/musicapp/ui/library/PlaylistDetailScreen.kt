@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package xyz.pepe.glacierclient.musicapp.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import xyz.pepe.glacierclient.musicapp.ui.components.GlassCard
import xyz.pepe.glacierclient.musicapp.ui.components.HeroHeader
import xyz.pepe.glacierclient.musicapp.ui.components.SectionHeader
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallMerge
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import xyz.pepe.glacierclient.musicapp.data.local.PlaylistTrackEntity
import xyz.pepe.glacierclient.musicapp.di.AppContainer
import xyz.pepe.glacierclient.musicapp.domain.util.PlaylistTransfer
import xyz.pepe.glacierclient.musicapp.ui.components.EmptyState
import xyz.pepe.glacierclient.musicapp.ui.components.PageScaffold

@Composable
fun PlaylistDetailScreen(container: AppContainer, playlistId: Long, onBack: () -> Unit) {
  val playlists by container.playlists.observePlaylists().collectAsState(initial = emptyList())
  val playlist = playlists.firstOrNull { it.id == playlistId }
  val loadedTracks by container.playlists.observeTracks(playlistId).collectAsState(initial = null)
  val tracks = loadedTracks ?: emptyList()
  val covers by container.playlists.observeCovers().collectAsState(initial = emptyMap())
  var showDelete by rememberSaveable { mutableStateOf(false) }
  val scope = rememberCoroutineScope()
  var menuOpen by remember { mutableStateOf(false) }
  var sortOpen by remember { mutableStateOf(false) }
  var showRename by rememberSaveable { mutableStateOf(false) }
  var showMerge by rememberSaveable { mutableStateOf(false) }
  val context = androidx.compose.ui.platform.LocalContext.current

  if (showRename && playlist != null) {
    RenameDialog(
      initial = playlist.name,
      onRename = { name -> scope.launch { container.playlists.rename(playlistId, name) } },
      onDismiss = { showRename = false },
    )
  }

  if (showDelete) {
    AlertDialog(
      onDismissRequest = { showDelete = false },
      title = { Text("Delete playlist?") },
      text = { Text("\"${playlist?.name ?: "This playlist"}\" will be removed. The songs themselves stay in your library.") },
      confirmButton = { TextButton(onClick = { showDelete = false; scope.launch { container.playlists.delete(playlistId) }; onBack() }) { Text("Delete") } },
      dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } },
    )
  }

  if (showMerge) {
    val others = playlists.filter { it.id != playlistId }
    AlertDialog(
      onDismissRequest = { showMerge = false },
      title = { Text("Merge into \"${playlist?.name ?: "playlist"}\"") },
      text = {
        if (others.isEmpty()) Text("You have no other playlists to merge in.")
        else LazyColumn(Modifier.height(280.dp)) {
          items(others.size, key = { others[it].id }) { i ->
            val o = others[i]
            Row(
              Modifier.fillMaxWidth().clickable {
                showMerge = false
                scope.launch {
                  val added = container.playlists.merge(o.id, playlistId)
                  container.notifications.post("Playlists merged", "$added new songs from \"${o.name}\"")
                }
              }.padding(vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(o.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
              Text("${o.trackCount}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      },
      confirmButton = {},
      dismissButton = { TextButton(onClick = { showMerge = false }) { Text("Cancel") } },
    )
  }

  PageScaffold(
    title = playlist?.name ?: "Playlist",
    onBack = onBack,
    actions = {
      Box {
        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = "More") }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
          DropdownMenuItem(
            text = { Text("Rename") }, leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
            onClick = { menuOpen = false; showRename = true },
          )
          DropdownMenuItem(
            text = { Text("Duplicate") }, leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
            onClick = {
              menuOpen = false
              scope.launch {
                container.playlists.duplicate(playlistId, "${playlist?.name ?: "Playlist"} (copy)")
                container.notifications.post("Playlist duplicated", "\"${playlist?.name}\" was copied")
              }
            },
          )
          DropdownMenuItem(
            text = { Text("Merge another playlist in") }, leadingIcon = { Icon(Icons.Rounded.CallMerge, contentDescription = null) },
            onClick = { menuOpen = false; showMerge = true },
          )
          DropdownMenuItem(
            text = { Text("Download all") }, leadingIcon = { Icon(Icons.Rounded.Download, contentDescription = null) },
            onClick = {
              menuOpen = false
              tracks.forEach { container.downloads.download(it.toTrack()) }
              container.notifications.post("Downloading playlist", "${tracks.size} songs queued (Wi-Fi only follows your setting)")
            },
          )
          DropdownMenuItem(
            text = { Text("Copy as JSON") }, leadingIcon = { Icon(Icons.Rounded.DataObject, contentDescription = null) },
            onClick = {
              menuOpen = false
              scope.launch {
                val json = container.playlists.exportJson(playlistId)
                context.getSystemService(android.content.ClipboardManager::class.java)
                  ?.setPrimaryClip(android.content.ClipData.newPlainText("Playlist", json))
                container.notifications.post("Playlist copied", "Paste it into Import playlist in Library")
              }
            },
          )
          DropdownMenuItem(
            text = { Text("Export as M3U") }, leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
            onClick = { menuOpen = false; xyz.pepe.glacierclient.musicapp.ui.util.exportM3u(context, playlist?.name ?: "playlist", tracks.map { it.toTrack() }) },
          )
          DropdownMenuItem(
            text = { Text("Delete playlist") }, leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
            onClick = { menuOpen = false; showDelete = true },
          )
        }
      }
    },
  ) { padding ->
    if (loadedTracks == null) {
      Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { androidx.compose.material3.LoadingIndicator(Modifier.size(64.dp)) }
      return@PageScaffold
    }
    if (tracks.isEmpty()) {
      Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        EmptyState(icon = Icons.Rounded.QueueMusic, title = "No tracks yet", body = "Add tracks from Search or Home using the playlist-plus icon.")
      }
      return@PageScaffold
    }
    val listState = rememberLazyListState()
    val reorder = rememberReorderableLazyListState(listState) { from, to ->
      val ids = tracks.map { it.id }
      val f = ids.indexOf(from.key); val t = ids.indexOf(to.key)
      if (f < 0 || t < 0) return@rememberReorderableLazyListState
      val ordered = ids.toMutableList().apply { add(t, removeAt(f)) }
      scope.launch { container.playlists.reorder(playlistId, ordered) }
    }
    LazyColumn(Modifier.fillMaxSize().padding(padding), state = listState, contentPadding = PaddingValues(bottom = 24.dp)) {
      item(key = "hero") {
        HeroHeader(
          title = playlist?.name ?: "Playlist",
          subtitle = "${tracks.size} songs",
          artworkUrl = tracks.firstOrNull { it.artworkUrl != null }?.artworkUrl,
          cover = { PlaylistCover(covers[playlistId].orEmpty(), size = null, modifier = Modifier.fillMaxSize(), cornerRadius = 0.dp) },
        )
      }
      item(key = "header") {
        Row(Modifier.fillMaxWidth().padding(16.dp, 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          Button(onClick = { container.player.play(tracks.map { it.toTrack() }, 0) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null); Text("Play", maxLines = 1, modifier = Modifier.padding(start = 4.dp))
          }
          FilledTonalButton(onClick = { container.player.play(tracks.map { it.toTrack() }.shuffled(), 0) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
            Icon(Icons.Rounded.Shuffle, contentDescription = null); Text("Shuffle", maxLines = 1, modifier = Modifier.padding(start = 4.dp))
          }
          Box {
            FilledTonalIconButton(onClick = { sortOpen = true }, modifier = Modifier.size(48.dp)) { Icon(Icons.Rounded.SortByAlpha, contentDescription = "Sort playlist") }
            DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
              PlaylistTransfer.Sort.entries.forEach { sort ->
                DropdownMenuItem(
                  text = { Text("Sort: ${sort.label}") },
                  onClick = { sortOpen = false; scope.launch { container.playlists.sort(playlistId, sort) } },
                )
              }
            }
          }
        }
      }
      item(key = "songs-header") { SectionHeader("Songs", icon = Icons.Rounded.QueueMusic) }
      items(tracks.size, key = { tracks[it].id }) { i ->
        val row = tracks[i]
        ReorderableItem(reorder, key = row.id) {
          GlassCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp), shape = RoundedCornerShape(18.dp), onClick = { container.player.play(tracks.map { it.toTrack() }, i) }) {
            Row(Modifier.fillMaxWidth().padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
              if (row.artworkUrl != null) {
                AsyncImage(model = row.artworkUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
              } else {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                  Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
              Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                PlaylistTrackText(row)
              }
              IconButton(onClick = { scope.launch { container.playlists.removeTrack(playlistId, row.id) } }) {
                Icon(Icons.Rounded.Close, contentDescription = "Remove from playlist")
              }
              Box(Modifier.size(48.dp).draggableHandle(), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.DragHandle, contentDescription = "Drag to reorder")
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PlaylistTrackText(row: PlaylistTrackEntity) {
  Text(row.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
  Text(row.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun RenameDialog(initial: String, onRename: (String) -> Unit, onDismiss: () -> Unit) {
  var name by remember { mutableStateOf(initial) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Rename playlist") },
    text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
    confirmButton = { TextButton(onClick = { if (name.isNotBlank()) { onRename(name); onDismiss() } }) { Text("Save") } },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
