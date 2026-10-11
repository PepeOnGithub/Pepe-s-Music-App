package xyz.pepe.glacierclient.musicapp.data.remote

import android.content.SharedPreferences
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

data class LyricsLine(val atMs: Long, val text: String)
data class Lyrics(val plain: String?, val synced: List<LyricsLine>?)

/**
 * lrclib.net — a free, keyless, open lyrics database (LRC-format synced lyrics + plain lyrics),
 * the same source several open-source players (e.g. some LRCGET-based tools) use. No API key,
 * no login. Returns null rather than placeholder text when nothing matches.
 *
 * Every real (non-null) result is cached to disk keyed by title+artist, so opening lyrics for a
 * track you've already looked up works fully offline on repeat views — a real "offline lyrics"
 * behavior, not a fake toggle with nothing behind it.
 */
class LyricsClient(private val http: OkHttpClient, private val cachePrefs: SharedPreferences? = null) {
  /** Returns null when lrclib has nothing for this track; throws on network failure so the UI can
   * say "couldn't load" instead of wrongly claiming there are no lyrics. */
  fun search(title: String, artist: String, durationMs: Long? = null): Lyrics? {
    val cleanTitle = cleanTitle(title)
    val cleanArtist = cleanArtist(artist)
    val cacheKey = "$cleanTitle|$cleanArtist".lowercase()
    cachePrefs?.getString(cacheKey, null)?.let { cached -> return runCatching { decode(JSONObject(cached)) }.getOrNull() }

    // Exact signature match first (title + artist + duration is how lrclib disambiguates covers,
    // remixes and live versions); fall back to fuzzy search, which used to be the only path and
    // was fed raw YouTube titles like "Song (Official Video) [4K]" that rarely matched anything.
    val exact = durationMs?.takeIf { it > 0 }?.let { d ->
      val q = "track_name=${enc(cleanTitle)}&artist_name=${enc(cleanArtist)}&duration=${d / 1000}"
      get("https://lrclib.net/api/get?$q")?.let { runCatching { JSONObject(it) }.getOrNull() }
    }
    val best = exact ?: run {
      val body = get("https://lrclib.net/api/search?q=${enc("$cleanTitle $cleanArtist")}") ?: return null
      val arr = runCatching { JSONArray(body) }.getOrNull() ?: return null
      // lrclib ranks by relevance already; take the first non-instrumental result.
      (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }.firstOrNull { !it.optBoolean("instrumental") }
    } ?: return null
    val plain = best.optString("plainLyrics").takeIf { it.isNotBlank() && it != "null" }
    val syncedRaw = best.optString("syncedLyrics").takeIf { it.isNotBlank() && it != "null" }
    val synced = syncedRaw?.let(::parseLrc)?.takeIf { it.isNotEmpty() }
    if (plain == null && synced == null) return null
    cachePrefs?.edit()?.putString(cacheKey, encode(plain, syncedRaw))?.apply()
    return Lyrics(plain, synced)
  }

  /** Body on 2xx, null on 404 (not found), throws on any other failure. */
  private fun get(url: String): String? {
    val request = Request.Builder().url(url).header("User-Agent", "PepesMusic/1.0").build()
    return http.newCall(request).execute().use { resp ->
      when {
        resp.isSuccessful -> resp.body?.string().orEmpty()
        resp.code == 404 -> null
        else -> throw java.io.IOException("lrclib HTTP ${resp.code}")
      }
    }
  }

  private fun enc(v: String) = java.net.URLEncoder.encode(v, "UTF-8")

  private fun encode(plain: String?, syncedRaw: String?) = JSONObject().put("plain", plain ?: "").put("synced", syncedRaw ?: "").toString()
  private fun decode(o: JSONObject): Lyrics {
    val plain = o.optString("plain").takeIf { it.isNotBlank() }
    val synced = o.optString("synced").takeIf { it.isNotBlank() }?.let(::parseLrc)
    return Lyrics(plain, synced)
  }

  // The tested domain parser (multiple timestamps per line, [mm:ss] without fractions, [offset:]);
  // the ad-hoc regex that was here silently dropped all three.
  private fun parseLrc(raw: String): List<LyricsLine> =
    xyz.pepe.glacierclient.musicapp.domain.util.Lrc.parse(raw).map { LyricsLine(it.timeMs, it.text) }

  companion object {
    private val NOISE = Regex("""\s*[(\[][^)\]]*(official|video|audio|lyric|visuali[sz]er|4k|hd|hq|remaster|explicit|clean)[^)\]]*[)\]]""", RegexOption.IGNORE_CASE)
    private val FEAT = Regex("""\s*[(\[]?\s*(feat\.?|ft\.?|featuring)\s.*$""", RegexOption.IGNORE_CASE)

    fun cleanTitle(t: String) = t.replace(NOISE, "").replace(FEAT, "").trim().ifEmpty { t }
    fun cleanArtist(a: String) = a.removeSuffix(" - Topic").split(",", "&", " x ", " X ").first().replace(FEAT, "").trim().ifEmpty { a }
  }
}
