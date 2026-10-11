@file:OptIn(
  kotlinx.coroutines.FlowPreview::class,
  androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
  androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package xyz.pepe.glacierclient.musicapp.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import xyz.pepe.glacierclient.musicapp.R
import xyz.pepe.glacierclient.musicapp.di.AppContainer
import xyz.pepe.glacierclient.musicapp.domain.model.Source
import xyz.pepe.glacierclient.musicapp.domain.model.Track
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.LibraryMusic
import xyz.pepe.glacierclient.musicapp.ui.components.EmptyState
import xyz.pepe.glacierclient.musicapp.ui.components.GlassCard
import xyz.pepe.glacierclient.musicapp.ui.components.IconChip
import xyz.pepe.glacierclient.musicapp.ui.components.SectionHeader
import xyz.pepe.glacierclient.musicapp.ui.components.glassSurface
import xyz.pepe.glacierclient.musicapp.ui.library.PlaylistCover
import xyz.pepe.glacierclient.musicapp.ui.components.LikeButton
import xyz.pepe.glacierclient.musicapp.ui.components.TrackOverflowMenu
import xyz.pepe.glacierclient.musicapp.ui.components.TrackRow
import xyz.pepe.glacierclient.musicapp.ui.library.AddToPlaylistButton

private val FullSpan: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }

private fun sourceLabel(s: Source): String = when (s) {
  Source.YOUTUBE_MUSIC -> "YouTube Music"
  Source.AUDIUS -> "Audius"
  Source.INTERNET_ARCHIVE -> "Internet Archive"
  Source.CCMIXTER -> "ccMixter"
  else -> "Other"
}

/** Same rounded pill search bar pattern as Pepe's Notes: no top app bar, the bar itself is the header.
 * Query/results/view-mode live in [xyz.pepe.glacierclient.musicapp.ui.search.SearchViewState] on the
 * container so they survive switching tabs and coming back. */
@Composable
fun SearchScreen(container: AppContainer, openSettings: () -> Unit, onOpenArtist: (String) -> Unit = {}) {
  val st = container.searchState
  val queryChannel = remember { Channel<String>(Channel.CONFLATED) }
  val settings by container.settings.settings.collectAsState()
  val liked by container.favorites.liked.collectAsState()
  var activeCategory by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
  var failed by remember { mutableStateOf(false) }
  val covers by container.playlists.observeCovers().collectAsState(initial = emptyMap())
  val playlists by container.playlists.observePlaylists().collectAsState(initial = emptyList())
  val searchScope = androidx.compose.runtime.rememberCoroutineScope()

  LaunchedEffect(Unit) {
    queryChannel.consumeAsFlow()
      .debounce(300)
      .distinctUntilChanged()
      .collect { q ->
        if (q.length < 2) { st.results = emptyList(); st.searched = false; return@collect }
        st.loading = true
        st.searched = true
        failed = false
        val found = runCatching { container.catalog.search(q, settings) }
        st.results = found.getOrDefault(emptyList())
        failed = found.isFailure
        st.loading = false
        if (!settings.privateSession) container.searchHistory.record(q)
        launch { container.catalog.prefetch(st.results) }
      }
  }

  fun runSearch(q: String) {
    st.query = q
    queryChannel.trySend(q)
  }

  Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    Row(
      Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp).fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Surface(
        shape = MaterialTheme.shapes.extraLarge, color = Color.Transparent,
        modifier = Modifier.weight(1f).heightIn(min = 56.dp).glassSurface(MaterialTheme.shapes.extraLarge),
      ) {
        Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
          BasicTextField(
            value = st.query,
            onValueChange = { activeCategory = null; runSearch(it) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {}),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
              Box(contentAlignment = Alignment.CenterStart) {
                if (st.query.isEmpty()) Text(stringResource(R.string.tab_search), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                inner()
              }
            },
          )
          if (st.query.isNotEmpty()) {
            IconButton(onClick = { activeCategory = null; runSearch("") }) { Icon(Icons.Rounded.Close, contentDescription = "Clear") }
          }
        }
      }
      IconButton(onClick = { st.grid = !st.grid }) {
        Icon(if (st.grid) Icons.Rounded.ViewList else Icons.Rounded.GridView, contentDescription = "Toggle grid/list")
      }
      IconButton(onClick = openSettings) { Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.settings)) }
    }

    if (settings.categories.isNotEmpty()) {
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(settings.categories.size) { i ->
          val c = settings.categories[i]
          FilterChip(
            selected = activeCategory == c,
            onClick = { activeCategory = c; runSearch("$c songs") },
            label = { Text(c) },
            shape = CircleShape,
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary),
            border = null,
          )
        }
      }
    }

    val recentQueries by container.searchHistory.recent.collectAsState()
    if (st.query.isEmpty() && !st.searched && recentQueries.isNotEmpty()) {
      SectionHeader("Recent searches", icon = Icons.Rounded.History, trailing = {
        androidx.compose.material3.TextButton(onClick = { container.searchHistory.clear() }) { Text("Clear all") }
      })
      androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxWidth().heightIn(max = 260.dp)) {
        items(recentQueries.size, key = { recentQueries[it] }) { i ->
          val q = recentQueries[i]
          Row(
            Modifier.fillMaxWidth().clickable { activeCategory = null; runSearch(q) }.padding(16.dp, 10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(androidx.compose.material.icons.Icons.Rounded.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(q, modifier = Modifier.weight(1f).padding(start = 16.dp))
            IconButton(onClick = { container.searchHistory.remove(q) }) { Icon(Icons.Rounded.Close, contentDescription = "Remove") }
          }
        }
      }
    }

    if (st.results.isNotEmpty()) {
      Row(Modifier.fillMaxWidth().padding(16.dp, 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Songs", "Artists", "Albums", "Playlists").forEachIndexed { i, label ->
          FilterChip(
            selected = st.tab == i, onClick = { st.tab = i }, label = { Text(label) },
            shape = CircleShape,
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer, selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer),
            border = null,
          )
        }
      }
    }

    Box(Modifier.weight(1f).fillMaxWidth()) {
      when {
        st.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator(Modifier.size(64.dp)) }
        failed && st.results.isEmpty() -> EmptyState(
          icon = Icons.Rounded.CloudOff, title = "Search failed",
          body = "Couldn't reach the music sources. Check your connection and try again.",
          modifier = Modifier.align(Alignment.Center),
        )
        st.searched && st.results.isEmpty() -> EmptyState(
          icon = Icons.Rounded.SearchOff, title = stringResource(R.string.no_results),
          body = "No matches on YouTube Music or Audius — try a different search.",
          modifier = Modifier.align(Alignment.Center),
        )
        st.results.isNotEmpty() && st.tab == 1 -> {
          // Artists derived from these search results — not a separate artist API/page, just a
          // grouping of what actually came back, tapping one re-searches for more by that name.
          val artists = remember(st.results) {
            st.results.groupBy { it.artist }.map { (artist, tracks) -> Triple(artist, tracks.size, tracks.firstOrNull { it.artworkUrl != null }?.artworkUrl) }
              .sortedByDescending { it.second }
          }
          androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(artists.size, key = { artists[it].first }) { i ->
              val (artist, count, art) = artists[i]
              GlassCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp), shape = RoundedCornerShape(18.dp), onClick = { onOpenArtist(artist) }) {
                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                  Box(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
                    if (art != null) AsyncImage(model = art, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    else Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                  Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(artist, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("$count song${if (count == 1) "" else "s"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                }
              }
            }
          }
        }
        st.results.isNotEmpty() && st.tab == 3 -> {
          // Your own playlists whose name matches the query; tap to play one.
          val words = st.query.lowercase().split(" ").filter { it.isNotBlank() }
          val matches = playlists.filter { p -> words.all { it in p.name.lowercase() } }
          if (matches.isEmpty()) {
            EmptyState(icon = Icons.Rounded.Search, title = "No playlists match", body = "None of your playlists have \"${st.query}\" in their name.", modifier = Modifier.align(Alignment.Center))
          } else {
            androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
              items(matches.size, key = { matches[it].id }) { i ->
                val p = matches[i]
                GlassCard(
                  Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp), shape = RoundedCornerShape(18.dp),
                  onClick = {
                    searchScope.launch {
                      val list = container.playlists.tracks(p.id)
                      if (list.isNotEmpty()) container.player.play(list, 0)
                    }
                  },
                ) {
                  Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    PlaylistCover(covers[p.id].orEmpty(), size = 48.dp, cornerRadius = 12.dp)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                      Text(p.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                      Text("${p.trackCount} tracks · tap to play", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                  }
                }
              }
            }
          }
        }
        st.results.isNotEmpty() && st.tab == 2 -> {
          // "Albums" — tracks sharing both artist and cover art, the closest honest proxy for a
          // release without a dedicated album API on any of these sources. Singles (no match)
          // don't get a fake one-track album entry here; they just stay out of this tab.
          val albums = remember(st.results) {
            st.results.filter { it.artworkUrl != null }.groupBy { it.artist to it.artworkUrl }
              .filter { it.value.size >= 2 }.map { (k, tracks) -> Triple(k.first, k.second, tracks) }
          }
          if (albums.isEmpty()) {
            EmptyState(icon = Icons.Rounded.Search, title = "No albums in these results", body = "None of these sources expose album metadata directly — try Songs or Artists instead.", modifier = Modifier.align(Alignment.Center))
          } else {
            androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
              items(albums.size, key = { "${albums[it].first}-${albums[it].second}" }) { i ->
                val (artist, art, tracks) = albums[i]
                GlassCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp), shape = RoundedCornerShape(18.dp), onClick = { container.player.play(tracks, 0) }) {
                  Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
                      if (art != null) AsyncImage(model = art, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                      else Icon(Icons.Rounded.Album, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                      Text(artist, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                      Text("${tracks.size} tracks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                  }
                }
              }
            }
          }
        }
        st.results.isNotEmpty() -> {
          val bySource = st.results.groupBy { it.source }
          if (st.grid) {
            LazyVerticalGrid(columns = GridCells.Adaptive(160.dp), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
              bySource.forEach { (source, tracks) ->
                if (bySource.size > 1) item(span = FullSpan) {
                  SectionHeader(sourceLabel(source), icon = Icons.Rounded.LibraryMusic)
                }
                items(tracks.size, key = { tracks[it].key }) { i ->
                  SearchGridCard(container, tracks[i], liked, onClick = { container.player.play(st.results, st.results.indexOf(tracks[i])) })
                }
              }
            }
          } else {
            androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
              bySource.forEach { (source, tracks) ->
                if (bySource.size > 1) item {
                  SectionHeader(sourceLabel(source), icon = Icons.Rounded.LibraryMusic)
                }
                items(tracks.size, key = { tracks[it].key }) { i ->
                  val track = tracks[i]
                  var menuOpen by remember { mutableStateOf(false) }
                  var showAddToPlaylist by remember { mutableStateOf(false) }
                  val context = androidx.compose.ui.platform.LocalContext.current
                  if (showAddToPlaylist) xyz.pepe.glacierclient.musicapp.ui.library.AddToPlaylistDialogHost(container, track) { showAddToPlaylist = false }
                  TrackRow(
                    track = track, onClick = { container.player.play(st.results, st.results.indexOf(track)) },
                    onLongPress = { menuOpen = true },
                    onSwipeRight = xyz.pepe.glacierclient.musicapp.ui.util.swipeLikeAction(container, track),
                    onSwipeLeft = xyz.pepe.glacierclient.musicapp.ui.util.swipeQueueAction(container, track),
                    trailing = {
                      TrackOverflowMenu(
                        expanded = menuOpen, onExpandedChange = { menuOpen = it },
                        onPlayNext = { container.player.playNext(track) },
                        onAddToQueue = { container.player.addToQueue(track) },
                        onAddToPlaylist = { showAddToPlaylist = true },
                        onShare = { xyz.pepe.glacierclient.musicapp.ui.util.shareTrack(context, track) },
                        onStartRadio = { container.player.startRadio(track) },
                        onGoToArtist = { onOpenArtist(track.artist) },
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
        else -> EmptyState(
          icon = Icons.Rounded.Search, title = stringResource(R.string.tab_search),
          body = "Full tracks from YouTube Music and Audius — never 30-second previews.",
          modifier = Modifier.align(Alignment.Center),
        )
      }
    }
  }
}

@Composable
private fun SearchGridCard(container: AppContainer, track: Track, liked: List<Track>, onClick: () -> Unit) {
  var menuOpen by remember { mutableStateOf(false) }
  var showAddToPlaylist by remember { mutableStateOf(false) }
  val context = androidx.compose.ui.platform.LocalContext.current
  if (showAddToPlaylist) xyz.pepe.glacierclient.musicapp.ui.library.AddToPlaylistDialogHost(container, track) { showAddToPlaylist = false }
  Column(
    Modifier.padding(8.dp).glassSurface(RoundedCornerShape(22.dp))
      .combinedClickable(onClick = onClick, onLongClick = { menuOpen = true })
      .padding(8.dp),
  ) {
    Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
      if (track.artworkUrl != null) AsyncImage(model = track.artworkUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
      else Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f).padding(top = 8.dp)) {
        Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
      TrackOverflowMenu(
        expanded = menuOpen, onExpandedChange = { menuOpen = it }, showButton = false,
        onPlayNext = { container.player.playNext(track) },
        onAddToQueue = { container.player.addToQueue(track) },
        onAddToPlaylist = { showAddToPlaylist = true },
        onShare = { xyz.pepe.glacierclient.musicapp.ui.util.shareTrack(context, track) },
        liked = liked.any { it.key == track.key },
        onToggleLike = { container.favorites.toggle(track) },
      )
    }
  }
}
