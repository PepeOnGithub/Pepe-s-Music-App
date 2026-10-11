package xyz.pepe.glacierclient.musicapp.ui.components

import android.provider.Settings
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalContext

/** Mirrors Settings > Haptic feedback for components that have no AppContainer in reach. */
val LocalHapticsEnabled = compositionLocalOf { true }

/** True when the system "remove animations" / animator duration scale is 0. */
@Composable
fun rememberReducedMotion(): Boolean {
  val context = LocalContext.current
  return remember {
    runCatching {
      Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }.getOrDefault(false)
  }
}

/** Press-state scale (0.96) driven by a spring; a no-op under reduced motion. */
@Composable
fun Modifier.pressScale(source: InteractionSource, pressed: Float = 0.96f): Modifier {
  val reduced = rememberReducedMotion()
  val isPressed by source.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed && !reduced) pressed else 1f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
    label = "pressScale",
  )
  return this.graphicsLayer { scaleX = scale; scaleY = scale }
}

/** Hand cursor for mouse users on anything clickable. */
fun Modifier.hoverHand(): Modifier = this.pointerHoverIcon(PointerIcon.Hand)

/** Mouse wheel over this element; [onScroll] gets the vertical delta (positive = wheel down). */
fun Modifier.mouseWheel(onScroll: (Float) -> Unit): Modifier = this.pointerInput(Unit) {
  awaitPointerEventScope {
    while (true) {
      val e = awaitPointerEvent()
      if (e.type == PointerEventType.Scroll) {
        val dy = e.changes.firstOrNull()?.scrollDelta?.y ?: 0f
        if (dy != 0f) onScroll(dy)
      }
    }
  }
}

/** Right mouse button acts like a long press. */
fun Modifier.onSecondaryClick(action: () -> Unit): Modifier = this.pointerInput(Unit) {
  awaitPointerEventScope {
    while (true) {
      val e = awaitPointerEvent()
      if (e.type == PointerEventType.Press && e.buttons.isSecondaryPressed) action()
    }
  }
}
