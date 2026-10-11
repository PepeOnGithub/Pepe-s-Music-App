package xyz.pepe.glacierclient.musicapp.ui.nav

import android.content.Context
import android.media.AudioManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import xyz.pepe.glacierclient.musicapp.di.AppContainer

/** One row in the Keyboard shortcuts sheet: the keys and what they do. */
val KEYBOARD_SHORTCUTS: List<Pair<String, String>> = listOf(
  "Space" to "Play / pause",
  "Left / Right" to "Seek 5 s (Now Playing, or hold Alt)",
  "Shift + Left / Right" to "Seek 30 s",
  "Ctrl + Left / Right" to "Previous / next song",
  "Up / Down" to "Volume (Now Playing, or hold Alt)",
  "M" to "Mute",
  "L" to "Like the current song",
  "S" to "Shuffle",
  "R" to "Cycle repeat",
  "Q" to "Open Now Playing and queue",
  "/ or Ctrl + F" to "Search",
  "Ctrl + 1 to 4" to "Home, Search, Library, Radio",
  "Ctrl + ," to "Settings",
  "Esc" to "Back",
)

/**
 * In-app keyboard shortcuts. Called from a root `onKeyEvent`, so it only sees keys no focused
 * control (text field, button) already consumed. Plain arrows are left to focus navigation unless
 * Now Playing is open or Alt is held.
 */
fun handleShortcut(event: KeyEvent, container: AppContainer, context: Context): Boolean {
  if (event.type != KeyEventType.KeyDown) return false
  val ctrl = event.isCtrlPressed
  val shift = event.isShiftPressed
  val arrowsActive = container.nowPlayingVisible || event.isAltPressed
  val p = container.player
  val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
  return when (event.key) {
    Key.Spacebar -> { p.togglePlayPause(); true }
    Key.DirectionLeft -> when {
      ctrl -> { p.previous(); true }
      arrowsActive -> { p.seekBy(if (shift) -30_000L else -5_000L); true }
      else -> false
    }
    Key.DirectionRight -> when {
      ctrl -> { p.next(); true }
      arrowsActive -> { p.seekBy(if (shift) 30_000L else 5_000L); true }
      else -> false
    }
    Key.DirectionUp -> if (arrowsActive) { audio?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI); true } else false
    Key.DirectionDown -> if (arrowsActive) { audio?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI); true } else false
    Key.M -> { audio?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI); true }
    Key.L -> {
      p.state.value.current?.let { container.favorites.toggle(it) }
      true
    }
    Key.S -> { p.toggleShuffle(); true }
    Key.R -> { p.cycleRepeat(); true }
    Key.Q -> { container.uiCommands.tryEmit("nowplaying"); true }
    Key.Slash -> { container.uiCommands.tryEmit("search"); true }
    Key.F -> if (ctrl) { container.uiCommands.tryEmit("search"); true } else false
    Key.One -> if (ctrl) { container.requestedTab.value = 0; true } else false
    Key.Two -> if (ctrl) { container.requestedTab.value = 1; true } else false
    Key.Three -> if (ctrl) { container.requestedTab.value = 2; true } else false
    Key.Four -> if (ctrl) { container.requestedTab.value = 3; true } else false
    Key.Comma -> if (ctrl) { container.uiCommands.tryEmit("settings"); true } else false
    Key.Escape -> { container.uiCommands.tryEmit("back"); true }
    else -> false
  }
}
