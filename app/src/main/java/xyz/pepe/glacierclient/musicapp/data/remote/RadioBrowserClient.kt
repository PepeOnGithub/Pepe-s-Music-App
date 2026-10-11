package xyz.pepe.glacierclient.musicapp.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import xyz.pepe.glacierclient.musicapp.domain.model.Station

/**
 * Client for Radio Browser (radio-browser.info), a free/open directory of live internet radio
 * stream URLs — no account or key needed. Like Audius, there's no single fixed base host: it's a
 * pool of community-run mirrors, so a live one is picked and rotated on failure (same pattern as
 * [AudiusClient]).
 */
class RadioBrowserClient(private val http: OkHttpClient) {
  @Volatile private var cachedHost: String? = null

  private fun get(url: String): Any {
    val request = Request.Builder().url(url).header("User-Agent", "PepesMusic/1.0").build()
    http.newCall(request).execute().use { resp ->
      val text = resp.body?.string().orEmpty()
      if (!resp.isSuccessful || text.isBlank()) throw RadioBrowserException("HTTP ${resp.code}")
      return if (text.trimStart().startsWith("[")) JSONArray(text) else JSONObject(text)
    }
  }

  private fun resolveHost(): String {
    cachedHost?.let { return it }
    val host = MIRRORS.random()
    cachedHost = host
    return host
  }

  fun rotate() { cachedHost = null }

  /** Top stations by vote count — used as the Radio tab's default listing. */
  fun topStations(limit: Int = 40): List<Station> = withRotation { host ->
    get("$host/json/stations/topvote/$limit") as? JSONArray
  }

  fun search(query: String, limit: Int = 40): List<Station> = withRotation { host ->
    val q = java.net.URLEncoder.encode(query, "UTF-8")
    get("$host/json/stations/search?name=$q&limit=$limit&hidebroken=true") as? JSONArray
  }

  private fun withRotation(call: (String) -> JSONArray?): List<Station> {
    val arr = try {
      call(resolveHost())
    } catch (e: Exception) {
      rotate()
      try { call(resolveHost()) } catch (e2: Exception) { null }
    } ?: return emptyList()
    return (0 until arr.length()).mapNotNull { i -> stationFrom(arr.optJSONObject(i)) }
  }

  private fun stationFrom(o: JSONObject?): Station? {
    o ?: return null
    val streamUrl = o.optString("url_resolved").takeIf { it.isNotEmpty() } ?: o.optString("url").takeIf { it.isNotEmpty() } ?: return null
    val uuid = o.optString("stationuuid").takeIf { it.isNotEmpty() } ?: return null
    return Station(
      uuid = uuid,
      name = o.optString("name").ifBlank { "Unnamed station" },
      streamUrl = streamUrl,
      faviconUrl = o.optString("favicon").takeIf { it.isNotEmpty() },
      country = o.optString("country").takeIf { it.isNotEmpty() },
      countryCode = o.optString("countrycode").takeIf { it.isNotEmpty() },
      tags = o.optString("tags").split(",").map { it.trim() }.filter { it.isNotEmpty() },
      codec = o.optString("codec").takeIf { it.isNotEmpty() },
      bitrate = o.optInt("bitrate").takeIf { it > 0 },
      homepage = o.optString("homepage").takeIf { it.isNotEmpty() },
    )
  }

  private companion object {
    // Known long-lived mirrors of the Radio Browser server pool (the DNS round-robin at
    // all.api.radio-browser.info resolves to these same machines); a fixed list avoids adding a
    // DNS-over-HTTP dependency just to discover them.
    val MIRRORS = listOf(
      "https://de1.api.radio-browser.info",
      "https://de2.api.radio-browser.info",
      "https://nl1.api.radio-browser.info",
      "https://at1.api.radio-browser.info",
    )
  }
}

class RadioBrowserException(message: String) : Exception(message)
