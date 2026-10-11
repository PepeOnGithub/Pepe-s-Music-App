@file:OptIn(kotlinx.coroutines.FlowPreview::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package xyz.pepe.glacierclient.musicapp.ui.radio

import androidx.compose.foundation.background
import xyz.pepe.glacierclient.musicapp.ui.components.GlassCard
import xyz.pepe.glacierclient.musicapp.ui.components.SectionHeader
import xyz.pepe.glacierclient.musicapp.ui.components.glassSurface
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import xyz.pepe.glacierclient.musicapp.R
import xyz.pepe.glacierclient.musicapp.di.AppContainer
import xyz.pepe.glacierclient.musicapp.domain.model.Station
import xyz.pepe.glacierclient.musicapp.ui.components.EmptyState

/** Live internet radio via Radio Browser — same rounded-pill search pattern as the Search tab. */
@Composable
fun RadioScreen(container: AppContainer, openSettings: () -> Unit) {
  var query by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
  var stations by remember { mutableStateOf<List<Station>>(emptyList()) }
  var loading by remember { mutableStateOf(true) }
  var errored by remember { mutableStateOf(false) }
  val queryChannel = remember { Channel<String>(Channel.CONFLATED) }
  val playerState by container.player.state.collectAsState()

  LaunchedEffect(Unit) {
    loading = true
    errored = false
    stations = runCatching { withContext(Dispatchers.IO) { container.radioBrowser.topStations() } }.getOrElse { errored = true; emptyList() }
    loading = false
    if (query.isNotBlank()) queryChannel.trySend(query)
  }

  LaunchedEffect(Unit) {
    queryChannel.consumeAsFlow()
      .debounce(400)
      .distinctUntilChanged()
      .collect { q ->
        loading = true
        errored = false
        stations = runCatching {
          withContext(Dispatchers.IO) { if (q.isBlank()) container.radioBrowser.topStations() else container.radioBrowser.search(q) }
        }.getOrElse { errored = true; emptyList() }
        loading = false
      }
  }

  Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
    Surface(
      shape = MaterialTheme.shapes.extraLarge, color = androidx.compose.ui.graphics.Color.Transparent,
      modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp).fillMaxWidth().heightIn(min = 56.dp)
        .glassSurface(MaterialTheme.shapes.extraLarge),
    ) {
      Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        BasicTextField(
          value = query,
          onValueChange = { query = it; queryChannel.trySend(it) },
          singleLine = true,
          textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
          cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
          keyboardActions = KeyboardActions(onSearch = {}),
          modifier = Modifier.weight(1f),
          decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
              if (query.isEmpty()) Text(stringResource(R.string.tab_radio), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
              inner()
            }
          },
        )
        if (query.isNotEmpty()) {
          IconButton(onClick = { query = ""; queryChannel.trySend("") }) { Icon(Icons.Rounded.Close, contentDescription = "Clear") }
        } else {
          IconButton(onClick = openSettings) { Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.settings)) }
        }
      }
    }

    Box(Modifier.weight(1f).fillMaxWidth()) {
      when {
        loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { androidx.compose.material3.LoadingIndicator(Modifier.size(64.dp)) }
        errored -> EmptyState(
          icon = Icons.Rounded.Radio, title = "Couldn't reach Radio Browser",
          body = "All mirrors failed to respond — check your connection and try again.",
          modifier = Modifier.align(Alignment.Center),
        )
        stations.isEmpty() -> EmptyState(
          icon = Icons.Rounded.Radio, title = stringResource(R.string.no_results),
          body = "No stations matched — try a different search.",
          modifier = Modifier.align(Alignment.Center),
        )
        else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)) {
          item { SectionHeader(if (query.isBlank()) "Top stations" else "Results", icon = Icons.Rounded.Radio) }
          items(stations.size, key = { stations[it].uuid }) { i ->
            val station = stations[i]
            val isCurrent = playerState.current?.key == station.toTrack().key
            StationRow(station = station, playing = isCurrent && playerState.isPlaying, onClick = { container.player.play(listOf(station.toTrack()), 0) })
          }
        }
      }
    }
  }
}

@Composable
private fun StationRow(station: Station, playing: Boolean, onClick: () -> Unit) {
  GlassCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp), shape = RoundedCornerShape(18.dp), onClick = onClick) {
    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
      if (station.faviconUrl != null) {
        AsyncImage(model = station.faviconUrl, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
      } else {
        Box(
          Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh),
          contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Radio, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
      }
      Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
        Text(station.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
        val subtitle = listOfNotNull(station.country, station.tags.firstOrNull()).joinToString(" • ")
        if (subtitle.isNotEmpty()) Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      if (playing) Icon(Icons.Rounded.SignalCellularAlt, contentDescription = "Now playing", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
    }
  }
}
