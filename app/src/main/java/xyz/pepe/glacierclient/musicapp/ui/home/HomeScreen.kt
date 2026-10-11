@file:OptIn(
  androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
  androidx.compose.material3.ExperimentalMaterial3Api::class,
  androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package xyz.pepe.glacierclient.musicapp.ui.home

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import xyz.pepe.glacierclient.musicapp.R
import xyz.pepe.glacierclient.musicapp.data.repository.AppNotification
import xyz.pepe.glacierclient.musicapp.di.AppContainer
import xyz.pepe.glacierclient.musicapp.domain.model.Track
import xyz.pepe.glacierclient.musicapp.ui.components.EmptyState
import xyz.pepe.glacierclient.musicapp.ui.components.GlassBorder
import xyz.pepe.glacierclient.musicapp.ui.components.GlassCard
import xyz.pepe.glacierclient.musicapp.ui.components.LocalGlassEnabled
import xyz.pepe.glacierclient.musicapp.ui.components.SectionHeader
import xyz.pepe.glacierclient.musicapp.ui.components.glass
import xyz.pepe.glacierclient.musicapp.ui.components.LikeButton
import xyz.pepe.glacierclient.musicapp.ui.components.PageScaffold
import xyz.pepe.glacierclient.musicapp.ui.components.SectionTitle
import xyz.pepe.glacierclient.musicapp.ui.components.TrackRow
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import xyz.pepe.glacierclient.ecosystem.CardSize
import xyz.pepe.glacierclient.ecosystem.DashboardLayout
import xyz.pepe.glacierclient.musicapp.domain.model.AppSettings
import java.util.Calendar


@Composable
fun HomeScreen(container: AppContainer, openSettings: () -> Unit) {
  val settings by container.settings.settings.collectAsState()
  val liked by container.favorites.liked.collectAsState()
  val account by container.ytAuth.activeAccount.collectAsState()
  val recent by container.history.recent.collectAsState()
  var recommended by remember { mutableStateOf<List<Track>>(emptyList()) }
  var recommendedFor by remember { mutableStateOf<String?>(null) }
  val inbox by container.notifications.items.collectAsState()
  val unread = inbox.count { !it.read }
  var showInbox by remember { mutableStateOf(false) }
  // null = "All" (trending); otherwise a category searched as "<name> songs".
  var category by rememberSaveable { mutableStateOf<String?>(null) }
  var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
  var loading by remember { mutableStateOf(true) }
  var failed by remember { mutableStateOf(false) }
  var reload by remember { mutableStateOf(0) }
  // "Quick picks" — a blend of your Liked tracks and recent plays, always rendered as a list
  // (TrackRow), independent of the Home grid/list toggle, same as the always-list section in
  // the YouTube Music screenshots this was modeled on. Real data, no recommendation model.
  val quickPicks = remember(liked, recent) { (liked + recent).distinctBy { it.key }.take(10) }

  LaunchedEffect(category, reload) {
    loading = true
    failed = false
    val result = runCatching {
      category?.let { container.catalog.search("$it songs", settings) } ?: container.catalog.trending(settings)
    }
    tracks = result.getOrDefault(emptyList())
    failed = result.isFailure
    loading = false
    if (tracks.isNotEmpty()) runCatching { container.catalog.prefetch(tracks) }
  }

  // "Because you listened to X" — the most-played artist in real history, not a fabricated
  // recommendation engine. Only shown on the default (All/trending) view.
  LaunchedEffect(recent) {
    if (category != null || recent.isEmpty()) { recommended = emptyList(); return@LaunchedEffect }
    val topArtist = recent.groupingBy { it.artist }.eachCount().maxByOrNull { it.value }?.key
    if (topArtist == null || topArtist == recommendedFor) return@LaunchedEffect
    recommendedFor = topArtist
    recommended = runCatching { container.catalog.search(topArtist, settings) }.getOrDefault(emptyList()).filterNot { r -> recent.any { it.key == r.key } }
  }

  if (showInbox) {
    NotificationsSheet(inbox, onClear = { container.notifications.clear() }, onDismiss = { container.notifications.markAllRead(); showInbox = false })
  }

  var editing by rememberSaveable { mutableStateOf(false) }
  var layout by remember { mutableStateOf(HomeSections.load(settings.homeLayout)) }
  var lastWritten by remember { mutableStateOf(settings.homeLayout) }
  LaunchedEffect(settings.homeLayout) {
    // Only adopt changes made elsewhere (Settings reset); our own writes are already in [layout].
    if (settings.homeLayout != lastWritten) { lastWritten = settings.homeLayout; layout = HomeSections.load(settings.homeLayout) }
  }
  val applyLayout: (DashboardLayout) -> Unit = { n ->
    layout = n
    val saved = HomeSections.save(n)
    lastWritten = saved
    container.settings.update { it.copy(homeLayout = saved) }
  }
  val editRequested by HomeEditRequests.requested.collectAsState()
  LaunchedEffect(editRequested) {
    if (editRequested) { editing = true; HomeEditRequests.requested.value = false }
  }
  val rootFocus = remember { FocusRequester() }
  LaunchedEffect(Unit) { runCatching { rootFocus.requestFocus() } }
  var overflowOpen by remember { mutableStateOf(false) }
  val greetName = settings.displayName.ifBlank { account?.label ?: "" }
  val ctx = HomeCtx(
    container = container, settings = settings, liked = liked, recent = recent, quickPicks = quickPicks,
    recommended = recommended, recommendedFor = recommendedFor, category = category, tracks = tracks,
    loading = loading, failed = failed, onRetry = { reload++ }, onCategory = { category = it },
  )

  PageScaffold(
    title = if (editing) "Edit home" else stringResource(R.string.tab_home),
    modifier = Modifier
      .onPreviewKeyEvent { e ->
        if (e.type != KeyEventType.KeyDown) false
        else if (e.isCtrlPressed && e.key == Key.E) { editing = !editing; true }
        else if (editing && e.key == Key.Escape) { editing = false; true }
        else false
      }
      .focusRequester(rootFocus)
      .focusable(),
    actions = {
      if (editing) {
        TextButton(onClick = { applyLayout(DashboardLayout.of(HomeSections.DEFAULTS)) }) { Text("Reset to default") }
        FilledTonalButton(onClick = { editing = false }, modifier = Modifier.padding(horizontal = 4.dp)) {
          Icon(Icons.Rounded.Done, contentDescription = null, modifier = Modifier.size(18.dp))
          Text("Done", modifier = Modifier.padding(start = 4.dp))
        }
      } else {
        IconButton(onClick = { editing = true }) { Icon(Icons.Rounded.Edit, contentDescription = "Edit home layout") }
        IconButton(onClick = { container.settings.update { it.copy(homeGrid = !it.homeGrid) } }) {
          Icon(if (settings.homeGrid) Icons.Rounded.ViewList else Icons.Rounded.GridView, contentDescription = "Toggle grid/list")
        }
        if (settings.showNotificationButton) {
          IconButton(onClick = { showInbox = true }) {
            BadgedBox(badge = { if (unread > 0) Badge { Text("$unread") } }) {
              Icon(Icons.Rounded.Notifications, contentDescription = "Notifications")
            }
          }
        }
        Box {
          IconButton(onClick = { overflowOpen = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = "More options") }
          DropdownMenu(expanded = overflowOpen, onDismissRequest = { overflowOpen = false }) {
            DropdownMenuItem(text = { Text("Edit home layout") }, onClick = { overflowOpen = false; editing = true })
            DropdownMenuItem(text = { Text("Reset home layout") }, onClick = { overflowOpen = false; applyLayout(DashboardLayout.of(HomeSections.DEFAULTS)) })
          }
        }
        SettingsAction(openSettings)
      }
    },
  ) { padding ->
    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
      isRefreshing = loading && tracks.isNotEmpty(),
      onRefresh = { reload++ },
      modifier = Modifier.fillMaxSize().padding(padding),
    ) {
      if (editing) HomeEditList(ctx, layout, applyLayout, greetName, account?.photoUrl, container.streaks.currentStreak())
      else HomeNormalList(ctx, layout, greetName, account?.photoUrl, container.streaks.currentStreak())
    }
  }
}

/** Everything the Home sections read, so normal and edit mode render from one place. */
internal class HomeCtx(
  val container: AppContainer,
  val settings: AppSettings,
  val liked: List<Track>,
  val recent: List<Track>,
  val quickPicks: List<Track>,
  val recommended: List<Track>,
  val recommendedFor: String?,
  val category: String?,
  val tracks: List<Track>,
  val loading: Boolean,
  val failed: Boolean,
  val onRetry: () -> Unit,
  val onCategory: (String?) -> Unit,
) {
  fun hasContent(id: String): Boolean = when (id) {
    HomeSections.CATEGORIES -> settings.showCategories && settings.categories.isNotEmpty()
    HomeSections.QUICK_PICKS -> category == null && quickPicks.isNotEmpty()
    HomeSections.SPEED_DIAL -> category == null && recent.isNotEmpty()
    HomeSections.BECAUSE -> category == null && recommended.isNotEmpty()
    else -> true
  }
}

@Composable
private fun HomeNormalList(ctx: HomeCtx, layout: DashboardLayout, greetName: String, photoUrl: String?, streak: Int) {
  val settings = ctx.settings
  val rows = remember(layout, ctx.category, ctx.quickPicks.isEmpty(), ctx.recent.isEmpty(), ctx.recommended.isEmpty(), settings.showCategories, settings.categories) {
    DashboardLayout.of(layout.visible.filter { ctx.hasContent(it.id) }).rows()
  }
  LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
    if (settings.showGreeting) item(key = "greeting") {
      Greeting(greetName, photoUrl, streak)
    }
    rows.forEach { row ->
      val first = row.first()
      if (row.size == 1 && (first.size == CardSize.FULL || first.id == HomeSections.TRENDING)) {
        if (first.id == HomeSections.TRENDING) trendingItems(ctx)
        else item(key = "sec_${first.id}") { SectionBody(ctx, first.id, compact = false, preview = false) }
      } else {
        item(key = "row_${row.joinToString("_") { it.id }}") {
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            row.forEach { s -> Box(Modifier.weight(1f)) { SectionBody(ctx, s.id, compact = true, preview = false) } }
          }
        }
      }
    }
  }
}

/** The song list (trending or the chosen category) with loading / error / empty states; stays lazy. */
private fun LazyListScope.trendingItems(ctx: HomeCtx) {
  val container = ctx.container
  item(key = "trend_header") {
    SectionHeader(ctx.category ?: stringResource(R.string.trending_now), icon = if (ctx.category == null) Icons.Rounded.TrendingUp else Icons.Rounded.Category)
  }
  val tracks = ctx.tracks
  when {
    ctx.loading -> item(key = "trend_loading") {
      Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) { LoadingIndicator(Modifier.size(64.dp)) }
    }
    ctx.failed -> item(key = "trend_failed") {
      Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        EmptyState(icon = Icons.Rounded.CloudOff, title = "Couldn't load music", body = "Check your connection, or try again.")
        FilledTonalButton(onClick = ctx.onRetry, modifier = Modifier.padding(top = 8.dp)) { Text("Retry") }
      }
    }
    tracks.isEmpty() -> item(key = "trend_empty") {
      EmptyState(icon = Icons.Rounded.Home, title = stringResource(R.string.continue_listening), body = stringResource(R.string.empty_library_body))
    }
    ctx.settings.homeGrid -> {
      val pairs = tracks.chunked(2)
      items(pairs.size, key = { "tg${pairs[it].first().key}" }) { i ->
        Row(Modifier.fillMaxWidth()) {
          pairs[i].forEachIndexed { j, t ->
            Box(Modifier.weight(1f)) { TrackCard(container, t, ctx.liked, onClick = { container.player.play(tracks, i * 2 + j) }) }
          }
          if (pairs[i].size == 1) Box(Modifier.weight(1f))
        }
      }
    }
    else -> items(tracks.size, key = { "tl${tracks[it].key}" }) { i ->
      TrackRowFull(container, tracks[i], ctx.liked, onClick = { container.player.play(tracks, i) })
    }
  }
}

/**
 * One Home section's content. [compact] is the half-width form; [preview] is the short form used
 * inside edit-mode tiles (a few rows, and no network-backed song list).
 */
@Composable
internal fun SectionBody(ctx: HomeCtx, id: String, compact: Boolean, preview: Boolean) {
  val container = ctx.container
  when (id) {
    HomeSections.CATEGORIES -> Column {
      SectionHeader("Categories", icon = Icons.Rounded.Category)
      if (preview) {
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          CategoryChip("All", ctx.category == null) {}
          ctx.settings.categories.take(3).forEach { CategoryChip(it, false) {} }
        }
      } else {
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          item { CategoryChip("All", ctx.category == null) { ctx.onCategory(null) } }
          items(ctx.settings.categories.size) { i ->
            val c = ctx.settings.categories[i]
            CategoryChip(c, ctx.category == c) { ctx.onCategory(c) }
          }
        }
      }
    }
    HomeSections.QUICK_PICKS -> Column {
      SectionHeader("Quick picks", icon = Icons.Rounded.Bolt, trailing = {
        if (!compact && !preview) TextButton(onClick = { container.player.play(ctx.quickPicks, 0) }) {
          Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
          Text("Play all", modifier = Modifier.padding(start = 4.dp))
        }
      })
      if (ctx.quickPicks.isEmpty()) Text("Likes and recent plays appear here.", Modifier.padding(horizontal = 20.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
      val n = if (preview) 3 else if (compact) 5 else 10
      ctx.quickPicks.take(n).forEachIndexed { i, t ->
        TrackRowFull(container, t, ctx.liked, onClick = { container.player.play(ctx.quickPicks, i) })
      }
    }
    HomeSections.SPEED_DIAL -> Column {
      SectionHeader("Speed dial", icon = Icons.Rounded.Apps)
      if (ctx.recent.isEmpty()) Text("Songs you play show up here.", Modifier.padding(horizontal = 20.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
      else SpeedDialPager(ctx.recent, onClick = { i -> container.player.play(ctx.recent, i) })
    }
    HomeSections.BECAUSE -> Column {
      SectionHeader(if (ctx.recommendedFor != null) "Because you listened to ${ctx.recommendedFor}" else "Because you listened", icon = Icons.Rounded.AutoAwesome)
      if (ctx.recommended.isEmpty()) Text("Recommendations appear once you have listening history.", Modifier.padding(horizontal = 20.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
      LazyRow(contentPadding = PaddingValues(horizontal = 8.dp), userScrollEnabled = !preview) {
        items(ctx.recommended.size, key = { "rec${ctx.recommended[it].key}" }) { i ->
          Box(Modifier.width(172.dp)) { TrackCard(container, ctx.recommended[i], ctx.liked, onClick = { container.player.play(ctx.recommended, i) }) }
        }
      }
    }
    HomeSections.TRENDING -> Column {
      SectionHeader(ctx.category ?: stringResource(R.string.trending_now), icon = if (ctx.category == null) Icons.Rounded.TrendingUp else Icons.Rounded.Category)
      Text(
        if (ctx.tracks.isEmpty()) "Songs load here." else "${ctx.tracks.size} songs",
        Modifier.padding(horizontal = 20.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall,
      )
    }
  }
}

/** A [TrackRow] wired with the full long-press/⋮ menu (like, play next, add to queue, add to
 * playlist, share) — the one place that assembly lives, so every Home list uses the same menu. */
@Composable
internal fun TrackRowFull(container: AppContainer, track: Track, liked: List<Track>, onClick: () -> Unit) {
  var menuOpen by remember { mutableStateOf(false) }
  var showAddToPlaylist by remember { mutableStateOf(false) }
  val context = androidx.compose.ui.platform.LocalContext.current
  if (showAddToPlaylist) xyz.pepe.glacierclient.musicapp.ui.library.AddToPlaylistDialogHost(container, track) { showAddToPlaylist = false }
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
        liked = liked.any { it.key == track.key },
        onToggleLike = { container.favorites.toggle(track) },
      )
      LikeButton(liked = liked.any { it.key == track.key }, onToggle = { container.favorites.toggle(track) })
    },
  )
}

/** 3x3-per-page paged grid of recently-played tracks, like YouTube Music's "Speed dial" —
 * built from real history. Plain rows (not a nested lazy grid) so tile height follows the width
 * on any screen and nothing is clipped or left as dead space. */
@Composable
internal fun SpeedDialPager(recent: List<Track>, onClick: (Int) -> Unit) {
  val items = recent.take(27)
  val pageCount = ((items.size + 8) / 9).coerceIn(1, 3)
  val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { pageCount })
  // Every page uses the first page's row count so the pager never changes height while swiping.
  val rows = ((items.size.coerceAtMost(9) + 2) / 3).coerceAtLeast(1)
  androidx.compose.foundation.pager.HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      repeat(rows) { r ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          repeat(3) { c ->
            val globalIndex = page * 9 + r * 3 + c
            Box(Modifier.weight(1f)) {
              if (globalIndex < items.size) SpeedDialTile(items[globalIndex], onClick = { onClick(globalIndex) })
              else Box(Modifier.fillMaxWidth().aspectRatio(1f))
            }
          }
        }
      }
    }
  }
  if (pageCount > 1) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
      repeat(pageCount) { i ->
        Box(
          Modifier.padding(3.dp).size(6.dp).clip(CircleShape)
            .background(if (pagerState.currentPage == i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest),
        )
      }
    }
  }
}

@Composable
private fun SpeedDialTile(track: Track, onClick: () -> Unit) {
  Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick)) {
    if (track.artworkUrl != null) {
      AsyncImage(model = track.artworkUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    } else {
      Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
    Box(
      Modifier.fillMaxSize().background(
        androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))),
      ),
    )
    Text(
      track.title, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis,
      modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
    )
  }
}

/** Grid tile: square artwork with a title/artist caption, like the "Popular Songs" cards in the
 * concept — long-press (or its ⋮, shown on tap) opens the same menu as the list rows. */
@Composable
internal fun TrackCard(container: AppContainer, track: Track, liked: List<Track>, onClick: () -> Unit) {
  var menuOpen by remember { mutableStateOf(false) }
  var showAddToPlaylist by remember { mutableStateOf(false) }
  val context = androidx.compose.ui.platform.LocalContext.current
  if (showAddToPlaylist) xyz.pepe.glacierclient.musicapp.ui.library.AddToPlaylistDialogHost(container, track) { showAddToPlaylist = false }
  val glass = LocalGlassEnabled.current
  val cardShape = RoundedCornerShape(22.dp)
  Column(
    Modifier.padding(8.dp)
      .then(
        if (glass) Modifier.glass(cardShape).border(GlassBorder, cardShape)
        else Modifier.clip(cardShape).background(MaterialTheme.colorScheme.surfaceContainerLow),
      )
      .combinedClickable(onClick = onClick, onLongClick = { menuOpen = true })
      .padding(8.dp),
  ) {
    Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
      if (track.artworkUrl != null) {
        AsyncImage(model = track.artworkUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
      } else {
        Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
      }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f).padding(top = 8.dp)) {
        Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
      xyz.pepe.glacierclient.musicapp.ui.components.TrackOverflowMenu(
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

@Composable
private fun NotificationsSheet(items: List<AppNotification>, onClear: () -> Unit, onDismiss: () -> Unit) {
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
      Text("Notifications", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
      if (items.isNotEmpty()) TextButton(onClick = onClear) { Text("Clear all") }
    }
    if (items.isEmpty()) {
      Text(
        "Nothing yet. Sign-ins, playlist changes and playback problems show up here.",
        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(24.dp),
      )
    } else {
      LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
        items(items.size, key = { items[it].id }) { i ->
          val n = items[i]
          Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp)) {
            Box(Modifier.padding(top = 6.dp).size(8.dp).clip(CircleShape).background(if (n.read) Color.Transparent else MaterialTheme.colorScheme.primary))
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
              Text(n.title, style = MaterialTheme.typography.titleSmall)
              Text(n.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
              DateUtils.getRelativeTimeSpanString(n.atMs, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString(),
              style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

@Composable
internal fun Greeting(name: String, photoUrl: String?, streak: Int) {
  val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
  val greeting = when (hour) { in 5..11 -> "Good morning"; in 12..16 -> "Good afternoon"; in 17..21 -> "Good evening"; else -> "Good night" }
  GlassCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
      Box(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
        if (photoUrl != null) {
          AsyncImage(model = photoUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
          Text(
            name.firstOrNull()?.uppercase() ?: "♪",
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
        }
      }
      Column(Modifier.weight(1f).padding(start = 12.dp)) {
        Text(if (name.isBlank()) "$greeting!" else "$greeting,", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (name.isNotBlank()) Text(name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
      // A real listening streak (consecutive days with at least one play), not a fabricated number.
      if (streak >= 2) {
        Row(
          Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.tertiaryContainer).padding(horizontal = 12.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("🔥", style = MaterialTheme.typography.titleMedium)
          Text(
            "$streak", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.padding(start = 4.dp),
          )
        }
      }
    }
  }
}

@Composable
internal fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
  FilterChip(
    selected = selected, onClick = onClick, label = { Text(label) },
    shape = CircleShape,
    colors = FilterChipDefaults.filterChipColors(
      selectedContainerColor = MaterialTheme.colorScheme.primary,
      selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
    ),
    border = null,
  )
}

@Composable
fun RowScope.SettingsAction(openSettings: () -> Unit) {
  IconButton(onClick = openSettings) {
    Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.settings))
  }
}
