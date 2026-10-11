package xyz.pepe.glacierclient.musicapp.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.pepe.glacierclient.musicapp.R
import xyz.pepe.glacierclient.musicapp.ui.theme.BrandGreen
import xyz.pepe.glacierclient.musicapp.ui.theme.BrandGreenLight

/** Animated equalizer glyph. Bars bounce only while [isPlaying] is true; otherwise they rest flat. */
@Composable
fun LiveLogo(modifier: Modifier = Modifier, size: Dp = 96.dp, isPlaying: Boolean = true) {
  val transition = rememberInfiniteTransition(label = "equalizer")
  val bar0 by transition.animateFloat(0.25f, if (isPlaying) 1f else 0.25f, infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse), label = "bar0")
  val bar1 by transition.animateFloat(0.25f, if (isPlaying) 1f else 0.25f, infiniteRepeatable(tween(510, easing = LinearEasing), RepeatMode.Reverse), label = "bar1")
  val bar2 by transition.animateFloat(0.25f, if (isPlaying) 1f else 0.25f, infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse), label = "bar2")
  val bar3 by transition.animateFloat(0.25f, if (isPlaying) 1f else 0.25f, infiniteRepeatable(tween(690, easing = LinearEasing), RepeatMode.Reverse), label = "bar3")
  val heights = listOf(bar0, bar1, bar2, bar3)
  val desc = stringResource(R.string.live_logo_description)
  Canvas(modifier.size(size).semantics { contentDescription = desc }) {
    val barWidth = this.size.width / 8f
    val gap = barWidth
    val maxHeight = this.size.height * 0.8f
    val baseY = this.size.height * 0.9f
    val brush = Brush.verticalGradient(listOf(BrandGreenLight, BrandGreen))
    heights.forEachIndexed { i, h ->
      val x = gap + i * (barWidth + gap)
      val barHeight = maxHeight * h
      drawLine(
        brush = brush,
        start = Offset(x, baseY),
        end = Offset(x, baseY - barHeight),
        strokeWidth = barWidth,
        cap = StrokeCap.Round,
      )
    }
  }
}
