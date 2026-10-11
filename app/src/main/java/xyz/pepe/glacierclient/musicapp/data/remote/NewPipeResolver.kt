package xyz.pepe.glacierclient.musicapp.data.remote

import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response

/**
 * Resolves full YouTube audio streams through NewPipeExtractor, which deciphers the player's
 * signature/"n" parameters the way the web player does. Hand-rolled innertube clients can't do
 * this: the ones that return plain URLs (IOS, ANDROID) get cut off after ~1 MB without a PO
 * token, and the ones that don't (ANDROID_VR) are bot-walled on many music tracks — both
 * verified with curl against a real track.
 */
class NewPipeResolver(private val http: OkHttpClient) {
  @Volatile private var initialized = false

  private fun ensureInit() {
    if (initialized) return
    synchronized(this) {
      if (!initialized) { NewPipe.init(OkHttpDownloader(http)); initialized = true }
    }
  }

  /** Resolved URLs stay valid for ~6h (googlevideo `expire` param); reuse them well inside that. */
  private val cache = java.util.concurrent.ConcurrentHashMap<String, Pair<String, Long>>()

  /** Initializes NewPipe off the critical path so the first tap doesn't pay for it. */
  fun warmUp() = ensureInit()

  fun audioUrl(videoId: String): String? {
    cache[videoId]?.let { (url, at) -> if (System.currentTimeMillis() - at < CACHE_MS) return url }
    ensureInit()
    // The bare stream extractor, not StreamInfo.getInfo — getInfo also loads related videos,
    // comments metadata, subtitles etc. that playback never uses.
    val extractor = ServiceList.YouTube.getStreamExtractor("https://www.youtube.com/watch?v=$videoId")
    extractor.fetchPage()
    val url = extractor.audioStreams.maxByOrNull { it.averageBitrate }?.content ?: return null
    cache[videoId] = url to System.currentTimeMillis()
    return url
  }

  private companion object { const val CACHE_MS = 4 * 60 * 60 * 1000L }

  private class OkHttpDownloader(private val http: OkHttpClient) : Downloader() {
    override fun execute(request: Request): Response {
      val builder = okhttp3.Request.Builder().url(request.url())
      request.headers().forEach { (k, values) ->
        builder.removeHeader(k)
        values.forEach { builder.addHeader(k, it) }
      }
      if (request.headers().keys.none { it.equals("User-Agent", true) }) {
        builder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0")
      }
      val data = request.dataToSend()
      builder.method(request.httpMethod(), data?.toRequestBody())
      http.newCall(builder.build()).execute().use { resp ->
        return Response(resp.code, resp.message, resp.headers.toMultimap(), resp.body?.string(), resp.request.url.toString())
      }
    }
  }
}
