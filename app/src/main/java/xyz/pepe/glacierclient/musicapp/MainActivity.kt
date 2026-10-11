package xyz.pepe.glacierclient.musicapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import xyz.pepe.glacierclient.musicapp.ui.nav.handleShortcut
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import xyz.pepe.glacierclient.ecosystem.PepeEcosystem
import xyz.pepe.glacierclient.musicapp.ui.nav.AppRoot
import xyz.pepe.glacierclient.musicapp.ui.theme.PepesMusicTheme

class MainActivity : ComponentActivity() {
  private val container get() = (application as MusicApplication).container

  // Music's ecosystem agent: `pepe://music/play?q=…` from any sibling app plays that search.
  private val agent = PepeEcosystem.Agent { action ->
    when (action) {
      is PepeEcosystem.PlayMusic -> { container.player.playQuery(action.query); true }
      else -> false
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    if (savedInstanceState == null) route(intent) // rotation must not replay the request
    setContent {
      val settings by container.settings.settings.collectAsState()
      PepesMusicTheme(wallpaperColors = settings.wallpaperColors) {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
        androidx.compose.runtime.CompositionLocalProvider(
          xyz.pepe.glacierclient.musicapp.ui.components.LocalGlassEnabled provides settings.glassEffects,
          xyz.pepe.glacierclient.musicapp.ui.components.LocalHapticsEnabled provides settings.haptics,
        ) {
          Box(
            Modifier.fillMaxSize()
              .focusRequester(focusRequester)
              .onKeyEvent { handleShortcut(it, container, this@MainActivity) }
              .focusable(),
          ) { AppRoot(container) }
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    route(intent)
  }

  private fun route(intent: Intent?) {
    // Launcher shortcuts: open the Search tab or the Library (liked songs live there).
    when (intent?.action) {
      "xyz.pepe.glacierclient.musicapp.SHORTCUT_SEARCH" -> { container.requestedTab.value = xyz.pepe.glacierclient.musicapp.ui.nav.Tab.SEARCH.ordinal; return }
      "xyz.pepe.glacierclient.musicapp.SHORTCUT_LIKED" -> { container.requestedTab.value = xyz.pepe.glacierclient.musicapp.ui.nav.Tab.LIBRARY.ordinal; return }
    }
    PepeEcosystem.actionFrom(intent)?.let { agent.handle(it); return }
    // The manifest has always accepted shared text, but nothing read it — sharing a song title or
    // link to Music silently did nothing. Treat it as a search-and-play request.
    if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
      intent.getStringExtra(Intent.EXTRA_TEXT)?.lineSequence()?.firstOrNull { it.isNotBlank() }
        ?.trim()?.take(PepeEcosystem.MAX_QUERY)
        ?.let { agent.handle(PepeEcosystem.PlayMusic(it)) }
    }
  }
}
