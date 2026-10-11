package xyz.pepe.glacierclient.musicapp.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import xyz.pepe.glacierclient.musicapp.domain.model.Track

@OptIn(ExperimentalFoundationApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun TrackRow(
  track: Track,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  onLongPress: (() -> Unit)? = null,
  /** Swipe right (e.g. like) and swipe left (e.g. add to queue); each fires once past the threshold. */
  onSwipeRight: (() -> Unit)? = null,
  onSwipeLeft: (() -> Unit)? = null,
  trailing: @Composable (RowScope.() -> Unit) = {},
) {
  val glass = LocalGlassEnabled.current
  val reduced = rememberReducedMotion()
  val hapticsOn = LocalHapticsEnabled.current
  val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
  val cardShape = RoundedCornerShape(18.dp)
  val source = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
  val focused by source.collectIsFocusedAsState()
  val swipe = remember { androidx.compose.animation.core.Animatable(0f) }
  val scope = androidx.compose.runtime.rememberCoroutineScope()
  val swipeModifier = if (onSwipeRight == null && onSwipeLeft == null) Modifier else Modifier.pointerInput(onSwipeRight, onSwipeLeft) {
    var fired = false
    detectHorizontalDragGestures(
      onDragStart = { fired = false },
      onDragEnd = { scope.launch { swipe.animateTo(0f, spring(stiffness = Spring.StiffnessMedium)) } },
      onDragCancel = { scope.launch { swipe.animateTo(0f) } },
    ) { change, delta ->
      change.consume()
      val allowed = (swipe.value + delta * 0.6f).coerceIn(if (onSwipeLeft != null) -160f else 0f, if (onSwipeRight != null) 160f else 0f)
      scope.launch { swipe.snapTo(allowed) }
      if (!fired && allowed > 120f) { fired = true; if (hapticsOn) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.Confirm); onSwipeRight?.invoke() }
      if (!fired && allowed < -120f) { fired = true; if (hapticsOn) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.Confirm); onSwipeLeft?.invoke() }
    }
  }
  Row(
    modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 3.dp)
      .graphicsLayer { translationX = swipe.value }
      .pressScale(source)
      .then(
        if (glass) Modifier.glass(cardShape).border(if (focused) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else GlassBorder, cardShape)
        else Modifier.clip(cardShape).background(MaterialTheme.colorScheme.surfaceContainerLow)
          .then(if (focused) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, cardShape) else Modifier),
      )
      .then(swipeModifier)
      .onSecondaryClick { onLongPress?.invoke() }
      .combinedClickable(interactionSource = source, indication = androidx.compose.material3.ripple(), onClick = onClick, onLongClick = onLongPress)
      .hoverHand()
      .padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (track.artworkUrl != null) {
      AsyncImage(
        model = track.artworkUrl,
        contentDescription = null,
        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)),
      )
    } else {
      Box(
        Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
      ) { Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
      Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
      Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    trailing()
  }
}

/**
 * The "⋮" menu — also the menu a long-press on the card opens (pass the same [expanded]/
 * [onExpandedChange] pair to [TrackRow]'s `onLongPress`). Every entry is optional: pass only the
 * actions that make sense where you're calling this from (e.g. a queue row has no "add to queue").
 */
@Composable
fun TrackOverflowMenu(
  onPlayNext: (() -> Unit)? = null,
  onAddToQueue: (() -> Unit)? = null,
  onAddToPlaylist: (() -> Unit)? = null,
  onShare: (() -> Unit)? = null,
  onStartRadio: (() -> Unit)? = null,
  onGoToArtist: (() -> Unit)? = null,
  liked: Boolean? = null,
  onToggleLike: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
  expanded: Boolean? = null,
  onExpandedChange: ((Boolean) -> Unit)? = null,
  showButton: Boolean = true,
) {
  var selfOpen by remember { mutableStateOf(false) }
  val open = expanded ?: selfOpen
  val setOpen: (Boolean) -> Unit = onExpandedChange ?: { selfOpen = it }
  Box(modifier) {
    if (showButton) {
      IconButton(onClick = { setOpen(true) }) {
        Icon(Icons.Rounded.MoreVert, contentDescription = "More")
      }
    }
    DropdownMenu(expanded = open, onDismissRequest = { setOpen(false) }) {
      if (onToggleLike != null) {
        DropdownMenuItem(
          text = { Text(if (liked == true) "Remove from Liked" else "Add to Liked") },
          leadingIcon = { Icon(if (liked == true) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, contentDescription = null) },
          onClick = { onToggleLike(); setOpen(false) },
        )
      }
      if (onPlayNext != null) {
        DropdownMenuItem(text = { Text("Play next") }, leadingIcon = { Icon(Icons.Rounded.SkipNext, contentDescription = null) }, onClick = { onPlayNext(); setOpen(false) })
      }
      if (onAddToQueue != null) {
        DropdownMenuItem(text = { Text("Add to queue") }, leadingIcon = { Icon(Icons.Rounded.QueueMusic, contentDescription = null) }, onClick = { onAddToQueue(); setOpen(false) })
      }
      if (onAddToPlaylist != null) {
        DropdownMenuItem(text = { Text("Add to playlist") }, leadingIcon = { Icon(Icons.Rounded.PlaylistAdd, contentDescription = null) }, onClick = { onAddToPlaylist(); setOpen(false) })
      }
      if (onStartRadio != null) {
        DropdownMenuItem(text = { Text("Start radio") }, leadingIcon = { Icon(Icons.Rounded.Radio, contentDescription = null) }, onClick = { onStartRadio(); setOpen(false) })
      }
      if (onGoToArtist != null) {
        DropdownMenuItem(text = { Text("Go to artist") }, leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) }, onClick = { onGoToArtist(); setOpen(false) })
      }
      if (onShare != null) {
        DropdownMenuItem(text = { Text("Share") }, leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) }, onClick = { onShare(); setOpen(false) })
      }
    }
  }
}

@Composable
fun LikeButton(liked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
  val reduced = rememberReducedMotion()
  val scale = remember { androidx.compose.animation.core.Animatable(1f) }
  var first by remember { mutableStateOf(true) }
  androidx.compose.runtime.LaunchedEffect(liked) {
    if (first) { first = false; return@LaunchedEffect }
    if (liked && !reduced) {
      scale.animateTo(1.45f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh))
      scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    }
  }
  val tint by androidx.compose.animation.animateColorAsState(
    if (liked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, label = "likeTint",
  )
  IconButton(onClick = onToggle, modifier = modifier.hoverHand()) {
    Icon(
      if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
      contentDescription = if (liked) "Unlike" else "Like",
      tint = tint,
      modifier = Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value },
    )
  }
}
