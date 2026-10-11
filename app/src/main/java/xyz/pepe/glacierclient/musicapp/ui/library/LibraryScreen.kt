package xyz.pepe.glacierclient.musicapp.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import xyz.pepe.glacierclient.musicapp.R
import xyz.pepe.glacierclient.musicapp.di.AppContainer
import xyz.pepe.glacierclient.musicapp.domain.model.Playlist
import xyz.pepe.glacierclient.musicapp.ui.components.EmptyState
import xyz.pepe.glacierclient.musicapp.ui.components.GlassCard
import xyz.pepe.glacierclient.musicapp.ui.components.IconChip
import xyz.pepe.glacierclient.musicapp.ui.components.SectionHeader
import xyz.pepe.glacierclient.musicapp.ui.components.LikeButton
import xyz.pepe.glacierclient.musicapp.ui.components.PageScaffold
import xyz.pepe.glacierclient.musicapp.ui.components.SectionTitle
import xyz.pepe.glacierclient.musicapp.ui.components.TrackRow
import xyz.pepe.glacierclient.musicapp.ui.home.SettingsAction

@Composable
fun LibraryScreen(container: AppContainer, openSettings: () -> Unit, onOpenArtist: (String) -> Unit = {}) {
  var openPlaylistId by rememberSaveable { mutableStateOf<Long?>(null) }
  BackHandler(enabled = openPlaylistId != null) { openPlaylistId = null }

  openPlaylistId?.let { id ->
    PlaylistDetailScreen(container, playlistId = id, onBack = { openPlaylistId = null })
    return
  }

  val liked by container.favorites.liked.collectAsState()
  val recent by container.history.recent.collectAsState()
  val playCounts by container.history.counts.collectAsState()
  val mostPlayed = remember(recent, playCounts) { recent.filter { (playCounts[it.key] ?: 0) > 1 }.sortedByDescending { playCounts[it.key] ?: 0 }.take(20) }
  val bookmarks by container.bookmarks.bookmarks.collectAsState()
  val downloaded by container.downloads.downloads.collectAsState(initial = emptyList())
  val playlists by container.playlists.observePlaylists().collectAsState(initial = emptyList())
  val covers by container.playlists.observeCovers().collectAsState(initial = emptyMap())
  var showNewPlaylist by remember { mutableStateOf(false) }
  var showImportPlaylist by remember { mutableStateOf(false) }
  var sortMenuOpen by remember { mutableStateOf(false) }
  var likedSort by rememberSaveable { mutableStateOf(LikedSort.RECENT) }
  val sortedLiked = remember(liked, likedSort) {
    when (likedSort) {
      LikedSort.RECENT -> liked
      LikedSort.TITLE -> liked.sortedBy { it.title.lowercase() }
      LikedSort.ARTIST -> liked.sortedBy { it.artist.lowercase() }
    }
  }
  val scope = rememberCoroutineScope()

  if (showNewPlaylist) {
    NewPlaylistDialog(
      onCreate = { name -> scope.launch { container.playlists.create(name) } },
      onDismiss = { showNewPlaylist = false },
    )
  }

  if (showImportPlaylist) ImportPlaylistDialog(container, onDismiss = { showImportPlaylist = false })

  PageScaffold(title = stringResource(R.string.tab_library), actions = { SettingsAction(openSettings) }) { padding ->
    if (liked.isEmpty() && playlists.isEmpty() && recent.isEmpty() && bookmarks.isEmpty() && downloaded.isEmpty()) {
      Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        EmptyState(icon = Icons.Rounded.LibraryMusic, title = stringResource(R.string.empty_library_title), body = stringResource(R.string.empty_library_body))
      }
    } else {
      LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)) {
        item { StatsRow(container) }
        item { WeeklyListeningChart(container) }
        item { TopArtistsSection(recent, playCounts, onOpenArtist) }
        item {
          SectionHeader("Playlists (${playlists.size})", icon = Icons.Rounded.QueueMusic, trailing = {
            IconButton(onClick = { showImportPlaylist = true }) { Icon(Icons.Rounded.FileDownload, contentDescription = "Import playlist") }
            IconButton(onClick = { showNewPlaylist = true }) { Icon(Icons.Rounded.Add, contentDescription = "New playlist") }
          })
        }
        if (playlists.isEmpty()) {
          item { InlineEmpty(Icons.Rounded.QueueMusic, "No playlists yet. Tap + to create one.") }
        } else {
          val pairs = playlists.chunked(2)
          items(pairs.size, key = { "pp${pairs[it].first().id}" }) { r ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              pairs[r].forEach { p ->
                PlaylistTile(p, covers[p.id].orEmpty(), onClick = { openPlaylistId = p.id }, modifier = Modifier.weight(1f))
              }
              if (pairs[r].size == 1) Box(Modifier.weight(1f))
            }
          }
        }
        if (downloaded.isNotEmpty()) {
          item { SectionHeader("Downloaded (${downloaded.size}) · plays offline", icon = Icons.Rounded.Download) }
          items(downloaded.size, key = { "d${downloaded[it].key}" }) { i ->
            val tracks = downloaded.map { it.toTrack() }
            LibraryTrackRow(container, tracks[i], liked, onClick = { container.player.play(tracks, i) })
          }
        }
        if (bookmarks.isNotEmpty()) item { SectionHeader("Bookmarks", icon = Icons.Rounded.Bookmark) }
        items(bookmarks.size, key = { "bm${bookmarks[it].track.key}-${bookmarks[it].positionMs}" }) { i ->
          val bm = bookmarks[i]
          TrackRow(
            track = bm.track,
            onClick = { container.player.play(listOf(bm.track), 0, startPositionMs = bm.positionMs) },
            trailing = {
              Text(formatBookmarkTime(bm.positionMs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 4.dp))
              IconButton(onClick = { container.bookmarks.remove(bm) }) { Icon(Icons.Rounded.Close, contentDescription = "Remove bookmark") }
            },
          )
        }
        if (mostPlayed.isNotEmpty()) item { SectionHeader("Most played", icon = Icons.Rounded.Whatshot) }
        items(mostPlayed.size, key = { "m${mostPlayed[it].key}" }) { i ->
          LibraryTrackRow(container, mostPlayed[i], liked, onClick = { container.player.play(mostPlayed, i) })
        }
        if (recent.isNotEmpty()) item { SectionHeader("Recently played", icon = Icons.Rounded.History) }
        items(recent.size, key = { "r${recent[it].key}" }) { i ->
          LibraryTrackRow(container, recent[i], liked, onClick = { container.player.play(recent, i) })
        }
        item {
          SectionHeader("Liked (${liked.size})", icon = Icons.Rounded.Favorite, trailing = {
            if (liked.isNotEmpty()) {
              IconButton(onClick = { container.player.play(sortedLiked, 0) }) { Icon(Icons.Rounded.PlayArrow, contentDescription = "Play liked songs") }
              Box {
                IconButton(onClick = { sortMenuOpen = true }) { Icon(Icons.Rounded.Sort, contentDescription = "Sort liked songs") }
                androidx.compose.material3.DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                  LikedSort.entries.forEach { sort ->
                    androidx.compose.material3.DropdownMenuItem(
                      text = { Text(sort.label) }, onClick = { likedSort = sort; sortMenuOpen = false },
                      trailingIcon = { if (sort == likedSort) Icon(Icons.Rounded.Check, contentDescription = null) },
                    )
                  }
                }
              }
            }
          })
        }
        if (liked.isEmpty()) {
          item { InlineEmpty(Icons.Rounded.Favorite, "Songs you like will show up here.") }
        } else {
          items(sortedLiked.size, key = { sortedLiked[it].key }) { i ->
            LibraryTrackRow(container, sortedLiked[i], liked, onClick = { container.player.play(sortedLiked, i) })
          }
        }
      }
    }
  }
}

/** Shared long-press/⋮-menu-wired row for Library's Most Played/Recently Played/Liked sections. */
@Composable
private fun LibraryTrackRow(container: AppContainer, track: xyz.pepe.glacierclient.musicapp.domain.model.Track, liked: List<xyz.pepe.glacierclient.musicapp.domain.model.Track>, onClick: () -> Unit) {
  var menuOpen by remember { mutableStateOf(false) }
  var showAddToPlaylist by remember { mutableStateOf(false) }
  val context = androidx.compose.ui.platform.LocalContext.current
  if (showAddToPlaylist) AddToPlaylistDialogHost(container, track) { showAddToPlaylist = false }
  TrackRow(
    track = track, onClick = onClick, onLongPress = { menuOpen = true },
    onSwipeRight = xyz.pepe.glacierclient.musicapp.ui.util.swipeLikeAction(container, track),
    onSwipeLeft = xyz.pepe.glacierclient.musicapp.ui.util.swipeQueueAction(container, track),
    trailing = {
      xyz.pepe.glacierclient.musicapp.ui.components.TrackOverflowMenu(
        expanded = menuOpen, onExpandedChange = { menuOpen = it },
        onPlayNext = { container.player.playNext(track) },
        onAddToQueue = { container.player.addToQueue(track) },
        onAddToPlaylist = { showAddToPlaylist = true },
        onShare = { xyz.pepe.glacierclient.musicapp.ui.util.shareTrack(context, track) },
        onStartRadio = { container.player.startRadio(track) },
        liked = liked.any { it.key == track.key },
        onToggleLike = { container.favorites.toggle(track) },
      )
      LikeButton(liked = liked.any { it.key == track.key }, onToggle = { container.favorites.toggle(track) })
    },
  )
}

/** Real lifetime listening stats — total plays and an estimate of minutes listened derived from
 * actual track durations, plus the current streak. Never reset by "Clear play history". */
@Composable
private fun StatsRow(container: AppContainer) {
  val totalPlays by container.history.totalPlays.collectAsState()
  val totalMs by container.history.totalMs.collectAsState()
  val streak = remember { container.streaks.currentStreak() }
  if (totalPlays == 0) return
  val minutes = totalMs / 60_000
  Row(
    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    StatItem(Icons.Rounded.GraphicEq, "$totalPlays", "plays", Modifier.weight(1f))
    StatItem(Icons.Rounded.Timer, if (minutes < 60) "${minutes}m" else "${minutes / 60}h ${minutes % 60}m", "listened", Modifier.weight(1f))
    if (streak > 0) StatItem(Icons.Rounded.LocalFireDepartment, "$streak", "day streak", Modifier.weight(1f))
  }
}

@Composable
private fun StatItem(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
  GlassCard(modifier) {
    Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
      IconChip(icon, size = 32.dp)
      Text(value, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
      Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
  }
}

/** One-line empty state inside a card: icon + a sentence. */
@Composable
internal fun InlineEmpty(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
  GlassCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
      IconChip(icon, size = 36.dp)
      Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 12.dp))
    }
  }
}

private enum class LikedSort(val label: String) { RECENT("Recently liked"), TITLE("Title A–Z"), ARTIST("Artist A–Z") }

private fun formatBookmarkTime(ms: Long): String {
  val totalSeconds = ms / 1000
  return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

@Composable
private fun PlaylistTile(playlist: Playlist, coverUrls: List<String>, onClick: () -> Unit, modifier: Modifier = Modifier) {
  GlassCard(modifier, onClick = onClick) {
    Column(Modifier.fillMaxWidth().padding(10.dp)) {
      PlaylistCover(coverUrls, size = null, modifier = Modifier.fillMaxWidth().aspectRatio(1f), cornerRadius = 16.dp)
      Text(
        playlist.name, maxLines = 1, overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp, start = 2.dp),
      )
      Text(
        "${playlist.trackCount} songs", maxLines = 1, style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 2.dp),
      )
    }
  }
}

// Reused by TrackRow's "add to playlist" action from Search/Home/Radio.
@Composable
fun NewPlaylistDialog(onCreate: (String) -> Unit, onDismiss: () -> Unit) {
  var name by remember { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.new_playlist)) },
    text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text(stringResource(R.string.new_playlist)) }) },
    confirmButton = { TextButton(onClick = { if (name.isNotBlank()) { onCreate(name); onDismiss() } }) { Text("Create") } },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
