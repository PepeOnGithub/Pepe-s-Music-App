package xyz.pepe.glacierclient.musicapp.ui.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import xyz.pepe.glacierclient.musicapp.domain.model.Track

/**
 * Search's query/results/view-mode, held once in [xyz.pepe.glacierclient.musicapp.di.AppContainer]
 * instead of `remember`ed inside `SearchScreen`. Switching tabs replaces `SearchScreen` in the
 * composition entirely (the tab `when` isn't a `NavHost` that keeps hidden destinations alive), so
 * plain `remember`/`rememberSaveable` state was wiped every time you left and came back — this
 * survives because it lives on the container, not on the composable.
 */
class SearchViewState {
  var query by mutableStateOf("")
  var results by mutableStateOf<List<Track>>(emptyList())
  var loading by mutableStateOf(false)
  var searched by mutableStateOf(false)
  var grid by mutableStateOf(false)
  /** 0 = Songs, 1 = Artists, 2 = Albums — which results view is showing. */
  var tab by mutableStateOf(0)
}
