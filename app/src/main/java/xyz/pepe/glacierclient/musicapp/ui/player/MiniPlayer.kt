package xyz.pepe.glacierclient.musicapp.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import xyz.pepe.glacierclient.musicapp.di.AppContainer
import xyz.pepe.glacierclient.musicapp.playback.PlayerState
import xyz.pepe.glacierclient.musicapp.ui.components.GlassBorder
import xyz.pepe.glacierclient.musicapp.ui.components.Haptics
import xyz.pepe.glacierclient.musicapp.ui.components.rememberHaptics
import xyz.pepe.glacierclient.musicapp.ui.components.glass
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput

/** Swipe left/right on the mini player to skip (gated by the setting); drag up always opens Now Playing. */
private fun Modifier.swipeToSkip(container: AppContainer, enabled: Boolean, haptics: Haptics, onOpen: () -> Unit): Modifier =
  this.pointerInput(haptics, enabled) {
    var dx = 0f
    var dy = 0f
    detectDragGestures(
      onDragStart = { dx = 0f; dy = 0f },
      onDrag = { change, amount -> change.consume(); dx += amount.x; dy += amount.y },
      onDragEnd = {
        if (kotlin.math.abs(dy) > kotlin.math.abs(dx) && dy < -80f) { haptics.tick(); onOpen() }
        else if (enabled && dx < -120f) { haptics.confirm(); container.player.next() }
        else if (enabled && dx > 120f) { haptics.confirm(); container.player.previous() }
      },
    )
  }

/**
 * Both mini players collect [AppContainer.player]'s state themselves, rather than receiving it as
 * a parameter from `AppRoot`. That keeps the 4x/second position updates scoped to just this small
 * composable — `AppRoot` only needs to know *whether* to reserve room for a mini player
 * ([PlayerController.hasTrack]), not the live position, so it never has to recompose (and force a
 * relayout of the whole screen) on every tick.
 */

/** Flush, full-width mini player docked directly above the classic bottom nav bar. */
@Composable
fun ClassicMiniPlayer(container: AppContainer, onOpen: () -> Unit, modifier: Modifier = Modifier) {
  val state by container.player.state.collectAsState()
  val track = state.current ?: return
  val gestures by container.settings.settings.collectAsState()
  val haptics = rememberHaptics(gestures.haptics)
  Column(
    modifier
      .fillMaxWidth()
      .swipeToSkip(container, gestures.swipeMiniPlayerToSkip, haptics, onOpen)
      .clickable(onClick = onOpen)
      .background(MaterialTheme.colorScheme.surfaceContainerHigh),
  ) {
    ProgressLine(state, Modifier.fillMaxWidth())
    MiniPlayerRow(track, state, { haptics.tick(); container.player.togglePlayPause() }, { haptics.tick(); container.player.next() })
  }
}

/** Rounded pill card that floats with a gap above the floating nav pill, same corner family as it. */
@Composable
fun FloatingMiniPlayer(container: AppContainer, onOpen: () -> Unit, glassEffects: Boolean = true, modifier: Modifier = Modifier) {
  val state by container.player.state.collectAsState()
  val track = state.current ?: return
  val shape = RoundedCornerShape(28.dp)
  val gestures by container.settings.settings.collectAsState()
  val haptics = rememberHaptics(gestures.haptics)
  Surface(
    onClick = onOpen,
    modifier = (if (glassEffects) modifier.fillMaxWidth().glass(shape) else modifier.fillMaxWidth()).swipeToSkip(container, gestures.swipeMiniPlayerToSkip, haptics, onOpen),
    shape = shape,
    color = if (glassEffects) androidx.compose.ui.graphics.Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh,
    border = if (glassEffects) GlassBorder else null,
    shadowElevation = 12.dp,
  ) {
    Column {
      ProgressLine(state, Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)))
      MiniPlayerRow(track, state, { haptics.tick(); container.player.togglePlayPause() }, { haptics.tick(); container.player.next() })
    }
  }
}

@Composable
private fun ProgressLine(state: PlayerState, modifier: Modifier = Modifier) {
  val progress = if (state.durationMs > 0) (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f
  LinearProgressIndicator(progress = { progress }, modifier = modifier.height(2.dp))
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MiniPlayerRow(track: xyz.pepe.glacierclient.musicapp.domain.model.Track, state: PlayerState, onTogglePlay: () -> Unit, onNext: () -> Unit) {
  Row(Modifier.height(70.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
    AnimatedContent(targetState = track.artworkUrl, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(220)) }, label = "miniArt") { art ->
      if (art != null) {
        AsyncImage(model = art, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
      } else {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
          Icon(Icons.Rounded.MusicNote, contentDescription = null)
        }
      }
    }
    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
      Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
      Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    IconButton(onClick = onTogglePlay) {
      if (state.isBuffering) androidx.compose.material3.LoadingIndicator(Modifier.size(32.dp))
      else AnimatedContent(targetState = state.isPlaying, transitionSpec = { (fadeIn(tween(150)) + scaleIn(initialScale = 0.7f)) togetherWith (fadeOut(tween(100)) + scaleOut(targetScale = 0.7f)) }, label = "miniPlay") { playing ->
        Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = if (playing) "Pause" else "Play")
      }
    }
    IconButton(onClick = onNext) {
      Icon(Icons.Rounded.SkipNext, contentDescription = "Next")
    }
  }
}
