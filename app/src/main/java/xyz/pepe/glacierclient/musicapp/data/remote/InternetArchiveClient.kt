package xyz.pepe.glacierclient.musicapp.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import xyz.pepe.glacierclient.musicapp.domain.model.Source
import xyz.pepe.glacierclient.musicapp.domain.model.Track
import java.net.URLEncoder

/**
 * Client for the Internet Archive's audio collection (archive.org) — free, no key, no account,
 * and genuinely full-length: every item's `/metadata/<id>` response lists a direct file URL
 * (verified by hand: HTTP 200, `Content-Type: audio/mpeg`, `Accept-Ranges: bytes` — a real,
 * seekable stream, not a preview). Mostly independent/Creative-Commons/public-domain releases
 * rather than mainstream chart music, but a genuinely reliable full-track source with none of
 * YouTube Music's bot-detection problems.
 */
class InternetArchiveClient(private val http: OkHttpClient) {
  private fun get(url: String): JSONObject {
    val request = Request.Builder().url(url).build()
    http.newCall(request).execute().use { resp ->
      val text = resp.body?.string().orEmpty()
      if (!resp.isSuccessful || text.isBlank()) throw InternetArchiveException("HTTP ${resp.code}")
      return JSONObject(text)
    }
  }

  suspend fun search(query: String, limit: Int = 15): List<Track> = coroutineScope {
    val q = URLEncoder.encode("($query) AND mediatype:(audio)", "UTF-8")
    val url = "https://archive.org/advancedsearch.php?q=$q&fl[]=identifier&fl[]=title&fl[]=creator&rows=$limit&output=json"
    val docs = runCatching { get(url).optJSONObject("response")?.optJSONArray("docs") }.getOrNull() ?: return@coroutineScope emptyList()
    (0 until docs.length()).map { i ->
      async(Dispatchers.IO) { runCatching { trackFrom(docs.optJSONObject(i)) }.getOrNull() }
    }.awaitAll().filterNotNull()
  }

  private fun trackFrom(doc: JSONObject?): Track? {
    val id = doc?.optString("identifier")?.takeIf { it.isNotEmpty() } ?: return null
    val meta = get("https://archive.org/metadata/$id")
    val server = meta.optString("server").takeIf { it.isNotEmpty() } ?: return null
    val dir = meta.optString("dir").takeIf { it.isNotEmpty() } ?: return null
    val files = meta.optJSONArray("files") ?: return null
    var best: JSONObject? = null
    for (i in 0 until files.length()) {
      val f = files.optJSONObject(i) ?: continue
      val name = f.optString("name")
      if (!AUDIO_EXTENSIONS.any { name.endsWith(it, ignoreCase = true) }) continue
      // Prefer a plain MP3 derivative over lossless masters (much faster to stream on mobile data).
      if (best == null || f.optString("format") == "VBR MP3") best = f
    }
    val fileName = best?.optString("name")?.takeIf { it.isNotEmpty() } ?: return null
    val streamUrl = "https://$server$dir/${URLEncoder.encode(fileName, "UTF-8").replace("+", "%20")}"
    val durationSec = best.optString("length").toDoubleOrNull()
    return Track(
      source = Source.INTERNET_ARCHIVE,
      sourceId = id,
      title = doc.optString("title").ifBlank { fileName },
      artist = doc.optString("creator").ifBlank { "Internet Archive" },
      durationMs = durationSec?.let { (it * 1000).toLong() },
      streamUrl = streamUrl,
      isPreview = false,
      shareUrl = "https://archive.org/details/$id",
    )
  }

  private companion object {
    val AUDIO_EXTENSIONS = listOf(".mp3", ".ogg", ".flac")
  }
}

class InternetArchiveException(message: String) : Exception(message)
