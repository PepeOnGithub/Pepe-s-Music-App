package xyz.pepe.glacierclient.musicapp.data.repository

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import xyz.pepe.glacierclient.musicapp.domain.model.Track

data class Bookmark(val track: Track, val positionMs: Long, val createdAtMs: Long)

/** Named/quick position bookmarks — "remember this exact spot in this track", distinct from
 * Resume Playback (which only remembers where the whole queue was on app restart). */
class BookmarkRepository(private val prefs: SharedPreferences) {
  private val _bookmarks = MutableStateFlow(load())
  val bookmarks: StateFlow<List<Bookmark>> = _bookmarks

  fun add(track: Track, positionMs: Long) {
    val next = listOf(Bookmark(track, positionMs, System.currentTimeMillis())) + _bookmarks.value
    _bookmarks.value = next
    save(next)
  }

  fun remove(bookmark: Bookmark) {
    val next = _bookmarks.value.filterNot { it.track.key == bookmark.track.key && it.positionMs == bookmark.positionMs }
    _bookmarks.value = next
    save(next)
  }

  private fun load(): List<Bookmark> {
    val json = prefs.getString(KEY, null) ?: return emptyList()
    return runCatching {
      val arr = JSONArray(json)
      (0 until arr.length()).mapNotNull { i ->
        val o = arr.optJSONObject(i) ?: return@mapNotNull null
        val track = FavoritesRepository.trackFrom(o.optJSONObject("track")) ?: return@mapNotNull null
        Bookmark(track, o.optLong("positionMs"), o.optLong("createdAtMs"))
      }
    }.getOrDefault(emptyList())
  }

  private fun save(list: List<Bookmark>) {
    val arr = JSONArray()
    list.forEach { b ->
      val t = b.track
      val trackJson = JSONObject()
        .put("source", t.source.name).put("sourceId", t.sourceId).put("title", t.title).put("artist", t.artist)
        .put("artworkUrl", t.artworkUrl ?: "").put("durationMs", t.durationMs ?: 0)
        .put("streamUrl", t.streamUrl ?: "").put("shareUrl", t.shareUrl ?: "")
      arr.put(JSONObject().put("track", trackJson).put("positionMs", b.positionMs).put("createdAtMs", b.createdAtMs))
    }
    prefs.edit().putString(KEY, arr.toString()).apply()
  }

  private companion object { const val KEY = "bookmarks_json" }
}
