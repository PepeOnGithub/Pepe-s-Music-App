@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package xyz.pepe.glacierclient.musicapp.ui.artist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.pepe.glacierclient.musicapp.di.AppContainer
import xyz.pepe.glacierclient.musicapp.domain.model.Track
import xyz.pepe.glacierclient.musicapp.ui.components.EmptyState
import xyz.pepe.glacierclient.musicapp.ui.components.HeroHeader
import xyz.pepe.glacierclient.musicapp.ui.components.LikeButton
import xyz.pepe.glacierclient.musicapp.ui.components.SectionHeader
import xyz.pepe.glacierclient.musicapp.ui.components.PageScaffold
import xyz.pepe.glacierclient.musicapp.ui.components.TrackOverflowMenu
import xyz.pepe.glacierclient.musicapp.ui.components.TrackRow

/**
 * An artist page built from the catalog: every song the enabled sources return for the artist's
 * name (preferring ones whose artist field really matches), with play, shuffle and radio.
 */
@Composable
fun ArtistScreen(container: AppContainer, artist: String, onBack: () -> Unit) {
  var tracks by remember(artist) { mutableStateOf<List<Track>>(emptyList()) }
  var loading by remember(artist) { mutableStateOf(true) }
  val liked by container.favorites.liked.collectAsState()
  val context = androidx.compose.ui.platform.LocalContext.current
  LaunchedEffect(artist) {
    loading = true
    val results = runCatching { container.catalog.search(artist, container.settings.settings.value) }.getOrDefault(emptyList())
    val exact = results.filter { it.artist.contains(artist, ignoreCase = true) || artist.contains(it.artist, ignoreCase = true) }
    tracks = exact.ifEmpty { results }
    loading = false
  }
  PageScaffold(title = "Artist", onBack = onBack) { padding ->
    when {
      loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { LoadingIndicator(Modifier.size(64.dp)) }
      tracks.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        EmptyState(icon = Icons.Rounded.SearchOff, title = "No songs found", body = "Nothing came back for \"$artist\" from the enabled sources.")
      }
      else -> LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
          HeroHeader(
            title = artist,
            subtitle = "${tracks.size} songs",
            artworkUrl = tracks.firstOrNull { it.artworkUrl != null }?.artworkUrl,
            circular = true,
          )
        }
        item {
          Row(Modifier.fillMaxWidth().padding(16.dp, 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { container.player.play(tracks, 0) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
              Icon(Icons.Rounded.PlayArrow, contentDescription = null); Text("Play", maxLines = 1, modifier = Modifier.padding(start = 4.dp))
            }
            FilledTonalButton(onClick = { container.player.play(tracks.shuffled(), 0) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
              Icon(Icons.Rounded.Shuffle, contentDescription = null); Text("Shuffle", maxLines = 1, modifier = Modifier.padding(start = 4.dp))
            }
            FilledTonalButton(onClick = { container.player.startRadio(tracks.first()) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
              Icon(Icons.Rounded.Radio, contentDescription = null); Text("Radio", maxLines = 1, modifier = Modifier.padding(start = 4.dp))
            }
          }
        }
        item { SectionHeader("Top songs", icon = Icons.Rounded.MusicNote) }
        items(tracks.size, key = { tracks[it].key }) { i ->
          val track = tracks[i]
          TrackRow(
            track = track, onClick = { container.player.play(tracks, i) },
            trailing = {
              TrackOverflowMenu(
                onPlayNext = { container.player.playNext(track) },
                onAddToQueue = { container.player.addToQueue(track) },
                onShare = { xyz.pepe.glacierclient.musicapp.ui.util.shareTrack(context, track) },
                onStartRadio = { container.player.startRadio(track) },
                liked = liked.any { it.key == track.key },
                onToggleLike = { container.favorites.toggle(track) },
              )
              LikeButton(liked = liked.any { it.key == track.key }, onToggle = { container.favorites.toggle(track) })
            },
          )
        }
      }
    }
  }
}
