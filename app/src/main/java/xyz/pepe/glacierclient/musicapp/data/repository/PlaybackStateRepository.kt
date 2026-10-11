package xyz.pepe.glacierclient.musicapp.data.repository

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import xyz.pepe.glacierclient.musicapp.domain.model.Source
import xyz.pepe.glacierclient.musicapp.domain.model.Track

data class SavedPlaybackState(val queue: List<Track>, val index: Int, val positionMs: Long)

/** Persists the last queue/index/position so playback can resume after the app is killed. */
class PlaybackStateRepository(private val prefs: SharedPreferences) {
  fun save(queue: List<Track>, index: Int, positionMs: Long) {
    val arr = JSONArray()
    queue.forEach { t ->
      arr.put(
        JSONObject()
          .put("source", t.source.name)
          .put("sourceId", t.sourceId)
          .put("title", t.title)
          .put("artist", t.artist)
          .put("artworkUrl", t.artworkUrl ?: "")
          .put("durationMs", t.durationMs ?: 0)
          // Audius/Internet Archive/ccMixter/radio have no resolve step — CatalogRepository just
          // returns the track's own URL — so dropping it here made every resumed non-YouTube queue
          // fail with "no stream available". YouTube URLs expire and are always re-resolved.
          .put("streamUrl", t.streamUrl?.takeIf { t.source != Source.YOUTUBE_MUSIC && it.startsWith("http") } ?: "")
          .put("shareUrl", t.shareUrl ?: ""),
      )
    }
    prefs.edit()
      .putString(KEY_QUEUE, arr.toString())
      .putInt(KEY_INDEX, index)
      .putLong(KEY_POSITION, positionMs)
      .apply()
  }

  fun load(): SavedPlaybackState? {
    val json = prefs.getString(KEY_QUEUE, null) ?: return null
    val queue = runCatching {
      val arr = JSONArray(json)
      (0 until arr.length()).mapNotNull { i -> trackFrom(arr.optJSONObject(i)) }
    }.getOrDefault(emptyList())
    if (queue.isEmpty()) return null
    val index = prefs.getInt(KEY_INDEX, 0).coerceIn(0, queue.lastIndex)
    val positionMs = prefs.getLong(KEY_POSITION, 0L)
    return SavedPlaybackState(queue, index, positionMs)
  }

  fun clear() { prefs.edit().clear().apply() }

  private fun trackFrom(o: JSONObject?): Track? {
    o ?: return null
    return Track(
      source = runCatching { Source.valueOf(o.optString("source")) }.getOrElse { return null },
      sourceId = o.optString("sourceId"),
      title = o.optString("title"),
      artist = o.optString("artist"),
      artworkUrl = o.optString("artworkUrl").takeIf { it.isNotEmpty() },
      durationMs = o.optLong("durationMs").takeIf { it > 0 },
      streamUrl = o.optString("streamUrl").takeIf { it.isNotEmpty() },
      isPreview = false,
      shareUrl = o.optString("shareUrl").takeIf { it.isNotEmpty() },
    )
  }

  private companion object {
    const val KEY_QUEUE = "saved_queue_json"
    const val KEY_INDEX = "saved_index"
    const val KEY_POSITION = "saved_position_ms"
  }
}
