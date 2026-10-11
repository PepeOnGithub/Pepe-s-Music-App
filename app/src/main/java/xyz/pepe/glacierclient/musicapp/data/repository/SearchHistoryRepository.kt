package xyz.pepe.glacierclient.musicapp.data.repository

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray

/** Recent search query strings, most recent first — the "Recent searches" chips YouTube Music
 * shows above an empty search box. Real past queries only, never suggested/fabricated ones. */
class SearchHistoryRepository(private val prefs: SharedPreferences) {
  private val _recent = MutableStateFlow(load())
  val recent: StateFlow<List<String>> = _recent

  fun record(query: String) {
    val q = query.trim()
    if (q.length < 2) return
    val next = (listOf(q) + _recent.value.filterNot { it.equals(q, ignoreCase = true) }).take(MAX)
    _recent.value = next
    save(next)
  }

  fun remove(query: String) {
    val next = _recent.value.filterNot { it == query }
    _recent.value = next
    save(next)
  }

  fun clear() { _recent.value = emptyList(); save(emptyList()) }

  private fun load(): List<String> = runCatching {
    val arr = JSONArray(prefs.getString(KEY, "[]"))
    List(arr.length()) { arr.getString(it) }
  }.getOrDefault(emptyList())

  private fun save(list: List<String>) {
    prefs.edit().putString(KEY, JSONArray(list).toString()).apply()
  }

  private companion object { const val KEY = "queries"; const val MAX = 15 }
}
