package xyz.pepe.glacierclient.musicapp.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch
import xyz.pepe.glacierclient.musicapp.di.AppContainer
import xyz.pepe.glacierclient.musicapp.domain.model.Track
import xyz.pepe.glacierclient.musicapp.domain.util.ListeningStats
import xyz.pepe.glacierclient.musicapp.ui.components.GlassCard
import xyz.pepe.glacierclient.musicapp.ui.components.IconChip
import xyz.pepe.glacierclient.musicapp.ui.components.SectionHeader

/** A 2x2 mosaic of up to four album covers (one cover fills the tile; none shows the queue icon). */
@Composable
fun PlaylistCover(urls: List<String>, size: Dp? = 52.dp, modifier: Modifier = Modifier, cornerRadius: Dp = 10.dp) {
  val shape = RoundedCornerShape(cornerRadius)
  Box((if (size != null) modifier.size(size) else modifier).clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
    when {
      urls.isEmpty() -> Icon(Icons.Rounded.QueueMusic, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
      urls.size < 4 -> AsyncImage(model = urls.first(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
      else -> Column(Modifier.fillMaxSize()) {
        for (r in 0..1) Row(Modifier.weight(1f).fillMaxWidth()) {
          for (c in 0..1) AsyncImage(model = urls[r * 2 + c], contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.weight(1f).fillMaxHeight())
        }
      }
    }
  }
}

/** Seven bars of minutes actually listened per day, plus the week's total. */
@Composable
fun WeeklyListeningChart(container: AppContainer) {
  val days by container.listeningStats.days.collectAsState()
  val week = remember(days) { ListeningStats.lastDays(days, LocalDate.now(), 7) }
  val total = week.sumOf { it.second }
  if (total <= 0L) return
  val max = week.maxOf { it.second }.coerceAtLeast(1L)
  val minutes = ListeningStats.minutes(total)
  GlassCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
      Text(
        "This week: " + if (minutes < 60) "$minutes min" else "${minutes / 60} h ${minutes % 60} min",
        style = MaterialTheme.typography.titleSmall,
      )
      Row(Modifier.fillMaxWidth().height(72.dp).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
        week.forEach { (date, ms) ->
          Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
            Box(
              Modifier.fillMaxWidth().height((44.dp * (ms.toFloat() / max)).coerceAtLeast(3.dp))
                .clip(RoundedCornerShape(6.dp)).background(if (ms > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest),
            )
            Text(date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }
    }
  }
}

/** The five most-played artists, derived from real play counts; tap one to open their page. */
@Composable
fun TopArtistsSection(tracks: List<Track>, counts: Map<String, Int>, onOpenArtist: (String) -> Unit) {
  val top = remember(tracks, counts) { ListeningStats.topArtists(tracks, counts, 5) }
  if (top.isEmpty()) return
  Column {
    SectionHeader("Top artists", icon = Icons.Rounded.Person)
    GlassCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
      top.forEach { (artist, plays) ->
        Row(Modifier.fillMaxWidth().clickable(onClickLabel = "Open artist page") { onOpenArtist(artist) }.heightIn(min = 52.dp).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
          IconChip(Icons.Rounded.Person, size = 32.dp)
          Text(artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
          Text("$plays plays", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    }
  }
}

/** Paste an exported JSON playlist or an M3U list; a new playlist is created from it. */
@Composable
fun ImportPlaylistDialog(container: AppContainer, onDismiss: () -> Unit) {
  var text by remember { mutableStateOf("") }
  var error by remember { mutableStateOf<String?>(null) }
  var busy by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()
  AlertDialog(
    onDismissRequest = { if (!busy) onDismiss() },
    title = { Text("Import playlist") },
    text = {
      OutlinedTextField(
        value = text, onValueChange = { text = it; error = null }, singleLine = false, isError = error != null,
        label = { Text("Paste playlist JSON or M3U") },
        supportingText = { Text(error ?: if (busy) "Importing, matching songs..." else "Works with text exported from this app's playlist menu.") },
        modifier = Modifier.fillMaxWidth().height(200.dp),
      )
    },
    confirmButton = {
      TextButton(enabled = !busy && text.isNotBlank(), onClick = {
        busy = true
        scope.launch {
          val body = text.trim()
          val result = if (body.startsWith("{")) container.playlists.importJson(body)
          else container.playlists.importM3u("Imported playlist", body) { q ->
            runCatching { container.catalog.search(q, container.settings.settings.value).firstOrNull() }.getOrNull()
          }
          busy = false
          if (result == null || result.second == 0) error = "Couldn't find any songs in that text"
          else {
            container.notifications.post("Playlist imported", "${result.second} songs added")
            onDismiss()
          }
        }
      }) { Text("Import") }
    },
    dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancel") } },
  )
}
