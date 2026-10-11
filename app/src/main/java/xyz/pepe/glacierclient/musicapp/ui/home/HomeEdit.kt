@file:OptIn(
  androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
  androidx.compose.material3.ExperimentalMaterial3Api::class,
  androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package xyz.pepe.glacierclient.musicapp.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import kotlinx.coroutines.launch
import xyz.pepe.glacierclient.ecosystem.CardSize
import xyz.pepe.glacierclient.ecosystem.CardSlot
import xyz.pepe.glacierclient.ecosystem.DashboardLayout
import xyz.pepe.glacierclient.musicapp.ui.components.SectionHeader
import xyz.pepe.glacierclient.musicapp.ui.components.rememberHaptics
import xyz.pepe.glacierclient.musicapp.ui.components.rememberReducedMotion

/**
 * Home in edit mode: every section is an outlined tile that can be long-press dragged to reorder,
 * moved with Alt+Up/Down or the arrow buttons, resized with the right-edge handle (where HALF
 * looks right) and hidden into the "Available sections" tray. Every change is saved immediately.
 */
@Composable
internal fun HomeEditList(
  ctx: HomeCtx,
  layout: DashboardLayout,
  apply: (DashboardLayout) -> Unit,
  greetName: String,
  photoUrl: String?,
  streak: Int,
) {
  val haptics = rememberHaptics(ctx.settings.haptics)
  val reduced = rememberReducedMotion()
  val listState = rememberLazyListState()
  val scope = rememberCoroutineScope()
  val currentLayout by rememberUpdatedState(layout)
  val currentApply by rememberUpdatedState(apply)
  var selectedId by remember { mutableStateOf<String?>(null) }
  var dragId by remember { mutableStateOf<String?>(null) }
  val bounds = remember { HashMap<String, Rect>() }
  var containerRect by remember { mutableStateOf(Rect.Zero) }
  val chrome = remember { Animatable(if (reduced) 1f else 0f) }
  LaunchedEffect(Unit) { if (!reduced) chrome.animateTo(1f, tween(220)) }
  val rows = remember(layout) { layout.rows() }
  val visibleIds = layout.visible.map { it.id }

  Box(
    Modifier
      .fillMaxSize()
      .onGloballyPositioned { containerRect = it.boundsInRoot() }
      .pointerInput(Unit) {
        var pos = Offset.Zero
        var lastSwap = 0L
        detectDragGesturesAfterLongPress(
          onDragStart = { off ->
            pos = off + containerRect.topLeft
            val id = bounds.entries.firstOrNull { it.value.contains(pos) }?.key
            dragId = id
            if (id != null) { selectedId = id; haptics.tick() }
          },
          onDrag = { change, delta ->
            change.consume()
            val id = dragId
            if (id != null) {
              pos += delta
              val now = System.currentTimeMillis()
              if (now - lastSwap > 220L) {
                val lay = currentLayout
                val target = lay.visible.firstOrNull { it.id != id && bounds[it.id]?.contains(pos) == true }
                if (target != null) {
                  val idx = lay.visible.indexOfFirst { it.id == target.id }
                  currentApply(lay.moveTo(id, idx))
                  lastSwap = now
                  haptics.tick()
                }
              }
              if (pos.y < containerRect.top + 120f) scope.launch { listState.scrollBy(-30f) }
              else if (pos.y > containerRect.bottom - 120f) scope.launch { listState.scrollBy(30f) }
            }
          },
          onDragEnd = { if (dragId != null) haptics.confirm(); dragId = null },
          onDragCancel = { dragId = null },
        )
      },
  ) {
    LazyColumn(
      state = listState,
      userScrollEnabled = dragId == null,
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      if (ctx.settings.showGreeting) item(key = "greeting") { Greeting(greetName, photoUrl, streak) }
      item(key = "hint") {
        Text(
          "Long-press a section and drag to reorder. Tap one to show its resize handle. Alt+Up/Down also moves the focused section.",
          style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 20.dp),
        )
      }
      items(rows, key = { r -> "row_" + r.first().id }) { row ->
        Row(
          Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .then(if (reduced) Modifier else Modifier.animateItem().animateContentSize()),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          row.forEach { slot ->
            key(slot.id) {
              val vi = visibleIds.indexOf(slot.id)
              Box(Modifier.weight(1f)) {
                EditTile(
                  ctx = ctx, slot = slot, selected = selectedId == slot.id, dragging = dragId == slot.id,
                  canUp = vi > 0, canDown = vi in 0 until visibleIds.lastIndex,
                  chrome = chrome.value, reduced = reduced, bounds = bounds,
                  onSelect = { selectedId = if (selectedId == slot.id) null else slot.id },
                  onMoveBy = { d -> haptics.tick(); currentApply(currentLayout.moveBy(slot.id, d)) },
                  onHide = { haptics.tick(); if (selectedId == slot.id) selectedId = null; currentApply(currentLayout.setHidden(slot.id, true)) },
                  onResize = { size -> haptics.tick(); currentApply(currentLayout.resize(slot.id, size)) },
                )
              }
            }
          }
        }
      }
      item(key = "avail_header") { SectionHeader("Available sections") }
      val hidden = layout.hiddenSlots
      if (hidden.isEmpty()) {
        item(key = "avail_empty") {
          Text(
            "Nothing hidden. Use the eye button on a section to move it here.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp),
          )
        }
      } else {
        items(hidden, key = { "avail_" + it.id }) { slot ->
          Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp).clip(RoundedCornerShape(16.dp))
              .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
              .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(HomeSections.title(slot.id), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            IconButton(onClick = { haptics.tick(); currentApply(currentLayout.setHidden(slot.id, false)) }) {
              Icon(Icons.Rounded.Add, contentDescription = "Add ${HomeSections.title(slot.id)} back to Home")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun EditTile(
  ctx: HomeCtx,
  slot: CardSlot,
  selected: Boolean,
  dragging: Boolean,
  canUp: Boolean,
  canDown: Boolean,
  chrome: Float,
  reduced: Boolean,
  bounds: MutableMap<String, Rect>,
  onSelect: () -> Unit,
  onMoveBy: (Int) -> Unit,
  onHide: () -> Unit,
  onResize: (CardSize) -> Unit,
) {
  val fr = remember { FocusRequester() }
  val shape = RoundedCornerShape(20.dp)
  val half = slot.size == CardSize.HALF
  DisposableEffect(slot.id) { onDispose { bounds.remove(slot.id) } }
  val scale by animateFloatAsState(if (dragging && !reduced) 1.03f else 1f, label = "dragScale")
  val outline = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
  Box(
    Modifier
      .fillMaxWidth()
      .onGloballyPositioned { bounds[slot.id] = it.boundsInRoot() }
      .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (dragging) 0.92f else 1f }
      .onKeyEvent { e ->
        if (e.type == KeyEventType.KeyDown && e.isAltPressed && e.key == Key.DirectionUp) { onMoveBy(-1); true }
        else if (e.type == KeyEventType.KeyDown && e.isAltPressed && e.key == Key.DirectionDown) { onMoveBy(1); true }
        else false
      }
      .focusRequester(fr)
      .focusable()
      .clip(shape)
      .background(MaterialTheme.colorScheme.surfaceContainerLow)
      .border(if (selected) 2.dp else 1.dp, outline, shape),
  ) {
    androidx.compose.foundation.layout.Column {
      Row(
        Modifier.fillMaxWidth().padding(start = 8.dp, end = 2.dp).graphicsLayer { alpha = chrome },
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          Icons.Rounded.DragHandle, contentDescription = "Drag handle. Long-press and drag to move.",
          tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp).size(22.dp),
        )
        if (!half) {
          Text(
            HomeSections.title(slot.id), style = MaterialTheme.typography.labelLarge, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
          )
        } else {
          Box(Modifier.weight(1f))
        }
        if (canUp) IconButton(onClick = { onMoveBy(-1) }, modifier = Modifier.size(36.dp)) {
          Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Move ${HomeSections.title(slot.id)} up")
        }
        if (canDown) IconButton(onClick = { onMoveBy(1) }, modifier = Modifier.size(36.dp)) {
          Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Move ${HomeSections.title(slot.id)} down")
        }
        IconButton(onClick = onHide, modifier = Modifier.size(36.dp)) {
          Icon(Icons.Rounded.VisibilityOff, contentDescription = "Hide ${HomeSections.title(slot.id)}")
        }
      }
      Box(Modifier.fillMaxWidth().heightIn(max = 240.dp).clipToBounds()) {
        SectionBody(ctx, slot.id, compact = half, preview = true)
        // Blocks the preview's own controls; a tap selects the tile (and focuses it for Alt+arrows).
        Box(
          Modifier.matchParentSize().pointerInput(slot.id) {
            detectTapGestures(onTap = { onSelect(); runCatching { fr.requestFocus() } })
          },
        )
      }
    }
    if (selected && HomeSections.halfAllowed(slot.id)) {
      Box(
        Modifier
          .align(Alignment.CenterEnd)
          .padding(end = 2.dp)
          .size(width = 30.dp, height = 72.dp)
          .semantics { contentDescription = "Resize handle. Tap or drag to switch between full and half width." }
          .clickable { onResize(if (half) CardSize.FULL else CardSize.HALF) }
          .pointerInput(slot.id, slot.size) {
            var acc = 0f
            detectHorizontalDragGestures(
              onDragStart = { acc = 0f },
              onHorizontalDrag = { change, dx ->
                change.consume()
                acc += dx
                if (acc < -60f && !half) { acc = 0f; onResize(CardSize.HALF) }
                else if (acc > 60f && half) { acc = 0f; onResize(CardSize.FULL) }
              },
            )
          },
        contentAlignment = Alignment.Center,
      ) {
        Box(Modifier.size(width = 6.dp, height = 44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
      }
    }
  }
}
