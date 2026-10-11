package xyz.pepe.glacierclient.musicapp.data.repository

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import xyz.pepe.glacierclient.musicapp.domain.model.Source
import xyz.pepe.glacierclient.musicapp.domain.model.Track

/** Liked tracks, persisted as JSON so Library can render them without re-fetching from a source. */
class FavoritesRepository(private val prefs: SharedPreferences) {
  private val _liked = MutableStateFlow(load())
  val liked: StateFlow<List<Track>> = _liked

  fun isLiked(track: Track): Boolean = _liked.value.any { it.key == track.key }

  fun toggle(track: Track) {
    val current = _liked.value
    val next = if (current.any { it.key == track.key }) current.filterNot { it.key == track.key } else listOf(track) + current
    _liked.value = next
    save(next)
  }

  private fun load(): List<Track> {
    val json = prefs.getString(KEY, null) ?: return emptyList()
    return runCatching {
      val arr = JSONArray(json)
      (0 until arr.length()).mapNotNull { i -> trackFrom(arr.optJSONObject(i)) }
    }.getOrDefault(emptyList())
  }

  companion object {
    const val KEY = "liked_tracks_json"
    internal fun trackFrom(o: JSONObject?): Track? {
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

}
