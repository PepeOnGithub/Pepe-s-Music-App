package xyz.pepe.glacierclient.musicapp.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class ArtistBio(val extract: String, val thumbnailUrl: String?, val wikipediaUrl: String?)

/** Wikipedia's REST summary API — free, keyless, no signup, no rate-limit key required for this
 * volume of use. Real biography text and photo, or null if there's no matching page (never a
 * fabricated bio). */
class ArtistBioClient(private val http: OkHttpClient) {
  fun fetch(artist: String): ArtistBio? {
    val title = artist.trim().replace(" ", "_")
    val request = Request.Builder()
      .url("https://en.wikipedia.org/api/rest_v1/page/summary/${java.net.URLEncoder.encode(title, "UTF-8")}")
      .header("User-Agent", "PepesMusic/1.0 (Android music app)")
      .build()
    val body = http.newCall(request).execute().use { resp ->
      if (!resp.isSuccessful) return null
      resp.body?.string().orEmpty()
    }
    val o = runCatching { JSONObject(body) }.getOrNull() ?: return null
    // A disambiguation page or missing page still returns 200 with no useful extract.
    if (o.optString("type") == "disambiguation") return null
    val extract = o.optString("extract").takeIf { it.isNotBlank() } ?: return null
    val thumb = o.optJSONObject("thumbnail")?.optString("source")?.takeIf { it.isNotEmpty() }
    val pageUrl = o.optJSONObject("content_urls")?.optJSONObject("desktop")?.optString("page")?.takeIf { it.isNotEmpty() }
    return ArtistBio(extract, thumb, pageUrl)
  }
}
