package xyz.pepe.glacierclient.musicapp.data.repository

import android.content.SharedPreferences
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import xyz.pepe.glacierclient.musicapp.domain.util.ListeningStats

/** Real time spent listening per calendar day (device-local), kept for the last 60 days. */
class ListeningStatsRepository(private val prefs: SharedPreferences) {
  private val _days = MutableStateFlow(load())
  /** Day key (yyyy-MM-dd) to milliseconds actually played. */
  val days: StateFlow<Map<String, Long>> = _days

  fun add(ms: Long) {
    if (ms <= 0) return
    val today = LocalDate.now()
    val key = today.toString()
    val next = ListeningStats.prune(_days.value + (key to ((_days.value[key] ?: 0L) + ms)), today)
    _days.value = next
    prefs.edit().putString(KEY, JSONObject(next as Map<*, *>).toString()).apply()
  }

  private fun load(): Map<String, Long> = runCatching {
    val o = JSONObject(prefs.getString(KEY, null) ?: "{}")
    o.keys().asSequence().associateWith { o.getLong(it) }
  }.getOrDefault(emptyMap())

  private companion object { const val KEY = "days_json" }
}
