package xyz.pepe.glacierclient.musicapp.data.repository

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import xyz.pepe.glacierclient.musicapp.domain.model.Track

/** Recently-played tracks, most recent first, deduped, capped. No local library scan exists
 * (this app streams; it doesn't index device files), so this is Library's "Recently Played". */
class HistoryRepository(private val prefs: SharedPreferences) {
  private val _recent = MutableStateFlow(load())
  val recent: StateFlow<List<Track>> = _recent

  private val _counts = MutableStateFlow(loadCounts())
  /** Real play counts, keyed by track key — only for tracks still present in [recent] (the
   * recency list is capped, so a count for a track that has aged out of it is dropped too;
   * "Most Played" is therefore a most-played-among-recent view, not all-time, which is stated
   * honestly in the Library section header rather than implied to be more than it is. */
  val counts: StateFlow<Map<String, Int>> = _counts

  // Lifetime totals, independent of the capped/evictable recent list above — a real running count
  // and estimated minutes listened, never reset by "Clear play history" (that only clears what's
  // shown in Recently Played/Most Played, not your actual listening totals).
  private val _totalPlays = MutableStateFlow(prefs.getInt(TOTAL_PLAYS_KEY, 0))
  val totalPlays: StateFlow<Int> = _totalPlays
  private val _totalMs = MutableStateFlow(prefs.getLong(TOTAL_MS_KEY, 0L))
  val totalMs: StateFlow<Long> = _totalMs

  fun record(track: Track) {
    val next = (listOf(track) + _recent.value.filterNot { it.key == track.key }).take(MAX)
    _recent.value = next
    save(next)
    val nextCounts = _counts.value.toMutableMap()
    nextCounts[track.key] = (nextCounts[track.key] ?: 0) + 1
    val dropped = nextCounts.keys - next.map { it.key }.toSet()
    dropped.forEach { nextCounts.remove(it) }
    _counts.value = nextCounts
    saveCounts(nextCounts)
    _totalPlays.value += 1
    _totalMs.value += track.durationMs ?: 0L
    prefs.edit().putInt(TOTAL_PLAYS_KEY, _totalPlays.value).putLong(TOTAL_MS_KEY, _totalMs.value).apply()
  }

  fun clear() { _recent.value = emptyList(); _counts.value = emptyMap(); save(emptyList()); saveCounts(emptyMap()) }

  private fun loadCounts(): Map<String, Int> = runCatching {
    val o = JSONObject(prefs.getString(COUNTS_KEY, null) ?: "{}")
    o.keys().asSequence().associateWith { o.getInt(it) }
  }.getOrDefault(emptyMap())

  private fun saveCounts(counts: Map<String, Int>) {
    val o = JSONObject()
    counts.forEach { (k, v) -> o.put(k, v) }
    prefs.edit().putString(COUNTS_KEY, o.toString()).apply()
  }

  private fun load(): List<Track> {
    val json = prefs.getString(KEY, null) ?: return emptyList()
    return runCatching {
      val arr = JSONArray(json)
      (0 until arr.length()).mapNotNull { i -> FavoritesRepository.trackFrom(arr.optJSONObject(i)) }
    }.getOrDefault(emptyList())
  }

  private fun save(tracks: List<Track>) {
    val arr = JSONArray()
    tracks.forEach { t ->
      arr.put(
        JSONObject()
          .put("source", t.source.name)
          .put("sourceId", t.sourceId)
          .put("title", t.title)
          .put("artist", t.artist)
          .put("artworkUrl", t.artworkUrl ?: "")
          .put("durationMs", t.durationMs ?: 0)
          .put("streamUrl", t.streamUrl ?: "")
          .put("shareUrl", t.shareUrl ?: ""),
      )
    }
    prefs.edit().putString(KEY, arr.toString()).apply()
  }

  private companion object {
    const val KEY = "recent_json"; const val COUNTS_KEY = "counts_json"; const val MAX = 50
    const val TOTAL_PLAYS_KEY = "total_plays"; const val TOTAL_MS_KEY = "total_ms"
  }
}
