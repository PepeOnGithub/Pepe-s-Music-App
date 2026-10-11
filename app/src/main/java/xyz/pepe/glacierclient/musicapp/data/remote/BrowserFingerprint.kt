package xyz.pepe.glacierclient.musicapp.data.remote

/**
 * A realistic, internally-consistent desktop Chrome header set for talking to YouTube Music.
 *
 * Verified against the real implementation in brahmkshatriya/echo's YouTube extension
 * (echo-youtube-extension-old, `YoutubeiBrowseResponse`/`YoutubeExtension.kt`): the earlier
 * attempt here only set a `User-Agent` string, but YouTube's bot detection also checks that a
 * request's Chrome "Client Hints" (`sec-ch-ua-*`) and `Sec-Fetch-*` headers are present and agree
 * with that UA — Echo sends the full set on *both* the innertube API calls and the actual
 * audio-byte fetch from googlevideo.com, not just one or the other. The Echo app also does this
 * with a rotating pool of UA strings and a multi-strategy retry ladder; this is a smaller, static
 * subset of that (one consistent header set, no rotation) — enough to stop sending an obviously
 * non-browser fingerprint, not a full reimplementation of Echo's retry engine.
 */
object BrowserFingerprint {
  const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/139.0.7258.128 Safari/537.36"

  private val CLIENT_HINTS = mapOf(
    "sec-ch-ua" to "\"Not;A=Brand\";v=\"99\", \"Google Chrome\";v=\"139\", \"Chromium\";v=\"139\"",
    "sec-ch-ua-mobile" to "?0",
    "sec-ch-ua-platform" to "\"Windows\"",
  )

  /** For requests to music.youtube.com/youtubei/v1/ endpoints (search, player). */
  val INNERTUBE_HEADERS: Map<String, String> = CLIENT_HINTS + mapOf(
    "User-Agent" to DESKTOP_USER_AGENT,
    "Origin" to "https://music.youtube.com",
    "Referer" to "https://music.youtube.com/",
    "Sec-Fetch-Dest" to "empty",
    "Sec-Fetch-Mode" to "cors",
    "Sec-Fetch-Site" to "same-origin",
  )

  /** For the actual audio-byte fetch from *.googlevideo.com once a stream URL is resolved. */
  val STREAMING_HEADERS: Map<String, String> = CLIENT_HINTS + mapOf(
    "User-Agent" to DESKTOP_USER_AGENT,
    "Origin" to "https://music.youtube.com",
    "Referer" to "https://music.youtube.com/",
    "Sec-Fetch-Dest" to "empty",
    "Sec-Fetch-Mode" to "cors",
    "Sec-Fetch-Site" to "cross-site",
    "X-Browser-Channel" to "stable",
    "X-Browser-Year" to "2025",
  )
}
