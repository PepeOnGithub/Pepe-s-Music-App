@file:OptIn(ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package xyz.pepe.glacierclient.musicapp.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.SliderDefaults
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventType
import xyz.pepe.glacierclient.musicapp.ui.components.mouseWheel
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import xyz.pepe.glacierclient.musicapp.ui.components.EmptyState
import xyz.pepe.glacierclient.musicapp.ui.components.GlassCard
import xyz.pepe.glacierclient.musicapp.ui.components.LocalHapticsEnabled
import xyz.pepe.glacierclient.musicapp.ui.components.hoverHand
import xyz.pepe.glacierclient.musicapp.ui.components.pressScale
import xyz.pepe.glacierclient.musicapp.ui.components.rememberArtworkTint
import xyz.pepe.glacierclient.musicapp.ui.components.rememberReducedMotion
import xyz.pepe.glacierclient.ecosystem.PepeEcosystem
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import xyz.pepe.glacierclient.musicapp.di.AppContainer
import xyz.pepe.glacierclient.musicapp.domain.model.RepeatMode
import xyz.pepe.glacierclient.musicapp.ui.components.PageScaffold

private val SLEEP_TIMER_OPTIONS = listOf(5, 15, 30, 45, 60)
private val SPEED_OPTIONS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
private val PITCH_OPTIONS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

@Composable
fun NowPlayingScreen(container: AppContainer, onBack: () -> Unit, onOpenArtist: (String) -> Unit = {}) {
  val stateFlow = remember(container) {
    container.player.state.map { it.copy(positionMs = 0L) }.distinctUntilChanged()
  }
  val state by stateFlow.collectAsState(initial = container.player.state.value.copy(positionMs = 0L))
  val track = state.current
  var sleepMenuOpen by remember { mutableStateOf(false) }
  var speedMenuOpen by remember { mutableStateOf(false) }
  val liked by container.favorites.liked.collectAsState()
  val settings by container.settings.settings.collectAsState()
  val context = androidx.compose.ui.platform.LocalContext.current

  PageScaffold(
    title = "Now Playing",
    compact = true,
    onBack = onBack,
    actions = {
      Box {
        IconButton(onClick = { speedMenuOpen = true }) {
          Text("${state.speed}x", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = speedMenuOpen, onDismissRequest = { speedMenuOpen = false }) {
          SPEED_OPTIONS.forEach { sp ->
            DropdownMenuItem(text = { Text("${sp}x") }, onClick = { container.player.setPlaybackSpeed(sp); speedMenuOpen = false })
          }
        }
      }
      if (track != null) {
        var showLyrics by remember(track.key) { mutableStateOf(false) }
        if (showLyrics) LyricsSheet(container, track, onDismiss = { showLyrics = false })
        IconButton(onClick = { showLyrics = true }) { Icon(Icons.Rounded.Lyrics, contentDescription = "Lyrics") }
        xyz.pepe.glacierclient.musicapp.ui.components.DownloadButton(container, track)
        Box {
          var moreOpen by remember { mutableStateOf(false) }
          IconButton(onClick = { moreOpen = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = "More") }
          val hasNotes = remember { PepeEcosystem.isInstalled(context, PepeEcosystem.App.NOTES) }
          DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
            DropdownMenuItem(text = { Text("Bookmark this position") }, onClick = {
              moreOpen = false
              val nowMs = container.player.state.value.positionMs
              container.bookmarks.add(track, nowMs)
              container.notifications.post("Bookmarked", "${track.title} at ${formatMs(nowMs)}")
            })
            DropdownMenuItem(text = { Text("Share") }, onClick = { moreOpen = false; xyz.pepe.glacierclient.musicapp.ui.util.shareTrack(context, track) })
            androidx.compose.material3.HorizontalDivider()
            Text("Pitch", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            Row(Modifier.padding(horizontal = 8.dp)) {
              PITCH_OPTIONS.forEach { p ->
                DropdownMenuItem(text = { Text("${p}x", color = if (state.pitch == p) MaterialTheme.colorScheme.primary else Color.Unspecified) }, onClick = { container.player.setPitch(p) }, modifier = Modifier.weight(1f, fill = false))
              }
            }
            val loopA = state.loopA
            val loopB = state.loopB
            DropdownMenuItem(text = { Text(if (loopA == null) "Set A–B loop: point A" else "Point A: ${formatMs(loopA)}") }, onClick = { container.player.setLoopA() })
            DropdownMenuItem(text = { Text(if (loopB == null) "Set A–B loop: point B" else "Point B: ${formatMs(loopB)}") }, onClick = { container.player.setLoopB() })
            if (loopA != null || loopB != null) DropdownMenuItem(text = { Text("Clear loop") }, onClick = { container.player.clearLoop(); moreOpen = false })
            androidx.compose.material3.HorizontalDivider()
            DropdownMenuItem(text = { Text("Start radio from this song") }, onClick = { moreOpen = false; container.player.startRadio(track) })
            DropdownMenuItem(text = { Text("Go to artist") }, onClick = { moreOpen = false; onOpenArtist(track.artist) })
            DropdownMenuItem(text = { Text("Shuffle up next") }, onClick = { moreOpen = false; container.player.shuffleUpNext() })
            DropdownMenuItem(text = { Text("Clear up next") }, onClick = { moreOpen = false; container.player.clearUpNext() })
            if (hasNotes) DropdownMenuItem(
              text = { Text("Send to Pepe's Notes") },
              onClick = {
                moreOpen = false
                val body = buildString {
                  append("${track.title} — ${track.artist}")
                  append("\nAt ${formatMs(container.player.state.value.positionMs)} of ${formatMs(state.durationMs)}")
                  track.shareUrl?.let { append("\n$it") }
                }
                PepeEcosystem.send(context, PepeEcosystem.CreateNote(body.take(PepeEcosystem.MAX_TEXT), title = track.title))
              },
            )
          }
        }
      }
      Box {
        IconButton(onClick = { sleepMenuOpen = true }) {
          Icon(
            Icons.Rounded.Bedtime, contentDescription = "Sleep timer",
            tint = if (state.sleepTimerEndAtMs != null || state.sleepAtTrackEnd) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        DropdownMenu(expanded = sleepMenuOpen, onDismissRequest = { sleepMenuOpen = false }) {
          SLEEP_TIMER_OPTIONS.forEach { minutes ->
            DropdownMenuItem(text = { Text("$minutes min") }, onClick = { container.player.setSleepTimer(minutes); sleepMenuOpen = false })
          }
          DropdownMenuItem(text = { Text("End of this track") }, onClick = { container.player.sleepAtEndOfTrack(); sleepMenuOpen = false })
          if (state.sleepTimerEndAtMs != null || state.sleepAtTrackEnd) {
            DropdownMenuItem(text = { Text("Turn off timer") }, onClick = { container.player.cancelSleepTimer(); sleepMenuOpen = false })
          }
        }
      }
    },
  ) { padding ->
    if (track == null) {
      Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        EmptyState(icon = Icons.Rounded.MusicNote, title = "Nothing playing", body = "Pick a song from Home, Search or Library.")
      }
      return@PageScaffold
    }
    val reduced = rememberReducedMotion()
    val hapticsOn = LocalHapticsEnabled.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val tint = rememberArtworkTint(track.artworkUrl)
    val animatedTint by animateColorAsState(tint ?: MaterialTheme.colorScheme.primary, tween(500), label = "npTint")
    var showSaveQueue by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    if (showSaveQueue) {
      xyz.pepe.glacierclient.musicapp.ui.library.NewPlaylistDialog(
        onCreate = { name ->
          scope.launch {
            val id = container.playlists.create(name)
            state.queue.forEach { container.playlists.addTrack(id, it) }
            container.notifications.post("Playlist created", "\"$name\" with ${state.queue.size} tracks from the queue")
          }
        },
        onDismiss = { showSaveQueue = false },
      )
    }
    val listState = rememberLazyListState()
    val reorder = rememberReorderableLazyListState(listState) { from, to ->
      val q = container.player.state.value.queue
      val f = q.indexOfFirst { it.key == from.key }; val t = q.indexOfFirst { it.key == to.key }
      if (f >= 0 && t >= 0) container.player.moveQueueItem(f, t)
    }
    Box(Modifier.fillMaxSize().padding(padding)) {
      Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(animatedTint.copy(alpha = 0.40f), animatedTint.copy(alpha = 0.12f), MaterialTheme.colorScheme.background))))
      LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 24.dp)) {
        item(key = "player") {
          Column(Modifier.padding(horizontal = 24.dp)) {
            val seekSeconds = settings.doubleTapSeekSeconds
            var seekFlash by remember { mutableStateOf<Int?>(null) }
            androidx.compose.runtime.LaunchedEffect(seekFlash) { if (seekFlash != null) { kotlinx.coroutines.delay(700); seekFlash = null } }
            val artScale by animateFloatAsState(
              targetValue = if (reduced || state.isPlaying) 1f else 0.92f,
              animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "artScale",
            )
            val gestureModifier = Modifier
              .pointerInput(seekSeconds, settings.doubleTapSeek) {
                if (settings.doubleTapSeek) detectTapGestures(
                  onDoubleTap = { offset ->
                    val back = offset.x < size.width / 2
                    container.player.seekBy((if (back) -seekSeconds else seekSeconds) * 1000L)
                    seekFlash = if (back) -seekSeconds else seekSeconds
                    if (hapticsOn) haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                  },
                )
              }
              .pointerInput(Unit) {
                // One gesture, two meanings: horizontal swipe skips, a downward pull collapses.
                var dx = 0f
                var dy = 0f
                detectDragGestures(
                  onDragStart = { dx = 0f; dy = 0f },
                  onDrag = { change, amount -> change.consume(); dx += amount.x; dy += amount.y },
                  onDragEnd = {
                    if (kotlin.math.abs(dy) > kotlin.math.abs(dx) && dy > 160f) { if (hapticsOn) haptic.performHapticFeedback(HapticFeedbackType.SegmentTick); onBack() }
                    else if (kotlin.math.abs(dx) > kotlin.math.abs(dy) && dx < -140f) { if (hapticsOn) haptic.performHapticFeedback(HapticFeedbackType.Confirm); container.player.next() }
                    else if (kotlin.math.abs(dx) > kotlin.math.abs(dy) && dx > 140f) { if (hapticsOn) haptic.performHapticFeedback(HapticFeedbackType.Confirm); container.player.previous() }
                  },
                )
              }
            val artShape = RoundedCornerShape(28.dp)
            Box(
              Modifier.fillMaxWidth().padding(top = 8.dp).aspectRatio(1f)
                .graphicsLayer { scaleX = artScale; scaleY = artScale }
                .shadow(24.dp, artShape, ambientColor = animatedTint, spotColor = animatedTint)
                .clip(artShape).then(gestureModifier),
            ) {
              AnimatedContent(
                targetState = track.artworkUrl, modifier = Modifier.fillMaxSize(),
                transitionSpec = { fadeIn(tween(if (reduced) 0 else 320)) togetherWith fadeOut(tween(if (reduced) 0 else 320)) }, label = "npArt",
              ) { art ->
                if (art != null) {
                  AsyncImage(model = art, contentDescription = "Album art for ${track.title}", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                  Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.MusicNote, contentDescription = null, modifier = Modifier.size(72.dp))
                  }
                }
              }
              androidx.compose.animation.AnimatedVisibility(
                visible = seekFlash != null, modifier = Modifier.align(if ((seekFlash ?: 0) < 0) Alignment.CenterStart else Alignment.CenterEnd),
                enter = fadeIn(tween(80)) + scaleIn(initialScale = 0.6f), exit = fadeOut(tween(300)),
              ) {
                Box(Modifier.padding(24.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 14.dp, vertical = 10.dp)) {
                  Text(
                    (if ((seekFlash ?: 0) < 0) "-" else "+") + "${kotlin.math.abs(seekFlash ?: 0)}s",
                    color = Color.White, style = MaterialTheme.typography.titleMedium,
                  )
                }
              }
            }
            Row(Modifier.fillMaxWidth().padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
              Column(Modifier.weight(1f)) {
                Text(track.title, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                  track.artist, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1, overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.clickable(onClickLabel = "Open artist page") { onOpenArtist(track.artist) }.hoverHand(),
                )
              }
              xyz.pepe.glacierclient.musicapp.ui.library.AddToPlaylistButton(container, track)
              xyz.pepe.glacierclient.musicapp.ui.components.LikeButton(
                liked = liked.any { it.key == track.key },
                onToggle = { container.favorites.toggle(track) },
              )
            }

            SeekBar(container, state.durationMs, hapticsOn)

            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
              IconButton(onClick = { container.player.toggleShuffle() }, modifier = Modifier.hoverHand()) {
                Icon(Icons.Rounded.Shuffle, contentDescription = "Shuffle", tint = if (state.shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
              }
              IconButton(onClick = { container.player.previous() }, modifier = Modifier.size(56.dp).hoverHand()) { Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(36.dp)) }
              val corner by animateDpAsState(if (state.isPlaying) 32.dp else 22.dp, spring(stiffness = Spring.StiffnessMediumLow), label = "playShape")
              val playSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
              FilledIconButton(
                onClick = { container.player.togglePlayPause() },
                interactionSource = playSource,
                shape = RoundedCornerShape(corner),
                modifier = Modifier.size(68.dp).pressScale(playSource).hoverHand(),
              ) {
                if (state.isBuffering) androidx.compose.material3.LoadingIndicator(Modifier.size(40.dp), color = MaterialTheme.colorScheme.onPrimary)
                else AnimatedContent(
                  targetState = state.isPlaying,
                  transitionSpec = { (fadeIn(tween(160)) + scaleIn(initialScale = 0.6f)) togetherWith (fadeOut(tween(100)) + scaleOut(targetScale = 0.6f)) },
                  label = "playPause",
                ) { playing ->
                  Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = if (playing) "Pause" else "Play", modifier = Modifier.size(34.dp))
                }
              }
              IconButton(onClick = { container.player.next() }, modifier = Modifier.size(56.dp).hoverHand()) { Icon(Icons.Rounded.SkipNext, contentDescription = "Next", modifier = Modifier.size(36.dp)) }
              IconButton(onClick = { container.player.cycleRepeat() }, modifier = Modifier.hoverHand()) {
                Icon(
                  if (state.repeat == RepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                  contentDescription = "Repeat",
                  tint = if (state.repeat != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
        item(key = "queue-header") {
          Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.QueueMusic, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("UP NEXT (${state.queue.size})", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.8.sp, modifier = Modifier.weight(1f).padding(start = 8.dp))
            if (state.queue.isNotEmpty()) {
              IconButton(onClick = { showSaveQueue = true }) { Icon(Icons.Rounded.PlaylistAdd, contentDescription = "Save queue as playlist") }
              IconButton(onClick = { xyz.pepe.glacierclient.musicapp.ui.util.exportM3u(context, "queue", state.queue) }) {
                Icon(Icons.Rounded.Share, contentDescription = "Export queue as M3U")
              }
            }
          }
        }
        items(state.queue.size, key = { state.queue[it].key }) { i ->
          val t = state.queue[i]
          ReorderableItem(reorder, key = t.key) {
            val current = i == state.index
            GlassCard(
              Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
              shape = RoundedCornerShape(18.dp),
              onClick = { container.player.play(state.queue, i) },
            ) {
              Row(
                Modifier.fillMaxWidth()
                  .background(if (current) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f) else Color.Transparent)
                  .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(Modifier.weight(1f)) {
                  Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                  Text(t.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { container.player.removeFromQueue(i) }) { Icon(Icons.Rounded.Close, contentDescription = "Remove from queue") }
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
}

/**
 * Seek bar that owns the 4x/second position updates, so only this small composable recomposes
 * while playing. Thumb grows while scrubbing; mouse wheel seeks 5 s per notch.
 */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun SeekBar(container: AppContainer, durationMs: Long, hapticsOn: Boolean) {
  val positionMs by remember { container.player.state.map { it.positionMs }.distinctUntilChanged() }
    .collectAsState(initial = container.player.state.value.positionMs)
  // The position loop updates every 250ms; binding the slider straight to it made the thumb fight
  // your finger mid-drag. Track the drag locally and only seek once you let go.
  var dragPositionMs by remember { mutableStateOf<Float?>(null) }
  val durationF = durationMs.toFloat().coerceAtLeast(1f)
  val source = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
  Slider(
    value = (dragPositionMs ?: positionMs.toFloat()).coerceIn(0f, durationF),
    onValueChange = { dragPositionMs = it },
    onValueChangeFinished = {
      val target = dragPositionMs
      if (target != null) container.player.seekTo(target.toLong())
      dragPositionMs = null
    },
    valueRange = 0f..durationF,
    interactionSource = source,
    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
      .mouseWheel { dy -> container.player.seekBy((-dy * 5000f).toLong()) },
  )
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(formatMs((dragPositionMs ?: positionMs.toFloat()).toLong()), style = MaterialTheme.typography.labelSmall)
    Text(formatMs(durationMs), style = MaterialTheme.typography.labelSmall)
  }
}

private fun formatMs(ms: Long): String {
  val d = ms.milliseconds
  val minutes = d.inWholeMinutes
  val seconds = d.inWholeSeconds % 60
  return "%d:%02d".format(minutes, seconds)
}

/**
 * Lyrics from lrclib.net (free, keyless, open lyrics DB) — plain text, or synced with the current
 * line highlighted and auto-scrolled if the track has timed lyrics there. Shows "not found"
 * honestly instead of placeholder text when lrclib has nothing for this track.
 */
@Composable
private fun LyricsSheet(container: AppContainer, track: xyz.pepe.glacierclient.musicapp.domain.model.Track, onDismiss: () -> Unit) {
  val positionMs by remember { container.player.state.map { it.positionMs / 250 * 250 }.distinctUntilChanged() }
    .collectAsState(initial = container.player.state.value.positionMs)
  var lyrics by remember(track.key) { mutableStateOf<xyz.pepe.glacierclient.musicapp.data.remote.Lyrics?>(null) }
  var loading by remember(track.key) { mutableStateOf(true) }
  var failed by remember(track.key) { mutableStateOf(false) }
  var attempt by remember(track.key) { mutableStateOf(0) }
  androidx.compose.runtime.LaunchedEffect(track.key, attempt) {
    loading = true
    val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
      runCatching { container.lyrics.search(track.title, track.artist, track.durationMs) }
    }
    lyrics = result.getOrNull()
    failed = result.isFailure
    loading = false
  }
  val settings by container.settings.settings.collectAsState()
  val scale = settings.lyricsFontScale
  androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
      Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
          Text(track.title, style = MaterialTheme.typography.titleLarge)
          Text(track.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = { container.settings.update { it.copy(lyricsFontScale = (it.lyricsFontScale - 0.15f).coerceAtLeast(0.7f)) } }) { Text("A", style = MaterialTheme.typography.bodySmall) }
        IconButton(onClick = { container.settings.update { it.copy(lyricsFontScale = (it.lyricsFontScale + 0.15f).coerceAtMost(1.6f)) } }) { Text("A", style = MaterialTheme.typography.titleLarge) }
      }
      // Cross-app: hand the lyrics to Translator or Notes. Each chip only appears when that app
      // is installed, so there's never a button that leads nowhere.
      val ctx = androidx.compose.ui.platform.LocalContext.current
      val lyricsText = lyrics?.let { l -> l.plain ?: l.synced?.joinToString("\n") { it.text } }
      if (!lyricsText.isNullOrBlank()) {
        val hasTranslator = remember { PepeEcosystem.isInstalled(ctx, PepeEcosystem.App.TRANSLATOR) }
        val hasNotes = remember { PepeEcosystem.isInstalled(ctx, PepeEcosystem.App.NOTES) }
        Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
          if (hasTranslator) androidx.compose.material3.AssistChip(
            onClick = { PepeEcosystem.send(ctx, PepeEcosystem.Translate(lyricsText.take(PepeEcosystem.MAX_TEXT))) },
            label = { Text("Translate") },
          )
          if (hasNotes) androidx.compose.material3.AssistChip(
            onClick = { PepeEcosystem.send(ctx, PepeEcosystem.CreateNote("${track.title} — ${track.artist}\n\n$lyricsText".take(PepeEcosystem.MAX_TEXT), title = track.title)) },
            label = { Text("Save to Notes") },
          )
        }
      }
      if (lyrics?.synced != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            "Sync " + (if (settings.lyricsOffsetMs == 0) "in time" else "%+.1f s".format(settings.lyricsOffsetMs / 1000f)),
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
          )
          androidx.compose.material3.TextButton(onClick = { container.settings.update { it.copy(lyricsOffsetMs = (it.lyricsOffsetMs - 500).coerceAtLeast(-5000)) } }) { Text("Earlier") }
          androidx.compose.material3.TextButton(onClick = { container.settings.update { it.copy(lyricsOffsetMs = (it.lyricsOffsetMs + 500).coerceAtMost(5000)) } }) { Text("Later") }
        }
      }
      androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
      when {
        loading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { androidx.compose.material3.LoadingIndicator() }
        failed -> Column {
          Text("Couldn't load lyrics — check your connection.", color = MaterialTheme.colorScheme.onSurfaceVariant)
          androidx.compose.material3.TextButton(onClick = { attempt++ }) { Text("Retry") }
        }
        lyrics == null -> Text("No lyrics found for this track on lrclib.net.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        lyrics?.synced != null -> {
          val lines = lyrics!!.synced!!
          val currentIndex = lines.indexOfLast { it.atMs + settings.lyricsOffsetMs <= positionMs }
          val listState = rememberLazyListState()
          androidx.compose.runtime.LaunchedEffect(currentIndex) { if (currentIndex >= 0) listState.animateScrollToItem((currentIndex - 2).coerceAtLeast(0)) }
          LazyColumn(state = listState, modifier = Modifier.heightIn(max = 420.dp)) {
            // Keyed by index: LRC routinely has two lines with the same timestamp (repeated chorus
            // tags, duets), and keying by time crashed with "Key was already used".
            items(lines.size, key = { it }) { i ->
              Text(
                lines[i].text.ifBlank { "♪" },
                style = (if (i == currentIndex) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge).let { it.copy(fontSize = it.fontSize * scale) },
                color = animateColorAsState(if (i == currentIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, tween(250), label = "lyric").value,
                modifier = Modifier.fillMaxWidth()
                  .clickable(onClickLabel = "Seek to this line") { container.player.seekTo(lines[i].atMs) }
                  .padding(vertical = 6.dp),
              )
            }
          }
        }
        else -> LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
          item { Text(lyrics!!.plain.orEmpty(), style = MaterialTheme.typography.bodyLarge.let { it.copy(fontSize = it.fontSize * scale) }) }
        }
      }
    }
  }
}
