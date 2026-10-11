package xyz.pepe.glacierclient.musicapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * Hero header for artist and playlist pages: blurred artwork (or the artwork tint) washing into the
 * page background, a centred cover with a soft shadow, then title and subtitle. [cover] lets a
 * playlist draw its mosaic; by default the artwork (or a music-note placeholder) is shown.
 */
@Composable
fun HeroHeader(
  title: String,
  subtitle: String,
  artworkUrl: String?,
  modifier: Modifier = Modifier,
  circular: Boolean = false,
  cover: (@Composable () -> Unit)? = null,
) {
  val tint = rememberArtworkTint(artworkUrl)
  val bg = MaterialTheme.colorScheme.background
  val base = tint ?: MaterialTheme.colorScheme.primary
  Box(modifier.fillMaxWidth()) {
    Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(base.copy(alpha = 0.40f), bg))))
    if (artworkUrl != null) {
      AsyncImage(
        model = artworkUrl, contentDescription = null, contentScale = ContentScale.Crop,
        modifier = Modifier.matchParentSize().alpha(0.35f).blur(28.dp),
      )
      Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, bg))))
    }
    Column(
      Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      val coverShape = if (circular) CircleShape else RoundedCornerShape(24.dp)
      Box(
        Modifier.size(144.dp).shadow(20.dp, coverShape, ambientColor = base, spotColor = base).clip(coverShape)
          .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
      ) {
        when {
          cover != null -> cover()
          artworkUrl != null -> AsyncImage(model = artworkUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
          else -> Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(56.dp))
        }
      }
      Text(
        title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center,
        maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 16.dp),
      )
      Text(
        subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.padding(top = 2.dp),
      )
    }
  }
}
