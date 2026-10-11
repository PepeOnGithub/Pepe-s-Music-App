@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package xyz.pepe.glacierclient.musicapp.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.contentColorFor
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer

/** Haptics wrapper that respects the "Haptic feedback" setting. */
class Haptics(private val enabled: Boolean, private val fb: androidx.compose.ui.hapticfeedback.HapticFeedback) {
  fun toggle(on: Boolean) { if (enabled) fb.performHapticFeedback(if (on) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff) }
  fun tick() { if (enabled) fb.performHapticFeedback(HapticFeedbackType.SegmentTick) }
  fun confirm() { if (enabled) fb.performHapticFeedback(HapticFeedbackType.Confirm) }
}

@Composable
fun rememberHaptics(enabled: Boolean): Haptics {
  val fb = LocalHapticFeedback.current
  return remember(enabled, fb) { Haptics(enabled, fb) }
}

/** Single-choice selector: a connected row of toggle buttons. Scrolls horizontally if it overflows. */
@Composable
fun <T> ConnectedChoiceRow(
  options: List<T>,
  selected: T,
  label: @Composable (T) -> String,
  onSelect: (T) -> Unit,
  haptics: Haptics,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier.horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
  ) {
    options.forEachIndexed { i, opt ->
      val shapes = when (i) {
        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
        options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
      }
      ToggleButton(
        checked = opt == selected,
        onCheckedChange = { if (it) { haptics.tick(); onSelect(opt) } },
        shapes = shapes,
        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp).semantics { role = Role.RadioButton },
      ) { Text(label(opt), maxLines = 1) }
    }
  }
}

/** Multi-choice connected row (repeat days). Each item has its own on/off state. */
@Composable
fun <T> ConnectedMultiRow(
  options: List<T>,
  selected: Set<T>,
  label: @Composable (T) -> String,
  onToggle: (T, Boolean) -> Unit,
  haptics: Haptics,
  modifier: Modifier = Modifier,
) {
  Row(modifier, horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
    options.forEachIndexed { i, opt ->
      val shapes = when (i) {
        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
        options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
      }
      ToggleButton(
        checked = opt in selected,
        onCheckedChange = { haptics.toggle(it); onToggle(opt, it) },
        shapes = shapes,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        modifier = Modifier.weight(1f).defaultMinSize(minWidth = 40.dp, minHeight = 48.dp),
      ) { Text(label(opt), maxLines = 1, style = MaterialTheme.typography.labelLarge) }
    }
  }
}

/** Badge: an icon on a Material expressive shape (Cookie9Sided, Sunny, Clover4Leaf, Pill, Gem...). */
enum class BadgeShape { COOKIE, SUNNY, CLOVER, PILL, GEM, PUFFY, SOFT_BURST, CIRCLE }

@Composable
fun ShapeBadge(
  icon: ImageVector,
  modifier: Modifier = Modifier,
  shape: BadgeShape = BadgeShape.COOKIE,
  container: Color = MaterialTheme.colorScheme.primaryContainer,
  content: Color = MaterialTheme.colorScheme.onPrimaryContainer,
  size: androidx.compose.ui.unit.Dp = 48.dp,
) {
  val s: Shape = when (shape) {
    BadgeShape.COOKIE -> MaterialShapes.Cookie9Sided
    BadgeShape.SUNNY -> MaterialShapes.Sunny
    BadgeShape.CLOVER -> MaterialShapes.Clover4Leaf
    BadgeShape.PILL -> MaterialShapes.Pill
    BadgeShape.GEM -> MaterialShapes.Gem
    BadgeShape.PUFFY -> MaterialShapes.Puffy
    BadgeShape.SOFT_BURST -> MaterialShapes.SoftBurst
    BadgeShape.CIRCLE -> MaterialShapes.Circle
  }.toShape()
  Box(modifier.size(size).clip(s).background(container), contentAlignment = Alignment.Center) {
    Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(size * 0.5f))
  }
}

/** One of a fixed rotation of tonal container colours for a settings row's leading icon circle. */
@Composable
fun rowIconColors(index: Int): Pair<Color, Color> {
  val cs = MaterialTheme.colorScheme
  return listOf(
    cs.primaryContainer to cs.onPrimaryContainer,
    Color(0xFF5B4300) to Color(0xFFFFDF9E),
    Color(0xFF263526) to Color(0xFFD0E8CF),
    cs.errorContainer to cs.onErrorContainer,
  )[index.mod(4)]
}

/** Switch with a check mark in the thumb, per the design system. */
@Composable
fun CheckSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, haptics: Haptics, modifier: Modifier = Modifier, enabled: Boolean = true) {
  Switch(
    checked = checked,
    onCheckedChange = { haptics.toggle(it); onCheckedChange(it) },
    modifier = modifier,
    enabled = enabled,
    thumbContent = if (checked) {
      { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
    } else null,
  )
}

/** A vertical group of segmented list items sharing connected corner shapes. */
@Composable
fun SegmentedGroup(count: Int, modifier: Modifier = Modifier, item: @Composable (index: Int, shapes: androidx.compose.material3.ListItemShapes) -> Unit) {
  GlassRows {
    Column(modifier) {
      for (i in 0 until count) item(i, ListItemDefaults.segmentedShapes(index = i, count = count))
    }
  }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
  SectionHeader(title = text, modifier = modifier, icon = icon)
}

/** Frosted glass: segmented list rows use the same translucent fill as GlassCard instead of an opaque surface. */
@Composable
fun GlassRows(content: @Composable () -> Unit) {
  val cs = MaterialTheme.colorScheme
  val fill = if (LocalGlassEnabled.current) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.10f) else cs.surfaceContainerHighest
  MaterialTheme(
    colorScheme = cs.copy(
      surface = fill, surfaceContainer = fill, surfaceContainerLow = fill, surfaceContainerHigh = fill,
      surfaceContainerHighest = fill, surfaceBright = fill,
    ),
    content = content,
  )
}
