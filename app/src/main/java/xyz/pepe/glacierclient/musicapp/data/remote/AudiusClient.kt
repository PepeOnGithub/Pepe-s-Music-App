package xyz.pepe.glacierclient.musicapp.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import xyz.pepe.glacierclient.musicapp.domain.model.Source
import xyz.pepe.glacierclient.musicapp.domain.model.Track

/**
 * Client for Audius, a free/open full-track streaming API. Audius has no single fixed base URL:
 * you discover a list of live hosts and pick one, then use it for every call and rotate on
 * failure — see [resolveHost]/[rotate].
 */
class AudiusClient(private val http: OkHttpClient) {
  @Volatile private var cachedHost: String? = null

  private fun get(url: String): JSONObject {
    val request = Request.Builder().url(url).build()
    http.newCall(request).execute().use { resp ->
      val text = resp.body?.string().orEmpty()
      if (!resp.isSuccessful || text.isBlank()) throw AudiusException("HTTP ${resp.code}")
      return JSONObject(text)
    }
  }

  /** Picks a live host, caching it for reuse. Call [rotate] on failure to force a fresh pick. */
  private fun resolveHost(): String {
    cachedHost?.let { return it }
    val hosts = get("https://api.audius.co").optJSONArray("data")
      ?: throw AudiusException("no hosts")
    if (hosts.length() == 0) throw AudiusException("no hosts")
    val host = hosts.getString((0 until hosts.length()).random())
    cachedHost = host
    return host
  }

  fun rotate() { cachedHost = null }

  fun search(query: String): List<Track> {
    val host = resolveHost()
    val url = "$host/v1/tracks/search?query=${java.net.URLEncoder.encode(query, "UTF-8")}&app_name=$APP_NAME"
    val data = try {
      get(url).optJSONArray("data")
    } catch (e: Exception) {
      rotate()
      get("${resolveHost()}/v1/tracks/search?query=${java.net.URLEncoder.encode(query, "UTF-8")}&app_name=$APP_NAME").optJSONArray("data")
    } ?: return emptyList()
    return (0 until data.length()).mapNotNull { i -> trackFrom(data.optJSONObject(i), host) }
  }

  fun trending(): List<Track> {
    val host = resolveHost()
    val data = get("$host/v1/tracks/trending?app_name=$APP_NAME").optJSONArray("data") ?: return emptyList()
    return (0 until data.length()).mapNotNull { i -> trackFrom(data.optJSONObject(i), host) }
  }

  private fun trackFrom(o: JSONObject?, host: String): Track? {
    o ?: return null
    val id = o.optString("id").takeIf { it.isNotEmpty() } ?: return null
    val artwork = o.optJSONObject("artwork")
    return Track(
      source = Source.AUDIUS,
      sourceId = id,
      title = o.optString("title"),
      artist = o.optJSONObject("user")?.optString("name").orEmpty(),
      artworkUrl = artwork?.optString("1000x1000")?.takeIf { it.isNotEmpty() }
        ?: artwork?.optString("480x480")?.takeIf { it.isNotEmpty() }
        ?: artwork?.optString("150x150"),
      durationMs = o.optLong("duration") * 1000,
      streamUrl = "$host/v1/tracks/$id/stream?app_name=$APP_NAME",
      isPreview = false,
      shareUrl = o.optString("permalink").takeIf { it.isNotEmpty() }?.let { "https://audius.co$it" },
    )
  }

  private companion object { const val APP_NAME = "PepesMusic" }
}

class AudiusException(message: String) : Exception(message)
