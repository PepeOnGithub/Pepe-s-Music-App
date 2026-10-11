@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package xyz.pepe.glacierclient.musicapp.ui.nav

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected as semSelected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import xyz.pepe.glacierclient.musicapp.ui.components.rememberHaptics
import xyz.pepe.glacierclient.musicapp.R
import xyz.pepe.glacierclient.musicapp.di.AppContainer
import xyz.pepe.glacierclient.musicapp.domain.model.NavLabels
import xyz.pepe.glacierclient.musicapp.domain.model.NavStyle
import xyz.pepe.glacierclient.musicapp.ui.components.GlassBorder
import xyz.pepe.glacierclient.musicapp.ui.components.glass
import xyz.pepe.glacierclient.musicapp.ui.home.HomeScreen
import xyz.pepe.glacierclient.musicapp.ui.library.LibraryScreen
import xyz.pepe.glacierclient.musicapp.ui.player.ClassicMiniPlayer
import xyz.pepe.glacierclient.musicapp.ui.player.FloatingMiniPlayer
import xyz.pepe.glacierclient.musicapp.ui.player.NowPlayingScreen
import xyz.pepe.glacierclient.musicapp.ui.radio.RadioScreen
import xyz.pepe.glacierclient.musicapp.ui.search.SearchScreen
import xyz.pepe.glacierclient.musicapp.ui.settings.SettingsHost

enum class Tab(val label: Int, val icon: ImageVector) {
  HOME(R.string.tab_home, Icons.Rounded.Home),
  SEARCH(R.string.tab_search, Icons.Rounded.Search),
  LIBRARY(R.string.tab_library, Icons.Rounded.LibraryMusic),
  RADIO(R.string.tab_radio, Icons.Rounded.Radio),
}

/** Single-activity root: bottom bar on phones, rail on screens >= 600 dp wide. */
@Composable
fun AppRoot(container: AppContainer) {
  var tabIndex by rememberSaveable { mutableIntStateOf(Tab.HOME.ordinal) }
  var showSettings by rememberSaveable { mutableStateOf(false) }
  var showNowPlaying by rememberSaveable { mutableStateOf(false) }
  // Artist page overlay (opened from search, library, or Now Playing); back closes just this page.
  var artistName by rememberSaveable { mutableStateOf<String?>(null) }
  // Launcher shortcuts ask for a tab through the container; consume the request and clear it.
  LaunchedEffect(Unit) {
    container.requestedTab.collect { requested ->
      if (requested != null) {
        tabIndex = requested.coerceIn(0, Tab.entries.lastIndex)
        showSettings = false; showNowPlaying = false; artistName = null
        container.requestedTab.value = null
      }
    }
  }
  // Settings > "Edit home layout" asks for the Home tab; HomeScreen then consumes the request.
  LaunchedEffect(Unit) {
    xyz.pepe.glacierclient.musicapp.ui.home.HomeEditRequests.requested.collect { r ->
      if (r) { tabIndex = Tab.HOME.ordinal; showSettings = false; showNowPlaying = false; artistName = null }
    }
  }
  // Prompted once per app launch when signed out of YouTube Music, since a logged-in session
  // makes stream resolution far more reliable. Dismissible (the page's back arrow just closes
  // it) rather than a hard gate — the app still works fully signed out, via Audius.
  val ytAccounts by container.ytAuth.accounts.collectAsState()
  var loginPromptDismissed by rememberSaveable { mutableStateOf(false) }
  val showLoginPrompt = ytAccounts.isEmpty() && !loginPromptDismissed
  val wide = LocalConfiguration.current.screenWidthDp >= 600

  BackHandler(enabled = showSettings || showNowPlaying) { showSettings = false; showNowPlaying = false }
  BackHandler(enabled = artistName != null) { artistName = null }

  if (showLoginPrompt) {
    xyz.pepe.glacierclient.musicapp.ui.settings.YtLoginDialog(container = container, onDismiss = { loginPromptDismissed = true })
  }
  androidx.compose.runtime.SideEffect { container.nowPlayingVisible = showNowPlaying && artistName == null && !showSettings }
  LaunchedEffect(Unit) {
    container.uiCommands.collect { cmd ->
      when (cmd) {
        "settings" -> showSettings = true
        "nowplaying" -> if (container.player.state.value.current != null) { showNowPlaying = !showNowPlaying; showSettings = false; artistName = null }
        "search" -> { tabIndex = Tab.SEARCH.ordinal; showSettings = false; showNowPlaying = false; artistName = null }
        "back" -> when {
          artistName != null -> artistName = null
          showSettings || showNowPlaying -> { showSettings = false; showNowPlaying = false }
        }
      }
    }
  }
  // 0 tabs, 1 settings, 2 now playing, 3 artist: faded/scaled through, Now Playing slides up.
  val screen = when { artistName != null -> 3; showSettings -> 1; showNowPlaying -> 2; else -> 0 }
  val reduced = xyz.pepe.glacierclient.musicapp.ui.components.rememberReducedMotion()
  AnimatedContent(
    targetState = screen,
    transitionSpec = {
      if (reduced) EnterTransition.None togetherWith ExitTransition.None
      else if (targetState == 2) {
        (slideInVertically(spring(stiffness = Spring.StiffnessMediumLow)) { it / 6 } + fadeIn(tween(260))) togetherWith fadeOut(tween(160))
      } else if (initialState == 2) {
        fadeIn(tween(220)) togetherWith (slideOutVertically(tween(260)) { it / 4 } + fadeOut(tween(220)))
      } else {
        (fadeIn(tween(240, delayMillis = 60)) + scaleIn(tween(280), initialScale = 0.96f)) togetherWith fadeOut(tween(120))
      }
    },
    label = "screens",
  ) { target ->
    when (target) {
      3 -> xyz.pepe.glacierclient.musicapp.ui.artist.ArtistScreen(container, artistName ?: "", onBack = { artistName = null })
      1 -> SettingsHost(container = container, onBack = { showSettings = false })
      2 -> NowPlayingScreen(container = container, onBack = { showNowPlaying = false }, onOpenArtist = { artistName = it })
      else -> MainShell(
        container = container, tabIndex = tabIndex, onTab = { tabIndex = it }, wide = wide,
        openSettings = { showSettings = true }, openNowPlaying = { showNowPlaying = true }, openArtist = { artistName = it },
      )
    }
  }
}

@Composable
private fun MainShell(
  container: AppContainer,
  tabIndex: Int,
  onTab: (Int) -> Unit,
  wide: Boolean,
  openSettings: () -> Unit,
  openNowPlaying: () -> Unit,
  openArtist: (String) -> Unit,
) {
  // Only whether a track exists is read here — never the live position, which changes 4x/second
  // and would otherwise force this whole screen (nav bar + current tab) to recompose on every
  // tick. The mini players collect the full, fast-changing state themselves.
  val hasTrack by container.player.hasTrack.collectAsState()
  val settings by container.settings.settings.collectAsState()
  val floating = !wide && settings.navStyle == NavStyle.FLOATING

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(Unit) {
    container.player.errorEvents.collect { message -> snackbarHostState.showSnackbar(message) }
  }
  LaunchedEffect(Unit) {
    container.messages.collect { m ->
      snackbarHostState.currentSnackbarData?.dismiss()
      val result = snackbarHostState.showSnackbar(m.text, actionLabel = if (m.onUndo != null) "Undo" else null, duration = androidx.compose.material3.SnackbarDuration.Short)
      if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) m.onUndo?.invoke()
    }
  }

  val haptics = rememberHaptics(settings.haptics)
  val selectTab: (Int) -> Unit = { i -> if (i != tabIndex) { haptics.tick(); onTab(i) } }
  // Height of the floating mini player + nav pill (incl. their own padding), measured so tab
  // content is never hidden behind them regardless of font scale, labels or whether a track plays.
  val density = LocalDensity.current
  var floatingBarHeight by remember { mutableStateOf(0.dp) }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    bottomBar = {
      if (!wide && !floating) {
        Column {
          if (hasTrack) {
            ClassicMiniPlayer(container = container, onOpen = { openNowPlaying() })
          }
          ShortNavigationBar {
            Tab.entries.forEachIndexed { i, tab ->
              val label = stringResource(tab.label)
              val showLabel = shouldShowLabel(settings.navLabels, i == tabIndex)
              ShortNavigationBarItem(
                selected = tabIndex == i,
                onClick = { selectTab(i) },
                icon = { Icon(tab.icon, contentDescription = if (showLabel) null else label) },
                label = if (showLabel) { { Text(label) } } else null,
              )
            }
          }
        }
      }
    },
  ) { padding ->
    Row(Modifier.fillMaxSize().padding(padding)) {
      if (wide) {
        NavigationRail {
          Tab.entries.forEachIndexed { i, tab ->
            val label = stringResource(tab.label)
            val showLabel = shouldShowLabel(settings.navLabels, i == tabIndex)
            NavigationRailItem(
              selected = tabIndex == i,
              onClick = { selectTab(i) },
              icon = { Icon(tab.icon, contentDescription = if (showLabel) null else label) },
              label = if (showLabel) { { Text(label) } } else null,
            )
          }
        }
      }
      Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
          Box(Modifier.weight(1f).fillMaxWidth().padding(bottom = if (floating) floatingBarHeight else 0.dp)) {
            when (Tab.entries[tabIndex]) {
              Tab.HOME -> HomeScreen(container, openSettings)
              Tab.SEARCH -> SearchScreen(container, openSettings, onOpenArtist = openArtist)
              Tab.LIBRARY -> LibraryScreen(container, openSettings, onOpenArtist = openArtist)
              Tab.RADIO -> RadioScreen(container, openSettings)
            }
          }
          // Wide layouts use the rail, so the mini player docks flush at the bottom of the content.
          if (wide && hasTrack) ClassicMiniPlayer(container = container, onOpen = { openNowPlaying() })
        }
        if (floating) {
          // Scaffold's padding above already keeps this clear of the system navigation bar.
          Column(
            Modifier.align(Alignment.BottomCenter)
              .onSizeChanged { floatingBarHeight = with(density) { it.height.toDp() } }
              .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            if (hasTrack) {
              FloatingMiniPlayer(container = container, onOpen = { openNowPlaying() }, glassEffects = settings.glassEffects)
            }
            FloatingNavBar(selected = tabIndex, labels = settings.navLabels, glassEffects = settings.glassEffects, onSelect = selectTab)
          }
        }
      }
    }
  }
}

/** SELECTED shows the label only on the active tab, ALL always, NONE never. Shared by every nav style. */
private fun shouldShowLabel(labels: NavLabels, selected: Boolean) = when (labels) {
  NavLabels.ALL -> true
  NavLabels.SELECTED -> selected
  NavLabels.NONE -> false
}

/** iOS-style floating pill nav bar, ported from Pepe's Clock's AppRoot. */
@Composable
private fun FloatingNavBar(selected: Int, labels: NavLabels, onSelect: (Int) -> Unit, glassEffects: Boolean = true, modifier: Modifier = Modifier) {
  val shape = RoundedCornerShape(36.dp)
  Surface(
    modifier = if (glassEffects) modifier.fillMaxWidth().glass(shape) else modifier.fillMaxWidth(),
    shape = shape,
    color = if (glassEffects) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh,
    border = if (glassEffects) GlassBorder else null,
    shadowElevation = 12.dp,
  ) {
    Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      Tab.entries.forEachIndexed { i, tab ->
        val on = i == selected
        val showLabel = shouldShowLabel(labels, on)
        val label = stringResource(tab.label)
        Surface(
          onClick = { onSelect(i) },
          shape = RoundedCornerShape(30.dp),
          color = animateColorAsState(if (on) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent, label = "pill").value,
          modifier = Modifier.weight(1f).defaultMinSize(minHeight = 56.dp)
            .semantics { role = Role.Tab; semSelected = on; contentDescription = label },
        ) {
          Column(
            Modifier.height(60.dp).padding(horizontal = 8.dp).animateContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            Icon(tab.icon, contentDescription = null, tint = if (on) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
            if (showLabel) Text(
              label, style = MaterialTheme.typography.labelSmall, maxLines = 1,
              color = if (on) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}
