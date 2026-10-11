@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package xyz.pepe.glacierclient.musicapp.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.VolumeUp
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwipeLeft
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.WavingHand
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import xyz.pepe.glacierclient.musicapp.BuildConfig
import xyz.pepe.glacierclient.musicapp.R
import xyz.pepe.glacierclient.musicapp.di.AppContainer
import xyz.pepe.glacierclient.musicapp.domain.model.AppSettings
import xyz.pepe.glacierclient.musicapp.domain.model.Source
import xyz.pepe.glacierclient.musicapp.ui.components.BadgeShape
import xyz.pepe.glacierclient.musicapp.ui.components.CheckSwitch
import xyz.pepe.glacierclient.musicapp.ui.components.ConnectedChoiceRow
import xyz.pepe.glacierclient.musicapp.ui.components.LiveLogo
import xyz.pepe.glacierclient.musicapp.ui.components.PageScaffold
import xyz.pepe.glacierclient.musicapp.ui.components.SectionTitle
import xyz.pepe.glacierclient.musicapp.ui.components.SegmentedGroup
import xyz.pepe.glacierclient.musicapp.ui.components.ShapeBadge
import xyz.pepe.glacierclient.musicapp.ui.components.rememberHaptics
import xyz.pepe.glacierclient.musicapp.ui.components.rowIconColors

private fun sourceLabel(s: Source) = when (s) {
  Source.YOUTUBE_MUSIC -> "YouTube Music"
  Source.AUDIUS -> "Audius"
  Source.INTERNET_ARCHIVE -> "Internet Archive"
  Source.CCMIXTER -> "ccMixter"
  Source.RADIO_BROWSER -> "Radio Browser"
  Source.LOCAL_DOWNLOAD -> "Downloads"
}

private fun sourceHint(s: Source) = when (s) {
  Source.YOUTUBE_MUSIC -> "Full tracks, unofficial API (default)"
  Source.AUDIUS -> "Full tracks, free and open"
  Source.INTERNET_ARCHIVE -> "Full tracks, independent/Creative Commons catalog"
  Source.CCMIXTER -> "Full tracks, Creative Commons remix community"
  Source.RADIO_BROWSER -> "Live internet radio stations"
  Source.LOCAL_DOWNLOAD -> "Downloaded tracks"
}

private typealias HapticsT = xyz.pepe.glacierclient.musicapp.ui.components.Haptics
private typealias Shapes = androidx.compose.material3.ListItemShapes

/** One switch row with the standard icon badge; keeps every page's rows identical. */
@Composable
private fun ToggleRow(
  shapes: Shapes,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  colorIndex: Int,
  title: String,
  hint: String,
  checked: Boolean,
  haptics: HapticsT,
  enabled: Boolean = true,
  onChange: (Boolean) -> Unit,
) {
  SegmentedListItem(
    onClick = { if (enabled) onChange(!checked) }, shapes = shapes,
    leadingContent = { ShapeBadge(icon, shape = BadgeShape.CIRCLE, size = 40.dp, container = rowIconColors(colorIndex).first, content = rowIconColors(colorIndex).second) },
    content = { Text(title) },
    supportingContent = { Text(hint) },
    trailingContent = { CheckSwitch(checked, onChange, haptics, enabled = enabled) },
  )
}

/** Label above a chip row, same spacing everywhere. */
@Composable
private fun ChoiceLabel(text: String) {
  Text(text, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
}

/**
 * A row on a settings page, sorted A-Z by [title] with its siblings. Either a segmented-list row
 * ([grouped]; neighbours merge into one rounded group so shapes stay right after sorting) or a
 * free-standing [block] (choice chips, sliders, text fields).
 */
private class SetRow(
  val title: String,
  val grouped: (@Composable (Shapes, Int) -> Unit)? = null,
  val block: (@Composable () -> Unit)? = null,
)

private fun groupedRow(title: String, content: @Composable (Shapes, Int) -> Unit) = SetRow(title, grouped = content)
private fun blockRow(title: String, content: @Composable () -> Unit) = SetRow(title, block = content)

/** Emits [rows] sorted A-Z by visible title (case-insensitive). */
private fun LazyListScope.sortedRows(rows: List<SetRow>) {
  val sorted = rows.sortedBy { it.title.lowercase() }
  var i = 0
  while (i < sorted.size) {
    val block = sorted[i].block
    if (block != null) {
      item { block() }
      i++
    } else {
      var j = i
      while (j < sorted.size && sorted[j].block == null) j++
      val run = sorted.subList(i, j)
      item { SegmentedGroup(run.size) { k, shapes -> run[k].grouped?.invoke(shapes, k) } }
      i = j
    }
  }
}

/** Titled sections, sorted A-Z by title, each with its rows sorted A-Z. */
private fun LazyListScope.sortedSections(sections: List<Pair<String, List<SetRow>>>) {
  sections.sortedBy { it.first.lowercase() }.forEach { (title, rows) ->
    item { SectionTitle(title) }
    sortedRows(rows)
  }
}

private data class MainRow(val icon: androidx.compose.ui.graphics.vector.ImageVector, val title: String, val hint: String, val page: SettingsPage)

private fun pageIcon(page: SettingsPage) = when (page) {
  SettingsPage.PLAYBACK -> Icons.Rounded.PlayCircle
  SettingsPage.EQUALIZER -> Icons.Rounded.GraphicEq
  SettingsPage.GESTURES -> Icons.Rounded.TouchApp
  SettingsPage.SOURCES -> Icons.Rounded.Hub
  SettingsPage.DOWNLOADS -> Icons.Rounded.Download
  SettingsPage.APPEARANCE -> Icons.Rounded.PhoneAndroid
  SettingsPage.HOME -> Icons.Rounded.Home
  SettingsPage.PRIVACY -> Icons.Rounded.Lock
  SettingsPage.ABOUT, SettingsPage.LICENSES -> Icons.Rounded.Info
  SettingsPage.MAIN -> Icons.Rounded.Search
}

@Composable
fun SettingsHost(container: AppContainer, onBack: () -> Unit) {
  val settings by container.settings.settings.collectAsState()
  var page by rememberSaveable { mutableStateOf(SettingsPage.MAIN) }
  // True when the current page was opened from a search result, so Back goes to the main page.
  var fromSearch by rememberSaveable { mutableStateOf(false) }
  var query by rememberSaveable { mutableStateOf("") }
  var showYtLogin by rememberSaveable { mutableStateOf(false) }
  val haptics = rememberHaptics(settings.haptics)
  val goBack: () -> Unit = {
    page = if (fromSearch) SettingsPage.MAIN else when (page) {
      SettingsPage.LICENSES -> SettingsPage.ABOUT
      SettingsPage.EQUALIZER -> SettingsPage.PLAYBACK
      else -> SettingsPage.MAIN
    }
    fromSearch = false
  }
  BackHandler(enabled = page != SettingsPage.MAIN, onBack = goBack)
  val openNested: (SettingsPage) -> Unit = { fromSearch = false; page = it }

  if (showYtLogin) YtLoginDialog(container, onDismiss = { showYtLogin = false })

  when (page) {
    SettingsPage.MAIN -> MainPage(query, { query = it }, onBack) { p -> fromSearch = query.isNotBlank(); page = p }
    SettingsPage.PLAYBACK -> PlaybackPage(settings, container, haptics, onOpenEqualizer = { openNested(SettingsPage.EQUALIZER) }, goBack)
    SettingsPage.EQUALIZER -> EqualizerPage(settings, container, haptics, goBack)
    SettingsPage.GESTURES -> GesturesPage(settings, container, haptics, goBack)
    SettingsPage.SOURCES -> SourcesPage(settings, container, haptics, onOpenLogin = { showYtLogin = true }, goBack)
    SettingsPage.DOWNLOADS -> DownloadsPage(settings, container, haptics, goBack)
    SettingsPage.APPEARANCE -> AppearancePage(settings, container, haptics, goBack)
    SettingsPage.HOME -> HomePage(settings, container, haptics, goBack, onBack)
    SettingsPage.PRIVACY -> PrivacyPage(settings, container, haptics, goBack)
    SettingsPage.ABOUT -> AboutPage(onBack = goBack, onLicenses = { openNested(SettingsPage.LICENSES) })
    SettingsPage.LICENSES -> LicensesPage(goBack)
  }
}

@Composable
private fun MainPage(query: String, onQuery: (String) -> Unit, onBack: () -> Unit, onOpen: (SettingsPage) -> Unit) {
  val rows = listOf(
    MainRow(Icons.Rounded.Info, stringResource(R.string.about), stringResource(R.string.about_tagline), SettingsPage.ABOUT),
    MainRow(Icons.Rounded.PhoneAndroid, stringResource(R.string.settings_appearance), "Navigation bar, colors and haptics", SettingsPage.APPEARANCE),
    MainRow(Icons.Rounded.Download, "Downloads", "Offline songs, storage and Wi-Fi only", SettingsPage.DOWNLOADS),
    MainRow(Icons.Rounded.TouchApp, "Gestures", "Double-tap seek, swipe to skip and shake", SettingsPage.GESTURES),
    MainRow(Icons.Rounded.Home, "Home & library", "Greeting, categories, layout and lyrics", SettingsPage.HOME),
    MainRow(Icons.Rounded.PlayCircle, stringResource(R.string.settings_playback), "Queue defaults, speed, fades, sound and equalizer", SettingsPage.PLAYBACK),
    MainRow(Icons.Rounded.Lock, "Privacy & data", "Private session, history, cache and backup", SettingsPage.PRIVACY),
    MainRow(Icons.Rounded.Hub, stringResource(R.string.settings_sources), "Choose and reorder where music comes from", SettingsPage.SOURCES),
  ).sortedBy { it.title.lowercase() }
  val results = remember(query) { SettingsSearch.filter(query) }
  PageScaffold(title = stringResource(R.string.settings), subtitle = "Playback, sources and look", onBack = onBack) { padding ->
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      item {
        OutlinedTextField(
          value = query, onValueChange = onQuery, singleLine = true,
          placeholder = { Text("Search settings") },
          leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
          trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { onQuery("") }) { Icon(Icons.Rounded.Close, contentDescription = "Clear search") } },
          shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
          modifier = Modifier.fillMaxWidth(),
        )
      }
      if (query.isNotBlank()) {
        if (results.isEmpty()) {
          item {
            Text(
              "No settings match", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
              textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
          }
        } else {
          item {
            SegmentedGroup(results.size) { i, shapes ->
              val r = results[i]
              SegmentedListItem(
                onClick = { onOpen(r.page) }, shapes = shapes,
                leadingContent = { ShapeBadge(pageIcon(r.page), shape = BadgeShape.CIRCLE, size = 40.dp, container = rowIconColors(i).first, content = rowIconColors(i).second) },
                content = { Text(r.title) },
                supportingContent = { Text(r.description + "\n" + r.page.label) },
              )
            }
          }
        }
        return@LazyColumn
      }
      item {
        SegmentedGroup(rows.size) { i, shapes ->
          val r = rows[i]
          SegmentedListItem(
            onClick = { onOpen(r.page) }, shapes = shapes,
            leadingContent = { ShapeBadge(r.icon, shape = BadgeShape.CIRCLE, size = 40.dp, container = rowIconColors(i).first, content = rowIconColors(i).second) },
            content = { Text(r.title) },
            supportingContent = { Text(r.hint) },
          )
        }
      }
    }
  }
}

@Composable
private fun DownloadsPage(s: AppSettings, container: AppContainer, haptics: HapticsT, onBack: () -> Unit) {
  val saved by container.downloads.downloads.collectAsState(initial = emptyList())
  val bytes = remember(saved) { saved.sumOf { it.sizeBytes } }
  val scope = rememberCoroutineScope()
  var confirmDeleteAll by remember { mutableStateOf(false) }
  if (confirmDeleteAll) {
    androidx.compose.material3.AlertDialog(
      onDismissRequest = { confirmDeleteAll = false },
      title = { Text("Delete all downloads?") },
      text = { Text("${saved.size} songs (${formatBytes(bytes)}) will be removed from this device. Your playlists and likes are kept.") },
      confirmButton = { androidx.compose.material3.TextButton(onClick = { scope.launch { container.downloads.deleteAll() }; confirmDeleteAll = false }) { Text("Delete") } },
      dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmDeleteAll = false }) { Text("Cancel") } },
    )
  }
  val rows = listOf(
    groupedRow("Auto-download liked songs") { shapes, i ->
      ToggleRow(shapes, Icons.Rounded.Favorite, i, "Auto-download liked songs", "Every song you like is saved for offline play (respects Wi-Fi only)", s.autoDownloadLiked, haptics) { v ->
        container.settings.update { it.copy(autoDownloadLiked = v) }
      }
    },
    groupedRow("Delete all downloads") { shapes, _ ->
      SegmentedListItem(
        onClick = { if (saved.isNotEmpty()) confirmDeleteAll = true }, shapes = shapes, enabled = saved.isNotEmpty(),
        leadingContent = { ShapeBadge(Icons.Rounded.DeleteSweep, shape = BadgeShape.CIRCLE, size = 40.dp, container = MaterialTheme.colorScheme.errorContainer, content = MaterialTheme.colorScheme.onErrorContainer) },
        content = { Text("Delete all downloads") },
        supportingContent = { Text("Frees the space; songs can be downloaded again any time") },
      )
    },
    groupedRow("Download on Wi-Fi only") { shapes, i ->
      ToggleRow(shapes, Icons.Rounded.Download, i, "Download on Wi-Fi only", "Saves mobile data. Turn off to download on any connection.", s.wifiOnlyDownloads, haptics) { v ->
        container.settings.update { it.copy(wifiOnlyDownloads = v) }
      }
    },
    groupedRow("Offline mode") { shapes, i ->
      ToggleRow(shapes, Icons.Rounded.CloudOff, i, "Offline mode", "Search and play only downloaded songs; no network is used", s.offlineMode, haptics) { v ->
        container.settings.update { it.copy(offlineMode = v) }
      }
    },
    groupedRow("Storage used") { shapes, i ->
      SegmentedListItem(
        onClick = {}, shapes = shapes,
        leadingContent = { ShapeBadge(Icons.Rounded.Storage, shape = BadgeShape.CIRCLE, size = 40.dp, container = rowIconColors(i).first, content = rowIconColors(i).second) },
        content = { Text("Storage used") },
        supportingContent = { Text(if (saved.isEmpty()) "No downloaded songs yet" else "${saved.size} songs · ${formatBytes(bytes)}") },
      )
    },
  )
  PageScaffold(title = "Downloads", subtitle = "Offline songs and storage", onBack = onBack) { padding ->
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      sortedRows(rows)
    }
  }
}

private fun formatBytes(b: Long): String = when {
  b >= 1_000_000_000 -> "%.1f GB".format(b / 1e9)
  b >= 1_000_000 -> "%.1f MB".format(b / 1e6)
  else -> "${b / 1000} KB"
}

@Composable
private fun PlaybackPage(s: AppSettings, container: AppContainer, haptics: HapticsT, onOpenEqualizer: () -> Unit, onBack: () -> Unit) {
  val efficiency = listOf(
    groupedRow("Battery saver") { shapes, _ ->
      ToggleRow(shapes, Icons.Rounded.BatterySaver, 1, "Battery saver", "Updates the progress bar once a second and prepares fewer upcoming tracks at a time", s.batterySaver, haptics) { v ->
        container.settings.update { it.copy(batterySaver = v) }
      }
    },
  )
  val sleep = listOf(
    blockRow("Sleep timer fade-out") {
      Column {
        ChoiceLabel("Sleep timer fade-out")
        ConnectedChoiceRow(
          listOf(5, 10, 20, 30), s.sleepFadeSeconds, { "${it}s" },
          { v -> container.settings.update { it.copy(sleepFadeSeconds = v) } }, haptics,
          modifier = Modifier.padding(horizontal = 16.dp),
        )
        Text(
          "The volume fades over this long before the sleep timer pauses playback.",
          style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
      }
    },
  )
  val sound = listOf(
    groupedRow("Equalizer") { shapes, i ->
      SegmentedListItem(
        onClick = onOpenEqualizer, shapes = shapes,
        leadingContent = { ShapeBadge(Icons.Rounded.GraphicEq, shape = BadgeShape.CIRCLE, size = 40.dp, container = rowIconColors(i).first, content = rowIconColors(i).second) },
        content = { Text("Equalizer") },
        supportingContent = { Text(if (s.eqEnabled) "On · presets, bass and treble" else "Off · presets, bass and treble") },
      )
    },
    groupedRow("Fade in on play") { shapes, i ->
      ToggleRow(shapes, Icons.Rounded.Tune, i, "Fade in on play", "Ramps volume up over the first 0.6 s of every track", s.fadeInOnPlay, haptics) { v ->
        container.settings.update { it.copy(fadeInOnPlay = v) }
      }
    },
    groupedRow("Fade out at track end") { shapes, i ->
      ToggleRow(shapes, Icons.Rounded.Tune, i, "Fade out at track end", "Ramps volume down over the last 0.6 s of every track", s.fadeOutOnEnd, haptics) { v ->
        container.settings.update { it.copy(fadeOutOnEnd = v) }
      }
    },
    groupedRow("Loudness boost") { shapes, i ->
      ToggleRow(shapes, Icons.Rounded.VolumeUp, i, "Loudness boost", "Raises quiet tracks so volume feels more even", s.loudnessBoost, haptics) { v ->
        container.settings.update { it.copy(loudnessBoost = v) }
      }
    },
    groupedRow("Pause when headphones disconnect") { shapes, i ->
      ToggleRow(shapes, Icons.Rounded.Headphones, i, "Pause when headphones disconnect", "Pause playback when headphones are unplugged or Bluetooth drops", s.pauseOnUnplug, haptics) { v ->
        container.settings.update { it.copy(pauseOnUnplug = v) }
      }
    },
    groupedRow("Skip silence") { shapes, i ->
      ToggleRow(shapes, Icons.Rounded.FastForward, i, "Skip silence", "Skips silent stretches inside tracks", s.skipSilence, haptics) { v ->
        container.settings.update { it.copy(skipSilence = v) }
      }
    },
  )
  val queue = listOf(
    blockRow("Default repeat mode") {
      Column {
        ChoiceLabel("Default repeat mode")
        ConnectedChoiceRow(
          xyz.pepe.glacierclient.musicapp.domain.model.RepeatMode.entries, s.defaultRepeat,
          { when (it) {
            xyz.pepe.glacierclient.musicapp.domain.model.RepeatMode.OFF -> "Off"
            xyz.pepe.glacierclient.musicapp.domain.model.RepeatMode.ALL -> "All"
            xyz.pepe.glacierclient.musicapp.domain.model.RepeatMode.ONE -> "One"
          } },
          { v -> container.settings.update { it.copy(defaultRepeat = v) } }, haptics,
          modifier = Modifier.padding(horizontal = 16.dp),
        )
      }
    },
    blockRow("Default speed (applies to each new queue)") {
      Column {
        ChoiceLabel("Default speed (applies to each new queue)")
        ConnectedChoiceRow(
          listOf(0.75f, 1f, 1.25f, 1.5f, 2f), s.defaultSpeed, { "${it}x" },
          { v -> container.settings.update { it.copy(defaultSpeed = v) } }, haptics,
          modifier = Modifier.padding(horizontal = 16.dp),
        )
      }
    },
    groupedRow("Resume playback on launch") { shapes, i ->
      ToggleRow(shapes, Icons.Rounded.PlayCircle, i, "Resume playback on launch", "Reopen the last queue and position; tap play to continue", s.resumePlayback, haptics) { v ->
        container.settings.update { it.copy(resumePlayback = v) }
      }
    },
    groupedRow("Shuffle by default") { shapes, i ->
      ToggleRow(shapes, Icons.Rounded.Shuffle, i, "Shuffle by default", "New queues start shuffled", s.shuffleOnPlay, haptics) { v ->
        container.settings.update { it.copy(shuffleOnPlay = v) }
      }
    },
  )
  PageScaffold(title = stringResource(R.string.settings_playback), subtitle = "Queue defaults, fades and sound", onBack = onBack) { padding ->
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      sortedSections(listOf("Efficiency" to efficiency, "Sleep timer" to sleep, "Sound" to sound, "When a queue starts" to queue))
    }
  }
}

@Composable
private fun GesturesPage(s: AppSettings, container: AppContainer, haptics: HapticsT, onBack: () -> Unit) {
  val rows = buildList {
    add(groupedRow("Double-tap to seek") { shapes, i ->
      ToggleRow(shapes, Icons.Rounded.TouchApp, i, "Double-tap to seek", "Double-tap the left or right half of the cover art on Now Playing", s.doubleTapSeek, haptics) { v ->
        container.settings.update { it.copy(doubleTapSeek = v) }
      }
    })
    add(groupedRow("Shake to skip") { shapes, i ->
      ToggleRow(shapes, Icons.Rounded.Vibration, i, "Shake to skip", "Shake the phone to skip to the next track while music is playing", s.shakeToSkip, haptics) { v ->
        container.settings.update { it.copy(shakeToSkip = v) }
      }
    })
    add(groupedRow("Swipe mini player to skip") { shapes, i ->
      ToggleRow(shapes, Icons.Rounded.SwipeLeft, i, "Swipe mini player to skip", "Swipe left for next, right for previous", s.swipeMiniPlayerToSkip, haptics) { v ->
        container.settings.update { it.copy(swipeMiniPlayerToSkip = v) }
      }
    })
    if (s.doubleTapSeek) add(blockRow("Double-tap seek amount") {
      Column {
        ChoiceLabel("Double-tap seek amount")
        ConnectedChoiceRow(
          listOf(5, 10, 15, 30), s.doubleTapSeekSeconds, { "${it}s" },
          { v -> container.settings.update { it.copy(doubleTapSeekSeconds = v) } }, haptics,
          modifier = Modifier.padding(horizontal = 16.dp),
        )
      }
    })
  }
  PageScaffold(title = "Gestures", subtitle = "Taps, swipes and shakes", onBack = onBack) { padding ->
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      sortedRows(rows)
    }
  }
}

@Composable
private fun EqualizerPage(s: AppSettings, container: AppContainer, haptics: HapticsT, onBack: () -> Unit) {
  val eq by container.player.eq.collectAsState()
  val rows = buildList {
    add(blockRow("Bass boost") {
      Column {
        androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Bass boost")
          Text(if (s.bassBoost == 0) "Off" else "${s.bassBoost}")
        }
        androidx.compose.material3.Slider(
          value = s.bassBoost.toFloat(), onValueChange = { v -> container.settings.update { it.copy(bassBoost = v.toInt()) } },
          valueRange = 0f..10f, steps = 9,
          modifier = Modifier.padding(horizontal = 16.dp),
        )
      }
    })
    if (eq.supported) {
      add(groupedRow("Equalizer") { shapes, i ->
        ToggleRow(shapes, Icons.Rounded.GraphicEq, i, "Equalizer", "Uses the device's built-in audio effect", eq.enabled, haptics) { v -> container.player.setEqEnabled(v) }
      })
      if (eq.presets.isNotEmpty()) add(blockRow("Equalizer preset") {
        Column {
          ChoiceLabel("Equalizer preset (picking one turns the equalizer on)")
          ConnectedChoiceRow(
            eq.presets.indices.toList(), eq.preset, { eq.presets.getOrElse(it) { "?" } },
            { i -> container.player.setEqPreset(i) }, haptics,
            modifier = Modifier.padding(horizontal = 16.dp),
          )
        }
      })
      add(blockRow("Treble boost") {
        Column {
          androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Treble boost")
            Text(if (s.trebleBoost == 0) "Off" else "${s.trebleBoost}")
          }
          androidx.compose.material3.Slider(
            value = s.trebleBoost.toFloat(), onValueChange = { v -> container.settings.update { it.copy(trebleBoost = v.toInt()) } },
            valueRange = 0f..10f, steps = 9,
            modifier = Modifier.padding(horizontal = 16.dp),
          )
          Text(
            "Raises the highest bands on top of the preset, even when the equalizer is off.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
          )
        }
      })
    }
  }
  PageScaffold(title = "Equalizer", subtitle = "Presets, bass and treble", onBack = onBack) { padding ->
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      if (!eq.supported) {
        item {
          Text(
            "This device doesn't expose a system equalizer effect, so presets and treble boost are unavailable.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
          )
        }
      }
      sortedRows(rows)
    }
  }
}

/** Keeps at least one source on, and keeps the primary pointing at an enabled one. */
private fun toggleSource(st: AppSettings, src: Source, on: Boolean): AppSettings {
  val enabled = if (on) st.enabledSources + src else st.enabledSources - src
  if (enabled.isEmpty()) return st
  val primary = if (st.primarySource in enabled) st.primarySource
  else st.sourcePriority.firstOrNull { it in enabled } ?: enabled.first()
  return st.copy(enabledSources = enabled, primarySource = primary)
}

/** Picking a primary source enables it and moves it to the top of the priority list. */
private fun pickPrimary(st: AppSettings, src: Source): AppSettings =
  st.copy(primarySource = src, enabledSources = st.enabledSources + src, sourcePriority = listOf(src) + st.sourcePriority.filter { it != src })

/**
 * Very customizable: every source can be toggled on/off, dragged to reorder priority, and a primary is picked explicitly.
 * Sections run A-Z; the source list itself is user-ordered (priority), so it is intentionally not alphabetized.
 */
@Composable
private fun SourcesPage(s: AppSettings, container: AppContainer, haptics: HapticsT, onOpenLogin: () -> Unit, onBack: () -> Unit) {
  var order by remember(s.sourcePriority) { mutableStateOf(s.sourcePriority) }
  val listState = rememberLazyListState()
  val reorder = rememberReorderableLazyListState(listState) { from, to ->
    val f = order.indexOfFirst { it.name == from.key }
    val t = order.indexOfFirst { it.name == to.key }
    if (f >= 0 && t >= 0) {
      order = order.toMutableList().apply { add(t, removeAt(f)) }
      haptics.tick()
    }
  }
  val ytAccounts by container.ytAuth.accounts.collectAsState()
  val ytActive by container.ytAuth.activeAccount.collectAsState()
  PageScaffold(title = stringResource(R.string.settings_sources), subtitle = "Where your music comes from", onBack = onBack) { padding ->
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      // --- "Enable and reorder priority" ---
      item { SectionTitle("Enable and reorder priority (drag to reorder)") }
      items(order.size, key = { order[it].name }) { idx ->
        val src = order[idx]
        ReorderableItem(reorder, key = src.name) {
          val handle = Modifier.draggableHandle(onDragStopped = {
            // The top enabled source becomes the primary, so the two controls never disagree.
            container.settings.update { st -> st.copy(sourcePriority = order, primarySource = order.firstOrNull { it in st.enabledSources } ?: st.primarySource) }
          })
          val on = src in s.enabledSources
          SegmentedGroup(1) { _, shapes ->
            SegmentedListItem(
              onClick = { if (!(on && s.enabledSources.size <= 1)) container.settings.update { st -> toggleSource(st, src, !on) } }, shapes = shapes,
              leadingContent = { ShapeBadge(Icons.Rounded.Hub, shape = BadgeShape.CIRCLE, size = 40.dp, container = rowIconColors(idx).first, content = rowIconColors(idx).second) },
              content = { Text(sourceLabel(src)) },
              supportingContent = { Text(if (on && s.enabledSources.size <= 1) "${sourceHint(src)} · at least one source must stay on" else sourceHint(src)) },
              trailingContent = {
                androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                  CheckSwitch(
                    on,
                    { v -> container.settings.update { st -> toggleSource(st, src, v) } },
                    haptics,
                    enabled = !(on && s.enabledSources.size <= 1),
                  )
                  Icon(Icons.Rounded.DragHandle, contentDescription = "Drag to reorder", modifier = handle.padding(start = 12.dp))
                }
              },
            )
          }
        }
      }
      // --- "Primary source" ---
      item { SectionTitle("Primary source: ${sourceLabel(s.primarySource)}") }
      item {
        Column(Modifier.padding(bottom = 8.dp)) {
          ConnectedChoiceRow(
            order, s.primarySource, { sourceLabel(it) },
            { picked -> container.settings.update { pickPrimary(it, picked) } }, haptics,
            modifier = Modifier.padding(horizontal = 4.dp),
          )
        }
      }
      // --- "YouTube Music accounts" ---
      item { SectionTitle("YouTube Music accounts") }
      if (ytAccounts.isNotEmpty()) item {
        SegmentedGroup(ytAccounts.size) { i, shapes ->
          val acc = ytAccounts[i]
          val active = acc.id == ytActive?.id
          SegmentedListItem(
            onClick = { if (!active) container.ytAuth.switchTo(acc.id) }, shapes = shapes,
            leadingContent = {
              if (acc.photoUrl != null) coil3.compose.AsyncImage(model = acc.photoUrl, contentDescription = null, modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape))
              else ShapeBadge(Icons.Rounded.AccountCircle, shape = BadgeShape.CIRCLE, size = 40.dp, container = rowIconColors(i).first, content = rowIconColors(i).second)
            },
            content = { Text(acc.label) },
            supportingContent = { Text(if (active) "Active — requests use this account" else "Tap to switch") },
            trailingContent = { IconButton(onClick = { container.ytAuth.removeAccount(acc.id) }) { Icon(Icons.Rounded.Close, contentDescription = "Remove account") } },
          )
        }
      }
      item {
        SegmentedGroup(1) { _, shapes ->
          SegmentedListItem(
            onClick = onOpenLogin, shapes = shapes,
            leadingContent = { ShapeBadge(Icons.Rounded.Add, shape = BadgeShape.CIRCLE, size = 40.dp, container = rowIconColors(ytAccounts.size).first, content = rowIconColors(ytAccounts.size).second) },
            content = { Text(if (ytAccounts.isEmpty()) "Sign in to YouTube Music" else "Add another account") },
            supportingContent = { Text("Logged-in requests are less likely to be blocked when resolving streams") },
          )
        }
      }
      item {
        Text(
          "YouTube Music uses an unofficial, reverse-engineered API (no account, no key) and can break without notice. The other sources are official free public APIs and act as fallbacks.",
          style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun AppearancePage(s: AppSettings, container: AppContainer, haptics: HapticsT, onBack: () -> Unit) {
  val dynamicColorSupported = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
  val navStyleTitle = stringResource(R.string.nav_style)
  val navLabelsTitle = stringResource(R.string.nav_labels)
  val wallpaperTitle = stringResource(R.string.wallpaper_colors)
  val wallpaperHint = stringResource(R.string.wallpaper_colors_hint)
  val hapticTitle = stringResource(R.string.haptic_feedback)
  val rows = buildList {
    add(groupedRow("Frosted glass") { shapes, i ->
      ToggleRow(shapes, Icons.Rounded.BlurOn, i, "Frosted glass", "Translucent floating nav bar and mini player; off uses solid surfaces", s.glassEffects, haptics) { v ->
        container.settings.update { it.copy(glassEffects = v) }
      }
    })
    add(groupedRow(hapticTitle) { shapes, i ->
      ToggleRow(shapes, Icons.Rounded.Vibration, i, hapticTitle, "Light vibration on switches, tabs and player buttons", s.haptics, haptics) { v ->
        container.settings.update { it.copy(haptics = v) }
      }
    })
    add(blockRow(navStyleTitle) {
      Column {
        ChoiceLabel(navStyleTitle)
        ConnectedChoiceRow(
          xyz.pepe.glacierclient.musicapp.domain.model.NavStyle.entries, s.navStyle,
          { if (it == xyz.pepe.glacierclient.musicapp.domain.model.NavStyle.FLOATING) stringResource(R.string.nav_floating) else stringResource(R.string.nav_classic) },
          { v -> container.settings.update { it.copy(navStyle = v) } }, haptics,
          modifier = Modifier.padding(horizontal = 16.dp),
        )
        Text(
          "Floating is a pill over the content; Classic is the standard bar. Wide screens use a side rail either way.",
          style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
      }
    })
    add(blockRow(navLabelsTitle) {
      Column {
        ChoiceLabel(navLabelsTitle)
        ConnectedChoiceRow(
          xyz.pepe.glacierclient.musicapp.domain.model.NavLabels.entries, s.navLabels,
          { when (it) {
            xyz.pepe.glacierclient.musicapp.domain.model.NavLabels.SELECTED -> stringResource(R.string.labels_selected)
            xyz.pepe.glacierclient.musicapp.domain.model.NavLabels.ALL -> stringResource(R.string.labels_all)
            xyz.pepe.glacierclient.musicapp.domain.model.NavLabels.NONE -> stringResource(R.string.labels_none)
          } },
          { v -> container.settings.update { it.copy(navLabels = v) } }, haptics,
          modifier = Modifier.padding(horizontal = 16.dp),
        )
      }
    })
    if (dynamicColorSupported) add(groupedRow(wallpaperTitle) { shapes, i ->
      ToggleRow(shapes, Icons.Rounded.Palette, i, wallpaperTitle, wallpaperHint, s.wallpaperColors, haptics) { v ->
        container.settings.update { it.copy(wallpaperColors = v) }
      }
    })
  }
  PageScaffold(title = stringResource(R.string.settings_appearance), subtitle = "Navigation bar, colors and haptics", onBack = onBack) { padding ->
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      sortedRows(rows)
    }
  }
}

@Composable
private fun HomePage(s: AppSettings, container: AppContainer, haptics: HapticsT, onBack: () -> Unit, exitSettings: () -> Unit) {
  var name by remember { mutableStateOf(s.displayName) }
  var categoriesText by remember { mutableStateOf(s.categories.joinToString(", ")) }
  val home = buildList {
    add(blockRow("Your name") {
      OutlinedTextField(
        value = name,
        onValueChange = { v -> name = v.take(40); container.settings.update { it.copy(displayName = v.take(40)) } },
        label = { Text("Your name") },
        supportingText = { Text("Shown in the greeting. Empty uses your YouTube Music account name.") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
      )
    })
    add(groupedRow("Greeting") { shapes, i ->
      ToggleRow(shapes, Icons.Rounded.WavingHand, i, "Greeting", "\"Good morning\" header with your name", s.showGreeting, haptics) { v ->
        container.settings.update { it.copy(showGreeting = v) }
      }
    })
    add(groupedRow("Notification button") { shapes, i ->
      ToggleRow(shapes, Icons.Rounded.Notifications, i, "Notification button", "Bell on Home with your in-app notifications", s.showNotificationButton, haptics) { v ->
        container.settings.update { it.copy(showNotificationButton = v) }
      }
    })
    add(groupedRow("Categories") { shapes, i ->
      ToggleRow(shapes, Icons.Rounded.Category, i, "Categories", "Genre chips on Home that switch the song list", s.showCategories, haptics) { v ->
        container.settings.update { it.copy(showCategories = v) }
      }
    })
    add(blockRow("Edit home layout") {
      androidx.compose.material3.TextButton(onClick = {
        xyz.pepe.glacierclient.musicapp.ui.home.HomeEditRequests.requested.value = true
        exitSettings()
      }) { Text("Edit home layout") }
    })
    add(blockRow("Reset home layout") {
      androidx.compose.material3.TextButton(onClick = {
        container.settings.update { it.copy(homeLayout = "") }
      }) { Text("Reset home layout") }
    })
    add(groupedRow("Grid layout") { shapes, i ->
      ToggleRow(shapes, Icons.Rounded.GridView, i, "Grid layout", "Show Home songs as cards instead of a list", s.homeGrid, haptics) { v ->
        container.settings.update { it.copy(homeGrid = v) }
      }
    })
    if (s.showCategories) {
      add(blockRow("Categories (comma-separated)") {
        OutlinedTextField(
          value = categoriesText,
          onValueChange = { v ->
            categoriesText = v
            container.settings.update { it.copy(categories = v.split(",").map { c -> c.trim() }.filter { c -> c.isNotEmpty() }.distinct()) }
          },
          label = { Text("Categories (comma-separated)") },
          supportingText = { Text("Each one searches \"<category> songs\" when tapped. Empty hides the chips.") },
          modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        )
      })
      add(blockRow("Reset categories") {
        androidx.compose.material3.TextButton(onClick = {
          categoriesText = xyz.pepe.glacierclient.musicapp.domain.model.DEFAULT_CATEGORIES.joinToString(", ")
          container.settings.update { it.copy(categories = xyz.pepe.glacierclient.musicapp.domain.model.DEFAULT_CATEGORIES) }
        }) { Text("Reset categories") }
      })
    }
  }
  val lyrics = listOf(
    blockRow("Lyrics sync offset") {
      Column {
        androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Lyrics sync offset")
          Text(if (s.lyricsOffsetMs == 0) "None" else "%+.1f s".format(s.lyricsOffsetMs / 1000f))
        }
        androidx.compose.material3.Slider(
          value = s.lyricsOffsetMs.toFloat(), onValueChange = { v -> container.settings.update { it.copy(lyricsOffsetMs = (Math.round(v / 100f) * 100).coerceIn(-5000, 5000)) } },
          valueRange = -5000f..5000f, steps = 99,
          modifier = Modifier.padding(horizontal = 16.dp),
        )
        Text(
          "Positive highlights lines later, negative earlier. Fixes lyrics that run ahead of or behind the music.",
          style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 16.dp),
        )
      }
    },
    blockRow("Lyrics text size") {
      Column {
        androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Lyrics text size")
          Text("${(s.lyricsFontScale * 100).toInt()}%")
        }
        androidx.compose.material3.Slider(
          value = s.lyricsFontScale, onValueChange = { v -> container.settings.update { it.copy(lyricsFontScale = v.coerceIn(0.7f, 1.6f)) } },
          valueRange = 0.7f..1.6f, steps = 5,
          modifier = Modifier.padding(horizontal = 16.dp),
        )
        Text(
          "Also adjustable with the A buttons in the lyrics view on Now Playing.",
          style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 16.dp),
        )
      }
    },
  )
  PageScaffold(title = "Home & library", subtitle = "Greeting, categories, layout and lyrics", onBack = onBack) { padding ->
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      sortedSections(listOf("Home screen" to home, "Lyrics" to lyrics))
    }
  }
}

@Composable
private fun PrivacyPage(s: AppSettings, container: AppContainer, haptics: HapticsT, onBack: () -> Unit) {
  val context = androidx.compose.ui.platform.LocalContext.current
  var showImport by remember { mutableStateOf(false) }
  var importText by remember { mutableStateOf("") }
  var importError by remember { mutableStateOf(false) }
  var confirmReset by remember { mutableStateOf(false) }
  if (showImport) {
    androidx.compose.material3.AlertDialog(
      onDismissRequest = { showImport = false },
      title = { Text("Import settings") },
      text = {
        OutlinedTextField(
          value = importText, onValueChange = { importText = it; importError = false }, singleLine = false,
          label = { Text("Paste exported settings JSON") }, isError = importError,
          supportingText = if (importError) { { Text("Couldn't read that — make sure it's the full exported text") } } else null,
          modifier = Modifier.fillMaxWidth().height(160.dp),
        )
      },
      confirmButton = {
        androidx.compose.material3.TextButton(onClick = {
          if (container.settings.importJson(importText.trim())) { showImport = false; container.notifications.post("Settings imported", "Your settings were restored") }
          else importError = true
        }) { Text("Import") }
      },
      dismissButton = { androidx.compose.material3.TextButton(onClick = { showImport = false }) { Text("Cancel") } },
    )
  }
  if (confirmReset) {
    androidx.compose.material3.AlertDialog(
      onDismissRequest = { confirmReset = false },
      title = { Text("Reset all settings?") },
      text = { Text("Every setting goes back to its default. Your library, playlists, downloads and accounts are kept.") },
      confirmButton = {
        androidx.compose.material3.TextButton(onClick = {
          container.settings.update { AppSettings() }
          container.notifications.post("Settings reset", "Everything is back to defaults")
          confirmReset = false
        }) { Text("Reset") }
      },
      dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
    )
  }
  val backup = listOf(
    groupedRow("Export settings") { shapes, i ->
      SegmentedListItem(
        onClick = {
          val clip = context.getSystemService(android.content.ClipboardManager::class.java)
          clip?.setPrimaryClip(android.content.ClipData.newPlainText("Pepe's Music settings", container.settings.exportJson()))
          container.notifications.post("Settings exported", "Copied to clipboard — paste it into Import on another device")
        },
        shapes = shapes,
        leadingContent = { ShapeBadge(Icons.Rounded.Upload, shape = BadgeShape.CIRCLE, size = 40.dp, container = rowIconColors(i).first, content = rowIconColors(i).second) },
        content = { Text("Export settings") },
        supportingContent = { Text("Copies your settings as text you can back up or move to another device") },
      )
    },
    groupedRow("Import settings") { shapes, i ->
      SegmentedListItem(
        onClick = { importText = ""; importError = false; showImport = true }, shapes = shapes,
        leadingContent = { ShapeBadge(Icons.Rounded.Download, shape = BadgeShape.CIRCLE, size = 40.dp, container = rowIconColors(i).first, content = rowIconColors(i).second) },
        content = { Text("Import settings") },
        supportingContent = { Text("Paste a previously exported settings block") },
      )
    },
    groupedRow("Reset all settings") { shapes, _ ->
      SegmentedListItem(
        onClick = { confirmReset = true }, shapes = shapes,
        leadingContent = { ShapeBadge(Icons.Rounded.RestartAlt, shape = BadgeShape.CIRCLE, size = 40.dp, container = MaterialTheme.colorScheme.errorContainer, content = MaterialTheme.colorScheme.onErrorContainer) },
        content = { Text("Reset all settings") },
        supportingContent = { Text("Puts every setting on every page back to its default") },
      )
    },
  )
  val privacy = listOf(
    groupedRow("Clear play history") { shapes, i ->
      SegmentedListItem(
        onClick = { container.history.clear(); container.notifications.post("History cleared", "Recently Played and Speed dial were emptied") }, shapes = shapes,
        leadingContent = { ShapeBadge(Icons.Rounded.History, shape = BadgeShape.CIRCLE, size = 40.dp, container = rowIconColors(i).first, content = rowIconColors(i).second) },
        content = { Text("Clear play history") },
        supportingContent = { Text("Removes Recently Played and Speed dial data") },
      )
    },
    groupedRow("Clear search history") { shapes, i ->
      SegmentedListItem(
        onClick = { container.searchHistory.clear(); container.notifications.post("Search history cleared", "Recent searches were removed") }, shapes = shapes,
        leadingContent = { ShapeBadge(Icons.Rounded.Search, shape = BadgeShape.CIRCLE, size = 40.dp, container = rowIconColors(i).first, content = rowIconColors(i).second) },
        content = { Text("Clear search history") },
        supportingContent = { Text("Removes recent searches shown in Search") },
      )
    },
    groupedRow("Private session") { shapes, i ->
      ToggleRow(shapes, Icons.Rounded.VisibilityOff, i, "Private session", "Nothing is added to Recently Played, Most Played, search history or your streak while this is on", s.privateSession, haptics) { v ->
        container.settings.update { it.copy(privateSession = v) }
      }
    },
  )
  val storage = listOf(
    groupedRow("Clear cache") { shapes, i ->
      SegmentedListItem(
        onClick = {
          xyz.pepe.glacierclient.musicapp.ui.util.clearAppCache(context)
          container.notifications.post("Cache cleared", "Removed temporary image and export files")
        },
        shapes = shapes,
        leadingContent = { ShapeBadge(Icons.Rounded.CleaningServices, shape = BadgeShape.CIRCLE, size = 40.dp, container = rowIconColors(i).first, content = rowIconColors(i).second) },
        content = { Text("Clear cache") },
        supportingContent = { Text("Frees space used by cached artwork and exported files — doesn't affect playlists, likes or downloads") },
      )
    },
  )
  PageScaffold(title = "Privacy & data", subtitle = "Private session, history, cache and backup", onBack = onBack) { padding ->
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      sortedSections(listOf("Backup and reset" to backup, "Privacy" to privacy, "Storage" to storage))
    }
  }
}

@Composable
private fun AboutPage(onBack: () -> Unit, onLicenses: () -> Unit) {
  val licensesTitle = stringResource(R.string.open_source_licenses)
  val versionTitle = stringResource(R.string.version)
  var showShortcuts by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
  val shortcutsTitle = "Keyboard shortcuts"
  if (showShortcuts) {
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = { showShortcuts = false }) {
      LazyColumn(contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text(shortcutsTitle, style = MaterialTheme.typography.titleLarge) }
        items(xyz.pepe.glacierclient.musicapp.ui.nav.KEYBOARD_SHORTCUTS.size) { i ->
          val (keys, action) = xyz.pepe.glacierclient.musicapp.ui.nav.KEYBOARD_SHORTCUTS[i]
          androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(keys, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(0.42f))
            Text(action, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.58f))
          }
        }
      }
    }
  }
  val rows = listOf(
    groupedRow(shortcutsTitle) { shapes, _ -> SegmentedListItem(onClick = { showShortcuts = true }, shapes = shapes, content = { Text(shortcutsTitle) }, supportingContent = { Text("Space, arrows, L, S, R, Ctrl+1 to 4") }) },
    groupedRow(licensesTitle) { shapes, _ -> SegmentedListItem(onClick = onLicenses, shapes = shapes, content = { Text(licensesTitle) }) },
    groupedRow(versionTitle) { shapes, _ -> SegmentedListItem(onClick = {}, shapes = shapes, content = { Text(versionTitle) }, supportingContent = { Text(BuildConfig.VERSION_NAME) }) },
  )
  PageScaffold(title = stringResource(R.string.about), subtitle = "Version, privacy and licenses", onBack = onBack) { padding ->
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      item {
        Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
          LiveLogo(size = 112.dp, isPlaying = true)
          Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMediumEmphasized)
          Text(stringResource(R.string.about_tagline), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
      }
      sortedRows(rows)
      item { Text(stringResource(R.string.about_privacy), color = MaterialTheme.colorScheme.onSurfaceVariant) }
      item { xyz.pepe.glacierclient.ecosystem.PepeAppsSection() }
    }
  }
}

// Only what the app actually ships with — never list a dependency or API it doesn't call.
private val licenses = listOf(
  "AndroidX (Compose, Material 3, Lifecycle, Media3)" to "Apache License 2.0",
  "Kotlin and kotlinx.coroutines" to "Apache License 2.0",
  "Retrofit and OkHttp (Square)" to "Apache License 2.0",
  "Moshi (Square)" to "Apache License 2.0",
  "Coil 3" to "Apache License 2.0",
  "Compose Reorderable (Calvin Liu)" to "Apache License 2.0",
  "YouTube Music (unofficial innertube API)" to "Not an official API — see Sources",
  "Audius" to "Free/open API",
  "Internet Archive" to "Free/open API",
  "ccMixter" to "Creative Commons community",
  "Radio Browser" to "Public radio directory",
  "Font Awesome Free \"music\" glyph" to "CC BY 4.0",
).sortedBy { it.first.lowercase() }

@Composable
private fun LicensesPage(onBack: () -> Unit) {
  PageScaffold(title = stringResource(R.string.open_source_licenses), subtitle = "Libraries used by this app", onBack = onBack) { padding ->
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp)) {
      item {
        SegmentedGroup(licenses.size) { i, shapes ->
          SegmentedListItem(onClick = {}, shapes = shapes, content = { Text(licenses[i].first) }, supportingContent = { Text(licenses[i].second) })
        }
      }
    }
  }
}
