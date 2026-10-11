package xyz.pepe.glacierclient.musicapp.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Same frosted-glass recipe as Pepe's Weather's GlassCard, tinted for this app's black/green theme. */
private val GlassTop = Color(0x26FFFFFF)
private val GlassBottom = Color(0x14FFFFFF)
private val GlassBorderColor = Color(0x33FFFFFF)

val GlassBorder = BorderStroke(1.dp, GlassBorderColor)
val GlassCornerRadius = 22.dp

/** Driven by the "Frosted glass" setting (provided once in MainActivity); false = solid tonal surfaces. */
val LocalGlassEnabled = compositionLocalOf { true }

/** Clips to [shape] and paints a translucent white gradient, simulating frosted glass over the theme. */
fun Modifier.glass(shape: Shape): Modifier = this
  .clip(shape)
  .background(Brush.verticalGradient(listOf(GlassTop, GlassBottom)))

/** Glass (gradient + hairline border) when the setting is on, else a solid tonal fill, both clipped to [shape]. */
@Composable
fun Modifier.glassSurface(shape: Shape): Modifier =
  if (LocalGlassEnabled.current) this.glass(shape).border(GlassBorder, shape)
  else this.clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHigh)

/**
 * Family-wide card: frosted glass when the setting is on, a solid tonal container when off.
 * Pass [onClick] to make the whole card tappable (touch ripple + 48dp+ target by content size).
 */
@Composable
fun GlassCard(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(GlassCornerRadius),
  onClick: (() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  val glass = LocalGlassEnabled.current
  val m = if (glass) modifier.glass(shape) else modifier
  val color = if (glass) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh
  val border = if (glass) GlassBorder else null
  if (onClick != null) {
    Surface(onClick = onClick, modifier = m, shape = shape, color = color, border = border) { Column(content = content) }
  } else {
    Surface(modifier = m, shape = shape, color = color, border = border) { Column(content = content) }
  }
}

/** Small tonal icon chip used in headers and stat tiles. */
@Composable
fun IconChip(icon: ImageVector, modifier: Modifier = Modifier, size: Dp = 36.dp, container: Color = MaterialTheme.colorScheme.primaryContainer, content: Color = MaterialTheme.colorScheme.onPrimaryContainer) {
  androidx.compose.foundation.layout.Box(
    modifier.size(size).clip(RoundedCornerShape(size / 3)).background(container),
    contentAlignment = Alignment.Center,
  ) { Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(size * 0.55f)) }
}

/** Uppercase, letter-spaced section header with a leading icon (Weather's GlassCard header recipe). */
@Composable
fun SectionHeader(
  title: String,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  trailing: @Composable RowScope.() -> Unit = {},
) {
  Row(
    modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
      if (icon != null) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
      }
      Text(
        title.uppercase(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        maxLines = 1,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
      )
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End, content = trailing)
  }
}

/**
 * Dominant colour of an artwork image, used to tint hero headers and the Now Playing backdrop.
 * Null while loading, when there is no artwork or if decoding fails.
 */
@Composable
fun rememberArtworkTint(url: String?): Color? {
  val context = LocalContext.current
  var tint by remember(url) { mutableStateOf<Color?>(null) }
  LaunchedEffect(url) {
    if (url == null) { tint = null; return@LaunchedEffect }
    tint = withContext(Dispatchers.IO) {
      runCatching {
        val request = ImageRequest.Builder(context).data(url).allowHardware(false).size(96).build()
        val result = SingletonImageLoader.get(context).execute(request) as? SuccessResult ?: return@runCatching null
        val bmp: Bitmap = result.image.toBitmap()
        val p = Palette.from(bmp).maximumColorCount(12).generate()
        val rgb = p.getVibrantColor(p.getMutedColor(p.getDominantColor(0)))
        if (rgb == 0) null else Color(rgb)
      }.getOrNull()
    }
  }
  return tint
}

/** Vertical gradient from an artwork tint (or the theme primary) fading to the page background. */
@Composable
fun tintedBackdrop(tint: Color?): Brush {
  val base = tint ?: MaterialTheme.colorScheme.primary
  val bg = MaterialTheme.colorScheme.background
  return Brush.verticalGradient(listOf(base.copy(alpha = 0.45f), base.copy(alpha = 0.15f), bg))
}
