package xyz.pepe.glacierclient.musicapp.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import xyz.pepe.glacierclient.musicapp.domain.model.Source
import xyz.pepe.glacierclient.musicapp.domain.model.Track
import java.net.URLEncoder

/**
 * Client for ccMixter (ccmixter.org) — a Creative Commons remix community with a free, documented,
 * no-key JSON API. Verified by hand: `download_url`s return full-length files (a real request
 * returned a 6.3MB MP3, not a preview), but the host hotlink-protects them — a plain request gets
 * `403`, a request with `Referer: https://ccmixter.org/` gets `200`. See
 * [xyz.pepe.glacierclient.musicapp.playback.PlaybackService] for where that header is attached
 * only for this host, not globally.
 */
class CcMixterClient(private val http: OkHttpClient) {
  private fun get(url: String): JSONArray {
    val request = Request.Builder().url(url).build()
    http.newCall(request).execute().use { resp ->
      val text = resp.body?.string().orEmpty()
      if (!resp.isSuccessful || text.isBlank()) throw CcMixterException("HTTP ${resp.code}")
      return JSONArray(text)
    }
  }

  fun search(query: String, limit: Int = 15): List<Track> {
    val q = URLEncoder.encode(query, "UTF-8")
    val arr = runCatching { get("https://ccmixter.org/api/query?f=json&limit=$limit&search=$q") }.getOrNull() ?: return emptyList()
    return (0 until arr.length()).mapNotNull { i -> trackFrom(arr.optJSONObject(i)) }
  }

  private fun trackFrom(o: JSONObject?): Track? {
    o ?: return null
    val files = o.optJSONArray("files") ?: return null
    var best: JSONObject? = null
    for (i in 0 until files.length()) {
      val f = files.optJSONObject(i) ?: continue
      if (f.optJSONObject("file_format_info")?.optString("media-type") != "audio") continue
      // Prefer MP3 over FLAC — much lighter for mobile data, and every result has one.
      if (best == null || f.optJSONObject("file_format_info")?.optString("default-ext") == "mp3") best = f
    }
    val url = best?.optString("download_url")?.takeIf { it.isNotEmpty() } ?: return null
    val durationMs = best.optJSONObject("file_format_info")?.optString("ps")?.let { parseDuration(it) }
    return Track(
      source = Source.CCMIXTER,
      sourceId = o.optString("upload_id"),
      title = o.optString("upload_name").ifBlank { "Untitled" },
      artist = o.optString("user_name").ifBlank { "ccMixter" },
      durationMs = durationMs,
      streamUrl = url,
      isPreview = false,
      license = o.optString("license_name").takeIf { it.isNotEmpty() },
      shareUrl = o.optString("file_page_url").takeIf { it.isNotEmpty() },
    )
  }

  private fun parseDuration(mmss: String): Long? {
    val parts = mmss.split(":").mapNotNull { it.toIntOrNull() }
    if (parts.size != 2) return null
    return ((parts[0] * 60 + parts[1]) * 1000).toLong()
  }
}

class CcMixterException(message: String) : Exception(message)
