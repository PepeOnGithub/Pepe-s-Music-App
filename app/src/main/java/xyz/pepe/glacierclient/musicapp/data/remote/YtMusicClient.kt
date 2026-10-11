package xyz.pepe.glacierclient.musicapp.data.remote

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import xyz.pepe.glacierclient.musicapp.data.repository.YtAuthRepository
import xyz.pepe.glacierclient.musicapp.domain.model.Source
import xyz.pepe.glacierclient.musicapp.domain.model.Track

/**
 * Client for YouTube Music's internal "innertube" API (the same unofficial endpoint SimpMusic,
 * NewPipe and youtube-music-api use). There is no public/free contract for this: no auth key is
 * needed for search, but the player (stream-resolution) endpoint increasingly bot-walls anonymous
 * requests with a generic "video unavailable" error — verified by hand against the live API. When
 * [auth] has a session cookie (from [xyz.pepe.glacierclient.musicapp.ui.settings.YtLoginPage]),
 * requests are signed the same way a logged-in browser tab would be, which is far less likely to
 * hit that wall. See docs/DECISIONS.md for the full investigation.
 *
 * Deciphering signature-ciphered stream URLs (YouTube's per-video JS obfuscation) is NOT
 * implemented here; only formats that already expose a direct `url` are used. This is a known
 * limitation documented in docs/RELIABILITY.md — some tracks may have no playable stream.
 */
class YtMusicClient(private val http: OkHttpClient, private val auth: YtAuthRepository) {
  private val json = "application/json".toMediaType()

  private fun context() = JSONObject().put(
    "context",
    JSONObject().put(
      "client",
      JSONObject()
        .put("clientName", "WEB_REMIX")
        .put("clientVersion", "1.20240101.01.00")
        .put("hl", "en")
        .put("gl", "US"),
    ),
  )

  private fun post(endpoint: String, body: JSONObject): JSONObject {
    val builder = Request.Builder()
      .url("https://music.youtube.com/youtubei/v1/$endpoint?key=$API_KEY")
      .header("X-Goog-Api-Format-Version", "1")
      .header("Content-Type", "application/json")
    // A real browser sends a full, internally-consistent Client-Hints fingerprint alongside its
    // User-Agent, not just the UA string alone — see BrowserFingerprint's doc comment for how
    // this was verified against a real, working implementation.
    BrowserFingerprint.INNERTUBE_HEADERS.forEach { (k, v) -> builder.header(k, v) }
    auth.cookie.value?.let { cookie ->
      builder.header("Cookie", cookie)
      builder.header("X-Origin", "https://music.youtube.com")
      SapisidHash.authorizationHeader(cookie)?.let { builder.header("Authorization", it) }
    }
    val request = builder.post(body.toString().toRequestBody(json)).build()
    http.newCall(request).execute().use { resp ->
      val text = resp.body?.string().orEmpty()
      if (!resp.isSuccessful || text.isBlank()) throw YtMusicException("HTTP ${resp.code}")
      return JSONObject(text)
    }
  }

  /** Searches songs and maps results to [Track]s (isPreview = false; streamUrl resolved lazily). */
  fun search(query: String): List<Track> {
    val body = context().put("query", query).put("params", SONGS_PARAMS)
    val root = post("search", body)
    val results = mutableListOf<Track>()
    findRenderers(root, "musicResponsiveListItemRenderer") { renderer ->
      trackFromRenderer(renderer)?.let(results::add)
    }
    return results
  }

  /** Resolves a playable stream URL for [videoId], or null if only ciphered formats are available. */
  private val newPipe = NewPipeResolver(http)

  /** The signed-in account's display name and avatar URL, from the same menu the web app's avatar opens. */
  fun accountInfo(): Pair<String, String?>? {
    if (auth.cookie.value == null) return null
    val header = post("account/account_menu", context())
      .optJSONArray("actions")?.optJSONObject(0)
      ?.optJSONObject("openPopupAction")?.optJSONObject("popup")
      ?.optJSONObject("multiPageMenuRenderer")?.optJSONObject("header")
      ?.optJSONObject("activeAccountHeaderRenderer") ?: return null
    val name = header.optJSONObject("accountName")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
      ?.takeIf { it.isNotBlank() } ?: return null
    val photo = header.optJSONObject("accountPhoto")?.optJSONArray("thumbnails")
      ?.let { it.optJSONObject(it.length() - 1)?.optString("url") }
    return name to photo
  }

  fun warmUp() = runCatching { newPipe.warmUp() }

  fun streamUrl(videoId: String): String? {
    runCatching { newPipe.audioUrl(videoId) }
      .onFailure { android.util.Log.w("YtMusicClient", "NewPipe $videoId failed", it) }
      .getOrNull()?.let { return it }
    // WEB_REMIX's player response only carries signatureCipher formats (needs JS deciphering and
    // a PO token), so it never yields a playable URL. The IOS and ANDROID_VR clients return
    // direct googlevideo URLs anonymously (verified with curl: 206 on a ranged GET). Each gets
    // "confirm you're not a bot" on some videos, so try them in turn.
    val root = STREAM_CLIENTS.firstNotNullOfOrNull { (client, ua) ->
      val body = JSONObject().put("videoId", videoId).put("context", JSONObject().put("client", JSONObject(client.toString())))
      val request = Request.Builder()
        .url("https://www.youtube.com/youtubei/v1/player?prettyPrint=false")
        .header("Content-Type", "application/json")
        .header("User-Agent", ua)
        .post(body.toString().toRequestBody(json))
        .build()
      val r = runCatching {
        http.newCall(request).execute().use { resp -> JSONObject(resp.body?.string().orEmpty()) }
      }.getOrNull()
      val status = r?.optJSONObject("playabilityStatus")?.optString("status")
      if (status != "OK") android.util.Log.w("YtMusicClient", "player $videoId via ${client.optString("clientName")}: $status")
      r?.takeIf { status == "OK" }
    } ?: return null
    val formats = JSONArray()
    root.optJSONObject("streamingData")?.let { sd ->
      sd.optJSONArray("adaptiveFormats")?.let { formats.putAll(it) }
      sd.optJSONArray("formats")?.let { formats.putAll(it) }
    }
    // Highest-bitrate audio-only format with a direct URL (signatureCipher-only ones are skipped,
    // undeciphered). The old filter let muxed video formats through and, having the higher
    // bitrate, they won — streaming a whole video just to play its audio.
    val direct = (0 until formats.length()).mapNotNull { formats.optJSONObject(it) }.filter { it.has("url") }
    val best = direct.filter { it.optString("mimeType").startsWith("audio/") }.maxByOrNull { it.optInt("bitrate") }
      ?: direct.minByOrNull { it.optInt("bitrate") } // muxed fallback: smallest, it's only for the audio
    return best?.optString("url")
  }

  private fun JSONArray.putAll(other: JSONArray) { for (i in 0 until other.length()) put(other.get(i)) }

  private fun trackFromRenderer(renderer: JSONObject): Track? {
    val columns = renderer.optJSONArray("flexColumns") ?: return null
    val title = textFromColumn(columns.optJSONObject(0)) ?: return null
    val subtitle = textFromColumn(columns.optJSONObject(1)).orEmpty()
    val artist = subtitle.split(" • ").firstOrNull()?.trim().orEmpty().ifEmpty { "Unknown" }
    // Must be the videoId this exact row plays, not just any videoId nested somewhere inside the
    // renderer's JSON (the row also embeds a "…" menu with unrelated videoIds for "Start radio",
    // related videos, etc. — a blind recursive search picks those up and plays the wrong song).
    val videoId = renderer.optJSONObject("playlistItemData")?.optString("videoId")?.takeIf { it.isNotEmpty() }
      ?: playButtonVideoId(renderer)
      ?: return null
    val thumb = renderer.optJSONObject("thumbnail")
      ?.optJSONObject("musicThumbnailRenderer")
      ?.optJSONObject("thumbnail")
      ?.optJSONArray("thumbnails")
      ?.let { arr -> arr.optJSONObject(arr.length() - 1)?.optString("url") }
      ?.let { upscaleThumbnail(it) }
    return Track(
      source = Source.YOUTUBE_MUSIC,
      sourceId = videoId,
      title = title,
      artist = artist,
      artworkUrl = thumb,
      streamUrl = null, // resolved on demand via streamUrl(videoId) right before playback
      isPreview = false,
      shareUrl = "https://music.youtube.com/watch?v=$videoId",
    )
  }

  /**
   * Search-result thumbnails come back tiny (e.g. `=w60-h60-l90-rj`) — real clients request a
   * larger render of the same underlying image by bumping the size in the URL itself rather than
   * fetching a different asset; verified with curl that `=w544-h544-l90-rj` 200s and actually
   * decodes at 544×544, not just accepts the URL.
   */
  private fun upscaleThumbnail(url: String): String =
    if (url.contains("googleusercontent.com")) Regex("=w\\d+-h\\d+").replaceFirst(url, "=w544-h544") else url

  /**
   * The canonical field for "the video this row plays": the play button overlaid on the row's own
   * thumbnail. This is the same path ytmusicapi/SimpMusic-style parsers use, specifically because
   * it's the one place in the renderer that's guaranteed to reference this row's own track rather
   * than a related/menu video.
   */
  private fun playButtonVideoId(renderer: JSONObject): String? =
    renderer.optJSONObject("overlay")
      ?.optJSONObject("musicItemThumbnailOverlayRenderer")
      ?.optJSONObject("content")
      ?.optJSONObject("musicPlayButtonRenderer")
      ?.optJSONObject("playNavigationEndpoint")
      ?.optJSONObject("watchEndpoint")
      ?.optString("videoId")
      ?.takeIf { it.isNotEmpty() }

  private fun textFromColumn(column: JSONObject?): String? {
    val runs = column
      ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
      ?.optJSONObject("text")
      ?.optJSONArray("runs") ?: return null
    return (0 until runs.length()).joinToString("") { runs.optJSONObject(it)?.optString("text").orEmpty() }
  }

  /** Recursively finds every JSON object with key == [key] and invokes [onFound] on each. */
  private fun findRenderers(node: Any?, key: String, onFound: (JSONObject) -> Unit) {
    when (node) {
      is JSONObject -> {
        node.optJSONObject(key)?.let(onFound)
        for (k in node.keys()) findRenderers(node.opt(k), key, onFound)
      }
      is JSONArray -> for (i in 0 until node.length()) findRenderers(node.opt(i), key, onFound)
    }
  }

  private companion object {
    val STREAM_CLIENTS = listOf(
      JSONObject("""{"clientName":"ANDROID_VR","clientVersion":"1.61.48","deviceMake":"Oculus","deviceModel":"Quest 3","androidSdkVersion":32,"osName":"Android","osVersion":"12L","hl":"en","gl":"US"}""") to
        "com.google.android.apps.youtube.vr.oculus/1.61.48 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip",
    )
    // Public web client key used by music.youtube.com itself; not a secret credential.
    const val API_KEY = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30"
    const val SONGS_PARAMS = "EgWKAQIIAWoKEAMQBBAJEAoQBQ%3D%3D"
  }
}

class YtMusicException(message: String) : Exception(message)
